package de.nqueensfaf.impl;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.core.SolverExecutionState;

public class SymSolver extends AbstractSolver {

    private long start, end;
    private long solutions90, solutions180;
    private int mask, L;

    @Override
    public void solve() {
	start = System.currentTimeMillis();
	L = 1 << (getN() - 1);
	mask = (L - 1) | L;

	// occupies the middle column, if the board has odd size
	int mid = (getN() % 2) * (1 << (getN() / 2));

	// the main diagonals can only be occupied, if the corresponding queen is in the
	// middle of
	// the board
	// this can only be the case for odd n
	// the queen in the middle of the board never has to be set - if we reach the
	// middle row, we
	// already found a solution
	rot180Solver(1, L, mid, L, 1, 0);
	rot90Solver(1, L, mid, L, 1, mid, 0, 0);

	// solver for 180 degree symmetric solutions counts every 180 symmetric solution
	// 4 times and
	// every 90 degree symmetric solution 4 times
	// the solver for 90 degree symmetric solutions counts every 90 degree symmetric
	// solution
	// double
	// following values are the unique solution number with their respective
	// symmetry ONLY
	solutions180 -= solutions90;
	solutions180 /= 4;
	solutions90 /= 2;

	end = System.currentTimeMillis();
    }

    private void rot90Solver(int ld, int rd, int col, int ldbot, int rdbot, int row, int rowidx, int queens) {
	// in the mid row we are done
	if (rowidx == getN() / 2) {
	    solutions90++;
	    return;
	}
	// by rotating 90 degrees we can occupy a row before we reached it
	// in this case just skip
	if (((row >>> rowidx) & 1) > 0) {
	    rot90Solver(ld << 1, rd >>> 1, col, ldbot >>> 1, rdbot << 1, row, rowidx + 1, queens);
	    return;
	}

	// revbit is reversed bit on the board
	int bit, revbit, rowbit = (1 << rowidx), revrowbit = Integer.reverse(rowbit) >>> (32 - getN());
	int free = ~(ld | rd | col | (ldbot >>> (getN() - 1 - 2 * rowidx)) | (rdbot << (getN() - 1 - 2 * rowidx)))
		& mask;

	while ((free & mask) > 0) {

	    bit = free & (-free);
	    revbit = Integer.reverse(bit) >>> (32 - getN());
	    free &= ~bit;

	    rot90Solver((ld | bit | revbit) << 1, (rd | bit | revbit) >>> 1, col | bit | revbit | rowbit | revrowbit,
		    (ldbot | bit | revbit) >>> 1, (rdbot | bit | revbit) << 1, row | rowbit | revrowbit | bit | revbit,
		    rowidx + 1, queens + 1);
	}
    }

    // similar to 90 degree symmetric solver, just with less extra constraints
    // main idea in both solvers is, to set a queen and also the queen that comes
    // from rotation by
    // 180 degrees
    // realize occupation by solving board from top to bottom and vice versa
    // simultaneously
    private void rot180Solver(int ld, int rd, int col, int ldbot, int rdbot, int rowidx) {
	if (rowidx == getN() / 2) {
	    solutions180++;
	    return;
	}
	int free = (~(ld | rd | col | (ldbot >>> (getN() - 1 - 2 * rowidx)) | (rdbot << (getN() - 1 - 2 * rowidx))))
		& (int) mask;
	int bit, revbit;

	while ((free & mask) > 0) {
	    bit = free & (-free);
	    revbit = Integer.reverse(bit) >>> (32 - getN());
	    free &= ~bit;

	    rot180Solver((ld | bit) << 1, (rd | bit) >>> 1, col | bit | revbit, (ldbot | revbit) >>> 1,
		    (rdbot | revbit) << 1, rowidx + 1);
	}
    }

    public long getSolutions90() {
	if (getExecutionState().isBefore(SolverExecutionState.FINISHED))
	    return 0;
	return solutions90;
    }

    public long getSolutions180() {
	if (getExecutionState().isBefore(SolverExecutionState.FINISHED))
	    return 0;
	return solutions180;
    }

    public long getUniqueSolutionsTotal(long solutions) {
	if (getExecutionState().isBefore(SolverExecutionState.FINISHED))
	    return 0;
	return (solutions + 4 * solutions180 + 6 * solutions90) / 8;
    }

    @Override
    public long getDuration() {
	if (end != 0)
	    return end - start;
	else
	    return System.currentTimeMillis() - start;
    }

    @Override
    public float getProgress() {
	return 0;
    }

    @Override
    public long getSolutions() {
	return 0;
    }
}
