package de.nqueensfaf.demo.gui.extension;

import javax.swing.JComponent;

import de.nqueensfaf.demo.gui.ImmutableAppConfig;

public interface SolverExtensionConfig {
    
    JComponent getUi();
    
    String checkIfValid();
    
    String checkIfCompatible(ImmutableAppConfig appConfig);
    
}
