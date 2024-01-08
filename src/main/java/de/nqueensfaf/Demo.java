package de.nqueensfaf;

import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolver;

public class Demo {

    public static void main(String[] args) {
	cpu();
    }
    
    static void cpu() {
	CPUSolver sc = new CPUSolver();
	sc.config().threadcount = 1;
	sc.config().updateInterval = 800;
	sc.config().presetQueens = 5;
	sc.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(16)
        	.solve();
    }
    
    static void gpu() {
	GPUSolver sg = new GPUSolver();
	sg.config().updateInterval = 200;
	sg.config().presetQueens = 6;
	sg.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
		.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(18)
        	.solve();
    }
}
