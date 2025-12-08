package net.simplehardware.engine.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.PlayerBot;
import net.simplehardware.engine.server.security.SessionManager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Bot management handlers
 */
public class BotHandler {
    private static final Gson gson = new Gson();

    /**
         * Upload bot handler
         */
    public record UploadBotHandler(DatabaseManager db, SessionManager sessionManager,
                                       Properties config) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                try {
                    // Validate session
                    SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                    if (session == null) {
                        HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                        return;
                    }

                    // Parse multipart form data
                    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                    if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                        HandlerUtils.sendResponse(exchange, 400,
                                Map.of("error", "Content-Type must be multipart/form-data"));
                        return;
                    }

                    // Read the uploaded file
                    byte[] requestBody = IOUtils.toByteArray(exchange.getRequestBody());

                    // Simple multipart parsing (for production, use a proper library)
                    String boundary = contentType.split("boundary=")[1];
                    String bodyStr = new String(requestBody, StandardCharsets.UTF_8);

                    // Extract bot name and file data
                    String botName = extractFormField(bodyStr);
                    byte[] fileData = extractFileData(requestBody, boundary);

                    if (botName == null || botName.trim().isEmpty()) {
                        HandlerUtils.sendResponse(exchange, 400, Map.of("error", "Bot name is required"));
                        return;
                    }

                    // Check if bot name already exists
                    if (db.checkBotNameExists(session.userId(), botName)) {
                        HandlerUtils.sendResponse(exchange, 409,
                                Map.of("error", "Bot name '" + botName + "' is already in use"));
                        return;
                    }

                    if (fileData == null || fileData.length == 0) {
                        HandlerUtils.sendResponse(exchange, 400, Map.of("error", "JAR file is required"));
                        return;
                    }

                    // Validate file size
                    int maxSizeMB = Integer.parseInt(config.getProperty("upload.max.size.mb", "10"));
                    if (fileData.length > maxSizeMB * 1024 * 1024) {
                        HandlerUtils.sendResponse(exchange, 400,
                                Map.of("error", "File too large (max " + maxSizeMB + "MB)"));
                        return;
                    }

                    // Save file
                    String uploadDir = config.getProperty("upload.directory", "data/bots");
                    new File(uploadDir).mkdirs();

                    String fileName = "user" + session.userId() + "_" + System.currentTimeMillis() + ".jar";
                    String filePath = uploadDir + "/" + fileName;

                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        fos.write(fileData);
                    }

                    // Store in database
                    PlayerBot bot = db.createPlayerBot(session.userId(), botName, filePath);

                    // Send response
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("botId", bot.getId());
                    response.put("botName", bot.getBotName());
                    response.put("isDefault", bot.isDefault());

                    HandlerUtils.sendResponse(exchange, 201, response);

                } catch (Exception e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
                }
            }

            private String extractFormField(String body) {
                String marker = "name=\"" + "botName" + "\"";
                int start = body.indexOf(marker);
                if (start == -1)
                    return null;

                start = body.indexOf("\r\n\r\n", start) + 4;
                int end = body.indexOf("\r\n", start);

                return body.substring(start, end);
            }

            private byte[] extractFileData(byte[] body, String boundary) {
                String bodyStr = new String(body, StandardCharsets.ISO_8859_1);
                String fileMarker = "Content-Type: application/";
                int start = bodyStr.indexOf(fileMarker);
                if (start == -1)
                    return null;

                start = bodyStr.indexOf("\r\n\r\n", start) + 4;
                String endBoundary = "\r\n--" + boundary;
                int end = bodyStr.indexOf(endBoundary, start);

                byte[] fileData = new byte[end - start];
                System.arraycopy(body, start, fileData, 0, fileData.length);
                return fileData;
            }
        }

    /**
         * List bots handler
         */
        public record ListBotsHandler(DatabaseManager db, SessionManager sessionManager) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                try {
                    SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                    if (session == null) {
                        HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                        return;
                    }

                    List<PlayerBot> bots = db.getUserBots(session.userId());

                    List<Map<String, Object>> botList = new ArrayList<>();
                    for (PlayerBot bot : bots) {
                        Map<String, Object> botData = new HashMap<>();
                        botData.put("id", bot.getId());
                        botData.put("botName", bot.getBotName());
                        botData.put("uploadedAt", bot.getUploadedAt().toString());
                        botData.put("isDefault", bot.isDefault());
                        botList.add(botData);
                    }

                    HandlerUtils.sendResponse(exchange, 200, Map.of("bots", botList));

                } catch (Exception e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
                }
            }
        }

    /**
         * Set default bot handler
         */
        public record SetDefaultBotHandler(DatabaseManager db, SessionManager sessionManager) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                try {
                    // Validate session
                    SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                    if (session == null) {
                        HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                        return;
                    }

                    // Parse request body
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), Map.class);
                    if (!body.containsKey("botId")) {
                        HandlerUtils.sendResponse(exchange, 400, Map.of("error", "botId is required"));
                        return;
                    }

                    int botId = ((Double) body.get("botId")).intValue();

                    // Set default
                    db.setUserDefaultBot(session.userId(), botId);

                    HandlerUtils.sendResponse(exchange, 200, Map.of("success", true));

                } catch (Exception e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
                }
            }
        }

    /**
         * Delete bot handler
         */
        public record DeleteBotHandler(DatabaseManager db, SessionManager sessionManager) implements HttpHandler {

        @Override
            @SuppressWarnings("unchecked")
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    HandlerUtils.sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                try {
                    // Validate session
                    SessionManager.SessionData session = HandlerUtils.validateSession(exchange, sessionManager);
                    if (session == null) {
                        HandlerUtils.sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                        return;
                    }

                    // Parse request body
                    Map<String, Object> body = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), Map.class);
                    if (!body.containsKey("botId")) {
                        HandlerUtils.sendResponse(exchange, 400, Map.of("error", "botId is required"));
                        return;
                    }

                    int botId = ((Double) body.get("botId")).intValue();

                    // Delete from DB and get file path
                    String filePath = db.deletePlayerBot(session.userId(), botId);

                    if (filePath != null) {
                        // Delete file
                        File file = new File(filePath);
                        if (file.exists()) {
                            file.delete();
                        }
                        HandlerUtils.sendResponse(exchange, 200, Map.of("success", true));
                    } else {
                        HandlerUtils.sendResponse(exchange, 404, Map.of("error", "Bot not found or not owned by user"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    HandlerUtils.sendResponse(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
                }
            }
        }
}
