package de.nqueensfaf;

/**
 * Defines the interface for a solver capable of finding the solution
 * count to the N-Queens problem. Provides methods for configuring and
 * controlling the solver and for retrieving its results.
 * 
 * @see AbstractSolver
 */
public interface Solver {
    
    /**
     * Sets the value of {@code n}.
     * 
     * @param n represents both the dimensions of the chess board and
     * the number of queens to be positioned.
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
     * Starts the solving process for the configured value of {@code n}.
     * This method should block until the solving process is finished or 
     * canceled.
     * <p>
     * It is advised to continuously update the values returned by
     * {@link #getSolutions()}, {@link #getDuration()} and
     * {@link #getProgress()} (when applicable) during the runtime of this
     * method so that the user can track the solver's progress.
     * Additionally, the value returned by {@link #getStatus()} should be
     * continuously updated according to {@link SolverStatus}.
     * 
     * @see #setN(int)
     * @see #getSolutions()
     */
    void solve();
    
    /**
     * Returns the (current) total count of solutions found by the solver
     * for the configured value of {@code n}.
     * 
     * @return the (current) total number of found solutions.
     * 
     * @see #solve()
     */
    long getSolutions();
    
    /**
     * Returns the (current) total duration of the solving process in
     * milliseconds.
     * 
     * @return the (current) total duration of the solving process in
     * milliseconds.
     * 
     * @see #solve()
     */
    long getDuration();

    /**
     * Returns the (current) progress of the solving algorithm in percent.
     * <p>
     * The default implementation returns {@code 0f} if {@link #getStatus()} returns
     * anything other than {@link SolverStatus#FINISHED}. Otherwise it returns
     * {@code 1f}.
     * 
     * @return the (current) progress of the solving algorithm in percent.
     * 
     * @see #solve()
     * @see SolverStatus
     */
    default float getProgress() {
	return getStatus() == SolverStatus.FINISHED ? 1f : 0f;
    }
    
    /**
     * Returns the current status of the solver as an enum value of
     * {@link SolverStatus}.
     * 
     * @return the current status of the solver as an enum value of
     * {@link SolverStatus}.
     * 
     * @see #solve()
     */
    SolverStatus getStatus();
    
}
