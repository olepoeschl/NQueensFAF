package de.nqueensfaf.demo.gui;

public class AppConfig {

    private int n;
    private int updateInterval;
    private int autoSaveInterval;
    
    public AppConfig(int n, int updateInterval, int autoSaveInterval) {
	this.n = n;
	this.updateInterval = updateInterval;
	this.autoSaveInterval = autoSaveInterval;
    }
    
    public void updateFrom(AppConfig appConfig) {
	setN(appConfig.n);
	setUpdateInterval(appConfig.updateInterval);
	setAutoSaveInterval(appConfig.autoSaveInterval);
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public void setAutoSaveInterval(int autoSaveInterval) {
        this.autoSaveInterval = autoSaveInterval;
    }
}
