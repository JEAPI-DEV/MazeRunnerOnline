package net.simplehardware.dialogs;

import net.simplehardware.utils.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends Dialog {

    public SettingsDialog(Component parent, ConfigManager configManager) {
        super(parent, "Settings");

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Application Settings");
        title.setFont(UIManager.getFont("Label.font").deriveFont(16f));
        content.add(title, BorderLayout.NORTH);

        JCheckBox confirmToggle = new JCheckBox(
                "Show confirmation dialogs for destructive actions",
                ConfigManager.isConfirmationsEnabled()
        );

        confirmToggle.addActionListener(e ->
                ConfigManager.setConfirmationsEnabled(confirmToggle.isSelected())
        );

        content.add(confirmToggle, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeBtn);

        content.add(buttonPanel, BorderLayout.SOUTH);

        setContent(content);
        dialog.setMinimumSize(new Dimension(400, 180));
    }
}
