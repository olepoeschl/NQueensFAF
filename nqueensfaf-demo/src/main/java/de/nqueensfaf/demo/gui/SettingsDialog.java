package de.nqueensfaf.demo.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class SettingsDialog extends JDialog {
    
    public SettingsDialog(Frame owner, int initialUpdateInterval, int initialAutoSaveInterval) {
	super(owner, "Settings", true);
	
	var container = new JPanel(new GridBagLayout());
	container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	
	var propConfigUi = new PropertyGroupConfigUi(container);
	propConfigUi.addIntProperty("updateInterval", "Update Interval", 0, 60_000, initialUpdateInterval);
	propConfigUi.addPropertyChangeListener("updateInterval", e -> firePropertyChange("updateInterval", null, (int) e.getNewValue()));
	propConfigUi.addIntProperty("autoSaveInterval", "Auto Save Interval", 0, 100, initialAutoSaveInterval);
	propConfigUi.addPropertyChangeListener("autoSaveInterval", e -> firePropertyChange("autoSaveInterval", null, (int) e.getNewValue()));
	
	setContentPane(container);
	pack();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	setLocation(screenSize.width / 2 - getPreferredSize().width / 2, screenSize.height / 2 - getPreferredSize().height / 2);
    }
}
