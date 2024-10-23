package de.nqueensfaf.demo.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import de.nqueensfaf.core.ExecutionState;
import de.nqueensfaf.demo.gui.extension.CpuSolverExtension;
import de.nqueensfaf.demo.gui.extension.GpuSolverExtension;
import de.nqueensfaf.demo.gui.extension.SimpleRecursiveSolverExtension;
import de.nqueensfaf.demo.gui.extension.SolverExtension;
import de.nqueensfaf.impl.SymSolver;

public class Model {

    private final PropertyChangeSupport prop = new PropertyChangeSupport(this);
    
    private Settings settings = new Settings(120);
    
    private final SolverExtension[] solverExtensions;
    private int selectedSolverExtensionIdx = 0;
    private final SymSolver[] symSolvers;
    
    private int n = 16;
    private int autoSaveInterval = 0;

    private float progress;
    private long solutions;
    private long uniqueSolutions;
    private long duration;
    
    private boolean restored = false;
    
    public Model() {
	solverExtensions = new SolverExtension[3];
	solverExtensions[0] = new SimpleRecursiveSolverExtension();
	solverExtensions[1] = new CpuSolverExtension();
	solverExtensions[2] = new GpuSolverExtension();
	
	symSolvers = new SymSolver[3];
	for(int i = 0; i < symSolvers.length; i++)
	    symSolvers[i] = new SymSolver();
    }
    
    // configuration for external access
    public void setN(int n) {
	var oldValue = this.n;
	this.n = n;
	prop.firePropertyChange("n", oldValue, n);
    }
    
    public int getN() {
	return n;
    }
    
    public void setAutoSaveInterval(int autoSaveInterval) {
	var oldValue = this.autoSaveInterval;
	this.autoSaveInterval = autoSaveInterval;
	prop.firePropertyChange("autoSaveInterval", oldValue, autoSaveInterval);
    }
    
    public int getAutoSaveInterval() {
	return autoSaveInterval;
    }
    
    public void setSettings(Settings settings) {
	var oldValue = this.settings;
	this.settings = settings;
	prop.firePropertyChange("settings", oldValue, settings);
    }
    
    public Settings getSettings() {
	return settings;
    }
    
    public void setSelectedSolverExtension(int index) {
	if(index < 0)
	    throw new IndexOutOfBoundsException("invalid index: " + index + " exceeds lower limit of 0");
	if(index >= solverExtensions.length)
	    throw new IndexOutOfBoundsException("invalid index: there is no solver extension with index " + index);
	var oldValue = selectedSolverExtensionIdx;
	selectedSolverExtensionIdx = index;
	prop.firePropertyChange("selectedSolverExtension", solverExtensions[oldValue], solverExtensions[index]);
    }
    
    public SolverExtension getSelectedSolverExtension() {
	return solverExtensions[selectedSolverExtensionIdx];
    }
    
    public SolverExtension[] getSolverExtensions() {
	return solverExtensions;
    }

    public SymSolver configureAndGetSymSolver() {
	var symSolver = symSolvers[selectedSolverExtensionIdx];
	symSolver.setN(n);
	symSolver.onProgressUpdate((progress, solutions, duration) -> {
	    // continue unique solutions updates if solver is finished but SymSolver still running
	    var runningSolverImpl = solverExtensions[selectedSolverExtensionIdx].getSolver();
	    if(runningSolverImpl.getExecutionState().equals(ExecutionState.FINISHED))
		prop.firePropertyChange("uniqueSolutions", null, symSolver.getUniqueSolutionsTotal(runningSolverImpl.getSolutions()));
	});
	return symSolver;
    }
    
    public SymSolver getCurrentSymSolver() {
	return symSolvers[selectedSolverExtensionIdx];
    }
    
    public void setRestored(boolean restored) {
	var oldValue = this.restored;
	this.restored = restored;
	prop.firePropertyChange("restored", oldValue, restored);
    }
    
    public boolean isRestored() {
	return restored;
    }
    
    // current solver progress properties
    public void updateSolverProgress(float progress, long solutions, long uniqueSolutions, long duration) {
	setProgress(progress);
	setSolutions(solutions);
	setUniqueSolutions(uniqueSolutions);
	setDuration(duration);
    }
    
    public void setProgress(float progress) {
	var oldValue = this.progress;
	this.progress = progress;
	prop.firePropertyChange("progress", oldValue, progress);
    }

    public float getProgress() {
	return progress;
    }
    
    public void setSolutions(long solutions) {
	var oldValue = this.solutions;
	this.solutions = solutions;
	prop.firePropertyChange("solutions", oldValue, solutions);
    }
    
    public long getSolutions() {
	return solutions;
    }

    public void setUniqueSolutions(long uniqueSolutions) {
	var oldValue = this.uniqueSolutions;
	this.uniqueSolutions = uniqueSolutions;
	prop.firePropertyChange("uniqueSolutions", oldValue, uniqueSolutions);
    }

    public long getUniqueSolutions() {
	return uniqueSolutions;
    }
    
    public void setDuration(long duration) {
	var oldValue = this.duration;
	this.duration = duration;
	prop.firePropertyChange("duration", oldValue, duration);
    }

    public long getDuration() {
	return duration;
    }
    
    // listeners
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
	prop.addPropertyChangeListener(propertyName, l);
    }
    
    // classes and types
    public static class Settings {
	private int updateInterval;

	public Settings(int updateInterval) {
	    this.updateInterval = updateInterval;
	}

	public int getUpdateInterval() {
	    return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
	    this.updateInterval = updateInterval;
	}
    }
}