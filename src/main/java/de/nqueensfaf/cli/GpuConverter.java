package de.nqueensfaf.cli;

import de.nqueensfaf.cli.GpuCommand.GPURequest;
import picocli.CommandLine.ITypeConverter;

public class GpuConverter implements ITypeConverter<GPURequest> {
    @Override
    public GPURequest convert(String input) throws Exception {
	String[] props = input.split(":");
	GPURequest gpu = new GPURequest();
	
	gpu.nameContains = props[0];
	
	for(int i = 1; i < props.length; i++) {
	    if(props[i].length() < 2)
		throw new IllegalArgumentException("invalid gpu property: '" + props[i] + "'");
	    
	    var key = props[i].substring(0, 2);
	    var value = props[i].substring(2);
	    
	    switch(key) {
	    case GPURequest.workgroupSizeKey:
		gpu.workgroupSize = Integer.parseInt(value);
		break;
	    case GPURequest.benchmarkScoreKey:
		gpu.benchmarkScore = Integer.parseInt(value);
		break;
	    case GPURequest.useAllMatchingGpusKey:
		gpu.useAllMatchingGpus = true;
		break;
	    default:
		throw new IllegalArgumentException("invalid gpu property: '" + props[i] + "'");
	    }
	}

	return gpu;
    }
}
