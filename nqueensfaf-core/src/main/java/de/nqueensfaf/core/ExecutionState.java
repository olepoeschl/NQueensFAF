package de.nqueensfaf.core;

/**
 * Defines the possible execution states of a {@link Solver}.
 */
public enum ExecutionState {

    /**
     * There are configurations that still have to be applied to the {@link Solver}
     * before it can be started.
     */
    NOT_INITIALIZED,

    /**
     * All nessessary configurations have been made and the {@link Solver} is ready
     * to be started.
     */
    READY,

    /**
     * The {@link Solver} was started, but is not executing the solving algorithm
     * yet. Optional.
     */
    STARTING,

    /**
     * The {@link Solver} is executing the solving algorithm.
     */
    RUNNING,

    /**
     * The solving algorithm has finished, but there are still tasks left to be
     * teared down (for example terminating threads). Optional.
     */
    TERMINATING,

    /**
     * The {@link Solver} has finished the computation.
     */
    FINISHED,

    /**
     * The {@link Solver} could not be successfully finish the computation.
     */
    CANCELED;

    /**
     * Checks if this execution state chronologically comes before {@code state}.
     * 
     * @param state the execution state to be compared to this execution state.
     * 
     * @return true if the ordinal of this execution state is lower than the ordinal
     *         of {@code state}, otherwise false.
     */
    public boolean isBefore(ExecutionState state) {
	return ordinal() < state.ordinal();
    }

    /**
     * Checks if this execution state chronologically comes after {@code state}.
     * 
     * @param state the execution state to be compared to this execution state.
     * 
     * @return true if the ordinal of this execution state is greater than the
     *         ordinal of {@code state}, otherwise false.
     */
    public boolean isAfter(ExecutionState state) {
	return ordinal() > state.ordinal();
    }
    
    /**
     * Checks if this execution state represents the state of a running (busy) solver.
     * Applies to any {@link Solver} instance that was started, but did not finish yet.
     * 
     * @return true if this execution state is {@link #STARTING}, {@link #RUNNING} or 
     * {@link #TERMINATING}, otherwise false.
     */
    public boolean isBusy() {
	return isAfter(READY) && isBefore(FINISHED);
    }

    /**
     * The opposite of {@link #isBusy()}.
     * <p>
     * Checks if this execution state represents the state of an idle solver.
     * Applies to any {@link Solver} instance that either was not started yet, that was 
     * already finished or that was canceled.
     * 
     * @return true if this execution state is {@link #NOT_INITIALIZED}, {@link #READY},
     * {@link #FINISHED} or {@link #CANCELED}, otherwise false.
     */
    public boolean isIdle() {
	return !isBusy();
    }
}
