module com.studyflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires java.sql;
    requires javafx.web;

    opens com.studyflow to javafx.fxml;
    opens com.studyflow.controllers to javafx.fxml;

    exports com.studyflow;
    exports com.studyflow.controllers;
    exports com.studyflow.models;
    exports com.studyflow.utils;
    exports com.studyflow.interfaces;
    exports com.studyflow.services;
    opens com.studyflow.utils to javafx.fxml;
    opens com.studyflow.services to javafx.fxml;
}
