package com.studyflow.presentation;

import com.studyflow.models.SlideContent;
import com.studyflow.services.ClaudeService;
import com.studyflow.services.PresentationExporter;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * PresentationStudioView — Canva-like presentation editor.
 *
 * Layout:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  TOP NAVBAR  (logo · topic · theme pills · generate · export)        │
 * ├──────┬──────────────────────────────────────────────┬────────────────┤
 * │ LEFT │  TOOLBAR (text/shape/image/icon tools)       │  RIGHT PANEL   │
 * │SLIDE │  ──────────────────────────────────────────  │  (properties   │
 * │STRIP │  CANVAS (drag-drop interactive elements)     │   + layers)    │
 * │      │                                              │                │
 * │      │  ──────────────────────────────────────────  │                │
 * │      │  SLIDE NAV + INDICATOR                       │                │
 * └──────┴──────────────────────────────────────────────┴────────────────┘
 */
public class PresentationStudioView extends BorderPane {

    // ── Palette ──────────────────────────────────────────────────────
    private static final String BG          = "#0D1117";
    private static final String SURFACE     = "#161B22";
    private static final String CARD        = "#1C2333";
    private static final String BORDER      = "rgba(127,119,221,0.22)";
    private static final String ACCENT      = "#7F77DD";
    private static final String ACCENT_HOT  = "#A78BFA";
    private static final String ACCENT_DIM  = "rgba(127,119,221,0.15)";
    private static final String TEXT_PRI    = "#F0F6FC";
    private static final String TEXT_SEC    = "#8B949E";
    private static final String TEXT_MUT    = "#484F58";
    private static final String SUCCESS     = "#3FB950";
    private static final String DANGER      = "#F85149";
    private static final String WARNING     = "#F59E0B";

    private static final double CANVAS_W = 960;
    private static final double CANVAS_H = 540;

    private static final List<String> LOADING_MSGS = List.of(
            "Analysing your topic…",
            "Structuring the narrative…",
            "Generating content with AI…",
            "Applying visual theme…",
            "Finalising slides…"
    );

    // ── Services ─────────────────────────────────────────────────────
    private final ClaudeService claudeService = new ClaudeService();
    private final PresentationExporter exporter = new PresentationExporter();

    // ── State ─────────────────────────────────────────────────────────
    private final ObservableList<SlideContent> slides = FXCollections.observableArrayList();
    private int activeSlideIndex = 0;
    private javafx.concurrent.Task<List<SlideContent>> currentTask;
    private Timeline loadingTimeline;
    private int loadingMsgIdx = 0;

    // ── Canvas element state ─────────────────────────────────────────
    /** Each slide has a list of draggable CanvasElement objects */
    private final Map<Integer, List<CanvasElement>> slideElements = new HashMap<>();
    private CanvasElement selectedElement = null;
    private String currentTool = "select"; // select, text, rect, circle, line, image

    // ── Top-bar controls ─────────────────────────────────────────────
    private final TextField topicField         = new TextField();
    private final ToggleGroup themeGroup       = new ToggleGroup();
    private final ComboBox<String> langCombo   = new ComboBox<>();
    private final ComboBox<String> audCombo    = new ComboBox<>();
    private final Slider slideCountSlider      = new Slider(4, 20, 8);
    private final Label slideCountLabel        = new Label("8");
    private final Button generateBtn           = new Button();
    private final Button exportBtn             = new Button();
    private final Button previewBtn            = new Button();
    private final Label statusLabel            = new Label("Ready");
    private final ProgressBar progressBar      = new ProgressBar(0);

    // ── Left slide strip ─────────────────────────────────────────────
    private final VBox slideStrip = new VBox(8);
    private final ScrollPane stripScroll = new ScrollPane();

    // ── Canvas ───────────────────────────────────────────────────────
    private final Pane canvasPane = new Pane();
    private final Label slideIndicatorLabel = new Label();

    // ── Right properties panel ────────────────────────────────────────
    private final VBox rightPanel = new VBox(0);
    private final TextField propTitle   = new TextField();
    private final TextArea  propBullets = new TextArea();
    private final TextArea  propNotes   = new TextArea();
    private final ComboBox<String> propType = new ComboBox<>();
    private final Label propSlideNum = new Label();

    // ── Element property controls ─────────────────────────────────────
    private final VBox elementPropsPanel = new VBox(0);
    private final TextField elemXField = new TextField();
    private final TextField elemYField = new TextField();
    private final TextField elemWField = new TextField();
    private final TextField elemHField = new TextField();
    private final ColorPicker elemFillPicker = new ColorPicker(Color.web(ACCENT));
    private final ColorPicker elemStrokePicker = new ColorPicker(Color.TRANSPARENT);
    private final Slider elemOpacitySlider = new Slider(0, 1, 1);
    private final ComboBox<String> elemFontSize = new ComboBox<>();
    private final CheckBox elemBoldCheck = new CheckBox("Bold");
    private final TextField elemTextField = new TextField();
    private final VBox layersPanel = new VBox(4);

    // ── Tool toggle group ─────────────────────────────────────────────
    private final ToggleGroup toolGroup = new ToggleGroup();

    // ────────────────────────────────────────────────────────────────────
    public PresentationStudioView() {
        setStyle("-fx-background-color:" + BG + ";");
        buildUi();
        attachListeners();
        refreshCanvas();
    }

