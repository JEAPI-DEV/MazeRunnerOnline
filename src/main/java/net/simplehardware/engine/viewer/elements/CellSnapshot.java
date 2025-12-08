package net.simplehardware.engine.viewer.elements;

import net.simplehardware.engine.cells.Cell;
import net.simplehardware.engine.cells.FloorCell;
import net.simplehardware.engine.cells.FinishCell;
import net.simplehardware.engine.cells.WallCell;

import java.io.Serial;
import java.io.Serializable;

/**
 * Snapshot of a cell's state
 */
public record CellSnapshot(CellType type, int x, int y, Character form, Integer formOwner, boolean hasSheet,
                           Integer finishPlayerId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum CellType {
        WALL, FLOOR, FINISH
    }

    public static CellSnapshot fromCell(Cell cell) {
        if (cell instanceof WallCell) {
            return new CellSnapshot(CellType.WALL, cell.getX(), cell.getY(),
                    null, null, false, null);
        } else if (cell instanceof FinishCell finish) {
            return new CellSnapshot(CellType.FINISH, cell.getX(), cell.getY(),
                    null, null, false, finish.getPlayerId());
        } else if (cell instanceof FloorCell floor) {
            return new CellSnapshot(CellType.FLOOR, cell.getX(), cell.getY(),
                    floor.getForm(), floor.getFormOwner(),
                    floor.hasSheet(), null);
        }
        // Default to floor
        return new CellSnapshot(CellType.FLOOR, cell.getX(), cell.getY(),
                null, null, false, null);
    }
}
