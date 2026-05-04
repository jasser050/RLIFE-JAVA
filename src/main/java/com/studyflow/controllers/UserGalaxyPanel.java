package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 3D Galaxy visualization — each star = one user.
 * Canvas nebulae backdrop + live 3D floating background stars + glowing user stars.
 */
public class UserGalaxyPanel extends StackPane {

    private static final Color ADMIN_COLOR    = Color.web("#FFD700");
    private static final Color BANNED_COLOR   = Color.web("#FF3333");
    private static final Color NEW_COLOR      = Color.web("#60A5FA");
    private static final Color ACTIVE_COLOR   = Color.web("#34D399");
    private static final Color INACTIVE_COLOR = Color.web("#6B7280");

    private final Group root3D = new Group();
    private final Group starsGroup = new Group();
    private final Group bgStarsGroup = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private double cameraAngleX = -20;
    private double cameraAngleY = 0;
    private double cameraDistance = 160;
    private double dragStartX, dragStartY;
    private double dragAngleX, dragAngleY;
    private double autoRotateAngle = 0;
    private boolean isDragging = false;
    private long lastDragTime = 0;

    private final List<UserStarData> userStars = new ArrayList<>();
    private final List<BgStarData> bgStars = new ArrayList<>();
    private final Map<String, Image> glowCache = new HashMap<>();
    private final Random random = new Random(42);
    private AnimationTimer timer;
    private VBox userCard;
    private Canvas bgCanvas;

    /** User star — glowing sphere representing a real user. */
    private record UserStarData(Sphere sphere, User user, Color color,
                                Scale scale, double pulsePhase, double pulseSpeed) {}

    /** Background star — alive, floating, twinkling in 3D space. */
    private record BgStarData(Sphere sphere, Scale scale,
                              double baseX, double baseY, double baseZ,
                              double twinklePhase, double twinkleSpeed,
                              double driftPhase, double driftSpeed, double driftAmount) {}

