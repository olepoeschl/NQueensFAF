package de.nqueensfaf.demo.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JDialog;

@SuppressWarnings("serial")
class SettingsDialog extends JDialog {
    
    public SettingsDialog(Frame owner, int initialUpdateInterval, int initialAutoSaveInterval) {
	super(owner, "Settings", true);
	
	var propConfigUi = new PropertyGroupConfigUi1();
	propConfigUi.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	propConfigUi.addIntProperty("updateInterval", "Update Interval", 0, 60_000, initialUpdateInterval);
	propConfigUi.getProperty("updateInterval").addChangeListener(e -> firePropertyChange("updateInterval", null, (int) e.getNewValue()));
	propConfigUi.addIntProperty("autoSaveInterval", "Auto Save Interval", 0, 100, initialAutoSaveInterval);
	propConfigUi.getProperty("autoSaveInterval").addChangeListener(e -> firePropertyChange("autoSaveInterval", null, (int) e.getNewValue()));
	
	setContentPane(propConfigUi);
	pack();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	setLocation(screenSize.width / 2 - getPreferredSize().width / 2, screenSize.height / 2 - getPreferredSize().height / 2);
    }
}
