package games;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class CapitalQuizGame {
    private final Random random = new Random();
    private final List<GeoGameData.CapitalEntry> questions = new ArrayList<>(GeoGameData.CAPITALS);
    private int index, score, correct, remaining = 15;
    private long qStart;
    private Timeline timer;
    private Runnable onClose;
    private Label q = new Label(), status = new Label();
    private ProgressBar bar = new ProgressBar(1);
    private List<Button> opts = new ArrayList<>();

    public static void open(Stage owner, Runnable onClose) { new CapitalQuizGame().show(owner, onClose); }

    private void show(Stage owner, Runnable closeAction) {
        this.onClose = closeAction;
        Collections.shuffle(questions);
        Stage s = new Stage();
        if (owner != null) s.initOwner(owner);
        s.setTitle("🏙️ Capital Quiz");
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:#0d1b2a;");
        q.setTextFill(Color.WHITE); q.setFont(Font.font("System", FontWeight.BOLD, 22));
        status.setTextFill(Color.web("#ffd54f"));
        for (int i = 0; i < 4; i++) {
            Button b = new Button(); b.setMinWidth(300);
            int k = i; b.setOnAction(e -> answer(opts.get(k).getText())); opts.add(b);
        }
        root.getChildren().addAll(q, bar, opts.get(0), opts.get(1), opts.get(2), opts.get(3), status);
        s.setScene(new Scene(root, 760, 520));
        s.setOnHidden(e -> { if (timer != null) timer.stop(); if (onClose != null) onClose.run(); });
        s.show(); next();
    }

    private void next() {
        if (index >= 10) { finish(); return; }
        GeoGameData.CapitalEntry cur = questions.get(index++);
        q.setText("What is the capital of " + cur.country() + " " + cur.flag() + "?");
        List<String> choices = new ArrayList<>(); choices.add(cur.capital());
        while (choices.size() < 4) {
            String c = questions.get(random.nextInt(questions.size())).capital();
            if (!choices.contains(c)) choices.add(c);
        }
        Collections.shuffle(choices);
        for (int i = 0; i < 4; i++) opts.get(i).setText(choices.get(i));
        remaining = 15; qStart = System.currentTimeMillis(); bar.setProgress(1);
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining--; bar.setProgress(remaining / 15.0);
            if (remaining <= 0) { status.setText("Time up!"); next(); }
        }));
        timer.setCycleCount(15); timer.playFromStart();
    }

    private void answer(String selected) {
        if (timer != null) timer.stop();
        String good = questions.get(index - 1).capital();
        if (selected.equals(good)) {
            correct++;
            long elapsed = (System.currentTimeMillis() - qStart) / 1000;
            score += elapsed < 3 ? 150 : (elapsed < 7 ? 100 : 50);
            status.setText("Correct");
        } else status.setText("Wrong. Correct: " + good);
        next();
    }

    private void finish() {
        ScoreManager.updateHighScore("capital", score);
        if (correct == 10) BadgeManager.award("capital", "🏙️ City Expert");
        WinScreens.show("capital", score, "Correct: " + correct + "/10", onClose);
    }
}
