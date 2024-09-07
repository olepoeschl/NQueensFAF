package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.CpuSolver;

class CpuSolverConfigPanel extends SolverImplConfigPanel {

    private final CpuSolverConfig model = new CpuSolverConfig();
    
    private final PropertyGroupConfigUi propConfigUi;
    
    public CpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	propConfigUi.addIntProperty("threads", "Threads", 1, 
		Runtime.getRuntime().availableProcessors() * 2, 1, 1);
	propConfigUi.addPropertyChangeListener(
		"threads", e -> model.setThreadCount((int) e.getNewValue()));
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 4, 1);
	propConfigUi.addPropertyChangeListener(
		"prequeens", e -> model.setPresetQueens((int) e.getNewValue()));
	propConfigUi.fillRemainingVerticalSpace();
    }

    @Override
    SolverImplConfig getModel() {
	return model;
    }
    
    class CpuSolverConfig implements SolverImplConfig {
	
	private final CpuSolver solver = new CpuSolver();
	
	@Override
	public AbstractSolver getConfiguredSolver() {
	    return solver;
	}

	@Override
	public String checkValid() {
	    if(solver.getPresetQueens() >= solver.getN() - 1)
		return "Number of pre placed queens must be lower than N - 1";
	    return "";
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
    }
}
