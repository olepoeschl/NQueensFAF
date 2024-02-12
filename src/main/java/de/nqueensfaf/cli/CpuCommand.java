package de.nqueensfaf.cli;

import de.nqueensfaf.impl.CpuSolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "cpu", description = "use CPU", mixinStandardHelpOptions = true)
public class CpuCommand implements Runnable {
    
    @ParentCommand
    BaseCommand base;

    @Option(names = { "-p", "--preset-queens" }, required = false, description = "How many queens should be placed for a start constellation")
    int presetQueens;

    @Option(names = { "-t", "--threads" }, required = false, description = "How many CPU threads should be used")
    int threads;
    
    private CpuSolver solver;
    
    public CpuCommand() {}

    @Override
    public void run() {
	solver = new CpuSolver();
	
	base.applySolverConfig(solver);
	
	if(presetQueens != 0)
	    solver.setPresetQueens(presetQueens);
	if(threads != 0)
	    solver.setThreadCount(threads);
	
	solver.solve();
	System.out.println("(" + base.getUniqueSolutions(solver) + " unique solutions)");
    }
}
