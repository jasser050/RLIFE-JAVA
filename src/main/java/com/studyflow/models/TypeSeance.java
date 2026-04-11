package com.studyflow.models;

public class TypeSeance {
    private int id;
    private String name;
    private Integer userId;

    public TypeSeance() {
    }

    public TypeSeance(int id, String name, Integer userId) {
        this.id = id;
        this.name = name;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return name;
    }
}
