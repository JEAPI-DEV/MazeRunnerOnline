package net.simplehardware.engine.server.database;

import net.simplehardware.engine.server.database.models.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database manager for SQLite operations
 */
public class DatabaseManager {
    private final String dbPath;
    private Connection connection;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Initialize database connection and create tables
     */
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(true);

        // Enable foreign keys
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        // Create tables from schema file
        executeSchema();

        System.out.println("Database initialized: " + dbPath);
    }

    /**
     * Execute schema.sql to create tables
     */
    private void executeSchema() throws SQLException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new SQLException("schema.sql not found in resources");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sql = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sql.append(line).append(" ");
            }

            // Execute each statement
            String[] statements = sql.toString().split(";");
            try (Statement stmt = connection.createStatement()) {
                for (String statement : statements) {
                    if (!statement.trim().isEmpty()) {
                        stmt.execute(statement.trim());
                    }
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to execute schema", e);
        }
    }

    /**
     * Close database connection
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Database connection closed");
        }
    }

    // ==================== USER OPERATIONS ====================

    /**
     * Create a new user
     */
    public User createUser(String username, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();

            // SQLite doesn't support getGeneratedKeys(), use last_insert_rowid()
            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getUserById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    /**
     * Get user by ID
     */
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at"));
            }
        }
        return null;
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at"));
            }
        }
        return null;
    }

    // ==================== PLAYER BOT OPERATIONS ====================

    /**
     * Create a new player bot
     */
    public PlayerBot createPlayerBot(int userId, String botName, String jarPath) throws SQLException {
        String sql = "INSERT INTO player_bots (user_id, bot_name, jar_path) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, botName);
            pstmt.setString(3, jarPath);
            pstmt.executeUpdate();

            // SQLite doesn't support getGeneratedKeys(), use last_insert_rowid()
            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getPlayerBotById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    /**
     * Get player bot by ID
     */
    public PlayerBot getPlayerBotById(int id) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new PlayerBot(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("bot_name"),
                        rs.getString("jar_path"),
                        rs.getTimestamp("uploaded_at"));
            }
        }
        return null;
    }

    /**
     * Get all bots for a user
     */
    public List<PlayerBot> getUserBots(int userId) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE user_id = ? ORDER BY uploaded_at DESC";
        List<PlayerBot> bots = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bots.add(new PlayerBot(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("bot_name"),
                        rs.getString("jar_path"),
                        rs.getTimestamp("uploaded_at")));
            }
        }
        return bots;
    }

    /**
     * Get user's most recent bot
     */
    public PlayerBot getUserLatestBot(int userId) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE user_id = ? ORDER BY uploaded_at DESC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new PlayerBot(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("bot_name"),
                        rs.getString("jar_path"),
                        rs.getTimestamp("uploaded_at"));
            }
        }
        return null;
    }

    // ==================== MAZE OPERATIONS ====================

    /**
     * Create a new maze
     */
    public Maze createMaze(String name, String filePath, int minSteps, int forms, int size, Maze.Difficulty difficulty)
            throws SQLException {
        String sql = "INSERT INTO mazes (name, file_path, min_steps, forms, size, difficulty) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, filePath);
            pstmt.setInt(3, minSteps);
            pstmt.setInt(4, forms);
            pstmt.setInt(5, size);
            pstmt.setString(6, difficulty.name());
            pstmt.executeUpdate();

            // SQLite doesn't support getGeneratedKeys(), use last_insert_rowid()
            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getMazeById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    /**
     * Get maze by ID
     */
    public Maze getMazeById(int id) throws SQLException {
        String sql = "SELECT * FROM mazes WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Maze(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("file_path"),
                        rs.getInt("min_steps"),
                        rs.getInt("forms"),
                        rs.getInt("size"),
                        Maze.Difficulty.valueOf(rs.getString("difficulty")),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("active"));
            }
        }
        return null;
    }

    /**
     * Get all active mazes
     */
    public List<Maze> getActiveMazes() throws SQLException {
        String sql = "SELECT * FROM mazes WHERE active = 1 ORDER BY created_at DESC";
        List<Maze> mazes = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mazes.add(new Maze(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("file_path"),
                        rs.getInt("min_steps"),
                        rs.getInt("forms"),
                        rs.getInt("size"),
                        Maze.Difficulty.valueOf(rs.getString("difficulty")),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("active")));
            }
        }
        return mazes;
    }

    /**
     * Deactivate old mazes
     */
    public void deactivateOldMazes(int daysOld) throws SQLException {
        String sql = "UPDATE mazes SET active = 0 WHERE created_at < datetime('now', '-' || ? || ' days')";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, daysOld);
            int updated = pstmt.executeUpdate();
            System.out.println("Deactivated " + updated + " old mazes");
        }
    }

    // ==================== GAME RESULT OPERATIONS ====================

    /**
     * Create a new game result
     */
    public GameResult createGameResult(int userId, int botId, int mazeId, int stepsTaken,
            double scorePercentage, boolean completed, String gameDataPath) throws SQLException {
        String sql = "INSERT INTO game_results (user_id, bot_id, maze_id, steps_taken, score_percentage, completed, game_data_path) "
                +
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

            // SQLite doesn't support getGeneratedKeys(), use last_insert_rowid()
            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getGameResultById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    /**
     * Get game result by ID
     */
    public GameResult getGameResultById(int id) throws SQLException {
        String sql = "SELECT * FROM game_results WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
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
        return null;
    }

    /**
     * Get user's game history
     */
    public List<GameResult> getUserGameHistory(int userId, int limit) throws SQLException {
        String sql = "SELECT * FROM game_results WHERE user_id = ? ORDER BY played_at DESC LIMIT ?";
        List<GameResult> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(new GameResult(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("bot_id"),
                        rs.getInt("maze_id"),
                        rs.getInt("steps_taken"),
                        rs.getDouble("score_percentage"),
                        rs.getBoolean("completed"),
                        rs.getString("game_data_path"),
                        rs.getTimestamp("played_at")));
            }
        }
        return results;
    }

    // ==================== LEADERBOARD OPERATIONS ====================

    /**
     * Get global leaderboard
     */
    public List<LeaderboardEntry> getLeaderboard(int limit) throws SQLException {
        String sql = "SELECT * FROM leaderboard LIMIT ?";
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entries.add(new LeaderboardEntry(
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

    /**
     * Get leaderboard filtered by difficulty
     */
    public List<LeaderboardEntry> getLeaderboardByDifficulty(String difficulty, int limit) throws SQLException {
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
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, difficulty);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entries.add(new LeaderboardEntry(
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

    /**
     * Leaderboard entry class
     */
    public static class LeaderboardEntry {
        public final int userId;
        public final String username;
        public final int gamesPlayed;
        public final double avgScore;
        public final double worstScore;
        public final double bestScore;
        public final Timestamp lastPlayed;

        public LeaderboardEntry(int userId, String username, int gamesPlayed, double avgScore,
                double worstScore, double bestScore, Timestamp lastPlayed) {
            this.userId = userId;
            this.username = username;
            this.gamesPlayed = gamesPlayed;
            this.avgScore = avgScore;
            this.worstScore = worstScore;
            this.bestScore = bestScore;
            this.lastPlayed = lastPlayed;
        }
    }
}
