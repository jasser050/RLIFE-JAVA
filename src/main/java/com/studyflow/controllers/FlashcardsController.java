package com.studyflow.controllers;

import com.studyflow.models.Flashcard;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlashcardsController — validation complète avec :
 *  - Popups d'erreur stylisés (sous chaque champ, auto-masqués 3s)
 *  - Bordures rouge/vert dynamiques
 *  - Compteurs de caractères en temps réel
 *  - Conseils 💡 intégrés dans le FXML
 *  - File pickers image / PDF avec contrôle de taille et extension
 */
public class FlashcardsController extends FlashcardsFeaturesController {

    /* ── FXML ids supplémentaires (ajoutés dans le nouveau FXML) ─────── */
    @FXML protected Label errTitre;
    @FXML protected Label errQuestion;
    @FXML protected Label errReponse;
    @FXML protected Label errDescription;
    @FXML protected Label errDifficulte;
    @FXML protected Label errEtat;
    @FXML protected Label errImage;
    @FXML protected Label errPdf;

    @FXML protected Label titleCharCount;
    @FXML protected Label questionCharCount;
    @FXML protected Label answerCharCount;

    @FXML protected TextField fieldImage;
    @FXML protected TextField fieldPdf;
    @FXML protected Label     imagePreviewLabel;
    @FXML protected Label     pdfPreviewLabel;

    // Suivi des popups actifs par ancre
    private final Map<Object, Popup> popupMap = new HashMap<>();

    // Styles de base
    private static final String FIELD_BASE =
            "-fx-background-color:#1E293B;-fx-text-fill:#F8FAFC;" +
                    "-fx-prompt-text-fill:#475569;-fx-border-width:2;" +
                    "-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-padding:11 16;-fx-font-size:13px;";

    private static final String TEXTAREA_BASE =
            "-fx-control-inner-background:#1E293B;-fx-background-color:#1E293B;" +
                    "-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                    "-fx-border-width:2;-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-font-size:13px;";

    private static final String COMBO_BASE =
            "-fx-background-color:#1E293B;-fx-border-width:2;" +
                    "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;";

    private static final String C_DEFAULT = "#334155";
    private static final String C_OK      = "#34D399";
    private static final String C_ERR     = "#FB7185";

