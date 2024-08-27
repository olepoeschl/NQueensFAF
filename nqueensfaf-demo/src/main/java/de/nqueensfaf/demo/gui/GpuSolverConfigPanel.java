package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.SolverSelectionPanel.SolverImplConfigPanel;
import de.nqueensfaf.impl.CpuSolver;
import de.nqueensfaf.impl.GpuSolver;

class GpuSolverConfigPanel extends SolverImplConfigPanel {

    private final GpuSolver solver = new GpuSolver();
    
    public GpuSolverConfigPanel() {
	var propConfigUi = new PropertyGroupConfigUi(this);
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 4, 1);
	propConfigUi.addPropertyChangeListener(
		"prequeens", e -> solver.setPresetQueens((int) e.getNewValue()));
    }
    
    @Override
    AbstractSolver getConfiguredSolver() {
	return solver;
    }

}
