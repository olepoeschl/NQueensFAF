package de.nqueensfaf.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceConfig {

    public static final DeviceConfig ALL_DEVICES = new DeviceConfig(-420, 0, 0, 0);

    public int index;
    public int workgroupSize ;
    public int weight;
    public int maxGlobalWorkSize;

    public DeviceConfig() {
    }

    @JsonCreator
    public DeviceConfig(@JsonProperty(value = "index", required = true) int index,
	    @JsonProperty(value = "workgroupSize") int workgroupSize, @JsonProperty(value = "weight") int weight,
	    @JsonProperty(value = "maxGlobalWorkSize") int maxGlobalWorkSize) {
	super();
	this.index = index;
	this.workgroupSize = workgroupSize;
	this.weight = weight;
	this.maxGlobalWorkSize = maxGlobalWorkSize;
	fillEmptyFields();
    }

    private void fillEmptyFields() {
	if (workgroupSize == 0)
	    workgroupSize = 64;
	if (maxGlobalWorkSize == 0)
	    maxGlobalWorkSize = 1_000_000_000;
    }

    public void validate() {
	if(index < 0)
	    throw new IllegalArgumentException("invalid value for index: only numbers >=0 are allowed");
	if(workgroupSize <= 0)
	    throw new IllegalArgumentException("invalid value for workgroup size: only numbers >0 are allowed");
	if(maxGlobalWorkSize < workgroupSize)
	    throw new IllegalArgumentException("invalid value for max global work size: only numbers >=[workgroup size] are allowed");
    }
    
    @Override
    public boolean equals(Object obj) {
	if (obj instanceof DeviceConfig) {
	    DeviceConfig dvcCfg = (DeviceConfig) obj;
	    return index == dvcCfg.index && workgroupSize == dvcCfg.workgroupSize
		    && weight == dvcCfg.weight && maxGlobalWorkSize == dvcCfg.maxGlobalWorkSize;
	}
	return false;
    }
}
