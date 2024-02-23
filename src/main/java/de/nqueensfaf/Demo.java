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
	GpuSolver g = new GpuSolver();
	List<GpuInfo> availableGpus = g.getAvailableGpus();
	g.gpuSelection().choose(availableGpus.get(0).id());
	g.setUpdateInterval(400);
	g.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
		.setN(18)
		.solve();
    }
}
