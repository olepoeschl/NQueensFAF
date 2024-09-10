package de.nqueensfaf.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

public class MultiGpuExample {

    public static void main(String[] args) {

	System.out.println("===== MultiGpu Example =====");

	// instantiate GpuSolver and fetch available GPUs
	final var gpuSolver = new GpuSolver();
	final var availableGpus = gpuSolver.getAvailableGpus();
	if (availableGpus.size() == 0) {
	    System.err.println("no GPU available, terminating.");
	    System.exit(0);
	}

	printAvailableGpus(availableGpus);

	try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
	    // prompt the user to enter N
	    System.out.println("Please enter N.");
	    System.out.print("> ");
	    final int n = Integer.parseInt(reader.readLine().trim());

	    /*
	     * In addition to the workgroup size, each GPU can be assigned a benchmark. The
	     * higher the benchmark, the more work will be assigned to the GPU. For example:
	     * GPU #1 has a benchmark of 80, GPU #2 has a benchmark of 20 -> GPU #1 gets 80%
	     * of the workload, GPU #2 gets 20%. Now, prompt the user to enter the index and
	     * optional configuration of each GPU that should be used for the computation.
	     */
	    System.out.println("Which GPUs should be used? \nSpecify them one by one in the format of "
		    + "\"<Index>[,bm=<Benchmark>][,wg=<Workgroup-size>]\". (without the double quotes)");
	    String input = null;

	    System.out.printf("GPU #%d: ", gpuSolver.gpuSelection().get().size() + 1);
	    input = reader.readLine();
	    while (input.trim().length() > 0) {
		var gpuConfigs = input.split(",");
		
		int index = Integer.parseInt(gpuConfigs[0].trim());
		var gpu = availableGpus.get(index);

		// if present, parse the configuration entered by the user
		if (gpuConfigs.length > 1)
		    parseAndApplyGpuConfiguration(gpu, gpuConfigs);

		// add this GPU to the GpuSolver's GPU selection
		gpuSolver.gpuSelection().add(gpu);

		// prompt the user to enter the index and optional configuration of the next GPU
		if(gpuSolver.gpuSelection().get().size() >= availableGpus.size())
		    break;
		System.out.printf("GPU #%d: ", gpuSolver.gpuSelection().get().size() + 1);
		input = reader.readLine();
	    }

	    // configure N for the GpuSolver
	    gpuSolver.setN(n);

	    // define callbacks for when the CpuSolver starts and finishes and when it makes
	    // progress
	    gpuSolver.onStart(() -> System.out.printf("Starting GpuSolver for N=%d.\n", n));
	    gpuSolver.onProgressUpdate((progress, solutions, duration) -> System.out.printf("\t%.2f%% [%d solutions, %.2fs]\n",
		    progress * 100, solutions, duration / 1000f));
	    gpuSolver.onFinish(() -> System.out.printf("GpuSolver found %d solutions in %d ms for N=%d\n",
		    gpuSolver.getSolutions(), gpuSolver.getDuration(), n));

	    // start the solver
	    gpuSolver.start();

	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private static void printAvailableGpus(List<Gpu> availableGpus) {
	System.out.println(AsciiTable.getTable(AsciiTable.BASIC_ASCII, availableGpus,
		Arrays.asList(new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
			.dataAlign(HorizontalAlign.CENTER).with(gpu -> Integer.toString(availableGpus.indexOf(gpu))),
			new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().vendor()),
			new Column().header("Name").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().name()))));
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
		    throw new NumberFormatException("invalid weight: '" + configValue + "' is not an integer");
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
