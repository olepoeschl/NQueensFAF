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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.nqueensfaf.Constants;
import de.nqueensfaf.Solver;
import de.nqueensfaf.config.Config;
import de.nqueensfaf.persistence.Constellation;
import de.nqueensfaf.persistence.SolverState;

public class CPUSolver extends Solver {

    // for very small N it is overkill to use this method
    // thus we use a straightforward recursive implementation of Jeff Somers Bit
    // method for such N
    // smallestN marks the border, when to use this simpler solver
    private static final int smallestN = 6;
    // how many threads in parallel
    private int threadcount = Config.getDefaultConfig().getCPUThreadcount();
    // we fill up the board, until <preQueens> queens are set
    private int preQueens = 4, L, mask, LD, RD, counter;
    // for time measurement
    private long start, end;
    // for generating the start constellations
    // a start constellation contains the starting row, the occupancies of ld and rd
    // and col, and the values of i, j, k, l
    private HashSet<Integer> ijklList = new HashSet<Integer>();
    private ArrayList<Constellation> constellations = new ArrayList<Constellation>();
    // for the threads and their respective time measurement etc.
    private ArrayList<CPUSolverThread> threads = new ArrayList<CPUSolverThread>();
    private ArrayList<ArrayList<Constellation>> threadConstellations;
    private long solutions, duration, storedDuration;
    private float progress;
    private boolean injected = false;

    // non public constructor
    protected CPUSolver() {
    }

    // inherited functions
    @Override
    protected void run() {
	// check if run is called without calling reset after a run call had finished
	if (start != 0) {
	    throw new IllegalStateException(
		    "You first have to call reset() when calling solve() multiple times on the same object");
	}

	start = System.currentTimeMillis();
	if (N <= smallestN) { // if N is very small, use the simple Solver from the parent class
	    solutions = solveSmallBoard();
	    end = System.currentTimeMillis();
	    // simulate progress = 100
	    progress = 1;
	    return;
	}

	if (!injected) {
	    genConstellations();
	}

	// split starting constellations in [threadcount] lists (splitting the work for
	// the
	// threads)
	threadConstellations = new ArrayList<ArrayList<Constellation>>();
	for (int i = 0; i < threadcount; i++) {
	    threadConstellations.add(new ArrayList<Constellation>());
	}
	int i = constellations.size() - 1;
	for (Constellation c : constellations) {
	    if (c.getSolutions() >= 0) // ignore injected constellations that have already been
				       // solved
		continue;
	    threadConstellations.get((i--) % threadcount).add(c);
	}

	// start the threads and wait until they are all finished
	ExecutorService executor = Executors.newFixedThreadPool(threadcount);
	for (i = 0; i < threadcount; i++) {
	    CPUSolverThread cpuSolverThread = new CPUSolverThread(N, threadConstellations.get(i));
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

    @Override
    public void config(Consumer<Config> configConsumer) {
	
    }
    
    @Override
    protected void store_(String filepath) throws IOException {
	// if Solver was not even started yet, throw exception
	if (constellations.size() == 0) {
	    throw new IllegalStateException("Nothing to be saved");
	}
	Kryo kryo = Constants.kryo;
	kryo.register(SolverState.class);
	try (Output output = new Output(new FileOutputStream(filepath))) {
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
	kryo.register(SolverState.class);
	try (Input input = new Input(new FileInputStream(filepath))) {
	    SolverState state = kryo.readObject(input, SolverState.class);
	    setN(state.getN());
	    storedDuration = state.getStoredDuration();
	    constellations = state.getConstellations();
	    injected = true;
	}
    }

    @Override
    public boolean isInjected() {
	return injected;
    }

    @Override
    public void reset() {
	start = 0;
	end = 0;
	duration = 0;
	storedDuration = 0;
	solutions = 0;
	progress = 0;
	ijklList.clear();
	constellations.clear();
	threads.clear();
	injected = false;
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

    // own functions
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

			if (!checkRotations(i, j, k, l)) { // if no rotation-symmetric starting
							   // constellation already
							   // found
			    ijklList.add(toijkl(i, j, k, l));
			}
		    }
		}
	    }
	}
	// calculating start constellations with the first Queen on the corner square
	// (0,0)
	for (int j = 1; j < N - 2; j++) { // j is idx of Queen in last row
	    for (int l = j + 1; l < N - 1; l++) { // l is idx of Queen in last col
		ijklList.add(toijkl(0, j, 0, l));
	    }
	}

