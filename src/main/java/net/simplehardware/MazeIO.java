package net.simplehardware;

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
                DialogUtils.showMessageDialog(
                    editor,
                    "Maze loaded successfully!",
                    "Load Complete",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                DialogUtils.showMessageDialog(
                    editor,
                    "Failed to load: " + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
                );
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
                if (2 * x + 1 >= row.length()) {
                    break;
                }
                
                char chType = row.charAt(2 * x);
                char chOwner = row.charAt(2 * x + 1);
                int pid = Character.isDigit(chOwner) ? chOwner - '0' : 0;
                Mode mode = switch (chType) {
                    case '#' -> Mode.WALL;
                    case '@' -> Mode.START;
                    case '!' -> Mode.FINISH;
                    case 'S' -> Mode.SHEET;
                    case 'A' -> Mode.FORM_A;
                    case 'B' -> Mode.FORM_B;
                    case 'C' -> Mode.FORM_C;
                    case 'D' -> Mode.FORM_D;
                    case 'E' -> Mode.FORM_E;
                    case 'F' -> Mode.FORM_F;
                    case 'G' -> Mode.FORM_G;
                    case 'H' -> Mode.FORM_H;
                    case 'I' -> Mode.FORM_I;
                    case 'J' -> Mode.FORM_J;
                    case 'K' -> Mode.FORM_K;
                    case 'L' -> Mode.FORM_L;
                    case 'M' -> Mode.FORM_M;
                    case 'N' -> Mode.FORM_N;
                    case 'O' -> Mode.FORM_O;
                    case 'P' -> Mode.FORM_P;
                    case 'Q' -> Mode.FORM_Q;
                    case 'R' -> Mode.FORM_R;
                    case '$' -> Mode.FORM_S;
                    case 'T' -> Mode.FORM_T;
                    case 'U' -> Mode.FORM_U;
                    case 'V' -> Mode.FORM_V;
                    case 'W' -> Mode.FORM_W;
                    case 'X' -> Mode.FORM_X;
                    case 'Y' -> Mode.FORM_Y;
                    case 'Z' -> Mode.FORM_Z;
                    default -> Mode.FLOOR;
                };
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
                char content = switch (c.getMode()) {
                    case WALL -> '#';
                    case START -> '@';
                    case FINISH -> '!';
                    case SHEET -> 'S';
                    case FORM_A -> {
                        formsFound.add('A');
                        yield 'A';
                    }
                    case FORM_B -> {
                        formsFound.add('B');
                        yield 'B';
                    }
                    case FORM_C -> {
                        formsFound.add('C');
                        yield 'C';
                    }
                    case FORM_D -> {
                        formsFound.add('D');
                        yield 'D';
                    }
                    case FORM_E -> {
                        formsFound.add('E');
                        yield 'E';
                    }
                    case FORM_F -> {
                        formsFound.add('F');
                        yield 'F';
                    }
                    case FORM_G -> {
                        formsFound.add('G');
                        yield 'G';
                    }
                    case FORM_H -> {
                        formsFound.add('H');
                        yield 'H';
                    }
                    case FORM_I -> {
                        formsFound.add('I');
                        yield 'I';
                    }
                    case FORM_J -> {
                        formsFound.add('J');
                        yield 'J';
                    }
                    case FORM_K -> {
                        formsFound.add('K');
                        yield 'K';
                    }
                    case FORM_L -> {
                        formsFound.add('L');
                        yield 'L';
                    }
                    case FORM_M -> {
                        formsFound.add('M');
                        yield 'M';
                    }
                    case FORM_N -> {
                        formsFound.add('N');
                        yield 'N';
                    }
                    case FORM_O -> {
                        formsFound.add('O');
                        yield 'O';
                    }
                    case FORM_P -> {
                        formsFound.add('P');
                        yield 'P';
                    }
                    case FORM_Q -> {
                        formsFound.add('Q');
                        yield 'Q';
                    }
                    case FORM_R -> {
                        formsFound.add('R');
                        yield 'R';
                    }
                    case FORM_S -> {
                        formsFound.add('S');
                        yield '$';
                    }
                    case FORM_T -> {
                        formsFound.add('T');
                        yield 'T';
                    }
                    case FORM_U -> {
                        formsFound.add('U');
                        yield 'U';
                    }
                    case FORM_V -> {
                        formsFound.add('V');
                        yield 'V';
                    }
                    case FORM_W -> {
                        formsFound.add('W');
                        yield 'W';
                    }
                    case FORM_X -> {
                        formsFound.add('X');
                        yield 'X';
                    }
                    case FORM_Y -> {
                        formsFound.add('Y');
                        yield 'Y';
                    }
                    case FORM_Z -> {
                        formsFound.add('Z');
                        yield 'Z';
                    }
                    default -> ' ';
                };
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
                DialogUtils.showMessageDialog(
                    editor,
                    "Maze saved successfully!",
                    "Save Complete",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IOException e) {
                DialogUtils.showMessageDialog(
                    editor,
                    "Error saving: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
