package de.nqueensfaf.examples;

import java.util.Scanner;

import de.nqueensfaf.impl.CpuSolver;

public class CpuSolverExample {

    public static void main(String[] args) {

	try (Scanner scanner = new Scanner(System.in)) {
	    // prompt the user to enter N
	    System.out.println("Please enter N.");
	    System.out.print("> ");
	    final int n = scanner.nextInt();

	    // prompt the user to enter the number of threads that should be used
	    System.out.println("Please enter the number of threads that should be used.");
	    System.out.print("> ");
	    final int threads = scanner.nextInt();
	    
	    // instantiate and configure the CpuSolver
	    final var cpuSolver = new CpuSolver();
	    cpuSolver.setN(n);
	    cpuSolver.setThreadCount(threads);
	    
	    // define callbacks for when the CpuSolver starts and finishes and when it makes progress
	    cpuSolver.onStart(() -> System.out.printf("Starting CpuSolver for N=%d.\n", n));
	    cpuSolver.onUpdate((progress, solutions, duration) -> System.out.printf("\t%.2f%% [%d solutions, %.2fs]\n",
			progress * 100, solutions, duration / 1000f));
	    cpuSolver.onFinish(() -> System.out.printf("CpuSolver found %d solutions in %d ms for N=%d\n",
		    cpuSolver.getSolutions(), cpuSolver.getDuration(), n));

	    // start the solver
	    cpuSolver.start();
	}

    }

}
