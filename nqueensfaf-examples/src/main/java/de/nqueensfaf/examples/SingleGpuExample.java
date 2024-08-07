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

public class SingleGpuExample {

    public static void main(String[] args) {

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

	    // prompt the user to enter the index of the GPU that should perform the
	    // computation
	    System.out.println("Please enter the index of the GPU that should be used.");
	    System.out.print("> ");
	    final int gpuIndex = Integer.parseInt(reader.readLine().trim());

	    // configure the GpuSolver
	    gpuSolver.setN(n);
	    gpuSolver.gpuSelection().choose(availableGpus.get(gpuIndex));

	    // define callbacks for when the CpuSolver starts and finishes and when it makes
	    // progress
	    gpuSolver.onStart(() -> System.out.printf("Starting GpuSolver for N=%d.\n", n));
	    gpuSolver.onUpdate((progress, solutions, duration) -> System.out.printf("\t%.2f%% [%d solutions, %.2fs]\n",
		    progress * 100, solutions, duration / 1000f));
	    gpuSolver.onFinish(() -> System.out.printf("GpuSolver found %d solutions in %d ms for N=%d\n",
		    gpuSolver.getSolutions(), gpuSolver.getDuration(), n));

	    // start the solver
	    gpuSolver.start();

	} catch (NumberFormatException e) {
	    e.printStackTrace();
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

}
