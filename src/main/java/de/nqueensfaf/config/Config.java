package de.nqueensfaf.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public abstract class Config {

    public long updateInterval;
    public boolean autoSaveEnabled;
    public boolean autoDeleteEnabled;
    public int autoSavePercentageStep;
    public String autoSavePath;

    protected Config() {
    }

    protected abstract void validate_();
    
    public final void validate() {
	if (updateInterval <= 0)
	    throw new IllegalArgumentException("invalid value for update interval: only numbers >0 are allowed");

	if (autoSavePercentageStep <= 0 || autoSavePercentageStep >= 100)
	    throw new IllegalArgumentException("invalid value for update interval: only numbers >0 and <100 are allowed");

	if (autoSavePath != null) {
	    File file = new File(autoSavePath);
	    try {
		if (!file.exists()) {
		    // try creating the file. if it works, the path is valid
		    file.createNewFile();
		    file.delete();
		}
	    } catch (Exception e) {
		// if something goes wrong, the path is invalid
		throw new IllegalArgumentException("invalid value for auto save path: " + e.getMessage());
	    }
	}
	validate_();
    }

    public abstract void from(File file) throws StreamReadException, DatabindException, IOException;
    
    public final Config getDefaultConfig() {
	return new ConfigImpl();
    }
    
    protected final void copyParentFields(Config config) {
	updateInterval = config.updateInterval;
	autoSaveEnabled = config.autoSaveEnabled;
	autoDeleteEnabled = config.autoDeleteEnabled;
	autoSavePercentageStep = config.autoSavePercentageStep;
	autoSavePath = config.autoSavePath;
    }
    
    public final void writeTo(File file) throws StreamWriteException, DatabindException, IOException {
	validate();
	ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
	out.writeValue(file, this);
    }
    
    public class ConfigImpl extends Config {
	public ConfigImpl() {
	    updateInterval = 128;
	    autoSaveEnabled = false;
	    autoDeleteEnabled = false;
	    autoSavePercentageStep = 10;
	    autoSavePath = "nqueensfaf{N}.dat";
	}
	
	@Override
	protected void validate_() {
	}
	@Override
	public void from(File file) throws StreamReadException, DatabindException, IOException {
	}
    }
}
