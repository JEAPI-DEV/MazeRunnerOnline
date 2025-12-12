package net.simplehardware.engine.core;

import net.simplehardware.engine.game.ActionResult;
import net.simplehardware.engine.game.Direction;
import net.simplehardware.engine.game.Maze;
import net.simplehardware.engine.players.Player;
import net.simplehardware.engine.cells.Cell;
import net.simplehardware.engine.viewer.elements.GameState;
import net.simplehardware.engine.viewer.elements.PlayerLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class GameEngine {
    private final Maze maze;
    private final List<Player> players;
    private final Map<Player, PlayerProcess> playerProcesses;
    private final Referee referee;
    private final int leagueLevel;
    private final int maxTurns;
    private final long turnTimeout;
    private final long firstTurnTimeout;
    private final int sheetsPerPlayer;
    private final int logging;
    private final int turnInfo;

    private final Map<Player, ActionResult> lastResults;

    private boolean randomSpawn = false;
    private final Map<Integer, StringBuilder> playerLogs = new HashMap<>();
    private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCapture = new ByteArrayOutputStream();
    private final StringBuilder protocolCapture = new StringBuilder();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final List<GameState> gameHistory = new ArrayList<>();
    private final Map<Integer, PlayerLog> currentTurnLogs = new HashMap<>();

    public GameEngine(Maze maze, List<String> jarPaths, GameConfig config) {
        this.maze = maze;
        this.leagueLevel = config.leagueLevel;
        this.maxTurns = config.maxTurns;
        this.turnTimeout = config.turnTimeoutMs;
        this.firstTurnTimeout = config.firstTurnTimeoutMs;
        this.sheetsPerPlayer = config.sheetsPerPlayer;
        this.logging = config.logging;
        this.turnInfo = config.turnInfo;

        this.players = new ArrayList<>();
        this.playerProcesses = new HashMap<>();
        this.lastResults = new HashMap<>();

        initializePlayers(jarPaths);
        maze.applyLevelRestrictions(leagueLevel);

        assignForms();
        maze.removeUnusedPlayerCells(players);
        maze.updateFinishCells(players);
        this.referee = new Referee(maze, players, leagueLevel, config.debug == 1);
    }

    public void setRandomSpawn(boolean randomSpawn) {
        this.randomSpawn = randomSpawn;
    }

    private void initializePlayers(List<String> jarPaths) {
        List<int[]> validStarts = new ArrayList<>();
        if (randomSpawn) {
            for (int y = 0; y < maze.getHeight(); y++) {
                for (int x = 0; x < maze.getWidth(); x++) {
                    Cell cell = maze.getCell(x, y);
                    if (cell instanceof net.simplehardware.engine.cells.FloorCell) {
                        validStarts.add(new int[] { x, y });
                    }
                }
            }
            Collections.shuffle(validStarts);
        }

        for (int i = 0; i < jarPaths.size(); i++) {
            int playerId = i + 1;
            int[] startPos;

            if (randomSpawn && i < validStarts.size()) {
                startPos = validStarts.get(i);
                maze.setStartPosition(playerId, startPos[0], startPos[1]);
            } else {
                startPos = maze.getStartPosition(playerId);
            }

            if (startPos == null) {
                System.err.println("No start position found for player " + playerId);
                continue;
            }

            Player player = new Player(playerId, startPos[0], startPos[1], sheetsPerPlayer);
            players.add(player);

            try {
                PlayerProcess process = new PlayerProcess(playerId, jarPaths.get(i));
                playerProcesses.put(player, process);
                lastResults.put(player, ActionResult.ok(""));
                playerLogs.put(playerId, new StringBuilder());
            } catch (IOException e) {
                System.err.println("Failed to start player " + playerId + ": " + e.getMessage());
                player.setActive(false);
            }
        }
    }

    private void assignForms() {
        for (Player player : players) {
            for (int y = 0; y < maze.getHeight(); y++) {
                for (int x = 0; x < maze.getWidth(); x++) {
                    Cell cell = maze.getCell(x, y);
                    if (cell instanceof net.simplehardware.engine.cells.FloorCell floor) {
                        if (floor.getForm() != null && floor.getFormOwner() == player.getId()) {
                            char form = floor.getForm();
                            if (!player.getAssignedForms().contains(form)) {
                                player.addAssignedForm(form);
                            }
                        }
                    }
                }
            }
            player.getAssignedForms().sort(Character::compareTo);
        }
    }

    public void initialize() {
        System.out.println("=== Game Initialization ===");
        System.out.println("Maze: " + maze.getName());
        System.out.println("Players: " + players.size());
        System.out.println("League Level: " + leagueLevel);
        System.out.println("Max Turns: " + maxTurns);

        for (Player player : players) {
            if (!player.isActive())
                continue;

            PlayerProcess process = playerProcesses.get(player);
            // MAZE_WIDTH MAZE_HEIGHT LEAGUE_LEVEL
            process.sendLine(maze.getWidth() + " " + maze.getHeight() + " " + leagueLevel);

            // PLAYER_ID START_X START_Y SHEETS_PER_PLAYER (Level 5+)
            String line2 = player.getId() + " " + player.getStartX() + " " + player.getStartY();
            if (leagueLevel >= 5) {
                line2 += " " + sheetsPerPlayer;
            }
            process.sendLine(line2);

            System.out.println("Player " + player.getId() + " initialized at (" +
                    player.getStartX() + "," + player.getStartY() + ")");
        }
    }

    public void runGame() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n=== Starting Game ===\n");
        System.out.println(
                "DEBUG: Entering game loop. MaxTurns: " + maxTurns + ", CurrentTurn: " + referee.getCurrentTurn());


        while (!referee.isGameOver(maxTurns)) {
            System.out.println("DEBUG: Running turn " + (referee.getCurrentTurn() + 1));

            captureGameState();
            runTurn();
            referee.updateTurn();
        }
        captureGameState();
        System.out.println("\n=== Game Over ===");
        printFinalResults();
        for (PlayerProcess process : playerProcesses.values()) {
            process.destroy();
        }
    }

    private void runTurn() {
        int turn = referee.getCurrentTurn();
        outputCapture.reset();
        errorCapture.reset();
        protocolCapture.setLength(0);
        System.setOut(new PrintStream(outputCapture));
        System.setErr(new PrintStream(errorCapture));

        if (turnInfo == 1)
            System.out.println("--- Turn " + turn + " ---");

        for (Player player : players) {
            if (!player.isActive())
                continue;

            PlayerProcess process = playerProcesses.get(player);

            protocolCapture.append("=== Player ").append(player.getId()).append(" ===\n");

            // Send turn data (6 lines)
            sendTurnData(player, process);
            protocolCapture.append("\n");
            try {
                long timeout = (turn == 1 || turn == 2) ? firstTurnTimeout : turnTimeout;

                List<String> outputs = new ArrayList<>();
                String firstLine = process.readLine(timeout);
                turn = referee.getCurrentTurn()+1;

                if (firstLine == null || firstLine.trim().isEmpty() && turnInfo == 1) {
                    System.out.println("Player " + player.getId() + ": <no action>");
                    lastResults.put(player, ActionResult.fail("INVALID"));
                    continue;
                }

                outputs.add(firstLine);

                try {
                    while (process.hasMoreOutput()) {
                        String extraLine = process.readLineNonBlocking();
                        if (extraLine != null && !extraLine.trim().isEmpty()) {
                            outputs.add(extraLine);
                        } else {
                            break;
                        }
                    }
                } catch (IOException ignored) {
                }

                String action = outputs.getLast();
                if (action.startsWith("Listening ") && outputs.size() > 1) {
                    action = outputs.getLast();
                }
                if (outputs.size() > 1) {
                    System.out.println("Player " + player.getId() + " output " + outputs.size() + " lines, using: " + action);
                }

                if (turnInfo == 1) System.out.println("Player " + player.getId() + ": " + action);

                ActionResult result = referee.processAction(player, action);
                lastResults.put(player, result);

                logToPlayer(player.getId(), action);

                if (turnInfo == 1)
                    System.out.println("  Result: " + result);

            } catch (TimeoutException e) {
                System.out.println("Player " + player.getId() + ": TIMEOUT");
                player.setTimedOut(true);
                player.setActive(false);
                lastResults.put(player, ActionResult.fail("TIMEOUT"));
            }
        }

        if (turnInfo == 1)
            System.out.println();

        StringBuilder playerStdoutAll = new StringBuilder();
        StringBuilder playerStderrAll = new StringBuilder();
        for (Player player : players) {
            PlayerProcess process = playerProcesses.get(player);
            String stdout = process.getStdout();
            String stderr = process.getStderr();

            currentTurnLogs.put(player.getId(), new PlayerLog(stdout, stderr));
            if (!stdout.isEmpty()) {
                playerStdoutAll.append("=== Player ").append(player.getId()).append(" stdout ===\n");
                playerStdoutAll.append(stdout);
            }
            if (!stderr.isEmpty()) {
                playerStderrAll.append("=== Player ").append(player.getId()).append(" stderr ===\n");
                playerStderrAll.append(stderr);
            }
            process.resetIO();
        }

        String gameLog = outputCapture.toString();
        String playerStderr = playerStderrAll.toString();
        System.setOut(originalOut);
        System.setErr(originalErr);

        System.out.print(gameLog);
        if (!playerStderr.isEmpty() && logging == 1) {
            System.err.print(playerStderr);
        }
    }

    private void sendTurnData(Player player, PlayerProcess process) {
        ActionResult lastResult = lastResults.get(player);
        String line1 = lastResult.toString();
        process.sendLine(line1);
        protocolCapture.append(line1).append("\n");
        logToPlayer(player.getId(), line1);

        String currentCell = maze.getCellInfo(player.getX(), player.getY(), players, player, null, leagueLevel);
        process.sendLine(currentCell);
        protocolCapture.append(currentCell).append("\n");
        logToPlayer(player.getId(), currentCell);

        for (Direction dir : Direction.values()) {
            int nx = player.getX() + dir.getDx();
            int ny = player.getY() + dir.getDy();
            String cellInfo = maze.getCellInfo(nx, ny, players, player, dir, leagueLevel);
            process.sendLine(cellInfo);
            protocolCapture.append(cellInfo).append("\n");
            logToPlayer(player.getId(), cellInfo);
        }
    }

    private void printFinalResults() {
        System.out.println("Final Scores:");

        long activePlayers = players.stream().filter(Player::isActive).count();
        if (activePlayers == 1) {
            Player lastStanding = players.stream().filter(Player::isActive).findFirst().orElse(null);
            if (lastStanding != null) {
                System.out.println("Last Player Standing Bonus: " + lastStanding.getId() + " (+20 points)");
                lastStanding.addScore(20);
            }
        }

        List<Player> sortedPlayers = new ArrayList<>(players);
        sortedPlayers.sort((p1, p2) -> {
            int scoreCompare = Integer.compare(p2.getScore(), p1.getScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Integer.compare(p2.getCollectedForms().size(), p1.getCollectedForms().size());
        });

        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player p = sortedPlayers.get(i);
            String status = p.isFinished() ? "FINISHED"
                    : p.isTimedOut() ? "TIMEOUT" : !p.isActive() ? "INACTIVE" : "ACTIVE";
            System.out.println((i + 1) + ". Player " + p.getId() + ": " +
                    p.getScore() + " points (" + status + ") - Forms: " +
                    p.getCollectedForms().size() + "/" + p.getAssignedForms().size());

        }

        Player winner = referee.getWinner();
        if (winner != null) {
            System.out.println("\nWinner: Player " + winner.getId() + " with " +
                    winner.getScore() + " points!");
        }
        System.out.println("Total Turns: " + referee.getCurrentTurn());
    }

    private void logToPlayer(int playerId, String line) {
        StringBuilder log = playerLogs.get(playerId);
        if (log != null) {
            log.append(line).append("\n");
        }
    }

    private void captureGameState() {
        Cell[][] cellGrid = new Cell[maze.getWidth()][maze.getHeight()];
        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {
                cellGrid[x][y] = maze.getCell(x, y);
            }
        }

        GameState state = new GameState(
                referee.getCurrentTurn(),
                maze.getWidth(),
                maze.getHeight(),
                players,
                cellGrid,
                new HashMap<>(currentTurnLogs));

        gameHistory.add(state);
        currentTurnLogs.clear();
    }

    public List<GameState> getGameHistory() {
        return new ArrayList<>(gameHistory);
    }

    public Maze getMaze() {
        return maze;
    }

    public static class GameConfig {
        public int debug = 0;
        public int turnInfo = 1;
        public int leagueLevel = 5;
        public int maxTurns = 150;
        public int logging = 1;
        public long turnTimeoutMs = 100;
        public long firstTurnTimeoutMs = 1000;
        public int sheetsPerPlayer = 2;
    }
}
