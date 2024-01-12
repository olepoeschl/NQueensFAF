package de.nqueensfaf.cli;

import java.io.IOException;
import java.util.function.Consumer;

import de.nqueensfaf.Solver;
import de.nqueensfaf.Solver.OnUpdateConsumer;
import de.nqueensfaf.impl.SolverState;
import de.nqueensfaf.impl.Stateful;
import de.nqueensfaf.impl.SymSolver;
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
    float autoSaveProgressStep;

    // define solver callbacks
    // for printing the progress
    static final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %18d\tduration: %12s";
    // for showing the loading animation
    static final char[] loadingChars = new char[] { '-', '\\', '|', '/' };

    static final Consumer<Solver> onInit = (solver) -> {
	System.out.println("starting solver for board size " + solver.getN() + "...");
    };
    
    static final Consumer<Solver> onFinish = (solver) -> {
	if(solver.getUpdateInterval() > 0)
	    System.out.println();
	System.out.println("found " + solver.getSolutions() + " solutions in "
		+ getDurationPrettyString(solver.getDuration()));
    };

    char loadingCharIdx = 0;
    float lastProgress;
    
    public BaseCommand() {}
    
    private OnUpdateConsumer onUpdate(Solver solver) {
	if(solver instanceof Stateful && autoSaveProgressStep > 0) {
	    return (self, progress, solutions, duration) -> {
		if (loadingCharIdx == BaseCommand.loadingChars.length)
		    loadingCharIdx = 0;
		System.out.format(BaseCommand.progressStringFormat, BaseCommand.loadingChars[loadingCharIdx++], progress, solutions,
			BaseCommand.getDurationPrettyString(duration));

		if (progress - lastProgress >= autoSaveProgressStep)
		    try {
			((Stateful) solver).getState().save(solver.getN() + "-queens.faf");
		    } catch (IOException e) {
			System.err.println("could not save solver state: " + e.getMessage());
		    }
	    };
	} else {
	    return (self, progress, solutions, duration) -> {
		if (loadingCharIdx == BaseCommand.loadingChars.length)
		    loadingCharIdx = 0;
		System.out.format(BaseCommand.progressStringFormat, BaseCommand.loadingChars[loadingCharIdx++], progress, solutions,
			BaseCommand.getDurationPrettyString(duration));

	    };
	}
	
    }
    
    void applySolverConfig(Solver solver){
	solver.onInit(onInit);
	solver.onFinish(onFinish);
	solver.onUpdate(onUpdate(solver));
	
	if(updateInterval != 0)
	    solver.setUpdateInterval(updateInterval);
	
	if(solver instanceof Stateful && nOrState.state != null) {
	    ((Stateful) solver).setState(nOrState.state);
	} else {
	    solver.setN(nOrState.n);
	}
    }
    
    long getUniqueSolutions(int n) {
	SymSolver symSolver = new SymSolver();
	symSolver.setN(n);
	symSolver.solve();
	return symSolver.getSolutions();
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
