package net.simplehardware.engine.core;

import net.simplehardware.engine.cells.Cell;
import net.simplehardware.engine.cells.FloorCell;
import net.simplehardware.engine.cells.FinishCell;
import net.simplehardware.engine.game.ActionName;
import net.simplehardware.engine.game.ActionResult;
import net.simplehardware.engine.game.Direction;
import net.simplehardware.engine.game.Maze;
import net.simplehardware.engine.players.Player;

import java.util.List;

/**
 * Game referee that enforces rules and processes player actions
 */
public class Referee {
    private final Maze maze;
    private final List<Player> players;
    private final int leagueLevel;
    private int currentTurn;

    public Referee(Maze maze, List<Player> players, int leagueLevel) {
        this.maze = maze;
        this.players = players;
        this.leagueLevel = leagueLevel;
        this.currentTurn = 0;
    }

    public ActionResult processAction(Player player, String actionLine) {
        if (!player.isActive()) {
            return ActionResult.fail("INACTIVE");
        }

        // Check if player is talking (penalty turn)
        if (player.isTalking()) {
            player.addScore(-5);
            return ActionResult.fail("TALKING");
        }

        String[] parts = actionLine.trim().split("\\s+");
        if (parts.length == 0) {
            return ActionResult.fail("INVALID");
        }

        try {
            ActionName action = ActionName.valueOf(parts[0].toUpperCase());

            return switch (action) {
                case GO -> handleGo(player, parts);
                case POSITION -> handlePosition(player);
                case TAKE -> handleTake(player);
                case KICK -> handleKick(player, parts);
                case PUT -> handlePut(player);
                case FINISH -> handleFinish(player);
            };
        } catch (IllegalArgumentException e) {
            return ActionResult.fail("INVALID");
        }
    }

    private ActionResult handleGo(Player player, String[] parts) {
        if (parts.length < 2) {
            return ActionResult.fail("INVALID");
        }

        if (player.isTalking()) {
            return ActionResult.fail("TALKING");
        }

        if (player.isTaking()) {
            return ActionResult.fail("TAKING");
        }

        try {
            Direction direction = Direction.fromString(parts[1]);
            int newX = player.getX() + direction.getDx();
            int newY = player.getY() + direction.getDy();

            Cell targetCell = maze.getCell(newX, newY);
            if (targetCell == null || !targetCell.isWalkable()) {
                return ActionResult.fail("BLOCKED");
            }

            // Move the player
            player.setPosition(newX, newY);

            // Collision check moved to updateTurn to handle swapping correctly

            return ActionResult.ok(direction.name());
        } catch (IllegalArgumentException e) {
            return ActionResult.fail("INVALID");
        }
    }

    private ActionResult handlePosition(Player player) {
        return ActionResult.ok(player.getX() + " " + player.getY());
    }

    private ActionResult handleTake(Player player) {
        if (player.isTalking()) {
            return ActionResult.fail("TALKING");
        }

        if (player.isTaking()) {
            return ActionResult.fail("TAKING");
        }

        Cell cell = maze.getCell(player.getX(), player.getY());
        if (!(cell instanceof FloorCell)) {
            return ActionResult.fail("EMPTY");
        }

        FloorCell floor = (FloorCell) cell;

        // Try to take a sheet (Level 5+) - Priority over Form
        if (leagueLevel >= 5 && floor.hasSheet()) {
            player.addSheet();
            floor.setSheet(false);
            player.setTaking(true);
            return ActionResult.ok("SHEET");
        }

        // Try to take a form (Level 2+)
        if (leagueLevel >= 2 && floor.getForm() != null) {
            char form = floor.getForm();
            int formOwner = floor.getFormOwner();

            if (formOwner != player.getId()) {
                return ActionResult.fail("NOTYOURS");
            }

            Character expectedForm = player.getNextExpectedForm();
            if (expectedForm == null || form != expectedForm) {
                return ActionResult.fail("WRONGORDER");
            }

            player.addForm(form);
            floor.removeForm();
            return ActionResult.ok("FORM");
        }

        return ActionResult.fail("EMPTY");
    }

