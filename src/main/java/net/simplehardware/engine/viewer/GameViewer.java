package net.simplehardware.engine.viewer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Swing GUI for visualizing game execution with timeline controls
 */
public class GameViewer extends JFrame {
    private final List<GameState> gameHistory;
    private int currentTurnIndex = 0;

    private MazePanel mazePanel;
    private JSlider timelineSlider;
    private JLabel turnLabel;
    private JPanel playerStatsPanel;

    private static int CELL_SIZE = 80;
    private static final Color[] PLAYER_COLORS = {
            new Color(255, 100, 100), // Player 1 - Red
            new Color(100, 100, 255), // Player 2 - Blue
            new Color(100, 255, 100), // Player 3 - Green
            new Color(255, 255, 100) // Player 4 - Yellow
    };

    // Dark theme colors
    private static final Color BG_DARK = new Color(40, 44, 52);
    private static final Color BG_DARKER = new Color(30, 34, 42);
    private static final Color FG_LIGHT = new Color(171, 178, 191);
    private static final Color BORDER_COLOR = new Color(60, 64, 72);

    public GameViewer(List<GameState> gameHistory, String mazeName) {
        super("Maze Runner - " + mazeName);
        this.gameHistory = gameHistory;

        if (gameHistory.isEmpty()) {
            throw new IllegalArgumentException("Game history is empty");
        }

        initializeUI();
        updateDisplay();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1650, 1000));
        setLocationRelativeTo(null);

        // Apply dark theme
        getContentPane().setBackground(BG_DARK);

        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(BG_DARK);

        // Top panel - Turn info
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBackground(BG_DARK);
        turnLabel = new JLabel();
        turnLabel.setForeground(FG_LIGHT);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(turnLabel);
        add(topPanel, BorderLayout.NORTH);

        // Left - Maze grid
        GameState firstState = gameHistory.getFirst();
        mazePanel = new MazePanel(firstState.getMazeWidth(), firstState.getMazeHeight());
        mazePanel.setBackground(BG_DARK);
        JScrollPane scrollPane = new JScrollPane(mazePanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(BG_DARKER);
        add(scrollPane, BorderLayout.CENTER);

        // Right side panel - Player stats and logs
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(800, 0));
        rightPanel.setBackground(BG_DARK);

        // Player stats at top of right panel
        playerStatsPanel = new JPanel();
