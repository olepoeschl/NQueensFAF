package de.nqueensfaf.impl;

public class ConstellationResult {

    private long solutions;
    
    public ConstellationResult() {
    }

    public ConstellationResult(long solutions) {
	this.solutions = solutions;
    }
    
    public void setSolutions(long solutions) {
	this.solutions = solutions;
    }
    
    public long getSolutions() {
	return solutions;
    }
}
