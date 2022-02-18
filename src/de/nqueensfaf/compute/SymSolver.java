package de.nqueensfaf.compute;

import de.nqueensfaf.Solver;
import java.io.IOException;


public class SymSolver extends Solver{
	
	private long start, end;
	private long solutions90, solutions180, solutionsUnique; 
	private int mask, L;

	@Override
	protected void run() {
		if(start != 0) {
			throw new IllegalStateException("You first have to call reset() when calling solve() multiple times on the same object");
		}

		start = System.currentTimeMillis();
		L = 1 << (N - 1);
		mask = (L - 1) | L;
		int mid = (N % 2) * (1 << (N/2));
		
		rot180Solver(1, L, mid, L, 1, 0);
		rot90Solver(1, L, mid, L, 1, mid, 0, 0);
		
		solutions180 -= solutions90;
		solutions180 /= 4;
		solutions90 /= 2;
		
		solutionsUnique = 0 + solutions180 + solutions90;
	}
	
	private void rot90Solver(int ld, int rd, int col, int ldbot, int rdbot, int row, int rowidx, int queens) {
		
		if(rowidx == N/2) {
			solutions90++;
			return;
		}
		
		if(((row >>>rowidx) & 1) > 0) {
			rot90Solver(ld << 1, rd >>>1, col, ldbot >>>1, rdbot << 1, row, rowidx+1, queens);
			return;
		}
		
		int bit, revbit, rowbit = (1 << rowidx), revrowbit = Integer.reverse(rowbit) >>> (32-N);
		int free = ~( ld | rd | col | (ldbot >>>(N-1-2*rowidx)) | (rdbot << (N-1-2*rowidx)) ) & mask;
		
		while((free & mask) > 0) {
			
			bit = free & (-free);
			revbit = Integer.reverse(bit) >>> (32-N);
			free &= ~bit;
			
			rot90Solver((ld | bit | revbit) << 1, (rd | bit | revbit) >>> 1, col | bit | revbit | rowbit | revrowbit, (ldbot | bit | revbit) >>>1, (rdbot | bit | revbit) << 1, row | rowbit | revrowbit | bit | revbit, rowidx+1, queens+1);
		}
	}
	
	private void rot180Solver(int ld, int rd, int col, int ldbot, int rdbot, int rowidx) {
		if(rowidx == N/2) {
			solutions180++;
			return;
		}
		int free = ( ~( ld | rd | col | (ldbot >>>(N-1-2*rowidx)) | (rdbot << (N-1-2*rowidx)) ) ) & (int)mask;
		int bit, revbit;
		
		while((free & mask) > 0) {
			bit = free & (-free);
			revbit = Integer.reverse(bit) >>> (32-N);
			free &= ~bit;
			
			rot180Solver((ld | bit) << 1, (rd | bit) >>>1, col | bit | revbit, (ldbot | revbit) >>>1, (rdbot | revbit) << 1, rowidx+1);
		}
	}
	
	public long getSolutions90() {
		return solutions90;
	}
	
	public long getSolutions180() {
		return solutions180;
	}


	@Override
	public void reset() {
		start = 0;
		end = 0;
		solutions90 = 0;
		solutions180 = 0;
		solutionsUnique = 0;
	}

	@Override
	public long getDuration() {
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
	@Override
	protected void store_(String filepath) throws IOException {
	}
	@Override
	protected void restore_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
	}
	@Override
	public boolean isRestored() {
		return false;
	}

}
