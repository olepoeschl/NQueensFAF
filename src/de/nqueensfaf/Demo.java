package de.nqueensfaf;

import java.io.IOException;

import de.nqueensfaf.compute.CPUSolver;
import de.nqueensfaf.compute.GPUSolver;

public class Demo {
	
	public static void main(String[] args) {
		//store();
		//restore();
		rerestore();
	}
	
	static void store() {
		GPUSolver s = new GPUSolver();
		s.setN(19);
		s.setDevice(0);
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + s.getDuration() + "ms");
			if(progress > 0.85) {
				try {
					s.store("test.faf");
				} catch (IllegalArgumentException | IOException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		s.solve();
	}

	static void restore() {
		CPUSolver s = new CPUSolver();
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + s.getDuration() + "ms");
			if(progress > 0.5) {
				try {
					s.store("test.faf");
				} catch (IllegalArgumentException | IOException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		s.setThreadcount(7);
		try {
			s.restore("test.faf");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}
	
	static void rerestore() {
		GPUSolver s = new GPUSolver();
		s.setDevice(0);
//		s.setThreadcount(12);
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + s.getDuration() + "ms");
		});
		try {
			s.restore("test.faf");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}

}
