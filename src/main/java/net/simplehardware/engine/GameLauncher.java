package net.simplehardware.engine;

import com.google.gson.Gson;
import net.simplehardware.engine.core.GameEngine;
import net.simplehardware.engine.game.Maze;
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
                    default:
                        // Ignore unknown args or handle as needed
                        break;
                }
            }

            if (mapPath == null) {
                throw new IllegalArgumentException("--map argument is required");
            }

            launchGame(mapPath, playerPaths, maxTurns, randomSpawn, level);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println(
                "Usage: java -jar MazeRunner.jar --map \"path/to/file\" --players <count> \"path/to/player/1\" ... --max-turns <count> --randomSpawn <0|1> --level <int>");
    }

    public static void launchGame(String mazeFile, List<String> jarPaths, int maxTurns, boolean randomSpawn, int level)
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

        // Create game configuration
        GameEngine.GameConfig config = new GameEngine.GameConfig();
        config.leagueLevel = level;
        config.maxTurns = maxTurns;
        config.turnTimeoutMs = 50;
        config.firstTurnTimeoutMs = 1000;
        config.sheetsPerPlayer = 2; // Default or could be arg
        // randomSpawn is not in GameConfig yet, might need to handle it.
        // If randomSpawn is true, we might need to randomize start positions.
        // The current GameEngine/Maze implementation might handle start positions from
        // the map.
        // If randomSpawn is requested, we might need to override that.
        // For now, I will pass it to GameEngine or handle it here.
        // Since I can't easily see Maze implementation details right now without
        // reading more files,
        // I'll assume GameEngine or Maze has a way to handle it, or I'll add it to
        // config.

        // Let's add randomSpawn to GameConfig if possible, or handle it before creating
        // Maze?
        // Maze constructor takes MazeData.
        // I'll update GameConfig to include randomSpawn and let GameEngine handle it.

        // Create and run game
        GameEngine engine = new GameEngine(maze, jarPaths, config);
        // We need to pass randomSpawn to engine
        engine.setRandomSpawn(randomSpawn);

        engine.initialize();
        engine.runGame();
    }
}
