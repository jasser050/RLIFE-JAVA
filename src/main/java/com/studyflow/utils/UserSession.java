package com.studyflow.utils;

import com.studyflow.models.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserSession {
    private static UserSession instance;
    private User currentUser;

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
        clearSession();
    }

    /**
     * Save the user's email to a local file so the session persists
     * across app restarts.
     */
    public void saveSession() {
        if (currentUser == null || currentUser.getEmail() == null) return;
        try {
            Files.createDirectories(SESSION_FILE.getParent());
            Files.writeString(SESSION_FILE, currentUser.getEmail());
        } catch (IOException e) {
            System.out.println("Could not save session: " + e.getMessage());
        }
    }

    /**
     * Try to load a previously saved session.
     * Returns the stored email, or null if no session exists.
     */
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

    /**
     * Delete the saved session file (called on logout).
     */
    public void clearSession() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (IOException e) {
            System.out.println("Could not clear session: " + e.getMessage());
        }
    }
}
