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

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;
import de.nqueensfaf.impl.SymSolver;

class SolverModel {

    private final EventListenerList listenerList = new EventListenerList();

    private final PropertyChangeSupport prop = new PropertyChangeSupport(this);

    private final Map<AbstractSolver, SymSolver> symSolvers = new HashMap<AbstractSolver, SymSolver>();
    
    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	setProgress(progress);
	setSolutions(solutions);
	setDuration(duration);
	setUniqueSolutions(symSolvers.get(getSelectedSolverImplWithConfig().getConfiguredSolver()).getUniqueSolutionsTotal(solutions));
    };
    private final Runnable onStart = () -> fireSolverStarted();
    private final Runnable onFinish = () -> {
	fireSolverFinished();
	fireSolverTerminated();
    };
    private final Consumer<Exception> onCancel = e -> {
	cancelSymSolver();
	fireSolverCanceled(e);
	fireSolverTerminated();
    };
    
    private SolverImplWithConfig selectedSolverImplWithConfig;

    private int n = 16;

    private float progress;
    private long solutions;
    private long duration;
    private long uniqueSolutions;
    
    private final Map<AbstractSolver, Boolean> loaded = new HashMap<AbstractSolver, Boolean>();
    
    public SolverModel() {
	addSolverListener(new SolverListener() {
	    @Override
	    public void solverTerminated() {
		setLoaded(false);
	    }
	    @Override
	    public void solverStarted() {
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

    void removeSolverStartListener(SolverListener l) {
	listenerList.remove(SolverListener.class, l);
    }
    
    void startSolver() {
	var selectedSolver = getSelectedSolverImplWithConfig().getConfiguredSolver();
	selectedSolver.start();
    }
    
    void startSymSolver() {
	var selectedSolver = getSelectedSolverImplWithConfig().getConfiguredSolver();
	symSolvers.get(selectedSolver).setN(n);
	symSolvers.get(selectedSolver).start();
    }
    
    void cancelSymSolver() {
	var selectedSolver = getSelectedSolverImplWithConfig().getConfiguredSolver();
	symSolvers.get(selectedSolver).cancel();
    }

    void setSelectedSolverImplWithConfig(SolverImplWithConfig solverImplWithConfig) {
	var oldValue = this.selectedSolverImplWithConfig;
	this.selectedSolverImplWithConfig = solverImplWithConfig;

	var solver = selectedSolverImplWithConfig.getConfiguredSolver();

	if(symSolvers.get(solver) == null) {
	    var symSolver = new SymSolver();
	    symSolver.onProgressUpdate((progress, solutions, duration) -> {
		setUniqueSolutions(symSolver.getUniqueSolutionsTotal(SolverModel.this.solutions));
	    });
	    symSolvers.put(solver, symSolver);
	}
	
	if(loaded.get(solver) == null)
	    loaded.put(solver, false);
	
	update();
	
	prop.firePropertyChange("selectedSolverImplWithConfig", oldValue, solverImplWithConfig);
    }
    
    private void update() {
	var solver = selectedSolverImplWithConfig.getConfiguredSolver();
	
	setProgress(solver.getProgress());
	setSolutions(solver.getSolutions());
	setDuration(solver.getDuration());
	
	if(solutions > 0)
	    setUniqueSolutions(symSolvers.get(solver).getUniqueSolutionsTotal(solutions));
	else
	    setUniqueSolutions(0);
	
	prop.firePropertyChange("loaded", null, isLoaded());
	
	setN(solver.getN());
    }
    
    SolverImplWithConfig getSelectedSolverImplWithConfig() {
	return selectedSolverImplWithConfig;
    }
    
    void applyGeneralConfig() {
	var selectedSolver = selectedSolverImplWithConfig.getConfiguredSolver();
	selectedSolver.onProgressUpdate(onProgressUpdate);
	selectedSolver.onStart(onStart);
	selectedSolver.onFinish(onFinish);
	selectedSolver.onCancel(onCancel);
	selectedSolver.setUpdateInterval(100);
	if(!loaded.get(selectedSolver))
	    selectedSolver.setN(n);
    }

    void setN(int n) {
	int oldValue = this.n;
	this.n = n;
	prop.firePropertyChange("n", oldValue, n);
    }

    int getN() {
	return n;
    }

    void setProgress(float progress) {
	float oldValue = this.progress;
	this.progress = progress;
	prop.firePropertyChange("progress", oldValue, progress);
    }

    float getProgress() {
	return progress;
    }

    void setSolutions(long solutions) {
	long oldValue = this.solutions;
	this.solutions = solutions;
	prop.firePropertyChange("solutions", oldValue, solutions);
    }

    long getSolutions() {
	return solutions;
    }

    void setDuration(long duration) {
	long oldValue = this.duration;
	this.duration = duration;
	prop.firePropertyChange("duration", oldValue, duration);
    }

    long getDuration() {
	return duration;
    }

    void setUniqueSolutions(long uniqueSolutions) {
	long oldValue = this.uniqueSolutions;
	this.uniqueSolutions = uniqueSolutions;
	prop.firePropertyChange("unique_solutions", oldValue, uniqueSolutions);
    }

    long getUniqueSolutions() {
	return uniqueSolutions;
    }

    void load(String path) throws IOException {
	selectedSolverImplWithConfig.getConfiguredSolver().load(path);
	
	setLoaded(true);
	
	update();
    }
    
    private void setLoaded(boolean val) {
	loaded.put(selectedSolverImplWithConfig.getConfiguredSolver(), val);
	prop.firePropertyChange("loaded", null, val);
    }

    boolean isLoaded() {
	return loaded.get(selectedSolverImplWithConfig.getConfiguredSolver());
    }

    public void save(String targetPath) throws IOException {
	selectedSolverImplWithConfig.getConfiguredSolver().save(targetPath);
    }

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
    
    private void fireSolverCanceled(Exception e) {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverCanceled(e));
	}
    }

    static interface SolverListener extends EventListener {
	void solverStarted();
	void solverTerminated();
	default void solverFinished() {}
	default void solverCanceled(Exception e) {}
    }
}
