package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.SimpleSolver;

@SuppressWarnings("serial")
class SimpleRecursiveSolverConfigPanel extends SolverImplConfigPanel {

    private final SimpleRecursiveSolverWithConfig model = new SimpleRecursiveSolverWithConfig();
	    
    public SimpleRecursiveSolverConfigPanel() {
    }

    @Override
    SolverImplWithConfig getModel() {
	return model;
    }

    class SimpleRecursiveSolverWithConfig implements SolverImplWithConfig {

	private SimpleSolver solver = new SimpleSolver();
	
	@Override
	public AbstractSolver getSolver() {
	    return solver;
	}

	@Override
	public void loaded() {
	    // TODO not supported
	}

	@Override
	public String checkConfigValid() {
	    return "";
	}

	@Override
	public String getName() {
	    return "SimpleRec";
	}

	@Override
	public String getDiscipline() {
	    return "SimpleRec";
	}
	
    }
}
