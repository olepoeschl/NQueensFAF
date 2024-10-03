package de.nqueensfaf.demo.gui.extension;

import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.util.QuickGBC;

public interface SolverExtension {
    
    default void onStartup() {} // e.g. load old config from file
    
    default void onClose() {} // e.g. save current config to file
    
    default void onSolverStarted() {}
    
    default void onSolverFinished() {}
    
    default void onSolverCanceled() {}
    
    default void onSolverTerminated() {}
    
    default void onSolverSaved() {}
    
    default void onSolverRestored() {}
    
    default void onSolverReset() {}
    
    AbstractSolver getSolver();
    
    String getName();
    
    default String getCurrentRecordCategory() {
	return getName();
    }
    
    default JComponent getConfigUi() {
	var panel = new JPanel(new GridBagLayout());
	panel.add(new JLabel("Nothing to configure :-)"), new QuickGBC(0, 0));
	panel.add(Box.createVerticalGlue(), new QuickGBC(0, 1));
	return panel;
    }
    
    default void getConfig(Map<String, Object> configMap) {}
    
    default void setConfig(Map<String, Object> configMap) {}
    
}
