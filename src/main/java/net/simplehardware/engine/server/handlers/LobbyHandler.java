package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.Lobby;
import net.simplehardware.engine.server.database.models.LobbyPlayer;
import net.simplehardware.engine.server.database.models.PlayerBot;
import net.simplehardware.engine.server.security.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class LobbyHandler {
    private static final Gson gson = new Gson();

    private static void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static SessionManager.SessionData getSession(HttpExchange exchange, SessionManager sessionManager) {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return sessionManager.validateSession(token);
        }
        return null;
    }

    public static class CreateLobbyHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public CreateLobbyHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                SessionManager.SessionData session = getSession(exchange, sessionManager);
                if (session == null) {
                    sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                String body;
                try (Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                    body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
                Map<String, Object> request = gson.fromJson(body, Map.class);
                String name = (String) request.get("name");
                int mazeId = ((Double) request.get("mazeId")).intValue();
                int maxPlayers = request.containsKey("maxPlayers") ? ((Double) request.get("maxPlayers")).intValue()
                        : 4;

                if (name == null || name.trim().isEmpty()) {
                    sendResponse(exchange, 400, Map.of("error", "Lobby name is required"));
                    return;
                }

                Lobby lobby = db.createLobby(session.userId, name, mazeId, maxPlayers);
                if (lobby == null) {
                    sendResponse(exchange, 500, Map.of("error", "Failed to create lobby"));
                    return;
                }

                PlayerBot defaultBot = db.getUserDefaultBot(session.userId);
                if (defaultBot != null) {
                    db.joinLobby(lobby.getId(), session.userId, defaultBot.getId());
                }

                sendResponse(exchange, 200, Map.of("lobby", lobby));

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    public static class ListLobbiesHandler implements HttpHandler {
        private final DatabaseManager db;

        public ListLobbiesHandler(DatabaseManager db) {
            this.db = db;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                // Cleanup inactive lobbies first
                db.cleanupInactiveLobbies();

                List<Lobby> lobbies = db.getActiveLobbies();
                List<Map<String, Object>> lobbyData = new ArrayList<>();

                for (Lobby lobby : lobbies) {
                    List<LobbyPlayer> players = db.getLobbyPlayers(lobby.getId());
                    Map<String, Object> data = new HashMap<>();
                    data.put("lobby", lobby);
                    data.put("playerCount", players.size());
                    lobbyData.add(data);
                }

                sendResponse(exchange, 200, Map.of("lobbies", lobbyData));

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    public static class GetLobbyHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public GetLobbyHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");
                int lobbyId = Integer.parseInt(parts[parts.length - 1]);

                Lobby lobby = db.getLobby(lobbyId);
                if (lobby == null) {
                    sendResponse(exchange, 404, Map.of("error", "Lobby not found"));
                    return;
                }

                List<LobbyPlayer> lobbyPlayers = db.getLobbyPlayers(lobbyId);
                List<Map<String, Object>> playersData = new ArrayList<>();

                for (LobbyPlayer lp : lobbyPlayers) {
                    PlayerBot bot = db.getPlayerBotById(lp.getBotId());
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("userId", lp.getUserId());
                    playerData.put("botId", lp.getBotId());
                    playerData.put("botName", bot != null ? bot.getBotName() : "Unknown");
                    playerData.put("joinedAt", lp.getJoinedAt());
                    playersData.add(playerData);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("lobby", lobby);
                response.put("players", playersData);

                // Add maze name
                response.put("mazeName", db.getMazeName(lobby.getMazeId()));

                // Heartbeat if host
                SessionManager.SessionData session = getSession(exchange, sessionManager);
                if (session != null && session.userId == lobby.getHostUserId()) {
                    db.heartbeatLobby(lobbyId);
                }

                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    public static class JoinLobbyHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public JoinLobbyHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                SessionManager.SessionData session = getSession(exchange, sessionManager);
                if (session == null) {
                    sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                String body;
                try (Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                    body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
                Map<String, Object> request = gson.fromJson(body, Map.class);
                int lobbyId = ((Double) request.get("lobbyId")).intValue();

                PlayerBot defaultBot = db.getUserDefaultBot(session.userId);
                if (defaultBot == null) {
                    sendResponse(exchange, 400, Map.of("error", "No default bot selected"));
                    return;
                }

                boolean success = db.joinLobby(lobbyId, session.userId, defaultBot.getId());
                if (!success) {
                    sendResponse(exchange, 400, Map.of("error", "Failed to join lobby (full or not waiting)"));
                    return;
                }

                sendResponse(exchange, 200, Map.of("success", true));

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    public static class LeaveLobbyHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;

        public LeaveLobbyHandler(DatabaseManager db, SessionManager sessionManager) {
            this.db = db;
            this.sessionManager = sessionManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                SessionManager.SessionData session = getSession(exchange, sessionManager);
                if (session == null) {
                    sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                String body;
                try (Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                    body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
                Map<String, Object> request = gson.fromJson(body, Map.class);
                int lobbyId = ((Double) request.get("lobbyId")).intValue();

                boolean success = db.leaveLobby(lobbyId, session.userId);

                Lobby lobby = db.getLobby(lobbyId);
                if (lobby != null && lobby.getHostUserId() == session.userId) {
                    db.deleteLobby(lobbyId);
                }

                sendResponse(exchange, 200, Map.of("success", success));

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    public static class StartLobbyHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;
        private final net.simplehardware.engine.server.services.GameExecutionService gameService;

        public StartLobbyHandler(DatabaseManager db, SessionManager sessionManager,
                net.simplehardware.engine.server.services.GameExecutionService gameService) {
            this.db = db;
            this.sessionManager = sessionManager;
            this.gameService = gameService;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                SessionManager.SessionData session = getSession(exchange, sessionManager);
                if (session == null) {
                    sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                    return;
                }

                String body;
                try (Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                    body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
                Map<String, Object> request = gson.fromJson(body, Map.class);
                int lobbyId = ((Double) request.get("lobbyId")).intValue();

                Lobby lobby = db.getLobby(lobbyId);
                if (lobby == null) {
                    sendResponse(exchange, 404, Map.of("error", "Lobby not found"));
                    return;
                }

                if (lobby.getHostUserId() != session.userId) {
                    sendResponse(exchange, 403, Map.of("error", "Only host can start the game"));
                    return;
                }

                List<LobbyPlayer> players = db.getLobbyPlayers(lobbyId);
                if (players.size() < 2) {
                    sendResponse(exchange, 400, Map.of("error", "Need at least 2 players to start"));
                    return;
                }

                db.updateLobbyStatus(lobbyId, "IN_PROGRESS");

                // Execute game (this might take a while)
                Map<String, Object> result = gameService.executeMultiplayerGame(lobbyId);

                sendResponse(exchange, 200, Map.of("success", true, "lobbyId", lobbyId, "result", result));

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }
}
