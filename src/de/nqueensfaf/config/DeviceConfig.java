package de.nqueensfaf.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceConfig {
	
	@JsonProperty(value = "index", required = true)
	private int index;
	@JsonProperty(value = "workgroupSize", required = true)
	private int workgroupSize;
	@JsonProperty(value = "presetQueens", required = true)
	private int presetQueens;
	@JsonProperty(value = "weight")
	private int weight;
	
	public DeviceConfig() {
		super();
	}

	public DeviceConfig(int index, int workgroupSize, int presetQueens, int weight) {
		super();
		this.index = index;
		this.workgroupSize = workgroupSize;
		this.presetQueens = presetQueens;
		this.weight = weight;
	}

	public int getIndex() {
		return index;
	}

	public void setIdx(int index) {
		this.index = index;
	}

	public int getWorkgroupSize() {
		return workgroupSize;
	}

	public void setWorkgroupSize(int workgroupSize) {
		this.workgroupSize = workgroupSize;
	}

	public int getPresetQueens() {
		return presetQueens;
	}

	public void setPresetQueens(int presetQueens) {
		this.presetQueens = presetQueens;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DeviceConfig) {
			DeviceConfig dvcCfg = (DeviceConfig) obj;
			return index == dvcCfg.index 
					&& workgroupSize == dvcCfg.workgroupSize 
					&& presetQueens == dvcCfg.presetQueens 
					&& weight == dvcCfg.weight;
		}
		return false;
	}
}
