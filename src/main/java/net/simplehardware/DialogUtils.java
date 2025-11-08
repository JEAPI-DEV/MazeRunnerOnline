package net.simplehardware;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Utility class for creating properly sized dialogs that work well with Material Design
 */
public class DialogUtils {
    
    /**
     * Shows a message dialog with proper sizing
     */
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        JDialog dialog = createDialog(parent, message, title, messageType);
        dialog.setVisible(true);
    }
    
    /**
     * Shows a message dialog for pathfinding results with proper formatting
     */
    public static void showPathfindingResult(Component parent, int playerId, int moves) {
        String message = String.format("Minimum moves needed for player %d: %d", playerId, moves);
        showMessageDialog(parent, message, "Pathfinding Result", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows an error message for pathfinding issues
     */
    public static void showPathfindingError(Component parent, String message) {
        showMessageDialog(parent, message, "Pathfinder Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Creates a properly sized dialog
     */
    private static JDialog createDialog(Component parent, String message, String title, int messageType) {
        // Get the parent frame
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(parent);
        if (frame == null) {
            frame = new Frame();
        }
        
        // Create the dialog
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Create message panel with proper layout
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Add icon
        JLabel iconLabel = new JLabel();
        Icon icon = UIManager.getIcon("OptionPane." + getIconName(messageType));
        if (icon != null) {
            iconLabel.setIcon(icon);
        }
        contentPanel.add(iconLabel, BorderLayout.WEST);
        
        // Add message text with proper wrapping
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setFont(UIManager.getFont("OptionPane.font"));
        
        // Set preferred size with reduced height for better proportions
        int maxWidth = 400;
        int maxHeight = 120;  // Reduced from 200 to 120 for better height
        messageArea.setPreferredSize(new Dimension(maxWidth, maxHeight));
        
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setMaximumSize(new Dimension(maxWidth, maxHeight));
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set content and pack
        dialog.setContentPane(contentPanel);
        dialog.pack();
        
        // Center the dialog
        dialog.setLocationRelativeTo(frame);
        
        // Set minimum size to prevent it from being too small
        dialog.setMinimumSize(new Dimension(200, 100));
        
        return dialog;
    }
    
    /**
     * Gets the icon name for the message type
     */
    private static String getIconName(int messageType) {
        return switch (messageType) {
            case JOptionPane.ERROR_MESSAGE -> "errorIcon";
            case JOptionPane.INFORMATION_MESSAGE -> "informationIcon";
            case JOptionPane.WARNING_MESSAGE -> "warningIcon";
            case JOptionPane.QUESTION_MESSAGE -> "questionIcon";
            default -> "informationIcon";
        };
    }
}