package net.simplehardware;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class ToolbarFactory {

    private final MazeEditor editor;
    private final MazeGrid grid;
    private JSpinner gridSizeSpinner;

    public ToolbarFactory(MazeEditor editor, MazeGrid grid) {
        this.editor = editor;
        this.grid = grid;
    }

    private void styleButton(JButton button, Color background, Color border) {
        button.setBackground(background);
        button.setBorder(BorderFactory.createLineBorder(border, 1));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(100, 32));
    }

    public JPanel createTopToolbar() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        panel.setBackground(new Color(250, 250, 250)); // Light Material Design background

        JButton floorBtn = new JButton("Floor");
        floorBtn.setToolTipText("Paint floor tiles (walkable areas)");
        styleButton(floorBtn, new Color(245, 245, 245), new Color(76, 175, 80)); // Light gray with green accent
        floorBtn.addActionListener(e -> editor.setCurrentMode(Mode.FLOOR));

        JButton wallBtn = new JButton("Wall");
        wallBtn.setToolTipText("Paint wall tiles (obstacles)");
        styleButton(wallBtn, new Color(255, 255, 255), new Color(244, 67, 54)); // White with red accent
        wallBtn.addActionListener(e -> editor.setCurrentMode(Mode.WALL));

        JButton startBtn = new JButton("Player Start");
        startBtn.setToolTipText("Set the starting position for a player");
        styleButton(startBtn, new Color(255, 255, 255), new Color(76, 175, 80)); // White with green accent
        startBtn.addActionListener(e -> editor.setCurrentMode(Mode.START));

        JButton finishBtn = new JButton("Finish");
        finishBtn.setToolTipText("Set the finish/goal position for a player");
        styleButton(finishBtn, new Color(255, 255, 255), new Color(33, 150, 243)); // White with blue accent
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
        
        return panel;
    }

    public JPanel createLeftToolbar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Tools"));
        panel.setBackground(new Color(250, 250, 250)); // Light Material Design background

        // Clear All Button - Red Material Design with SVG Icon
        SVGButton clearBtn = new SVGButton("Clear", "/images/clearAll_Icon.svg");
        clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            for (CellButton[] row : grid.getCells()) {
                for (CellButton cell : row) {
                    cell.setMode(Mode.FLOOR, 0);
                }
            }
        });

        // Load Button - Blue Material Design with SVG Icon
        SVGButton loadBtn = new SVGButton("Load", "/images/load_Icon.svg");
        loadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadBtn.addActionListener(e ->
                MazeIO.loadFromJson(editor, grid, gridSizeSpinner)
        );

        // Save Button - Green Material Design with SVG Icon
        SVGButton saveBtn = new SVGButton("Save", "/images/save_Icon.svg");
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.addActionListener(e -> MazeIO.exportJson(editor, grid));

        // Calculate Minimum Moves Button - Purple Material Design with SVG Icon
        SVGButton calcMinMovesBtn = new SVGButton("Min Moves", "/images/DistanceToWalk_Icon.svg");
        calcMinMovesBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcMinMovesBtn.addActionListener(e -> {
            int playerId = editor.getCurrentPlayerId();
            int minMoves = Pathfinder.calculateMinimumMoves(grid.getCells(), playerId);
            if (minMoves != -1) {
                DialogUtils.showPathfindingResult(editor, playerId, minMoves);
            }
        });

        // Edge Walls Button - Orange Material Design with SVG Icon
        SVGButton topWall = new SVGButton("Edge Walls", "/images/generateWalls_Icon.svg");
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

        // Generate Labyrinth Button - Teal Material Design with SVG Icon
        SVGButton genBtn = new SVGButton("Generate", "/images/generate_Icon.svg");
        genBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        genBtn.addActionListener(e -> {
            LabyrinthGenerator.generateBalancedMaze(grid, 4);
        });


        panel.add(clearBtn);
        panel.add(Box.createVerticalStrut(8));
        panel.add(loadBtn);
        panel.add(Box.createVerticalStrut(8));
        panel.add(saveBtn);
        panel.add(Box.createVerticalStrut(8));
        panel.add(calcMinMovesBtn);
        panel.add(Box.createVerticalStrut(8));
        panel.add(topWall);
        panel.add(Box.createVerticalStrut(8));
        panel.add(genBtn);
        panel.add(Box.createVerticalStrut(12));
        Dimension min = panel.getMinimumSize();
        panel.setMaximumSize(new Dimension(min.width, Integer.MAX_VALUE));
        panel.setMinimumSize(min);
        panel.setPreferredSize(min);
        return panel;
    }
}
