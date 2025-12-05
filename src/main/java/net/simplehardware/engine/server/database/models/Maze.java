package net.simplehardware.engine.server.database.models;

import java.sql.Timestamp;

/**
 * Model class representing a maze
 */
public class Maze {
    private int id;
    private String name;
    private String filePath;
    private int minSteps;
    private int forms;
    private int size;
    private Difficulty difficulty;
    private Timestamp createdAt;
    private boolean active;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public Maze() {
    }

    public Maze(int id, String name, String filePath, int minSteps, int forms, int size,
            Difficulty difficulty, Timestamp createdAt, boolean active) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.minSteps = minSteps;
        this.forms = forms;
        this.size = size;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.active = active;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getMinSteps() {
        return minSteps;
    }

    public void setMinSteps(int minSteps) {
        this.minSteps = minSteps;
    }

    public int getForms() {
        return forms;
    }

    public void setForms(int forms) {
        this.forms = forms;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Maze{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", minSteps=" + minSteps +
                ", forms=" + forms +
                ", size=" + size +
                ", difficulty=" + difficulty +
                ", active=" + active +
                '}';
    }
}
