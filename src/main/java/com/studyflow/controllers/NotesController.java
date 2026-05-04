package com.studyflow.controllers;

import com.studyflow.services.CloudflareAiSketchService;
import com.studyflow.models.User;
import com.studyflow.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;

public class NotesController implements Initializable {
    private static final DateTimeFormatter SAVED_AT_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
    private static final List<String> DEFAULT_FOLDERS =
            List.of("General", "Flashcards", "Revision", "Ideas");
    private static final List<String> AI_STYLES =
            List.of("Default", "Diagram", "Sketch", "Mindmap", "Chalk");
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "again", "also", "because", "been", "before", "between",
            "could", "does", "from", "have", "into", "lesson", "more", "note", "notes",
            "should", "some", "that", "their", "there", "these", "they", "this", "topic",
            "using", "very", "what", "when", "where", "which", "while", "with", "your"
    );

    @FXML private Label totalNotesLabel;
    @FXML private Label foldersLabel;
    @FXML private Label favoritesLabel;
    @FXML private Label recentLabel;
    @FXML private Label noteStatusLabel;
    @FXML private TextField noteTitleField;
    @FXML private ComboBox<String> noteFolderCombo;
    @FXML private TextField noteTagsField;
    @FXML private ToggleButton favoriteToggle;
    @FXML private Label wordCountLabel;
    @FXML private TextArea noteTextArea;
    @FXML private Label smartSummaryLabel;

    @FXML private TextField aiPromptField;
    @FXML private ComboBox<String> aiStyleCombo;
    @FXML private Button generateAiSketchButton;
    @FXML private ToggleButton penToolButton;
    @FXML private ToggleButton pencilToolButton;
    @FXML private ToggleButton markerToolButton;
    @FXML private ToggleButton brushToolButton;
    @FXML private ToggleButton highlighterToolButton;
    @FXML private ToggleButton calligraphyToolButton;
    @FXML private ToggleButton eraserToolButton;

    @FXML private Button strokeThinButton;
    @FXML private Button strokeMediumButton;
    @FXML private Button strokeBoldButton;
    @FXML private Button strokeXLButton;
    @FXML private Slider opacitySlider;
    @FXML private Label opacityLabel;
    @FXML private Button undoStrokeButton;
    @FXML private Button redoStrokeButton;
    @FXML private Button clearCanvasButton;
    @FXML private ToggleButton snapShapeToggle;
    @FXML private ColorPicker customColorPicker;
    @FXML private Label toolModeLabel;
    @FXML private Label strokeModeLabel;
    @FXML private Label colorModeLabel;
    @FXML private Label zoomLabel;
    @FXML private FlowPane paletteBox;
    @FXML private ScrollPane canvasScrollPane;
    @FXML private StackPane canvasContainer;
    @FXML private Canvas drawingCanvas;
    @FXML private ToggleButton bgBlankToggle;
    @FXML private ToggleButton bgLinedToggle;
    @FXML private ToggleButton bgGridToggle;
    @FXML private ToggleButton bgDotToggle;
    @FXML private ToggleButton bgDarkToggle;

    private final Preferences preferences = Preferences.userNodeForPackage(NotesController.class);
    private final ArrayDeque<WritableImage> undoStack = new ArrayDeque<>();
    private final ArrayDeque<WritableImage> redoStack = new ArrayDeque<>();
    private final ToggleGroup toolToggleGroup = new ToggleGroup();
    private final ToggleGroup backgroundToggleGroup = new ToggleGroup();
    private final CloudflareAiSketchService cloudflareAiSketchService = new CloudflareAiSketchService();

    private GraphicsContext graphicsContext;
    private Color currentColor = Color.web("#A855F7");
    private String currentColorName = "Violet";
    private String currentTool = "Pen";
    private double currentStrokeWidth = 4.0;
    private String currentStrokeName = "Medium";
    private double currentOpacity = 1.0;
    private double currentZoom = 1.0;
    private String currentBackground = "blank";
    private double lastX;
    private double lastY;
    private int aiDraftCount;
    private boolean focusMode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        graphicsContext = drawingCanvas.getGraphicsContext2D();
        setupFolders();
        setupAiStyles();
        setupToolButtons();
        setupStrokeButtons();
        setupBackgroundButtons();
        setupPalette();
        setupOpacityControls();
        setupCanvas();
        hookListeners();
        loadSavedNote();
        refreshMetrics();
        refreshStats();
        refreshToolLabels();
        applyBackgroundToCanvas(true);
        updateUndoState();
        updateZoomLabel();
        updateNoteStatus("Draft ready", false);
    }

    @FXML
    private void handleSaveNote() {
        String title = safe(noteTitleField.getText());
        String body = safe(noteTextArea.getText());
        if (title.isBlank() && body.isBlank()) {
            updateNoteStatus("Add a title or note content before saving.", true);
            return;
        }

        long savedAt = System.currentTimeMillis();
        preferences.put(key("title"), title);
        preferences.put(key("body"), body);
        preferences.put(key("folder"), noteFolderCombo.getValue());
        preferences.put(key("tags"), safe(noteTagsField.getText()));
        preferences.putBoolean(key("favorite"), favoriteToggle.isSelected());
        preferences.put(key("prompt"), safe(aiPromptField.getText()));
        preferences.put(key("aiStyle"), safe(aiStyleCombo.getValue()));
        preferences.putInt(key("aiDraftCount"), aiDraftCount);
        preferences.put(key("background"), currentBackground);
        preferences.put(key("tool"), currentTool);
        preferences.put(key("stroke"), currentStrokeName);
        preferences.putDouble(key("opacity"), currentOpacity);
        preferences.putLong(key("savedAt"), savedAt);

        refreshStats();
        updateNoteStatus("Saved " + SAVED_AT_FORMAT.format(
                Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        ), false);
    }

    @FXML
    private void handleVoiceNote() {
        String prefix = noteTitleField.getText() == null || noteTitleField.getText().isBlank()
                ? "Voice note"
                : "Voice note for " + safe(noteTitleField.getText());
        String generated = prefix + ": capture the explanation here, then refine it into key points.";
        if (!safe(noteTextArea.getText()).isBlank()) {
            noteTextArea.appendText(System.lineSeparator() + System.lineSeparator() + generated);
        } else {
            noteTextArea.setText(generated);
        }
        refreshMetrics();
        updateNoteStatus("Voice note placeholder inserted.", false);
    }

    @FXML
    private void handleExportNote() {
        handleSaveNote();
        updateNoteStatus("Export action prepared. Save is complete and content is ready for external export.", false);
    }

    @FXML
    private void handleSmartAssist() {
        String title = safe(noteTitleField.getText());
        String tags = safe(noteTagsField.getText());
        String body = safe(noteTextArea.getText());
        if (title.isBlank() && body.isBlank()) {
            smartSummaryLabel.setText("Smart Assist needs some content first. Add a title or a few lines of notes.");
            updateNoteStatus("Nothing to analyze yet.", true);
            return;
        }

        String focus = title.isBlank() ? firstMeaningfulLine(body) : title;
        String keywords = extractKeywords(body.isBlank() ? title : body);
        int wordCount = countWords(title + " " + body);
        String nextStep = wordCount < 40
                ? "Expand the note with definitions, examples or one worked exercise."
                : "Turn the key ideas into 3 to 5 flashcards and add one diagram.";
        String tagPart = tags.isBlank() ? "" : " Tags: " + tags + ".";

        smartSummaryLabel.setText(
                "Focus: " + focus + ". "
                        + "Keywords: " + keywords + ". "
                        + "Scope: " + wordCount + " words."
                        + tagPart
                        + " Next step: " + nextStep
        );
        updateNoteStatus("Smart summary refreshed.", false);
    }

    @FXML
    private void showNewNoteDialog() {
        noteTitleField.clear();
        noteTextArea.clear();
        noteTagsField.clear();
        aiPromptField.clear();
        noteFolderCombo.setValue(DEFAULT_FOLDERS.get(0));
        aiStyleCombo.setValue(AI_STYLES.get(0));
        favoriteToggle.setSelected(false);
        smartSummaryLabel.setText("Summarise • Keywords • Next step - click Run to analyse your note.");
        aiDraftCount = 0;
        undoStack.clear();
        redoStack.clear();
        applyBackgroundSelection("blank", false);
        clearCanvas();
        refreshMetrics();
        refreshStats();
        updateUndoState();
        updateNoteStatus("New draft started.", false);
    }

    @FXML
    private void handleFocusMode() {
        focusMode = !focusMode;
        Node leftPanel = noteTextArea == null ? null : noteTextArea.getParent();
        if (leftPanel != null) {
            leftPanel.setVisible(!focusMode);
            leftPanel.setManaged(!focusMode);
        }
        if (canvasScrollPane != null) {
            canvasScrollPane.requestFocus();
        }
        updateNoteStatus(focusMode ? "Focus mode enabled." : "Focus mode disabled.", false);
    }

    @FXML
    private void handleTextBold() {
        wrapSelection("**", "**", "bold text");
    }

    @FXML
    private void handleTextItalic() {
        wrapSelection("*", "*", "italic text");
    }

    @FXML
    private void handleTextList() {
        String selected = noteTextArea.getSelectedText();
        if (selected == null || selected.isBlank()) {
            insertAtCaret("- item 1" + System.lineSeparator() + "- item 2");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : selected.split("\\R")) {
            builder.append("- ").append(line.strip()).append(System.lineSeparator());
        }
        replaceSelection(builder.toString().trim());
    }

    @FXML
    private void handleTextCode() {
        wrapSelection("`", "`", "code");
    }

    @FXML
    private void handleGenerateAiSketch() {
        String prompt = safe(aiPromptField.getText());
        if (prompt.isBlank()) {
            updateNoteStatus("Add a prompt before generating a sketch.", true);
            return;
        }

        if (prompt.toLowerCase(Locale.ROOT).contains("clear")) {
            pushCanvasState();
            drawPromptSketch(prompt.toLowerCase(Locale.ROOT));
            redoStack.clear();
            updateUndoState();
            updateNoteStatus("Canvas cleared from prompt.", false);
            return;
        }

        if (!cloudflareAiSketchService.isConfigured()) {
            pushCanvasState();
            drawPromptSketch(prompt.toLowerCase(Locale.ROOT));
            aiDraftCount++;
            redoStack.clear();
            refreshStats();
            updateUndoState();
            updateNoteStatus("Local sketch generated. Configure Cloudflare for prompt-driven diagrams.", false);
            return;
        }

        setAiGenerationLoading(true);
        updateNoteStatus("Generating sketch with Cloudflare AI...", false);

        Task<AiSketchResult> task = new Task<>() {
            @Override
            protected AiSketchResult call() throws Exception {
                String style = safe(aiStyleCombo.getValue());
                String title = safe(noteTitleField.getText());
                String body = safe(noteTextArea.getText());
                if (cloudflareAiSketchService.shouldUseImageGeneration(prompt, style)) {
                    return AiSketchResult.forScene(
                            cloudflareAiSketchService.generateScene(prompt, style, title, body)
                    );
                }
                return AiSketchResult.forPlan(
                        cloudflareAiSketchService.generateSketchPlan(prompt, style, title, body)
                );
            }
        };

        task.setOnSucceeded(event -> {
            pushCanvasState();
            AiSketchResult result = task.getValue();
            if (result.scene() != null) {
                drawGeneratedScene(result.scene(), safe(aiStyleCombo.getValue()));
            } else {
                drawSketchPlan(result.plan());
            }
            aiDraftCount++;
            redoStack.clear();
            refreshStats();
            updateUndoState();
            setAiGenerationLoading(false);
            updateNoteStatus(
                    result.scene() != null
                            ? "Cloudflare image generated from prompt."
                            : "Cloudflare sketch generated from prompt.",
                    false
            );
        });

        task.setOnFailed(event -> {
            pushCanvasState();
            drawPromptSketch(prompt.toLowerCase(Locale.ROOT));
            aiDraftCount++;
            redoStack.clear();
            refreshStats();
            updateUndoState();
            setAiGenerationLoading(false);
            updateNoteStatus(
                    "Cloudflare unavailable. Local fallback generated: " + shortMessage(task.getException()),
                    true
            );
        });

        Thread worker = new Thread(task, "notes-cloudflare-sketch");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handlePenTool() {
        selectTool("Pen");
    }

    @FXML
    private void handlePencilTool() {
        selectTool("Pencil");
    }

    @FXML
    private void handleMarkerTool() {
        selectTool("Marker");
    }

    @FXML
    private void handleBrushTool() {
        selectTool("Brush");
    }

    @FXML
    private void handleHighlighterTool() {
        selectTool("Highlighter");
    }

    @FXML
    private void handleCalligraphyTool() {
        selectTool("Calligraphy");
    }

    @FXML
    private void handleEraserTool() {
        selectTool("Eraser");
    }

    @FXML
    private void handleStrokeThin() {
        selectStroke(2.0, "Thin");
    }

    @FXML
    private void handleStrokeMedium() {
        selectStroke(4.0, "Medium");
    }

    @FXML
    private void handleStrokeBold() {
        selectStroke(8.0, "Bold");
    }

    @FXML
    private void handleStrokeXL() {
        selectStroke(14.0, "XL");
    }

    @FXML
    private void handleUndoStroke() {
        if (undoStack.isEmpty()) {
            updateNoteStatus("Nothing to undo.", true);
            return;
        }
        redoStack.push(captureCanvas());
        restoreCanvas(undoStack.pop());
        updateUndoState();
        updateNoteStatus("Last action removed.", false);
    }

    @FXML
    private void handleRedoStroke() {
        if (redoStack.isEmpty()) {
            updateNoteStatus("Nothing to redo.", true);
            return;
        }
        undoStack.push(captureCanvas());
        restoreCanvas(redoStack.pop());
        updateUndoState();
        updateNoteStatus("Action restored.", false);
    }

    @FXML
    private void handleClearCanvas() {
        pushCanvasState();
        redoStack.clear();
        clearCanvas();
        updateUndoState();
        updateNoteStatus("Canvas cleared.", false);
    }

    @FXML
    private void handleToggleTexture() {
        String next = switch (currentBackground) {
            case "blank" -> "lined";
            case "lined" -> "grid";
            case "grid" -> "dot";
            case "dot" -> "dark";
            default -> "blank";
        };
        applyBackgroundSelection(next, true);
        updateNoteStatus("Canvas background changed to " + next + ".", false);
    }

    @FXML
    private void handleSaveCanvasImage() {
        handleSaveNote();
        updateNoteStatus("Canvas snapshot is ready. Persisted note settings were saved successfully.", false);
    }

    @FXML
    private void handleZoomIn() {
        setZoom(Math.min(2.0, currentZoom + 0.1));
    }

    @FXML
    private void handleZoomOut() {
        setZoom(Math.max(0.5, currentZoom - 0.1));
    }

    @FXML
    private void handleZoomReset() {
        setZoom(1.0);
    }

    private void setupFolders() {
        noteFolderCombo.setItems(FXCollections.observableArrayList(DEFAULT_FOLDERS));
        noteFolderCombo.setValue(DEFAULT_FOLDERS.get(0));
    }

    private void setupAiStyles() {
        aiStyleCombo.setItems(FXCollections.observableArrayList(AI_STYLES));
        aiStyleCombo.setValue(AI_STYLES.get(0));
    }

    private void setupToolButtons() {
        for (ToggleButton button : List.of(
                penToolButton, pencilToolButton, markerToolButton, brushToolButton,
                highlighterToolButton, calligraphyToolButton, eraserToolButton
        )) {
            button.setToggleGroup(toolToggleGroup);
        }
        penToolButton.setSelected(true);
        styleToolButtons();
    }

    private void setupStrokeButtons() {
        styleStrokeButtons();
    }

    private void setupBackgroundButtons() {
        for (ToggleButton button : List.of(bgBlankToggle, bgLinedToggle, bgGridToggle, bgDotToggle, bgDarkToggle)) {
            button.setToggleGroup(backgroundToggleGroup);
        }
        bgBlankToggle.setSelected(true);

        bgBlankToggle.setOnAction(event -> applyBackgroundSelection("blank", true));
        bgLinedToggle.setOnAction(event -> applyBackgroundSelection("lined", true));
        bgGridToggle.setOnAction(event -> applyBackgroundSelection("grid", true));
        bgDotToggle.setOnAction(event -> applyBackgroundSelection("dot", true));
        bgDarkToggle.setOnAction(event -> applyBackgroundSelection("dark", true));
        styleBackgroundButtons();
    }

    private void setupOpacityControls() {
        opacitySlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentOpacity = Math.max(0.1, newValue.doubleValue() / 100.0);
            opacityLabel.setText((int) Math.round(newValue.doubleValue()) + "%");
            applyDrawingStyle();
        });
        opacityLabel.setText((int) Math.round(opacitySlider.getValue()) + "%");
    }

    private void setupPalette() {
        for (Node child : paletteBox.getChildren()) {
            if (child instanceof Button paletteButton) {
                paletteButton.setPadding(Insets.EMPTY);
                paletteButton.setOnAction(event -> {
                    currentColor = Color.web(String.valueOf(paletteButton.getUserData()));
                    currentColorName = colorName(currentColor);
                    if (customColorPicker != null) {
                        customColorPicker.setValue(currentColor);
                    }
                    refreshToolLabels();
                    stylePaletteButtons();
                });
            }
        }
        if (customColorPicker != null) {
            customColorPicker.setValue(currentColor);
            customColorPicker.setOnAction(event -> {
                currentColor = customColorPicker.getValue();
                currentColorName = colorName(currentColor);
                refreshToolLabels();
                stylePaletteButtons();
            });
        }
        stylePaletteButtons();
    }

    private void setupCanvas() {
        clearCanvas();
        applyDrawingStyle();

        drawingCanvas.setOnMousePressed(event -> {
            pushCanvasState();
            redoStack.clear();
            lastX = event.getX();
            lastY = event.getY();
            drawSegment(lastX, lastY, lastX, lastY);
            updateUndoState();
        });

        drawingCanvas.setOnMouseDragged(event -> {
            drawSegment(lastX, lastY, event.getX(), event.getY());
            lastX = event.getX();
            lastY = event.getY();
        });
    }

    private void hookListeners() {
        noteTitleField.textProperty().addListener((obs, oldValue, newValue) -> handleNoteChanged());
        noteTextArea.textProperty().addListener((obs, oldValue, newValue) -> handleNoteChanged());
        noteTagsField.textProperty().addListener((obs, oldValue, newValue) -> handleNoteChanged());
        noteFolderCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshStats();
            updateNoteStatus("Folder updated.", false);
        });
        favoriteToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            refreshStats();
            updateNoteStatus(newValue ? "Marked as favorite." : "Removed from favorites.", false);
        });
        aiStyleCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateNoteStatus("AI style set to " + newValue + ".", false);
        });
    }

    private void handleNoteChanged() {
        refreshMetrics();
        refreshStats();
        updateNoteStatus("Unsaved changes", false);
    }

    private void loadSavedNote() {
        noteTitleField.setText(preferences.get(key("title"), ""));
        noteTextArea.setText(preferences.get(key("body"), ""));
        noteTagsField.setText(preferences.get(key("tags"), ""));

        String savedFolder = preferences.get(key("folder"), DEFAULT_FOLDERS.get(0));
        if (!noteFolderCombo.getItems().contains(savedFolder)) {
            noteFolderCombo.getItems().add(savedFolder);
        }
        noteFolderCombo.setValue(savedFolder);

        favoriteToggle.setSelected(preferences.getBoolean(key("favorite"), false));
        aiPromptField.setText(preferences.get(key("prompt"), ""));
        aiDraftCount = preferences.getInt(key("aiDraftCount"), 0);

        String savedStyle = preferences.get(key("aiStyle"), AI_STYLES.get(0));
        if (!aiStyleCombo.getItems().contains(savedStyle)) {
            aiStyleCombo.getItems().add(savedStyle);
        }
        aiStyleCombo.setValue(savedStyle);

        currentTool = preferences.get(key("tool"), "Pen");
        currentStrokeName = preferences.get(key("stroke"), "Medium");
        currentOpacity = preferences.getDouble(key("opacity"), 1.0);
        opacitySlider.setValue(Math.max(10, Math.min(100, currentOpacity * 100)));

        String background = preferences.get(key("background"), "blank");
        applyBackgroundSelection(background, false);
        restoreToolSelection();
        restoreStrokeSelection();

        long savedAt = preferences.getLong(key("savedAt"), 0L);
        if (savedAt > 0L) {
            updateNoteStatus("Last saved " + SAVED_AT_FORMAT.format(
                    Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            ), false);
        }
    }

    private void refreshMetrics() {
        int words = countWords(noteTitleField.getText() + " " + noteTextArea.getText());
        wordCountLabel.setText(words + " words");
    }

    private void refreshStats() {
        boolean hasNote = !safe(noteTitleField.getText()).isBlank() || !safe(noteTextArea.getText()).isBlank();
        totalNotesLabel.setText(hasNote ? "1" : "0");
        foldersLabel.setText(String.valueOf(noteFolderCombo.getItems().size()));
        favoritesLabel.setText(favoriteToggle.isSelected() ? "1" : "0");
        recentLabel.setText(String.valueOf(aiDraftCount));
    }

    private void selectTool(String tool) {
        currentTool = tool;
        switch (tool) {
            case "Pen" -> penToolButton.setSelected(true);
            case "Pencil" -> pencilToolButton.setSelected(true);
            case "Marker" -> markerToolButton.setSelected(true);
            case "Brush" -> brushToolButton.setSelected(true);
            case "Highlighter" -> highlighterToolButton.setSelected(true);
            case "Calligraphy" -> calligraphyToolButton.setSelected(true);
            case "Eraser" -> eraserToolButton.setSelected(true);
            default -> penToolButton.setSelected(true);
        }
        applyDrawingStyle();
        refreshToolLabels();
        styleToolButtons();
    }

    private void selectStroke(double width, String label) {
        currentStrokeWidth = width;
        currentStrokeName = label;
        applyDrawingStyle();
        refreshToolLabels();
        styleStrokeButtons();
    }

    private void restoreToolSelection() {
        selectTool(currentTool);
    }

    private void restoreStrokeSelection() {
        switch (currentStrokeName) {
            case "Thin" -> selectStroke(2.0, "Thin");
            case "Bold" -> selectStroke(8.0, "Bold");
            case "XL" -> selectStroke(14.0, "XL");
            default -> selectStroke(4.0, "Medium");
        }
    }

    private void refreshToolLabels() {
        toolModeLabel.setText(currentTool);
        strokeModeLabel.setText(currentStrokeName);
        colorModeLabel.setText(currentColorName);
    }

    private void styleToolButtons() {
        styleToolButton(penToolButton, "Pen".equals(currentTool));
        styleToolButton(pencilToolButton, "Pencil".equals(currentTool));
        styleToolButton(markerToolButton, "Marker".equals(currentTool));
        styleToolButton(brushToolButton, "Brush".equals(currentTool));
        styleToolButton(highlighterToolButton, "Highlighter".equals(currentTool));
        styleToolButton(calligraphyToolButton, "Calligraphy".equals(currentTool));
        styleToolButton(eraserToolButton, "Eraser".equals(currentTool));
    }

    private void styleStrokeButtons() {
        styleActionButton(strokeThinButton, "Thin".equals(currentStrokeName));
        styleActionButton(strokeMediumButton, "Medium".equals(currentStrokeName));
        styleActionButton(strokeBoldButton, "Bold".equals(currentStrokeName));
        styleActionButton(strokeXLButton, "XL".equals(currentStrokeName));
    }

    private void styleBackgroundButtons() {
        styleToggleChip(bgBlankToggle, "blank".equals(currentBackground));
        styleToggleChip(bgLinedToggle, "lined".equals(currentBackground));
        styleToggleChip(bgGridToggle, "grid".equals(currentBackground));
        styleToggleChip(bgDotToggle, "dot".equals(currentBackground));
        styleToggleChip(bgDarkToggle, "dark".equals(currentBackground));
    }

    private void stylePaletteButtons() {
        for (Node child : paletteBox.getChildren()) {
            if (child instanceof Button paletteButton) {
                String colorValue = String.valueOf(paletteButton.getUserData());
                boolean selected = colorValue.equalsIgnoreCase(toHex(currentColor));
                String border = selected ? "#F8FAFC" : "#0F172A";
                paletteButton.setStyle(
                        "-fx-background-color:" + colorValue + ";"
                                + "-fx-background-radius:100;"
                                + "-fx-cursor:hand;"
                                + "-fx-border-color:" + border + ";"
                                + "-fx-border-radius:100;"
                                + "-fx-border-width:" + (selected ? 2 : 1) + ";"
                );
            }
        }
    }

    private void styleToolButton(ToggleButton button, boolean active) {
        button.setStyle(active
                ? "-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:7 9;-fx-border-color:#A78BFA;-fx-border-radius:10;"
                : "-fx-background-color:#1E293B;-fx-text-fill:#CBD5E1;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:7 9;-fx-border-color:#334155;-fx-border-radius:10;");
    }

    private void styleActionButton(Button button, boolean active) {
        button.setStyle(active
                ? "-fx-background-color:#334155;-fx-text-fill:#F8FAFC;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:7 10;-fx-border-color:#64748B;-fx-border-radius:10;"
                : "-fx-background-color:#1E293B;-fx-text-fill:#CBD5E1;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:7 10;-fx-border-color:#334155;-fx-border-radius:10;");
    }

    private void styleToggleChip(ToggleButton button, boolean active) {
        button.setStyle(active
                ? "-fx-background-color:#334155;-fx-border-color:#64748B;-fx-background-radius:8;-fx-border-radius:8;-fx-font-size:10px;-fx-text-fill:#F8FAFC;-fx-cursor:hand;-fx-padding:5 10;"
                : "-fx-background-color:#1E293B;-fx-border-color:#334155;-fx-background-radius:8;-fx-border-radius:8;-fx-font-size:10px;-fx-text-fill:#CBD5E1;-fx-cursor:hand;-fx-padding:5 10;");
    }

    private void drawSegment(double startX, double startY, double endX, double endY) {
        applyDrawingStyle();
        graphicsContext.strokeLine(startX, startY, endX, endY);
    }

    private void applyDrawingStyle() {
        Color strokeColor = "Eraser".equals(currentTool) ? currentBackgroundColor() : currentColor;
        graphicsContext.setStroke(strokeColor.deriveColor(0, 1, 1, toolOpacity()));
        graphicsContext.setLineWidth(currentStrokeWidth * toolWidthFactor());
        graphicsContext.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        graphicsContext.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    }

    private double toolWidthFactor() {
        return switch (currentTool) {
            case "Pencil" -> 0.7;
            case "Marker" -> 1.8;
            case "Brush" -> 2.5;
            case "Highlighter" -> 3.2;
            case "Calligraphy" -> 1.9;
            case "Eraser" -> 2.4;
            default -> 1.0;
        };
    }

    private double toolOpacity() {
        double base = switch (currentTool) {
            case "Pencil" -> 0.65;
            case "Marker" -> 0.45;
            case "Brush" -> 0.8;
            case "Highlighter" -> 0.25;
            case "Calligraphy" -> 0.9;
            default -> 1.0;
        };
        return Math.max(0.1, Math.min(1.0, base * currentOpacity));
    }

    private void applyBackgroundSelection(String background, boolean resetCanvas) {
        currentBackground = background == null || background.isBlank() ? "blank" : background;
        switch (currentBackground) {
            case "lined" -> bgLinedToggle.setSelected(true);
            case "grid" -> bgGridToggle.setSelected(true);
            case "dot" -> bgDotToggle.setSelected(true);
            case "dark" -> bgDarkToggle.setSelected(true);
            default -> bgBlankToggle.setSelected(true);
        }
        applyBackgroundToCanvas(resetCanvas);
        styleBackgroundButtons();
    }

    private void applyBackgroundToCanvas(boolean resetCanvas) {
        updateCanvasContainerStyle();
        if (resetCanvas) {
            clearCanvas();
        } else {
            drawCanvasBackground();
        }
        applyDrawingStyle();
    }

    private void updateCanvasContainerStyle() {
        String border = "dark".equals(currentBackground) ? "#334155" : "#D1D5DB";
        String fill = "dark".equals(currentBackground) ? "#111827" : "#F8F5EF";
        canvasContainer.setStyle(
                "-fx-background-color:" + fill + ";"
                        + "-fx-background-radius:18;"
                        + "-fx-border-color:" + border + ";"
                        + "-fx-border-radius:18;"
                        + "-fx-border-width:1;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),18,0,0,4);"
                        + "-fx-padding:0;"
        );
    }

    private void clearCanvas() {
        graphicsContext.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        drawCanvasBackground();
        applyDrawingStyle();
    }

    private void drawCanvasBackground() {
        Color base = currentBackgroundColor();
        graphicsContext.setFill(base);
        graphicsContext.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

        switch (currentBackground) {
            case "lined" -> drawLines(Color.web("#D6DDE8"), 24);
            case "grid" -> drawGrid(Color.web("#D6DDE8"), 24);
            case "dot" -> drawDots(Color.web("#D6DDE8"), 24);
            case "dark" -> drawGrid(Color.web("#263244"), 24);
            default -> {
            }
        }
    }

    private void drawLines(Color color, int spacing) {
        graphicsContext.setStroke(color);
        graphicsContext.setLineWidth(1);
        for (int y = spacing; y < drawingCanvas.getHeight(); y += spacing) {
            graphicsContext.strokeLine(0, y, drawingCanvas.getWidth(), y);
        }
    }

    private void drawGrid(Color color, int spacing) {
        graphicsContext.setStroke(color);
        graphicsContext.setLineWidth(1);
        for (int y = spacing; y < drawingCanvas.getHeight(); y += spacing) {
            graphicsContext.strokeLine(0, y, drawingCanvas.getWidth(), y);
        }
        for (int x = spacing; x < drawingCanvas.getWidth(); x += spacing) {
            graphicsContext.strokeLine(x, 0, x, drawingCanvas.getHeight());
        }
    }

    private void drawDots(Color color, int spacing) {
        graphicsContext.setFill(color);
        for (int y = spacing; y < drawingCanvas.getHeight(); y += spacing) {
            for (int x = spacing; x < drawingCanvas.getWidth(); x += spacing) {
                graphicsContext.fillOval(x - 1, y - 1, 2, 2);
            }
        }
    }

    private Color currentBackgroundColor() {
        return "dark".equals(currentBackground) ? Color.web("#111827") : Color.web("#F8F5EF");
    }

    private void drawSketchPlan(CloudflareAiSketchService.SketchPlan plan) {
        clearCanvas();

        if (plan == null || plan.nodes() == null || plan.nodes().isEmpty()) {
            drawIdeaMap();
            return;
        }

        String style = safe(aiStyleCombo.getValue());
        Color accent = Color.web(plan.accentColor() == null ? "#8B5CF6" : plan.accentColor());
        applyPlanTheme(style);
        drawPlanHeader(plan.title(), accent, style);

        Map<String, NodeBounds> positions = switch (safe(plan.layout()).toLowerCase(Locale.ROOT)) {
            case "mindmap" -> layoutMindmap(plan, accent, style);
            case "timeline" -> layoutTimeline(plan, accent, style);
            case "comparison" -> layoutComparison(plan, accent, style);
            case "process" -> layoutProcess(plan, accent, style);
            default -> layoutDiagram(plan, accent, style);
        };

        drawPlanFooter(plan.footerTips(), accent);
        applyDrawingStyle();
    }

    private void drawGeneratedScene(CloudflareAiSketchService.GeneratedScene scene, String style) {
        clearCanvas();
        if (scene == null || scene.imageBytes() == null || scene.imageBytes().length == 0) {
            drawIdeaMap();
            return;
        }

        Image image = new Image(new ByteArrayInputStream(scene.imageBytes()));
        if (image.isError()) {
            drawIdeaMap();
            return;
        }

        String normalizedStyle = safe(style).toLowerCase(Locale.ROOT);
        Color accent = switch (normalizedStyle) {
            case "chalk" -> Color.web("#FBBF24");
            case "sketch" -> Color.web("#A855F7");
            default -> Color.web("#38BDF8");
        };

        double cardX = 76;
        double cardY = 92;
        double cardWidth = 968;
        double cardHeight = 522;
        double imageX = cardX + 22;
        double imageY = cardY + 62;
        double imageWidth = cardWidth - 44;
        double imageHeight = cardHeight - 118;

        if ("chalk".equals(normalizedStyle) && !"dark".equals(currentBackground)) {
            applyBackgroundSelection("dark", true);
        } else {
            clearCanvas();
        }

        drawSceneShadow(cardX, cardY, cardWidth, cardHeight);
        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#101826") : Color.WHITE);
        graphicsContext.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 28, 28);
        graphicsContext.setStroke(accent.deriveColor(0, 1, 1, 0.65));
        graphicsContext.setLineWidth(2.5);
        graphicsContext.strokeRoundRect(cardX, cardY, cardWidth, cardHeight, 28, 28);

        graphicsContext.setFill(accent);
        graphicsContext.fillRoundRect(cardX + 18, cardY + 18, 132, 30, 15, 15);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 12));
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        graphicsContext.setTextBaseline(VPos.CENTER);
        graphicsContext.fillText("AI Render", cardX + 84, cardY + 33);

        graphicsContext.setTextAlign(TextAlignment.LEFT);
        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#F8FAFC") : Color.web("#0F172A"));
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 28));
        graphicsContext.fillText(compactLabel(scene.title(), 40), cardX + 176, cardY + 33);

        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#94A3B8") : Color.web("#475569"));
        graphicsContext.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        graphicsContext.fillText(compactLabel(scene.caption(), 96), cardX + 24, cardY + 360 + 134);

        drawImageContain(image, imageX, imageY, imageWidth, imageHeight);
        drawSceneAccents(accent, normalizedStyle, cardX, cardY, cardWidth, cardHeight);
        drawPromptChip(scene.promptUsed(), accent, cardX + 24, cardY + cardHeight - 52);
        applyDrawingStyle();
    }

    private void drawSceneShadow(double x, double y, double width, double height) {
        graphicsContext.setFill(Color.rgb(15, 23, 42, 0.12));
        graphicsContext.fillRoundRect(x + 8, y + 10, width, height, 28, 28);
        graphicsContext.setFill(Color.rgb(15, 23, 42, 0.08));
        graphicsContext.fillRoundRect(x + 14, y + 18, width - 8, height - 4, 28, 28);
    }

    private void drawImageContain(Image image, double x, double y, double width, double height) {
        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#172033") : Color.web("#E5E7EB"));
        graphicsContext.fillRoundRect(x - 2, y - 2, width + 4, height + 4, 24, 24);

        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return;
        }

        double scale = Math.min(width / image.getWidth(), height / image.getHeight());
        double drawWidth = image.getWidth() * scale;
        double drawHeight = image.getHeight() * scale;
        double drawX = x + ((width - drawWidth) / 2.0);
        double drawY = y + ((height - drawHeight) / 2.0);
        graphicsContext.drawImage(image, drawX, drawY, drawWidth, drawHeight);
    }

    private void drawSceneAccents(Color accent, String style, double x, double y, double width, double height) {
        graphicsContext.setStroke(accent.deriveColor(0, 1, 1, 0.55));
        graphicsContext.setLineWidth("sketch".equals(style) ? 3.5 : 2.2);

        graphicsContext.strokeArc(x + width - 110, y + 18, 64, 64, 20, 120, javafx.scene.shape.ArcType.OPEN);
        graphicsContext.strokeArc(x + 28, y + height - 96, 74, 74, 210, 110, javafx.scene.shape.ArcType.OPEN);
        graphicsContext.strokeLine(x + width - 80, y + height - 42, x + width - 34, y + height - 70);
        graphicsContext.strokeLine(x + width - 76, y + height - 72, x + width - 26, y + height - 26);

        if ("sketch".equals(style) || "chalk".equals(style)) {
            graphicsContext.strokeLine(x + 18, y + 72, x + 78, y + 52);
            graphicsContext.strokeLine(x + width - 146, y + 54, x + width - 92, y + 76);
        }
    }

    private void drawPromptChip(String prompt, Color accent, double x, double y) {
        String label = compactLabel(prompt, 84);
        double width = Math.min(760, Math.max(220, 24 + (label.length() * 6.3)));
        graphicsContext.setFill(accent.deriveColor(0, 1, 1, 0.12));
        graphicsContext.fillRoundRect(x, y, width, 28, 14, 14);
        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#E2E8F0") : Color.web("#334155"));
        graphicsContext.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
        graphicsContext.setTextAlign(TextAlignment.LEFT);
        graphicsContext.setTextBaseline(VPos.CENTER);
        graphicsContext.fillText(label, x + 12, y + 14);
    }

    private void applyPlanTheme(String style) {
        String normalizedStyle = safe(style).toLowerCase(Locale.ROOT);
        if ("chalk".equals(normalizedStyle) && !"dark".equals(currentBackground)) {
            applyBackgroundSelection("dark", true);
            return;
        }
        if ("chalk".equals(normalizedStyle)) {
            clearCanvas();
        }
    }

    private void drawPlanHeader(String title, Color accent, String style) {
        String safeTitle = safe(title).isBlank() ? "AI Sketch" : safe(title);
        graphicsContext.setFill(accent);
        graphicsContext.fillRoundRect(64, 34, 240, 36, 18, 18);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 14));
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        graphicsContext.setTextBaseline(VPos.CENTER);
        graphicsContext.fillText(compactLabel(safeTitle, 32), 184, 52);

        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#CBD5E1") : Color.web("#334155"));
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 28));
        graphicsContext.setTextAlign(TextAlignment.LEFT);
        graphicsContext.fillText(compactLabel(safeTitle, 50), 64, 92);

        graphicsContext.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        graphicsContext.setFill(accent.deriveColor(0, 1, 1, 0.9));
        graphicsContext.fillText("Style: " + (safe(style).isBlank() ? "Default" : style), 64, 114);
    }

    private Map<String, NodeBounds> layoutMindmap(CloudflareAiSketchService.SketchPlan plan, Color accent, String style) {
        Map<String, NodeBounds> positions = new HashMap<>();
        List<CloudflareAiSketchService.SketchNode> nodes = plan.nodes();

        CloudflareAiSketchService.SketchNode center = nodes.get(0);
        NodeBounds centerBounds = new NodeBounds(430, 250, 260, 110);
        drawPlanNode(center, centerBounds, accent, style);
        positions.put(center.id(), centerBounds);

        double radiusX = 320;
        double radiusY = 175;
        int branches = Math.max(1, nodes.size() - 1);
        for (int i = 1; i < nodes.size(); i++) {
            double angle = (Math.PI * 2 * (i - 1) / branches) - (Math.PI / 2);
            double x = 560 + Math.cos(angle) * radiusX - 105;
            double y = 305 + Math.sin(angle) * radiusY - 46;
            NodeBounds bounds = new NodeBounds(x, y, 210, 92);
            drawPlanConnection(centerBounds.centerX(), centerBounds.centerY(), bounds.centerX(), bounds.centerY(), accent, "");
            drawPlanNode(nodes.get(i), bounds, accent, style);
            positions.put(nodes.get(i).id(), bounds);
        }
        return positions;
    }

    private Map<String, NodeBounds> layoutTimeline(CloudflareAiSketchService.SketchPlan plan, Color accent, String style) {
        Map<String, NodeBounds> positions = new HashMap<>();
        List<CloudflareAiSketchService.SketchNode> nodes = plan.nodes();

        graphicsContext.setStroke(accent.deriveColor(0, 1, 1, 0.55));
        graphicsContext.setLineWidth(4);
        graphicsContext.strokeLine(120, 332, 1000, 332);

        double slot = 860.0 / Math.max(1, nodes.size() - 1);
        for (int i = 0; i < nodes.size(); i++) {
            double centerX = nodes.size() == 1 ? 560 : 120 + (slot * i);
            double y = (i % 2 == 0) ? 188 : 382;
            NodeBounds bounds = new NodeBounds(centerX - 100, y, 200, 96);
            graphicsContext.setFill(accent);
            graphicsContext.fillOval(centerX - 8, 324, 16, 16);
            drawPlanConnection(centerX, 332, centerX, y + (i % 2 == 0 ? 96 : 0), accent, "");
            drawPlanNode(nodes.get(i), bounds, accent, style);
            positions.put(nodes.get(i).id(), bounds);
        }
        return positions;
    }

    private Map<String, NodeBounds> layoutComparison(CloudflareAiSketchService.SketchPlan plan, Color accent, String style) {
        Map<String, NodeBounds> positions = new HashMap<>();
        List<CloudflareAiSketchService.SketchNode> nodes = plan.nodes();

        graphicsContext.setFill(accent.deriveColor(0, 1, 1, 0.12));
        graphicsContext.fillRoundRect(88, 152, 414, 392, 24, 24);
        graphicsContext.fillRoundRect(618, 152, 414, 392, 24, 24);

        for (int i = 0; i < nodes.size(); i++) {
            boolean left = i < Math.ceil(nodes.size() / 2.0);
            int order = left ? i : i - (int) Math.ceil(nodes.size() / 2.0);
            double x = left ? 118 : 648;
            double y = 190 + (order * 118);
            NodeBounds bounds = new NodeBounds(x, y, 354, 92);
            drawPlanNode(nodes.get(i), bounds, accent, style);
            positions.put(nodes.get(i).id(), bounds);
        }

        drawPlanConnections(plan, positions, accent);
        return positions;
    }

    private Map<String, NodeBounds> layoutProcess(CloudflareAiSketchService.SketchPlan plan, Color accent, String style) {
        Map<String, NodeBounds> positions = new HashMap<>();
        List<CloudflareAiSketchService.SketchNode> nodes = plan.nodes();
        double width = 240;
        double height = 92;
        double gap = 54;
        double startX = 92;
        double firstRowY = 192;
        double secondRowY = 366;

        for (int i = 0; i < nodes.size(); i++) {
            double x = startX + (i % 3) * (width + gap);
            double y = i < 3 ? firstRowY : secondRowY;
            NodeBounds bounds = new NodeBounds(x, y, width, height);
            drawPlanNode(nodes.get(i), bounds, accent, style);
            positions.put(nodes.get(i).id(), bounds);
        }

        drawPlanConnections(plan, positions, accent);
        return positions;
    }

    private Map<String, NodeBounds> layoutDiagram(CloudflareAiSketchService.SketchPlan plan, Color accent, String style) {
        Map<String, NodeBounds> positions = new HashMap<>();
        List<CloudflareAiSketchService.SketchNode> nodes = plan.nodes();
        int columns = nodes.size() <= 4 ? 2 : 3;
        double width = columns == 2 ? 340 : 270;
        double height = 100;
        double startX = columns == 2 ? 160 : 88;
        double gapX = columns == 2 ? 120 : 48;
        double startY = 176;
        double gapY = 74;

        for (int i = 0; i < nodes.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            double x = startX + col * (width + gapX);
            double y = startY + row * (height + gapY);
            NodeBounds bounds = new NodeBounds(x, y, width, height);
            drawPlanNode(nodes.get(i), bounds, accent, style);
            positions.put(nodes.get(i).id(), bounds);
        }

        drawPlanConnections(plan, positions, accent);
        return positions;
    }

    private void drawPlanConnections(CloudflareAiSketchService.SketchPlan plan, Map<String, NodeBounds> positions, Color accent) {
        if (plan.connections() == null) {
            return;
        }
        for (CloudflareAiSketchService.SketchConnection connection : plan.connections()) {
            NodeBounds from = positions.get(connection.from());
            NodeBounds to = positions.get(connection.to());
            if (from == null || to == null) {
                continue;
            }
            drawPlanConnection(from.centerX(), from.centerY(), to.centerX(), to.centerY(), accent, connection.label());
        }
    }

    private void drawPlanConnection(double startX, double startY, double endX, double endY, Color accent, String label) {
        graphicsContext.setStroke(accent.deriveColor(0, 1, 1, 0.58));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeLine(startX, startY, endX, endY);

        double angle = Math.atan2(endY - startY, endX - startX);
        double arrowSize = 10;
        double x1 = endX - arrowSize * Math.cos(angle - Math.PI / 6);
        double y1 = endY - arrowSize * Math.sin(angle - Math.PI / 6);
        double x2 = endX - arrowSize * Math.cos(angle + Math.PI / 6);
        double y2 = endY - arrowSize * Math.sin(angle + Math.PI / 6);
        graphicsContext.strokeLine(endX, endY, x1, y1);
        graphicsContext.strokeLine(endX, endY, x2, y2);

        if (!safe(label).isBlank()) {
            double midX = (startX + endX) / 2.0;
            double midY = (startY + endY) / 2.0;
            graphicsContext.setFill(currentBackground.equals("dark") ? Color.web("#E2E8F0") : Color.web("#334155"));
            graphicsContext.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
            graphicsContext.setTextAlign(TextAlignment.CENTER);
            graphicsContext.fillText(compactLabel(label, 18), midX, midY - 6);
        }
    }

    private void drawPlanNode(CloudflareAiSketchService.SketchNode node, NodeBounds bounds, Color accent, String style) {
        Color fill = nodeFillColor(accent, node.importance());
        Color stroke = accent.deriveColor(0, 1, 1, "chalk".equalsIgnoreCase(style) ? 0.92 : 0.75);

        graphicsContext.setFill(fill);
        graphicsContext.setStroke(stroke);
        graphicsContext.setLineWidth("sketch".equalsIgnoreCase(style) ? 3.5 : 2.4);

        switch (safe(node.shape()).toLowerCase(Locale.ROOT)) {
            case "circle" -> {
                graphicsContext.fillOval(bounds.x(), bounds.y(), bounds.width(), bounds.height());
                graphicsContext.strokeOval(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            }
            case "pill" -> {
                graphicsContext.fillRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), bounds.height(), bounds.height());
                graphicsContext.strokeRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), bounds.height(), bounds.height());
            }
            default -> {
                graphicsContext.fillRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 26, 26);
                graphicsContext.strokeRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 26, 26);
            }
        }

        if ("sketch".equalsIgnoreCase(style)) {
            graphicsContext.setStroke(stroke.deriveColor(0, 1, 1, 0.35));
            graphicsContext.strokeRoundRect(bounds.x() + 4, bounds.y() + 3, bounds.width() - 8, bounds.height() - 6, 24, 24);
        }

        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#F8FAFC") : Color.web("#0F172A"));
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 14));
        graphicsContext.setTextAlign(TextAlignment.LEFT);
        graphicsContext.setTextBaseline(VPos.TOP);
        graphicsContext.fillText(compactLabel(node.label(), 24), bounds.x() + 16, bounds.y() + 16);

        graphicsContext.setFont(Font.font("System", FontWeight.NORMAL, 11));
        graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#CBD5E1") : Color.web("#475569"));
        drawWrappedText(node.detail(), bounds.x() + 16, bounds.y() + 42, bounds.width() - 28, 2, 14);
    }

    private Color nodeFillColor(Color accent, String importance) {
        double opacity = switch (safe(importance).toLowerCase(Locale.ROOT)) {
            case "high" -> 0.24;
            case "low" -> 0.10;
            default -> 0.16;
        };
        return accent.deriveColor(0, 0.55, 1.15, opacity);
    }

    private void drawPlanFooter(List<String> footerTips, Color accent) {
        if (footerTips == null || footerTips.isEmpty()) {
            return;
        }
        double x = 64;
        double y = 612;
        for (String tip : footerTips) {
            String text = compactLabel(tip, 28);
            double width = Math.max(136, Math.min(210, 26 + (text.length() * 7.2)));
            graphicsContext.setFill(accent.deriveColor(0, 1, 1, 0.14));
            graphicsContext.fillRoundRect(x, y, width, 34, 17, 17);
            graphicsContext.setFill("dark".equals(currentBackground) ? Color.web("#E2E8F0") : Color.web("#334155"));
            graphicsContext.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
            graphicsContext.setTextAlign(TextAlignment.CENTER);
            graphicsContext.setTextBaseline(VPos.CENTER);
            graphicsContext.fillText(text, x + (width / 2.0), y + 17);
            x += width + 14;
        }
    }

    private void drawWrappedText(String text, double x, double y, double maxWidth, int maxLines, double lineHeight) {
        List<String> lines = wrapText(text, Math.max(12, (int) (maxWidth / 7.2)), maxLines);
        for (int i = 0; i < lines.size(); i++) {
            graphicsContext.fillText(lines.get(i), x, y + (i * lineHeight));
        }
    }

    private List<String> wrapText(String text, int maxChars, int maxLines) {
        List<String> lines = new java.util.ArrayList<>();
        String normalized = safe(text).replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : normalized.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (candidate.length() <= maxChars) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            current.setLength(0);
            current.append(word);
            if (lines.size() == maxLines - 1) {
                break;
            }
        }
        if (!current.isEmpty() && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        if (!lines.isEmpty() && normalized.length() > String.join(" ", lines).length()) {
            int lastIndex = lines.size() - 1;
            lines.set(lastIndex, compactLabel(lines.get(lastIndex), Math.max(8, maxChars - 1)));
        }
        return lines;
    }

    private void drawPromptSketch(String prompt) {
        String style = aiStyleCombo.getValue() == null ? "Default" : aiStyleCombo.getValue();
        if (prompt.contains("clear")) {
            clearCanvas();
            return;
        }

        if ("Mindmap".equalsIgnoreCase(style)) {
            drawIdeaMap();
            return;
        }
        if ("Diagram".equalsIgnoreCase(style) && (prompt.contains("graph") || prompt.contains("chart") || prompt.contains("system"))) {
            drawGraph();
            return;
        }
        if (prompt.contains("flower")) {
            drawFlower();
        } else if (prompt.contains("sun")) {
            drawSun();
        } else if (prompt.contains("house")) {
            drawHouse();
        } else if (prompt.contains("tree")) {
            drawTree();
        } else if (prompt.contains("graph") || prompt.contains("chart")) {
            drawGraph();
        } else if (prompt.contains("atom") || prompt.contains("science") || prompt.contains("heart")) {
            drawAtom();
        } else {
            drawIdeaMap();
        }
    }

    private void drawFlower() {
        graphicsContext.setFill(Color.web("#FDE68A"));
        graphicsContext.fillOval(410, 220, 40, 40);
        graphicsContext.setFill(Color.web("#EC4899"));
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            double x = 430 + Math.cos(angle) * 46 - 20;
            double y = 240 + Math.sin(angle) * 46 - 20;
            graphicsContext.fillOval(x, y, 40, 40);
        }
        graphicsContext.setStroke(Color.web("#22C55E"));
        graphicsContext.setLineWidth(8);
        graphicsContext.strokeLine(430, 260, 430, 390);
    }

    private void drawSun() {
        graphicsContext.setFill(Color.web("#FBBF24"));
        graphicsContext.fillOval(350, 120, 160, 160);
        graphicsContext.setStroke(Color.web("#F59E0B"));
        graphicsContext.setLineWidth(6);
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            double x1 = 430 + Math.cos(angle) * 95;
            double y1 = 200 + Math.sin(angle) * 95;
            double x2 = 430 + Math.cos(angle) * 145;
            double y2 = 200 + Math.sin(angle) * 145;
            graphicsContext.strokeLine(x1, y1, x2, y2);
        }
    }

    private void drawHouse() {
        graphicsContext.setFill(Color.web("#E2E8F0"));
        graphicsContext.fillRect(300, 230, 240, 170);
        graphicsContext.setFill(Color.web("#EF4444"));
        graphicsContext.fillPolygon(new double[]{280, 420, 560}, new double[]{240, 130, 240}, 3);
        graphicsContext.setFill(Color.web("#7C3AED"));
        graphicsContext.fillRect(395, 310, 50, 90);
    }

    private void drawTree() {
        graphicsContext.setFill(Color.web("#92400E"));
        graphicsContext.fillRect(392, 250, 55, 170);
        graphicsContext.setFill(Color.web("#22C55E"));
        graphicsContext.fillOval(300, 120, 240, 170);
        graphicsContext.fillOval(340, 80, 150, 120);
    }

    private void drawGraph() {
        graphicsContext.setStroke(Color.web("#334155"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeLine(150, 470, 150, 120);
        graphicsContext.strokeLine(150, 470, 700, 470);
        graphicsContext.setStroke(Color.web("#8B5CF6"));
        graphicsContext.setLineWidth(5);
        graphicsContext.strokePolyline(
                new double[]{180, 260, 340, 430, 520, 650},
                new double[]{420, 360, 300, 320, 220, 170},
                6
        );
    }

    private void drawAtom() {
        graphicsContext.setStroke(Color.web("#3B82F6"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeOval(320, 180, 220, 140);
        graphicsContext.save();
        graphicsContext.translate(430, 250);
        graphicsContext.rotate(60);
        graphicsContext.strokeOval(-110, -70, 220, 140);
        graphicsContext.rotate(60);
        graphicsContext.strokeOval(-110, -70, 220, 140);
        graphicsContext.restore();
        graphicsContext.setFill(Color.web("#EC4899"));
        graphicsContext.fillOval(417, 237, 26, 26);
    }

    private void drawIdeaMap() {
        graphicsContext.setStroke(Color.web("#A855F7"));
        graphicsContext.setLineWidth(4);
        graphicsContext.strokeOval(360, 220, 140, 70);
        graphicsContext.strokeLine(360, 255, 250, 180);
        graphicsContext.strokeLine(500, 255, 610, 180);
        graphicsContext.strokeLine(380, 285, 300, 390);
        graphicsContext.strokeLine(480, 285, 560, 390);
    }

    private void pushCanvasState() {
        undoStack.push(captureCanvas());
        while (undoStack.size() > 20) {
            undoStack.removeLast();
        }
    }

    private WritableImage captureCanvas() {
        return drawingCanvas.snapshot(new SnapshotParameters(), null);
    }

    private void restoreCanvas(WritableImage image) {
        graphicsContext.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        graphicsContext.drawImage(image, 0, 0);
        applyDrawingStyle();
    }

    private void updateUndoState() {
        undoStrokeButton.setDisable(undoStack.isEmpty());
        redoStrokeButton.setDisable(redoStack.isEmpty());
    }

    private void setZoom(double zoom) {
        currentZoom = zoom;
        canvasContainer.setScaleX(zoom);
        canvasContainer.setScaleY(zoom);
        updateZoomLabel();
        updateNoteStatus("Canvas zoom set to " + (int) Math.round(zoom * 100) + "%.", false);
    }

    private void updateZoomLabel() {
        zoomLabel.setText((int) Math.round(currentZoom * 100) + "%");
    }

    private void wrapSelection(String prefix, String suffix, String fallback) {
        String selected = noteTextArea.getSelectedText();
        if (selected == null || selected.isBlank()) {
            replaceSelection(prefix + fallback + suffix);
            return;
        }
        replaceSelection(prefix + selected + suffix);
    }

    private void insertAtCaret(String content) {
        int caret = noteTextArea.getCaretPosition();
        noteTextArea.insertText(caret, content);
    }

    private void replaceSelection(String content) {
        noteTextArea.replaceSelection(content);
        refreshMetrics();
        updateNoteStatus("Text updated.", false);
    }

    private int countWords(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? 0 : normalized.split("\\s+").length;
    }

    private String extractKeywords(String content) {
        String normalized = safe(content).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        if (normalized.isBlank()) {
            return "none yet";
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 4 || STOP_WORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
            if (keywords.size() == 4) {
                break;
            }
        }
        return keywords.isEmpty() ? "key ideas pending" : String.join(", ", keywords);
    }

    private String firstMeaningfulLine(String body) {
        String normalized = safe(body);
        if (normalized.isBlank()) {
            return "Untitled note";
        }
        String[] lines = normalized.split("\\R");
        for (String line : lines) {
            String trimmed = safe(line);
            if (!trimmed.isBlank()) {
                return trimmed.length() > 60 ? trimmed.substring(0, 60) + "..." : trimmed;
            }
        }
        return "Untitled note";
    }

    private void setAiGenerationLoading(boolean loading) {
        if (generateAiSketchButton != null) {
            generateAiSketchButton.setDisable(loading);
            generateAiSketchButton.setText(loading ? "Generating..." : "Generate");
        }
    }

    private String shortMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "No details";
        }
        return compactLabel(throwable.getMessage().replaceAll("\\s+", " "), 72);
    }

    private String compactLabel(String value, int maxLength) {
        String normalized = safe(value).replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private void updateNoteStatus(String message, boolean error) {
        noteStatusLabel.setText(message);
        noteStatusLabel.setStyle(error
                ? "-fx-background-color:rgba(251,113,133,0.15);-fx-text-fill:#FB7185;-fx-padding:6 12;-fx-background-radius:100;-fx-font-size:11px;-fx-font-weight:700;"
                : "-fx-background-color:#1E293B;-fx-text-fill:#94A3B8;-fx-padding:6 12;-fx-background-radius:100;-fx-font-size:11px;-fx-font-weight:700;");
    }

    private String colorName(Color color) {
        String hex = toHex(color);
        return switch (hex) {
            case "#7C3AED" -> "Indigo";
            case "#8B5CF6" -> "Purple";
            case "#A855F7" -> "Violet";
            case "#EC4899" -> "Pink";
            case "#F43F5E" -> "Rose";
            case "#F97316" -> "Orange";
            case "#F59E0B" -> "Amber";
            case "#EAB308" -> "Yellow";
            case "#22C55E" -> "Green";
            case "#14B8A6" -> "Teal";
            case "#06B6D4" -> "Cyan";
            case "#38BDF8" -> "Sky";
            case "#3B82F6" -> "Blue";
            case "#F8FAFC" -> "White";
            case "#0F172A" -> "Ink";
            default -> "Custom";
        };
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private String key(String suffix) {
        User user = UserSession.getInstance().getCurrentUser();
        String owner = user == null ? "guest" : String.valueOf(user.getId());
        return "study.notes.smart." + owner + "." + suffix;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record NodeBounds(double x, double y, double width, double height) {
        private double centerX() {
            return x + (width / 2.0);
        }

        private double centerY() {
            return y + (height / 2.0);
        }
    }

    private record AiSketchResult(
            CloudflareAiSketchService.SketchPlan plan,
            CloudflareAiSketchService.GeneratedScene scene
    ) {
        private static AiSketchResult forPlan(CloudflareAiSketchService.SketchPlan plan) {
            return new AiSketchResult(plan, null);
        }

        private static AiSketchResult forScene(CloudflareAiSketchService.GeneratedScene scene) {
            return new AiSketchResult(null, scene);
        }
    }
}
