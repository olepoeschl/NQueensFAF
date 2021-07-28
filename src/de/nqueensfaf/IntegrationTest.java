package de.nqueensfaf;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationTest {

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

	@Test
	void test() {
		
		// cpu
		NQueensFAF nqf = new NQueensFAF(new CpuSolver().setThreadcount(2));
		nqf.setN(16);
		nqf.setOnUpdateCallback((done, total, duration, solutions) -> {
			// ...
		});
		nqf.setUpdateDelay(128);
		nqf.solveAsync();
		while(nqf.isRunning()) {
			// do stuff
			nqf.pause();
			// Thread.sleep(500)
			nqf.unpause();
			// Thread.sleep(500)
		}
		long duration = nqf.getDuration();
		long solutions = nqf.getSolutions();
		
		// gpu
		nqf.setSolver(new GpuSolver().useDefaultDevice().setWorkgroupSize(64));
		nqf.setN(17);
		nqf.setOnUpdateCallback((done, total, duration, solutions) -> {
			// ...
		});
		// nqf.setUpdateDelay(NQueensFAF.DEFAULT_UPDATE_DELAY);
		nqf.solve();
		duration = nqf.getDuration();
		solutions = nqf.getSolutions();
		
		// custom solver
		nqf.setSolver(new MySolver().setAttributeX(0).setAttributeY(1));
		// ... see above ...
	}

}
