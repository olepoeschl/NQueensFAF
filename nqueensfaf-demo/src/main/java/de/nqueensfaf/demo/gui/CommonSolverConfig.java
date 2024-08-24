package de.nqueensfaf.demo.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

class CommonSolverConfig {

    private int n;
    private int updateInterval;
    private float autoSaveIntervalPercentage;

    private final PropertyChangeSupport propChange = new PropertyChangeSupport(this);

    CommonSolverConfig(int n, int updateInterval, float autoSaveIntervalPercentage) {
	setN(n);
	setUpdateInterval(updateInterval);
	setAutoSaveIntervalPercentage(autoSaveIntervalPercentage);
    }

    void setN(int n) {
	int oldVal = this.n;
	this.n = n;
	propChange.firePropertyChange("n", oldVal, n);
    }
    
    int getN() {
	return n;
    }

    void setUpdateInterval(int updateInterval) {
	int oldVal = this.updateInterval;
	this.updateInterval = updateInterval;
	propChange.firePropertyChange("updateInterval", oldVal, n);
    }
    
    int getUpdateInterval() {
	return updateInterval;
    }

    void setAutoSaveIntervalPercentage(float autoSaveIntervalPercentage) {
	float oldVal = this.autoSaveIntervalPercentage;
	this.autoSaveIntervalPercentage = autoSaveIntervalPercentage;
	propChange.firePropertyChange("autoSaveIntervalPercentage", oldVal, n);
    }
    
    float getAutoSaveIntervalPercentage() {
	return autoSaveIntervalPercentage;
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
	propChange.addPropertyChangeListener(listener);
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
	propChange.removePropertyChangeListener(listener);
    }
}
