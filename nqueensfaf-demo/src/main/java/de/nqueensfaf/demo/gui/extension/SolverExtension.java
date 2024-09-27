package de.nqueensfaf.demo.gui.extension;

import javax.swing.JComponent;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.Event;

public interface SolverExtension {
    
    AbstractSolver getSolver();
    
    JComponent getConfigUi();
    
    SolverExtensionConfig getConfig();
    
    void setConfig(SolverExtensionConfig config);
    
    void receiveEvent(Event event);
    
}
