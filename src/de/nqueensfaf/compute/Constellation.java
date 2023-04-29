package de.nqueensfaf.compute;

class Constellation {
	
	private int ld, rd, col, startijkl;
	private long solutions;
	
	Constellation(int ld, int rd, int col, int startijkl, long solutions) {
		this.ld = ld;
		this.rd = rd;
		this.col = col;
		this.startijkl = startijkl;
		this.solutions = solutions;
	}
	
	int getLd() {
		return ld;
	}
	void setLd(int ld) {
		this.ld = ld;
	}
	int getRd() {
		return rd;
	}
	void setRd(int rd) {
		this.rd = rd;
	}
	int getCol() {
		return col;
	}
	void setCol(int col) {
		this.col = col;
	}
	int getStartijkl() {
		return startijkl;
	}
	void setStartijkl(int startijkl) {
		this.startijkl = startijkl;
	}
	long getSolutions() {
		return solutions;
	}
	void setSolutions(long solutions) {
		this.solutions = solutions;
	}
}
