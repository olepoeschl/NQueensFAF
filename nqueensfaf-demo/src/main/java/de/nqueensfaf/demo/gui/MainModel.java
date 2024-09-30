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
import javax.swing.event.SwingPropertyChangeSupport;

import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;
import de.nqueensfaf.core.ExecutionState;
import de.nqueensfaf.demo.gui.extension.SolverExtension;
import de.nqueensfaf.impl.SymSolver;

class MainModel {
    
    private final PropertyChangeSupport prop = new SwingPropertyChangeSupport(this);
    
    private final EventListenerList listenerList = new EventListenerList();
    
    private final Map<SolverExtension, SymSolver> symSolvers = new HashMap<SolverExtension, SymSolver>();
    
    // solver callbacks
    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	update(progress, solutions, duration);
    };
    private final Runnable onStart = () -> fireSolverStarted();
    private final Runnable onFinish = () -> {
	fireSolverFinished();
	fireSolverTerminated();
    };
    private final Consumer<Exception> onCancel = e -> {
	fireSolverTerminated();
    };

    private final SolverExtension[] solverExtensions;
    private SolverExtension selectedSolverExtension;
    
    // auto save = 0: disabled by default
    private final AppConfig appConfig = new AppConfig(16, 100, 0); // TODO
    
    private int lastAutoSave;
    private boolean fileOpened = false;
    private volatile boolean saving = false;
    
    public MainModel() {
	solverExtensions = new SolverExtension[3];
	solverExtensions[0] = new CpuSolverExtension();
	solverExtensions[1] = new GpuSolverExtension();
	solverExtensions[2] = new SimpleSolverExtension();
	selectedSolverExtension = solverExtensions[0];
	
	addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		lastAutoSave = (int) (selectedSolverExtension.getSolver().getProgress() * 100);
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
	
	// auto saving logic
	addPropertyChangeListener("progress", e -> {
	    if(appConfig.getAutoSaveInterval() <= 0) 
		return;
	    if(saving)
		return;
	    int progress = (int) ((float) e.getNewValue() * 100);
	    if(progress - lastAutoSave >= appConfig.getAutoSaveInterval()) {
		Thread.ofVirtual().start(() -> {
		    try {
			saveToFile(appConfig.getN() + "-queens.faf");
		    } catch (IOException ex) {
			Utils.error(null, "could not save to file: " + ex.getMessage());
		    }
		});
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
		    Utils.error(null, "could not complete saving to file: " + e.getMessage());
		}
	};
	Runtime.getRuntime().addShutdownHook(saveOnExitCompletionThreadBuilder.unstarted(saveOnExitCompletion));
    }

    private void update(float progress, long solutions, long duration) {
	prop.firePropertyChange("progress", null, progress);
	prop.firePropertyChange("solutions", null, solutions);
	prop.firePropertyChange("duration", null, duration);
	prop.firePropertyChange("uniqueSolutions", null, getUniqueSolutions(solutions));
    }
    
    private void update() {
	var solver = selectedSolverExtension.getSolver();
	update(solver.getProgress(), solver.getSolutions(), solver.getDuration());
    }

    // ------------ getters and setters -------------
    SolverExtension[] getSolverExtensions() {
	return solverExtensions;
    }
    
    void setSelectedSolverExtension(SolverExtension solverExtension) {
	if(symSolvers.get(solverExtension) == null) {
	    var symSolver = new SymSolver();
	    symSolver.onProgressUpdate((progress, solutions, duration) -> {
		// continue unique solutions updates if solver is finished but SymSolver still running
		var runningSolverImpl = solverExtension.getSolver();
		if(runningSolverImpl.getExecutionState().equals(ExecutionState.FINISHED))
		    prop.firePropertyChange("uniqueSolutions", null, symSolver.getUniqueSolutionsTotal(runningSolverImpl.getSolutions()));
	    });
	    symSolvers.put(solverExtension, symSolver);
	}
	selectedSolverExtension = solverExtension;
	prop.firePropertyChange("selectedSolverExtension", null, solverExtension);
    }
    
    SolverExtension getSelectedSolverExtension() {
	return selectedSolverExtension;
    }
    
    void setN(int n) {
	appConfig.setN(n);
	prop.firePropertyChange("n", null, n);
    }
    
    int getN() {
	return appConfig.getN();
    }
    
    void setUpdateInterval(int updateInterval) {
	appConfig.setUpdateInterval(updateInterval);
	prop.firePropertyChange("updateInterval", null, updateInterval);
    }
    
    int getUpdateInterval() {
	return appConfig.getUpdateInterval();
    }
    
    void setAutoSaveInterval(int autoSaveInterval) {
	if(autoSaveInterval > 100)
	    throw new IllegalArgumentException("invalid config: auto save interval must be <= 100");
	appConfig.setAutoSaveInterval(autoSaveInterval);
	prop.firePropertyChange("autoSaveInterval", null, autoSaveInterval);
    }
    
    int getAutoSaveInterval() {
	return appConfig.getAutoSaveInterval();
    }
    
    float getProgress() {
	return selectedSolverExtension.getSolver().getProgress();
    }
    
    long getSolutions() {
	return selectedSolverExtension.getSolver().getSolutions();
    }
    
    long getDuration() {
	return selectedSolverExtension.getSolver().getDuration();
    }
    
    long getUniqueSolutions(long solutions) {
	return symSolvers.get(selectedSolverExtension).getUniqueSolutionsTotal(solutions);
    }

    boolean isFileOpened() {
	return fileOpened;
    }

    // ------------ actions (data manipulation) -------------
    void startSolver() throws Exception {
	applyConfigs();
	
	String errorMessage = selectedSolverExtension.getConfig().checkIfValid(appConfig);
	if(errorMessage.length() > 0)
	    throw new Exception(errorMessage);
	
	Thread.ofVirtual().start(() -> symSolvers.get(selectedSolverExtension).start());
	try {
	    selectedSolverExtension.getSolver().start();
	} catch(Exception e) {
	    symSolvers.get(selectedSolverExtension).cancel();
	    throw e;
	}
    }
    
    private void applyConfigs() {
	var solver = selectedSolverExtension.getSolver();
	solver.onProgressUpdate(onProgressUpdate);
	solver.onStart(onStart);
	solver.onFinish(onFinish);
	solver.onCancel(onCancel);
	solver.setUpdateInterval(appConfig.getUpdateInterval());
	if(!fileOpened)
	    solver.setN(appConfig.getN());
	
	symSolvers.get(selectedSolverExtension).setN(appConfig.getN());
    }
    
    void openFile(String path) throws IOException {
	// TODO
	// read snapshot from file
	// check, which class the savepoint and config respectively belong to
	// update appConfig
	// load savepoint & update solutions, progress, duration
	// update solver extension config
	
//	selectedSolverExtension.getSolver().load(path);
//	
//	fireSnapshotRestored();
//	
//	setN(selectedSolverExtension.getSolver().getN());
	
	
	fileOpened = true;
	update();
	fireSolverFileOpened();
    }
    
    void saveToFile(String path) throws IOException {
	saving = true;
	
	var snapshot = new Snapshot(appConfig, selectedSolverExtension.getSolver().getSavePoint(), selectedSolverExtension.getConfig());
	// TODO: write the snapshot to a file
	
	selectedSolverExtension.getSolver().save(path);
	saving = false;
    }
    
    void reset() {
	selectedSolverExtension.getSolver().reset();
	symSolvers.get(selectedSolverExtension).reset();
	update();
	fireSolverReset();
    }
    
    // ------------ listener handling -------------
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
    
    // ------------ event management -------------
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
    
    private void fireSolverFinished() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverFinished());
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
    
    private void fireSnapshotRestored() {
	selectedSolverExtension.handleEvent(new Event(Event.SNAPSHOT_RESTORED));
    }

    // ------------ classes and types -------------
    static interface SolverListener extends EventListener {
	void solverStarted();
	void solverTerminated();
	default void solverFinished() {}
	default void solverFileOpened() {}
	default void solverReset() {}
    }
}
