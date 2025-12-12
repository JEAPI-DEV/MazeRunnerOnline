package net.simplehardware.engine.server.database;

import net.simplehardware.engine.server.database.models.*;
import net.simplehardware.engine.server.database.repositories.*;
import net.simplehardware.engine.server.database.repositories.impl.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * Database manager for SQLite operations
 * Acts as a facade over repository implementations
 */
public class DatabaseManager {
    private final String dbPath;
    private Connection connection;
    
    // Repository instances
    private UserRepository userRepository;
    private BotRepository botRepository;
    private MazeRepository mazeRepository;
    private LobbyRepository lobbyRepository;
    private GameRepository gameRepository;
    private MetricsRepository metricsRepository;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Initialize database connection and create tables
     */
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(true);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");  // Write-Ahead Logging for better concurrency
            stmt.execute("PRAGMA synchronous = NORMAL"); // Faster writes, still safe
            stmt.execute("PRAGMA cache_size = -64000");  // 64MB cache
            stmt.execute("PRAGMA temp_store = MEMORY");  // Keep temp tables in memory
        }
        executeSchema();
        initializeRepositories();

        System.out.println("Database initialized: " + dbPath);
    }
    
    /**
     * Initialize repository instances
     */
    private void initializeRepositories() {
        this.userRepository = new UserRepositoryImpl(connection);
        this.botRepository = new BotRepositoryImpl(connection);
        this.mazeRepository = new MazeRepositoryImpl(connection);
        this.lobbyRepository = new LobbyRepositoryImpl(connection);
        this.gameRepository = new GameRepositoryImpl(connection);
        this.metricsRepository = new MetricsRepositoryImpl(connection);
    }



    public void heartbeatLobby(int lobbyId) throws SQLException {
        lobbyRepository.heartbeatLobby(lobbyId);
    }

    public void cleanupInactiveLobbies() throws SQLException {
        lobbyRepository.cleanupInactiveLobbies();
    }
    
    public String getMazeName(int mazeId) throws SQLException {
        return mazeRepository.getMazeName(mazeId);
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
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sql.append(line).append(" ");
            }

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
        return userRepository.createUser(username, passwordHash);
    }

    /**
     * Get user by ID
     */
    public User getUserById(int id) throws SQLException {
        return userRepository.getUserById(id);
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) throws SQLException {
        return userRepository.getUserByUsername(username);
    }

    /**
     * Search users by username (partial match)
     */
    public List<User> searchUsersByUsername(String query) throws SQLException {
        return userRepository.searchUsersByUsername(query);
    }

    // ==================== PLAYER BOT OPERATIONS ====================

    /**
     * Create a new player bot
     */
    public PlayerBot createPlayerBot(int userId, String botName, String jarPath) throws SQLException {
        return botRepository.createPlayerBot(userId, botName, jarPath);
    }

    /**
     * Get player bot by ID
     */
    public PlayerBot getPlayerBotById(int id) throws SQLException {
        return botRepository.getPlayerBotById(id);
    }

    /**
     * Get all bots for a user
     */
    public List<PlayerBot> getUserBots(int userId) throws SQLException {
        return botRepository.getUserBots(userId);
    }

    /**
     * Get user's most recent bot
     */
    public PlayerBot getUserLatestBot(int userId) throws SQLException {
        return botRepository.getUserLatestBot(userId);
    }

    /**
     * Set a bot as default for a user
     */
    public void setUserDefaultBot(int userId, int botId) throws SQLException {
        botRepository.setUserDefaultBot(userId, botId);
    }

    public PlayerBot getUserDefaultBot(int userId) throws SQLException {
        return botRepository.getUserDefaultBot(userId);
    }

    public String deletePlayerBot(int userId, int botId) throws SQLException {
        return botRepository.deletePlayerBot(userId, botId);
    }

    public boolean checkBotNameExists(int userId, String botName) throws SQLException {
        return botRepository.checkBotNameExists(userId, botName);
    }

    // ==================== MAZE OPERATIONS ====================

    /**
     * Create a new maze
     */
    public Maze createMaze(String name, String filePath, int minSteps, int forms, int size, Maze.Difficulty difficulty)
            throws SQLException {
        return mazeRepository.createMaze(name, filePath, minSteps, forms, size, difficulty);
    }

    /**
     * Get maze by ID
     */
    public Maze getMazeById(int id) throws SQLException {
        return mazeRepository.getMazeById(id);
    }

    /**
     * Get all mazes
     */
    public List<Maze> getAllMazes() throws SQLException {
        return mazeRepository.getAllMazes();
    }

    /**
     * Get all active mazes
     */
    public List<Maze> getActiveMazes() throws SQLException {
        return mazeRepository.getActiveMazes();
    }

    /**
     * Get mazes that a user has not played yet
     */
    public List<Maze> getUnplayedMazes(int userId, String difficulty) throws SQLException {
        return mazeRepository.getUnplayedMazes(userId, difficulty);
    }

    // ==================== GAME RESULT OPERATIONS ====================

    /**
     * Create a new game result
     */
    public GameResult createGameResult(int userId, int botId, int mazeId, int stepsTaken,
            double scorePercentage, boolean completed, String gameDataPath) throws SQLException {
        return gameRepository.createGameResult(userId, botId, mazeId, stepsTaken, scorePercentage, completed, gameDataPath);
    }

    /**
     * Get game result by ID
     */
    public GameResult getGameResultById(int id) throws SQLException {
        return gameRepository.getGameResultById(id);
    }

    /**
     * Get user's game history
     */
    public List<GameResult> getUserGameHistory(int userId, int limit, String type) throws SQLException {
        return gameRepository.getUserGameHistory(userId, limit, type);
    }

    /**
     * Get user's game history by difficulty
     */
    public List<GameResult> getUserGameHistoryByDifficulty(int userId, String difficulty) throws SQLException {
        return gameRepository.getUserGameHistoryByDifficulty(userId, difficulty);
    }

    /**
     * Get the best game result for a user on a specific maze
     */
    public GameResult getBestScoreForMaze(int userId, int mazeId) throws SQLException {
        return gameRepository.getBestScoreForMaze(userId, mazeId);
    }

    public String deleteGameResult(int gameResultId) throws SQLException {
        return gameRepository.deleteGameResult(gameResultId);
    }

    // ==================== LOBBY OPERATIONS ====================

    public Lobby createLobby(int hostUserId, String name, int mazeId, int maxPlayers) throws SQLException {
        return lobbyRepository.createLobby(hostUserId, name, mazeId, maxPlayers);
    }

    public Lobby getLobby(int lobbyId) throws SQLException {
        return lobbyRepository.getLobby(lobbyId);
    }

    public List<Lobby> getActiveLobbies() throws SQLException {
        return lobbyRepository.getActiveLobbies();
    }

    public boolean joinLobby(int lobbyId, int userId, int botId) throws SQLException {
        return lobbyRepository.joinLobby(lobbyId, userId, botId);
    }

    public boolean leaveLobby(int lobbyId, int userId) throws SQLException {
        return lobbyRepository.leaveLobby(lobbyId, userId);
    }

    public List<LobbyPlayer> getLobbyPlayers(int lobbyId) throws SQLException {
        return lobbyRepository.getLobbyPlayers(lobbyId);
    }

    public void updateLobbyStatus(int lobbyId, String status) throws SQLException {
        lobbyRepository.updateLobbyStatus(lobbyId, status);
    }

    public void deleteLobby(int lobbyId) throws SQLException {
        lobbyRepository.deleteLobby(lobbyId);
    }

    public void updateLobbyLastGameId(int lobbyId, int gameId) throws SQLException {
        lobbyRepository.updateLobbyLastGameId(lobbyId, gameId);
    }

    // ==================== LEADERBOARD OPERATIONS ====================

    public List<LeaderboardEntry> getLeaderboard(int limit) throws SQLException {
        return metricsRepository.getLeaderboard(limit);
    }

    /**
     * Get leaderboard filtered by difficulty
     */
    public List<LeaderboardEntry> getLeaderboardByDifficulty(String difficulty, int limit) throws SQLException {
        return metricsRepository.getLeaderboardByDifficulty(difficulty, limit);
    }

    /**
     * Leaderboard entry class
     */
    public record LeaderboardEntry(int userId, String username, int gamesPlayed, double avgScore, double worstScore,
                                       double bestScore, Timestamp lastPlayed) {
    }

    // ==================== ADMIN METRICS OPERATIONS ====================

    public void recordMetric(String metricType, double value, String metadata) throws SQLException {
        metricsRepository.recordMetric(metricType, value, metadata);
    }

    public List<Map<String, Object>> getMetricsHistory(String metricType, int hours) throws SQLException {
        return metricsRepository.getMetricsHistory(metricType, hours);
    }

    public Map<String, Object> getGameStatistics(String period) throws SQLException {
        return metricsRepository.getGameStatistics(period);
    }

    public Map<String, Object> getUserRegistrationStats(String period) throws SQLException {
        return metricsRepository.getUserRegistrationStats(period);
    }

    public Map<String, Object> getDatabaseStats() throws SQLException {
        return metricsRepository.getDatabaseStats();
    }

    public List<Map<String, Object>> getAverageWaitTimesByDifficulty() throws SQLException {
        return metricsRepository.getAverageWaitTimesByDifficulty();
    }

    public void cleanOldMetrics(int daysToKeep) throws SQLException {
        metricsRepository.cleanOldMetrics(daysToKeep);
    }

    public void vacuumDatabase() throws SQLException {
        metricsRepository.vacuumDatabase();
    }
}
