package de.nqueensfaf.impl;

import java.util.Arrays;

import de.nqueensfaf.Solver;

public class BooleanField extends Solver{
	
	private int N;
	private long start, sol, time;
//	private int[] queens;
	
	public BooleanField(int N) {
		this.N = N;
		this.sol = 0;
//		queens = new int[N];
	}
	
	private void solve_board() {
		this.start = System.currentTimeMillis();
		this.sq(0, new boolean[N][N]);
		this.time = System.currentTimeMillis() - start;
	}
	private void sq(int row, boolean[][] board) {
		if(row == N) {
			sol++;
//			print_queens();
			return;
		}
		boolean[][] newboard = new boolean[N][N];
		for(int col = 0; col < N; col++) {
			if(board[row][col] == false) {
//				queens[row] = col;
				newboard = set_queen(row, col, board);
				sq(row+1, newboard);
			}
		}
	}
	private boolean[][] set_queen(int i, int j, boolean[][] board) {
		boolean[][] newboard = new boolean[N][N];
		for(int k = 0; k <N; k++) {
			newboard[k] = Arrays.copyOf(board[k], N); // deep copy of the subarray
		}
//		row and col
		for(int k = 0; k < N; k++) {
			newboard[i][k] = true;
			newboard[k][j] = true;
		}
//		ld and rd
		int k = 0;
		while((i-k>=0) && (j+k<=N-1)) {
			newboard[i-k][j+k] = true;
			k++;
		}
		k = 0;
		while((i+k<=N-1) && (j-k>=0)) {
			newboard[i+k][j-k] = true;
			k++;
		}
		k = 0;
		while((i-k>=0) && (j-k>=0)) {
			newboard[i-k][j-k] = true;
			k++;
		}
		k = 0;
		while((i+k<=N-1) && (j+k<=N-1)) {
			newboard[i+k][j+k] = true;
			k++;
		}
		return newboard;
	}
	
//	private void print_queens() {
//		for(int i = 0; i < N; i++) {
//			for(int j = 0; j < N; j++) {
//				System.out.print('[');
//				if (queens[i] == j)
//					System.out.print('X');
//				else
//					System.out.print(' ');
//				System.out.print(']');
//			}
//			System.out.print('\n');
//		}
//		System.out.print('\n');
//	}
//	private void print_occupancy(boolean[][] board) {
//		for(int i = 0; i < N; i++) {
//			for(int j = 0; j < N; j++) {
//				System.out.print('[');
//				if (board[i][j] == true)
//					System.out.print('X');
//				else
//					System.out.print(' ');
//				System.out.print(']');
//			}
//			System.out.print('\n');
//		}
//		System.out.print('\n');
//	}

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
		for(int N = 1; N <= 14; N++) {
			BooleanField B = new BooleanField(N);
			B.solve_board();
			System.out.println("N = " + B.N + ": " + B.sol + " solutions in " + B.time + "ms");
		}
	}

}