	HashSet<Integer> ijklListJasmin = new HashSet<Integer>();
	// rotate and mirror all start constellations, such that the queen in the last
	// row is as close to the right border as possible
	for (int startConstellation : ijklList) {
	    ijklListJasmin.add(jasmin(startConstellation));
	}
	ijklList = ijklListJasmin;

	int i, j, k, l, ld, rd, col, currentSize = 0;
	for (int sc : ijklList) {
	    i = geti(sc);
	    j = getj(sc);
	    k = getk(sc);
	    l = getl(sc);
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
			.setStartijkl(constellations.get(currentSize - a - 1).getStartijkl() | toijkl(i, j, k, l));
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
	if (queens == preQueens) {
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

    // true, if starting constellation rotated by any angle has already been found
    private boolean checkRotations(int i, int j, int k, int l) {
	// rot90
	if (ijklList.contains(((N - 1 - k) << 15) + ((N - 1 - l) << 10) + (j << 5) + i))
	    return true;

	// rot180
	if (ijklList.contains(((N - 1 - j) << 15) + ((N - 1 - i) << 10) + ((N - 1 - l) << 5) + N - 1 - k))
	    return true;

	// rot270
	if (ijklList.contains((l << 15) + (k << 10) + ((N - 1 - i) << 5) + N - 1 - j))
	    return true;

	return false;
    }

    // i, j, k, l to ijkl and functions to get specific entry
    private int toijkl(int i, int j, int k, int l) {
	return (i << 15) + (j << 10) + (k << 5) + l;
    }

    private int geti(int ijkl) {
	return ijkl >> 15;
    }

    private int getj(int ijkl) {
	return (ijkl >> 10) & 31;
    }

    private int getk(int ijkl) {
	return (ijkl >> 5) & 31;
    }

    private int getl(int ijkl) {
	return ijkl & 31;
    }

    // rotate and mirror board, so that the queen closest to a corner is on the
    // right side of the last row
    private int jasmin(int ijkl) {
	int min = Math.min(getj(ijkl), N - 1 - getj(ijkl)), arg = 0;

	if (Math.min(geti(ijkl), N - 1 - geti(ijkl)) < min) {
	    arg = 2;
	    min = Math.min(geti(ijkl), N - 1 - geti(ijkl));
	}
	if (Math.min(getk(ijkl), N - 1 - getk(ijkl)) < min) {
	    arg = 3;
	    min = Math.min(getk(ijkl), N - 1 - getk(ijkl));
	}
	if (Math.min(getl(ijkl), N - 1 - getl(ijkl)) < min) {
	    arg = 1;
	    min = Math.min(getl(ijkl), N - 1 - getl(ijkl));
	}

	for (int i = 0; i < arg; i++) {
	    ijkl = rot90(ijkl);
	}

	if (getj(ijkl) < N - 1 - getj(ijkl))
	    ijkl = mirvert(ijkl);

	return ijkl;
    }

    // mirror left-right
    private int mirvert(int ijkl) {
	return toijkl(N - 1 - geti(ijkl), N - 1 - getj(ijkl), getl(ijkl), getk(ijkl));
    }

    // rotate 90 degrees clockwise
    private int rot90(int ijkl) {
	return ((N - 1 - getk(ijkl)) << 15) + ((N - 1 - getl(ijkl)) << 10) + (getj(ijkl) << 5) + geti(ijkl);
    }

    // getters and setters
    public void setThreadcount(int threadcount) {
	if (threadcount < 1 || threadcount > Runtime.getRuntime().availableProcessors()) {
	    throw new IllegalArgumentException(
		    "threadcount must be a number between 1 and " + Runtime.getRuntime().availableProcessors()
			    + " (=your CPU's number of logical cores) (inclusive)");
	}
	this.threadcount = threadcount;
    }

    public int getThreadcount() {
	return threadcount;
    }
}
