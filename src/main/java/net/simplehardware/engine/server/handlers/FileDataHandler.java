package net.simplehardware.engine.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.GameResult;
import net.simplehardware.engine.server.database.models.Maze;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * Handler for serving maze and game data files by ID
 */
public class FileDataHandler {

    /**
         * Serve maze file by maze ID
         */
        public record MazeFileHandler(DatabaseManager db) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendError(exchange, 405, "Method not allowed");
                    return;
                }

                try {
                    String mazeIdStr = HandlerUtils.getQueryParam(exchange, "id");
                    if (mazeIdStr == null) {
                        HandlerUtils.sendError(exchange, 400, "Missing maze ID");
                        return;
                    }

                    int mazeId = Integer.parseInt(mazeIdStr);
                    Maze maze = db.getMazeById(mazeId);

                    if (maze == null) {
                        HandlerUtils.sendError(exchange, 404, "Maze not found");
                        return;
                    }

                    File file = new File(maze.getFilePath());
                    if (!file.exists() || !file.isFile()) {
                        HandlerUtils.sendError(exchange, 404, "Maze file not found");
                        return;
                    }

                    byte[] fileBytes = Files.readAllBytes(file.toPath());

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
                    exchange.sendResponseHeaders(200, fileBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(fileBytes);
                    }

                } catch (NumberFormatException e) {
                    HandlerUtils.sendError(exchange, 400, "Invalid maze ID");
                } catch (SQLException e) {
                    System.err.println("Database error: " + e.getMessage());
                    HandlerUtils.sendError(exchange, 500, "Database error");
                }
            }
        }

    /**
         * Serve game data file by game result ID
         */
        public record GameFileHandler(DatabaseManager db) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendError(exchange, 405, "Method not allowed");
                    return;
                }

                try {
                    String gameIdStr = HandlerUtils.getQueryParam(exchange, "id");
                    if (gameIdStr == null) {
                        HandlerUtils.sendError(exchange, 400, "Missing game ID");
                        return;
                    }

                    int gameId = Integer.parseInt(gameIdStr);
                    GameResult gameResult = db.getGameResultById(gameId);

                    if (gameResult == null) {
                        HandlerUtils.sendError(exchange, 404, "Game not found");
                        return;
                    }

                    String path = gameResult.getGameDataPath();
                    if (path == null || path.isEmpty()) {
                        HandlerUtils.sendError(exchange, 404, "Game data not available");
                        return;
                    }

                    // Security: verify path is in allowed directory
                    if (!path.startsWith("data/games/") || path.contains("..")) {
                        HandlerUtils.sendError(exchange, 403, "Forbidden");
                        return;
                    }

                    File file = new File(path);
                    if (!file.exists() || !file.isFile()) {
                        HandlerUtils.sendError(exchange, 404, "Game file not found");
                        return;
                    }

                    byte[] fileBytes = Files.readAllBytes(file.toPath());

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, fileBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(fileBytes);
                    }

                } catch (NumberFormatException e) {
                    HandlerUtils.sendError(exchange, 400, "Invalid game ID");
                } catch (SQLException e) {
                    System.err.println("Database error: " + e.getMessage());
                    HandlerUtils.sendError(exchange, 500, "Database error");
                } catch (Exception e) {
                    System.err.println("Error serving game file: " + e.getMessage());
                    HandlerUtils.sendError(exchange, 500, "Error reading file");
                }
            }
        }
}
