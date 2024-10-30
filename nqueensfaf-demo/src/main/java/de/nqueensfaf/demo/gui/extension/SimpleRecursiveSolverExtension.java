package de.nqueensfaf.demo.gui.extension;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.SimpleSolver;

public class SimpleRecursiveSolverExtension implements SolverExtension {

    private final SimpleSolver solver = new SimpleSolver(); 
    
    public SimpleRecursiveSolverExtension() {
    }

    @Override
    public AbstractSolver getSolver() {
	return solver;
    }

    @Override
    public String getName() {
	return "SimpleRec";
    }

}
