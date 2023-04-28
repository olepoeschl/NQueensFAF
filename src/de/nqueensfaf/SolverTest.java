package de.nqueensfaf;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolverTest {

	Solver solver;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	// Manual test
//	@Test
//	void testCpuSolver() {
//		de.nqueensfaf.compute.CpuSolver s = new de.nqueensfaf.compute.CpuSolver();
//		s.setN(19);
//		s.setThreadcount(6);
//		s.setProgressUpdatesEnabled(true);
//		s.setProgressUpdateDelay(512);
//		s.setOnProgressUpdateCallback((progress, solutions) -> {
//			System.out.println(progress + " " + s.getDuration() + " " + s.getSolutions());
//		});
//		s.addTerminationCallback(() -> {
//			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + " ms"); 
//		});
//		s.solve();
//	}

	// Manual test
	@Test
	void testGpuSolver() {
		de.nqueensfaf.compute.GpuSolver s = new de.nqueensfaf.compute.GpuSolver();
		s.setDevice(0);
		s.setProgressUpdatesEnabled(true);
		s.setN(18);
		new Thread(() -> {
			while(true) {
				if(s.getGlobalWorkSize() == 0) {
					try {
						Thread.sleep(s.progressUpdateDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				System.out.println("globalWorkSize: " + s.getGlobalWorkSize());
				break;
			}
		}).start();
		s.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println(progress + " " + s.getDuration() + " " + s.getSolutions());
		});
		s.addTerminationCallback(() -> {
			System.out.println(s.getSolutions() + " solutions found in " + s.getDuration() + " ms");
		});
		s.solve();
	}
}
