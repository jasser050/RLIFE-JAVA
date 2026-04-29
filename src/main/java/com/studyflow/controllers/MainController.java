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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.embed.swing.SwingNode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import me.friwi.jcefmaven.CefAppBuilder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.lang.reflect.Method;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Main controller for the application layout
 * Handles navigation, content switching, and window controls
 */
public class MainController implements Initializable {

    @FXML private StackPane contentArea;
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
    @FXML private VBox globalQuoteCard;
    @FXML private Label globalQuoteTextLabel;
    @FXML private Label globalQuoteMetaLabel;
    @FXML private Pane quoteOverlayPane;
    @FXML private StackPane spotifyFloatingButton;
    @FXML private VBox spotifyPanel;
    @FXML private Button spotifyPresetNatureBtn;
    @FXML private Button spotifyPresetQuranBtn;
    @FXML private Button spotifyPresetRelaxBtn;
    @FXML private Button spotifyPresetMotivationBtn;
    private VBox spotifyMockCard;
    private Label spotifyCoverLabelDynamic;
    private Label spotifyTrack1Dynamic;
    private Label spotifyTrack2Dynamic;
    private Label spotifyTrack3Dynamic;
    private Label spotifyTrack4Dynamic;
    private Label spotifyNowPlayingDynamic;
    private Label spotifyNowTitle;
    private Label spotifyNowMeta;
    private ImageView spotifyNowCover;
    private SwingNode spotifyBrowserNode;
    private javafx.scene.control.ListView<String> spotifyChoiceList;
    private String currentSpotifyChoiceUri;
    private WebView soundCloudView;
    private String currentAudioSource = "soundcloud";
    private String lastLoadedSoundCloudUrl;
    private String lastPreparedSoundCloudUrl;
    private boolean audioPlaybackActive;
    private final Image moodGreatImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/studyflow/assets/mood/great.png")));
    private final Image moodGoodImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/studyflow/assets/mood/good.png")));
    private final Image moodOkayImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/studyflow/assets/mood/okay.png")));
    private final Image moodStressedImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/studyflow/assets/mood/stressed.png")));
    private final Image moodTiredImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/studyflow/assets/mood/tired.png")));
    private final Image thumbNature = new Image("https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=120&h=120&q=70", true);
    private final Image thumbQuran = new Image("https://images.unsplash.com/photo-1542816417-0983c9c9ad53?auto=format&fit=crop&w=120&h=120&q=70", true);
    private final Image thumbRelax = new Image("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=120&h=120&q=70", true);
    private final Image thumbMotivation = new Image("https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=120&h=120&q=70", true);

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
    private static final String PREF_SPOTIFY_POS_X = "global.spotify.position.x";
    private static final String PREF_SPOTIFY_POS_Y = "global.spotify.position.y";
    private static final String PREF_SPOTIFY_VISIBLE = "global.spotify.visible";
    private static final String PREF_SPOTIFY_EXPANDED = "global.spotify.expanded";
    private static final String PREF_SPOTIFY_PRESET = "global.spotify.preset";
    private static final double SPOTIFY_MARGIN = 16.0;
    private double quoteDragStartX;
    private double quoteDragStartY;
    private double quoteStartLayoutX;
    private double quoteStartLayoutY;
    private double spotifyDragStartX;
    private double spotifyDragStartY;
    private double spotifyStartLayoutX;
    private double spotifyStartLayoutY;
    private boolean spotifyDragging;
    private final Map<String, String> spotifyEmbeds = Map.of(
            "nature", "https://open.spotify.com/embed/playlist/37i9dQZF1DX4PP3DA4J0N8?utm_source=generator&theme=0",
            "quran", "https://open.spotify.com/embed/playlist/2mIXv4QFfQbcNv89QmT6XQ?utm_source=generator&theme=0",
            "relax", "https://open.spotify.com/embed/playlist/37i9dQZF1DX3Ogo9pFvBkY?utm_source=generator&theme=0",
            "motivation", "https://open.spotify.com/embed/playlist/37i9dQZF1DX1s9knjP51Oa?utm_source=generator&theme=0"
    );
    private final Map<String, String> spotifyAppUris = Map.of(
            "nature", "spotify:playlist:37i9dQZF1DX4PP3DA4J0N8",
            "quran", "spotify:playlist:2mIXv4QFfQbcNv89QmT6XQ",
            "relax", "spotify:playlist:37i9dQZF1DX3Ogo9pFvBkY",
            "motivation", "spotify:playlist:37i9dQZF1DX1s9knjP51Oa"
    );
    private record SpotifyChoice(String label, String uri) {}
    private final Map<String, List<SpotifyChoice>> spotifyPresetChoices = Map.of(
            "nature", List.of(
                    new SpotifyChoice("Nature Sounds Mix", "spotify:playlist:37i9dQZF1DX4PP3DA4J0N8"),
                    new SpotifyChoice("Deep Focus Nature", "spotify:playlist:37i9dQZF1DWZeKCadgRdKQ"),
                    new SpotifyChoice("Calm Forest", "spotify:playlist:37i9dQZF1DX3Ogo9pFvBkY"),
                    new SpotifyChoice("Ocean & Rain", "spotify:search:ocean%20rain%20sounds"),
                    new SpotifyChoice("Birds & Water", "spotify:search:birds%20water%20nature"),
                    new SpotifyChoice("Nature Ambience", "spotify:search:nature%20ambience"),
                    new SpotifyChoice("Rain Sleep", "spotify:search:rain%20sleep"),
                    new SpotifyChoice("Forest Meditation", "spotify:search:forest%20meditation")
            ),
            "quran", List.of(
                    new SpotifyChoice("Yasser Al-Dossari", "spotify:search:yasser%20al%20dossari"),
                    new SpotifyChoice("Al-Luhaidan", "spotify:search:al%20luhaidan%20quran"),
                    new SpotifyChoice("Quran Recitation", "spotify:playlist:2mIXv4QFfQbcNv89QmT6XQ"),
                    new SpotifyChoice("Tilawat", "spotify:search:tilawat"),
                    new SpotifyChoice("Surah Yasin Recitation", "spotify:search:surah%20yasin%20recitation"),
                    new SpotifyChoice("Abdul Rahman Al-Sudais", "spotify:search:abdul%20rahman%20al%20sudais"),
                    new SpotifyChoice("Mishary Alafasy", "spotify:search:mishary%20alafasy"),
                    new SpotifyChoice("Maher Al-Muaiqly", "spotify:search:maher%20al%20muaiqly"),
                    new SpotifyChoice("Yasser - Surah Maryam", "spotify:search:yasser%20dossari%20surah%20maryam"),
                    new SpotifyChoice("Yasser - Surah Taha", "spotify:search:yasser%20dossari%20surah%20taha"),
                    new SpotifyChoice("Yasser - Surah Yusuf", "spotify:search:yasser%20dossari%20surah%20yusuf"),
                    new SpotifyChoice("Luhaidan - Surah Al-Mulk", "spotify:search:al%20luhaidan%20surah%20al%20mulk"),
                    new SpotifyChoice("Luhaidan - Surah An-Najm", "spotify:search:al%20luhaidan%20surah%20an%20najm"),
                    new SpotifyChoice("Luhaidan - Surah At-Tawbah", "spotify:search:al%20luhaidan%20surah%20at%20tawbah")
            ),
            "relax", List.of(
                    new SpotifyChoice("Calm Vibes", "spotify:playlist:37i9dQZF1DX3Ogo9pFvBkY"),
                    new SpotifyChoice("Sleep", "spotify:playlist:37i9dQZF1DWStPkKCDmQbL"),
                    new SpotifyChoice("Ambient Chill", "spotify:search:ambient%20chill"),
                    new SpotifyChoice("Soft Piano Relax", "spotify:search:soft%20piano%20relax"),
                    new SpotifyChoice("Evening Chill", "spotify:search:evening%20chill"),
                    new SpotifyChoice("Lo-Fi Relax", "spotify:search:lofi%20relax"),
                    new SpotifyChoice("Deep Sleep Calm", "spotify:search:deep%20sleep%20calm"),
                    new SpotifyChoice("Zen Relaxation", "spotify:search:zen%20relaxation")
            ),
            "motivation", List.of(
                    new SpotifyChoice("Motivation", "spotify:playlist:37i9dQZF1DX1s9knjP51Oa"),
                    new SpotifyChoice("Workout Energy", "spotify:search:workout%20motivation"),
                    new SpotifyChoice("Power Focus", "spotify:search:focus%20energy"),
                    new SpotifyChoice("Study Drive", "spotify:search:study%20motivation"),
                    new SpotifyChoice("Morning Energy", "spotify:search:morning%20energy"),
                    new SpotifyChoice("Gym Power", "spotify:search:gym%20power"),
                    new SpotifyChoice("No Excuses", "spotify:search:motivation%20speech%20mix"),
                    new SpotifyChoice("Productive Focus", "spotify:search:productive%20focus")
            )
    );
    private final Map<String, List<SpotifyChoice>> soundCloudPresetChoices = Map.of(
            "nature", List.of(
                    new SpotifyChoice("Ambient Music Playlists", "https://soundcloud.com/ambientplaylists/sets/ambient-music-playlists"),
                    new SpotifyChoice("Ambient Playlist", "https://soundcloud.com/ambientmusicstreaming/sets/spa-music"),
                    new SpotifyChoice("Ambient Collection", "https://soundcloud.com/headstashmusic/sets/ambient"),
                    new SpotifyChoice("Ambient Discovery", "https://soundcloud.com/music_discovery/sets/ambient-playlist"),
                    new SpotifyChoice("Rain & Calm", "https://soundcloud.com/music-for-life-playlists/sets/ambient"),
                    new SpotifyChoice("Ambient by 7013", "https://soundcloud.com/7013/sets/ambient"),
                    new SpotifyChoice("Free Ambient Music", "https://soundcloud.com/freeambientmusic/sets/ambient-music"),
                    new SpotifyChoice("Ambient Label Set", "https://soundcloud.com/ambient-recordings")
            ),
            "quran", List.of(
                    new SpotifyChoice("Yasser Al-Dossari - Ash-Shu'ara", "https://soundcloud.com/shrekawareness/yasser-al-dossari-surah-ash"),
                    new SpotifyChoice("Yasser Al-Dossari - Al-Hashr", "https://soundcloud.com/user-213886513/yasser-al-dossari-surah-hashr"),
                    new SpotifyChoice("Yasser Al-Dossari - Surah Sad", "https://soundcloud.com/dzulgd/yasser-al-dossari-surah-sad"),
                    new SpotifyChoice("Yasser Al-Dossari - Surah Fatir", "https://soundcloud.com/krew-256025666/yasser-al-dossari-surah-fatir"),
                    new SpotifyChoice("Yasser Al-Dossari - Al-Shu'ara (Alt)", "https://soundcloud.com/yassin-bayoumi-251383896/yasser-al-dossari-surah-al-3"),
                    new SpotifyChoice("Al-Luhaidan Playlist", "https://soundcloud.com/8wsydziwukpr/sets/al-luhaidan"),
                    new SpotifyChoice("Al-Luhaidan - Best Recitation", "https://soundcloud.com/8wsydziwukpr/best-muhammad-al-luhaidan-recitation-ever-on-youtube"),
                    new SpotifyChoice("Muhammad Al-Luhaidan Collection", "https://soundcloud.com/thequranchannel/sets/muhammad-al-luhaidan"),
                    new SpotifyChoice("Quran Recitation", "https://soundcloud.com/islamicvision4u/sets/quran-recitation"),
                    new SpotifyChoice("Quran Playlist", "https://soundcloud.com/user59096526/sets/quran")
            ),
            "relax", List.of(
                    new SpotifyChoice("Relaxing Ambient", "https://soundcloud.com/twisterium/sets/ambient-and-relaxing-music"),
                    new SpotifyChoice("Ambient Music", "https://soundcloud.com/music_discovery/sets/ambient-playlist"),
                    new SpotifyChoice("Calm Ambient", "https://soundcloud.com/ambientmusicstreaming/sets/spa-music"),
                    new SpotifyChoice("Relax Mix", "https://soundcloud.com/music-for-life-playlists/sets/ambient"),
                    new SpotifyChoice("Meditation Ambient", "https://soundcloud.com/headstashmusic/sets/ambient"),
                    new SpotifyChoice("Ambient Stream", "https://soundcloud.com/ambientmusicstreaming"),
                    new SpotifyChoice("Ambient Music Channel", "https://soundcloud.com/freeambientmusic"),
                    new SpotifyChoice("Calm Playlist Alt", "https://soundcloud.com/ambientplaylists/sets/ambient-music-playlists")
            ),
            "motivation", List.of(
                    new SpotifyChoice("Boost Motivation", "https://soundcloud.com/sound-playlist/sets/boost-your-motivation-the"),
                    new SpotifyChoice("For Motivation", "https://soundcloud.com/clngr/sets/for-motivation"),
                    new SpotifyChoice("Workout Motivation", "https://soundcloud.com/the-game-29/sets/motivation"),
                    new SpotifyChoice("Motivation Set A", "https://soundcloud.com/mathdelt11/sets/motivation"),
                    new SpotifyChoice("Motivation Set B", "https://soundcloud.com/rick-the-hunter/sets/motivation"),
                    new SpotifyChoice("Workout Playlists", "https://soundcloud.com/workout_playlists/sets"),
                    new SpotifyChoice("Study Motivation", "https://soundcloud.com/studymotivation-music"),
                    new SpotifyChoice("Discipline", "https://soundcloud.com/dj_metju/sets/motivation")
            )
    );
    private final Map<String, String> spotifyAudioUrls = Map.of(
            "nature", "nature",
            "quran", "quran",
            "relax", "relax",
            "motivation", "motivation"
    );
    private final Map<String, String> generatedAudioUris = new HashMap<>();
    private final Map<String, Path> userAudioPaths = new HashMap<>();
    private MediaPlayer spotifyPlayer;
    private Clip spotifyClip;
    private SourceDataLine spotifyLine;
    private Thread spotifyToneThread;
    private volatile boolean spotifyToneRunning;
    private String currentSpotifyPresetLoaded = "";
    private Object spotifyCefApp;
    private Object spotifyCefClient;
    private Object spotifyCefBrowser;
    private String spotifyJcefLastError;

