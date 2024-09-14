package de.nqueensfaf.demo.gui;

import java.io.IOException;

import de.nqueensfaf.core.AbstractSolver;

interface SolverImplWithConfig {

    AbstractSolver getSolver();
    
    void load(String path) throws IOException;
    
    String checkConfigValid();
}
