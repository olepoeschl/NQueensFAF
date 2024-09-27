package de.nqueensfaf.demo.gui;

public final class Event {

    public static final int SNAPSHOT_RESTORED = 1;
    
    private final int id;
    private final Object data;
    
    public Event(int id) {
	this(id, null);
    }
    
    public Event(int id, Object data) {
	this.id = 0;
	this.data = data;
    }
    
    public int getId() {
	return id;
    }
    
    public Object getData() {
	return data;
    }
}
