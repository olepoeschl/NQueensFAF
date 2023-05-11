package de.nqueensfaf.files;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceConfig {
	
	@JsonProperty(required = true)
	private long id;
	@JsonProperty(required = true)
	private int workgroupSize;
	@JsonProperty(required = true)
	private int presetQueens;
	private int weight;
	
	public DeviceConfig() {
		super();
	}

	public DeviceConfig(long id, int workgroupSize, int presetQueens, int weight) {
		super();
		this.id = id;
		this.workgroupSize = workgroupSize;
		this.presetQueens = presetQueens;
		this.weight = weight;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
			return id == dvcCfg.id 
					&& workgroupSize == dvcCfg.workgroupSize 
					&& presetQueens == dvcCfg.presetQueens 
					&& weight == dvcCfg.weight;
		}
		return false;
	}
}
