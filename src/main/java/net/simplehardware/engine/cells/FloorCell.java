package net.simplehardware.engine.cells;

/**
 * Walkable floor cell that can contain forms, sheets, or be empty
 */
public class FloorCell extends Cell {
    private Character form;
    private Integer formOwner;
    private boolean hasSheet;

    public FloorCell(int x, int y) {
        super(x, y);
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public String getCellType() {
        if (form != null) {
            return "FORM";
        } else if (hasSheet) {
            return "SHEET";
        }
        return "FLOOR";
    }

    @Override
    public String getCellDetails() {
        if (form != null && formOwner != null) {
            return formOwner + " " + (form - 'A' + 1);
        }
        return "";
    }

    public Character getForm() {
        return form;
    }

    public void setForm(Character form, int playerId) {
        this.form = form;
        this.formOwner = playerId;
    }

    public void removeForm() {
        this.form = null;
        this.formOwner = null;
    }

    public Integer getFormOwner() {
        return formOwner;
    }

    public boolean hasSheet() {
        return hasSheet;
    }

    public void setSheet(boolean hasSheet) {
        this.hasSheet = hasSheet;
    }
}
