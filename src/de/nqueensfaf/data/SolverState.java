package de.nqueensfaf.data;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SolverState {

	@JsonProperty(value = "N", required = true)
	private int N;
	
	@JsonProperty(value = "storedDuration", required = true)
	private long storedDuration;
	
	@JsonProperty(value = "constellations", required = true)
	private ArrayList<Constellation> constellations;
	
	public SolverState() {
		super();
	}
	
	public SolverState(int n, long storedDuration, ArrayList<Constellation> constellations) {
		N = n;
		this.storedDuration = storedDuration;
		this.constellations = constellations;
	}

	public int getN() {
		return N;
	}
	public void setN(int n) {
		N = n;
	}
	public long getStoredDuration() {
		return storedDuration;
	}
	public void setStoredDuration(long storedDuration) {
		this.storedDuration = storedDuration;
	}
	public ArrayList<Constellation> getConstellations() {
		return constellations;
	}
	public void setConstellations(ArrayList<Constellation> constellations) {
		this.constellations = constellations;
	}
}
