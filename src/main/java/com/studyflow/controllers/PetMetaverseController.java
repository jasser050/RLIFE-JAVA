package com.studyflow.controllers;

import com.studyflow.LocalServer;
import com.studyflow.models.Pet;
import com.studyflow.models.User;
import com.studyflow.services.PetService;
import com.studyflow.utils.PetPreviewSupport;
import com.studyflow.utils.PetUiSupport;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class PetMetaverseController implements Initializable {
    @FXML private Label metaverseTitleLabel;
    @FXML private Label metaverseSubtitleLabel;
    @FXML private Label metaverseStatusLabel;
    @FXML private Label metaverseStatsLabel;
    @FXML private ImageView metaversePetImageView;
    @FXML private WebView metaverseWebView;

    private PetService petService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            petService = new PetService();
        } catch (RuntimeException ignored) {
            petService = null;
        }
        loadMetaverse();
    }

    @FXML
    private void handleBackToPet() {
        MainController.loadContentInMainArea("views/Notes.fxml");
    }

    @FXML
    private void handleReload() {
        loadMetaverse();
    }

    private void loadMetaverse() {
        User user = UserSession.getInstance().getCurrentUser();
        Pet pet = null;
        if (user != null && petService != null && petService.isDatabaseAvailable()) {
            try {
                pet = petService.findByUserId(user.getId()).map(petService::syncPetState).orElse(null);
            } catch (RuntimeException ignored) {
                pet = null;
            }
        }

        if (pet == null) {
            metaverseTitleLabel.setText("Pet Metaverse");
            metaverseSubtitleLabel.setText("Adopt a companion first to enter the garden.");
            metaverseStatusLabel.setText("No active companion");
            metaverseStatsLabel.setText("Your metaverse map unlocks after adoption.");
            PetPreviewSupport.showPetPreview(metaversePetImageView, "cat");
            loadMap("[]");
            return;
        }

        String owner = user == null ? "You" : displayName(user);
        String rarity = pet.getRarity() == null ? "Common" : pet.getRarity();
        String mood = pet.getMood();
        String renderType = PetUiSupport.previewType(pet.getType());
        String payload = "[{\"userId\":" + pet.getUserId()
                + ",\"type\":\"" + escapeJson(renderType) + "\""
                + ",\"name\":\"" + escapeJson(pet.getName()) + "\""
                + ",\"owner\":\"" + escapeJson(owner) + "\""
                + ",\"level\":" + pet.getLevel()
                + ",\"mood\":\"" + escapeJson(mood.toLowerCase()) + "\""
                + ",\"rarity\":\"" + escapeJson(rarity) + "\""
                + ",\"sharedProjectsCount\":1"
                + ",\"hasSharedProjects\":true"
                + ",\"isSelf\":true}]";

        metaverseTitleLabel.setText(pet.getName() + "'s Metaverse");
        metaverseSubtitleLabel.setText("A live PetVerse garden view using your current companion data.");
        metaverseStatusLabel.setText(rarity + " | " + PetUiSupport.moodFromHunger(pet.getHunger()));
        metaverseStatsLabel.setText("Level " + pet.getLevel() + " | " + pet.getHunger() + "/100 hunger | " + pet.getEvolutionStage());
        PetPreviewSupport.showPetPreview(metaversePetImageView, renderType);
        loadMap(payload);
    }

    private void loadMap(String payload) {
        WebEngine engine = metaverseWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        String url = LocalServer.url("/metaverse/petverse_map.html") + "?pets=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
        engine.load(url);
    }

    private String displayName(User user) {
        String fullName = user.getFullName() == null ? "" : user.getFullName().trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (!username.isBlank()) {
            return username;
        }
        return "You";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
