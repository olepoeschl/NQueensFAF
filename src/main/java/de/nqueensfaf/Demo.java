package de.nqueensfaf;

import java.util.List;

import de.nqueensfaf.impl.CpuSolver;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.GpuInfo;

public class Demo {

    public static void main(String[] args) {
	cpu();
    }
    
    static void cpu() {
	new CpuSolver()
        	.setPresetQueens(4)
        	.setThreadCount(1)
        	.setUpdateInterval(800)
        	.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(21)
        	.solve();
    }
    
    static void gpu() {
	GpuSolver gs = new GpuSolver();
	var availableGpus = gs.getAvailableGpus();
	gs.gpuSelection().add(availableGpus.get(0));
	gs.setUpdateInterval(400);
	gs.setPresetQueens(6);
	gs.onInit(() -> System.out.println("Starting Solver for board size " + gs.getN() + "..."));
        gs.onUpdate((progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration));
        gs.onFinish(() -> System.out.println("Found " + gs.getSolutions() + " solutions in " + gs.getDuration() + " ms"));
	gs.setN(19);
	gs.solve();
    }
}
