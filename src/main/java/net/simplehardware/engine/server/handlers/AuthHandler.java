package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.User;
import net.simplehardware.engine.server.security.PasswordHasher;
import net.simplehardware.engine.server.security.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Authentication handlers
 */
public class AuthHandler {
    private static final Gson gson = new Gson();

    /**
     * Register handler
     */
    public static class RegisterHandler implements HttpHandler {
        private final DatabaseManager db;
        private final SessionManager sessionManager;
        private final String requiredRegisterKey;

        public RegisterHandler(DatabaseManager db, SessionManager sessionManager, String requiredRegisterKey) {
            this.db = db;
            this.sessionManager = sessionManager;
            this.requiredRegisterKey = requiredRegisterKey;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                String body = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8).useDelimiter("\\A").next();
                @SuppressWarnings("unchecked")
                Map<String, String> request = gson.fromJson(body, Map.class);

                String username = request.get("username");
                String password = request.get("password");
                String registerKey = request.get("registerKey");

                // Validate registration key
                if (requiredRegisterKey != null && !requiredRegisterKey.isEmpty()) {
                    if (registerKey == null || !registerKey.equals(requiredRegisterKey)) {
                        sendResponse(exchange, 403, Map.of("error", "Invalid registration key"));
                        return;
                    }
                }

                if (!PasswordHasher.isValidUsername(username)) {
                    sendResponse(exchange, 400, Map.of("error", "Invalid username (3-20 alphanumeric characters)"));
                    return;
                }

                if (!PasswordHasher.isValidPassword(password)) {
                    sendResponse(exchange, 400,
                            Map.of("error", "Invalid password (min 8 chars, must contain letter and number)"));
                    return;
                }

                if (db.getUserByUsername(username) != null) {
                    sendResponse(exchange, 409, Map.of("error", "Username already exists"));
                    return;
                }

                String passwordHash = PasswordHasher.hashPassword(password);
                User user = db.createUser(username, passwordHash);

                String token = sessionManager.createSession(user.getId(), user.getUsername());
                exchange.getResponseHeaders().add("Set-Cookie", 
                    "token=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + (60 * 60 * 24 * 7));

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("token", token);
                response.put("userId", user.getId());
                response.put("username", user.getUsername());

                sendResponse(exchange, 201, response);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
            }
        }
    }

    /**
         * Login handler
         */
    public record LoginHandler(DatabaseManager db, SessionManager sessionManager) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                try {
                    String body = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8).useDelimiter("\\A").next();
                    @SuppressWarnings("unchecked")
                    Map<String, String> request = gson.fromJson(body, Map.class);

                    String username = request.get("username");
                    String password = request.get("password");

                    User user = db.getUserByUsername(username);
                    if (user == null) {
                        sendResponse(exchange, 401, Map.of("error", "Invalid credentials"));
                        return;
                    }

                    if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
                        sendResponse(exchange, 401, Map.of("error", "Invalid credentials"));
                        return;
                    }
                    String token = sessionManager.createSession(user.getId(), user.getUsername());
                    exchange.getResponseHeaders().add("Set-Cookie", 
                        "token=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + (60 * 60 * 24 * 7));

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("token", token);
                    response.put("userId", user.getId());
                    response.put("username", user.getUsername());

                    sendResponse(exchange, 200, response);

                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
                }
            }
        }

    /**
     * Logout handler
     */
    public static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, Map.of("success", true));
        }
    }

    /**
     * Send JSON response
     */
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
}
