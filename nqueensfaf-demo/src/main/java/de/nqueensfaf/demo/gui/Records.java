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
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.EventListenerList;

class Records {
    
    static final String DEFAULT_PATH = "records";
    
    private final EventListenerList listeners = new EventListenerList();
    
    private Map<Integer, Map<String, Long>> records;
    
    public Records() {	  
	records = new HashMap<Integer, Map<String, Long>>();
    }
    
    @SuppressWarnings("unchecked")
    void open(String path) throws FileNotFoundException, IOException, ClassNotFoundException {
	try (var in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(path))))) {
	    records = (HashMap<Integer, Map<String, Long>>) in.readObject();
	}
    }
    
    public void save(String path) throws FileNotFoundException, IOException {
	if(records.size() == 0)
	    return;
	try (var out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(path))))) {
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
	    records.put(n, new HashMap<String, Long>());
	records.get(n).put(device, duration);
	
	for(var l : listeners.getListeners(RecordListener.class))
	    l.newRecord(n);
    }
    
    // Map: deviceName -> duration
    Map<String, Long> getRecordsByN(int n) {
	return records.get(n);
    }
    
    void addRecordListener(RecordListener l) {
	listeners.add(RecordListener.class, l);
    }
    
    void removeRecordListener(RecordListener l) {
	listeners.remove(RecordListener.class, l);
    }
    
    static interface RecordListener extends EventListener {
	void newRecord(int n);
    }
}
