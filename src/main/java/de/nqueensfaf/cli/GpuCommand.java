package de.nqueensfaf.cli;

import java.util.Arrays;
import java.util.Scanner;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GpuSolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "gpu", description = "use one or more GPUs", mixinStandardHelpOptions = true)
public class GpuCommand implements Runnable {
    
    @ParentCommand
    BaseCommand base;

    @Option(names = { "-p", "--preset-queens" }, required = false, defaultValue = "6", description = "How many queens should be placed for a start positions")
    int presetQueens;

    private GpuSolver solver;
    
    public GpuCommand() {}
    
    @Override
    public void run() {
	solver = new GpuSolver();
	base.applySolverConfig(solver);
	
	if(presetQueens != 0)
	    solver.setPresetQueens(presetQueens);

	var availableGpus = solver.getAvailableGpus();
	System.out.println(
		AsciiTable.getTable(AsciiTable.BASIC_ASCII, availableGpus,
			Arrays.asList(
				new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> Integer.toString(availableGpus.indexOf(gpu))),
				new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().vendor()),
				new Column().header("Name").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().name()),
				new Column().header("Benchmark").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.RIGHT).with(gpu -> Integer.toString(gpu.getConfig().getBenchmark()))
				)));

	// let user choose which GPUs should be used
	System.out.println("Please specify the indices of all GPU's you want to use, separated by commata.");
	System.out.print("> ");
	try (Scanner scanner = new Scanner(System.in)) {
	    String input = scanner.nextLine();
	    var indices = input.split(",");

	    for(int i = 0; i < indices.length; i++) {
		var configProperties = indices[i].split(":");
		
		int index;
		try {
		    index = Integer.parseInt(configProperties[0].trim());
		    if(index < 0 || index >= availableGpus.size())
			throw new IllegalArgumentException("invalid index: there is no GPU for index " + index);
		} catch (NumberFormatException e) {
		    throw new NumberFormatException("invalid index: '" + indices[i].trim() + "' is not an integer");
		}

		var gpu = availableGpus.get(index);
		
		if(configProperties.length > 1) {
		    for(int j = 1; j < configProperties.length; j++) {
			String configProperty = configProperties[j];
			String propertyKey = configProperty.substring(0, 2);
			String propertyVal = configProperty.substring(2).trim();
			
			switch(propertyKey) {
			case "bm":
			    try {
				int benchmark = Integer.parseInt(propertyVal);
				gpu.getConfig().setBenchmark(benchmark);
			    } catch (NumberFormatException e) {
				throw new NumberFormatException("invalid benchmark: '" + propertyVal + "' is not an integer");
			    }
			    break;
			case "mu":
			    try {
				float maxUsage = Float.parseFloat(propertyVal);
				gpu.getConfig().setMaxUsage(maxUsage);
			    } catch (NumberFormatException e) {
				throw new NumberFormatException("invalid max usage percentage: '" + propertyVal + "' is not a float");
			    }
			    break;
			case "ws":
			    try {
				int workgroupSize = Integer.parseInt(propertyVal);
				gpu.getConfig().setWorkgroupSize(workgroupSize);
			    } catch (NumberFormatException e) {
				throw new NumberFormatException("invalid workgroup size: '" + propertyVal + "' is not an integer");
			    }
			    break;
			default:
			    throw new IllegalArgumentException("invalid property key: '" + propertyKey + "'");
			}
		    }
		}
	    
		solver.gpuSelection().add(gpu);
	    }
	}

	solver.solve();
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
