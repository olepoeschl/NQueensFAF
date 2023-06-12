package de.nqueensfaf.compute;

public class DeviceInfo {
    private int index;
    private String vendor;
    private String name;

    public DeviceInfo(int index, String vendor, String name) {
        super();
        this.index = index;
        this.vendor = vendor;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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
