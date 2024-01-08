package de.nqueensfaf;

import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolver;

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
	new GPUSolver()
		.setPresetQueens(6)
		.setUpdateInterval(200)
		.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
		.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(18)
        	.solve();
    }
}
