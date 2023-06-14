package de.nqueensfaf.util;

public interface OnUpdateConsumer {
    void accept(float progress, long solutions, long duration);
}