    // Window dragging
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeButton = btnDashboard;
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
        setupSpotifyWidget();
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

    private void setupSpotifyWidget() {
        if (spotifyFloatingButton == null || quoteOverlayPane == null) {
            return;
        }
        // Always start hidden; it is opened only from coping tools start action.
        preferences.putBoolean(PREF_SPOTIFY_VISIBLE, false);
        preferences.putBoolean(PREF_SPOTIFY_EXPANDED, false);

        Tooltip.install(spotifyFloatingButton, new Tooltip("Spotify"));
        buildSpotifyMockCardIfNeeded();
        applySpotifyVisibility();
        applySpotifyExpanded();
        applySpotifyPreset(preferences.get(PREF_SPOTIFY_PRESET, "nature"));
        quoteOverlayPane.widthProperty().addListener((obs, oldVal, newVal) -> clampSpotifyToOverlay());
        quoteOverlayPane.heightProperty().addListener((obs, oldVal, newVal) -> clampSpotifyToOverlay());
        preferences.addPreferenceChangeListener(event -> {
            String key = event.getKey();
            if (PREF_SPOTIFY_VISIBLE.equals(key)) {
                Platform.runLater(this::applySpotifyVisibility);
            } else if (PREF_SPOTIFY_EXPANDED.equals(key)) {
                Platform.runLater(this::applySpotifyExpanded);
            } else if (PREF_SPOTIFY_PRESET.equals(key)) {
                Platform.runLater(() -> applySpotifyPreset(preferences.get(PREF_SPOTIFY_PRESET, "nature")));
            } else if (PREF_SPOTIFY_POS_X.equals(key) || PREF_SPOTIFY_POS_Y.equals(key)) {
                Platform.runLater(this::applySavedSpotifyPosition);
            }
        });
        Platform.runLater(this::applySavedSpotifyPosition);
    }

