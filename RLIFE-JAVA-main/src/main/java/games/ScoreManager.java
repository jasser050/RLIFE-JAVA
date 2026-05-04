package games;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ScoreManager {
    private static final Path FILE = Path.of("geo_highscores.json");
    private static final Gson GSON = new Gson();

    private ScoreManager() {}

    public static int getHighScore(String gameKey) {
        return load().getOrDefault(gameKey, 0);
    }

    public static void updateHighScore(String gameKey, int score) {
        Map<String, Integer> all = load();
        int old = all.getOrDefault(gameKey, 0);
        if (score > old) {
            all.put(gameKey, score);
            save(all);
        }
    }

    private static Map<String, Integer> load() {
        try {
            if (!Files.exists(FILE)) return new HashMap<>();
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            Type t = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> map = GSON.fromJson(json, t);
            return map == null ? new HashMap<>() : new HashMap<>(map);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static void save(Map<String, Integer> data) {
        try {
            Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
