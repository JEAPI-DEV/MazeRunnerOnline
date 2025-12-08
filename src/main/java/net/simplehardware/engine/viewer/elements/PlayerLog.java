package net.simplehardware.engine.viewer.elements;

import java.io.Serial;
import java.io.Serializable;

/**
 * Player log data for a specific turn
 */
public record PlayerLog(String stdout, String stderr) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public PlayerLog(String stdout, String stderr) {
        this.stdout = stdout != null ? stdout : "";
        this.stderr = stderr != null ? stderr : "";
    }
}
