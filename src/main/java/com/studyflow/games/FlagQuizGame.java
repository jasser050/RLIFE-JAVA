package com.studyflow.games;

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

public final class FlagQuizGame {
    private final Random random = new Random();
    private final List<GeoGameData.FlagCountry> questions = new ArrayList<>(GeoGameData.FLAGS);
    private int index;
    private int score;
    private int correct;
    private int remaining = 10;
    private long qStart;
    private Timeline timer;
    private Runnable onClose;

    private Label flag = new Label();
    private Label q = new Label();
    private Label status = new Label();
    private ProgressBar bar = new ProgressBar(1);
    private List<Button> opts = new ArrayList<>();

    public static void open(Stage owner, Runnable onClose) {
        new FlagQuizGame().show(owner, onClose);
    }

    private void show(Stage owner, Runnable closeAction) {
        this.onClose = closeAction;
        Collections.shuffle(questions);
        Stage s = new Stage();
        if (owner != null) s.initOwner(owner);
        s.setTitle("Ã°Å¸Å¡Â© Flag Quiz");
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:#0d1b2a;");
        q.setTextFill(Color.WHITE);
        q.setFont(Font.font("System", FontWeight.BOLD, 20));
        flag.setFont(Font.font(150));
        status.setTextFill(Color.web("#ffd54f"));
        for (int i = 0; i < 4; i++) {
            Button b = new Button();
            b.setMinWidth(300);
            int k = i;
            b.setOnAction(e -> answer(opts.get(k).getText()));
            opts.add(b);
        }
        root.getChildren().addAll(q, flag, bar, opts.get(0), opts.get(1), opts.get(2), opts.get(3), status);
        s.setScene(new Scene(root, 700, 620));
        s.setOnHidden(e -> { if (timer != null) timer.stop(); if (onClose != null) onClose.run(); });
        s.show();
        next();
    }

    private void next() {
        if (index >= 10) { finish(); return; }
        GeoGameData.FlagCountry cur = questions.get(index++);
        q.setText("Question " + index + "/10");
        flag.setText(cur.flag());
        List<String> pool = new ArrayList<>();
        pool.add(cur.country());
        while (pool.size() < 4) {
            String c = questions.get(random.nextInt(questions.size())).country();
            if (!pool.contains(c)) pool.add(c);
        }
        Collections.shuffle(pool);
        for (int i = 0; i < 4; i++) opts.get(i).setText(pool.get(i));
        remaining = 10;
        qStart = System.currentTimeMillis();
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining--;
            bar.setProgress(remaining / 10.0);
            if (remaining <= 0) {
                status.setText("Time up!");
                next();
            }
        }));
        timer.setCycleCount(10);
        bar.setProgress(1);
        timer.playFromStart();
    }

    private void answer(String answer) {
        if (timer != null) timer.stop();
        String correctCountry = questions.get(index - 1).country();
        if (answer.equals(correctCountry)) {
            correct++;
            long elapsed = (System.currentTimeMillis() - qStart) / 1000;
            score += elapsed < 3 ? 150 : (elapsed < 7 ? 100 : 50);
            status.setText("Correct");
        } else {
            status.setText("Wrong. Correct: " + correctCountry);
        }
        next();
    }

    private void finish() {
        ScoreManager.updateHighScore("flag", score);
        if (correct == 10) BadgeManager.award("flag", "Ã°Å¸Å’Â Globe Trotter");
        WinScreens.show("flag", score, "Correct: " + correct + "/10", onClose);
    }
}

