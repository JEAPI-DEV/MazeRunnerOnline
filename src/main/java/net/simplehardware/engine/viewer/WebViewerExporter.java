package net.simplehardware.engine.viewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.simplehardware.engine.viewer.elements.CellSnapshot;
import net.simplehardware.engine.viewer.elements.GameState;
import net.simplehardware.engine.viewer.elements.PlayerLog;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to export game history to JSON format for the web viewer
 */
public class WebViewerExporter {

    /**
     * Export game history to a JSON file for the web viewer
     * 
     * @param gameHistory List of game states
     * @param mazeName    Name of the maze
     * @param outputPath  Path to the output JSON file
     * @throws IOException if file writing fails
     */
    public static void exportToJSON(List<GameState> gameHistory, String mazeName, String outputPath)
            throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("mazeName", mazeName);
        data.put("gameHistory", convertGameHistory(gameHistory));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(data);

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(json);
        }

        System.out.println("Game data exported to: " + outputPath);
    }

    private static List<Map<String, Object>> convertGameHistory(List<GameState> gameHistory) {
        return gameHistory.stream().map(WebViewerExporter::convertGameState).toList();
    }

    private static Map<String, Object> convertGameState(GameState state) {
        Map<String, Object> stateMap = new HashMap<>();
        stateMap.put("turnNumber", state.getTurnNumber());
        stateMap.put("mazeWidth", state.getMazeWidth());
        stateMap.put("mazeHeight", state.getMazeHeight());
        stateMap.put("players", convertPlayers(state.getPlayers()));
        stateMap.put("cells", convertCells(state.getCells()));
        stateMap.put("playerLogs", convertPlayerLogs(state.getPlayerLogs()));

        return stateMap;
    }

    private static Map<Integer, Map<String, Object>> convertPlayers(Map<Integer, GameState.PlayerSnapshot> players) {
        Map<Integer, Map<String, Object>> result = new HashMap<>();

        for (Map.Entry<Integer, GameState.PlayerSnapshot> entry : players.entrySet()) {
            GameState.PlayerSnapshot player = entry.getValue();
            Map<String, Object> playerMap = new HashMap<>();

            playerMap.put("id", player.getId());
            playerMap.put("x", player.getX());
            playerMap.put("y", player.getY());
            playerMap.put("score", player.getScore());
            playerMap.put("formsCollected", player.getFormsCollected());
            playerMap.put("formsRequired", player.getFormsRequired());
            playerMap.put("active", player.isActive());
            playerMap.put("finished", player.isFinished());

            result.put(entry.getKey(), playerMap);
        }

        return result;
    }

    private static CellData[][] convertCells(CellSnapshot[][] cells) {
        int width = cells.length;
        int height = cells[0].length;
        CellData[][] result = new CellData[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result[x][y] = convertCell(cells[x][y]);
            }
        }

        return result;
    }

    private static CellData convertCell(CellSnapshot cell) {
        CellData data = new CellData();
        data.type = cell.getType().name();
        data.x = cell.getX();
        data.y = cell.getY();
        data.form = cell.getForm();
        data.formOwner = cell.getFormOwner();
        data.hasSheet = cell.hasSheet();
        data.finishPlayerId = cell.getFinishPlayerId();

        return data;
    }

    private static Map<Integer, Map<String, String>> convertPlayerLogs(Map<Integer, PlayerLog> playerLogs) {
        Map<Integer, Map<String, String>> result = new HashMap<>();

        for (Map.Entry<Integer, PlayerLog> entry : playerLogs.entrySet()) {
            PlayerLog log = entry.getValue();
            Map<String, String> logMap = new HashMap<>();
            logMap.put("stdout", log.getStdout());
            logMap.put("stderr", log.getStderr());

            result.put(entry.getKey(), logMap);
        }

        return result;
    }

    /**
     * Helper class for JSON serialization of cell data
     */
    private static class CellData {
        String type;
        int x;
        int y;
        Character form;
        Integer formOwner;
        boolean hasSheet;
        Integer finishPlayerId;
    }
}
