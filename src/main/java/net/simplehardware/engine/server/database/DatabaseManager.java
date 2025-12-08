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

        // Enable foreign keys and performance optimizations
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");  // Write-Ahead Logging for better concurrency
            stmt.execute("PRAGMA synchronous = NORMAL"); // Faster writes, still safe
            stmt.execute("PRAGMA cache_size = -64000");  // 64MB cache
            stmt.execute("PRAGMA temp_store = MEMORY");  // Keep temp tables in memory
        }

        // Create tables from schema file
        executeSchema();

        System.out.println("Database initialized: " + dbPath);

        // Run migrations
        runMigrations();
    }

    private void runMigrations() throws SQLException {
        // Check if is_default column exists in player_bots
        boolean hasIsDefault = false;
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "player_bots", "is_default")) {
            if (rs.next()) {
                hasIsDefault = true;
            }
        }

        if (!hasIsDefault) {
            System.out.println("Migrating: Adding is_default column to player_bots");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE player_bots ADD COLUMN is_default BOOLEAN DEFAULT 0");
            }
        }

        // Check if last_heartbeat column exists in lobbies
        boolean hasLastHeartbeat = false;
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "lobbies", "last_heartbeat")) {
            if (rs.next()) {
                hasLastHeartbeat = true;
            }
        }

        if (!hasLastHeartbeat) {
            System.out.println("Migrating: Adding last_heartbeat column to lobbies");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE lobbies ADD COLUMN last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
        }
    }

    public void heartbeatLobby(int lobbyId) throws SQLException {
        String sql = "UPDATE lobbies SET last_heartbeat = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.executeUpdate();
        }
    }

    public void cleanupInactiveLobbies() throws SQLException {
        // Delete lobbies inactive for more than 30 seconds
        // SQLite syntax for timestamp comparison
        String sql = "DELETE FROM lobbies WHERE last_heartbeat < datetime('now', '-30 seconds') AND status != 'FINISHED'";
        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("Cleaned up " + deleted + " inactive lobbies");
            }
        }
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



    public String getMazeName(int mazeId) throws SQLException {
        String sql = "SELECT name FROM mazes WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, mazeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        }
        return "Unknown Maze";
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

    /**
     * Search users by username (partial match)
     */
    public List<User> searchUsersByUsername(String query) throws SQLException {
        String sql = "SELECT * FROM users WHERE username LIKE ? ORDER BY username LIMIT 50";
        List<User> users = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at")));
            }
        }
        return users;
    }

    // ==================== PLAYER BOT OPERATIONS ====================

    /**
     * Create a new player bot
     */
    public PlayerBot createPlayerBot(int userId, String botName, String jarPath) throws SQLException {
        // Check if this is the first bot for the user, if so make it default
        boolean isFirstBot = getUserBots(userId).isEmpty();

        String sql = "INSERT INTO player_bots (user_id, bot_name, jar_path, is_default) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, botName);
            pstmt.setString(3, jarPath);
            pstmt.setBoolean(4, isFirstBot);
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
                        rs.getTimestamp("uploaded_at"),
                        rs.getBoolean("is_default"));
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
                        rs.getTimestamp("uploaded_at"),
                        rs.getBoolean("is_default")));
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

    /**
     * Set a bot as default for a user
     */
    public void setUserDefaultBot(int userId, int botId) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            String resetSql = "UPDATE player_bots SET is_default = 0 WHERE user_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(resetSql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }

            String setSql = "UPDATE player_bots SET is_default = 1 WHERE id = ? AND user_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(setSql)) {
                pstmt.setInt(1, botId);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    public PlayerBot getUserDefaultBot(int userId) throws SQLException {
        String sql = "SELECT * FROM player_bots WHERE user_id = ? AND is_default = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new PlayerBot(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("bot_name"),
                        rs.getString("jar_path"),
                        rs.getTimestamp("uploaded_at"),
                        rs.getBoolean("is_default"));
            }
        }
        return null;
    }

    public String deletePlayerBot(int userId, int botId) throws SQLException {
        PlayerBot bot = getPlayerBotById(botId);
        if (bot == null || bot.getUserId() != userId) {
            return null;
        }

        boolean wasDefault = bot.isDefault();

        String sql = "DELETE FROM player_bots WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, botId);
            pstmt.setInt(2, userId);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                // If we deleted the default bot, try to make another one default
                if (wasDefault) {
                    PlayerBot latest = getUserLatestBot(userId);
                    if (latest != null) {
                        setUserDefaultBot(userId, latest.getId());
                    }
                }
                return bot.getJarPath();
            }
        }
        return null;
    }

    public boolean checkBotNameExists(int userId, String botName) throws SQLException {
        String sql = "SELECT 1 FROM player_bots WHERE user_id = ? AND bot_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, botName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
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
     * Get all mazes
     */
    public List<Maze> getAllMazes() throws SQLException {
        String sql = "SELECT * FROM mazes ORDER BY created_at DESC";
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
     * Get mazes that a user has not played yet
     */
    public List<Maze> getUnplayedMazes(int userId, String difficulty) throws SQLException {
        String sql = "SELECT m.* FROM mazes m " +
                "LEFT JOIN game_results gr ON m.id = gr.maze_id AND gr.user_id = ? " +
                "WHERE m.active = 1 AND gr.id IS NULL";

        if (difficulty != null) {
            sql += " AND m.difficulty = ?";
        }

        List<Maze> mazes = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            if (difficulty != null) {
                pstmt.setString(2, difficulty);
            }
            ResultSet rs = pstmt.executeQuery();

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

    /**
     * Get user's game history by difficulty
     */
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

    /**
     * Get the best game result for a user on a specific maze
     */
    public GameResult getBestScoreForMaze(int userId, int mazeId) throws SQLException {
        String sql = "SELECT * FROM game_results " +
                "WHERE user_id = ? AND maze_id = ? " +
                "ORDER BY score_percentage DESC, steps_taken ASC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, mazeId);
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
                // Already joined, just update bot if needed or return true
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

    public boolean leaveLobby(int lobbyId, int userId) throws SQLException {
        String sql = "DELETE FROM lobby_players WHERE lobby_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

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

    public void updateLobbyStatus(int lobbyId, String status) throws SQLException {
        String sql = "UPDATE lobbies SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, lobbyId);
            pstmt.executeUpdate();
        }
    }

    public void deleteLobby(int lobbyId) throws SQLException {
        String sql = "DELETE FROM lobbies WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, lobbyId);
            pstmt.executeUpdate();
        }
    }

    public void updateLobbyLastGameId(int lobbyId, int gameId) throws SQLException {
        String sql = "UPDATE lobbies SET last_game_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, lobbyId);
            pstmt.executeUpdate();
        }
    }

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
    public record LeaderboardEntry(int userId, String username, int gamesPlayed, double avgScore, double worstScore,
                                       double bestScore, Timestamp lastPlayed) {
    }
}
