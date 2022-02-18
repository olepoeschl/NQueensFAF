package de.nqueensfaf;

/**
 * <p>
 * Provides constants and default values used by this library and some configs.
 * </p>
 * 
 * @author olepo
 */
public final class NQueensFAF {
	
	private NQueensFAF() {}
	
	/**
	 * The {@link Solver} is idle.
	 */
	static final int IDLE = 1;
	/**
	 * The {@link Solver} calls the initialization callbacks and starts the threads for continous time and progress updates.
	 */
	static final int INITIALIZING = 2;
	/**
	 * The {@link Solver} is executing the solving mechanism.
	 */
	static final int RUNNING = 3;
	/**
	 * The {@link Solver} calls the termination callbacks and terminates all threads.
	 */
	static final int TERMINATING = 4;
	
	/**
	 * Default value for {@link Solver#timeUpdateDelay} in milliseconds.
	 */
	static final int DEFAULT_TIME_UPDATE_DELAY = 100;
	/**
	 * Default value for {@link Solver#progressUpdateDelay} in milliseconds.
	 */
	static final int DEFAULT_PROGRESS_UPDATE_DELAY = 128;
	
	/**
	 * if true, mutes the warning about the existence of an OpenCL-capable device and proceeds anyways.
	 */
	private static boolean ignoreOpenCLCheck = false;
	/**
	 * Sets {@link #ignoreOpenCLCheck}
	 * @param ignore {@link #ignoreOpenCLCheck}
	 */
	public static void setIgnoreOpenCLCheck(boolean ignore) {
		ignoreOpenCLCheck = ignore;
	}
	/**
	 * Gets {@link #ignoreOpenCLCheck}
	 */
	public static boolean getIgnoreOpenCLCheck() {
		return ignoreOpenCLCheck;
	}
}
