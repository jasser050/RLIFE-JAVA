package com.studyflow.controllers;

import com.studyflow.models.Deck;
import com.studyflow.models.Flashcard;
import com.studyflow.models.Rating;
import com.studyflow.services.DeckService;
import com.studyflow.services.FlashcardService;
import com.studyflow.services.RatingService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Stream;

public class RevisionsController implements Initializable {

    // ── List-view fields (null when loaded as standalone form FXML) ──
    @FXML private VBox listView;
    @FXML private VBox ratingView;
    @FXML private Label totalDecksLabel;
    @FXML private Label masteredLabel;
    @FXML private Label dueReviewLabel;
    @FXML private Label streakLabel;
    @FXML private FlowPane decksGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button exportPdfBtn;
    @FXML private StackPane donutChartPane;
    @FXML private StackPane lineChartPane;
    @FXML private StackPane barChartPane;
    @FXML private Label selectedRatingDeckLabel;
    @FXML private Label ratingsSummaryLabel;
    @FXML private VBox ratingsListContainer;

    // ── Form fields ──
    @FXML private VBox formView;
    @FXML private Label formTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextField fieldMatiere;
    @FXML private ComboBox<String> combNiveau;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldImage;
    @FXML private TextField fieldPdf;          // hidden legacy field
    @FXML private Label imagePreviewLabel;
    @FXML private Label pdfPreviewLabel;
    @FXML private Label errTitre, errMatiere, errNiveau, errImage, errPdf;
    @FXML private VBox pdfListContainer;       // dynamic PDF rows go here
    @FXML private Button addPdfBtn;            // "+ Add PDF" button

    /** Holds the absolute paths of all PDFs currently in the form. */
    private final List<String> selectedPdfPaths = new ArrayList<>();

    private final DeckService deckService = new DeckService();
    private final FlashcardService flashcardService = new FlashcardService();
    private final RatingService ratingService = new RatingService();
    private List<Deck> decks;
    private List<Deck> visibleDecks = List.of();
    private Deck selectedDeck = null;
    private Deck selectedRatingsDeck = null;
    private final Map<Integer, List<Flashcard>> flashcardsByDeck = new HashMap<>();
    private final Map<Object, Popup> popupMap = new HashMap<>();
    private final Set<Integer> masteredDeckIds = new HashSet<>();

    private boolean suppressValidation = false;

    private static final String[] COLORS = {"primary","success","warning","accent","danger"};
    private static final String[] ICONS  = {"fth-database","fth-trending-up","fth-terminal","fth-grid","fth-wifi","fth-cpu","fth-hard-drive","fth-check-square"};

    private static final String BORDER_DEFAULT = "#334155";
    private static final String BORDER_ERROR   = "#FB7185";
    private static final String BORDER_OK      = "#34D399";
    private static final String FIELD_STYLE_BASE =
            "-fx-background-color:#1E293B;-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                    "-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:11 16;-fx-font-size:13px;";
    private static final String COMBO_STYLE_BASE =
            "-fx-background-color:#1E293B;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;";

    // ─────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (combNiveau != null)
            combNiveau.setItems(FXCollections.observableArrayList("Beginner","Intermediate","Advanced"));
        if (fieldDescription != null)
            fieldDescription.setStyle("-fx-control-inner-background:#1E293B;-fx-background-color:#1E293B;" +
                    "-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                    "-fx-border-color:#334155;-fx-border-width:1.5;-fx-border-radius:10;" +
                    "-fx-background-radius:10;-fx-font-size:13px;");

        // ── FIX: always init form listeners (works for both standalone & embedded FXML) ──
        initFormListeners();

        // ── FIX: make sure pdfListContainer and addPdfBtn are visible & managed ──
        if (pdfListContainer != null) {
            pdfListContainer.setVisible(true);
            pdfListContainer.setManaged(true);
        }
        if (addPdfBtn != null) {
            addPdfBtn.setVisible(true);
            addPdfBtn.setManaged(true);
        }

        // List-view setup (only when embedded in the main layout)
        if (listView != null) {
            goToList();
            setupSearchFilter();
            refreshAll();
        }

        // ── FIX: if this FXML is the standalone form (no listView), show form immediately ──
        if (listView == null && formView == null) {
            // Standalone ScrollPane form — form fields are directly in the scene
            // Nothing to toggle; fields are already visible
        }

        if (searchField != null && searchField.getParent() != null) {
            searchField.getParent().setOnMouseClicked(event -> searchField.requestFocus());
        }

