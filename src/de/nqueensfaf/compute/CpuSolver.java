package de.nqueensfaf.compute;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.nqueensfaf.Solver;

public class CpuSolver extends Solver {

	private int threadcount = 1;
	private long start, end;
	private HashSet<Integer> 
		startConstellations = new HashSet<Integer>();
	private ArrayList<CpuSolverThread> threads = new ArrayList<CpuSolverThread>();
	private int total;
	private long passed = 0;
	private long solutions;
	private boolean restored = false;
	
	@Override
	protected void run() {
		// check if run is called without calling reset after a run call had finished
		if(threads.size() != 0) {
			throw new IllegalStateException("You first have to call reset() when calling solve() multiple times on the same object");
		}
		
		start = System.currentTimeMillis();
		if(!restored) {
//			startConstellations.addAll(CpuSolverUtils.genConstellations(N));
			genConstellations();
			total = startConstellations.size();
		}

		// split starting constellations in [cpu] many lists (splitting the work for the threads)
		ArrayList<ArrayDeque<Integer>> threadConstellations = new ArrayList<ArrayDeque<Integer>>(threadcount);
		for(int i = 0; i < threadcount; i++) {
			threadConstellations.add(new ArrayDeque<Integer>());
		}
		int i = 0;
		for(int constellation : startConstellations) {
			threadConstellations.get((i++) % threadcount).addFirst(constellation);
		}

		// start the threads and wait until they are all finished
		ExecutorService executor = Executors.newFixedThreadPool(threadcount);
		for(i = 0; i < threadcount; i++) {
			CpuSolverThread cpuSolverThread = new CpuSolverThread(N, threadConstellations.get(i));
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
	}
	
	@Override
	public void save(String filename) {
		// write passed, startConstellations, solutions, total into file
	}

	@Override
	public void restore(String filename) {
		reset();
		// read passed, startConstellations, solutions, total from file
		restored = true;
	}

	@Override
	public void reset() {
		start = 0;
		end = 0;
		passed = 0;
		startConstellations.clear();
		solutions = 0;
		threads.clear();
		total = 0;
		restored = false;
		System.gc();
	}

	private void genConstellations() {
		startConstellations.clear();

		// halfN half of N rounded up
		final int halfN = (N + 1) / 2;

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

	// wrap i, j, k and l to one integer using bitwise movement
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}

	@Override
	public long getDuration() {
		return isRunning() ? passed + System.currentTimeMillis() - start : passed + end - start;
	}

	@Override
	public float getProgress() {
		float done = 0;
		for(CpuSolverThread t : threads) {
			done += t.getDone();
		}
		return done / total;
	}

	@Override
	public long getSolutions() {
		long solutions = this.solutions;
		for(CpuSolverThread t : threads) {
			solutions += t.getSolutions();
		}
		return solutions;
	}
	
	public void setThreadcount(int threadcount) {
		if(threadcount < 1 || threadcount > Runtime.getRuntime().availableProcessors()) {
			throw new IllegalArgumentException("threadcount must be a number between 1 and " + Runtime.getRuntime().availableProcessors() + " (=your CPU's number of logical cores) (inclusive)");
		}
		this.threadcount = threadcount;
	}
	
	public int getThreadcount() {
		return threadcount;
	}
	
	// for testing
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		CpuSolver s = new CpuSolver();
		s.setN(16);
		s.setThreadcount(1);
		s.addTerminationCallback(() -> System.out.println("duration: " + s.getDuration()));
		s.addTerminationCallback(() -> System.out.println("solutions: " + s.getSolutions()));
		for(int i = 0; i < 100; i++) {
			s.solve();
			s.threads.clear();
			in.nextLine();
		}
		s.solve();
		in.close();
	}
}
