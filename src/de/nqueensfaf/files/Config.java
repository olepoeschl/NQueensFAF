package de.nqueensfaf.files;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Config {
	
	// all configurable field and their default values
	
	// CPU or GPU ?
	private String type;
	// for CPU
	private int cpuThreadcount;
	// for GPU
	private int gpuDevice, gpuWorkgroupSize, gpuPresetQueens;
	// general
	private long progressUpdateDelay;
	private boolean autoSaveEnabled, autoDeleteEnabled;
	private int autoSavePercentageStep;
	private String autosaveFilePath;
		
	public Config() {
		super();
	}
	
	public Config(String type, int cpuThreadcount, int gpuDevice, int gpuWorkgroupSize, int gpuPresetQueens,
			long progressUpdateDelay, boolean autoSaveEnabled, boolean autoDeleteEnabled, int autoSavePercentageStep,
			String autosaveFilePath) {
		this.type = type;
		this.cpuThreadcount = cpuThreadcount;
		this.gpuDevice = gpuDevice;
		this.gpuWorkgroupSize = gpuWorkgroupSize;
		this.gpuPresetQueens = gpuPresetQueens;
		this.progressUpdateDelay = progressUpdateDelay;
		this.autoSaveEnabled = autoSaveEnabled;
		this.autoDeleteEnabled = autoDeleteEnabled;
		this.autoSavePercentageStep = autoSavePercentageStep;
		this.autosaveFilePath = autosaveFilePath;
	}

	public static Config getDefaultConfig() {
		final Config c = new Config();
		c.setType("CPU");
		c.setCPUThreadcount(1);
		c.setGPUDevice(0);
		c.setGPUWorkgroupSize(64);
		c.setGPUPresetQueens(6);
		c.setProgressUpdateDelay(128);
		c.setAutoSaveEnabled(false);
		c.setAutoDeleteEnabled(false);
		c.setAutoSavePercentageStep(10);
		c.setAutosaveFilePath("n{N}.faf");
		return c;
	}
	
	public static Config fromFile(File configFile) throws StreamReadException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Config config = mapper.readValue(configFile, Config.class);
		config.validate();
		return config;
	}
	
	public void validate() {
		if(!(type.toLowerCase().equals("cpu") || type.toLowerCase().equals("gpu")))
			type = getDefaultConfig().getType();
		
		if(cpuThreadcount <= 0 || cpuThreadcount > Runtime.getRuntime().availableProcessors())
			cpuThreadcount = getDefaultConfig().getCPUThreadcount();
		
		if(gpuDevice < 0)
			gpuDevice = 0;
		
		if(gpuWorkgroupSize <= 0)
			gpuWorkgroupSize = getDefaultConfig().getGPUWorkgroupSize();
		
		if(gpuPresetQueens < 4)
			gpuPresetQueens = getDefaultConfig().getGPUPresetQueens();
		
		if(progressUpdateDelay <= 0)
			progressUpdateDelay = getDefaultConfig().getProgressUpdateDelay();
		
		if(autoSavePercentageStep <= 0 || autoSavePercentageStep > 100)
			autoSavePercentageStep = getDefaultConfig().getAutoSavePercentageStep();
		
		File file = new File(autosaveFilePath);
		try {
			if(!file.exists()) {
				// try creating the file. if it works, the path is valid.
				file.createNewFile();
				file.delete();
			}
		} catch(Exception e) {
			// if something goes wrong, the path is invalid.
			autosaveFilePath = getDefaultConfig().getAutosaveFilePath();
		}
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public int getCPUThreadcount() {
		return cpuThreadcount;
	}
	public void setCPUThreadcount(int cpuThreadcount) {
		this.cpuThreadcount = cpuThreadcount;
	}
	
	public int getGPUDevice() {
		return gpuDevice;
	}
	public void setGPUDevice(int gpuDevice) {
		this.gpuDevice = gpuDevice;
	}
	
	public int getGPUWorkgroupSize() {
		return gpuWorkgroupSize;
	}
	public void setGPUWorkgroupSize(int gpuWorkgroupSize) {
		this.gpuWorkgroupSize = gpuWorkgroupSize;
	}
	
	public int getGPUPresetQueens() {
		return gpuPresetQueens;
	}
	public void setGPUPresetQueens(int gpuPresetQueens) {
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
	
	public String getAutosaveFilePath() {
		return autosaveFilePath;
	}
	public void setAutosaveFilePath(String autosaveFilePath) {
		this.autosaveFilePath = autosaveFilePath;
	}
}
