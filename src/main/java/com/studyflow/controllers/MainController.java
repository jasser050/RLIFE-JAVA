package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Assignment;
import com.studyflow.models.Notification;
import com.studyflow.models.Pet;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.NotificationService;
import com.studyflow.services.PetService;
import com.studyflow.services.ProjectService;
import com.studyflow.services.UserCoinService;
import com.studyflow.services.WellbeingAiService;
import com.studyflow.utils.CrudViewContext;
import com.studyflow.utils.PetPreviewSupport;
import com.studyflow.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * Main controller for the application layout
 * Handles navigation, content switching, and window controls
 */
public class MainController implements Initializable {
    private static final long COIN_REWARD_INTERVAL_MILLIS = 60_000L;
    private static final long ACTIVE_IDLE_TIMEOUT_MILLIS = 120_000L;
    private static MainController activeInstance;

    @FXML private StackPane contentArea;
    @FXML private ScrollPane contentScroll;
    @FXML private TextField searchField;
    @FXML private HBox titleBar;
    @FXML private FontIcon maximizeIcon;

    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserSub;
    @FXML private Label sidebarAvatar;
    @FXML private ImageView sidebarLogo;

    @FXML private HBox userProfileRow;
    @FXML private VBox userInfoBox;

    @FXML private Button btnDashboard;
    @FXML private Button btnCourses;
    @FXML private Button btnAssignments;
    @FXML private Button btnPlanning;
    @FXML private Button btnRevisions;
    @FXML private Button btnProjects;
    @FXML private Button btnNotes;
    @FXML private Button btnWellbeing;
    @FXML private Button btnStats;
    @FXML private Button notificationsButton;
    @FXML private Button themeToggleButton;
    @FXML private FontIcon themeToggleIcon;
    @FXML private StackPane globalQuoteCard;
    @FXML private Label globalQuoteTextLabel;
    @FXML private Label globalQuoteMetaLabel;
    @FXML private Pane quoteOverlayPane;
    @FXML private StackPane globalQuotePetBadge;
    @FXML private StackPane globalQuotePetPreviewPane;

    private Button activeButton;
    private final ProjectService projectService = new ProjectService();
    private final AssignmentService assignmentService = new AssignmentService();
    private final NotificationService notificationService = new NotificationService();
    private final UserCoinService userCoinService = new UserCoinService();
    private final WellbeingAiService wellbeingAiService = new WellbeingAiService();
    private final PetService petService = new PetService();
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
    private Timeline quoteTicker;
    private PauseTransition quoteResumeTimer;
    private PreferenceChangeListener quotePrefsListener;
    private static final String PREF_QUOTE_TYPE = "global.quote.type";
    private static final String PREF_QUOTE_ENABLED = "global.quote.enabled";
    private static final String PREF_QUOTE_DISMISSED_UNTIL = "global.quote.dismissed.until";
    private static final String PREF_QUOTE_POS_X = "global.quote.position.x";
    private static final String PREF_QUOTE_POS_Y = "global.quote.position.y";
    private static final long QUOTE_DISMISS_MILLIS = 60_000L;
    private static final int QUOTE_ROTATION_SECONDS = 20;
    private static final double QUOTE_MARGIN = 16.0;
    private double quoteDragStartX;
    private double quoteDragStartY;
    private double quoteStartLayoutX;
    private double quoteStartLayoutY;
    private Timeline usageCoinTimeline;
    private long lastActivityAtMillis = System.currentTimeMillis();
    private long lastUsageTickAtMillis = System.currentTimeMillis();
    private long accruedActiveMillis = 0L;
    private Pet quotePet;

    // Window dragging
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeInstance = this;
        activeButton = btnDashboard;
        syncThemeToggleUi();
        showDashboard();

        // Load sidebar logo
        if (sidebarLogo != null) {
            URL logoUrl = getClass().getResource("/com/studyflow/images/logo.png");
            if (logoUrl != null) {
                sidebarLogo.setImage(new Image(logoUrl.toExternalForm()));
            }
        }

