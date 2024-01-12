package de.nqueensfaf.cli;

import java.util.Arrays;
import java.util.List;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GPUSolver;
import de.nqueensfaf.impl.GPUSolver.GPUInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "gpu", description = "use one or more GPUs", mixinStandardHelpOptions = true)
public class GPUCommand implements Runnable {
    
    @ParentCommand
    BaseCommand base;

    @Option(names = { "-p", "--preset-queens" }, required = false, defaultValue = "6", description = "How many queens should be placed for a start positions")
    int presetQueens;

    @Option(names = { "-g", "--gpus" }, required = false, split = ",", converter = GPUConverter.class, description = "GPUs that should be used and their workgroup sizes")
    GPU[] gpu;

    @Option(names = { "-l", "--list-gpus" }, required = false, description = "Print a list of all available GPUs")
    boolean printGpuList;

    private GPUSolver solver;
    
    public GPUCommand() {}
    
    @Override
    public void run() {
	if(printGpuList) {
	    var gpus = new GPUSolver().getAvailableGpus();
	    System.out.println(
		    AsciiTable.getTable(AsciiTable.BASIC_ASCII, gpus,
			    Arrays.asList(
				    new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.vendor()),
				    new Column().header("Name").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.name()))));
	    return;
	}

	solver = new GPUSolver();
	base.applySolverConfig(solver);
	
	
	if(presetQueens != 0)
	    solver.setPresetQueens(presetQueens);
	
	List<GPUInfo> availableGpus = solver.getAvailableGpus();
	if(gpu != null) {
	    // TODO: benchmarks for every gpu ?
	    for(var g : gpu) {
		long id = availableGpus.stream().filter(gi -> gi.name().contains(g.nameContains)).findFirst().get().id();
		solver.gpuSelection().add(id, 1, g.workgroupSize);
	    }
	} else {
	    solver.gpuSelection().add(availableGpus.get(0).id());
	}
	
	solver.solve();
	System.out.println("(" + base.getUniqueSolutions(solver) + " unique solutions)");
    }

    static class GPU {
	
	public GPU() {}
	
	static final String workgroupSizeKey = "wg";

	String nameContains;
	int workgroupSize;
    }
}
