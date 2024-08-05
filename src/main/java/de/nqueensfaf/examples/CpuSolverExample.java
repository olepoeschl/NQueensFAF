package de.nqueensfaf.examples;

import de.nqueensfaf.impl.CpuSolver;

public class CpuSolverExample {

    public static void main(String[] args) {
	
	final int n = 16; // 16-queens problem
	final var cpuSolver = new CpuSolver();
	
	cpuSolver.setN(n);
	cpuSolver.setThreadCount(4); // perform the computation using 4 platform threads
	cpuSolver.onFinish(() -> System.out.printf("CpuSolver found %d solutions in %d ms for N=%d\n",
		cpuSolver.getSolutions(), cpuSolver.getDuration(), n));
	
	cpuSolver.start();
	
    }

}
