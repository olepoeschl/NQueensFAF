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
		// sync solve
		NQueensFAF nqf = new NQueensFAF();
		nqf.setN(16);
		nqf.setThreadcount(1);
		nqf.setOnProgress((done, total) -> {
			float progress = done / total;
			System.out.println("Progress: " + progress);
		});
		nqf.setOnProgressDelay(NQueensFAF.DEFAULT_ON_PROGRESS_DELAY);
		nqf.setSolver(NQueensFAF.DEFAULT);
		nqf.solve();
		long solutions = nqf.getSolutionCount();
		long duration = nqf.getDuration();
		int score = nqf.getScore();
		
		// async solve
		nqf = new NQueensFAF(16, NQueensFAF.MAX_THREADS);
		nqf.setOnProgress((done, total) -> {
			float progress = done / total;
			System.out.println("Progress: " + progress);
		});
		nqf.setOnProgressDelay(128); 	// progress update delay in ms
		nqf.setSolver(NQueensFAF.OPENCL);
		OpenCLDevice[] dvcs = nqf.getOpenCLDevices();		
		nqf.setOpenCLDevice(dvcs[0]);
		nqf.solveAsync();
		nqf.waitFor();
		solutions = nqf.getSolutionCount();
		duration = nqf.getDuration();
		score = nqf.getScore();
		
		// async solve with while loop for doing stuff during computation
		nqf = new NQueensFAF(16, NQueensFAF.MAX_THREADS);
		nqf.setOnProgress((done, total) -> {
			float progress = done / total;
			System.out.println("Progress: " + progress);
		});
		nqf.setSolver(NQueensFAF.DEFAULT);
		nqf.solveAsync();
		while(nqf.isRunning()) {
			// do stuff
		}
		solutions = nqf.getSolutionCount();
		duration = nqf.getDuration();
		score = nqf.getScore();
	}

}
