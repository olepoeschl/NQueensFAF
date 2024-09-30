package de.nqueensfaf.demo.gui.extension;

import java.io.File;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.AppConfig;
import de.nqueensfaf.demo.gui.Event;
import de.nqueensfaf.demo.gui.ImmutableAppConfig;

public interface SolverExtension {
    
    AbstractSolver getSolver();
    
    SolverExtensionConfig getConfig();
    
    void handleEvent(Event event);
    
    // save current solver progress to file. Optionally also the extension config
    // and the app config
    default void saveFile(File file, ImmutableAppConfig currentAppConfig) {
	throw new UnsupportedOperationException("Not supported by this solver");
    }
    
    // read current solver progress from file. Optionally also the extension config
    // and modify the app config
    default void openFile(File file, AppConfig appConfig) {
	throw new UnsupportedOperationException("Not supported by this solver");
    }
    
    String getName();
    
    String getCurrentRecordCategory();
    
}
