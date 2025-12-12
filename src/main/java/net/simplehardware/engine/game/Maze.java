package net.simplehardware.engine.game;

import net.simplehardware.engine.cells.Cell;
import net.simplehardware.engine.cells.FloorCell;
import net.simplehardware.engine.cells.FinishCell;
import net.simplehardware.engine.cells.WallCell;
import net.simplehardware.engine.players.Player;
import net.simplehardware.models.FormInfo;
import net.simplehardware.models.MazeInfoData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the maze structure and layout
 */
public class Maze {
    private final int width;
    private final int height;
    private final Cell[][] cells;
    private final String name;
    private final List<FormInfo> forms;
    private final Map<Integer, int[]> startPositions = new HashMap<>();

    public Maze(MazeInfoData data) {
        this.name = data.name;
        this.forms = data.forms != null ? data.forms : new ArrayList<>();

        String[] rows = data.maze.split("/");
        this.height = rows.length;
        this.width = rows[0].length() / 2; // Each cell is 2 characters
        this.cells = new Cell[width][height];

        parseMaze(rows);
    }

    private void parseMaze(String[] rows) {
        for (int y = 0; y < height; y++) {
            String row = rows[y];
            for (int x = 0; x < width; x++) {
                int charIndex = x * 2;
                if (charIndex >= row.length())
                    break;

                char cellType = row.charAt(charIndex);
                char cellData = row.charAt(charIndex + 1);
                int playerId = Character.isDigit(cellData) ? cellData - '0' : 0;

                Cell cell = createCell(x, y, cellType, playerId);
                cells[x][y] = cell;
                if (cellType == '@') {
                    startPositions.put(playerId, new int[] { x, y });
                }
            }
        }
    }

    private Cell createCell(int x, int y, char type, int playerId) {
        return switch (type) {
            case '#' -> new WallCell(x, y);
            case '@' -> {
                startPositions.put(playerId, new int[] { x, y });
                yield new FloorCell(x, y);
            }
            case '!' -> new FinishCell(x, y, playerId);
            case 'S' -> {
                FloorCell floor = new FloorCell(x, y);
                floor.setSheet(true);
                yield floor;
            }
            default -> {
                if (Character.isLetter(type) && Character.isUpperCase(type)) {
                    FloorCell floor = new FloorCell(x, y);
                    floor.setForm(type, playerId);
                    yield floor;
                }
                yield new FloorCell(x, y);
            }
        };
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return cells[x][y];
    }

    public int[] getStartPosition(int playerId) {
        return startPositions.get(playerId);
    }

    public void setStartPosition(int playerId, int x, int y) {
        startPositions.put(playerId, new int[] { x, y });
    }

    public String getName() {
        return name;
    }

    public List<FormInfo> getForms() {
        return forms;
    }

    /**
     * Get cell information including opponent proximity
     * 
     * @param dir The direction we are looking in (null for current cell)
     */
    public String getCellInfo(int x, int y, List<Player> allPlayers, Player currentPlayer, Direction dir,
            int leagueLevel) {
        Cell cell = getCell(x, y);
        if (cell == null) {
            return "WALL";
        }

        StringBuilder info = new StringBuilder(cell.getCellType());
        String details = cell.getCellDetails();
        if (!details.isEmpty()) {
            info.append(" ").append(details);
        }

        // Add opponent proximity indicator (Level 3+)
        if (leagueLevel >= 3) {
            if (hasOpponent(x, y, allPlayers, currentPlayer)) {
                info.append(" !");
            } else if (dir != null) {
                int distance = findOpponentInDirection(x, y, dir, allPlayers, currentPlayer);
                if (distance > 0) { info.append(" !").append(distance); }
            }
        }
        return info.toString();
    }

    private boolean hasOpponent(int x, int y, List<Player> allPlayers, Player currentPlayer) {
        for (Player player : allPlayers) {
            if (player.getId() != currentPlayer.getId() && player.isActive() &&
                    player.getX() == x && player.getY() == y) {
                return true;
            }
        }
        return false;
    }

    private int findOpponentInDirection(int startX, int startY, Direction dir, List<Player> allPlayers,
            Player currentPlayer) {
        int dx = dir.getDx();
        int dy = dir.getDy();

        int checkX = startX + dx;
        int checkY = startY + dy;
        int distance = 1;

        while (checkX >= 0 && checkX < width && checkY >= 0 && checkY < height) {
            Cell cell = cells[checkX][checkY];
            if (cell instanceof WallCell) {
                break;
            }
            if (hasOpponent(checkX, checkY, allPlayers, currentPlayer)) return distance;
            checkX += dx;
            checkY += dy;
            distance++;
        }

        return 0; // No opponent found
    }

    /**
     * Update finish cells with required form counts based on player assignments
     */
    public void updateFinishCells(List<Player> players) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = cells[x][y];
                if (cell instanceof FinishCell finishCell) {
                    int playerId = finishCell.getPlayerId();
                    for (Player player : players) {
                        if (player.getId() == playerId) {
                            finishCell.setRequiredFormCount(player.getAssignedForms().size());
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove forms and finish cells for players that aren't loaded
     */
    public void removeUnusedPlayerCells(List<Player> players) {
        java.util.Set<Integer> loadedPlayerIds = new java.util.HashSet<>();
        for (Player player : players) {
            loadedPlayerIds.add(player.getId());
        }

        System.out.println("Loaded player IDs: " + loadedPlayerIds);
        int formsRemoved = 0;
        int finishRemoved = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = cells[x][y];

                if (cell instanceof FinishCell finishCell) {
                    if (!loadedPlayerIds.contains(finishCell.getPlayerId())) {
                        cells[x][y] = new FloorCell(x, y);
                        finishRemoved++;
                    }
                }

                else if (cell instanceof FloorCell floor) {
                    if (floor.getForm() != null && floor.getFormOwner() != null) {
                        if (!loadedPlayerIds.contains(floor.getFormOwner())) {
                            System.out.println("Removing form " + floor.getForm() + " for player "
                                    + floor.getFormOwner() + " at (" + x + "," + y + ")");
                            floor.removeForm();
                            formsRemoved++;
                        }
                    }
                }
            }
        }
        System.out.println(
                "Removed " + formsRemoved + " forms and " + finishRemoved + " finish cells for unloaded players");

        int remainingForms = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = cells[x][y];
                if (cell instanceof FloorCell floor) {
                    if (floor.getForm() != null) {
                        System.out.println("Remaining form: " + floor.getForm() + " for player " + floor.getFormOwner()
                                + " at (" + x + "," + y + ")");
                        remainingForms++;
                    }
                }
            }
        }
        System.out.println("Total remaining forms: " + remainingForms);
    }

    /**
     * Apply level-specific restrictions to the maze
     */
    public void applyLevelRestrictions(int level) {
        if (level == 1) {
            System.out.println("Applying Level 1 restrictions: Removing all forms");
            int formsRemoved = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Cell cell = cells[x][y];
                    if (cell instanceof FloorCell floor) {
                        if (floor.getForm() != null) {
                            floor.removeForm();
                            formsRemoved++;
                        }
                    }
                }
            }
            System.out.println("Removed " + formsRemoved + " forms for Level 1");
        }
    }
}
