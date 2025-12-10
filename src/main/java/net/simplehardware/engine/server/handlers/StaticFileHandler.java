package net.simplehardware.engine.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.User;
import net.simplehardware.engine.server.security.SessionManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * Static file handler for serving web pages with authentication
 */
public class StaticFileHandler implements HttpHandler {
    private final String webDirectory;
    private final SessionManager sessionManager;
    private final DatabaseManager databaseManager;

    public StaticFileHandler(String webDirectory, SessionManager sessionManager, DatabaseManager databaseManager) {
        this.webDirectory = webDirectory;
        this.sessionManager = sessionManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        if (path.contains("..")) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        if (requiresAuthentication(path)) {
            String token = extractToken(exchange);
            System.out.println("DEBUG: Checking auth for " + path + ", token found: " + (token != null));
            SessionManager.SessionData session = token != null ? sessionManager.validateSession(token) : null;
            
            if (session == null) {
                System.out.println("DEBUG: No valid session, redirecting to index.html");
                sendRedirect(exchange, "/index.html");
                return;
            }

            if (path.equals("/admin.html")) {
                try {
                    User user = databaseManager.getUserById(session.userId());
                    System.out.println("DEBUG: User " + session.username() + " (ID: " + session.userId() + ") - isAdmin: " + (user != null && user.isAdmin()));
                    if (user == null || !user.isAdmin()) {
                        sendRedirect(exchange, "/dashboard.html");
                        return;
                    }
                } catch (SQLException e) {
                    System.err.println("DEBUG: Database error checking admin status: " + e.getMessage());
                    sendError(exchange, 500, "Database error");
                    return;
                }
            }
        }

        File file;

        if (path.startsWith("/data/")) {
            file = new File(path.substring(1));
        } else {
            file = new File(webDirectory + path);
        }

        if (!file.exists() || !file.isFile()) {
            sendError(exchange, 404, "Not Found");
            return;
        }

        String contentType = getContentType(path);
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") ||
                path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif")) {
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400"); // 1 day
        } else {
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        }

        exchange.sendResponseHeaders(200, fileBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html"))
            return "text/html";
        if (path.endsWith(".css"))
            return "text/css";
        if (path.endsWith(".js"))
            return "application/javascript";
        if (path.endsWith(".json"))
            return "application/json";
        if (path.endsWith(".png"))
            return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        if (path.endsWith(".gif"))
            return "image/gif";
        if (path.endsWith(".svg"))
            return "image/svg+xml";
        return "application/octet-stream";
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.getResponseBody().close();
    }

    private boolean requiresAuthentication(String path) {
        if (path.equals("/index.html") || path.equals("/robots.txt")) {
            return false;
        }
        if (path.endsWith(".css") || path.endsWith(".js") || 
            path.endsWith(".png") || path.endsWith(".jpg") || 
            path.endsWith(".jpeg") || path.endsWith(".gif") || path.endsWith(".svg")) {
            return false;
        }
        return path.endsWith(".html");
    }

    private String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        System.out.println("DEBUG: Cookie header: " + cookieHeader);
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                System.out.println("DEBUG: Cookie part: " + parts[0] + " = " + (parts.length > 1 ? parts[1].substring(0, Math.min(20, parts[1].length())) + "..." : ""));
                if (parts.length == 2 && parts[0].equals("token")) {
                    return parts[1];
                }
            }
        }

        return null;
    }
}
