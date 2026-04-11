package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.animation.AnimationTimer;

import java.net.URL;
import java.util.*;

public class AdminStatisticsController implements Initializable {

    @FXML private StackPane chart3DContainer;
    @FXML private BarChart<String, Number> activityChart;
    @FXML private AreaChart<String, Number> growthChart;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        build3DChart();
        buildActivityChart();
        buildGrowthChart();
    }

    private void build3DChart() {
        List<User> allUsers = serviceUser.getAll();
        Map<String, Integer> regByMonth = serviceUser.getUserRegistrationsByMonth();

        // Build 3D bars
        Group barsGroup = new Group();
        Group world = new Group();
        world.getChildren().add(barsGroup);

        // Floor grid
        Box floor = new Box(14, 0.05, 8);
        PhongMaterial floorMat = new PhongMaterial(Color.color(0.1, 0.12, 0.2, 0.6));
        floor.setMaterial(floorMat);
        floor.setTranslateY(0.025);
        world.getChildren().add(floor);

        // Colors for bars
        Color[] barColors = {
            Color.color(0.39, 0.35, 0.96),  // indigo
            Color.color(0.55, 0.36, 0.96),  // purple
            Color.color(0.93, 0.28, 0.60),  // pink
            Color.color(0.98, 0.57, 0.23),  // orange
            Color.color(0.20, 0.83, 0.60),  // green
            Color.color(0.05, 0.57, 0.82),  // blue
        };

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(regByMonth.entrySet());
        Collections.reverse(entries);

        int maxVal = entries.stream().mapToInt(Map.Entry::getValue).max().orElse(1);
        double barWidth = 1.5;
        double spacing = 2.2;
        double startX = -(entries.size() - 1) * spacing / 2.0;

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            double height = Math.max(0.2, (e.getValue() / (double) maxVal) * 5.0);

            Box bar = new Box(barWidth, height, barWidth);
            PhongMaterial mat = new PhongMaterial(barColors[i % barColors.length]);
            mat.setSpecularColor(Color.color(0.8, 0.8, 0.9));
            mat.setSpecularPower(20);
            bar.setMaterial(mat);
            bar.setTranslateX(startX + i * spacing);
            bar.setTranslateY(-height / 2.0);
            bar.setTranslateZ(0);
            barsGroup.getChildren().add(bar);

            // Value label on top of bar
            Text valText = new Text(String.valueOf(e.getValue()));
            valText.setFont(Font.font("System", 14));
            valText.setFill(Color.WHITE);
            // Position above bar — use a Group to hold the text in 3D space
            Group textGroup = new Group(valText);
            textGroup.setTranslateX(startX + i * spacing - 0.3);
            textGroup.setTranslateY(-height - 0.4);
            textGroup.setTranslateZ(0);
            textGroup.setScaleX(0.02);
            textGroup.setScaleY(0.02);
            barsGroup.getChildren().add(textGroup);

            // Month label below
            String monthLabel = e.getKey().length() >= 7 ? e.getKey().substring(5) : e.getKey();
            Text monthText = new Text(monthLabel);
            monthText.setFont(Font.font("System", 14));
            monthText.setFill(Color.color(0.6, 0.65, 0.8));
            Group monthGroup = new Group(monthText);
            monthGroup.setTranslateX(startX + i * spacing - 0.3);
            monthGroup.setTranslateY(0.5);
            monthGroup.setTranslateZ(0);
            monthGroup.setScaleX(0.02);
            monthGroup.setScaleY(0.02);
            barsGroup.getChildren().add(monthGroup);
        }

        // Lighting
        AmbientLight ambient = new AmbientLight(Color.color(0.4, 0.38, 0.5));
        PointLight key = new PointLight(Color.color(0.9, 0.85, 0.95));
        key.setTranslateX(5); key.setTranslateY(-8); key.setTranslateZ(8);
        PointLight fill = new PointLight(Color.color(0.3, 0.3, 0.6));
        fill.setTranslateX(-5); fill.setTranslateY(-3); fill.setTranslateZ(-5);
        world.getChildren().addAll(ambient, key, fill);

        // Camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(45);
        camera.setNearClip(0.1);
        camera.setFarClip(100);
        camera.setTranslateX(0);
        camera.setTranslateY(-4);
        camera.setTranslateZ(-12);
        camera.getTransforms().add(new Rotate(-20, Rotate.X_AXIS));

        SubScene sub = new SubScene(world, 800, 400, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);

        // Bind size to container
        sub.widthProperty().bind(chart3DContainer.widthProperty());
        sub.heightProperty().bind(chart3DContainer.heightProperty());

        // Slow rotation
        Rotate rot = new Rotate(0, Rotate.Y_AXIS);
        barsGroup.getTransforms().add(rot);
        AnimationTimer timer = new AnimationTimer() {
            private long last = 0;
            @Override public void handle(long now) {
                if (last != 0) rot.setAngle(rot.getAngle() + (now - last) * 1e-9 * 12.0);
                last = now;
            }
        };
        timer.start();

        chart3DContainer.getChildren().add(sub);
    }

    private void buildActivityChart() {
        List<User> all = serviceUser.getAll();
        long active = all.stream().filter(u -> !u.isBanned()).count();
        long banned = all.stream().filter(User::isBanned).count();

        XYChart.Series<String, Number> activeSeries = new XYChart.Series<>();
        activeSeries.setName("Active");
        activeSeries.getData().add(new XYChart.Data<>("Users", active));

        XYChart.Series<String, Number> bannedSeries = new XYChart.Series<>();
        bannedSeries.setName("Banned");
        bannedSeries.getData().add(new XYChart.Data<>("Users", banned));

        activityChart.getData().addAll(activeSeries, bannedSeries);
    }

    private void buildGrowthChart() {
        Map<String, Integer> reg = serviceUser.getUserRegistrationsByMonth();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(reg.entrySet());
        Collections.reverse(entries);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Cumulative");
        int cumulative = 0;
        for (Map.Entry<String, Integer> e : entries) {
            cumulative += e.getValue();
            series.getData().add(new XYChart.Data<>(e.getKey(), cumulative));
        }
        growthChart.getData().add(series);
    }
}
