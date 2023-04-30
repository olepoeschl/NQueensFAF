package de.nqueensfaf.files;

public class Constellation {
	
	private int ld, rd, col, startijkl;
	private long solutions;
	
	public Constellation() {
		super();
	}
	
	public Constellation(int ld, int rd, int col, int startijkl, long solutions) {
		this.ld = ld;
		this.rd = rd;
		this.col = col;
		this.startijkl = startijkl;
		this.solutions = solutions;
	}
	
	public int getLd() {
		return ld;
	}
	public void setLd(int ld) {
		this.ld = ld;
	}
	public int getRd() {
		return rd;
	}
	public void setRd(int rd) {
		this.rd = rd;
	}
	public int getCol() {
		return col;
	}
	public void setCol(int col) {
		this.col = col;
	}
	public int getStartijkl() {
		return startijkl;
	}
	public void setStartijkl(int startijkl) {
		this.startijkl = startijkl;
	}
	public long getSolutions() {
		return solutions;
	}
	public void setSolutions(long solutions) {
		this.solutions = solutions;
	}
}
