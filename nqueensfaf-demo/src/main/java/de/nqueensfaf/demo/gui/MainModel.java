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
import de.nqueensfaf.core.ExecutionState;
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
    private int autoSaveInterval = 0; // disabled by default
    
    private boolean fileOpened = false;
    
    private volatile boolean saving = false;
    private int lastAutoSave;
    
    public MainModel() {
	addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		lastAutoSave = (int) (selectedSolverImplWithConfig.getSolver().getProgress() * 100);
	    }
	    @Override
	    public void solverTerminated() {
		fileOpened = false;
	    }
	    @Override
	    public void solverReset() {
		fileOpened = false;
	    }
	});
	
	addPropertyChangeListener("progress", e -> {
	    if(autoSaveInterval <= 0) 
		return;
	    int progress = (int) ((float) e.getNewValue() * 100);
	    if(progress - lastAutoSave >= autoSaveInterval) {
		saveToFile(getN() + "-queens.faf", ex -> Dialog.error("could not save to file: " + ex.getMessage()));
		lastAutoSave = progress;
	    }
	});
	
	// when the application is still busy saving to a file when the user closes the window,
	// complete the saving process before shutting down
	var saveOnExitCompletionThreadBuilder = Thread.ofVirtual().name("saveOnExitCompleter");
	Runnable saveOnExitCompletion = () -> {
	    while(saving)
		try {
		    Thread.sleep(500);
		} catch (InterruptedException e) {
		    Dialog.error("could not complete saving to file: " + e.getMessage());
		}
	};
	Runtime.getRuntime().addShutdownHook(saveOnExitCompletionThreadBuilder.unstarted(saveOnExitCompletion));
    }

    void addPropertyChangeListener(PropertyChangeListener l) {
	prop.addPropertyChangeListener(l);
    }

    void removePropertyChangeListener(PropertyChangeListener l) {
	prop.removePropertyChangeListener(l);
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
	if(solutions > 0)
	    prop.firePropertyChange("uniqueSolutions", null, getUniqueSolutions(solutions));
    }
    
    private void update() {
	var solver = selectedSolverImplWithConfig.getSolver();
	update(solver.getProgress(), solver.getSolutions(), solver.getDuration());
    }
    
    void setSelectedSolverImplWithConfig(SolverImplWithConfig solverImplWithConfig) {
	selectedSolverImplWithConfig = solverImplWithConfig;
	
	if(symSolvers.get(solverImplWithConfig) == null) {
	    var symSolver = new SymSolver();
	    symSolver.onProgressUpdate((progress, solutions, duration) -> {
		// continue unique solutions updates if solver is finished but SymSolver still running
		var runningSolverImpl = selectedSolverImplWithConfig.getSolver();
		if(runningSolverImpl.getExecutionState().equals(ExecutionState.FINISHED))
		    prop.firePropertyChange("uniqueSolutions", null, symSolver.getUniqueSolutionsTotal(runningSolverImpl.getSolutions()));
	    });
	    symSolvers.put(selectedSolverImplWithConfig, symSolver);
	}
	
//	update();
	
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
    
    void setUpdateInterval(int updateInterval) {
	this.updateInterval = updateInterval;
	prop.firePropertyChange("updateInterval", null, updateInterval);
    }
    
    int getUpdateInterval() {
	return updateInterval;
    }
    
    void setAutoSaveInterval(int autoSaveInterval) {
	if(autoSaveInterval > 100)
	    throw new IllegalArgumentException("invalid config: auto save interval must be <= 100");
	this.autoSaveInterval = autoSaveInterval;
	prop.firePropertyChange("autoSaveInterval", null, autoSaveInterval);
    }
    
    int getAutoSaveInterval() {
	return autoSaveInterval;
    }
    
    float getProgress() {
	return selectedSolverImplWithConfig.getSolver().getProgress();
    }
    
    long getSolutions() {
	return selectedSolverImplWithConfig.getSolver().getSolutions();
    }
    
    long getDuration() {
	return selectedSolverImplWithConfig.getSolver().getDuration();
    }
    
    long getUniqueSolutions(long solutions) {
	return symSolvers.get(selectedSolverImplWithConfig).getUniqueSolutionsTotal(solutions);
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
	selectedSolverImplWithConfig.getSolver().load(path);
	selectedSolverImplWithConfig.loaded();
	setN(selectedSolverImplWithConfig.getSolver().getN());
	fileOpened = true;
	
	update();
	fireSolverFileOpened();
    }
    
    boolean isFileOpened() {
	return fileOpened;
    }
    
    void saveToFile(String path, Consumer<Exception> onError) {
	Thread.ofVirtual().start(() -> {
	    saving = true;
	    try {
		selectedSolverImplWithConfig.getSolver().save(path);
	    } catch (IOException e) {
		onError.accept(e);
	    }
	    saving = false;
	});
    }
    
    void reset() {
	selectedSolverImplWithConfig.getSolver().reset();
	symSolvers.get(selectedSolverImplWithConfig).reset();
	update();
	fireSolverReset();
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
    
    private void fireSolverFileOpened() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverFileOpened());
	}
    }
    
    private void fireSolverReset() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverReset());
	}
    }
    
    static interface SolverListener extends EventListener {
	void solverStarted();
	void solverTerminated();
	default void solverFileOpened() {}
	default void solverReset() {}
    }
}
