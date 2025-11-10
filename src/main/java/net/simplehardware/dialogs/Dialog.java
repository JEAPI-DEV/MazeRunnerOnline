package net.simplehardware.dialogs;

import javax.swing.*;
import java.awt.*;

public abstract class Dialog {
    protected Frame parentFrame;
    protected JDialog dialog;
    protected String title;
    protected Component parent;

    public Dialog(Component parent, String title) {
        this.parent = parent;
        this.parentFrame = (Frame) SwingUtilities.getWindowAncestor(parent);
        if (parentFrame == null) parentFrame = new Frame();
        this.title = title;
        this.dialog = new JDialog(parentFrame, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    protected void setContent(JPanel contentPanel) {
        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
    }

    public void show() {
        dialog.setVisible(true);
    }
}
