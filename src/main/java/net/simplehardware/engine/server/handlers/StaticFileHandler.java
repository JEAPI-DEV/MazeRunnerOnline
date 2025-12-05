package net.simplehardware.engine.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Static file handler for serving web pages
 */
public class StaticFileHandler implements HttpHandler {
    private final String webDirectory;

    public StaticFileHandler(String webDirectory) {
        this.webDirectory = webDirectory;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Default to index.html
        if (path.equals("/")) {
            path = "/index.html";
        }

        // Security: prevent directory traversal
        if (path.contains("..")) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        File file;

        // Check if requesting a file from data directory (e.g., /data/games/...)
        if (path.startsWith("/data/")) {
            file = new File(path.substring(1)); // Remove leading slash
        } else {
            // Serve from web directory
            file = new File(webDirectory + path);
        }

        if (!file.exists() || !file.isFile()) {
            sendError(exchange, 404, "Not Found");
            return;
        }

        // Determine content type
        String contentType = getContentType(path);

        // Read and send file
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
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
}
