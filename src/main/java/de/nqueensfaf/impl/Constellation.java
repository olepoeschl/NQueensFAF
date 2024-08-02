package de.nqueensfaf.impl;

public class Constellation implements ImmutableConstellation {

    private int ld;
    private int rd;
    private int col;
    private int startIjkl;

    public Constellation() {
    }

    public Constellation(int ld, int rd, int col, int startijkl) {
	this.ld = ld;
	this.rd = rd;
	this.col = col;
	this.startIjkl = startijkl;
    }

    public void setLd(int ld) {
	this.ld = ld;
    }

    @Override
    public int getLd() {
	return ld;
    }

    public void setRd(int rd) {
	this.rd = rd;
    }

    @Override
    public int getRd() {
	return rd;
    }

    public void setCol(int col) {
	this.col = col;
    }

    @Override
    public int getCol() {
	return col;
    }

    public void setStartIjkl(int startIjkl) {
	this.startIjkl = startIjkl;
    }

    @Override
    public int getStartIjkl() {
	return startIjkl;
    }

    @Override
    public int getStart() {
	return startIjkl >> 20;
    }

    @Override
    public int getIjkl() {
	return startIjkl & 0b11111111111111111111;
    }
}
