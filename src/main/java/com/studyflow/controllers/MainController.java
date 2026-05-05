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
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.OverrunStyle;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private WebView soundCloudPlayerWebView;
    private SwingNode spotifyBrowserNode;
    private javafx.scene.control.ListView<String> spotifyChoiceList;
    private TextField spotifySearchField;
    private String currentSpotifyChoiceUri;
    private List<SpotifyChoice> visibleSpotifyChoices = List.of();
    private final Map<String, String> choiceArtworkByLabel = new HashMap<>();
    private final Map<String, String> choiceArtistByLabel = new HashMap<>();
    private final Map<String, String> choiceMoodByLabel = new HashMap<>();
    private final Map<String, String> soundCloudMoodByUri = new HashMap<>();
    private javafx.scene.control.Slider spotifyVolumeSlider;
    private javafx.scene.control.Slider spotifySeekSlider;
    private Label spotifyTimeLabel;
    private boolean isSeeking;
    private boolean userStoppedPlayback;
    private int currentChoiceIndex = -1;
    private WebView soundCloudView;
    private ImageView soundMusicPreviewImage;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
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
    private static final double SPOTIFY_MIN_WIDTH = 300.0;
    private static final double SPOTIFY_MIN_HEIGHT = 400.0;
    private static final double SPOTIFY_RESIZE_MARGIN = 8.0;
    private double quoteDragStartX;
    private double quoteDragStartY;
    private double quoteStartLayoutX;
    private double quoteStartLayoutY;
    private double spotifyDragStartX;
    private double spotifyDragStartY;
    private double spotifyStartLayoutX;
    private double spotifyStartLayoutY;
    private boolean spotifyDragging;
    private double spotifyResizeStartX;
    private double spotifyResizeStartY;
    private double spotifyResizeStartW;
    private double spotifyResizeStartH;
    private double spotifyResizeStartLayoutX;
    private double spotifyResizeStartLayoutY;
    private ResizeDirection spotifyResizeDirection = ResizeDirection.NONE;
    private boolean spotifyResizing;

    private enum ResizeDirection {
        NONE,
        N, S, E, W,
        NE, NW, SE, SW
    }
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
        buildSoundCloudMoodIndex();
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

        Tooltip.install(spotifyFloatingButton, new Tooltip("Sound Music"));
        buildSpotifyMockCardIfNeeded();
        if (spotifyPanel != null) {
            spotifyPanel.setStyle("-fx-background-color: linear-gradient(to bottom,#0d0d18,#050512); -fx-border-color: #2b2350; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 0;");
            setupSpotifyResizeHandlers();
        }
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
        // Keep small floating icon visible.
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
        // Hide floating icon while panel is open.
        spotifyFloatingButton.setVisible(visible);
        spotifyFloatingButton.setManaged(visible);
    }

    private void setupSpotifyResizeHandlers() {
        if (spotifyPanel == null) {
            return;
        }
        Rectangle panelClip = new Rectangle();
        panelClip.widthProperty().bind(spotifyPanel.widthProperty());
        panelClip.heightProperty().bind(spotifyPanel.heightProperty());
        spotifyPanel.setClip(panelClip);
        spotifyPanel.setMinWidth(SPOTIFY_MIN_WIDTH);
        spotifyPanel.setMinHeight(SPOTIFY_MIN_HEIGHT);
        spotifyPanel.setOnMouseMoved(event -> {
            if (spotifyResizing) {
                return;
            }
            ResizeDirection dir = detectResizeDirection(event.getX(), event.getY());
            spotifyPanel.setCursor(cursorForResizeDirection(dir));
        });
        spotifyPanel.setOnMouseExited(event -> {
            if (!spotifyResizing) {
                spotifyPanel.setCursor(Cursor.DEFAULT);
            }
        });
        spotifyPanel.setOnMousePressed(event -> {
            ResizeDirection dir = detectResizeDirection(event.getX(), event.getY());
            if (dir == ResizeDirection.NONE) {
                return;
            }
            spotifyResizing = true;
            spotifyResizeDirection = dir;
            spotifyResizeStartX = event.getScreenX();
            spotifyResizeStartY = event.getScreenY();
            spotifyResizeStartW = spotifyPanel.getWidth() > 0 ? spotifyPanel.getWidth() : spotifyPanel.getPrefWidth();
            spotifyResizeStartH = spotifyPanel.getHeight() > 0 ? spotifyPanel.getHeight() : spotifyPanel.getPrefHeight();
            spotifyResizeStartLayoutX = spotifyPanel.getLayoutX();
            spotifyResizeStartLayoutY = spotifyPanel.getLayoutY();
            event.consume();
        });
        spotifyPanel.setOnMouseDragged(event -> {
            if (!spotifyResizing || spotifyResizeDirection == ResizeDirection.NONE) {
                return;
            }
            resizeSpotifyPanel(event.getScreenX() - spotifyResizeStartX, event.getScreenY() - spotifyResizeStartY);
            event.consume();
        });
        spotifyPanel.setOnMouseReleased(event -> {
            if (spotifyResizing) {
                spotifyResizing = false;
                spotifyResizeDirection = ResizeDirection.NONE;
                spotifyPanel.setCursor(Cursor.DEFAULT);
            }
        });
    }

    private ResizeDirection detectResizeDirection(double localX, double localY) {
        if (spotifyPanel == null) {
            return ResizeDirection.NONE;
        }
        double w = spotifyPanel.getWidth() > 0 ? spotifyPanel.getWidth() : spotifyPanel.getPrefWidth();
        double h = spotifyPanel.getHeight() > 0 ? spotifyPanel.getHeight() : spotifyPanel.getPrefHeight();
        boolean left = localX <= SPOTIFY_RESIZE_MARGIN;
        boolean right = localX >= w - SPOTIFY_RESIZE_MARGIN;
        boolean top = localY <= SPOTIFY_RESIZE_MARGIN;
        boolean bottom = localY >= h - SPOTIFY_RESIZE_MARGIN;
        if (top && left) return ResizeDirection.NW;
        if (top && right) return ResizeDirection.NE;
        if (bottom && left) return ResizeDirection.SW;
        if (bottom && right) return ResizeDirection.SE;
        if (top) return ResizeDirection.N;
        if (bottom) return ResizeDirection.S;
        if (left) return ResizeDirection.W;
        if (right) return ResizeDirection.E;
        return ResizeDirection.NONE;
    }

    private Cursor cursorForResizeDirection(ResizeDirection dir) {
        return switch (dir) {
            case N -> Cursor.N_RESIZE;
            case S -> Cursor.S_RESIZE;
            case E -> Cursor.E_RESIZE;
            case W -> Cursor.W_RESIZE;
            case NE -> Cursor.NE_RESIZE;
            case NW -> Cursor.NW_RESIZE;
            case SE -> Cursor.SE_RESIZE;
            case SW -> Cursor.SW_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private void resizeSpotifyPanel(double deltaX, double deltaY) {
        if (spotifyPanel == null || quoteOverlayPane == null) {
            return;
        }
        double newW = spotifyResizeStartW;
        double newH = spotifyResizeStartH;
        double newX = spotifyResizeStartLayoutX;
        double newY = spotifyResizeStartLayoutY;

        if (spotifyResizeDirection == ResizeDirection.E || spotifyResizeDirection == ResizeDirection.NE || spotifyResizeDirection == ResizeDirection.SE) {
            newW = spotifyResizeStartW + deltaX;
        }
        if (spotifyResizeDirection == ResizeDirection.S || spotifyResizeDirection == ResizeDirection.SE || spotifyResizeDirection == ResizeDirection.SW) {
            newH = spotifyResizeStartH + deltaY;
        }
        if (spotifyResizeDirection == ResizeDirection.W || spotifyResizeDirection == ResizeDirection.NW || spotifyResizeDirection == ResizeDirection.SW) {
            newW = spotifyResizeStartW - deltaX;
            newX = spotifyResizeStartLayoutX + deltaX;
        }
        if (spotifyResizeDirection == ResizeDirection.N || spotifyResizeDirection == ResizeDirection.NE || spotifyResizeDirection == ResizeDirection.NW) {
            newH = spotifyResizeStartH - deltaY;
            newY = spotifyResizeStartLayoutY + deltaY;
        }

        if (newW < SPOTIFY_MIN_WIDTH) {
            if (spotifyResizeDirection == ResizeDirection.W || spotifyResizeDirection == ResizeDirection.NW || spotifyResizeDirection == ResizeDirection.SW) {
                newX -= (SPOTIFY_MIN_WIDTH - newW);
            }
            newW = SPOTIFY_MIN_WIDTH;
        }
        if (newH < SPOTIFY_MIN_HEIGHT) {
            if (spotifyResizeDirection == ResizeDirection.N || spotifyResizeDirection == ResizeDirection.NE || spotifyResizeDirection == ResizeDirection.NW) {
                newY -= (SPOTIFY_MIN_HEIGHT - newH);
            }
            newH = SPOTIFY_MIN_HEIGHT;
        }

        double overlayW = quoteOverlayPane.getWidth();
        double overlayH = quoteOverlayPane.getHeight();
        if (overlayW > 0) {
            newX = Math.max(SPOTIFY_MARGIN, Math.min(newX, overlayW - newW - SPOTIFY_MARGIN));
            newW = Math.min(newW, overlayW - (SPOTIFY_MARGIN * 2));
            newW = Math.max(newW, SPOTIFY_MIN_WIDTH);
        }
        if (overlayH > 0) {
            newY = Math.max(SPOTIFY_MARGIN, Math.min(newY, overlayH - newH - SPOTIFY_MARGIN));
            newH = Math.min(newH, overlayH - (SPOTIFY_MARGIN * 2));
            newH = Math.max(newH, SPOTIFY_MIN_HEIGHT);
        }

        spotifyPanel.setLayoutX(newX);
        spotifyPanel.setLayoutY(newY);
        spotifyPanel.setMinWidth(SPOTIFY_MIN_WIDTH);
        spotifyPanel.setMinHeight(SPOTIFY_MIN_HEIGHT);
        spotifyPanel.setPrefWidth(newW);
        spotifyPanel.setPrefHeight(newH);
        if (spotifyMockCard != null) {
            spotifyMockCard.setPrefWidth(newW);
            spotifyMockCard.setPrefHeight(newH);
        }
        clampSpotifyCardToContainer();
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
        playCurrentInApp();
    }

    private void playPreset(String preset) {
        String safePreset = spotifyEmbeds.containsKey(preset) ? preset : "nature";
        String chosenUri = (currentSpotifyChoiceUri == null || currentSpotifyChoiceUri.isBlank())
                ? soundCloudPresetChoices.getOrDefault(safePreset, List.of()).stream().findFirst().map(SpotifyChoice::uri).orElse("")
                : currentSpotifyChoiceUri;
        currentSpotifyChoiceUri = chosenUri;
        playCurrentInApp();
        audioPlaybackActive = true;
        updateNowPlayingCard(spotifyChoiceList == null ? null : spotifyChoiceList.getSelectionModel().getSelectedItem());
        currentSpotifyPresetLoaded = safePreset;
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Playing in app");
        }
    }

    private void playPreviousSpotifyTrack() {
        shiftSoundCloudSelection(-1);
        playCurrentInApp();
    }

    private void playNextSpotifyTrack() {
        shiftSoundCloudSelection(1);
        playCurrentInApp();
    }

    private boolean isSoundCloudSource() {
        return true;
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
        currentChoiceIndex = idx;
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
        // Use parent panel as the only frame to avoid double-card look.
        spotifyMockCard.setStyle("-fx-background-color:linear-gradient(to bottom,#0d0d18,#050512);-fx-border-color:#2b2350;-fx-border-radius:20;-fx-background-radius:20;-fx-padding:12 12 10 12;");
        spotifyMockCard.setMinWidth(SPOTIFY_MIN_WIDTH);
        spotifyMockCard.setPrefWidth(390);
        spotifyMockCard.setMaxWidth(Double.MAX_VALUE);
        spotifyMockCard.setMinHeight(SPOTIFY_MIN_HEIGHT);
        spotifyMockCard.setPrefHeight(640);
        spotifyMockCard.setMaxHeight(Double.MAX_VALUE);
        spotifyNowPlayingDynamic = new Label("LIVE");
        spotifyNowPlayingDynamic.getStyleClass().add("spotify-now-playing");
        spotifyNowPlayingDynamic.setStyle("-fx-text-fill:#f0abfc;-fx-font-family:Monospaced;-fx-font-size:10px;-fx-background-color:#03060f;-fx-padding:3 10;-fx-background-radius:999;-fx-border-color:#c084fc;-fx-border-radius:999;");
        spotifyNowTitle = new Label("Select a track");
        spotifyNowTitle.setStyle("-fx-text-fill:#f3f4ff;-fx-font-size:36px;-fx-font-weight:800;");
        spotifyNowMeta = new Label("SoundCloud");
        spotifyNowMeta.setStyle("-fx-text-fill:#b5b0cf;-fx-font-size:18px;");
        spotifyNowCover = new ImageView(moodGoodImg);
        spotifyNowCover.setFitWidth(330);
        spotifyNowCover.setFitHeight(160);
        spotifyNowCover.setPreserveRatio(true);
        StackPane coverBox = new StackPane(spotifyNowCover);
        coverBox.setStyle("-fx-background-color:#120f2b;-fx-background-radius:18;-fx-border-color:#3e2d75;-fx-border-radius:18;-fx-padding:8;");

        Label statusHint = new Label("UP NEXT");
        statusHint.setStyle("-fx-text-fill:#6b6b8f;-fx-font-family:Monospaced;-fx-font-size:10px;");
        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color:#ffffff10;");

        Circle logoCircle = new Circle(16);
        logoCircle.setFill(javafx.scene.paint.Color.web("#a78bfa"));
        Label logoTxt = new Label("\u266A");
        logoTxt.setStyle("-fx-text-fill:white;-fx-font-size:12px;");
        StackPane logo = new StackPane(logoCircle, logoTxt);
        Label appName = new Label("Soundflow");
        appName.setStyle("-fx-text-fill:#f0f0ff;-fx-font-weight:700;-fx-font-size:13px;");
        HBox leftHdr = new HBox(8, logo, appName);
        leftHdr.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label closeBadge = new Label("\u00D7");
        closeBadge.setStyle("-fx-text-fill:#6b6b8f;-fx-font-size:14px;-fx-background-color:#16162a;-fx-border-color:#ffffff20;-fx-border-radius:999;-fx-background-radius:999;-fx-padding:3 8;");
        closeBadge.setOnMouseClicked(e -> hideSpotifyPanel());
        HBox headerRow = new HBox();
        HBox.setHgrow(leftHdr, Priority.ALWAYS);
        headerRow.getChildren().addAll(leftHdr, closeBadge);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ToggleButton chipNature = new ToggleButton("Nature");
        ToggleButton chipQuran = new ToggleButton("Quran");
        ToggleButton chipRelax = new ToggleButton("Relax");
        ToggleButton chipFocus = new ToggleButton("Focus");
        ToggleButton chipLofi = new ToggleButton("Lofi");
        ToggleGroup chipGroup = new ToggleGroup();
        chipNature.setToggleGroup(chipGroup);
        chipQuran.setToggleGroup(chipGroup);
        chipRelax.setToggleGroup(chipGroup);
        chipFocus.setToggleGroup(chipGroup);
        chipLofi.setToggleGroup(chipGroup);
        for (ToggleButton chip : List.of(chipNature, chipQuran, chipRelax, chipFocus, chipLofi)) {
            chip.setMinWidth(0);
            chip.setPrefWidth(70);
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setTextOverrun(OverrunStyle.CLIP);
        }
        chipNature.setSelected(true);
        String chipOff = "-fx-background-color:linear-gradient(to bottom,#1a1b34,#111227);-fx-text-fill:#9ea0be;-fx-background-radius:999;-fx-padding:6 16;-fx-border-color:#3a3458;-fx-border-radius:999;";
        String chipOn = "-fx-background-color:linear-gradient(to right,#8b5cf6,#ec4899);-fx-text-fill:white;-fx-background-radius:999;-fx-padding:6 16;-fx-font-weight:700;";
        chipNature.setStyle(chipOn); chipQuran.setStyle(chipOff); chipRelax.setStyle(chipOff); chipFocus.setStyle(chipOff); chipLofi.setStyle(chipOff);
        HBox sourceRow = new HBox(6, chipNature, chipQuran, chipRelax, chipFocus, chipLofi);
        sourceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(chipNature, Priority.ALWAYS);
        HBox.setHgrow(chipQuran, Priority.ALWAYS);
        HBox.setHgrow(chipRelax, Priority.ALWAYS);
        HBox.setHgrow(chipFocus, Priority.ALWAYS);
        HBox.setHgrow(chipLofi, Priority.ALWAYS);
        chipNature.prefWidthProperty().bind(sourceRow.widthProperty().subtract(24).divide(5));
        chipQuran.prefWidthProperty().bind(sourceRow.widthProperty().subtract(24).divide(5));
        chipRelax.prefWidthProperty().bind(sourceRow.widthProperty().subtract(24).divide(5));
        chipFocus.prefWidthProperty().bind(sourceRow.widthProperty().subtract(24).divide(5));
        chipLofi.prefWidthProperty().bind(sourceRow.widthProperty().subtract(24).divide(5));
        chipGroup.selectedToggleProperty().addListener((obs, ov, nv) -> {
            chipNature.setStyle(nv == chipNature ? chipOn : chipOff);
            chipQuran.setStyle(nv == chipQuran ? chipOn : chipOff);
            chipRelax.setStyle(nv == chipRelax ? chipOn : chipOff);
            chipFocus.setStyle(nv == chipFocus ? chipOn : chipOff);
            chipLofi.setStyle(nv == chipLofi ? chipOn : chipOff);
            if (nv == chipQuran) {
                preferences.put(PREF_SPOTIFY_PRESET, "quran");
            } else if (nv == chipRelax) {
                preferences.put(PREF_SPOTIFY_PRESET, "relax");
            } else if (nv == chipFocus || nv == chipLofi) {
                preferences.put(PREF_SPOTIFY_PRESET, "motivation");
            } else {
                preferences.put(PREF_SPOTIFY_PRESET, "nature");
            }
            refreshSpotifyChoices(preferences.get(PREF_SPOTIFY_PRESET, "nature"));
        });

        spotifyChoiceList = new javafx.scene.control.ListView<>();
        spotifyChoiceList.setPrefHeight(145);
        spotifyChoiceList.setMaxHeight(145);
        spotifyChoiceList.setStyle(
                "-fx-control-inner-background: #070716;" +
                "-fx-background-color: #070716;" +
                "-fx-text-fill: #f0f0ff;" +
                "-fx-border-color: #2f2951;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
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
                txt.setStyle("-fx-text-fill:#f0f0ff;-fx-font-size:12px;-fx-font-weight:600;");
                sub.setStyle("-fx-text-fill:#9898b8;-fx-font-size:10px;");
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
                String artist = choiceArtistByLabel.get(item);
                sub.setText((artist == null || artist.isBlank()) ? (isSoundCloudSource() ? "SoundCloud" : "Spotify") : artist);
                String artworkUrl = choiceArtworkByLabel.get(item);
                if (artworkUrl != null && !artworkUrl.isBlank()) {
                    iv.setImage(new Image(artworkUrl, true));
                } else {
                    String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
                    iv.setImage(resolveThumbForPreset(preset));
                }
                setText(null);
                setGraphic(row);
                    setStyle(isSelected()
                        ? "-fx-background-color: rgba(168,85,247,0.20); -fx-border-color: rgba(236,72,153,0.45); -fx-border-radius: 12; -fx-background-radius: 12;"
                        : "-fx-background-color: transparent;");
            }
        });
        spotifyChoiceList.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV == null ? -1 : newV.intValue();
            List<SpotifyChoice> choices = visibleSpotifyChoices;
            if (idx >= 0 && idx < choices.size()) {
                currentSpotifyChoiceUri = choices.get(idx).uri();
                updateNowPlayingCard(choices.get(idx).label());
                currentChoiceIndex = idx;
                if (spotifyNowPlayingDynamic != null) {
                    spotifyNowPlayingDynamic.setText("Selected: " + choices.get(idx).label());
                }
            }
        });
        spotifyChoiceList.setOnMouseClicked(e -> {
            int idx = spotifyChoiceList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                List<SpotifyChoice> choices = visibleSpotifyChoices;
                if (idx < choices.size()) {
                    currentSpotifyChoiceUri = choices.get(idx).uri();
                    updateNowPlayingCard(choices.get(idx).label());
                    currentChoiceIndex = idx;
                }
            }
        });

        soundMusicPreviewImage = new ImageView(resolveThumbForPreset(preferences.get(PREF_SPOTIFY_PRESET, "nature")));
        soundMusicPreviewImage.setFitHeight(155);
        soundMusicPreviewImage.setFitWidth(340);
        soundMusicPreviewImage.setPreserveRatio(true);
        StackPane previewPane = new StackPane(soundMusicPreviewImage);
        previewPane.setMinHeight(168);
        previewPane.setPrefHeight(168);
        previewPane.setMaxHeight(168);
        previewPane.setStyle("-fx-background-color:#110d27;-fx-border-color:#4b2e82;-fx-border-radius:18;-fx-background-radius:18;-fx-padding:10;");
        soundCloudPlayerWebView = new WebView();
        soundCloudPlayerWebView.setPrefSize(1, 1);
        soundCloudPlayerWebView.setMinSize(1, 1);
        soundCloudPlayerWebView.setMaxSize(1, 1);
        soundCloudPlayerWebView.setOpacity(0.01);
        soundCloudPlayerWebView.setVisible(true);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        spotifySearchField = new TextField();
        spotifySearchField.setPromptText("Search music...");
        spotifySearchField.setStyle(
                "-fx-background-color:linear-gradient(to right,#2a2a2a,#363636);" +
                "-fx-text-fill:#f0f0ff;" +
                "-fx-prompt-text-fill:#6b6b8f;" +
                "-fx-background-radius:12;" +
                "-fx-border-color:#4a4a4a;" +
                "-fx-border-radius:12;" +
                "-fx-padding:9 12 9 12;"
        );
        HBox.setHgrow(spotifySearchField, Priority.ALWAYS);
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("spotify-control-btn");
        searchBtn.setVisible(false);
        searchBtn.setManaged(false);
        searchBtn.setOnAction(e -> applyAudioSearch());
        spotifySearchField.setOnAction(e -> applyAudioSearch());
        searchRow.getChildren().addAll(spotifySearchField, searchBtn);

        HBox transportRow = new HBox(8);
        transportRow.setAlignment(javafx.geometry.Pos.CENTER);
        Button shuffle = new Button("\u21C4");
        Button prev = new Button("\u23EE");
        Button play = new Button("\u275A\u275A");
        Button plus = new Button("\u23ED");
        Button repeat = new Button("\u21BA");
        play.setUserData("pause");
        play.setText("\u275A\u275A");
        play.setOnAction(e -> {
            if (spotifyPlayer == null) {
                playCurrentInApp();
                play.setText("\u275A\u275A");
                play.setUserData("pause");
                return;
            }
            MediaPlayer.Status status = spotifyPlayer.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                spotifyPlayer.pause();
                play.setText("\u25B6");
                play.setUserData("play");
            } else {
                spotifyPlayer.play();
                play.setText("\u275A\u275A");
                play.setUserData("pause");
            }
        });
        shuffle.setOnAction(e -> {
            if (spotifyChoiceList == null || spotifyChoiceList.getItems().isEmpty()) {
                return;
            }
            int size = spotifyChoiceList.getItems().size();
            int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(size);
            spotifyChoiceList.getSelectionModel().select(idx);
            currentChoiceIndex = idx;
            playCurrentInApp();
            play.setText("\u275A\u275A");
            play.setUserData("pause");
        });
        prev.getStyleClass().add("spotify-control-btn");
        play.getStyleClass().add("spotify-control-btn");
        plus.getStyleClass().add("spotify-control-btn");
        repeat.getStyleClass().add("spotify-control-btn");
        shuffle.getStyleClass().add("spotify-control-btn");
        String controlBtnStyle = "-fx-background-color:#090a18;-fx-border-color:#3d3f5f;-fx-border-radius:10;-fx-background-radius:10;-fx-text-fill:#f0f0ff;-fx-padding:7 10;-fx-font-weight:700;-fx-font-size:12px;";
        shuffle.setStyle(controlBtnStyle);
        prev.setStyle(controlBtnStyle);
        play.setStyle(controlBtnStyle);
        plus.setStyle(controlBtnStyle);
        repeat.setStyle(controlBtnStyle);
        shuffle.setPrefWidth(46); prev.setPrefWidth(46); play.setPrefWidth(78); plus.setPrefWidth(46); repeat.setPrefWidth(46);
        play.setStyle("-fx-background-color:#121326;-fx-border-color:#f472b6;-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:white;-fx-font-size:18px;-fx-font-weight:700;-fx-padding:12 18;-fx-effect:dropshadow(gaussian, rgba(244,114,182,0.35), 10, 0.4, 0, 0);");
        shuffle.setTooltip(new Tooltip("Shuffle"));
        prev.setTooltip(new Tooltip("Previous"));
        play.setTooltip(new Tooltip("Play/Pause"));
        plus.setTooltip(new Tooltip("Next"));
        repeat.setTooltip(new Tooltip("Replay"));
        repeat.setOnAction(e -> {
            if (spotifyPlayer != null) {
                spotifyPlayer.seek(javafx.util.Duration.ZERO);
                spotifyPlayer.play();
            } else {
                playCurrentInApp();
            }
            play.setText("\u275A\u275A");
            play.setUserData("pause");
        });
        prev.setOnAction(e -> playPreviousSpotifyTrack());
        plus.setOnAction(e -> playNextSpotifyTrack());
        spotifyVolumeSlider = new javafx.scene.control.Slider(0, 100, 70);
        spotifyVolumeSlider.setPrefWidth(96);
        spotifyVolumeSlider.setStyle("-fx-accent:#e879f9;");
        spotifyVolumeSlider.valueProperty().addListener((obs, o, n) -> {
            if (spotifyPlayer != null) {
                spotifyPlayer.setVolume(n.doubleValue() / 100.0);
            }
        });
        spotifySeekSlider = new javafx.scene.control.Slider(0, 100, 0);
        spotifySeekSlider.setPrefWidth(160);
        spotifySeekSlider.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing) {
                seekToPercent(spotifySeekSlider.getValue());
            }
        });
        spotifySeekSlider.setOnMousePressed(e -> isSeeking = true);
        spotifySeekSlider.setOnMouseReleased(e -> {
            isSeeking = false;
            seekToPercent(spotifySeekSlider.getValue());
        });
        spotifyTimeLabel = new Label("00:00 / 00:00");
        spotifyTimeLabel.setStyle("-fx-text-fill:#6b6b8f;-fx-font-size:10px;-fx-font-family:Monospaced;");
        Label seekLabel = new Label("Seek");
        seekLabel.setStyle("-fx-text-fill:#6b6b8f;-fx-font-size:10px;");
        Label volLabel = new Label("Vol");
        volLabel.setStyle("-fx-text-fill:#6b6b8f;-fx-font-size:10px;");
        transportRow.getChildren().addAll(shuffle, prev, play, plus, repeat);
        HBox slidersRow = new HBox(8, seekLabel, spotifySeekSlider, spotifyTimeLabel, volLabel, spotifyVolumeSlider);
        slidersRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(spotifySeekSlider, Priority.ALWAYS);
        VBox controls = new VBox(8, transportRow, slidersRow);
        controls.setStyle("-fx-padding:4 0 0 0;");

        // Visual order like reference: player card on top, list under it.
        VBox titleBlock = new VBox(3, spotifyNowTitle, spotifyNowMeta);
        titleBlock.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBlock.setStyle("-fx-padding:2 2 0 2;");
        HBox liveRow = new HBox(spotifyNowPlayingDynamic);
        liveRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        spotifyMockCard.getChildren().addAll(headerRow, sourceRow, searchRow, previewPane, liveRow, titleBlock, controls, divider, statusHint, spotifyChoiceList, soundCloudPlayerWebView);

        int footIndex = Math.max(spotifyPanel.getChildren().size() - 1, 0);
        spotifyPanel.getChildren().add(footIndex, spotifyMockCard);
        refreshSpotifyChoices(preferences.get(PREF_SPOTIFY_PRESET, "nature"));
        adjustSpotifyCardSizing(640);
    }

    private void clampSpotifyCardToContainer() {
        if (spotifyMockCard == null) {
            return;
        }
        double w = clamp(spotifyMockCard.getPrefWidth(), SPOTIFY_MIN_WIDTH, getSpotifyCardMaxWidth());
        double h = clamp(spotifyMockCard.getPrefHeight(), SPOTIFY_MIN_HEIGHT, getSpotifyCardMaxHeight());
        spotifyMockCard.setPrefWidth(w);
        spotifyMockCard.setPrefHeight(h);
        adjustSpotifyCardSizing(h);
    }

    private double getSpotifyCardMaxWidth() {
        if (spotifyPanel == null) {
            return 900;
        }
        double available = spotifyPanel.getWidth() - 24;
        return Math.max(SPOTIFY_MIN_WIDTH, Math.min(900, available));
    }

    private double getSpotifyCardMaxHeight() {
        if (spotifyPanel == null) {
            return 980;
        }
        // Card should grow with panel; only keep a small inner padding margin.
        double available = spotifyPanel.getHeight() - 24;
        return Math.max(SPOTIFY_MIN_HEIGHT, Math.min(980, available));
    }

    private void adjustSpotifyCardSizing(double cardHeight) {
        if (spotifyChoiceList == null || soundMusicPreviewImage == null || spotifyNowCover == null) {
            return;
        }
        double previewH = clamp(cardHeight * 0.28, 140, 240);
        soundMusicPreviewImage.setFitHeight(previewH);
        soundMusicPreviewImage.setFitWidth(previewH * 1.55);
        spotifyNowCover.setFitHeight(previewH);
        spotifyNowCover.setFitWidth(previewH * 1.55);
        double listH = clamp(cardHeight * 0.44, 170, 560);
        spotifyChoiceList.setPrefHeight(listH);
        spotifyChoiceList.setMaxHeight(listH);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void refreshSpotifyChoices(String preset) {
        if (spotifyChoiceList == null) {
            return;
        }
        String safePreset = spotifyPresetChoices.containsKey(preset) ? preset : "nature";
        Map<String, List<SpotifyChoice>> sourceMap = soundCloudPresetChoices;
        List<SpotifyChoice> allChoices = sourceMap.values().stream().flatMap(List::stream).distinct().toList();
        List<SpotifyChoice> choices = filterChoicesByMood(allChoices, safePreset);
        visibleSpotifyChoices = choices;
        choiceArtworkByLabel.clear();
        choiceArtistByLabel.clear();
        choiceMoodByLabel.clear();
        for (SpotifyChoice choice : choices) {
            choiceMoodByLabel.put(choice.label(), resolveChoiceMood(choice));
        }
        spotifyChoiceList.setItems(FXCollections.observableArrayList(
                choices.stream().map(SpotifyChoice::label).toList()
        ));
        if (!choices.isEmpty()) {
            spotifyChoiceList.getSelectionModel().select(0);
            currentSpotifyChoiceUri = choices.get(0).uri();
            currentChoiceIndex = 0;
            updateNowPlayingCard(choices.get(0).label());
            if (spotifySearchField != null) {
                spotifySearchField.clear();
            }
        } else {
            visibleSpotifyChoices = List.of();
            currentChoiceIndex = -1;
            currentSpotifyChoiceUri = spotifyAppUris.getOrDefault(safePreset, spotifyAppUris.get("nature"));
        }
    }

    private void applyAudioSearch() {
        if (spotifySearchField == null) {
            return;
        }
        String query = spotifySearchField.getText() == null ? "" : spotifySearchField.getText().trim();
        if (query.isBlank()) {
            refreshSpotifyChoices(preferences.get(PREF_SPOTIFY_PRESET, "nature"));
            return;
        }
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Searching: " + query);
        }
        List<SpotifyChoice> results = buildSearchChoices(query);
        visibleSpotifyChoices = results;
        spotifyChoiceList.setItems(FXCollections.observableArrayList(
                results.stream().map(SpotifyChoice::label).toList()
        ));
        if (results.isEmpty()) {
            currentChoiceIndex = -1;
            if (spotifyNowPlayingDynamic != null) {
                spotifyNowPlayingDynamic.setText("No result found");
            }
            spotifyChoiceList.getSelectionModel().clearSelection();
            return;
        }
        spotifyChoiceList.getSelectionModel().select(0);
        currentSpotifyChoiceUri = results.get(0).uri();
        currentChoiceIndex = 0;
        updateNowPlayingCard(results.get(0).label());
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Search results: " + query);
        }
    }

    private String buildSearchUri(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        if (isSoundCloudSource()) {
            return "https://soundcloud.com/search/sounds?q=" + encoded;
        }
        return "spotify:search:" + encoded;
    }

    private List<SpotifyChoice> buildSearchChoices(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        if (q.isBlank()) {
            return List.of();
        }
        List<String> tokens = List.of(q.replaceAll("[^\\p{L}\\p{N}\\s]", " ").split("\\s+"));
        List<SpotifyChoice> localMatches = soundCloudPresetChoices.values().stream()
                .flatMap(List::stream)
                .filter(choice -> {
                    String haystack = (choice.label() + " " + choice.uri()).toLowerCase(Locale.ROOT);
                    for (String token : tokens) {
                        if (!token.isBlank() && !haystack.contains(token)) {
                            return false;
                        }
                    }
                    return true;
                })
                .distinct()
                .toList();
        if (!localMatches.isEmpty()) {
            return localMatches;
        }
        // Fallback: show online results when local catalog has no match.
        return fetchSearchChoices(query);
    }

    private List<SpotifyChoice> fetchSearchChoices(String query) {
        try {
            String endpoint = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&entity=song&limit=20";
            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();
            String body = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
            List<SpotifyChoice> list = new java.util.ArrayList<>();
            choiceArtworkByLabel.clear();
            choiceArtistByLabel.clear();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "\"trackName\":\"(.*?)\".*?\"artistName\":\"(.*?)\".*?\"artworkUrl100\":\"(.*?)\".*?\"previewUrl\":\"(.*?)\"",
                    java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher m = p.matcher(body);
            while (m.find() && list.size() < 20) {
                String track = unescapeJson(m.group(1));
                String artist = unescapeJson(m.group(2));
                String artwork = unescapeJson(m.group(3)).replace("\\/", "/");
                String preview = unescapeJson(m.group(4)).replace("\\/", "/");
                String label = track + " - " + artist;
                list.add(new SpotifyChoice(label, preview));
                choiceArtworkByLabel.put(label, artwork);
                choiceArtistByLabel.put(label, artist);
                choiceMoodByLabel.put(label, preferences.get(PREF_SPOTIFY_PRESET, "nature"));
            }
            return list;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String unescapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\\", "\\");
    }

    private void playCurrentInApp() {
        if (currentSpotifyChoiceUri == null || currentSpotifyChoiceUri.isBlank()) {
            return;
        }
        try {
            // Always stop any currently playing source before starting the new one.
            stopAudioPlayback();
            String playable = currentSpotifyChoiceUri;
            if (isSoundCloudUrl(playable)) {
                playSoundCloudEmbedded(playable);
                userStoppedPlayback = false;
                audioPlaybackActive = true;
                if (spotifyNowPlayingDynamic != null) {
                    spotifyNowPlayingDynamic.setText("Playing full track");
                }
                return;
            }
            String selectedLabel = spotifyChoiceList == null ? null : spotifyChoiceList.getSelectionModel().getSelectedItem();
            String fallbackQuery = (selectedLabel == null || selectedLabel.isBlank()) ? playable : selectedLabel;
            if (requiresPreviewResolution(playable)) {
                String resolved = fetchFirstPreviewUrl(fallbackQuery);
                if (resolved != null && !resolved.isBlank()) {
                    playable = resolved;
                }
            }
            if (requiresPreviewResolution(playable)) {
                if (spotifyNowPlayingDynamic != null) {
                    spotifyNowPlayingDynamic.setText("Cannot play this item");
                }
                return;
            }
            if (spotifyPlayer != null) {
                spotifyPlayer.stop();
                spotifyPlayer.dispose();
            }
            Media media = new Media(playable);
            spotifyPlayer = new MediaPlayer(media);
            spotifyPlayer.setVolume(spotifyVolumeSlider == null ? 0.7 : spotifyVolumeSlider.getValue() / 100.0);
            userStoppedPlayback = false;
            spotifyPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (spotifyPlayer == null || isSeeking) {
                    return;
                }
                javafx.util.Duration total = spotifyPlayer.getTotalDuration();
                if (total != null && total.toMillis() > 0) {
                    double pct = (newT.toMillis() / total.toMillis()) * 100.0;
                    if (spotifySeekSlider != null) {
                        spotifySeekSlider.setValue(Math.max(0, Math.min(100, pct)));
                    }
                    if (spotifyTimeLabel != null) {
                        spotifyTimeLabel.setText(formatTime(newT) + " / " + formatTime(total));
                    }
                }
            });
            spotifyPlayer.setOnReady(() -> {
                if (spotifySeekSlider != null) {
                    spotifySeekSlider.setValue(0);
                }
                if (spotifyTimeLabel != null) {
                    spotifyTimeLabel.setText("00:00 / " + formatTime(spotifyPlayer.getTotalDuration()));
                }
            });
            spotifyPlayer.setOnEndOfMedia(() -> {
                if (!userStoppedPlayback) {
                    playNextSpotifyTrack();
                }
            });
            spotifyPlayer.play();
            audioPlaybackActive = true;
            if (spotifyNowPlayingDynamic != null) {
                spotifyNowPlayingDynamic.setText("Playing in app");
            }
        } catch (Exception ex) {
            if (spotifyNowPlayingDynamic != null) {
                spotifyNowPlayingDynamic.setText("Cannot play this item");
            }
        }
    }

    private boolean isSoundCloudUrl(String uri) {
        return uri != null && uri.toLowerCase(Locale.ROOT).contains("soundcloud.com");
    }

    private void playSoundCloudEmbedded(String trackUrl) {
        if (soundCloudPlayerWebView == null || trackUrl == null || trackUrl.isBlank()) {
            return;
        }
        String embed = "https://w.soundcloud.com/player/?url="
                + URLEncoder.encode(trackUrl, StandardCharsets.UTF_8)
                + "&auto_play=true&hide_related=true&show_comments=false&show_user=true&visual=false";
        soundCloudPlayerWebView.getEngine().load(embed);
    }

    private boolean requiresPreviewResolution(String uri) {
        if (uri == null || uri.isBlank()) {
            return true;
        }
        String lower = uri.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http")) {
            return true;
        }
        if (lower.contains("soundcloud.com")) {
            return true;
        }
        return !(lower.endsWith(".mp3")
                || lower.endsWith(".m4a")
                || lower.endsWith(".aac")
                || lower.endsWith(".wav")
                || lower.contains("preview")
                || lower.contains("audio"));
    }

    private void buildSoundCloudMoodIndex() {
        soundCloudMoodByUri.clear();
        soundCloudPresetChoices.forEach((mood, choices) -> {
            for (SpotifyChoice choice : choices) {
                soundCloudMoodByUri.put(choice.uri(), mood);
            }
        });
    }

    private String resolveChoiceMood(SpotifyChoice choice) {
        if (choice == null) {
            return "nature";
        }
        return soundCloudMoodByUri.getOrDefault(choice.uri(), preferences.get(PREF_SPOTIFY_PRESET, "nature"));
    }

    private List<SpotifyChoice> filterChoicesByMood(List<SpotifyChoice> choices, String mood) {
        String targetMood = (mood == null || mood.isBlank()) ? "nature" : mood.toLowerCase(Locale.ROOT);
        return choices.stream()
                .filter(choice -> targetMood.equals(resolveChoiceMood(choice)))
                .toList();
    }

    private String fetchFirstPreviewUrl(String query) {
        try {
            String endpoint = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&entity=song&limit=1";
            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();
            String body = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"previewUrl\":\"(.*?)\"").matcher(body);
            if (m.find()) {
                return unescapeJson(m.group(1)).replace("\\/", "/");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void updateNowPlayingCard(String label) {
        if (spotifyNowTitle == null || spotifyNowMeta == null || spotifyNowCover == null) {
            return;
        }
        String preset = preferences.get(PREF_SPOTIFY_PRESET, "nature");
        spotifyNowTitle.setText(label == null || label.isBlank() ? "Select a track" : label);
        spotifyNowMeta.setText("Sound Music • " + preset.substring(0, 1).toUpperCase(Locale.ROOT) + preset.substring(1));
        Image img = switch (preset) {
            case "quran" -> resolveThumbForPreset("quran");
            case "relax" -> resolveThumbForPreset("relax");
            case "motivation" -> resolveThumbForPreset("motivation");
            default -> resolveThumbForPreset("nature");
        };
        String selected = spotifyChoiceList == null ? null : spotifyChoiceList.getSelectionModel().getSelectedItem();
        String artworkUrl = selected == null ? null : choiceArtworkByLabel.get(selected);
        if (artworkUrl != null && !artworkUrl.isBlank()) {
            img = new Image(artworkUrl, true);
        }
        spotifyNowCover.setImage(img);
        if (soundMusicPreviewImage != null) {
            soundMusicPreviewImage.setImage(img);
        }
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
        if (targetUrl == null || targetUrl.isBlank()) {
            return;
        }
        openSpotifyUri(targetUrl);
        lastLoadedSoundCloudUrl = targetUrl;
        lastPreparedSoundCloudUrl = targetUrl;
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Opened in browser");
        }
    }

    private void prepareSoundCloudTrack(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return;
        }
        lastPreparedSoundCloudUrl = targetUrl;
    }

    private void stopAudioPlayback() {
        userStoppedPlayback = true;
        if (spotifyPlayer != null) {
            try {
                spotifyPlayer.stop();
            } catch (Exception ignored) {
            }
        }
        if (soundCloudPlayerWebView != null) {
            try {
                soundCloudPlayerWebView.getEngine().load("about:blank");
            } catch (Exception ignored) {
            }
        }
        audioPlaybackActive = false;
        if (spotifyNowPlayingDynamic != null) {
            spotifyNowPlayingDynamic.setText("Playback stopped");
        }
    }

    private void seekBySeconds(int deltaSeconds) {
        if (spotifyPlayer == null) {
            return;
        }
        javafx.util.Duration current = spotifyPlayer.getCurrentTime();
        javafx.util.Duration total = spotifyPlayer.getTotalDuration();
        if (total == null || total.lessThanOrEqualTo(javafx.util.Duration.ZERO)) {
            return;
        }
        javafx.util.Duration target = current.add(javafx.util.Duration.seconds(deltaSeconds));
        if (target.lessThan(javafx.util.Duration.ZERO)) {
            target = javafx.util.Duration.ZERO;
        }
        if (target.greaterThan(total)) {
            target = total;
        }
        spotifyPlayer.seek(target);
    }

    private void seekToPercent(double percent) {
        if (spotifyPlayer == null) {
            return;
        }
        javafx.util.Duration total = spotifyPlayer.getTotalDuration();
        if (total == null || total.lessThanOrEqualTo(javafx.util.Duration.ZERO)) {
            return;
        }
        double p = Math.max(0, Math.min(100, percent));
        spotifyPlayer.seek(total.multiply(p / 100.0));
    }

    private String formatTime(javafx.util.Duration d) {
        if (d == null || d.isUnknown() || d.lessThanOrEqualTo(javafx.util.Duration.ZERO)) {
            return "00:00";
        }
        int s = (int) Math.floor(d.toSeconds());
        int mm = s / 60;
        int ss = s % 60;
        return String.format("%02d:%02d", mm, ss);
    }

    private void applySpotifyCardTheme(String preset) {
        if (spotifyMockCard == null) {
            return;
        }
        String style;
        switch (preset) {
            case "quran" -> style = "-fx-background-color: #6a1b6d; -fx-background-radius: 12; -fx-padding: 8;";
            case "relax" -> style = "-fx-background-color: #4a413f; -fx-background-radius: 12; -fx-padding: 8;";
            case "motivation" -> style = "-fx-background-color: #2b1d5a; -fx-background-radius: 12; -fx-padding: 8;";
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

