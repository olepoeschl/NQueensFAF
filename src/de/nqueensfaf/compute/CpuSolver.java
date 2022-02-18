package de.nqueensfaf.compute;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.nqueensfaf.Solver;

public class CpuSolver extends Solver {

	private static final int smallestN = 6;
	private int threadcount = 1;
	private int kbit, lbit, preQueens = 5, L, mask, LD, RD, counter;
	private long start, end;
	private HashSet<Integer> startConstellations = new HashSet<Integer>();
	private ArrayList<Integer> ldList = new ArrayList<Integer>(), rdList = new ArrayList<Integer>(), colList = new ArrayList<Integer>(); 
	private ArrayList<Integer> startQueensIjklList = new ArrayList<Integer>();
	private ArrayList<CpuSolverThread> threads = new ArrayList<CpuSolverThread>();
	private int startConstCount, solvedConstellations;
	private long timePassed = 0, pauseStart = 0;
	private long solutions;
	private boolean restored = false;
	private ArrayList<Runnable> pausing = new ArrayList<Runnable>();

	// inherited functions
	@Override
	protected void run() {
		// check if run is called without calling reset after a run call had finished
		if(start != 0) {
			throw new IllegalStateException("You first have to call reset() when calling solve() multiple times on the same object");
		}

		start = System.currentTimeMillis();
		if(N <= smallestN) {	// if N is very small, use the simple Solver from the parent class
			solutions = solveSmallBoard();
			end = System.currentTimeMillis();
			// simulate progress = 100
			startConstCount = 1;
			solvedConstellations = 1;
			return;
		}
		if(!restored) {
			genConstellations();
			startConstCount = startConstellations.size();
		}
		
		// split starting constellations in [cpu] many lists (splitting the work for the threads)
		ArrayList<ArrayList<ArrayDeque<Integer>>> threadConstellations = new ArrayList<ArrayList<ArrayDeque<Integer>>>();
		threadConstellations.add(new ArrayList<ArrayDeque<Integer>>(threadcount));	// startConstellations, just keeping for backwards compatibility
		threadConstellations.add(new ArrayList<ArrayDeque<Integer>>(threadcount));	// ld
		threadConstellations.add(new ArrayList<ArrayDeque<Integer>>(threadcount));	// rd
		threadConstellations.add(new ArrayList<ArrayDeque<Integer>>(threadcount));	// col
		threadConstellations.add(new ArrayList<ArrayDeque<Integer>>(threadcount));	// startQueensIjkl
		for(var list : threadConstellations) {
			for(int i = 0; i < threadcount; i++) {
				list.add(new ArrayDeque<Integer>());
			}
		}
		// startConstellations
		int i = 0;
		for(int constellation : startConstellations) {
			threadConstellations.get(0).get((i++) % threadcount).addFirst(constellation);
		}
		// ld
		i = 0;
		for(int ld : ldList) {
			threadConstellations.get(1).get((i++) % threadcount).addFirst(ld);
		}
		// rd
		i = 0;
		for(int rd : rdList) {
			threadConstellations.get(2).get((i++) % threadcount).addFirst(rd);
		}
		// col
		i = 0;
		for(int col : colList) {
			threadConstellations.get(3).get((i++) % threadcount).addFirst(col);
		}
		// startQueensIjkl
		i = 0;
		for(int startQueensIjkl : startQueensIjklList) {
			threadConstellations.get(4).get((i++) % threadcount).addFirst(startQueensIjkl);
		}

		// start the threads and wait until they are all finished
		ExecutorService executor = Executors.newFixedThreadPool(threadcount);
		for(i = 0; i < threadcount; i++) {
			CpuSolverThread cpuSolverThread = new CpuSolverThread(this, N, threadConstellations.get(0).get(i), threadConstellations.get(1).get(i), 
					threadConstellations.get(2).get(i), threadConstellations.get(3).get(i), threadConstellations.get(4).get(i));
			threads.add(cpuSolverThread);
			executor.submit(cpuSolverThread);
		}

		// wait for the threads to finish
		executor.shutdown();
		try {
			if(executor.awaitTermination(365, TimeUnit.DAYS)) {
				end = System.currentTimeMillis();
				// done 
			} else {
				end = System.currentTimeMillis();
				// not done
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		restored = false;
	}

	@Override
	public void store_(String filepath) throws IOException {
		// if Solver was not even started yet, throw exception
		if(start == 0) {
			throw new IllegalStateException("Nothing to be saved");
		}
		// get current progress values
		HashSet<Integer> startConstellations = new HashSet<Integer>();
		for(CpuSolverThread t : threads) {
			startConstellations.addAll(t.getRemainingConstellations());
		}
		long solutions = this.solutions;
		for(CpuSolverThread t : threads) {
			solutions += t.getSolutions();
		}
		long timePassed = getDuration();
		RestorationInformation resInfo = new RestorationInformation(N, startConstellations, timePassed, solutions, startConstCount);

		FileOutputStream fos = new FileOutputStream(filepath);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(resInfo);
		oos.flush();
		oos.close();
		fos.close();
	}

	@Override
	public void restore_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
		if(!isIdle()) {
			throw new IllegalStateException("Cannot restore while the Solver is running");
		}
		RestorationInformation resInfo;
		FileInputStream fis = new FileInputStream(filepath);
		ObjectInputStream ois = new ObjectInputStream(fis);
		resInfo = (RestorationInformation) ois.readObject();
		ois.close();
		fis.close();

		reset();
		N = resInfo.N;
		startConstellations = resInfo.startConstellations;
		timePassed = resInfo.timePassed;
		solutions = resInfo.solutions;
		startConstCount = resInfo.startConstCount;
		solvedConstellations = startConstCount - startConstellations.size();
		restored = true;
	}

	@Override
	public boolean isRestored() {
		return restored;
	}

	@Override
	public void reset() {
		start = 0;
		end = 0;
		timePassed = 0;
		pauseStart = 0;
		startConstellations.clear();
		ldList.clear();
		rdList.clear();
		colList.clear();
		startQueensIjklList.clear();
		solvedConstellations = 0;
		solutions = 0;
		threads.clear();
		startConstCount = 0;
		restored = false;
	}

	@Override
	public long getDuration() {
		if(isRunning()) {
			if(isPaused()) {
				return timePassed;
			} else {
				return timePassed + System.currentTimeMillis() - start;
			}
		} else {
			if(isPaused()) {
				return timePassed;
			} else {
				return timePassed + end - start;
			}
		}
	}

	@Override
	public float getProgress() {
		if(restored && isIdle())
			return (float) solvedConstellations / startConstCount;
		float done = solvedConstellations;
		for(CpuSolverThread t : threads) {
			done += t.getDone();
		}
		System.out.println(done / startConstCount);
		return done / startConstCount;
	}

	@Override
	public long getSolutions() {
		long solutions = this.solutions;
		for(CpuSolverThread t : threads) {
			solutions += t.getSolutions();
		}
		return solutions;
	}

	// own functions
	private void genConstellations() {
		startConstellations.clear();

		// halfN half of N rounded up
		final int halfN = (N + 1) / 2;
		L = 1 << (N -1);
		mask = (1 << N) - 1;

		// calculating start constellations with the first Queen on square (0,0)
		for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
			for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
				startConstellations.add(toijkl(0, j, 0, l));
			}
		}

