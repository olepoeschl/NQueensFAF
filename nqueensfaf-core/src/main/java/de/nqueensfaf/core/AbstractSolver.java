package de.nqueensfaf.core;

import static de.nqueensfaf.core.ExecutionState.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * This class provides a skeletal implementation of the {@link Solver} interface
 * to minimize the effort required to implement this interface. It wraps the
 * {@link Solver#solve()}, keeping the {@link ExecutionState} updated and
 * providing the possibility to define callbacks for certain events.
 * 
 * @see Solver
 */
public abstract class AbstractSolver implements Solver {

    private int n = 0;
    private volatile ExecutionState executionState = NOT_INITIALIZED;

    private Runnable onStart = () -> {
    };
    private Runnable onFinish = () -> {
    };
    private Consumer<Exception> onCancel = e -> {
    };
    private OnProgressUpdateConsumer onProgressUpdate = (p, s, d) -> {
    };
    private int updateInterval = 200;
    private Timer timer;

    @Override
    public final void setN(int n) {
	if (executionState.isBusy()) {
	    throw new IllegalStateException("could not set board size: solver has already started");
	}
	if (n <= 0 || n > 31) {
	    throw new IllegalArgumentException(
		    "could not set board size: " + n + " is not a number between 0 and 32 (exclusive)");
	}
	this.n = n;
    }

    @Override
    public final int getN() {
	return n;
    }

    /**
     * Wraps the {@link Solver#solve()} method. Keeps the
     * {@link ExecutionState} updated and executes callbacks (if defined) when
     * the {@link Solver} starts, finishes or when it makes progress. The progress
     * is tracked by a background thread that continuously queries the solution
     * count, the progress and the duration of the {@link Solver}.
     * 
     * @see ExecutionState
     */
    public final void start() {
	preconditions();

	executionState = STARTING;

	onStart.run();

	if (updateInterval > 0) { // if updateInterval is 0, it means disable progress updates
	    timer = new Timer();
	    timer.schedule(new TimerTask() {
		@Override
		public void run() {
		    if(executionState != RUNNING || getProgress() >= 1f)
			return;
		    onProgressUpdate.accept(getProgress(), getSolutions(), getDuration());
		}
	    }, 0, updateInterval);
	}

	executionState = RUNNING;
	try {
	    solve();
	} catch (Exception e) {
	    executionState = CANCELED;
	    onCancel.accept(e);
	    throw new RuntimeException("error while running solver: " + e.getMessage(), e);
	}

	executionState = TERMINATING;

	if (updateInterval > 0) {
	    timer.cancel();
	    onProgressUpdate.accept(getProgress(), getSolutions(), getDuration()); // one last update
	}

	onFinish.run();

	executionState = FINISHED;
    }

    private void preconditions() {
	if (n == 0)
	    throw new IllegalStateException("starting conditions not fullfilled: board size was not set");

	if (executionState.isAfter(READY) && executionState.isBefore(FINISHED))
	    throw new IllegalStateException(
		    "starting conditions not fullfilled: solver is neither ready nor finished, nor canceled");
    }

    @Override
    public final ExecutionState getExecutionState() {
	return executionState;
    }

    /**
     * Sets the callback that is executed when {@link #start()} is called, just
     * before {@link Solver#solve()} is executed.
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
     * Sets the callback that is executed when an Exception was thrown during
     * {@link Solver#solve()}, meaning that the {@link Solver} did not finish
     * successfully.
     * 
     * @param cb the runnable to be executed. Must not be {@code null}.
     * 
     * @see #start()
     */
    public final void onCancel(Consumer<Exception> cb) {
	if (cb == null) {
	    throw new IllegalArgumentException("could not set cancel callback: callback must not be null");
	}
	onCancel = cb;
    }

    /**
     * Sets the callback that is executed when {@link Solver#solve()} has returned
     * and, if alive, the background thread for tracking the {@link Solver}'s
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
     * Sets the callback that is executed on progress updates of the
     * {@link Solver#solve()}. The callback is executed only during the solving
     * process.
     * 
     * @param onUpdate the consumer to be executed. Must not be {@code null}.
     * 
     * @see #start()
     */
    public final void onProgressUpdate(OnProgressUpdateConsumer onProgressUpdate) {
	if (onProgressUpdate == null) {
	    throw new IllegalArgumentException("could not set onProgressUpdate callback: callback must not be null");
	}
	this.onProgressUpdate = onProgressUpdate;
    }

    /**
     * Sets the interval in which the background thread queries the {@link Solver}'s
     * progress and executes the callback defined in {@link #onProgressUpdate}.
     * 
     * @param updateInterval the number of milliseconds to wait between progress
     *                       updates.
     */
    public final void setUpdateInterval(int updateInterval) {
	if (executionState.isBusy())
	    throw new IllegalStateException("could not set update interval: solver has already started");
	if (updateInterval < 0)
	    throw new IllegalArgumentException(
		    "invalid value for updateInterval: must be a number >=0 (0 means disabling updates)");
	this.updateInterval = updateInterval;
    }

    /**
     * Gets the interval in which the background thread queries the {@link Solver}'s
     * progress and executes the callback defined in {@link #onProgressUpdate}.
     * 
     * @return the number of milliseconds to wait between progress updates.
     */
    public final int getUpdateInterval() {
	return updateInterval;
    }

    /**
     * Defines the interface of a consumer callback to be executed on progress
     * updates of the {@link Solver}.
     */
    public interface OnProgressUpdateConsumer {
	/**
	 * Consumes the current progress, solution count and solving duration of a
	 * {@link Solver}.
	 * 
	 * @param progress  the current progress of the {@link Solver}.
	 * @param solutions the current total count of solutions found by the
	 *                  {@link Solver}.
	 * @param duration  the current duration the {@link Solver} spent computing.
	 */
	void accept(float progress, long solutions, long duration);
    }
}
