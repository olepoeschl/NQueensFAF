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
			startConstellations.addAll(CpuSolverUtils.genConstellations(N));
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
	
	// for testing
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		CpuSolver s = new CpuSolver();
		s.setN(16);
		s.setThreadcount(1);
		s.addTerminationCallback(() -> System.out.println("duration: " + s.getDuration()));
		s.addTerminationCallback(() -> System.out.println("solutions: " + s.getSolutions()));
		for(int i = 0; i < 5; i++) {
			s.solve();
			s.threads.clear();
			in.nextLine();
		}
		s.solve();
		in.close();
	}
}
