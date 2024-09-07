package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;

interface SolverImplConfig {

    AbstractSolver getConfiguredSolver();
    
    String checkValid();
}
