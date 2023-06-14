package de.nqueensfaf.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.nqueensfaf.impl.GPUSolver.GPUSolverConfig;

public class Config implements Configurable {

    public long updateInterval;
    public boolean autoSaveEnabled;
    public boolean autoDeleteEnabled;
    public int autoSavePercentageStep;
    public String autoSavePath;
    
    public Config() {
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Config getDefaultConfig() {
	Config c = new Config();
	c.updateInterval = 128;
	c.autoSaveEnabled = false;
	c.autoDeleteEnabled = false;
	c.autoSavePercentageStep = 10;
	c.autoSavePath = "nqueensfaf{N}.dat";
	return c;
    }
    
    @Override
    public void validate() {
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
    }

    public <T extends Config> void from(File file) throws StreamReadException, DatabindException, IOException, IllegalArgumentException, IllegalAccessException {
	    ObjectMapper mapper = new ObjectMapper();
	    @SuppressWarnings("unchecked")
	    T config = (T) mapper.readValue(file, this.getClass());
	    config.validate();
	    for(var field : getClass().getFields()) {
		field.set(this, field.get(config));
	    }
	}
    
    public final void writeTo(File file) throws StreamWriteException, DatabindException, IOException {
	validate();
	ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
	out.writeValue(file, this);
    }
}
