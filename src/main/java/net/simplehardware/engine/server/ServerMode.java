package net.simplehardware.engine.server;

import com.sun.net.httpserver.HttpServer;
import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.handlers.*;
import net.simplehardware.engine.server.security.SessionManager;
import net.simplehardware.engine.server.services.GameExecutionService;
import net.simplehardware.engine.server.services.MazeGenerationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * Main server mode entry point
 */
public class ServerMode {
    private final DatabaseManager db;
    private final SessionManager sessionManager;
    private final MazeGenerationService mazeService;
    private final GameExecutionService gameService;
    private HttpServer server;
    private final Properties config;

    public ServerMode(String configPath) throws Exception {
        // Load configuration
        this.config = loadConfig(configPath);

        // Initialize database
        String dbPath = config.getProperty("database.path", "data/mazerunner.db");
        this.db = new DatabaseManager(dbPath);
        this.db.initialize();

        // Initialize session manager
        String jwtSecret = config.getProperty("jwt.secret", "your-secret-key-change-this-in-production");
        this.sessionManager = new SessionManager(jwtSecret);

        // Initialize services
        String mazeCreatorJar = config.getProperty("maze.creator.jar", "MazeCreator-1_4_2.jar");
        String mazesDir = config.getProperty("mazes.directory", "data/mazes");
        this.mazeService = new MazeGenerationService(db, mazeCreatorJar, mazesDir);

        String gameDataDir = config.getProperty("game.data.directory", "data/games");
        this.gameService = new GameExecutionService(db, gameDataDir);
    }

    /**
     * Start the server
     */
    public void start() throws IOException {
        int port = Integer.parseInt(config.getProperty("server.port", "8080"));

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register API handlers
        server.createContext("/api/register", new AuthHandler.RegisterHandler(db, sessionManager));
        server.createContext("/api/login", new AuthHandler.LoginHandler(db, sessionManager));
        server.createContext("/api/logout", new AuthHandler.LogoutHandler());
        server.createContext("/api/bot/upload", new BotHandler.UploadBotHandler(db, sessionManager, config));
        server.createContext("/api/bot/list", new BotHandler.ListBotsHandler(db, sessionManager));
        server.createContext("/api/bot/default", new BotHandler.SetDefaultBotHandler(db, sessionManager));
        server.createContext("/api/bot/delete", new BotHandler.DeleteBotHandler(db, sessionManager));

        server.createContext("/api/lobby/create", new LobbyHandler.CreateLobbyHandler(db, sessionManager));
        server.createContext("/api/lobby/list", new LobbyHandler.ListLobbiesHandler(db));
        server.createContext("/api/lobby/join", new LobbyHandler.JoinLobbyHandler(db, sessionManager));
        server.createContext("/api/lobby/leave", new LobbyHandler.LeaveLobbyHandler(db, sessionManager));
        server.createContext("/api/lobby/start", new LobbyHandler.StartLobbyHandler(db, sessionManager, gameService));
        server.createContext("/api/lobby/", new LobbyHandler.GetLobbyHandler(db, sessionManager));

        server.createContext("/api/game/play",
                new GameHandler.PlayGameHandler(db, sessionManager, mazeService, gameService));
        server.createContext("/api/game-result", new GameHandler.GameResultHandler(db, sessionManager));
        server.createContext("/api/user/history", new GameHandler.UserHistoryHandler(db, sessionManager));
        server.createContext("/api/leaderboard", new LeaderboardHandler(db));
        server.createContext("/api/mazes", new GameHandler.ListMazesHandler(db, sessionManager));

        // Static file handler for web pages
        server.createContext("/", new StaticFileHandler(config.getProperty("web.directory", "web")));

        server.setExecutor(null);
        server.start();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("  MAZE RUNNER SERVER MODE");
        System.out.println("=".repeat(50));
        System.out.println("Server started on port: " + port);
        System.out.println("URL: http://localhost:" + port);
        System.out.println("Database: " + config.getProperty("database.path"));
        System.out.println("=".repeat(50) + "\n");

        // Start maze generation service
        int mazeGenInterval = Integer.parseInt(config.getProperty("maze.generation.interval.hours", "6"));
        mazeService.startScheduledGeneration(mazeGenInterval);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Shutdown the server
     */
    public void shutdown() {
        System.out.println("\nShutting down server...");

        if (server != null) {
            server.stop(0);
        }

        mazeService.stop();
        gameService.shutdown();

        try {
            db.close();
        } catch (Exception e) {
            System.err.println("Error closing database: " + e.getMessage());
        }

        System.out.println("Server shutdown complete.");
    }

    /**
     * Load configuration from file
     */
    private Properties loadConfig(String configPath) throws IOException {
        Properties props = new Properties();

        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        } else {
            System.out.println("Config file not found, using defaults: " + configPath);
        }

        return props;
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            String configPath = args.length > 0 ? args[0] : "server.properties";
            ServerMode server = new ServerMode(configPath);
            server.start();

            // Keep running
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
