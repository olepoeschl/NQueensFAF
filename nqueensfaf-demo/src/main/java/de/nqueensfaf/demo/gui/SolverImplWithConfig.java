package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;

interface SolverImplWithConfig {

    AbstractSolver getSolver();
    
    void loaded();
    
    String checkConfigValid();
    
    String getName();
    
    String getDiscipline();
}