//        playerStatsPanel.setLayout(new BoxLayout(playerStatsPanel, BoxLayout.Y_AXIS));
        playerStatsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                "Players",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                FG_LIGHT));
        playerStatsPanel.setLayout(new FlowLayout());

        playerStatsPanel.setBackground(BG_DARK);
        playerStatsPanel.setPreferredSize(new Dimension(0, 150));
        rightPanel.add(playerStatsPanel, BorderLayout.NORTH);

        // Player logs panel (2x2 grid)
        GridLayout layout = new GridLayout(2, 2, 10, 10);

        JPanel logsPanel = new JPanel(layout);

        logsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        logsPanel.setBackground(BG_DARK);

        // Create log panels for each player
        int numPlayers = firstState.getPlayers().size();
        for (int i = 1; i <= numPlayers; i++) {
            JPanel playerLogPanel = createPlayerLogPanel(i);
            logsPanel.add(playerLogPanel);
        }

        // Fill empty slots if less than 4 players
        for (int i = numPlayers; i < 4; i++) {
            logsPanel.add(new JPanel());
        }

        rightPanel.add(logsPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Bottom - Timeline controls
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.setBackground(BG_DARK);

        timelineSlider = new JSlider(0, gameHistory.size() - 1, 0);
        timelineSlider.setBackground(BG_DARK);
        timelineSlider.setForeground(FG_LIGHT);
        timelineSlider.setMajorTickSpacing(Math.max(1, gameHistory.size() / 10));
        timelineSlider.setMinorTickSpacing(1);
        timelineSlider.setPaintTicks(true);
        timelineSlider.setPaintLabels(true);
        timelineSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentTurnIndex = timelineSlider.getValue();
                updateDisplay();
            }
        });

        JLabel turnLabelLeft = new JLabel("Turn:");
        turnLabelLeft.setForeground(FG_LIGHT);
        bottomPanel.add(turnLabelLeft, BorderLayout.WEST);
        bottomPanel.add(timelineSlider, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createPlayerLogPanel(int playerId) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PLAYER_COLORS[playerId - 1], 2),
                "Player " + playerId,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                PLAYER_COLORS[playerId - 1]));
        panel.setBackground(new Color(40, 44, 52));

        // Create areas
        JTextArea stdoutArea = createLogTextArea(1); // 1 row
        JTextArea stderrArea = createLogTextArea(4); // initial 4 rows (will expand)

        // Labels
        JLabel stdoutLabel = createLogLabel("Standard Output", new Color(97, 175, 239));
        JLabel stderrLabel = createLogLabel("Standard Error", new Color(224, 108, 117));

        // Panels with labels
        JPanel stdoutPanel = new JPanel(new BorderLayout());
        stdoutPanel.setBackground(new Color(40, 44, 52));
        stdoutPanel.add(stdoutLabel, BorderLayout.NORTH);
        stdoutPanel.add(new JScrollPane(stdoutArea), BorderLayout.CENTER);

        JPanel stderrPanel = new JPanel(new BorderLayout());
        stderrPanel.setBackground(new Color(40, 44, 52));
        stderrPanel.add(stderrLabel, BorderLayout.NORTH);
        stderrPanel.add(new JScrollPane(stderrArea), BorderLayout.CENTER);

        // Use GridBagLayout for proportional sizing: stdout = 1 part, stderr = 3 parts
        JPanel logsContainer = new JPanel(new GridBagLayout());
        logsContainer.setBackground(new Color(40, 44, 52));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Stdout: small, fixed-ish height
        gbc.gridy = 0;
        gbc.weighty = 0.2; // ~1 line
        gbc.insets = new Insets(0, 0, 5, 0);
        logsContainer.add(stdoutPanel, gbc);

        // Stderr: takes remaining space
        gbc.gridy = 1;
        gbc.weighty = 0.8; // rest of the space
        gbc.insets = new Insets(0, 0, 0, 0);
        logsContainer.add(stderrPanel, gbc);

        panel.add(logsContainer, BorderLayout.CENTER);

        // Store references
        stdoutArea.setName("stdout_" + playerId);
        stderrArea.setName("stderr_" + playerId);

        return panel;
    }

    // Helper: creates a styled log text area
    private JTextArea createLogTextArea(int rows) {
        JTextArea area = new JTextArea(rows, 15);
        area.setEditable(false);
        area.setBackground(Color.DARK_GRAY);
        area.setForeground(new Color(171, 178, 191));
        area.setFont(new Font("Monospaced", Font.PLAIN, 15));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    // Helper: creates a styled log label
    private JLabel createLogLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.BOLD, 10));
        return label;
    }

    private void updateDisplay() {
        GameState state = gameHistory.get(currentTurnIndex);

        // Update turn label
        turnLabel.setText(String.format("Turn %d / %d",
                state.getTurnNumber(), gameHistory.get(gameHistory.size() - 1).getTurnNumber()));

        // Update maze panel
        mazePanel.setState(state);
        mazePanel.repaint();

        // Update player stats
        updatePlayerStats(state);

        // Update player logs
        updatePlayerLogs(state);
    }

    private void updatePlayerLogs(GameState state) {
        Map<Integer, net.simplehardware.engine.viewer.PlayerLog> logs = state.getPlayerLogs();

        // Find all log text areas and update them
        for (Component comp : getContentPane().getComponents()) {
            updateLogComponents(comp, logs);
        }
    }

    private void updateLogComponents(Component comp, Map<Integer, net.simplehardware.engine.viewer.PlayerLog> logs) {
        if (comp instanceof JTextArea) {
            JTextArea textArea = (JTextArea) comp;
            String name = textArea.getName();
            if (name != null && name.startsWith("std")) {
                String[] parts = name.split("_");
                if (parts.length == 2) {
                    int playerId = Integer.parseInt(parts[1]);
                    net.simplehardware.engine.viewer.PlayerLog log = logs.get(playerId);
                    if (log != null) {
                        if (name.startsWith("stdout")) {
                            textArea.setText(log.getStdout());
                        } else if (name.startsWith("stderr")) {
                            textArea.setText(log.getStderr());
                        }
                    } else {
                        textArea.setText("");
                    }
                    textArea.setCaretPosition(0);
                }
            }
        } else if (comp instanceof Container) {
            Container container = (Container) comp;
            for (Component child : container.getComponents()) {
                updateLogComponents(child, logs);
            }
        }
    }

    private void updatePlayerStats(GameState state) {
        playerStatsPanel.removeAll();

        for (GameState.PlayerSnapshot player : state.getPlayers().values()) {
            JPanel playerPanel = new JPanel();
            playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
            playerPanel.setBorder(BorderFactory.createLineBorder(PLAYER_COLORS[player.getId() - 1], 2));
            playerPanel.setBackground(BG_DARKER);

            JLabel nameLabel = new JLabel("Player " + player.getId());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            nameLabel.setForeground(PLAYER_COLORS[player.getId() - 1]);

            JLabel scoreLabel = new JLabel("Score: " + player.getScore());
            scoreLabel.setForeground(FG_LIGHT);
            JLabel formsLabel = new JLabel(String.format("Forms: %d/%d",
                    player.getFormsCollected(), player.getFormsRequired()));
            formsLabel.setForeground(FG_LIGHT);
            JLabel statusLabel = new JLabel(
                    player.isFinished() ? "FINISHED" : (player.isActive() ? "Active" : "Inactive"));
            statusLabel.setForeground(FG_LIGHT);

            playerPanel.add(nameLabel);
            playerPanel.add(scoreLabel);
            playerPanel.add(formsLabel);
            playerPanel.add(statusLabel);

            playerStatsPanel.add(playerPanel);
            playerStatsPanel.add(Box.createVerticalStrut(5));
        }

        playerStatsPanel.revalidate();
        playerStatsPanel.repaint();
    }

    /**
     * Custom panel for rendering the maze grid
     */
    private static class MazePanel extends JPanel {
        private final int width;
        private final int height;
        private GameState currentState;

        public MazePanel(int width, int height) {
            this.width = width;
            this.height = height;
            setBackground(BG_DARKER);
        }

        public void setState(GameState state) {
            this.currentState = state;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            CELL_SIZE = getWidth()/this.width;

            if (currentState == null)
                return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            CellSnapshot[][] cells = currentState.getCells();

            // Draw cells
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    drawCell(g2d, cells[x][y], x, y);
                }
            }

            // Draw players on top
            for (GameState.PlayerSnapshot player : currentState.getPlayers().values()) {
                if (player.isActive()) {
                    drawPlayer(g2d, player);
                }
            }
        }

        private void drawCell(Graphics2D g2d, CellSnapshot cell, int x, int y) {
            int px = x * CELL_SIZE;
            int py = y * CELL_SIZE;

            // Fill background
            switch (cell.getType()) {
                case WALL:
                    g2d.setColor(new Color(50, 54, 62));
                    g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    break;
                case FINISH:
                    if (cell.getFinishPlayerId() != null) {
                        g2d.setColor(PLAYER_COLORS[cell.getFinishPlayerId() - 1].darker());
                    } else {
                        g2d.setColor(new Color(60, 100, 60));
                    }
                    g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    break;
                case FLOOR:
                    g2d.setColor(new Color(70, 74, 82));
                    g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    break;
            }

            // Draw grid lines
            g2d.setColor(new Color(90, 94, 102));
            g2d.drawRect(px, py, CELL_SIZE, CELL_SIZE);

            // Draw form if present
            if (cell.getForm() != null) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                FontMetrics fm = g2d.getFontMetrics();
                String formStr = String.valueOf(cell.getForm());
                int textX = px + (CELL_SIZE - fm.stringWidth(formStr)) / 2;
                int textY = py + ((CELL_SIZE - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(formStr, textX, textY);
            }

            // Draw sheet if present
            if (cell.hasSheet()) {
                g2d.setColor(new Color(255, 200, 0));
                g2d.fillOval(px + CELL_SIZE / 4, py + CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
            }
        }

        private void drawPlayer(Graphics2D g2d, GameState.PlayerSnapshot player) {
            int px = player.getX() * CELL_SIZE;
            int py = player.getY() * CELL_SIZE;

            // Draw player circle
            g2d.setColor(PLAYER_COLORS[player.getId() - 1]);
            int margin = 5;
            g2d.fillOval(px + margin, py + margin, CELL_SIZE - 2 * margin, CELL_SIZE - 2 * margin);

            // Draw player number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            String playerNum = String.valueOf(player.getId());
            int textX = px + (CELL_SIZE - fm.stringWidth(playerNum)) / 2;
            int textY = py + ((CELL_SIZE - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(playerNum, textX, textY);
        }
    }
}
