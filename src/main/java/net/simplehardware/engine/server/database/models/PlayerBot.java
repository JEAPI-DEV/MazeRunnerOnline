package net.simplehardware.engine.server.database.models;

import java.sql.Timestamp;

/**
 * Model class representing a player bot
 */
public class PlayerBot {
    private int id;
    private int userId;
    private String botName;
    private String jarPath;
    private Timestamp uploadedAt;
    private boolean isDefault;

    public PlayerBot() {
    }

    public PlayerBot(int id, int userId, String botName, String jarPath, Timestamp uploadedAt, boolean isDefault) {
        this.id = id;
        this.userId = userId;
        this.botName = botName;
        this.jarPath = jarPath;
        this.uploadedAt = uploadedAt;
        this.isDefault = isDefault;
    }

    public PlayerBot(int id, int userId, String botName, String jarPath, Timestamp uploadedAt) {
        this(id, userId, botName, jarPath, uploadedAt, false);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return "PlayerBot{" +
                "id=" + id +
                ", userId=" + userId +
                ", botName='" + botName + '\'' +
                ", jarPath='" + jarPath + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", isDefault=" + isDefault +
                '}';
    }
}
