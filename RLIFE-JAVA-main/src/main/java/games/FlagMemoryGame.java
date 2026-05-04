package games;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FlagMemoryGame {
    private final List<String> pool = GeoGameData.FLAGS.stream().map(GeoGameData.FlagCountry::flag).toList();
    private Runnable onClose;
    private int score, mistakes;
    private long startMs;
    private Card first, second;
    private GridPane grid = new GridPane();
    private Label stats = new Label();
    private int pairs;

    public static void open(Stage owner, Runnable onClose) { new FlagMemoryGame().show(owner, onClose); }

    private void show(Stage owner, Runnable closeAction) {
        this.onClose = closeAction;
        Stage s = new Stage();
        if (owner != null) s.initOwner(owner);
        s.setTitle("🃏 Flag Memory");
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:#0d1b2a;");
        ComboBox<String> difficulty = new ComboBox<>();
        difficulty.getItems().addAll("Easy", "Medium", "Hard");
        difficulty.getSelectionModel().selectFirst();
        Button start = new Button("Start");
        start.setOnAction(e -> setup(difficulty.getValue()));
        stats.setTextFill(Color.WHITE);
        stats.setFont(Font.font("System", FontWeight.BOLD, 16));
        root.getChildren().addAll(difficulty, start, stats, grid);
        s.setScene(new Scene(root, 900, 650));
        s.setOnHidden(e -> { if (onClose != null) onClose.run(); });
        s.show();
    }

    private void setup(String level) {
        int cols = level.equals("Hard") ? 6 : (level.equals("Medium") ? 5 : 4);
        int rows = 4;
        pairs = (cols * rows) / 2;
        List<String> picked = new ArrayList<>(pool.subList(0, pairs));
        List<String> cards = new ArrayList<>(picked);
        cards.addAll(picked);
        Collections.shuffle(cards);
        grid.getChildren().clear();
        grid.setHgap(8); grid.setVgap(8);
        score = 0; mistakes = 0; first = null; second = null; startMs = System.currentTimeMillis();
        int i = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Card card = new Card(cards.get(i++));
                int cc = c, rr = r;
                card.button.setOnAction(e -> flip(card));
                grid.add(card.button, cc, rr);
            }
        }
        updateStats();
    }

    private void flip(Card card) {
        if (card.matched || card.open || second != null) return;
        animateFlip(card, true);
        if (first == null) { first = card; return; }
        second = card;
        if (first.flag.equals(second.flag)) {
            first.matched = true; second.matched = true;
            first.button.setStyle("-fx-background-color:#1b5e20; -fx-background-radius:10;");
            second.button.setStyle("-fx-background-color:#1b5e20; -fx-background-radius:10;");
            score += 100;
            first = null; second = null;
            if (isDone()) finish();
        } else {
            mistakes++;
            PauseTransition p = new PauseTransition(Duration.seconds(1));
            Card a = first, b = second;
            p.setOnFinished(e -> {
                animateFlip(a, false);
                animateFlip(b, false);
                first = null; second = null;
                updateStats();
            });
            p.play();
        }
        updateStats();
    }

    private void animateFlip(Card card, boolean open) {
        ScaleTransition s1 = new ScaleTransition(Duration.millis(150), card.button);
        s1.setFromX(1); s1.setToX(0);
        ScaleTransition s2 = new ScaleTransition(Duration.millis(150), card.button);
        s2.setFromX(0); s2.setToX(1);
        s1.setOnFinished(e -> {
            card.open = open;
            card.button.setText(open ? card.flag : "🌍");
            card.button.setStyle(open ? "-fx-background-color:#1565c0; -fx-text-fill:white; -fx-font-size:40; -fx-background-radius:10;"
                    : "-fx-background-color:#1b2a3b; -fx-text-fill:white; -fx-font-size:24; -fx-background-radius:10;");
            s2.play();
        });
        s1.play();
    }

    private boolean isDone() {
        return grid.getChildren().stream().allMatch(n -> ((Button) n).getStyle().contains("#1b5e20"));
    }

    private void updateStats() { stats.setText("Score: " + score + " | Mistakes: " + mistakes); }

    private void finish() {
        int secs = Math.max(0, 180 - (int) ((System.currentTimeMillis() - startMs) / 1000));
        score += secs;
        if (mistakes == 0) score += 500;
        ScoreManager.updateHighScore("memory", score);
        if (mistakes == 0) BadgeManager.award("memory", "🃏 Memory Master");
        WinScreens.show("memory", score, "Mistakes: " + mistakes + " | Time bonus: " + secs, onClose);
    }

    private static class Card {
        final String flag; boolean open; boolean matched;
        final Button button = new Button("🌍");
        Card(String flag) {
            this.flag = flag;
            button.setPrefSize(90, 90);
            button.setStyle("-fx-background-color:#1b2a3b; -fx-text-fill:white; -fx-font-size:24; -fx-background-radius:10;");
        }
    }
}
