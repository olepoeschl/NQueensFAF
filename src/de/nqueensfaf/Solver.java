package de.nqueensfaf;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.nqueensfaf.compute.CPUSolver;
import de.nqueensfaf.compute.GPUSolver;
import de.nqueensfaf.files.Config;
import de.nqueensfaf.util.BasicCallback;
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
	protected OnProgressUpdateCallback  onProgressUpdateCallback = (progress, solutions, duration) -> {};
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
	 * callback that is executed before the run() function of the solver is called
	 * @see #solve()
	 */
	private BasicCallback initializationCallback;
	/**
	 * callback that is executed after the run() function of the solver is finished
	 * @see #solve()
	 */
	private BasicCallback terminationCallback;
	/**
	 * current state of the {@link Solver}
	 * @see NQueensFAF
	 */
	private int state = NQueensFAF.IDLE;
	/**
	 * thread for the {@link #autoSave} function
	 * @see 
	 */
	private Thread autoSaverThread;
	/**
	 * if true, the Solver automatically deletes a file created by the auto save function using
	 * only used when {@link #autoSave} is true
	 */
	private boolean autoDeleteEnabled = false;
	/**
	 * if true, the Solver automatically calls store() using {@link #autoSaveFilename} if a specific progress percentage step is completed
	 * @see #autoSavePercentageStep
	 */
	private boolean autoSaveEnabled = false;
	/**
	 * after the progress increases by this or more percentage, store() is automatically called.
	 * only used when {@link #autoSave} is true
	 */
	private int autoSavePercentageStep = 10;
	/**
	 * the filename (format) used for the autosave feature.
	 * only used when {@link #autoSave} is true
	 * @see #autoSavePercentageStep
	 */
	private String autoSaveFilename = "N{N}.faf";
	/**
	 * set to true when store() is called and set to false again when store() returned.
	 */
	private boolean isStoring = false;
	/**
	 * if true, Solver stores() one last time and after that not any more.
	 */
	private boolean finishStoring = false;
	/**
	 * number of solutions - only used by the method that solves the problem for small N's.
	 * @see #solveSmallBoard()
	 * @see #nq(int, int, int, int, int, int)
	 */
	int solutionsSmallN = 0;

	// abstract methods
	/**
	 * Solves the N-Queens Problem.
	 */
	protected abstract void run();
	/**
	 * Saves the current progress of the {@link Solver} in a file so that it can be continued at some time later.
	 * @param filepath path/name of the file the progress should be written in (existent or non existent)
	 * @throws IOException
	 */
	protected abstract void store_(String filepath) throws IOException;
	/**
	 * Reads the progress of an old run of the {@link Solver} and restores this state so that it can be continued.
	 * @param filepath path/name of the file the progress was written in
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	protected abstract void restore_(String filepath) throws IOException, ClassNotFoundException, ClassCastException;
	/**
	 * States if the Solver has been restored and therefore contains restored values.
	 * @return true if restore() was called and the Solver was not started or resetted since, otherwise false
	 */
	public abstract boolean isRestored();
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
	
	// type-specific
	@SuppressWarnings("unchecked")
	public static <T extends Solver> T withConfig(File configFile) throws StreamReadException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Config config = mapper.readValue(configFile, Config.class);
		Solver solver;
		CPUSolver cpuSolver = null;
		GPUSolver gpuSolver = null;
		switch(config.getType().toLowerCase()) {
		case "cpu":
			cpuSolver = new CPUSolver();
			cpuSolver.setThreadcount(config.getCpuThreadcount());
			solver = cpuSolver;
			break;
		case "gpu":
			gpuSolver = new GPUSolver();
			gpuSolver.setDevice(config.getGpuDevice());
			gpuSolver.setWorkgroupSize(config.getGpuWorkgroupSize());
			gpuSolver.setNumberOfPresetQueens(config.getGpuPresetQueens());
			solver = gpuSolver;
			break;
		default:
			throw new IllegalArgumentException("Invalid config value '" + config.getType() + "' for solver type: has to be 'cpu' or 'gpu'");
		}
		
		// general settings
		solver.setProgressUpdateDelay(config.getProgressUpdateDelay());
		solver.setAutoSaveEnabled(config.isAutoSaveEnabled());
		solver.setAutoDeleteEnabled(config.isAutoDeleteEnabled());
		solver.setAutoSavePercentageStep(config.getAutoSavePercentageStep());
		solver.setAutoSaveFilename(config.getAutosaveFilename());
		
		// TODO: check for forbidden values using a method inside of Config class
		
		return config.getType().toLowerCase().equals("cpu") ? (T) cpuSolver : (T) gpuSolver;
	}
	
	/**
	 * Calls all initialization callbacks, then starts the {@link Solver}'s run()-method, then calls all termination callbacks.
	 * @throws InterruptedException if the time limit is exceeded while waiting for the Solverthread to shutdown after terminating (time limit is the bigger one of progressUpdateDelay and timeUpdateDelay)
	 * @see #checkForPrepation()
	 * @see #run()
	 */
	public final void solve() {
		finishStoring = false;	// reset finishStoring to false, otherwise autosave doesn't work
		
		checkForPreparation();
		state = NQueensFAF.INITIALIZING;
		if(initializationCallback != null)
			initializationCallback.callback(this);
		ucExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

		state = NQueensFAF.RUNNING;
		startUpdateCallerThreads();
		if(autoSaveEnabled)
			startAutoSaverThread();
		run();

		state = NQueensFAF.TERMINATING;
		ucExecutor.shutdown();
		try {
			ucExecutor.awaitTermination(NQueensFAF.DEFAULT_TIME_UPDATE_DELAY > progressUpdateDelay ? NQueensFAF.DEFAULT_TIME_UPDATE_DELAY+100 : progressUpdateDelay+100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(terminationCallback != null)
			terminationCallback.callback(this);
		
		state = NQueensFAF.IDLE;
	}
	/**
	 * Checks if all prepartions are done correctly.
	 * * @throws {@link IllegalStateException} if N equals 0, the {@link Solver} is already running or if getProgress() >= 100
	 */
	private void checkForPreparation() {
		if(N == 0) {
			state = NQueensFAF.IDLE;
			throw new IllegalStateException("Board size was not set");
		}
		if(!isIdle()) {
			state = NQueensFAF.IDLE;
			throw new IllegalStateException("Solver is already started");
		}
		if(getProgress() == 1.0f) {
			state = NQueensFAF.IDLE;
			throw new IllegalStateException("Solver is already done, nothing to do here");
		}
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
				long tmpTime = 0;
				while(isRunning()) {
					if(getDuration() != tmpTime) {
						onTimeUpdateCallback.onTimeUpdate(getDuration());
						tmpTime = getDuration();
					}
					if(!isRunning())
						break;
					try {
						Thread.sleep(NQueensFAF.DEFAULT_TIME_UPDATE_DELAY);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				onTimeUpdateCallback.onTimeUpdate(getDuration());
			});
		}
		if(onProgressUpdateCallback != null) {
			ucExecutor.submit(() -> {
				float tmpProgress = 0;
				long tmpSolutions = 0;
				while(isRunning()) {
					if(getProgress() != tmpProgress || getSolutions() != tmpSolutions) {
						onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions(), getDuration());
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
				onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions(), getDuration());
			});
		}
	}

	/**
	 * Starts the thread that continously checks if the progress has increased by {@link #autoSavePercentageStep} or more,
	 * and if yes, calls store() using {@link #autoSaveFilename} the current Solver.
	 * Only used when {@link #autoSave} is true
	 * @see #autoSaverThread
	 */
	private void startAutoSaverThread() {
		autoSaverThread = new Thread(() -> {
			String filename = autoSaveFilename;
			filename = filename.replaceAll("#N#", ""+ N);
			if(!filename.endsWith(".faf")) {
				filename += ".faf";
			}
			float progress = getProgress() * 100;
			int tmpProgress = (int) progress / autoSavePercentageStep * autoSavePercentageStep;
			while(isRunning() && !finishStoring) {
				progress = getProgress() * 100;
				if(progress >= 100)
					break;
				else if(progress >= tmpProgress + autoSavePercentageStep) {
					try {
						store(filename);
					} catch (IOException e) {
						e.printStackTrace();
					}
					tmpProgress = (int) progress;
				}
				try {
					Thread.sleep(progressUpdateDelay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			progress = getProgress() * 100;
			if(progress >= 100) {
				if(autoDeleteEnabled)
					try {
						new File(filename).delete();
					} catch(SecurityException e) {}
			}
		});
		autoSaverThread.start();
	}
	
	/**
	 * Can be used for solving the problem for small N's (board sizes).
	 * @return the number of solutions
	 */
	protected int solveSmallBoard() {
		solutionsSmallN = 0;
		int mask = (1 << getN()) - 1;
		nq(0, 0, 0, 0, mask, mask);
		return solutionsSmallN;
	}
	/**
	 * Solves the N-Queens-problem in a very simple and easy way.
	 * Only for small N's, bigger N's would take much longer with this method.
	 * @param ld soon to read about in an extern documentation paper
	 * @param rd soon to read about in an extern documentation paper
	 * @param col soon to read about in an extern documentation paper
	 * @param row soon to read about in an extern documentation paper
	 * @param free soon to read about in an extern documentation paper
	 * @param mask soon to read about in an extern documentation paper
	 * @param solutions soon to read about in an extern documentation paper
	 */
	private void nq(int ld, int rd, int col, int row, int free, int mask) {
		if(row == getN()-1) {
			solutionsSmallN++;
			return;
		}

		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~((ld|bit)<<1 | (rd|bit)>>1 | col|bit) & mask;

			if(nextfree > 0)
				nq((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree, mask);
		}
	}
	
	/**
	 * Wraps {@link #store_(String)}.
	 * @param filepath path/name of the file the Solver's progress state should be stored in
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public final synchronized void store(String filepath) throws IOException, IllegalArgumentException {
		isStoring = true;
		store_(filepath);
		isStoring = false;
	}

	/**
	 * Wraps {@link #restore_(String)}.
	 * @param filepath path/name of the file the Solver's progress state should be restored from
	 * @throws IOException 
	 * @throws ClassCastException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalArgumentException
	 */
	public final void restore(String filepath) throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
		restore_(filepath);
		solve();
	}
	
	/**
	 * Adds a callback that will be executed on start of the {@link Solver}.
	 * The callbacks will be called in reversed insertion order.
	 * @param r the callback as {@link Runnable}
	 * @throws {@link IllegalArgumentException} if r is null
	 * @see #solve()
	 */
	public final void setInitializationCallback(BasicCallback r) {
		if(r == null) {
			throw new IllegalArgumentException("initializationCallback must not be null");
		}
		initializationCallback = r;
	}

	/**
	 * Adds a callback that will be executed on finish of the {@link Solver}.
	 * The callbacks will be called in reversed insertion order.
	 * @param r the callback as {@link Runnable}
	 * @throws {@link IllegalArgumentException} if r is null
	 * @see #solve()
	 */
	public final void setTerminationCallback(BasicCallback r) {
		if(r == null) {
			throw new IllegalArgumentException("terminationCallback must not be null");
		}
		terminationCallback = r;
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
	 * @param n boardsize 
	 * @throws {@link IllegalStateException} if the {@link Solver} is already running
	 * @throws {@link IllegalArgumentException} if the boardsize is invalid 
	 */
	public final void setN(int n) {
		if(!isIdle()) {
			throw new IllegalStateException("Cannot set board size while solving");
		}
		if(n <= 0 || n > 31) {
			throw new IllegalArgumentException("Board size must be a number between 0 and 32 (not inclusive)");
		}
		N = n;
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
	 * @param onTimeUpdateCallback callback to be called on each time update
	 */
	public final void setOnTimeUpdateCallback(OnTimeUpdateCallback onTimeUpdateCallback) {
		if(onTimeUpdateCallback == null) {
			this.onTimeUpdateCallback = (duration) -> {};
		} else {
			this.onTimeUpdateCallback = onTimeUpdateCallback;
		}
	}

	/**
	 * Gets {@link #onProgressUpdateCallback}.
	 * @return {@link #onProgressUpdateCallback}
	 */
	public final OnProgressUpdateCallback getOnProgressUpdateCallback() {
		return onProgressUpdateCallback;
	}
	/**
	 * Sets {@link #onProgressUpdateCallback} or deletes an existing callback if the parameter is null.
	 * @param onProgressUpdateCallback callback to be called on each progress update
	 */
	public final void setOnProgressUpdateCallback(OnProgressUpdateCallback onProgressUpdateCallback) {
		if(onProgressUpdateCallback == null) {
			this.onProgressUpdateCallback = (progress, solutions, duration) -> {};
		} else {
			this.onProgressUpdateCallback = onProgressUpdateCallback;
		}
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
	 * @param progressUpdateDelay
	 * @throws {@link IllegalArgumentException} if the given delay is <= 0
	 */
	public final void setProgressUpdateDelay(long progressUpdateDelay) {
		if(progressUpdateDelay < 0) {
			throw new IllegalArgumentException("progressUpdateDelay must be a number >= 0");
		} else if(progressUpdateDelay== 0) {
			this.progressUpdateDelay = NQueensFAF.DEFAULT_PROGRESS_UPDATE_DELAY;
		}
		this.progressUpdateDelay = progressUpdateDelay;
	}

	/**
	 * Gets {@link #autoSaveEnabled}.
	 * @return {@link #autoSaveEnabled}
	 */
	public final boolean isAutoSaveEnabled() {
		return autoSaveEnabled;
	}
	/**
	 * Enables or disables the auto save function.
	 * @param autoSaveEnabled if true, enables automatic saving of the current Solver; if false, disables automatic saving (default value).
	 */
	public final void setAutoSaveEnabled(boolean autoSaveEnabled) {
		this.autoSaveEnabled = autoSaveEnabled;
	}

	/**
	 * Gets {@link #autoDeleteEnabled}.
	 * @return {@link #autoDeleteEnabled}
	 */
	public final boolean isAutoDeleteEnabled() {
		return autoDeleteEnabled;
	}
	/**
	 * Enables or disables the auto delete function.
	 * @param autoDeleteEnabled if true, enables automatic deleting of the files created by the auto save function
	 */
	public final void setAutoDeleteEnabled(boolean autoDeleteEnabled) {
		this.autoDeleteEnabled = autoDeleteEnabled;
	}
	
	/**
	 * Gets {@link #autoSavePercentageStep}.
	 * @return {@link #autoSavePercentageStep}
	 */
	public final int getAutoSavePercentageStep() {
		return autoSavePercentageStep;
	}
	/**
	 * Sets {@link #autoSavePercentageStep}.
	 * @param autoSavePercentageStep
	 */
	public final void setAutoSavePercentageStep(int autoSavePercentageStep) {
		if(autoSavePercentageStep <= 0 || autoSavePercentageStep >= 100) {
			throw new IllegalArgumentException("progressUpdateDelay must be a number between 0 and 100");
		}
		this.autoSavePercentageStep = autoSavePercentageStep;
	}
	
	/**
	 * Can be called before exiting the program.
	 * If the Solver is currently storing, this method blocks until the storing is done.
	 * @return {@link #isStoring}
	 */
	public final void finishStoring() {
		finishStoring = true;
		while(isStoring) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	/**
	 * Gets {@link #autoSaveFilename}.
	 * @return {@link #autoSaveFilename}
	 */
	public final String getAutoSaveFilename() {
		return autoSaveFilename;
	}
	/**
	 * Sets {@link #autoSaveFilename}.
	 * @param autoSaveFilename
	 */
	public final void setAutoSaveFilename(String autoSaveFilename) {
		this.autoSaveFilename = autoSaveFilename;
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
