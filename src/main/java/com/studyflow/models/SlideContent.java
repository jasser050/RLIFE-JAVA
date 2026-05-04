package com.studyflow.models;

import java.util.ArrayList;
import java.util.List;

public class SlideContent {
    private int slideNumber;
    private String title;
    private List<String> bulletPoints = new ArrayList<>();
    private String speakerNotes;
    private SlideType type = SlideType.CONTENT;

    public int getSlideNumber() {
        return slideNumber;
    }

    public void setSlideNumber(int slideNumber) {
        this.slideNumber = slideNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getBulletPoints() {
        return bulletPoints;
    }

    public void setBulletPoints(List<String> bulletPoints) {
        this.bulletPoints = bulletPoints == null ? new ArrayList<>() : new ArrayList<>(bulletPoints);
    }

    public String getSpeakerNotes() {
        return speakerNotes;
    }

    public void setSpeakerNotes(String speakerNotes) {
        this.speakerNotes = speakerNotes;
    }

    public SlideType getType() {
        return type;
    }

    public void setType(SlideType type) {
        this.type = type == null ? SlideType.CONTENT : type;
    }

    public enum SlideType {
        TITLE,
        CONTENT,
        DIAGRAM,
        SUMMARY,
        CONCLUSION;

        public static SlideType fromApiValue(String value) {
            if (value == null || value.isBlank()) {
                return CONTENT;
            }
            return switch (value.trim().toLowerCase()) {
                case "title" -> TITLE;
                case "diagram" -> DIAGRAM;
                case "summary" -> SUMMARY;
                case "conclusion" -> CONCLUSION;
                default -> CONTENT;
            };
        }
    }
}