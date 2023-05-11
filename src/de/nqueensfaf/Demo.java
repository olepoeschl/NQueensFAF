package de.nqueensfaf;

import de.nqueensfaf.compute.GPUSolver;
import de.nqueensfaf.compute.Solver;

public class Demo {
	
	public static void main(String[] args) {
		run();
		//store();
		//restore();
		//rerestore();
	}
	
	static void run() {
		GPUSolver s = Solver.createGPUSolver();
		s.setN(18);
//		s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 100));
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
		});
		s.solve();
	}
}
