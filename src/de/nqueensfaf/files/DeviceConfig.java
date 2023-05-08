package de.nqueensfaf.files;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceConfig {
	
	@JsonProperty(required = true)
	private long id;
	@JsonProperty(required = true)
	private int workgroupSize;
	@JsonProperty(required = true)
	private int presetQueens;
	
	public DeviceConfig() {
		super();
	}

	public DeviceConfig(long id, int workgroupSize, int presetQueens) {
		super();
		this.id = id;
		this.workgroupSize = workgroupSize;
		this.presetQueens = presetQueens;
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
}
