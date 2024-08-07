package de.nqueensfaf;

/**
 * Defines the possible execution states of a {@link Solver}.
 */
public enum SolverExecutionState {

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
    public boolean isBefore(SolverExecutionState state) {
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
    public boolean isAfter(SolverExecutionState state) {
	return ordinal() > state.ordinal();
    }

    public boolean isBusy() {
	return isAfter(READY) && isBefore(FINISHED);
    }

    public boolean isIdle() {
	return !isBusy();
    }
}
