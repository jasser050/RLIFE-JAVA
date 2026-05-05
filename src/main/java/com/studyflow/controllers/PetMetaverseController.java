package com.studyflow.controllers;

import com.google.gson.Gson;
import com.studyflow.models.Pet;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.PetService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.PetPreviewSupport;
import com.studyflow.utils.PetUiSupport;
import com.studyflow.utils.UserSession;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

public class PetMetaverseController implements Initializable {
    private static final String PETVERSE_RESOURCE = "/petverse_embedded.html";

    @FXML private Label metaverseTitleLabel;
    @FXML private Label metaverseSubtitleLabel;
    @FXML private Label metaverseStatusLabel;
    @FXML private Label metaverseStatsLabel;
    @FXML private Label metaverseEmbedStatusLabel;
    @FXML private StackPane metaversePetPreviewPane;
    @FXML private WebView metaverseWebView;
    @FXML private StackPane metaverseBrowserOverlay;

    private final Gson gson = new Gson();
    private PetService petService;
    private ProjectService projectService;
    private WebEngine webEngine;
    private Pet currentPet;
    private String pendingPetsJson = "[]";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            petService = new PetService();
        } catch (RuntimeException ignored) {
            petService = null;
        }
        try {
            projectService = new ProjectService();
        } catch (RuntimeException ignored) {
            projectService = null;
        }
        configureWebView();
        loadMetaverse();
    }

    @FXML
    private void handleBackToPet() {
        MainController.loadContentInMainArea("views/MyPet.fxml");
    }

    @FXML
    private void handleReload() {
        if (webEngine != null) {
            setBrowserStatus("Reloading metaverse...");
            webEngine.reload();
            return;
        }
        loadMetaverse();
    }

    private void configureWebView() {
        if (metaverseWebView == null) {
            return;
        }
        webEngine = metaverseWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        metaverseWebView.setContextMenuEnabled(false);
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SCHEDULED || newState == Worker.State.RUNNING) {
                setBrowserStatus("Starting PetVerse Garden...");
                return;
            }
            if (newState == Worker.State.SUCCEEDED) {
                injectPendingPet();
                if (currentPet == null) {
                    setBrowserStatus("Adopt a companion first to sync PetVerse.");
                } else {
                    setBrowserStatus("");
                }
                return;
            }
            if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                Throwable error = webEngine.getLoadWorker().getException();
                setBrowserStatus("PetVerse failed to load: " + rootMessage(error));
            }
        });
        webEngine.setOnAlert(event -> handlePetEvent(event.getData()));
    }

    private void loadMetaverse() {
        User user = UserSession.getInstance().getCurrentUser();
        currentPet = null;
        if (user != null && petService != null && petService.isDatabaseAvailable()) {
            try {
                currentPet = petService.findByUserId(user.getId()).map(petService::syncPetState).orElse(null);
            } catch (RuntimeException ignored) {
                currentPet = null;
            }
        }

        if (currentPet == null) {
            metaverseTitleLabel.setText("Pet Metaverse");
            metaverseSubtitleLabel.setText("Adopt a companion first to enter the garden.");
            metaverseStatusLabel.setText("No active companion");
            metaverseStatsLabel.setText("Your metaverse map unlocks after adoption.");
            renderPetPreview("cat");
            pendingPetsJson = "[]";
            loadEmbeddedPetverse();
            return;
        }

        String rarity = normalizeRarity(currentPet.getRarity());
        pendingPetsJson = gson.toJson(buildMetaversePets(user, currentPet));

        metaverseTitleLabel.setText(currentPet.getName() + "'s Metaverse");
        metaverseSubtitleLabel.setText("A live PetVerse garden view using your current companion data.");
        metaverseStatusLabel.setText(rarity + " | " + currentPet.getMood());
        metaverseStatsLabel.setText("Level " + currentPet.getLevel() + " | " + currentPet.getHunger() + "/100 hunger | " + currentPet.getEvolutionStage());
        renderPetPreview(currentPet.getType());
        loadEmbeddedPetverse();
    }

    private void renderPetPreview(String type) {
        if (metaversePetPreviewPane == null) {
            return;
        }
        metaversePetPreviewPane.getChildren().setAll(PetPreviewSupport.createPreview(type, 212, 212));
    }

    private void loadEmbeddedPetverse() {
        if (webEngine == null) {
            return;
        }
        URL resource = getClass().getResource(PETVERSE_RESOURCE);
        if (resource == null) {
            setBrowserStatus("Missing resource: " + PETVERSE_RESOURCE);
            return;
        }
        webEngine.load(resource.toExternalForm());
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

    private void setBrowserStatus(String message) {
        if (metaverseEmbedStatusLabel != null) {
            metaverseEmbedStatusLabel.setText(message == null ? "" : message.trim());
        }
        if (metaverseBrowserOverlay != null) {
            boolean visible = message != null && !message.isBlank();
            metaverseBrowserOverlay.setVisible(visible);
            metaverseBrowserOverlay.setManaged(visible);
        }
    }

    private void injectPendingPet() {
        if (webEngine == null || pendingPetsJson == null || pendingPetsJson.isBlank()) {
            return;
        }
        try {
            webEngine.executeScript("window.PETVERSE_API && window.PETVERSE_API.injectPets(" + pendingPetsJson + ")");
        } catch (RuntimeException ex) {
            setBrowserStatus("PetVerse injection failed: " + rootMessage(ex));
        }
    }

    private String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage() == null ? "" : current.getMessage().trim();
        return message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private Map<String, Object> buildPetData(Pet pet, String owner, String rarity) {
        return buildPetData(pet, owner, rarity, true, 1);
    }

    private Map<String, Object> buildPetData(Pet pet, String owner, String rarity, boolean isSelf, int sharedProjectsCount) {
        String type = PetUiSupport.normalizeType(pet.getType()).toLowerCase();
        Map<String, Object> palette = paletteFor(type);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", type);
        payload.put("userId", pet.getUserId());
        payload.put("name", pet.getName());
        payload.put("owner", owner);
        payload.put("rarity", rarity);
        payload.put("color", palette.get("color"));
        payload.put("glow", palette.get("glow"));
        payload.put("desc", petDescription(pet));
        payload.put("lvl", pet.getLevel());
        payload.put("pow", clampMetric(pet.getHealth()));
        payload.put("spd", clampMetric(pet.getEnergy()));
        payload.put("charm", clampMetric(pet.getHappiness()));
        payload.put("fly", isFlyingType(type));
        payload.put("sharedProjectsCount", Math.max(0, sharedProjectsCount));
        payload.put("hasSharedProjects", sharedProjectsCount > 0 || isSelf);
        payload.put("isSelf", isSelf);
        return payload;
    }

    private Map<String, Object> paletteFor(String type) {
        Map<String, Object> palette = new LinkedHashMap<>();
        switch (type) {
            case "dog" -> {
                palette.put("color", "#c8964a");
                palette.put("glow", "rgba(200,150,74,");
            }
            case "dragon" -> {
                palette.put("color", "#44cc55");
                palette.put("glow", "rgba(68,204,85,");
            }
            case "fox" -> {
                palette.put("color", "#ee7733");
                palette.put("glow", "rgba(238,119,51,");
            }
            case "bird" -> {
                palette.put("color", "#bb9944");
                palette.put("glow", "rgba(187,153,68,");
            }
            case "hamster" -> {
                palette.put("color", "#ddaa66");
                palette.put("glow", "rgba(221,170,102,");
            }
            case "panda" -> {
                palette.put("color", "#aabbcc");
                palette.put("glow", "rgba(170,187,204,");
            }
            case "rabbit" -> {
                palette.put("color", "#ddccee");
                palette.put("glow", "rgba(221,204,238,");
            }
            default -> {
                palette.put("color", "#dd8844");
                palette.put("glow", "rgba(221,136,68,");
            }
        }
        return palette;
    }

    private String petDescription(Pet pet) {
        String personality = pet.getPersonality() == null ? "companion" : pet.getPersonality().toLowerCase();
        return pet.getName() + " is your " + personality + " " + PetUiSupport.normalizeType(pet.getType()).toLowerCase()
                + " companion, currently feeling " + pet.getMood().toLowerCase() + ".";
    }

    private int clampMetric(int value) {
        return Math.max(0, Math.min(99, value));
    }

    private boolean isFlyingType(String type) {
        return "bird".equalsIgnoreCase(type) || "dragon".equalsIgnoreCase(type);
    }

    private void handlePetEvent(String payload) {
        if (payload == null || payload.isBlank() || currentPet == null || petService == null) {
            return;
        }
        if (payload.startsWith("PET_FED:")) {
            handleFeed(payload);
            return;
        }
        if (payload.startsWith("PET_PLAYED:")) {
            handlePlay(payload);
        }
    }

    private void handleFeed(String ignoredPayload) {
        try {
            currentPet = petService.feedPet(currentPet.getUserId(), currentPet.getId(), "basic");
            syncSessionCoins();
            pendingPetsJson = gson.toJson(buildMetaversePets(currentUser(), currentPet));
            refreshPetSummary();
            injectPendingPet();
            setBrowserStatus("");
        } catch (RuntimeException ex) {
            setBrowserStatus(ex.getMessage());
        }
    }

    private void handlePlay(String ignoredPayload) {
        try {
            currentPet = petService.performAction(currentPet.getUserId(), currentPet.getId(), "play");
            syncSessionCoins();
            pendingPetsJson = gson.toJson(buildMetaversePets(currentUser(), currentPet));
            refreshPetSummary();
            injectPendingPet();
            setBrowserStatus("");
        } catch (RuntimeException ex) {
            setBrowserStatus(ex.getMessage());
        }
    }

    private void refreshPetSummary() {
        if (currentPet == null) {
            return;
        }
        metaverseTitleLabel.setText(currentPet.getName() + "'s Metaverse");
        metaverseStatusLabel.setText(currentPet.getRarity() + " | " + currentPet.getMood());
        metaverseStatsLabel.setText("Level " + currentPet.getLevel() + " | " + currentPet.getHunger() + "/100 hunger | " + currentPet.getEvolutionStage());
        renderPetPreview(currentPet.getType());
    }

    private void syncSessionCoins() {
        User user = currentUser();
        if (user == null || petService == null || !petService.isDatabaseAvailable()) {
            return;
        }
        user.setCoins(petService.getUserCoins(user.getId()));
    }

    private User currentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private List<Map<String, Object>> buildMetaversePets(User sessionUser, Pet sessionPet) {
        List<Map<String, Object>> pets = new ArrayList<>();
        if (sessionPet == null) {
            return pets;
        }

        Map<Integer, String> collaboratorNames = new LinkedHashMap<>();
        Map<Integer, Integer> sharedProjectCounts = new LinkedHashMap<>();
        collectSharedProjectCollaborators(sessionUser, collaboratorNames, sharedProjectCounts);

        String sessionOwner = sessionUser == null ? "You" : displayName(sessionUser);
        pets.add(buildPetData(
                sessionPet,
                sessionOwner,
                normalizeRarity(sessionPet.getRarity()),
                true,
                Math.max(1, sharedProjectCounts.getOrDefault(sessionPet.getUserId(), 1))
        ));

        if (petService == null || !petService.isDatabaseAvailable()) {
            return pets;
        }

        for (Map.Entry<Integer, Integer> entry : sharedProjectCounts.entrySet()) {
            int collaboratorUserId = entry.getKey();
            if (collaboratorUserId == sessionPet.getUserId()) {
                continue;
            }
            try {
                Pet collaboratorPet = petService.findByUserId(collaboratorUserId)
                        .map(petService::syncPetState)
                        .orElse(null);
                if (collaboratorPet == null) {
                    continue;
                }
                pets.add(buildPetData(
                        collaboratorPet,
                        collaboratorNames.getOrDefault(collaboratorUserId, "Collaborator"),
                        normalizeRarity(collaboratorPet.getRarity()),
                        false,
                        entry.getValue()
                ));
            } catch (RuntimeException ignored) {
                // Ignore individual collaborator pet failures and continue building the garden.
            }
        }
        return pets;
    }

    private void collectSharedProjectCollaborators(User sessionUser, Map<Integer, String> collaboratorNames, Map<Integer, Integer> sharedProjectCounts) {
        if (sessionUser == null || projectService == null || !projectService.isDatabaseAvailable()) {
            return;
        }
        List<Project> projects;
        try {
            projects = projectService.getByUserId(sessionUser.getId());
        } catch (RuntimeException ex) {
            return;
        }

        Set<Integer> ownedProjectIds = new LinkedHashSet<>();
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            int ownerUserId = project.getOwnerUserId();
            if (ownerUserId > 0 && ownerUserId != sessionUser.getId()) {
                incrementCollaborator(sharedProjectCounts, ownerUserId);
                collaboratorNames.putIfAbsent(ownerUserId, fallbackName(project.getOwnerName()));
            }
            if (project.isOwnedByCurrentUser()) {
                ownedProjectIds.add(project.getId());
            }
        }

        for (Integer projectId : ownedProjectIds) {
            try {
                for (User sharedUser : projectService.getSharedUsers(projectId)) {
                    if (sharedUser == null || sharedUser.getId() <= 0 || sharedUser.getId() == sessionUser.getId()) {
                        continue;
                    }
                    incrementCollaborator(sharedProjectCounts, sharedUser.getId());
                    collaboratorNames.putIfAbsent(sharedUser.getId(), displayName(sharedUser));
                }
            } catch (RuntimeException ignored) {
                // Ignore per-project share lookup failures.
            }
        }
    }

    private void incrementCollaborator(Map<Integer, Integer> sharedProjectCounts, int userId) {
        sharedProjectCounts.put(userId, sharedProjectCounts.getOrDefault(userId, 0) + 1);
    }

    private String normalizeRarity(String rarity) {
        return rarity == null || rarity.isBlank() ? "Common" : rarity;
    }

    private String fallbackName(String value) {
        return value == null || value.isBlank() ? "Collaborator" : value.trim();
    }
}