    public static void show(Window owner) {
        PresentationStudioView root = new PresentationStudioView();

        javafx.geometry.Rectangle2D screen =
                javafx.stage.Screen.getPrimary().getVisualBounds();

        double initW = Math.min(screen.getWidth()  * 0.92, 1600);
        double initH = Math.min(screen.getHeight() * 0.92, 960);
        double minW  = Math.min(screen.getWidth(),  960);
        double minH  = Math.min(screen.getHeight(), 640);

        Scene scene = new Scene(root, initW, initH);

        URL css = PresentationStudioView.class.getResource("/com/studyflow/styles/dark-theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        // Global keyboard shortcuts
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                root.deleteSelectedElement();
            }
            if (e.isControlDown() && e.getCode() == KeyCode.D) {
                root.duplicateSelectedElement();
                e.consume();
            }
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                // Undo placeholder
                e.consume();
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                root.deselectAll();
            }
        });

        Stage stage = new Stage(StageStyle.DECORATED);
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        }
        stage.setTitle("StudyFlow • Presentation Studio");
        stage.setScene(scene);
        stage.setMinWidth(minW);
        stage.setMinHeight(minH);

        stage.setX(screen.getMinX() + (screen.getWidth()  - initW) / 2.0);
        stage.setY(screen.getMinY() + (screen.getHeight() - initH) / 2.0);

        if (screen.getWidth() < 1400 || screen.getHeight() < 860) {
            stage.setMaximized(true);
        }

        stage.show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ══════════════════════════════════════════════════════════════════

    private void buildUi() {
        setTop(buildTopBar());
        setLeft(buildLeftStrip());

        VBox centerArea = buildCenterArea();
        BorderPane.setAlignment(centerArea, Pos.TOP_LEFT);
        setCenter(centerArea);

        setRight(buildRightPanel());
    }

    // ── TOP BAR ──────────────────────────────────────────────────────

    private VBox buildTopBar() {
        // Main top bar
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 18, 0, 0));
        bar.setPrefHeight(56);
        bar.setMinHeight(56);
        bar.setStyle("-fx-background-color:" + SURFACE + ";"
                + "-fx-border-color:" + ACCENT_DIM + ";-fx-border-width:0 0 1 0;");

        HBox logo = buildLogo();
        Region d1 = divider();
        HBox topicBox = buildTopicBox();
        Region d2 = divider();
        HBox themePills = buildThemePills();
        Region d3 = divider();
        HBox optionsRow = buildOptionsRow();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = buildTopActions();

        bar.getChildren().addAll(logo, d1, topicBox, d2, themePills, d3, optionsRow, spacer, actions);

        VBox topSection = new VBox(0, bar);
        return topSection;
    }

    private HBox buildLogo() {
        Label icon = new Label("⬡");
        icon.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:24px;");
        Label text = new Label("Studio");
        text.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:800;"
                + "-fx-font-family:'Segoe UI';");
        HBox box = new HBox(8, icon, text);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 18, 0, 16));
        box.setMinWidth(120);
        return box;
    }

    private HBox buildTopicBox() {
        topicField.setPromptText("Topic / subject…");
        topicField.setPrefWidth(220);
        topicField.setStyle(inputStyle() + "-fx-padding:7 12;-fx-font-size:13px;");

        HBox chips = new HBox(5);
        chips.setAlignment(Pos.CENTER_LEFT);
        for (String t : new String[]{"Machine Learning", "Climate Change", "Cybersecurity"}) {
            Button chip = chip(t);
            chip.setOnAction(e -> topicField.setText(t));
            chips.getChildren().add(chip);
        }

        VBox inner = new VBox(4, topicField, chips);
        inner.setAlignment(Pos.CENTER_LEFT);
        HBox box = new HBox(inner);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 14, 0, 14));
        return box;
    }

    private HBox buildThemePills() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(0, 10, 0, 10));
        for (String t : new String[]{"Modern", "Minimal", "Bold", "Academic", "Creative"}) {
            ToggleButton tb = new ToggleButton(t);
            tb.setUserData(t);
            tb.setToggleGroup(themeGroup);
            if ("Modern".equals(t)) tb.setSelected(true);
            tb.setStyle(themeToggleStyle(tb.isSelected()));
            tb.selectedProperty().addListener((obs, o, n) -> refreshThemeStyles());
            row.getChildren().add(tb);
        }
        return row;
    }

    private HBox buildOptionsRow() {
        langCombo.setItems(FXCollections.observableArrayList("Français", "English", "Arabic"));
        langCombo.setValue("Français");
        langCombo.setStyle(comboStyle());
        langCombo.setPrefWidth(100);

        audCombo.setItems(FXCollections.observableArrayList("Student", "Professional", "Beginner", "Expert"));
        audCombo.setValue("Student");
        audCombo.setStyle(comboStyle());
        audCombo.setPrefWidth(110);

        slideCountSlider.setMinorTickCount(0);
        slideCountSlider.setMajorTickUnit(1);
        slideCountSlider.setSnapToTicks(true);
        slideCountSlider.setBlockIncrement(1);
        slideCountSlider.setStyle("-fx-accent:" + ACCENT + ";");
        slideCountSlider.setPrefWidth(90);
        slideCountLabel.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:12px;-fx-font-weight:700;");

        HBox countBox = new HBox(6, slideCountSlider, slideCountLabel, label("slides", TEXT_SEC, 11));
        countBox.setAlignment(Pos.CENTER);

        HBox row = new HBox(10,
                label("Lang:", TEXT_SEC, 11), langCombo,
                label("Audience:", TEXT_SEC, 11), audCombo,
                countBox
        );
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(0, 10, 0, 6));
        return row;
    }

    private HBox buildTopActions() {
        generateBtn.setText("✦ Generate");
        generateBtn.setStyle(accentBtnStyle());
        generateBtn.setOnAction(e -> startGeneration(false));

        previewBtn.setGraphic(icon("fth-eye", TEXT_SEC, 13));
        previewBtn.setText("Preview");
        previewBtn.setStyle(ghostBtnStyle());
        previewBtn.setOnAction(e -> showPreviewDialog());
        previewBtn.setDisable(true);

        exportBtn.setText("↓ Export .pptx");
        exportBtn.setStyle(successBtnStyle());
        exportBtn.setOnAction(e -> exportPresentation());
        exportBtn.setDisable(true);

        VBox progressCapsule = new VBox(3);
        progressCapsule.setAlignment(Pos.CENTER);
        progressBar.setPrefWidth(110);
        progressBar.setPrefHeight(4);
        progressBar.setStyle("-fx-accent:" + ACCENT + ";-fx-background-color:#2D333B;");
        statusLabel.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:10px;-fx-font-weight:600;");
        progressCapsule.getChildren().addAll(progressBar, statusLabel);
        progressCapsule.setPadding(new Insets(0, 0, 0, 10));

        HBox box = new HBox(8, progressCapsule, previewBtn, generateBtn, exportBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 8, 0, 0));
        return box;
    }

    // ── LEFT STRIP ────────────────────────────────────────────────────

    private ScrollPane buildLeftStrip() {
        slideStrip.setPadding(new Insets(12, 8, 12, 8));
        slideStrip.setFillWidth(true);

        Button addBtn = new Button("+ Add Slide");
        addBtn.setStyle(ghostBtnStyle() + "-fx-font-size:11px;-fx-padding:8 14;-fx-pref-width:164;");
        addBtn.setOnAction(e -> addEmptySlide());
        slideStrip.getChildren().add(addBtn);

        stripScroll.setContent(slideStrip);
        stripScroll.setFitToWidth(true);
        stripScroll.setPrefWidth(185);
        stripScroll.setMinWidth(185);
        stripScroll.setMaxWidth(185);
        stripScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        stripScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        stripScroll.setStyle("-fx-background-color:" + SURFACE + ";-fx-border-color:" + ACCENT_DIM + ";"
                + "-fx-border-width:0 1 0 0;");
        return stripScroll;
    }

    private VBox buildSlideThumb(SlideContent sc, int idx) {
        boolean active = idx == activeSlideIndex;

        StackPane miniCanvas = new StackPane();
        miniCanvas.setPrefSize(164, 92);
        miniCanvas.setMinSize(164, 92);
        miniCanvas.setMaxSize(164, 92);
        miniCanvas.setStyle("-fx-background-color:" + themeCanvasColor() + ";"
                + "-fx-background-radius:8;-fx-cursor:hand;");

        // Mini accent band
        Rectangle miniBand = new Rectangle(0, 0, 164, 18);
        miniBand.setFill(Color.web(accentFromTheme(), 0.8));
        miniBand.setArcWidth(16);
        miniBand.setArcHeight(16);
        StackPane.setAlignment(miniBand, Pos.TOP_LEFT);

        Label miniTitle = new Label(compact(sc.getTitle(), 28));
        miniTitle.setWrapText(true);
        miniTitle.setMaxWidth(140);
        miniTitle.setStyle("-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:800;"
                + "-fx-text-alignment:center;");
        miniTitle.setAlignment(Pos.CENTER);

        // Bullet dots
        HBox dots = new HBox(3);
        dots.setAlignment(Pos.CENTER);
        int dotCount = Math.min(sc.getBulletPoints().size(), 5);
        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(2.5);
            dot.setFill(Color.web("#FFFFFF", 0.4));
            dots.getChildren().add(dot);
        }

        VBox miniContent = new VBox(4, miniTitle, dots);
        miniContent.setAlignment(Pos.CENTER);
        miniContent.setTranslateY(4);

        miniCanvas.getChildren().addAll(miniBand, miniContent);

        // Slide number + type
        Label badge = new Label(String.valueOf(sc.getSlideNumber()));
        badge.setStyle("-fx-text-fill:" + (active ? ACCENT : TEXT_MUT) + ";"
                + "-fx-font-size:10px;-fx-font-weight:800;");

        Label typeL = new Label(sc.getType().name().toLowerCase());
        typeL.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:8px;-fx-font-weight:700;"
                + "-fx-background-color:rgba(127,119,221,0.12);-fx-background-radius:999;"
                + "-fx-padding:1 5;");

        Button del = new Button("✕");
        del.setStyle("-fx-background-color:transparent;-fx-text-fill:" + DANGER + ";"
                + "-fx-font-size:9px;-fx-cursor:hand;-fx-padding:1 3;");
        del.setOnAction(e -> deleteSlide(idx));

        HBox header = new HBox(3, badge, typeL);
        header.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(3, header, spacer, del);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(5, topRow, miniCanvas);
        card.setPadding(new Insets(8));
        card.setPrefWidth(172);
        card.setStyle(thumbStyle(active));
        card.setCursor(Cursor.HAND);

        card.setOnMouseClicked(e -> {
            saveCurrentSlideElements();
            activeSlideIndex = idx;
            refreshStrip();
            refreshCanvas();
            loadPropertiesPanel(sc);
        });

        // Hover
        card.setOnMouseEntered(e -> {
            if (!active) card.setStyle(thumbStyle(true));
        });
        card.setOnMouseExited(e -> {
            if (idx != activeSlideIndex) card.setStyle(thumbStyle(false));
        });

        // Drag-reorder
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.put(SlidePreviewCard.DRAG_FORMAT, String.valueOf(idx));
            db.setContent(cc);
            card.setOpacity(0.4);
            e.consume();
        });
        card.setOnDragDone(e -> { card.setOpacity(1); e.consume(); });
        card.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(SlidePreviewCard.DRAG_FORMAT)
                    && e.getGestureSource() != card) {
                e.acceptTransferModes(TransferMode.MOVE);
                card.setStyle(thumbStyle(true));
            }
            e.consume();
        });
        card.setOnDragExited(e -> {
            if (e.getGestureSource() != card) card.setStyle(thumbStyle(idx == activeSlideIndex));
            e.consume();
        });
        card.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(SlidePreviewCard.DRAG_FORMAT)) {
                int src = Integer.parseInt(String.valueOf(db.getContent(SlidePreviewCard.DRAG_FORMAT)));
                reorderSlides(src, idx);
            }
            e.setDropCompleted(true);
            e.consume();
        });

        return card;
    }

    // ── CENTER AREA (toolbar + canvas) ────────────────────────────────

    private VBox buildCenterArea() {
        HBox toolbar = buildElementToolbar();

        // Canvas setup
        canvasPane.setPrefSize(CANVAS_W, CANVAS_H);
        canvasPane.setMinSize(CANVAS_W, CANVAS_H);
        canvasPane.setMaxSize(CANVAS_W, CANVAS_H);
        canvasPane.setStyle("-fx-background-color:" + themeCanvasColor()
                + ";-fx-background-radius:14;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.5));
        shadow.setRadius(30);
        shadow.setOffsetY(5);
        canvasPane.setEffect(shadow);

        // Canvas click handler — for adding elements or deselecting
        canvasPane.setOnMouseClicked(e -> {
            if (e.getTarget() == canvasPane) {
                if (!"select".equals(currentTool)) {
                    addElementAtPosition(e.getX(), e.getY());
                } else {
                    deselectAll();
                }
            }
        });

        // Clip canvas
        Rectangle canvasClip = new Rectangle(0, 0, CANVAS_W, CANVAS_H);
        canvasClip.setArcWidth(28);
        canvasClip.setArcHeight(28);
        canvasPane.setClip(canvasClip);

        // Scaling host
        Pane scalingHost = new Pane(canvasPane);
        scalingHost.setStyle("-fx-background-color:" + BG + ";");
        scalingHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Rectangle clip = new Rectangle();
        scalingHost.setClip(clip);

        scalingHost.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            double hw = newB.getWidth();
            double hh = newB.getHeight();
            if (hw <= 0 || hh <= 0) return;

            double pad = 28;
            double availW = hw - pad * 2;
            double availH = hh - pad * 2;
            double scale  = Math.min(availW / CANVAS_W, availH / CANVAS_H);
            scale = Math.max(0.05, scale);

            double scaledW = CANVAS_W * scale;
            double scaledH = CANVAS_H * scale;

            double tx = (hw - scaledW) / 2.0;
            double ty = (hh - scaledH) / 2.0;

            canvasPane.setScaleX(scale);
            canvasPane.setScaleY(scale);
            canvasPane.setLayoutX(tx - (CANVAS_W - scaledW) / 2.0);
            canvasPane.setLayoutY(ty - (CANVAS_H - scaledH) / 2.0);

            clip.setWidth(hw);
            clip.setHeight(hh);
        });

        // Nav arrows
        Button prevArrow = navArrow("‹");
        Button nextArrow = navArrow("›");
        prevArrow.setOnAction(e -> navigateSlide(-1));
        nextArrow.setOnAction(e -> navigateSlide(1));

        StackPane hostStack = new StackPane(scalingHost, prevArrow, nextArrow);
        StackPane.setAlignment(prevArrow, Pos.CENTER_LEFT);
        StackPane.setAlignment(nextArrow, Pos.CENTER_RIGHT);
        StackPane.setMargin(prevArrow, new Insets(0, 0, 0, 8));
        StackPane.setMargin(nextArrow, new Insets(0, 8, 0, 0));
        hostStack.setPickOnBounds(false);
        scalingHost.setPickOnBounds(true);
        VBox.setVgrow(hostStack, Priority.ALWAYS);

        // Slide indicator + quick actions
        slideIndicatorLabel.setStyle("-fx-text-fill:" + TEXT_MUT
                + ";-fx-font-size:11px;-fx-font-weight:700;");

        Button dupSlideBtn = miniActionBtn("⧉ Duplicate Slide");
        dupSlideBtn.setOnAction(e -> duplicateCurrentSlide());
        Button delSlideBtn = miniActionBtn("✕ Delete Slide");
        delSlideBtn.setStyle(miniActionBtnStyle() + "-fx-text-fill:" + DANGER + ";");
        delSlideBtn.setOnAction(e -> deleteSlide(activeSlideIndex));

        Region indSpacer = new Region();
        HBox.setHgrow(indSpacer, Priority.ALWAYS);

        HBox indicatorBar = new HBox(12, slideIndicatorLabel, indSpacer, dupSlideBtn, delSlideBtn);
        indicatorBar.setAlignment(Pos.CENTER);
        indicatorBar.setPadding(new Insets(6, 16, 6, 16));
        indicatorBar.setStyle("-fx-background-color:" + SURFACE + ";"
                + "-fx-border-color:" + ACCENT_DIM + ";-fx-border-width:1 0 0 0;");
        indicatorBar.setMinHeight(32);
        indicatorBar.setMaxHeight(32);

        VBox col = new VBox(0, toolbar, hostStack, indicatorBar);
        col.setStyle("-fx-background-color:" + BG + ";");
        col.setFillWidth(true);
        return col;
    }

    // ── ELEMENT TOOLBAR ──────────────────────────────────────────────

    private HBox buildElementToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color:" + SURFACE + ";"
                + "-fx-border-color:" + ACCENT_DIM + ";-fx-border-width:0 0 1 0;");
        bar.setMinHeight(44);
        bar.setMaxHeight(44);

        // Tool buttons
        ToggleButton selectTool = toolToggle("↖", "Select", "select");
        selectTool.setSelected(true);
        ToggleButton textTool   = toolToggle("T", "Text", "text");
        ToggleButton rectTool   = toolToggle("▢", "Rectangle", "rect");
        ToggleButton circleTool = toolToggle("●", "Circle", "circle");
        ToggleButton lineTool   = toolToggle("╱", "Line", "line");
        ToggleButton imageTool  = toolToggle("🖼", "Image", "image");

        bar.getChildren().addAll(
                label("Tools", TEXT_SEC, 11),
                selectTool, textTool,
                toolbarDivider(),
                label("Shapes", TEXT_SEC, 11),
                rectTool, circleTool, lineTool,
                toolbarDivider(),
                label("Media", TEXT_SEC, 11),
                imageTool
        );

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Quick insert buttons
        Button insertTitle = quickInsertBtn("+ Title");
        insertTitle.setOnAction(e -> insertPresetTitle());
        Button insertSubtitle = quickInsertBtn("+ Subtitle");
        insertSubtitle.setOnAction(e -> insertPresetSubtitle());
        Button insertBody = quickInsertBtn("+ Body Text");
        insertBody.setOnAction(e -> insertPresetBody());
        Button insertBullets = quickInsertBtn("+ Bullet List");
        insertBullets.setOnAction(e -> insertPresetBullets());

        bar.getChildren().addAll(sp,
                toolbarDivider(),
                label("Quick Insert", TEXT_SEC, 11),
                insertTitle, insertSubtitle, insertBody, insertBullets
        );

        return bar;
    }

    private ToggleButton toolToggle(String iconText, String tooltip, String toolId) {
        ToggleButton tb = new ToggleButton(iconText);
        tb.setToggleGroup(toolGroup);
        tb.setUserData(toolId);
        tb.setTooltip(new Tooltip(tooltip));
        tb.setStyle(toolToggleStyle(false));
        tb.selectedProperty().addListener((obs, o, n) -> {
            tb.setStyle(toolToggleStyle(n));
            if (n) {
                currentTool = toolId;
                canvasPane.setCursor("select".equals(toolId) ? Cursor.DEFAULT : Cursor.CROSSHAIR);
            }
        });
        return tb;
    }

    private Button quickInsertBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:rgba(127,119,221,0.1);-fx-text-fill:" + ACCENT_HOT + ";"
                + "-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:8;"
                + "-fx-padding:5 10;-fx-cursor:hand;"
                + "-fx-border-color:rgba(127,119,221,0.2);-fx-border-radius:8;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace("0.1", "0.2")));
        b.setOnMouseExited(e -> b.setStyle(b.getStyle().replace("0.2", "0.1")));
        return b;
    }

    // ── RIGHT PANEL ────────────────────────────────────────────────────

    private ScrollPane buildRightPanel() {
        rightPanel.setSpacing(0);
        rightPanel.setPrefWidth(270);
        rightPanel.setMinWidth(270);
        rightPanel.setMaxWidth(270);
        rightPanel.setStyle("-fx-background-color:" + SURFACE + ";"
                + "-fx-border-color:" + ACCENT_DIM + ";-fx-border-width:0 0 0 1;");
        rightPanel.setPadding(Insets.EMPTY);

        buildRightPanelContent();

        ScrollPane sp = new ScrollPane(rightPanel);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color:" + SURFACE + ";");
        sp.setPrefWidth(270);
        sp.setMinWidth(270);
        sp.setMaxWidth(270);
        return sp;
    }

    private void buildRightPanelContent() {
        rightPanel.getChildren().clear();

        // ── Tab-like header ──
        HBox tabBar = new HBox(0);
        tabBar.setAlignment(Pos.CENTER);
        tabBar.setStyle("-fx-background-color:" + CARD + ";");

        Button slideTab = tabBtn("Slide", true);
        Button elementTab = tabBtn("Element", false);
        Button layersTab = tabBtn("Layers", false);

        slideTab.setOnAction(e -> showSlideProps(slideTab, elementTab, layersTab));
        elementTab.setOnAction(e -> showElementProps(slideTab, elementTab, layersTab));
        layersTab.setOnAction(e -> showLayersPanel(slideTab, elementTab, layersTab));

        tabBar.getChildren().addAll(slideTab, elementTab, layersTab);
        rightPanel.getChildren().add(tabBar);
        rightPanel.getChildren().add(hRule());

        // ── Slide properties section ──
        buildSlidePropsSection();

        // ── Element properties section (hidden by default) ──
        buildElementPropsSection();

        // ── Layers section (hidden by default) ──
        buildLayersSection();
    }

    private void buildSlidePropsSection() {
        VBox slidePropsBox = new VBox(0);
        slidePropsBox.setUserData("slideProps");

        Label header = new Label("Slide Properties");
        header.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:12px;-fx-font-weight:800;"
                + "-fx-padding:14 14 10 14;");
        slidePropsBox.getChildren().add(header);

        // Slide number
        slidePropsBox.getChildren().add(propSection("Slide Number", propSlideNum("—")));

        // Type selector
        propType.setItems(FXCollections.observableArrayList("TITLE", "CONTENT", "DIAGRAM", "SUMMARY", "CONCLUSION"));
        propType.setValue("CONTENT");
        propType.setStyle(comboStyle() + "-fx-pref-width:230;");
        propType.valueProperty().addListener((obs, o, n) -> {
            if (!slides.isEmpty() && n != null) {
                slides.get(activeSlideIndex).setType(SlideContent.SlideType.fromApiValue(n));
                refreshCanvas();
            }
        });
        slidePropsBox.getChildren().add(propSection("Type", propType));

        // Title
        propTitle.setStyle(inputStyle() + "-fx-pref-width:230;-fx-font-size:12px;-fx-padding:8 10;");
        propTitle.setPromptText("Slide title…");
        propTitle.textProperty().addListener((obs, o, n) -> {
            if (!slides.isEmpty()) {
                slides.get(activeSlideIndex).setTitle(n);
                refreshCanvas();
            }
        });
        slidePropsBox.getChildren().add(propSection("Title", propTitle));

        // Bullets
        propBullets.setPromptText("• Bullet point 1\n• Bullet point 2");
        propBullets.setPrefRowCount(6);
        propBullets.setWrapText(true);
        propBullets.setStyle(inputStyle() + "-fx-pref-width:230;-fx-font-size:11px;");
        propBullets.textProperty().addListener((obs, o, n) -> {
            if (!slides.isEmpty()) {
                List<String> bullets = Arrays.stream(n.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .map(s -> s.startsWith("•") ? s.substring(1).trim() : s)
                        .toList();
                slides.get(activeSlideIndex).setBulletPoints(bullets);
                refreshCanvas();
            }
        });
        slidePropsBox.getChildren().add(propSection("Bullet Points", propBullets));

        // Speaker notes
        propNotes.setPromptText("Speaker notes…");
        propNotes.setPrefRowCount(4);
        propNotes.setWrapText(true);
        propNotes.setStyle(inputStyle() + "-fx-pref-width:230;-fx-font-size:11px;");
        propNotes.textProperty().addListener((obs, o, n) -> {
            if (!slides.isEmpty()) {
                slides.get(activeSlideIndex).setSpeakerNotes(n);
            }
        });
        slidePropsBox.getChildren().add(propSection("Speaker Notes", propNotes));

        slidePropsBox.getChildren().add(hRule());

        // Color scheme dots
        Label designHeader = new Label("Accent Color");
        designHeader.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;-fx-font-weight:700;"
                + "-fx-padding:10 14 6 14;");
        slidePropsBox.getChildren().add(designHeader);

        HBox colorRow = new HBox(8);
        colorRow.setPadding(new Insets(0, 14, 12, 14));
        for (String c : new String[]{"#7F77DD", "#EC4899", "#3FB950", "#F59E0B", "#38BDF8", "#F85149", "#8B5CF6"}) {
            Circle dot = new Circle(10);
            dot.setFill(Color.web(c));
            dot.setStroke(Color.web("#FFFFFF", 0.2));
            dot.setStrokeWidth(1.5);
            dot.setCursor(Cursor.HAND);
            dot.setOnMouseClicked(e -> applyAccentOverride(c));
            colorRow.getChildren().add(dot);
        }
        slidePropsBox.getChildren().add(colorRow);

        // Stats
        slidePropsBox.getChildren().add(hRule());
        Label statsContent = new Label("0 slides · 0 bullet points · 0 elements");
        statsContent.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:10px;-fx-padding:8 14;");
        slides.addListener((javafx.collections.ListChangeListener<SlideContent>) c -> {
            int totalBullets = slides.stream().mapToInt(s -> s.getBulletPoints().size()).sum();
            int totalElements = slideElements.values().stream().mapToInt(List::size).sum();
            statsContent.setText(slides.size() + " slides · " + totalBullets + " bullets · " + totalElements + " elements");
        });
        slidePropsBox.getChildren().add(statsContent);

        rightPanel.getChildren().add(slidePropsBox);
    }

    private void buildElementPropsSection() {
        elementPropsPanel.setUserData("elementProps");
        elementPropsPanel.setVisible(false);
        elementPropsPanel.setManaged(false);

        Label header = new Label("Element Properties");
        header.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:12px;-fx-font-weight:800;"
                + "-fx-padding:14 14 10 14;");
        elementPropsPanel.getChildren().add(header);

        // No element selected message
        Label noElem = new Label("Select an element on the canvas\nto edit its properties.");
        noElem.setWrapText(true);
        noElem.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:11px;-fx-padding:14;");
        noElem.setUserData("noElemMsg");
        elementPropsPanel.getChildren().add(noElem);

        // Position
        HBox posRow = new HBox(8);
        posRow.setPadding(new Insets(0, 14, 6, 14));
        elemXField.setPrefWidth(55);
        elemXField.setStyle(smallInputStyle());
        elemXField.setPromptText("X");
        elemYField.setPrefWidth(55);
        elemYField.setStyle(smallInputStyle());
        elemYField.setPromptText("Y");
        posRow.getChildren().addAll(label("X", TEXT_SEC, 10), elemXField, label("Y", TEXT_SEC, 10), elemYField);
        posRow.setAlignment(Pos.CENTER_LEFT);

        VBox posSection = new VBox(4,
                sectionLabel("Position"),
                posRow
        );
        posSection.setUserData("posSection");
        posSection.setVisible(false);
        posSection.setManaged(false);
        elementPropsPanel.getChildren().add(posSection);

        // Size
        HBox sizeRow = new HBox(8);
        sizeRow.setPadding(new Insets(0, 14, 6, 14));
        elemWField.setPrefWidth(55);
        elemWField.setStyle(smallInputStyle());
        elemWField.setPromptText("W");
        elemHField.setPrefWidth(55);
        elemHField.setStyle(smallInputStyle());
        elemHField.setPromptText("H");
        sizeRow.getChildren().addAll(label("W", TEXT_SEC, 10), elemWField, label("H", TEXT_SEC, 10), elemHField);
        sizeRow.setAlignment(Pos.CENTER_LEFT);

        VBox sizeSection = new VBox(4,
                sectionLabel("Size"),
                sizeRow
        );
        sizeSection.setUserData("sizeSection");
        sizeSection.setVisible(false);
        sizeSection.setManaged(false);
        elementPropsPanel.getChildren().add(sizeSection);

        // Fill & Stroke
        HBox colorRow = new HBox(8);
        colorRow.setPadding(new Insets(0, 14, 6, 14));
        colorRow.setAlignment(Pos.CENTER_LEFT);
        elemFillPicker.setPrefWidth(50);
        elemFillPicker.setPrefHeight(26);
        elemStrokePicker.setPrefWidth(50);
        elemStrokePicker.setPrefHeight(26);
        colorRow.getChildren().addAll(
                label("Fill", TEXT_SEC, 10), elemFillPicker,
                label("Stroke", TEXT_SEC, 10), elemStrokePicker
        );

        VBox colorSection = new VBox(4,
                sectionLabel("Colors"),
                colorRow
        );
        colorSection.setUserData("colorSection");
        colorSection.setVisible(false);
        colorSection.setManaged(false);
        elementPropsPanel.getChildren().add(colorSection);

        // Opacity
        elemOpacitySlider.setPrefWidth(140);
        elemOpacitySlider.setStyle("-fx-accent:" + ACCENT + ";");
        HBox opacityRow = new HBox(8, label("Opacity", TEXT_SEC, 10), elemOpacitySlider);
        opacityRow.setPadding(new Insets(0, 14, 6, 14));
        opacityRow.setAlignment(Pos.CENTER_LEFT);

        VBox opacitySection = new VBox(4, opacityRow);
        opacitySection.setUserData("opacitySection");
        opacitySection.setVisible(false);
        opacitySection.setManaged(false);
        elementPropsPanel.getChildren().add(opacitySection);

        // Text properties
        elemFontSize.setItems(FXCollections.observableArrayList(
                "10", "12", "14", "16", "18", "20", "24", "28", "32", "36", "42", "48", "56", "72"));
        elemFontSize.setValue("20");
        elemFontSize.setStyle(comboStyle() + "-fx-pref-width:70;");
        elemBoldCheck.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;");

        HBox textStyleRow = new HBox(8, label("Size", TEXT_SEC, 10), elemFontSize, elemBoldCheck);
        textStyleRow.setPadding(new Insets(0, 14, 6, 14));
        textStyleRow.setAlignment(Pos.CENTER_LEFT);

        elemTextField.setStyle(inputStyle() + "-fx-pref-width:230;-fx-font-size:12px;-fx-padding:8 10;");
        elemTextField.setPromptText("Text content…");
        HBox textContentRow = new HBox(elemTextField);
        textContentRow.setPadding(new Insets(0, 14, 6, 14));

        VBox textSection = new VBox(4,
                sectionLabel("Text"),
                textContentRow,
                textStyleRow
        );
        textSection.setUserData("textSection");
        textSection.setVisible(false);
        textSection.setManaged(false);
        elementPropsPanel.getChildren().add(textSection);

        // Element actions
        HBox actionsRow = new HBox(8);
        actionsRow.setPadding(new Insets(8, 14, 8, 14));
        Button dupElemBtn = miniActionBtn("⧉ Duplicate");
        dupElemBtn.setOnAction(e -> duplicateSelectedElement());
        Button delElemBtn = miniActionBtn("✕ Delete");
        delElemBtn.setStyle(miniActionBtnStyle() + "-fx-text-fill:" + DANGER + ";");
        delElemBtn.setOnAction(e -> deleteSelectedElement());
        Button toFrontBtn = miniActionBtn("↑ Front");
        toFrontBtn.setOnAction(e -> bringToFront());
        Button toBackBtn = miniActionBtn("↓ Back");
        toBackBtn.setOnAction(e -> sendToBack());
        actionsRow.getChildren().addAll(dupElemBtn, delElemBtn, toFrontBtn, toBackBtn);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox actionsSection = new VBox(4, hRule(), actionsRow);
        actionsSection.setUserData("actionsSection");
        actionsSection.setVisible(false);
        actionsSection.setManaged(false);
        elementPropsPanel.getChildren().add(actionsSection);

        // Bind property changes to selected element
        attachElementPropertyListeners();

        rightPanel.getChildren().add(elementPropsPanel);
    }

    private void buildLayersSection() {
        VBox layersBox = new VBox(0);
        layersBox.setUserData("layersBox");
        layersBox.setVisible(false);
        layersBox.setManaged(false);

        Label header = new Label("Layers");
        header.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:12px;-fx-font-weight:800;"
                + "-fx-padding:14 14 10 14;");
        layersBox.getChildren().add(header);

        layersPanel.setPadding(new Insets(0, 8, 8, 8));
        layersBox.getChildren().add(layersPanel);

        rightPanel.getChildren().add(layersBox);
    }

    private void attachElementPropertyListeners() {
        elemXField.setOnAction(e -> {
            if (selectedElement != null) {
                try { selectedElement.setX(Double.parseDouble(elemXField.getText())); } catch (NumberFormatException ignored) {}
            }
        });
        elemYField.setOnAction(e -> {
            if (selectedElement != null) {
                try { selectedElement.setY(Double.parseDouble(elemYField.getText())); } catch (NumberFormatException ignored) {}
            }
        });
        elemWField.setOnAction(e -> {
            if (selectedElement != null) {
                try { selectedElement.setWidth(Double.parseDouble(elemWField.getText())); } catch (NumberFormatException ignored) {}
            }
        });
        elemHField.setOnAction(e -> {
            if (selectedElement != null) {
                try { selectedElement.setHeight(Double.parseDouble(elemHField.getText())); } catch (NumberFormatException ignored) {}
            }
        });
        elemFillPicker.setOnAction(e -> {
            if (selectedElement != null) selectedElement.setFillColor(elemFillPicker.getValue());
        });
        elemStrokePicker.setOnAction(e -> {
            if (selectedElement != null) selectedElement.setStrokeColor(elemStrokePicker.getValue());
        });
        elemOpacitySlider.valueProperty().addListener((obs, o, n) -> {
            if (selectedElement != null) selectedElement.setOpacity(n.doubleValue());
        });
        elemTextField.textProperty().addListener((obs, o, n) -> {
            if (selectedElement != null && selectedElement.isTextElement()) {
                selectedElement.setTextContent(n);
            }
        });
        elemFontSize.valueProperty().addListener((obs, o, n) -> {
            if (selectedElement != null && selectedElement.isTextElement() && n != null) {
                try { selectedElement.setFontSize(Double.parseDouble(n)); } catch (NumberFormatException ignored) {}
            }
        });
        elemBoldCheck.selectedProperty().addListener((obs, o, n) -> {
            if (selectedElement != null && selectedElement.isTextElement()) {
                selectedElement.setBold(n);
            }
        });
    }

    // ── Tab switching ────────────────────────────────────────────────

    private void showSlideProps(Button slideTab, Button elementTab, Button layersTab) {
        slideTab.setStyle(tabBtnStyle(true));
        elementTab.setStyle(tabBtnStyle(false));
        layersTab.setStyle(tabBtnStyle(false));
        setVisibleByUserData("slideProps", true);
        setVisibleByUserData("elementProps", false);
        setVisibleByUserData("layersBox", false);
    }

    private void showElementProps(Button slideTab, Button elementTab, Button layersTab) {
        slideTab.setStyle(tabBtnStyle(false));
        elementTab.setStyle(tabBtnStyle(true));
        layersTab.setStyle(tabBtnStyle(false));
        setVisibleByUserData("slideProps", false);
        setVisibleByUserData("elementProps", true);
        setVisibleByUserData("layersBox", false);
    }

    private void showLayersPanel(Button slideTab, Button elementTab, Button layersTab) {
        slideTab.setStyle(tabBtnStyle(false));
        elementTab.setStyle(tabBtnStyle(false));
        layersTab.setStyle(tabBtnStyle(true));
        setVisibleByUserData("slideProps", false);
        setVisibleByUserData("elementProps", false);
        setVisibleByUserData("layersBox", true);
        refreshLayersPanel();
    }

    private void setVisibleByUserData(String userData, boolean visible) {
        for (Node n : rightPanel.getChildren()) {
            if (userData.equals(n.getUserData())) {
                n.setVisible(visible);
                n.setManaged(visible);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  CANVAS ELEMENT MANAGEMENT
    // ══════════════════════════════════════════════════════════════════

    private void addElementAtPosition(double x, double y) {
        CanvasElement elem = null;

        switch (currentTool) {
            case "text" -> elem = CanvasElement.createText("Double-click to edit", x, y, 200, 40, 20, false, Color.WHITE);
            case "rect" -> elem = CanvasElement.createRect(x, y, 160, 100, Color.web(ACCENT, 0.6), Color.web(ACCENT));
            case "circle" -> elem = CanvasElement.createCircle(x, y, 80, Color.web(ACCENT, 0.4), Color.web(ACCENT));
            case "line" -> elem = CanvasElement.createLine(x, y, x + 200, y, Color.web(ACCENT));
            case "image" -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Select Image");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
                Window owner = getScene() != null ? getScene().getWindow() : null;
                File file = fc.showOpenDialog(owner);
                if (file != null) {
                    elem = CanvasElement.createImage(file.toURI().toString(), x, y, 200, 150);
                }
            }
        }

        if (elem != null) {
            addElementToCurrentSlide(elem);
            selectElement(elem);
            // Reset to select tool
            currentTool = "select";
            canvasPane.setCursor(Cursor.DEFAULT);
            toolGroup.getToggles().stream()
                    .filter(t -> "select".equals(((ToggleButton) t).getUserData()))
                    .findFirst().ifPresent(t -> ((ToggleButton) t).setSelected(true));
        }
    }

    private void insertPresetTitle() {
        CanvasElement elem = CanvasElement.createText("Slide Title", 48, 100, 860, 60, 36, true, Color.WHITE);
        addElementToCurrentSlide(elem);
        selectElement(elem);
    }

    private void insertPresetSubtitle() {
        CanvasElement elem = CanvasElement.createText("Subtitle text here", 48, 170, 600, 40, 22, false, Color.web("#CBD5E1"));
        addElementToCurrentSlide(elem);
        selectElement(elem);
    }

    private void insertPresetBody() {
        CanvasElement elem = CanvasElement.createText("Body text content goes here. Double-click to edit.", 60, 200, 840, 120, 16, false, Color.web("#E2E8F0"));
        addElementToCurrentSlide(elem);
        selectElement(elem);
    }

    private void insertPresetBullets() {
        String bullets = "• First bullet point\n• Second bullet point\n• Third bullet point\n• Fourth bullet point";
        CanvasElement elem = CanvasElement.createText(bullets, 60, 200, 840, 180, 16, false, Color.web("#E2E8F0"));
        addElementToCurrentSlide(elem);
        selectElement(elem);
    }

    private void addElementToCurrentSlide(CanvasElement elem) {
        int slideIdx = activeSlideIndex;
        slideElements.computeIfAbsent(slideIdx, k -> new ArrayList<>()).add(elem);
        Node node = elem.buildNode();
        makeDraggable(elem, node);
        canvasPane.getChildren().add(node);
        refreshLayersPanel();
    }

    private void makeDraggable(CanvasElement elem, Node node) {
        final double[] dragStart = new double[4]; // mouseX, mouseY, elemX, elemY

        node.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectElement(elem);
                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                dragStart[2] = elem.getX();
                dragStart[3] = elem.getY();
                node.setCursor(Cursor.MOVE);
                e.consume();
            }
        });

        node.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double scale = canvasPane.getScaleX();
                double dx = (e.getSceneX() - dragStart[0]) / scale;
                double dy = (e.getSceneY() - dragStart[1]) / scale;
                double newX = Math.max(0, Math.min(CANVAS_W - 20, dragStart[2] + dx));
                double newY = Math.max(0, Math.min(CANVAS_H - 20, dragStart[3] + dy));
                elem.setX(newX);
                elem.setY(newY);
                updateElementPropertyFields(elem);
                e.consume();
            }
        });

        node.setOnMouseReleased(e -> {
            node.setCursor(Cursor.HAND);
            e.consume();
        });

        node.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && elem.isTextElement()) {
                startInlineTextEdit(elem);
            }
            e.consume();
        });

        node.setCursor(Cursor.HAND);
    }

    private void selectElement(CanvasElement elem) {
        deselectAll();
        selectedElement = elem;
        elem.setSelected(true);
        updateElementPropertyFields(elem);
        showElementPropertySections(true, elem.isTextElement());
    }

    private void deselectAll() {
        if (selectedElement != null) {
            selectedElement.setSelected(false);
            selectedElement = null;
        }
        showElementPropertySections(false, false);
    }

    private void showElementPropertySections(boolean show, boolean isText) {
        for (Node n : elementPropsPanel.getChildren()) {
            if ("noElemMsg".equals(n.getUserData())) {
                n.setVisible(!show);
                n.setManaged(!show);
            }
            if (n.getUserData() != null && Set.of("posSection", "sizeSection", "colorSection", "opacitySection", "actionsSection").contains(n.getUserData())) {
                n.setVisible(show);
                n.setManaged(show);
            }
            if ("textSection".equals(n.getUserData())) {
                n.setVisible(show && isText);
                n.setManaged(show && isText);
            }
        }
    }

    private void updateElementPropertyFields(CanvasElement elem) {
        if (elem == null) return;
        elemXField.setText(String.valueOf((int) elem.getX()));
        elemYField.setText(String.valueOf((int) elem.getY()));
        elemWField.setText(String.valueOf((int) elem.getWidth()));
        elemHField.setText(String.valueOf((int) elem.getHeight()));
        elemFillPicker.setValue(elem.getFillColor());
        elemStrokePicker.setValue(elem.getStrokeColor());
        elemOpacitySlider.setValue(elem.getOpacity());
        if (elem.isTextElement()) {
            elemTextField.setText(elem.getTextContent());
            elemFontSize.setValue(String.valueOf((int) elem.getFontSize()));
            elemBoldCheck.setSelected(elem.isBold());
        }
    }

    private void startInlineTextEdit(CanvasElement elem) {
        TextArea ta = new TextArea(elem.getTextContent());
        ta.setWrapText(true);
        ta.setLayoutX(elem.getX());
        ta.setLayoutY(elem.getY());
        ta.setPrefWidth(elem.getWidth());
        ta.setPrefHeight(Math.max(elem.getHeight(), 60));
        ta.setStyle("-fx-control-inner-background:rgba(0,0,0,0.7);-fx-text-fill:white;"
                + "-fx-font-size:" + (int) elem.getFontSize() + "px;"
                + "-fx-border-color:" + ACCENT + ";-fx-border-radius:6;"
                + "-fx-background-radius:6;-fx-padding:4 8;");

        canvasPane.getChildren().add(ta);
        ta.requestFocus();
        ta.selectAll();

        Runnable commit = () -> {
            String val = ta.getText();
            elem.setTextContent(val);
            elemTextField.setText(val);
            canvasPane.getChildren().remove(ta);
        };

        ta.focusedProperty().addListener((obs, o, n) -> { if (!n) commit.run(); });
        ta.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                commit.run();
                e.consume();
            }
        });
    }

    private void deleteSelectedElement() {
        if (selectedElement == null) return;
        List<CanvasElement> elems = slideElements.get(activeSlideIndex);
        if (elems != null) {
            elems.remove(selectedElement);
        }
        canvasPane.getChildren().remove(selectedElement.getNode());
        selectedElement = null;
        showElementPropertySections(false, false);
        refreshLayersPanel();
    }

    private void duplicateSelectedElement() {
        if (selectedElement == null) return;
        CanvasElement dup = selectedElement.duplicate();
        dup.setX(dup.getX() + 20);
        dup.setY(dup.getY() + 20);
        addElementToCurrentSlide(dup);
        selectElement(dup);
    }

    private void bringToFront() {
        if (selectedElement == null || selectedElement.getNode() == null) return;
        selectedElement.getNode().toFront();
        refreshLayersPanel();
    }

    private void sendToBack() {
        if (selectedElement == null || selectedElement.getNode() == null) return;
        selectedElement.getNode().toBack();
        refreshLayersPanel();
    }

    private void refreshLayersPanel() {
        layersPanel.getChildren().clear();
        List<CanvasElement> elems = slideElements.getOrDefault(activeSlideIndex, List.of());
        if (elems.isEmpty()) {
            Label empty = new Label("No elements on this slide.");
            empty.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:11px;-fx-padding:8;");
            layersPanel.getChildren().add(empty);
            return;
        }
        for (int i = elems.size() - 1; i >= 0; i--) {
            CanvasElement elem = elems.get(i);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 8, 5, 8));
            row.setStyle("-fx-background-color:" + (elem == selectedElement ? "#252F45" : CARD) + ";"
                    + "-fx-background-radius:8;-fx-cursor:hand;");

            Label typeIcon = new Label(elem.getTypeIcon());
            typeIcon.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12px;");

            Label nameLabel = new Label(compact(elem.getLayerName(), 20));
            nameLabel.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:11px;-fx-font-weight:600;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            row.getChildren().addAll(typeIcon, nameLabel);
            row.setOnMouseClicked(e -> selectElement(elem));

            layersPanel.getChildren().add(row);
        }
    }

    private void saveCurrentSlideElements() {
        // Elements are already stored in slideElements map
    }

    // ══════════════════════════════════════════════════════════════════
    //  STATE / LOGIC
    // ══════════════════════════════════════════════════════════════════

    private void attachListeners() {
        slideCountSlider.valueProperty().addListener((obs, o, n) -> {
            int v = (int) Math.round(n.doubleValue());
            slideCountSlider.setValue(v);
            slideCountLabel.setText(String.valueOf(v));
        });
        slides.addListener((javafx.collections.ListChangeListener<SlideContent>) c -> {
            refreshStrip();
            updateButtons();
        });
    }

    private void startGeneration(boolean retry) {
        if (currentTask != null && currentTask.isRunning()) return;

        String topic = topicField.getText() == null ? "" : topicField.getText().trim();
        if (topic.isBlank()) {
            alert(Alert.AlertType.ERROR, "Topic required", "Please enter a topic before generating.");
            return;
        }
        if (!retry && !claudeService.hasApiKey()) {
            alert(Alert.AlertType.ERROR, "API Key missing",
                    "Add your Anthropic API key to config.properties.");
            return;
        }

        setLoading(true);
        slides.clear();
        slideElements.clear();
        activeSlideIndex = 0;

        currentTask = claudeService.createGenerationTask(
                topic, selectedTheme(), langCombo.getValue(),
                audCombo.getValue(), (int) Math.round(slideCountSlider.getValue())
        );

        currentTask.setOnSucceeded(ev -> {
            List<SlideContent> result = currentTask.getValue();
            slides.setAll(result);
            activeSlideIndex = 0;

            // Auto-generate canvas elements from slide content
            for (int i = 0; i < result.size(); i++) {
                slideElements.put(i, generateElementsFromContent(result.get(i)));
            }

            refreshStrip();
            refreshCanvas();
            if (!result.isEmpty()) loadPropertiesPanel(result.get(0));
            setLoading(false);
            progressBar.setProgress(1.0);
            statusLabel.setText(result.size() + " slides ready ✓");
        });

        currentTask.setOnFailed(ev -> {
            Throwable err = currentTask.getException();
            setLoading(false);
            progressBar.setProgress(0);
            statusLabel.setText("Generation failed.");
            handleFailure(err);
        });

        Thread t = new Thread(currentTask, "studio-ai");
        t.setDaemon(true);
        t.start();
    }

    /** Generate draggable CanvasElements from a SlideContent */
    private List<CanvasElement> generateElementsFromContent(SlideContent sc) {
        List<CanvasElement> elems = new ArrayList<>();

        // Title element
        String title = sc.getTitle() != null ? sc.getTitle() : "Untitled";
        boolean isTitleSlide = sc.getType() == SlideContent.SlideType.TITLE;
        double titleSize = isTitleSlide ? 38 : 30;
        double titleY = isTitleSlide ? 160 : 100;
        elems.add(CanvasElement.createText(title, 48, titleY, 860, 60, titleSize, true, Color.WHITE));

        // Type badge
        elems.add(CanvasElement.createText(sc.getType().name(), 30, 24, 120, 28, 10, true, Color.WHITE));

        // Bullet points
        if (sc.getBulletPoints() != null && !sc.getBulletPoints().isEmpty()) {
            StringBuilder bulletsText = new StringBuilder();
            for (String bp : sc.getBulletPoints()) {
                bulletsText.append("• ").append(bp).append("\n");
            }
            double bulletY = isTitleSlide ? 260 : 190;
            elems.add(CanvasElement.createText(bulletsText.toString().trim(), 60, bulletY, 840, 280, 16, false, Color.web("#E2E8F0")));
        }

        // Decorative rectangle (accent band)
        elems.add(CanvasElement.createRect(0, 0, 960, 72, Color.web(accentFromTheme(), 0.85), Color.TRANSPARENT));

        return elems;
    }

    private void setLoading(boolean on) {
        generateBtn.setDisable(on);
        topicField.setDisable(on);
        langCombo.setDisable(on);
        audCombo.setDisable(on);
        slideCountSlider.setDisable(on);
        themeGroup.getToggles().forEach(tg -> ((ToggleButton) tg).setDisable(on));

        if (on) {
            loadingMsgIdx = 0;
            progressBar.setProgress(0.08);
            statusLabel.setText(LOADING_MSGS.get(0));
            stopLoadingTimeline();
            loadingTimeline = new Timeline(new KeyFrame(Duration.seconds(0.75), ev -> {
                loadingMsgIdx = (loadingMsgIdx + 1) % LOADING_MSGS.size();
                statusLabel.setText(LOADING_MSGS.get(loadingMsgIdx));
                progressBar.setProgress(Math.min(0.92, progressBar.getProgress() + 0.14));
            }));
            loadingTimeline.setCycleCount(Timeline.INDEFINITE);
            loadingTimeline.play();
        } else {
            stopLoadingTimeline();
            topicField.setDisable(false);
            langCombo.setDisable(false);
            audCombo.setDisable(false);
            slideCountSlider.setDisable(false);
            themeGroup.getToggles().forEach(tg -> ((ToggleButton) tg).setDisable(false));
        }
        updateButtons();
    }

    private void stopLoadingTimeline() {
        if (loadingTimeline != null) { loadingTimeline.stop(); loadingTimeline = null; }
    }

    private void refreshStrip() {
        slideStrip.getChildren().clear();
        for (int i = 0; i < slides.size(); i++) {
            VBox thumb = buildSlideThumb(slides.get(i), i);
            slideStrip.getChildren().add(thumb);

            thumb.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(180), thumb);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 35L));
            ft.play();
        }
        Button addBtn = new Button("+ Add Slide");
        addBtn.setStyle(ghostBtnStyle() + "-fx-font-size:11px;-fx-padding:8 14;-fx-pref-width:164;");
        addBtn.setOnAction(e -> addEmptySlide());
        slideStrip.getChildren().add(addBtn);
    }

    private void refreshCanvas() {
        canvasPane.setStyle("-fx-background-color:" + themeCanvasColor() + ";"
                + "-fx-background-radius:14;");

        // Clear canvas
        canvasPane.getChildren().clear();

        // Re-apply clip
        Rectangle canvasClip = new Rectangle(0, 0, CANVAS_W, CANVAS_H);
        canvasClip.setArcWidth(28);
        canvasClip.setArcHeight(28);
        canvasPane.setClip(canvasClip);

        if (slides.isEmpty()) {
            // Empty state
            Label emptyIcon = new Label("✦");
            emptyIcon.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:48px;");
            emptyIcon.setLayoutX(CANVAS_W / 2 - 24);
            emptyIcon.setLayoutY(180);

            Label emptyText = new Label("Click Generate ✦ to create slides");
            emptyText.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:18px;-fx-font-weight:700;");
            emptyText.setLayoutX(CANVAS_W / 2 - 180);
            emptyText.setLayoutY(250);

            Label emptyHint = new Label("Or use the toolbar to add elements manually");
            emptyHint.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:13px;");
            emptyHint.setLayoutX(CANVAS_W / 2 - 180);
            emptyHint.setLayoutY(285);

            canvasPane.getChildren().addAll(emptyIcon, emptyText, emptyHint);
            slideIndicatorLabel.setText("");
            return;
        }

        activeSlideIndex = Math.max(0, Math.min(activeSlideIndex, slides.size() - 1));
        SlideContent sc = slides.get(activeSlideIndex);

        slideIndicatorLabel.setText("Slide " + (activeSlideIndex + 1) + " / " + slides.size()
                + "  ·  " + sc.getType().name());

        // Render all elements for this slide
        List<CanvasElement> elems = slideElements.getOrDefault(activeSlideIndex, new ArrayList<>());
        for (CanvasElement elem : elems) {
            Node node = elem.buildNode();
            makeDraggable(elem, node);
            canvasPane.getChildren().add(node);

            // Animate in
            node.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(200), node);
            ft.setToValue(elem.getOpacity());
            ft.setDelay(Duration.millis(elems.indexOf(elem) * 40L));
            ft.play();
        }

        // Decorative elements (non-interactive)
        Circle deco1 = new Circle(880, 460, 80);
        deco1.setFill(Color.web(accentFromTheme(), 0.08));
        deco1.setMouseTransparent(true);
        Circle deco2 = new Circle(850, 440, 45);
        deco2.setFill(Color.web(accentFromTheme(), 0.05));
        deco2.setMouseTransparent(true);

        // Footer
        Rectangle footerLine = new Rectangle(36, 505, 888, 1);
        footerLine.setFill(Color.web("#FFFFFF", 0.08));
        footerLine.setMouseTransparent(true);
        Label footerLabel = new Label("StudyFlow Presentation Studio  ·  Slide " + sc.getSlideNumber());
        footerLabel.setStyle("-fx-text-fill:rgba(255,255,255,0.2);-fx-font-size:9px;-fx-font-weight:600;");
        footerLabel.setLayoutX(36);
        footerLabel.setLayoutY(512);
        footerLabel.setMouseTransparent(true);

        // Slide number watermark
        Label slideNum = new Label(String.format("%02d", sc.getSlideNumber()));
        slideNum.setStyle("-fx-text-fill:rgba(255,255,255,0.08);-fx-font-size:120px;-fx-font-weight:900;");
        slideNum.setLayoutX(720);
        slideNum.setLayoutY(380);
        slideNum.setMouseTransparent(true);

        canvasPane.getChildren().addAll(deco1, deco2, footerLine, footerLabel, slideNum);

        // Move decorative elements to back
        deco1.toBack();
        deco2.toBack();
        slideNum.toBack();

        refreshLayersPanel();
    }

    private void loadPropertiesPanel(SlideContent sc) {
        propSlideNum.setText("Slide " + sc.getSlideNumber());
        propTitle.setText(sc.getTitle() == null ? "" : sc.getTitle());
        propType.setValue(sc.getType().name());

        StringBuilder bullets = new StringBuilder();
        for (String b : sc.getBulletPoints()) bullets.append("• ").append(b).append("\n");
        propBullets.setText(bullets.toString().trim());
        propNotes.setText(sc.getSpeakerNotes() == null ? "" : sc.getSpeakerNotes());
    }

    private void navigateSlide(int delta) {
        if (slides.isEmpty()) return;
        saveCurrentSlideElements();
        activeSlideIndex = Math.floorMod(activeSlideIndex + delta, slides.size());
        deselectAll();
        refreshCanvas();
        refreshStrip();
        loadPropertiesPanel(slides.get(activeSlideIndex));
    }

    private void reorderSlides(int src, int dst) {
        if (src == dst || src < 0 || dst < 0 || src >= slides.size() || dst >= slides.size()) return;
        SlideContent moved = slides.remove(src);
        slides.add(dst, moved);

        // Also reorder elements
        List<CanvasElement> movedElems = slideElements.remove(src);
        Map<Integer, List<CanvasElement>> newMap = new HashMap<>();
        for (Map.Entry<Integer, List<CanvasElement>> entry : slideElements.entrySet()) {
            int key = entry.getKey();
            if (src < dst) {
                if (key > src && key <= dst) newMap.put(key - 1, entry.getValue());
                else newMap.put(key, entry.getValue());
            } else {
                if (key >= dst && key < src) newMap.put(key + 1, entry.getValue());
                else newMap.put(key, entry.getValue());
            }
        }
        newMap.put(dst, movedElems != null ? movedElems : new ArrayList<>());
        slideElements.clear();
        slideElements.putAll(newMap);

        renumber();
        activeSlideIndex = dst;
        refreshStrip();
        refreshCanvas();
    }

    private void renumber() {
        for (int i = 0; i < slides.size(); i++) slides.get(i).setSlideNumber(i + 1);
    }

    private void addEmptySlide() {
        int num = slides.size() + 1;
        SlideContent sc = new SlideContent();
        sc.setSlideNumber(num);
        sc.setTitle("New Slide " + num);
        sc.setBulletPoints(List.of("Add your first point here.", "And another idea.", "Key takeaway."));
        sc.setSpeakerNotes("Speaker notes for slide " + num);
        sc.setType(SlideContent.SlideType.CONTENT);
        slides.add(sc);

        // Generate default elements
        slideElements.put(slides.size() - 1, generateElementsFromContent(sc));

        activeSlideIndex = slides.size() - 1;
        refreshCanvas();
        loadPropertiesPanel(sc);
    }

    private void duplicateCurrentSlide() {
        if (slides.isEmpty()) return;
        SlideContent orig = slides.get(activeSlideIndex);
        SlideContent dup = new SlideContent();
        dup.setTitle(orig.getTitle() + " (copy)");
        dup.setBulletPoints(new ArrayList<>(orig.getBulletPoints()));
        dup.setSpeakerNotes(orig.getSpeakerNotes());
        dup.setType(orig.getType());
        slides.add(activeSlideIndex + 1, dup);

        // Duplicate elements
        List<CanvasElement> origElems = slideElements.getOrDefault(activeSlideIndex, new ArrayList<>());
        List<CanvasElement> dupElems = new ArrayList<>();
        for (CanvasElement e : origElems) {
            dupElems.add(e.duplicate());
        }
        slideElements.put(activeSlideIndex + 1, dupElems);

        renumber();
        activeSlideIndex = activeSlideIndex + 1;
        refreshCanvas();
        loadPropertiesPanel(slides.get(activeSlideIndex));
    }

    private void deleteSlide(int idx) {
        if (slides.isEmpty() || idx < 0 || idx >= slides.size()) return;
        slides.remove(idx);
        slideElements.remove(idx);

        // Shift element indices
        Map<Integer, List<CanvasElement>> newMap = new HashMap<>();
        for (Map.Entry<Integer, List<CanvasElement>> entry : slideElements.entrySet()) {
            int key = entry.getKey();
            if (key > idx) newMap.put(key - 1, entry.getValue());
            else if (key < idx) newMap.put(key, entry.getValue());
        }
        slideElements.clear();
        slideElements.putAll(newMap);

        renumber();
        activeSlideIndex = Math.max(0, Math.min(idx, slides.size() - 1));
        deselectAll();
        refreshCanvas();
        if (!slides.isEmpty()) loadPropertiesPanel(slides.get(activeSlideIndex));
    }

    private void applyAccentOverride(String hex) {
        // Update accent band elements on current slide
        List<CanvasElement> elems = slideElements.getOrDefault(activeSlideIndex, new ArrayList<>());
        for (CanvasElement e : elems) {
            if (e.getType() == CanvasElement.ElementType.RECT && e.getWidth() > 900) {
                e.setFillColor(Color.web(hex, 0.85));
            }
        }
        refreshCanvas();
    }

    private void updateButtons() {
        boolean has = !slides.isEmpty();
        boolean loading = currentTask != null && currentTask.isRunning();
        exportBtn.setDisable(!has || loading);
        previewBtn.setDisable(!has || loading);
    }

    private void showPreviewDialog() {
        if (slides.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "No content", "Generate a presentation first.");
            return;
        }
        Stage stage = dialogStage("Full Preview");

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:" + BG + ";");

        Label title = new Label("Preview — " + topicField.getText().trim());
        title.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:22px;-fx-font-weight:800;");
        content.getChildren().add(title);

        for (SlideContent sc : slides) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(16));
            card.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:16;"
                    + "-fx-border-color:" + BORDER + ";-fx-border-radius:16;");

            Label st = new Label("Slide " + sc.getSlideNumber() + " — " + sc.getTitle());
            st.setWrapText(true);
            st.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:16px;-fx-font-weight:800;");
            card.getChildren().add(st);

            for (String b : sc.getBulletPoints()) {
                Label bl = new Label("• " + b);
                bl.setWrapText(true);
                bl.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:13px;");
                card.getChildren().add(bl);
            }

            Label notes = new Label("Notes: " + sc.getSpeakerNotes());
            notes.setWrapText(true);
            notes.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:11px;-fx-font-style:italic;");
            card.getChildren().add(notes);
            content.getChildren().add(card);
        }

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:" + BG + ";");
        stage.setScene(new Scene(sp, 900, 740));
        stage.showAndWait();
    }

    private void exportPresentation() {
        if (slides.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "No content", "Generate a presentation first.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Presentation");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PowerPoint", "*.pptx"));
        fc.setInitialFileName(safeFilename(topicField.getText().trim()) + ".pptx");
        Window owner = getScene() == null ? null : getScene().getWindow();
        File f = fc.showSaveDialog(owner);
        if (f == null) return;
        try {
            exporter.exportToPptx(new ArrayList<>(slides), topicField.getText().trim(), selectedTheme(), f);
            alert(Alert.AlertType.INFORMATION, "Export successful",
                    "Saved to:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Export failed",
                    compact(ex.getMessage() == null ? "Unknown error." : ex.getMessage(), 240));
        }
    }

    private void handleFailure(Throwable err) {
        String msg = err == null ? "Unknown error." : compact(err.getMessage(), 240);
        if (err instanceof TimeoutException || (msg != null && msg.toLowerCase().contains("timeout"))) {
            if (retryDialog("Timeout", "Generation took too long. Retry?")) {
                Platform.runLater(() -> startGeneration(true));
            }
            return;
        }
        alert(Alert.AlertType.ERROR, "Generation failed", msg);
    }

    private boolean retryDialog(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        ButtonType retry = new ButtonType("Retry", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(retry, cancel);
        styleDialog(a.getDialogPane());
        return a.showAndWait().orElse(cancel) == retry;
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(content);
        styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private void styleDialog(DialogPane dp) {
        dp.setStyle("-fx-background-color:" + BG + ";");
        if (dp.lookup(".content.label") != null)
            dp.lookup(".content.label").setStyle("-fx-text-fill:" + TEXT_SEC + ";");
    }

    // ══════════════════════════════════════════════════════════════════
    //  THEME / STYLE HELPERS
    // ══════════════════════════════════════════════════════════════════

    private void refreshThemeStyles() {
        themeGroup.getToggles().forEach(tg -> {
            ToggleButton tb = (ToggleButton) tg;
            tb.setStyle(themeToggleStyle(tb.isSelected()));
        });
        // Regenerate elements with new theme colors
        if (!slides.isEmpty()) {
            List<CanvasElement> elems = slideElements.getOrDefault(activeSlideIndex, new ArrayList<>());
            for (CanvasElement e : elems) {
                if (e.getType() == CanvasElement.ElementType.RECT && e.getWidth() > 900) {
                    e.setFillColor(Color.web(accentFromTheme(), 0.85));
                }
            }
        }
        refreshCanvas();
    }

    private String selectedTheme() {
        ToggleButton sel = (ToggleButton) themeGroup.getSelectedToggle();
        return sel == null ? "Modern" : String.valueOf(sel.getUserData());
    }

    private String themeCanvasColor() {
        return switch (selectedTheme().toLowerCase()) {
            case "minimal"   -> "#F1F5F9";
            case "bold"      -> "#1A1A2E";
            case "academic"  -> "#0F172A";
            case "creative"  -> "#170E2D";
            default          -> "#111827";
        };
    }

    private String accentFromTheme() {
        return switch (selectedTheme().toLowerCase()) {
            case "minimal"   -> "#7F77DD";
            case "bold"      -> "#F59E0B";
            case "academic"  -> "#1B3A5C";
            case "creative"  -> "#EC4899";
            default          -> "#7F77DD";
        };
    }

    // ── Style strings ─────────────────────────────────────────────────

    private String inputStyle() {
        return "-fx-background-color:#0D1117;-fx-text-fill:" + TEXT_PRI + ";"
                + "-fx-prompt-text-fill:" + TEXT_MUT + ";"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-font-size:12px;-fx-padding:8 12;";
    }

    private String smallInputStyle() {
        return "-fx-background-color:#0D1117;-fx-text-fill:" + TEXT_PRI + ";"
                + "-fx-prompt-text-fill:" + TEXT_MUT + ";"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;"
                + "-fx-font-size:11px;-fx-padding:5 8;";
    }

    private String comboStyle() {
        return "-fx-background-color:#0D1117;-fx-text-fill:" + TEXT_PRI + ";"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-font-size:11px;";
    }

    private String accentBtnStyle() {
        return "-fx-background-color:" + ACCENT + ";-fx-text-fill:white;"
                + "-fx-font-size:12px;-fx-font-weight:800;-fx-background-radius:10;"
                + "-fx-padding:9 18;-fx-cursor:hand;";
    }

    private String successBtnStyle() {
        return "-fx-background-color:" + SUCCESS + ";-fx-text-fill:#0D1117;"
                + "-fx-font-size:12px;-fx-font-weight:800;-fx-background-radius:10;"
                + "-fx-padding:9 18;-fx-cursor:hand;";
    }

    private String ghostBtnStyle() {
        return "-fx-background-color:" + CARD + ";-fx-text-fill:" + TEXT_SEC + ";"
                + "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:10;"
                + "-fx-padding:8 14;-fx-cursor:hand;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:10;";
    }

    private String miniActionBtnStyle() {
        return "-fx-background-color:transparent;-fx-text-fill:" + TEXT_SEC + ";"
                + "-fx-font-size:10px;-fx-font-weight:600;-fx-background-radius:6;"
                + "-fx-padding:4 8;-fx-cursor:hand;";
    }

    private String toolToggleStyle(boolean selected) {
        return selected
                ? "-fx-background-color:" + ACCENT + ";-fx-text-fill:white;"
                + "-fx-font-size:12px;-fx-font-weight:800;-fx-background-radius:8;"
                + "-fx-padding:6 12;-fx-cursor:hand;"
                : "-fx-background-color:" + CARD + ";-fx-text-fill:" + TEXT_SEC + ";"
                + "-fx-font-size:12px;-fx-font-weight:700;-fx-background-radius:8;"
                + "-fx-padding:6 12;-fx-cursor:hand;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:8;";
    }

    private String themeToggleStyle(boolean selected) {
        return selected
                ? "-fx-background-color:" + ACCENT + ";-fx-text-fill:white;"
                + "-fx-font-size:11px;-fx-font-weight:800;-fx-background-radius:10;"
                + "-fx-padding:6 10;-fx-cursor:hand;"
                : "-fx-background-color:" + CARD + ";-fx-text-fill:" + TEXT_SEC + ";"
                + "-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:10;"
                + "-fx-padding:6 10;-fx-cursor:hand;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:10;";
    }

    private String thumbStyle(boolean active) {
        return "-fx-background-color:" + (active ? "#252F45" : CARD) + ";"
                + "-fx-background-radius:10;-fx-border-radius:10;"
                + "-fx-border-width:1.5;"
                + "-fx-border-color:" + (active ? ACCENT : "rgba(127,119,221,0.15)") + ";"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),8,0,0,3);";
    }

    private String tabBtnStyle(boolean active) {
        return "-fx-background-color:" + (active ? SURFACE : CARD) + ";"
                + "-fx-text-fill:" + (active ? TEXT_PRI : TEXT_MUT) + ";"
                + "-fx-font-size:11px;-fx-font-weight:" + (active ? "800" : "600") + ";"
                + "-fx-background-radius:0;-fx-padding:10 18;-fx-cursor:hand;"
                + "-fx-border-color:" + (active ? ACCENT : "transparent") + ";"
                + "-fx-border-width:0 0 2 0;";
    }

    // ── Widget builders ────────────────────────────────────────────────

    private Button tabBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setStyle(tabBtnStyle(active));
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private Button miniActionBtn(String text) {
        Button b = new Button(text);
        b.setStyle(miniActionBtnStyle());
        b.setOnMouseEntered(e -> b.setStyle(miniActionBtnStyle()
                + "-fx-background-color:rgba(127,119,221,0.1);"));
        b.setOnMouseExited(e -> b.setStyle(miniActionBtnStyle()));
        return b;
    }

    private Button chip(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + CARD + ";-fx-text-fill:" + ACCENT_HOT + ";"
                + "-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:999;"
                + "-fx-padding:3 8;-fx-cursor:hand;"
                + "-fx-border-color:rgba(127,119,221,0.2);-fx-border-radius:999;");
        return b;
    }

    private Button navArrow(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:rgba(28,35,51,0.85);-fx-text-fill:" + TEXT_PRI + ";"
                + "-fx-font-size:26px;-fx-font-weight:900;-fx-background-radius:999;"
                + "-fx-padding:5 14;-fx-cursor:hand;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:999;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle()
                .replace("rgba(28,35,51,0.85)", "rgba(127,119,221,0.3)")));
        b.setOnMouseExited(e -> b.setStyle(b.getStyle()
                .replace("rgba(127,119,221,0.3)", "rgba(28,35,51,0.85)")));
        return b;
    }

    private Label label(String text, String color, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:" + size + "px;-fx-font-weight:600;");
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;-fx-font-weight:700;"
                + "-fx-padding:8 14 4 14;");
        return l;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setPrefHeight(38);
        r.setStyle("-fx-background-color:" + ACCENT_DIM + ";");
        return r;
    }

    private Region toolbarDivider() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setPrefHeight(20);
        r.setStyle("-fx-background-color:" + ACCENT_DIM + ";");
        HBox.setMargin(r, new Insets(0, 4, 0, 4));
        return r;
    }

    private Region hRule() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:rgba(127,119,221,0.1);");
        return r;
    }

    private FontIcon icon(String literal, String color, int size) {
        FontIcon i = new FontIcon();
        i.setIconLiteral(literal);
        i.setIconColor(Color.web(color));
        i.setIconSize(size);
        return i;
    }

    private VBox propSection(String label, javafx.scene.Node control) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:10px;-fx-font-weight:700;"
                + "-fx-padding:10 14 4 14;");
        VBox section = new VBox(0, lbl, wrap(control));
        return section;
    }

    private HBox wrap(javafx.scene.Node n) {
        HBox h = new HBox(n);
        h.setPadding(new Insets(0, 14, 6, 14));
        return h;
    }

    private Label propSlideNum(String val) {
        propSlideNum.setText(val);
        propSlideNum.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:12px;-fx-font-weight:700;");
        return propSlideNum;
    }

    private Stage dialogStage(String title) {
        Stage s = new Stage(StageStyle.DECORATED);
        if (getScene() != null && getScene().getWindow() != null)
            s.initOwner(getScene().getWindow());
        s.initModality(Modality.WINDOW_MODAL);
        s.setTitle(title);
        return s;
    }

    // ── Utilities ─────────────────────────────────────────────────────

    private String compact(String v, int max) {
        String n = v == null ? "" : v.replaceAll("\\s+", " ").trim();
        return n.length() <= max ? n : n.substring(0, Math.max(0, max - 3)).trim() + "…";
    }

    private String safeFilename(String raw) {
        String base = raw == null || raw.isBlank() ? "presentation" : raw.trim();
        return base.replaceAll("[\\\\/:*?\"<>|]", "-").replaceAll("\\s+", "-").toLowerCase(Locale.ROOT);
    }
}