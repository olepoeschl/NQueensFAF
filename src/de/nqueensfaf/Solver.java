package de.nqueensfaf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.nqueensfaf.util.OnProgressUpdateCallback;
import de.nqueensfaf.util.OnTimeUpdateCallback;

/**
 * <p>
 * Abstract class for implementing Solvers.
 * </p>
 * <p>
 * To implement an own solver for the n queens problem, simply create a class that extends this one. 
 * Then the solver will automatically have the util functions like adding callbacks etc. and the given basic solver class structure makes it easier.
 * </p>
 * 
 * @author olepo
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
	 * @see #startUpdateCallerThreads()
	 */
	private ThreadPoolExecutor ucExecutor;
	/**
	 * list of callbacks to be executed before the run() function of the solver is called
	 * @see #solve()
	 */
	private ArrayList<Runnable> initialization = new ArrayList<Runnable>();
	/**
	 * list of callbacks to be executed after the run() function of the solver is finished
	 * @see #solve()
	 */
	private ArrayList<Runnable> termination = new ArrayList<Runnable>();
	
	/**
	 * current state of the {@link Solver}
	 * @see NQueensFAF
	 */
	private int state = NQueensFAF.IDLE;
	/**
	 * Thread that executes the solvers run() function if {@link #solveAsync()} is called
	 */
	private Thread t;
	
	// abstract methods
	/**
	 * Solves the N-Queens Problem.
	 */
	protected abstract void run();
	/**
	 * Saves the current progress of the {@link Solver} in a file so that it can be continued at some time later.
	 * @param filename name of the file the progress should be written in (existent or non existent)
	 * @throws IOException
	 */
	public abstract void store(String filepath) throws IOException;
	/**
	 * Reads the progress of an old run of the {@link Solver} and restores this state so that it can be continued.
	 * @param filename name of the file the progress was written in
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public abstract void restore(String filepath) throws IOException, ClassNotFoundException;
	/**
	 * Resets the {@link Solver}; therefore, applies default values to all Solver state and progress related variables.
	 */
	public abstract void reset();
	/**
	 * Gets the total runtime of the {@link Solver} in the current run.
	 * @return total runtime of the current run or 0 if the Solver is currently not running
	 */
	public abstract long getDuration();
	/**
	 * Gets the percentage of the {@link Solver}'s task that is completed.
	 * @return percentage of the {@link Solver}'s task that is completed
	 */
	public abstract float getProgress();
	/**
	 * Gets the current count of found solutions.
	 * @return current count of found solutions
	 */
	public abstract long getSolutions();
	
	/**
	 * Calls all initialization callbacks, then starts the {@link Solver}'s run()-method, then calls all termination callbacks.
	 * @throws {@link IllegalStateException} if N equals 0 or the {@link Solver} is already running
	 * @see #run()
	 */
	public final void solve() {
		if(N == 0) {
			state = NQueensFAF.TERMINATING;	// for solveAsync() to break the loop
			throw new IllegalStateException("Board size was not set");
		}
		if(!isIdle()) {
			state = NQueensFAF.TERMINATING;	// for solveAsync() to break the loop
			throw new IllegalStateException("Solver is already started");
		}
		// check if Solver is already done
		if(getProgress() == 1.0f) {
			state = NQueensFAF.TERMINATING;	// for solveAsync() to break the loop
			throw new IllegalStateException("Solver is already done, nothing to do here");
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
	/**
	 * Asynchronously starts the {@link Solver} using a new thread and then waits till the Solver is successfully started.
	 * @throws {@link IllegalStateException} if the {@link Solver} is already running
	 * @see #solve()
	 */
	public final void solveAsync() {
		if(!isIdle()) {
			throw new IllegalStateException("Solver is already started");
		}
		t = new Thread(() -> solve());
		t.start();
		while(isIdle()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		t = null;
	}
	/**
	 * Waits for the asynchronously running {@link Solver} to finish.
	 * @throws {@link InterruptedException} if the {@link Solver} is not running
	 * @see #solveAsync()
	 */
	public final void waitFor() throws InterruptedException {
		if(t == null) {
			return;
		}
		t.join();
	}
	
	/**
	 * Starts the threads that continously update the {@link Solver}'s duration and progress using the related delay.
	 * The threads run until the {@link Solver} is finished.
	 * @see #timeUpdateDelay
	 * @see #progressUpdateDelay
	 */
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

	/**
	 * Adds a callback that will be executed on start of the {@link Solver}.
	 * The callbacks will be called in reversed insertion order.
	 * Chainable.
	 * @param r the callback as {@link Runnable}
	 * @return the {@link Solver}
	 * @throws {@link IllegalArgumentException} if r is null
	 * @see #solve()
	 */
	public final Solver addInitializationCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("initializationCallback must not be null");
		}
		initialization.add(r);
		return this;
	}

	/**
	 * Adds a callback that will be executed on finish of the {@link Solver}.
	 * The callbacks will be called in reversed insertion order.
	 * Chainable.
	 * @param r the callback as {@link Runnable}
	 * @return the {@link Solver}
	 * @throws {@link IllegalArgumentException} if r is null
	 * @see #solve()
	 */
	public final Solver addTerminationCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("terminationCallback must not be null");
		}
		termination.add(r);
		return this;
	}
	
	/**
	 * Calls all initialization callbacks in reversed insertion order.
	 */
	private void initializationCaller() {
		for(int i = initialization.size()-1; i >= 0; i--) {
			initialization.get(i).run();
		}
	}
	
	/**
	 * Calls all termination callbacks in reversed insertion order.
	 */
	private void terminationCaller() {
		for(int i = termination.size()-1; i >= 0; i--) {
			termination.get(i).run();
		}
	}
	
	// Getters and Setters
	/**
	 * Gets {@link #N}.
	 * @return {@link #N}
	 */
	public final int getN() {
		return N;
	}
	/**
	 * Sets {@link #N}.
	 * Chainable.
	 * @param n boardsize 
	 * @return the {@link Solver}
	 * @throws {@link IllegalStateException} if the {@link Solver} is already running
	 * @throws {@link IllegalArgumentException} if the boardsize is invalid 
	 */
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
	
	/**
	 * Gets {@link #onTimeUpdateCallback}.
	 * @return {@link #onTimeUpdateCallback}
	 */
	public final OnTimeUpdateCallback getOnTimeUpdateCallback() {
		return onTimeUpdateCallback;
	}
	/**
	 * Sets {@link #onTimeUpdateCallback} or sets a void callback if the parameter is null.
	 * Chainable.
	 * @param onTimeUpdateCallback callback to be called on each time update
	 * @return the {@link Solver}
	 */
	public final Solver setOnTimeUpdateCallback(OnTimeUpdateCallback onTimeUpdateCallback) {
		if(onTimeUpdateCallback == null) {
			this.onTimeUpdateCallback = (duration) -> {};
		} else {
			this.onTimeUpdateCallback = onTimeUpdateCallback;
		}
		return this;
	}

	/**
	 * Gets {@link #onProgressUpdateCallback}.
	 * @return {@link #onProgressUpdateCallback}
	 */
	public final OnProgressUpdateCallback getOnProgressUpdateCallback() {
		return onProgressUpdateCallback;
	}
	/**
	 * Sets {@link #onProgressUpdateCallback} or sets a void callback if the parameter is null.
	 * Chainable.
	 * @param onProgressUpdateCallback callback to be called on each progress update
	 * @return the {@link Solver}
	 */
	public final Solver setOnProgressUpdateCallback(OnProgressUpdateCallback onProgressUpdateCallback) {
		if(onProgressUpdateCallback == null) {
			this.onProgressUpdateCallback = (progress, solutions) -> {};
		} else {
			this.onProgressUpdateCallback = onProgressUpdateCallback;
		}
		return this;
	}
	
	/**
	 * Gets {@link #timeUpdateDelay}.
	 * @return {@link #timeUpdateDelay}
	 */
	public final long getTimeUpdateDelay() {
		return timeUpdateDelay;
	}
	/**
	 * Sets {@link #timeUpdateDelay}.
	 * Chainable.
	 * @param timeUpdateDelay
	 * @return the {@link Solver}
	 * @throws {@link IllegalArgumentException} if the given delay is <= 0
	 */
	public final Solver setTimeUpdateDelay(long timeUpdateDelay) {
		if(timeUpdateDelay < 0) {
			throw new IllegalArgumentException("timeUpdateDelay must be a number >= 0");
		}
		this.timeUpdateDelay = timeUpdateDelay;
		return this;
	}

	/**
	 * Gets {@link #progressUpdateDelay}.
	 * @return {@link #progressUpdateDelay}
	 */
	public final long getProgressUpdateDelay() {
		return progressUpdateDelay;
	}
	/**
	 * Sets {@link #progressUpdateDelay}.
	 * Chainable.
	 * @param progressUpdateDelay
	 * @return the {@link Solver}
	 * @throws {@link IllegalArgumentException} if the given delay is <= 0
	 */
	public final Solver setProgressUpdateDelay(long progressUpdateDelay) {
		if(progressUpdateDelay < 0) {
			throw new IllegalArgumentException("progressUpdateDelay must be a number >= 0");
		}
		this.progressUpdateDelay = progressUpdateDelay;
		return this;
	}

	/**
	 * Returns true if the {@link Solver}'s state is {@link NQueensFAF#IDLE}
	 * @return true if the current state of the {@link Solver} is {@link NQueensFAF#IDLE}
	 */
	public final boolean isIdle() {
		return state == NQueensFAF.IDLE;
	}
	/**
	 * Returns true if the {@link Solver}'s state is {@link NQueensFAF#INITIALIZING}
	 * @return true if the current state of the {@link Solver} is {@link NQueensFAF#INITIALIZING}
	 */
	public final boolean isInitializing() {
		return state == NQueensFAF.INITIALIZING;
	}
	/**
	 * Returns true if the {@link Solver}'s state is {@link NQueensFAF#RUNNING}
	 * @return true if the current state of the {@link Solver} is {@link NQueensFAF#RUNNING}
	 */
	public final boolean isRunning() {
		return state == NQueensFAF.RUNNING;
	}
	/**
	 * Returns true if the {@link Solver}'s state is {@link NQueensFAF#TERMINATING}
	 * @return true if the current state of the {@link Solver} is {@link NQueensFAF#TERMINATING}
	 */
	public final boolean isTerminating() {
		return state == NQueensFAF.TERMINATING;
	}
}
