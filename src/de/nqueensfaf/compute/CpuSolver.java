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
	private int startConstCount, solvedConstellations;
	private long timePassed = 0;
	private long solutions;
	private boolean restored = false;
	
	// inherited functions
	@Override
	protected void run() {
		// check if run is called without calling reset after a run call had finished
		if(start != 0) {
			throw new IllegalStateException("You first have to call reset() when calling solve() multiple times on the same object");
		}
		
		start = System.currentTimeMillis();
		if(!restored) {
			genConstellations();
			startConstCount = startConstellations.size();
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
	public void save(String filepath) throws IOException {
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
		long timePassed;
		if(isRunning()) {
			timePassed = this.timePassed + System.currentTimeMillis() - start;
		} else {
			timePassed = this.timePassed + end - start;
		}
		RestorationInformation resInfo = new RestorationInformation(N, startConstellations, timePassed, solutions, startConstCount);
		System.out.println("saving: solutions: " + resInfo.solutions + "; startConstCount: " + resInfo.startConstCount);
		
		FileOutputStream fos = new FileOutputStream(filepath);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(resInfo);
		oos.flush();
		oos.close();
		fos.close();
	}

	@Override
	public void restore(String filepath) throws IOException, ClassNotFoundException {
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
	public void reset() {
		start = 0;
		end = 0;
		timePassed = 0;
		startConstellations.clear();
		solutions = 0;
		threads.clear();
		startConstCount = 0;
		restored = false;
		System.gc();
	}

	@Override
	public long getDuration() {
		return isRunning() ? timePassed + System.currentTimeMillis() - start : timePassed + end - start;
	}

	@Override
	public float getProgress() {
		float done = solvedConstellations;
		for(CpuSolverThread t : threads) {
			done += t.getDone();
		}
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

	public void cancel() {
		for(CpuSolverThread t : threads) {
			t.cancel();
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
	
	
	
	
	// for testing
	public static void main(String[] args) {
//		write();
		read();
//		goOn();
	}
	
	static void write() {
		Scanner in = new Scanner(System.in);
		CpuSolver s = new CpuSolver();
		s.setN(17);
		s.setThreadcount(4);
//		s.addTerminationCallback(() -> System.out.println("duration: " + s.getDuration()));
		s.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("solutions: " + solutions));
		for(int i = 0; i < 4; i++) {
			s.solveAsync();
			String str = in.nextLine();
			if(str.equals("hi")) {
				try {
					s.save("hi.faf");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			s.threads.clear();
		}
		in.close();
	}
	
	static void read() {
		CpuSolver s = new CpuSolver();
		try {
			s.restore("hi.faf");
			System.out.println("solutions: " + s.getSolutions());
			System.out.println("duration: " + s.getDuration());
			System.out.println("progress: " + s.getProgress());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void goOn() {
		Scanner in = new Scanner(System.in);
		CpuSolver s = new CpuSolver();
		s.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("solutions: " + solutions));
		try {
			s.restore("hi.faf");
			s.solveAsync();
			
			in.nextLine();
			s.save("hi.faf");
			s.cancel();
			in.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
