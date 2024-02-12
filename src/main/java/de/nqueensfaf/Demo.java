package de.nqueensfaf;

import java.util.List;

import de.nqueensfaf.impl.CpuSolver;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.GpuInfo;

public class Demo {

    public static void main(String[] args) {
	gpu();
    }
    
    static void cpu() {
	var c = new CpuSolver()
        	.setPresetQueens(5)
        	.setThreadCount(1)
        	.setUpdateInterval(800)
        	.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(16)
        	.solve();
	c.reset();
	c.solve();
    }
    
    static void gpu() {
	GpuSolver g = new GpuSolver();
	List<GpuInfo> availableGpus = g.getAvailableGpus();
	for(var gpu : availableGpus) {
	    if(gpu.vendor().toLowerCase().contains("nvidia")) {
		g.gpuSelection().add(gpu.id(), 5, 64);
	    } else {
//		g.gpuSelection().add(gpu.id(), 50, 64);
	    }
	}
	g.setUpdateInterval(1000);
	g.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
		.setN(18)
		.solve();
	g.reset();
	g.solve();
	g.reset();
	g.solve();
	
//	new GPUSolver()
//		.setPresetQueens(6)
//		.setUpdateInterval(200)
//		.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
//		.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
//        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
//        	.setN(18)
//        	.solve();
    }
}
