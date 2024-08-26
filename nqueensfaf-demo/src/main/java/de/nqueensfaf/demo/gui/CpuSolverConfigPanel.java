package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.CpuSolver;

class CpuSolverConfigPanel extends SolverImplConfigPanel {

    private final CpuSolver solver = new CpuSolver();
    private PropertyGroupConfigUi configs;
    
    CpuSolverConfigPanel() {
	super();
	initUi();
    }
    
    private void initUi() {
	setLayout(new BorderLayout());
	configs = new PropertyGroupConfigUi(this);
	configs.addIntProperty("Threads", 1, Runtime.getRuntime().availableProcessors() * 2, 1, 1);
	configs.addIntProperty("Preplaced Queens", 4, 10, 4, 1);
    }
    
    @Override
    AbstractSolver getConfiguredSolver() {
	solver.setThreadCount((int) configs.getProperty("Threads"));
	solver.setPresetQueens((int) configs.getProperty("Preplaced Queens"));
	return solver;
    }

}
