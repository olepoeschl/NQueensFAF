package de.nqueensfaf;

import de.nqueensfaf.compute.CpuSolver;

public class Demo {
	
	public static void main(String[] args) {
		CpuSolver s = new CpuSolver();
		s.setN(16);
		s.setThreadcount(1);
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + "ms");
		});
		s.solve();
	}
	
}
