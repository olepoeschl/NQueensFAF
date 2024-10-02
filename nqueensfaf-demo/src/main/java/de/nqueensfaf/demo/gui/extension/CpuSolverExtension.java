package de.nqueensfaf.demo.gui.extension;

import javax.swing.JComponent;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi1;
import de.nqueensfaf.impl.CpuSolver;

public class CpuSolverExtension implements SolverExtension {

    private final CpuSolver solver = new CpuSolver();

    private PropertyGroupConfigUi1 propConfigUi;
    
    public CpuSolverExtension() {
	createConfigUi();
    }
    
    private void createConfigUi() {
	propConfigUi = new PropertyGroupConfigUi1();
	propConfigUi.addIntProperty("threads", "Threads", 1, 
		Runtime.getRuntime().availableProcessors() * 2, 1, 1);
	propConfigUi.addPropertyChangeListener(
		"threads", e -> solver.setThreadCount((int) e.getNewValue()));
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 5, 1);
	propConfigUi.addPropertyChangeListener(
		"prequeens", e -> solver.setPresetQueens((int) e.getNewValue()));
	propConfigUi.fillRemainingVerticalSpace();
    }

    @Override
    public AbstractSolver getSolver() {
	return solver;
    }

    @Override
    public String getName() {
	return "CPU";
    }
    
    @Override
    public String getCurrentRecordCategory() {
	if(solver.getThreadCount() == 1)
	    return "CPU: Single-Core";
	return "CPU: " + solver.getThreadCount() + " Threads";
    }

    @Override
    public JComponent getConfigUi() {
	return propConfigUi.getUi();
    }
}
