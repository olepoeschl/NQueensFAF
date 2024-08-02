package de.nqueensfaf;

import static de.nqueensfaf.SolverExecutionState.*;

/**
 * This class provides a skeletal implementation of the {@link Solver}
 * interface to minimize the effort required to implement this interface.
 * It wraps the {@link Solver#solve()}, keeping the
 * {@link SolverExecutionState} updated and providing the possibility to define
 * callbacks for certain events.
 * 
 * @see Solver
 */
public abstract class AbstractSolver implements Solver {
    
    private int n;
    private volatile SolverExecutionState executionState = NOT_INITIALIZED;
    
    private Runnable onStart, onFinish;
    private OnUpdateConsumer onUpdate;
    private int updateInterval = 128;
    private Thread bgThread;

    @Override
    public final void setN(int n) {
	if (executionState.isAfter(READY) && executionState.isBefore(FINISHED)) {
	    throw new IllegalStateException("could not set board size: solver has already started");
	}
	if (n <= 0 || n > 31) {
	    throw new IllegalArgumentException("could not set board size: " + n + " is not a number between 0 and 32 (exclusive)");
	}
	this.n = n;
    }

    @Override
    public final int getN() {
	return n;
    }

    /**
     * Wraps the {@link Solver#solve()} method. Keeps the {@link SolverExecutionState}
     * updated and executes callbacks (if defined) when the {@link Solver} starts, 
     * finishes or when it makes progress. The progress is tracked by a
     * background thread that continuously queries the solution count, the 
     * progress and the duration of the {@link Solver}.
     * 
     * @see SolverExecutionState
     */
    public final void start() {
	preconditions();

	executionState = STARTING;
	
	if (onStart != null)
	    onStart.run();

	if(updateInterval > 0 && onUpdate != null) { // if updateInterval is 0, it means disable progress updates
	    bgThread = Thread.ofVirtual().start(() -> {
		while (executionState == RUNNING && getProgress() < 1f) {
		    onUpdate.accept(getProgress(), getSolutions(), getDuration());
		    try {
			Thread.sleep(updateInterval);
		    } catch (InterruptedException e) {
			// ignore
		    }
		}
		onUpdate.accept(getProgress(), getSolutions(), getDuration());
	    });
	}
  
	executionState = RUNNING;
	try {
	    solve();
	} catch (Exception e) {
	    executionState = CANCELED;
	    throw new RuntimeException("error while running solver: " + e.getMessage(), e);
	}
	
	executionState = TERMINATING;
	
	if(bgThread != null) {
	    try {
		bgThread.join();
	    } catch (InterruptedException e) {
		throw new RuntimeException("could not wait for background thread to terminate: " + e.getMessage(), e);
	    }
	}
	if (onFinish != null)
	    onFinish.run();
	
	executionState = FINISHED;
    }
    
    private void preconditions() {
	if (n == 0)
	    throw new IllegalStateException("starting conditions not fullfilled: board size was not set");
	
	if (executionState.isAfter(READY) && executionState.isBefore(FINISHED))
	    throw new IllegalStateException("starting conditions not fullfilled: solver is neither ready nor finished, nor canceled");
	
	if (getProgress() == 1.0f)
	    throw new IllegalStateException("starting conditions not fullfilled: solver is already done, nothing to do here");
    }

    @Override
    public final SolverExecutionState getExecutionState() {
	return executionState;
    }
    
    /**
     * Sets the callback that is executed when {@link #start()} is called,
     * just before {@link Solver#solve()} is executed.
     * 
     * @param cb the runnable to be executed. Must not be {@code null}.
     * 
     * @see #start()
     */
    public final void onStart(Runnable cb) {
	if (cb == null) {
	    throw new IllegalArgumentException("could not set starting callback: callback must not be null");
	}
	onStart = cb;
    }

    /**
     * Sets the callback that is executed when {@link Solver#solve()} has
     * returned and, if alive, the background thread for tracking the {@link Solver}'s
     * progress has terminated.
     * 
     * @param cb the runnable to be executed. Must not be {@code null}.
     * 
     * @see #start()
     */
    public final void onFinish(Runnable cb) {
	if (cb == null) {
	    throw new IllegalArgumentException("could not set finish callback: callback must not be null");
	}
	onFinish = cb;
    }

    /**
     * Defines the interface of a consumer callback to be executed on progress
     * updates of the {@link Solver}.
     */
    public interface OnUpdateConsumer {
	/**
	 * Consumes the current progress, solution count and solving duration of a
	 * {@link Solver}.
	 * 
	 * @param progress the current progress of the {@link Solver}.
	 * @param solutions the current total count of solutions found by the
	 * {@link Solver}.
	 * @param duration the current duration the {@link Solver} spent
	 * computing.
	 */
	void accept(float progress, long solutions, long duration);
    }

    /**
     * Sets the callback that is executed on progress updates of the
     * {@link Solver#solve()}. The callback is executed only during the
     * solving process.
     * 
     * @param onUpdate the consumer to be executed. Must not be {@code null}.
     * 
     * @see #start()
     */
    public final void onUpdate(OnUpdateConsumer onUpdate) {
	if (onUpdate == null) {
	    throw new IllegalArgumentException("could not set onUpdate callback: callback must not be null");
	}
	this.onUpdate = onUpdate;
    }
    
    /**
     * Sets the interval in which the background thread queries the
     * {@link Solver}'s progress and executes the callback defined in
     * {@link #onUpdate}.
     * 
     * @param updateInterval the number of milliseconds to wait between
     * progress updates.
     */
    public final void setUpdateInterval(int updateInterval) {
	if (updateInterval < 0)
	    throw new IllegalArgumentException("invalid value for updateInterval: must be a number >=0 (0 means disabling updates)");
	this.updateInterval = updateInterval;
    }
    
    /**
     * Gets the interval in which the background thread queries the
     * {@link Solver}'s progress and executes the callback defined in
     * {@link #onUpdate}.
     * 
     * @return the number of milliseconds to wait between
     * progress updates.
     */
    public final int getUpdateInterval() {
	return updateInterval;
    }
}
