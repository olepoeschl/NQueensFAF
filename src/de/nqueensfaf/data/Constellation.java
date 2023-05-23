package de.nqueensfaf.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Constellation {
	
	@JsonProperty(value = "i")
	private int id;
	@JsonProperty(value = "l")
	private int ld;
	@JsonProperty(value = "r")
	private int rd;
	@JsonProperty(value = "c")
	private int col;
	@JsonProperty(value = "t")
	private int startijkl;
	@JsonProperty(value = "s")
	private long solutions;
	
	public Constellation() {
		super();
	}
	
	public Constellation(int id, int ld, int rd, int col, int startijkl, long solutions) {
		this.id = id;
		this.ld = ld;
		this.rd = rd;
		this.col = col;
		this.startijkl = startijkl;
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