package de.nqueensfaf.demo.gui.extension;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.nqueensfaf.core.AbstractSolver;

public interface SolverExtension {
    
    default void onStartup() {} // e.g. load old config from file
    
    default void onClose() {} // e.g. save current config to file
    
    default void onSolverStarted() {}
    
    default void onSolverFinished() {}
    
    default void onSolverCanceled() {}
    
    default void onSolverTerminated() {}
    
    default void onSolverRestored() {}
    
    default void onSolverReset() {}
    
    AbstractSolver getSolver();
    
    String getName();
    
    default String getCurrentRecordCategory() {
	return getName();
    }
    
    default JComponent getConfigUi() {
	return new JLabel(":-)");
    }
    
    default void getConfig(Map<String, Object> configMap) {}
    
    default void setConfig(Map<String, Object> configMap) {}
    
}