    private ActionResult handleKick(Player player, String[] parts) {
        if (leagueLevel < 4) {
            return ActionResult.fail("NOTSUPPORTED");
        }

        if (parts.length < 2) {
            return ActionResult.fail("INVALID");
        }

        if (player.isTalking()) {
            return ActionResult.fail("TALKING");
        }

        if (player.isTaking()) {
            return ActionResult.fail("TAKING");
        }

        try {
            Direction direction = Direction.fromString(parts[1]);
            Cell currentCell = maze.getCell(player.getX(), player.getY());
            if (!(currentCell instanceof FloorCell)) {
                return ActionResult.fail("EMPTY");
            }

            FloorCell floor = (FloorCell) currentCell;
            int targetX = player.getX() + direction.getDx();
            int targetY = player.getY() + direction.getDy();
            Cell targetCell = maze.getCell(targetX, targetY);

            if (targetCell == null || !(targetCell instanceof FloorCell)) {
                return ActionResult.fail("BLOCKED");
            }

            FloorCell targetFloor = (FloorCell) targetCell;

            // Kick sheet (Level 5+) - Priority over Form
            if (leagueLevel >= 5 && floor.hasSheet()) {
                if (targetFloor.hasSheet()) {
                    return ActionResult.fail("BLOCKED");
                }
                targetFloor.setSheet(true);
                floor.setSheet(false);
                return ActionResult.ok(direction.name());
            }

            // Kick form (Level 4+)
            if (floor.getForm() != null) {
                if (targetFloor.getForm() != null) {
                    return ActionResult.fail("BLOCKED");
                }
                targetFloor.setForm(floor.getForm(), floor.getFormOwner());
                floor.removeForm();
                return ActionResult.ok(direction.name());
            }

            return ActionResult.fail("EMPTY");
        } catch (IllegalArgumentException e) {
            return ActionResult.fail("INVALID");
        }
    }

    private ActionResult handlePut(Player player) {
        if (leagueLevel < 5) {
            return ActionResult.fail("NOTSUPPORTED");
        }

        if (player.isTalking()) {
            return ActionResult.fail("TALKING");
        }

        if (player.isTaking()) {
            return ActionResult.fail("TAKING");
        }

        if (!player.hasSheets()) {
            return ActionResult.fail("EMPTY");
        }

        Cell cell = maze.getCell(player.getX(), player.getY());
        if (!(cell instanceof FloorCell)) {
            return ActionResult.fail("BLOCKED");
        }

        FloorCell floor = (FloorCell) cell;
        if (floor.hasSheet()) {
            return ActionResult.fail("BLOCKED");
        }

        player.removeSheet();
        floor.setSheet(true);
        return ActionResult.ok("");
    }

    private ActionResult handleFinish(Player player) {
        if (!player.hasCollectedAllForms()) {
            return ActionResult.fail("FORM");
        }

        Cell cell = maze.getCell(player.getX(), player.getY());
        if (!(cell instanceof FinishCell)) {
            return ActionResult.fail("BLOCKED");
        }

        FinishCell finish = (FinishCell) cell;
        if (finish.getPlayerId() != player.getId()) {
            return ActionResult.fail("NOTYOURS");
        }

        player.setFinished(true);
        return ActionResult.ok("");
    }

    public void updateTurn() {
        currentTurn++;

        // Check for collisions (Level 3+)
        // Match reference logic: Toggle talking state based on collisions
        if (leagueLevel >= 3) {
            for (Player p : players) {
                if (!p.isActive())
                    continue;

                if (p.isTalking()) {
                    p.setTalking(false);
                } else {
                    boolean collision = false;
                    for (Player other : players) {
                        if (other.getId() != p.getId() && other.isActive() &&
                                other.getX() == p.getX() && other.getY() == p.getY()) {
                            collision = true;
                            break;
                        }
                    }
                    if (collision) {
                        p.setTalking(true);
                    }
                }
            }
        }

        // Reset temporary states
        for (Player player : players) {
            if (player.isActive()) {
                player.setTaking(false);
            }
        }
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver(int maxTurns) {
        if (currentTurn >= maxTurns) {
            return true;
        }

        // Check if any active player has finished
        for (Player player : players) {
            if (player.isFinished()) {
                return true;
            }
        }

        // Check if no players are still active
        long activePlayers = players.stream().filter(Player::isActive).count();
        return activePlayers == 0;
    }

    public Player getWinner() {
        // Find player with highest score
        // Tie-breaker: Number of collected forms

        List<Player> sortedPlayers = new java.util.ArrayList<>(players);
        sortedPlayers.sort((p1, p2) -> {
            int scoreCompare = Integer.compare(p2.getScore(), p1.getScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Integer.compare(p2.getCollectedForms().size(), p1.getCollectedForms().size());
        });

        if (sortedPlayers.isEmpty())
            return null;

        // Check if there's a unique winner or a tie for first place
        if (sortedPlayers.size() > 1) {
            Player first = sortedPlayers.get(0);
            Player second = sortedPlayers.get(1);
            if (first.getScore() == second.getScore() &&
                    first.getCollectedForms().size() == second.getCollectedForms().size()) {
                return null; // Tie
            }
        }

        return sortedPlayers.get(0);
    }
}
