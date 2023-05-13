package de.nqueensfaf;

import de.nqueensfaf.compute.GPUSolver;
import de.nqueensfaf.compute.Solver;
import de.nqueensfaf.files.DeviceConfig;

public class Demo {
	
	public static void main(String[] args) {
		run();
		//store();
		//restore();
		//rerestore();
	}
	
	static void run() {
		GPUSolver s = Solver.createGPUSolver();
		s.setN(20);
//		s.setDeviceConfigs(GPUSolver.ALL_DEVICES);
		s.setDeviceConfigs(new DeviceConfig(0, 24, 6, 1), new DeviceConfig(1, 64, 6, 10));
//		s.setDeviceConfigs(new DeviceConfig(1, 64, 6, 5));
//		s.setDeviceConfigs(new DeviceConfig(0, 24, 6, 5));
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setProgressUpdateDelay(50);
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
		});
		s.solve();
	}
}
