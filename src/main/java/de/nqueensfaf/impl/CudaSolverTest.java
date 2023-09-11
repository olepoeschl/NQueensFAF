package de.nqueensfaf.impl;

public class CudaSolverTest {

    public static void main(String[] args) {
	GPUSolverCUDA c = new GPUSolverCUDA();
	c.setN(18).onFinish((self) -> {
	   System.out.println("solutions: " + self.getSolutions()); 
	})
	.onUpdate((self, progress, solutions, duration) -> {
	    System.out.println("progress: " + progress + ", solutions: " + solutions + ", duration: " + duration + "ms");
	})
	;
	c.solve();
    }
    
}
