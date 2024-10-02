package de.nqueensfaf.demo.gui;

public class Settings {

    private int updateInterval;
    
    public Settings(int updateInterval) {
	this.updateInterval = updateInterval;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }
}