    private void applySpotifyVisibility() {
        if (spotifyFloatingButton == null) {
            return;
        }
        boolean visible = preferences.getBoolean(PREF_SPOTIFY_VISIBLE, false);
        spotifyFloatingButton.setVisible(visible);
        spotifyFloatingButton.setManaged(visible);
        if (!visible && spotifyPanel != null) {
            spotifyPanel.setVisible(false);
            spotifyPanel.setManaged(false);
        }
    }

    private void applySpotifyExpanded() {
        if (spotifyPanel == null || spotifyFloatingButton == null) {
            return;
        }
        boolean visible = preferences.getBoolean(PREF_SPOTIFY_VISIBLE, false);
        boolean expanded = preferences.getBoolean(PREF_SPOTIFY_EXPANDED, false);
        spotifyPanel.setVisible(visible && expanded);
        spotifyPanel.setManaged(visible && expanded);
        spotifyPanel.setLayoutX(spotifyFloatingButton.getLayoutX());
        spotifyPanel.setLayoutY(spotifyFloatingButton.getLayoutY() + 66);
    }

    @FXML
    private void onSpotifyWidgetPressed(MouseEvent event) {
        if (spotifyFloatingButton == null) {
            return;
        }
        spotifyDragging = false;
        spotifyDragStartX = event.getSceneX();
        spotifyDragStartY = event.getSceneY();
        spotifyStartLayoutX = spotifyFloatingButton.getLayoutX();
        spotifyStartLayoutY = spotifyFloatingButton.getLayoutY();
        event.consume();
    }

