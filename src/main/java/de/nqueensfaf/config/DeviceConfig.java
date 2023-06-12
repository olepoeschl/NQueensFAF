package de.nqueensfaf.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceConfig {

    public static final DeviceConfig ALL_DEVICES = new DeviceConfig(-420, 0, 0, 0, 0);

    private int index;
    private int workgroupSize = 0;
    private int presetQueens = 0;
    private int weight = 0;
    private int maxGlobalWorkSize = 0;

    public DeviceConfig() {
        super();
    }

    @JsonCreator
    public DeviceConfig(@JsonProperty(value = "index", required = true) int index,
            @JsonProperty(value = "workgroupSize") int workgroupSize,
            @JsonProperty(value = "presetQueens") int presetQueens,
            @JsonProperty(value = "weight") int weight,
            @JsonProperty(value = "maxGlobalWorkSize") int maxGlobalWorkSize) {
        super();
        this.index = index;
        this.workgroupSize = workgroupSize;
        this.presetQueens = presetQueens;
        this.weight = weight;
        this.maxGlobalWorkSize = maxGlobalWorkSize;
    }

    public static DeviceConfig getDefaultDeviceConfig() {
        final DeviceConfig dc = new DeviceConfig();
        dc.setIndex(0);
        dc.setWorkgroupSize(64);
        dc.setPresetQueens(6);
        dc.setWeight(1);
        dc.setMaxGlobalWorkSize(1_000_000_000);
        return dc;
    }

    public void fillEmptyFields() {
        if (workgroupSize == 0)
            workgroupSize = getDefaultDeviceConfig().workgroupSize;
        if (presetQueens == 0)
            presetQueens = getDefaultDeviceConfig().presetQueens;
        if (weight == 0)
            weight = getDefaultDeviceConfig().weight;
        if (maxGlobalWorkSize == 0)
            maxGlobalWorkSize = getDefaultDeviceConfig().maxGlobalWorkSize;
    }

    public boolean isValid() {
        return index >= 0 && workgroupSize > 0 && presetQueens >= 4
                && maxGlobalWorkSize >= workgroupSize;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
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

    public int getMaxGlobalWorkSize() {
        return maxGlobalWorkSize;
    }

    public void setMaxGlobalWorkSize(int maxGlobalWorkSize) {
        this.maxGlobalWorkSize = maxGlobalWorkSize;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeviceConfig) {
            DeviceConfig dvcCfg = (DeviceConfig) obj;
            return index == dvcCfg.index && workgroupSize == dvcCfg.workgroupSize
                    && presetQueens == dvcCfg.presetQueens && weight == dvcCfg.weight
                    && maxGlobalWorkSize == dvcCfg.maxGlobalWorkSize;
        }
        return false;
    }
}
