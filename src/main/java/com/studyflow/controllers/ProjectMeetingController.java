package com.studyflow.controllers;

import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.ActivityLogService;
import com.studyflow.services.NotificationService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.CrudViewContext;
import com.studyflow.utils.EmbeddedMeetingBrowser;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectMeetingController implements Initializable {
    @FXML private Label meetingProjectTitleLabel;
    @FXML private Label meetingProjectMetaLabel;
    @FXML private Label meetingRoomLabel;
    @FXML private Label meetingHintLabel;
    @FXML private Label meetingShareStatusLabel;
    @FXML private Label meetingEmbedStatusLabel;
    @FXML private Button backToProjectsButton;
    @FXML private Button reloadMeetingButton;
    @FXML private Button shareMeetingButton;
    @FXML private VBox meetingWindowCard;

    private final ProjectService projectService = new ProjectService();
    private final NotificationService notificationService = new NotificationService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private Project project;
    private String meetingUrl;
    private User currentUser;
    private EmbeddedMeetingBrowser.BrowserSession browserSession;
    private boolean meetingStarted;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        project = CrudViewContext.consumeProject();
        currentUser = UserSession.getInstance().getCurrentUser();

        if (project == null || currentUser == null) {
            showUnavailable("No project was selected for this meeting.");
            return;
        }
        if (!projectService.userHasProjectAccess(project.getId(), currentUser.getId())) {
            showUnavailable("You do not have access to this project's meeting room.");
            return;
        }

        String roomName = buildRoomName(project);
        meetingUrl = buildMeetingUrl(roomName, currentUser);

        if (meetingProjectTitleLabel != null) {
            meetingProjectTitleLabel.setText(project.getTitle());
        }
        if (meetingProjectMetaLabel != null) {
            meetingProjectMetaLabel.setText(project.isOwnedByCurrentUser() ? "Owner room" : "Shared project room");
        }
        if (meetingRoomLabel != null) {
            meetingRoomLabel.setText(roomName);
        }
        if (meetingHintLabel != null) {
            meetingHintLabel.setText("Anyone with access to this project joins the same Jitsi room.");
        }
        updateShareButtonState();
        initializeMeetingState();
    }

    @FXML
    private void handleBackToProjects() {
        if (project != null) {
            CrudViewContext.rememberProjectSelection(project.getId());
        }
        MainController.loadContentInMainArea("views/Projects.fxml");
    }

    @FXML
    private void handleReloadMeeting() {
        if (!meetingStarted) {
            setMeetingStatus(project != null && project.isOwnedByCurrentUser()
                    ? "Share the meeting to start the room."
                    : "The meeting has not started yet.");
            return;
        }
        if (browserSession != null && !browserSession.isClosed()) {
            setMeetingStatus("Bringing meeting window to front...");
            browserSession.focus();
            return;
        }
        browserSession = null;
        loadMeeting();
    }

    @FXML
    private void handleShareMeeting() {
        if (project == null || currentUser == null) {
            setShareStatus("Open a project meeting before sharing it.", true);
            return;
        }
        if (!project.isOwnedByCurrentUser()) {
            setShareStatus("Only the project owner can send meeting notifications.", true);
            return;
        }

        List<User> sharedUsers = projectService.getSharedUsers(project.getId());
        if (sharedUsers.isEmpty()) {
            setShareStatus("This project is not shared with anyone yet.", true);
            return;
        }

        String roomName = buildRoomName(project);
        String link = "project_meeting:" + project.getId();
        String message = "Join the meeting for project \"" + safe(project.getTitle()) + "\". Room: " + roomName;
        int sentCount = 0;

        for (User user : sharedUsers) {
            if (user == null || user.getId() <= 0 || user.getId() == currentUser.getId()) {
                continue;
            }
            notificationService.addNotificationForUser(
                    user.getId(),
                    "Project meeting started",
                    message,
                    "project_meeting",
                    link
            );
            sentCount++;
        }

        if (sentCount == 0) {
            setShareStatus("There are no other shared users to notify.", true);
            return;
        }
        activityLogService.addActivity("project", project.getId(), currentUser.getId(), "meeting_started", "Started the project meeting room and notified the team.");
        setShareStatus("Meeting shared with " + sentCount + " project member" + (sentCount == 1 ? "" : "s") + ".", false);
        startMeetingIfNeeded();
    }

    private void initializeMeetingState() {
        if (project == null || currentUser == null) {
            return;
        }
        if (project.isOwnedByCurrentUser()) {
            meetingStarted = false;
            setMeetingStatus("Share the meeting to start the room.");
            return;
        }
        startMeetingIfNeeded();
    }

    private void startMeetingIfNeeded() {
        if (meetingStarted && browserSession != null && !browserSession.isClosed()) {
            return;
        }
        boolean firstStart = !meetingStarted;
        meetingStarted = true;
        if (firstStart) {
            activityLogService.addActivity("project", project.getId(), currentUser.getId(), "meeting_joined", "Joined the project meeting room.");
        }
        loadMeeting();
    }

    private void loadMeeting() {
        if (meetingUrl == null || meetingUrl.isBlank()) {
            return;
        }
        if (browserSession != null && !browserSession.isClosed()) {
            browserSession.focus();
            setMeetingStatus("Meeting window is already open.");
            return;
        }
        browserSession = null;
        setMeetingStatus("Opening meeting window...");
        EmbeddedMeetingBrowser.openWindow(project == null ? "Project Meeting" : project.getTitle() + " Meeting", meetingUrl, this::setMeetingStatus)
                .thenAccept(session -> browserSession = session)
                .exceptionally(ex -> {
                    setMeetingStatus("Meeting window failed to start: " + rootMessage(ex));
                    return null;
                });
    }

    private void showUnavailable(String message) {
        if (meetingProjectTitleLabel != null) {
            meetingProjectTitleLabel.setText("Project Meeting");
        }
        if (meetingProjectMetaLabel != null) {
            meetingProjectMetaLabel.setText("Unavailable");
        }
        if (meetingRoomLabel != null) {
            meetingRoomLabel.setText("-");
        }
        if (meetingHintLabel != null) {
            meetingHintLabel.setText(message);
        }
        if (shareMeetingButton != null) {
            shareMeetingButton.setDisable(true);
        }
        setMeetingStatus(message);
    }

    private String buildMeetingUrl(String roomName, User user) {
        String displayName = safe(user.getFullName()).isBlank() ? safe(user.getUsername()) : safe(user.getFullName());
        String roomPath = encode(roomName).replace("+", "%20");
        String quotedName = "%22" + encode(displayName).replace("+", "%20") + "%22";
        String quotedSubject = "%22" + encode(safe(project.getTitle())).replace("+", "%20") + "%22";
        return "https://meet.jit.si/" + roomPath
                + "#config.prejoinPageEnabled=false"
                + "&config.disableDeepLinking=true"
                + "&config.startWithAudioMuted=true"
                + "&config.startWithVideoMuted=true"
                + "&interfaceConfig.MOBILE_APP_PROMO=false"
                + "&userInfo.displayName=" + quotedName
                + "&config.subject=" + quotedSubject;
    }

    private void updateShareButtonState() {
        if (shareMeetingButton == null) {
            return;
        }
        boolean canShare = project != null
                && currentUser != null
                && project.isOwnedByCurrentUser()
                && !projectService.getSharedUsers(project.getId()).isEmpty();
        shareMeetingButton.setDisable(!canShare);

        if (meetingShareStatusLabel != null) {
            if (project == null || currentUser == null) {
                meetingShareStatusLabel.setText("");
            } else if (!project.isOwnedByCurrentUser()) {
                meetingShareStatusLabel.setText("Only the owner can send meeting invites.");
            } else if (shareMeetingButton.isDisable()) {
                meetingShareStatusLabel.setText("Share the project with users first to invite them here.");
            } else {
                meetingShareStatusLabel.setText("Share this meeting with the users already added to the project.");
            }
        }
    }

    private void setShareStatus(String message, boolean error) {
        if (meetingShareStatusLabel == null) {
            return;
        }
        meetingShareStatusLabel.setText(safe(message));
        meetingShareStatusLabel.getStyleClass().removeAll("inline-alert", "inline-alert-success", "inline-alert-error", "text-muted");
        meetingShareStatusLabel.getStyleClass().add("inline-alert");
        meetingShareStatusLabel.getStyleClass().add(error ? "inline-alert-error" : "inline-alert-success");
    }

    private String buildRoomName(Project project) {
        return "rlife-project-" + project.getId() + "-" + slugify(project.getTitle());
    }

    private String slugify(String value) {
        String normalized = safe(value).toLowerCase().replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "room" : normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void setMeetingStatus(String message) {
        if (meetingEmbedStatusLabel != null) {
            meetingEmbedStatusLabel.setText(safe(message));
        }
        if (meetingWindowCard != null) {
            meetingWindowCard.setDisable(false);
        }
    }

    private String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = safe(current.getMessage());
        return message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
