package net.simplehardware.engine.cells;

public abstract class Cell {
    protected final int x;
    protected final int y;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Check if this cell is walkable
     */
    public abstract boolean isWalkable();

    /**
     * Get the cell type name for protocol output
     */
    public abstract String getCellType();

    /**
     * Get detailed cell information for protocol output
     */
    public String getCellDetails() {
        return "";
    }
}
