package com.studyflow.controllers;

import com.studyflow.models.Rating;
import com.studyflow.services.RatingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminRatingsController implements Initializable {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label totalRatingsLabel;
    @FXML private Label uniqueStudentsLabel;
    @FXML private Label lowRatingsLabel;
    @FXML private Label averageLabel;

    @FXML private TableView<Rating> ratingsTable;
    @FXML private TableColumn<Rating, String> colStudent;
    @FXML private TableColumn<Rating, String> colEmail;
    @FXML private TableColumn<Rating, String> colDeck;
    @FXML private TableColumn<Rating, String> colSubject;
    @FXML private TableColumn<Rating, String> colStars;
    @FXML private TableColumn<Rating, String> colComment;
    @FXML private TableColumn<Rating, String> colDate;

    private final RatingService ratingService = new RatingService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTable();
        loadRatings();
    }

    @FXML
    private void refreshRatings() {
        loadRatings();
    }

    private void configureTable() {
        ratingsTable.setPlaceholder(new Label("No ratings yet."));

        colStudent.setCellValueFactory(cell -> new SimpleStringProperty(displayStudent(cell.getValue())));
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(fallback(cell.getValue().getUserEmail(), "-")));
        colDeck.setCellValueFactory(cell -> new SimpleStringProperty(fallback(cell.getValue().getDeckName(), "Unknown deck")));
        colSubject.setCellValueFactory(cell -> new SimpleStringProperty(fallback(cell.getValue().getDeckSubject(), "-")));
        colStars.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStars() + " / 5"));
        colComment.setCellValueFactory(cell -> new SimpleStringProperty(buildComment(cell.getValue())));
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue())));

        colStars.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                Rating rating = getTableView().getItems().get(getIndex());
                setText(item + "  " + rating.getStarsLabel());
                setStyle("-fx-text-fill: " + rating.getStarsColor() + "; -fx-font-weight: bold;");
            }
        });
    }

    private void loadRatings() {
        List<Rating> ratings = ratingService.getAllRatingsForAdmin();
        ratingsTable.setItems(FXCollections.observableArrayList(ratings));

        long total = ratings.size();
        long uniqueStudents = ratings.stream().map(Rating::getUserId).distinct().count();
        long lowRatings = ratings.stream().filter(Rating::isWeak).count();
        double average = ratings.stream().mapToInt(Rating::getStars).average().orElse(0.0);

        totalRatingsLabel.setText(String.valueOf(total));
        uniqueStudentsLabel.setText(String.valueOf(uniqueStudents));
        lowRatingsLabel.setText(String.valueOf(lowRatings));
        averageLabel.setText(total == 0 ? "-" : String.format("%.1f / 5", average));
    }

    private String displayStudent(Rating rating) {
        String name = fallback(rating.getUserName(), "");
        if (!name.isBlank()) {
            return name;
        }
        return fallback(rating.getUsername(), "User #" + rating.getUserId());
    }

    private String buildComment(Rating rating) {
        String comment = fallback(rating.getComment(), "").trim();
        if (!comment.isEmpty()) {
            return comment;
        }
        return rating.getStarsLabel();
    }

    private String formatDate(Rating rating) {
        LocalDateTime date = rating.getUpdatedAt() != null ? rating.getUpdatedAt() : rating.getCreatedAt();
        return date != null ? DATE_FORMAT.format(date) : "-";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