    public UserGalaxyPanel() {
        // Layer 1: Canvas — nebulae, vignette, milky way (static atmospheric backdrop)
        bgCanvas = new Canvas();
        bgCanvas.widthProperty().bind(this.widthProperty());
        bgCanvas.heightProperty().bind(this.heightProperty());
        this.getChildren().add(bgCanvas);

        // Layer 2: 3D SubScene (transparent) — live bg stars + user stars
        buildScene();

        // Layer 3: UI overlays
        buildUserCard();
        buildLegend();

        // Repaint canvas on resize
        this.widthProperty().addListener((o, a, b) -> paintNebulaeBackdrop());
        this.heightProperty().addListener((o, a, b) -> paintNebulaeBackdrop());

        // Consume scroll so admin dashboard doesn't scroll
        this.addEventFilter(ScrollEvent.SCROLL, e -> {
            cameraDistance = Math.max(30, Math.min(400, cameraDistance - e.getDeltaY() * 0.3));
            e.consume();
        });

        // Defer heavy work until in scene
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && userStars.isEmpty()) {
                paintNebulaeBackdrop();
                buildLiveBgStars();
                loadUsers();
                startAnimation();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  3D Scene setup
    // ══════════════════════════════════════════════════════════════════════════════

    private void buildScene() {
        camera.setNearClip(0.1);
        camera.setFarClip(3000);
        camera.setFieldOfView(55);

        // Ambient light — enough to softly see everything
        AmbientLight ambient = new AmbientLight(Color.web("#1e1e3a"));
        root3D.getChildren().add(ambient);

        // Central purple glow
        PointLight centerLight = new PointLight(Color.web("#8B5CF6", 0.6));
        root3D.getChildren().add(centerLight);

        // Warm accent
        PointLight warmLight = new PointLight(Color.web("#EC4899", 0.25));
        warmLight.setTranslateX(50);
        warmLight.setTranslateY(-30);
        warmLight.setTranslateZ(50);
        root3D.getChildren().add(warmLight);

        // Cool accent
        PointLight coolLight = new PointLight(Color.web("#3B82F6", 0.2));
        coolLight.setTranslateX(-40);
        coolLight.setTranslateY(20);
        coolLight.setTranslateZ(-40);
        root3D.getChildren().add(coolLight);

        root3D.getChildren().add(bgStarsGroup);
        root3D.getChildren().add(starsGroup);

        SubScene subScene = new SubScene(root3D, 100, 100, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);
        subScene.widthProperty().bind(this.widthProperty());
        subScene.heightProperty().bind(this.heightProperty());

        subScene.setOnMousePressed(this::onMousePressed);
        subScene.setOnMouseDragged(this::onMouseDragged);
        subScene.setOnMouseReleased(e -> { isDragging = false; lastDragTime = System.currentTimeMillis(); });
        subScene.setOnMouseClicked(this::onStarClick);

        this.getChildren().add(subScene);
        this.setStyle("-fx-background-color: #010409;");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Canvas — static atmospheric nebula/vignette backdrop
    // ══════════════════════════════════════════════════════════════════════════════

    private void paintNebulaeBackdrop() {
        double w = bgCanvas.getWidth();
        double h = bgCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = bgCanvas.getGraphicsContext2D();

        // Deep space base
        gc.setFill(Color.web("#010409"));
        gc.fillRect(0, 0, w, h);

        // Nebula glow clouds
        nebulaBlob(gc, w * 0.45, h * 0.40, w * 0.55, "#4C1D95", 0.06, "#7C3AED", 0.025);
        nebulaBlob(gc, w * 0.72, h * 0.25, w * 0.35, "#1E3A5F", 0.05, "#3B82F6", 0.02);
        nebulaBlob(gc, w * 0.22, h * 0.68, w * 0.30, "#831843", 0.04, "#EC4899", 0.015);
        nebulaBlob(gc, w * 0.60, h * 0.72, w * 0.25, "#134E4A", 0.03, "#14B8A6", 0.012);
        gc.applyEffect(new GaussianBlur(45));

        // Second depth layer
        nebulaBlob(gc, w * 0.50, h * 0.50, w * 0.70, "#2E1065", 0.03, "#4C1D95", 0.01);
        nebulaBlob(gc, w * 0.30, h * 0.35, w * 0.45, "#172554", 0.025, "#1E40AF", 0.008);
        gc.applyEffect(new GaussianBlur(70));

        // Vignette
        double cx = w / 2, cy = h / 2, r = Math.max(w, h) * 0.72;
        RadialGradient vig = new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.25, Color.TRANSPARENT),
                new Stop(0.75, Color.web("#010409", 0.55)),
                new Stop(1.00, Color.web("#010409", 0.90)));
        gc.setFill(vig);
        gc.fillRect(0, 0, w, h);

        // Milky way band — faint star cluster stripe
        Random rng = new Random(77777);
        gc.save();
        gc.translate(w / 2, h / 2);
        gc.rotate(-12);
        for (int i = 0; i < 500; i++) {
            double sx = rng.nextGaussian() * w * 0.38;
            double sy = rng.nextGaussian() * h * 0.04;
            double a = 0.10 + rng.nextDouble() * 0.25;
            double sz = 0.3 + rng.nextDouble() * 0.6;
            gc.setGlobalAlpha(a);
            double t = rng.nextDouble();
            if (t < 0.5) gc.setFill(Color.color(0.82, 0.85, 0.96));
            else if (t < 0.8) gc.setFill(Color.color(0.90, 0.88, 0.95));
            else gc.setFill(Color.color(0.75, 0.80, 1.0));
            gc.fillOval(sx, sy, sz, sz);
        }
        gc.setGlobalAlpha(1.0);
        gc.restore();
    }

