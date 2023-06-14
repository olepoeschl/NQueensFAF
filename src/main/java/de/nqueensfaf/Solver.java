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
    protected long updateInterval = Config.getDefaultConfig().getProgressUpdateDelay();
    
    private int state = IDLE;
    private Config config = Config.getDefaultConfig();
    private OnUpdateConsumer onUpdateConsumer;
    private Consumer<Solver> initCb, finishCb;
    private int solutionsSmallN = 0;
    private ExecutorService executor;
    private boolean isStoring = false;
    
    private boolean autoDeleteEnabled = Config.getDefaultConfig().isAutoDeleteEnabled();
    private boolean autoSaveEnabled = Config.getDefaultConfig().isAutoSaveEnabled();
    private int autoSavePercentageStep = Config.getDefaultConfig().getAutoSavePercentageStep();
    private String autoSaveFilePath = Config.getDefaultConfig().getAutoSaveFilePath();
    
    protected Solver() {
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
	executor = Executors.newFixedThreadPool(2);
    }

    public abstract long getDuration();
    public abstract float getProgress();
    public abstract long getSolutions();
    public abstract boolean isInjected();	// get rid of ?
    public abstract void reset();		// get rid of ?
    protected abstract void run();
    protected abstract void store_(String filepath) throws IOException;
    protected abstract void inject_(String filepath) throws IOException, ClassNotFoundException, ClassCastException;
    
    public final void solve() {
	preconditions();
	
	state = INITIALIZING;
	if (initCb != null)
	    initCb.accept(this);
	if (onUpdateConsumer != null)
	    executor.submit(consumeUpdates());
	if (autoSaveEnabled)
	    executor.submit(autoSaver());

	state = RUNNING;
	run();

	state = TERMINATING;
	executor.shutdown();
	try {
	    executor.awaitTermination(updateInterval*2, TimeUnit.MILLISECONDS);
	} catch (InterruptedException e) {
	    e.printStackTrace();
	    Thread.currentThread().interrupt();
	}
	if (finishCb != null)
	    finishCb.accept(this);

	state = IDLE;
    }

    public final void solveAsync() {
	new Thread(() -> solve()).start();
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
		    Thread.sleep(updateInterval);
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
		String filePath = autoSaveFilePath;
		filePath = filePath.replaceAll("\\{N\\}", "" + N);
		float progress = getProgress() * 100;
		int tmpProgress = (int) progress / autoSavePercentageStep * autoSavePercentageStep;
		while (isRunning()) {
		    progress = getProgress() * 100;
		    if (progress >= 100)
			break;
		    else if (progress >= tmpProgress + autoSavePercentageStep) {
			store(filePath);
			tmpProgress = (int) progress;
		    }
		    try {
			Thread.sleep(updateInterval);
		    } catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		    }
		}
		progress = getProgress() * 100;
		if (progress >= 100) {
		    if (autoDeleteEnabled) {
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

    public final void inject(String filepath)
	    throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
	inject_(filepath);
	autoSaveFilePath = filepath;
	solve();
    }

    public final void inject(File file)
	    throws IOException, ClassNotFoundException, ClassCastException, IllegalArgumentException {
	inject_(file.getAbsolutePath());
	autoSaveFilePath = file.getAbsolutePath();
	solve();
    }

    public <T extends Solver> T config(Consumer<Config> configConsumer) {
	configConsumer.accept(config);
	return (T) this;
    }
    
    public final <T extends Solver> T onInit(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("initializationCallback must not be null");
	}
	initCb = c;
	return (T) this;
    }

    public final <T extends Solver> T onFinish(Consumer<Solver> c) {
	if (c == null) {
	    throw new IllegalArgumentException("terminationCallback must not be null");
	}
	finishCb = c;
	return (T) this;
    }

    public final <T extends Solver> T onUpdate(OnUpdateConsumer onUpdateConsumer) {
	if (onUpdateConsumer == null) {
	    this.onUpdateConsumer = (self, progress, solutions, duration) -> {};
	} else {
	    this.onUpdateConsumer = onUpdateConsumer;
	}
	return (T) this;
    }

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

    // get rid of all those config setters and getters
    public final long getUpdateInterval() {
	return updateInterval;
    }

    public final void setUpdateInterval(long updateInterval) {
	if (updateInterval < 0) {
	    throw new IllegalArgumentException("updateInterval must be a number >= 0");
	} else if (updateInterval == 0) {
	    this.updateInterval = Config.getDefaultConfig().getTimeUpdateDelay();
	}
	this.updateInterval = updateInterval;
    }

    public final boolean isAutoSaveEnabled() {
	return autoSaveEnabled;
    }

    public final void setAutoSaveEnabled(boolean autoSaveEnabled) {
	this.autoSaveEnabled = autoSaveEnabled;
    }

    public final boolean isAutoDeleteEnabled() {
	return autoDeleteEnabled;
    }

    public final void setAutoDeleteEnabled(boolean autoDeleteEnabled) {
	this.autoDeleteEnabled = autoDeleteEnabled;
    }

    public final int getAutoSavePercentageStep() {
	return autoSavePercentageStep;
    }

    public final void setAutoSavePercentageStep(int autoSavePercentageStep) {
	if (autoSavePercentageStep <= 0 || autoSavePercentageStep >= 100) {
	    throw new IllegalArgumentException("progressUpdateDelay must be a number between 0 and 100");
	}
	this.autoSavePercentageStep = autoSavePercentageStep;
    }

    public final String getAutoSaveFilePath() {
	return autoSaveFilePath;
    }

    public final void setAutoSaveFilePath(String autoSaveFilePath) {
	this.autoSaveFilePath = autoSaveFilePath;
    }
    // -----
    
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
    
    interface OnUpdateConsumer {
	void accept(Solver self, float progress, long solutions, long duration);
    }
}
