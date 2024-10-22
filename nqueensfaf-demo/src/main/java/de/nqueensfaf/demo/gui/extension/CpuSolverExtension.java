package de.nqueensfaf.demo.gui.extension;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.impl.CpuSolver;

public class CpuSolverExtension implements SolverExtension {

    private final CpuSolver solver = new CpuSolver();

    private PropertyGroupConfigUi configUi;
    
    public CpuSolverExtension() {
	createConfigUi();
    }
    
    private void createConfigUi() {
	var propConfigUi = new PropertyGroupConfigUi();
	propConfigUi.addIntProperty("threads", "Threads", 1, 
		Runtime.getRuntime().availableProcessors() * 2, 1, 1);
	propConfigUi.getProperty("threads").addChangeListener(e -> solver.setThreadCount((int) e.getNewValue()));
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 5, 1);
	propConfigUi.getProperty("prequeens").addChangeListener(e -> solver.setPresetQueens((int) e.getNewValue()));
	propConfigUi.fillRemainingVerticalSpace();
	
	configUi = propConfigUi;
    }

    @Override
    public AbstractSolver getSolver() {
	return solver;
    }

    @Override
    public String getName() {
	return "CPU";
    }
    
    @Override
    public String getCurrentRecordCategory() {
	if(solver.getThreadCount() == 1)
	    return "CPU: Single-Core";
	return "CPU: Multithreaded";
    }

    @Override
    public JComponent getConfigUi() {
	return configUi;
    }
    
    @Override
    public void setConfig(Map<String, Object> configMap) {
	for(var key : configMap.keySet()) {
	    switch(key) {
	    case "threads":
	    case "prequeens":
		configUi.getProperty(key).setValue(configMap.get(key));
	    }
	}
    }
    
    @Override
    public Map<String, Object> getConfig() {
	var configMap = new HashMap<String, Object>();
	configMap.put("threads", configUi.getProperty("threads").getValue());
	configMap.put("prequeens", configUi.getProperty("prequeens").getValue());
	return configMap;
    }
}
