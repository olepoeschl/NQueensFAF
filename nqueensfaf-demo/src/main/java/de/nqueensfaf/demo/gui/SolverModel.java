package de.nqueensfaf.demo.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EventListener;

import javax.swing.event.EventListenerList;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;

class SolverModel {

    private final EventListenerList listenerList = new EventListenerList();

    private final PropertyChangeSupport prop = new PropertyChangeSupport(this);

    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	setProgress(progress);
	setSolutions(solutions);
	setDuration(duration);
    };
    private final Runnable onStart = () -> fireSolverStarted();
    private final Runnable onFinish = () -> fireSolverFinished();

    private AbstractSolver selectedSolver;

    private int n = 16;

    private float progress;
    private long solutions;
    private long duration;
    
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
    
    void setSelectedSolver(AbstractSolver solver) {
	var oldValue = this.selectedSolver;
	this.selectedSolver = solver;
	prop.firePropertyChange("selectedSolver", oldValue, solver);
    }
    
    void applySolverConfig(AbstractSolver solver) {
	solver.onProgressUpdate(onProgressUpdate);
	solver.onStart(onStart);
	solver.onFinish(onFinish);
	solver.setN(n);
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

    private void fireSolverStarted() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    listener.solverStarted();
	}
    }
    
    private void fireSolverFinished() {
	for(var listener : listenerList.getListeners(SolverListener.class)) {
	    listener.solverFinished();
	}
    }

    static interface SolverListener extends EventListener {
	void solverStarted();
	void solverFinished();
    }
}
