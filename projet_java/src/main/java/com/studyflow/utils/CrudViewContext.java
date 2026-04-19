package com.studyflow.utils;

import com.studyflow.models.Assignment;
import com.studyflow.models.Project;

import java.util.ArrayList;
import java.util.List;

public final class CrudViewContext {
    private static Assignment assignment;
    private static Project project;
    private static List<Project> ownedProjects = new ArrayList<>();
    private static Integer assignmentSelectionId;
    private static Integer projectSelectionId;
    private static String flashMessage;
    private static boolean flashError;

    private CrudViewContext() {
    }

    public static void setAssignmentContext(Assignment value, List<Project> projects) {
        assignment = value;
        ownedProjects = projects == null ? new ArrayList<>() : new ArrayList<>(projects);
    }

    public static Assignment consumeAssignment() {
        Assignment value = assignment;
        assignment = null;
        return value;
    }

    public static List<Project> consumeOwnedProjects() {
        List<Project> value = new ArrayList<>(ownedProjects);
        ownedProjects = new ArrayList<>();
        return value;
    }

    public static void setProjectContext(Project value) {
        project = value;
    }

    public static Project consumeProject() {
        Project value = project;
        project = null;
        return value;
    }

    public static void rememberAssignmentSelection(Integer value) {
        assignmentSelectionId = value;
    }

    public static Integer consumeAssignmentSelection() {
        Integer value = assignmentSelectionId;
        assignmentSelectionId = null;
        return value;
    }

    public static void rememberProjectSelection(Integer value) {
        projectSelectionId = value;
    }

    public static Integer consumeProjectSelection() {
        Integer value = projectSelectionId;
        projectSelectionId = null;
        return value;
    }

    public static void setFlashMessage(String message, boolean error) {
        flashMessage = message;
        flashError = error;
    }

    public static FlashMessage consumeFlashMessage() {
        if (flashMessage == null || flashMessage.isBlank()) {
            return null;
        }
        FlashMessage value = new FlashMessage(flashMessage, flashError);
        flashMessage = null;
        flashError = false;
        return value;
    }

    public record FlashMessage(String message, boolean error) {
    }
}
