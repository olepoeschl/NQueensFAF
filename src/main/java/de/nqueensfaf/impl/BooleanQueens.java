package de.nqueensfaf.impl;

import java.util.Arrays;

import de.nqueensfaf.Solver;

public class BooleanQueens extends Solver{

	private int N;
	private long start, sol, time;
	private boolean[][] queens;
	
	public BooleanQueens(int N) {
		this.N = N;
		this.sol = 0;
		this.queens = new boolean[N][N];
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
		for(int col = 0; col < N; col++) {
			if(occupied(row, col) == false) {
				queens[row][col] = true;
				sq(row+1);
				queens[row][col] = false;
			}
		}
	}
	private boolean occupied(int row, int col) {
		for(int k = row; k >= 0; k--) {
			if (queens[k][col])
				return true;
		}
		int k = 0;
		while(row-k>=0 && col-k>=0) {
			if (queens[row-k][col-k])
				return true;
			k++;
		}
		k = 0;
		while(row-k>=0 && col+k<=N-1) {
			if(queens[row-k][col+k])
				return true;
			k++;
		}
		return false;
	}
	
	private void print_queens() {
		for(int row = 0; row < N; row++) {
			for(int col = 0; col < N; col++) {
				System.out.print('[');
				if (queens[row][col])
					System.out.print('X');
				else
					System.out.print(' ');
				System.out.print(']');
			}
			System.out.print('\n');
		}
		System.out.print('\n');
	}
	private void print_occupancy(boolean[][] board) {
		for(int row = 0; row < N; row++) {
			for(int col = 0; col < N; col++) {
				System.out.print('[');
				if (board[row][col] == true)
					System.out.print('X');
				else
					System.out.print(' ');
				System.out.print(']');
			}
			System.out.print('\n');
		}
		System.out.print('\n');
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
		for(int N = 16; N <= 16; N++) {
			BooleanQueens B = new BooleanQueens(N);
			B.solve_board();
//			System.out.println("N = " + B.N + ": " + B.sol + " solutions in " + B.time + "ms");
			System.out.println(N + " " + B.time + " " + B.sol);
		}
	}

}