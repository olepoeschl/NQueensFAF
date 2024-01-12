package de.nqueensfaf;

import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolverNew;
import de.nqueensfaf.impl.GPUSolverNew.GPUInfo;

public class Demo {

    public static void main(String[] args) {
	gpu();
    }
    
    static void cpu() {
	new CPUSolver()
        	.setPresetQueens(5)
        	.setThreadCount(1)
        	.setUpdateInterval(700)
        	.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(16)
        	.solve();
    }
    
    static void gpu() {
	GPUSolverNew g = new GPUSolverNew();
	GPUInfo[] availableGpus = g.getAvailableGpus();
	for(var gpu : availableGpus) {
	    if(gpu.vendor().toLowerCase().contains("nvidia")) {
		g.gpuSelection().add(gpu.id(), 50, 64);
	    } else {
//		g.gpuSelection().add(gpu.id(), 1, 64);
	    }
	}
	g.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
		.setN(18)
		.solve();
	
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
