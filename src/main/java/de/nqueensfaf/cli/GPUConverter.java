package de.nqueensfaf.cli;

import de.nqueensfaf.cli.GPUCommand.GPURequest;
import picocli.CommandLine.ITypeConverter;

public class GPUConverter implements ITypeConverter<GPURequest> {
    @Override
    public GPURequest convert(String input) throws Exception {
	String[] props = input.split(":");
	GPURequest gpu = new GPURequest();
	
	gpu.nameContains = props[0];
	
	for(int i = 1; i < props.length; i++) {
	    if(props[i].length() < 3)
		throw new IllegalArgumentException("invalid gpu property: '" + props[i] + "'");
	    
	    var key = props[i].substring(0, 2);
	    var value = props[i].substring(2);

	    switch(key) {
	    case GPURequest.workgroupSizeKey:
		gpu.workgroupSize = Integer.parseInt(value);
		break;
	    case GPURequest.benchmarkScoreKey:
		gpu.benchmarkScore = Integer.parseInt(value);
	    default:
		throw new IllegalArgumentException("invalid gpu property: '" + props[i] + "'");
	    }
	}

	return gpu;
    }
}
