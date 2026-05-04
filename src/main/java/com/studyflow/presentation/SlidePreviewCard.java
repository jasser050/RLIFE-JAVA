package com.studyflow.presentation;

import com.studyflow.models.SlideContent;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * SlidePreviewCard — thumbnail card in the slide strip.
 * Supports drag-and-drop reordering.
 */
public class SlidePreviewCard extends VBox {

    public static final DataFormat DRAG_FORMAT =
            new DataFormat("application/x-studyflow-slide-index");

    private static final String ACCENT  = "#7F77DD";
    private static final String CARD    = "#1C2333";
    private static final String SURFACE = "#161B22";
    private static final String TEXT_PRI = "#F0F6FC";
    private static final String TEXT_SEC = "#8B949E";
    private static final String TEXT_MUT = "#484F58";
    private static final String DANGER  = "#F85149";

    private final int index;
    private final SlideContent slideContent;
    private final String theme;
    private boolean isActive;

    public SlidePreviewCard(
            SlideContent slideContent,
            int index,
            String theme,
            boolean isActive,
            Consumer<SlideContent> previewHandler,
            BiConsumer<Integer, Integer> reorderHandler
    ) {
        this.slideContent = slideContent;
        this.index = index;
        this.theme = theme == null ? "Modern" : theme;
        this.isActive = isActive;

        setSpacing(0);
        setPadding(new Insets(10));
        setPrefWidth(190);
        setMinWidth(190);
        setMaxWidth(190);
        setAlignment(Pos.TOP_LEFT);
        setCursor(Cursor.HAND);
        setStyle(cardStyle(isActive));

        getChildren().addAll(
                buildHeader(),
                buildMiniCanvas(),
                buildFooter()
        );

        // Click to preview/select
        setOnMouseClicked(e -> previewHandler.accept(this.slideContent));

        // Hover effects
        setOnMouseEntered(e -> {
            if (!this.isActive) setStyle(cardStyle(true));
            animateScale(1.02);
        });
        setOnMouseExited(e -> {
            if (!this.isActive) setStyle(cardStyle(false));
            animateScale(1.0);
        });
        setOnMousePressed(e -> animateScale(0.98));
        setOnMouseReleased(e -> animateScale(1.0));

        // Drag-and-drop
        setupDragAndDrop(reorderHandler);
    }

    // ── Legacy constructor (backwards compat) ─────────────────────────
    public SlidePreviewCard(
            SlideContent slideContent,
            int index,
            String theme,
            Consumer<SlideContent> previewHandler,
            BiConsumer<Integer, Integer> reorderHandler
    ) {
        this(slideContent, index, theme, false, previewHandler, reorderHandler);
    }

    // ──────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        // Slide number
        Label numLabel = new Label("Slide " + slideContent.getSlideNumber());
        numLabel.setStyle("-fx-text-fill:" + (isActive ? ACCENT : TEXT_MUT) + ";"
                + "-fx-font-size:10px;-fx-font-weight:800;");

        // Type chip
        Label typeChip = new Label(slideContent.getType().name().toLowerCase(Locale.ROOT));
        typeChip.setStyle("-fx-background-color:rgba(127,119,221,0.14);-fx-text-fill:" + ACCENT + ";"
                + "-fx-background-radius:999;-fx-padding:2 7;-fx-font-size:9px;-fx-font-weight:700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(6, numLabel, spacer, typeChip);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 8, 0));
        return row;
    }

    private StackPane buildMiniCanvas() {
        StackPane canvas = new StackPane();
        canvas.setPrefSize(168, 94);
        canvas.setMinSize(168, 94);
        canvas.setMaxSize(168, 94);
        canvas.setStyle("-fx-background-color:" + miniCanvasColor() + ";"
                + "-fx-background-radius:10;");

        // Accent band
        Rectangle band = new Rectangle(0, 0, 168, 22);
        band.setFill(Color.web(accentColor(), 0.80));
        band.setArcWidth(20);
        band.setArcHeight(20);

        // Title text
        Label titleLabel = new Label(compact(slideContent.getTitle(), 26));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(150);
        titleLabel.setStyle("-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:800;"
                + "-fx-text-alignment:center;");
        titleLabel.setAlignment(Pos.CENTER);

        // Bullet dots
        HBox dots = new HBox(4);
        dots.setAlignment(Pos.CENTER);
        int dotCount = Math.min(slideContent.getBulletPoints().size(), 5);
        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(3);
            dot.setFill(Color.web("#FFFFFF", 0.45));
            dots.getChildren().add(dot);
        }

