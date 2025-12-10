package net.simplehardware.engine.server.database.repositories.impl;

import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.repositories.MetricsRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of MetricsRepository for SQLite
 */
public class MetricsRepositoryImpl implements MetricsRepository {
    private final Connection connection;

    public MetricsRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void recordMetric(String metricType, double value, String metadata) throws SQLException {
        String sql = "INSERT INTO admin_metrics (metric_type, metric_value, metadata) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, metricType);
            pstmt.setDouble(2, value);
            pstmt.setString(3, metadata);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Map<String, Object>> getMetricsHistory(String metricType, int hours) throws SQLException {
        String sql = "SELECT timestamp, metric_value, metadata FROM admin_metrics " +
                "WHERE metric_type = ? AND timestamp > datetime('now', '-' || ? || ' hours') " +
                "ORDER BY timestamp DESC";
        List<Map<String, Object>> metrics = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, metricType);
            pstmt.setInt(2, hours);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> metric = new HashMap<>();
                metric.put("timestamp", rs.getTimestamp("timestamp"));
                metric.put("value", rs.getDouble("metric_value"));
                metric.put("metadata", rs.getString("metadata"));
                metrics.add(metric);
            }
        }
        return metrics;
    }

    @Override
    public Map<String, Object> getGameStatistics(String period) throws SQLException {
        String timeFilter = switch (period) {
            case "daily" -> "datetime('now', '-1 day')";
            case "weekly" -> "datetime('now', '-7 days')";
            case "monthly" -> "datetime('now', '-30 days')";
            default -> "datetime('now', '-1 day')";
        };

        Map<String, Object> stats = new HashMap<>();

        String sql = "SELECT COUNT(*) as total_games, " +
                "SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completed_games, " +
                "AVG(score_percentage) as avg_score, " +
                "AVG(steps_taken) as avg_steps " +
                "FROM game_results WHERE played_at > " + timeFilter;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("total_games", rs.getInt("total_games"));
                stats.put("completed_games", rs.getInt("completed_games"));
                stats.put("avg_score", rs.getDouble("avg_score"));
                stats.put("avg_steps", rs.getDouble("avg_steps"));
            }
        }

        String difficultySql = "SELECT m.difficulty, COUNT(*) as count, AVG(gr.score_percentage) as avg_score " +
                "FROM game_results gr JOIN mazes m ON gr.maze_id = m.id " +
                "WHERE gr.played_at > " + timeFilter + " GROUP BY m.difficulty";

        Map<String, Map<String, Object>> byDifficulty = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(difficultySql)) {
            while (rs.next()) {
                Map<String, Object> diffStats = new HashMap<>();
                diffStats.put("count", rs.getInt("count"));
                diffStats.put("avg_score", rs.getDouble("avg_score"));
                byDifficulty.put(rs.getString("difficulty"), diffStats);
            }
        }
        stats.put("by_difficulty", byDifficulty);

        return stats;
    }

    @Override
    public Map<String, Object> getUserRegistrationStats(String period) throws SQLException {
        String timeFilter = switch (period) {
            case "daily" -> "datetime('now', '-1 day')";
            case "weekly" -> "datetime('now', '-7 days')";
            case "monthly" -> "datetime('now', '-30 days')";
            default -> "datetime('now', '-7 days')";
        };

        Map<String, Object> stats = new HashMap<>();

        String sql = "SELECT COUNT(*) as new_users FROM users WHERE created_at > " + timeFilter;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("new_users", rs.getInt("new_users"));
            }
        }

        String totalSql = "SELECT COUNT(*) as total_users FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(totalSql)) {
            if (rs.next()) {
                stats.put("total_users", rs.getInt("total_users"));
            }
        }

        return stats;
    }

    @Override
    public Map<String, Object> getDatabaseStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        String[] tables = {"users", "player_bots", "mazes", "game_results", "lobbies", "lobby_players", "admin_metrics"};
        for (String table : tables) {
            String sql = "SELECT COUNT(*) as count FROM " + table;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    stats.put(table + "_count", rs.getInt("count"));
                }
            }
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA page_count")) {
            if (rs.next()) {
                int pageCount = rs.getInt(1);
                stats.put("page_count", pageCount);
            }
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA page_size")) {
            if (rs.next()) {
                int pageSize = rs.getInt(1);
                stats.put("page_size", pageSize);
                int pageCount = (int) stats.getOrDefault("page_count", 0);
                stats.put("db_size_bytes", (long) pageCount * pageSize);
            }
        }

        return stats;
    }

    @Override
    public List<Map<String, Object>> getAverageWaitTimesByDifficulty() throws SQLException {
        String sql = "SELECT m.difficulty, " +
                "AVG(CAST((julianday(gr.played_at) - julianday(lp.joined_at)) * 86400 AS INTEGER)) as avg_wait_seconds, " +
                "COUNT(*) as game_count " +
                "FROM lobby_players lp " +
                "JOIN lobbies l ON lp.lobby_id = l.id " +
                "JOIN mazes m ON l.maze_id = m.id " +
                "JOIN game_results gr ON l.last_game_id = gr.id " +
                "WHERE l.status = 'FINISHED' AND gr.played_at > datetime('now', '-7 days') " +
                "GROUP BY m.difficulty";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("difficulty", rs.getString("difficulty"));
                row.put("avg_wait_seconds", rs.getDouble("avg_wait_seconds"));
                row.put("game_count", rs.getInt("game_count"));
                results.add(row);
            }
        }
        return results;
    }

    @Override
    public List<DatabaseManager.LeaderboardEntry> getLeaderboard(int limit) throws SQLException {
        String sql = "SELECT * FROM leaderboard LIMIT ?";
        List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entries.add(new DatabaseManager.LeaderboardEntry(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getInt("games_played"),
                        rs.getDouble("avg_score"),
                        rs.getDouble("worst_score"),
                        rs.getDouble("best_score"),
                        rs.getTimestamp("last_played")));
            }
        }
        return entries;
    }

    @Override
    public List<DatabaseManager.LeaderboardEntry> getLeaderboardByDifficulty(String difficulty, int limit) throws SQLException {
        String sql = "SELECT u.id as user_id, u.username, " +
                "COUNT(gr.id) as games_played, " +
                "AVG(gr.score_percentage) as avg_score, " +
                "MIN(gr.score_percentage) as worst_score, " +
                "MAX(gr.score_percentage) as best_score, " +
                "MAX(gr.played_at) as last_played " +
                "FROM users u " +
                "JOIN game_results gr ON u.id = gr.user_id " +
                "JOIN mazes m ON gr.maze_id = m.id " +
                "WHERE m.difficulty = ? " +
                "GROUP BY u.id, u.username " +
                "ORDER BY avg_score DESC " +
                "LIMIT ?";
        List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, difficulty);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entries.add(new DatabaseManager.LeaderboardEntry(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getInt("games_played"),
                        rs.getDouble("avg_score"),
                        rs.getDouble("worst_score"),
                        rs.getDouble("best_score"),
                        rs.getTimestamp("last_played")));
            }
        }
        return entries;
    }

    @Override
    public void cleanOldMetrics(int daysToKeep) throws SQLException {
        String sql = "DELETE FROM admin_metrics WHERE timestamp < datetime('now', '-' || ? || ' days')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, daysToKeep);
            int deleted = pstmt.executeUpdate();
            System.out.println("Cleaned up " + deleted + " old metrics records");
        }
    }

    @Override
    public void vacuumDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("VACUUM");
            System.out.println("Database vacuumed successfully");
        }
    }
}
