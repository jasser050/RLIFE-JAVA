package com.studyflow.games;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class WinScreens {
    private WinScreens() {}

    static void show(String gameKey, int score, String details, Runnable onClose) {
        Stage stage = new Stage();
        stage.setTitle("Ã°Å¸Ââ€  Congratulations!");
        Pane confettiLayer = new Pane();
        confettiLayer.setMouseTransparent(true);
        List<Circle> confetti = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < 30; i++) {
            Circle c = new Circle(4 + r.nextDouble() * 4, Color.hsb(r.nextDouble() * 360, 0.8, 1));
            c.setCenterX(20 + r.nextDouble() * 760);
            c.setCenterY(-r.nextDouble() * 250);
            confetti.add(c);
        }
        confettiLayer.getChildren().addAll(confetti);
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color:#0d1b2a;");
        Label t = new Label("Ã°Å¸Ââ€  Congratulations!");
        t.setFont(Font.font("System", FontWeight.BOLD, 32));
        t.setTextFill(Color.web("#ffd600"));
        Label s = new Label("Score: " + score + " | High Score: " + ScoreManager.getHighScore(gameKey));
        s.setTextFill(Color.WHITE);
        Label d = new Label(details);
        d.setTextFill(Color.web("#ffd54f"));
        Label b = new Label("Badge: " + BadgeManager.getBadge(gameKey));
        b.setTextFill(Color.web("#80cbc4"));
        Button playAgain = new Button("Play Again");
        playAgain.setOnAction(e -> stage.close());
        Button back = new Button("Back to Games");
        back.setOnAction(e -> stage.close());
        box.getChildren().addAll(t, s, d, b, playAgain, back);
        Pane root = new Pane(box, confettiLayer);
        box.setPrefSize(800, 500);
        confettiLayer.setPrefSize(800, 500);
        stage.setScene(new Scene(root, 800, 500));
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                for (Circle c : confetti) {
                    c.setCenterY(c.getCenterY() + 2.4);
                    if (c.getCenterY() > 520) c.setCenterY(-20);
                }
            }
        };
        stage.setOnShown(e -> timer.start());
        stage.setOnHidden(e -> { timer.stop(); if (onClose != null) onClose.run(); });
        stage.show();
    }
}

