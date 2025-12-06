package net.simplehardware.engine.server.services;

import net.simplehardware.engine.core.GameEngine;
import net.simplehardware.engine.game.Maze;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.GameResult;
import net.simplehardware.engine.server.database.models.Lobby;
import net.simplehardware.engine.server.database.models.LobbyPlayer;
import net.simplehardware.engine.server.database.models.PlayerBot;
import net.simplehardware.engine.viewer.WebViewerExporter;
import net.simplehardware.engine.viewer.elements.GameState;
import net.simplehardware.models.MazeInfoData;
import com.google.gson.Gson;

import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        MazeInfoData mazeData;
        try (FileReader reader = new FileReader(mazeModel.getFilePath())) {
            mazeData = new Gson().fromJson(reader, MazeInfoData.class);
        }

        Maze maze = new Maze(mazeData);

        GameEngine.GameConfig config = new GameEngine.GameConfig();
        config.debug = 0;
        config.turnInfo = 0;
        config.leagueLevel = 5;
        config.logging = 0;
        config.maxTurns = 5000;
        config.turnTimeoutMs = 500;
        config.firstTurnTimeoutMs = 1000;
        config.sheetsPerPlayer = 2;

        List<String> playerJars = new ArrayList<>();
        playerJars.add(bot.getJarPath());

        GameEngine engine = new GameEngine(maze, playerJars, config);
        engine.setRandomSpawn(false);
        engine.initialize();

        Future<?> gameFuture = executor.submit(engine::runGame);

        boolean timedOut = false;
        try {
            gameFuture.get(GAME_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            gameFuture.cancel(true);
            timedOut = true;
            System.out.println("Game timed out after " + GAME_TIMEOUT_SECONDS + " seconds");
        }

        List<GameState> history = engine.getGameHistory();
        if (history.isEmpty()) {
            throw new IllegalStateException("No game history available");
        }

        GameState finalState = history.getLast();
        int stepsTaken = finalState.getTurnNumber();

        GameState.PlayerSnapshot playerSnapshot = finalState.getPlayers().get(1);
        boolean completed = playerSnapshot != null && playerSnapshot.isFinished() && !timedOut;

        double scorePercentage = calculateScore(stepsTaken, mazeModel.getMinSteps(), completed);
        // Export game data for replay (even if timed out, so user can review)
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

        GameResult previousBest = db.getBestScoreForMaze(userId, mazeId);

        if (previousBest != null && previousBest.getId() != result.getId()) {
            boolean newIsBetter = result.getScorePercentage() > previousBest.getScorePercentage() ||
                    (result.getScorePercentage() == previousBest.getScorePercentage() &&
                            result.getStepsTaken() < previousBest.getStepsTaken());

            if (newIsBetter) {
                // Delete the old result
                String oldFilePath = db.deleteGameResult(previousBest.getId());
                if (oldFilePath != null) {
                    java.io.File oldFile = new java.io.File(oldFilePath);
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }
            } else {
                String newFilePath = db.deleteGameResult(result.getId());
                if (newFilePath != null) {
                    java.io.File newFile = new java.io.File(newFilePath);
                    if (newFile.exists()) {
                        newFile.delete();
                    }
                }
                result = previousBest;
            }
        }

        System.out.println("Game complete: " + (completed ? "FINISHED" : timedOut ? "TIMED OUT" : "INCOMPLETE") +
                ", Steps: " + stepsTaken + "/" + mazeModel.getMinSteps() +
                ", Score: " + String.format("%.2f%%", scorePercentage));

        return result;
    }

    /**
     * Calculate score based on steps taken vs minimum steps
     * Score formula: (minSteps / stepsTaken) * 100
     * - Perfect (100%): steps == minSteps
     * - Efficiency decreases linearly as steps increase
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

        // Calculate efficiency as (minSteps / stepsTaken) * 100
        double score = ((double) minSteps / stepsTaken) * 100.0;

        // Ensure score doesn't exceed 100 (though it shouldn't with the above logic)
        return Math.min(100.0, score);
    }

    public Map<String, Object> executeMultiplayerGame(int lobbyId) throws Exception {
        Lobby lobby = db.getLobby(lobbyId);
        if (lobby == null) {
            throw new IllegalArgumentException("Lobby not found");
        }

        List<LobbyPlayer> lobbyPlayers = db.getLobbyPlayers(lobbyId);
        if (lobbyPlayers.isEmpty()) {
            throw new IllegalStateException("No players in lobby");
        }

        net.simplehardware.engine.server.database.models.Maze mazeModel = db.getMazeById(lobby.getMazeId());
        if (mazeModel == null) {
            throw new IllegalArgumentException("Maze not found");
        }

        System.out.println(
                "Executing multiplayer game for lobby " + lobbyId + " with " + lobbyPlayers.size() + " players");

        MazeInfoData mazeData;
        try (FileReader reader = new FileReader(mazeModel.getFilePath())) {
            mazeData = new Gson().fromJson(reader, MazeInfoData.class);
        }

        Maze maze = new Maze(mazeData);

        GameEngine.GameConfig config = new GameEngine.GameConfig();
        config.debug = 0;
        config.turnInfo = 0;
        config.maxTurns = 1000;
        config.turnTimeoutMs = 100;
        config.firstTurnTimeoutMs = 1000;
        config.sheetsPerPlayer = 2;

        List<String> playerJars = new ArrayList<>();
        for (LobbyPlayer lp : lobbyPlayers) {
            PlayerBot bot = db.getPlayerBotById(lp.getBotId());
            if (bot != null) {
                playerJars.add(bot.getJarPath());
            }
        }

        GameEngine engine = new GameEngine(maze, playerJars, config);
        engine.setRandomSpawn(false);
        engine.initialize();

        Future<?> gameFuture = executor.submit(engine::runGame);

        boolean timedOut = false;
        try {
            gameFuture.get(GAME_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            gameFuture.cancel(true);
            timedOut = true;
            System.out.println("Multiplayer game timed out after " + GAME_TIMEOUT_SECONDS + " seconds");
        }

        List<GameState> history = engine.getGameHistory();
        if (history.isEmpty()) {
            throw new IllegalStateException("No game history available");
        }

        GameState finalState = history.getLast();
        int stepsTaken = finalState.getTurnNumber();

        String gameDataPath = gameDataDirectory + "/game_" + System.currentTimeMillis() + "_lobby" + lobbyId + ".json";
        WebViewerExporter.exportToJSON(engine.getGameHistory(), mazeModel.getName(), gameDataPath);

        Map<Integer, GameResult> playerResults = new HashMap<>();
        int firstGameResultId = -1;
        for (int i = 0; i < lobbyPlayers.size(); i++) {
            LobbyPlayer lp = lobbyPlayers.get(i);
            int playerId = i + 1;

            GameState.PlayerSnapshot playerSnapshot = finalState.getPlayers().get(playerId);
            boolean completed = playerSnapshot != null && playerSnapshot.isFinished() && !timedOut;
            double scorePercentage = calculateScore(stepsTaken, mazeModel.getMinSteps(), completed);

            GameResult result = db.createGameResult(
                    lp.getUserId(),
                    lp.getBotId(),
                    lobby.getMazeId(),
                    stepsTaken,
                    scorePercentage,
                    completed,
                    gameDataPath);

            playerResults.put(lp.getUserId(), result);
            if (firstGameResultId == -1) {
                firstGameResultId = result.getId(); // Assuming GameResult has an getId() method
            }
        }

        db.updateLobbyStatus(lobbyId, "FINISHED");
        if (firstGameResultId != -1) {
            db.updateLobbyLastGameId(lobbyId, firstGameResultId); // Link game to lobby
        }

        Map<String, Object> response = new HashMap<>();
        response.put("lobbyId", lobbyId);
        response.put("gameDataPath", gameDataPath);
        response.put("stepsTaken", stepsTaken);
        response.put("timedOut", timedOut);
        response.put("results", playerResults);

        return response;
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