        clearRatingsPanel();
    }

    private void initFormListeners() {
        showErr(errTitre, false);
        showErr(errMatiere, false);
        showErr(errNiveau, false);
        showErr(errImage, false);
        showErr(errPdf, false);

        setBorderNeutral(fieldTitre);
        setBorderNeutral(fieldMatiere);
        setBorderComboNeutral(combNiveau);
        setBorderNeutral(fieldImage);
        // NOTE: fieldPdf is hidden — do NOT call setBorderNeutral on it for visual purposes

        if (fieldTitre   != null) fieldTitre.textProperty().addListener((o,ov,nv)   -> { if (!suppressValidation) validateTitreInline(); });
        if (fieldMatiere != null) fieldMatiere.textProperty().addListener((o,ov,nv) -> { if (!suppressValidation) validateMatiereInline(); });
        if (combNiveau   != null) combNiveau.valueProperty().addListener((o,ov,nv)  -> { if (!suppressValidation) validateNiveauInline(); });
        if (fieldImage   != null) fieldImage.textProperty().addListener((o,ov,nv)   -> { if (!suppressValidation) validateImageInline(); });
        // No listener on fieldPdf — validation is driven by selectedPdfPaths list
    }

    // ══════════════════════════════════════════════
    // PDF — MULTI-PDF SUPPORT
    // ══════════════════════════════════════════════

    /**
     * Called by the "+ Add PDF" button (onAction="#pickPdf" in FXML).
     * Opens a multi-file chooser and adds each selected PDF as a row.
     */
    @FXML
    public void pickPdf() {
        // ── FIX: get window from ANY visible scene node, not just addPdfBtn ──
        javafx.stage.Window window = getAnyWindow();
        if (window == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Select PDF(s)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> files = fc.showOpenMultipleDialog(window);
        if (files == null || files.isEmpty()) return;

        for (File f : files) {
            String path = f.getAbsolutePath();
            if (!selectedPdfPaths.contains(path)) {
                selectedPdfPaths.add(path);
                addPdfRow(path);
            }
        }

        syncLegacyPdfField();
        showErr(errPdf, false);   // hide error once at least one PDF added
    }

    /**
     * Build one PDF row: [icon | filename | spacer | delete button]
     * and append it to pdfListContainer.
     */
    private void addPdfRow(String path) {
        if (pdfListContainer == null) return;

        File f = new File(path);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(path);
        row.setStyle(
                "-fx-background-color:#1E293B;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:#334155;" +
                        "-fx-border-radius:8;" +
                        "-fx-border-width:1;" +
                        "-fx-padding:8 12;"
        );

        // PDF icon
        FontIcon pdfIcon = new FontIcon("fth-file-text");
        pdfIcon.setIconSize(13);
        pdfIcon.setIconColor(Color.web("#F472B6"));

        // Filename label
        Label nameLbl = new Label(f.getName());
        nameLbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        nameLbl.setMaxWidth(Double.MAX_VALUE);

        // Delete button
        Button delBtn = new Button();
        FontIcon trashIcon = new FontIcon("fth-trash-2");
        trashIcon.setIconSize(12);
        trashIcon.setIconColor(Color.web("#FB7185"));
        delBtn.setGraphic(trashIcon);
        delBtn.setStyle(
                "-fx-background-color:rgba(251,113,133,0.12);" +
                        "-fx-background-radius:6;-fx-cursor:hand;" +
                        "-fx-padding:4 8;" +
                        "-fx-border-color:#FB7185;" +
                        "-fx-border-radius:6;-fx-border-width:1;"
        );
        delBtn.setOnMouseEntered(e -> delBtn.setStyle(
                "-fx-background-color:rgba(251,113,133,0.28);" +
                        "-fx-background-radius:6;-fx-cursor:hand;" +
                        "-fx-padding:4 8;" +
                        "-fx-border-color:#FB7185;" +
                        "-fx-border-radius:6;-fx-border-width:1;"));
        delBtn.setOnMouseExited(e -> delBtn.setStyle(
                "-fx-background-color:rgba(251,113,133,0.12);" +
                        "-fx-background-radius:6;-fx-cursor:hand;" +
                        "-fx-padding:4 8;" +
                        "-fx-border-color:#FB7185;" +
                        "-fx-border-radius:6;-fx-border-width:1;"));

        delBtn.setOnAction(e -> {
            selectedPdfPaths.remove(path);
            pdfListContainer.getChildren().remove(row);
            syncLegacyPdfField();
            if (selectedPdfPaths.isEmpty()) {
                if (errPdf != null) errPdf.setText("⚠  Au moins un PDF est obligatoire.");
                showErr(errPdf, true);
            }
        });

        row.getChildren().addAll(pdfIcon, nameLbl, delBtn);
        pdfListContainer.getChildren().add(row);
    }

    /** Keep the hidden fieldPdf pointing to the first path (backward compat). */
    private void syncLegacyPdfField() {
        if (fieldPdf == null) return;
        fieldPdf.setText(selectedPdfPaths.isEmpty() ? "" : selectedPdfPaths.get(0));
    }

    // ── FIX: helper to get a Window from ANY injected node that is on a scene ──
    private javafx.stage.Window getAnyWindow() {
        // Try nodes in priority order
        javafx.scene.Node[] candidates = {
                addPdfBtn, fieldTitre, fieldMatiere, fieldImage, fieldPdf,
                combNiveau, fieldDescription
        };
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null && n.getScene().getWindow() != null) {
                return n.getScene().getWindow();
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════
    // EXPORT PDF  — Beautiful Card-Style Layout
    // ══════════════════════════════════════════════

    @FXML public void exportDecksToPdf() {
        if (decks == null || decks.isEmpty()) { showErrorPopup(exportPdfBtn, "No decks to export."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export decks to PDF");
        fc.setInitialFileName("RLIFE_Decks_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        javafx.stage.Window win = (exportPdfBtn != null && exportPdfBtn.getScene() != null) ? exportPdfBtn.getScene().getWindow() : null;
        File file = (win != null) ? fc.showSaveDialog(win) : null;
        if (file == null) return;
        try {
            generateBeautifulPdf(file, decks);
            showSuccessPopup("✨ PDF exported: " + file.getName());
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file); } catch (Exception ignored) {}
        } catch (Exception e) { showErrorPopup(exportPdfBtn, "Error: " + e.getMessage()); }
    }

    private static final float PW = 595f;
    private static final float PH = 842f;
    private static final float MARGIN     = 36f;
    private static final float CARD_W     = (PW - MARGIN * 2 - 12) / 2f;
    private static final float CARD_H     = 130f;
    private static final float CARD_GAP_X = 12f;
    private static final float CARD_GAP_Y = 14f;
    private static final float CARDS_TOP  = PH - 148f;

    private static final float[][] LEVEL_COLORS = {
            {0.067f, 0.624f, 0.596f},
            {0.486f, 0.227f, 0.929f},
            {0.863f, 0.392f, 0.145f},
            {0.863f, 0.145f, 0.365f},
    };
    private static final float[][] ACCENT = {
            {0.486f, 0.227f, 0.929f},
            {0.039f, 0.600f, 0.510f},
            {0.855f, 0.498f, 0.082f},
            {0.953f, 0.278f, 0.369f},
            {0.231f, 0.510f, 0.965f},
    };

    private void generateBeautifulPdf(File file, List<Deck> list) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            List<byte[]> streams = buildBeautifulPages(list);
            int pageCount = streams.size();
            List<Long> offsets = new ArrayList<>();
            byte[] header = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes("ISO-8859-1");
            fos.write(header);
            long pos = header.length;
            offsets.add(pos);
            pos += write(fos, "1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n");
            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < pageCount; i++) kids.append(3 + i * 2).append(" 0 R ");
            offsets.add(pos);
            pos += write(fos, "2 0 obj\n<</Type/Pages/Kids[" + kids + "]/Count " + pageCount + ">>\nendobj\n");
            for (int i = 0; i < pageCount; i++) {
                int pn = 3 + i * 2, cn = 4 + i * 2;
                byte[] content = streams.get(i);
                offsets.add(pos);
                String pageDict = pn + " 0 obj\n<</Type/Page/Parent 2 0 R"
                        + "/MediaBox[0 0 " + (int)PW + " " + (int)PH + "]"
                        + "/Contents " + cn + " 0 R"
                        + "/Resources<</Font<</F1<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>"
                        + "/F2<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Bold>>"
                        + "/F3<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Oblique>>"
                        + ">>>>>>\n>>\nendobj\n";
                pos += write(fos, pageDict);
                offsets.add(pos);
                String sh = cn + " 0 obj\n<</Length " + content.length + ">>\nstream\n";
                pos += write(fos, sh);
                fos.write(content); pos += content.length;
                pos += write(fos, "\nendstream\nendobj\n");
            }
            long xrefPos = pos;
            int total = 2 + pageCount * 2 + 1;
            StringBuilder xref = new StringBuilder("xref\n0 ").append(total).append("\n0000000000 65535 f \n");
            for (long off : offsets) xref.append(String.format("%010d 00000 n \n", off));
            fos.write(xref.toString().getBytes("ISO-8859-1"));
            write(fos, "trailer\n<</Size " + total + "/Root 1 0 R>>\nstartxref\n" + xrefPos + "\n%%EOF\n");
        }
    }

    private long write(FileOutputStream fos, String s) throws IOException {
        byte[] b = s.getBytes("ISO-8859-1"); fos.write(b); return b.length;
    }

    private List<byte[]> buildBeautifulPages(List<Deck> list) throws IOException {
        List<byte[]> pages = new ArrayList<>();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm"));
        int rowsPerPage = (int) Math.floor((CARDS_TOP - MARGIN + CARD_GAP_Y) / (CARD_H + CARD_GAP_Y));
        int cardsPerPage = rowsPerPage * 2;
        int pageCount = (int) Math.ceil(list.size() / (double) cardsPerPage);
        if (pageCount == 0) pageCount = 1;
        for (int pg = 0; pg < pageCount; pg++) {
            StringBuilder sb = new StringBuilder();
            buildPageBackground(sb, date, list.size(), pg + 1, pageCount);
            int startIdx = pg * cardsPerPage;
            int endIdx = Math.min(startIdx + cardsPerPage, list.size());
            for (int i = startIdx; i < endIdx; i++) {
                int slot = i - startIdx;
                int col = slot % 2;
                int row = slot / 2;
                float cx = MARGIN + col * (CARD_W + CARD_GAP_X);
                float cy = CARDS_TOP - row * (CARD_H + CARD_GAP_Y);
                buildDeckCard(sb, list.get(i), i + 1, cx, cy, ACCENT[i % ACCENT.length]);
            }
            buildPageFooter(sb, pg + 1, pageCount);
            pages.add(sb.toString().getBytes("ISO-8859-1"));
        }
        return pages;
    }

    private void buildPageBackground(StringBuilder sb, String date, int total, int pageNum, int pageCount) {
        sb.append("0.012 0.024 0.063 rg\n0 0 ").append((int)PW).append(" ").append((int)PH).append(" re f\n");
        sb.append("0.486 0.227 0.929 rg\n0 ").append((int)(PH-90)).append(" ").append((int)PW).append(" 90 re f\n");
        sb.append("0.300 0.130 0.600 rg\n0 ").append((int)(PH-120)).append(" ").append((int)PW).append(" 32 re f\n");
        sb.append("0.06 0.10 0.17 rg\n");
        for (int gx = 20; gx < PW; gx += 28) {
            for (int gy = (int)(PH-120); gy < PH; gy += 28) {
                drawCircle(sb, gx, gy, 1.2f);
                sb.append("f\n");
            }
        }
        sb.append("1 1 1 rg\n");
        roundRect(sb, MARGIN, PH - 74, 72, 26, 8);
        sb.append("f\n");
        sb.append("0.486 0.227 0.929 rg\n");
        sb.append("BT /F2 11 Tf ").append(MARGIN + 8).append(" ").append(PH - 65).append(" Td (RLIFE) Tj ET\n");
        sb.append("1 1 1 rg\n");
        sb.append("BT /F2 26 Tf ").append(MARGIN + 82).append(" ").append(PH - 68).append(" Td (Decks Collection) Tj ET\n");
        sb.append("0.04 0.07 0.13 rg\n");
        roundRect(sb, MARGIN, PH - 108, PW - MARGIN * 2, 24, 6);
        sb.append("f\n");
        sb.append("0.55 0.55 0.65 rg\n");
        sb.append("BT /F1 9 Tf ").append(MARGIN + 10).append(" ").append(PH - 101)
                .append(" Td (Exported: ").append(pdfSafe(date)).append(") Tj ET\n");
        sb.append("0.486 0.227 0.929 rg\n");
        float pillX = MARGIN + 170;
        roundRect(sb, pillX, PH - 108, 90, 22, 8);
        sb.append("f\n");
        sb.append("1 1 1 rg\n");
        sb.append("BT /F2 9 Tf ").append(pillX + 8).append(" ").append(PH - 101)
                .append(" Td (").append(total).append(" Deck").append(total > 1 ? "s" : "").append(" Total) Tj ET\n");
        sb.append("0.35 0.35 0.45 rg\n");
        sb.append("BT /F1 9 Tf ").append(PW - MARGIN - 60).append(" ").append(PH - 101)
                .append(" Td (Page ").append(pageNum).append(" / ").append(pageCount).append(") Tj ET\n");
        sb.append("0.486 0.227 0.929 rg\n");
        sb.append("BT /F2 8 Tf ").append(MARGIN).append(" ").append(CARDS_TOP + 16)
                .append(" Td (YOUR DECKS) Tj ET\n");
        sb.append("0.486 0.227 0.929 rg\n");
        sb.append(MARGIN).append(" ").append(CARDS_TOP + 11).append(" 54 1.5 re f\n");
        sb.append("0.15 0.17 0.22 rg\n");
        sb.append(MARGIN + 60).append(" ").append(CARDS_TOP + 11).append(" ")
                .append(PW - MARGIN * 2 - 60).append(" 0.5 re f\n");
    }

    private void buildDeckCard(StringBuilder sb, Deck deck, int num, float cx, float cy, float[] accent) {
        float cBottom = cy - CARD_H;
        sb.append("0.01 0.02 0.04 rg\n");
        roundRect(sb, cx + 3, cBottom - 4, CARD_W, CARD_H, 12);
        sb.append("f\n");
        sb.append("0.059 0.090 0.145 rg\n");
        roundRect(sb, cx, cBottom, CARD_W, CARD_H, 12);
        sb.append("f\n");
        sb.append(accent[0]).append(" ").append(accent[1]).append(" ").append(accent[2]).append(" rg\n");
        roundRect(sb, cx, cy - 34, CARD_W, 34, 12);
        sb.append("f\n");
        sb.append(accent[0]).append(" ").append(accent[1]).append(" ").append(accent[2]).append(" rg\n");
        sb.append(cx).append(" ").append(cy - 34).append(" ").append(CARD_W).append(" 10 re f\n");
        sb.append("1 1 1 rg\n");
        drawCircle(sb, cx + 18, cy - 17, 10);
        sb.append("f\n");
        sb.append(accent[0]).append(" ").append(accent[1]).append(" ").append(accent[2]).append(" rg\n");
        String numStr = String.valueOf(num);
        float numX = cx + (numStr.length() > 1 ? 13 : 15);
        sb.append("BT /F2 9 Tf ").append(numX).append(" ").append(cy - 21).append(" Td (").append(numStr).append(") Tj ET\n");
        sb.append("1 1 1 rg\n");
        String titre = pdfSafe(truncateStr(safe(deck.getTitre()), 28));
        sb.append("BT /F2 12 Tf ").append(cx + 34).append(" ").append(cy - 22).append(" Td (").append(titre).append(") Tj ET\n");
        if (deck.getPdf() != null && !deck.getPdf().isEmpty()) {
            sb.append("1 1 1 rg\n");
            roundRect(sb, cx + CARD_W - 36, cy - 29, 28, 16, 4);
            sb.append("f\n");
            sb.append(accent[0]).append(" ").append(accent[1]).append(" ").append(accent[2]).append(" rg\n");
            sb.append("BT /F2 7 Tf ").append(cx + CARD_W - 31).append(" ").append(cy - 24).append(" Td (PDF) Tj ET\n");
        }
        sb.append("0.55 0.60 0.70 rg\n");
        sb.append("BT /F3 9 Tf ").append(cx + 12).append(" ").append(cy - 52).append(" Td (")
                .append(pdfSafe(truncateStr(safe(deck.getMatiere()), 30))).append(") Tj ET\n");
        float[] lc = levelColor(safe(deck.getNiveau()));
        sb.append(lc[0]).append(" ").append(lc[1]).append(" ").append(lc[2]).append(" rg\n");
        float badgeX = cx + CARD_W - 90;
        roundRect(sb, badgeX, cy - 56, 80, 16, 6);
        sb.append("f\n");
        sb.append("1 1 1 rg\n");
        String lvl = pdfSafe(truncateStr(safe(deck.getNiveau()), 12));
        sb.append("BT /F2 8 Tf ").append(badgeX + 6).append(" ").append(cy - 48).append(" Td (").append(lvl).append(") Tj ET\n");
        sb.append("0.10 0.13 0.20 rg\n");
        sb.append(cx + 10).append(" ").append(cy - 64).append(" ").append(CARD_W - 20).append(" 0.8 re f\n");
        String desc = safe(deck.getDescription());
        if (!desc.isEmpty()) {
            sb.append("0.42 0.47 0.56 rg\n");
            String line1 = pdfSafe(truncateStr(desc, 46));
            sb.append("BT /F1 8 Tf ").append(cx + 12).append(" ").append(cy - 80).append(" Td (").append(line1).append(") Tj ET\n");
            if (desc.length() > 46) {
                String line2 = pdfSafe(truncateStr(desc.substring(46), 46));
                sb.append("BT /F1 8 Tf ").append(cx + 12).append(" ").append(cy - 91).append(" Td (").append(line2).append(") Tj ET\n");
            }
        } else {
            sb.append("0.25 0.28 0.36 rg\n");
            sb.append("BT /F3 8 Tf ").append(cx + 12).append(" ").append(cy - 80).append(" Td (No description provided.) Tj ET\n");
        }
        sb.append(accent[0]).append(" ").append(accent[1]).append(" ").append(accent[2]).append(" rg\n");
        drawCircle(sb, cx + 16, cBottom + 14, 4);
        sb.append("f\n");
        sb.append("0.15 0.17 0.25 rg\n");
        roundRect(sb, cx + CARD_W - 48, cBottom + 6, 40, 16, 6);
        sb.append("f\n");
        sb.append("0.40 0.44 0.55 rg\n");
        sb.append("BT /F1 7 Tf ").append(cx + CARD_W - 44).append(" ").append(cBottom + 12).append(" Td (#").append(num).append(") Tj ET\n");
    }

    private void buildPageFooter(StringBuilder sb, int pageNum, int pageCount) {
        sb.append("0.486 0.227 0.929 rg\n");
        sb.append(MARGIN).append(" ").append(26).append(" ").append(PW - MARGIN * 2).append(" 1 re f\n");
        sb.append("0.30 0.33 0.42 rg\n");
        sb.append("BT /F1 8 Tf ").append(MARGIN).append(" 14 Td (RLIFE - Your Personal Study Companion) Tj ET\n");
        sb.append("BT /F1 8 Tf ").append(PW / 2 - 24).append(" 14 Td (rlife.app) Tj ET\n");
        sb.append("BT /F1 8 Tf ").append(PW - MARGIN - 60).append(" 14 Td (Page ").append(pageNum).append(" / ").append(pageCount).append(") Tj ET\n");
    }

    private void roundRect(StringBuilder sb, float x, float y, float w, float h, float r) {
        float k = 0.5523f * r;
        sb.append(x + r).append(" ").append(y).append(" m\n")
                .append(x + w - r).append(" ").append(y).append(" l\n")
                .append(x + w - r + k).append(" ").append(y).append(" ").append(x + w).append(" ").append(y + r - k).append(" ").append(x + w).append(" ").append(y + r).append(" c\n")
                .append(x + w).append(" ").append(y + h - r).append(" l\n")
                .append(x + w).append(" ").append(y + h - r + k).append(" ").append(x + w - r + k).append(" ").append(y + h).append(" ").append(x + w - r).append(" ").append(y + h).append(" c\n")
                .append(x + r).append(" ").append(y + h).append(" l\n")
                .append(x + r - k).append(" ").append(y + h).append(" ").append(x).append(" ").append(y + h - r + k).append(" ").append(x).append(" ").append(y + h - r).append(" c\n")
                .append(x).append(" ").append(y + r).append(" l\n")
                .append(x).append(" ").append(y + r - k).append(" ").append(x + r - k).append(" ").append(y).append(" ").append(x + r).append(" ").append(y).append(" c\n");
    }

    private void drawCircle(StringBuilder sb, float cx, float cy, float r) {
        float k = 0.5523f * r;
        sb.append(cx).append(" ").append(cy + r).append(" m\n")
                .append(cx + k).append(" ").append(cy + r).append(" ").append(cx + r).append(" ").append(cy + k).append(" ").append(cx + r).append(" ").append(cy).append(" c\n")
                .append(cx + r).append(" ").append(cy - k).append(" ").append(cx + k).append(" ").append(cy - r).append(" ").append(cx).append(" ").append(cy - r).append(" c\n")
                .append(cx - k).append(" ").append(cy - r).append(" ").append(cx - r).append(" ").append(cy - k).append(" ").append(cx - r).append(" ").append(cy).append(" c\n")
                .append(cx - r).append(" ").append(cy + k).append(" ").append(cx - k).append(" ").append(cy + r).append(" ").append(cx).append(" ").append(cy + r).append(" c\n");
    }

    private float[] levelColor(String level) {
        if (level == null) return LEVEL_COLORS[3];
        return switch (level.toLowerCase()) {
            case "beginner"     -> LEVEL_COLORS[0];
            case "intermediate" -> LEVEL_COLORS[1];
            case "advanced"     -> LEVEL_COLORS[2];
            default             -> LEVEL_COLORS[3];
        };
    }

    private String pdfSafe(String s) {
        if (s == null) return "";
        return s.replace("\\", "").replace("(", "[").replace(")", "]")
                .replace("\n", " ").replace("\r", "").replace("\t", " ");
    }

    // ══════════════════════════════════════════════
    // POPUPS
    // ══════════════════════════════════════════════
    private void showErrorPopup(javafx.scene.Node anchor, String msg) {
        if (anchor == null) return;
        // ── FIX: fallback to any available window if anchor has no scene yet ──
        hidePopup(anchor);
        Platform.runLater(() -> {
            javafx.stage.Window win = (anchor.getScene() != null) ? anchor.getScene().getWindow() : getAnyWindow();
            if (win == null) return;
            HBox c = new HBox(10); c.setAlignment(Pos.CENTER_LEFT); c.setPadding(new Insets(10,16,10,14)); c.setMaxWidth(380);
            c.setStyle("-fx-background-color:#1A0A14;-fx-background-radius:12;-fx-border-color:#FB7185;-fx-border-radius:12;-fx-border-width:1.5;-fx-effect:dropshadow(gaussian,rgba(251,113,133,0.55),18,0,0,5);");
            Label icon = new Label("⚠"); icon.setStyle("-fx-text-fill:#FB7185;-fx-font-size:14px;-fx-font-weight:700;");
            Label lbl  = new Label(msg); lbl.setStyle("-fx-text-fill:#FECDD3;-fx-font-size:12px;-fx-font-weight:600;"); lbl.setWrapText(true); lbl.setMaxWidth(320);
            c.getChildren().addAll(icon, lbl);
            Popup popup = new Popup(); popup.setAutoHide(true); popup.getContent().add(c);
            javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            if (b != null) popup.show(win, b.getMinX(), b.getMaxY() + 5);
            else popup.show(win, win.getX() + 40, win.getY() + 100);
            popupMap.put(anchor, popup);
            FadeTransition fade = new FadeTransition(Duration.millis(700), c);
            fade.setFromValue(1.0); fade.setToValue(0.0);
            fade.setOnFinished(e -> { popup.hide(); popupMap.remove(anchor); });
            new SequentialTransition(new PauseTransition(Duration.seconds(3)), fade).play();
        });
    }

    private void showSuccessPopup(String msg) {
        Platform.runLater(() -> {
            javafx.stage.Window win = getAnyWindow();
            if (win == null) return;
            HBox c = new HBox(10); c.setAlignment(Pos.CENTER_LEFT); c.setPadding(new Insets(12,20,12,16)); c.setMaxWidth(420);
            c.setStyle("-fx-background-color:#0A1F14;-fx-background-radius:14;-fx-border-color:#34D399;-fx-border-radius:14;-fx-border-width:1.5;-fx-effect:dropshadow(gaussian,rgba(52,211,153,0.5),20,0,0,5);");
            Label icon = new Label("✓"); icon.setStyle("-fx-text-fill:#34D399;-fx-font-size:15px;-fx-font-weight:700;");
            Label lbl  = new Label(msg); lbl.setStyle("-fx-text-fill:#A7F3D0;-fx-font-size:13px;-fx-font-weight:600;");
            c.getChildren().addAll(icon, lbl);
            Popup popup = new Popup(); popup.setAutoHide(true); popup.getContent().add(c);
            popup.show(win, win.getX() + (win.getWidth() - 420) / 2.0, win.getY() + win.getHeight() - 100);
            FadeTransition fade = new FadeTransition(Duration.millis(600), c);
            fade.setFromValue(1.0); fade.setToValue(0.0); fade.setOnFinished(e -> popup.hide());
            new SequentialTransition(new PauseTransition(Duration.seconds(2.5)), fade).play();
        });
    }

    private void hidePopup(javafx.scene.Node anchor) { if (anchor == null) return; Popup p = popupMap.remove(anchor); if (p != null) p.hide(); }
    private void hideAllPopups() { new ArrayList<>(popupMap.values()).forEach(Popup::hide); popupMap.clear(); }

    // ══════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════
    private void showErr(Label l, boolean s) { if (l == null) return; l.setVisible(s); l.setManaged(s); }
    private void setBorderNeutral(TextField f) { if (f == null) return; f.setStyle(FIELD_STYLE_BASE + "-fx-border-color:" + BORDER_DEFAULT + ";"); }
    private void setBorderComboNeutral(ComboBox<?> c) { if (c == null) return; c.setStyle(COMBO_STYLE_BASE + "-fx-border-color:" + BORDER_DEFAULT + ";"); }
    private void setBorder(TextField f, boolean ok) { if (f == null) return; f.setStyle(FIELD_STYLE_BASE + "-fx-border-color:" + (ok ? BORDER_OK : BORDER_ERROR) + ";"); }
    private void setBorderCombo(ComboBox<?> c, boolean ok) { if (c == null) return; c.setStyle(COMBO_STYLE_BASE + "-fx-border-color:" + (ok ? BORDER_OK : BORDER_ERROR) + ";"); }

    @FXML public void validateTitreInline() {
        if (fieldTitre == null || suppressValidation) return;
        String t = fieldTitre.getText().trim();
        if (t.isEmpty()) { setBorderNeutral(fieldTitre); showErr(errTitre, false); return; }
        if (t.length() < Deck.TITRE_MIN_LENGTH) { String m = "Min " + Deck.TITRE_MIN_LENGTH + " chars (" + t.length() + "/" + Deck.TITRE_MIN_LENGTH + ")"; setBorder(fieldTitre, false); if (errTitre != null) errTitre.setText("⚠ " + m); showErr(errTitre, true); showErrorPopup(fieldTitre, m); }
        else if (t.length() > Deck.TITRE_MAX_LENGTH) { String m = "Max " + Deck.TITRE_MAX_LENGTH + " chars exceeded"; setBorder(fieldTitre, false); if (errTitre != null) errTitre.setText("⚠ " + m); showErr(errTitre, true); showErrorPopup(fieldTitre, m); }
        else { setBorder(fieldTitre, true); showErr(errTitre, false); hidePopup(fieldTitre); }
    }

    @FXML public void validateMatiereInline() {
        if (fieldMatiere == null || suppressValidation) return;
        String t = fieldMatiere.getText().trim();
        if (t.isEmpty()) { setBorderNeutral(fieldMatiere); showErr(errMatiere, false); return; }
        if (t.length() < Deck.MATIERE_MIN_LENGTH) { String m = "Min " + Deck.MATIERE_MIN_LENGTH + " chars"; setBorder(fieldMatiere, false); if (errMatiere != null) errMatiere.setText("⚠ " + m); showErr(errMatiere, true); showErrorPopup(fieldMatiere, m); }
        else if (t.length() > Deck.MATIERE_MAX_LENGTH) { String m = "Max " + Deck.MATIERE_MAX_LENGTH + " exceeded"; setBorder(fieldMatiere, false); if (errMatiere != null) errMatiere.setText("⚠ " + m); showErr(errMatiere, true); showErrorPopup(fieldMatiere, m); }
        else { setBorder(fieldMatiere, true); showErr(errMatiere, false); hidePopup(fieldMatiere); }
    }

    @FXML public void validateNiveauInline() {
        if (combNiveau == null || suppressValidation) return;
        boolean ok = combNiveau.getValue() != null;
        setBorderCombo(combNiveau, ok);
        if (!ok) { showErr(errNiveau, true); showErrorPopup(combNiveau, "Please select a level."); }
        else { showErr(errNiveau, false); hidePopup(combNiveau); }
    }

    @FXML public void validateImageInline() {
        if (fieldImage == null || suppressValidation) return;
        String path = fieldImage.getText().trim();
        if (path.isEmpty()) { setBorderNeutral(fieldImage); showErr(errImage, false); return; }
        File f = new File(path); String low = f.getName().toLowerCase();
        boolean extOk = Deck.IMAGE_EXTENSIONS.stream().anyMatch(e -> low.endsWith("." + e));
        if (!extOk) { String m = "Invalid format. Use: " + String.join(", ", Deck.IMAGE_EXTENSIONS); setBorder(fieldImage, false); if (errImage != null) errImage.setText("⚠ " + m); showErr(errImage, true); showErrorPopup(fieldImage, m); }
        else if (f.exists() && f.length() > Deck.FILE_MAX_BYTES) { String m = "Image too large. Max: " + Deck.FILE_MAX_SIZE_MB + "MB"; setBorder(fieldImage, false); if (errImage != null) errImage.setText("⚠ " + m); showErr(errImage, true); showErrorPopup(fieldImage, m); }
        else { setBorder(fieldImage, true); showErr(errImage, false); hidePopup(fieldImage); }
    }

    @FXML public void validatePdfInline() {
        // This is kept for FXML compatibility but validation is now driven by selectedPdfPaths
        // Validate each path in the list
        if (suppressValidation) return;
        boolean allOk = true;
        for (String path : selectedPdfPaths) {
            File f = new File(path);
            if (!f.getName().toLowerCase().endsWith(".pdf") || (f.exists() && f.length() > Deck.FILE_MAX_BYTES)) {
                allOk = false;
                break;
            }
        }
        if (!selectedPdfPaths.isEmpty() && !allOk) {
            showErr(errPdf, true);
        } else {
            showErr(errPdf, false);
        }
    }

    private boolean validateAll() {
        suppressValidation = false;
        validateTitreInline();
        validateMatiereInline();
        validateNiveauInline();

        if (fieldImage != null && fieldImage.getText().trim().isEmpty()) {
            setBorder(fieldImage, false);
            String m = "Image required";
            if (errImage != null) errImage.setText("⚠ " + m);
            showErr(errImage, true);
            showErrorPopup(fieldImage, m);
        } else {
            validateImageInline();
        }

        // ── FIX: validate against selectedPdfPaths, NOT fieldPdf ──
        if (selectedPdfPaths.isEmpty()) {
            if (errPdf != null) errPdf.setText("⚠  Au moins un PDF est obligatoire.");
            showErr(errPdf, true);
            // Show popup on addPdfBtn if available, else any node
            javafx.scene.Node popupAnchor = addPdfBtn != null ? addPdfBtn : fieldImage;
            if (popupAnchor != null) showErrorPopup(popupAnchor, "At least one PDF is required.");
        } else {
            showErr(errPdf, false);
        }

        boolean titreOk    = fieldTitre   != null && fieldTitre.getText().trim().length()   >= Deck.TITRE_MIN_LENGTH;
        boolean matiereOk  = fieldMatiere != null && fieldMatiere.getText().trim().length() >= Deck.MATIERE_MIN_LENGTH;
        boolean niveauOk   = combNiveau   != null && combNiveau.getValue()                  != null;
        boolean imageOk    = fieldImage   != null && !fieldImage.getText().trim().isEmpty() && (errImage == null || !errImage.isVisible());
        boolean pdfOk      = !selectedPdfPaths.isEmpty();

        return titreOk && matiereOk && niveauOk && imageOk && pdfOk;
    }

    // ══════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════
    private void goToList() {
        if (listView != null) {
            listView.setVisible(true);
            listView.setManaged(true);
        }
        if (formView != null) {
            formView.setVisible(false);
            formView.setManaged(false);
        }
        if (ratingView != null) {
            ratingView.setVisible(false);
            ratingView.setManaged(false);
        }
    }

    private void goToForm() {
        if (listView != null) {
            listView.setVisible(false);
            listView.setManaged(false);
        }
        if (formView != null) {
            formView.setVisible(true);
            formView.setManaged(true);
        }
        if (ratingView != null) {
            ratingView.setVisible(false);
            ratingView.setManaged(false);
        }
    }

    private void goToRatingView() {
        if (listView != null) {
            listView.setVisible(false);
            listView.setManaged(false);
        }
        if (formView != null) {
            formView.setVisible(false);
            formView.setManaged(false);
        }
        if (ratingView != null) {
            ratingView.setVisible(true);
            ratingView.setManaged(true);
        }
    }

    @FXML public void showAddView() { selectedDeck = null; clearForm(); if (formTitle != null) formTitle.setText("New Deck"); goToForm(); }

    @FXML public void handleCancel() { hideAllPopups(); clearForm(); goToList(); }

    @FXML public void handleRatingBack() {
        selectedRatingsDeck = null;
        clearRatingsPanel();
        goToList();
    }

    private void openEditForm(Deck d) {
        selectedDeck = d;
        suppressValidation = true;
        hideAllPopups();
        showErr(errTitre, false); showErr(errMatiere, false); showErr(errNiveau, false);
        showErr(errImage, false); showErr(errPdf, false);
        setBorderNeutral(fieldTitre); setBorderNeutral(fieldMatiere);
        setBorderComboNeutral(combNiveau); setBorderNeutral(fieldImage);
        if (imagePreviewLabel != null) { imagePreviewLabel.setVisible(false); imagePreviewLabel.setManaged(false); }
        if (pdfPreviewLabel   != null) { pdfPreviewLabel.setVisible(false);   pdfPreviewLabel.setManaged(false); }

        if (formTitle      != null) formTitle.setText("Edit Deck");
        if (fieldTitre     != null) fieldTitre.setText(d.getTitre());
        if (fieldMatiere   != null) fieldMatiere.setText(d.getMatiere());
        if (combNiveau     != null) combNiveau.setValue(d.getNiveau());
        if (fieldDescription != null) fieldDescription.setText(safe(d.getDescription()));
        if (fieldImage     != null && d.getImage() != null && !d.getImage().isEmpty()) {
            fieldImage.setText(d.getImage());
            setFileLabel(imagePreviewLabel, "✓ " + new File(d.getImage()).getName());
        }

        // ── Reload multi-PDF list ──
        selectedPdfPaths.clear();
        if (pdfListContainer != null) pdfListContainer.getChildren().clear();
        if (d.getPdf() != null && !d.getPdf().isEmpty()) {
            for (String p : d.getPdf().split("\\|")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    selectedPdfPaths.add(trimmed);
                    addPdfRow(trimmed);
                }
            }
            syncLegacyPdfField();
        }

        suppressValidation = false;
        applySilentBorderFeedback();
        goToForm();
    }

    private void applySilentBorderFeedback() {
        if (fieldTitre   != null) { String t = fieldTitre.getText().trim();   if (!t.isEmpty()) setBorder(fieldTitre,   t.length() >= Deck.TITRE_MIN_LENGTH   && t.length() <= Deck.TITRE_MAX_LENGTH); }
        if (fieldMatiere != null) { String t = fieldMatiere.getText().trim(); if (!t.isEmpty()) setBorder(fieldMatiere, t.length() >= Deck.MATIERE_MIN_LENGTH && t.length() <= Deck.MATIERE_MAX_LENGTH); }
        if (combNiveau   != null && combNiveau.getValue() != null) setBorderCombo(combNiveau, true);
        if (fieldImage   != null && !fieldImage.getText().trim().isEmpty()) {
            File f = new File(fieldImage.getText().trim()); String low = f.getName().toLowerCase();
            boolean ok = Deck.IMAGE_EXTENSIONS.stream().anyMatch(e -> low.endsWith("." + e));
            setBorder(fieldImage, ok);
        }
    }

    @FXML public void pickImage() {
        if (fieldImage == null) return;
        javafx.stage.Window win = getAnyWindow();
        if (win == null) return;
        FileChooser fc = new FileChooser(); fc.setTitle("Select Cover Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.bmp","*.webp"),
                new FileChooser.ExtensionFilter("All", "*.*"));
        File f = fc.showOpenDialog(win);
        if (f != null) { fieldImage.setText(f.getAbsolutePath()); setFileLabel(imagePreviewLabel, "✓ " + f.getName()); }
    }

    @FXML public void handleSave() {
        if (!validateAll()) return;
        String titre = fieldTitre.getText().trim();
        String mat   = fieldMatiere.getText().trim();
        String niv   = combNiveau.getValue();
        String desc  = fieldDescription != null ? fieldDescription.getText().trim() : "";
        String img   = fieldImage       != null ? fieldImage.getText().trim()       : "";
        String pdf   = String.join("|", selectedPdfPaths);   // pipe-separated
        try {
            if (selectedDeck == null) {
                deckService.add(new Deck(2, titre, mat, niv, desc, img, pdf));
                showSuccessPopup("Deck \"" + titre + "\" added!");
            } else {
                selectedDeck.setTitre(titre); selectedDeck.setMatiere(mat); selectedDeck.setNiveau(niv);
                selectedDeck.setDescription(desc); selectedDeck.setImage(img); selectedDeck.setPdf(pdf);
                deckService.update(selectedDeck);
                showSuccessPopup("Deck \"" + titre + "\" updated!");
            }
            clearForm(); goToList(); if (listView != null) refreshAll();
        } catch (IllegalArgumentException e) { showErrorPopup(fieldTitre, e.getMessage()); }
        catch (Exception e)               { showErrorPopup(fieldTitre, "Error: " + e.getMessage()); }
    }

    private void handleDelete(Deck d) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Delete Deck"); dlg.setHeaderText("Delete \"" + d.getTitre() + "\"?"); dlg.setContentText("This action cannot be undone.");
        Optional<ButtonType> r = dlg.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) { deckService.delete(d); refreshAll(); }
    }

    // ══════════════════════════════════════════════
    // REFRESH
    // ══════════════════════════════════════════════
    private void refreshAll() {
        decks = deckService.getAll();
        if (selectedRatingsDeck != null) {
            int selectedDeckId = selectedRatingsDeck.getIdDeck();
            selectedRatingsDeck = decks.stream()
                    .filter(deck -> deck.getIdDeck() == selectedDeckId)
                    .findFirst()
                    .orElse(null);
        }
        refreshDeckFlashcardsCache();
        applySearchFilter();
        if (selectedRatingsDeck == null) {
            clearRatingsPanel();
        } else if (ratingView != null && ratingView.isVisible()) {
            showDeckRatings(selectedRatingsDeck);
        }
    }

    private void setupSearchFilter() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, o, n) -> applySearchFilter());
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList("Newest","Oldest","Title A-Z","Title Z-A","Subject A-Z"));
            sortCombo.setValue("Newest");
            sortCombo.valueProperty().addListener((obs, o, n) -> applySearchFilter());
        }
    }

    private void applySearchFilter() {
        if (decks == null) return;
        String q = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Stream<Deck> stream = decks.stream();
        if (!q.isBlank()) stream = stream.filter(d -> deckSearchText(d).contains(q));
        List<Deck> filtered = stream.sorted(getDeckComparator()).toList();
        visibleDecks = filtered;
        updateOverviewStats(filtered);
        displayDecks(filtered);
        Platform.runLater(this::drawCharts);
    }

    private Comparator<Deck> getDeckComparator() {
        String v = sortCombo == null || sortCombo.getValue() == null ? "Newest" : sortCombo.getValue();
        Comparator<Deck> byDate    = Comparator.comparing(Deck::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Deck> byTitle   = Comparator.comparing(d -> safe(d.getTitre()).toLowerCase(Locale.ROOT));
        Comparator<Deck> bySubject = Comparator.comparing(d -> safe(d.getMatiere()).toLowerCase(Locale.ROOT));
        return switch (v) {
            case "Oldest"      -> byDate.thenComparingInt(Deck::getIdDeck);
            case "Title A-Z"   -> byTitle.thenComparingInt(Deck::getIdDeck);
            case "Title Z-A"   -> byTitle.reversed().thenComparingInt(Deck::getIdDeck);
            case "Subject A-Z" -> bySubject.thenComparingInt(Deck::getIdDeck);
            default            -> byDate.reversed().thenComparingInt(Deck::getIdDeck);
        };
    }

    private String deckSearchText(Deck d) {
        return String.join(" ", safe(d.getTitre()), safe(d.getMatiere()), safe(d.getNiveau()), safe(d.getDescription()),
                String.valueOf(d.getIdDeck()), d.getDateCreation() == null ? "" : d.getDateCreation().toString()).toLowerCase(Locale.ROOT);
    }

    // ══════════════════════════════════════════════
    // CARDS
    // ══════════════════════════════════════════════
    private void displayDecks(List<Deck> list) {
        if (decksGrid == null) return;
        decksGrid.getChildren().clear();
        if (list.isEmpty()) { Label e = new Label("✨ No decks yet. Click Create Deck to start!"); e.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;"); decksGrid.getChildren().add(e); return; }
        for (int i = 0; i < list.size(); i++) decksGrid.getChildren().add(buildCard(list.get(i), COLORS[i % COLORS.length], ICONS[i % ICONS.length]));
    }

    private VBox buildCard(Deck deck, String color, String iconLit) {
        VBox card = new VBox(0); card.setPrefWidth(260);
        String sN = "-fx-background-color:#0F172A;-fx-background-radius:16;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),12,0,0,4);";
        String sH = "-fx-background-color:#1E293B;-fx-background-radius:16;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,8);";
        card.setStyle(sN); card.setOnMouseEntered(e -> card.setStyle(sH)); card.setOnMouseExited(e -> card.setStyle(sN));
        StackPane header = new StackPane(); header.setPrefHeight(100);
        header.setStyle("-fx-background-color:" + grad(color) + ";-fx-background-radius:16 16 0 0;");
        String imgPath = deck.getImage();
        if (imgPath != null && !imgPath.trim().isEmpty()) {
            File f = new File(imgPath.trim());
            if (f.exists()) {
                try {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(260); iv.setFitHeight(100); iv.setPreserveRatio(false);
                    Rectangle clip = new Rectangle(260, 100); clip.setArcWidth(32); clip.setArcHeight(32); iv.setClip(clip);
                    Region overlay = new Region(); overlay.setStyle("-fx-background-color:rgba(0,0,0,0.28);-fx-background-radius:16 16 0 0;");
                    header.getChildren().addAll(iv, overlay);
                } catch (Exception ex) { addIcon(header, iconLit); }
            } else addIcon(header, iconLit);
        } else addIcon(header, iconLit);

        VBox content = new VBox(8); content.setPadding(new Insets(16));
        Label name = new Label(deck.getTitre()); name.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:800;-fx-font-size:15px;"); name.setWrapText(true);
        Label sub  = new Label(deck.getMatiere() + " • " + deck.getNiveau()); sub.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");

        List<Flashcard> deckFlashcards = getFlashcardsForDeck(deck);
        int totalCards    = deckFlashcards.size();
        int masteredCards = (int) deckFlashcards.stream().filter(fc -> "mastered".equalsIgnoreCase(fc.getEtat())).count();
        int dueCards      = (int) deckFlashcards.stream().filter(fc -> !"mastered".equalsIgnoreCase(fc.getEtat())).count();
        boolean forceMastered = masteredDeckIds.contains(deck.getIdDeck());
        double mastery = (totalCards == 0) ? (forceMastered ? 1.0 : 0.0) : (double) masteredCards / totalCards;

        HBox ph = new HBox();
        Label pt = new Label(masteredCards + "/" + totalCards + " mastered"); pt.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:10px;");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label pc = new Label(String.format("%.0f%%", mastery * 100)); pc.setStyle("-fx-text-fill:" + hex(color) + ";-fx-font-size:11px;-fx-font-weight:700;");
        ph.getChildren().addAll(pt, sp2, pc);
        ProgressBar bar = new ProgressBar(mastery); bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(6); bar.setStyle("-fx-accent:" + hex(color) + ";");
        content.getChildren().addAll(name, sub, ph, bar);
        if (deck.getDescription() != null && !deck.getDescription().isEmpty()) {
            Label desc = new Label(deck.getDescription()); desc.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;"); desc.setWrapText(true); desc.setMaxHeight(36);
            content.getChildren().add(desc);
        }

        HBox acts = new HBox(8); acts.setAlignment(Pos.CENTER_RIGHT); acts.setPadding(new Insets(12, 0, 0, 0));
        boolean alreadyM = (dueCards == 0 && totalCards > 0) || (totalCards == 0 && forceMastered);
        Button masterBtn = alreadyM ? chipBtn("✓ Mastered", "rgba(52,211,153,0.20)", "#34D399", "rgba(52,211,153,0.35)") : chipBtn("★ Master", "rgba(251,191,36,0.15)", "#FBBF24", "rgba(251,191,36,0.3)");
        masterBtn.setOnAction(e -> markDeckAsMastered(deck, masterBtn));
        Button ratingBtn = chipBtn("★ Rating", "rgba(236,72,153,0.15)", "#F472B6", "rgba(236,72,153,0.30)");
        Button editBtn = chipBtn("✎ Edit", "rgba(99,102,241,0.15)", "#818CF8", "rgba(99,102,241,0.3)");
        Button delBtn  = chipBtn("🗑 Delete", "rgba(244,63,94,0.15)", "#FB7185", "rgba(244,63,94,0.3)");
        ratingBtn.setOnAction(e -> showDeckRatings(deck));
        editBtn.setOnAction(e -> openEditForm(deck));
        delBtn.setOnAction(e -> handleDelete(deck));
        acts.getChildren().addAll(masterBtn, ratingBtn, editBtn, delBtn);
        content.getChildren().add(acts);
        card.getChildren().addAll(header, content);
        return card;
    }

    private void showDeckRatings(Deck deck) {
        selectedRatingsDeck = deck;
        goToRatingView();
        if (selectedRatingDeckLabel != null) {
            selectedRatingDeckLabel.setText(deck.getTitre() + "  |  " + deck.getMatiere());
        }

        List<Rating> ratings = ratingService.getRatingsForDeckAdmin(deck.getIdDeck());
        if (ratingsSummaryLabel != null) {
            if (ratings.isEmpty()) {
                ratingsSummaryLabel.setText("No student has rated this deck yet.");
            } else {
                double average = ratings.stream().mapToInt(Rating::getStars).average().orElse(0.0);
                ratingsSummaryLabel.setText(ratings.size() + " rating(s)  |  average " + String.format(Locale.US, "%.1f", average) + "/5");
            }
        }

        if (ratingsListContainer == null) {
            return;
        }

        ratingsListContainer.getChildren().clear();
        if (ratings.isEmpty()) {
            Label empty = new Label("No ratings available for this deck.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
            ratingsListContainer.getChildren().add(empty);
            return;
        }

        for (Rating rating : ratings) {
            ratingsListContainer.getChildren().add(buildRatingCard(rating));
        }
    }

    private void clearRatingsPanel() {
        if (selectedRatingDeckLabel != null) {
            selectedRatingDeckLabel.setText("No deck selected");
        }
        if (ratingsSummaryLabel != null) {
            ratingsSummaryLabel.setText("Click Rating on a deck to see who rated it.");
        }
        if (ratingsListContainer != null) {
            ratingsListContainer.getChildren().clear();
            Label placeholder = new Label("No ratings loaded yet.");
            placeholder.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
            ratingsListContainer.getChildren().add(placeholder);
        }
    }

    private VBox buildRatingCard(Rating rating) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color:#111827;-fx-background-radius:12;-fx-border-color:#1E293B;-fx-border-radius:12;-fx-border-width:1;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox studentBox = new VBox(3);
        HBox.setHgrow(studentBox, Priority.ALWAYS);

        Label studentName = new Label(displayRatingUser(rating));
        studentName.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:12px;-fx-font-weight:700;");

        Label studentMeta = new Label(buildStudentMeta(rating));
        studentMeta.setWrapText(true);
        studentMeta.setStyle("-fx-text-fill:#64748B;-fx-font-size:10px;");

        studentBox.getChildren().addAll(studentName, studentMeta);

        Label stars = new Label(rating.getStars() + "/5  " + rating.getStarsLabel());
        stars.setStyle("-fx-text-fill:" + rating.getStarsColor() + ";-fx-font-size:11px;-fx-font-weight:700;");

        header.getChildren().addAll(studentBox, stars);

        Label comment = new Label(buildRatingComment(rating));
        comment.setWrapText(true);
        comment.setStyle("-fx-text-fill:#CBD5E1;-fx-font-size:11px;");

        Label date = new Label("Date: " + formatRatingDate(rating));
        date.setStyle("-fx-text-fill:#475569;-fx-font-size:10px;");

        card.getChildren().addAll(header, comment, date);
        return card;
    }

    private String displayRatingUser(Rating rating) {
        if (rating.getUserName() != null && !rating.getUserName().isBlank()) {
            return rating.getUserName();
        }
        if (rating.getUsername() != null && !rating.getUsername().isBlank()) {
            return rating.getUsername();
        }
        return "User #" + rating.getUserId();
    }

    private String buildStudentMeta(Rating rating) {
        List<String> parts = new ArrayList<>();
        if (rating.getUserEmail() != null && !rating.getUserEmail().isBlank()) {
            parts.add(rating.getUserEmail());
        }
        if (rating.getUsername() != null && !rating.getUsername().isBlank()) {
            parts.add("@" + rating.getUsername());
        }
        return parts.isEmpty() ? "Student ID: " + rating.getUserId() : String.join("  |  ", parts);
    }

    private String buildRatingComment(Rating rating) {
        if (rating.getComment() != null && !rating.getComment().isBlank()) {
            return rating.getComment();
        }
        return "Meaning: " + rating.getStarsLabel();
    }

    private String formatRatingDate(Rating rating) {
        LocalDateTime ts = rating.getUpdatedAt() != null ? rating.getUpdatedAt() : rating.getCreatedAt();
        return ts == null ? "-" : ts.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private void markDeckAsMastered(Deck deck, Button anchor) {
        List<Flashcard> deckFlashcards = getFlashcardsForDeck(deck);
        boolean alreadyAllMastered = masteredDeckIds.contains(deck.getIdDeck()) ||
                (!deckFlashcards.isEmpty() && deckFlashcards.stream().allMatch(fc -> "mastered".equalsIgnoreCase(fc.getEtat())));
        if (alreadyAllMastered) {
            masteredDeckIds.remove(deck.getIdDeck());
            for (Flashcard fc : deckFlashcards) flashcardService.updateEtat(fc.getIdFlashcard(), "to_review");
            refreshAll();
            showSuccessPopup("↩ Deck \"" + deck.getTitre() + "\" unmarked — back to review.");
        } else {
            for (Flashcard fc : deckFlashcards) if (!"mastered".equalsIgnoreCase(fc.getEtat())) flashcardService.updateEtat(fc.getIdFlashcard(), "mastered");
            masteredDeckIds.add(deck.getIdDeck());
            refreshAll();
            showSuccessPopup("🏆 Deck \"" + deck.getTitre() + "\" marked as 100% mastered!");
        }
    }

    private void addIcon(StackPane p, String l) { FontIcon i = new FontIcon(l); i.setIconSize(40); i.setIconColor(Color.WHITE); p.getChildren().add(i); }

    private Button chipBtn(String t, String bg, String fg, String bgH) {
        String s  = "-fx-background-color:" + bg  + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:5 10;";
        String sh = "-fx-background-color:" + bgH + ";-fx-text-fill:" + fg + ";-fx-font-size:11px;-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:5 10;";
        Button b = new Button(t); b.setStyle(s); b.setOnMouseEntered(e -> b.setStyle(sh)); b.setOnMouseExited(e -> b.setStyle(s)); return b;
    }

    private void clearForm() {
        suppressValidation = true;
        if (fieldTitre      != null) fieldTitre.clear();
        if (fieldMatiere    != null) fieldMatiere.clear();
        if (combNiveau      != null) combNiveau.setValue(null);
        if (fieldDescription!= null) fieldDescription.clear();
        if (fieldImage      != null) fieldImage.clear();
        if (fieldPdf        != null) fieldPdf.clear();
        selectedPdfPaths.clear();
        if (pdfListContainer != null) pdfListContainer.getChildren().clear();
        resetBorder(fieldTitre); resetBorder(fieldMatiere); setBorderComboNeutral(combNiveau);
        resetBorder(fieldImage);
        showErr(errTitre, false); showErr(errMatiere, false); showErr(errNiveau, false);
        showErr(errImage, false); showErr(errPdf, false);
        if (imagePreviewLabel != null) { imagePreviewLabel.setVisible(false); imagePreviewLabel.setManaged(false); }
        if (pdfPreviewLabel   != null) { pdfPreviewLabel.setVisible(false);   pdfPreviewLabel.setManaged(false); }
        hideAllPopups();
        suppressValidation = false;
    }

    private void resetBorder(TextField f) { if (f == null) return; f.setStyle(FIELD_STYLE_BASE + "-fx-border-color:" + BORDER_DEFAULT + ";"); }
    private void setFileLabel(Label l, String t) { if (l == null) return; l.setText(t); l.setVisible(true); l.setManaged(true); }

    // ══════════════════════════════════════════════
    // CHARTS
    // ══════════════════════════════════════════════
    private void drawCharts() { drawDonut(); drawLine(); drawBars(); }

    private void drawDonut() {
        if (donutChartPane == null) return;
        double w = donutChartPane.getWidth() > 0 ? donutChartPane.getWidth() : 300, h = donutChartPane.getHeight() > 0 ? donutChartPane.getHeight() : 160;
        Canvas c = new Canvas(w, h); GraphicsContext g = c.getGraphicsContext2D();
        List<Flashcard> flashcards = getFlashcardsForDecks(visibleDecks);
        long masteredFC = flashcards.stream().filter(fc -> "mastered".equalsIgnoreCase(fc.getEtat())).count();
        long totalFC = flashcards.size();
        long forcedExtra = visibleDecks.stream().filter(d -> masteredDeckIds.contains(d.getIdDeck()) && getFlashcardsForDeck(d).isEmpty()).count();
        long grandTotal = totalFC + forcedExtra, grandMastered = masteredFC + forcedExtra;
        double pct = grandTotal == 0 ? 0 : (grandMastered * 100.0) / grandTotal;
        double cx = w / 2, cy = h / 2, r = Math.min(w, h) * 0.38, thick = r * 0.38;
        g.setStroke(Color.web("#1E293B")); g.setLineWidth(thick);
        g.strokeArc(cx - r, cy - r, r * 2, r * 2, 0, 360, javafx.scene.shape.ArcType.OPEN);
        if (pct > 0) { g.setStroke(Color.web(pct >= 100 ? "#34D399" : "#8B5CF6")); g.setLineWidth(thick); g.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -(pct / 100.0 * 360), javafx.scene.shape.ArcType.OPEN); }
        g.setFill(Color.web("#F8FAFC")); g.setFont(Font.font("System", FontWeight.BOLD, 22)); g.setTextAlign(TextAlignment.CENTER); g.fillText(String.format("%.0f%%", pct), cx, cy + 8);
        g.setFont(Font.font("System", 11)); g.setFill(Color.web("#64748B")); g.fillText("Success Rate", cx, cy + 24);
        donutChartPane.getChildren().setAll(c);
        donutChartPane.setOnMouseClicked(evt -> showChartDetailPopup(donutChartPane, "🎯 Global Success Rate",
                String.format("Total flashcards: %d\nMastered: %d\nTo review: %d\nSuccess rate: %.1f%%\nDecks force-mastered: %d", grandTotal, grandMastered, grandTotal - grandMastered, pct, forcedExtra),
                pct >= 100 ? "#34D399" : "#8B5CF6"));
        donutChartPane.setStyle("-fx-cursor:hand;");
    }

    private void drawLine() {
        if (lineChartPane == null) return;
        double w = lineChartPane.getWidth() > 0 ? lineChartPane.getWidth() : 300, h = lineChartPane.getHeight() > 0 ? lineChartPane.getHeight() : 150;
        Canvas c = new Canvas(w, h); GraphicsContext g = c.getGraphicsContext2D();
        LocalDate today = LocalDate.now(), start = today.minusDays(6);
        String[] days = new String[7];
        for (int i = 0; i < 7; i++) days[i] = start.plusDays(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        double[][] series = buildWeeklySeriesReal(start);
        String[] colors = {"#10B981","#8B5CF6","#F59E0B"};
        String[] labels = {"Decks created","Flashcards added","Study activity"};
        double padL = 36, padR = 12, padT = 14, padB = 30, gw = w - padL - padR, gh = h - padT - padB;
        double max = 2;
        for (double[] row : series) for (double v : row) max = Math.max(max, v);
        g.setStroke(Color.web("#1E293B")); g.setLineWidth(1);
        for (int i = 0; i <= 4; i++) { double y = padT + gh - (i / 4.0) * gh; g.strokeLine(padL, y, padL + gw, y); g.setFill(Color.web("#475569")); g.setFont(Font.font("System", 9)); g.setTextAlign(TextAlignment.RIGHT); g.fillText(String.valueOf((int) Math.round((i / 4.0) * max)), padL - 5, y + 3); }
        for (int i = 0; i < 7; i++) { double x = padL + (i / (double)(6)) * gw; g.setFill(Color.web(start.plusDays(i).equals(today) ? "#A78BFA" : "#475569")); g.setFont(Font.font("System", start.plusDays(i).equals(today) ? FontWeight.BOLD : FontWeight.NORMAL, 9)); g.setTextAlign(TextAlignment.CENTER); g.fillText(days[i], x, h - 5); }
        for (int s = 0; s < series.length; s++) {
            g.setStroke(Color.web(colors[s])); g.setLineWidth(s == 2 ? 1.5 : 2.2); g.setLineDashes(s == 2 ? 6 : 0);
            g.beginPath();
            for (int i = 0; i < 7; i++) { double x = padL + (i / (double)(6)) * gw, y = padT + gh - (series[s][i] / max) * gh; if (i == 0) g.moveTo(x, y); else g.lineTo(x, y); }
            g.stroke(); g.setLineDashes(0);
            for (int i = 0; i < 7; i++) { double x = padL + (i / (double)(6)) * gw, y = padT + gh - (series[s][i] / max) * gh; g.setFill(Color.web(colors[s])); g.fillOval(x - 3.5, y - 3.5, 7, 7); if (series[s][i] > 0) { g.setFill(Color.web("#F8FAFC")); g.setFont(Font.font("System", FontWeight.BOLD, 8)); g.setTextAlign(TextAlignment.CENTER); g.fillText(String.valueOf((int) series[s][i]), x, y - 6); } }
        }
        for (int s = 0; s < labels.length; s++) { double lx = padL + gw - 90, ly = padT + s * 13; g.setFill(Color.web(colors[s])); g.fillOval(lx, ly, 7, 7); g.setFill(Color.web("#94A3B8")); g.setFont(Font.font("System", 8)); g.setTextAlign(TextAlignment.LEFT); g.fillText(labels[s], lx + 10, ly + 7); }
        lineChartPane.getChildren().setAll(c);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) sb.append(String.format("%-6s  Decks:%-3.0f  Cards:%-3.0f  Study:%-3.0f\n", days[i], series[0][i], series[1][i], series[2][i]));
        lineChartPane.setOnMouseClicked(evt -> showChartDetailPopup(lineChartPane, "📅 Weekly Activity (Last 7 Days)", sb.toString().trim(), "#10B981"));
        lineChartPane.setStyle("-fx-cursor:hand;");
    }

    private void drawBars() {
        if (barChartPane == null) return;
        if (visibleDecks == null || visibleDecks.isEmpty()) { barChartPane.getChildren().clear(); return; }
        double w = barChartPane.getWidth() > 0 ? barChartPane.getWidth() : 300, h = barChartPane.getHeight() > 0 ? barChartPane.getHeight() : 130;
        Canvas c = new Canvas(w, h); GraphicsContext g = c.getGraphicsContext2D();
        int shown = Math.min(visibleDecks.size(), 8);
        double padL = 32, padR = 8, padT = 10, padB = 30, gw = w - padL - padR, gh = h - padT - padB;
        double groupW = gw / shown, barW = groupW * 0.32;
        g.setStroke(Color.web("#1E293B")); g.setLineWidth(1);
        for (int i = 0; i <= 4; i++) { double y = padT + gh - (i / 4.0) * gh; g.strokeLine(padL, y, padL + gw, y); g.setFill(Color.web("#475569")); g.setFont(Font.font("System", 9)); g.setTextAlign(TextAlignment.RIGHT); g.fillText((i * 25) + "%", padL - 4, y + 3); }
        StringBuilder detailSb = new StringBuilder("Deck mastery breakdown:\n\n");
        for (int i = 0; i < shown; i++) {
            Deck deck = visibleDecks.get(i);
            List<Flashcard> dfc = getFlashcardsForDeck(deck);
            int total = dfc.size(); boolean forced = masteredDeckIds.contains(deck.getIdDeck());
            double mPct = (total == 0) ? (forced ? 100.0 : 0.0) : (dfc.stream().filter(fc -> "mastered".equalsIgnoreCase(fc.getEtat())).count() * 100.0) / total;
            double dPct = 100.0 - mPct;
            String abbr = (deck.getTitre().length() > 4 ? deck.getTitre().substring(0, 4) : deck.getTitre()).toUpperCase();
            double cx = padL + i * groupW + groupW / 2;
            double bH1 = (mPct / 100.0) * gh, bx1 = cx - barW - 1, by1 = padT + gh - bH1;
            g.setFill(Color.web("#8B5CF6")); g.fillRoundRect(bx1, by1, barW, Math.max(bH1, 1), 4, 4);
            double bH2 = (dPct / 100.0) * gh, bx2 = cx + 1, by2 = padT + gh - bH2;
            g.setFill(Color.web("#F59E0B")); g.fillRoundRect(bx2, by2, barW, Math.max(bH2, 1), 4, 4);
            if (mPct > 5) { g.setFill(Color.web("#F8FAFC")); g.setFont(Font.font("System", FontWeight.BOLD, 7)); g.setTextAlign(TextAlignment.CENTER); g.fillText(String.format("%.0f%%", mPct), bx1 + barW / 2, by1 - 2); }
            g.setFill(Color.web("#64748B")); g.setFont(Font.font("System", 8)); g.setTextAlign(TextAlignment.CENTER); g.fillText(abbr, cx, h - 4);
            detailSb.append(String.format("%-12s  ✅ %.0f%%  ⏳ %.0f%%  (%d cards)\n", deck.getTitre().length() > 12 ? deck.getTitre().substring(0, 12) + "…" : deck.getTitre(), mPct, dPct, total));
        }
        g.setFill(Color.web("#8B5CF6")); g.fillRoundRect(padL, padT, 8, 8, 3, 3);
        g.setFill(Color.web("#94A3B8")); g.setFont(Font.font("System", 8)); g.setTextAlign(TextAlignment.LEFT); g.fillText("Mastered", padL + 11, padT + 7);
        g.setFill(Color.web("#F59E0B")); g.fillRoundRect(padL + 70, padT, 8, 8, 3, 3);
        g.setFill(Color.web("#94A3B8")); g.fillText("To Review", padL + 81, padT + 7);
        barChartPane.getChildren().setAll(c);
        String detail = detailSb.toString().trim();
        barChartPane.setOnMouseClicked(evt -> showChartDetailPopup(barChartPane, "📊 Mastery Distribution", detail, "#8B5CF6"));
        barChartPane.setStyle("-fx-cursor:hand;");
    }

    private double[][] buildWeeklySeriesReal(LocalDate start) {
        double[][] series = new double[3][7];
        if (visibleDecks == null) return series;
        for (Deck deck : visibleDecks) {
            if (deck.getDateCreation() != null) { int idx = (int)(deck.getDateCreation().toLocalDate().toEpochDay() - start.toEpochDay()); if (idx >= 0 && idx < 7) series[0][idx]++; }
            for (Flashcard fc : getFlashcardsForDeck(deck)) {
                if (fc.getDateCreation()     != null) { int idx = (int)(fc.getDateCreation().toLocalDate().toEpochDay()     - start.toEpochDay()); if (idx >= 0 && idx < 7) series[1][idx]++; }
                if (fc.getDateModification() != null) { int idx = (int)(fc.getDateModification().toLocalDate().toEpochDay() - start.toEpochDay()); if (idx >= 0 && idx < 7) series[2][idx]++; }
            }
        }
        return series;
    }

    private void showChartDetailPopup(javafx.scene.Node anchor, String title, String body, String accentHex) {
        Platform.runLater(() -> {
            if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null) return;
            hidePopup(anchor);
            VBox box = new VBox(8); box.setPadding(new Insets(14,18,14,16)); box.setMaxWidth(360);
            box.setStyle("-fx-background-color:#0D1B2A;-fx-background-radius:14;-fx-border-color:" + accentHex + ";-fx-border-radius:14;-fx-border-width:1.8;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.7),24,0,0,6);");
            Label titleLbl = new Label(title); titleLbl.setStyle("-fx-text-fill:" + accentHex + ";-fx-font-size:13px;-fx-font-weight:800;");
            Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color:" + accentHex + ";-fx-opacity:0.3;");
            Label bodyLbl = new Label(body); bodyLbl.setStyle("-fx-text-fill:#CBD5E1;-fx-font-size:11px;-fx-font-family:monospace;"); bodyLbl.setWrapText(true);
            box.getChildren().addAll(titleLbl, sep, bodyLbl);
            Popup popup = new Popup(); popup.setAutoHide(true); popup.getContent().add(box);
            javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            if (b != null) { double px = Math.min(b.getMinX() + 10, anchor.getScene().getWindow().getX() + anchor.getScene().getWindow().getWidth() - 380); popup.show(anchor.getScene().getWindow(), px, b.getMaxY() + 8); }
            popupMap.put(anchor, popup);
            FadeTransition fade = new FadeTransition(Duration.millis(600), box);
            fade.setFromValue(1.0); fade.setToValue(0.0);
            fade.setOnFinished(ev -> { popup.hide(); popupMap.remove(anchor); });
            new SequentialTransition(new PauseTransition(Duration.seconds(6)), fade).play();
        });
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════
    private void updateOverviewStats(List<Deck> deckList) {
        List<Flashcard> flashcards = getFlashcardsForDecks(deckList);
        long mastered = flashcards.stream().filter(fc -> "mastered".equalsIgnoreCase(fc.getEtat())).count();
        long due      = flashcards.stream().filter(fc -> !"mastered".equalsIgnoreCase(fc.getEtat())).count();
        if (totalDecksLabel != null) totalDecksLabel.setText(String.valueOf(deckList.size()));
        if (masteredLabel   != null) masteredLabel.setText(String.valueOf(mastered));
        if (dueReviewLabel  != null) dueReviewLabel.setText(String.valueOf(due));
        if (streakLabel     != null) streakLabel.setText(String.valueOf(calculateStudyStreak(deckList)));
    }

    private void refreshDeckFlashcardsCache() {
        flashcardsByDeck.clear();
        if (decks == null) return;
        for (Deck deck : decks) flashcardsByDeck.put(deck.getIdDeck(), flashcardService.getByDeck(deck.getIdDeck()));
    }

    private List<Flashcard> getFlashcardsForDeck(Deck deck) {
        if (deck == null) return List.of();
        return flashcardsByDeck.getOrDefault(deck.getIdDeck(), List.of());
    }

    private List<Flashcard> getFlashcardsForDecks(List<Deck> deckList) {
        if (deckList == null || deckList.isEmpty()) return List.of();
        List<Flashcard> result = new ArrayList<>();
        for (Deck deck : deckList) result.addAll(getFlashcardsForDeck(deck));
        return result;
    }

    private int calculateStudyStreak(List<Deck> deckList) {
        Set<LocalDate> activeDays = new HashSet<>();
        for (Deck deck : deckList) {
            if (deck.getDateCreation() != null) activeDays.add(deck.getDateCreation().toLocalDate());
            for (Flashcard fc : getFlashcardsForDeck(deck)) {
                if (fc.getDateCreation()     != null) activeDays.add(fc.getDateCreation().toLocalDate());
                if (fc.getDateModification() != null) activeDays.add(fc.getDateModification().toLocalDate());
            }
        }
        int streak = 0; LocalDate cursor = LocalDate.now();
        while (activeDays.contains(cursor)) { streak++; cursor = cursor.minusDays(1); }
        return streak;
    }

    private String safe(String v) { return v == null ? "" : v; }
    private String truncateStr(String s, int max) { if (s == null || s.isEmpty()) return ""; return s.length() <= max ? s : s.substring(0, max - 1) + "."; }
    private String hex(String c)  { return switch (c) { case "primary" -> "#A78BFA"; case "success" -> "#34D399"; case "warning" -> "#FBBF24"; case "danger" -> "#FB7185"; case "accent" -> "#FB923C"; default -> "#94A3B8"; }; }
    private String grad(String c) { return switch (c) { case "primary" -> "linear-gradient(to bottom right,#7C3AED,#8B5CF6)"; case "success" -> "linear-gradient(to bottom right,#059669,#10B981)"; case "warning" -> "linear-gradient(to bottom right,#D97706,#F59E0B)"; case "danger" -> "linear-gradient(to bottom right,#DC2626,#F43F5E)"; case "accent" -> "linear-gradient(to bottom right,#EA580C,#F97316)"; default -> "linear-gradient(to bottom right,#475569,#64748B)"; }; }
}
