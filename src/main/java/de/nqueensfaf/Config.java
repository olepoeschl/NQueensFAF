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

    public Config() {
	// default values
	updateInterval = 128;
    }

    public void validate() {
	if (updateInterval < 0)
	    throw new IllegalArgumentException("invalid value for updateInterval: must be a number >=0 (0 means disabling updates)");
    }

    public void load(Config config) {
	config.validate();
	updateInterval = config.updateInterval;
    }

    public <T extends Config> void load(File file) throws StreamReadException, DatabindException, IOException,
	    IllegalArgumentException, IllegalAccessException {
	ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ;
	@SuppressWarnings("unchecked")
	T config = (T) mapper.readValue(file, this.getClass());
	config.validate();
	for (var field : getClass().getFields()) {
	    field.set(this, field.get(config));
	}
    }

    public final void save(File file) throws StreamWriteException, DatabindException, IOException {
	validate();
	ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
	out.writeValue(file, this);
    }
}
