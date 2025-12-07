package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.GameResult;
import net.simplehardware.engine.server.database.models.Maze;
import net.simplehardware.engine.server.database.models.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Handler for user profile operations
 */
public class UserProfileHandler {

    /**
     * Search for users by username
     */
    public static class SearchUsersHandler implements HttpHandler {
        private final DatabaseManager db;
        private final Gson gson = new Gson();

        public SearchUsersHandler(DatabaseManager db) {
            this.db = db;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                HandlerUtils.sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                // Parse query parameter
                String query = HandlerUtils.getQueryParam(exchange, "q");
                if (query == null || query.trim().isEmpty()) {
                    HandlerUtils.sendError(exchange, 400, "Query parameter 'q' is required");
                    return;
                }

                // Search users
                List<User> users = db.searchUsersByUsername(query.trim());

                // Create response (without password hashes)
                List<Map<String, Object>> userList = new ArrayList<>();
                for (User user : users) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("createdAt", user.getCreatedAt().toString());
                    userList.add(userMap);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("users", userList);

                HandlerUtils.sendJsonResponse(exchange, 200, response);

            } catch (SQLException e) {
                System.err.println("Database error in search users: " + e.getMessage());
                HandlerUtils.sendError(exchange, 500, "Database error");
            }
        }
    }

    /**
     * Get user profile with game history by difficulty
     */
    public static class GetUserProfileHandler implements HttpHandler {
        private final DatabaseManager db;
        private final Gson gson = new Gson();

        public GetUserProfileHandler(DatabaseManager db) {
            this.db = db;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                HandlerUtils.sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                // Get username from path (e.g., /api/user/profile/john)
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");
                
                if (parts.length < 5) {
                    HandlerUtils.sendError(exchange, 400, "Username is required");
                    return;
                }

                String username = parts[4];

                // Get user
                User user = db.getUserByUsername(username);
                if (user == null) {
                    HandlerUtils.sendError(exchange, 404, "User not found");
                    return;
                }

                // Get game history by difficulty
                Map<String, List<Map<String, Object>>> gamesByDifficulty = new HashMap<>();
                
                for (String difficulty : Arrays.asList("EASY", "MEDIUM", "HARD")) {
                    List<GameResult> games = db.getUserGameHistoryByDifficulty(user.getId(), difficulty);
                    List<Map<String, Object>> gameList = new ArrayList<>();

                    for (GameResult game : games) {
                        Maze maze = db.getMazeById(game.getMazeId());
                        
                        Map<String, Object> gameMap = new HashMap<>();
                        gameMap.put("id", game.getId());
                        gameMap.put("mazeId", game.getMazeId());
                        gameMap.put("mazeName", maze != null ? maze.getName() : "Unknown");
                        gameMap.put("stepsTaken", game.getStepsTaken());
                        gameMap.put("scorePercentage", game.getScorePercentage());
                        gameMap.put("completed", game.isCompleted());
                        gameMap.put("playedAt", game.getPlayedAt().toString());
                        gameMap.put("gameId", game.getId());
                        
                        gameList.add(gameMap);
                    }

                    gamesByDifficulty.put(difficulty, gameList);
                }

                // Create response
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("createdAt", user.getCreatedAt().toString());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", userInfo);
                response.put("games", gamesByDifficulty);

                HandlerUtils.sendJsonResponse(exchange, 200, response);

            } catch (SQLException e) {
                System.err.println("Database error in get user profile: " + e.getMessage());
                e.printStackTrace();
                HandlerUtils.sendError(exchange, 500, "Database error");
            }
        }
    }
}