		// calculate starting constellations for no Queens in corners
		for(int k = 1; k < halfN; k++) {						// go through first col
			for(int l = k+1; l < N-1; l++) {					// go through last col
				for(int i = k+1; i < N-1; i++) {				// go through first row
					if(i == N-1-l)								// skip if occupied
						continue;
					for(int j = N-k-2; j > 0; j--) {			// go through last row
						if(j==i || l == j)
							continue;

						if(!checkRotations(i, j, k, l)) {		// if no rotation-symmetric starting constellation already found
							startConstellations.add(toijkl(i, j, k, l));
						}
					}
				}
			}
		}
		HashSet<Integer> startConstellationsJasmin = new HashSet<Integer>();
		// rotate and mirror all start constellations, such that the queen in the last row is as close to the right border as possible 
		for(int startConstellation : startConstellations) {
			startConstellationsJasmin.add(jasmin(startConstellation));
		}
		startConstellations = startConstellationsJasmin;

		int i, j, k, l, ld, rd, col, currentSize = 0;
		for(int sc : startConstellations) {
			i = geti(sc); j = getj(sc); k = getk(sc); l = getl(sc);
			// fill up the board with preQueens queens and generate corresponding variables ld, rd, col, start_queens_ijkl for each constellation 
			// occupy the board corresponding to the queens on the borders of the board 
			ld = (L >>> (i-1)) | (1 << (N-k));
			rd = (L >>> (i+1)) | (1 << (l-1));
			col = 1 | L | (L >>> j) | (L >>> i);
			// occupy diagonals of the queens j k l in the last row 
			// later we are going to shift them upwards the board 
			LD = (L >>> j) | (L >>> l);
			RD = (L >>> j) | (1 << k);
			// this is the queen in row k and l 
			// their diagonals have to be occupied later 
			// we can not do this right now, because in row k, the queen k has to be actually set 
			kbit = (1 << (N-k-1));
			lbit = (1 << l);

			// counts all subconstellations 
			counter = 0;
			// generate all subconstellations 
			setPreQueens(ld, rd, col, k, l, 1, 4);
			currentSize = startQueensIjklList.size();
			// jkl and sym and start are the same for all subconstellations 
			for(int a = 0; a < counter; a++) {
				startQueensIjklList.set(currentSize-a-1, startQueensIjklList.get(currentSize-a-1) | (preQueens << 20) | toijkl(i, j, k, l));
			}
		}
	}


	// generate subconstellations for each starting constellation with 3 or 4 queens 
	private void setPreQueens(int ld, int rd, int col, int k, int l, int row, int queens) {
		// in row k and l just go further 
		if(row == k || row == l) {
			setPreQueens(ld<<1, rd>>>1, col, k, l, row+1, queens);
			return;
		}
		// add queens until we have preQueens queens 
		// this should be variable for the distributed version and different N 
		if(queens == preQueens) {
			// occupy diagonals from queen k and l, that will end in the left or right border 
			// the following 2 lines are probably TRASH
			ld &= ~(kbit << row);
			rd &= ~(lbit >>> row);
			// make left and right col free 
			col &= ~(1 | L);
			// if k already came, then occupy it on the board 
			if(k < row) {
				rd |= (L >> (row-k));
				col |= L;
			}
			// same for l 
			if(l < row) {
				ld |= (1 << (row-l));
				col |= 1;
			}
			// add the subconstellations to the list 
			ldList.add(ld);
			rdList.add(rd);
			colList.add(col);
			startQueensIjklList.add(row << 25);
			counter++;
			return;
		}
		// if not done or row k or l, just place queens and occupy the board and go further 
		else {
			int free = ~(ld | rd | col | (LD >>> (N-1-row)) | (RD << (N-1-row))) & mask;
			int bit;

			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				setPreQueens((ld|bit) << 1, (rd|bit) >>> 1, col|bit, k, l, row+1, queens+1);
			}
		}
	}


	// true, if starting constellation rotated by any angle has already been found
	private boolean checkRotations(int i, int j, int k, int l) {
		// rot90
		if(startConstellations.contains(((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i)) 
			return true;

		// rot180
		if(startConstellations.contains(((N-1-j)<<24) + ((N-1-i)<<16) + ((N-1-l)<<8) + N-1-k)) 
			return true;

		// rot270
		if(startConstellations.contains((l<<24) + (k<<16) + ((N-1-i)<<8) + N-1-j)) 
			return true;

		return false;
	}

	// i, j, k, l to ijkl and functions to get specific entry
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}
	private int geti(int ijkl) {
		return ijkl >> 24;
	}
	private int getj(int ijkl) {
		return (ijkl >> 16) & 255;
	}
	private int getk(int ijkl) {
		return (ijkl >> 8) & 255;
	}
	private int getl(int ijkl) {
		return ijkl & 255;
	}
	// rotate and mirror board, so that the queen closest to a corner is on the right side of the last row
	private int jasmin(int ijkl) {
		int min = Math.min(getj(ijkl), N-1 - getj(ijkl)), arg = 0;

		if(Math.min(geti(ijkl), N-1 - geti(ijkl)) < min) {
			arg = 2;
			min = Math.min(geti(ijkl), N-1 - geti(ijkl));
		}
		if(Math.min(getk(ijkl), N-1 - getk(ijkl)) < min) {
			arg = 3;
			min = Math.min(getk(ijkl), N-1 - getk(ijkl));
		}
		if(Math.min(getl(ijkl), N-1 - getl(ijkl)) < min) {
			arg = 1;
			min = Math.min(getl(ijkl), N-1 - getl(ijkl));
		}

		for(int i = 0; i < arg; i++) {
			ijkl = rot90(ijkl);
		}

		if(getj(ijkl) < N-1 - getj(ijkl))
			ijkl = mirvert(ijkl);

		return ijkl;
	}
	// mirror left-right
	private int mirvert(int ijkl) {
		return toijkl(N-1-geti(ijkl), N-1-getj(ijkl), getl(ijkl), getk(ijkl));
	}
	// rotate 90 degrees clockwise
	private int rot90(int ijkl) {
		return ((N-1-getk(ijkl))<<24) + ((N-1-getl(ijkl))<<16) + (getj(ijkl)<<8) + geti(ijkl);
	}

	// for user interaction
	public void pause() {
		if(!isRunning()) {
			throw new IllegalStateException("unable to pause a CpuSolver when it is not running");
		}
		if(isPaused()) {
			throw new IllegalStateException("unable to pause a CpuSolver when it is already paused");
		}
		for(CpuSolverThread t : threads) {
			t.pauseThread();
		}
	}

	public void cancel() {
		if(!isRunning()) {
			throw new IllegalStateException("unable to cancel a CpuSolver when it is not running");
		}
		for(CpuSolverThread t : threads) {
			t.cancelThread();
		}
	}

	public void resume() {
		if(!isRunning()) {
			throw new IllegalStateException("unable to resume a CpuSolver when it is not running");
		}
		boolean paused = isPaused();
		for(CpuSolverThread t : threads) {
			t.resumeThread();
		}
		if(paused) {
			start = System.currentTimeMillis();
			pauseStart = 0;
		}
	}

	public boolean isPaused() {
		if(threads.size() <= 0) {
			return false;
		}
		for(CpuSolverThread t : threads) {
			if(!t.isPaused())
				return false;
		}
		return true;
	}

	public boolean wasCanceled() {
		if(isRunning() || N <= smallestN) {
			return false;
		}
		for(CpuSolverThread t : threads) {
			if(!t.wasCanceled())
				return false;
		}
		return true;
	}

	public void addOnPauseCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("pausing callback must not be null");
		}
		pausing.add(r);
	}

	// is called from the SolverThreads to measure correct time of the start of the pause 
	synchronized void onPauseStart() {
		if(isPaused() && pauseStart == 0) {
			long now = System.currentTimeMillis();
			timePassed += now - start;
			pauseStart = now;
			for(Runnable r : pausing) {
				r.run();
			}
		}
	}

	// getters and setters
	public void setThreadcount(int threadcount) {
		if(threadcount < 1 || threadcount > Runtime.getRuntime().availableProcessors()) {
			throw new IllegalArgumentException("threadcount must be a number between 1 and " + Runtime.getRuntime().availableProcessors() + " (=your CPU's number of logical cores) (inclusive)");
		}
		this.threadcount = threadcount;
	}

	public int getThreadcount() {
		return threadcount;
	}

	// for saving and restoring
	private record RestorationInformation(int N, HashSet<Integer> startConstellations, long timePassed, long solutions, int startConstCount) implements Serializable {
		RestorationInformation(int N, HashSet<Integer> startConstellations, long timePassed, long solutions, int startConstCount) {
			this.N = N;
			this.startConstellations = startConstellations;
			this.timePassed = timePassed;
			this.solutions = solutions;
			this.startConstCount = startConstCount;
		}
	}
}
