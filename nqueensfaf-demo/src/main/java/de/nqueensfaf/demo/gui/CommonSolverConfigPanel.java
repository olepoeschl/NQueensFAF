package de.nqueensfaf.demo.gui;

import javax.swing.JPanel;

class CommonSolverConfigPanel extends JPanel {

    class CommonSolverConfig {
	private int n;
	private int updateInterval;
	private float autoSaveIntervalPercentage;

	CommonSolverConfig(int n, int updateInterval, float autoSaveIntervalPercentage) {
	    setN(n);
	    setUpdateInterval(updateInterval);
	    setAutoSaveIntervalPercentage(autoSaveIntervalPercentage);
	}

	void setN(int n) {
	    int oldVal = this.n;
	    this.n = n;
	    firePropertyChange("n", oldVal, n);
	}

	int getN() {
	    return n;
	}

	void setUpdateInterval(int updateInterval) {
	    int oldVal = this.updateInterval;
	    this.updateInterval = updateInterval;
	    firePropertyChange("updateInterval", oldVal, n);
	}

	int getUpdateInterval() {
	    return updateInterval;
	}

	void setAutoSaveIntervalPercentage(float autoSaveIntervalPercentage) {
	    float oldVal = this.autoSaveIntervalPercentage;
	    this.autoSaveIntervalPercentage = autoSaveIntervalPercentage;
	    firePropertyChange("autoSaveIntervalPercentage", oldVal, n);
	}

	float getAutoSaveIntervalPercentage() {
	    return autoSaveIntervalPercentage;
	}
    }

    CommonSolverConfigPanel() {
	init();
    }

    private void init() {

    }

    private final CommonSolverConfig model = new CommonSolverConfig(16, 200, 0);
}
