package de.nqueensfaf.cli;

import java.io.IOException;

import de.nqueensfaf.impl.SolverState;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(name = "nqueensfaf", mixinStandardHelpOptions = true, subcommands = {CPUCommand.class, GPUCommand.class})
public class BaseCommand {
    
    @Spec
    CommandSpec spec;
    
    @ArgGroup(exclusive = true, multiplicity = "1")
    NOrState nOrState;

    static class NOrState {
	@Parameters(description = "Size of the chess board")
	int n;

	SolverState state;
	@Parameters(description = "Path to the solver state file")
	public void pathToSolverStateFile(String input) {
	    try {
		state = SolverState.load(input);
	    } catch (IOException e) {
		throw new TypeConversionException("invalid path: " + e.getMessage());
	    }
	}
    }

    @Option(names = { "-u", "--update-interval" }, required = false, description = "delay between progress updates")
    int updateInterval;

    @Option(names = { "-s", "--auto-save" }, required = false, 
	    description = "How much progress should be made each time until the solver state is saved into a file")
    int autoSavePercentageStep;

    // for printing the progress in the progress callback
    static final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %18d\tduration: %12s";
    // for showing the loading animation in the progress callback
    static final char[] loadingChars = new char[] { '-', '\\', '|', '/' };
    
    char loadingCharIdx = 0;
    
    public BaseCommand() {}
    
    public void run() {
//	try {
//	    // set callbacks
//	    solver.onInit(self -> System.out.println("starting solver for board size " + self.getN() + "..."))
//	    .onUpdate((self, progress, solutions, duration) -> {
//		if (loadingCharIdx == loadingChars.length)
//		    loadingCharIdx = 0;
//		System.out.format(progressStringFormat, loadingChars[loadingCharIdx++], progress, solutions,
//			getDurationPrettyString(duration));
//	    })
//	    .onFinish(self -> {
//		if(self.getUpdateInterval() > 0)
//		    System.out.println();
//		System.out.println("found " + self.getSolutions() + " solutions in "
//			+ getDurationPrettyString(self.getDuration()));
//	    });
//
//	    // TODO: start solver, with state or with board size
//
//	    // calculate unique solutions
//	    SymSolver symSolver = new SymSolver();
//	    symSolver.onFinish(self -> System.out
//		    .println("(" + symSolver.getUniqueSolutionsTotal(solver.getSolutions()) + " unique solutions)"));
//	    symSolver.setN(solver.getN());
//	    symSolver.solve();
//
//	} catch (Exception e) {
//	    System.err.println("could not create or execute solver: " + e.getMessage());
//	}
    }

    static String getDurationPrettyString(long time) {
	long h = time / 1000 / 60 / 60;
	long m = time / 1000 / 60 % 60;
	long s = time / 1000 % 60;
	long ms = time % 1000;

	String strh, strm, strs, strms;
	// hours
	if (h == 0) {
	    strh = "00";
	} else if ((h + "").toString().length() == 3) {
	    strh = "" + h;
	} else if ((h + "").toString().length() == 2) {
	    strh = "0" + h;
	} else {
	    strh = "00" + h;
	}
	// minutes
	if ((m + "").toString().length() == 2) {
	    strm = "" + m;
	} else {
	    strm = "0" + m;
	}
	// seconds
	if ((s + "").toString().length() == 2) {
	    strs = "" + s;
	} else {
	    strs = "0" + s;
	}
	// milliseconds
	if ((ms + "").toString().length() == 3) {
	    strms = "" + ms;
	} else if ((ms + "").toString().length() == 2) {
	    strms = "0" + ms;
	} else {
	    strms = "00" + ms;
	}

	return strh + ":" + strm + ":" + strs + "." + strms;
    }
}
