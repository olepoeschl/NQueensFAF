package de.nqueensfaf.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import de.nqueensfaf.AbstractSolver;
import de.nqueensfaf.SolverStatus;

public class CpuSolver extends AbstractSolver implements Stateful {

    private ArrayList<Constellation> constellations = new ArrayList<Constellation>();
    private ArrayList<ArrayList<Constellation>> threadConstellations = new ArrayList<ArrayList<Constellation>>();
    private long start, duration, storedDuration;
    private boolean stateLoaded, ready = true;
    private int presetQueens = 5, threadCount = 1;

    public void reset() {
	constellations.clear();
	threadConstellations.clear();
	start = duration = storedDuration = 0;
	stateLoaded = false;
	ready = true;
    }
    
    @Override
    public SolverState getState() {
	return new SolverState(getN(), getDuration(), constellations);
    }

    @Override
    public void setState(SolverState state) {
	if(!ready)
	    throw new IllegalStateException("could not set solver state: solver was already used and must be reset first");
	setN(state.getN());
	storedDuration = state.getStoredDuration();
	constellations = state.getConstellations();
	stateLoaded = true;
    }

    @Override
    public long getDuration() {
	if (getStatus().isBefore(SolverStatus.FINISHED) && start != 0) {
	    return System.currentTimeMillis() - start + storedDuration;
	}
	return duration;
    }

    @Override
    public float getProgress() {
	if(constellations.size() == 0)
	    return 0;
	
	int solvedConstellations = 0;
	for (var c : constellations) {
	    if (c.extractStart() == 69) // start=69 is for pseudo constellations
		continue;
	    if (c.getSolutions() >= 0) {
		solvedConstellations++;
	    }
	}
	return (float) solvedConstellations / constellations.size();
    }

    @Override
    public long getSolutions() {
	if(constellations.size() == 0)
	    return 0;
	
	return constellations.stream().filter(c -> c.getSolutions() >= 0).map(c -> c.getSolutions())
		.reduce(0l, (cAcc, c) -> cAcc + c);
    }

    @Override
    public void solve() {
	ready = false;
	
	start = System.currentTimeMillis();
	if (getN() <= 6) { // if n is very small, use the simple Solver from the parent class
	    AbstractSolver simpleSolver = new SimpleSolver(getN());
	    simpleSolver.start();
	    
	    long solutions = simpleSolver.getSolutions();
	    constellations.add(new Constellation(0, 0, 0, 0, 0, solutions));
	    duration = simpleSolver.getDuration();
	    return;
	}

	if (!stateLoaded)
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);

	// split starting constellations in [threadcount] lists (splitting the work for
	// the threads)
	for (int i = 0; i < threadCount; i++) {
	    threadConstellations.add(new ArrayList<Constellation>());
	}
	int i = constellations.size() - 1;
	for (Constellation c : constellations) {
	    if (c.getSolutions() >= 0) // ignore loaded constellations that have already been solved
		continue;
	    threadConstellations.get((i--) % threadCount).add(c);
	}

	// start the threads and wait until they are all finished
	ExecutorService executor = Executors.newFixedThreadPool(threadCount);
	for (i = 0; i < threadCount; i++) {
	    CpuSolverThread cpuSolverThread = new CpuSolverThread(getN(), threadConstellations.get(i));
	    executor.submit(cpuSolverThread);
	}

	// wait for the threads to finish
	executor.shutdown();
	try {
	    if (executor.awaitTermination(365, TimeUnit.DAYS)) {
		// finished
		duration = System.currentTimeMillis() - start + storedDuration;
	    }
	} catch (InterruptedException e) {
	    throw new RuntimeException("could not wait for solver cpu threads to terminate: " + e.getMessage(), e);
	}
	stateLoaded = false;
    }
    
    // setters and getters
    public CpuSolver setPresetQueens(int presetQueens) {
	this.presetQueens = presetQueens;
	return this;
    }
    
    public int getPresetQueens() {
	return presetQueens;
    }
    
    public CpuSolver setThreadCount(int threadCount) {
	if (threadCount < 1)
	    throw new IllegalArgumentException("invalid value for thread count: not a number >0");
	this.threadCount = threadCount;
	return this;
    }
    
    public int getThreadCount() {
	return threadCount;
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
}
