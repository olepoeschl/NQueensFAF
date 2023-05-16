package de.nqueensfaf.compute;

public class DeviceInfo {
	private int idx;
	private String vendor;
	private String name;
	
	public DeviceInfo(int idx, String vendor, String name) {
		super();
		this.idx = idx;
		this.vendor = vendor;
		this.name = name;
	}

	public long getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
