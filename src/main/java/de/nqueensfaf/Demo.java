package de.nqueensfaf;

import java.io.IOException;

import de.nqueensfaf.config.Config;
import de.nqueensfaf.impl.CPUSolver;

public class Demo {

    public static void main(String[] args) {
	run();
	
	CPUSolver s = new CPUSolver()
		.config((config) -> {
		    config.from("config.txt");
		    config.threadcount = 4;
		})
		.onProgress((progress, solutions, duration) -> System.out.println("progress"))
		.onFinish((solutions, duration) -> System.out.println("finish"))
		.boardSize(16)
		.solve();
	
	
	
    }

    static void run() {
	CPUSolver s = Solver.createCPUSolver();
//	s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 200, 10_000_000), new DeviceConfig(1, 24, 6, 1, 10_000_000));
//	s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 300, 1_000_000));
	s.setInitializationCallback((self) -> {
	    System.out.println("Starting solver for board size " + self.getN() + "...");
	});
	s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
	    System.out.format("\r\tprogress: %1.10f\tsolutions: %18d\tduration: %dms", progress, solutions, duration);
	    if(progress > 0.01) {
		try {
		    s.store("test.faf");
		} catch (IllegalArgumentException | IOException e) {
		    e.printStackTrace();
		}
		System.exit(0);
	    }
	});
	s.setTerminationCallback((self) -> {
	    System.out.println();
	    System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
	});
	s.setN(18);
	s.solve();
    }
    
    static void restore() {
	CPUSolver s = Solver.createCPUSolver();
//	s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 300, 1_000_000));
	s.setInitializationCallback((self) -> {
	    System.out.println("Starting solver for board size " + self.getN() + "...");
	});
	s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
	    System.out.format("\r\tprogress: %1.10f\tsolutions: %18d\tduration: %dms", progress, solutions, duration);
//	    if(progress > 0.4)
//		try {
//		    s.store("test.faf");
//		} catch (IllegalArgumentException | IOException e) {
//		    e.printStackTrace();
//		}
	});
	s.setTerminationCallback((self) -> {
	    System.out.println();
	    System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
	});
	try {
	    s.inject("test.faf");
	} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
	    e.printStackTrace();
	}
    }
}
