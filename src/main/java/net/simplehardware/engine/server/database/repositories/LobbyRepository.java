package net.simplehardware.engine.server.database.repositories;

import net.simplehardware.engine.server.database.models.Lobby;
import net.simplehardware.engine.server.database.models.LobbyPlayer;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for Lobby operations
 */
public interface LobbyRepository {
    /**
     * Create a new lobby
     */
    Lobby createLobby(int hostUserId, String name, int mazeId, int maxPlayers) throws SQLException;

    /**
     * Get lobby by ID
     */
    Lobby getLobby(int lobbyId) throws SQLException;

    /**
     * Get active lobbies
     */
    List<Lobby> getActiveLobbies() throws SQLException;

    /**
     * Join a lobby
     */
    boolean joinLobby(int lobbyId, int userId, int botId) throws SQLException;

    /**
     * Leave a lobby
     */
    boolean leaveLobby(int lobbyId, int userId) throws SQLException;

    /**
     * Get lobby players
     */
    List<LobbyPlayer> getLobbyPlayers(int lobbyId) throws SQLException;

    /**
     * Update lobby status
     */
    void updateLobbyStatus(int lobbyId, String status) throws SQLException;

    /**
     * Delete a lobby
     */
    void deleteLobby(int lobbyId) throws SQLException;

    /**
     * Update lobby last game ID
     */
    void updateLobbyLastGameId(int lobbyId, int gameId) throws SQLException;

    /**
     * Update lobby heartbeat
     */
    void heartbeatLobby(int lobbyId) throws SQLException;

    /**
     * Cleanup inactive lobbies
     */
    void cleanupInactiveLobbies() throws SQLException;
}
