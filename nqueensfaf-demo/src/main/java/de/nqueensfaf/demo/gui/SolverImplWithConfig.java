package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;

interface SolverImplWithConfig {

    AbstractSolver getConfiguredSolver();
    
    String checkValid();
}
