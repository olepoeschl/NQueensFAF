package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

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
	setUniqueSolutions(symSolvers.get(getSelectedSolver()).getUniqueSolutionsTotal(solutions));
    };
    private final Runnable onStart = () -> fireSolverStarted();
    private final Runnable onFinish = () -> fireSolverFinished();

    private AbstractSolver selectedSolver;

    private int n = 16;

    private float progress;
    private long solutions;
    private long duration;
    private long uniqueSolutions;
    
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
    
    void startSymSolver(AbstractSolver solver) {
	symSolvers.get(solver).setN(n);
	symSolvers.get(solver).start();
    }
    
    void configureCallbacks() {
	selectedSolver.onProgressUpdate(onProgressUpdate);
	selectedSolver.onStart(onStart);
	selectedSolver.onFinish(onFinish);
    }

    void setSelectedSolver(AbstractSolver solver) {
	var oldValue = this.selectedSolver;
	this.selectedSolver = solver;
	
	setProgress(solver.getProgress());
	setSolutions(solver.getSolutions());
	setDuration(solver.getDuration());

	if(symSolvers.get(solver) == null)
	    symSolvers.put(solver, new SymSolver());
	if(solutions > 0)
	    setUniqueSolutions(symSolvers.get(solver).getUniqueSolutionsTotal(solutions));
	else
	    setUniqueSolutions(0);
	
	prop.firePropertyChange("selectedSolver", oldValue, solver);
    }
    
    AbstractSolver getSelectedSolver() {
	return selectedSolver;
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

    private void fireSolverStarted() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverStarted());
	}
    }
    
    private void fireSolverFinished() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    EventQueue.invokeLater(() -> listener.solverFinished());
	}
    }

    static interface SolverListener extends EventListener {
	void solverStarted();
	void solverFinished();
    }
}
