package de.nqueensfaf.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

public class SolverCommand {
    
    @Command(name = "cpu")
    static class CPUCommand {

	@Option(names = { "-p", "--preset-queens" }, required = false, description = "how many queens should be placed as starting positions")
	int presetQueens;

	@Option(names = { "-t", "--threads" }, required = false, description = "how many CPU threads should be used")
	int threads;

    }

    @Command(name = "gpu")
    static class GPUCommand {
	
	@Option(names = { "-p", "--preset-queens" }, required = false, description = "how many queens should be placed as starting positions")
	int presetQueens;

	@Option(names = { "-g", "--gpus" }, required = false, split = ",", converter = GPUConverter.class, description = "GPUs that should be used and their workgroup sizes")
	GPU[] gpus;
    }
    
    static class GPU {
	
	static final String workgroupSizeKey = "wg";
	
	String nameContains;
	int workgroupSize;
    }
    
    class GPUConverter implements ITypeConverter<GPU> {
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
}
