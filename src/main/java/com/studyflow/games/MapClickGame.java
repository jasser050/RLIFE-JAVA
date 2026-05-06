package com.studyflow.games;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MapClickGame {
    private final List<GeoGameData.MapRegion> qs = new ArrayList<>(GeoGameData.MAP_REGIONS);
    private int index, score, correct;
    private int remaining = 20;
    private long qStart;
    private Timeline timer;
    private Runnable onClose;
    private GeoGameData.MapRegion target;
    private Color flash = null;
    private final Canvas canvas = new Canvas(800, 460);
    private final Label info = new Label();

    public static void open(Stage owner, Runnable onClose) { new MapClickGame().show(owner, onClose); }

    private void show(Stage owner, Runnable closeAction) {
        this.onClose = closeAction;
        Collections.shuffle(qs);
        Stage s = new Stage();
        if (owner != null) s.initOwner(owner);
        s.setTitle("Ã°Å¸â€”ÂºÃ¯Â¸Â Map Click");
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:#0d1b2a;");
        info.setTextFill(Color.WHITE);
        info.setFont(Font.font("System", FontWeight.BOLD, 20));
        canvas.setOnMouseClicked(e -> click(e.getX(), e.getY()));
        root.getChildren().addAll(info, canvas);
        s.setScene(new Scene(root, 860, 560));
        s.setOnHidden(e -> { if (timer != null) timer.stop(); if (onClose != null) onClose.run(); });
        s.show();
        next();
    }

    private void next() {
        if (index >= 10) { finish(); return; }
        target = qs.get(index++);
        remaining = 20;
        qStart = System.currentTimeMillis();
        info.setText("Click on " + target.country() + "  | Score: " + score + " | " + remaining + "s");
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining--;
            info.setText("Click on " + target.country() + "  | Score: " + score + " | " + remaining + "s");
            if (remaining <= 0) next();
        }));
        timer.setCycleCount(20);
        timer.playFromStart();
        draw();
    }

    private void click(double x, double y) {
        if (target == null) return;
        boolean hitTarget = in(target, x, y);
        if (hitTarget) {
            correct++;
            long sec = (System.currentTimeMillis() - qStart) / 1000;
            score += sec < 5 ? 200 : 100;
            flash = Color.web("#4caf50");
        } else {
            score -= 50;
            flash = Color.web("#f44336");
        }
        draw();
        Timeline t = new Timeline(new KeyFrame(Duration.millis(280), e -> { flash = null; next(); }));
        t.play();
    }

    private static boolean in(GeoGameData.MapRegion r, double x, double y) {
        return x >= r.x() && x <= r.x() + r.w() && y >= r.y() && y <= r.y() + r.h();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#10253d"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawContinent(g, "#6a1b9a", 90, 110, 210, 320);
        drawContinent(g, "#1565c0", 300, 130, 160, 120);
        drawContinent(g, "#2e7d32", 470, 120, 220, 220);
        drawContinent(g, "#e65100", 350, 220, 150, 200);
        drawContinent(g, "#00838f", 620, 330, 130, 90);
        for (GeoGameData.MapRegion r : qs) {
            g.setFill(r == target ? Color.web("#ffd600") : Color.color(1, 1, 1, 0.18));
            g.fillRect(r.x(), r.y(), r.w(), r.h());
        }
        if (flash != null) {
            g.setFill(Color.color(flash.getRed(), flash.getGreen(), flash.getBlue(), 0.35));
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void drawContinent(GraphicsContext g, String color, double x, double y, double w, double h) {
        g.setFill(Color.web(color));
        g.fillRoundRect(x, y, w, h, 28, 28);
    }

    private void finish() {
        ScoreManager.updateHighScore("map", score);
        if (correct == 10) BadgeManager.award("map", "Ã°Å¸â€”ÂºÃ¯Â¸Â Navigator");
        WinScreens.show("map", score, "Correct: " + correct + "/10", onClose);
    }
}

