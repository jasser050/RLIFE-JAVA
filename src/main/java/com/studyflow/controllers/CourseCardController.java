package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the CourseCard component
 */
public class CourseCardController implements Initializable {

    @FXML private StackPane iconBox;
    @FXML private FontIcon courseIcon;
    @FXML private Label codeLabel;
    @FXML private Label nameLabel;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;

    private static int cardIndex = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set different data for each card instance
        switch (cardIndex % 3) {
            case 0 -> setupCard("CS301", "Data Structures", 0.68, "primary");
            case 1 -> setupCard("CS305", "Database Systems", 0.45, "success");
            case 2 -> setupCard("CS202", "Algorithms", 0.82, "warning");
        }
        cardIndex++;
    }

    private void setupCard(String code, String name, double progress, String color) {
        codeLabel.setText(code);
        nameLabel.setText(name);
        progressBar.setProgress(progress);
        progressLabel.setText((int)(progress * 100) + "%");

        // Update color classes
        iconBox.getStyleClass().removeAll("primary", "success", "warning", "accent", "danger");
        iconBox.getStyleClass().add(color);

        progressBar.getStyleClass().removeAll("primary", "success", "warning", "accent", "danger");
        progressBar.getStyleClass().add(color);
    }

    /**
     * Set course data programmatically
     */
    public void setCourse(String code, String name, double progress, String color) {
        setupCard(code, name, progress, color);
    }
}
