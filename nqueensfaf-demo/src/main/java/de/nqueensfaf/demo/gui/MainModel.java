package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.event.EventListenerList;

import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;
import de.nqueensfaf.demo.gui.util.Dialog;
import de.nqueensfaf.impl.SymSolver;

class MainModel {

    private final PropertyChangeSupport prop = new PropertyChangeSupport(this);
    
    private final EventListenerList listenerList = new EventListenerList();
    
    private final Map<SolverImplWithConfig, SymSolver> symSolvers = new HashMap<SolverImplWithConfig, SymSolver>();
    
    // solver callbacks
    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	update(progress, solutions, duration);
    };
    private final Runnable onStart = () -> fireSolverStarted();
    private final Runnable onFinish = () -> {
	fireSolverTerminated();
    };
    private final Consumer<Exception> onCancel = e -> {
	fireSolverTerminated();
    };

    private SolverImplWithConfig selectedSolverImplWithConfig;
    private int n = 16;
    private int updateInterval = 100;
    private float autoSaveInterval = 0; // disabled by default
    
    private boolean fileOpened = false;
    
    public MainModel() {
	addSolverListener(new SolverListener() {
	    @Override
	    public void solverTerminated() {
		setFileOpened(false);
	    }
	});
    }

    void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
	prop.addPropertyChangeListener(propertyName, l);
    }
    
    void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
	prop.removePropertyChangeListener(propertyName, l);
    }
    
    void addSolverListener(SolverListener l) {
	listenerList.add(SolverListener.class, l);
    }
    
    void removeSolverListener(SolverListener l) {
	listenerList.remove(SolverListener.class, l);
    }
    
    private void update(float progress, long solutions, long duration) {
	prop.firePropertyChange("progress", null, progress);
	prop.firePropertyChange("solutions", null, solutions);
	prop.firePropertyChange("duration", null, duration);
	prop.firePropertyChange("uniqueSolutions", null, getUniqueSolutions(solutions));
    }
    
    private void update() {
	var solver = selectedSolverImplWithConfig.getSolver();
	update(solver.getProgress(), solver.getSolutions(), solver.getDuration());
    }
    
    private long getUniqueSolutions(long solutions) {
	return symSolvers.get(selectedSolverImplWithConfig).getUniqueSolutionsTotal(solutions);
    }
    
    void setSelectedSolverImplWithConfig(SolverImplWithConfig solverImplWithConfig) {
	selectedSolverImplWithConfig = solverImplWithConfig;
	
	if(symSolvers.get(solverImplWithConfig) == null) {
	    var symSolver = new SymSolver();
	    symSolver.onProgressUpdate((progress, solutions, duration) -> {
		prop.firePropertyChange("uniqueSolutions", null, symSolver.getUniqueSolutionsTotal(selectedSolverImplWithConfig.getSolver().getSolutions()));
	    });
	    symSolvers.put(selectedSolverImplWithConfig, symSolver);
	}
	
	update();
	
	prop.firePropertyChange("selectedSolverImplWithConfig", null, solverImplWithConfig);
    }
    
    SolverImplWithConfig getSelectedSolverImplWithConfig() {
	return selectedSolverImplWithConfig;
    }
    
    void setN(int n) {
	this.n = n;
	prop.firePropertyChange("n", null, n);
    }
    
    int getN() {
	return n;
    }
    
    private void setFileOpened(boolean fileOpened) {
	this.fileOpened = fileOpened;
	prop.firePropertyChange("fileOpened", null, fileOpened);
    }
    
    boolean isFileOpened() {
	return fileOpened;
    }
    
    void startSolver() {
	applyConfigs();
	
	String errorMessage = selectedSolverImplWithConfig.checkConfigValid();
	if(errorMessage.length() > 0) {
	    Dialog.error(errorMessage);
	    return;
	}
	
	Thread.ofVirtual().start(() -> symSolvers.get(selectedSolverImplWithConfig).start());
	Thread.ofVirtual().start(() -> {
	    try {
		selectedSolverImplWithConfig.getSolver().start();
	    } catch(Exception e) {
		Dialog.error(e.getMessage());
		symSolvers.get(selectedSolverImplWithConfig).cancel();
	    }
	});
    }
    
    private void applyConfigs() {
	var solver = selectedSolverImplWithConfig.getSolver();
	solver.onProgressUpdate(onProgressUpdate);
	solver.onStart(onStart);
	solver.onFinish(onFinish);
	solver.onCancel(onCancel);
	solver.setUpdateInterval(updateInterval);
	if(!fileOpened)
	    solver.setN(n);
	
	symSolvers.get(selectedSolverImplWithConfig).setN(n);
    }
    
    void openFile(String path) throws IOException {
	selectedSolverImplWithConfig.load(path);
	setN(selectedSolverImplWithConfig.getSolver().getN());
	setFileOpened(true);
	update();
    }
    
    void saveToFile(String path) throws IOException {
	selectedSolverImplWithConfig.getSolver().save(path);
    }
    
    // solver events management
    private void fireSolverStarted() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverStarted());
	}
    }
    
    private void fireSolverTerminated() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverTerminated());
	}
    }
    
    static interface SolverListener extends EventListener {
	default void solverStarted() {}
	default void solverTerminated() {}
    }
}
