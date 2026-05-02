package com.studyflow.controllers;

import com.studyflow.models.Pet;
import com.studyflow.models.PetEvent;
import com.studyflow.models.User;
import com.studyflow.services.PetService;
import com.studyflow.utils.PetPreviewSupport;
import com.studyflow.utils.PetUiSupport;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class NotesController implements Initializable {
    private static final DateTimeFormatter EVENT_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm");
    private static final Map<String, PetTypeDef> PET_TYPES = buildPetTypes();

    @FXML private Label globalMessageLabel;
    @FXML private Label petCountLabel;
    @FXML private Label coinsLabel;
    @FXML private Label moodSummaryLabel;
    @FXML private Label evolutionSummaryLabel;
    @FXML private VBox chooseSection;
    @FXML private VBox manageSection;
    @FXML private FlowPane adoptPetGrid;
    @FXML private FlowPane changeTypeGrid;
    @FXML private TextField petNameField;
    @FXML private Label selectedPetTypeLabel;
    @FXML private Label selectedPetHintLabel;
    @FXML private StackPane petPreviewPane;
    @FXML private Label petDisplayNameLabel;
    @FXML private Label petMetaLabel;
    @FXML private Label petPersonalityLabel;
    @FXML private Label petRarityBadge;
    @FXML private Label currentTypeBadge;
    @FXML private TextField renameField;
    @FXML private Label hungerValueLabel;
    @FXML private Label happinessValueLabel;
    @FXML private Label energyValueLabel;
    @FXML private Label healthValueLabel;
    @FXML private Label xpValueLabel;
    @FXML private ProgressBar hungerBar;
    @FXML private ProgressBar happinessBar;
    @FXML private ProgressBar energyBar;
    @FXML private ProgressBar healthBar;
    @FXML private ProgressBar xpBar;
    @FXML private VBox eventListBox;
    @FXML private FlowPane achievementsPane;
    @FXML private Button changeTypeButton;

    private PetService petService;
    private final List<PetCardRefs> adoptCards = new ArrayList<>();
    private final List<PetCardRefs> changeCards = new ArrayList<>();
    private String selectedPetType = "cat";
    private Pet currentPet;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildPetSelection(adoptPetGrid, adoptCards, true);
        buildPetSelection(changeTypeGrid, changeCards, false);
        try {
            petService = new PetService();
        } catch (RuntimeException ex) {
            petService = null;
        }
        refreshPage();
    }

    @FXML
    private void handleRefresh() {
        refreshPage();
    }

    @FXML
    private void handleAdoptPet() {
        User user = currentUser();
        if (user == null) {
            showMessage("Please log in to adopt a companion.", true);
            return;
        }
        String name = petNameField == null ? "" : petNameField.getText().trim();
        if (name.length() < 2 || name.length() > 25) {
            showMessage("Pet name must be between 2 and 25 characters.", true);
            return;
        }
        try {
            currentPet = petService.createPet(user.getId(), selectedPetType, name);
            showMessage(name + " joined your journey.", false);
            refreshPage();
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    @FXML
    private void handleRenamePet() {
        if (currentPet == null) {
            return;
        }
        String value = renameField == null ? "" : renameField.getText().trim();
        if (value.length() < 2 || value.length() > 25) {
            showMessage("Pet name must be between 2 and 25 characters.", true);
            return;
        }
        try {
            currentPet = petService.renamePet(currentPet.getUserId(), currentPet.getId(), value);
            showMessage("Companion renamed.", false);
            refreshPage();
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    @FXML
    private void handleChangeType() {
        if (currentPet == null) {
            return;
        }
        try {
            currentPet = petService.changeType(currentPet.getUserId(), currentPet.getId(), selectedPetType);
            showMessage("Companion type updated.", false);
            refreshPage();
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    @FXML
    private void handleFeedBasic() {
        feed("basic");
    }

    @FXML
    private void handleFeedPremium() {
        feed("premium");
    }

    @FXML
    private void handleFeedDeluxe() {
        feed("deluxe");
    }

    @FXML
    private void handleActionStudy() {
        act("study");
    }

    @FXML
    private void handleActionPlay() {
        act("play");
    }

    @FXML
    private void handleActionRest() {
        act("rest");
    }

    @FXML
    private void handleActionClean() {
        act("clean");
    }

    private void feed(String foodType) {
        if (currentPet == null) {
            return;
        }
        try {
            currentPet = petService.feedPet(currentPet.getUserId(), currentPet.getId(), foodType);
            syncSessionCoins();
            showMessage(currentPet.getName() + " enjoyed the meal.", false);
            refreshPage();
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    private void act(String action) {
        if (currentPet == null) {
            return;
        }
        try {
            currentPet = petService.performAction(currentPet.getUserId(), currentPet.getId(), action);
            syncSessionCoins();
            showMessage(currentPet.getName() + " completed a " + action + " action.", false);
            refreshPage();
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    private void refreshPage() {
        User user = currentUser();
        if (user == null) {
            showMessage("Please log in to access My Pet.", true);
            return;
        }
        if (petService == null || !petService.isDatabaseAvailable()) {
            petCountLabel.setText("0");
            coinsLabel.setText(user.getCoins() + "");
            moodSummaryLabel.setText("Offline");
            evolutionSummaryLabel.setText("Unavailable");
            chooseSection.setVisible(true);
            chooseSection.setManaged(true);
            manageSection.setVisible(false);
            manageSection.setManaged(false);
            showMessage("Pet database is unavailable. Check the MySQL connection.", true);
            return;
        }
        syncSessionCoins();
        Optional<Pet> pet = petService.findByUserId(user.getId());
        currentPet = pet.map(petService::syncPetState).orElse(null);
        petCountLabel.setText(currentPet == null ? "0" : "1");
        coinsLabel.setText(String.valueOf(currentUser().getCoins()));
        moodSummaryLabel.setText(currentPet == null ? "No pet yet" : currentPet.getMood());
        evolutionSummaryLabel.setText(currentPet == null ? "Ready to adopt" : currentPet.getEvolutionStage());
        if (currentPet == null) {
            renderChooseState();
        } else {
            renderManageState();
        }
    }

    private void renderChooseState() {
        chooseSection.setVisible(true);
        chooseSection.setManaged(true);
        manageSection.setVisible(false);
        manageSection.setManaged(false);
        petNameField.clear();
        updateSelectedPetSummary();
        highlightCards(adoptCards, selectedPetType);
    }

    private void renderManageState() {
        chooseSection.setVisible(false);
        chooseSection.setManaged(false);
        manageSection.setVisible(true);
        manageSection.setManaged(true);
        renameField.setText(currentPet.getName());
        petDisplayNameLabel.setText(currentPet.getName());
        petMetaLabel.setText("Level " + currentPet.getLevel() + " - " + currentPet.getMood() + " - " + currentPet.getEvolutionStage());
        petPersonalityLabel.setText(currentPet.getPersonality());
        petRarityBadge.setText(currentPet.getRarity());
        currentTypeBadge.setText(capitalize(currentPet.getType()));
        hungerValueLabel.setText(currentPet.getHunger() + "/100");
        happinessValueLabel.setText(currentPet.getHappiness() + "/100");
        energyValueLabel.setText(currentPet.getEnergy() + "/100");
        healthValueLabel.setText(currentPet.getHealth() + "/100");
        xpValueLabel.setText(currentPet.getXp() + " / " + currentPet.getXpToNextLevel());
        hungerBar.setProgress(currentPet.getHunger() / 100d);
        happinessBar.setProgress(currentPet.getHappiness() / 100d);
        energyBar.setProgress(currentPet.getEnergy() / 100d);
        healthBar.setProgress(currentPet.getHealth() / 100d);
        xpBar.setProgress(Math.min(1d, currentPet.getXp() / (double) currentPet.getXpToNextLevel()));
        renderPetPreview(petPreviewPane, currentPet.getType(), 224, 224);
        selectedPetType = currentPet.getType();
        updateSelectedPetSummary();
        highlightCards(changeCards, selectedPetType);
        renderEvents();
        renderAchievements();
        changeTypeButton.setDisable(currentPet.getType().equalsIgnoreCase(selectedPetType));
    }

    private void renderEvents() {
        eventListBox.getChildren().clear();
        List<PetEvent> events;
        try {
            events = petService.findRecentEvents(currentPet.getId(), 6);
        } catch (RuntimeException ex) {
            eventListBox.getChildren().add(createEmptyState("Events unavailable", ex.getMessage()));
            showMessage(ex.getMessage(), true);
            return;
        }
        if (events.isEmpty()) {
            eventListBox.getChildren().add(createEmptyState("No events yet", "Feed or interact with your pet to start its story."));
            return;
        }
        for (PetEvent event : events) {
            VBox row = new VBox(4);
            row.getStyleClass().add("pet-event-item");

            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label(event.getTitle());
            title.getStyleClass().add("pet-event-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label time = new Label(event.getCreatedAt().format(EVENT_FORMATTER));
            time.getStyleClass().add("pet-event-time");
            top.getChildren().addAll(title, spacer, time);

            Label desc = new Label(event.getDescription());
            desc.setWrapText(true);
            desc.getStyleClass().add("pet-event-desc");
            row.getChildren().addAll(top, desc);
            eventListBox.getChildren().add(row);
        }
    }

    private void renderAchievements() {
        achievementsPane.getChildren().clear();
        List<AchievementDef> defs = List.of(
                new AchievementDef("First Bond", "Adopt your first companion", true),
                new AchievementDef("First Meal", "Feed your companion once", currentPet.getCoinsSpent() >= 50),
                new AchievementDef("Rising Star", "Reach level 3", currentPet.getLevel() >= 3),
                new AchievementDef("Evolution Ready", "Reach level 5", currentPet.getLevel() >= 5),
                new AchievementDef("Care Master", "Keep happiness above 80", currentPet.getHappiness() >= 80),
                new AchievementDef("Focused Buddy", "Earn coins from study sessions", currentUser().getCoins() > 0 && countStudyEvents() > 0)
        );
        for (AchievementDef def : defs) {
            VBox card = new VBox(6);
            card.getStyleClass().addAll("pet-achievement-card", def.unlocked ? "unlocked" : "locked");
            Label title = new Label(def.title);
            title.getStyleClass().add("pet-achievement-title");
            Label desc = new Label(def.description);
            desc.setWrapText(true);
            desc.getStyleClass().add("pet-achievement-desc");
            Label state = new Label(def.unlocked ? "Unlocked" : "Locked");
            state.getStyleClass().add("pet-achievement-state");
            card.getChildren().addAll(title, desc, state);
            achievementsPane.getChildren().add(card);
        }
    }

    private int countStudyEvents() {
        try {
            return (int) petService.findRecentEvents(currentPet.getId(), 30).stream()
                    .filter(event -> "study".equalsIgnoreCase(event.getEventType()))
                    .count();
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), true);
            return 0;
        }
    }

    private VBox createEmptyState(String titleText, String descriptionText) {
        VBox box = new VBox(6);
        box.getStyleClass().add("pet-empty-card");
        Label title = new Label(titleText);
        title.getStyleClass().add("pet-empty-title");
        Label desc = new Label(descriptionText);
        desc.setWrapText(true);
        desc.getStyleClass().add("pet-empty-desc");
        box.getChildren().addAll(title, desc);
        return box;
    }

    private void buildPetSelection(FlowPane host, List<PetCardRefs> refs, boolean adoptionMode) {
        host.getChildren().clear();
        refs.clear();
        for (PetTypeDef def : PET_TYPES.values()) {
            VBox card = new VBox(12);
            card.getStyleClass().add("pet-choice-card");
            card.setPrefWidth(adoptionMode ? 180 : 152);
            card.setPadding(new Insets(14));

            ImageView imageView = new ImageView(loadPetImage(def.key));
            imageView.setFitWidth(adoptionMode ? 120 : 96);
            imageView.setFitHeight(adoptionMode ? 120 : 96);
            imageView.setPreserveRatio(true);

            StackPane imageShell = new StackPane(imageView);
            imageShell.getStyleClass().add("pet-choice-image");
            imageShell.setPrefHeight(adoptionMode ? 140 : 112);

            Label name = new Label(def.label);
            name.getStyleClass().add("pet-choice-title");

            Label desc = new Label(def.personality + " companion");
            desc.getStyleClass().add("pet-choice-desc");
            desc.setWrapText(true);

            card.getChildren().addAll(imageShell, name, desc);
            card.setOnMouseClicked(event -> {
                selectedPetType = def.key;
                highlightCards(refs, selectedPetType);
                updateSelectedPetSummary();
                if (currentPet != null && changeTypeButton != null) {
                    changeTypeButton.setDisable(currentPet.getType().equalsIgnoreCase(selectedPetType));
                }
            });
            refs.add(new PetCardRefs(def.key, card));
            host.getChildren().add(card);
        }
        highlightCards(refs, selectedPetType);
    }

    private void highlightCards(List<PetCardRefs> refs, String selectedType) {
        for (PetCardRefs ref : refs) {
            if (ref.type.equalsIgnoreCase(selectedType)) {
                if (!ref.card.getStyleClass().contains("selected")) {
                    ref.card.getStyleClass().add("selected");
                }
            } else {
                ref.card.getStyleClass().remove("selected");
            }
        }
    }

    private void updateSelectedPetSummary() {
        PetTypeDef def = PET_TYPES.getOrDefault(selectedPetType, PET_TYPES.get("cat"));
        selectedPetTypeLabel.setText(def.label);
        selectedPetHintLabel.setText(def.personality + " - " + def.rarityHint);
    }

    private Image loadPetImage(String type) {
        return PetUiSupport.loadPetImage(type);
    }

    private void renderPetPreview(StackPane host, String type, double width, double height) {
        if (host == null) {
            return;
        }
        host.getChildren().setAll(PetPreviewSupport.createPreview(type, width, height));
    }

    @FXML
    private void handleOpenMetaverse() {
        MainController.loadContentInMainArea("views/PetMetaverse.fxml");
    }

    private void showMessage(String message, boolean error) {
        if (globalMessageLabel == null) {
            return;
        }
        globalMessageLabel.setText(message);
        globalMessageLabel.setVisible(message != null && !message.isBlank());
        globalMessageLabel.setManaged(message != null && !message.isBlank());
        globalMessageLabel.getStyleClass().removeAll("pet-message-error", "pet-message-success");
        globalMessageLabel.getStyleClass().add(error ? "pet-message-error" : "pet-message-success");
    }

    private void syncSessionCoins() {
        User sessionUser = currentUser();
        if (sessionUser == null) {
            return;
        }
        sessionUser.setCoins(petService.getUserCoins(sessionUser.getId()));
    }

    private User currentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private static Map<String, PetTypeDef> buildPetTypes() {
        Map<String, PetTypeDef> map = new LinkedHashMap<>();
        map.put("cat", new PetTypeDef("cat", "Cat", "Playful", "Quick learner", "/com/studyflow/assets/pets/cat-avatar.jpg"));
        map.put("dog", new PetTypeDef("dog", "Dog", "Loyal", "Strong team spirit", "/com/studyflow/assets/pets/dog-avatar.jpg"));
        map.put("dragon", new PetTypeDef("dragon", "Dragon", "Bold", "Rare power growth", "/com/studyflow/assets/pets/dragon-avatar.jpg"));
        map.put("fox", new PetTypeDef("fox", "Fox", "Curious", "Fast reaction time", "/com/studyflow/assets/pets/fox-avatar.jpg"));
        map.put("bird", new PetTypeDef("bird", "Bird", "Energetic", "High focus boost", "/com/studyflow/assets/pets/bird-avatar.jpg"));
        map.put("hamster", new PetTypeDef("hamster", "Hamster", "Cozy", "Low-maintenance friend", "/com/studyflow/assets/pets/hamster-avatar.jpg"));
        map.put("panda", new PetTypeDef("panda", "Panda", "Calm", "Stable wellbeing balance", "/com/studyflow/assets/pets/panda-avatar.jpg"));
        map.put("rabbit", new PetTypeDef("rabbit", "Rabbit", "Gentle", "Excellent recovery", "/com/studyflow/assets/pets/rabbit-avatar.jpg"));
        return map;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private record PetTypeDef(String key, String label, String personality, String rarityHint, String resourcePath) {}
    private record PetCardRefs(String type, VBox card) {}
    private record AchievementDef(String title, String description, boolean unlocked) {}
}
