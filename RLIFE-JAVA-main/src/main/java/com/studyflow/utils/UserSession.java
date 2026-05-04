package com.studyflow.utils;

import com.studyflow.models.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserSession {
    private static UserSession instance;
    private User currentUser;
    private String loginMethod = "password";
    private String pendingFaceIdSnapshotDataUrl;
    private String lastDetectedMood;
    private double lastDetectedMoodConfidence;

    private static final Path SESSION_FILE = Paths.get(
            System.getProperty("user.home"), ".studyflow", "session.txt"
    );

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null)
            instance = new UserSession();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() { return currentUser != null; }

    public void logout() {
        currentUser = null;
        loginMethod = "password";
        pendingFaceIdSnapshotDataUrl = null;
        lastDetectedMood = null;
        lastDetectedMoodConfidence = 0d;
        clearSession();
    }

    public String getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(String loginMethod) {
        this.loginMethod = (loginMethod == null || loginMethod.isBlank()) ? "password" : loginMethod.trim().toLowerCase();
    }

    public boolean isFaceIdLogin() {
        return "face_id".equalsIgnoreCase(loginMethod);
    }

    public void setFaceIdSnapshotDataUrl(String dataUrl) {
        this.pendingFaceIdSnapshotDataUrl = dataUrl;
    }

    public String consumeFaceIdSnapshotDataUrl() {
        String value = pendingFaceIdSnapshotDataUrl;
        pendingFaceIdSnapshotDataUrl = null;
        return value;
    }

    public String getLastDetectedMood() {
        return lastDetectedMood;
    }

    public double getLastDetectedMoodConfidence() {
        return lastDetectedMoodConfidence;
    }

    public void setLastDetectedMood(String mood, double confidence) {
        this.lastDetectedMood = mood;
        this.lastDetectedMoodConfidence = confidence;
    }

    public void saveSession() {
        if (currentUser == null || currentUser.getEmail() == null) return;
        try {
            Files.createDirectories(SESSION_FILE.getParent());
            Files.writeString(SESSION_FILE, currentUser.getEmail());
        } catch (IOException e) {
            System.out.println("Could not save session: " + e.getMessage());
        }
    }

    public String loadSession() {
        try {
            if (Files.exists(SESSION_FILE)) {
                String email = Files.readString(SESSION_FILE).trim();
                if (!email.isEmpty()) return email;
            }
        } catch (IOException e) {
            System.out.println("Could not load session: " + e.getMessage());
        }
        return null;
    }

    public void clearSession() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (IOException e) {
            System.out.println("Could not clear session: " + e.getMessage());
        }
    }
}
