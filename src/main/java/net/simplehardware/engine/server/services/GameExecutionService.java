package net.simplehardware.engine.server.services;

import net.simplehardware.engine.core.GameEngine;
import net.simplehardware.engine.game.Maze;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.GameResult;
import net.simplehardware.engine.server.database.models.PlayerBot;
import net.simplehardware.engine.viewer.WebViewerExporter;
import net.simplehardware.engine.viewer.elements.GameState;
import net.simplehardware.models.MazeInfoData;
import com.google.gson.Gson;

import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Service for executing games and calculating scores
 */
public class GameExecutionService {
    private final DatabaseManager db;
    private final ExecutorService executor;
    private final String gameDataDirectory;

    private static final int MAX_CONCURRENT_GAMES = 5;
    private static final int GAME_TIMEOUT_SECONDS = 60;

    public GameExecutionService(DatabaseManager db, String gameDataDirectory) {
        this.db = db;
        this.gameDataDirectory = gameDataDirectory;
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_GAMES);

        // Create game data directory
        new java.io.File(gameDataDirectory).mkdirs();
    }

    /**
     * Execute a game for a user
     * 
     * @param userId User ID
     * @param botId  Bot ID
     * @param mazeId Maze ID
     * @return GameResult
     */
    public GameResult executeGame(int userId, int botId, int mazeId) throws Exception {
        // Get bot and maze from database
        PlayerBot bot = db.getPlayerBotById(botId);
        net.simplehardware.engine.server.database.models.Maze mazeModel = db.getMazeById(mazeId);

        if (bot == null) {
            throw new IllegalArgumentException("Bot not found: " + botId);
        }
        if (mazeModel == null) {
            throw new IllegalArgumentException("Maze not found: " + mazeId);
        }
        if (bot.getUserId() != userId) {
            throw new SecurityException("Bot does not belong to user");
        }

        System.out.println("Executing game for user " + userId + " with bot " + bot.getBotName() + " on maze "
                + mazeModel.getName());

        // Load maze data
        MazeInfoData mazeData;
        try (FileReader reader = new FileReader(mazeModel.getFilePath())) {
            mazeData = new Gson().fromJson(reader, MazeInfoData.class);
        }

        // Create maze
        Maze maze = new Maze(mazeData);

        // Create game configuration
        GameEngine.GameConfig config = new GameEngine.GameConfig();
        config.debug = 0;
        config.turnInfo = 0;
        config.leagueLevel = 5;
        config.logging = 0;
        config.maxTurns = 500;
        config.turnTimeoutMs = 500;
        config.firstTurnTimeoutMs = 1000;
        config.sheetsPerPlayer = 2;

        // Create player list with single bot
        List<String> playerJars = new ArrayList<>();
        playerJars.add(bot.getJarPath());

        // Create and run game engine
        GameEngine engine = new GameEngine(maze, playerJars, config);
        engine.setRandomSpawn(false);
        engine.initialize();

        // Run game with timeout
        Future<?> gameFuture = executor.submit(() -> engine.runGame());

        try {
            gameFuture.get(GAME_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            gameFuture.cancel(true);
            throw new TimeoutException("Game execution timed out after " + GAME_TIMEOUT_SECONDS + " seconds");
        }

        // Get game results from the last game state
        List<GameState> history = engine.getGameHistory();
        if (history.isEmpty()) {
            throw new IllegalStateException("No game history available");
        }

        GameState finalState = history.get(history.size() - 1);
        int stepsTaken = finalState.getTurnNumber();

        // Check if player finished by looking at the final state
        GameState.PlayerSnapshot playerSnapshot = finalState.getPlayers().get(1);
        boolean completed = playerSnapshot != null && playerSnapshot.isFinished();

        // Calculate score
        double scorePercentage = calculateScore(stepsTaken, mazeModel.getMinSteps(), completed);

        // Export game data for replay
        String gameDataPath = gameDataDirectory + "/game_" + System.currentTimeMillis() + "_u" + userId + ".json";
        WebViewerExporter.exportToJSON(engine.getGameHistory(), mazeModel.getName(), gameDataPath);

        // Store result in database
        GameResult result = db.createGameResult(
                userId,
                botId,
                mazeId,
                stepsTaken,
                scorePercentage,
                completed,
                gameDataPath);

        System.out.println("Game complete: " + (completed ? "FINISHED" : "INCOMPLETE") +
                ", Steps: " + stepsTaken + "/" + mazeModel.getMinSteps() +
                ", Score: " + String.format("%.2f%%", scorePercentage));

        return result;
    }

    /**
     * Calculate score based on steps taken vs minimum steps
     * 
     * Score formula:
     * - Perfect (100%): steps == minSteps
     * - Good (90-99%): steps <= minSteps * 1.1
     * - Average (70-89%): steps <= minSteps * 1.5
     * - Poor (50-69%): steps <= minSteps * 2.0
     * - Very Poor (1-49%): steps > minSteps * 2.0
     * - Failed (0%): Did not complete
     * 
     * @param stepsTaken Steps taken by the bot
     * @param minSteps   Minimum steps required
     * @param completed  Whether the maze was completed
     * @return Score percentage (0-100)
     */
    public double calculateScore(int stepsTaken, int minSteps, boolean completed) {
        if (!completed) {
            return 0.0;
        }

        if (stepsTaken <= minSteps) {
            return 100.0;
        }

        // Calculate efficiency ratio
        double ratio = (double) stepsTaken / minSteps;

        // Score decreases as ratio increases
        // Using exponential decay for more gradual scoring
        double score = 100.0 * Math.exp(-0.5 * (ratio - 1.0));

        // Ensure score is between 1 and 100
        return Math.max(1.0, Math.min(100.0, score));
    }

    /**
     * Shutdown the executor
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
