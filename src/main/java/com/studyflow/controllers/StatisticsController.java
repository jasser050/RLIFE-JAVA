package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Statistics view
 */
public class StatisticsController implements Initializable {

    @FXML private BarChart<String, Number> monthlyChart;
    @FXML private PieChart distributionChart;
    @FXML private LineChart<String, Number> progressChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupMonthlyChart();
        setupDistributionChart();
        setupProgressChart();
    }

    private void setupMonthlyChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Study Hours");

        series.getData().add(new XYChart.Data<>("Sep", 32));
        series.getData().add(new XYChart.Data<>("Oct", 45));
        series.getData().add(new XYChart.Data<>("Nov", 38));
        series.getData().add(new XYChart.Data<>("Dec", 28));
        series.getData().add(new XYChart.Data<>("Jan", 52));

        monthlyChart.getData().add(series);
        monthlyChart.setAnimated(true);
    }

    private void setupDistributionChart() {
        distributionChart.getData().addAll(
            new PieChart.Data("Data Structures", 28),
            new PieChart.Data("Algorithms", 22),
            new PieChart.Data("Database Systems", 18),
            new PieChart.Data("Software Eng.", 15),
            new PieChart.Data("Networks", 17)
        );
        distributionChart.setAnimated(true);
    }

    private void setupProgressChart() {
        XYChart.Series<String, Number> tasksSeries = new XYChart.Series<>();
        tasksSeries.setName("Tasks Completed");
        tasksSeries.getData().add(new XYChart.Data<>("Week 1", 8));
        tasksSeries.getData().add(new XYChart.Data<>("Week 2", 12));
        tasksSeries.getData().add(new XYChart.Data<>("Week 3", 15));
        tasksSeries.getData().add(new XYChart.Data<>("Week 4", 11));
        tasksSeries.getData().add(new XYChart.Data<>("Week 5", 18));

        XYChart.Series<String, Number> hoursSeries = new XYChart.Series<>();
        hoursSeries.setName("Study Hours");
        hoursSeries.getData().add(new XYChart.Data<>("Week 1", 20));
        hoursSeries.getData().add(new XYChart.Data<>("Week 2", 28));
        hoursSeries.getData().add(new XYChart.Data<>("Week 3", 32));
        hoursSeries.getData().add(new XYChart.Data<>("Week 4", 25));
        hoursSeries.getData().add(new XYChart.Data<>("Week 5", 35));

        progressChart.getData().addAll(tasksSeries, hoursSeries);
        progressChart.setAnimated(true);
    }
}
