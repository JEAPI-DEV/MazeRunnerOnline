package net.simplehardware.engine.cells;

public class WallCell extends Cell {
    public WallCell(int x, int y) {
        super(x, y);
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public String getCellType() {
        return "WALL";
    }
}
