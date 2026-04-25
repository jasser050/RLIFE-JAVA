module com.studyflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires javafx.media;
    requires javafx.swing;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;

    requires java.sql;
    requires java.desktop;
    requires java.prefs;
    requires java.net.http;
    requires jdk.httpserver;
    requires jdk.jsobject;
    requires webcam.capture;

    requires mysql.connector.j;

    // iText 7
    requires kernel;
    requires layout;
    requires io;

    // Jackson
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    // PDFBox
    requires org.apache.pdfbox;
    requires org.apache.fontbox;

    // ZXing (QR code)
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // JGit
    requires org.eclipse.jgit;

    // Jakarta Mail
    requires jakarta.mail;

    // ✅ JglTF
    requires jgltf.model;

    opens com.studyflow to javafx.fxml;
    opens com.studyflow.controllers to javafx.fxml;
    opens com.studyflow.controllers.admin to javafx.fxml;
    opens com.studyflow.models to javafx.fxml;
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
