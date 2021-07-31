package de.nqueensfaf;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.nqueensfaf.util.OnProgressUpdateCallback;
import de.nqueensfaf.util.OnTimeUpdateCallback;

public abstract class Solver {
	
	protected int N;
	protected OnTimeUpdateCallback  onTimeUpdateCallback;
	protected OnProgressUpdateCallback  onProgressUpdateCallback;
	protected long 
		timeUpdateDelay = NQueensFAF.DEFAULT_TIME_UPDATE_DELAY,
		progressUpdateDelay = NQueensFAF.DEFAULT_PROGRESS_UPDATE_DELAY;
	private ThreadPoolExecutor ucExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
	private ArrayList<Runnable> onStart = new ArrayList<Runnable>(), onEnd = new ArrayList<Runnable>();
	
	private int state = NQueensFAF.IDLE;
	private Thread t = new Thread(() -> solve());
	
	// abstract methods
	protected abstract void run();
	public abstract void save();
	public abstract void restore();
	public abstract void reset();
	public abstract long getDuration();
	public abstract float getProgress();
	public abstract long getSolutions();
	
	public void solve() {
		if(N == 0) {
			throw new IllegalStateException("Board size was not set");
		}
		if(!isIdle()) {
			throw new IllegalStateException("Solver is already started");
		}
		state = NQueensFAF.INITIALIZING;
		onStartCaller();
		startUpdateCallerThreads();
		
		state = NQueensFAF.RUNNING;
		run();
		
		state = NQueensFAF.TERMINATING;
		onEndCaller();
		try {
			ucExecutor.awaitTermination(timeUpdateDelay > progressUpdateDelay ? timeUpdateDelay : progressUpdateDelay, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// nothing
		}
		// call callback methods with the final solver values
		onTimeUpdateCallback.onTimeUpdate(getDuration());
		onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
		
		state = NQueensFAF.IDLE;
	}
	
	public void solveAsync() {
		t.start();
	}
	
	public void waitFor() throws InterruptedException {
		if(t == null) {
			throw new IllegalStateException("solveAsync was not called");
		} 
		if(!t.isAlive()) {
			throw new IllegalStateException("Solver is not running");
		}
		t.join();
	}
	
	private void startUpdateCallerThreads() {
		if(onTimeUpdateCallback != null) {
			ucExecutor.submit(() -> {
				while(!isRunning());
				long tmpTime = 0;
				while(isRunning()) {
					if(getDuration() != tmpTime) {
						onTimeUpdateCallback.onTimeUpdate(getDuration());
						tmpTime = getDuration();
					}
					if(!isRunning())
						break;
					try {
						Thread.sleep(timeUpdateDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
		if(onProgressUpdateCallback != null) {
			ucExecutor.submit(() -> {
				while(!isRunning());
				float tmpProgress = 0;
				long tmpSolutions = 0;
				while(isRunning()) {
					if(getProgress() != tmpProgress || getSolutions() != tmpSolutions) {
						onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
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
			});
		}
	}

	public void addOnStartCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("onStartCallback must not be null");
		}
		onStart.add(r);
	}
	
	public void addOnEndCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("onEndCallback must not be null");
		}
		onEnd.add(r);
	}
	
	private void onStartCaller() {
		for(int i = onStart.size()-1; i >= 0; i--) {
			onStart.get(i).run();
		}
	}
	
	private void onEndCaller() {
		for(int i = onEnd.size()-1; i >= 0; i--) {
			onEnd.get(i).run();
		}
	}
	
	// Getters and Setters
	public int getN() {
		return N;
	}
	
	public void setN(int n) {
		if(!isIdle()) {
			throw new IllegalStateException("Cannot set board size while solving");
		}
		if(n < 0) {
			throw new IllegalStateException("Board size must be a number >= 0");
		}
		N = n;
	}
	
	public OnTimeUpdateCallback getOnTimeUpdateCallback() {
		return onTimeUpdateCallback;
	}
	
	public void setOnTimeUpdateCallback(OnTimeUpdateCallback onTimeUpdateCallback) {
		this.onTimeUpdateCallback = onTimeUpdateCallback;
	}
	
	public OnProgressUpdateCallback getOnProgressUpdateCallback() {
		return onProgressUpdateCallback;
	}
	
	public void setOnProgressUpdateCallback(OnProgressUpdateCallback onProgressUpdateCallback) {
		this.onProgressUpdateCallback = onProgressUpdateCallback;
	}
	
	public long getTimeUpdateDelay() {
		return timeUpdateDelay;
	}
	
	public void setTimeUpdateDelay(long timeUpdateDelay) {
		if(timeUpdateDelay < 0) {
			throw new IllegalArgumentException("timeUpdateDelay must be a number >= 0");
		}
		this.timeUpdateDelay = timeUpdateDelay;
	}
	
	public long getProgressUpdateDelay() {
		return progressUpdateDelay;
	}
	
	public void setProgressUpdateDelay(long progressUpdateDelay) {
		if(progressUpdateDelay < 0) {
			throw new IllegalArgumentException("progressUpdateDelay must be a number >= 0");
		}
		this.progressUpdateDelay = progressUpdateDelay;
	}

	public boolean isIdle() {
		return state == NQueensFAF.IDLE;
	}

	public boolean isInitializing() {
		return state == NQueensFAF.INITIALIZING;
	}
	
	public boolean isRunning() {
		return state == NQueensFAF.RUNNING;
	}

	public boolean isTerminating() {
		return state == NQueensFAF.TERMINATING;
	}
}
