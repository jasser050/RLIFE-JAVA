package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PresentationStudioController implements Initializable {

    @FXML
    private WebView webView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // Charge le fichier HTML de l'éditeur
        URL htmlFile = getClass().getResource("/com/studyflow/presentation-studio.html");
        if (htmlFile != null) {
            engine.load(htmlFile.toExternalForm());
        } else {
            engine.loadContent("""
                <html>
                <body style="background:#0F0F1A;color:#F8FAFC;font-family:sans-serif;
                             display:flex;align-items:center;justify-content:center;
                             height:100vh;margin:0;flex-direction:column;gap:16px;">
                    <div style="font-size:48px">⚠️</div>
                    <h2 style="color:#7F77DD;margin:0">Fichier introuvable</h2>
                    <p style="color:#94A3B8;margin:0">Placez <b>presentation-studio.html</b> dans :</p>
                    <code style="background:#16213E;padding:12px 24px;border-radius:10px;
                                 color:#A78BFA;font-size:13px;">
                        src/main/resources/com/studyflow/presentation-studio.html
                    </code>
                </body>
                </html>
            """);
        }
    }

    @FXML
    private void closeStudio() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }
}
