package de.nqueensfaf;

/**
 * 
 * @author olepo
 * 
 * <p>
 * Provides all constants used by this library.
 * </p>
 */
public class NQueensFAF {
	
	private NQueensFAF() {}
	
	/**
	 * integer constant
	 */
	static final int
	// Solver states
		IDLE = 1,
		INITIALIZING = 2,
		RUNNING = 3,
		TERMINATING = 4,
	// default values for Solver variables
		DEFAULT_TIME_UPDATE_DELAY = 128,
		DEFAULT_PROGRESS_UPDATE_DELAY = 128;
}
