package de.nqueensfaf.demo.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

class GeneralSolverConfig {

    private int n;
    private int updateInterval;

    private final PropertyChangeSupport propChange = new PropertyChangeSupport(this);

    GeneralSolverConfig(int n, int updateInterval) {
	setN(n);
	setUpdateInterval(updateInterval);
    }

    void setN(int n) {
	int oldVal = this.n;
	this.n = n;
	propChange.firePropertyChange("n", oldVal, n);
    }

    void setUpdateInterval(int updateInterval) {
	int oldVal = this.updateInterval;
	this.updateInterval = updateInterval;
	propChange.firePropertyChange("updateInterval", oldVal, n);
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
	propChange.addPropertyChangeListener(listener);
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
	propChange.removePropertyChangeListener(listener);
    }
}
