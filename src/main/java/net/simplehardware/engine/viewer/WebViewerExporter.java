package net.simplehardware.engine.viewer;

import com.google.gson.Gson;
import net.simplehardware.engine.viewer.elements.CellSnapshot;
import net.simplehardware.engine.viewer.elements.GameState;
import net.simplehardware.engine.viewer.elements.PlayerLog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to export game history to compact JSON format with delta
 * encoding
 */
public class WebViewerExporter {

    /**
     * Export game history to a compact JSON file with delta encoding
     * 
     * @param gameHistory List of game states
     * @param mazeName    Name of the maze
     * @param outputPath  Path to the output JSON file
     * @throws IOException if file writing fails
     */
    public static void exportToJSON(List<GameState> gameHistory, String mazeName, String outputPath)
            throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("n", mazeName);
        data.put("h", convertGameHistoryWithDeltas(gameHistory));

        Gson gson = new Gson();
        String json = gson.toJson(data);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(json);
            writer.flush();
        }

        System.out.println("Game data exported to: " + outputPath + " (" + json.length() + " bytes)");
    }

    private static List<Object> convertGameHistoryWithDeltas(List<GameState> gameHistory) {
        List<Object> result = new ArrayList<>();

        if (gameHistory.isEmpty()) {
            return result;
        }

        GameState firstState = gameHistory.get(0);
        result.add(convertGameStateFull(firstState));

        for (int i = 1; i < gameHistory.size(); i++) {
            GameState prevState = gameHistory.get(i - 1);
            GameState currState = gameHistory.get(i);
            result.add(convertGameStateDelta(prevState, currState));
        }

        return result;
    }

    private static Object[] convertGameStateFull(GameState state) {
        // Full state: [turnNumber, width, height, players, cells, logs]
        return new Object[] {
                state.getTurnNumber(),
                state.getMazeWidth(),
                state.getMazeHeight(),
                convertPlayers(state.getPlayers()),
                convertCells(state.getCells()),
                convertPlayerLogs(state.getPlayerLogs())
        };
    }

    private static Object convertGameStateDelta(GameState prevState, GameState currState) {
        // Delta state: [turnNumber, playerDeltas, cellDeltas, logDeltas]
        Map<String, Object> delta = new HashMap<>();
        delta.put("t", currState.getTurnNumber());

        // Only include changed players
        Map<Integer, Object[]> playerDeltas = getPlayerDeltas(prevState.getPlayers(), currState.getPlayers());
        if (!playerDeltas.isEmpty()) {
            delta.put("p", playerDeltas);
        }

        // Only include changed cells
        List<Object[]> cellDeltas = getCellDeltas(prevState.getCells(), currState.getCells());
        if (!cellDeltas.isEmpty()) {
            delta.put("c", cellDeltas);
        }

        // Only include changed logs
        Map<Integer, String[]> logDeltas = getLogDeltas(prevState.getPlayerLogs(), currState.getPlayerLogs());
        if (!logDeltas.isEmpty()) {
            delta.put("l", logDeltas);
        }

        return delta;
    }

    private static Map<Integer, Object[]> getPlayerDeltas(
            Map<Integer, GameState.PlayerSnapshot> prev,
            Map<Integer, GameState.PlayerSnapshot> curr) {
        Map<Integer, Object[]> deltas = new HashMap<>();

        for (Map.Entry<Integer, GameState.PlayerSnapshot> entry : curr.entrySet()) {
            int id = entry.getKey();
            GameState.PlayerSnapshot currPlayer = entry.getValue();
            GameState.PlayerSnapshot prevPlayer = prev.get(id);

            if (prevPlayer == null || !playersEqual(prevPlayer, currPlayer)) {
                deltas.put(id, new Object[] {
                        currPlayer.getId(),
                        currPlayer.getX(),
                        currPlayer.getY(),
                        currPlayer.getScore(),
                        currPlayer.getFormsCollected(),
                        currPlayer.getFormsRequired(),
                        currPlayer.isActive() ? 1 : 0,
                        currPlayer.isFinished() ? 1 : 0
                });
            }
        }

        return deltas;
    }

    private static boolean playersEqual(GameState.PlayerSnapshot p1, GameState.PlayerSnapshot p2) {
        return p1.getX() == p2.getX() &&
                p1.getY() == p2.getY() &&
                p1.getScore() == p2.getScore() &&
                p1.getFormsCollected() == p2.getFormsCollected() &&
                p1.isActive() == p2.isActive() &&
                p1.isFinished() == p2.isFinished();
    }

    private static List<Object[]> getCellDeltas(CellSnapshot[][] prev, CellSnapshot[][] curr) {
        List<Object[]> deltas = new ArrayList<>();
        int width = curr.length;
        int height = curr[0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                String prevCell = convertCell(prev[x][y]);
                String currCell = convertCell(curr[x][y]);

                if (!prevCell.equals(currCell)) {
                    // Delta format: [x, y, cellString]
                    deltas.add(new Object[] { x, y, currCell });
                }
            }
        }

        return deltas;
    }

    private static Map<Integer, String[]> getLogDeltas(
            Map<Integer, PlayerLog> prev,
            Map<Integer, PlayerLog> curr) {
        Map<Integer, String[]> deltas = new HashMap<>();

        for (Map.Entry<Integer, PlayerLog> entry : curr.entrySet()) {
            int id = entry.getKey();
            PlayerLog currLog = entry.getValue();
            PlayerLog prevLog = prev.get(id);

            String currStdout = currLog.getStdout();
            String currStderr = currLog.getStderr();
            String prevStdout = prevLog != null ? prevLog.getStdout() : "";
            String prevStderr = prevLog != null ? prevLog.getStderr() : "";

            if (!currStdout.equals(prevStdout) || !currStderr.equals(prevStderr)) {
                if ((currStdout != null && !currStdout.isEmpty()) ||
                        (currStderr != null && !currStderr.isEmpty())) {
                    deltas.put(id, new String[] { currStdout, currStderr });
                }
            }
        }

        return deltas;
    }

    private static Map<Integer, Object[]> convertPlayers(Map<Integer, GameState.PlayerSnapshot> players) {
        Map<Integer, Object[]> result = new HashMap<>();

        for (Map.Entry<Integer, GameState.PlayerSnapshot> entry : players.entrySet()) {
            GameState.PlayerSnapshot p = entry.getValue();
            result.put(entry.getKey(), new Object[] {
                    p.getId(),
                    p.getX(),
                    p.getY(),
                    p.getScore(),
                    p.getFormsCollected(),
                    p.getFormsRequired(),
                    p.isActive() ? 1 : 0,
                    p.isFinished() ? 1 : 0
            });
        }

        return result;
    }

    private static String[][] convertCells(CellSnapshot[][] cells) {
        int width = cells.length;
        int height = cells[0].length;
        String[][] result = new String[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result[x][y] = convertCell(cells[x][y]);
            }
        }

        return result;
    }

    private static String convertCell(CellSnapshot cell) {
        StringBuilder sb = new StringBuilder();

        switch (cell.getType()) {
            case WALL:
                sb.append('W');
                break;
            case FINISH:
                sb.append('N');
                break;
            case FLOOR:
            default:
                sb.append('F');
                break;
        }

        if (cell.getForm() != null) {
            sb.append(',').append(cell.getForm());
            if (cell.getFormOwner() != null) {
                sb.append(',').append(cell.getFormOwner());
            } else {
                sb.append(',');
            }
        }

        if (cell.hasSheet()) {
            if (cell.getForm() == null)
                sb.append(",,");
            sb.append(",S");
        }

        if (cell.getFinishPlayerId() != null) {
            if (cell.getForm() == null && !cell.hasSheet())
                sb.append(",,");
            if (!cell.hasSheet())
                sb.append(',');
            sb.append(",F:").append(cell.getFinishPlayerId());
        }

        return sb.toString();
    }

    private static Map<Integer, String[]> convertPlayerLogs(Map<Integer, PlayerLog> playerLogs) {
        Map<Integer, String[]> result = new HashMap<>();

        for (Map.Entry<Integer, PlayerLog> entry : playerLogs.entrySet()) {
            PlayerLog log = entry.getValue();
            String stdout = log.getStdout();
            String stderr = log.getStderr();
            if ((stdout != null && !stdout.isEmpty()) || (stderr != null && !stderr.isEmpty())) {
                result.put(entry.getKey(), new String[] { stdout, stderr });
            }
        }

        return result;
    }
}