        VBox content = new VBox(6, titleLabel, dots);
        content.setAlignment(Pos.CENTER);
        content.setTranslateY(6);

        canvas.getChildren().addAll(band, content);
        StackPane.setAlignment(band, Pos.TOP_LEFT);

        return canvas;
    }

    private HBox buildFooter() {
        int bulletCount = slideContent.getBulletPoints().size();
        Label info = new Label(bulletCount + " point" + (bulletCount != 1 ? "s" : ""));
        info.setStyle("-fx-text-fill:" + TEXT_MUT + ";-fx-font-size:9px;-fx-font-weight:600;");

        // Truncated title
        Label titleLabel = new Label(compact(slideContent.getTitle(), 24));
        titleLabel.setWrapText(false);
        titleLabel.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:10px;-fx-font-weight:700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(4, titleLabel, spacer, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

    private void setupDragAndDrop(BiConsumer<Integer, Integer> reorderHandler) {
        setOnDragDetected(e -> {
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.put(DRAG_FORMAT, String.valueOf(index));
            db.setContent(cc);
            setOpacity(0.4);
            e.consume();
        });

        setOnDragDone(e -> {
            setOpacity(1.0);
            setStyle(cardStyle(isActive));
            e.consume();
        });

        setOnDragOver(e -> {
            if (e.getDragboard().hasContent(DRAG_FORMAT) && e.getGestureSource() != this) {
                e.acceptTransferModes(TransferMode.MOVE);
                setStyle(cardStyle(true));
            }
            e.consume();
        });

        setOnDragExited(e -> {
            if (e.getGestureSource() != this) setStyle(cardStyle(isActive));
            e.consume();
        });

        setOnDragDropped(e -> {
            boolean ok = false;
            Dragboard db = e.getDragboard();
            if (db.hasContent(DRAG_FORMAT)) {
                int src = Integer.parseInt(String.valueOf(db.getContent(DRAG_FORMAT)));
                reorderHandler.accept(src, index);
                ok = true;
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    // ──────────────────────────────────────────────────────────────────

    private void animateScale(double target) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), this);
        st.setToX(target);
        st.setToY(target);
        st.play();
    }

    private String cardStyle(boolean active) {
        return "-fx-background-color:" + (active ? "#252F45" : CARD) + ";"
                + "-fx-background-radius:14;-fx-border-radius:14;"
                + "-fx-border-width:1.5;"
                + "-fx-border-color:" + (active ? ACCENT : "rgba(127,119,221,0.18)") + ";"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0," + (active ? "0.45" : "0.25") + "),12,0,0,4);";
    }

    private String miniCanvasColor() {
        return switch (theme.toLowerCase(Locale.ROOT)) {
            case "minimal"   -> "#E2E8F0";
            case "bold"      -> "#1A1A2E";
            case "academic"  -> "#0F172A";
            case "creative"  -> "#170E2D";
            default          -> "#111827";
        };
    }

    private String accentColor() {
        return switch (theme.toLowerCase(Locale.ROOT)) {
            case "minimal"   -> "#7F77DD";
            case "bold"      -> "#F59E0B";
            case "academic"  -> "#1B3A5C";
            case "creative"  -> "#EC4899";
            default          -> "#7F77DD";
        };
    }

    private String compact(String v, int max) {
        String n = v == null ? "" : v.replaceAll("\\s+", " ").trim();
        return n.length() <= max ? n : n.substring(0, Math.max(0, max - 1)).trim() + "…";
    }
}