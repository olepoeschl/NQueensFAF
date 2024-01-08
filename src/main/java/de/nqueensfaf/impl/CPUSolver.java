package de.nqueensfaf.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.nqueensfaf.Config;
import de.nqueensfaf.Solver;

public class CPUSolver extends Solver {

    // for very small n it is overkill to use this method
    // thus we use a straightforward recursive implementation of Jeff Somers Bit
    // method for such n
    // smallestN marks the border, when to use this simpler solver
    private static final int smallestN = 6;
    private static final Kryo kryo = new Kryo();
    static {
	kryo.register(SolverState.class);
	kryo.register(Constellation.class);
	kryo.register(ArrayList.class);
    }

    private long start, end;
    private ArrayList<Constellation> constellations;
    private ArrayList<CPUSolverThread> threads;
    private ArrayList<ArrayList<Constellation>> threadConstellations;
    private long solutions, duration, storedDuration;
    private float progress;
    private boolean loaded;

    private CPUSolverConfig config;

    public CPUSolver() {
	config = new CPUSolverConfig();
	constellations = new ArrayList<Constellation>();
	threads = new ArrayList<CPUSolverThread>();
	loaded = false;
    }

    @Override
    protected void run() {
	start = System.currentTimeMillis();
	if (n <= smallestN) { // if n is very small, use the simple Solver from the parent class
	    solutions = solveSmallBoard();
	    end = System.currentTimeMillis();
	    progress = 1;
	    return;
	}

	if (!loaded) {
	    constellations = new ConstellationsGenerator(n).generate(config.presetQueens);
	}

	// split starting constellations in [threadcount] lists (splitting the work for
	// the threads)
	threadConstellations = new ArrayList<ArrayList<Constellation>>();
	for (int i = 0; i < config.threadcount; i++) {
	    threadConstellations.add(new ArrayList<Constellation>());
	}
	int i = constellations.size() - 1;
	for (Constellation c : constellations) {
	    if (c.getSolutions() >= 0) // ignore loaded constellations that have already been solved
		continue;
	    threadConstellations.get((i--) % config.threadcount).add(c);
	}

	// start the threads and wait until they are all finished
	ExecutorService executor = Executors.newFixedThreadPool(config.threadcount);
	for (i = 0; i < config.threadcount; i++) {
	    CPUSolverThread cpuSolverThread = new CPUSolverThread(n, threadConstellations.get(i));
	    threads.add(cpuSolverThread);
	    executor.submit(cpuSolverThread);
	}

	// wait for the threads to finish
	executor.shutdown();
	try {
	    if (executor.awaitTermination(365, TimeUnit.DAYS)) {
		// finished
		end = System.currentTimeMillis();
		duration = end - start + storedDuration;
		int solvedConstellations = 0;
		for (var c : constellations) {
		    if (c.getSolutions() >= 0) {
			solutions += c.getSolutions();
			solvedConstellations++;
		    }
		}
		progress = (float) solvedConstellations / constellations.size();
	    }
	} catch (InterruptedException e) {
	    throw new RuntimeException("could not wait for solver cpu threads to terminate: " + e.getMessage());
	}
	loaded = false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CPUSolverConfig config() {
	return config;
    }

    @Override
    protected void save_(String filepath) throws IOException {
	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(filepath)))) {
	    kryo.writeObject(output,
		    new SolverState(n, System.currentTimeMillis() - start + storedDuration, constellations));
	    output.flush();
	}
    }

    @Override
    protected void load_(String filepath) throws IOException {
	try (Input input = new Input(new GZIPInputStream(new FileInputStream(filepath)))) {
	    SolverState state = kryo.readObject(input, SolverState.class);
	    setN(state.getN());
	    storedDuration = state.getStoredDuration();
	    constellations = state.getConstellations();
	    loaded = true;
	}
    }

    @Override
    public long getDuration() {
	if (isRunning()) {
	    return System.currentTimeMillis() - start + storedDuration;
	}
	return duration;
    }

    @Override
    public float getProgress() {
	if (isRunning()) {
	    int solvedConstellations = 0;
	    for (var c : constellations) {
		if (c.getSolutions() >= 0)
		    solvedConstellations++;
	    }
	    return constellations.size() > 0 ? (float) solvedConstellations / constellations.size() : 0f;
	}
	return progress;
    }

    @Override
    public long getSolutions() {
	if (isRunning()) {
	    long tmpSolutions = 0;
	    for (var c : constellations) {
		if (c.getSolutions() >= 0)
		    tmpSolutions += c.getSolutions();
	    }
	    return tmpSolutions;
	}
	return solutions;
    }
    
    // debug info
    public int getNumberOfConstellations() {
	return constellations.size();
    }

    public LinkedHashMap<Integer, Long> getSolutionsPerIjkl() {
	LinkedHashMap<Integer, Long> solutionsPerIjkl = new LinkedHashMap<Integer, Long>();
	constellations.stream().collect(Collectors.groupingBy(Constellation::extractIjkl)).values().stream()
		.forEach(cPerIjkl -> solutionsPerIjkl.put(cPerIjkl.get(0).extractIjkl(),
			cPerIjkl.stream().map(Constellation::getSolutions).reduce(0L, Long::sum)));
	return solutionsPerIjkl;
    }

    public static class CPUSolverConfig extends Config {
	public int threadcount;
	public int presetQueens;

	public CPUSolverConfig() {
	    // default values
	    super();
	    threadcount = 1;
	    presetQueens = 4;
	}

	@Override
	public void validate() {
	    super.validate();
	    if (threadcount < 1)
		throw new IllegalArgumentException("invalid value for threadcount: not a number >0");
	    if (presetQueens < 4)
		throw new IllegalArgumentException("invalid value for presetQueens: not a number >= 4");
	}
    }
}
