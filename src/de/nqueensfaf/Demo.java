package de.nqueensfaf;

import java.io.IOException;

import de.nqueensfaf.compute.GPUSolver;

public class Demo {
	
	public static void main(String[] args) {
		run();
		//store();
		//restore();
		//rerestore();
	}
	
	static void run() {
		GPUSolver s = new GPUSolver();
		s.setN(19);
		s.setDevice(0);
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
		});
		s.solve();
	}
	
	static void store() {
		GPUSolver s = new GPUSolver();
		s.setN(19);
		s.setDevice(0);
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
			if(progress > 0.4) {
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
		GPUSolver s = new GPUSolver();
		s.setDevice(0);
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
			if(progress > 0.7) {
				try {
					s.store("test.faf");
				} catch (IllegalArgumentException | IOException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		try {
			s.restore("test.faf");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}
	
	static void rerestore() {
		GPUSolver s = new GPUSolver();
		s.setDevice(0);
		s.setTerminationCallback((self) -> {
			System.out.println(self.getSolutions() + " solutions found in " + self.getDuration() + "ms");
		});
		s.setOnProgressUpdateCallback((progress, solutions, duration) -> {
			System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
		});
		try {
			s.restore("test.faf");
		} catch (ClassNotFoundException | ClassCastException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
	}

}
