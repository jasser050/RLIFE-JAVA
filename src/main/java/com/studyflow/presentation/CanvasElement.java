package com.studyflow.presentation;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.*;

/**
 * CanvasElement — A draggable, selectable, editable element on the slide canvas.
 *
 * Supports: Text, Rectangle, Circle, Line, Image, Icon.
 *
 * NEW in this version:
 *  - fontFamily (any system font)
 *  - italic, underline, strikethrough
 *  - textAlignment (LEFT, CENTER, RIGHT)
 *  - letterSpacing (via inline CSS)
 *  - lineHeight
 *  - backgroundFill + backgroundRadius for text boxes
 *  - borderRadius for rect/circle
 *  - strokeWidth (configurable)
 *  - shadow (drop shadow with color, radius, offset)
 *  - glowEffect
 *  - rotationAngle
 *  - gradientFill (two-color linear gradient for shapes)
 *  - gradientFillB (second gradient color)
 *  - gradientAngle
 *  - useGradient flag
 *  - icon emoji/unicode support as a separate element type
 *  - locked flag (prevents drag/select)
 *  - flipH / flipV
 */
public class CanvasElement {

    public enum ElementType {
        TEXT, RECT, CIRCLE, LINE, IMAGE, ICON
    }

    private static final String SELECTION_BORDER = "#7F77DD";

    // ── Core properties ──────────────────────────────────────────────
    private ElementType type;
    private double x, y, width, height;
    private Color fillColor    = Color.TRANSPARENT;
    private Color strokeColor  = Color.TRANSPARENT;
    private double strokeWidth = 2.0;
    private double opacity     = 1.0;
    private double rotationAngle = 0.0;
    private boolean selected   = false;
    private boolean locked     = false;
    private boolean flipH      = false;
    private boolean flipV      = false;

    // ── Gradient ─────────────────────────────────────────────────────
    private boolean useGradient   = false;
    private Color gradientFillA   = Color.web("#7F77DD");
    private Color gradientFillB   = Color.web("#EC4899");
    private double gradientAngle  = 90.0; // degrees

    // ── Shadow / Glow ────────────────────────────────────────────────
    private boolean hasShadow     = false;
    private Color shadowColor     = Color.web("#000000", 0.5);
    private double shadowRadius   = 12.0;
    private double shadowOffsetX  = 0.0;
    private double shadowOffsetY  = 4.0;
    private boolean hasGlow       = false;
    private double glowLevel      = 0.3;

    // ── Text-specific ─────────────────────────────────────────────────
    private String textContent    = "";
    private double fontSize       = 20;
    private String fontFamily     = "Segoe UI";
    private boolean bold          = false;
    private boolean italic        = false;
    private boolean underline     = false;
    private boolean strikethrough = false;
    private TextAlignment textAlignment = TextAlignment.LEFT;
    private Pos textPos           = Pos.TOP_LEFT;
    private Color textColor       = Color.WHITE;
    private Color textBackground  = Color.TRANSPARENT;
    private double textBgRadius   = 6.0;
    private double letterSpacing  = 0.0;
    private double lineHeight     = 1.4;

    // ── Shape-specific ────────────────────────────────────────────────
    private double borderRadius   = 12.0;

    // ── Image-specific ────────────────────────────────────────────────
    private String imageUrl       = "";

    // ── Icon-specific ─────────────────────────────────────────────────
    private String iconText       = "★";
    private double iconSize       = 40;

    // ── Line-specific ─────────────────────────────────────────────────
    private double endX, endY;
    private boolean dashedLine    = false;

    // The built JavaFX node
    private Node node;

    // ── Factory methods ──────────────────────────────────────────────