    /* ══════════════════════════════════════════════════════════════════════
       INITIALIZE — appelé par la super-classe ou directement
       ══════════════════════════════════════════════════════════════════════ */
    @FXML
    public void initialize() {
        // ComboBox options
        if (combDifficulte != null)
            combDifficulte.setItems(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        if (combEtat != null)
            combEtat.setItems(FXCollections.observableArrayList("new", "learning", "mastered"));

        // Listeners validation inline
        attachListeners();

        // Reset toutes les bordures
        resetAllBorders();
    }

    /* ══════════════════════════════════════════════════════════════════════
       LISTENERS — validation en temps réel
       ══════════════════════════════════════════════════════════════════════ */
    private void attachListeners() {
        // Titre
        if (fieldTitre != null) {
            fieldTitre.textProperty().addListener((o, old, val) -> {
                updateCharCount(titleCharCount, val, Flashcard.TITRE_MAX);
                validateTitreInline();
                updateCompletion();
            });
        }
        // Question
        if (fieldQuestion != null) {
            fieldQuestion.textProperty().addListener((o, old, val) -> {
                updateCharCount(questionCharCount, val, Flashcard.QUESTION_MAX);
                validateQuestionInline();
                updateCompletion();
            });
        }
        // Réponse
        if (fieldReponse != null) {
            fieldReponse.textProperty().addListener((o, old, val) -> {
                updateCharCount(answerCharCount, val, Flashcard.REPONSE_MAX);
                validateReponseInline();
                updateCompletion();
            });
        }
        // Description (optionnel)
        if (fieldDescription != null) {
            fieldDescription.textProperty().addListener((o, old, val) -> {
                validateDescriptionInline();
                updateCompletion();
            });
        }
        // Difficulty
        if (combDifficulte != null) {
            combDifficulte.valueProperty().addListener((o, old, val) -> {
                validateDifficulteInline();
                updateCompletion();
            });
        }
        // Status
        if (combEtat != null) {
            combEtat.valueProperty().addListener((o, old, val) -> {
                validateEtatInline();
                updateCompletion();
            });
        }
        // Image
        if (fieldImage != null) {
            fieldImage.textProperty().addListener((o, old, val) -> validateImageInline());
        }
        // PDF
        if (fieldPdf != null) {
            fieldPdf.textProperty().addListener((o, old, val) -> validatePdfInline());
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       COMPTEUR DE CARACTÈRES
       ══════════════════════════════════════════════════════════════════════ */
    private void updateCharCount(Label counter, String text, int max) {
        if (counter == null) return;
        int len = text == null ? 0 : text.length();
        counter.setText(len + "/" + max);
        counter.setStyle("-fx-font-size:10px;-fx-text-fill:" +
                (len > max ? C_ERR : len > max * 0.85 ? "#FBBF24" : "#334155") + ";");
    }

    /* ══════════════════════════════════════════════════════════════════════
       COMPLETION LABEL
       ══════════════════════════════════════════════════════════════════════ */
    private void updateCompletion() {
        if (formCompletionLabel == null) return;
        int total = 5; // titre, question, réponse, difficulty, status
        int done  = 0;
        if (fieldTitre   != null && fieldTitre.getText().trim().length()   >= Flashcard.TITRE_MIN)    done++;
        if (fieldQuestion != null && fieldQuestion.getText().trim().length() >= Flashcard.QUESTION_MIN) done++;
        if (fieldReponse  != null && fieldReponse.getText().trim().length()  >= Flashcard.REPONSE_MIN)  done++;
        if (combDifficulte != null && combDifficulte.getValue() != null)  done++;
        if (combEtat       != null && combEtat.getValue() != null)         done++;
        int pct = (done * 100) / total;
        formCompletionLabel.setText(pct + "% complete");
        String color = pct == 100 ? C_OK : pct >= 60 ? "#FBBF24" : "#64748B";
        formCompletionLabel.setStyle(
                "-fx-background-color:#1E293B;-fx-text-fill:" + color + ";" +
                        "-fx-font-size:11px;-fx-font-weight:700;" +
                        "-fx-padding:6 14;-fx-background-radius:20;");
    }

    /* ══════════════════════════════════════════════════════════════════════
       VALIDATIONS INLINE — par champ
       ══════════════════════════════════════════════════════════════════════ */

    private void validateTitreInline() {
        if (fieldTitre == null) return;
        String v = fieldTitre.getText().trim();
        if (v.isEmpty()) {
            setFieldBorder(fieldTitre, null); hideErr(errTitre); return;
        }
        if (v.length() < Flashcard.TITRE_MIN) {
            String msg = "Minimum " + Flashcard.TITRE_MIN + " caractères (" + v.length() + "/" + Flashcard.TITRE_MIN + ").";
            setFieldBorder(fieldTitre, false); showErr(errTitre, msg); showErrorPopup(fieldTitre, msg);
        } else if (v.length() > Flashcard.TITRE_MAX) {
            String msg = "Maximum " + Flashcard.TITRE_MAX + " caractères dépassé.";
            setFieldBorder(fieldTitre, false); showErr(errTitre, msg); showErrorPopup(fieldTitre, msg);
        } else {
            setFieldBorder(fieldTitre, true); hideErr(errTitre); hidePopup(fieldTitre);
        }
    }

    private void validateQuestionInline() {
        if (fieldQuestion == null) return;
        String v = fieldQuestion.getText().trim();
        if (v.isEmpty()) {
            setAreaBorder(fieldQuestion, null); hideErr(errQuestion); return;
        }
        if (v.length() < Flashcard.QUESTION_MIN) {
            String msg = "Minimum " + Flashcard.QUESTION_MIN + " caractères (" + v.length() + "/" + Flashcard.QUESTION_MIN + ").";
            setAreaBorder(fieldQuestion, false); showErr(errQuestion, msg); showErrorPopup(fieldQuestion, msg);
        } else if (v.length() > Flashcard.QUESTION_MAX) {
            String msg = "Maximum " + Flashcard.QUESTION_MAX + " caractères dépassé.";
            setAreaBorder(fieldQuestion, false); showErr(errQuestion, msg); showErrorPopup(fieldQuestion, msg);
        } else {
            setAreaBorder(fieldQuestion, true); hideErr(errQuestion); hidePopup(fieldQuestion);
        }
    }

    private void validateReponseInline() {
        if (fieldReponse == null) return;
        String v = fieldReponse.getText().trim();
        if (v.isEmpty()) {
            setAreaBorder(fieldReponse, null); hideErr(errReponse); return;
        }
        if (v.length() < Flashcard.REPONSE_MIN) {
            String msg = "Minimum " + Flashcard.REPONSE_MIN + " caractères (" + v.length() + "/" + Flashcard.REPONSE_MIN + ").";
            setAreaBorder(fieldReponse, false); showErr(errReponse, msg); showErrorPopup(fieldReponse, msg);
        } else if (v.length() > Flashcard.REPONSE_MAX) {
            String msg = "Maximum " + Flashcard.REPONSE_MAX + " caractères dépassé.";
            setAreaBorder(fieldReponse, false); showErr(errReponse, msg); showErrorPopup(fieldReponse, msg);
        } else {
            setAreaBorder(fieldReponse, true); hideErr(errReponse); hidePopup(fieldReponse);
        }
    }

    private void validateDescriptionInline() {
        if (fieldDescription == null) return;
        String v = fieldDescription.getText().trim();
        if (v.isEmpty()) {
            setAreaBorder(fieldDescription, null); hideErr(errDescription); return;
        }
        if (v.length() > Flashcard.DESCRIPTION_MAX) {
            String msg = "Maximum " + Flashcard.DESCRIPTION_MAX + " caractères dépassé.";
            setAreaBorder(fieldDescription, false); showErr(errDescription, msg); showErrorPopup(fieldDescription, msg);
        } else {
            setAreaBorder(fieldDescription, true); hideErr(errDescription); hidePopup(fieldDescription);
        }
    }

    private void validateDifficulteInline() {
        if (combDifficulte == null) return;
        boolean ok = combDifficulte.getValue() != null;
        setComboBorder(combDifficulte, ok ? true : null);
        if (!ok) { showErr(errDifficulte, "Veuillez sélectionner une difficulté."); showErrorPopup(combDifficulte, "Veuillez sélectionner une difficulté."); }
        else      { hideErr(errDifficulte); hidePopup(combDifficulte); }
    }

    private void validateEtatInline() {
        if (combEtat == null) return;
        boolean ok = combEtat.getValue() != null;
        setComboBorder(combEtat, ok ? true : null);
        if (!ok) { showErr(errEtat, "Veuillez sélectionner un statut."); showErrorPopup(combEtat, "Veuillez sélectionner un statut."); }
        else      { hideErr(errEtat); hidePopup(combEtat); }
    }

    private void validateImageInline() {
        if (fieldImage == null) return;
        String path = fieldImage.getText().trim();
        if (path.isEmpty()) {
            setFieldBorder(fieldImage, null); hideErr(errImage); return;
        }
        File f = new File(path);
        String low = f.getName().toLowerCase();
        List<String> exts = Flashcard.IMAGE_EXTENSIONS;
        boolean extOk = exts.stream().anyMatch(e -> low.endsWith("." + e));
        if (!extOk) {
            String msg = "Format invalide. Acceptés : " + String.join(", ", exts) + ".";
            setFieldBorder(fieldImage, false); showErr(errImage, msg); showErrorPopup(fieldImage, msg);
        } else if (f.exists() && f.length() > Flashcard.FILE_MAX_BYTES) {
            String msg = "Fichier trop grand. Max : " + Flashcard.FILE_MAX_LABEL
                    + " (actuel : " + String.format("%.1f", f.length() / 1048576.0) + " Mo).";
            setFieldBorder(fieldImage, false); showErr(errImage, msg); showErrorPopup(fieldImage, msg);
        } else {
            setFieldBorder(fieldImage, true); hideErr(errImage); hidePopup(fieldImage);
        }
    }

    private void validatePdfInline() {
        if (fieldPdf == null) return;
        String path = fieldPdf.getText().trim();
        if (path.isEmpty()) {
            setFieldBorder(fieldPdf, null); hideErr(errPdf); return;
        }
        File f = new File(path);
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            String msg = "Seuls les fichiers .pdf sont acceptés.";
            setFieldBorder(fieldPdf, false); showErr(errPdf, msg); showErrorPopup(fieldPdf, msg);
        } else if (f.exists() && f.length() > Flashcard.FILE_MAX_BYTES) {
            String msg = "PDF trop grand. Max : " + Flashcard.FILE_MAX_LABEL
                    + " (actuel : " + String.format("%.1f", f.length() / 1048576.0) + " Mo).";
            setFieldBorder(fieldPdf, false); showErr(errPdf, msg); showErrorPopup(fieldPdf, msg);
        } else {
            setFieldBorder(fieldPdf, true); hideErr(errPdf); hidePopup(fieldPdf);
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       POPUP D'ERREUR STYLISÉ
       ══════════════════════════════════════════════════════════════════════ */
    private void showErrorPopup(javafx.scene.Node anchor, String message) {
        if (anchor == null) return;
        hidePopup(anchor);
        Platform.runLater(() -> {
            if (anchor.getScene() == null || anchor.getScene().getWindow() == null) return;

            HBox container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(10, 16, 10, 14));
            container.setMaxWidth(380);
            container.setStyle(
                    "-fx-background-color:#1A0A14;" +
                            "-fx-background-radius:12;" +
                            "-fx-border-color:#FB7185;" +
                            "-fx-border-radius:12;" +
                            "-fx-border-width:1.5;" +
                            "-fx-effect:dropshadow(gaussian,rgba(251,113,133,0.55),18,0,0,5);"
            );

            Label icon = new Label("⚠");
            icon.setStyle("-fx-text-fill:#FB7185;-fx-font-size:14px;-fx-font-weight:700;");

            Label lbl = new Label(message);
            lbl.setWrapText(true);
            lbl.setMaxWidth(320);
            lbl.setStyle("-fx-text-fill:#FECDD3;-fx-font-size:12px;-fx-font-weight:600;");

            container.getChildren().addAll(icon, lbl);

            Popup popup = new Popup();
            popup.setAutoHide(true);
            popup.getContent().add(container);

            javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
            if (bounds != null)
                popup.show(anchor.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 5);

            popupMap.put(anchor, popup);

            FadeTransition fade = new FadeTransition(Duration.millis(700), container);
            fade.setFromValue(1.0); fade.setToValue(0.0);
            fade.setOnFinished(e -> { popup.hide(); popupMap.remove(anchor); });
            new SequentialTransition(new PauseTransition(Duration.seconds(3)), fade).play();
        });
    }

    private void hidePopup(javafx.scene.Node anchor) {
        if (anchor == null) return;
        Popup p = popupMap.remove(anchor);
        if (p != null) p.hide();
    }

    /* ══════════════════════════════════════════════════════════════════════
       BORDURES
       ══════════════════════════════════════════════════════════════════════ */
    /** ok=true→vert, ok=false→rouge, ok=null→gris neutre */
    private void setFieldBorder(TextField f, Boolean ok) {
        if (f == null) return;
        f.setStyle(FIELD_BASE + "-fx-border-color:" + borderColor(ok) + ";");
    }

    private void setAreaBorder(TextArea a, Boolean ok) {
        if (a == null) return;
        a.setStyle(TEXTAREA_BASE + "-fx-border-color:" + borderColor(ok) + ";");
    }

    private void setComboBorder(ComboBox<?> c, Boolean ok) {
        if (c == null) return;
        c.setStyle(COMBO_BASE + "-fx-border-color:" + borderColor(ok) + ";");
    }

    private String borderColor(Boolean ok) {
        if (ok == null)  return C_DEFAULT;
        return ok ? C_OK : C_ERR;
    }

    private void resetAllBorders() {
        setFieldBorder(fieldTitre,   null);
        setAreaBorder (fieldQuestion, null);
        setAreaBorder (fieldReponse,  null);
        setAreaBorder (fieldDescription, null);
        setComboBorder(combDifficulte, null);
        setComboBorder(combEtat,       null);
        if (fieldImage != null) setFieldBorder(fieldImage, null);
        if (fieldPdf   != null) setFieldBorder(fieldPdf,   null);
    }

    /* ══════════════════════════════════════════════════════════════════════
       LABELS D'ERREUR
       ══════════════════════════════════════════════════════════════════════ */
    private void showErr(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText("⚠  " + msg);
        lbl.setVisible(true); lbl.setManaged(true);
    }

    private void hideErr(Label lbl) {
        if (lbl == null) return;
        lbl.setVisible(false); lbl.setManaged(false);
    }

    /* ══════════════════════════════════════════════════════════════════════
       FILE PICKERS
       ══════════════════════════════════════════════════════════════════════ */
    @FXML public void pickImage() {
        if (fieldImage == null || fieldImage.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File f = fc.showOpenDialog(fieldImage.getScene().getWindow());
        if (f != null) {
            fieldImage.setText(f.getAbsolutePath());
            setFileLabel(imagePreviewLabel, "✔  " + f.getName());
        }
    }

    @FXML public void pickPdf() {
        if (fieldPdf == null || fieldPdf.getScene() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner un PDF");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File f = fc.showOpenDialog(fieldPdf.getScene().getWindow());
        if (f != null) {
            fieldPdf.setText(f.getAbsolutePath());
            setFileLabel(pdfPreviewLabel, "✔  " + f.getName());
        }
    }

    private void setFileLabel(Label lbl, String text) {
        if (lbl == null) return;
        lbl.setText(text); lbl.setVisible(true); lbl.setManaged(true);
    }

    /* ══════════════════════════════════════════════════════════════════════
       VALIDATE ALL — appelé au Save
       ══════════════════════════════════════════════════════════════════════ */
    @Override
    protected boolean validateForm() {
        // Déclencher toutes les validations
        validateTitreInline();
        validateQuestionInline();
        validateReponseInline();
        validateDescriptionInline();
        validateDifficulteInline();
        validateEtatInline();
        validateImageInline();
        validatePdfInline();

        // Champs obligatoires vides → forcer rouge
        boolean ok = true;

        if (fieldTitre != null && fieldTitre.getText().trim().isEmpty()) {
            setFieldBorder(fieldTitre, false);
            showErr(errTitre, "Le titre est obligatoire (minimum " + Flashcard.TITRE_MIN + " caractères).");
            showErrorPopup(fieldTitre, "Le titre est obligatoire.");
            ok = false;
        }
        if (fieldQuestion != null && fieldQuestion.getText().trim().isEmpty()) {
            setAreaBorder(fieldQuestion, false);
            showErr(errQuestion, "La question est obligatoire (minimum " + Flashcard.QUESTION_MIN + " caractères).");
            showErrorPopup(fieldQuestion, "La question est obligatoire.");
            ok = false;
        }
        if (fieldReponse != null && fieldReponse.getText().trim().isEmpty()) {
            setAreaBorder(fieldReponse, false);
            showErr(errReponse, "La réponse est obligatoire (minimum " + Flashcard.REPONSE_MIN + " caractères).");
            showErrorPopup(fieldReponse, "La réponse est obligatoire.");
            ok = false;
        }
        if (combDifficulte != null && combDifficulte.getValue() == null) {
            setComboBorder(combDifficulte, false);
            showErr(errDifficulte, "Veuillez sélectionner une difficulté.");
            showErrorPopup(combDifficulte, "Veuillez sélectionner une difficulté.");
            ok = false;
        }
        if (combEtat != null && combEtat.getValue() == null) {
            setComboBorder(combEtat, false);
            showErr(errEtat, "Veuillez sélectionner un statut.");
            showErrorPopup(combEtat, "Veuillez sélectionner un statut.");
            ok = false;
        }

        // Vérifier erreurs restantes
        if (isErrVisible(errTitre) || isErrVisible(errQuestion) || isErrVisible(errReponse)
                || isErrVisible(errDescription) || isErrVisible(errDifficulte)
                || isErrVisible(errEtat) || isErrVisible(errImage) || isErrVisible(errPdf))
            return false;

        return ok;
    }

    private boolean isErrVisible(Label l) { return l != null && l.isVisible(); }

    /* ══════════════════════════════════════════════════════════════════════
       SAVE
       ══════════════════════════════════════════════════════════════════════ */
    @Override
    @FXML
    public void handleSave() {
        if (!validateForm()) return;

        String titre    = fieldTitre.getText().trim();
        String question = fieldQuestion.getText().trim();
        String reponse  = fieldReponse.getText().trim();
        String desc     = fieldDescription != null ? fieldDescription.getText().trim() : "";
        int    diff     = difficultyFromLabel(combDifficulte.getValue());
        String etat     = combEtat.getValue();
        String image    = fieldImage != null ? fieldImage.getText().trim() : "";
        String pdf      = fieldPdf   != null ? fieldPdf.getText().trim()   : "";

        try {
            if (selectedFlashcard == null) {
                Flashcard fc = new Flashcard(currentDeck.getIdDeck(), titre, question, reponse, desc, diff, currentUserId);
                fc.setEtat(etat);
                fc.setImage(image.isEmpty() ? null : image);
                fc.setPdf  (pdf.isEmpty()   ? null : pdf);
                flashcardService.add(fc);
                showSuccessPopup("Flashcard \"" + titre + "\" ajoutée avec succès !");
            } else {
                selectedFlashcard.setTitre(titre);
                selectedFlashcard.setQuestion(question);
                selectedFlashcard.setReponse(reponse);
                selectedFlashcard.setDescription(desc);
                selectedFlashcard.setNiveauDifficulte(diff);
                selectedFlashcard.setEtat(etat);
                selectedFlashcard.setImage(image.isEmpty() ? null : image);
                selectedFlashcard.setPdf  (pdf.isEmpty()   ? null : pdf);
                flashcardService.update(selectedFlashcard);
                showSuccessPopup("Flashcard \"" + titre + "\" mise à jour !");
            }
            clearForm();
            goToFlashcards();
            refreshFlashcards();
        } catch (Exception ex) {
            showCriticalError("Erreur : " + ex.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════════════════════
       POPUP SUCCÈS
       ══════════════════════════════════════════════════════════════════════ */
    private void showSuccessPopup(String message) {
        Platform.runLater(() -> {
            javafx.scene.Node anchor = fieldTitre;
            if (anchor == null || anchor.getScene() == null) {
                new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
                return;
            }
            HBox container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(12, 20, 12, 16));
            container.setMaxWidth(420);
            container.setStyle(
                    "-fx-background-color:#0A1F14;-fx-background-radius:14;" +
                            "-fx-border-color:#34D399;-fx-border-radius:14;-fx-border-width:1.5;" +
                            "-fx-effect:dropshadow(gaussian,rgba(52,211,153,0.5),20,0,0,5);");
            Label icon = new Label("✔");
            icon.setStyle("-fx-text-fill:#34D399;-fx-font-size:15px;-fx-font-weight:700;");
            Label lbl = new Label(message);
            lbl.setStyle("-fx-text-fill:#A7F3D0;-fx-font-size:13px;-fx-font-weight:600;");
            container.getChildren().addAll(icon, lbl);

            Popup popup = new Popup();
            popup.setAutoHide(true);
            popup.getContent().add(container);

            javafx.stage.Window win = anchor.getScene().getWindow();
            popup.show(win, win.getX() + (win.getWidth() - 420) / 2.0, win.getY() + win.getHeight() - 100);

            FadeTransition fade = new FadeTransition(Duration.millis(600), container);
            fade.setFromValue(1.0); fade.setToValue(0.0);
            fade.setOnFinished(e -> popup.hide());
            new SequentialTransition(new PauseTransition(Duration.seconds(2.5)), fade).play();
        });
    }

    private void showCriticalError(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.getDialogPane().setStyle(
                "-fx-background-color:#0F172A;-fx-border-color:#FB7185;-fx-border-width:1.5;");
        a.showAndWait();
    }

    /* ══════════════════════════════════════════════════════════════════════
       AUTRES ACTIONS
       ══════════════════════════════════════════════════════════════════════ */
    @Override
    @FXML
    public void showAddFlashcardForm() {
        selectedFlashcard = null;
        clearForm();
        if (formTitle != null) formTitle.setText("New Flashcard  -  " + currentDeck.getTitre());
        goToForm();
    }

    @Override
    @FXML
    public void handleCancel() {
        clearForm();
        goToFlashcards();
    }

    @Override
    protected void handleDelete(Flashcard fc) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la flashcard \"" + fc.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.getDialogPane().setStyle("-fx-background-color:#0F172A;-fx-border-color:#334155;-fx-border-width:1;");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                flashcardService.delete(fc);
                refreshFlashcards();
            }
        });
    }

    @Override
    protected void openEditForm(Flashcard fc) {
        selectedFlashcard = fc;
        if (formTitle != null) formTitle.setText("Edit Flashcard  -  " + currentDeck.getTitre());
        fieldTitre.setText(fc.getTitre());
        fieldQuestion.setText(fc.getQuestion());
        fieldReponse.setText(fc.getReponse());
        if (fieldDescription != null) fieldDescription.setText(fc.getDescription() != null ? fc.getDescription() : "");
        combDifficulte.setValue(fc.getDifficultyLabel());
        combEtat.setValue(fc.getEtat());
        if (fieldImage != null && fc.getImage() != null && !fc.getImage().isEmpty()) {
            fieldImage.setText(fc.getImage());
            setFileLabel(imagePreviewLabel, "✔  " + new File(fc.getImage()).getName());
        }
        if (fieldPdf != null && fc.getPdf() != null && !fc.getPdf().isEmpty()) {
            fieldPdf.setText(fc.getPdf());
            setFileLabel(pdfPreviewLabel, "✔  " + new File(fc.getPdf()).getName());
        }
        // Valider les champs pré-remplis → bordures vertes
        validateTitreInline(); validateQuestionInline(); validateReponseInline();
        validateDifficulteInline(); validateEtatInline();
        goToForm();
    }

    @Override
    protected void clearForm() {
        if (fieldTitre       != null) fieldTitre.clear();
        if (fieldQuestion    != null) fieldQuestion.clear();
        if (fieldReponse     != null) fieldReponse.clear();
        if (fieldDescription != null) fieldDescription.clear();
        if (combDifficulte   != null) combDifficulte.setValue(null);
        if (combEtat         != null) combEtat.setValue(null);
        if (fieldImage       != null) fieldImage.clear();
        if (fieldPdf         != null) fieldPdf.clear();

        resetAllBorders();

        hideErr(errTitre); hideErr(errQuestion); hideErr(errReponse);
        hideErr(errDescription); hideErr(errDifficulte); hideErr(errEtat);
        hideErr(errImage); hideErr(errPdf);

        if (imagePreviewLabel != null) { imagePreviewLabel.setVisible(false); imagePreviewLabel.setManaged(false); }
        if (pdfPreviewLabel   != null) { pdfPreviewLabel.setVisible(false);   pdfPreviewLabel.setManaged(false); }

        updateCharCount(titleCharCount,    "", Flashcard.TITRE_MAX);
        updateCharCount(questionCharCount, "", Flashcard.QUESTION_MAX);
        updateCharCount(answerCharCount,   "", Flashcard.REPONSE_MAX);
        updateCompletion();
    }
}