package de.nqueensfaf.impl;

public class Constellation {

    private int id;
    private int ld;
    private int rd;
    private int col;
    private int startIjkl;
    private long solutions;

    public Constellation() {
	super();
    }

    public Constellation(int id, int ld, int rd, int col, int startijkl, long solutions) {
	this.id = id;
	this.ld = ld;
	this.rd = rd;
	this.col = col;
	this.startIjkl = startijkl;
	this.solutions = solutions;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
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

    public int getStartIjkl() {
	return startIjkl;
    }

    public void setStartIjkl(int startIjkl) {
	this.startIjkl = startIjkl;
    }

    public long getSolutions() {
	return solutions;
    }

    public void setSolutions(long solutions) {
	this.solutions = solutions;
    }
    
    public int extractIjkl() {
	return startIjkl & 0b11111111111111111111;
    }
    
    public int extractStart() {
	return startIjkl >> 20;
    }
}
