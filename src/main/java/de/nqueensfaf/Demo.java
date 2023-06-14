package de.nqueensfaf;

import java.io.File;
import java.io.IOException;

import de.nqueensfaf.config.ConfigOld;
import de.nqueensfaf.config.DeviceConfig;
import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolver;

public class Demo {

    public static void main(String[] args) {
	run();
    }

    static void run() {
	// @formatter:off
	GPUSolver s = new GPUSolver()
		.config((config) -> {
		    try {
			config.from(new File("config.txt"));
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		    config.deviceConfigs = new DeviceConfig[] {
			new DeviceConfig(0, 32, 5, 500000)
		    };
		    config.presetQueens = 7;
		})
		.onInit((self) -> {
		    System.out.println("Starting solver for board size " + self.getN() + "...");
		})
		.onUpdate((self, progress, solutions, duration) -> {
		    System.out.format("\r\tprogress: %1.10f\tsolutions: %18d\tduration: %dms", progress, solutions, duration);
		    if (duration > 3000) {
			new Thread(() -> {
        		    try {
        			self.store("hi.txt");
        		    } catch (IllegalArgumentException | IOException e) {
        			e.printStackTrace();
        		    }
			}).start();
			System.exit(0);
		    }
		})
		.onFinish((self) -> {
		    System.out.println();
		    System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		})
		.setN(20);
	// @formatter:on
//	s.setDeviceConfigs(new DeviceConfig(0, 64, 6, 200, 10_000_000), new DeviceConfig(1, 24, 6, 1, 10_000_000));
	s.setDeviceConfigs(new DeviceConfig(0, 64, 300, 1_000_000));
	s.solve();
    }
}
