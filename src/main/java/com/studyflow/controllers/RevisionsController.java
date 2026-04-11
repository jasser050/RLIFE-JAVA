package com.studyflow.controllers;

import com.studyflow.models.Deck;
import com.studyflow.services.DeckService;
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
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

public class RevisionsController implements Initializable {

    /* ── LIST VIEW ─────────────────────────────────────────────────────── */
    @FXML private VBox      listView;
    @FXML private Label     totalDecksLabel;
    @FXML private Label     masteredLabel;
    @FXML private Label     dueReviewLabel;
    @FXML private Label     streakLabel;
    @FXML private FlowPane  decksGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ToggleButton pdfOnlyCheck;
    @FXML private VBox      dueDecksBox;

    /* ── STAT CHART PANES ──────────────────────────────────────────────── */
    @FXML private StackPane donutChartPane;
    @FXML private StackPane lineChartPane;
    @FXML private StackPane barChartPane;

    /* ── FORM VIEW ─────────────────────────────────────────────────────── */
    @FXML private VBox             formView;
    @FXML private Label            formTitle;
    @FXML private TextField        fieldTitre;
    @FXML private TextField        fieldMatiere;
    @FXML private ComboBox<String> combNiveau;
    @FXML private TextArea         fieldDescription;
    @FXML private TextField        fieldImage;
    @FXML private TextField        fieldPdf;
    @FXML private Label            imagePreviewLabel;
    @FXML private Label            pdfPreviewLabel;

    /* ── ERROR LABELS ──────────────────────────────────────────────────── */
    @FXML private Label errTitre;
    @FXML private Label errMatiere;
    @FXML private Label errNiveau;

    /* ── State ─────────────────────────────────────────────────────────── */
    private final DeckService deckService = new DeckService();
    private List<Deck> decks;
    private Deck selectedDeck = null;
    private final Map<Integer, Double> masteryMap = new HashMap<>();

    private static final String[] COLORS = {"primary","success","warning","accent","danger"};
    private static final String[] ICONS  = {
            "fth-database","fth-trending-up","fth-terminal",
            "fth-grid","fth-wifi","fth-cpu","fth-hard-drive","fth-check-square"
    };

    private static final String BORDER_DEFAULT = "#334155";
    private static final String BORDER_ERROR   = "#FB7185";
    private static final String BORDER_OK      = "#34D399";

    /* ══════════════════════════════════════════════════════════════════════
       INITIALIZE
       ══════════════════════════════════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ComboBox niveau
        if (combNiveau != null)
            combNiveau.setItems(FXCollections.observableArrayList(
                    "Beginner", "Intermediate", "Advanced"));

        // Style TextArea
        if (fieldDescription != null)
            fieldDescription.setStyle(
                    "-fx-control-inner-background:#1E293B;-fx-background-color:#1E293B;" +
                            "-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                            "-fx-border-color:#334155;-fx-border-width:2;" +
                            "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");

        // ── CAS 1 : listView présent → mode complet (list + form) ──
        if (listView != null) {
            goToList();
            setupSearchFilter();
            refreshAll();
        }
        // ── CAS 2 : AddDeck.fxml standalone ──
        // Le FXML a déjà visible="true" sur les erreurs et bordures rouges
        // NE PAS appeler clearForm() ni showInitialErrors() ici
        // sinon ça écrase les valeurs du FXML
    }

    private void setupSearchFilter() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, o, n) -> applySearchFilter());
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Newest", "Oldest", "Title A-Z", "Title Z-A", "Subject A-Z"));
            sortCombo.setValue("Newest");
            sortCombo.valueProperty().addListener((obs, o, n) -> applySearchFilter());
        }
        if (pdfOnlyCheck != null)
            pdfOnlyCheck.selectedProperty().addListener((obs, o, n) -> applySearchFilter());
    }

    /* ══════════════════════════════════════════════════════════════════════
       NAVIGATION
       ══════════════════════════════════════════════════════════════════════ */
    private void goToList() {
        if (listView == null || formView == null) return;
        listView.setVisible(true);  listView.setManaged(true);
        formView.setVisible(false); formView.setManaged(false);
    }

