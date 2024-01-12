package de.nqueensfaf.cli;

import de.nqueensfaf.cli.GPUCommand.GPU;
import picocli.CommandLine.ITypeConverter;

public class GPUConverter implements ITypeConverter<GPU> {
    @Override
    public GPU convert(String input) throws Exception {
	String[] props = input.split(input);

	GPU gpu = new GPU();

	for(var prop : props) {
	    var key = prop.substring(0, 2);
	    var value = prop.substring(2);

	    switch(key) {
	    case GPU.workgroupSizeKey:
		gpu.workgroupSize = Integer.parseInt(value);
		break;
	    default: // GPU name has no key
		gpu.nameContains = prop;
	    }
	}

	return gpu;
    }
}
