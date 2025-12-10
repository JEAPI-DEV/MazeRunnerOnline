package net.simplehardware.engine.server.database.repositories.impl;

import net.simplehardware.engine.server.database.models.GameResult;
import net.simplehardware.engine.server.database.repositories.GameRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GameRepository for SQLite
 */
public class GameRepositoryImpl implements GameRepository {
    private final Connection connection;

    public GameRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public GameResult createGameResult(int userId, int botId, int mazeId, int stepsTaken,
                                        double scorePercentage, boolean completed, String gameDataPath) throws SQLException {
        String sql = "INSERT INTO game_results (user_id, bot_id, maze_id, steps_taken, score_percentage, completed, game_data_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, botId);
            pstmt.setInt(3, mazeId);
            pstmt.setInt(4, stepsTaken);
            pstmt.setDouble(5, scorePercentage);
            pstmt.setBoolean(6, completed);
            pstmt.setString(7, gameDataPath);
            pstmt.executeUpdate();

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getGameResultById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    @Override
    public GameResult getGameResultById(int id) throws SQLException {
        String sql = "SELECT * FROM game_results WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToGameResult(rs);
            }
        }
        return null;
    }

    @Override
    public List<GameResult> getUserGameHistory(int userId, int limit, String type) throws SQLException {
        String sql = "SELECT * FROM game_results WHERE user_id = ? ";

        if ("MULTIPLAYER".equals(type)) {
            sql += "AND game_data_path LIKE '%_lobby%' ";
        } else {
            sql += "AND game_data_path NOT LIKE '%_lobby%' ";
        }

        sql += "ORDER BY played_at DESC LIMIT ?";

        List<GameResult> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToGameResult(rs));
            }
        }
        return results;
    }

    @Override
    public List<GameResult> getUserGameHistoryByDifficulty(int userId, String difficulty) throws SQLException {
        String sql = "SELECT gr.* FROM game_results gr " +
                "JOIN mazes m ON gr.maze_id = m.id " +
                "WHERE gr.user_id = ? AND m.difficulty = ? " +
                "AND gr.game_data_path NOT LIKE '%_lobby%' " +
                "ORDER BY gr.score_percentage DESC, gr.steps_taken ASC";

        List<GameResult> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, difficulty);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToGameResult(rs));
            }
        }
        return results;
    }

    @Override
    public GameResult getBestScoreForMaze(int userId, int mazeId) throws SQLException {
        String sql = "SELECT * FROM game_results " +
                "WHERE user_id = ? AND maze_id = ? " +
                "ORDER BY score_percentage DESC, steps_taken ASC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, mazeId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToGameResult(rs);
            }
        }
        return null;
    }

    @Override
    public String deleteGameResult(int gameResultId) throws SQLException {
        GameResult result = getGameResultById(gameResultId);
        if (result == null) {
            return null;
        }

        String sql = "DELETE FROM game_results WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, gameResultId);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                return result.getGameDataPath();
            }
        }
        return null;
    }

    /**
     * Helper method to map ResultSet to GameResult
     */
    private GameResult mapResultSetToGameResult(ResultSet rs) throws SQLException {
        return new GameResult(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("bot_id"),
                rs.getInt("maze_id"),
                rs.getInt("steps_taken"),
                rs.getDouble("score_percentage"),
                rs.getBoolean("completed"),
                rs.getString("game_data_path"),
                rs.getTimestamp("played_at"));
    }
}
