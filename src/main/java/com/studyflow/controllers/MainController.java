package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.WellbeingAiService;
import com.studyflow.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
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
    @FXML private VBox globalQuoteCard;
    @FXML private Label globalQuoteTextLabel;
    @FXML private Label globalQuoteMetaLabel;
    @FXML private Pane quoteOverlayPane;

    private Button activeButton;
    private final WellbeingAiService wellbeingAiService = new WellbeingAiService();
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
        setupGlobalQuoteDrag();
        setupGlobalQuoteWidget();
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

    @FXML
    private void showNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(App.getPrimaryStage());
        alert.setTitle("Notifications");
        alert.setHeaderText("Notifications");
        alert.setContentText("Notifications are not wired yet in this build.");
        alert.showAndWait();
    }

    @FXML
    private void toggleTheme() {
        App.toggleTheme();
        syncThemeToggleUi();
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
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
