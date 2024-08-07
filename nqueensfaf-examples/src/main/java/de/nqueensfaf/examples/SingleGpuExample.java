package de.nqueensfaf.examples;

import java.util.Arrays;
import java.util.Scanner;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.impl.GpuSolver;

public class SingleGpuExample {

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

	// prompt user to choose the GPU
	System.out.println("Which GPU should be used? Specify its index.");
	System.out.print("> ");
	try (Scanner scanner = new Scanner(System.in)) {
	    int index = scanner.nextInt();
	    if (index < 0) {
		System.err.println("invalid input: index must not be <0");
		System.exit(0);
	    } else if (index >= availableGpus.size()) {
		System.err.printf("invalid input: index %d is too large for only %d available GPUs\n", index,
			availableGpus.size());
		System.exit(0);
	    }
	    gpuSolver.gpuSelection().choose(availableGpus.get(index));
	}

	gpuSolver.setN(n);
	gpuSolver.onFinish(() -> System.out.printf("GpuSolver found %d solutions in %d ms for N=%d\n",
		gpuSolver.getSolutions(), gpuSolver.getDuration(), n));

	gpuSolver.start();
    }

}
