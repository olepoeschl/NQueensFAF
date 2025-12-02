package de.nqueensfaf.demo.gui;

import de.nqueensfaf.demo.gui.extension.PropertyGroupConfigUi;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
class SettingsDialog extends JDialog {

    public SettingsDialog(Frame owner, int initialUpdateInterval, int initialAutoSaveInterval) {
        super(owner, "Settings", true);

        var propConfigUi = new PropertyGroupConfigUi();
        propConfigUi.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        propConfigUi.addIntProperty("updateInterval", "Update Interval", 0, 60_000, initialUpdateInterval);
        propConfigUi.getProperty("updateInterval").addChangeListener(e -> firePropertyChange("updateInterval", null, (int) e.getNewValue()));
        propConfigUi.addIntProperty("autoSaveInterval", "Auto Save Interval", 0, 100, initialAutoSaveInterval);
        propConfigUi.getProperty("autoSaveInterval").addChangeListener(e -> firePropertyChange("autoSaveInterval", null, (int) e.getNewValue()));

        setContentPane(propConfigUi);
        pack();
        setLocationRelativeTo(owner);
    }
}
