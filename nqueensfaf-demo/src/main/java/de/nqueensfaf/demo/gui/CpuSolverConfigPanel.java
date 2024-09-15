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

	@Override
	public void loaded() {
	    propConfigUi.getProperty("prequeens").setEnabled(false);
	}
    }
}
