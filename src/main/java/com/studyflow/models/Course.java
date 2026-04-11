package com.studyflow.models;

import javafx.beans.property.*;

/**
 * Model class representing a Course
 */
public class Course {

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty instructor = new SimpleStringProperty();
    private final StringProperty schedule = new SimpleStringProperty();
    private final StringProperty color = new SimpleStringProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final IntegerProperty notesCount = new SimpleIntegerProperty();
    private final IntegerProperty assignmentsCount = new SimpleIntegerProperty();

    public Course() {}

    public Course(String id, String code, String name, String instructor, String schedule,
                  String color, double progress, int notesCount, int assignmentsCount) {
        setId(id);
        setCode(code);
        setName(name);
        setInstructor(instructor);
        setSchedule(schedule);
        setColor(color);
        setProgress(progress);
        setNotesCount(notesCount);
        setAssignmentsCount(assignmentsCount);
    }

    // ID
    public String getId() { return id.get(); }
    public void setId(String value) { id.set(value); }
    public StringProperty idProperty() { return id; }

    // Code
    public String getCode() { return code.get(); }
    public void setCode(String value) { code.set(value); }
    public StringProperty codeProperty() { return code; }

    // Name
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // Instructor
    public String getInstructor() { return instructor.get(); }
    public void setInstructor(String value) { instructor.set(value); }
    public StringProperty instructorProperty() { return instructor; }

    // Schedule
    public String getSchedule() { return schedule.get(); }
    public void setSchedule(String value) { schedule.set(value); }
    public StringProperty scheduleProperty() { return schedule; }

    // Color
    public String getColor() { return color.get(); }
    public void setColor(String value) { color.set(value); }
    public StringProperty colorProperty() { return color; }

    // Progress
    public double getProgress() { return progress.get(); }
    public void setProgress(double value) { progress.set(value); }
    public DoubleProperty progressProperty() { return progress; }

    // Notes Count
    public int getNotesCount() { return notesCount.get(); }
    public void setNotesCount(int value) { notesCount.set(value); }
    public IntegerProperty notesCountProperty() { return notesCount; }

    // Assignments Count
    public int getAssignmentsCount() { return assignmentsCount.get(); }
    public void setAssignmentsCount(int value) { assignmentsCount.set(value); }
    public IntegerProperty assignmentsCountProperty() { return assignmentsCount; }
}
