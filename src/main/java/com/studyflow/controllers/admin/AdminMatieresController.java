package com.studyflow.controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminMatieresController implements Initializable {

    @FXML private FlowPane matieresPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        matieresPane.getChildren().setAll(
                AdminViewFactory.createSubjectCard("Matieres", "Organise les matieres, categories et parcours d'etude.", "12 modules", "primary"),
                AdminViewFactory.createSubjectCard("Cours", "Associe les cours aux matieres et garde une structure claire.", "28 cours", "success"),
                AdminViewFactory.createSubjectCard("Affectation", "Controle les dependances entre enseignants, groupes et contenu.", "8 regles", "warning"),
                AdminViewFactory.createSubjectCard("Archivage", "Prepare les donnees anciennes pour conserver un back office lisible.", "3 archives", "danger")
        );
    }
}
