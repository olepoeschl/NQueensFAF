package de.nqueensfaf.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.nqueensfaf.Config;
import de.nqueensfaf.Constants;
import de.nqueensfaf.Solver;
import de.nqueensfaf.persistence.Constellation;
import de.nqueensfaf.persistence.SolverState;

public class CPUSolver extends Solver {

    // for very small N it is overkill to use this method
    // thus we use a straightforward recursive implementation of Jeff Somers Bit
    // method for such N
    // smallestN marks the border, when to use this simpler solver
    private static final int smallestN = 6;

    private int L, mask, LD, RD, counter;
    private long start, end;
    private HashSet<Integer> ijklList;
    private ArrayList<Constellation> constellations;
    private ArrayList<CPUSolverThread> threads;
    private ArrayList<ArrayList<Constellation>> threadConstellations;
    private long solutions, duration, storedDuration;
    private float progress;
    private boolean injected;

    private CPUSolverConfig config;
    private SolverUtils utils;

    public CPUSolver() {
	config = new CPUSolverConfig();
	utils = new SolverUtils();
	ijklList = new HashSet<Integer>();
	constellations = new ArrayList<Constellation>();
	threads = new ArrayList<CPUSolverThread>();
	injected = false;
    }

    @Override
    protected void run() {
	start = System.currentTimeMillis();
	if (N <= smallestN) { // if N is very small, use the simple Solver from the parent class
	    solutions = solveSmallBoard();
	    end = System.currentTimeMillis();
	    progress = 1;
	    return;
	}

	utils.setN(N);
	if (!injected) {
	    genConstellations();
	}

	// split starting constellations in [threadcount] lists (splitting the work for
	// the threads)
	threadConstellations = new ArrayList<ArrayList<Constellation>>();
	for (int i = 0; i < config.threadcount; i++) {
	    threadConstellations.add(new ArrayList<Constellation>());
	}
	int i = constellations.size() - 1;
	for (Constellation c : constellations) {
	    if (c.getSolutions() >= 0) // ignore injected constellations that have already been solved
		continue;
	    threadConstellations.get((i--) % config.threadcount).add(c);
	}

	// start the threads and wait until they are all finished
	ExecutorService executor = Executors.newFixedThreadPool(config.threadcount);
	for (i = 0; i < config.threadcount; i++) {
	    CPUSolverThread cpuSolverThread = new CPUSolverThread(utils, N, threadConstellations.get(i));
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
	} catch (InterruptedException e1) {
//	    e.printStackTrace();
	    Thread.currentThread().interrupt();
	}
	injected = false;
    }

