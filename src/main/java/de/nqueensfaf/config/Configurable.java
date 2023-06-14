package de.nqueensfaf.config;

public interface Configurable {
    public <T extends Config> T getDefaultConfig();
    public void validate();
}
