package de.nqueensfaf;

import java.util.function.Consumer;

public abstract class Solver {
    
    private int n;
    private int updateInterval = 128;
    private volatile Status status = Status.IDLE;
    private Thread asyncSolverThread, bgThread;
    private OnUpdateConsumer onUpdateConsumer;
    private Consumer<Solver> initCb, finishCb;
    private int solutionsSmallN = 0;
    
    public abstract long getDuration();
    public abstract float getProgress();
    public abstract long getSolutions();
    
    protected abstract void run();
    
    @SuppressWarnings("unchecked")
    public final <T extends Solver> T solve() {
	preconditions();
	
	if (initCb != null)
	    initCb.accept(this);

	status = Status.RUNNING;
	if(updateInterval > 0) { // if updateInterval is 0, it means disable progress updates
	    boolean updateConsumer = false;
	    if (onUpdateConsumer != null)
		updateConsumer = true;
	    bgThread = backgroundThread(updateConsumer);
	    bgThread.start();
	}
	
	try {
	    run();
	} catch (Exception e) {
	    status = Status.IDLE;
	    throw new RuntimeException("error while running solver: " + e.getMessage());
	}

	status = Status.IDLE;
	if(updateInterval > 0) {
	    try {
		bgThread.join();
	    } catch (InterruptedException e) {
		throw new RuntimeException("could not wait for background thread to terminate: " + e.getMessage());
	    }
	}
	if (finishCb != null)
	    finishCb.accept(this);

	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T solveAsync() {
	asyncSolverThread = new Thread(() -> solve());
	asyncSolverThread.start();
	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T waitFor() {
	if(asyncSolverThread == null || !asyncSolverThread.isAlive())
	    throw new IllegalStateException("could not wait for solver thread to terminate: solver is not running asynchronous");
	try {
	    asyncSolverThread.join();
	} catch (InterruptedException e) {
	    throw new RuntimeException("could not wait for solver thread to terminate: " + e.getMessage());
	}
	return (T) this;
    }
    
    private void preconditions() {
	if (n == 0) {
	    state = Status.IDLE;
	    throw new IllegalStateException("starting conditions not fullfilled: board size was not set");
	}
	if (!isIdle()) {
	    state = Status.IDLE;
	    throw new IllegalStateException("starting conditions not fullfilled: solver is already started");
	}
	if (getProgress() == 1.0f) {
	    state = Status.IDLE;
	    throw new IllegalStateException("starting conditions not fullfilled: solver is already done, nothing to do here");
	}
    }

    private Thread backgroundThread(boolean updateConsumer) {
	return new Thread(() -> {
	    while (isRunning() && getProgress() < 1f) {
		if(updateConsumer)
		    onUpdateConsumer.accept(this, getProgress(), getSolutions(), getDuration());
		
		try {
		    Thread.sleep(updateInterval);
		} catch (InterruptedException e) {
		    // ignore
		}
	    }

	    if(updateConsumer)
		onUpdateConsumer.accept(this, getProgress(), getSolutions(), getDuration());
	});
    }
    
    protected int solveSmallBoard() {
	solutionsSmallN = 0;
	int mask = (1 << n) - 1;
	smallBoardNQ(0, 0, 0, 0, mask, mask);
	return solutionsSmallN;
    }

    private void smallBoardNQ(int ld, int rd, int col, int row, int free, int mask) {
	if (row == n - 1) {
	    solutionsSmallN++;
	    return;
	}

	int bit;
	int nextfree;

	while (free > 0) {
	    bit = free & (-free);
	    free -= bit;
	    nextfree = ~((ld | bit) << 1 | (rd | bit) >> 1 | col | bit) & mask;

	    if (nextfree > 0)
		smallBoardNQ((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree, mask);
	}
    }
    
    @SuppressWarnings("unchecked")
    public final <T extends Solver> T onInit(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("could not set initialization callback: callback must not be null");
	}
	initCb = c;
	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T onFinish(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("could not set finish callback: callback must not be null");
	}
	finishCb = c;
	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T onUpdate(OnUpdateConsumer onUpdateConsumer) {
	if (onUpdateConsumer == null) {
	    this.onUpdateConsumer = (self, progress, solutions, duration) -> {};
	} else {
	    this.onUpdateConsumer = onUpdateConsumer;
	}
	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T setN(int n) {
	if (!isIdle()) {
	    throw new IllegalStateException("could not set board size: solver already running");
	}
	if (n <= 0 || n > 31) {
	    throw new IllegalArgumentException("could not set board size: must be a number between >0 and <32");
	}
	this.n = n;
	return (T) this;
    }
    
    public final int getN() {
	return n;
    }
    
    @SuppressWarnings("unchecked")
    public final <T extends Solver> T setUpdateInterval(int updateInterval) {
	if (updateInterval < 0)
	    throw new IllegalArgumentException("invalid value for updateInterval: must be a number >=0 (0 means disabling updates)");
	this.updateInterval = updateInterval;
	return (T) this;
    }
    
    public final int getUpdateInterval() {
	return updateInterval;
    }
    
    public final boolean isIdle() {
	return status == Status.IDLE;
    }

    public final boolean isRunning() {
	return status == Status.RUNNING;
    }
    
    private static enum Status {
	IDLE, RUNNING
    }
    
    public interface OnUpdateConsumer {
	void accept(Solver self, float progress, long solutions, long duration);
    }
}
