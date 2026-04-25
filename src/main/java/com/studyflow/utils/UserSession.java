package com.studyflow.utils;

import com.studyflow.models.User;

public class UserSession {
    private static UserSession instance;
    private User currentUser;
    private String loginMethod = "password";
    private String pendingFaceIdSnapshotDataUrl;
    private String lastDetectedMood;
    private double lastDetectedMoodConfidence;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null)
            instance = new UserSession();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public boolean isLoggedIn() { return currentUser != null; }
    public void logout() {
        currentUser = null;
        loginMethod = "password";
        pendingFaceIdSnapshotDataUrl = null;
        lastDetectedMood = null;
        lastDetectedMoodConfidence = 0d;
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
}
