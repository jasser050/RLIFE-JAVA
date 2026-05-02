package com.studyflow.controllers;

import com.studyflow.models.Pet;
import com.studyflow.models.User;
import com.studyflow.pets.PetGardenView;
import com.studyflow.services.PetService;
import com.studyflow.utils.PetPreviewSupport;
import com.studyflow.utils.PetUiSupport;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PetMetaverseController implements Initializable {
    @FXML private Label metaverseTitleLabel;
    @FXML private Label metaverseSubtitleLabel;
    @FXML private Label metaverseStatusLabel;
    @FXML private Label metaverseStatsLabel;
    @FXML private ImageView metaversePetImageView;
    @FXML private StackPane metaverseGardenHost;

    private PetService petService;
    private PetGardenView petGardenView;
    private Pet currentPet;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            petService = new PetService();
        } catch (RuntimeException ignored) {
            petService = null;
        }
        petGardenView = new PetGardenView();
        petGardenView.setPrefSize(900, 700);
        petGardenView.setOnPetClicked(this::handlePetClicked);
        if (metaverseGardenHost != null) {
            metaverseGardenHost.getChildren().setAll(petGardenView);
        }
        loadMetaverse();
    }

    @FXML
    private void handleBackToPet() {
        MainController.loadContentInMainArea("views/Notes.fxml");
    }

    @FXML
    private void handleReload() {
        if (petGardenView != null) {
            petGardenView.reload();
        }
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
        currentPet = pet;

        if (pet == null) {
            metaverseTitleLabel.setText("Pet Metaverse");
            metaverseSubtitleLabel.setText("Adopt a companion first to enter the garden.");
            metaverseStatusLabel.setText("No active companion");
            metaverseStatsLabel.setText("Your metaverse map unlocks after adoption.");
            PetPreviewSupport.showPetPreview(metaversePetImageView, "cat");
            if (petGardenView != null) {
                petGardenView.setPets(List.of());
            }
            return;
        }

        String rarity = pet.getRarity() == null ? "Common" : pet.getRarity();
        String renderType = PetUiSupport.previewType(pet.getType());

        metaverseTitleLabel.setText(pet.getName() + "'s Metaverse");
        metaverseSubtitleLabel.setText("A live PetVerse garden view using your current companion data.");
        metaverseStatusLabel.setText(rarity + " | " + PetUiSupport.moodFromHunger(pet.getHunger()));
        metaverseStatsLabel.setText("Level " + pet.getLevel() + " | " + pet.getHunger() + "/100 hunger | " + pet.getEvolutionStage());
        PetPreviewSupport.showPetPreview(metaversePetImageView, renderType);
        if (petGardenView != null) {
            petGardenView.setPets(List.of(pet));
        }
    }

    private void handlePetClicked(String petType) {
        if (currentPet == null) {
            return;
        }
        String normalizedType = PetUiSupport.normalizeType(petType);
        if (!normalizedType.equals(PetUiSupport.normalizeType(currentPet.getType()))) {
            return;
        }
        String mood = currentPet.getMood();
        metaverseStatusLabel.setText(currentPet.getName() + " | " + mood);
        metaverseStatsLabel.setText(
                "Type " + currentPet.getType()
                        + " | Lv. " + currentPet.getLevel()
                        + " | HP " + currentPet.getHealth()
                        + " | Energy " + currentPet.getEnergy()
        );
    }
}