    private void goToForm() {
        if (listView == null || formView == null) return;
        listView.setVisible(false); listView.setManaged(false);
        formView.setVisible(true);  formView.setManaged(true);
    }

    /* ══════════════════════════════════════════════════════════════════════
       VALIDATION INLINE  (rouge → vert automatique)
       ══════════════════════════════════════════════════════════════════════ */
    private void showErr(Label lbl, boolean show) {
        if (lbl == null) return;
        lbl.setVisible(show);
        lbl.setManaged(show);
    }

    private void setBorder(TextField field, boolean ok) {
        if (field == null) return;
        String color = ok ? BORDER_OK : BORDER_ERROR;
        String base  = field.getStyle().replaceAll("-fx-border-color:[^;]+;", "");
        field.setStyle(base + "-fx-border-color:" + color + ";");
    }

    private void setBorderCombo(ComboBox<?> combo, boolean ok) {
        if (combo == null) return;
        String color = ok ? BORDER_OK : BORDER_ERROR;
        String base  = combo.getStyle().replaceAll("-fx-border-color:[^;]+;", "");
        combo.setStyle(base + "-fx-border-color:" + color + ";");
    }

    // ✅ Appelé à chaque touche sur fieldTitre → rouge si < 3 chars, vert si ok
    @FXML public void validateTitreInline() {
        if (fieldTitre == null) return;
        boolean ok = fieldTitre.getText().trim().length() >= 3;
        setBorder(fieldTitre, ok);   // rouge ou vert automatiquement
        showErr(errTitre, !ok);      // cache le message si ok
    }

    // ✅ Appelé à chaque touche sur fieldMatiere
    @FXML public void validateMatiereInline() {
        if (fieldMatiere == null) return;
        boolean ok = fieldMatiere.getText().trim().length() >= Deck.MATIERE_MIN_LENGTH;
        setBorder(fieldMatiere, ok);
        showErr(errMatiere, !ok);
    }

    // ✅ Appelé dès qu'on sélectionne une valeur dans combNiveau
    @FXML public void validateNiveauInline() {
        if (combNiveau == null) return;
        boolean ok = combNiveau.getValue() != null;
        setBorderCombo(combNiveau, ok);
        showErr(errNiveau, !ok);
    }

    private boolean validateAll() {
        validateTitreInline();
        validateMatiereInline();
        validateNiveauInline();
        if (errTitre != null)
            return !errTitre.isVisible() && !errMatiere.isVisible() && !errNiveau.isVisible();
        return fieldTitre.getText().trim().length() >= 3
                && !fieldMatiere.getText().trim().isEmpty()
                && combNiveau.getValue() != null;
    }

    /* ══════════════════════════════════════════════════════════════════════
       BOUTON "+ Create Deck"  (depuis la liste)
       ══════════════════════════════════════════════════════════════════════ */
    @FXML public void showAddView() {
        selectedDeck = null;
        clearForm();
        if (formTitle != null) formTitle.setText("New Deck");
        // clearForm() a remis tout à gris/caché → on remet erreurs rouges
        showInitialErrors();
        goToForm();
    }

    // Remet les bordures rouges et les messages d'erreur visibles
    private void showInitialErrors() {
        setBorder(fieldTitre,      false);   // rouge
        setBorder(fieldMatiere,    false);   // rouge
        setBorderCombo(combNiveau, false);   // rouge
        showErr(errTitre,   true);
        showErr(errMatiere, true);
        showErr(errNiveau,  true);
    }

    /* ══════════════════════════════════════════════════════════════════════
       BOUTON "Edit"
       ══════════════════════════════════════════════════════════════════════ */
    private void openEditForm(Deck deck) {
        selectedDeck = deck;
        if (formTitle    != null) formTitle.setText("Edit Deck");
        if (fieldTitre   != null) fieldTitre.setText(deck.getTitre());
        if (fieldMatiere != null) fieldMatiere.setText(deck.getMatiere());
        if (combNiveau   != null) combNiveau.setValue(deck.getNiveau());
        if (fieldDescription != null) fieldDescription.setText(safe(deck.getDescription()));
        if (fieldImage != null && deck.getImage() != null && !deck.getImage().isEmpty()) {
            fieldImage.setText(deck.getImage());
            setFileLabel(imagePreviewLabel, "✔  " + new File(deck.getImage()).getName());
        }
        if (fieldPdf != null && deck.getPdf() != null && !deck.getPdf().isEmpty()) {
            fieldPdf.setText(deck.getPdf());
            setFileLabel(pdfPreviewLabel, "✔  " + new File(deck.getPdf()).getName());
        }
        // Valide les champs pré-remplis → bordures vertes automatiquement
        validateTitreInline();
        validateMatiereInline();
        validateNiveauInline();
        goToForm();
    }

