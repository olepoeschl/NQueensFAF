package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver;

class SolverModel {

    private AbstractSolver selectedSolver;
    
    private int n = 16;
    
    void setSelectedSolver(AbstractSolver solver) {
	this.selectedSolver = solver;
    }
    
    AbstractSolver getSelectedSolver() {
	return selectedSolver;
    }
    
    void setN(int n) {
	this.n = n;
    }

    int getN() {
	return n;
    }
}
