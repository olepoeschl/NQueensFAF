package de.nqueensfaf;

import de.nqueensfaf.impl.CpuSolver;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.SymSolver;

public class Demo {

    public static void main(String[] args) {
	gpu();
    }
    
    static void cpu() {
	var cs = new CpuSolver();
        cs.setPresetQueens(5);
        cs.setThreadCount(1);
        cs.setUpdateInterval(800);
        cs.onInit(() -> System.out.println("Starting Solver for board size " + cs.getN() + "..."));
        cs.onUpdate((progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration));
        cs.onFinish(() -> System.out.println("Found " + cs.getSolutions() + " solutions in " + cs.getDuration() + " ms"));
        cs.setN(16);
        cs.solve();
    }
    
    static void gpu() {
	GpuSolver gs = new GpuSolver();
	var gpu = gs.getAvailableGpus().get(0);
	gs.gpuSelection().choose(gpu.getId());
	gs.setUpdateInterval(400);
	gs.onInit(() -> System.out.println("Starting Solver for board size " + gs.getN() + "..."));
        gs.onUpdate((progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration));
        gs.onFinish(() -> System.out.println("Found " + gs.getSolutions() + " solutions in " + gs.getDuration() + " ms"));
	gs.setN(21);
	gs.solve();
    }
}