    public CPUSolver config(Consumer<CPUSolverConfig> configConsumer) {
	var tmp = new CPUSolverConfig();
	tmp.from(config);
	
	configConsumer.accept(tmp);
	tmp.validate();
	
	config.from(tmp); // if given config is valid, apply it
	return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CPUSolverConfig getConfig() {
	return config;
    }

    @Override
    protected void store_(String filepath) throws IOException {
	// if Solver was not even started yet, throw exception
	if (constellations.size() == 0) {
	    throw new IllegalStateException("Nothing to be saved");
	}
	Kryo kryo = Constants.kryo;
	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(filepath)))) {
	    kryo.writeObject(output,
		    new SolverState(N, System.currentTimeMillis() - start + storedDuration, constellations));
	    output.flush();
	}
    }

    @Override
    protected void inject_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
	if (!isIdle()) {
	    throw new IllegalStateException("Cannot inject while the Solver is running");
	}
	Kryo kryo = Constants.kryo;
	try (Input input = new Input(new GZIPInputStream(new FileInputStream(filepath)))) {
	    SolverState state = kryo.readObject(input, SolverState.class);
	    setN(state.getN());
	    storedDuration = state.getStoredDuration();
	    constellations = state.getConstellations();
	    injected = true;
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

    private void genConstellations() {
	// halfN half of N rounded up
	final int halfN = (N + 1) / 2;
	L = 1 << (N - 1);
	mask = (1 << N) - 1;

	// calculate starting constellations for no Queens in corners
	for (int k = 1; k < halfN; k++) { // go through first col
	    for (int l = k + 1; l < N - 1; l++) { // go through last col
		for (int i = k + 1; i < N - 1; i++) { // go through first row
		    if (i == N - 1 - l) // skip if occupied
			continue;
		    for (int j = N - k - 2; j > 0; j--) { // go through last row
			if (j == i || l == j)
			    continue;

			if (!utils.checkRotations(ijklList, i, j, k, l)) { // if no rotation-symmetric starting
							   // constellation already
							   // found
			    ijklList.add(utils.toijkl(i, j, k, l));
			}
		    }
		}
	    }
	}
	// calculating start constellations with the first Queen on the corner square
	// (0,0)
	for (int j = 1; j < N - 2; j++) { // j is idx of Queen in last row
	    for (int l = j + 1; l < N - 1; l++) { // l is idx of Queen in last col
		ijklList.add(utils.toijkl(0, j, 0, l));
	    }
	}

	HashSet<Integer> ijklListJasmin = new HashSet<Integer>();
	// rotate and mirror all start constellations, such that the queen in the last
	// row is as close to the right border as possible
	for (int startConstellation : ijklList) {
	    ijklListJasmin.add(utils.jasmin(startConstellation));
	}
	ijklList = ijklListJasmin;

	int i, j, k, l, ld, rd, col, currentSize = 0;
	for (int sc : ijklList) {
	    i = utils.geti(sc);
	    j = utils.getj(sc);
	    k = utils.getk(sc);
	    l = utils.getl(sc);
	    // fill up the board with preQueens queens and generate corresponding variables
	    // ld, rd, col, start_queens_ijkl for each constellation
	    // occupy the board corresponding to the queens on the borders of the board
	    // we are starting in the first row that can be free, namely row 1
	    ld = (L >>> (i - 1)) | (1 << (N - k));
	    rd = (L >>> (i + 1)) | (1 << (l - 1));
	    col = 1 | L | (L >>> i) | (L >>> j);
	    // occupy diagonals of the queens j k l in the last row
	    // later we are going to shift them upwards the board
	    LD = (L >>> j) | (L >>> l);
	    RD = (L >>> j) | (1 << k);

	    // counts all subconstellations
	    counter = 0;
	    // generate all subconstellations
	    setPreQueens(ld, rd, col, k, l, 1, j == N - 1 ? 3 : 4);
	    currentSize = constellations.size();
	    // jkl and sym and start are the same for all subconstellations
	    for (int a = 0; a < counter; a++) {
		constellations.get(currentSize - a - 1)
			.setStartijkl(constellations.get(currentSize - a - 1).getStartijkl() | utils.toijkl(i, j, k, l));
	    }
	}
    }

    // generate subconstellations for each starting constellation with 3 or 4 queens
    private void setPreQueens(int ld, int rd, int col, int k, int l, int row, int queens) {
	// in row k and l just go further
	if (row == k || row == l) {
	    setPreQueens(ld << 1, rd >>> 1, col, k, l, row + 1, queens);
	    return;
	}
	// add queens until we have preQueens queens
	if (queens == config.presetQueens) {
	    // add the subconstellations to the list
	    constellations.add(new Constellation(-1, ld, rd, col, row << 20, -1));
	    counter++;
	    return;
	}
	// if not done or row k or l, just place queens and occupy the board and go
	// further
	else {
	    int free = (~(ld | rd | col | (LD >>> (N - 1 - row)) | (RD << (N - 1 - row)))) & mask;
	    int bit;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		setPreQueens((ld | bit) << 1, (rd | bit) >>> 1, col | bit, k, l, row + 1, queens + 1);
	    }
	}
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
		throw new IllegalArgumentException("invalid value for threadcount: only numbers >0 are allowed");
	    if (presetQueens < 4)
		throw new IllegalArgumentException("invalid value for presetQueens: only numbers >=4 are allowed");
	}

	public void from(CPUSolverConfig config) {
	    super.from(config);
	    threadcount = config.threadcount;
	    presetQueens = config.presetQueens;
	}
    }
}
