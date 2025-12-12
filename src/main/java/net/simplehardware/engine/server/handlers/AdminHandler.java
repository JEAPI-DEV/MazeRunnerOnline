package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.security.SessionManager;
import net.simplehardware.engine.server.services.AdminMetricsService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminHandler {

    public record AdminDashboardHandler(DatabaseManager db, SessionManager sessionManager,
                                        AdminMetricsService metricsService) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, "Method not allowed");
                    return;
                }

                Integer userId = HandlerUtils.getUserIdFromSession(exchange, sessionManager);
                if (userId == null) {
                    HandlerUtils.sendResponse(exchange, 401, "Unauthorized");
                    return;
                }

                try {
                    var user = db.getUserById(userId);
                    if (user == null || !user.isAdmin()) {
                        HandlerUtils.sendResponse(exchange, 403, "Forbidden - Admin access required");
                        return;
                    }

                    Map<String, Object> response = new HashMap<>();

                    Map<String, Object> currentMetrics = metricsService.getCurrentMetrics();
                    response.put("current_metrics", currentMetrics);

                    Map<String, Object> dbStats = db.getDatabaseStats();
                    response.put("database_stats", dbStats);

                    Map<String, Object> dailyGameStats = db.getGameStatistics("daily");
                    Map<String, Object> weeklyGameStats = db.getGameStatistics("weekly");
                    Map<String, Object> monthlyGameStats = db.getGameStatistics("monthly");

                    Map<String, Object> gameStats = new HashMap<>();
                    gameStats.put("daily", dailyGameStats);
                    gameStats.put("weekly", weeklyGameStats);
                    gameStats.put("monthly", monthlyGameStats);
                    response.put("game_statistics", gameStats);

                    Map<String, Object> weeklyUserStats = db.getUserRegistrationStats("weekly");
                    Map<String, Object> monthlyUserStats = db.getUserRegistrationStats("monthly");

                    Map<String, Object> userStats = new HashMap<>();
                    userStats.put("weekly", weeklyUserStats);
                    userStats.put("monthly", monthlyUserStats);
                    response.put("user_statistics", userStats);

                    List<Map<String, Object>> waitTimes = db.getAverageWaitTimesByDifficulty();
                    response.put("wait_times_by_difficulty", waitTimes);

                    HandlerUtils.sendJsonResponse(exchange, 200, response);

                } catch (SQLException e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, "Database error: " + e.getMessage());
                }
            }
        }

    public record MetricsHistoryHandler(DatabaseManager db, SessionManager sessionManager) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, "Method not allowed");
                    return;
                }

                Integer userId = HandlerUtils.getUserIdFromSession(exchange, sessionManager);
                if (userId == null) {
                    HandlerUtils.sendResponse(exchange, 401, "Unauthorized");
                    return;
                }

                try {
                    var user = db.getUserById(userId);
                    if (user == null || !user.isAdmin()) {
                        HandlerUtils.sendResponse(exchange, 403, "Forbidden - Admin access required");
                        return;
                    }

                    Map<String, String> params = HandlerUtils.parseQueryParams(exchange.getRequestURI().getQuery());
                    String metricType = params.getOrDefault("type", "cpu_load");
                    int hours = Integer.parseInt(params.getOrDefault("hours", "24"));

                    List<Map<String, Object>> history = db.getMetricsHistory(metricType, hours);

                    HandlerUtils.sendJsonResponse(exchange, 200, history);

                } catch (SQLException e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, "Database error: " + e.getMessage());
                } catch (NumberFormatException e) {
                    HandlerUtils.sendResponse(exchange, 400, "Invalid hours parameter");
                }
            }
        }

    public record DatabaseManagementHandler(DatabaseManager db, SessionManager sessionManager) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                Integer userId = HandlerUtils.getUserIdFromSession(exchange, sessionManager);
                if (userId == null) {
                    HandlerUtils.sendResponse(exchange, 401, "Unauthorized");
                    return;
                }

                try {
                    var user = db.getUserById(userId);
                    if (user == null || !user.isAdmin()) {
                        HandlerUtils.sendResponse(exchange, 403, "Forbidden - Admin access required");
                        return;
                    }

                    if ("POST".equals(exchange.getRequestMethod())) {
                        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        Gson gson = new Gson();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> request = gson.fromJson(body, Map.class);
                        String action = (String) request.get("action");

                        switch (action) {
                            case "cleanup_old_metrics":
                                int days = request.containsKey("days") ?
                                        ((Double) request.get("days")).intValue() : 30;
                                db.cleanOldMetrics(days);
                                HandlerUtils.sendResponse(exchange, 200, Map.of("success", true, "message", "Old metrics cleaned up"));
                                break;

                            case "cleanup_inactive_lobbies":
                                db.cleanupInactiveLobbies();
                                HandlerUtils.sendResponse(exchange, 200, Map.of("success", true, "message", "Inactive lobbies cleaned up"));
                                break;

                            case "vacuum_database":
                                db.vacuumDatabase();
                                HandlerUtils.sendResponse(exchange, 200, Map.of("success", true, "message", "Database vacuumed"));
                                break;

                            default:
                                HandlerUtils.sendResponse(exchange, 400, Map.of("success", false, "error", "Unknown action"));
                        }
                    } else {
                        HandlerUtils.sendResponse(exchange, 405, "Method not allowed");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, Map.of("success", false, "error", "Database error: " + e.getMessage()));
                }
            }
        }

    public record SystemStatusHandler(DatabaseManager db, SessionManager sessionManager,
                                      AdminMetricsService metricsService) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, "Method not allowed");
                    return;
                }

                Integer userId = HandlerUtils.getUserIdFromSession(exchange, sessionManager);
                if (userId == null) {
                    HandlerUtils.sendResponse(exchange, 401, "Unauthorized");
                    return;
                }

                try {
                    var user = db.getUserById(userId);
                    if (user == null || !user.isAdmin()) {
                        HandlerUtils.sendResponse(exchange, 403, "Forbidden - Admin access required");
                        return;
                    }

                    Map<String, Object> metrics = metricsService.getCurrentMetrics();
                    metrics.put("timestamp", System.currentTimeMillis());

                    HandlerUtils.sendJsonResponse(exchange, 200, metrics);

                } catch (SQLException e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, Map.of("success", false, "error", "Database error: " + e.getMessage()));
                }
            }
        }
}
