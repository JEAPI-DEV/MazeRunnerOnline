package net.simplehardware;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

public class ToolbarFactory {

    private final MazeEditor editor;
    private final MazeGrid grid;
    private JSpinner gridSizeSpinner;

    public ToolbarFactory(MazeEditor editor, MazeGrid grid) {
        this.editor = editor;
        this.grid = grid;
    }

    public JPanel createTopToolbar() {
        JPanel panel = new JPanel();

        JButton floorBtn = new JButton("Floor");
        floorBtn.addActionListener(e -> editor.setCurrentMode(Mode.FLOOR));

        JButton wallBtn = new JButton("Wall");
        wallBtn.addActionListener(e -> editor.setCurrentMode(Mode.WALL));

        JButton startBtn = new JButton("Player Start");
        startBtn.addActionListener(e -> editor.setCurrentMode(Mode.START));

        JButton finishBtn = new JButton("Finish");
        finishBtn.addActionListener(e -> editor.setCurrentMode(Mode.FINISH));

        JLabel playerLabel = new JLabel("Player ID:");
        JSpinner playerSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 4, 1)
        );
        playerSpinner.addChangeListener(e ->
            editor.setCurrentPlayerId((int) playerSpinner.getValue())
        );

        JLabel gridSizeLabel = new JLabel("Grid Size:");
        gridSizeSpinner = new JSpinner(
            new SpinnerNumberModel(grid.getGridSize(), 10, 500, 1)
        );
        gridSizeSpinner.addChangeListener(e -> {
            int newSize = (int) gridSizeSpinner.getValue();
            grid.resizeGrid(newSize);
        });

        JLabel formsLabel = new JLabel("Forms:");
        String[] formOptions = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
            "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z",
        };
        JComboBox<String> formsDropdown = new JComboBox<>(formOptions);
        formsDropdown.addActionListener(e -> {
            String selected = (String) formsDropdown.getSelectedItem();
            Mode formMode = Mode.valueOf("FORM_" + selected);
            editor.setCurrentMode(formMode);
        });

//        JButton sheetBtn = new JButton("Sheet");
//        sheetBtn.addActionListener(e -> editor.setCurrentMode(Mode.SHEET));


        panel.add(floorBtn);
        panel.add(wallBtn);
        panel.add(startBtn);
        panel.add(finishBtn);
        panel.add(playerLabel);
        panel.add(playerSpinner);
        panel.add(gridSizeLabel);
        panel.add(gridSizeSpinner);
        panel.add(formsLabel);
        panel.add(formsDropdown);
//        panel.add(sheetBtn);
        return panel;
    }

    public JPanel createLeftToolbar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Tools"));

        JButton loadBtn = new JButton("Load JSON");
        loadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadBtn.addActionListener(e ->
                MazeIO.loadFromJson(editor, grid, gridSizeSpinner)
        );

        JButton saveBtn = new JButton("Export JSON");
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.addActionListener(e -> MazeIO.exportJson(editor, grid));

        JButton calcMinMovesBtn = new JButton("Calc Min Moves");
        calcMinMovesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcMinMovesBtn.addActionListener(e -> {
            int playerId = editor.getCurrentPlayerId();
            int minMoves = Pathfinder.calculateMinimumMoves(grid.getCells(), playerId);
            if (minMoves != -1) {
                JOptionPane.showMessageDialog(null, 
                    "Minimum moves needed for player " + playerId + ": " + minMoves, 
                    "Pathfinding Result", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton clearBtn = new JButton("Clear All");
        clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            for (CellButton[] row : grid.getCells()) {
                for (CellButton cell : row) {
                    cell.setMode(Mode.FLOOR, 0);
                }
            }
        });

        JButton topWall = new JButton("Edge Walls");
        topWall.setAlignmentX(Component.CENTER_ALIGNMENT);
        topWall.addActionListener(e -> {
            int n = grid.getCells().length;
            for (int i = 0; i < n; i++) {
                grid.getCells()[i][0].setMode(Mode.WALL, 0);
                grid.getCells()[i][n - 1].setMode(Mode.WALL, 0);
                grid.getCells()[0][i].setMode(Mode.WALL, 0);
                grid.getCells()[n - 1][i].setMode(Mode.WALL, 0);
            }
        });

        JButton genBtn = new JButton("Gen Labyrinth");
        genBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        genBtn.addActionListener(e -> {
            LabyrinthGenerator.generateBalancedMaze(grid, 4);
        });

        JTextArea note = new JTextArea(
            "Note: Generation\nis Experimental\n(works better with\nodd Grid sizes.)"
        );
        note.setEditable(false);
        note.setOpaque(false);
        note.setFocusable(false);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(clearBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(loadBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(saveBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(calcMinMovesBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(topWall);
        panel.add(Box.createVerticalStrut(10));
        panel.add(genBtn);
        panel.add(Box.createVerticalStrut(5));
        panel.add(note);


        return panel;
    }
}
