package de.nqueensfaf;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolverTest {

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
	@Test
	void test() {
		Solver solver = new Solver() {
			int total, done;
			long start, end;
			
			@Override
			protected void run() {
				total = 100*N;
				start = System.currentTimeMillis();
				for(done = 0; done < total; done++) {
					end = System.currentTimeMillis();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void save() {}
			@Override
			public void restore() {}

			@Override
			public void reset() {
				done = 0;
				start = 0;
				end = 0;
			}
			
			@Override
			public long getDuration() {
				return end - start;
			}

			@Override
			public float getProgress() {
				return ((float) done) / total;
			}

			@Override
			public long getSolutions() {
				return done*10;
			}
		};
		solver.setN(5);
		solver.addOnStartCallback(() -> System.out.println("starte..."));
		solver.addOnEndCallback(() -> System.out.println("fertig!"));
		solver.setOnTimeUpdateCallback((duration) -> {
			System.out.println("Duration: " + duration/1000 + "." + duration%1000);
		});
		solver.setTimeUpdateDelay(500);
		solver.setOnProgressUpdateCallback((progress, solutions) -> {
			System.out.println("Progress: " + progress + ", solutions: " + solutions);
		});
		solver.setProgressUpdateDelay(1000);
		solver.solve();
		
		solver.reset();
		solver.solveAsync();
		assertThrows(IllegalStateException.class, () -> solver.solve());
		while(solver.isIdle() || solver.isInitializing());
		while(solver.isRunning()) {
			System.out.println("läuft");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(solver.getDuration() > 2500)
				break;
		}
		System.out.println("warte auf beendigung...");
		try {
			solver.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
