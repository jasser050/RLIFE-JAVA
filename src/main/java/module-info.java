module com.studyflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;

    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires jdk.httpserver;

    requires mysql.connector.j;
    requires jgltf.model;
    requires org.apache.pdfbox;
    requires jakarta.mail;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.eclipse.jgit;

    requires kernel;
    requires layout;
    requires io;
    requires java.prefs;

    opens com.studyflow to javafx.fxml;
    opens com.studyflow.controllers to javafx.fxml;
    opens com.studyflow.controllers.admin to javafx.fxml;
    opens com.studyflow.models to com.fasterxml.jackson.databind;
    opens com.studyflow.utils to javafx.fxml;
    opens com.studyflow.services to javafx.fxml;

    exports com.studyflow;
    exports com.studyflow.controllers;
    exports com.studyflow.controllers.admin;
    exports com.studyflow.models;
    exports com.studyflow.utils;
    exports com.studyflow.interfaces;
    exports com.studyflow.services;
}
