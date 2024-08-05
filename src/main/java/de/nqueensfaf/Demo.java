package de.nqueensfaf;

import java.util.List;

import de.nqueensfaf.impl.CpuSolver;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

public class Demo {

	public static void main(String[] args) {
		cpu();
	}

	static void cpu() {
		var cs = new CpuSolver();
		cs.setPresetQueens(5);
		cs.setThreadCount(1);
		cs.setUpdateInterval(800);
		cs.onStart(() -> System.out.println("Starting Solver for board size " + cs.getN() + "..."));
		cs.onUpdate((progress, solutions, duration) -> System.out
				.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration));
		cs.onFinish(
				() -> System.out.println("Found " + cs.getSolutions() + " solutions in " + cs.getDuration() + " ms"));
		cs.setN(16);
		cs.start();
	}

	static void gpu() {
		var gs = new GpuSolver();
		List<Gpu> availableGpus = gs.getAvailableGpus();
		gs.gpuSelection().add(availableGpus.get(0));
		gs.setUpdateInterval(400);
		gs.onStart(() -> System.out.println("Starting Solver for board size " + gs.getN() + "..."));
		gs.onUpdate((progress, solutions, duration) -> System.out
				.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration));
		gs.onFinish(
				() -> System.out.println("Found " + gs.getSolutions() + " solutions in " + gs.getDuration() + " ms"));
		gs.setN(18);
		gs.start();
	}
}
