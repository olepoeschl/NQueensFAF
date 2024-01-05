package de.nqueensfaf;

public interface Configurable {
    public <T extends Config> T config();
}
