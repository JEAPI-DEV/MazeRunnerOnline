package net.simplehardware.engine.server.database.models;

import java.sql.Timestamp;

public class Lobby {
    private int id;
    private String name;
    private int hostUserId;
    private int mazeId;
    private int maxPlayers;
    private String status;
    private Integer lastGameId;
    private Timestamp createdAt;

    public Lobby() {
    }

    public Lobby(int id, String name, int hostUserId, int mazeId, int maxPlayers, String status, Integer lastGameId,
            Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.hostUserId = hostUserId;
        this.mazeId = mazeId;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.lastGameId = lastGameId;
        this.createdAt = createdAt;
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

    public int getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(int hostUserId) {
        this.hostUserId = hostUserId;
    }

    public int getMazeId() {
        return mazeId;
    }

    public void setMazeId(int mazeId) {
        this.mazeId = mazeId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLastGameId() {
        return lastGameId;
    }

    public void setLastGameId(Integer lastGameId) {
        this.lastGameId = lastGameId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
