package de.nqueensfaf;

import static de.nqueensfaf.SolverStatus.*;

public abstract class AbstractSolver implements Solver {
    
    private int n;
    private volatile SolverStatus status = NOT_INITIALIZED;
    
    private Runnable onStart, onFinish;
    private OnUpdateConsumer onUpdate;
    private int updateInterval = 128;
    private Thread bgThread;

    @Override
    public final void setN(int n) {
	if (status.isAfter(READY) && status.isBefore(FINISHED)) {
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
     * a wrapper around the {@link #solve()} method.
     * Enables callbacks to be defined for certain events
     */
    public final void start() {
	preconditions();

	status = STARTING;
	
	if (onStart != null)
	    onStart.run();

	if(updateInterval > 0 && onUpdate != null) { // if updateInterval is 0, it means disable progress updates
	    bgThread = Thread.ofVirtual().start(() -> {
		while (status == RUNNING && getProgress() < 1f) {
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
  
	status = RUNNING;
	try {
	    solve();
	} catch (Exception e) {
	    status = CANCELED;
	    throw new RuntimeException("error while running solver: " + e.getMessage(), e);
	}
	
	status = TERMINATING;
	
	if(bgThread != null) {
	    try {
		bgThread.join();
	    } catch (InterruptedException e) {
		throw new RuntimeException("could not wait for background thread to terminate: " + e.getMessage(), e);
	    }
	}
	if (onFinish != null)
	    onFinish.run();
	
	status = FINISHED;
    }
    
    private void preconditions() {
	if (n == 0)
	    throw new IllegalStateException("starting conditions not fullfilled: board size was not set");
	
	if (status.isAfter(READY) && status.isBefore(FINISHED))
	    throw new IllegalStateException("starting conditions not fullfilled: solver is neither ready nor finished, nor canceled");
	
	if (getProgress() == 1.0f)
	    throw new IllegalStateException("starting conditions not fullfilled: solver is already done, nothing to do here");
    }

    @Override
    public final SolverStatus getStatus() {
	return status;
    }
    
    public final void onStart(Runnable c) {
	if (c == null) {
	    throw new IllegalArgumentException("could not set starting callback: callback must not be null");
	}
	onStart = c;
    }

    public final void onFinish(Runnable c) {
	if (c == null) {
	    throw new IllegalArgumentException("could not set finish callback: callback must not be null");
	}
	onFinish = c;
    }

    public interface OnUpdateConsumer {
	void accept(float progress, long solutions, long duration);
    }
    
    public final void onUpdate(OnUpdateConsumer onUpdate) {
	if (onUpdate == null) {
	    this.onUpdate = (progress, solutions, duration) -> {};
	} else {
	    this.onUpdate = onUpdate;
	}
    }
    
    public final void setUpdateInterval(int updateInterval) {
	if (updateInterval < 0)
	    throw new IllegalArgumentException("invalid value for updateInterval: must be a number >=0 (0 means disabling updates)");
	this.updateInterval = updateInterval;
    }
    
    public final int getUpdateInterval() {
	return updateInterval;
    }
}
