package de.nqueensfaf.demo.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import de.nqueensfaf.core.ExecutionState;
import de.nqueensfaf.demo.gui.extension.SimpleRecursiveSolverExtension;
import de.nqueensfaf.demo.gui.extension.SolverExtension;
import de.nqueensfaf.impl.SymSolver;

public class Model {

    private final PropertyChangeSupport prop = new PropertyChangeSupport(this);
    
    private Settings settings = new Settings(16, 120, 0); 
    
    private final SolverExtension[] solverExtensions;
    private int selectedSolverExtensionIdx = 0;
    
    private final SymSolver[] symSolvers;

    private float progress;
    private long solutions;
    private long uniqueSolutions;
    private long duration;
    
    public Model() {
	solverExtensions = new SolverExtension[3];
	solverExtensions[0] = new SimpleRecursiveSolverExtension();
//	solverExtensions[1] = new CpuSolverExtension();
//	solverExtensions[2] = new GpuSolverExtension();
	
	symSolvers = new SymSolver[3];
	for(int i = 0; i < symSolvers.length; i++)
	    symSolvers[i] = new SymSolver();
    }
    
    // configuration for external access
    public Settings getSettings() {
	return settings;
    }
    
    public void applyAppConfig(Settings settings) {
	var oldValue = this.settings;
	this.settings = settings;
	prop.firePropertyChange("settings", oldValue, settings);
    }
    
    public SolverExtension getSelectedSolverExtension() {
	return solverExtensions[selectedSolverExtensionIdx];
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

    public SymSolver getConfiguredSymSolver() {
	var symSolver = symSolvers[selectedSolverExtensionIdx];
	symSolver.setN(selectedSolverExtensionIdx);
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
    
    // current solver progress properties
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
    
}