    private void nebulaBlob(GraphicsContext gc, double cx, double cy, double r,
                            String innerHex, double innerA, String outerHex, double outerA) {
        RadialGradient rg = new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(innerHex, innerA)),
                new Stop(0.4, Color.web(outerHex, outerA)),
                new Stop(1.0, Color.TRANSPARENT));
        gc.setFill(rg);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Live 3D Background Stars — floating, twinkling, alive
    // ══════════════════════════════════════════════════════════════════════════════

    private void buildLiveBgStars() {
        // Layer 1: Distant stars — far away, gentle twinkle
        for (int i = 0; i < 600; i++) {
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * random.nextDouble() - 1);
            double r = 400 + random.nextDouble() * 600;

            double bx = r * Math.sin(phi) * Math.cos(theta);
            double by = r * Math.cos(phi);
            double bz = r * Math.sin(phi) * Math.sin(theta);

            Color color = randomStarColor(random);
            Sphere s = createBgStar(0.3 + random.nextDouble() * 0.5, color);
            s.setTranslateX(bx);
            s.setTranslateY(by);
            s.setTranslateZ(bz);

            Scale scale = new Scale(1, 1, 1, 0, 0, 0);
            s.getTransforms().add(scale);
            bgStarsGroup.getChildren().add(s);

            bgStars.add(new BgStarData(s, scale, bx, by, bz,
                    random.nextDouble() * Math.PI * 2,
                    0.4 + random.nextDouble() * 1.5,
                    random.nextDouble() * Math.PI * 2,
                    0.02 + random.nextDouble() * 0.06,
                    1.5 + random.nextDouble() * 4.0));
        }

        // Layer 2: Mid-range stars — closer, brighter, more visible twinkle + drift
        for (int i = 0; i < 300; i++) {
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * random.nextDouble() - 1);
            double r = 150 + random.nextDouble() * 350;

            double bx = r * Math.sin(phi) * Math.cos(theta);
            double by = r * Math.cos(phi);
            double bz = r * Math.sin(phi) * Math.sin(theta);

            Color color = randomStarColor(random);
            Sphere s = createBgStar(0.4 + random.nextDouble() * 0.7, color);
            s.setTranslateX(bx);
            s.setTranslateY(by);
            s.setTranslateZ(bz);

            Scale scale = new Scale(1, 1, 1, 0, 0, 0);
            s.getTransforms().add(scale);
            bgStarsGroup.getChildren().add(s);

            bgStars.add(new BgStarData(s, scale, bx, by, bz,
                    random.nextDouble() * Math.PI * 2,
                    0.6 + random.nextDouble() * 2.0,
                    random.nextDouble() * Math.PI * 2,
                    0.03 + random.nextDouble() * 0.08,
                    2.0 + random.nextDouble() * 5.0));
        }

        // Layer 3: Bright close stars — few, large, striking twinkle
        for (int i = 0; i < 40; i++) {
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * random.nextDouble() - 1);
            double r = 120 + random.nextDouble() * 250;

            double bx = r * Math.sin(phi) * Math.cos(theta);
            double by = r * Math.cos(phi);
            double bz = r * Math.sin(phi) * Math.sin(theta);

            double temp = random.nextDouble();
            Color color;
            if (temp < 0.3) color = Color.web("#BFDBFE");      // icy blue
            else if (temp < 0.5) color = Color.web("#FDE68A"); // golden
            else if (temp < 0.7) color = Color.web("#FCA5A5"); // rose
            else color = Color.web("#E0E7FF");                  // diamond

            Sphere s = createBgStar(0.8 + random.nextDouble() * 1.0, color);
            s.setTranslateX(bx);
            s.setTranslateY(by);
            s.setTranslateZ(bz);

            Scale scale = new Scale(1, 1, 1, 0, 0, 0);
            s.getTransforms().add(scale);
            bgStarsGroup.getChildren().add(s);

            bgStars.add(new BgStarData(s, scale, bx, by, bz,
                    random.nextDouble() * Math.PI * 2,
                    0.3 + random.nextDouble() * 1.0,
                    random.nextDouble() * Math.PI * 2,
                    0.01 + random.nextDouble() * 0.04,
                    3.0 + random.nextDouble() * 6.0));
        }
    }

    /** Creates a self-illuminating sphere that glows like a real star. */
    private Sphere createBgStar(double radius, Color color) {
        Sphere s = new Sphere(radius, 6);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(color.deriveColor(0, 0.6, 0.4, 1));
        mat.setSpecularColor(color.brighter());
        mat.setSpecularPower(50);
        mat.setSelfIlluminationMap(getGlowImage(color));
        s.setMaterial(mat);
        return s;
    }

    private Color randomStarColor(Random rng) {
        double t = rng.nextDouble();
        if (t < 0.30) return Color.color(0.78, 0.84, 1.0);   // blue-white
        if (t < 0.55) return Color.color(1.0, 0.96, 0.88);   // warm white
        if (t < 0.75) return Color.color(0.88, 0.90, 1.0);   // cool white
        if (t < 0.90) return Color.color(1.0, 0.87, 0.68);   // golden
        return Color.color(0.95, 0.80, 0.80);                  // soft rose
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  User Stars — self-illuminating glowing spheres
    // ══════════════════════════════════════════════════════════════════════════════

    private void loadUsers() {
        List<User> users;
        try {
            ServiceUser serviceUser = new ServiceUser();
            users = serviceUser.getAll();
        } catch (Exception e) {
            System.out.println("Galaxy: could not load users: " + e.getMessage());
            return;
        }

        double goldenRatio = (1 + Math.sqrt(5)) / 2;

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            Color starColor = getStarColor(user);
            double starSize = getStarSize(user);

            // Fibonacci sphere → flattened disk
            double idx = i + 0.5;
            double total = Math.max(users.size(), 1);
            double phi = Math.acos(1 - (2 * idx) / total);
            double theta = (2 * Math.PI * idx) / goldenRatio;
            double r = 12 + (i / total) * 50;

            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi) * 0.25;
            double z = r * Math.sin(phi) * Math.sin(theta);

            Sphere star = new Sphere(starSize, 16);

            // Self-illuminating material — bright, vivid color
            PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(starColor);
            mat.setSpecularColor(starColor.brighter());
            mat.setSpecularPower(5);
            mat.setSelfIlluminationMap(getGlowImage(starColor.brighter()));
            star.setMaterial(mat);

            star.setTranslateX(x);
            star.setTranslateY(y);
            star.setTranslateZ(z);
            star.setUserData(user);

            Scale scale = new Scale(1, 1, 1, 0, 0, 0);
            star.getTransforms().add(scale);

            starsGroup.getChildren().add(star);

            userStars.add(new UserStarData(
                    star, user, starColor, scale,
                    random.nextDouble() * Math.PI * 2,
                    0.8 + random.nextDouble() * 1.5
            ));
        }
    }

    /** 1x1 solid color image for self-illumination — cached per hex color. */
    private Image getGlowImage(Color color) {
        String key = toHex(color);
        return glowCache.computeIfAbsent(key, k -> {
            WritableImage img = new WritableImage(1, 1);
            img.getPixelWriter().setColor(0, 0, color);
            return img;
        });
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Star classification
    // ══════════════════════════════════════════════════════════════════════════════

    private Color getStarColor(User user) {
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase("admin@rlife.com"))
            return ADMIN_COLOR;
        if (user.isBanned()) return BANNED_COLOR;
        if (user.getCreatedAt() != null && !user.getCreatedAt().isEmpty()) {
            try {
                String ds = user.getCreatedAt().length() > 10 ? user.getCreatedAt().substring(0, 10) : user.getCreatedAt();
                long days = ChronoUnit.DAYS.between(LocalDate.parse(ds, DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.now());
                if (days < 7) return NEW_COLOR;
                if (days > 90) return INACTIVE_COLOR;
            } catch (Exception ignored) {}
        }
        return ACTIVE_COLOR;
    }

    private double getStarSize(User user) {
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase("admin@rlife.com")) return 1.1;
        if (user.isBanned()) return 0.5;
        return 0.65;
    }

    private String getStatusLabel(User user) {
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase("admin@rlife.com")) return "Admin";
        if (user.isBanned()) return "Banned";
        if (user.getCreatedAt() != null && !user.getCreatedAt().isEmpty()) {
            try {
                String ds = user.getCreatedAt().length() > 10 ? user.getCreatedAt().substring(0, 10) : user.getCreatedAt();
                long days = ChronoUnit.DAYS.between(LocalDate.parse(ds, DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.now());
                if (days < 7) return "New";
                if (days > 90) return "Inactive";
            } catch (Exception ignored) {}
        }
        return "Active";
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Animation — everything alive
    // ══════════════════════════════════════════════════════════════════════════════

    private void startAnimation() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = now / 1_000_000_000.0;

                // Cinematic auto-rotate
                if (!isDragging && System.currentTimeMillis() - lastDragTime > 2000) {
                    autoRotateAngle += 0.04;
                    cameraAngleY = autoRotateAngle;
                }
                updateCamera();

                // Background stars: twinkle + gentle float
                for (BgStarData bg : bgStars) {
                    // Twinkle — scale oscillates
                    double twinkle = 0.5 + 0.5 * Math.sin(t * bg.twinkleSpeed + bg.twinklePhase);
                    bg.scale.setX(twinkle);
                    bg.scale.setY(twinkle);
                    bg.scale.setZ(twinkle);

                    // Drift — gentle floating motion
                    double dx = Math.sin(t * bg.driftSpeed + bg.driftPhase) * bg.driftAmount;
                    double dy = Math.cos(t * bg.driftSpeed * 0.7 + bg.driftPhase) * bg.driftAmount * 0.5;
                    double dz = Math.sin(t * bg.driftSpeed * 0.5 + bg.driftPhase + 1.0) * bg.driftAmount * 0.8;
                    bg.sphere.setTranslateX(bg.baseX + dx);
                    bg.sphere.setTranslateY(bg.baseY + dy);
                    bg.sphere.setTranslateZ(bg.baseZ + dz);
                }

                // User stars: pulse
                for (UserStarData sd : userStars) {
                    double pulse = 1.0 + 0.22 * Math.sin(t * sd.pulseSpeed + sd.pulsePhase);
                    sd.scale.setX(pulse);
                    sd.scale.setY(pulse);
                    sd.scale.setZ(pulse);
                }
            }
        };
        timer.start();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Camera controls
    // ══════════════════════════════════════════════════════════════════════════════

    private void updateCamera() {
        camera.getTransforms().setAll(
                new Rotate(cameraAngleY, Rotate.Y_AXIS),
                new Rotate(cameraAngleX, Rotate.X_AXIS),
                new Translate(0, 0, -cameraDistance)
        );
    }

    private void onMousePressed(MouseEvent e) {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
        dragAngleX = cameraAngleX;
        dragAngleY = cameraAngleY;
        isDragging = true;
    }

    private void onMouseDragged(MouseEvent e) {
        cameraAngleY = dragAngleY + (e.getSceneX() - dragStartX) * 0.3;
        cameraAngleX = Math.max(-80, Math.min(80, dragAngleX - (e.getSceneY() - dragStartY) * 0.3));
        autoRotateAngle = cameraAngleY;
    }

    private void onStarClick(MouseEvent e) {
        if (e.getClickCount() != 1) return;
        PickResult pick = e.getPickResult();
        if (pick == null) return;
        Node picked = pick.getIntersectedNode();
        if (picked instanceof Sphere sphere && sphere.getUserData() instanceof User user) {
            showUserCard(user);
        } else {
            hideUserCard();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  User info popup
    // ══════════════════════════════════════════════════════════════════════════════

    private void buildUserCard() {
        userCard = new VBox(10);
        userCard.setMaxWidth(260);
        userCard.setMaxHeight(Region.USE_PREF_SIZE);
        userCard.setPadding(new Insets(18, 20, 18, 20));
        userCard.setStyle(
                "-fx-background-color: rgba(6,6,22,0.94);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(139,92,246,0.35);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );
        userCard.setEffect(new DropShadow(24, Color.web("#8B5CF6", 0.3)));
        userCard.setVisible(false);
        userCard.setMouseTransparent(true);
        StackPane.setAlignment(userCard, Pos.TOP_RIGHT);
        StackPane.setMargin(userCard, new Insets(16, 16, 0, 0));
        this.getChildren().add(userCard);
    }

    private void showUserCard(User user) {
        userCard.getChildren().clear();
        Color color = getStarColor(user);
        String hex = toHex(color);
        String status = getStatusLabel(user);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(48, 48);
        avatar.setMaxSize(48, 48);
        String initials = user.getInitials() != null ? user.getInitials() : "?";
        Label iLabel = new Label(initials);
        iLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        iLabel.setTextFill(Color.WHITE);
        avatar.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 24;");
        avatar.getChildren().add(iLabel);

        Label name = new Label(user.getFullName() != null ? user.getFullName().trim() : "Unknown");
        name.setFont(Font.font("System", FontWeight.BOLD, 15));
        name.setTextFill(Color.web("#F8FAFC"));

        Label email = new Label(user.getEmail() != null ? user.getEmail() : "");
        email.setFont(Font.font("System", 11));
        email.setTextFill(Color.web("#94A3B8"));

        Label badge = new Label(status);
        badge.setFont(Font.font("System", FontWeight.BOLD, 11));
        badge.setTextFill(color);
        badge.setPadding(new Insets(3, 10, 3, 10));
        badge.setStyle("-fx-background-color: " + hex + "22; -fx-background-radius: 99; -fx-border-color: " + hex + "55; -fx-border-radius: 99;");

        userCard.getChildren().addAll(avatar, name, email, badge);

        if (user.getUsername() != null && !user.getUsername().isEmpty())
            userCard.getChildren().add(infoRow("User", user.getUsername()));
        if (user.getCreatedAt() != null)
            userCard.getChildren().add(infoRow("Joined", user.getCreatedAt().substring(0, Math.min(10, user.getCreatedAt().length()))));
        if (user.isBanned() && user.getBanReason() != null)
            userCard.getChildren().add(infoRow("Reason", user.getBanReason()));
        if (user.getUniversity() != null && !user.getUniversity().isEmpty())
            userCard.getChildren().add(infoRow("Univ.", user.getUniversity()));
        userCard.getChildren().add(infoRow("Coins", String.valueOf(user.getCoins())));

        userCard.setVisible(true);
        userCard.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), userCard);
        ft.setToValue(1);
        ft.play();
    }

    private HBox infoRow(String label, String value) {
        Label l = new Label(label);
        l.setFont(Font.font("System", 11));
        l.setTextFill(Color.web("#64748B"));
        l.setMinWidth(55);
        Label v = new Label(value);
        v.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        v.setTextFill(Color.web("#CBD5E1"));
        v.setWrapText(true);
        return new HBox(8, l, v);
    }

    private void hideUserCard() {
        if (userCard != null) userCard.setVisible(false);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Legend
    // ══════════════════════════════════════════════════════════════════════════════

    private void buildLegend() {
        HBox legend = new HBox(14);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(8, 16, 8, 16));
        legend.setStyle("-fx-background-color: rgba(2,6,23,0.85); -fx-background-radius: 10;");
        legend.setMaxWidth(Region.USE_PREF_SIZE);
        legend.setMaxHeight(Region.USE_PREF_SIZE);

        legend.getChildren().addAll(
                legendDot(ADMIN_COLOR, "Admin"),
                legendDot(ACTIVE_COLOR, "Active"),
                legendDot(NEW_COLOR, "New"),
                legendDot(BANNED_COLOR, "Banned"),
                legendDot(INACTIVE_COLOR, "Inactive")
        );

        StackPane.setAlignment(legend, Pos.BOTTOM_CENTER);
        StackPane.setMargin(legend, new Insets(0, 0, 12, 0));
        this.getChildren().add(legend);
    }

    private HBox legendDot(Color c, String text) {
        Region dot = new Region();
        dot.setPrefSize(8, 8);
        dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color: " + toHex(c) + "; -fx-background-radius: 4;");
        Label lb = new Label(text);
        lb.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        lb.setTextFill(Color.web("#94A3B8"));
        HBox h = new HBox(5, dot, lb);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  Utilities
    // ══════════════════════════════════════════════════════════════════════════════

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    public void stopAnimation() {
        if (timer != null) timer.stop();
    }
}
