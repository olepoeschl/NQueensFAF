package de.nqueensfaf.demo.gui;

import java.util.List;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.MainFrame.SolverImplConfigPanel;
import de.nqueensfaf.impl.CpuSolver;

class CpuSolverConfigPanel extends SolverImplConfigPanel {

    private final CpuSolver solver = new CpuSolver();
    private final PropertyGroupConfigUi propConfigUi;
    
    public CpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	propConfigUi.addIntProperty("threads", "Threads", 1, 
		Runtime.getRuntime().availableProcessors() * 2, 1, 1);
	propConfigUi.addPropertyChangeListener(
		"threads", e -> solver.setThreadCount((int) e.getNewValue()));
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 4, 1);
	propConfigUi.addPropertyChangeListener(
		"prequeens", e -> solver.setPresetQueens((int) e.getNewValue()));
	propConfigUi.fillRemainingVerticalSpace();
    }
    
    @Override
    AbstractSolver getConfiguredSolver() {
	return solver;
    }

    @Override
    String isValidConfiguration() {
	// TODO
	return null;
    }
}
