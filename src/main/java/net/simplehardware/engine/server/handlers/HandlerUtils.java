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

    /**
     * Send JSON response (alias for sendResponse)
     */
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        sendResponse(exchange, statusCode, data);
    }

    /**
     * Send error response
     */
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        java.util.Map<String, Object> error = new java.util.HashMap<>();
        error.put("success", false);
        error.put("error", message);
        sendResponse(exchange, statusCode, error);
    }

    /**
     * Get query parameter from URL
     */
    public static String getQueryParam(HttpExchange exchange, String paramName) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                try {
                    return java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    public static java.util.Map<String, String> parseQueryParams(String query) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                } catch (Exception e) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    public static Integer getUserIdFromSession(HttpExchange exchange, SessionManager sessionManager) {
        SessionManager.SessionData session = validateSession(exchange, sessionManager);
        if (session == null) {
            return null;
        }
        return session.userId();
    }
}
