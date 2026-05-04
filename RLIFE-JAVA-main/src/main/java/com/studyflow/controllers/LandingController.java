package com.studyflow.controllers;

import com.studyflow.App;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class LandingController implements Initializable {

    @FXML private StackPane heroPane;
    @FXML private VBox heroContent;
    @FXML private VBox heroVisual;
    @FXML private VBox featuresSection;
    @FXML private HBox proofSection;

    private double xOffset = 0;
    private double yOffset = 0;
    private Canvas starCanvas;
    private AnimationTimer starTimer;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Create animated star canvas as hero background
        starCanvas = new Canvas();
        starCanvas.widthProperty().bind(heroPane.widthProperty());
        starCanvas.heightProperty().bind(heroPane.heightProperty());
        heroPane.getChildren().add(0, starCanvas);

        // Init particles
        for (int i = 0; i < 180; i++) {
            particles.add(new Particle(random, true));
        }

        // Start star animation
        starTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderStars(now);
            }
        };
        starTimer.start();

        // Entrance animations with staggered timing
        playEntrance(heroContent, 300, 30);
        playEntrance(heroVisual, 600, 40);
        playEntrance(featuresSection, 900, 25);
        playEntrance(proofSection, 1100, 20);
    }

    // ── Animated starfield ───────────────────────────────────────────────────

    private void renderStars(long now) {
        double w = starCanvas.getWidth();
        double h = starCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        double t = now / 1_000_000_000.0;
        GraphicsContext gc = starCanvas.getGraphicsContext2D();

        // Clear with dark background
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.web("#020617"));
        gc.fillRect(0, 0, w, h);

        // Subtle nebula glow (static, just paint once-ish feel but it's fast)
        gc.setGlobalAlpha(0.04);
        gc.setFill(Color.web("#7C3AED"));
        gc.fillOval(w * 0.3 - 200, h * 0.4 - 200, 400, 400);
        gc.setFill(Color.web("#3B82F6"));
        gc.fillOval(w * 0.7 - 150, h * 0.3 - 150, 300, 300);
        gc.setFill(Color.web("#EC4899"));
        gc.fillOval(w * 0.5 - 180, h * 0.7 - 180, 360, 360);

        // Draw particles
        for (Particle p : particles) {
            // Move upward slowly
            p.y -= p.speed;
            // Gentle horizontal drift
            p.x += Math.sin(t * p.driftSpeed + p.driftPhase) * 0.00008;

            // Reset if off screen
            if (p.y < -0.02) {
                p.reset(random, false);
            }

            double px = p.x * w;
            double py = p.y * h;

            // Twinkle
            double twinkle = 0.4 + 0.6 * Math.sin(t * p.twinkleSpeed + p.twinklePhase);
            double alpha = p.alpha * twinkle;

            // Soft halo for larger stars
            if (p.size > 1.2) {
                gc.setGlobalAlpha(alpha * 0.12);
                gc.setFill(Color.color(p.r, p.g, p.b));
                double haloSize = p.size * 5;
                gc.fillOval(px - haloSize / 2, py - haloSize / 2, haloSize, haloSize);
            }

            // Core dot
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.color(p.r, p.g, p.b));
            gc.fillOval(px - p.size / 2, py - p.size / 2, p.size, p.size);

            // Cross spikes for bright stars
            if (p.size > 1.8) {
                gc.setGlobalAlpha(alpha * 0.3);
                gc.setStroke(Color.color(p.r, p.g, p.b));
                gc.setLineWidth(0.4);
                double spike = p.size * 3;
                gc.strokeLine(px - spike, py, px + spike, py);
                gc.strokeLine(px, py - spike, px, py + spike);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private static class Particle {
        double x, y, size, speed, alpha;
        double r, g, b;
        double twinklePhase, twinkleSpeed;
        double driftPhase, driftSpeed;

        Particle(Random rng, boolean randomY) {
            reset(rng, randomY);
        }

        void reset(Random rng, boolean randomY) {
            x = rng.nextDouble();
            y = randomY ? rng.nextDouble() : 1.0 + rng.nextDouble() * 0.05;
            size = 0.4 + rng.nextDouble() * 1.8;
            speed = 0.00015 + rng.nextDouble() * 0.0006;
            alpha = 0.3 + rng.nextDouble() * 0.7;
            twinklePhase = rng.nextDouble() * Math.PI * 2;
            twinkleSpeed = 0.5 + rng.nextDouble() * 2.5;
            driftPhase = rng.nextDouble() * Math.PI * 2;
            driftSpeed = 0.3 + rng.nextDouble() * 1.0;

            // Color temperature variety
            double temp = rng.nextDouble();
            if (temp < 0.3)      { r = 0.75; g = 0.82; b = 1.0; }   // blue-white
            else if (temp < 0.55) { r = 0.95; g = 0.93; b = 0.90; } // warm white
            else if (temp < 0.75) { r = 0.70; g = 0.60; b = 1.0; }  // purple
            else if (temp < 0.9)  { r = 0.88; g = 0.90; b = 1.0; }  // cool white
            else                  { r = 1.0;  g = 0.85; b = 0.65; }  // golden
        }
    }

    // ── Entrance animation ───────────────────────────────────────────────────

    private void playEntrance(javafx.scene.Node node, int delayMs, double slideDistance) {
        node.setOpacity(0);
        node.setTranslateY(slideDistance);

        PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
        delay.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(700), node);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(700), node);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

            new ParallelTransition(fade, slide).play();
        });
        delay.play();
    }

    // ── Window controls ──────────────────────────────────────────────────────

    @FXML
    private void onDragStart(MouseEvent e) {
        xOffset = App.getPrimaryStage().getX() - e.getScreenX();
        yOffset = App.getPrimaryStage().getY() - e.getScreenY();
    }

    @FXML
    private void onDragged(MouseEvent e) {
        App.getPrimaryStage().setX(e.getScreenX() + xOffset);
        App.getPrimaryStage().setY(e.getScreenY() + yOffset);
    }

    @FXML
    private void goToLogin() throws IOException {
        if (starTimer != null) starTimer.stop();
        App.setRoot("views/Login");
    }

    @FXML
    private void goToRegister() throws IOException {
        if (starTimer != null) starTimer.stop();
        App.setRoot("views/Register");
    }

    @FXML
    private void minimizeWindow() {
        App.getPrimaryStage().setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() {
        if (starTimer != null) starTimer.stop();
        Platform.exit();
    }
}
