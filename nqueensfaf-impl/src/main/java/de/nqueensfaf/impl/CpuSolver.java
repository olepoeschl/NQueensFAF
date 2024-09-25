package de.nqueensfaf.impl;

import static de.nqueensfaf.impl.ConstellationUtils.getj;
import static de.nqueensfaf.impl.ConstellationUtils.getk;
import static de.nqueensfaf.impl.ConstellationUtils.getl;
import static de.nqueensfaf.impl.ConstellationUtils.symmetry;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    
    private final AtomicLong solutions = new AtomicLong(0);
    private final AtomicInteger solvedConstellations = new AtomicInteger(0); // for progress

    private final Kryo kryo = new Kryo();

    public CpuSolver() {
	kryo.register(CpuSolverProgressState.class);
	kryo.register(ArrayList.class);
	kryo.register(Constellation.class);
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
    
    @Override
    public void setN(int n) {
	if(stateLoaded)
	    throw new IllegalStateException("could not change N because a solver state was loaded");
	super.setN(n);
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
	
	// update solvedConstellations and solution count
	solutions.set(0);
	solvedConstellations.set(0);
	for (var c : constellations) {
	    if (c.getSolutions() >= 0) {
		solutions.addAndGet(c.getSolutions());
		solvedConstellations.incrementAndGet();
	    }
	}
	
	stateLoaded = true;
    }

    @Override
    public void reset() {
	solutions.set(0);
	solvedConstellations.set(0);
	duration = start = storedDuration = 0;
	threadConstellations.clear();
	constellations.clear();
	stateLoaded = false;
    }

    @Override
    public long getDuration() {
	if (getExecutionState().isBefore(ExecutionState.FINISHED) && start != 0)
	    return System.currentTimeMillis() - start + storedDuration;
	else if (getExecutionState().isIdle() && stateLoaded)
	    return storedDuration;
	return duration;
    }

    @Override
    public float getProgress() {
	if(constellations.size() == 0)
	    return 0;
	return (float) solvedConstellations.get() / constellations.size();
    }

    @Override
    public long getSolutions() {
	return solutions.get();
    }

    @Override
    public void solve() {
	duration = 0;
	threadConstellations.clear();
	start = System.currentTimeMillis();
	
	if (getN() <= 6) { // if n is very small, use the simple Solver from the parent class
	    constellations.clear();
	    solvedConstellations.set(1);
	    solutions.set(0);
	    
	    AbstractSolver simpleSolver = new SimpleSolver(getN());
	    simpleSolver.start();

	    duration = simpleSolver.getDuration();
	    constellations.add(new Constellation(0, 0, 0, 0, simpleSolver.getSolutions()));
	    solvedConstellations.set(1);
	    solutions.set(simpleSolver.getSolutions());
	    return;
	}

	if (!stateLoaded) {
	    solutions.set(0);
	    solvedConstellations.set(0);
	    storedDuration = 0;
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);
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

    private record CpuSolverProgressState(int n, long storedDuration, List<Constellation> constellations) {
    }

    // worker thread
    class CpuSolverThread extends Thread {

	private final int n, n3, n4, L, L3, L4; // boardsize
	private long tempcounter = 0; // tempcounter is #(unique solutions) of current start constellation,
	// solvecounter is #(all solutions)

	// mark1 and mark2 mark the lines k-1 and l-1 (not necessarily in this order),
	// because in from this line we will directly shift everything to the next free
	// row
	// endmark marks the row of the last free row
	// jmark marks the row j+1, where the diagonal jr from j has to be set
	private int mark1, mark2, endmark, jmark;

	// list of uncalculated starting positions, their indices
	private List<Constellation> constellations;

	CpuSolverThread(int n, List<Constellation> constellations) {
	    this.n = n;
	    n3 = n - 3;
	    n4 = n - 4;
	    L = 1 << (n - 1);
	    L3 = 1 << n3;
	    L4 = 1 << n4;
	    this.constellations = constellations;
	}

	// Recursive functions for Placing the Queens

	// IMPORTANT: since the left and right col are occupied by the
	// startConstalletaion, we only deal
	// with the bits in between,
	// hence n-2 bits for a board of size n
	// the functions recursively call themselves and travel through the board
	// row-wise
	// the occupancy of each row is represented with integers in binary
	// representation (1 occupied,
	// 0 free)
	// there are different recursive functions for different arrangements of the
	// queens i,j,k,l on
	// the border
	// in order to reduce the amount of different cases we rotate and mirror the
	// board in such a
	// way,
	// that the queen j in the last row is as close to the right corner as possible
	// this is done by the function jasmin (j as min)
	// we call this distance to the corner d and distinguish between d=0,d=1,d=2,d
	// <small enough>
	// and d <big>
	// for d <small enough> the diagonal jl from queen j going upwards to the left
	// can already be
	// set
	// in the first row of the start constellation
	// for d <big> we have to explicitly set occupy this diagonal in some row before
	// we can continue

	// NOTATION:
	// SQ stands for SetQueens and is the prefix of any of the following solver
	// functions
	// B stand for block and describes a block of free rows, where nothing special
	// has to be done
	// Blocks B are separated by the rows k and l for d<=2
	// and additionally by row jr for d <small enough> and additionally by row jl
	// for d <big>
	// jl is always first and jr is always last, k and l are in between in no fixed
	// order
	// (the last fact is a consequence of jasmin)

	// in the last function of every case, respectively, we check check in both next
	// rows, if there
	// are free spaces in the row

	// of course, when traveling over row k or l or both or jl or jr, we have to
	// shift ld and rd by
	// 2 or 3 rows at once
	// after skipping these rows we have to occupy the corresponding diagonals

	// for d = 0
	private void SQd0B(int ld, int rd, int col, int row, int free) {
	    if (row == endmark) {
		tempcounter++;
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;

		int next_ld = ((ld | bit) << 1);
		int next_rd = ((rd | bit) >> 1);
		int next_col = (col | bit);
		nextfree = ~(next_ld | next_rd | next_col);
		if (nextfree > 0)
		    if (row < endmark - 1) {
			if (~((next_ld << 1) | (next_rd >> 1) | (next_col)) > 0)
			    SQd0B(next_ld, next_rd, next_col, row + 1, nextfree);
		    } else {
			SQd0B(next_ld, next_rd, next_col, row + 1, nextfree);
		    }
	    }
	}

	private void SQd0BkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | L3);
		    if (nextfree > 0)
			SQd0B((ld | bit) << 2, ((rd | bit) >> 2) | L3, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd0BkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	// for d = 1
	private void SQd1BklB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 3) | ((rd | bit) >> 3) | (col | bit) | 1 | L4);
		    if (nextfree > 0)
			SQd1B(((ld | bit) << 3) | 1, ((rd | bit) >> 3) | L4, col | bit, row + 3, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd1BklB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd1B(int ld, int rd, int col, int row, int free) {
	    if (row == endmark) {
		tempcounter++;
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;

		int next_ld = ((ld | bit) << 1);
		int next_rd = ((rd | bit) >> 1);
		int next_col = (col | bit);
		nextfree = ~(next_ld | next_rd | next_col);
		if (nextfree > 0)
		    if (row + 1 < endmark) {
			if (~((next_ld << 1) | (next_rd >> 1) | (next_col)) > 0)
			    SQd1B(next_ld, next_rd, next_col, row + 1, nextfree);
		    } else {
			SQd1B(next_ld, next_rd, next_col, row + 1, nextfree);
		    }
	    }
	}

	private void SQd1BkBlB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | L3);
		    if (nextfree > 0)
			SQd1BlB(((ld | bit) << 2), ((rd | bit) >> 2) | L3, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd1BkBlB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd1BlB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark2) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;

		    int next_ld = ((ld | bit) << 2) | 1;
		    int next_rd = ((rd | bit) >> 2);
		    int next_col = (col | bit);
		    nextfree = ~(next_ld | next_rd | next_col);
		    if (nextfree > 0)
			if (row + 2 < endmark) {
			    if (~((next_ld << 1) | (next_rd >> 1) | (next_col)) > 0)
				SQd1B(next_ld, next_rd, next_col, row + 2, nextfree);
			} else {
			    SQd1B(next_ld, next_rd, next_col, row + 2, nextfree);
			}
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd1BlB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd1BlkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 3) | ((rd | bit) >> 3) | (col | bit) | 2 | L3);
		    if (nextfree > 0)
			SQd1B(((ld | bit) << 3) | 2, ((rd | bit) >> 3) | L3, col | bit, row + 3, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd1BlkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd1BlBkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | 1);
		    if (nextfree > 0)
			SQd1BkB(((ld | bit) << 2) | 1, (rd | bit) >> 2, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd1BlBkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd1BkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark2) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | L3);
		    if (nextfree > 0)
			SQd1B(((ld | bit) << 2), ((rd | bit) >> 2) | L3, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd1BkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	// for d = 2
	private void SQd2BlkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 3) | ((rd | bit) >> 3) | (col | bit) | L3 | 2);
		    if (nextfree > 0)
			SQd2B(((ld | bit) << 3) | 2, ((rd | bit) >> 3) | L3, col | bit, row + 3, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd2BlkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd2BklB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 3) | ((rd | bit) >> 3) | (col | bit) | L4 | 1);
		    if (nextfree > 0)
			SQd2B(((ld | bit) << 3) | 1, ((rd | bit) >> 3) | L4, col | bit, row + 3, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd2BklB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd2BlBkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | 1);
		    if (nextfree > 0)
			SQd2BkB(((ld | bit) << 2) | 1, (rd | bit) >> 2, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd2BlBkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd2BkBlB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | (1 << (n3)));
		    if (nextfree > 0)
			SQd2BlB(((ld | bit) << 2), ((rd | bit) >> 2) | (1 << (n3)), col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd2BkBlB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd2BlB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark2) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | 1);
		    if (nextfree > 0)
			SQd2B(((ld | bit) << 2) | 1, (rd | bit) >> 2, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd2BlB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd2BkB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark2) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | L3);
		    if (nextfree > 0)
			SQd2B(((ld | bit) << 2), ((rd | bit) >> 2) | L3, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQd2BkB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQd2B(int ld, int rd, int col, int row, int free) {
	    if (row == endmark) {
		if ((free & (~1)) > 0)
		    tempcounter++;
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;

		int next_ld = ((ld | bit) << 1);
		int next_rd = ((rd | bit) >> 1);
		int next_col = (col | bit);
		nextfree = ~(next_ld | next_rd | next_col);
		if (nextfree > 0)
		    if (row < endmark - 1) {
			if (~((next_ld << 1) | (next_rd >> 1) | (next_col)) > 0)
			    SQd2B(next_ld, next_rd, next_col, row + 1, nextfree);
		    } else {
			SQd2B(next_ld, next_rd, next_col, row + 1, nextfree);
		    }
	    }
	}

	// for d>2 but d <small enough>
	private void SQBkBlBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | (1 << (n3)));
		    if (nextfree > 0)
			SQBlBjrB(((ld | bit) << 2), ((rd | bit) >> 2) | (1 << (n3)), col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBkBlBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBlBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark2) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | 1);
		    if (nextfree > 0)
			SQBjrB(((ld | bit) << 2) | 1, (rd | bit) >> 2, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBlBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == jmark) {
		free &= (~1);
		ld |= 1;
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		    if (nextfree > 0)
			SQB(((ld | bit) << 1), (rd | bit) >> 1, col | bit, row + 1, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQB(int ld, int rd, int col, int row, int free) {
	    if (row == endmark) {
		tempcounter++;
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;

		int next_ld = ((ld | bit) << 1);
		int next_rd = ((rd | bit) >> 1);
		int next_col = (col | bit);
		nextfree = ~(next_ld | next_rd | next_col);
		if (nextfree > 0)
		    if (row < endmark - 1) {
			if (~((next_ld << 1) | (next_rd >> 1) | (next_col)) > 0)
			    SQB(next_ld, next_rd, next_col, row + 1, nextfree);
		    } else {
			SQB(next_ld, next_rd, next_col, row + 1, nextfree);
		    }
	    }
	}

	private void SQBlBkBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | 1);
		    if (nextfree > 0)
			SQBkBjrB(((ld | bit) << 2) | 1, (rd | bit) >> 2, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBlBkBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBkBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark2) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 2) | ((rd | bit) >> 2) | (col | bit) | L3);
		    if (nextfree > 0)
			SQBjrB(((ld | bit) << 2), ((rd | bit) >> 2) | L3, col | bit, row + 2, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBkBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBklBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 3) | ((rd | bit) >> 3) | (col | bit) | L4 | 1);
		    if (nextfree > 0)
			SQBjrB(((ld | bit) << 3) | 1, ((rd | bit) >> 3) | L4, col | bit, row + 3, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBklBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBlkBjrB(int ld, int rd, int col, int row, int free) {
	    int bit;
	    int nextfree;

	    if (row == mark1) {
		while (free > 0) {
		    bit = free & (-free);
		    free -= bit;
		    nextfree = ~(((ld | bit) << 3) | ((rd | bit) >> 3) | (col | bit) | L3 | 2);
		    if (nextfree > 0)
			SQBjrB(((ld | bit) << 3) | 2, ((rd | bit) >> 3) | L3, col | bit, row + 3, nextfree);
		}
		return;
	    }

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBlkBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	// for d <big>
	private void SQBjlBkBlBjrB(int ld, int rd, int col, int row, int free) {
	    if (row == n - 1 - jmark) {
		rd |= L;
		free &= ~L;
		SQBkBlBjrB(ld, rd, col, row, free);
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBjlBkBlBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBjlBlBkBjrB(int ld, int rd, int col, int row, int free) {
	    if (row == n - 1 - jmark) {
		rd |= L;
		free &= ~L;
		SQBlBkBjrB(ld, rd, col, row, free);
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBjlBlBkBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBjlBklBjrB(int ld, int rd, int col, int row, int free) {
	    if (row == n - 1 - jmark) {
		rd |= L;
		free &= ~L;
		SQBklBjrB(ld, rd, col, row, free);
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBjlBklBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}

	private void SQBjlBlkBjrB(int ld, int rd, int col, int row, int free) {
	    if (row == n - 1 - jmark) {
		rd |= L;
		free &= ~L;
		SQBlkBjrB(ld, rd, col, row, free);
		return;
	    }

	    int bit;
	    int nextfree;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		nextfree = ~(((ld | bit) << 1) | ((rd | bit) >> 1) | (col | bit));
		if (nextfree > 0)
		    SQBjlBlkBjrB((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree);
	    }
	}
	
	@Override
	public void run() {
	    int j, k, l, ijkl, ld, rd, col, startIjkl, start, free, LD;
	    final int n = this.n;
	    final int smallmask = (1 << (n - 2)) - 1;

	    for (Constellation constellation : constellations) {
		startIjkl = constellation.getStartIjkl();
		start = startIjkl >> 20;
		ijkl = startIjkl & ((1 << 20) - 1);
		j = getj(ijkl);
		k = getk(ijkl);
		l = getl(ijkl);

		// IMPORTANT NOTE: we shift ld and rd one to the right, because the right
		// column does not matter (always occupied by queen l)
		// add occupation of ld from queens j and l from the bottom row upwards
		LD = (L >>> j) | (L >>> l);
		ld = constellation.getLd() >>> 1;
		ld |= LD >>> (n - start);
		// add occupation of rd from queens j and k from the bottom row upwards
		rd = constellation.getRd() >>> 1;
		if (start > k)
		    rd |= (L >>> (start - k + 1));
		if (j >= 2 * n - 33 - start) // only add the rd from queen j if it does not
		    rd |= (L >>> j) << (n - 2 - start); // occupy the sign bit!

		// also occupy col and then calculate free
		col = (constellation.getCol() >>> 1) | (~smallmask);
		free = ~(ld | rd | col);

		// big case distinction for deciding which soling algorithm to use
		// it is a miracel that we got this to actually work..
		// if queen j is more than 2 columns away from the corner
		if (j < n - 3) {
		    jmark = j + 1;
		    endmark = n - 2;
		    // if the queen j is more than 2 columns away from the corner but the rd from
		    // the
		    // j-queen can be set right at start
		    if (j > 2 * n - 34 - start) {
			// k < l
			if (k < l) {
			    mark1 = k - 1;
			    mark2 = l - 1;
			    // if at least l is yet to come
			    if (start < l) {
				// if also k is yet to come
				if (start < k) {
				    // if there are free rows between k and l
				    if (l != k + 1) {
					SQBkBlBjrB(ld, rd, col, start, free);
				    }
				    // if there are no free rows between k and l
				    else {
					SQBklBjrB(ld, rd, col, start, free);
				    }
				}
				// if k already came before start and only l is left
				else {
				    SQBlBjrB(ld, rd, col, start, free);
				}
			    }
			    // if both k and l already came before start
			    else {
				SQBjrB(ld, rd, col, start, free);
			    }
			}
			// l < k
			else {
			    mark1 = l - 1;
			    mark2 = k - 1;
			    // if at least k is yet to come
			    if (start < k) {
				// if also l is yet to come
				if (start < l) {
				    // if there is at least one free row between l and k
				    if (k != l + 1) {
					SQBlBkBjrB(ld, rd, col, start, free);
				    }
				    // if there is no free row between l and k
				    else {
					SQBlkBjrB(ld, rd, col, start, free);
				    }
				}
				// if l already came and only k is yet to come
				else {
				    SQBkBjrB(ld, rd, col, start, free);
				}
			    }
			    // if both l and k already came before start
			    else {
				SQBjrB(ld, rd, col, start, free);
			    }
			}
		    }
		    // if we have to set some queens first in order to reach the row n-1-jmark where
		    // the
		    // rd from queen j
		    // can be set
		    else {
			// k < l
			if (k < l) {
			    mark1 = k - 1;
			    mark2 = l - 1;
			    // there is at least one free row between rows k and l
			    if (l != k + 1) {
				SQBjlBkBlBjrB(ld, rd, col, start, free);
			    }
			    // if l comes right after k
			    else {
				SQBjlBklBjrB(ld, rd, col, start, free);
			    }
			}
			// l < k
			else {
			    mark1 = l - 1;
			    mark2 = k - 1;
			    // there is at least on efree row between rows l and k
			    if (k != l + 1) {
				SQBjlBlBkBjrB(ld, rd, col, start, free);
			    }
			    // if k comes right after l
			    else {
				SQBjlBlkBjrB(ld, rd, col, start, free);
			    }
			}
		    }
		}
		// if the queen j is exactly 2 columns away from the corner
		else if (j == n - 3) {
		    // this means that the last row will always be row n-2
		    endmark = n - 2;
		    // k < l
		    if (k < l) {
			mark1 = k - 1;
			mark2 = l - 1;
			// if at least l is yet to come
			if (start < l) {
			    // if k is yet to come too
			    if (start < k) {
				// if there are free rows between k and l
				if (l != k + 1) {
				    SQd2BkBlB(ld, rd, col, start, free);
				} else {
				    SQd2BklB(ld, rd, col, start, free);
				}
			    }
			    // if k was set before start
			    else {
				mark2 = l - 1;
				SQd2BlB(ld, rd, col, start, free);
			    }
			}
			// if k and l already came before start
			else {
			    SQd2B(ld, rd, col, start, free);
			}
		    }
		    // l < k
		    else {
			mark1 = l - 1;
			mark2 = k - 1;
			endmark = n - 2;
			// if at least k is yet to come
			if (start < k) {
			    // if also l is yet to come
			    if (start < l) {
				// if there are free rows between l and k
				if (k != l + 1) {
				    SQd2BlBkB(ld, rd, col, start, free);
				}
				// if there are no free rows between l and k
				else {
				    SQd2BlkB(ld, rd, col, start, free);
				}
			    }
			    // if l came before start
			    else {
				mark2 = k - 1;
				SQd2BkB(ld, rd, col, start, free);
			    }
			}
			// if both l and k already came before start
			else {
			    SQd2B(ld, rd, col, start, free);
			}
		    }
		}
		// if the queen j is exactly 1 column away from the corner
		else if (j == n - 2) {
		    // k < l
		    if (k < l) {
			// k can not be first, l can not be last due to queen placement
			// thus always end in line n-2
			endmark = n - 2;
			// if at least l is yet to come
			if (start < l) {
			    // if k is yet to come too
			    if (start < k) {
				mark1 = k - 1;
				// if k and l are next to each other
				if (l != k + 1) {
				    mark2 = l - 1;
				    SQd1BkBlB(ld, rd, col, start, free);
				}
				//
				else {
				    SQd1BklB(ld, rd, col, start, free);
				}
			    }
			    // if only l is yet to come
			    else {
				mark2 = l - 1;
				SQd1BlB(ld, rd, col, start, free);
			    }
			}
			// if k and l already came
			else {
			    SQd1B(ld, rd, col, start, free);
			}
		    }
		    // l < k
		    else {
			// if at least k is yet to come
			if (start < k) {
			    // if also l is yet to come
			    if (start < l) {
				// if k is not at the end
				if (k < n - 2) {
				    mark1 = l - 1;
				    endmark = n - 2;
				    // if there are free rows between l and k
				    if (k != l + 1) {
					mark2 = k - 1;
					SQd1BlBkB(ld, rd, col, start, free);
				    }
				    // if there are no free rows between l and k
				    else {
					SQd1BlkB(ld, rd, col, start, free);
				    }
				}
				// if k is at the end
				else {
				    // if l is not right before k
				    if (l != n - 3) {
					mark2 = l - 1;
					endmark = n - 3;
					SQd1BlB(ld, rd, col, start, free);
				    }
				    // if l is right before k
				    else {
					endmark = n - 4;
					SQd1B(ld, rd, col, start, free);
				    }
				}
			    }
			    // if only k is yet to come
			    else {
				// if k is not at the end
				if (k != n - 2) {
				    mark2 = k - 1;
				    endmark = n - 2;
				    SQd1BkB(ld, rd, col, start, free);
				} else {
				    // if k is at the end
				    endmark = n - 3;
				    SQd1B(ld, rd, col, start, free);
				}
			    }
			}
			// k and l came before start
			else {
			    endmark = n - 2;
			    SQd1B(ld, rd, col, start, free);
			}
		    }
		}
		// if the queen j is placed in the corner
		else {
		    endmark = n - 2;
		    if (start > k) {
			SQd0B(ld, rd, col, start, free);
		    }
		    // k can not be in the last row due to the way we construct start constellations
		    // with a queen in the corner and
		    // due to the way we apply jasmin
		    else {
			mark1 = k - 1;
			SQd0BkB(ld, rd, col, start, free);
		    }
		}

		// for saving and loading progress remove the finished starting constellation
		var constellationSolutions = tempcounter * symmetry(n, ijkl);
		
		constellation.setSolutions(constellationSolutions);
		solutions.addAndGet(constellationSolutions);
		solvedConstellations.incrementAndGet();
		
		tempcounter = 0;
	    }
	}
    }

}
