package de.nqueensfaf.cli;

import java.util.Arrays;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GPUSolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "gpu")
public class GPUCommand implements Runnable {
    
    @ParentCommand
    private BaseCommand cli;

    public GPUCommand() {}

    @Option(names = { "-p", "--preset-queens" }, required = false, description = "how many queens should be placed as starting positions")
    int presetQueens;

    @Option(names = { "-g", "--gpus" }, required = false, split = ",", converter = GPUConverter.class, description = "GPUs that should be used and their workgroup sizes")
    GPU[] gpus;

    @Option(names = { "-l", "--list-gpus" }, required = false, description = "print a list of all available GPUs")
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

    public class GPU {
	
	public GPU() {
	    super();
	}
	
	static final String workgroupSizeKey = "wg";

	String nameContains;
	int workgroupSize;
    }

    public class GPUConverter implements ITypeConverter<GPU> {
	
	public GPUConverter() {
	    super();
	}
	
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
