package de.nqueensfaf.demo.gui.extension;

import java.util.Map;

public class SolverExtensionConfigClipboard {

    private static final SolverExtensionConfigClipboard instance = new SolverExtensionConfigClipboard();
    
    private Map<String, Object> configMap;
    
    private SolverExtensionConfigClipboard() {
    }

    public static SolverExtensionConfigClipboard getInstance() {
	return instance;
    }
    
    public synchronized void set(Map<String, Object> configMap) {
	this.configMap = configMap;
    }
    
    public Map<String, Object> get(){
	return configMap;
    }
}
