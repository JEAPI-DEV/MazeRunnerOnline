package net.simplehardware.engine;

public class GameLauncher {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        try {
            launchServer();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println(
                "Usage: java -jar MazeRunner.jar --map \"path/to/file\" --players <count> \"path/to/player/1\" ... --max-turns <count> --randomSpawn <0|1> --level <int> [--server]");
        System.out.println("  --server: Launch server mode for online competitive play");
    }

    /**
     * Launch server mode
     */
    private static void launchServer() {
        try {
            String configPath = "server.properties";
            net.simplehardware.engine.server.ServerMode serverMode = new net.simplehardware.engine.server.ServerMode(
                    configPath);
            serverMode.start();

            // Keep running
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
