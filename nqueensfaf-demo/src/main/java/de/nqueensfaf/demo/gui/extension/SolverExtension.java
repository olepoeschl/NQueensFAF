package de.nqueensfaf.demo.gui.extension;

import java.io.File;

import javax.swing.JComponent;

import com.esotericsoftware.kryo.Kryo;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.AppConfig;
import de.nqueensfaf.demo.gui.Event;
import de.nqueensfaf.demo.gui.ImmutableAppConfig;

public interface SolverExtension {
    
    void initialize(Kryo kryo); // e.g. load old config from file
    
    void terminate(Kryo kryo); // e.g. save current config to file
    
    JComponent getConfigUi();
    
    AbstractSolver getSolver();
    
    String getName();
    
    String getCurrentRecordCategory();
    
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
    
}
