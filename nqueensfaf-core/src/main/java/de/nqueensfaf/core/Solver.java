package de.nqueensfaf.core;

import java.io.IOException;

/**
 * Defines the interface for a solver capable of finding the solution count to
 * the N-Queens problem. Provides methods for configuring and controlling the
 * solver and for retrieving its results.
 * 
 * @see AbstractSolver
 */
public interface Solver {

    /**
     * Sets the value of {@code n}.
     * 
     * @param n represents both the dimensions of the chess board and the number of
     *          queens to be positioned.
     * 
     * @see #getN()
     */
    void setN(int n);

    /**
     * Returns the value of {@code n}.
     * 
     * @return the value of {@code n}.
     * 
     * @see #setN(int)
     */
    int getN();

    /**
     * Starts the solving process for the configured value of {@code n}. This method
     * should block until the solving process is finished or canceled.
     * <p>
     * It is advised to continuously update the values returned by
     * {@link #getSolutions()}, {@link #getDuration()} and {@link #getProgress()}
     * (when applicable) during the runtime of this method so that the user can
     * track the solver's progress. Additionally, the value returned by
     * {@link #getExecutionState()} should be continuously updated according to
     * {@link ExecutionState}.
     * 
     * @see #setN(int)
     * @see #getSolutions()
     */
    void solve();

    /**
     * Returns the (current) total count of solutions found by the solver for the
     * configured value of {@code n}.
     * 
     * @return the (current) total number of found solutions.
     * 
     * @see #solve()
     */
    long getSolutions();

    /**
     * Returns the (current) total duration of the solving process in milliseconds.
     * 
     * @return the (current) total duration of the solving process in milliseconds.
     * 
     * @see #solve()
     */
    long getDuration();

    /**
     * Returns the (current) progress of the solving algorithm in percent.
     * <p>
     * The default implementation returns {@code 0f} if {@link #getExecutionState()}
     * returns anything other than {@link ExecutionState#FINISHED}. Otherwise
     * it returns {@code 1f}.
     * 
     * @return the (current) progress of the solving algorithm in percent.
     * 
     * @see #solve()
     * @see ExecutionState
     */
    default float getProgress() {
	return getExecutionState() == ExecutionState.FINISHED ? 1f : 0f;
    }

    /**
     * Returns the current execution state of the solver.
     * 
     * @return the current execution state of the solver.
     * 
     * @see ExecutionState
     */
    ExecutionState getExecutionState();

    /**
     * Saves the current state of a {@link Solver} into a file under the path
     * {@code path}.
     * 
     * @param path the path the save file should be written to.
     * 
     * @see #load(String)
     */
    default void save(String path) throws IOException {
	throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Loads and restores a saved solver state, reading from the file under the path
     * {@code path}.
     * 
     * @param path the path to the file containing a saved solver state.
     * 
     * @see #save(String)
     */
    default void load(String path) throws IOException {
	throw new UnsupportedOperationException("Not implemented");
    }
}
