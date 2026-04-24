package com.studyflow.controllers;

import com.studyflow.models.Flashcard;
import com.studyflow.services.AIFlashcardService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class AIGeneratorController {

    @FXML private TextArea         topicArea;
    @FXML private Spinner<Integer> countSpinner;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private Label            statusLabel;
    @FXML private ProgressBar      progressBar;
    @FXML private Button           generateBtn;
    @FXML private Button           cancelBtn;

    private Consumer<List<Flashcard>> onSuccess;
    private final AIFlashcardService  aiService = new AIFlashcardService();

    @FXML
    public void initialize() {
        // ✅ FIX 1 : toujours initialiser le SpinnerValueFactory en Java
        // Le FXML min/max/initialValue ne suffit pas dans toutes les versions JavaFX
        countSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5)
        );
        countSpinner.setEditable(true);

        // ✅ FIX 2 : forcer la valeur si l'utilisateur tape manuellement
        countSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int val = Integer.parseInt(newVal.trim());
                if (val < 1)  countSpinner.getValueFactory().setValue(1);
                else if (val > 20) countSpinner.getValueFactory().setValue(20);
            } catch (NumberFormatException ignored) {}
        });

        difficultyCombo.setItems(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        difficultyCombo.setValue("Medium");

        // Cache les éléments de statut au départ
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
    }

    public void setOnSuccess(Consumer<List<Flashcard>> callback) {
        this.onSuccess = callback;
    }

    @FXML
    private void onGenerate() {
        String topic = topicArea.getText().trim();
        if (topic.isEmpty()) {
            showStatus("⚠  Please enter a topic or text.", "#FBBF24");
            return;
        }

        // ✅ FIX 3 : commit la valeur du spinner avant de la lire
        // (évite NullPointerException si l'utilisateur a tapé manuellement)
        countSpinner.commitValue();
        int count = countSpinner.getValue() != null ? countSpinner.getValue() : 5;

        String difficulty = difficultyCombo.getValue() != null
                ? difficultyCombo.getValue() : "Medium";

        setLoading(true);
        showStatus("🤖  Generating " + count + " flashcards...", "#A78BFA");

        // ✅ FIX 4 : nommer le thread pour faciliter le debug
        Thread t = new Thread(() -> {
            try {
                List<Flashcard> cards = aiService.generate(topic, count, difficulty);
                Platform.runLater(() -> {
                    setLoading(false);
                    if (cards == null || cards.isEmpty()) {
                        showStatus("⚠  No cards were generated. Try a different topic.", "#FBBF24");
                        return;
                    }
                    if (onSuccess != null) onSuccess.accept(cards);
                    closeDialog();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    showStatus("❌  Error: " + msg, "#FB7185");
                    System.err.println("[AIGenerator] Generation failed: " + msg);
                    e.printStackTrace();
                });
            }
        }, "AI-Flashcard-Generator");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void setLoading(boolean loading) {
        generateBtn.setDisable(loading);
        cancelBtn.setDisable(loading);
        progressBar.setVisible(loading);
        progressBar.setManaged(loading);
        if (loading) progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    private void showStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
                "-fx-text-fill:" + color + ";" +
                        "-fx-font-size:11px;" +
                        "-fx-font-weight:600;"
        );
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}