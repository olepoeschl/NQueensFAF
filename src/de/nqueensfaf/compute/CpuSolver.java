package de.nqueensfaf.compute;

import de.nqueensfaf.Solver;

public class CpuSolver extends Solver {

	private int threadcount = 0;
	private long start, end;
	
	@Override
	protected void run() {
		
	}

	@Override
	public void save() {

	}

	@Override
	public void restore() {

	}

	@Override
	public void reset() {

	}

	@Override
	public long getDuration() {
		return end - start;
	}

	@Override
	public float getProgress() {
		return 0;
	}

	@Override
	public long getSolutions() {
		return 0;
	}
	
	// getters and setters
	public void setThreadcount(int threadcount) {
		if(threadcount < 0 || threadcount > Runtime.getRuntime().availableProcessors()) {
			throw new IllegalArgumentException("threadcount must be a number between 0 and " + Runtime.getRuntime().availableProcessors() + " (=your CPU's number of logical cores)");
		}
		this.threadcount = threadcount;
	}
	
	public int getThreadcount() {
		return threadcount;
	}
}
