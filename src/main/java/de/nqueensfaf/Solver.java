package de.nqueensfaf;

public interface Solver {
    
    void setN(int n);
    
    int getN();
    
    void solve();
    
    long getSolutions();
    
    long getDuration();
    
    default float getProgress() {
	return 0f;
    }
    
    SolverStatus getSolverStatus();
    
}
