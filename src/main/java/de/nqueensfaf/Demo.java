package de.nqueensfaf;

import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolver;

public class Demo {

    public static void main(String[] args) {
	@SuppressWarnings("unused")
//	CPUSolver s = new CPUSolver()
//        	.config(config -> {
//        	    config.threadcount = 1;
//        	    config.updateInterval = 800;
//        	    config.presetQueens = 4;
//        	})
//        	.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
//        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
//        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
//        	.setN(16)
//        	.solve();
//    }
    
    GPUSolver s = new GPUSolver()
        	.config(config -> {
        	    config.updateInterval = 800;
        	    config.presetQueens = 6;
        	})
        	.onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
        	.onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
        	.onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
        	.setN(18)
        	.solve();
    }
}
