package de.nqueensfaf;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import de.nqueensfaf.config.Config;
import de.nqueensfaf.config.ConfigOld;
import de.nqueensfaf.config.DeviceConfig;
import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolver;
import de.nqueensfaf.impl.GPUSolver.GPUSolverConfig;

public class Demo {

    public static void main(String[] args) throws StreamWriteException, DatabindException, IOException, IllegalArgumentException, IllegalAccessException {
//	run();
	GPUSolver s = new GPUSolver().config((config) -> {
	    config.presetQueens = 8;
	    try {
		config.writeTo(new File("config.txt"));
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	});
    }

    static void run() {
	// @formatter:off
	GPUSolver s = new GPUSolver()
		.config((config) -> {
//		    try {
//			config.from(new File("config.txt"));
//		    } catch (IOException e) {
//			e.printStackTrace();
//		    }
		    config.deviceConfigs = new DeviceConfig[] {
			new DeviceConfig(0, 64, 1, 1_000_000_000)
		    };
		    config.presetQueens = 6;
//		    try {
//			config.writeTo(new File("config.txt"));
//		    } catch (IOException e) {
//			e.printStackTrace();
//		    }
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
		.setN(18);
	// @formatter:on
	s.solve();
    }
}
