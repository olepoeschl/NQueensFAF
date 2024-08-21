package de.nqueensfaf.impl;

import de.nqueensfaf.core.AbstractSolver;

public class SimpleSolver extends AbstractSolver {

    private long solutions = 0;
    private long duration = 0;

    public SimpleSolver(int n) {
	setN(n);
    }

    @Override
    public void solve() {
	long start = System.currentTimeMillis();

	int mask = (1 << getN()) - 1;
	backtrack(0, 0, 0, 0, mask, mask);

	duration = System.currentTimeMillis() - start;
    }

    private void backtrack(int ld, int rd, int col, int row, int free, int mask) {
	if (row == getN() - 1) {
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
}
