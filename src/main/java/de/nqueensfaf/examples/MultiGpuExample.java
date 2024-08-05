package de.nqueensfaf.examples;

import java.util.Arrays;
import java.util.Scanner;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GpuSolver;

public class MultiGpuExample {

    public static void main(String[] args) {

	final int n = 17; // 17-queens problem
	final var gpuSolver = new GpuSolver();

	final var availableGpus = gpuSolver.getAvailableGpus();
	if (availableGpus.size() == 0) {
	    System.err.println("no GPU available, terminating.");
	    System.exit(0);
	}

	// print a table showing all available GPUs
	System.out.println(AsciiTable.getTable(AsciiTable.BASIC_ASCII, availableGpus,
		Arrays.asList(new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
			.dataAlign(HorizontalAlign.CENTER).with(gpu -> Integer.toString(availableGpus.indexOf(gpu))),
			new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().vendor()),
			new Column().header("Name").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(gpu -> gpu.getInfo().name()))));

	/*
	 * In addition to the workgroup size, each GPU can be assigned a benchmark. The
	 * higher the benchmark, the more work will be assigned to the GPU. For example:
	 * GPU1 has a benchmark of 80, GPU2 has a benchmark of 20 -> GPU1 gets 80% of
	 * the workload, GPU2 gets 20%.
	 */
	System.out.println("Which GPUs should be used? \nSpecify them one by one in the format of "
		+ "\"<Index>[,bm=<Benchmark>][,wg=<Workgroup-size>]\". (without the double quotes)");
	try (Scanner scanner = new Scanner(System.in)) {
	    String input = null;
	    do {
		System.out.printf("GPU %d: ", gpuSolver.gpuSelection().get().size() + 1);
		input = scanner.nextLine();

		var gpuConfigs = input.split(",");

		int index = -1;
		try {
		    index = Integer.parseInt(gpuConfigs[0].trim());
		    if (index < 0 || index >= availableGpus.size()) {
			System.err.println("invalid index: there is no GPU for index " + index);
			System.exit(0);
		    }
		} catch (NumberFormatException e) {
		    System.err.println("invalid index: '" + gpuConfigs[0].trim() + "' is not an integer");
		    System.exit(0);
		}

		var gpu = availableGpus.get(index);

		if (gpuConfigs.length > 1) {
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
				int benchmark = Integer.parseInt(configValue);
				gpu.getConfig().setBenchmark(benchmark);
			    } catch (NumberFormatException e) {
				throw new NumberFormatException(
					"invalid benchmark: '" + configValue + "' is not an integer");
			    }
			    break;
			case "wg=":
			    try {
				int workgroupSize = Integer.parseInt(configValue);
				gpu.getConfig().setWorkgroupSize(workgroupSize);
			    } catch (NumberFormatException e) {
				throw new NumberFormatException(
					"invalid workgroup size: '" + configValue + "' is not an integer");
			    }
			    break;
			default:
			    throw new IllegalArgumentException("invalid config key: '" + configKey + "'");
			}
		    }
		}

		gpuSolver.gpuSelection().add(gpu);

	    } while (input.trim().length() > 0 && gpuSolver.gpuSelection().get().size() < availableGpus.size());
	}

	if (gpuSolver.gpuSelection().get().size() == 0) {
	    System.err.println("You have to choose at least 1 GPU.");
	    System.exit(0);
	}

	gpuSolver.setN(n);
	gpuSolver.onStart(() -> System.out.printf("Starting GpuSolver for N=%d.\n", n));
	gpuSolver.onUpdate((progress, solutions, duration) -> System.out.printf("\t%.2f%% [%d solutions, %.2fs]\n",
		progress * 100, solutions, duration / 1000f));
	gpuSolver.onFinish(() -> System.out.printf("GpuSolver found %d solutions in %.2fs.\n", gpuSolver.getSolutions(),
		gpuSolver.getDuration() / 1000f));

	gpuSolver.start();
    }

}
