package de.nqueensfaf;

import java.util.ArrayList;

import de.nqueensfaf.util.OnProgressUpdateCallback;
import de.nqueensfaf.util.OnTimeUpdateCallback;

public abstract class Solver {
	
	protected int N;
	protected OnTimeUpdateCallback  onTimeUpdateCallback;
	protected OnProgressUpdateCallback  onProgressUpdateCallback;
	protected long timeUpdateDelay, progressUpdateDelay;
	private ArrayList<Runnable> onStart = new ArrayList<Runnable>(), onEnd = new ArrayList<Runnable>();
	
	protected boolean running = false;
	private Thread t = new Thread(() -> run());
	private Thread asyncWaiter;
	
	// abstract methods
	protected abstract void run();
	public abstract void save();
	public abstract void restore();
	public abstract long getDuration();
	public abstract float getProgress();
	public abstract long getSolutions();
	
	public void solve() {
		onStart.add(() -> {
			running = true;
		});
		onEnd.add(() -> {
			running = false;
		});
		onStartCaller();
		startUpdateCallerThreads();
		run();
		// call callback methods with the final sovler values
		onTimeUpdateCallback.onTimeUpdate(getDuration());
		onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
		onEndCaller();
	}
	public void solveAsync() {
		onStart.add(() -> {
			running = true;
		});
		onEnd.add(() -> {
			running = false;
		});
		onStartCaller();
		startUpdateCallerThreads();
		t.start();
		asyncWaiter = new Thread(() -> {
			while(t.isAlive()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// call callback methods with the final sovler values
			onTimeUpdateCallback.onTimeUpdate(getDuration());
			onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
			onEndCaller();
		});
		asyncWaiter.start();
	}
	public void waitFor() throws InterruptedException {
		if(t == null) {
			throw new IllegalStateException("solveAsync was not called");
		}
		t.join();
		if(asyncWaiter == null)
			return;
		asyncWaiter.join();
	}
	
	private void startUpdateCallerThreads() {
		if(onTimeUpdateCallback != null) {
			new Thread(() -> {
				long tmpTime = 0;
				while(running) {
					if(getDuration() != tmpTime) {
						onTimeUpdateCallback.onTimeUpdate(getDuration());
						tmpTime = getDuration();
					}
					try {
						Thread.sleep(timeUpdateDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				onTimeUpdateCallback.onTimeUpdate(getDuration());
			}).start();
		}
		if(onProgressUpdateCallback != null) {
			new Thread(() -> {
				float tmpProgress = 0;
				long tmpSolutions = 0;
				while(running) {
					if(getProgress() != tmpProgress || getSolutions() != tmpSolutions) {
						onProgressUpdateCallback.onProgressUpdate(getProgress(), getSolutions());
						tmpProgress = getProgress();
						tmpSolutions = getSolutions();
					}
					try {
						Thread.sleep(progressUpdateDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
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
		N = n;
	}
	
	public OnTimeUpdateCallback getOnTimeUpdateCallback() {
		return onTimeUpdateCallback;
	}
	public void setOnTimeUpdateCallback(OnTimeUpdateCallback onTimeUpdateCallback) {
		if(onTimeUpdateCallback == null) {
			throw new IllegalStateException("onTimeUpdateCallback must not be null");
		}
		this.onTimeUpdateCallback = onTimeUpdateCallback;
	}
	
	public OnProgressUpdateCallback getOnProgressUpdateCallback() {
		return onProgressUpdateCallback;
	}
	public void setOnProgressUpdateCallback(OnProgressUpdateCallback onProgressUpdateCallback) {
		if(onProgressUpdateCallback == null) {
			throw new IllegalStateException("onProgressUpdateCallback must not be null");
		}
		this.onProgressUpdateCallback = onProgressUpdateCallback;
	}
	
	public long getTimeUpdateDelay() {
		return timeUpdateDelay;
	}
	public void setTimeUpdateDelay(long timeUpdateDelay) {
		this.timeUpdateDelay = timeUpdateDelay;
	}
	
	public long getProgressUpdateDelay() {
		return progressUpdateDelay;
	}
	public void setProgressUpdateDelay(long progressUpdateDelay) {
		this.progressUpdateDelay = progressUpdateDelay;
	}


	public void addOnStartCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("OnStartCallback must not be null");
		}
		onStart.add(r);
	}
	public void addOnEndCallback(Runnable r) {
		if(r == null) {
			throw new IllegalArgumentException("OnEndCallback must not be null");
		}
		onEnd.add(r);
	}
	
	public boolean isRunning() {
		return running;
	}
}
