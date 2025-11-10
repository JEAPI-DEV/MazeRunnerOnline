package net.simplehardware.dialogs;

import javax.swing.*;
import java.awt.*;

public class PathfindingResultDialog extends Dialog {

    public PathfindingResultDialog(Component parent, int playerId, int minMoves) {
        super(parent, "Pathfinding Result");

        String message = String.format("Minimum moves needed for player %d: %d", playerId, minMoves);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.informationIcon"));
        contentPanel.add(iconLabel, BorderLayout.WEST);

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(UIManager.getFont("OptionPane.font"));
        messageArea.setPreferredSize(new Dimension(400, 120));
        contentPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContent(contentPanel);
    }
}
