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

public final class BadgeManager {
    private static final Path FILE = Path.of("badges.json");
    private static final Gson GSON = new Gson();

    private BadgeManager() {}

    public static Map<String, String> getBadges() {
        return load();
    }

    public static String getBadge(String gameKey) {
        return load().getOrDefault(gameKey, "—");
    }

    public static void award(String key, String badgeText) {
        Map<String, String> b = load();
        b.put(key, badgeText);
        if (b.containsKey("flag") && b.containsKey("capital") && b.containsKey("map") && b.containsKey("memory")) {
            b.put("champion", "⭐ Geography Champion");
        }
        save(b);
    }

    private static Map<String, String> load() {
        try {
            if (!Files.exists(FILE)) return new HashMap<>();
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            Type t = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> map = GSON.fromJson(json, t);
            return map == null ? new HashMap<>() : new HashMap<>(map);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static void save(Map<String, String> data) {
        try {
            Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
