package de.nqueensfaf.demo.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class Records {

    private Map<Integer, Map<String, Long>> records;
    
    public Records() {
	records = new HashMap<Integer, Map<String, Long>>();
    }
    
    @SuppressWarnings("unchecked")
    public void open(File file) throws FileNotFoundException, IOException, ClassNotFoundException {
	try (var in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
	    records = (HashMap<Integer, Map<String, Long>>) in.readObject();
	}
    }
    
    public void save(File file) throws FileNotFoundException, IOException {
	try (var out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
	    out.writeObject(records);
	    out.flush();
	}
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