    @FXML
    private void onSpotifyWidgetDragged(MouseEvent event) {
        if (spotifyFloatingButton == null) {
            return;
        }
        double deltaX = event.getSceneX() - spotifyDragStartX;
        double deltaY = event.getSceneY() - spotifyDragStartY;
        if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
            spotifyDragging = true;
        }
        setSpotifyPosition(spotifyStartLayoutX + deltaX, spotifyStartLayoutY + deltaY);
        event.consume();
    }

    @FXML
    private void onSpotifyWidgetReleased(MouseEvent event) {
        saveSpotifyPosition();
        event.consume();
    }

    @FXML
    private void openSpotifyPlayer(MouseEvent event) {
        if (spotifyDragging) {
            spotifyDragging = false;
            return;
        }
        if (!preferences.getBoolean(PREF_SPOTIFY_VISIBLE, false)) {
            return;
        }
        boolean expanded = preferences.getBoolean(PREF_SPOTIFY_EXPANDED, false);
        // Display toggle only: keep playback running while user works.
        preferences.putBoolean(PREF_SPOTIFY_EXPANDED, !expanded);
    }

    @FXML
    private void hideSpotifyPanel() {
        // X button => full close until user starts Sound Music again.
        stopAudioPlayback();
        preferences.putBoolean(PREF_SPOTIFY_EXPANDED, false);
        preferences.putBoolean(PREF_SPOTIFY_VISIBLE, false);
        currentSpotifyChoiceUri = null;
        lastLoadedSoundCloudUrl = null;
        if (spotifyChoiceList != null) {
            spotifyChoiceList.getSelectionModel().clearSelection();
            spotifyChoiceList.getItems().clear();
        }
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Closed. Start Sound Music again.");
        }
    }

    @FXML
    private void selectSpotifyNature() {
        preferences.put(PREF_SPOTIFY_PRESET, "nature");
        playPreset("nature");
    }

    @FXML
    private void selectSpotifyQuran() {
        preferences.put(PREF_SPOTIFY_PRESET, "quran");
        playPreset("quran");
    }

    @FXML
    private void selectSpotifyRelax() {
        preferences.put(PREF_SPOTIFY_PRESET, "relax");
        playPreset("relax");
    }

    @FXML
    private void selectSpotifyMotivation() {
        preferences.put(PREF_SPOTIFY_PRESET, "motivation");
        playPreset("motivation");
    }

    private void applySpotifyPreset(String preset) {
        String safePreset = spotifyEmbeds.containsKey(preset) ? preset : "nature";
        if (spotifyCoverLabelDynamic != null) {
            switch (safePreset) {
                case "quran" -> {
                    spotifyCoverLabelDynamic.setText("Quran\nRecitation");
                    spotifyTrack1Dynamic.setText("1  Soura Al Anbiya");
                    spotifyTrack2Dynamic.setText("2  Ya-Sin");
                    spotifyTrack3Dynamic.setText("3  Al Mulk");
                    spotifyTrack4Dynamic.setText("4  Al Kahf");
                    spotifyNowPlayingDynamic.setText("Quran - Spotify");
                }
                case "relax" -> {
                    spotifyCoverLabelDynamic.setText("Relax\nVibes");
                    spotifyTrack1Dynamic.setText("1  Present Memory");
                    spotifyTrack2Dynamic.setText("2  Hidden Treasure");
                    spotifyTrack3Dynamic.setText("3  Velvet");
                    spotifyTrack4Dynamic.setText("4  Starbloom");
                    spotifyNowPlayingDynamic.setText("Relax Vibes - Spotify");
                }
                case "motivation" -> {
                    spotifyCoverLabelDynamic.setText("Mood\nBooster");
                    spotifyTrack1Dynamic.setText("1  So Easy (To Fall In Love)");
                    spotifyTrack2Dynamic.setText("2  Lose Control");
                    spotifyTrack3Dynamic.setText("3  Just Might");
                    spotifyTrack4Dynamic.setText("4  Onalite");
                    spotifyNowPlayingDynamic.setText("Mood Booster - Spotify");
                }
                default -> {
                    spotifyCoverLabelDynamic.setText("Nature\nSounds");
                    spotifyTrack1Dynamic.setText("1  Ocean Waves");
                    spotifyTrack2Dynamic.setText("2  Rain Ambience");
                    spotifyTrack3Dynamic.setText("3  Forest Wind");
                    spotifyTrack4Dynamic.setText("4  Birds in Dawn");
                    spotifyNowPlayingDynamic.setText("Nature Sounds - Spotify");
                }
            }
        }
        updateSpotifyPresetStyles(safePreset);
        applySpotifyCardTheme(safePreset);
        refreshSpotifyChoices(safePreset);
    }

    @FXML
    private void openSpotifyCurrentPreset() {
        String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
        String safePreset = spotifyEmbeds.containsKey(preset) ? preset : "nature";
        playPreset(safePreset);
    }

    private void playPreset(String preset) {
        String safePreset = spotifyEmbeds.containsKey(preset) ? preset : "nature";
        String chosenUri = (currentSpotifyChoiceUri == null || currentSpotifyChoiceUri.isBlank())
                ? (isSoundCloudSource()
                    ? soundCloudPresetChoices.getOrDefault(safePreset, List.of()).stream().findFirst().map(SpotifyChoice::uri).orElse("")
                    : spotifyAppUris.getOrDefault(safePreset, spotifyAppUris.get("nature")))
                : currentSpotifyChoiceUri;
        if (isSoundCloudSource()) {
            playSoundCloudInWidget(chosenUri);
        } else {
            openSpotifyUri(chosenUri);
            sendWindowsMediaKey(0xB3); // Play/Pause
        }
        audioPlaybackActive = true;
        updateNowPlayingCard(spotifyChoiceList == null ? null : spotifyChoiceList.getSelectionModel().getSelectedItem());
        currentSpotifyPresetLoaded = safePreset;
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText(isSoundCloudSource() ? "SoundCloud playing in widget" : "Spotify background play");
        }
    }

    private void playPreviousSpotifyTrack() {
        if (isSoundCloudSource()) {
            shiftSoundCloudSelection(-1);
            return;
        }
        sendWindowsMediaKey(0xB1); // Previous track
    }

    private void playNextSpotifyTrack() {
        if (isSoundCloudSource()) {
            shiftSoundCloudSelection(1);
            return;
        }
        sendWindowsMediaKey(0xB0); // Next track
    }

    private boolean isSoundCloudSource() {
        return "soundcloud".equalsIgnoreCase(currentAudioSource);
    }

    private void shiftSoundCloudSelection(int delta) {
        if (spotifyChoiceList == null || spotifyChoiceList.getItems().isEmpty()) {
            return;
        }
        int size = spotifyChoiceList.getItems().size();
        int idx = spotifyChoiceList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            idx = 0;
        } else {
            idx = (idx + delta + size) % size;
        }
        spotifyChoiceList.getSelectionModel().select(idx);
        openSpotifyCurrentPreset();
    }

    private void sendMediaKey(String sendKeysToken) {
        sendSpotifyCommand(sendKeysToken, false, 0);
    }

    private void sendWindowsMediaKey(int vkCode) {
        try {
            String script =
                    "$sig='[DllImport(\"user32.dll\")]public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);'; " +
                    "$u=Add-Type -MemberDefinition $sig -Name U32 -Namespace Win -PassThru; " +
                    "$u::keybd_event([byte]" + vkCode + ",0,0,0); " +
                    "Start-Sleep -Milliseconds 40; " +
                    "$u::keybd_event([byte]" + vkCode + ",0,2,0);";
            new ProcessBuilder("powershell", "-NoProfile", "-Command", script).start();
        } catch (Exception ignored) {
        }
    }

    private void sendSpotifyCommand(String sendKeysToken, boolean minimizeAfter, int delayMs) {
        try {
            StringBuilder script = new StringBuilder();
            if (delayMs > 0) {
                script.append("Start-Sleep -Milliseconds ").append(delayMs).append("; ");
            }
            script.append("$ws = New-Object -ComObject WScript.Shell; ");
            script.append("if ($ws.AppActivate('Spotify')) { ");
            script.append("$ws.SendKeys('").append(sendKeysToken).append("'); ");
            if (minimizeAfter) {
                script.append("Start-Sleep -Milliseconds 120; ");
                script.append("$ws.SendKeys('% {SPACE}n'); ");
            }
            script.append("}");
            new ProcessBuilder("powershell", "-NoProfile", "-Command", script.toString()).start();
        } catch (Exception ignored) {
        }
    }

    private void refocusAppStage() {
        Platform.runLater(() -> {
            Stage stage = App.getPrimaryStage();
            if (stage != null) {
                stage.toFront();
                stage.requestFocus();
            }
        });
    }

    private String resolvePresetAudioUri(String preset) {
        String[] exts = {".mp3", ".wav", ".m4a", ".aac"};
        for (String ext : exts) {
            try {
                URL resource = getClass().getResource("/com/studyflow/audio/" + preset + ext);
                if (resource != null) {
                    return resource.toExternalForm();
                }
            } catch (Exception ignored) {
            }
            try {
                Path sourcePath = Path.of("src", "main", "resources", "com", "studyflow", "audio", preset + ext);
                if (Files.exists(sourcePath)) {
                    return sourcePath.toUri().toString();
                }
            } catch (Exception ignored) {
            }
            try {
                Path rootAudioPath = Path.of("audio", preset + ext);
                if (Files.exists(rootAudioPath)) {
                    return rootAudioPath.toUri().toString();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Path promptUserAudioFile(String preset) {
        try {
            Stage stage = App.getPrimaryStage();
            if (stage == null) {
                return null;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select audio for " + preset);
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a", "*.aac")
            );
            if (Files.exists(Path.of("src/main/resources/com/studyflow/audio"))) {
                chooser.setInitialDirectory(Path.of("src/main/resources/com/studyflow/audio").toFile());
            }
            java.io.File selected = chooser.showOpenDialog(stage);
            if (selected == null) {
                return null;
            }
            return selected.toPath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Path resolveBundledPresetAudioPath(String preset) {
        String[] exts = {".wav", ".mp3", ".m4a", ".aac"};
        for (String ext : exts) {
            URL resource = getClass().getResource("/com/studyflow/audio/" + preset + ext);
            if (resource != null) {
                try {
                    return Path.of(resource.toURI());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private void openSpotifyInExternalPlayer(String preset) {
        String url = getSpotifyWebUrl(preset);
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String key = preset == null ? "nature" : preset.toLowerCase(Locale.ROOT);
            String appUri = spotifyAppUris.getOrDefault(key, spotifyAppUris.get("nature"));
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(URI.create(appUri));
                    hideSpotifyWindowSoon();
                    return;
                } catch (Exception ignored) {
                }
            }
            String externalUrl = url.replace("://open.spotify.com/embed/", "://open.spotify.com/");
            int qIndex = externalUrl.indexOf('?');
            if (qIndex >= 0) {
                externalUrl = externalUrl.substring(0, qIndex);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(externalUrl));
                hideSpotifyWindowSoon();
            }
        } catch (Exception ignored) {
        }
    }

    private void openSpotifyUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(uri));
            }
        } catch (Exception ignored) {
        }
    }

    private void hideSpotifyWindowSoon() {
        sendSpotifyCommand("% {SPACE}n", false, 1000);
    }

    private void triggerSpotifyPlayAfterOneSecond() {
        sendSpotifyCommand("{MEDIA_PLAY_PAUSE}", true, 1000);
    }

    private boolean openInJcefWindow(String preset) {
        String url = getSpotifyWebUrl(preset);
        if (url == null || url.isBlank()) {
            return false;
        }
        String withAutoplay = url;
        try {
            if (spotifyCefApp == null) {
                CefAppBuilder builder = new CefAppBuilder();
                builder.setInstallDir(new File("jcef-bundle"));
                spotifyCefApp = builder.build();
                spotifyCefClient = invokeMethod(spotifyCefApp, "createClient", new Class<?>[0]);
            }

            if (spotifyCefBrowser == null) {
                spotifyCefBrowser = invokeMethod(
                        spotifyCefClient,
                        "createBrowser",
                        new Class<?>[]{String.class, boolean.class, boolean.class},
                        withAutoplay, false, false
                );
                Object uiComponent = invokeMethod(spotifyCefBrowser, "getUIComponent", new Class<?>[0]);
                if (!(uiComponent instanceof Component component)) {
                    spotifyJcefLastError = "UI component unavailable";
                    return false;
                }
                if (spotifyBrowserNode != null) {
                    SwingUtilities.invokeLater(() -> {
                        JPanel panel = new JPanel(new BorderLayout());
                        panel.add(component, BorderLayout.CENTER);
                        spotifyBrowserNode.setContent(panel);
                    });
                }
            } else {
                invokeMethod(spotifyCefBrowser, "loadURL", new Class<?>[]{String.class}, withAutoplay);
            }
            return true;
        } catch (Throwable ex) {
            spotifyJcefLastError = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            return false;
        }
    }

    private Object invokeMethod(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method m = target.getClass().getMethod(name, parameterTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private String getSpotifyWebUrl(String preset) {
        String key = preset == null ? "nature" : preset.toLowerCase(Locale.ROOT);
        return spotifyEmbeds.getOrDefault(key, spotifyEmbeds.get("nature"));
    }

    private void stopSpotifyPlayback() {
        stopSpotifyToneEngine();
        if (spotifyBrowserNode != null) {
            SwingUtilities.invokeLater(() -> spotifyBrowserNode.setContent(new JPanel()));
        }
        spotifyCefBrowser = null;
        if (spotifyClip != null) {
            try {
                spotifyClip.stop();
                spotifyClip.close();
            } catch (Exception ignored) {
            }
            spotifyClip = null;
        }
        if (spotifyPlayer != null) {
            try {
                spotifyPlayer.stop();
                spotifyPlayer.dispose();
            } catch (Exception ignored) {
            }
            spotifyPlayer = null;
        }
    }

    private void startSpotifyToneEngine(String preset) throws Exception {
        AudioFormat format = new AudioFormat(22050f, 16, 1, true, false);
        spotifyLine = AudioSystem.getSourceDataLine(format);
        spotifyLine.open(format, 4096);
        spotifyLine.start();
        spotifyToneRunning = true;

        final double[] freqs = switch (preset) {
            case "quran" -> new double[]{196.00, 246.94};
            case "relax" -> new double[]{174.61, 220.00};
            case "motivation" -> new double[]{261.63, 329.63};
            default -> new double[]{130.81, 164.81};
        };

        spotifyToneThread = new Thread(() -> {
            int sampleRate = 22050;
            byte[] buffer = new byte[2048];
            long index = 0;
            while (spotifyToneRunning) {
                for (int i = 0; i < buffer.length / 2; i++) {
                    double t = (index++) / (double) sampleRate;
                    double env = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.07 * t);
                    double tone = Math.sin(2 * Math.PI * freqs[0] * t) * 0.55
                            + Math.sin(2 * Math.PI * freqs[1] * t) * 0.45;
                    short sample = (short) (tone * env * 12000);
                    buffer[i * 2] = (byte) (sample & 0xff);
                    buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
                }
                if (spotifyLine != null) {
                    spotifyLine.write(buffer, 0, buffer.length);
                }
            }
            if (spotifyLine != null) {
                spotifyLine.drain();
            }
        }, "spotify-tone-engine");
        spotifyToneThread.setDaemon(true);
        spotifyToneThread.start();
    }

    private void stopSpotifyToneEngine() {
        spotifyToneRunning = false;
        if (spotifyToneThread != null) {
            try {
                spotifyToneThread.join(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            spotifyToneThread = null;
        }
        if (spotifyLine != null) {
            try {
                spotifyLine.stop();
                spotifyLine.close();
            } catch (Exception ignored) {
            }
            spotifyLine = null;
        }
    }

    private String ensureLocalPresetAudio(String preset) {
        Path path = ensureLocalPresetAudioPath(preset);
        if (path == null) {
            return null;
        }
        String uri = path.toUri().toString();
        generatedAudioUris.put(preset, uri);
        return uri;
    }

    private Path ensureLocalPresetAudioPath(String preset) {
        if (generatedAudioUris.containsKey(preset)) {
            try {
                return Path.of(new URL(generatedAudioUris.get(preset)).toURI());
            } catch (Exception ignored) {
            }
        }
        try {
            byte[] wav = buildAmbientWavBytes(preset, 24, 22050);
            Path tmp = Files.createTempFile("studyflow-" + preset + "-", ".wav");
            Files.write(tmp, wav);
            tmp.toFile().deleteOnExit();
            String uri = tmp.toUri().toString();
            generatedAudioUris.put(preset, uri);
            return tmp;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] buildAmbientWavBytes(String preset, int seconds, int sampleRate) throws Exception {
        int totalSamples = Math.max(1, seconds) * sampleRate;
        byte[] pcm = new byte[totalSamples * 2];
        double base;
        double pulse;
        switch (preset) {
            case "quran" -> { base = 196.0; pulse = 246.94; }
            case "relax" -> { base = 174.61; pulse = 220.0; }
            case "motivation" -> { base = 261.63; pulse = 329.63; }
            default -> { base = 130.81; pulse = 164.81; }
        }
        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double envelope = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.07 * t);
            double tone = Math.sin(2 * Math.PI * base * t) * 0.55 + Math.sin(2 * Math.PI * pulse * t) * 0.45;
            short sample = (short) (tone * envelope * 12000);
            pcm[i * 2] = (byte) (sample & 0xff);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcm), format, totalSamples)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        }
        return out.toByteArray();
    }

    private void buildSpotifyMockCardIfNeeded() {
        if (spotifyPanel == null || spotifyMockCard != null) {
            return;
        }
        spotifyMockCard = new VBox(10);
        spotifyMockCard.getStyleClass().add("spotify-player-card");
        spotifyNowPlayingDynamic = new Label("Spotify controls");
        spotifyNowPlayingDynamic.getStyleClass().add("spotify-now-playing");
        spotifyNowTitle = new Label("Select a track");
        spotifyNowTitle.setStyle("-fx-text-fill:#f8fafc;-fx-font-size:20px;-fx-font-weight:800;");
        spotifyNowMeta = new Label("SoundCloud");
        spotifyNowMeta.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:12px;");
        spotifyNowCover = new ImageView(moodGoodImg);
        spotifyNowCover.setFitWidth(110);
        spotifyNowCover.setFitHeight(110);
        spotifyNowCover.setPreserveRatio(true);
        StackPane coverBox = new StackPane(spotifyNowCover);
        coverBox.setStyle("-fx-background-color:#0f172a;-fx-background-radius:12;-fx-padding:6;");
        VBox nowInfo = new VBox(6, spotifyNowTitle, spotifyNowMeta);
        nowInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox nowPlayingCard = new HBox(14, coverBox, nowInfo);
        nowPlayingCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        nowPlayingCard.setStyle("-fx-background-color: rgba(15,23,42,0.72); -fx-background-radius: 12; -fx-padding: 10;");

        Label statusHint = new Label("Choose source and track");
        statusHint.getStyleClass().add("spotify-time");

        ToggleButton sourceSpotifyBtn = new ToggleButton("Spotify");
        ToggleButton sourceSoundCloudBtn = new ToggleButton("SoundCloud");
        ToggleGroup sourceGroup = new ToggleGroup();
        sourceSpotifyBtn.setToggleGroup(sourceGroup);
        sourceSoundCloudBtn.setToggleGroup(sourceGroup);
        sourceSoundCloudBtn.setSelected(true);
        sourceSpotifyBtn.setStyle("-fx-background-color:#1f2937;-fx-text-fill:#cbd5e1;-fx-font-weight:700;-fx-background-radius:999;");
        sourceSoundCloudBtn.setStyle("-fx-background-color:#ea580c;-fx-text-fill:#fff7ed;-fx-font-weight:700;-fx-background-radius:999;");
        HBox sourceRow = new HBox(8, sourceSpotifyBtn, sourceSoundCloudBtn);
        sourceGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            currentAudioSource = (newV == sourceSoundCloudBtn) ? "soundcloud" : "spotify";
            sourceSpotifyBtn.setStyle((newV == sourceSpotifyBtn)
                    ? "-fx-background-color:#1d4ed8;-fx-text-fill:#dbeafe;-fx-font-weight:700;-fx-background-radius:999;"
                    : "-fx-background-color:#1f2937;-fx-text-fill:#cbd5e1;-fx-font-weight:700;-fx-background-radius:999;");
            sourceSoundCloudBtn.setStyle((newV == sourceSoundCloudBtn)
                    ? "-fx-background-color:#ea580c;-fx-text-fill:#fff7ed;-fx-font-weight:700;-fx-background-radius:999;"
                    : "-fx-background-color:#1f2937;-fx-text-fill:#cbd5e1;-fx-font-weight:700;-fx-background-radius:999;");
            refreshSpotifyChoices(preferences.get(PREF_SPOTIFY_PRESET, "nature"));
        });

        spotifyChoiceList = new javafx.scene.control.ListView<>();
        spotifyChoiceList.setPrefHeight(150);
        spotifyChoiceList.setMaxHeight(150);
        spotifyChoiceList.setStyle(
                "-fx-control-inner-background: #111827;" +
                "-fx-background-color: #111827;" +
                "-fx-text-fill: #e2e8f0;" +
                "-fx-border-color: #334155;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;"
        );
        spotifyChoiceList.setCellFactory(lv -> new ListCell<>() {
            private final ImageView iv = new ImageView();
            private final Label txt = new Label();
            private final Label sub = new Label();
            private final VBox textCol = new VBox(2, txt, sub);
            private final HBox row = new HBox(10, iv, textCol);
            {
                iv.setFitWidth(40);
                iv.setFitHeight(40);
                iv.setPreserveRatio(true);
                txt.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12px;-fx-font-weight:600;");
                sub.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
                row.setStyle("-fx-padding:6 4 6 2;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                txt.setText(item);
                sub.setText(isSoundCloudSource() ? "SoundCloud" : "Spotify");
                String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
                iv.setImage(resolveThumbForPreset(preset));
                setText(null);
                setGraphic(row);
                setStyle(isSelected()
                        ? "-fx-background-color: rgba(59,130,246,0.20); -fx-background-radius: 8;"
                        : "-fx-background-color: transparent;");
            }
        });
        spotifyChoiceList.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV == null ? -1 : newV.intValue();
            String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
            List<SpotifyChoice> choices = (isSoundCloudSource() ? soundCloudPresetChoices : spotifyPresetChoices)
                    .getOrDefault(preset, List.of());
            if (idx >= 0 && idx < choices.size()) {
                currentSpotifyChoiceUri = choices.get(idx).uri();
                updateNowPlayingCard(choices.get(idx).label());
                if (isSoundCloudSource()) {
                    playSoundCloudInWidget(currentSpotifyChoiceUri);
                    audioPlaybackActive = true;
                }
                if (spotifyNowPlayingDynamic != null) {
                    spotifyNowPlayingDynamic.setText("Selected: " + choices.get(idx).label());
                }
            }
        });
        spotifyChoiceList.setOnMouseClicked(e -> {
            int idx = spotifyChoiceList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
                List<SpotifyChoice> choices = (isSoundCloudSource() ? soundCloudPresetChoices : spotifyPresetChoices)
                        .getOrDefault(preset, List.of());
                if (idx < choices.size()) {
                    currentSpotifyChoiceUri = choices.get(idx).uri();
                    updateNowPlayingCard(choices.get(idx).label());
                    if (isSoundCloudSource()) {
                        playSoundCloudInWidget(currentSpotifyChoiceUri);
                        audioPlaybackActive = true;
                    }
                }
            }
        });

        soundCloudView = new WebView();
        soundCloudView.setContextMenuEnabled(false);
        soundCloudView.setPrefHeight(120);
        soundCloudView.setStyle("-fx-background-color:#0b1220;-fx-border-color:#334155;-fx-border-radius:8;-fx-background-radius:8;");

        HBox controls = new HBox(10);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button prev = new Button("\u23ee");
        Button play = new Button("\u25b6 Play");
        Button plus = new Button("\u23ed");
        Button stop = new Button("\u25a0 Stop");
        Button dots = new Button("\u2197");
        prev.getStyleClass().add("spotify-control-btn");
        play.getStyleClass().add("spotify-control-btn");
        plus.getStyleClass().add("spotify-control-btn");
        stop.getStyleClass().add("spotify-control-btn");
        dots.getStyleClass().add("spotify-control-btn");
        prev.setOnAction(e -> playPreviousSpotifyTrack());
        play.setOnAction(e -> openSpotifyCurrentPreset());
        plus.setOnAction(e -> playNextSpotifyTrack());
        stop.setOnAction(e -> stopAudioPlayback());
        dots.setOnAction(e -> openSpotifyCurrentPreset());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controls.getChildren().addAll(prev, spacer, play, plus, stop, dots);

        // Visual order like reference: player card on top, list under it.
        spotifyMockCard.getChildren().addAll(sourceRow, spotifyNowPlayingDynamic, statusHint, soundCloudView, spotifyChoiceList, controls);

        int footIndex = Math.max(spotifyPanel.getChildren().size() - 1, 0);
        spotifyPanel.getChildren().add(footIndex, spotifyMockCard);
        refreshSpotifyChoices(preferences.get(PREF_SPOTIFY_PRESET, "nature"));
    }

    private void refreshSpotifyChoices(String preset) {
        if (spotifyChoiceList == null) {
            return;
        }
        String safePreset = spotifyPresetChoices.containsKey(preset) ? preset : "nature";
        Map<String, List<SpotifyChoice>> sourceMap = isSoundCloudSource() ? soundCloudPresetChoices : spotifyPresetChoices;
        List<SpotifyChoice> choices = sourceMap.getOrDefault(safePreset, List.of());
        spotifyChoiceList.setItems(FXCollections.observableArrayList(
                choices.stream().map(SpotifyChoice::label).toList()
        ));
        if (!choices.isEmpty()) {
            spotifyChoiceList.getSelectionModel().select(0);
            currentSpotifyChoiceUri = choices.get(0).uri();
            updateNowPlayingCard(choices.get(0).label());
            if (isSoundCloudSource()) {
                prepareSoundCloudTrack(currentSpotifyChoiceUri);
            }
        } else {
            currentSpotifyChoiceUri = spotifyAppUris.getOrDefault(safePreset, spotifyAppUris.get("nature"));
        }
    }

    private void updateNowPlayingCard(String label) {
        if (spotifyNowTitle == null || spotifyNowMeta == null || spotifyNowCover == null) {
            return;
        }
        String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
        spotifyNowTitle.setText(label == null || label.isBlank() ? "Select a track" : label);
        spotifyNowMeta.setText((isSoundCloudSource() ? "SoundCloud" : "Spotify") + " • " + preset.substring(0, 1).toUpperCase(Locale.ROOT) + preset.substring(1));
        Image img = switch (preset) {
            case "quran" -> resolveThumbForPreset("quran");
            case "relax" -> resolveThumbForPreset("relax");
            case "motivation" -> resolveThumbForPreset("motivation");
            default -> resolveThumbForPreset("nature");
        };
        spotifyNowCover.setImage(img);
    }

    private Image resolveThumbForPreset(String preset) {
        Image networkImage = switch (preset) {
            case "quran" -> thumbQuran;
            case "relax" -> thumbRelax;
            case "motivation" -> thumbMotivation;
            default -> thumbNature;
        };
        if (networkImage == null || networkImage.isError()) {
            return switch (preset) {
                case "quran" -> moodGreatImg;
                case "relax" -> moodGoodImg;
                case "motivation" -> moodOkayImg;
                default -> moodTiredImg;
            };
        }
        return networkImage;
    }

    private void playSoundCloudInWidget(String targetUrl) {
        if (soundCloudView == null || targetUrl == null || targetUrl.isBlank()) {
            return;
        }
        String embed = "https://w.soundcloud.com/player/?url="
                + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8)
                + "&auto_play=true&hide_related=true&show_comments=false&show_user=true&show_reposts=false&show_teaser=false&visual=false";
        soundCloudView.getEngine().load(embed);
        lastLoadedSoundCloudUrl = targetUrl;
        lastPreparedSoundCloudUrl = targetUrl;
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("SoundCloud playing");
        }
    }

    private void prepareSoundCloudTrack(String targetUrl) {
        if (soundCloudView == null || targetUrl == null || targetUrl.isBlank()) {
            return;
        }
        if (targetUrl.equals(lastPreparedSoundCloudUrl)) {
            return;
        }
        String embed = "https://w.soundcloud.com/player/?url="
                + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8)
                + "&auto_play=false&hide_related=true&show_comments=false&show_user=true&show_reposts=false&show_teaser=false&visual=false";
        WebEngine engine = soundCloudView.getEngine();
        engine.load(embed);
        lastPreparedSoundCloudUrl = targetUrl;
    }

    private void stopAudioPlayback() {
        if (!audioPlaybackActive) {
            return;
        }
        if (isSoundCloudSource()) {
            if (soundCloudView != null) {
                soundCloudView.getEngine().load("about:blank");
            }
        } else {
            // Best effort pause for Spotify desktop
            sendWindowsMediaKey(0xB3);
        }
        audioPlaybackActive = false;
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Playback stopped");
        }
    }

    private void applySpotifyCardTheme(String preset) {
        if (spotifyMockCard == null) {
            return;
        }
        String style;
        switch (preset) {
            case "quran" -> style = "-fx-background-color: #c20f1f; -fx-background-radius: 12; -fx-padding: 8;";
            case "relax" -> style = "-fx-background-color: #4a413f; -fx-background-radius: 12; -fx-padding: 8;";
            case "motivation" -> style = "-fx-background-color: #3c4fca; -fx-background-radius: 12; -fx-padding: 8;";
            default -> style = "-fx-background-color: #2f2f31; -fx-background-radius: 12; -fx-padding: 8;";
        }
        spotifyMockCard.setStyle(style);
    }

    private void updateSpotifyPresetStyles(String selected) {
        styleSpotifyChip(spotifyPresetNatureBtn, "nature".equals(selected));
        styleSpotifyChip(spotifyPresetQuranBtn, "quran".equals(selected));
        styleSpotifyChip(spotifyPresetRelaxBtn, "relax".equals(selected));
        styleSpotifyChip(spotifyPresetMotivationBtn, "motivation".equals(selected));
    }

    private void styleSpotifyChip(Button chip, boolean selected) {
        if (chip == null) {
            return;
        }
        chip.getStyleClass().remove("active");
        if (selected) {
            chip.getStyleClass().add("active");
        }
    }

    private void applySavedSpotifyPosition() {
        if (spotifyFloatingButton == null) {
            return;
        }
        double savedX = preferences.getDouble(PREF_SPOTIFY_POS_X, Double.NaN);
        double savedY = preferences.getDouble(PREF_SPOTIFY_POS_Y, Double.NaN);
        if (Double.isNaN(savedX) || Double.isNaN(savedY)) {
            setSpotifyPosition(335, 16);
            saveSpotifyPosition();
            return;
        }
        setSpotifyPosition(savedX, savedY);
    }

    private void setSpotifyPosition(double x, double y) {
        if (spotifyFloatingButton == null || quoteOverlayPane == null) {
            return;
        }
        spotifyFloatingButton.applyCss();
        spotifyFloatingButton.autosize();

        double widgetWidth = spotifyFloatingButton.prefWidth(-1);
        double widgetHeight = spotifyFloatingButton.prefHeight(-1);
        double overlayWidth = quoteOverlayPane.getWidth();
        double overlayHeight = quoteOverlayPane.getHeight();

        double minX = SPOTIFY_MARGIN;
        double minY = SPOTIFY_MARGIN;
        double maxX = overlayWidth > 0 ? Math.max(minX, overlayWidth - widgetWidth - SPOTIFY_MARGIN) : x;
        double maxY = overlayHeight > 0 ? Math.max(minY, overlayHeight - widgetHeight - SPOTIFY_MARGIN) : y;

        double clampedX = Math.max(minX, Math.min(x, maxX));
        double clampedY = Math.max(minY, Math.min(y, maxY));
        spotifyFloatingButton.setLayoutX(clampedX);
        spotifyFloatingButton.setLayoutY(clampedY);
        if (spotifyPanel != null) {
            spotifyPanel.setLayoutX(clampedX);
            spotifyPanel.setLayoutY(clampedY + 66);
        }
    }

    private void clampSpotifyToOverlay() {
        if (spotifyFloatingButton == null) {
            return;
        }
        setSpotifyPosition(spotifyFloatingButton.getLayoutX(), spotifyFloatingButton.getLayoutY());
        saveSpotifyPosition();
    }

    private void saveSpotifyPosition() {
        if (spotifyFloatingButton == null) {
            return;
        }
        preferences.putDouble(PREF_SPOTIFY_POS_X, spotifyFloatingButton.getLayoutX());
        preferences.putDouble(PREF_SPOTIFY_POS_Y, spotifyFloatingButton.getLayoutY());
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
    private void showPlanning() {
        setActiveButton(btnPlanning);
        loadContent("views/Planning.fxml");
    }

    @FXML
    private void showRevisions() {
        setActiveButton(btnRevisions);
        loadContent("views/Revisions.fxml");
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
