package net.simplehardware.engine.game;

/**
 * Represents the four cardinal directions in the maze
 */
public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public boolean isOpositeTo(Direction dir) {
        if (dir == null)
            return false;
        return this.dx + dir.dx == 0 && this.dy + dir.dy == 0;
    }

    public static Direction fromString(String dir) {
        return valueOf(dir.toUpperCase());
    }
}
