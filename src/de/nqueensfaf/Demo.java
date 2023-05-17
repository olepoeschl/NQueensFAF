package de.nqueensfaf;

import java.io.IOException;

import de.nqueensfaf.compute.GPUSolver;
import de.nqueensfaf.compute.Solver;
import de.nqueensfaf.config.DeviceConfig;

public class Demo {
	
	public static void main(String[] args) {
//		store();
//		restore();
		rerestore();
	}
	
	static void store() {
		GPUSolver s = Solver.createGPUSolver();
		s.setN(20);
//		s.setDeviceConfigs(GPUSolver.ALL_DEVICES);
		s.setDeviceConfigs(new DeviceConfig(1, 24, 6, 1), new DeviceConfig(0, 64, 6, 9));
//		s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 5));
//		s.setDeviceConfigs(new DeviceConfig(1, 24, 6, 5));
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
			if(progress > 0.4)
				try {
					s.store("test_task.json");
					System.exit(0);
				} catch (IllegalArgumentException | IOException e) {
					e.printStackTrace();
				}
		});
		s.solve();
	}
	
	static void restore() {
		GPUSolver s = Solver.createGPUSolver();
//		s.setDeviceConfigs(GPUSolver.ALL_DEVICES);
		s.setDeviceConfigs(new DeviceConfig(1, 24, 6, 1), new DeviceConfig(0, 64, 6, 9));
		s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 5));
//		s.setDeviceConfigs(new DeviceConfig(1, 24, 6, 5));
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
			if(progress > 0.8)
				try {
					s.store("test_task.json");
					System.exit(0);
				} catch (IllegalArgumentException | IOException e) {
					e.printStackTrace();
				}
		});
		try {
			s.inject("test_task.json");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}
	
	static void rerestore() {
		GPUSolver s = Solver.createGPUSolver();
//		s.setDeviceConfigs(GPUSolver.ALL_DEVICES);
		s.setDeviceConfigs(new DeviceConfig(1, 24, 6, 1), new DeviceConfig(0, 64, 6, 9));
//		s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 5));
//		s.setDeviceConfigs(new DeviceConfig(1, 24, 6, 5));
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
		});
		try {
			s.inject("test_task.json");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}
}
