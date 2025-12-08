package net.simplehardware.engine.viewer.elements;

import net.simplehardware.engine.cells.Cell;
import net.simplehardware.engine.players.Player;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of game state at a specific turn
 */
public class GameState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int turnNumber;
    private final int mazeWidth;
    private final int mazeHeight;
    private final Map<Integer, PlayerSnapshot> players;
    private final CellSnapshot[][] cells;
    private final Map<Integer, PlayerLog> playerLogs;

    public GameState(int turnNumber, int mazeWidth, int mazeHeight,
            List<Player> playerList, Cell[][] cellGrid, Map<Integer, PlayerLog> playerLogs) {
        this.turnNumber = turnNumber;
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
        this.players = new HashMap<>();

        // Snapshot players
        for (Player player : playerList) {
            players.put(player.getId(), new PlayerSnapshot(
                    player.getId(),
                    player.getX(),
                    player.getY(),
                    player.getScore(),
                    player.getCollectedForms().size(),
                    player.getAssignedForms().size(),
                    player.isActive(),
                    player.isFinished()));
        }

        // Snapshot cells
        this.cells = new CellSnapshot[mazeWidth][mazeHeight];
        for (int x = 0; x < mazeWidth; x++) {
            for (int y = 0; y < mazeHeight; y++) {
                cells[x][y] = CellSnapshot.fromCell(cellGrid[x][y]);
            }
        }

        // Store player logs
        this.playerLogs = playerLogs != null ? new HashMap<>(playerLogs) : new HashMap<>();
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public int getMazeWidth() {
        return mazeWidth;
    }

    public int getMazeHeight() {
        return mazeHeight;
    }

    public Map<Integer, PlayerSnapshot> getPlayers() {
        return players;
    }

    public CellSnapshot[][] getCells() {
        return cells;
    }

    public Map<Integer, PlayerLog> getPlayerLogs() {
        return playerLogs;
    }

    public record PlayerSnapshot(int id, int x, int y, int score, int formsCollected, int formsRequired, boolean active,
                                 boolean finished) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;

    }
}
