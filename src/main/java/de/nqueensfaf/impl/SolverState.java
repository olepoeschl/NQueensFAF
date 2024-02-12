package de.nqueensfaf.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class SolverState {

    private static transient final Kryo kryo = new Kryo();
    static {
	kryo.register(SolverState.class);
	kryo.register(Constellation.class);
	kryo.register(ArrayList.class);
    }
    
    private int N;
    private long storedDuration;
    private ArrayList<Constellation> constellations;

    public SolverState() {
	super();
    }

    public SolverState(int n, long storedDuration, List<Constellation> constellations) {
	N = n;
	this.storedDuration = storedDuration;
	this.constellations = new ArrayList<Constellation>(constellations);
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

    public void setConstellations(List<Constellation> constellations) {
	this.constellations = new ArrayList<Constellation>(constellations);
    }
    
    public void save(String path) throws IOException {
	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(path)))) {
	    kryo.writeObject(output, this);
	    output.flush();
	} catch (IOException e) {
	    throw new IOException("could not write solver state to file: " + e.getMessage(), e);
	}
    }
    
    public static SolverState load(String path) throws IOException {
	try (Input input = new Input(new GZIPInputStream(new FileInputStream(path)))) {
	    return kryo.readObject(input, SolverState.class);
	} catch (Exception e) {
	    throw new IOException("could not load solver state from file: " + e.getMessage(), e);
	}
    }
}
