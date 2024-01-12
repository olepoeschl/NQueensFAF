package de.nqueensfaf.impl;

public interface Stateful {
    public SolverState getState();
    public void setState(SolverState state);
}
