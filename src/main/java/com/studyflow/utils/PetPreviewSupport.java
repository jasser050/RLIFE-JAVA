package com.studyflow.utils;

import com.studyflow.pets.CatGlbPreview;
import com.studyflow.pets.CatPetScene;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public final class PetPreviewSupport {
    private PetPreviewSupport() {
    }

    public static Node createPreview(String type, double width, double height) {
        String normalized = PetUiSupport.normalizeType(type);
        if ("cat".equals(normalized)) {
            return CatGlbPreview.create(width, height);
        }

        ImageView imageView = new ImageView(PetUiSupport.loadPetImage(normalized));
        imageView.setFitWidth(width - 16);
        imageView.setFitHeight(height - 16);
        imageView.setPreserveRatio(true);
        return wrap(imageView, width, height);
    }

    private static StackPane wrap(Node node, double width, double height) {
        StackPane pane = new StackPane(node);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        if (node instanceof SubScene subScene) {
            subScene.widthProperty().bind(pane.widthProperty());
            subScene.heightProperty().bind(pane.heightProperty());
        }
        return pane;
    }
}
