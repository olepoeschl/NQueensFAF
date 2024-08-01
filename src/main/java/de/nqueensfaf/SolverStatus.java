package de.nqueensfaf;

public enum SolverStatus {
	NOT_INITIALIZED, READY, STARTING, RUNNING, TERMINATING, FINISHED, CANCELED;
    
    public boolean isBefore(SolverStatus status) {
	return ordinal() < status.ordinal();
    }
    
    public boolean isAfter(SolverStatus status) {
	return ordinal() > status.ordinal();
    }
}
