package net.simplehardware.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.simplehardware.MazeEditor;
import net.simplehardware.MazeGrid;
import net.simplehardware.dialogs.MessageDialog;
import net.simplehardware.models.CellButton;
import net.simplehardware.models.Mode;
import net.simplehardware.models.FormInfo;
import net.simplehardware.models.MazeInfoData;

public class MazeIO {

    public static void loadFromJson(
        MazeEditor editor,
        MazeGrid grid,
        JSpinner gridSizeSpinner
    ) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Maze JSON to Load");
        if (chooser.showOpenDialog(editor) == JFileChooser.APPROVE_OPTION) {
            try (Reader reader = new FileReader(chooser.getSelectedFile())) {
                MazeInfoData data = new Gson().fromJson(
                    reader,
                    MazeInfoData.class
                );
                applyMazeData(grid, data, gridSizeSpinner);
                new MessageDialog(
                editor,
                    "Maze loaded successfully!",
                    "Load Complete",
                    JOptionPane.INFORMATION_MESSAGE
                            ).show();
            } catch (Exception ex) {
                new MessageDialog(
                    editor,
                    "Failed to load: " + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
                ).show();
            }
        }
    }

    private static void applyMazeData(
        MazeGrid grid,
        MazeInfoData data,
        JSpinner spinner
    ) {
        if (data.maze == null) return;
        String[] rows = data.maze.split("/");
        int numRows = rows.length;
        int numCols = rows[0].length() / 2;  // Each cell is 2 characters (type + placeholder/playerid)
        int gridSize = Math.max(numRows, numCols);
        grid.resizeGrid(gridSize);
        spinner.setValue(gridSize);

        CellButton[][] cells = grid.getCells();
        for (int y = 0; y < numRows && y < gridSize; y++) {
            String row = rows[y];
            int rowLength = row.length() / 2;

            for (int x = 0; x < rowLength && x < gridSize; x++) {
                if (2 * x + 1 >= row.length()) break;
                char chType = row.charAt(2 * x);
                char chOwner = row.charAt(2 * x + 1);
                int pid = Character.isDigit(chOwner) ? chOwner - '0' : 0;
                Mode mode = Mode.fromChar(chType);
                cells[x][y].setMode(mode, pid);
            }
        }
    }

    public static void exportJson(MazeEditor editor, MazeGrid grid) {
        String mazeName = JOptionPane.showInputDialog(
            editor,
            "Enter Maze Name:"
        );
        if (mazeName == null) return;

        List<String> lines = new ArrayList<>();
        Set<Character> formsFound = new HashSet<>();
        for (int y = 0; y < grid.getCells().length; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < grid.getCells().length; x++) {
                CellButton c = grid.getCells()[x][y];
                Mode mode = c.getMode();

                char content;
                switch (mode) {
                    case WALL -> content = '#';
                    case START -> content = '@';
                    case FINISH -> content = '!';
                    case SHEET -> content = 'S';
                    default -> {
                        if (mode.getLabel() != null) {
                            char letter = mode.getLabel().charAt(0);
                            formsFound.add(letter);
                            content = letter;
                        } else {
                            content = ' '; // FLOOR or unknown
                        }
                    }
                }

                if (content == '#' || content == ' ') {
                    sb.append(content).append(content);
                } else {
                    sb.append(content).append(c.getPlayerId());
                }
            }
            lines.add(sb.toString());
        }

        MazeInfoData maze = new MazeInfoData();
        maze.name = mazeName;
        maze.forms = new ArrayList<>();
        for (char formId : formsFound) {
            String formName = "Form " + formId;
            maze.forms.add(new FormInfo(formId, formName));
        }
        maze.maze = String.join("/", lines);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File( mazeName + ".json"));
        if (chooser.showSaveDialog(editor) == JFileChooser.APPROVE_OPTION) {
            try (
                FileWriter writer = new FileWriter(chooser.getSelectedFile())
            ) {
                gson.toJson(maze, writer);
                new MessageDialog(                    editor,
                    "Maze saved successfully!",
                    "Save Complete",
                    JOptionPane.INFORMATION_MESSAGE
                ).show();
            } catch (IOException e) {
                new MessageDialog(
                        editor,
                    "Error saving: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                ).show();
            }
        }
    }
}
