package de.nqueensfaf;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Config {
    
    public long updateInterval;
    public boolean autoSaveEnabled;
    public boolean autoDeleteEnabled;
    public float autoSavePercentageStep;
    public String autoSavePath;

    public Config() {
	// default values
	updateInterval = 128;
	autoSaveEnabled = false;
	autoDeleteEnabled = false;
	autoSavePercentageStep = 10;
	autoSavePath = "nqueensfaf{N}.dat";
    }

    public void validate() {
	if (updateInterval < 0)
	    throw new IllegalArgumentException("invalid value for updateInterval: only numbers >0 or 0 (no updates) are allowed");

	if (autoSaveEnabled) {
	    if (autoSavePercentageStep <= 0 || autoSavePercentageStep >= 100)
		throw new IllegalArgumentException(
			"invalid value for autoSavePercentageStep: only numbers >0 and <100 are allowed");
	    if (autoSavePath != null && autoSavePath.length() > 0) {
		File file = new File(autoSavePath);
		try {
		    if (!file.exists()) {
			// try creating the file. if it works, the path is valid
			file.createNewFile();
			file.delete();
		    }
		} catch (Exception e) {
		    // if something goes wrong, the path is invalid
		    throw new IllegalArgumentException("invalid value for autoSavePath: " + e.getMessage());
		}
	    } else {
		throw new IllegalArgumentException(
			"invalid value for autoSavePath: must not be null or empty when autoSaveEnabled is true");
	    }
	} else { // auto save is disabled
	    if (autoDeleteEnabled)
		throw new IllegalArgumentException(
			"invalid value for autoDeleteEnabled: must not be true when autoSaveEnabled is true");
	}
    }

    public void from(Config config) {
	config.validate();
	updateInterval = config.updateInterval;
	autoSaveEnabled = config.autoSaveEnabled;
	autoDeleteEnabled = config.autoDeleteEnabled;
	autoSavePercentageStep = config.autoSavePercentageStep;
	autoSavePath = config.autoSavePath;
    }

    public <T extends Config> void from(File file) throws StreamReadException, DatabindException, IOException,
	    IllegalArgumentException, IllegalAccessException {
	ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ;
	@SuppressWarnings("unchecked")
	T config = (T) mapper.readValue(file, this.getClass());
	config.validate();
	for (var field : getClass().getFields()) {
	    field.set(this, field.get(config));
	}
    }

    public final void writeTo(File file) throws StreamWriteException, DatabindException, IOException {
	validate();
	ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
	out.writeValue(file, this);
    }
}
