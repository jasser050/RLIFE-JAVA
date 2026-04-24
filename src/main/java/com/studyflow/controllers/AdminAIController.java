package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.GroqService;
import com.studyflow.services.ServiceUser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminAIController implements Initializable {

    @FXML private VBox chatContainer;
    @FXML private TextField messageField;
    @FXML private Button sendBtn;
    @FXML private ScrollPane chatScroll;

    private final GroqService gemini = new GroqService();
    private final ServiceUser serviceUser = new ServiceUser();

    private static final String SYSTEM_PROMPT = """
            You are RLife Admin Assistant, an AI agent that helps administrators manage users on the RLife student platform.
            You have access to the following actions. When the admin asks you to perform an action, respond with EXACTLY one ACTION line at the very start of your response, followed by your human-readable message.

            Available actions (respond with the ACTION line exactly as shown):

            1. Add a user:
               ACTION:ADD|email|firstName|lastName|username|password|gender
               Example: ACTION:ADD|john@test.com|John|Doe|johndoe|password123|male

            2. Ban a user:
               ACTION:BAN|email|reason
               Example: ACTION:BAN|john@test.com|Spam and advertising

            3. Unban a user:
               ACTION:UNBAN|email
               Example: ACTION:UNBAN|john@test.com

            4. Delete a user:
               ACTION:DELETE|email
               Example: ACTION:DELETE|john@test.com

            5. Update a user field:
               ACTION:UPDATE|email|field|newValue
               Fields: firstName, lastName, username, phone, university, studentId, gender, bio
               Example: ACTION:UPDATE|john@test.com|university|MIT

            6. List/search users:
               ACTION:SEARCH|query
               Example: ACTION:SEARCH|banned
               Use query "all" to list all users, "banned" for banned users only.

            7. Count users:
               ACTION:COUNT

            RULES:
            - If the admin's request is clear, perform the action immediately.
            - If information is missing (like email), ask for it — do NOT guess.
            - When adding a user, if password is not specified, generate a random one.
            - If gender is not specified, default to "male".
            - Never reveal the ACTION line format to the admin. Just show friendly messages.
            - Keep responses short and professional.
            - If the admin asks something unrelated to user management, politely redirect.
            - The ACTION line must be the VERY FIRST line of your response, nothing before it.
            - Only include ONE action per response.
            """;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Welcome message
        addBotMessage("Hello! I'm the RLife Admin AI Assistant.\n\nI can help you manage users. Try saying things like:\n" +
                "• \"Add a new user named Ahmed with email ahmed@esprit.tn\"\n" +
                "• \"Ban user john@test.com for spamming\"\n" +
                "• \"Show me all banned users\"\n" +
                "• \"How many users are registered?\"\n" +
                "• \"Delete the user with email old@test.com\"\n\nWhat would you like to do?");

        // Send on Enter
        messageField.setOnAction(e -> handleSend());
    }

    @FXML
    private void handleSend() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;

        addUserMessage(msg);
        messageField.clear();
        messageField.setDisable(true);
        sendBtn.setDisable(true);

        // Show typing indicator
        HBox typing = createTypingIndicator();
        chatContainer.getChildren().add(typing);
        scrollToBottom();

        // Call Gemini in background thread
        Thread thread = new Thread(() -> {
            try {
                String response = gemini.chat(SYSTEM_PROMPT, msg);
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typing);
                    processAIResponse(response);
                    messageField.setDisable(false);
                    sendBtn.setDisable(false);
                    messageField.requestFocus();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typing);
                    addBotMessage("Sorry, I couldn't process that request. Error: " + e.getMessage());
                    messageField.setDisable(false);
                    sendBtn.setDisable(false);
                    messageField.requestFocus();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processAIResponse(String response) {
        String displayMessage;
        String actionResult = null;

        if (response.startsWith("ACTION:")) {
            int newlineIdx = response.indexOf('\n');
            String actionLine = newlineIdx > 0 ? response.substring(0, newlineIdx).trim() : response.trim();
            displayMessage = newlineIdx > 0 ? response.substring(newlineIdx + 1).trim() : "";

            actionResult = executeAction(actionLine);
        } else {
            displayMessage = response;
        }

        if (displayMessage != null && !displayMessage.isEmpty()) {
            addBotMessage(displayMessage);
        }
        if (actionResult != null) {
            addSystemMessage(actionResult);
        }
    }

    private String executeAction(String actionLine) {
        try {
            String[] parts = actionLine.substring("ACTION:".length()).split("\\|");
            String action = parts[0].trim().toUpperCase();

            switch (action) {
                case "ADD": return executeAdd(parts);
                case "BAN": return executeBan(parts);
                case "UNBAN": return executeUnban(parts);
                case "DELETE": return executeDelete(parts);
                case "UPDATE": return executeUpdate(parts);
                case "SEARCH": return executeSearch(parts);
                case "COUNT": return executeCount();
                default: return "Unknown action: " + action;
            }
        } catch (Exception e) {
            return "Action failed: " + e.getMessage();
        }
    }

    private String executeAdd(String[] parts) {
        if (parts.length < 5) return "Missing required fields for adding a user.";
        String email = parts[1].trim();
        String firstName = parts[2].trim();
        String lastName = parts[3].trim();
        String username = parts.length > 4 ? parts[4].trim() : firstName.toLowerCase();
        String password = parts.length > 5 ? parts[5].trim() : "RLife" + (int)(Math.random() * 9000 + 1000);
        String gender = parts.length > 6 ? parts[6].trim() : "male";

        // Check if user exists
        if (serviceUser.findByEmail(email) != null) {
            return "A user with email " + email + " already exists.";
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(password);
        user.setGender(gender);
        serviceUser.add(user);

        return "User created successfully — " + firstName + " " + lastName + " (" + email + ")";
    }

    private String executeBan(String[] parts) {
        if (parts.length < 2) return "Email is required to ban a user.";
        String email = parts[1].trim();
        String reason = parts.length > 2 ? parts[2].trim() : "Violation of terms";

        User user = serviceUser.findByEmail(email);
        if (user == null) return "No user found with email: " + email;
        if (user.isBanned()) return user.getFullName().trim() + " is already banned.";

        serviceUser.banUser(user.getId(), reason);
        return "Banned " + user.getFullName().trim() + " — Reason: " + reason;
    }

    private String executeUnban(String[] parts) {
        if (parts.length < 2) return "Email is required to unban a user.";
        String email = parts[1].trim();

        User user = serviceUser.findByEmail(email);
        if (user == null) return "No user found with email: " + email;
        if (!user.isBanned()) return user.getFullName().trim() + " is not currently banned.";

        serviceUser.unbanUser(user.getId());
        return "Unbanned " + user.getFullName().trim() + " — access restored.";
    }

    private String executeDelete(String[] parts) {
        if (parts.length < 2) return "Email is required to delete a user.";
        String email = parts[1].trim();

        User user = serviceUser.findByEmail(email);
        if (user == null) return "No user found with email: " + email;

        String name = user.getFullName().trim();
        serviceUser.delete(user);
        return "Deleted user " + name + " (" + email + ") permanently.";
    }

    private String executeUpdate(String[] parts) {
        if (parts.length < 4) return "Need email, field name, and new value.";
        String email = parts[1].trim();
        String field = parts[2].trim().toLowerCase();
        String value = parts[3].trim();

        User user = serviceUser.findByEmail(email);
        if (user == null) return "No user found with email: " + email;

        switch (field) {
            case "firstname": user.setFirstName(value); break;
            case "lastname": user.setLastName(value); break;
            case "username": user.setUsername(value); break;
            case "phone": user.setPhoneNumber(value); break;
            case "university": user.setUniversity(value); break;
            case "studentid": user.setStudentId(value); break;
            case "gender": user.setGender(value); break;
            case "bio": user.setBio(value); break;
            default: return "Unknown field: " + field + ". Available: firstName, lastName, username, phone, university, studentId, gender, bio";
        }

        serviceUser.update(user);
        return "Updated " + user.getFullName().trim() + " — " + field + " set to \"" + value + "\"";
    }

    private String executeSearch(String[] parts) {
        String query = parts.length > 1 ? parts[1].trim().toLowerCase() : "all";
        List<User> users;

        if ("all".equals(query)) {
            users = serviceUser.getAll().stream()
                    .filter(u -> !"admin@rlife.com".equalsIgnoreCase(u.getEmail()))
                    .collect(Collectors.toList());
        } else if ("banned".equals(query)) {
            users = serviceUser.getAll().stream()
                    .filter(u -> u.isBanned() && !"admin@rlife.com".equalsIgnoreCase(u.getEmail()))
                    .collect(Collectors.toList());
        } else {
            users = serviceUser.searchUsers(query);
        }

        if (users.isEmpty()) return "No users found.";

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(users.size()).append(" user(s):\n");
        for (User u : users) {
            sb.append("  • ").append(u.getFullName().trim())
              .append(" — ").append(u.getEmail());
            if (u.isBanned()) sb.append(" [BANNED]");
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String executeCount() {
        int total = serviceUser.countUsers();
        int banned = serviceUser.countBannedUsers();
        return "Total users: " + total + " | Active: " + (total - banned) + " | Banned: " + banned;
    }

    // ============================================
    // CHAT UI HELPERS
    // ============================================

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 0, 4, 60));

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chat-bubble-user");
        bubble.setPadding(new Insets(12, 16, 12, 16));

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.getStyleClass().add("chat-text-user");
        bubble.getChildren().add(msg);

        row.getChildren().add(bubble);
        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(4, 60, 4, 0));

        // Bot avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-bot-avatar");
        FontIcon icon = new FontIcon("fth-cpu");
        icon.setIconSize(16);
        icon.setIconColor(Color.WHITE);
        avatar.getChildren().add(icon);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chat-bubble-bot");
        bubble.setPadding(new Insets(12, 16, 12, 16));

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.getStyleClass().add("chat-text-bot");
        bubble.getChildren().add(msg);

        row.getChildren().addAll(avatar, bubble);
        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void addSystemMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(2, 60, 2, 44));

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("chat-bubble-system");
        bubble.setPadding(new Insets(10, 14, 10, 14));

        FontIcon checkIcon = new FontIcon("fth-check-circle");
        checkIcon.setIconSize(14);
        checkIcon.setIconColor(Color.web("#34D399"));

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.getStyleClass().add("chat-text-system");

        HBox content = new HBox(8);
        content.setAlignment(Pos.TOP_LEFT);
        content.getChildren().addAll(checkIcon, msg);

        bubble.getChildren().add(content);
        row.getChildren().add(bubble);
        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    private HBox createTypingIndicator() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(4, 60, 4, 0));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-bot-avatar");
        FontIcon icon = new FontIcon("fth-cpu");
        icon.setIconSize(16);
        icon.setIconColor(Color.WHITE);
        avatar.getChildren().add(icon);

        HBox dots = new HBox(6);
        dots.getStyleClass().add("chat-bubble-bot");
        dots.setPadding(new Insets(14, 20, 14, 20));
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            StackPane dot = new StackPane();
            dot.getStyleClass().add("typing-dot");
            dots.getChildren().add(dot);
        }

        row.getChildren().addAll(avatar, dots);
        return row;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }
}
