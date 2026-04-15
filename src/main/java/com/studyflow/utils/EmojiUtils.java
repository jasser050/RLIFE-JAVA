package com.studyflow.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.Map;

/**
 * Utility class for loading and displaying emoji images.
 */
public class EmojiUtils {
    // OpenMoji PNG assets (JavaFX Image supports PNG)
    private static final String OPENMOJI_CDN_URL = "https://cdn.jsdelivr.net/gh/hfg-gmuend/openmoji@latest/color/72x72/";
    private static final String EMOJI_FORMAT = "png";
    private static final int DEFAULT_EMOJI_SIZE = 64;

    private static final Map<String, String> MOOD_EMOJI_MAP = Map.of(
            "great", "\uD83E\uDD29",     // 🤩
            "good", "\uD83D\uDE0A",      // 😊
            "okay", "\uD83D\uDE10",      // 😐
            "stressed", "\uD83D\uDE29",  // 😩
            "tired", "\uD83D\uDE34"      // 😴
    );

    /**
     * Get emoji image URL from OpenMoji CDN.
     *
     * @param emojiUnicode emoji character
     * @return URL to emoji image
     */
    public static String getEmojiImageUrl(String emojiUnicode) {
        if (emojiUnicode == null || emojiUnicode.isEmpty()) {
            return null;
        }

        String codepoint = toCodepoint(emojiUnicode);
        if (codepoint == null) {
            return null;
        }

        return OPENMOJI_CDN_URL + codepoint.toUpperCase() + "." + EMOJI_FORMAT;
    }

    /**
     * Convert emoji character to OpenMoji codepoint format.
     */
    private static String toCodepoint(String emoji) {
        if (emoji == null || emoji.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int[] codePoints = emoji.codePoints().toArray();

        for (int i = 0; i < codePoints.length; i++) {
            if (i > 0) {
                sb.append("-");
            }
            sb.append(String.format("%04x", codePoints[i]));
        }

        return sb.toString();
    }

    /**
     * Load emoji as ImageView.
     */
    public static ImageView loadEmojiImage(String emojiUnicode, int size) {
        String url = getEmojiImageUrl(emojiUnicode);
        if (url == null) {
            return null;
        }

        try {
            Image image = new Image(url, size, size, true, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            System.err.println("Error loading emoji image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Load emoji as ImageView with default size.
     */
    @SuppressWarnings("unused")
    public static ImageView loadEmojiImage(String emojiUnicode) {
        return loadEmojiImage(emojiUnicode, DEFAULT_EMOJI_SIZE);
    }

    /**
     * Load emoji from local resources as fallback.
     */
    @SuppressWarnings("unused")
    public static ImageView loadEmojiFromResources(String emojiName) {
        try {
            String resourcePath = "/com/studyflow/resources/emojis/" + emojiName + ".png";
            InputStream inputStream = EmojiUtils.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                Image image = new Image(inputStream);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(DEFAULT_EMOJI_SIZE);
                imageView.setFitHeight(DEFAULT_EMOJI_SIZE);
                imageView.setPreserveRatio(true);
                return imageView;
            }
        } catch (Exception e) {
            System.err.println("Error loading emoji from resources: " + e.getMessage());
        }
        return null;
    }

    /**
     * Map of moods to emoji unicode.
     */
    @SuppressWarnings("unused")
    public static Map<String, String> getMoodEmojiMap() {
        return MOOD_EMOJI_MAP;
    }

    /**
     * Map of moods to labels.
     */
    @SuppressWarnings("unused")
    public static Map<String, String> getMoodLabelMap() {
        return Map.of(
                "great", "Great",
                "good", "Good",
                "okay", "Okay",
                "stressed", "Stressed",
                "tired", "Tired"
        );
    }

    public static String getMoodEmojiUnicode(String moodKey) {
        if (moodKey == null) {
            return "\uD83D\uDE10";
        }
        return MOOD_EMOJI_MAP.getOrDefault(moodKey.toLowerCase(), "\uD83D\uDE10");
    }

    public static ImageView loadMoodEmojiImage(String moodKey, int size) {
        return loadEmojiImage(getMoodEmojiUnicode(moodKey), size);
    }
}
