package net.simplehardware.engine.cells;

public class FinishCell extends Cell {
    private final int playerId;
    private int requiredFormCount;

    public FinishCell(int x, int y, int playerId) {
        super(x, y);
        this.playerId = playerId;
        this.requiredFormCount = 0;
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public String getCellType() {
        return "FINISH";
    }

    @Override
    public String getCellDetails() {
        return playerId + " " + requiredFormCount;
    }

    public int getPlayerId() {
        return playerId;
    }
    
    public void setRequiredFormCount(int count) {
        this.requiredFormCount = count;
    }
}