    /* ══════════════════════════════════════════════════════════════════════
       FILE CHOOSERS
       ══════════════════════════════════════════════════════════════════════ */
    @FXML public void pickImage() {
        if (fieldImage == null || fieldImage.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Cover Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.bmp","*.webp"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File f = fc.showOpenDialog(fieldImage.getScene().getWindow());
        if (f != null) {
            fieldImage.setText(f.getAbsolutePath());
            setFileLabel(imagePreviewLabel, "✔  " + f.getName());
        }
    }

    @FXML public void pickPdf() {
        if (fieldPdf == null || fieldPdf.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Select PDF Document");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File f = fc.showOpenDialog(fieldPdf.getScene().getWindow());
        if (f != null) {
            fieldPdf.setText(f.getAbsolutePath());
            setFileLabel(pdfPreviewLabel, "✔  " + f.getName());
        }
    }

    private void setFileLabel(Label lbl, String text) {
        if (lbl == null) return;
        lbl.setText(text);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    /* ══════════════════════════════════════════════════════════════════════
       SAVE
       ══════════════════════════════════════════════════════════════════════ */
    @FXML public void handleSave() {
        if (!validateAll()) return;

        String titre       = fieldTitre.getText().trim();
        String matiere     = fieldMatiere.getText().trim();
        String niveau      = combNiveau.getValue();
        String description = fieldDescription != null ? fieldDescription.getText().trim() : "";
        String image       = fieldImage != null ? fieldImage.getText().trim() : "";
        String pdf         = fieldPdf   != null ? fieldPdf.getText().trim()   : "";

        try {
            if (selectedDeck == null) {
                Deck d = new Deck(2, titre, matiere, niveau, description, image, pdf);
                deckService.add(d);
                showInfo("Deck \"" + titre + "\" added successfully!");
            } else {
                selectedDeck.setTitre(titre);
                selectedDeck.setMatiere(matiere);
                selectedDeck.setNiveau(niveau);
                selectedDeck.setDescription(description);
                selectedDeck.setImage(image);
                selectedDeck.setPdf(pdf);
                deckService.update(selectedDeck);
                showInfo("Deck \"" + titre + "\" updated successfully!");
            }
            clearForm();
            goToList();
            if (listView != null) refreshAll();

        } catch (IllegalArgumentException e) {
            alert("⚠️ " + e.getMessage());
        } catch (Exception e) {
            alert("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       CANCEL
       ══════════════════════════════════════════════════════════════════════ */
    @FXML public void handleCancel() {
        clearForm();
        goToList();
    }

    /* ══════════════════════════════════════════════════════════════════════
       DELETE
       ══════════════════════════════════════════════════════════════════════ */
    private void handleDelete(Deck deck) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Delete Deck");
        dlg.setHeaderText("Delete \"" + deck.getTitre() + "\"?");
        dlg.setContentText("This action cannot be undone.");
        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            masteryMap.remove(deck.getIdDeck());
            deckService.delete(deck);
            refreshAll();
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       REFRESH
       ══════════════════════════════════════════════════════════════════════ */
    private void refreshAll() {
        decks = deckService.getAll();
        long masteredCount = masteryMap.values().stream().filter(v -> v >= 1.0).count();
        if (totalDecksLabel != null) totalDecksLabel.setText(String.valueOf(decks.size()));
        if (masteredLabel   != null) masteredLabel.setText(String.valueOf(masteredCount));
        if (dueReviewLabel  != null) dueReviewLabel.setText("0");
        if (streakLabel     != null) streakLabel.setText("14");
        applySearchFilter();
        displayDueDecks();
        Platform.runLater(this::drawCharts);
    }

    private void applySearchFilter() {
        if (decks == null) return;
        String query = (searchField == null || searchField.getText() == null)
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Stream<Deck> stream = decks.stream();
        if (!query.isBlank())
            stream = stream.filter(d -> deckSearchText(d).contains(query));
        if (pdfOnlyCheck != null && pdfOnlyCheck.isSelected())
            stream = stream.filter(d -> d.getPdf() != null && !d.getPdf().trim().isEmpty());
        List<Deck> filtered = stream.sorted(getDeckComparator()).toList();
        displayDecks(filtered);
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
        return String.join(" ",
                safe(d.getTitre()), safe(d.getMatiere()), safe(d.getNiveau()),
                safe(d.getDescription()), String.valueOf(d.getIdDeck()),
                d.getDateCreation() == null ? "" : d.getDateCreation().toString()
        ).toLowerCase(Locale.ROOT);
    }

    /* ══════════════════════════════════════════════════════════════════════
       DECK CARDS
       ══════════════════════════════════════════════════════════════════════ */
    private void displayDecks(List<Deck> list) {
        if (decksGrid == null) return;
        decksGrid.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("No decks yet. Click '+ Create Deck' to get started!");
            empty.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;");
            decksGrid.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < list.size(); i++)
            decksGrid.getChildren().add(buildCard(list.get(i), COLORS[i % COLORS.length], ICONS[i % ICONS.length]));
    }

    private VBox buildCard(Deck deck, String color, String iconLit) {
        VBox card = new VBox(0);
        card.setPrefWidth(240);
        String sN = "-fx-background-color:#0F172A;-fx-background-radius:16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),12,0,0,4);";
        String sH = "-fx-background-color:#1E293B;-fx-background-radius:16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,8);";
        card.setStyle(sN);
        card.setOnMouseEntered(e -> card.setStyle(sH));
        card.setOnMouseExited (e -> card.setStyle(sN));

        StackPane header = new StackPane();
        header.setPrefHeight(80);
        header.setStyle("-fx-background-color:" + grad(color) + ";-fx-background-radius:16 16 0 0;");
        String imgPath = deck.getImage();
        if (imgPath != null && !imgPath.trim().isEmpty()) {
            File f = new File(imgPath.trim());
            if (f.exists()) {
                try {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(240); iv.setFitHeight(80); iv.setPreserveRatio(false);
                    Rectangle clip = new Rectangle(240, 80);
                    clip.setArcWidth(32); clip.setArcHeight(32);
                    iv.setClip(clip);
                    Region overlay = new Region();
                    overlay.setStyle("-fx-background-color:rgba(0,0,0,0.28);-fx-background-radius:16 16 0 0;");
                    header.getChildren().addAll(iv, overlay);
                } catch (Exception ex) { addIcon(header, iconLit); }
            } else addIcon(header, iconLit);
        } else addIcon(header, iconLit);

        VBox content = new VBox(6);
        content.setPadding(new Insets(16));
        Label name = new Label(deck.getTitre());
        name.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:700;-fx-font-size:14px;");
        name.setWrapText(true);
        Label sub = new Label(deck.getMatiere() + " • " + deck.getNiveau());
        sub.setStyle("-fx-text-fill:#64748B;-fx-font-size:12px;");

        double mastery = masteryMap.getOrDefault(deck.getIdDeck(), 0.0);
        HBox ph = new HBox();
        Label pt = new Label("0/0 mastered");
        pt.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label pc = new Label(String.format("%.0f%%", mastery * 100));
        pc.setStyle("-fx-text-fill:" + hex(color) + ";-fx-font-size:11px;-fx-font-weight:700;");
        ph.getChildren().addAll(pt, sp, pc);

        ProgressBar bar = new ProgressBar(mastery);
        bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(6);
        bar.setStyle("-fx-accent:" + hex(color) + ";");
        content.getChildren().addAll(name, sub, ph, bar);

        if (deck.getDescription() != null && !deck.getDescription().isEmpty()) {
            Label desc = new Label(deck.getDescription());
            desc.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");
            desc.setWrapText(true); desc.setMaxHeight(32);
            content.getChildren().add(desc);
        }

        HBox acts = new HBox(6);
        acts.setAlignment(Pos.CENTER_RIGHT);
        acts.setPadding(new Insets(10, 0, 0, 0));

        boolean alreadyMastered = mastery >= 1.0;
        Button masterBtn = alreadyMastered
                ? chipBtn("✓ Mastered", "rgba(52,211,153,0.20)", "#34D399", "rgba(52,211,153,0.35)")
                : chipBtn("★ Master",   "rgba(251,191,36,0.15)", "#FBBF24", "rgba(251,191,36,0.3)");
        masterBtn.setOnAction(e -> {
            if (masteryMap.getOrDefault(deck.getIdDeck(), 0.0) >= 1.0)
                masteryMap.remove(deck.getIdDeck());
            else
                masteryMap.put(deck.getIdDeck(), 1.0);
            long mc = masteryMap.values().stream().filter(v -> v >= 1.0).count();
            if (masteredLabel != null) masteredLabel.setText(String.valueOf(mc));
            applySearchFilter();
        });

        Button editBtn = chipBtn("Edit",   "rgba(99,102,241,0.15)", "#818CF8", "rgba(99,102,241,0.3)");
        Button delBtn  = chipBtn("Delete", "rgba(244,63,94,0.15)",  "#FB7185", "rgba(244,63,94,0.3)");
        editBtn.setOnAction(e -> openEditForm(deck));
        delBtn .setOnAction(e -> handleDelete(deck));
        acts.getChildren().addAll(masterBtn, editBtn, delBtn);
        content.getChildren().add(acts);
        card.getChildren().addAll(header, content);
        return card;
    }

    private void addIcon(StackPane pane, String iconLit) {
        FontIcon ico = new FontIcon(iconLit);
        ico.setIconSize(32); ico.setIconColor(Color.WHITE);
        pane.getChildren().add(ico);
    }

    private Button chipBtn(String text, String bg, String fg, String bgH) {
        String s  = "-fx-background-color:" + bg  + ";-fx-text-fill:" + fg +
                ";-fx-font-size:11px;-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:6 10;";
        String sh = "-fx-background-color:" + bgH + ";-fx-text-fill:" + fg +
                ";-fx-font-size:11px;-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:6 10;";
        Button b = new Button(text);
        b.setStyle(s);
        b.setOnMouseEntered(e -> b.setStyle(sh));
        b.setOnMouseExited (e -> b.setStyle(s));
        return b;
    }

    private void displayDueDecks() {
        if (dueDecksBox == null) return;
        dueDecksBox.getChildren().clear();
        int i = 0;
        for (Deck deck : decks.stream().limit(4).toList()) {
            String color = COLORS[i % COLORS.length];
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(10, 12, 10, 12));
            String sN = "-fx-background-color:#1E293B;-fx-background-radius:10;-fx-cursor:hand;";
            String sH = "-fx-background-color:#334155;-fx-background-radius:10;-fx-cursor:hand;";
            item.setStyle(sN);
            item.setOnMouseEntered(e -> item.setStyle(sH));
            item.setOnMouseExited (e -> item.setStyle(sN));
            Region colorBar = new Region();
            colorBar.setPrefWidth(4); colorBar.setPrefHeight(32);
            colorBar.setStyle("-fx-background-color:" + hex(color) + ";-fx-background-radius:2;");
            VBox txt = new VBox(2);
            HBox.setHgrow(txt, Priority.ALWAYS);
            Label n = new Label(deck.getTitre());
            n.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:12px;-fx-font-weight:600;");
            n.setMaxWidth(150);
            Label s = new Label(deck.getMatiere());
            s.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
            txt.getChildren().addAll(n, s);
            StackPane play = new StackPane();
            play.setPrefSize(28, 28);
            play.setStyle("-fx-background-color:rgba(139,92,246,0.2);-fx-background-radius:100;");
            FontIcon pi = new FontIcon("fth-play");
            pi.setIconSize(12); pi.setIconColor(Color.web(hex(color)));
            play.getChildren().add(pi);
            item.getChildren().addAll(colorBar, txt, play);
            dueDecksBox.getChildren().add(item);
            i++;
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       CLEAR FORM
       ══════════════════════════════════════════════════════════════════════ */
    private void clearForm() {
        if (fieldTitre       != null) fieldTitre.clear();
        if (fieldMatiere     != null) fieldMatiere.clear();
        if (combNiveau       != null) combNiveau.setValue(null);
        if (fieldDescription != null) fieldDescription.clear();
        if (fieldImage       != null) fieldImage.clear();
        if (fieldPdf         != null) fieldPdf.clear();
        resetBorder(fieldTitre);
        resetBorder(fieldMatiere);
        if (combNiveau != null) combNiveau.setStyle(
                combNiveau.getStyle().replaceAll("-fx-border-color:[^;]+;","")
                        + "-fx-border-color:" + BORDER_DEFAULT + ";");
        showErr(errTitre,   false);
        showErr(errMatiere, false);
        showErr(errNiveau,  false);
        if (imagePreviewLabel != null) { imagePreviewLabel.setVisible(false); imagePreviewLabel.setManaged(false); }
        if (pdfPreviewLabel   != null) { pdfPreviewLabel.setVisible(false);   pdfPreviewLabel.setManaged(false); }
    }

    private void resetBorder(TextField f) {
        if (f == null) return;
        f.setStyle(f.getStyle().replaceAll("-fx-border-color:[^;]+;","")
                + "-fx-border-color:" + BORDER_DEFAULT + ";");
    }

    /* ══════════════════════════════════════════════════════════════════════
       DIALOGS
       ══════════════════════════════════════════════════════════════════════ */
    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    /* ══════════════════════════════════════════════════════════════════════
       CHARTS
       ══════════════════════════════════════════════════════════════════════ */
    private void drawCharts() {
        if (decks == null) return;
        drawDonut(); drawLine(); drawBars();
    }

    private void drawDonut() {
        if (donutChartPane == null) return;
        double w = donutChartPane.getWidth()  > 0 ? donutChartPane.getWidth()  : 300;
        double h = donutChartPane.getHeight() > 0 ? donutChartPane.getHeight() : 160;
        Canvas c = new Canvas(w, h); GraphicsContext g = c.getGraphicsContext2D();
        double pct = decks.isEmpty() ? 0 : Math.min(88.0, 60 + decks.size() * 4.0);
        double cx = w/2, cy = h/2, r = Math.min(w,h)*0.38, thick = r*0.38;
        g.setStroke(Color.web("#1E293B")); g.setLineWidth(thick);
        g.strokeArc(cx-r, cy-r, r*2, r*2, 0, 360, javafx.scene.shape.ArcType.OPEN);
        g.setStroke(Color.web("#8B5CF6")); g.setLineWidth(thick);
        g.strokeArc(cx-r, cy-r, r*2, r*2, 90, -(pct/100.0*360), javafx.scene.shape.ArcType.OPEN);
        g.setFill(Color.web("#F8FAFC")); g.setFont(Font.font("System", FontWeight.BOLD, 22));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(String.format("%.0f%%", pct), cx, cy+8);
        g.setFont(Font.font("System", 11)); g.setFill(Color.web("#64748B"));
        g.fillText("Réussite", cx, cy+24);
        donutChartPane.getChildren().setAll(c);
    }

    private void drawLine() {
        if (lineChartPane == null) return;
        double w = lineChartPane.getWidth()  > 0 ? lineChartPane.getWidth()  : 300;
        double h = lineChartPane.getHeight() > 0 ? lineChartPane.getHeight() : 150;
        Canvas c = new Canvas(w, h); GraphicsContext g = c.getGraphicsContext2D();
        String[] days = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        double[][] series = {{3,5,4,7,6,8,5},{2,4,6,5,8,7,9},{1,3,2,4,5,6,4}};
        String[] colors = {"#10B981","#8B5CF6","#F59E0B"};
        double padL=30,padR=10,padT=10,padB=30, gw=w-padL-padR, gh=h-padT-padB;
        int n = days.length;
        g.setStroke(Color.web("#1E293B")); g.setLineWidth(1);
        for (int i=0; i<=5; i++) {
            double y = padT+gh-(i/5.0)*gh;
            g.strokeLine(padL, y, padL+gw, y);
            g.setFill(Color.web("#475569")); g.setFont(Font.font("System",10));
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(String.valueOf(i*2), padL-4, y+4);
        }
        for (int i=0; i<n; i++) {
            double x = padL+(i/(double)(n-1))*gw;
            g.setFill(Color.web("#475569")); g.setFont(Font.font("System",10));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(days[i], x, h-6);
        }
        for (int s=0; s<series.length; s++) {
            g.setStroke(Color.web(colors[s])); g.setLineWidth(2);
            g.setLineDashes(s==2?5:0); g.beginPath();
            for (int i=0; i<n; i++) {
                double x=padL+(i/(double)(n-1))*gw, y=padT+gh-(series[s][i]/10.0)*gh;
                if(i==0) g.moveTo(x,y); else g.lineTo(x,y);
            }
            g.stroke(); g.setLineDashes(0);
            g.setFill(Color.web(colors[s]));
            for (int i=0; i<n; i++) {
                double x=padL+(i/(double)(n-1))*gw, y=padT+gh-(series[s][i]/10.0)*gh;
                g.fillOval(x-3, y-3, 6, 6);
            }
        }
        lineChartPane.getChildren().setAll(c);
    }

    private void drawBars() {
        if (barChartPane == null || decks == null || decks.isEmpty()) return;
        double w = barChartPane.getWidth()  > 0 ? barChartPane.getWidth()  : 300;
        double h = barChartPane.getHeight() > 0 ? barChartPane.getHeight() : 130;
        Canvas c = new Canvas(w, h); GraphicsContext g = c.getGraphicsContext2D();
        int shown = Math.min(decks.size(), 6);
        double padL=28,padR=8,padT=10,padB=28, gw=w-padL-padR, gh=h-padT-padB;
        double groupW=gw/shown, barW=groupW*0.35;
        String[] barColors = {"#8B5CF6","#F59E0B"};
        g.setStroke(Color.web("#1E293B")); g.setLineWidth(1);
        for (int i=0; i<=4; i++) {
            double y=padT+gh-(i/4.0)*gh;
            g.strokeLine(padL,y,padL+gw,y);
            g.setFill(Color.web("#475569")); g.setFont(Font.font("System",9));
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText((i*25)+"%", padL-3, y+3);
        }
        for (int i=0; i<shown; i++) {
            Deck deck = decks.get(i);
            String abbr = deck.getTitre().length()>3
                    ? deck.getTitre().substring(0,3).toUpperCase()
                    : deck.getTitre().toUpperCase();
            double cx = padL+i*groupW+groupW/2;
            double realM = masteryMap.getOrDefault(deck.getIdDeck(),0.0)*100;
            double[] vals = {realM>0?realM:(60+(i*7)%40), realM>0?realM:(10+(i*11)%35)};
            for (int b=0; b<2; b++) {
                double barH=(vals[b]/100.0)*gh, bx=cx+(b==0?-barW-1:1), by=padT+gh-barH;
                g.setFill(Color.web(barColors[b]));
                g.fillRoundRect(bx, by, barW, barH, 4, 4);
            }
            g.setFill(Color.web("#64748B")); g.setFont(Font.font("System",9));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(abbr, cx, h-4);
        }
        barChartPane.getChildren().setAll(c);
    }

    /* ══════════════════════════════════════════════════════════════════════
       COLOR HELPERS
       ══════════════════════════════════════════════════════════════════════ */
    private String safe(String v) { return v == null ? "" : v; }

    private String hex(String c) {
        return switch (c) {
            case "primary" -> "#A78BFA"; case "success" -> "#34D399";
            case "warning" -> "#FBBF24"; case "danger"  -> "#FB7185";
            case "accent"  -> "#FB923C"; default        -> "#94A3B8";
        };
    }

    private String grad(String c) {
        return switch (c) {
            case "primary" -> "linear-gradient(to bottom right,#7C3AED,#8B5CF6)";
            case "success" -> "linear-gradient(to bottom right,#059669,#10B981)";
            case "warning" -> "linear-gradient(to bottom right,#D97706,#F59E0B)";
            case "danger"  -> "linear-gradient(to bottom right,#DC2626,#F43F5E)";
            case "accent"  -> "linear-gradient(to bottom right,#EA580C,#F97316)";
            default        -> "linear-gradient(to bottom right,#475569,#64748B)";
        };
    }
}