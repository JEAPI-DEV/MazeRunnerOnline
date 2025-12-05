package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.simplehardware.engine.server.security.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for HTTP handlers
 */
public class HandlerUtils {
    private static final Gson gson = new Gson();

    /**
     * Send JSON response
     */
    public static void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Validate session from Authorization header
     */
    public static SessionManager.SessionData validateSession(HttpExchange exchange, SessionManager sessionManager) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        return sessionManager.validateSession(token);
    }
}
