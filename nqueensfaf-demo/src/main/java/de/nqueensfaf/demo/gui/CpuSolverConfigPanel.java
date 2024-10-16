package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.CpuSolver;

@SuppressWarnings("serial")
class CpuSolverConfigPanel extends SolverImplConfigPanel {

    private final CpuSolverWithConfig model = new CpuSolverWithConfig();
    
    private final PropertyGroupConfigUi propConfigUi;
    
    public CpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	propConfigUi.addIntProperty("threads", "Threads", 1, 
		Runtime.getRuntime().availableProcessors() * 2, 1, 1);
	propConfigUi.addPropertyChangeListener(
		"threads", e -> model.setThreadCount((int) e.getNewValue()));
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 5, 1);
	propConfigUi.addPropertyChangeListener(
		"prequeens", e -> model.setPresetQueens((int) e.getNewValue()));
	propConfigUi.fillRemainingVerticalSpace();
    }

    @Override
    SolverImplWithConfig getModel() {
	return model;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
	propConfigUi.setEnabled(enabled);
    }
    
    class CpuSolverWithConfig implements SolverImplWithConfig {
	
	private final CpuSolver solver = new CpuSolver();
	
	@Override
	public AbstractSolver getSolver() {
	    return solver;
	}

	@Override
	public String checkConfigValid() {
	    if(solver.getN() <= 6)
		return "This solver is only applicable for N >= 6";
	    
	    if(solver.getPresetQueens() >= solver.getN() - 1)
		return "Number of pre placed queens must be lower than N - 1";
	    return "";
	}
	
	@Override
	public String getName() {
	    return "CPU";
	}
	
	@Override
	public String toString() {
	    if(solver.getThreadCount() == 1)
		return "CPU: Single-Core";
	    return "CPU: " + solver.getThreadCount() + " Threads";
	}
	
	@Override
	public String getDiscipline() {
	    if(solver.getThreadCount() == 1)
		return "CPU: Single-Core";
	    return "CPU: Multithreaded";
	}
	
	void setThreadCount(int threads) {
	    solver.setThreadCount(threads);
	}
	
	int getThreadCount() {
	    return solver.getThreadCount();
	}
	
	void setPresetQueens(int prequeens) {
	    solver.setPresetQueens(prequeens);
	}
	
	int getPresetQueens() {
	    return solver.getPresetQueens();
	}

	@Override
	public void loaded() {
	    propConfigUi.getProperty("prequeens").setEnabled(false);
	}
    }
}
