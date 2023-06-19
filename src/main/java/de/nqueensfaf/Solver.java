package de.nqueensfaf;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import de.nqueensfaf.config.Config;

public abstract class Solver {

    private static final int IDLE = 1;
    private static final int INITIALIZING = 2;
    private static final int RUNNING = 3;
    private static final int TERMINATING = 4;
    
    protected int N;
    
    private int state = IDLE;
    private Thread asyncSolverThread;
    private OnUpdateConsumer onUpdateConsumer;
    private Consumer<Solver> initCb, finishCb;
    private int solutionsSmallN = 0;
    private ExecutorService executor;
    private boolean isStoring = false;
    
    protected Solver() {
	executor = Executors.newFixedThreadPool(2);
	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	    while (isStoring) {
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		    Thread.currentThread().interrupt();
		}
	    }
	}));
    }

    public abstract <T extends Config> T getConfig();
    public abstract long getDuration();
    public abstract float getProgress();
    public abstract long getSolutions();
    public abstract void reset();		// get rid of ?
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
	run();
	// start consumer threads
	if (onUpdateConsumer != null)
	    executor.submit(consumeUpdates());
	if (getConfig().autoSaveEnabled)
	    executor.submit(autoSaver());

	state = TERMINATING;
	executor.shutdown();
	try {
	    executor.awaitTermination(getConfig().updateInterval*2, TimeUnit.MILLISECONDS);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	    Thread.currentThread().interrupt();
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
	    e.printStackTrace();
	    Thread.currentThread().interrupt();
	}
	return (T) this;
    }
    
    private void preconditions() {
	if (N == 0) {
	    state = IDLE;
	    throw new IllegalStateException("Board size was not set");
	}
	if (!isIdle()) {
	    state = IDLE;
	    throw new IllegalStateException("Solver is already started");
	}
	if (getProgress() == 1.0f) {
	    state = IDLE;
	    throw new IllegalStateException("Solver is already done, nothing to do here");
	}
    }

    private Runnable consumeUpdates() {
	return () -> {
	    while (isRunning()) {
		onUpdateConsumer.accept(this, getProgress(), getSolutions(), getDuration());
		if (!isRunning())
		    break;
		try {
		    Thread.sleep(getConfig().updateInterval);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		    Thread.currentThread().interrupt();
		}
	    }
	    onUpdateConsumer.accept(this, getProgress(), getSolutions(), getDuration());
	};
    }

    private Runnable autoSaver() {
	return () -> {
	    try {
		String filePath = getConfig().autoSavePath;
		filePath = filePath.replaceAll("\\{N\\}", "" + N);
		float progress = getProgress() * 100;
		int tmpProgress = (int) progress / getConfig().autoSavePercentageStep * getConfig().autoSavePercentageStep;
		while (isRunning()) {
		    progress = getProgress() * 100;
		    if (progress >= 100)
			break;
		    else if (progress >= tmpProgress + getConfig().autoSavePercentageStep) {
			store(filePath);
			tmpProgress = (int) progress;
		    }
		    try {
			Thread.sleep(getConfig().updateInterval);
		    } catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		    }
		}
		progress = getProgress() * 100;
		if (progress >= 100) {
		    if (getConfig().autoDeleteEnabled) {
			try {
			    new File(filePath).delete();
			} catch (SecurityException e) {
			    e.printStackTrace();
			}
		    } else {
			store(filePath); // store one last time
		    }
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	};
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

    public final synchronized void store(String filepath) throws IOException, IllegalArgumentException {
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
	    throw new IllegalArgumentException("initializationCallback must not be null");
	}
	initCb = c;
	return (T) this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Solver> T onFinish(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("terminationCallback must not be null");
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
	    throw new IllegalStateException("Cannot set board size while solving");
	}
	if (n <= 0 || n > 31) {
	    throw new IllegalArgumentException("Board size must be a number between 0 and 32 (not inclusive)");
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
