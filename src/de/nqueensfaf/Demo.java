package de.nqueensfaf;

import de.nqueensfaf.compute.GPUSolver;
import de.nqueensfaf.compute.Solver;
import de.nqueensfaf.config.DeviceConfig;

public class Demo {
	
	public static void main(String[] args) {
		run();
	}
	
	static void run() {
		GPUSolver s =  Solver.createGPUSolver();
		var c = new DeviceConfig(0, 64, 6, 5);
		s.setDeviceConfigs(c);
		s.setN(20);
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.format("\r\tprogress: %1.10f\tsolutions: %18d\tduration: %dms", progress, solutions, duration);
		});
		s.setTerminationCallback((self) -> {
			System.out.println();
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.solve();
	}
}
