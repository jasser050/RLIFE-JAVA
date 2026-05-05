package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.AvatarCard;
import com.studyflow.utils.GlbLoader;
import com.studyflow.utils.UserSession;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private StackPane avatarContainer;
    @FXML private Label avatarInitials;
    @FXML private Label profileName;
    @FXML private Label profileBio;
    @FXML private Label profileEmail;
    @FXML private Label profileUsername;
    @FXML private Label coinsLabel;
    @FXML private Label saveStatus;
    @FXML private Label saveStatusBottom;

    @FXML private StackPane avatarPickerOverlay;
    @FXML private FlowPane profileAvatarContainer;

    private final List<AvatarCard> avatarCards = new ArrayList<>();
    private String selectedAvatar;
    private String currentAvatar;
    private AnimationTimer currentModelRotator = null;
    private AnimationTimer currentAvatarRotator = null;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ToggleButton genderMaleBtn;
    @FXML private ToggleButton genderFemaleBtn;
    @FXML private TextArea bioField;
    @FXML private Label bioCharCount;

    @FXML private TextField universityField;
    @FXML private TextField studentIdField;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label fontSizeLabel;
    @FXML private Slider fontSizeSlider;
    @FXML private ColorPicker accentColorPicker;
    @FXML private Label accentColorLabel;
    @FXML private CheckBox reduceMotionToggle;
    @FXML private CheckBox highContrastToggle;

    // Font family
    @FXML private Button fontSegoe;
    @FXML private Button fontInter;
    @FXML private Button fontJetbrains;
    @FXML private Button fontGeorgia;
    @FXML private Button fontCascadia;
    @FXML private Label fontFamilyLabel;

    // Accent buttons
    @FXML private Button btnDefault;
    @FXML private Button btnViolet;
    @FXML private Button btnIndigo;
    @FXML private Button btnBlue;
    @FXML private Button btnCyan;
    @FXML private Button btnGreen;
    @FXML private Button btnOrange;
    @FXML private Button btnPink;
    @FXML private Button btnRed;

    private String currentAccentHex = null; // null = default (no override)
    private int currentFontSizePx = 14;
    private String currentFontFamily = null; // null = default
    private double currentUiScale = 1.0;
    private String currentCursorStyle = "default";
    private String currentBorderRadius = "soft"; // sharp, soft, round, pill
    private Button activeAccentBtn;
    private Button activeFontBtn;
    private Button activeCursorBtn;
    private Button activeRadiusBtn;
    private String dynamicCssPath;

    // New accessibility FXML fields
    @FXML private Slider uiScaleSlider;
    @FXML private Label uiScaleLabel;
    // Border radius buttons
    @FXML private Button radiusSharp;
    @FXML private Button radiusSoft;
    @FXML private Button radiusRound;
    @FXML private Button radiusPill;
    @FXML private Label borderRadiusLabel;

    @FXML private Button cursorDefault;
    @FXML private Button cursorPen;
    @FXML private Button cursorStar;
    @FXML private Button cursorCrosshair;
    @FXML private Button cursorHeart;
    @FXML private Button cursorDiamond;
    @FXML private Button cursorCircle;
    @FXML private Button cursorArrowFancy;
    @FXML private Button cursorPanda;

    private final ServiceUser serviceUser = new ServiceUser();
    private User currentUser;
    private AnimationTimer avatarRotator;

    private static final int BIO_MAX_CHARS = 280;

    // Font family mappings
    private static final String[][] FONTS = {
        {"Segoe UI", "Segoe UI"},
        {"Inter", "Inter"},
        {"JetBrains Mono", "JetBrains Mono"},
        {"Georgia", "Georgia"},
        {"Cascadia Code", "Cascadia Code"}
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Gender toggle group
        ToggleGroup genderGroup = new ToggleGroup();
        genderMaleBtn.setToggleGroup(genderGroup);
        genderFemaleBtn.setToggleGroup(genderGroup);
        genderGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });

        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            populateFields(currentUser);
        }

        // Bio character counter
        bioField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal == null ? 0 : newVal.length();
            bioCharCount.setText(len + " / " + BIO_MAX_CHARS);
            if (len > BIO_MAX_CHARS) {
                bioCharCount.setStyle("-fx-text-fill: #EF4444;");
                bioField.setText(newVal.substring(0, BIO_MAX_CHARS));
            } else if (len > BIO_MAX_CHARS * 0.85) {
                bioCharCount.setStyle("-fx-text-fill: #F59E0B;");
            } else {
                bioCharCount.setStyle("-fx-text-fill: #64748B;");
            }
        });
        bioCharCount.setText((bioField.getText() == null ? 0 : bioField.getText().length()) + " / " + BIO_MAX_CHARS);

        // Font size slider — dynamic
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            fontSizeSlider.setValue(val);
            String[] names = {"Small", "Compact", "Normal", "Large", "Extra Large"};
            int[] pxSizes = {12, 13, 14, 15, 17};
            if (val >= 1 && val <= 5) {
                fontSizeLabel.setText(names[val - 1]);
                currentFontSizePx = pxSizes[val - 1];
                rebuildDynamicCss();
            }
        });

        // UI Scale slider
        uiScaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            uiScaleSlider.setValue(val);
            String[] names = {"80%", "90%", "100%", "110%", "120%"};
            double[] scales = {0.8, 0.9, 1.0, 1.1, 1.2};
            if (val >= 1 && val <= 5) {
                uiScaleLabel.setText(names[val - 1]);
                currentUiScale = scales[val - 1];
                App.getPrimaryStage().getScene().getRoot().setScaleX(currentUiScale);
                App.getPrimaryStage().getScene().getRoot().setScaleY(currentUiScale);
            }
        });

        // Default states
        activeAccentBtn = btnDefault;
        markAccentActive(btnDefault);
        accentColorPicker.setValue(Color.web("#8B5CF6"));

        activeFontBtn = fontSegoe;
        markFontActive(fontSegoe);

        activeRadiusBtn = radiusSoft;
        markRadiusActive(radiusSoft);

        activeCursorBtn = cursorDefault;
        markCursorActive(cursorDefault);

        // Re-apply dynamic CSS when theme toggles so tinting adapts
        App.setOnThemeChanged(() -> {
            if (currentAccentHex != null || !"soft".equals(currentBorderRadius)) rebuildDynamicCss();
        });
    }

    // ============================================
    // UNIFIED DYNAMIC CSS — rebuilds one stylesheet
    // for accent + font size + font family
    // ============================================

    private void rebuildDynamicCss() {
        StringBuilder css = new StringBuilder();
        css.append("/* StudyFlow dynamic overrides */\n");

        // --- Font size override ---
        css.append(".root {\n");
        css.append("    -fx-font-size: ").append(currentFontSizePx).append("px;\n");
        if (currentFontFamily != null) {
            css.append("    -fx-font-family: \"").append(currentFontFamily).append("\";\n");
        }
        css.append("}\n");

        // --- UI Scale ---
        if (currentUiScale != 1.0) {
            css.append(".scroll-content, .card-body, .card { -fx-scale-x: 1; -fx-scale-y: 1; }\n");
        }

        // --- Border Radius ---
        if (!"soft".equals(currentBorderRadius)) {
            String cardR, btnR, fieldR, pillR, smallR;
            switch (currentBorderRadius) {
                case "sharp":
                    cardR = "0"; btnR = "0"; fieldR = "0"; pillR = "0"; smallR = "0";
                    break;
                case "round":
                    cardR = "24"; btnR = "16"; fieldR = "16"; pillR = "24"; smallR = "12";
                    break;
                case "pill":
                    cardR = "32"; btnR = "50"; fieldR = "50"; pillR = "50"; smallR = "50";
                    break;
                default:
                    cardR = "16"; btnR = "10"; fieldR = "10"; pillR = "16"; smallR = "8";
            }
            css.append(".card { -fx-background-radius: ").append(cardR).append("; -fx-border-radius: ").append(cardR).append("; }\n");
            css.append(".toggle-card { -fx-background-radius: ").append(cardR).append("; -fx-border-radius: ").append(cardR).append("; }\n");
            css.append(".auth-field { -fx-background-radius: ").append(fieldR).append("; -fx-border-radius: ").append(fieldR).append("; }\n");
            css.append(".auth-btn-primary, .auth-btn-primary-sm { -fx-background-radius: ").append(btnR).append("; }\n");
            css.append(".btn-primary { -fx-background-radius: ").append(btnR).append("; }\n");
            css.append(".btn-danger-outline { -fx-background-radius: ").append(btnR).append("; -fx-border-radius: ").append(btnR).append("; }\n");
            css.append(".btn-ghost { -fx-background-radius: ").append(btnR).append("; }\n");
            css.append(".search-field { -fx-background-radius: ").append(fieldR).append("; -fx-border-radius: ").append(fieldR).append("; }\n");
            css.append(".badge { -fx-background-radius: ").append(pillR).append("; }\n");
            css.append(".nav-button { -fx-background-radius: ").append(btnR).append("; }\n");
            css.append(".topbar-button { -fx-background-radius: ").append(btnR).append("; }\n");
            css.append(".gender-card { -fx-background-radius: ").append(cardR).append("; -fx-border-radius: ").append(cardR).append("; }\n");
            css.append(".bio-container { -fx-background-radius: ").append(cardR).append("; -fx-border-radius: ").append(cardR).append("; }\n");
            css.append(".font-family-btn { -fx-background-radius: ").append(smallR).append("; -fx-border-radius: ").append(smallR).append("; }\n");
            css.append(".cursor-btn { -fx-background-radius: ").append(smallR).append("; -fx-border-radius: ").append(smallR).append("; }\n");
            css.append(".radius-style-btn { -fx-background-radius: ").append(smallR).append("; -fx-border-radius: ").append(smallR).append("; }\n");
            css.append(".pattern-btn { -fx-background-radius: ").append(smallR).append("; -fx-border-radius: ").append(smallR).append("; }\n");
            css.append(".accent-color-btn { -fx-background-radius: ").append(smallR).append("; -fx-border-radius: ").append(smallR).append("; }\n");
            css.append(".accent-default-btn { -fx-background-radius: ").append(smallR).append("; -fx-border-radius: ").append(smallR).append("; }\n");
            css.append(".font-size-preview { -fx-background-radius: ").append(pillR).append("; }\n");
            css.append(".font-size-slider .thumb { -fx-background-radius: ").append(pillR).append("; }\n");
            css.append(".profile-hero-banner { -fx-background-radius: ").append(cardR).append("; -fx-border-radius: ").append(cardR).append("; }\n");
            css.append(".profile-info-pill { -fx-background-radius: ").append(pillR).append("; }\n");
            css.append(".stat-icon-box { -fx-background-radius: ").append(smallR).append("; }\n");
            css.append(".toggle-icon, .toggle-icon-contrast { -fx-background-radius: ").append(smallR).append("; }\n");
            css.append(".profile-grad-card { -fx-background-radius: ").append(cardR).append("; }\n");
            css.append(".profile-grad-icon { -fx-background-radius: ").append(smallR).append("; }\n");
            css.append(".admin-input, .admin-textarea { -fx-background-radius: ").append(fieldR).append("; -fx-border-radius: ").append(fieldR).append("; }\n");
            css.append(".chat-bubble-user, .chat-bubble-ai { -fx-background-radius: ").append(cardR).append("; }\n");
            css.append(".chat-send-btn { -fx-background-radius: ").append(btnR).append("; }\n");
            css.append(".chat-input-area { -fx-background-radius: ").append(cardR).append("; -fx-border-radius: ").append(cardR).append("; }\n");
            css.append(".scroll-bar .thumb { -fx-background-radius: ").append(pillR).append("; }\n");
            css.append(".scroll-bar .track { -fx-background-radius: ").append(pillR).append("; }\n");
        }

        // --- Accent color overrides (massive app-wide impact) ---
        if (currentAccentHex != null) {
            Color c = Color.web(currentAccentHex);
            String r = String.valueOf((int)(c.getRed() * 255));
            String g = String.valueOf((int)(c.getGreen() * 255));
            String b = String.valueOf((int)(c.getBlue() * 255));
            String rgb = r + "," + g + "," + b;

            Color lighter = c.deriveColor(0, 0.85, 1.25, 1.0);
            String lHex = toHex(lighter);
            Color darker = c.deriveColor(0, 1.1, 0.75, 1.0);
            String dHex = toHex(darker);
            Color darkest = c.deriveColor(0, 1.2, 0.55, 1.0);
            String ddHex = toHex(darkest);
            String hex = currentAccentHex;

            // ══════════════════════════════════════════
            // FULL APP COLOR TINTING — blend accent into
            // every background, border, and surface
            // Adapts to dark or light mode automatically
            // ══════════════════════════════════════════
            boolean dark = App.isDarkTheme();
            String bg0, bg1, bg2, bg3, bg1h;
            if (dark) {
                bg0  = toHex(tint(Color.web("#020617"), c, 0.06));
                bg1  = toHex(tint(Color.web("#0F172A"), c, 0.07));
                bg2  = toHex(tint(Color.web("#1E293B"), c, 0.08));
                bg3  = toHex(tint(Color.web("#334155"), c, 0.09));
                bg1h = toHex(tint(Color.web("#131D32"), c, 0.07));
            } else {
                bg0  = toHex(tint(Color.web("#F3F6FB"), c, 0.06));
                bg1  = toHex(tint(Color.web("#FFFFFF"), c, 0.05));
                bg2  = toHex(tint(Color.web("#D7E0EE"), c, 0.08));
                bg3  = toHex(tint(Color.web("#B8C5D6"), c, 0.10));
                bg1h = toHex(tint(Color.web("#F8FBFF"), c, 0.06));
            }

            // Root & main backgrounds
            css.append(".root { -fx-background-color: ").append(bg0).append("; }\n");
            css.append(".main-content, .content-area { -fx-background-color: ").append(bg0).append("; }\n");

            // Sidebar
            css.append(".sidebar { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");

            // Topbar
            css.append(".topbar { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg3).append("; }\n");

            // Cards
            css.append(".card { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");
            css.append(".card:hover { -fx-border-color: ").append(bg3).append("; }\n");
            css.append(".card-header { -fx-border-color: ").append(bg2).append("; }\n");

            // Toggle cards
            css.append(".toggle-card { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");
            css.append(".toggle-card:hover { -fx-border-color: ").append(bg3).append("; }\n");

            // Search & input backgrounds
            if (dark) {
                css.append(".search-field { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");
                css.append(".auth-field { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");
            } else {
                css.append(".search-field { -fx-background-color: ").append(bg0).append("; -fx-border-color: ").append(bg2).append("; }\n");
                css.append(".auth-field { -fx-background-color: ").append(bg0).append("; -fx-border-color: ").append(bg2).append("; }\n");
            }

            // Topbar button hover
            css.append(".topbar-button:hover { -fx-background-color: ").append(bg2).append("; }\n");

            // Nav hover
            css.append(".nav-button:hover { -fx-background-color: ").append(bg2).append("; }\n");

            // Gender card backgrounds
            css.append(".gender-card { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");
            css.append(".gender-card:hover { -fx-background-color: ").append(bg1h).append("; -fx-border-color: ").append(bg3).append("; }\n");

            // Bio container
            css.append(".bio-container { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");

            // Font family buttons
            css.append(".font-family-btn { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");
            css.append(".font-family-btn:hover { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");

            // Cursor, radius & pattern buttons
            css.append(".cursor-btn { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");
            css.append(".cursor-btn:hover { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");
            css.append(".radius-style-btn { -fx-background-color: ").append(bg1).append("; -fx-border-color: ").append(bg2).append("; }\n");
            css.append(".radius-style-btn:hover { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");

            // Accent color buttons background
            css.append(".accent-default-btn { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");

            // Slider track
            css.append(".font-size-slider .track { -fx-background-color: ").append(bg2).append("; }\n");

            // Scroll bars
            css.append(".scroll-bar { -fx-background-color: ").append(bg0).append("; }\n");
            css.append(".scroll-bar .track { -fx-background-color: ").append(bg0).append("; }\n");
            css.append(".scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; }\n");

            // Admin panels
            css.append(".admin-input, .admin-textarea { -fx-background-color: ").append(bg2).append("; -fx-border-color: ").append(bg3).append("; }\n");

            // Chat input area
            css.append(".chat-input-area { -fx-background-color: ").append(bg1).append("; }\n");

            // Profile hero banner tint
            css.append(".profile-hero-bg { -fx-background-color: linear-gradient(to bottom right, ").append(bg0).append(", ").append(dHex).append(", ").append(hex).append(", derive(").append(hex).append(", -40%)); }\n");

            // ── Primary buttons ──
            css.append(".auth-btn-primary, .auth-btn-primary-sm {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(hex).append(", ").append(dHex).append(");\n}\n");
            css.append(".auth-btn-primary:hover, .auth-btn-primary-sm:hover {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(lHex).append(", ").append(hex).append(");\n}\n");
            css.append(".btn-primary {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".btn-primary:hover {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(dHex).append(");\n}\n");
            css.append(".btn-primary:pressed {\n")
               .append("    -fx-background-color: ").append(ddHex).append(";\n}\n");

            // ── Navigation ──
            String navText = dark ? lHex : dHex;
            css.append(".nav-button.active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", ").append(dark ? "0.15" : "0.12").append(");\n")
               .append("    -fx-text-fill: ").append(navText).append(";\n}\n");
            css.append(".nav-button.active .ikonli-font-icon {\n")
               .append("    -fx-icon-color: ").append(navText).append(";\n}\n");
            css.append(".nav-button:hover {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.08);\n}\n");
            css.append(".title-bar-logo {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(ddHex).append(");\n}\n");

            // ── Sidebar logo / avatar ──
            css.append(".avatar {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(lHex).append(", ").append(dHex).append(");\n}\n");

            // ── Cards & stat icons ──
            css.append(".stat-icon-box.primary .ikonli-font-icon {\n")
               .append("    -fx-icon-color: ").append(lHex).append(";\n}\n");
            css.append(".gradient-primary {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");

            // ── Search & text fields ──
            css.append(".search-field:focused {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".text-field:focused, .text-area:focused {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".auth-field:focused {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".bio-container:focused-within {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.2), 10, 0, 0, 0);\n}\n");

            // ── Progress bars ──
            css.append(".progress-bar.primary > .bar {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(hex).append(", ").append(lHex).append(");\n}\n");

            // ── Charts ──
            css.append(".default-color0.chart-series-line { -fx-stroke: ").append(hex).append("; }\n");
            css.append(".default-color0.chart-line-symbol { -fx-background-color: ").append(hex).append(", #0F172A; }\n");
            css.append(".default-color0.chart-bar { -fx-bar-fill: ").append(hex).append("; }\n");
            css.append(".default-color0.chart-series-area-line { -fx-stroke: ").append(hex).append("; }\n");
            css.append(".default-color0.chart-pie { -fx-pie-color: ").append(hex).append("; }\n");

            // ── Check/Radio/Toggle ──
            css.append(".check-box:selected .box {\n")
               .append("    -fx-background-color: ").append(dHex).append(";\n")
               .append("    -fx-border-color: ").append(dHex).append(";\n}\n");
            css.append(".radio-button:selected .radio {\n")
               .append("    -fx-background-color: ").append(dHex).append(";\n")
               .append("    -fx-border-color: ").append(dHex).append(";\n}\n");
            css.append(".toggle-button:selected {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.2);\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");
            css.append(".toggle-switch:selected .box {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n")
               .append("    -fx-border-color: ").append(lHex).append(";\n}\n");

            // ── Date picker ──
            css.append(".date-picker-popup .selected, .date-picker-popup .day-cell:selected, .date-picker-popup .day-cell:focused {\n")
               .append("    -fx-background-color: ").append(dHex).append(";\n}\n");
            css.append(".date-picker-popup .today {\n")
               .append("    -fx-border-color: ").append(lHex).append(";\n}\n");
            css.append(".date-picker-popup > .month-year-pane .button {\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");

            // ── Profile hero ──
            css.append(".profile-avatar-ring {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(lHex).append(", ").append(hex).append(", ").append(dHex).append(");\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.6), 20, 0, 0, 0);\n}\n");
            css.append(".profile-avatar-text {\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");
            css.append(".profile-hero-banner {\n")
               .append("    -fx-border-color: rgba(").append(rgb).append(", 0.2);\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.25), 30, 0, 0, 8);\n}\n");
            css.append(".profile-glow-orb {\n")
               .append("    -fx-background-color: radial-gradient(center 50% 50%, radius 50%, rgba(").append(rgb).append(", 0.4) 0%, transparent 70%);\n}\n");
            css.append(".profile-hero-username {\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");

            // ── Gradient cards ──
            css.append(".profile-grad-purple {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(dHex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".profile-grad-violet {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(hex).append(", ").append(lHex).append(");\n}\n");

            // ── Auth / links ──
            css.append(".auth-link { -fx-text-fill: ").append(hex).append("; }\n");
            css.append(".auth-link:hover { -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".auth-brand-logo, .auth-logo-badge {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".auth-btn-secondary {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n")
               .append("    -fx-background-color: linear-gradient(to right, rgba(").append(rgb).append(", 0.12), rgba(").append(rgb).append(", 0.08));\n}\n");
            css.append(".auth-btn-secondary:hover {\n")
               .append("    -fx-border-color: ").append(lHex).append(";\n")
               .append("    -fx-background-color: linear-gradient(to right, rgba(").append(rgb).append(", 0.25), rgba(").append(rgb).append(", 0.16));\n}\n");
            css.append(".auth-btn-secondary .ikonli-font-icon { -fx-icon-color: ").append(hex).append("; }\n");

            // ── Accessibility section styling ──
            css.append(".font-size-preview {\n")
               .append("    -fx-text-fill: ").append(hex).append(";\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.15);\n}\n");
            css.append(".font-size-tick-active { -fx-text-fill: ").append(hex).append("; }\n");
            css.append(".font-size-slider .thumb {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.5), 8, 0, 0, 2);\n}\n");
            css.append(".font-size-slider .thumb:hover { -fx-background-color: ").append(lHex).append("; }\n");
            css.append(".font-size-slider .track { -fx-background-color: rgba(").append(rgb).append(", 0.25); }\n");
            css.append(".font-family-btn.font-active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".font-family-btn.font-active .font-family-name { -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".toggle-icon {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(lHex).append(", ").append(hex).append(");\n}\n");
            css.append(".gender-card:selected {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.25), 12, 0, 0, 0);\n}\n");

            // ── Board / Kanban ──
            css.append(".board-accent-progress { -fx-background-color: ").append(hex).append("; }\n");
            css.append(".board-column-drop-target { -fx-border-color: ").append(hex).append("; }\n");
            css.append(".panel-switch-button .ikonli-font-icon { -fx-icon-color: ").append(lHex).append("; }\n");

            // ── Chat / AI ──
            css.append(".chat-bubble-user {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(hex).append(");\n}\n");
            css.append(".chat-send-btn {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(dHex).append(", ").append(hex).append(");\n}\n");
            css.append(".chat-send-btn:hover {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(hex).append(", ").append(lHex).append(");\n}\n");
            css.append(".ai-header-icon {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(hex).append(");\n}\n");

            // ── Admin elements ──
            css.append(".admin-export-btn {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(dHex).append(");\n}\n");
            css.append(".admin-export-btn:hover {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(lHex).append(", ").append(hex).append(");\n}\n");
            css.append(".admin-user-avatar {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(hex).append(");\n}\n");
            css.append(".admin-checkbox:selected .box {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".admin-input:focused, .admin-textarea:focused { -fx-border-color: ").append(hex).append("; }\n");

            // ── CRUD / form elements ──
            css.append(".crud-item-selected { -fx-border-color: ").append(hex).append("; }\n");
            css.append(".detail-row-icon { -fx-icon-color: ").append(lHex).append("; }\n");
            css.append(".form-date:focused, .form-textarea:focused { -fx-border-color: ").append(hex).append("; }\n");

            // ── PDF export button ──
            css.append(".pdf-export-button {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".pdf-export-button:hover {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(dHex).append(");\n}\n");

            // ── Avatar picker card ──
            css.append(".av-card-selected { -fx-border-color: ").append(hex).append("; }\n");
            css.append(".av-card-check { -fx-background-color: ").append(hex).append("; }\n");

            // ── Wizard steps ──
            css.append(".wizard-step-circle.wizard-step-active {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".wizard-step-label-active { -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".wizard-avatar-mini {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");

            // ── Marketplace ──
            css.append(".mrkt-h1-purple { -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".mrkt-ic, .mrkt-ic-purple {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(dHex).append(", ").append(hex).append(");\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.45), 10, 0, 0, 0);\n}\n");
            css.append(".mrkt-sn { -fx-text-fill: ").append(lHex).append("; }\n");

            // ── Planning / calendar ──
            css.append(".planning-ai-badge {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(hex).append(", ").append(dHex).append(");\n}\n");
            css.append(".planning-calendar-cell-today { -fx-border-color: ").append(hex).append("; }\n");
            css.append(".planning-calendar-day-label-today {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".planning-date-picker:focused, .planning-color-picker:focused { -fx-border-color: ").append(hex).append("; }\n");

            // ── Stress / pet ──
            css.append(".stress-drag-target { -fx-border-color: ").append(hex).append("; }\n");
            css.append(".pet-choice-card.selected { -fx-border-color: ").append(hex).append("; }\n");

            // ── Theme toggle hover ──
            css.append(".theme-toggle-btn:hover .ikonli-font-icon { -fx-icon-color: ").append(lHex).append("; }\n");

            // ── Scroll bar thumb tint ──
            css.append(".scroll-bar .thumb {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.3);\n}\n");
            css.append(".scroll-bar .thumb:hover {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.5);\n}\n");

            // ── Landing page elements ──
            css.append(".landing-cta-primary, .landing-cta-primary-sm {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".landing-cta-primary:hover, .landing-cta-primary-sm:hover {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(lHex).append(", ").append(hex).append(");\n}\n");
            css.append(".landing-cta-ghost:hover { -fx-border-color: ").append(hex).append("; -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".landing-cta-ghost:hover .ikonli-font-icon { -fx-icon-color: ").append(lHex).append("; }\n");
            css.append(".landing-pill-text { -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".landing-nav-signin:hover { -fx-border-color: ").append(hex).append("; -fx-text-fill: ").append(lHex).append("; }\n");
            css.append(".landing-sb-logo, .landing-sb-avatar, .landing-mock-avatar, .landing-testi-avatar {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".landing-step-num {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", ").append(ddHex).append(");\n}\n");
            css.append(".landing-bar-fill {\n")
               .append("    -fx-background-color: linear-gradient(to right, ").append(dHex).append(", ").append(lHex).append(");\n}\n");
            css.append(".landing-tdot-purple { -fx-background-color: ").append(hex).append("; }\n");
            css.append(".landing-ai-text { -fx-text-fill: ").append(lHex).append("; }\n");

            // ── New accessibility buttons ──
            css.append(".radius-style-btn.radius-active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".cursor-btn.cursor-active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.3), 8, 0, 0, 0);\n}\n");
        }

        applyDynamicStylesheet(css.toString());
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    /** Blend a base color with an accent color by the given amount (0..1) */
    private Color tint(Color base, Color accent, double amount) {
        double r = base.getRed() * (1 - amount) + accent.getRed() * amount;
        double g = base.getGreen() * (1 - amount) + accent.getGreen() * amount;
        double b = base.getBlue() * (1 - amount) + accent.getBlue() * amount;
        return Color.color(Math.min(r, 1), Math.min(g, 1), Math.min(b, 1));
    }

    private void applyDynamicStylesheet(String css) {
        try {
            if (dynamicCssPath != null) {
                App.getScene().getStylesheets().removeIf(s -> s.contains("studyflow-dynamic"));
            }
            File tmpFile = File.createTempFile("studyflow-dynamic", ".css");
            tmpFile.deleteOnExit();
            dynamicCssPath = tmpFile.toURI().toString();
            try (FileWriter fw = new FileWriter(tmpFile)) {
                fw.write(css);
            }
            App.getScene().getStylesheets().add(dynamicCssPath);
        } catch (IOException e) {
            System.err.println("Failed to apply dynamic CSS: " + e.getMessage());
        }
    }

    // ============================================
    // ACCENT COLOR
    // ============================================

    private void applyAccentColor(String hex, String name) {
        currentAccentHex = hex;
        accentColorLabel.setText(name);
        if (hex != null) {
            accentColorPicker.setValue(Color.web(hex));
        }
        rebuildDynamicCss();
    }

    private void markAccentActive(Button btn) {
        if (activeAccentBtn != null) {
            activeAccentBtn.getStyleClass().remove("accent-active");
        }
        activeAccentBtn = btn;
        btn.getStyleClass().add("accent-active");
    }

    @FXML
    private void setAccentDefault() {
        markAccentActive(btnDefault);
        applyAccentColor(null, "Default");
        accentColorPicker.setValue(Color.web("#8B5CF6"));
    }
    @FXML
    private void setAccentViolet() { markAccentActive(btnViolet); applyAccentColor("#8B5CF6", "Violet"); }
    @FXML
    private void setAccentIndigo() { markAccentActive(btnIndigo); applyAccentColor("#6366F1", "Indigo"); }
    @FXML
    private void setAccentBlue() { markAccentActive(btnBlue); applyAccentColor("#3B82F6", "Blue"); }
    @FXML
    private void setAccentCyan() { markAccentActive(btnCyan); applyAccentColor("#06B6D4", "Cyan"); }
    @FXML
    private void setAccentGreen() { markAccentActive(btnGreen); applyAccentColor("#10B981", "Green"); }
    @FXML
    private void setAccentOrange() { markAccentActive(btnOrange); applyAccentColor("#F97316", "Orange"); }
    @FXML
    private void setAccentPink() { markAccentActive(btnPink); applyAccentColor("#EC4899", "Pink"); }
    @FXML
    private void setAccentRed() { markAccentActive(btnRed); applyAccentColor("#EF4444", "Red"); }

    @FXML
    private void changeAccentColor() {
        Color c = accentColorPicker.getValue();
        String hex = String.format("#%02X%02X%02X",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
        if (activeAccentBtn != null) activeAccentBtn.getStyleClass().remove("accent-active");
        activeAccentBtn = null;
        accentColorLabel.setText("Custom");
        currentAccentHex = hex;
        rebuildDynamicCss();
    }

    // ============================================
    // FONT FAMILY
    // ============================================

    private void markFontActive(Button btn) {
        if (activeFontBtn != null) {
            activeFontBtn.getStyleClass().remove("font-active");
        }
        activeFontBtn = btn;
        btn.getStyleClass().add("font-active");
    }

    private void applyFontFamily(String family, String displayName, Button btn) {
        markFontActive(btn);
        currentFontFamily = family;
        fontFamilyLabel.setText(displayName);
        rebuildDynamicCss();
    }

    @FXML
    private void setFontSegoe() { applyFontFamily(null, "Segoe UI", fontSegoe); currentFontFamily = null; rebuildDynamicCss(); }
    @FXML
    private void setFontInter() { applyFontFamily("Inter", "Inter", fontInter); }
    @FXML
    private void setFontJetbrains() { applyFontFamily("JetBrains Mono", "JetBrains", fontJetbrains); }
    @FXML
    private void setFontGeorgia() { applyFontFamily("Georgia", "Georgia", fontGeorgia); }
    @FXML
    private void setFontCascadia() { applyFontFamily("Cascadia Code", "Cascadia", fontCascadia); }

    // ============================================
    // BORDER RADIUS
    // ============================================

    private void markRadiusActive(Button btn) {
        if (activeRadiusBtn != null) activeRadiusBtn.getStyleClass().remove("radius-active");
        activeRadiusBtn = btn;
        btn.getStyleClass().add("radius-active");
    }

    @FXML private void setRadiusSharp() { markRadiusActive(radiusSharp); currentBorderRadius = "sharp"; borderRadiusLabel.setText("Sharp"); rebuildDynamicCss(); }
    @FXML private void setRadiusSoft() { markRadiusActive(radiusSoft); currentBorderRadius = "soft"; borderRadiusLabel.setText("Soft"); rebuildDynamicCss(); }
    @FXML private void setRadiusRound() { markRadiusActive(radiusRound); currentBorderRadius = "round"; borderRadiusLabel.setText("Round"); rebuildDynamicCss(); }
    @FXML private void setRadiusPill() { markRadiusActive(radiusPill); currentBorderRadius = "pill"; borderRadiusLabel.setText("Pill"); rebuildDynamicCss(); }

    // ============================================
    // BACKGROUND PATTERN
    // ============================================

    // ============================================
    // CUSTOM CURSORS
    // ============================================

    private void markCursorActive(Button btn) {
        if (activeCursorBtn != null) activeCursorBtn.getStyleClass().remove("cursor-active");
        activeCursorBtn = btn;
        btn.getStyleClass().add("cursor-active");
    }

    private void applyCursor(String style, Button btn) {
        markCursorActive(btn);
        currentCursorStyle = style;
        javafx.scene.Scene scene = App.getPrimaryStage().getScene();
        if ("default".equals(style)) {
            scene.setCursor(javafx.scene.Cursor.DEFAULT);
            return;
        }
        // Generate custom cursor from canvas with transparent background
        javafx.scene.canvas.Canvas cursorCanvas = new javafx.scene.canvas.Canvas(32, 32);
        javafx.scene.canvas.GraphicsContext gc = cursorCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 32, 32); // ensure transparent

        switch (style) {
            case "pen":
                drawPenCursor(gc);
                break;
            case "star":
                drawStarCursor(gc);
                break;
            case "crosshair":
                scene.setCursor(javafx.scene.Cursor.CROSSHAIR);
                return;
            case "heart":
                drawHeartCursor(gc);
                break;
            case "diamond":
                drawDiamondCursor(gc);
                break;
            case "circle":
                drawCircleCursor(gc);
                break;
            case "arrow-fancy":
                drawArrowFancyCursor(gc);
                break;
            case "panda":
                javafx.scene.image.Image pandaImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/com/studyflow/images/panda-cursor.png"), 32, 32, true, true);
                scene.setCursor(new javafx.scene.ImageCursor(pandaImg, 16, 16));
                return;
        }
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        javafx.scene.image.WritableImage img = cursorCanvas.snapshot(sp, null);
        scene.setCursor(new javafx.scene.ImageCursor(img, style.equals("pen") ? 2 : 16, style.equals("pen") ? 30 : 16));
    }

    private void drawPenCursor(javafx.scene.canvas.GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        // Pen body
        gc.setLineWidth(2);
        gc.setStroke(Color.web("#E2E8F0"));
        gc.beginPath();
        gc.moveTo(4, 28); gc.lineTo(8, 18); gc.lineTo(22, 4); gc.lineTo(26, 8); gc.lineTo(12, 22); gc.closePath();
        gc.fill(); gc.stroke();
        // Pen tip
        gc.setFill(Color.web("#8B5CF6"));
        gc.beginPath();
        gc.moveTo(4, 28); gc.lineTo(8, 18); gc.lineTo(12, 22); gc.closePath();
        gc.fill();
        // Pen top accent
        gc.setStroke(Color.web("#8B5CF6"));
        gc.setLineWidth(1.5);
        gc.strokeLine(20, 6, 24, 10);
    }

    private void drawStarCursor(javafx.scene.canvas.GraphicsContext gc) {
        gc.setFill(Color.web("#FBBF24"));
        gc.setStroke(Color.web("#F59E0B"));
        gc.setLineWidth(1.2);
        double cx = 16, cy = 16, outerR = 13, innerR = 6;
        double[] xp = new double[10];
        double[] yp = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            double r = (i % 2 == 0) ? outerR : innerR;
            xp[i] = cx + r * Math.cos(angle);
            yp[i] = cy - r * Math.sin(angle);
        }
        gc.fillPolygon(xp, yp, 10);
        gc.strokePolygon(xp, yp, 10);
    }

    private void drawHeartCursor(javafx.scene.canvas.GraphicsContext gc) {
        gc.setFill(Color.web("#EC4899"));
        gc.setStroke(Color.web("#DB2777"));
        gc.setLineWidth(1.2);
        gc.beginPath();
        gc.moveTo(16, 28);
        gc.bezierCurveTo(8, 22, 2, 16, 2, 10);
        gc.bezierCurveTo(2, 5, 6, 2, 10, 2);
        gc.bezierCurveTo(13, 2, 15, 4, 16, 6);
        gc.bezierCurveTo(17, 4, 19, 2, 22, 2);
        gc.bezierCurveTo(26, 2, 30, 5, 30, 10);
        gc.bezierCurveTo(30, 16, 24, 22, 16, 28);
        gc.closePath();
        gc.fill(); gc.stroke();
    }

    private void drawDiamondCursor(javafx.scene.canvas.GraphicsContext gc) {
        gc.setFill(Color.web("#06B6D4"));
        gc.setStroke(Color.web("#0891B2"));
        gc.setLineWidth(1.5);
        double[] xp = {16, 28, 16, 4};
        double[] yp = {2, 16, 30, 16};
        gc.fillPolygon(xp, yp, 4);
        gc.strokePolygon(xp, yp, 4);
        // Inner shine
        gc.setFill(Color.color(1, 1, 1, 0.3));
        double[] ix = {16, 22, 16, 10};
        double[] iy = {6, 16, 20, 16};
        gc.fillPolygon(ix, iy, 4);
    }

    private void drawCircleCursor(javafx.scene.canvas.GraphicsContext gc) {
        gc.setFill(Color.TRANSPARENT);
        gc.setStroke(Color.web("#8B5CF6"));
        gc.setLineWidth(2.5);
        gc.strokeOval(4, 4, 24, 24);
        // Center dot
        gc.setFill(Color.web("#8B5CF6"));
        gc.fillOval(14, 14, 4, 4);
    }

    private void drawArrowFancyCursor(javafx.scene.canvas.GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.web("#8B5CF6"));
        gc.setLineWidth(1.5);
        // Arrow shape
        gc.beginPath();
        gc.moveTo(4, 2); gc.lineTo(4, 26); gc.lineTo(10, 20); gc.lineTo(16, 28); gc.lineTo(20, 24);
        gc.lineTo(14, 16); gc.lineTo(22, 16); gc.closePath();
        gc.fill(); gc.stroke();
    }


    @FXML private void setCursorDefault() { applyCursor("default", cursorDefault); }
    @FXML private void setCursorPen() { applyCursor("pen", cursorPen); }
    @FXML private void setCursorStar() { applyCursor("star", cursorStar); }
    @FXML private void setCursorCrosshair() { applyCursor("crosshair", cursorCrosshair); }
    @FXML private void setCursorHeart() { applyCursor("heart", cursorHeart); }
    @FXML private void setCursorDiamond() { applyCursor("diamond", cursorDiamond); }
    @FXML private void setCursorCircle() { applyCursor("circle", cursorCircle); }
    @FXML private void setCursorArrowFancy() { applyCursor("arrow-fancy", cursorArrowFancy); }
    @FXML private void setCursorPanda() { applyCursor("panda", cursorPanda); }

    // ============================================
    // AVATAR 3D
    // ============================================

    private void loadAvatar3D(String avatarKey) {
        String avatarPath = "/com/studyflow/avatars/" + avatarKey + ".glb";
        if (GlbLoader.class.getResourceAsStream(avatarPath) == null) return;

        Thread loader = new Thread(() -> {
            try {
                Group avatarModel = GlbLoader.loadFromResource(avatarPath);
                javafx.application.Platform.runLater(() -> renderProfileAvatarScene(avatarModel));
            } catch (Exception e) {
                System.err.println("Profile avatar load failed: " + e.getMessage());
            }
        }, "profile-avatar-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void renderProfileAvatarScene(Group avatarModel) {
        double sceneSize = 134;
        Group modelGroup = new Group();
        Group world = new Group();
        world.getChildren().add(modelGroup);

        AmbientLight ambient = new AmbientLight(Color.color(0.5, 0.47, 0.55));
        PointLight key = new PointLight(Color.color(0.9, 0.85, 0.95));
        key.setTranslateX(3); key.setTranslateY(-4); key.setTranslateZ(5);
        PointLight fill = new PointLight(Color.color(0.4, 0.38, 0.65));
        fill.setTranslateX(-3); fill.setTranslateY(2); fill.setTranslateZ(-3);
        world.getChildren().addAll(ambient, key, fill);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(38);
        camera.setNearClip(0.01);
        camera.setFarClip(200);
        camera.setTranslateZ(-3.2);

        SubScene sub = new SubScene(world, sceneSize, sceneSize, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);

        Group avatarHolder = new Group();
        avatarHolder.getChildren().add(avatarModel);
        fitModelToTarget(avatarModel, 1.8);
        modelGroup.getChildren().add(avatarHolder);

        Rotate rot = new Rotate(0, Rotate.Y_AXIS);
        modelGroup.getTransforms().add(rot);
        if (avatarRotator != null) avatarRotator.stop();
        avatarRotator = new AnimationTimer() {
            private long last = 0;
            @Override public void handle(long now) {
                if (last != 0) rot.setAngle(rot.getAngle() + (now - last) * 1e-9 * 35.0);
                last = now;
            }
        };
        avatarRotator.start();

        avatarInitials.setVisible(false);
        avatarInitials.setManaged(false);
        avatarContainer.getChildren().clear();
        StackPane innerBg = new StackPane();
        innerBg.getStyleClass().add("profile-avatar-inner");
        innerBg.getChildren().add(sub);
        avatarContainer.getChildren().add(innerBg);

        sub.setOpacity(0);
        sub.setScaleX(0.7);
        sub.setScaleY(0.7);
        FadeTransition fade = new FadeTransition(Duration.millis(600), sub);
        fade.setFromValue(0); fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(600), sub);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);
        fade.play();
        scale.play();
    }

    private void fitModelToTarget(Group loaded, double targetSize) {
        Bounds b = loaded.getBoundsInLocal();
        if (b.isEmpty() || b.getWidth() <= 1e-6) {
            return;
        }
        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
        double maxDim = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
        double fit = targetSize / maxDim;
        loaded.getTransforms().clear();
        loaded.getTransforms().add(new Scale(fit, fit, fit));
        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
    }

    // ============================================
    // POPULATE & SAVE
    // ============================================

    private void populateFields(User user) {
        String pic = user.getProfilePic();
        currentAvatar = pic; // Store current avatar for picker
        if (pic != null && !pic.isEmpty() && pic.contains("avatar")) {
            loadAvatar3D(pic);
        } else {
            avatarInitials.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        }
        profileName.setText(user.getFullName().trim());
        profileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        profileUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : ""));
        coinsLabel.setText(String.valueOf(user.getCoins()));
        
        // Display bio in header
        String userBio = user.getBio();
        if (userBio != null && !userBio.isEmpty()) {
            profileBio.setText(userBio);
            profileBio.setVisible(true);
            profileBio.setManaged(true);
        }

        firstNameField.setText(orEmpty(user.getFirstName()));
        lastNameField.setText(orEmpty(user.getLastName()));
        usernameField.setText(orEmpty(user.getUsername()));
        phoneField.setText(orEmpty(user.getPhoneNumber()));
        emailField.setText(orEmpty(user.getEmail()));
        bioField.setText(orEmpty(user.getBio()));
        universityField.setText(orEmpty(user.getUniversity()));
        studentIdField.setText(orEmpty(user.getStudentId()));

        if ("female".equalsIgnoreCase(user.getGender())) {
            genderFemaleBtn.setSelected(true);
        } else {
            genderMaleBtn.setSelected(true);
        }
    }

    @FXML
    private void handleSave() {
        if (currentUser == null) return;

        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!newPass.isEmpty()) {
            if (newPass.length() < 6) {
                showStatus("Password must be at least 6 characters.", false);
                return;
            }
            if (!newPass.equals(confirmPass)) {
                showStatus("Passwords do not match.", false);
                return;
            }
            currentUser.setPassword(newPass);
        }

        currentUser.setFirstName(firstNameField.getText().trim());
        currentUser.setLastName(lastNameField.getText().trim());
        currentUser.setUsername(usernameField.getText().trim());
        currentUser.setPhoneNumber(phoneField.getText().trim());
        currentUser.setBio(bioField.getText().trim());
        currentUser.setUniversity(universityField.getText().trim());
        currentUser.setStudentId(studentIdField.getText().trim());
        currentUser.setGender(genderMaleBtn.isSelected() ? "male" : "female");
        
        // Save avatar if changed
        if (selectedAvatar != null && !selectedAvatar.isEmpty()) {
            currentUser.setProfilePic(selectedAvatar);
            currentAvatar = selectedAvatar;
        }

        serviceUser.update(currentUser);

        profileName.setText(currentUser.getFullName().trim());
        profileUsername.setText("@" + currentUser.getUsername());
        
        // Update bio display
        String userBio = currentUser.getBio();
        if (userBio != null && !userBio.isEmpty()) {
            profileBio.setText(userBio);
            profileBio.setVisible(true);
            profileBio.setManaged(true);
        } else {
            profileBio.setVisible(false);
            profileBio.setManaged(false);
        }

        newPasswordField.clear();
        confirmPasswordField.clear();

        showStatus("Changes saved successfully!", true);
        
        // Reset selected avatar after saving
        selectedAvatar = null;
        if (currentAvatar != null && !currentAvatar.isEmpty() && currentAvatar.contains("avatar")) {
            loadAvatar3D(currentAvatar);
        }
    }

    @FXML
    private void handleDeleteAccount() {
        if (currentUser == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Are you sure you want to delete your account?");
        confirm.setContentText("This action cannot be undone. All your data will be permanently removed.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.delete(currentUser);
            UserSession.getInstance().logout();
            try {
                App.setRoot("views/Login");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String msg, boolean success) {
        // Update top badge
        saveStatus.setText(msg);
        saveStatus.getStyleClass().removeAll("success", "danger");
        saveStatus.getStyleClass().add(success ? "success" : "danger");
        saveStatus.setVisible(true);
        saveStatus.setManaged(true);
        
        // Update bottom label
        saveStatusBottom.setText(msg);
        saveStatusBottom.getStyleClass().removeAll("success", "danger");
        saveStatusBottom.getStyleClass().add(success ? "success" : "danger");
        saveStatusBottom.setVisible(true);
        saveStatusBottom.setManaged(true);
        
        // Auto-hide after 3 seconds
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    saveStatus.setVisible(false);
                    saveStatus.setManaged(false);
                    saveStatusBottom.setVisible(false);
                    saveStatusBottom.setManaged(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }


    // ============================================
    // AVATAR PICKER
    // ============================================

    private static final String[] AVATAR_KEYS = {
        "male-avatar", "male-avatar1", "male-avatar2", "male-avatar3",
        "male-avatar4", "male-avatar5", "male-avatar6", "male-avatar7",
        "female-avatar", "female-avatar2", "female-avatar3"
    };
    private static final String[] AVATAR_NAMES = {
        "Student", "Athlete", "Artist", "Scientist", "Creative", "Medic", "Classic", "Tech Lady", "Scholar", "Leader", "Gamer"
    };

    @FXML
    private void showAvatarPicker() {
        // Load avatar cards one by one with delay to avoid memory issues
        if (profileAvatarContainer.getChildren().isEmpty()) {
            loadAvatarCardsWithDelay();
        }
        
        // Show overlay
        avatarPickerOverlay.setVisible(true);
        avatarPickerOverlay.setManaged(true);
    }
    
    private void loadAvatarCardsWithDelay() {
        // Load first few avatars immediately
        int count = Math.min(4, AVATAR_KEYS.length);
        for (int i = 0; i < count; i++) {
            addAvatarCard(i);
        }
        
        // Load remaining after a delay
        if (AVATAR_KEYS.length > count) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    javafx.application.Platform.runLater(() -> {
                        for (int i = count; i < AVATAR_KEYS.length; i++) {
                            addAvatarCard(i);
                        }
                    });
                } catch (InterruptedException e) {}
            }).start();
        }
    }
    
    private void addAvatarCard(int i) {
        final String key = AVATAR_KEYS[i];
        final String name = AVATAR_NAMES[i];
        final String gen = key.startsWith("female") ? "female" : "male";
        
        AvatarCard card = new AvatarCard(key, name, gen, () -> {
            javafx.application.Platform.runLater(() -> onProfileAvatarSelected(key));
        });
        avatarCards.add(card);
        profileAvatarContainer.getChildren().add(card);
    }

    @FXML
    private void hideAvatarPicker() {
        avatarPickerOverlay.setVisible(false);
        avatarPickerOverlay.setManaged(false);
    }

    private void onProfileAvatarSelected(String avatarKey) {
        selectedAvatar = avatarKey;
        currentAvatar = avatarKey;
        
        // Update UI to show selected
        for (AvatarCard c : avatarCards) {
            c.setSelected(c.getAvatarKey().equals(avatarKey));
        }
        
        // Immediately load new avatar (direct change, no transition delay)
        avatarContainer.getChildren().clear();
        loadAvatar3D(avatarKey);
        
        // Hide picker after selection
        hideAvatarPicker();
    }
}
