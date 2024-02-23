package de.nqueensfaf.impl;

import de.nqueensfaf.Solver;

public class Bitsolver extends Solver{
	
	private int N, mask;
	private long start, sol, time;

	public Bitsolver(int N) {
		this.N = N;
		this.sol = 0;
		this.mask = (1 << N) - 1;
	}
	
	private void solve_board() {
		this.start = System.currentTimeMillis();
		this.sq(0, 0, ~mask, mask, 0);
		this.time = System.currentTimeMillis() - start;
	}
	
	private void sq(int ld, int rd, int col, int free, int row) {
		if(row == N-1) {
			sol++;
			return;
		}
		int bit, nextfree, next_ld, next_rd, next_col;
		while(free != 0) {
			bit = free & (-free);
			free -= bit;
			
			next_ld = ((ld | bit) << 1);
		    next_rd = ((rd | bit) >>> 1);
		    next_col = (col | bit);
		    
		    nextfree = ~(next_ld | next_rd | next_col);
		    
		    if (nextfree > 0) {
		    	if (row < N - 2) {
		    		if (~((next_ld << 1) | (next_rd >>> 1) | (next_col)) > 0)
		    			sq(next_ld, next_rd, next_col, nextfree, row+1);
		    	} else {
		    		sq(next_ld, next_rd, next_col, nextfree, row+1);
		    	}
		    }
		}
	}
		
	@Override
	public long getDuration() {
		return 0;
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
	protected void run() {
	}
	
	public static void main(String[] args){
		for(int N = 1; N <= 19; N++) {
			Bitsolver B = new Bitsolver(N);
			B.solve_board();
			System.out.println("N = " + B.N + ": " + B.sol + " solutions in " + B.time + "ms");
		}
	}
}
