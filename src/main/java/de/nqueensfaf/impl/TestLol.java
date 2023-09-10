package de.nqueensfaf.impl;

public class TestLol {

    public static void main(String[] args) {
	GPUSolverCUDA c = new GPUSolverCUDA();
	c.setN(10).onFinish((self) -> {
	   System.out.println("solutions: " + self.getSolutions()); 
	});
	c.solve();
    }
    
}
