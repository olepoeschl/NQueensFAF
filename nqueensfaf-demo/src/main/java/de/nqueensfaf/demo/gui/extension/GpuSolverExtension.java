package de.nqueensfaf.demo.gui.extension;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.GpuSolver;

public class GpuSolverExtension implements SolverExtension {

    private final GpuSolver solver = new GpuSolver();
    
    public GpuSolverExtension() {}

    @Override
    public AbstractSolver getSolver() {
	return solver;
    }

    @Override
    public String getName() {
	return "GPU";
    }

}