        // Populate sidebar user info from session
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            sidebarUserName.setText(user.getFullName().trim().isEmpty() ? user.getUsername() : user.getFullName().trim());
            sidebarUserSub.setText(user.getUniversity() != null && !user.getUniversity().isEmpty()
                    ? user.getUniversity() : user.getEmail());
            sidebarAvatar.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        }
        
        // Add click handler for user profile row
        if (userProfileRow != null) {
            userProfileRow.setOnMouseClicked(event -> showProfile());
        }
        if (userInfoBox != null) {
            userInfoBox.setOnMouseClicked(event -> showProfile());
        }
        syncNotifications();
        setupGlobalQuoteDrag();
        setupGlobalQuoteWidget();
        installActivityTracking();
        startUsageCoinTracking();
    }

    public static MainController getInstance() {
        return activeInstance;
    }

    // ============================================
    // WINDOW CONTROL METHODS
    // ============================================

    @FXML
    private void onTitleBarPressed(MouseEvent event) {
        Stage stage = App.getPrimaryStage();
        xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
    }

    @FXML
    private void onTitleBarDragged(MouseEvent event) {
        Stage stage = App.getPrimaryStage();
        if (!isMaximized) {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        }
    }

    @FXML
    private void minimizeWindow() {
        App.getPrimaryStage().setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();

        if (isMaximized) {
            // Restore to previous size
            stage.setX(savedX);
            stage.setY(savedY);
            stage.setWidth(savedWidth);
            stage.setHeight(savedHeight);
            isMaximized = false;
            maximizeIcon.setIconLiteral("fth-square");
        } else {
            // Save current size and position
            savedX = stage.getX();
            savedY = stage.getY();
            savedWidth = stage.getWidth();
            savedHeight = stage.getHeight();

            // Maximize to screen
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            isMaximized = true;
            maximizeIcon.setIconLiteral("fth-copy");
        }
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }

    @FXML
    private void showNotifications() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || notificationsButton == null) {
            return;
        }

        syncNotifications();
        List<Notification> notifications = notificationService.getRecentByUserId(user.getId(), 8);
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("notifications-menu");

        if (notifications.isEmpty()) {
            Label emptyLabel = new Label("No notifications yet.");
            emptyLabel.getStyleClass().add("notification-empty-label");
            menu.getItems().add(new CustomMenuItem(emptyLabel, false));
        } else {
            for (Notification notification : notifications) {
                VBox box = new VBox(4);
                box.getStyleClass().add("notification-item");

                Label title = new Label(notification.getTitle());
                title.getStyleClass().add("notification-title");
                title.setWrapText(true);

                Label message = new Label(notification.getMessage());
                message.getStyleClass().add("notification-message");
                message.setWrapText(true);

                Label meta = new Label(notification.getCreatedAt());
                meta.getStyleClass().add("notification-meta");

                box.getChildren().addAll(title, message, meta);
                CustomMenuItem item = new CustomMenuItem(box, true);
                item.setOnAction(event -> {
                    notificationService.markAsRead(notification.getId(), user.getId());
                    updateNotificationsButton();
                    menu.hide();
                    openNotification(notification);
                });
                menu.getItems().add(item);
            }

            menu.getItems().add(new SeparatorMenuItem());
            Button markReadButton = new Button("Mark all as read");
            markReadButton.getStyleClass().add("btn-secondary");
            markReadButton.setMaxWidth(Double.MAX_VALUE);
            markReadButton.setOnAction(event -> {
                notificationService.markAllAsRead(user.getId());
                updateNotificationsButton();
                menu.hide();
            });

            VBox footer = new VBox(markReadButton);
            VBox.setVgrow(markReadButton, Priority.NEVER);
            footer.getStyleClass().add("notification-footer");
            menu.getItems().add(new CustomMenuItem(footer, false));
        }

        menu.show(notificationsButton, javafx.geometry.Side.BOTTOM, 0, 8);
    }

    private void openNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        String link = notification.getLink() == null ? "" : notification.getLink().trim();
        if (link.startsWith("project_meeting:")) {
            Integer projectId = parseLinkedId(link, "project_meeting:");
            if (projectId == null) {
                return;
            }
            Project project = projectService.getProjectById(projectId);
            if (project == null) {
                return;
            }
            CrudViewContext.setProjectContext(project);
            CrudViewContext.rememberProjectSelection(projectId);
            loadContent("views/ProjectMeeting.fxml");
            return;
        }
        if (link.startsWith("project:")) {
            Integer projectId = parseLinkedId(link, "project:");
            if (projectId == null) {
                return;
            }
            CrudViewContext.rememberProjectSelection(projectId);
            loadContent("views/Projects.fxml");
            return;
        }
        if (link.startsWith("assignment:")) {
            Integer assignmentId = parseLinkedId(link, "assignment:");
            if (assignmentId == null) {
                return;
            }
            CrudViewContext.rememberAssignmentSelection(assignmentId);
            loadContent("views/Assignments.fxml");
        }
    }

    private Integer parseLinkedId(String value, String prefix) {
        try {
            return Integer.parseInt(value.substring(prefix.length()).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void syncNotifications() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || !projectService.isDatabaseAvailable() || !assignmentService.isDatabaseAvailable()
                || !notificationService.isDatabaseAvailable()) {
            updateNotificationsButton();
            return;
        }

        List<Project> projects = projectService.getByUserId(user.getId());
        List<Assignment> assignments = assignmentService.getByUserId(user.getId());
        notificationService.syncDueDateNotifications(user.getId(), projects, assignments);
        updateNotificationsButton();
    }

    private void updateNotificationsButton() {
        if (notificationsButton == null) {
            return;
        }

        User user = UserSession.getInstance().getCurrentUser();
        int unread = user == null ? 0 : notificationService.countUnreadByUserId(user.getId());
        notificationsButton.setText(unread > 0 ? String.valueOf(unread) : "");
        notificationsButton.setAccessibleText(unread > 0 ? unread + " unread notifications" : "Notifications");
    }

    @FXML
    private void toggleTheme() {
        App.toggleTheme();
        syncThemeToggleUi();
    }
    // ============================================
    // NAVIGATION METHODS
    // ============================================

    /**
     * Load content into the main content area
     */
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label(
                    "Unable to load page: " + fxmlPath + "\n"
                            + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : " - " + e.getMessage())
            );
            errorLabel.setWrapText(true);
            errorLabel.getStyleClass().add("text-small");
            errorLabel.setStyle("-fx-text-fill: #FCA5A5; -fx-padding: 20;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    public static void loadContentInMainArea(String fxmlPath) {
        if (activeInstance == null) {
            return;
        }
        activeInstance.loadContentInMainAreaInternal(fxmlPath);
    }

    private void loadContentInMainAreaInternal(String fxmlPath) {
        syncActiveButtonForView(fxmlPath);
        loadContent(fxmlPath);
    }

    private void syncActiveButtonForView(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            return;
        }
        switch (fxmlPath) {
            case "views/Dashboard.fxml" -> setActiveButton(btnDashboard);
            case "views/Courses.fxml" -> setActiveButton(btnCourses);
            case "views/Assignments.fxml" -> setActiveButton(btnAssignments);
            case "views/Planning.fxml" -> setActiveButton(btnPlanning);
            case "views/Revisions.fxml", "views/Flashcards.fxml" -> setActiveButton(btnRevisions);
            case "views/Projects.fxml" -> setActiveButton(btnProjects);
            case "views/Notes.fxml" -> setActiveButton(btnNotes);
            case "views/Wellbeing.fxml" -> setActiveButton(btnWellbeing);
            case "views/Statistics.fxml" -> setActiveButton(btnStats);
            default -> {
            }
        }
    }

    private void setupGlobalQuoteWidget() {
        if (globalQuoteCard == null || globalQuoteTextLabel == null || globalQuoteMetaLabel == null) {
            return;
        }
        normalizeQuotePreferences();
        setFallbackQuoteContent();

        quotePrefsListener = this::onQuotePrefChanged;
        preferences.addPreferenceChangeListener(quotePrefsListener);

        initGlobalQuoteTicker();
    }

    private void onQuotePrefChanged(PreferenceChangeEvent event) {
        String key = event.getKey();
        if (!PREF_QUOTE_TYPE.equals(key)
                && !PREF_QUOTE_ENABLED.equals(key)
                && !PREF_QUOTE_DISMISSED_UNTIL.equals(key)
                && !PREF_QUOTE_POS_X.equals(key)
                && !PREF_QUOTE_POS_Y.equals(key)) {
            return;
        }
        Platform.runLater(() -> {
            if (PREF_QUOTE_POS_X.equals(key) || PREF_QUOTE_POS_Y.equals(key)) {
                applySavedQuotePosition();
                return;
            }
            initGlobalQuoteTicker();
        });
    }

    private void initGlobalQuoteTicker() {
        if (quoteTicker != null) {
            quoteTicker.stop();
            quoteTicker = null;
        }
        if (quoteResumeTimer != null) {
            quoteResumeTimer.stop();
            quoteResumeTimer = null;
        }

        if (globalQuoteCard == null) {
            return;
        }

        if (!isQuoteEnabled()) {
            globalQuoteCard.setVisible(false);
            globalQuoteCard.setManaged(false);
            return;
        }

        if (isQuoteDismissed()) {
            globalQuoteCard.setVisible(false);
            globalQuoteCard.setManaged(false);
            scheduleQuoteResumeTimer();
            return;
        }

        globalQuoteCard.setVisible(true);
        globalQuoteCard.setManaged(true);
        applySavedQuotePosition();
        loadGlobalQuoteNow();

        quoteTicker = new Timeline(new KeyFrame(javafx.util.Duration.seconds(QUOTE_ROTATION_SECONDS), event -> {
            if (!isQuoteEnabled()) {
                if (quoteTicker != null) {
                    quoteTicker.stop();
                }
                globalQuoteCard.setVisible(false);
                globalQuoteCard.setManaged(false);
                return;
            }
            if (isQuoteDismissed()) {
                if (quoteTicker != null) {
                    quoteTicker.stop();
                }
                globalQuoteCard.setVisible(false);
                globalQuoteCard.setManaged(false);
                scheduleQuoteResumeTimer();
                return;
            }
            globalQuoteCard.setVisible(true);
            globalQuoteCard.setManaged(true);
            loadGlobalQuoteNow();
        }));
        quoteTicker.setCycleCount(Timeline.INDEFINITE);
        quoteTicker.play();
    }

    @FXML
    private void handleGlobalQuoteClose() {
        long until = System.currentTimeMillis() + QUOTE_DISMISS_MILLIS;
        preferences.putLong(PREF_QUOTE_DISMISSED_UNTIL, until);
        if (quoteTicker != null) {
            quoteTicker.stop();
            quoteTicker = null;
        }
        if (globalQuoteCard != null) {
            globalQuoteCard.setVisible(false);
            globalQuoteCard.setManaged(false);
        }
        scheduleQuoteResumeTimer();
    }

    private void scheduleQuoteResumeTimer() {
        if (quoteResumeTimer != null) {
            quoteResumeTimer.stop();
            quoteResumeTimer = null;
        }
        if (!isQuoteEnabled()) {
            return;
        }

        long dismissedUntil = preferences.getLong(PREF_QUOTE_DISMISSED_UNTIL, 0L);
        long remainingMs = dismissedUntil - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return;
        }

        quoteResumeTimer = new PauseTransition(javafx.util.Duration.millis(remainingMs));
        quoteResumeTimer.setOnFinished(event -> {
            quoteResumeTimer = null;
            if (!isQuoteEnabled() || isQuoteDismissed()) {
                return;
            }
            initGlobalQuoteTicker();
        });
        quoteResumeTimer.playFromStart();
    }

    private void loadGlobalQuoteNow() {
        if (globalQuoteTextLabel == null || globalQuoteMetaLabel == null) {
            return;
        }
        if (!isQuoteEnabled() || isQuoteDismissed()) {
            return;
        }
        globalQuoteCard.setVisible(true);
        globalQuoteCard.setManaged(true);
        quotePet = loadQuotePet(currentUser());
        refreshQuotePetPresentation();

        String type = safeQuoteType(preferences.get(PREF_QUOTE_TYPE, "motivation"));
        globalQuoteMetaLabel.setText(type.toUpperCase(Locale.ROOT) + " - LOADING");

        Task<WellbeingAiService.QuoteResult> task = new Task<>() {
            @Override
            protected WellbeingAiService.QuoteResult call() {
                return wellbeingAiService.generateMotivationQuote(type);
            }
        };

        task.setOnSucceeded(event -> {
            WellbeingAiService.QuoteResult result = task.getValue();
            if (result == null) {
                setFallbackQuoteContent();
                return;
            }
            globalQuoteTextLabel.setText(result.quote());
            globalQuoteMetaLabel.setText(result.type().toUpperCase(Locale.ROOT) + " - " + result.source().toUpperCase(Locale.ROOT));
        });

        task.setOnFailed(event -> setFallbackQuoteContent());

        Thread thread = new Thread(task, "global-quote-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isQuoteEnabled() {
        return preferences.getBoolean(PREF_QUOTE_ENABLED, true);
    }

    private boolean isQuoteDismissed() {
        long dismissedUntil = preferences.getLong(PREF_QUOTE_DISMISSED_UNTIL, 0L);
        return dismissedUntil > System.currentTimeMillis();
    }

    private String safeQuoteType(String value) {
        String type = value == null ? "motivation" : value.trim().toLowerCase(Locale.ROOT);
        if (!"funny".equals(type) && !"calm".equals(type) && !"focus".equals(type)) {
            return "motivation";
        }
        return type;
    }

    private void setFallbackQuoteContent() {
        if (globalQuoteTextLabel == null || globalQuoteMetaLabel == null) {
            return;
        }
        String type = safeQuoteType(preferences.get(PREF_QUOTE_TYPE, "motivation"));
        quotePet = loadQuotePet(currentUser());
        refreshQuotePetPresentation();
        globalQuoteTextLabel.setText("Small progress every day beats perfect plans.");
        globalQuoteMetaLabel.setText(type.toUpperCase(Locale.ROOT) + " - FALLBACK");
    }

    private void normalizeQuotePreferences() {
        String type = safeQuoteType(preferences.get(PREF_QUOTE_TYPE, "motivation"));
        preferences.put(PREF_QUOTE_TYPE, type);

        // Initialize once, but never override an explicit user choice.
        if (preferences.get(PREF_QUOTE_ENABLED, null) == null) {
            preferences.putBoolean(PREF_QUOTE_ENABLED, true);
        }

        // Initialize dismissed state only if key is missing.
        if (preferences.get(PREF_QUOTE_DISMISSED_UNTIL, null) == null) {
            preferences.putLong(PREF_QUOTE_DISMISSED_UNTIL, 0L);
        }
    }

    private void setupGlobalQuoteDrag() {
        if (globalQuoteCard == null || quoteOverlayPane == null) {
            return;
        }
        globalQuoteCard.setOnMousePressed(event -> {
            quoteDragStartX = event.getSceneX();
            quoteDragStartY = event.getSceneY();
            quoteStartLayoutX = globalQuoteCard.getLayoutX();
            quoteStartLayoutY = globalQuoteCard.getLayoutY();
            event.consume();
        });

        globalQuoteCard.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - quoteDragStartX;
            double deltaY = event.getSceneY() - quoteDragStartY;
            setQuoteCardPosition(quoteStartLayoutX + deltaX, quoteStartLayoutY + deltaY);
            event.consume();
        });

        globalQuoteCard.setOnMouseReleased(event -> {
            saveQuoteCardPosition();
            event.consume();
        });

        quoteOverlayPane.widthProperty().addListener((obs, oldVal, newVal) -> clampQuoteCardToOverlay());
        quoteOverlayPane.heightProperty().addListener((obs, oldVal, newVal) -> clampQuoteCardToOverlay());
        Platform.runLater(this::applySavedQuotePosition);
    }

    private void applySavedQuotePosition() {
        if (globalQuoteCard == null || quoteOverlayPane == null) {
            return;
        }
        double savedX = preferences.getDouble(PREF_QUOTE_POS_X, Double.NaN);
        double savedY = preferences.getDouble(PREF_QUOTE_POS_Y, Double.NaN);
        if (Double.isNaN(savedX) || Double.isNaN(savedY)) {
            setDefaultQuoteCardPosition();
            return;
        }
        setQuoteCardPosition(savedX, savedY);
    }

    private void setDefaultQuoteCardPosition() {
        if (globalQuoteCard == null || quoteOverlayPane == null) {
            return;
        }
        globalQuoteCard.applyCss();
        globalQuoteCard.autosize();

        double defaultX = QUOTE_MARGIN;
        double defaultY;

        if (btnStats != null && btnStats.getScene() != null && quoteOverlayPane.getScene() != null) {
            Bounds statsBoundsScene = btnStats.localToScene(btnStats.getBoundsInLocal());
            Point2D belowStats = quoteOverlayPane.sceneToLocal(statsBoundsScene.getMinX(), statsBoundsScene.getMaxY() + 10);
            defaultX = belowStats.getX();
            defaultY = belowStats.getY();
        } else {
            double overlayHeight = quoteOverlayPane.getHeight();
            double cardHeight = globalQuoteCard.prefHeight(-1);
            defaultY = overlayHeight > 0 ? Math.max(QUOTE_MARGIN, overlayHeight - cardHeight - QUOTE_MARGIN) : QUOTE_MARGIN;
        }

        setQuoteCardPosition(defaultX, defaultY);
        saveQuoteCardPosition();
    }

    private void setQuoteCardPosition(double x, double y) {
        if (globalQuoteCard == null || quoteOverlayPane == null) {
            return;
        }
        globalQuoteCard.applyCss();
        globalQuoteCard.autosize();

        double cardWidth = globalQuoteCard.prefWidth(-1);
        double cardHeight = globalQuoteCard.prefHeight(-1);
        double overlayWidth = quoteOverlayPane.getWidth();
        double overlayHeight = quoteOverlayPane.getHeight();

        double minX = QUOTE_MARGIN;
        double minY = QUOTE_MARGIN;
        double maxX = overlayWidth > 0 ? Math.max(minX, overlayWidth - cardWidth - QUOTE_MARGIN) : x;
        double maxY = overlayHeight > 0 ? Math.max(minY, overlayHeight - cardHeight - QUOTE_MARGIN) : y;

        double clampedX = Math.max(minX, Math.min(x, maxX));
        double clampedY = Math.max(minY, Math.min(y, maxY));
        globalQuoteCard.setLayoutX(clampedX);
        globalQuoteCard.setLayoutY(clampedY);
    }

    private void clampQuoteCardToOverlay() {
        if (globalQuoteCard == null) {
            return;
        }
        setQuoteCardPosition(globalQuoteCard.getLayoutX(), globalQuoteCard.getLayoutY());
        saveQuoteCardPosition();
    }

    private void saveQuoteCardPosition() {
        if (globalQuoteCard == null) {
            return;
        }
        preferences.putDouble(PREF_QUOTE_POS_X, globalQuoteCard.getLayoutX());
        preferences.putDouble(PREF_QUOTE_POS_Y, globalQuoteCard.getLayoutY());
    }

    private User currentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private Pet loadQuotePet(User user) {
        if (user == null || !petService.isDatabaseAvailable()) {
            return null;
        }
        try {
            return petService.findByUserId(user.getId()).map(petService::syncPetState).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void refreshQuotePetPresentation() {
        if (globalQuotePetPreviewPane == null || globalQuotePetBadge == null) {
            return;
        }
        if (quotePet == null) {
            globalQuotePetPreviewPane.getChildren().clear();
            globalQuotePetBadge.setVisible(false);
            globalQuotePetBadge.setManaged(false);
            return;
        }
        Node preview = PetPreviewSupport.createPreview(quotePet.getType(), 34, 34);
        preview.setMouseTransparent(true);
        globalQuotePetPreviewPane.getChildren().setAll(preview);
        globalQuotePetBadge.setVisible(true);
        globalQuotePetBadge.setManaged(true);
    }

    private void syncThemeToggleUi() {
        if (themeToggleButton == null) {
            return;
        }
        boolean darkTheme = App.isDarkTheme();
        themeToggleButton.setText(darkTheme ? "Light" : "Dark");
        if (themeToggleIcon != null) {
            themeToggleIcon.setIconLiteral(darkTheme ? "fth-sun" : "fth-moon");
        }
        updateNotificationsButton();
    }

    /**
     * Update active navigation button
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);
        loadContent("views/Dashboard.fxml");
    }

    @FXML
    private void showCourses() {
        setActiveButton(btnCourses);
        loadContent("views/Courses.fxml");
    }

    @FXML
    private void showAssignments() {
        setActiveButton(btnAssignments);
        loadContent("views/Assignments.fxml");
    }

    @FXML
    public void showPlanning() {
        setActiveButton(btnPlanning);
        loadContent("views/Planning.fxml");
    }

    @FXML
    private void showRevisions() {
        setActiveButton(btnRevisions);
        loadContent("views/Flashcards.fxml");
    }

    @FXML
    private void showProjects() {
        setActiveButton(btnProjects);
        loadContent("views/Projects.fxml");
    }

    @FXML
    private void showNotes() {
        setActiveButton(btnNotes);
        loadContent("views/Notes.fxml");
    }

    @FXML
    private void showWellbeing() {
        setActiveButton(btnWellbeing);
        loadContent("views/Wellbeing.fxml");
    }

    @FXML
    private void showStats() {
        setActiveButton(btnStats);
        loadContent("views/Statistics.fxml");
    }

    @FXML
    private void showProfile() {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = null;
        loadContent("views/Profile.fxml");
    }

    @FXML
    private void handleLogout() {
        stopUsageCoinTracking();
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void installActivityTracking() {
        Platform.runLater(() -> {
            if (contentArea == null || contentArea.getScene() == null) {
                return;
            }
            contentArea.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, this::recordUserActivity);
            contentArea.getScene().addEventFilter(MouseEvent.MOUSE_MOVED, this::recordUserActivity);
            contentArea.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::recordUserActivity);
            contentArea.getScene().addEventFilter(ScrollEvent.SCROLL, this::recordUserActivity);
            Stage stage = App.getPrimaryStage();
            if (stage != null) {
                stage.focusedProperty().addListener((obs, oldValue, focused) -> {
                    if (focused) {
                        lastActivityAtMillis = System.currentTimeMillis();
                        lastUsageTickAtMillis = lastActivityAtMillis;
                    }
                });
            }
        });
    }

    private void recordUserActivity(Event event) {
        lastActivityAtMillis = System.currentTimeMillis();
    }

    private void startUsageCoinTracking() {
        stopUsageCoinTracking();
        lastActivityAtMillis = System.currentTimeMillis();
        lastUsageTickAtMillis = lastActivityAtMillis;
        accruedActiveMillis = 0L;
        usageCoinTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(15), event -> rewardUsageCoinsIfNeeded()));
        usageCoinTimeline.setCycleCount(Timeline.INDEFINITE);
        usageCoinTimeline.play();
    }

    private void stopUsageCoinTracking() {
        if (usageCoinTimeline != null) {
            usageCoinTimeline.stop();
            usageCoinTimeline = null;
        }
    }

    private void rewardUsageCoinsIfNeeded() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - lastUsageTickAtMillis);
        lastUsageTickAtMillis = now;

        if (currentUser == null) {
            accruedActiveMillis = 0L;
            return;
        }

        Stage stage = App.getPrimaryStage();
        boolean isActiveWindow = stage != null && stage.isFocused() && !stage.isIconified();
        boolean isRecentlyActive = now - lastActivityAtMillis <= ACTIVE_IDLE_TIMEOUT_MILLIS;
        if (!isActiveWindow || !isRecentlyActive) {
            return;
        }

        accruedActiveMillis += elapsed;
        int coinsToAward = (int) (accruedActiveMillis / COIN_REWARD_INTERVAL_MILLIS);
        if (coinsToAward <= 0) {
            return;
        }

        accruedActiveMillis %= COIN_REWARD_INTERVAL_MILLIS;
        try {
            int awardedCoins = userCoinService.addUsageCoins(currentUser.getId(), coinsToAward);
            if (awardedCoins > 0) {
                currentUser.setCoins(currentUser.getCoins() + awardedCoins);
            }
        } catch (RuntimeException ex) {
            System.out.println("MainController.rewardUsageCoinsIfNeeded: " + ex.getMessage());
        }
    }
}
