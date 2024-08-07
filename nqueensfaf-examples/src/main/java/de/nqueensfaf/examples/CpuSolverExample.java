package de.nqueensfaf.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.nqueensfaf.impl.CpuSolver;

public class CpuSolverExample {

    public static void main(String[] args) {

	try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
	    // prompt the user to enter N
	    System.out.println("Please enter N.");
	    System.out.print("> ");
	    final int n = Integer.parseInt(reader.readLine().trim());

	    // prompt the user to enter the number of threads that should be used
	    System.out.println("Please enter the number of threads that should be used.");
	    System.out.print("> ");
	    final int threads = Integer.parseInt(reader.readLine().trim());

	    // instantiate and configure the CpuSolver
	    final var cpuSolver = new CpuSolver();
	    cpuSolver.setN(n);
	    cpuSolver.setThreadCount(threads);

	    // define callbacks for when the CpuSolver starts and finishes and when it makes
	    // progress
	    cpuSolver.onStart(() -> System.out.printf("Starting CpuSolver for N=%d.\n", n));
	    cpuSolver.onUpdate((progress, solutions, duration) -> System.out.printf("\t%.2f%% [%d solutions, %.2fs]\n",
		    progress * 100, solutions, duration / 1000f));
	    cpuSolver.onFinish(() -> System.out.printf("CpuSolver found %d solutions in %d ms for N=%d\n",
		    cpuSolver.getSolutions(), cpuSolver.getDuration(), n));

	    // start the solver
	    cpuSolver.start();

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

}
