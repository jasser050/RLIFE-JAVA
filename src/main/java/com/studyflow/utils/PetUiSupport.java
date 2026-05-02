package com.studyflow.utils;

import javafx.scene.image.Image;

import java.io.InputStream;

public final class PetUiSupport {
    private static final String CAT_TYPE = "cat";

    private PetUiSupport() {
    }

    public static Image loadPetImage(String type) {
        String path = resourcePathForType(type);
        InputStream stream = PetUiSupport.class.getResourceAsStream(path);
        if (stream != null) {
            return new Image(stream);
        }
        InputStream fallback = PetUiSupport.class.getResourceAsStream("/com/studyflow/images/logo.png");
        return fallback == null ? null : new Image(fallback);
    }

    public static Image loadPetPreviewImage(String type) {
        String path = previewResourcePathForType(type);
        InputStream stream = PetUiSupport.class.getResourceAsStream(path);
        if (stream != null) {
            return new Image(stream);
        }
        return loadPetImage(type);
    }

    public static String resourcePathForType(String type) {
        String normalized = normalizeType(type);
        return switch (normalized) {
            case "dog" -> "/com/studyflow/assets/pets/dog-avatar.jpg";
            case "dragon" -> "/com/studyflow/assets/pets/dragon-avatar.jpg";
            case "fox" -> "/com/studyflow/assets/pets/fox-avatar.jpg";
            case "bird" -> "/com/studyflow/assets/pets/bird-avatar.jpg";
            case "hamster" -> "/com/studyflow/assets/pets/hamster-avatar.jpg";
            case "panda" -> "/com/studyflow/assets/pets/panda-avatar.jpg";
            case "rabbit" -> "/com/studyflow/assets/pets/rabbit-avatar.jpg";
            default -> "/com/studyflow/assets/pets/cat-avatar.jpg";
        };
    }

    public static String previewResourcePathForType(String type) {
        String normalized = normalizeType(type);
        return switch (normalized) {
            case "cat" -> "/com/studyflow/assets/pets/cat-preview.png";
            default -> resourcePathForType(normalized);
        };
    }

    public static String normalizeType(String type) {
        return type == null ? CAT_TYPE : type.trim().toLowerCase();
    }

    public static String previewType(String type) {
        return normalizeType(type);
    }

    public static String moodFromHunger(int hunger) {
        if (hunger < 25) {
            return "Excellent";
        }
        if (hunger < 50) {
            return "Good";
        }
        if (hunger < 75) {
            return "Needs care";
        }
        return "Critical";
    }
}
