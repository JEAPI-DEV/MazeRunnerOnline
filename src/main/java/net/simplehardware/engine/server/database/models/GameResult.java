package net.simplehardware.engine.server.database.models;

import java.sql.Timestamp;

/**
 * Model class representing a game result
 */
public class GameResult {
    private int id;
    private int userId;
    private int botId;
    private int mazeId;
    private int stepsTaken;
    private double scorePercentage;
    private boolean completed;
    private String gameDataPath;
    private Timestamp playedAt;

    public GameResult() {
    }

    public GameResult(int id, int userId, int botId, int mazeId, int stepsTaken,
            double scorePercentage, boolean completed, String gameDataPath, Timestamp playedAt) {
        this.id = id;
        this.userId = userId;
        this.botId = botId;
        this.mazeId = mazeId;
        this.stepsTaken = stepsTaken;
        this.scorePercentage = scorePercentage;
        this.completed = completed;
        this.gameDataPath = gameDataPath;
        this.playedAt = playedAt;
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

    public int getBotId() {
        return botId;
    }

    public void setBotId(int botId) {
        this.botId = botId;
    }

    public int getMazeId() {
        return mazeId;
    }

    public void setMazeId(int mazeId) {
        this.mazeId = mazeId;
    }

    public int getStepsTaken() {
        return stepsTaken;
    }

    public void setStepsTaken(int stepsTaken) {
        this.stepsTaken = stepsTaken;
    }

    public double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getGameDataPath() {
        return gameDataPath;
    }

    public void setGameDataPath(String gameDataPath) {
        this.gameDataPath = gameDataPath;
    }

    public Timestamp getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Timestamp playedAt) {
        this.playedAt = playedAt;
    }

    @Override
    public String toString() {
        return "GameResult{" +
                "id=" + id +
                ", userId=" + userId +
                ", botId=" + botId +
                ", mazeId=" + mazeId +
                ", stepsTaken=" + stepsTaken +
                ", scorePercentage=" + scorePercentage +
                ", completed=" + completed +
                ", playedAt=" + playedAt +
                '}';
    }
}
