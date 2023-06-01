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
		s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 200, 10_000_000), new DeviceConfig(1, 24, 6, 1, 10_000_000));
		s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 300, 1_000_000));
		s.setN(18);
		s.setInitializationCallback((self) -> {
			System.out.println("Starting solver for board size " + self.getN() + "...");
		});
		s.setOnTimeUpdateCallback((progress, solutions, duration) -> {
			System.out.format("\r\tprogress: %1.10f\tsolutions: %18d\tduration: %dms", progress, solutions, duration);
		});
		s.setTerminationCallback((self) -> {
			System.out.println();
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.solve();
	}
}
