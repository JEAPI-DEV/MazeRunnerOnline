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
public record StaticFileHandler(String webDirectory) implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        // Security: prevent directory traversal
        if (path.contains("..")) {
            sendError(exchange, 403, "Forbidden");
            return;
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
}
