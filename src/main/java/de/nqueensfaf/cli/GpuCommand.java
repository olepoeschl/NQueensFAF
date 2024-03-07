package de.nqueensfaf.cli;

import java.util.Arrays;
import java.util.List;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "gpu", description = "use one or more GPUs", mixinStandardHelpOptions = true)
public class GpuCommand implements Runnable {
    
    @ParentCommand
    BaseCommand base;

    @Option(names = { "-p", "--preset-queens" }, required = false, defaultValue = "6", description = "How many queens should be placed for a start positions")
    int presetQueens;

    @Option(names = { "-g", "--gpus" }, required = false, split = ",", converter = GpuConverter.class, 
	    description = "GPUs that should be used in the format of <string_contained_in_name>[:<attr><val>[,:<attr><val>]]"
	    	+ "\n<attr> can be one of the following: wg, bm, al"
	    	+ "\n<val> is the value that should be assigned to the attribute, if the attribute expects one")
    GPURequest[] gpu;

    @Option(names = { "-l", "--list-gpus" }, required = false, description = "Print a list of all available GPUs")
    boolean printGpuList;

    private GpuSolver solver;
    
    public GpuCommand() {}
    
    @Override
    public void run() {
	if(printGpuList) {
	    var gpus = new GpuSolver().getAvailableGpus();
	    System.out.println(
		    AsciiTable.getTable(AsciiTable.BASIC_ASCII, gpus,
			    Arrays.asList(
				    new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().vendor()),
				    new Column().header("Name").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().name()))));
	    return;
	}

	solver = new GpuSolver();
	base.applySolverConfig(solver);
	
	if(presetQueens != 0)
	    solver.setPresetQueens(presetQueens);
	
	List<Gpu> availableGpus = solver.getAvailableGpus();
	if(gpu != null) {
	    for(var requestedGpu : gpu) {
		if(requestedGpu.useAllMatchingGpus) {
		    var matchingGpus = availableGpus.stream().filter(gi -> gi.getInfo().name().toLowerCase().contains(requestedGpu.nameContains)).toList();
		    for(var matchingGpu : matchingGpus) {
			matchingGpu.getConfig().setBenchmark(requestedGpu.benchmarkScore);
			matchingGpu.getConfig().setWorkgroupSize(requestedGpu.workgroupSize);
			solver.gpuSelection().add(matchingGpu);
		    }
		} else {
		    var matchingGpu = availableGpus.stream().filter(gi -> gi.getInfo().name().toLowerCase().contains(requestedGpu.nameContains)).findFirst().get();
		    matchingGpu.getConfig().setBenchmark(requestedGpu.benchmarkScore);
		    matchingGpu.getConfig().setWorkgroupSize(requestedGpu.workgroupSize);
		    solver.gpuSelection().add(matchingGpu);
		}
	    }
	} else {
	    solver.gpuSelection().choose(availableGpus.get(0));
	}
	
	solver.solve();
	System.out.println("(" + base.getUniqueSolutions(solver) + " unique solutions)");
    }

    static class GPURequest {
	
	static final String workgroupSizeKey = "wg", 
		benchmarkScoreKey = "bm",
		useAllMatchingGpusKey = "al";

	String nameContains;
	int workgroupSize;
	int benchmarkScore;
	boolean useAllMatchingGpus;
	
	public GPURequest() {}
    }
}
