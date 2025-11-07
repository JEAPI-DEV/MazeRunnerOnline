package net.simplehardware;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class CellButton extends JPanel {

    final int x, y;
    private Mode mode = Mode.FLOOR;
    private int playerId = 0;
    private final MazeEditor editor;

    public CellButton(int x, int y, MazeEditor editor) {
        this.x = x;
        this.y = y;
        this.editor = editor;
        setPreferredSize(new Dimension(60, 60));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createLineBorder(new Color(189, 189, 189), 1));

        // Click and drag
        addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        applyCurrentMode();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (
                        (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0
                    ) {
                        Mode m = editor.getCurrentMode();
                        if (m == Mode.WALL || m == Mode.FLOOR) {
                            applyCurrentMode();
                        }
                    }
                }
            }
        );
    }

    private void applyCurrentMode() {
        Mode current = editor.getCurrentMode();
        int pid = (current == Mode.START ||
                current == Mode.FINISH ||
                current == Mode.SHEET ||
                isFormMode(current))
            ? editor.getCurrentPlayerId()
            : 0;
        setMode(current, pid);
    }

    public void setMode(Mode m, int pid) {
        this.mode = m;
        this.playerId = (m == Mode.START ||
                m == Mode.FINISH ||
                m == Mode.SHEET ||
                isFormMode(m))
            ? pid
            : 0;
        updateColor();
        repaint();
    }

    private void updateColor() {
        switch (mode) {
            case FLOOR -> setBackground(new Color(245, 245, 245));
            case WALL -> setBackground(new Color(120, 20, 20));
            case START -> setBackground(new Color(76, 175, 80));
            case FINISH -> setBackground(new Color(33, 150, 243));
            case SHEET -> setBackground(new Color(255, 152, 0));
            default -> {
                if (isFormMode(mode)) {
                    setBackground(getPlayerColor(playerId));
                }
            }
        }
    }

    public Mode getMode() {
        return mode;
    }

    public int getPlayerId() {
        return playerId;
    }

    private boolean isFormMode(Mode m) {
        return m.name().startsWith("FORM_");
    }

    private Color getPlayerColor(int pid) {
        float hue = (pid - 1) * 0.125f;
        return Color.getHSBColor(hue, 0.7f, 0.9f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g.getFontMetrics();

        if (mode == Mode.START || mode == Mode.FINISH) {
            String text = (mode == Mode.START ? "@" : "!") + playerId;
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = (getHeight() + fm.getAscent()) / 2 - 4;
            g.drawString(text, tx, ty);
        } else if (isFormMode(mode)) {
            String formLetter = mode.name().substring(5);
            String text = formLetter + playerId;
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = (getHeight() + fm.getAscent()) / 2 - 4;
            g.drawString(text, tx, ty);
        } else if (mode == Mode.SHEET) {
            String text = "S" + playerId;
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = (getHeight() + fm.getAscent()) / 2 - 4;
            g.drawString(text, tx, ty);
        }
    }
}
