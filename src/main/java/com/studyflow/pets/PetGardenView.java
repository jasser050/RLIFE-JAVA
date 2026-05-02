package com.studyflow.pets;

import com.studyflow.LocalServer;
import com.studyflow.models.Pet;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PetGardenView extends StackPane {
    private static final String GARDEN_URL = "/pets/petverse_garden_sky.html";

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private final JavaBridge javaBridge = new JavaBridge();
    private final List<Pet> pendingPets = new ArrayList<>();

    private Consumer<String> petClickHandler;
    private boolean bridgeInstalled;

    public PetGardenView() {
        getStyleClass().add("pet-garden-view");
        webView.setContextMenuEnabled(false);
        webView.setPageFill(null);
        webView.setPrefSize(800, 600);
        getChildren().add(webView);

        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                installBridge();
                flushPendingPets();
            }
        });
        engine.load(LocalServer.url(GARDEN_URL));
    }

    public void setPets(List<Pet> pets) {
        List<Pet> snapshot = pets == null ? List.of() : new ArrayList<>(pets);
        if (Platform.isFxApplicationThread()) {
            pendingPets.clear();
            pendingPets.addAll(snapshot);
            flushPendingPets();
        } else {
            Platform.runLater(() -> {
                pendingPets.clear();
                pendingPets.addAll(snapshot);
                flushPendingPets();
            });
        }
    }

    public void reload() {
        if (Platform.isFxApplicationThread()) {
            bridgeInstalled = false;
            engine.reload();
        } else {
            Platform.runLater(() -> {
                bridgeInstalled = false;
                engine.reload();
            });
        }
    }

    public void setOnPetClicked(Consumer<String> petClickHandler) {
        this.petClickHandler = petClickHandler;
    }

    private void installBridge() {
        try {
            Object window = engine.executeScript("window");
            if (window instanceof JSObject jsWindow) {
                jsWindow.setMember("javaBridge", javaBridge);
                bridgeInstalled = true;
            }
        } catch (RuntimeException ignored) {
            bridgeInstalled = false;
        }
    }

    private void flushPendingPets() {
        if (!bridgeInstalled) {
            return;
        }
        String json = toJson(pendingPets);
        String script = "window.setPetsFromJava(" + toJavaScriptString(json) + ");";
        try {
            engine.executeScript(script);
        } catch (RuntimeException ignored) {
            bridgeInstalled = false;
        }
    }

    private String toJson(List<Pet> pets) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < pets.size(); i++) {
            Pet pet = pets.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append('{')
                    .append("\"id\":").append(pet.getId()).append(',')
                    .append("\"userId\":").append(pet.getUserId()).append(',')
                    .append("\"type\":\"").append(escapeJson(normalizeType(pet.getType()))).append("\",")
                    .append("\"name\":\"").append(escapeJson(defaultString(pet.getName(), "Companion"))).append("\",")
                    .append("\"level\":").append(Math.max(1, pet.getLevel())).append(',')
                    .append("\"mood\":\"").append(escapeJson(pet.getMood())).append("\",")
                    .append("\"rarity\":\"").append(escapeJson(defaultString(pet.getRarity(), "Common"))).append("\",")
                    .append("\"personality\":\"").append(escapeJson(defaultString(pet.getPersonality(), "Curious"))).append("\",")
                    .append("\"hunger\":").append(pet.getHunger()).append(',')
                    .append("\"happiness\":").append(pet.getHappiness()).append(',')
                    .append("\"energy\":").append(pet.getEnergy()).append(',')
                    .append("\"health\":").append(pet.getHealth()).append(',')
                    .append("\"xp\":").append(pet.getXp()).append(',')
                    .append("\"stage\":\"").append(escapeJson(pet.getEvolutionStage())).append("\"")
                    .append('}');
        }
        builder.append(']');
        return builder.toString();
    }

    private String normalizeType(String type) {
        return defaultString(type, "cat").trim().toLowerCase();
    }

    private String defaultString(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String toJavaScriptString(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                + "'";
    }

    public final class JavaBridge {
        public void onPetClicked(String petType) {
            if (petClickHandler != null) {
                Platform.runLater(() -> petClickHandler.accept(petType == null ? "" : petType.trim().toLowerCase()));
            }
        }
    }
}
