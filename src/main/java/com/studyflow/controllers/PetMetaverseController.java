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
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class PetMetaverseController implements Initializable {
    @FXML private Label metaverseTitleLabel;
    @FXML private Label metaverseSubtitleLabel;
    @FXML private Label metaverseStatusLabel;
    @FXML private Label metaverseStatsLabel;
    @FXML private StackPane metaversePetPreviewPane;
    @FXML private WebView metaverseWebView;

    private PetService petService;
    private boolean metaverseLoadHookInstalled;

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
        MainController.loadContentInMainArea("views/MyPet.fxml");
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
            renderPetPreview("cat");
            loadMap("[]");
            return;
        }

        String owner = user == null ? "You" : displayName(user);
        String rarity = pet.getRarity() == null ? "Common" : pet.getRarity();
        String mood = pet.getMood();
        String payload = "[{\"userId\":" + pet.getUserId()
                + ",\"type\":\"" + escapeJson(PetUiSupport.normalizeType(pet.getType())) + "\""
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
        renderPetPreview(pet.getType());
        loadMap(payload);
    }

    private void renderPetPreview(String type) {
        if (metaversePetPreviewPane == null) {
            return;
        }
        metaversePetPreviewPane.getChildren().setAll(PetPreviewSupport.createPreview(type, 212, 212));
    }

    private void loadMap(String payload) {
        WebEngine engine = metaverseWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        installMetaverseLoadHook(engine);

        String encodedPayload = URLEncoder.encode(payload, StandardCharsets.UTF_8);
        String baseUrl = LocalServer.url("/metaverse/petverse_map.html");
        String url = baseUrl + "?pets=" + encodedPayload;
        engine.load(url);
    }

    private void installMetaverseLoadHook(WebEngine engine) {
        if (metaverseLoadHookInstalled) {
            return;
        }
        metaverseLoadHookInstalled = true;

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }

            String bootstrapScript = """
                    (function () {
                      if (window.__petverseJavaFxBootstrapInstalled) return;
                      window.__petverseJavaFxBootstrapInstalled = true;

                      function forceStart() {
                        var loading = document.getElementById('loading');
                        if (loading) {
                          loading.style.display = 'none';
                          if (typeof loading.remove === 'function') loading.remove();
                        }

                        if (typeof window.animate === 'function' && !window.__petverseAnimateStarted) {
                          window.__petverseAnimateStarted = true;
                          try { window.animate(); } catch (e) {}
                        }
                      }

                      window.setTimeout(forceStart, 2500);
                      window.setTimeout(forceStart, 5000);
                    })();
                    """;

            try {
                engine.executeScript(bootstrapScript);
            } catch (Exception ignored) {
                // Ignore JS bootstrap errors; the page may already be running.
            }
        });
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
