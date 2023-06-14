package de.nqueensfaf.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Config {

    // all configurable fields and their default values

    // which Solver implementation?
    private String type;
    // for CPU
    private int cpuThreadcount;
    // for GPU
    private DeviceConfig[] gpuDeviceConfigs;
    private int gpuPresetQueens;
    // general
    private long timeUpdateDelay, progressUpdateDelay;
    private boolean autoSaveEnabled;
    private boolean autoDeleteEnabled;
    private int autoSavePercentageStep;
    private String autoSaveFilePath;

    public Config() {
	super();
    }

    @JsonCreator
    public Config(@JsonProperty(value = "type", required = true) String type,
	    @JsonProperty(value = "cpuThreadcount") int cpuThreadcount,
	    @JsonProperty(value = "gpuDeviceConfigs") DeviceConfig[] gpuDeviceConfigs,
	    @JsonProperty(value = "gpuPresetQueens") int gpuPresetQueens,
	    @JsonProperty(value = "timeUpdateDelay") long timeUpdateDelay,
	    @JsonProperty(value = "progressUpdateDelay") long progressUpdateDelay,
	    @JsonProperty(value = "autoSaveEnabled") boolean autoSaveEnabled,
	    @JsonProperty(value = "autoDeleteEnabled") boolean autoDeleteEnabled,
	    @JsonProperty(value = "autoSavePercentageStep") int autoSavePercentageStep,
	    @JsonProperty(value = "autoSaveFilePath") String autoSaveFilePath) {
	this.type = type;
	this.cpuThreadcount = cpuThreadcount;
	this.gpuDeviceConfigs = gpuDeviceConfigs;
	this.gpuPresetQueens = gpuPresetQueens;
	this.timeUpdateDelay = timeUpdateDelay;
	this.progressUpdateDelay = progressUpdateDelay;
	this.autoSaveEnabled = autoSaveEnabled;
	this.autoDeleteEnabled = autoDeleteEnabled;
	this.autoSavePercentageStep = autoSavePercentageStep;
	this.autoSaveFilePath = autoSaveFilePath;
    }

    public static Config getDefaultConfig() {
	final Config c = new Config();
	c.setType("CPU");
	c.setCPUThreadcount(1);
	c.setGPUDeviceConfigs(DeviceConfig.getDefaultDeviceConfig());
	c.setGPUPresetQueens(6);
	c.setTimeUpdateDelay(128);
	c.setProgressUpdateDelay(128);
	c.setAutoSaveEnabled(false);
	c.setAutoDeleteEnabled(false);
	c.setAutoSavePercentageStep(10);
	c.setAutoSaveFilePath("n{N}.faf");
	return c;
    }

    public static Config from(File configFile) throws StreamReadException, DatabindException, IOException {
	ObjectMapper mapper = new ObjectMapper();
	Config config = mapper.readValue(configFile, Config.class);
	config.validate();
	return config;
    }

    public void writeTo(File configFile) throws StreamWriteException, DatabindException, IOException {
	validate();
	ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
	out.writeValue(configFile, this);
    }

    public void validate() {
	if (!(type.toLowerCase().equals("cpu") || type.toLowerCase().equals("gpu")))
	    type = getDefaultConfig().getType();

	if (cpuThreadcount <= 0 || cpuThreadcount > Runtime.getRuntime().availableProcessors())
	    cpuThreadcount = getDefaultConfig().getCPUThreadcount();

	if (gpuDeviceConfigs == null || gpuDeviceConfigs.length == 0)
	    gpuDeviceConfigs = getDefaultConfig().getGPUDeviceConfigs();
	else {
	    // check for invalid values and remove each invalid value that is found
	    ArrayList<DeviceConfig> gpuDeviceConfigsTmp = new ArrayList<DeviceConfig>();
	    for (var deviceConfig : gpuDeviceConfigs) {
		if (gpuDeviceConfigsTmp.stream().anyMatch(dvcCfg -> deviceConfig.getIndex() == dvcCfg.getIndex())) // check
														   // for
														   // duplicates
		    continue;
		deviceConfig.fillEmptyFields();
		if (deviceConfig.isValid())
		    gpuDeviceConfigsTmp.add(deviceConfig);
	    }
	    gpuDeviceConfigs = new DeviceConfig[gpuDeviceConfigsTmp.size()];
	    for (int i = 0; i < gpuDeviceConfigsTmp.size(); i++) {
		gpuDeviceConfigs[i] = gpuDeviceConfigsTmp.get(i);
	    }
	}

	if (gpuPresetQueens < 4)
	    gpuPresetQueens = getDefaultConfig().getGPUPresetQueens();

	if (progressUpdateDelay <= 0)
	    progressUpdateDelay = getDefaultConfig().getProgressUpdateDelay();

	if (autoSavePercentageStep <= 0 || autoSavePercentageStep > 100)
	    autoSavePercentageStep = getDefaultConfig().getAutoSavePercentageStep();

	if (autoSaveFilePath != null) {
	    File file = new File(autoSaveFilePath);
	    try {
		if (!file.exists()) {
		    // try creating the file. if it works, the path is valid.
		    file.createNewFile();
		    file.delete();
		}
	    } catch (Exception e) {
		// if something goes wrong, the path is invalid.
		autoSaveFilePath = getDefaultConfig().getAutoSaveFilePath();
	    }
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

    public DeviceConfig[] getGPUDeviceConfigs() {
	return gpuDeviceConfigs;
    }

    public void setGPUDeviceConfigs(DeviceConfig... gpuDeviceConfigs) {
	this.gpuDeviceConfigs = gpuDeviceConfigs;
    }

    public int getGPUPresetQueens() {
	return gpuPresetQueens;
    }

    public void setGPUPresetQueens(int gpuPresetQueens) {
	this.gpuPresetQueens = gpuPresetQueens;
    }

    public long getTimeUpdateDelay() {
	return timeUpdateDelay;
    }

    public void setTimeUpdateDelay(long timeUpdateDelay) {
	this.timeUpdateDelay = timeUpdateDelay;
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

    public String getAutoSaveFilePath() {
	return autoSaveFilePath;
    }

    public void setAutoSaveFilePath(String autosaveFilePath) {
	this.autoSaveFilePath = autosaveFilePath;
    }
}
