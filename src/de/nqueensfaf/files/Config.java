package de.nqueensfaf.files;

public class Config {
	
	// all configurable field and their default values
	
	// CPU or GPU ?
	private String type;
	// for CPU
	private int cpuThreadcount = 1;
	// for GPU
	private int gpuDevice = 0, gpuWorkgroupSize = 64, gpuPresetQueens = 6;
	// general
	private long progressUpdateDelay = 128;
	private boolean autoSaveEnabled = false, autoDeleteEnabled = false;
	private int autoSavePercentageStep = 10;
	private String autosaveFilename = "n{N}.faf";
		
	public Config() {
		super();
	}
	
	public Config(String type, int cpuThreadcount, int gpuDevice, int gpuWorkgroupSize, int gpuPresetQueens,
			long progressUpdateDelay, boolean autoSaveEnabled, boolean autoDeleteEnabled, int autoSavePercentageStep,
			String autosaveFilename) {
		this.type = type;
		this.cpuThreadcount = cpuThreadcount;
		this.gpuDevice = gpuDevice;
		this.gpuWorkgroupSize = gpuWorkgroupSize;
		this.gpuPresetQueens = gpuPresetQueens;
		this.progressUpdateDelay = progressUpdateDelay;
		this.autoSaveEnabled = autoSaveEnabled;
		this.autoDeleteEnabled = autoDeleteEnabled;
		this.autoSavePercentageStep = autoSavePercentageStep;
		this.autosaveFilename = autosaveFilename;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public int getCpuThreadcount() {
		return cpuThreadcount;
	}
	public void setCpuThreadcount(int cpuThreadcount) {
		this.cpuThreadcount = cpuThreadcount;
	}
	
	public int getGpuDevice() {
		return gpuDevice;
	}
	public void setGpuDevice(int gpuDevice) {
		this.gpuDevice = gpuDevice;
	}
	
	public int getGpuWorkgroupSize() {
		return gpuWorkgroupSize;
	}
	public void setGpuWorkgroupSize(int gpuWorkgroupSize) {
		this.gpuWorkgroupSize = gpuWorkgroupSize;
	}
	
	public int getGpuPresetQueens() {
		return gpuPresetQueens;
	}
	public void setGpuPresetQueens(int gpuPresetQueens) {
		this.gpuPresetQueens = gpuPresetQueens;
	}
	
	public long getProgressUpdateDelay() {
		return progressUpdateDelay;
	}
	public void setProgressUpdateDelay(long progressUpdateDelay) {
		this.progressUpdateDelay = progressUpdateDelay;
	}
	
	public boolean isAutoSaveEnabled() {
		return autoSaveEnabled;
	}
	public void setAutoSaveEnabled(boolean autoSaveEnabled) {
		this.autoSaveEnabled = autoSaveEnabled;
	}
	
	public boolean isAutoDeleteEnabled() {
		return autoDeleteEnabled;
	}
	public void setAutoDeleteEnabled(boolean autoDeleteEnabled) {
		this.autoDeleteEnabled = autoDeleteEnabled;
	}
	
	public int getAutoSavePercentageStep() {
		return autoSavePercentageStep;
	}
	public void setAutoSavePercentageStep(int autoSavePercentageStep) {
		this.autoSavePercentageStep = autoSavePercentageStep;
	}
	
	public String getAutosaveFilename() {
		return autosaveFilename;
	}
	public void setAutosaveFilename(String autosaveFilename) {
		this.autosaveFilename = autosaveFilename;
	}
}
