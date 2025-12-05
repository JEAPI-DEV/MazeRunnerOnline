package net.simplehardware.engine;

import com.google.gson.Gson;
import net.simplehardware.engine.core.GameEngine;
import net.simplehardware.engine.game.Maze;
import net.simplehardware.engine.viewer.GameViewer;
import net.simplehardware.engine.viewer.WebViewerExporter;
import net.simplehardware.engine.viewer.WebViewerServer;
import net.simplehardware.models.MazeInfoData;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main launcher for the Maze Runner game engine
 * Can be invoked from command line or programmatically
 */
public class GameLauncher {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String mapPath = null;
        List<String> playerPaths = new ArrayList<>();
        int maxTurns = 150;
        boolean randomSpawn = false;
        int level = 5;
        int logging = 1, turninfo = 1, debug = 0;
        boolean gui = false;
        boolean web = false;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--map":
                        if (i + 1 < args.length) {
                            mapPath = args[++i];
                        } else {
                            throw new IllegalArgumentException("Missing value for --map");
                        }
                        break;
                    case "--players":
                        if (i + 1 < args.length) {
                            int playerCount = Integer.parseInt(args[++i]);
                            for (int j = 0; j < playerCount; j++) {
                                if (i + 1 < args.length) {
                                    playerPaths.add(args[++i]);
                                } else {
                                    throw new IllegalArgumentException("Not enough player paths provided");
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Missing value for --players");
                        }
                        break;
                    case "--max-turns":
                        if (i + 1 < args.length) {
                            maxTurns = Integer.parseInt(args[++i]);
                        } else {
                            throw new IllegalArgumentException("Missing value for --max-turns");
                        }
                        break;
                    case "--randomSpawn":
                        if (i + 1 < args.length) {
                            randomSpawn = "1".equals(args[++i]) || "true".equalsIgnoreCase(args[i]);
                        } else {
                            throw new IllegalArgumentException("Missing value for --randomSpawn");
                        }
                        break;
                    case "--level":
                        if (i + 1 < args.length) {
                            level = Integer.parseInt(args[++i]);
                        } else {
                            throw new IllegalArgumentException("Missing value for --level");
                        }
                        break;
                    case "--log":
                        if (i + 1 < args.length) {
                            logging = Integer.parseInt(args[++i]);
                        } else {
                            throw new IllegalArgumentException("Missing value for --log");
                        }
                        break;
                    case "--turnInfo":
                        if (i + 1 < args.length) {
                            turninfo = Integer.parseInt(args[++i]);
                        } else {
                            throw new IllegalArgumentException("Missing value for --turnInfo");
                        }
                        break;
                    case "--debug":
                        if (i + 1 < args.length) {
                            debug = Integer.parseInt(args[++i]);
                        } else {
                            throw new IllegalArgumentException("Missing value for --debug");
                        }
                        break;
                    case "--gui":
                        gui = true;
                        break;
                    case "--web":
                        web = true;
                        break;
                    default:
                        // Ignore unknown args or handle as needed
                        break;
                }
            }

            if (mapPath == null) {
                throw new IllegalArgumentException("--map argument is required");
            }

            maxTurns *= playerPaths.size();

            launchGame(mapPath, playerPaths, maxTurns, randomSpawn, level, logging, turninfo, debug, gui, web);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println(
                "Usage: java -jar MazeRunner.jar --map \"path/to/file\" --players <count> \"path/to/player/1\" ... --max-turns <count> --randomSpawn <0|1> --level <int> [--gui] [--web]");
        System.out.println("  --gui: Launch Swing GUI viewer after game completion");
        System.out.println("  --web: Export game data and open web viewer in browser");
    }

    public static void launchGame(String mazeFile, List<String> jarPaths, int maxTurns, boolean randomSpawn, int level,
            int logging, int turninfo, int debug, boolean gui, boolean web)
            throws IOException {
        // Load maze data
        MazeInfoData mazeData;
        try (FileReader reader = new FileReader(mazeFile)) {
            mazeData = new Gson().fromJson(reader, MazeInfoData.class);
        }

        // If no JARs provided via arguments, use those from the maze file (fallback)
        if (jarPaths.isEmpty() && mazeData.playerJars != null && !mazeData.playerJars.isEmpty()) {
            jarPaths = mazeData.playerJars;
        }

        if (jarPaths.isEmpty()) {
            throw new IOException("No player JAR files specified");
        }

        // Validate JAR files exist
        for (String jarPath : jarPaths) {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                throw new IOException("JAR file not found: " + jarPath);
            }
        }

        System.out.println("=================================");
        System.out.println("  MAZE RUNNER CLI");
        System.out.println("=================================");
        System.out.println("Maze file: " + mazeFile);
        System.out.println("Players: " + jarPaths.size());
        System.out.println("Level: " + level);
        System.out.println("Max Turns: " + maxTurns);
        System.out.println("Random Spawn: " + randomSpawn);
        for (int i = 0; i < jarPaths.size(); i++) {
            System.out.println("  Player " + (i + 1) + ": " + jarPaths.get(i));
        }
        System.out.println("=================================\n");

        // Create maze
        Maze maze = new Maze(mazeData);

        System.out.println("Current debug status: " + debug);
        // Create game configuration
        GameEngine.GameConfig config = new GameEngine.GameConfig();
        config.debug = debug;
        config.turnInfo = turninfo;
        config.leagueLevel = level;
        config.logging = logging;
        config.maxTurns = maxTurns;
        config.turnTimeoutMs = 500; // (was 50ms)
        config.firstTurnTimeoutMs = 1000;
        config.sheetsPerPlayer = 2; // Default or could be arg
        // Create and run game
        GameEngine engine = new GameEngine(maze, jarPaths, config);
        // We need to pass randomSpawn to engine
        engine.setRandomSpawn(randomSpawn);

        engine.initialize();
        engine.runGame();

        // Launch GUI viewer if requested
        if (gui) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                new GameViewer(engine.getGameHistory(), mazeData.name);
            });
        }

        // Launch web viewer if requested
        if (web) {
            try {
                // Export game data to JSON
                String outputPath = "game-data.json";
                WebViewerExporter.exportToJSON(engine.getGameHistory(), mazeData.name, outputPath);

                // Check if web viewer files exist
                File htmlFile = new File("game-viewer.html");
                if (!htmlFile.exists()) {
                    System.err.println("Error: game-viewer.html not found in current directory.");
                    System.out.println("Game data exported to: " + outputPath);
                    System.out.println(
                            "Please ensure game-viewer.html, game-viewer.css, and game-viewer.js are in the current directory.");
                    return;
                }

                // Start HTTP server and open browser
                WebViewerServer server = new WebViewerServer();
                server.startAndWait();

            } catch (Exception e) {
                System.err.println("Error launching web viewer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
