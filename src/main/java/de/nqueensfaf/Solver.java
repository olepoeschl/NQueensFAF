package de.nqueensfaf;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class Solver {

    private static final int IDLE = 1;
    private static final int INITIALIZING = 2;
    private static final int RUNNING = 3;
    private static final int TERMINATING = 4;
    
    protected int N;
    
    private volatile int state = IDLE;
    private Thread asyncSolverThread, bgThread;
    private OnUpdateConsumer onUpdateConsumer;
    private Consumer<Solver> initCb, finishCb;
    private int solutionsSmallN = 0;
    private boolean isStoring = false;
    
    protected Solver() {
	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	    while (isStoring) {
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	    }
	}));
    }

    public abstract <T extends Config> T getConfig();
    public abstract long getDuration();
    public abstract float getProgress();
    public abstract long getSolutions();
    protected abstract void run();
    protected abstract void store_(String filepath) throws IOException;
    protected abstract void inject_(String filepath) throws IOException, ClassNotFoundException, ClassCastException;
    
    @SuppressWarnings("unchecked")
    public final <T extends Solver> T solve() {
	preconditions();
	
	state = INITIALIZING;
	if (initCb != null)
	    initCb.accept(this);

	state = RUNNING;
	boolean updateConsumer = false, autoSaver = false;
	if (onUpdateConsumer != null && getConfig().updateInterval > 0) // if updateInterval is 0, it means, disable progress updates
	    updateConsumer = true;
	if (getConfig().autoSaveEnabled)
	    autoSaver = true;
	bgThread = backgroundThread(updateConsumer, autoSaver);
	bgThread.start();
	
	try {
	    run();
	} catch (Exception e) {
	    state = TERMINATING;
	    throw e;
	}

	state = TERMINATING;
	try {
	    bgThread.join();
	} catch (InterruptedException e) {
	    throw new RuntimeException("unexpected error while waiting for background thread to die: " + e.getMessage());
	}
	if (finishCb != null)
	    finishCb.accept(this);

	state = IDLE;
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
	    throw new IllegalStateException("waitFor() can not be called: solver is not running asynchronous at the moment");
	try {
	    asyncSolverThread.join();
	} catch (InterruptedException e) {
	    Thread.currentThread().interrupt();
	}
	return (T) this;
    }
    
    private void preconditions() {
	if (N == 0) {
	    state = IDLE;
	    throw new IllegalStateException("board size was not set");
	}
	if (!isIdle()) {
	    state = IDLE;
	    throw new IllegalStateException("solver is already started");
	}
	if (getProgress() == 1.0f) {
	    state = IDLE;
	    throw new IllegalStateException("solver is already done, nothing to do here");
	}
    }

    private Thread backgroundThread(boolean updateConsumer, boolean autoSaver) {
	return new Thread(() -> {
	    // for autoSaver
	    String filePath = getConfig().autoSavePath;
	    filePath = filePath.replaceAll("\\{N\\}", "" + N);
	    float progress = getProgress() * 100;
	    float tmpProgress = progress;
	    
	    while (isRunning() && getProgress() < 1f) {
		if(updateConsumer) {
		    onUpdateConsumer.accept(this, getProgress(), getSolutions(), getDuration());
		}
		
		if(autoSaver) {
		    progress = getProgress() * 100;
		    if (progress >= 100)
			break;
		    else if (progress >= tmpProgress + getConfig().autoSavePercentageStep) {
			try {
			    store(filePath);
			} catch (IllegalArgumentException | IOException e) {
			    System.err.println("error in autosaver thread: " + e.getMessage());
			}
			tmpProgress = progress;
		    }
		}
		
		try {
		    Thread.sleep(getConfig().updateInterval);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	    }

	    if(updateConsumer) {
		onUpdateConsumer.accept(this, getProgress(), getSolutions(), getDuration());
	    }
		
	    if(autoSaver) {
		progress = getProgress() * 100;
		if (progress >= 100) {
		    if (getConfig().autoDeleteEnabled) {
			try {
			    new File(filePath).delete();
			} catch (SecurityException e) {
			    throw new SecurityException("unable to delete autosave file", e);
			}
		    }
		}
	    }
	});
    }
    
    protected int solveSmallBoard() {
	solutionsSmallN = 0;
	int mask = (1 << getN()) - 1;
	nq(0, 0, 0, 0, mask, mask);
	return solutionsSmallN;
    }

    private void nq(int ld, int rd, int col, int row, int free, int mask) {
	if (row == getN() - 1) {
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
		nq((ld | bit) << 1, (rd | bit) >> 1, col | bit, row + 1, nextfree, mask);
	}
    }

    public final void store(String filepath) throws IOException, IllegalArgumentException {
	if(isStoring)
	    return;
	isStoring = true;
	store_(filepath);
	isStoring = false;
    }

    public final synchronized void inject(File file)
	    throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
	inject_(file.getAbsolutePath());
	getConfig().autoSavePath = file.getAbsolutePath();
	solve();
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T onInit(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("argument must not be null");
	}
	initCb = c;
	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T onFinish(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("argument must not be null");
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
	    throw new IllegalStateException("cannot set board size while solving");
	}
	if (n <= 0 || n > 31) {
	    throw new IllegalArgumentException("board size must be a number between 0 and 32 (not inclusive)");
	}
	N = n;
	return (T) this;
    }
    
    // getters
    public final int getN() {
	return N;
    }
    
    public final boolean isIdle() {
	return state == IDLE;
    }

    public final boolean isInitializing() {
	return state == INITIALIZING;
    }

    public final boolean isRunning() {
	return state == RUNNING;
    }

    public final boolean isTerminating() {
	return state == TERMINATING;
    }
    
    public interface OnUpdateConsumer {
	void accept(Solver self, float progress, long solutions, long duration);
    }
}
