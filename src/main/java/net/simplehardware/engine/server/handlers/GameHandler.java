package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.GameResult;
import net.simplehardware.engine.server.database.models.Maze;
import net.simplehardware.engine.server.database.models.PlayerBot;
import net.simplehardware.engine.server.security.SessionManager;
import net.simplehardware.engine.server.services.GameExecutionService;
import net.simplehardware.engine.server.services.MazeGenerationService;

import java.io.IOException;
import java.util.*;

/**
 * Game execution handlers
 */
public class GameHandler {
    private static final Gson gson = new Gson();

    /**
     * Play game handler - executes a game with a random maze
     */
    public static class PlayGameHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;
        private final MazeGenerationService mazeService;
        private final GameExecutionService gameService;

        public PlayGameHandler(DatabaseManager db, SessionManager sessionManager,
                MazeGenerationService mazeService, GameExecutionService gameService) {
            this.db = db;
            this.sessionManager = sessionManager;
            this.mazeService = mazeService;
            this.gameService = gameService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                // Validate session
                SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                if (session == null) {
                    HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                // Get user's default bot
                PlayerBot bot = db.getUserDefaultBot(session.userId);
                if (bot == null) {
                    HandlerUtils.sendResponse(exchange, 400,
                            Map.of("error", "No default bot selected. Please upload and select a bot first."));
                    return;
                }

                // Parse difficulty from query parameter
                String difficulty = null;
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2 && "difficulty".equals(keyValue[0])) {
                            difficulty = keyValue[1].toUpperCase();
                        }
                    }
                }

                // Get active mazes (filtered by difficulty if specified)
                List<Maze> activeMazes;

                // First, try to get unplayed mazes
                List<Maze> unplayedMazes = db.getUnplayedMazes(session.userId, difficulty);

                if (!unplayedMazes.isEmpty()) {
                    // Prioritize unplayed mazes
                    activeMazes = unplayedMazes;
                } else {
                    // All mazes have been played, select from all active mazes
                    activeMazes = db.getActiveMazes();
                    if (difficulty != null) {
                        final String filterDifficulty = difficulty;
                        activeMazes = activeMazes.stream()
                                .filter(m -> m.getDifficulty().name().equals(filterDifficulty))
                                .toList();
                    }
                }

                if (activeMazes.isEmpty()) {
                    HandlerUtils.sendResponse(exchange, 503,
                            Map.of("error", "No mazes available. Please try again later."));
                    return;
                }

                Random random = new Random();
                Maze maze = activeMazes.get(random.nextInt(activeMazes.size()));

                // Execute game
                GameResult result = gameService.executeGame(session.userId, bot.getId(), maze.getId());

                // Send response
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("gameId", result.getId());
                response.put("mazeName", maze.getName());
                response.put("difficulty", maze.getDifficulty().name());
                response.put("stepsTaken", result.getStepsTaken());
                response.put("minSteps", maze.getMinSteps());
                response.put("score", result.getScorePercentage());
                response.put("completed", result.isCompleted());
                response.put("gameDataPath", result.getGameDataPath());

                HandlerUtils.sendResponse(exchange, 200, response);

            } catch (Exception e) {
                e.printStackTrace();
                HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Game execution failed: " + e.getMessage()));
            }
        }
    }

    /**
     * Get game result handler
     */
    public static class GameResultHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public GameResultHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                // Get game ID from query
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.startsWith("id=")) {
                    HandlerUtils.sendResponse(exchange, 400, Map.of("error", "Game ID required"));
                    return;
                }

                int gameId = Integer.parseInt(query.substring(3));
                GameResult result = db.getGameResultById(gameId);

                if (result == null) {
                    HandlerUtils.sendResponse(exchange, 404, Map.of("error", "Game not found"));
                    return;
                }

                // Send game data file path (no authentication required for viewing replays)
                Map<String, Object> response = new HashMap<>();
                response.put("gameDataPath", result.getGameDataPath());
                response.put("score", result.getScorePercentage());
                response.put("completed", result.isCompleted());

                HandlerUtils.sendResponse(exchange, 200, response);

            } catch (Exception e) {
                e.printStackTrace();
                HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    /**
     * Get user game history handler
     */
    public static class UserHistoryHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public UserHistoryHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                // Validate session
                SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                if (session == null) {
                    HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                // Get type from query
                String type = "SINGLEPLAYER";
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2 && "type".equals(keyValue[0])) {
                            type = keyValue[1].toUpperCase();
                        }
                    }
                }

                // Get user's game history
                List<GameResult> history = db.getUserGameHistory(session.userId, 50, type);

                // Convert to response format
                List<Map<String, Object>> gameList = new ArrayList<>();
                for (GameResult result : history) {
                    Maze maze = db.getMazeById(result.getMazeId());

                    Map<String, Object> gameData = new HashMap<>();
                    gameData.put("id", result.getId());
                    gameData.put("mazeName", maze != null ? maze.getName() : "Unknown");
                    gameData.put("difficulty", maze != null ? maze.getDifficulty().name() : "UNKNOWN");
                    gameData.put("score", result.getScorePercentage());
                    gameData.put("completed", result.isCompleted());
                    gameData.put("stepsTaken", result.getStepsTaken());
                    gameData.put("playedAt", result.getPlayedAt().toString());
                    gameData.put("gameDataPath", result.getGameDataPath());
                    gameList.add(gameData);
                }

                HandlerUtils.sendResponse(exchange, 200, Map.of("games", gameList));

            } catch (Exception e) {
                e.printStackTrace();
                HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    /**
     * List mazes handler
     */
    public static class ListMazesHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public ListMazesHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                // Validate session
                SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                if (session == null) {
                    HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                List<Maze> mazes = db.getAllMazes();
                List<Map<String, Object>> mazeList = new ArrayList<>();

                for (Maze maze : mazes) {
                    Map<String, Object> mazeData = new HashMap<>();
                    mazeData.put("id", maze.getId());
                    mazeData.put("name", maze.getName());
                    mazeData.put("difficulty", maze.getDifficulty());
                    mazeList.add(mazeData);
                }

                HandlerUtils.sendResponse(exchange, 200, Map.of("mazes", mazeList));

            } catch (Exception e) {
                e.printStackTrace();
                HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }
}
