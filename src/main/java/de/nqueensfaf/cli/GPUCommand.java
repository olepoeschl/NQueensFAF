package de.nqueensfaf.cli;

import java.util.Arrays;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GPUSolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "gpu", description = "use one or more GPUs")
public class GPUCommand implements Runnable {
    
    @ParentCommand
    BaseCommand cli;

    public GPUCommand() {}

    @Option(names = { "-p", "--preset-queens" }, required = false, description = "How many queens should be placed for a start positions")
    int presetQueens;

    @Option(names = { "-g", "--gpus" }, required = false, split = ",", converter = GPUConverter.class, description = "GPUs that should be used and their workgroup sizes")
    GPU[] gpus;

    @Option(names = { "-l", "--list-gpus" }, required = false, description = "Print a list of all available GPUs")
    boolean printGpuList;
    
    @Override
    public void run() {
	if(printGpuList) {
	    var gpus = Arrays.asList(new GPUSolver().getAvailableGpus());
	    System.out.println(
		    AsciiTable.getTable(AsciiTable.BASIC_ASCII, gpus,
			    Arrays.asList(
				    new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.vendor()),
				    new Column().header("Name").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.name()))));
	    return;
	}

	System.out.println("gpu solver");
	
	// TODO: create and execute solver
    }

    static class GPU {
	
	public GPU() {}
	
	static final String workgroupSizeKey = "wg";

	String nameContains;
	int workgroupSize;
    }

    
}
