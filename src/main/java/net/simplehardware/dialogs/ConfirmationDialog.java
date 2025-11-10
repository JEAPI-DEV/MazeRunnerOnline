package net.simplehardware.dialogs;

import javax.swing.*;
import java.awt.*;

public class ConfirmationDialog extends Dialog {
    private boolean confirmed = false;

    public ConfirmationDialog(Component parent, String message, String title) {
        super(parent, title);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        contentPanel.add(iconLabel, BorderLayout.WEST);

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(UIManager.getFont("OptionPane.font"));
        messageArea.setPreferredSize(new Dimension(400, 100));
        contentPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");

        yesButton.addActionListener(e -> { confirmed = true; dialog.dispose(); });
        noButton.addActionListener(e -> { confirmed = false; dialog.dispose(); });

        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContent(contentPanel);
    }

    public boolean isConfirmed() {
        show();
        return confirmed;
    }
}
