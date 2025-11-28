package net.simplehardware.engine.players;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Represents a player in the game with position, inventory, and state
 */
public class Player {
    private final int id;
    private int x;
    private int y;
    private final Stack<Character> collectedForms;
    private final Stack<Integer> sheets;
    private boolean talking; // Penalty state when players meet
    private boolean taking; // Animation state during sheet pickup
    private boolean finished;
    private boolean active;
    private boolean timedOut;
    private int score;

    private final List<Character> assignedForms; // Forms this player needs to collect
    private final int startX;
    private final int startY;

    public Player(int id, int startX, int startY, int initialSheets) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.startX = startX;
        this.startY = startY;
        this.collectedForms = new Stack<>();
        this.sheets = new Stack<>();
        this.assignedForms = new ArrayList<>();
        this.active = true;
        this.finished = false;
        this.talking = false;
        this.taking = false;
        this.score = 0;

        // Initialize sheets
        for (int i = 0; i < initialSheets; i++) {
            sheets.push(i);
        }
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public Stack<Character> getCollectedForms() {
        return collectedForms;
    }

    public void addForm(char form) {
        collectedForms.push(form);
        score += 10; // +10 points per form
    }

    public Character getNextExpectedForm() {
        if (assignedForms.isEmpty()) {
            return null;
        }
        int nextIndex = collectedForms.size();
        if (nextIndex >= assignedForms.size()) {
            return null;
        }
        return assignedForms.get(nextIndex);
    }

    public boolean hasCollectedAllForms() {
        return collectedForms.size() == assignedForms.size();
    }

    public List<Character> getAssignedForms() {
        return assignedForms;
    }

    public void addAssignedForm(char form) {
        assignedForms.add(form);
    }

    public Stack<Integer> getSheets() {
        return sheets;
    }

    public boolean hasSheets() {
        return !sheets.isEmpty();
    }

    public void addSheet() {
        sheets.push(sheets.size());
    }

    public void removeSheet() {
        if (!sheets.isEmpty()) {
            sheets.pop();
        }
    }

    public boolean isTalking() {
        return talking;
    }

    public void setTalking(boolean talking) {
        this.talking = talking;
    }

    public boolean isTaking() {
        return taking;
    }

    public void setTaking(boolean taking) {
        this.taking = taking;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            score += 100; // +100 points for completion
        }
    }

    public boolean isActive() {
        return active && !finished && !timedOut;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        score = Math.max(0, score + points);
    }
}
