package de.nqueensfaf.cli;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.nqueensfaf.Solver;
import de.nqueensfaf.Solver.OnUpdateConsumer;
import de.nqueensfaf.impl.SolverState;
import de.nqueensfaf.impl.Stateful;
import de.nqueensfaf.impl.SymSolver;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(name = "nqueensfaf", mixinStandardHelpOptions = true, subcommands = {CpuCommand.class, GpuCommand.class})
public class BaseCommand {
    
    @Spec
    CommandSpec spec;
    
    @ArgGroup(exclusive = true, multiplicity = "1")
    NOrState nOrState;

    static class NOrState {
	SolverState state;
	
	@Option(names = { "-n", "--board-size" }, required = true, description = "Size of the chess board")
	int n;

	@Option(names = { "-r", "--restore-state" }, description = "Path to the solver state file")
	public void pathToSolverStateFile(String input) {
	    try {
		state = SolverState.load(input);
	    } catch (IOException e) {
		throw new TypeConversionException("invalid path: " + e.getMessage());
	    }
	}
    }

    @Option(names = { "-u", "--update-interval" }, required = false, description = "Delay between progress updates")
    int updateInterval;

    @Option(names = { "-s", "--auto-save" }, required = false, 
	    description = "How much progress should be made each time until the solver state is saved into a file")
    float autoSaveProgressStep;

    // for printing the progress
    private static final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %20s\tduration: %12s";
    private Future<?> autoSaveFuture;
    // for showing the loading animation
    private static final char[] loadingChars = new char[] { '-', '\\', '|', '/' };
    private char loadingCharIdx = 0;
    private float lastProgress;
    private final ExecutorService autoSaveExecutorService = Executors.newFixedThreadPool(1);
    
    public BaseCommand() {}
    
    private OnUpdateConsumer onUpdate(Solver solver) {
	if(solver instanceof Stateful && autoSaveProgressStep > 0) {
	    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		try {
		    autoSaveExecutorService.shutdown();
		    autoSaveExecutorService.awaitTermination(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		    System.err.println("could not wait for completion of saving solver state: " + e.getMessage());
		}
	    }));
	    
	    return (progress, solutions, duration) -> {
		if (loadingCharIdx == loadingChars.length)
		    loadingCharIdx = 0;
		System.out.format(progressStringFormat, loadingChars[loadingCharIdx++], progress, getSolutionsPrettyString(solutions),
			getDurationPrettyString(duration));

		if (progress - lastProgress >= autoSaveProgressStep && 
			(autoSaveFuture == null || autoSaveFuture.isDone())) {
		    autoSaveFuture = autoSaveExecutorService.submit(() -> {
			try {
			    ((Stateful) solver).getState().save(solver.getN() + "-queens.faf");
			} catch (IOException e) {
			    System.err.println("could not save solver state: " + e.getMessage());
			}
		    });
		    lastProgress = progress;
		}
	    };
	} else {
	    return (progress, solutions, duration) -> {
		if (loadingCharIdx == loadingChars.length)
		    loadingCharIdx = 0;
		System.out.format(progressStringFormat, loadingChars[loadingCharIdx++], progress, getSolutionsPrettyString(solutions),
			getDurationPrettyString(duration));

	    };
	}
	
    }
    
    private Runnable onFinish(Solver solver) {
	final Runnable defaultOnFinish = () -> {
		if(solver.getUpdateInterval() > 0)
		    System.out.println();
		System.out.println("found " + getSolutionsPrettyString(solver.getSolutions()) + " solutions in "
			+ getDurationPrettyString(solver.getDuration()));
		System.out.println("(" + getSolutionsPrettyString(getUniqueSolutions(solver)) + " unique solutions)");
	}; 
		
	if(solver instanceof Stateful && autoSaveProgressStep > 0) {
	    return () -> {
		defaultOnFinish.run();
		
		autoSaveExecutorService.shutdown();
		try {
		    autoSaveExecutorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		    System.err.println("could not wait for completion of saving solver state");
		}
	    };
	} else
	    return defaultOnFinish;
    }
    
    void applySolverConfig(Solver solver){
	solver.onInit(() -> System.out.println("starting solver for board size " + solver.getN() + "..."));
	solver.onFinish(onFinish(solver));
	solver.onUpdate(onUpdate(solver));
	
	if(updateInterval != 0)
	    solver.setUpdateInterval(updateInterval);
	
	if(solver instanceof Stateful && nOrState.state != null) {
	    ((Stateful) solver).setState(nOrState.state);
	    lastProgress = solver.getProgress();
	} else {
	    solver.setN(nOrState.n);
	}
    }
    
    long getUniqueSolutions(Solver solver) {
	SymSolver symSolver = new SymSolver();
	symSolver.setN(solver.getN());
	symSolver.solve();
	return symSolver.getUniqueSolutionsTotal(solver.getSolutions());
    }

    static String getSolutionsPrettyString(long solutions) {
	StringBuilder sb = new StringBuilder(Long.toString(solutions));
	for(int i = sb.length() - 3; i >= 0; i -= 3) {
	    if(i <= 0)
		break;
	    sb.insert(i, ".");
	}
	return sb.toString();
    }
    
    private static String getDurationPrettyString(long time) {
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