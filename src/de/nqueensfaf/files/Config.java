package de.nqueensfaf.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Config {
	
	// all configurable fields and their default values
	
	// CPU or GPU ?
	@JsonProperty(required = true)
	private String type;
	// for CPU
	private int cpuThreadcount;
	// for GPU
	private DeviceConfig[] deviceConfigs;
	private int presetQueens;
	// general
	private long progressUpdateDelay;
	private boolean autoSaveEnabled, autoDeleteEnabled;
	private int autoSavePercentageStep;
	private String autosaveFilePath;
		
	public Config() {
		super();
	}
	
	public Config(String type, int cpuThreadcount, DeviceConfig[] deviceConfigs, int presetQueens,
			long progressUpdateDelay, boolean autoSaveEnabled, boolean autoDeleteEnabled, int autoSavePercentageStep,
			String autosaveFilePath) {
		this.type = type;
		this.cpuThreadcount = cpuThreadcount;
		this.deviceConfigs = deviceConfigs;
		this.presetQueens = presetQueens;
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
		c.setDeviceConfigs(new DeviceConfig(0, 64, 6, 1)); // -69 -> use default device
		c.setPresetQueens(6);
		c.setProgressUpdateDelay(128);
		c.setAutoSaveEnabled(false);
		c.setAutoDeleteEnabled(false);
		c.setAutoSavePercentageStep(10);
		c.setAutosaveFilePath("n{N}.faf");
		return c;
	}
	
	public static Config read(File configFile) throws StreamReadException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Config config = mapper.readValue(configFile, Config.class);
		config.validate();
		return config;
	}
	
	public void write(File configFile) throws StreamWriteException, DatabindException, IOException {
		validate();
		ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
		out.writeValue(configFile, this);
	}
	
	public void validate() {
		if(!(type.toLowerCase().equals("cpu") || type.toLowerCase().equals("gpu")))
			type = getDefaultConfig().getType();
		
		if(cpuThreadcount <= 0 || cpuThreadcount > Runtime.getRuntime().availableProcessors())
			cpuThreadcount = getDefaultConfig().getCPUThreadcount();
		
		if(deviceConfigs.length == 0)
			deviceConfigs = getDefaultConfig().getDeviceConfigs();
		// check for invalid values and remove each invalid value that is found from the array
		ArrayList<DeviceConfig> deviceConfigsTmp = new ArrayList<DeviceConfig>();
		for(DeviceConfig deviceConfig : deviceConfigs) {
			if((deviceConfig.getIdx() < 0 && deviceConfig.getIdx() != -420) || deviceConfig.getWorkgroupSize() <= 0 || deviceConfig.getPresetQueens() < 4)
				continue;
			if(deviceConfigsTmp.stream().anyMatch(dvcCfg -> deviceConfig.getIdx() == dvcCfg.getIdx())) // check for duplicates
				continue;
			deviceConfigsTmp.add(deviceConfig);
		}
		deviceConfigs = new DeviceConfig[deviceConfigsTmp.size()];
		for(int i = 0; i < deviceConfigsTmp.size(); i++) {
			deviceConfigs[i] = deviceConfigsTmp.get(i);
		}
		
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
	
	public DeviceConfig[] getDeviceConfigs() {
		return deviceConfigs;
	}
	public void setDeviceConfigs(DeviceConfig... deviceConfigs) {
		this.deviceConfigs = deviceConfigs;
	}
	
	public int getPresetQueens() {
		return presetQueens;
	}
	public void setPresetQueens(int presetQueens) {
		this.presetQueens = presetQueens;
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
