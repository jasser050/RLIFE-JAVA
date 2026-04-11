package com.studyflow.controllers;

import com.studyflow.models.Flashcard;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class FlashcardsController extends FlashcardsFeaturesController {

    @Override
    @FXML
    public void showAddFlashcardForm() {
        selectedFlashcard = null;
        clearForm();
        formTitle.setText("New Flashcard  -  " + currentDeck.getTitre());
        goToForm();
    }

    @Override
    @FXML
    public void handleSave() {
        if (!validateForm()) return;

        String titre = fieldTitre.getText().trim();
        String question = fieldQuestion.getText().trim();
        String reponse = fieldReponse.getText().trim();
        String desc = fieldDescription.getText().trim();
        int diff = difficultyFromLabel(combDifficulte.getValue());
        String etat = combEtat.getValue();

        try {
            if (selectedFlashcard == null) {
                Flashcard fc = new Flashcard(currentDeck.getIdDeck(), titre, question, reponse, desc, diff, currentUserId);
                fc.setEtat(etat);
                flashcardService.add(fc);
                showInfo("Flashcard \"" + titre + "\" added!");
            } else {
                selectedFlashcard.setTitre(titre);
                selectedFlashcard.setQuestion(question);
                selectedFlashcard.setReponse(reponse);
                selectedFlashcard.setDescription(desc);
                selectedFlashcard.setNiveauDifficulte(diff);
                selectedFlashcard.setEtat(etat);
                flashcardService.update(selectedFlashcard);
                showInfo("Flashcard \"" + titre + "\" updated!");
            }
            clearForm();
            goToFlashcards();
            refreshFlashcards();
        } catch (Exception ex) {
            alert("Error: " + ex.getMessage());
        }
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
                "Delete flashcard \"" + fc.getTitre() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
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
        formTitle.setText("Edit Flashcard  -  " + currentDeck.getTitre());
        fieldTitre.setText(fc.getTitre());
        fieldQuestion.setText(fc.getQuestion());
        fieldReponse.setText(fc.getReponse());
        fieldDescription.setText(fc.getDescription() != null ? fc.getDescription() : "");
        combDifficulte.setValue(fc.getDifficultyLabel());
        combEtat.setValue(fc.getEtat());
        goToForm();
    }

    @Override
    protected boolean validateForm() {
        if (fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            alert("Title is required.");
            return false;
        }
        if (fieldQuestion.getText() == null || fieldQuestion.getText().trim().isEmpty()) {
            alert("Question is required.");
            return false;
        }
        if (fieldReponse.getText() == null || fieldReponse.getText().trim().isEmpty()) {
            alert("Answer is required.");
            return false;
        }
        if (combDifficulte.getValue() == null) {
            alert("Please select a difficulty.");
            return false;
        }
        if (combEtat.getValue() == null) {
            alert("Please select a status.");
            return false;
        }
        return true;
    }

    @Override
    protected void clearForm() {
        fieldTitre.clear();
        fieldQuestion.clear();
        fieldReponse.clear();
        fieldDescription.clear();
        combDifficulte.setValue(null);
        combEtat.setValue(null);
    }
}
