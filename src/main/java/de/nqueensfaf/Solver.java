package de.nqueensfaf;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class Solver {
    
    protected int n;
    
    private volatile Status state = Status.IDLE;
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
		    e.printStackTrace();
		}
	    }
	}));
    }
    
    public abstract long getDuration();
    public abstract float getProgress();
    public abstract long getSolutions();
    
    public abstract <T extends Config> T config();
    
    protected abstract void run();
    protected abstract void save_(String filepath) throws IOException;
    protected abstract void load_(String filepath) throws IOException, ClassNotFoundException, ClassCastException;
    
    @SuppressWarnings("unchecked")
    public final <T extends Solver> T solve() {
	preconditions();
	
	state = Status.INITIALIZING;
	if (initCb != null)
	    initCb.accept(this);

	state = Status.RUNNING;
	if(config().updateInterval > 0) { // if updateInterval is 0, it means disable progress updates
	    boolean updateConsumer = false, autoSaver = false;
	    if (onUpdateConsumer != null)
		updateConsumer = true;
	    if (config().autoSaveEnabled)
		autoSaver = true;
	    bgThread = backgroundThread(updateConsumer, autoSaver);
	    bgThread.start();
	}
	
	try {
	    run();
	} catch (Exception e) {
	    state = Status.TERMINATING;
	    throw e;
	}

	state = Status.TERMINATING;
	if(config().updateInterval > 0) {
	    try {
		bgThread.join();
	    } catch (InterruptedException e) {
		throw new RuntimeException("unexpected error while waiting for background thread to die: " + e.getMessage());
	    }
	}
	if (finishCb != null)
	    finishCb.accept(this);

	state = Status.IDLE;
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
	if (n == 0) {
	    state = Status.IDLE;
	    throw new IllegalStateException("board size was not set");
	}
	if (!isIdle()) {
	    state = Status.IDLE;
	    throw new IllegalStateException("solver is already started");
	}
	if (getProgress() == 1.0f) {
	    state = Status.IDLE;
	    throw new IllegalStateException("solver is already done, nothing to do here");
	}
    }

    private Thread backgroundThread(boolean updateConsumer, boolean autoSaver) {
	return new Thread(() -> {
	    // for autoSaver
	    String filePath = config().autoSavePath;
	    filePath = filePath.replaceAll("\\{n\\}", "" + n);
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
		    else if (progress >= tmpProgress + config().autoSavePercentageStep) {
			try {
			    save(filePath);
			} catch (IllegalArgumentException | IOException e) {
			    System.err.println("error in autosaver thread: " + e.getMessage());
			}
			tmpProgress = progress;
		    }
		}
		
		try {
		    Thread.sleep(config().updateInterval);
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
		    if (config().autoDeleteEnabled) {
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

    public final void save(String filepath) throws IOException, IllegalArgumentException {
	if(isStoring)
	    return;
	isStoring = true;
	save_(filepath);
	isStoring = false;
    }

    public final synchronized void load(File file)
	    throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
	load_(file.getAbsolutePath());
	config().autoSavePath = file.getAbsolutePath();
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
	this.n = n;
	return (T) this;
    }
    
    // getters
    public final int getN() {
	return n;
    }
    
    public final boolean isIdle() {
	return state == Status.IDLE;
    }

    public final boolean isInitializing() {
	return state == Status.INITIALIZING;
    }

    public final boolean isRunning() {
	return state == Status.RUNNING;
    }

    public final boolean isTerminating() {
	return state == Status.TERMINATING;
    }
    
    private static enum Status {
	IDLE, INITIALIZING, RUNNING, TERMINATING
    }
    
    public interface OnUpdateConsumer {
	void accept(Solver self, float progress, long solutions, long duration);
    }
}
