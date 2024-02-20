package de.nqueensfaf.impl;

import java.util.Arrays;

import de.nqueensfaf.Solver;

public class BooleanLDRDCOL extends Solver{
	
	private int N;
	private long start, sol, time;
	private boolean[] ld, rd, col;
//	private int[] queens;
	
	public BooleanLDRDCOL(int N) {
		this.N = N;
		this.sol = 0;
		this.ld = new boolean[2*N-1];
		this.rd = new boolean[2*N-1];
		this.col = new boolean[N];
//		queens = new int[N];
	}
	
	private void solve_board() {
		this.start = System.currentTimeMillis();
		this.sq(0);
		this.time = System.currentTimeMillis() - start;
	}
	private void sq(int row) {
		if(row == N) {
			sol++;
//			print_queens();
			return;
		}
		for(int j = 0; j < N; j++) {
			if(check_occupancy(row, j) == false) {
//				queens[row] = col;
				set_queen(row, j);
				sq(row+1);
				remove_queen(row, j);
			}
		}
	}
	private void set_queen(int i, int j) {
		col[j] = true;
		ld[i+j] = true;
		rd[i-j+N-1] = true;
	}
	private void remove_queen(int i, int j) {
		col[j] = false;
		ld[i+j] = false;
		rd[i-j+N-1] = false;
	}
	private boolean check_occupancy(int i, int j) {
		return (col[j] || ld[i+j] || rd[i-j+N-1]);
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
		return sol;
	}

	@Override
	protected void run() {
		return;
	}
	
	public static void main(String[] args){
		for(int N = 1; N <= 16; N++) {
			BooleanLDRDCOL B = new BooleanLDRDCOL(N);
			B.solve_board();
			System.out.println("N = " + B.N + ": " + B.sol + " solutions in " + B.time + "ms");
		}
	}

}