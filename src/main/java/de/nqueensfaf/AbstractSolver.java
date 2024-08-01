package de.nqueensfaf;

import static de.nqueensfaf.SolverStatus.*;

public abstract class AbstractSolver implements Solver {
    
    private int n;
    private volatile SolverStatus status = NOT_INITIALIZED;
    
    private Runnable onInit, onFinish;
    private OnUpdateConsumer onUpdate;
    private int updateInterval = 128;
    private Thread bgThread;
    
    protected abstract void run();
    
    public final void solve() {
	preconditions();

	status = STARTING;
	
	if (onInit != null)
	    onInit.run();

	if(updateInterval > 0 && onUpdate != null) { // if updateInterval is 0, it means disable progress updates
	    bgThread = new Thread(() -> {
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
	    bgThread.start();
	}
  
	status = RUNNING;
	try {
	    run();
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
    
    public final void onInit(Runnable c) {
	if (c == null) {
	    throw new IllegalArgumentException("could not set initialization callback: callback must not be null");
	}
	onInit = c;
    }

    public final void onFinish(Runnable c) {
	if (c == null) {
	    throw new IllegalArgumentException("could not set finish callback: callback must not be null");
	}
	onFinish = c;
    }

    public final void onUpdate(OnUpdateConsumer onUpdate) {
	if (onUpdate == null) {
	    this.onUpdate = (progress, solutions, duration) -> {};
	} else {
	    this.onUpdate = onUpdate;
	}
    }

    public final void setN(int n) {
	if (status.isAfter(READY) && status.isBefore(FINISHED)) {
	    throw new IllegalStateException("could not set board size: solver has already started");
	}
	if (n <= 0 || n > 31) {
	    throw new IllegalArgumentException("could not set board size: must be a number between >0 and <32");
	}
	this.n = n;
    }
    
    public final int getN() {
	return n;
    }
    
    public final void setUpdateInterval(int updateInterval) {
	if (updateInterval < 0)
	    throw new IllegalArgumentException("invalid value for updateInterval: must be a number >=0 (0 means disabling updates)");
	this.updateInterval = updateInterval;
    }
    
    public final int getUpdateInterval() {
	return updateInterval;
    }
    
    public interface OnUpdateConsumer {
	void accept(float progress, long solutions, long duration);
    }
    
    public SolverStatus getStatus() {
	return status;
    }
}
