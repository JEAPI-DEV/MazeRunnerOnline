package net.simplehardware.engine.server.database.models;

import java.sql.Timestamp;

public class LobbyPlayer {
    private int lobbyId;
    private int userId;
    private int botId;
    private Timestamp joinedAt;

    public LobbyPlayer() {
    }

    public LobbyPlayer(int lobbyId, int userId, int botId, Timestamp joinedAt) {
        this.lobbyId = lobbyId;
        this.userId = userId;
        this.botId = botId;
        this.joinedAt = joinedAt;
    }

    public int getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(int lobbyId) {
        this.lobbyId = lobbyId;
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

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }
}
