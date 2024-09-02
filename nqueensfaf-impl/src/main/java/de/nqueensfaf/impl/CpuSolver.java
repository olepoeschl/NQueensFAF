package de.nqueensfaf.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.core.ExecutionState;

public class CpuSolver extends AbstractSolver {

    private List<Constellation> constellations = new ArrayList<Constellation>();
    private final List<List<Constellation>> threadConstellations = new ArrayList<List<Constellation>>();
    private long start, duration, storedDuration;
    private boolean stateLoaded;
    private int presetQueens = 5, threadCount = 1;

    private final Kryo kryo = new Kryo();

    private record CpuSolverProgressState(int n, long storedDuration, List<Constellation> constellations) {
    }

    public CpuSolver() {
	kryo.register(CpuSolverProgressState.class);
	kryo.register(ArrayList.class);
	kryo.register(Constellation.class);
    }

    @Override
    public void save(String path) throws IOException {
	if (!getExecutionState().isBusy())
	    throw new IllegalStateException("progress of CpuSolver can only be saved during the solving process");

	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(path)))) {
	    kryo.writeObject(output, new CpuSolverProgressState(getN(), getDuration(), constellations));
	    output.flush();
	} catch (IOException e) {
	    throw new IOException("could not write cpu solver progress to file: " + e.getMessage(), e);
	}
    }

    @Override
    public void load(String path) throws IOException {
	if (!getExecutionState().isIdle())
	    throw new IllegalStateException("solver progress can only be restored from a file when idle");

	try (Input input = new Input(new GZIPInputStream(new FileInputStream(path)))) {
	    CpuSolverProgressState progress = kryo.readObject(input, CpuSolverProgressState.class);
	    load(progress.n(), progress.storedDuration(), progress.constellations());
	} catch (Exception e) {
	    throw new IOException("could not read solver state from file: " + e.getMessage(), e);
	}
    }

    public void load(int n, long storedDuration, List<Constellation> constellations) {
	if (!getExecutionState().isIdle())
	    throw new IllegalStateException("solver progress can only be injected when idle");

	setN(n);
	this.storedDuration = storedDuration;
	this.constellations = constellations;
	stateLoaded = true;
    }

    @Override
    public long getDuration() {
	if (getExecutionState().isBefore(ExecutionState.FINISHED) && start != 0) {
	    return System.currentTimeMillis() - start + storedDuration;
	}
	return duration;
    }

    @Override
    public float getProgress() {
	if (constellations.size() == 0)
	    return 0;

	int solvedConstellations = 0;
	for (var c : constellations) {
	    if (c.getStart() == 69) // start=69 is for pseudo constellations
		continue;
	    if (c.getSolutions() >= 0) {
		solvedConstellations++;
	    }
	}
	return (float) solvedConstellations / constellations.size();
    }

    @Override
    public long getSolutions() {
	if (constellations.size() == 0)
	    return 0;

	return constellations.stream().filter(c -> c.getSolutions() >= 0).map(c -> c.getSolutions()).reduce(0l,
		(cAcc, c) -> cAcc + c);
    }

    @Override
    public void solve() {
	if (getN() <= 6) { // if n is very small, use the simple Solver from the parent class
	    AbstractSolver simpleSolver = new SimpleSolver(getN());
	    simpleSolver.start();

	    long solutions = simpleSolver.getSolutions();
	    constellations.add(new Constellation(0, 0, 0, 0, solutions));
	    duration = simpleSolver.getDuration();
	    return;
	}

	start = System.currentTimeMillis();
	duration = 0;
	threadConstellations.clear();

	if (!stateLoaded) {
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);
	    storedDuration = 0;
	} else {
	    stateLoaded = false;
	}

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
    public void setPresetQueens(int presetQueens) {
	this.presetQueens = presetQueens;
    }

    public int getPresetQueens() {
	return presetQueens;
    }

    public void setThreadCount(int threadCount) {
	if (threadCount < 1)
	    throw new IllegalArgumentException("invalid value for thread count: not a number >0");
	this.threadCount = threadCount;
    }

    public int getThreadCount() {
	return threadCount;
    }

    // debug info
    public int getTotalNumberOfConstellations() {
	return constellations.size();
    }

    public LinkedHashMap<Integer, Long> getSolutionsPerIjkl() {
	LinkedHashMap<Integer, Long> solutionsPerIjkl = new LinkedHashMap<Integer, Long>();
	constellations.stream().collect(Collectors.groupingBy(Constellation::getIjkl)).values().stream()
		.forEach(cPerIjkl -> solutionsPerIjkl.put(cPerIjkl.get(0).getIjkl(),
			cPerIjkl.stream().map(Constellation::getSolutions).reduce(0L, Long::sum)));
	return solutionsPerIjkl;
    }
}
