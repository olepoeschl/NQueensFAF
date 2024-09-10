package de.nqueensfaf.demo.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

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

    @Option(names = { "-p",
	    "--preset-queens" }, required = false, defaultValue = "6", description = "How many queens should be placed for a start positions")
    int presetQueens;

    @Option(names = { "-0", "--use-default-gpu" }, description = "Use the default GPU", defaultValue = "false")
    boolean useDefaultGpu;

    private GpuSolver solver;

    public GpuCommand() {
    }

    @Override
    public void run() {
	solver = new GpuSolver();

	try {
	    base.applySolverConfig(solver);
	} catch (IOException e) {
	    System.err.println("could not apply solver config: " + e.getMessage());
	}

	if (presetQueens != 0)
	    solver.setPresetQueens(presetQueens);

	final var availableGpus = solver.getAvailableGpus();
	if (availableGpus.size() == 0) {
	    System.err.println("no GPU available, terminating.");
	    System.exit(0);
	}

	if (useDefaultGpu) {
	    solver.gpuSelection().add(availableGpus.get(0));
	} else {
	    System.out.println(AsciiTable.getTable(AsciiTable.BASIC_ASCII, availableGpus, Arrays.asList(
		    new Column().header("Index").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
			    .with(gpu -> Integer.toString(availableGpus.indexOf(gpu))),
		    new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
			    .with(gpu -> gpu.getInfo().vendor()),
		    new Column().header("Name").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
			    .with(gpu -> gpu.getInfo().name()))));

	    // let user choose which GPUs should be used
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
		System.out.println("Which GPUs should be used? \nSpecify them one by one in the format of "
			+ "\"<Index>[,bm=<Benchmark>][,wg=<Workgroup-size>]\". (without the double quotes)");
		String input = null;

		System.out.printf("GPU #%d: ", solver.gpuSelection().get().size() + 1);
		input = reader.readLine();
		while (input.trim().length() > 0) {
		    var gpuConfigs = input.split(",");

		    int index = Integer.parseInt(gpuConfigs[0].trim());
		    var gpu = availableGpus.get(index);

		    // if present, parse the configuration entered by the user
		    if (gpuConfigs.length > 1)
			parseAndApplyGpuConfiguration(gpu, gpuConfigs);

		    // add this GPU to the GpuSolver's GPU selection
		    solver.gpuSelection().add(gpu);

		    // prompt the user to enter the index and optional configuration of the next GPU
		    if (solver.gpuSelection().get().size() >= availableGpus.size())
			break;
		    System.out.printf("GPU #%d: ", solver.gpuSelection().get().size() + 1);
		    input = reader.readLine();
		}
	    } catch (IOException e) {
		System.err.println("could not read user input: " + e.getMessage());
		System.exit(0);
	    }
	}

	solver.start();
    }

    private static void parseAndApplyGpuConfiguration(Gpu gpu, String[] gpuConfigs) {
	for (int j = 1; j < gpuConfigs.length; j++) {
	    String config = gpuConfigs[j];
	    if (config.length() < 4) {
		System.err.printf(
			"invalid GPU configuration: '%s' is not a valid key-value combination with one of the available configuration keys",
			config);
		System.exit(0);
	    }

	    String configKey = config.substring(0, 3);
	    String configValue = config.substring(3).trim();

	    switch (configKey) {
	    case "bm=":
		try {
		    int weight = Integer.parseInt(configValue);
		    gpu.getConfig().setWeight(weight);
		} catch (NumberFormatException e) {
		    throw new NumberFormatException("invalid benchmark: '" + configValue + "' is not an integer");
		}
		break;
	    case "wg=":
		try {
		    int workgroupSize = Integer.parseInt(configValue);
		    gpu.getConfig().setWorkgroupSize(workgroupSize);
		} catch (NumberFormatException e) {
		    throw new NumberFormatException("invalid workgroup size: '" + configValue + "' is not an integer");
		}
		break;
	    default:
		throw new IllegalArgumentException("invalid config key: '" + configKey + "'");
	    }
	}
    }
}