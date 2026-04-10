package com.studyflow.models;

import javafx.beans.property.*;

import java.time.LocalDate;

/**
 * Model class representing an Assignment
 */
public class Assignment {

    public enum Priority { LOW, MEDIUM, HIGH }
    public enum Status { PENDING, IN_PROGRESS, COMPLETED }

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty courseId = new SimpleStringProperty();
    private final StringProperty courseName = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();
    private final ObjectProperty<Priority> priority = new SimpleObjectProperty<>(Priority.MEDIUM);
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.PENDING);

    public Assignment() {}

    public Assignment(String id, String title, String description, String courseId,
                      String courseName, LocalDate dueDate, Priority priority, Status status) {
        setId(id);
        setTitle(title);
        setDescription(description);
        setCourseId(courseId);
        setCourseName(courseName);
        setDueDate(dueDate);
        setPriority(priority);
        setStatus(status);
    }

    // ID
    public String getId() { return id.get(); }
    public void setId(String value) { id.set(value); }
    public StringProperty idProperty() { return id; }

    // Title
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }
    public StringProperty titleProperty() { return title; }

    // Description
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }

    // Course ID
    public String getCourseId() { return courseId.get(); }
    public void setCourseId(String value) { courseId.set(value); }
    public StringProperty courseIdProperty() { return courseId; }

    // Course Name
    public String getCourseName() { return courseName.get(); }
    public void setCourseName(String value) { courseName.set(value); }
    public StringProperty courseNameProperty() { return courseName; }

    // Due Date
    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate value) { dueDate.set(value); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    // Priority
    public Priority getPriority() { return priority.get(); }
    public void setPriority(Priority value) { priority.set(value); }
    public ObjectProperty<Priority> priorityProperty() { return priority; }

    // Status
    public Status getStatus() { return status.get(); }
    public void setStatus(Status value) { status.set(value); }
    public ObjectProperty<Status> statusProperty() { return status; }

    /**
     * Check if the assignment is overdue
     */
    public boolean isOverdue() {
        return dueDate.get() != null && dueDate.get().isBefore(LocalDate.now()) && status.get() != Status.COMPLETED;
    }

    /**
     * Get the priority color
     */
    public String getPriorityColor() {
        return switch (priority.get()) {
            case HIGH -> "danger";
            case MEDIUM -> "warning";
            case LOW -> "success";
        };
    }
}
