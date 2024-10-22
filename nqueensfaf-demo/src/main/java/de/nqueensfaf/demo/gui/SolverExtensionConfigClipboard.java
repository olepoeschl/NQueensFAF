package de.nqueensfaf.demo.gui;

import java.util.Map;

class SolverExtensionConfigClipboard {

    private static final SolverExtensionConfigClipboard instance = new SolverExtensionConfigClipboard();
    
    private Map<String, Object> configMap;
    
    private SolverExtensionConfigClipboard() {
    }

    static SolverExtensionConfigClipboard getInstance() {
	return instance;
    }
    
    synchronized void set(Map<String, Object> configMap) {
	this.configMap = configMap;
    }
    
    Map<String, Object> get(){
	return configMap;
    }
}
