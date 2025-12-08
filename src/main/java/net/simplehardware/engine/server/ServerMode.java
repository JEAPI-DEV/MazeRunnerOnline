package net.simplehardware.engine.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.handlers.*;
import net.simplehardware.engine.server.security.SessionManager;
import net.simplehardware.engine.server.services.GameExecutionService;
import net.simplehardware.engine.server.services.MazeGenerationService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
    private ExecutorService executorService;

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
        boolean sslEnabled = Boolean.parseBoolean(config.getProperty("server.ssl.enabled", "false"));

        if (sslEnabled) {
            try {
                initSSL(port);
            } catch (Exception e) {
                throw new IOException("Failed to initialize SSL", e);
            }
        } else {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }

        // Check if debug mode is enabled
        boolean debugMode = Boolean.parseBoolean(config.getProperty("server.debug", "true"));
        
        // Get registration key from config
        String registerKey = config.getProperty("register.key");
        
        // Register API handlers (with logging if debug enabled)
        createContext(server, "/api/register", new AuthHandler.RegisterHandler(db, sessionManager, registerKey), debugMode);
        createContext(server, "/api/login", new AuthHandler.LoginHandler(db, sessionManager), debugMode);
        createContext(server, "/api/logout", new AuthHandler.LogoutHandler(), debugMode);
        createContext(server, "/api/bot/upload", new BotHandler.UploadBotHandler(db, sessionManager, config), debugMode);
        createContext(server, "/api/bot/list", new BotHandler.ListBotsHandler(db, sessionManager), debugMode);
        createContext(server, "/api/bot/default", new BotHandler.SetDefaultBotHandler(db, sessionManager), debugMode);
        createContext(server, "/api/bot/delete", new BotHandler.DeleteBotHandler(db, sessionManager), debugMode);

        createContext(server, "/api/lobby/create", new LobbyHandler.CreateLobbyHandler(db, sessionManager), debugMode);
        createContext(server, "/api/lobby/list", new LobbyHandler.ListLobbiesHandler(db), debugMode);
        createContext(server, "/api/lobby/join", new LobbyHandler.JoinLobbyHandler(db, sessionManager), debugMode);
        createContext(server, "/api/lobby/leave", new LobbyHandler.LeaveLobbyHandler(db, sessionManager), debugMode);
        createContext(server, "/api/lobby/start", new LobbyHandler.StartLobbyHandler(db, sessionManager, gameService), debugMode);
        createContext(server, "/api/lobby/", new LobbyHandler.GetLobbyHandler(db, sessionManager), debugMode);

        createContext(server, "/api/game/play", new GameHandler.PlayGameHandler(db, sessionManager, gameService), debugMode);
        createContext(server, "/api/game-result", new GameHandler.GameResultHandler(db), debugMode);
        createContext(server, "/api/user/history", new GameHandler.UserHistoryHandler(db, sessionManager), debugMode);
        createContext(server, "/api/leaderboard", new LeaderboardHandler(db), debugMode);
        createContext(server, "/api/mazes", new GameHandler.ListMazesHandler(db, sessionManager), debugMode);

        // User profile handlers
        createContext(server, "/api/user/search", new UserProfileHandler.SearchUsersHandler(db), debugMode);
        createContext(server, "/api/user/profile/", new UserProfileHandler.GetUserProfileHandler(db), debugMode);

        // File data handlers (serve files by ID instead of direct file access)
        createContext(server, "/api/maze/file", new FileDataHandler.MazeFileHandler(db), debugMode);
        createContext(server, "/api/game/file", new FileDataHandler.GameFileHandler(db), debugMode);

        // Static file handler for web pages
        createContext(server, "/", new StaticFileHandler(config.getProperty("web.directory", "web")), debugMode);

        // Set up thread pool executor for handling requests concurrently
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        server.setExecutor(executorService);
        server.start();

        String protocol = sslEnabled ? "https" : "http";
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  MAZE RUNNER SERVER MODE");
        System.out.println("=".repeat(50));
        System.out.println("Server started on port: " + port);
        System.out.println("URL: " + protocol + "://localhost:" + port);
        System.out.println("Database: " + config.getProperty("database.path"));
        System.out.println("Thread pool size: " + threadPoolSize);
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

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
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

    private void initSSL(int port) throws Exception {
        String keystorePath = config.getProperty("server.ssl.keystore");
        String password = config.getProperty("server.ssl.password");

        if (keystorePath == null || password == null) {
            throw new RuntimeException("SSL enabled but keystore or password missing");
        }

        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
        SSLContext sslContext = SSLContext.getInstance("TLS");

        char[] passwordChars = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, passwordChars);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwordChars);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    SSLContext c = getSSLContext();
                    SSLParameters sslParams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslParams);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        this.server = httpsServer;
    }

    /**
     * Helper method to create context with optional logging
     */
    private void createContext(HttpServer server, String path, com.sun.net.httpserver.HttpHandler handler, boolean debug) {
        if (debug) {
            server.createContext(path, new LoggingHandler(handler));
        } else {
            server.createContext(path, handler);
        }
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
