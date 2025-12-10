package net.simplehardware.engine.server.database.repositories.impl;

import net.simplehardware.engine.server.database.models.Lobby;
import net.simplehardware.engine.server.database.models.LobbyPlayer;
import net.simplehardware.engine.server.database.repositories.LobbyRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of LobbyRepository for SQLite
 */
public class LobbyRepositoryImpl implements LobbyRepository {
    private final Connection connection;

    public LobbyRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Lobby createLobby(int hostUserId, String name, int mazeId, int maxPlayers) throws SQLException {
        String sql = "INSERT INTO lobbies (name, host_user_id, maze_id, max_players, status) VALUES (?, ?, ?, ?, 'WAITING')";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, hostUserId);
            pstmt.setInt(3, mazeId);
            pstmt.setInt(4, maxPlayers);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        int lobbyId = rs.getInt(1);
                        return getLobby(lobbyId);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Lobby getLobby(int lobbyId) throws SQLException {
        String sql = "SELECT * FROM lobbies WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Lobby(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("host_user_id"),
                        rs.getInt("maze_id"),
                        rs.getInt("max_players"),
                        rs.getString("status"),
                        (Integer) rs.getObject("last_game_id"),
                        rs.getTimestamp("created_at"));
            }
        }
        return null;
    }

    @Override
    public List<Lobby> getActiveLobbies() throws SQLException {
        String sql = "SELECT * FROM lobbies WHERE status = 'WAITING' ORDER BY created_at DESC";
        List<Lobby> lobbies = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                lobbies.add(new Lobby(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("host_user_id"),
                        rs.getInt("maze_id"),
                        rs.getInt("max_players"),
                        rs.getString("status"),
                        (Integer) rs.getObject("last_game_id"),
                        rs.getTimestamp("created_at")));
            }
        }
        return lobbies;
    }

    @Override
    public boolean joinLobby(int lobbyId, int userId, int botId) throws SQLException {
        // Check if lobby exists and is waiting
        Lobby lobby = getLobby(lobbyId);
        if (lobby == null || !"WAITING".equals(lobby.getStatus())) {
            return false;
        }

        // Check if already joined
        String checkSql = "SELECT 1 FROM lobby_players WHERE lobby_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.setInt(2, userId);
            if (pstmt.executeQuery().next()) {
                // Already joined
                return true;
            }
        }

        // Check player count
        String countSql = "SELECT COUNT(*) FROM lobby_players WHERE lobby_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(countSql)) {
            pstmt.setInt(1, lobbyId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) >= lobby.getMaxPlayers()) {
                return false;
            }
        }

        String sql = "INSERT INTO lobby_players (lobby_id, user_id, bot_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, botId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean leaveLobby(int lobbyId, int userId) throws SQLException {
        String sql = "DELETE FROM lobby_players WHERE lobby_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<LobbyPlayer> getLobbyPlayers(int lobbyId) throws SQLException {
        String sql = "SELECT * FROM lobby_players WHERE lobby_id = ? ORDER BY joined_at";
        List<LobbyPlayer> players = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                players.add(new LobbyPlayer(
                        rs.getInt("lobby_id"),
                        rs.getInt("user_id"),
                        rs.getInt("bot_id"),
                        rs.getTimestamp("joined_at")));
            }
        }
        return players;
    }

    @Override
    public void updateLobbyStatus(int lobbyId, String status) throws SQLException {
        String sql = "UPDATE lobbies SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, lobbyId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteLobby(int lobbyId) throws SQLException {
        String sql = "DELETE FROM lobbies WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateLobbyLastGameId(int lobbyId, int gameId) throws SQLException {
        String sql = "UPDATE lobbies SET last_game_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, lobbyId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void heartbeatLobby(int lobbyId) throws SQLException {
        String sql = "UPDATE lobbies SET last_heartbeat = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void cleanupInactiveLobbies() throws SQLException {
        String sql = "DELETE FROM lobbies WHERE last_heartbeat < datetime('now', '-30 seconds')";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }
}
