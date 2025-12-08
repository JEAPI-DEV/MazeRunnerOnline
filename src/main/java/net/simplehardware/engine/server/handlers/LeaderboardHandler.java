package net.simplehardware.engine.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leaderboard handler
 */
public record LeaderboardHandler(DatabaseManager db) implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            // Parse query parameters
            int limit = 100;
            String difficulty = null;
            String query = exchange.getRequestURI().getQuery();

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        if ("limit".equals(keyValue[0])) {
                            limit = Integer.parseInt(keyValue[1]);
                        } else if ("difficulty".equals(keyValue[0])) {
                            difficulty = keyValue[1].toUpperCase();
                        }
                    }
                }
            }

            // Get leaderboard (filtered by difficulty if specified)
            List<DatabaseManager.LeaderboardEntry> entries;
            if (difficulty != null) {
                entries = db.getLeaderboardByDifficulty(difficulty, limit);
            } else {
                entries = db.getLeaderboard(limit);
            }

            // Convert to response format
            List<Map<String, Object>> leaderboard = new ArrayList<>();
            int rank = 1;
            for (DatabaseManager.LeaderboardEntry entry : entries) {
                Map<String, Object> entryData = new HashMap<>();
                entryData.put("rank", rank++);
                entryData.put("username", entry.username());
                entryData.put("gamesPlayed", entry.gamesPlayed());
                entryData.put("avgScore", entry.avgScore());
                entryData.put("bestScore", entry.bestScore());
                entryData.put("worstScore", entry.worstScore());
                entryData.put("lastPlayed", entry.lastPlayed() != null ? entry.lastPlayed().toString() : null);
                leaderboard.add(entryData);
            }

            HandlerUtils.sendResponse(exchange, 200, Map.of("leaderboard", leaderboard));

        } catch (Exception e) {
            e.printStackTrace();
            HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}
