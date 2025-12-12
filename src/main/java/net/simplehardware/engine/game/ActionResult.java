package net.simplehardware.engine.game;

/**
 * Represents the result of an action
 */
public record ActionResult(boolean success, String details) {

    public static ActionResult ok(String details) {
        return new ActionResult(true, details);
    }

    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }

    @Override
    public String toString() {
        return (success ? "OK" : "NOK") + (details != null && !details.isEmpty() ? " " + details : "");
    }
}
