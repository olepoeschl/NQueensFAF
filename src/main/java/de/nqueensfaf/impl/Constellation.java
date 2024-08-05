package de.nqueensfaf.impl;

public final class Constellation {

	private int ld, rd, col, startIjkl;
	private long solutions;

	// default constructor needed for Kryo deserialization
	public Constellation() {
	}

	public Constellation(int ld, int rd, int col, int startIjkl) {
		this.ld = ld;
		this.rd = rd;
		this.col = col;
		this.startIjkl = startIjkl;
	}

	public Constellation(int ld, int rd, int col, int startIjkl, long solutions) {
		this(ld, rd, col, startIjkl);
		this.solutions = solutions;
	}

	public int getLd() {
		return ld;
	}

	public int getRd() {
		return rd;
	}

	public int getCol() {
		return col;
	}

	public int getStartIjkl() {
		return startIjkl;
	}

	public final int getStart() {
		return startIjkl >> 20;
	}

	public final int getIjkl() {
		return startIjkl & ((1 << 20) - 1);
	}

	public void setSolutions(long solutions) {
		this.solutions = solutions;
	}

	public long getSolutions() {
		return solutions;
	}
}
