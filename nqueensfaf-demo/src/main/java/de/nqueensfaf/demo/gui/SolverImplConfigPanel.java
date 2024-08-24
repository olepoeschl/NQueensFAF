package de.nqueensfaf.demo.gui;

import javax.swing.JPanel;

import de.nqueensfaf.core.AbstractSolver;

abstract class SolverImplConfigPanel extends JPanel {
    
    abstract AbstractSolver getConfiguredSolver();
    
}
