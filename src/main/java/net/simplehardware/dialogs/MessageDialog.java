package net.simplehardware.dialogs;

import javax.swing.*;
import java.awt.*;

public class MessageDialog extends Dialog {
    public MessageDialog(Component parent, String message, String title, int messageType) {
        super(parent, title);
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane." + getIconName(messageType)));
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

    private String getIconName(int messageType) {
        return switch (messageType) {
            case JOptionPane.ERROR_MESSAGE -> "errorIcon";
            case JOptionPane.INFORMATION_MESSAGE -> "informationIcon";
            case JOptionPane.WARNING_MESSAGE -> "warningIcon";
            case JOptionPane.QUESTION_MESSAGE -> "questionIcon";
            default -> "informationIcon";
        };
    }
}
