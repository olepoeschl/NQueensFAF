package de.nqueensfaf;

@SuppressWarnings("serial")
public class SolverException extends RuntimeException {

    public SolverException(String errMsg) {
	super(errMsg);
    }
    
    public SolverException(String errMsg, Throwable err) {
	super(errMsg, err);
    }
    
}
