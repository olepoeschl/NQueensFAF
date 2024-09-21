package de.nqueensfaf.demo.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class Records {

    private Map<Integer, Map<String, Long>> records;
    
    public Records(String path) {
	records = new HashMap<Integer, Map<String, Long>>();
	// TODO: read data from path into records
    }
    
    boolean isNewRecord(long duration, int n, String device) {
	if(records.get(n) == null)
	    return true;
	if(records.get(n).get(device) == null)
	    return true;
	return records.get(n).get(device) > duration;
    }
    
    void putRecord(long duration, int n, String device) {
	if(records.get(n) == null)
	    records.put(n, new TreeMap<String, Long>());
	records.get(n).put(device, duration);
    }
    
    // Map: deviceName -> duration
    Map<String, Long> getRecordsByN(int n) {
	return records.get(n);
    }
}
