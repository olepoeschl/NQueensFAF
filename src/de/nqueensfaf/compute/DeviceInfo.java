package de.nqueensfaf.compute;

public class DeviceInfo {
	private long id;
	private String vendor;
	private String name;
	
	public DeviceInfo(long id, String vendor, String name) {
		super();
		this.id = id;
		this.vendor = vendor;
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
