package de.nqueensfaf;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.nqueensfaf.util.OnProgressUpdateCallback;
import de.nqueensfaf.util.OnTimeUpdateCallback;

/**
 * 
 * @author olepo
 *
 * <p>
 * Abstract class for implementing new Solvers.
 * </p>
 * <p>
 * To implement an own solver for the n queens problem, simply create a class that extends this one. 
 * Then the solver will automatically have the util functions like adding callbacks etc. and the given basic solver class structure makes it easier.
 * </p>
 */
public abstract class Solver {
	
	/**
	 * board size
	 */
	protected int N;
	/**
	 * callback that is executed on every time update
	 */
	protected OnTimeUpdateCallback  onTimeUpdateCallback = (duration) -> {};
	/**
	 * callback that is executed on every progress update
	 */
	protected OnProgressUpdateCallback  onProgressUpdateCallback = (progress, solutions) -> {};
	/**
	 * delay between time updates
	 */
	protected long timeUpdateDelay = NQueensFAF.DEFAULT_TIME_UPDATE_DELAY;
	/**
	 * delay between progress updates
	 */
	protected long progressUpdateDelay = NQueensFAF.DEFAULT_PROGRESS_UPDATE_DELAY;
	/**
	 * executor of the update callbacks (uc)
	 */
	private ThreadPoolExecutor ucExecutor;
	/**
	 * list of callbacks to be executed before the run() function of the solver is called
	 */
	private ArrayList<Runnable> initialization = new ArrayList<Runnable>();
	/**
	 * list of callbacks to be executed after the run() function of the solver is finished
	 */
	private ArrayList<Runnable> termination = new ArrayList<Runnable>();
	
	/**
	 * current state of the solver
	 * @see NQueensFAF
	 */
	private int state = NQueensFAF.IDLE;
	/**
	 * Thread that executes the solvers run() function if {@link #solveAsync()} is called
	 */
	private Thread t = new Thread(() -> solve());
	
	// abstract methods
	protected abstract void run();
	public abstract void save(String filename);
	public abstract void restore(String filename);
	public abstract void reset();
	public abstract long getDuration();
	public abstract float getProgress();
	public abstract long getSolutions();
	
	public final void solve() {
		if(N == 0) {
			throw new IllegalStateException("Board size was not set");
		}
		if(!isIdle()) {
			throw new IllegalStateException("Solver is already started");
		}
		state = NQueensFAF.INITIALIZING;
		initializationCaller();
		ucExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
		startUpdateCallerThreads();
		
		state = NQueensFAF.RUNNING;
		run();
		
		state = NQueensFAF.TERMINATING;
		terminationCaller();
		ucExecutor.shutdown();
		try {
			ucExecutor.awaitTermination(timeUpdateDelay > progressUpdateDelay ? timeUpdateDelay : progressUpdateDelay, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		state = NQueensFAF.IDLE;
	}
	
	public final void solveAsync() {
		if(!isIdle()) {
			throw new IllegalStateException("Solver is already started");
		}
		t.start();
		while(isIdle()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public final void waitFor() throws InterruptedException {
		if(!t.isAlive()) {
			throw new IllegalStateException("Solver is not running");
		}
		t.join();
	}
	
	private void startUpdateCallerThreads() {
		if(onTimeUpdateCallback != null) {
			ucExecutor.submit(() -> {
				while(!isRunning());
				long tmpTime = 0;
				while(isRunning()) {
					if(getDuration() != tmpTime) {
						onTimeUpdateCallback.onTimeUpdate(getDuration());
						tmpTime = getDuration();
					}
					if(!isRunning())
						break;
					try {
						Thread.sleep(timeUpdateDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				onTimeUpdateCallback.onTimeUpdate(getDuration());
			});
		}
		if(onProgressUpdateCallback != null) {
			ucExecutor.submit(() -> {
				while(!isRunning());
				float tmpProgress = 0;
				long tmpSolutions = 0;
				while(isRunning()) {
					if(getProgress() != tmpProgress || getSolutions() != tmpSolutions) {
						onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
						tmpProgress = getProgress();
						tmpSolutions = getSolutions();
					}
					if(!isRunning())
						break;
					try {
						Thread.sleep(progressUpdateDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
			});
		}
	}

	public final Solver addInitializationCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("initializationCallback must not be null");
		}
		initialization.add(r);
		return this;
	}
	
	public final Solver addTerminationCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("terminationCallback must not be null");
		}
		termination.add(r);
		return this;
	}
	
	private void initializationCaller() {
		for(int i = initialization.size()-1; i >= 0; i--) {
			initialization.get(i).run();
		}
	}
	
	private void terminationCaller() {
		for(int i = termination.size()-1; i >= 0; i--) {
			termination.get(i).run();
		}
	}
	
	// Getters and Setters
	public final int getN() {
		return N;
	}
	
	public final Solver setN(int n) {
		if(!isIdle()) {
			throw new IllegalStateException("Cannot set board size while solving");
		}
		if(n < 0 || n > 31) {
			throw new IllegalArgumentException("Board size must be a number between 0 and 32 (not inclusive)");
		}
		N = n;
		return this;
	}
	
	public final OnTimeUpdateCallback getOnTimeUpdateCallback() {
		return onTimeUpdateCallback;
	}
	
	public final Solver setOnTimeUpdateCallback(OnTimeUpdateCallback onTimeUpdateCallback) {
		if(onTimeUpdateCallback == null) {
			this.onTimeUpdateCallback = (duration) -> {};
		} else {
			this.onTimeUpdateCallback = onTimeUpdateCallback;
		}
		return this;
	}
	
	public final OnProgressUpdateCallback getOnProgressUpdateCallback() {
		return onProgressUpdateCallback;
	}
	
	public final Solver setOnProgressUpdateCallback(OnProgressUpdateCallback onProgressUpdateCallback) {
		if(onProgressUpdateCallback == null) {
			this.onProgressUpdateCallback = (progress, solutions) -> {};
		} else {
			this.onProgressUpdateCallback = onProgressUpdateCallback;
		}
		return this;
	}
	
	public final long getTimeUpdateDelay() {
		return timeUpdateDelay;
	}
	
	public final Solver setTimeUpdateDelay(long timeUpdateDelay) {
		if(timeUpdateDelay < 0) {
			throw new IllegalArgumentException("timeUpdateDelay must be a number >= 0");
		}
		this.timeUpdateDelay = timeUpdateDelay;
		return this;
	}
	
	public final long getProgressUpdateDelay() {
		return progressUpdateDelay;
	}
	
	public final Solver setProgressUpdateDelay(long progressUpdateDelay) {
		if(progressUpdateDelay < 0) {
			throw new IllegalArgumentException("progressUpdateDelay must be a number >= 0");
		}
		this.progressUpdateDelay = progressUpdateDelay;
		return this;
	}

	public final boolean isIdle() {
		return state == NQueensFAF.IDLE;
	}

	public final boolean isInitializing() {
		return state == NQueensFAF.INITIALIZING;
	}
	
	public final boolean isRunning() {
		return state == NQueensFAF.RUNNING;
	}

	public final boolean isTerminating() {
		return state == NQueensFAF.TERMINATING;
	}
}
