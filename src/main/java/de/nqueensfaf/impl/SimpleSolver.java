package de.nqueensfaf.impl;

import static de.nqueensfaf.SolverStatus.*;

import de.nqueensfaf.Solver;
import de.nqueensfaf.SolverStatus;

public class SimpleSolver implements Solver {

    private int n = 0;
    private long solutions = 0;
    private long duration = 0;
    private SolverStatus status = NOT_INITIALIZED;
    
    public SimpleSolver() {
    }

    @Override
    public void setN(int n) {
	if(n <= 0 || n >= 32)
	    throw new IllegalArgumentException("invalid value for n: 0 < " + n + " < 32 does not apply");
	this.n = n;
	status = READY;
    }

    @Override
    public int getN() {
	return n;
    }

    @Override
    public void solve() {
	status = RUNNING;
	
	long start = System.currentTimeMillis();

	int mask = (1 << n) - 1;
	backtrack(0, 0, 0, 0, mask, mask);
	
	duration = System.currentTimeMillis() - start;
	
	status = FINISHED;
    }

    private void backtrack(int ld, int rd, int col, int row, int free, int mask) {
	if (row == n - 1) {
	    solutions++;
	    return;
	}

	int bit;
	int nextfree;

	while (free > 0) {
	    bit = free & (-free);
	    free -= bit;
	    nextfree = ~((ld | bit) << 1 | (rd | bit) >> 1 | col | bit) & mask;

	    if (nextfree > 0)
		backtrack((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree, mask);
	}
    }
    
    @Override
    public long getSolutions() {
	return solutions;
    }

    @Override
    public long getDuration() {
	return duration;
    }

    @Override
    public SolverStatus getStatus() {
	return status;
    }

}