    public static CanvasElement createText(String text, double x, double y,
                                           double w, double h, double fontSize,
                                           boolean bold, Color textColor) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.TEXT;
        e.x = x; e.y = y; e.width = w; e.height = h;
        e.textContent = text;
        e.fontSize = fontSize;
        e.bold = bold;
        e.textColor = textColor;
        return e;
    }

    public static CanvasElement createText(String text, double x, double y,
                                           double w, double h, double fontSize,
                                           String fontFamily, boolean bold, boolean italic,
                                           Color textColor, TextAlignment align) {
        CanvasElement e = createText(text, x, y, w, h, fontSize, bold, textColor);
        e.fontFamily = fontFamily;
        e.italic = italic;
        e.textAlignment = align;
        e.textPos = alignmentToPos(align);
        return e;
    }

    public static CanvasElement createRect(double x, double y, double w, double h,
                                           Color fill, Color stroke) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.RECT;
        e.x = x; e.y = y; e.width = w; e.height = h;
        e.fillColor = fill;
        e.strokeColor = stroke;
        return e;
    }

    public static CanvasElement createRectGradient(double x, double y, double w, double h,
                                                   Color colorA, Color colorB, double angleDeg) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.RECT;
        e.x = x; e.y = y; e.width = w; e.height = h;
        e.useGradient = true;
        e.gradientFillA = colorA;
        e.gradientFillB = colorB;
        e.gradientAngle = angleDeg;
        e.strokeColor = Color.TRANSPARENT;
        return e;
    }

    public static CanvasElement createCircle(double x, double y, double radius,
                                             Color fill, Color stroke) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.CIRCLE;
        e.x = x; e.y = y;
        e.width = radius * 2; e.height = radius * 2;
        e.fillColor = fill;
        e.strokeColor = stroke;
        return e;
    }

    public static CanvasElement createLine(double x1, double y1, double x2, double y2,
                                           Color stroke) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.LINE;
        e.x = x1; e.y = y1;
        e.endX = x2; e.endY = y2;
        e.width = Math.abs(x2 - x1);
        e.height = Math.abs(y2 - y1);
        e.strokeColor = stroke;
        return e;
    }

    public static CanvasElement createImage(String imageUrl, double x, double y,
                                            double w, double h) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.IMAGE;
        e.x = x; e.y = y; e.width = w; e.height = h;
        e.imageUrl = imageUrl;
        return e;
    }

    public static CanvasElement createIcon(String iconText, double x, double y,
                                           double size, Color color) {
        CanvasElement e = new CanvasElement();
        e.type = ElementType.ICON;
        e.x = x; e.y = y;
        e.iconText = iconText;
        e.iconSize = size;
        e.textColor = color;
        e.width = size + 16;
        e.height = size + 16;
        return e;
    }

    // ── Build the JavaFX Node ────────────────────────────────────────

    public Node buildNode() {
        node = switch (type) {
            case TEXT   -> buildTextNode();
            case RECT   -> buildRectNode();
            case CIRCLE -> buildCircleNode();
            case LINE   -> buildLineNode();
            case IMAGE  -> buildImageNode();
            case ICON   -> buildIconNode();
        };

        // Apply shared transforms
        node.setRotate(rotationAngle);
        node.setOpacity(opacity);
        node.setScaleX(flipH ? -1 : 1);
        node.setScaleY(flipV ? -1 : 1);

        // Apply shadow / glow effects
        applyEffects(node);

        return node;
    }

    private void applyEffects(Node n) {
        if (hasShadow) {
            DropShadow ds = new DropShadow();
            ds.setColor(shadowColor);
            ds.setRadius(shadowRadius);
            ds.setOffsetX(shadowOffsetX);
            ds.setOffsetY(shadowOffsetY);
            if (hasGlow) {
                Glow glow = new Glow(glowLevel);
                glow.setInput(ds);
                n.setEffect(glow);
            } else {
                n.setEffect(ds);
            }
        } else if (hasGlow) {
            n.setEffect(new Glow(glowLevel));
        }
    }

    private Node buildTextNode() {
        Label label = new Label(textContent);
        label.setWrapText(true);
        label.setMaxWidth(width);
        label.setPrefWidth(width);
        label.setMinHeight(height);
        label.setTextAlignment(textAlignment);
        label.setAlignment(textPos);

        FontWeight fw = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture fp = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        label.setFont(Font.font(fontFamily, fw, fp, fontSize));
        label.setTextFill(textColor);
        label.setLayoutX(x);
        label.setLayoutY(y);

        StringBuilder style = new StringBuilder("-fx-padding:6;");

        // Text background
        if (!textBackground.equals(Color.TRANSPARENT)) {
            style.append("-fx-background-color:").append(toHexAlpha(textBackground)).append(";");
            style.append("-fx-background-radius:").append(textBgRadius).append(";");
        }

        // Underline / strikethrough
        if (underline) style.append("-fx-underline:true;");
        if (strikethrough) style.append("-fx-strikethrough:true;");

        // Letter spacing (approximate via CSS)
        if (letterSpacing != 0.0) {
            style.append("-fx-letter-spacing:").append(letterSpacing).append("px;");
        }

        // Selection border
        if (selected) {
            style.append("-fx-border-color:").append(SELECTION_BORDER).append(";")
                    .append("-fx-border-width:2;-fx-border-style:dashed;-fx-border-radius:4;");
        }

        label.setStyle(style.toString());
        return label;
    }

    private Node buildRectNode() {
        Rectangle rect = new Rectangle(width, height);

        if (useGradient) {
            rect.setFill(buildLinearGradient(gradientAngle, gradientFillA, gradientFillB));
        } else {
            rect.setFill(fillColor);
        }

        if (!strokeColor.equals(Color.TRANSPARENT)) {
            rect.setStroke(strokeColor);
            rect.setStrokeWidth(strokeWidth);
        } else {
            rect.setStrokeWidth(0);
        }

        rect.setArcWidth(borderRadius * 2);
        rect.setArcHeight(borderRadius * 2);
        rect.setLayoutX(x);
        rect.setLayoutY(y);

        if (selected) {
            rect.setStroke(Color.web(SELECTION_BORDER));
            rect.setStrokeWidth(Math.max(strokeWidth, 2));
            rect.getStrokeDashArray().addAll(8.0, 5.0);
        }
        return rect;
    }

    private Node buildCircleNode() {
        double radius = width / 2.0;
        Circle circle = new Circle(radius);

        if (useGradient) {
            circle.setFill(buildLinearGradient(gradientAngle, gradientFillA, gradientFillB));
        } else {
            circle.setFill(fillColor);
        }

        if (!strokeColor.equals(Color.TRANSPARENT)) {
            circle.setStroke(strokeColor);
            circle.setStrokeWidth(strokeWidth);
        } else {
            circle.setStrokeWidth(0);
        }

        circle.setLayoutX(x + radius);
        circle.setLayoutY(y + radius);

        if (selected) {
            circle.setStroke(Color.web(SELECTION_BORDER));
            circle.setStrokeWidth(Math.max(strokeWidth, 2));
            circle.getStrokeDashArray().addAll(8.0, 5.0);
        }
        return circle;
    }

    private Node buildLineNode() {
        Line line = new Line(0, 0, endX - x, endY - y);
        line.setStroke(strokeColor);
        line.setStrokeWidth(strokeWidth);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        if (dashedLine) {
            line.getStrokeDashArray().addAll(12.0, 6.0);
        }
        line.setLayoutX(x);
        line.setLayoutY(y);
        if (selected) {
            line.setStroke(Color.web(SELECTION_BORDER));
        }
        return line;
    }

    private Node buildImageNode() {
        StackPane container = new StackPane();
        container.setPrefSize(width, height);
        container.setMaxSize(width, height);
        container.setLayoutX(x);
        container.setLayoutY(y);

        try {
            Image img = new Image(imageUrl, width, height, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(width);
            iv.setFitHeight(height);
            iv.setPreserveRatio(true);

            // Clip image to rounded corners
            Rectangle clip = new Rectangle(width, height);
            clip.setArcWidth(borderRadius * 2);
            clip.setArcHeight(borderRadius * 2);
            iv.setClip(clip);

            container.getChildren().add(iv);
        } catch (Exception ex) {
            Label placeholder = new Label("🖼  Image");
            placeholder.setStyle("-fx-text-fill:#8B949E;-fx-font-size:14px;-fx-font-weight:700;");
            container.getChildren().add(placeholder);
            container.setStyle("-fx-background-color:rgba(127,119,221,0.1);"
                    + "-fx-background-radius:8;-fx-border-color:rgba(127,119,221,0.3);"
                    + "-fx-border-radius:8;-fx-border-style:dashed;");
        }

        if (selected) {
            container.setStyle((container.getStyle() != null ? container.getStyle() : "")
                    + "-fx-border-color:" + SELECTION_BORDER + ";-fx-border-width:2;"
                    + "-fx-border-style:dashed;-fx-border-radius:6;");
        }
        return container;
    }

    private Node buildIconNode() {
        StackPane container = new StackPane();
        container.setPrefSize(width, height);
        container.setMaxSize(width, height);
        container.setLayoutX(x);
        container.setLayoutY(y);

        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size:" + iconSize + "px;");
        icon.setTextFill(textColor);
        icon.setAlignment(Pos.CENTER);

        container.getChildren().add(icon);

        if (selected) {
            container.setStyle("-fx-border-color:" + SELECTION_BORDER + ";-fx-border-width:2;"
                    + "-fx-border-style:dashed;-fx-border-radius:6;");
        }
        return container;
    }

    // ── Gradient helper ──────────────────────────────────────────────

    private static LinearGradient buildLinearGradient(double angleDeg, Color a, Color b) {
        double rad = Math.toRadians(angleDeg);
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));

        double startX = (Math.cos(rad) >= 0) ? 0 : 1;
        double startY = (Math.sin(rad) >= 0) ? 0 : 1;
        double endX   = 1 - startX;
        double endY   = 1 - startY;

        return new LinearGradient(startX, startY, endX, endY,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, a), new Stop(1, b));
    }

    // ── Alignment helper ─────────────────────────────────────────────

    private static Pos alignmentToPos(TextAlignment a) {
        return switch (a) {
            case CENTER -> Pos.TOP_CENTER;
            case RIGHT  -> Pos.TOP_RIGHT;
            default     -> Pos.TOP_LEFT;
        };
    }

    // ── Duplicate ────────────────────────────────────────────────────

    public CanvasElement duplicate() {
        CanvasElement d = new CanvasElement();
        d.type          = this.type;
        d.x             = this.x + 20;
        d.y             = this.y + 20;
        d.width         = this.width;
        d.height        = this.height;
        d.fillColor     = this.fillColor;
        d.strokeColor   = this.strokeColor;
        d.strokeWidth   = this.strokeWidth;
        d.opacity       = this.opacity;
        d.rotationAngle = this.rotationAngle;
        d.locked        = false;
        d.flipH         = this.flipH;
        d.flipV         = this.flipV;
        d.useGradient   = this.useGradient;
        d.gradientFillA = this.gradientFillA;
        d.gradientFillB = this.gradientFillB;
        d.gradientAngle = this.gradientAngle;
        d.hasShadow     = this.hasShadow;
        d.shadowColor   = this.shadowColor;
        d.shadowRadius  = this.shadowRadius;
        d.shadowOffsetX = this.shadowOffsetX;
        d.shadowOffsetY = this.shadowOffsetY;
        d.hasGlow       = this.hasGlow;
        d.glowLevel     = this.glowLevel;
        d.textContent   = this.textContent;
        d.fontSize      = this.fontSize;
        d.fontFamily    = this.fontFamily;
        d.bold          = this.bold;
        d.italic        = this.italic;
        d.underline     = this.underline;
        d.strikethrough = this.strikethrough;
        d.textAlignment = this.textAlignment;
        d.textPos       = this.textPos;
        d.textColor     = this.textColor;
        d.textBackground = this.textBackground;
        d.textBgRadius  = this.textBgRadius;
        d.letterSpacing = this.letterSpacing;
        d.lineHeight    = this.lineHeight;
        d.borderRadius  = this.borderRadius;
        d.imageUrl      = this.imageUrl;
        d.iconText      = this.iconText;
        d.iconSize      = this.iconSize;
        d.endX          = this.endX + 20;
        d.endY          = this.endY + 20;
        d.dashedLine    = this.dashedLine;
        return d;
    }

    // ── Getters / Setters with live node update ──────────────────────

    public ElementType getType() { return type; }
    public Node getNode() { return node; }
    public boolean isTextElement() { return type == ElementType.TEXT; }
    public boolean isShapeElement() { return type == ElementType.RECT || type == ElementType.CIRCLE; }

    public double getX() { return x; }
    public void setX(double x) {
        this.x = x;
        if (node != null) {
            if (type == ElementType.CIRCLE) node.setLayoutX(x + width / 2.0);
            else node.setLayoutX(x);
        }
    }

    public double getY() { return y; }
    public void setY(double y) {
        this.y = y;
        if (node != null) {
            if (type == ElementType.CIRCLE) node.setLayoutY(y + height / 2.0);
            else node.setLayoutY(y);
        }
    }

    public double getWidth()  { return width; }
    public void setWidth(double w)  { this.width = w; rebuildNodeInPlace(); }

    public double getHeight() { return height; }
    public void setHeight(double h) { this.height = h; rebuildNodeInPlace(); }

    public Color getFillColor()  { return fillColor; }
    public void setFillColor(Color c) {
        this.fillColor = c;
        this.useGradient = false;
        if (node instanceof Rectangle r) r.setFill(c);
        else if (node instanceof Circle ci) ci.setFill(c);
    }

    public Color getStrokeColor() { return strokeColor; }
    public void setStrokeColor(Color c) {
        this.strokeColor = c;
        if (node instanceof Rectangle r) { r.setStroke(c); r.setStrokeWidth(c.equals(Color.TRANSPARENT) ? 0 : strokeWidth); }
        else if (node instanceof Circle ci) { ci.setStroke(c); ci.setStrokeWidth(c.equals(Color.TRANSPARENT) ? 0 : strokeWidth); }
        else if (node instanceof Line l) l.setStroke(c);
    }

    public double getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(double w) {
        this.strokeWidth = w;
        if (node instanceof Rectangle r && !strokeColor.equals(Color.TRANSPARENT)) r.setStrokeWidth(w);
        else if (node instanceof Circle ci && !strokeColor.equals(Color.TRANSPARENT)) ci.setStrokeWidth(w);
        else if (node instanceof Line l) l.setStrokeWidth(w);
    }

    public double getOpacity() { return opacity; }
    public void setOpacity(double o) {
        this.opacity = o;
        if (node != null) node.setOpacity(o);
    }

    public double getRotationAngle() { return rotationAngle; }
    public void setRotationAngle(double deg) {
        this.rotationAngle = deg;
        if (node != null) node.setRotate(deg);
    }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean s) {
        this.selected = s;
        rebuildNodeInPlace();
    }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean isFlipH() { return flipH; }
    public void setFlipH(boolean f) { this.flipH = f; if (node != null) node.setScaleX(f ? -1 : 1); }

    public boolean isFlipV() { return flipV; }
    public void setFlipV(boolean f) { this.flipV = f; if (node != null) node.setScaleY(f ? -1 : 1); }

    // Gradient
    public boolean isUseGradient() { return useGradient; }
    public void setUseGradient(boolean g) { this.useGradient = g; rebuildNodeInPlace(); }

    public Color getGradientFillA() { return gradientFillA; }
    public void setGradientFillA(Color c) { this.gradientFillA = c; if (useGradient) rebuildNodeInPlace(); }

    public Color getGradientFillB() { return gradientFillB; }
    public void setGradientFillB(Color c) { this.gradientFillB = c; if (useGradient) rebuildNodeInPlace(); }

    public double getGradientAngle() { return gradientAngle; }
    public void setGradientAngle(double a) { this.gradientAngle = a; if (useGradient) rebuildNodeInPlace(); }

    // Shadow / Glow
    public boolean isHasShadow() { return hasShadow; }
    public void setHasShadow(boolean s) { this.hasShadow = s; applyEffects(node); }

    public Color getShadowColor() { return shadowColor; }
    public void setShadowColor(Color c) { this.shadowColor = c; if (hasShadow) applyEffects(node); }

    public double getShadowRadius() { return shadowRadius; }
    public void setShadowRadius(double r) { this.shadowRadius = r; if (hasShadow) applyEffects(node); }

    public double getShadowOffsetX() { return shadowOffsetX; }
    public void setShadowOffsetX(double v) { this.shadowOffsetX = v; if (hasShadow) applyEffects(node); }

    public double getShadowOffsetY() { return shadowOffsetY; }
    public void setShadowOffsetY(double v) { this.shadowOffsetY = v; if (hasShadow) applyEffects(node); }

    public boolean isHasGlow() { return hasGlow; }
    public void setHasGlow(boolean g) { this.hasGlow = g; applyEffects(node); }

    public double getGlowLevel() { return glowLevel; }
    public void setGlowLevel(double l) { this.glowLevel = l; if (hasGlow) applyEffects(node); }

    // Text
    public String getTextContent() { return textContent; }
    public void setTextContent(String t) {
        this.textContent = t;
        if (node instanceof Label l) l.setText(t);
    }

    public double getFontSize() { return fontSize; }
    public void setFontSize(double s) {
        this.fontSize = s;
        if (node instanceof Label l) l.setFont(buildFont());
        else if (type == ElementType.ICON) rebuildNodeInPlace();
    }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String f) {
        this.fontFamily = f;
        if (node instanceof Label l) l.setFont(buildFont());
    }

    public boolean isBold() { return bold; }
    public void setBold(boolean b) {
        this.bold = b;
        if (node instanceof Label l) l.setFont(buildFont());
    }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean i) {
        this.italic = i;
        if (node instanceof Label l) l.setFont(buildFont());
    }

    public boolean isUnderline() { return underline; }
    public void setUnderline(boolean u) {
        this.underline = u;
        rebuildNodeInPlace();
    }

    public boolean isStrikethrough() { return strikethrough; }
    public void setStrikethrough(boolean s) {
        this.strikethrough = s;
        rebuildNodeInPlace();
    }

    public TextAlignment getTextAlignment() { return textAlignment; }
    public void setTextAlignment(TextAlignment a) {
        this.textAlignment = a;
        this.textPos = alignmentToPos(a);
        if (node instanceof Label l) {
            l.setTextAlignment(a);
            l.setAlignment(textPos);
        }
    }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color c) {
        this.textColor = c;
        if (node instanceof Label l) l.setTextFill(c);
    }

    public Color getTextBackground() { return textBackground; }
    public void setTextBackground(Color c) {
        this.textBackground = c;
        rebuildNodeInPlace();
    }

    public double getTextBgRadius() { return textBgRadius; }
    public void setTextBgRadius(double r) { this.textBgRadius = r; rebuildNodeInPlace(); }

    public double getLetterSpacing() { return letterSpacing; }
    public void setLetterSpacing(double s) { this.letterSpacing = s; rebuildNodeInPlace(); }

    public double getLineHeight() { return lineHeight; }
    public void setLineHeight(double h) { this.lineHeight = h; }

    public double getBorderRadius() { return borderRadius; }
    public void setBorderRadius(double r) {
        this.borderRadius = r;
        if (node instanceof Rectangle rect) {
            rect.setArcWidth(r * 2);
            rect.setArcHeight(r * 2);
        } else {
            rebuildNodeInPlace();
        }
    }

    public String getIconText() { return iconText; }
    public void setIconText(String t) {
        this.iconText = t;
        rebuildNodeInPlace();
    }

    public double getIconSize() { return iconSize; }
    public void setIconSize(double s) {
        this.iconSize = s;
        rebuildNodeInPlace();
    }

    public boolean isDashedLine() { return dashedLine; }
    public void setDashedLine(boolean d) {
        this.dashedLine = d;
        rebuildNodeInPlace();
    }

    // ── Layer name for the layers panel ──────────────────────────────

    public String getLayerName() {
        return switch (type) {
            case TEXT -> {
                String preview = textContent != null ? textContent.replace("\n", " ") : "Text";
                yield "T: " + (preview.length() > 20 ? preview.substring(0, 20) + "…" : preview);
            }
            case RECT   -> "Rectangle";
            case CIRCLE -> "Circle";
            case LINE   -> "Line";
            case IMAGE  -> "Image";
            case ICON   -> "Icon: " + iconText;
        };
    }

    public String getTypeIcon() {
        return switch (type) {
            case TEXT   -> "T";
            case RECT   -> "▢";
            case CIRCLE -> "●";
            case LINE   -> "╱";
            case IMAGE  -> "🖼";
            case ICON   -> "★";
        };
    }

    // ── Internal helpers ─────────────────────────────────────────────

    private Font buildFont() {
        FontWeight fw = bold   ? FontWeight.BOLD   : FontWeight.NORMAL;
        FontPosture fp = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        return Font.font(fontFamily, fw, fp, fontSize);
    }

    private String toHexAlpha(Color c) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int)(c.getRed()*255), (int)(c.getGreen()*255),
                (int)(c.getBlue()*255), c.getOpacity());
    }

    void rebuildNodeInPlace() {
        if (node != null && node.getParent() != null) {
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) node.getParent();
            int idx = parent.getChildren().indexOf(node);
            if (idx >= 0) {
                var oldPressed  = node.getOnMousePressed();
                var oldDragged  = node.getOnMouseDragged();
                var oldReleased = node.getOnMouseReleased();
                var oldClicked  = node.getOnMouseClicked();

                parent.getChildren().remove(idx);
                Node newNode = buildNode();
                newNode.setOnMousePressed(oldPressed);
                newNode.setOnMouseDragged(oldDragged);
                newNode.setOnMouseReleased(oldReleased);
                newNode.setOnMouseClicked(oldClicked);
                if (!locked) newNode.setCursor(Cursor.HAND);
                parent.getChildren().add(idx, newNode);
                node = newNode;
            }
        }
    }
}