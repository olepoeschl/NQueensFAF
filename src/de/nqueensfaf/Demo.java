package de.nqueensfaf;

import java.io.IOException;

import de.nqueensfaf.compute.CPUSolver;

public class Demo {
	
	public static void main(String[] args) {
		//store();
		//restore();
		//rerestore();
	}
	
	static void store() {
		CPUSolver s = new CPUSolver();
		s.setN(16);
		s.setThreadcount(2);
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + s.getDuration() + "ms");
			if(progress > 0.2) {
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
		CPUSolver s = new CPUSolver();
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + s.getDuration() + "ms");
		});
		s.setThreadcount(2);
		try {
			s.restore("test.faf");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}

}
