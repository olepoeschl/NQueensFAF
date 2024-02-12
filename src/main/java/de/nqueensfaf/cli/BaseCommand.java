package de.nqueensfaf.cli;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

@Command(name = "nqueensfaf", mixinStandardHelpOptions = true, subcommands = {CpuCommand.class, GpuCommand.class})
public class BaseCommand {
    
    @Spec
    CommandSpec spec;
    
    @ArgGroup(exclusive = true, multiplicity = "1")
    NOrState nOrState;

    static class NOrState {
	int n;
	SolverState state;
	
	@Parameters(description = "Size of the chess board")
	public void n(String input) {
	    try {
		n = Integer.parseInt(input);
	    } catch (NumberFormatException e) {
		pathToSolverStateFile(input);
	    }
	}

	// picocli always calls n() and never this method
	// therefore, the functionality is implemented in the n() method 
	// and the following method is just for the picocli help message
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

    // for printing the progress
    private static final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %18d\tduration: %12s";
    // for showing the loading animation
    private static final char[] loadingChars = new char[] { '-', '\\', '|', '/' };
    private char loadingCharIdx = 0;
    private float lastProgress;
    private final ExecutorService autoSaveExecutorService = Executors.newFixedThreadPool(1);
    
    public BaseCommand() {}
    
    private <T extends Solver<T>> OnUpdateConsumer<T> onUpdate(T solver) {
	if(solver instanceof Stateful && autoSaveProgressStep > 0) {
	    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		try {
		    autoSaveExecutorService.shutdown();
		    autoSaveExecutorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		    System.err.println("could not wait for completion of saving solver state: " + e.getMessage());
		}
	    }));
	    
	    return (self, progress, solutions, duration) -> {
		if (loadingCharIdx == BaseCommand.loadingChars.length)
		    loadingCharIdx = 0;
		System.out.format(BaseCommand.progressStringFormat, BaseCommand.loadingChars[loadingCharIdx++], progress, solutions,
			BaseCommand.getDurationPrettyString(duration));

		if (progress - lastProgress >= autoSaveProgressStep) {
		    autoSaveExecutorService.submit(() -> {
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
	    return (self, progress, solutions, duration) -> {
		if (loadingCharIdx == BaseCommand.loadingChars.length)
		    loadingCharIdx = 0;
		System.out.format(BaseCommand.progressStringFormat, BaseCommand.loadingChars[loadingCharIdx++], progress, solutions,
			BaseCommand.getDurationPrettyString(duration));

	    };
	}
	
    }
    
    private <T extends Solver<T>> Consumer<T> onFinish(T solver){
	if(solver instanceof Stateful && autoSaveProgressStep > 0) {
	    return (s) -> {
		if(s.getUpdateInterval() > 0)
		    System.out.println();
		System.out.println("found " + s.getSolutions() + " solutions in "
			+ getDurationPrettyString(s.getDuration()));
		
		autoSaveExecutorService.shutdown();
		try {
		    autoSaveExecutorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		    System.err.println("could not wait for completion of saving solver state");
		}
	    };
	} else {
	    return (s) -> {
		if(s.getUpdateInterval() > 0)
		    System.out.println();
		System.out.println("found " + s.getSolutions() + " solutions in "
			+ getDurationPrettyString(s.getDuration()));
	    };
	}
    }
    
    <T extends Solver<T>> void applySolverConfig(T solver){
	solver.onInit(s -> System.out.println("starting solver for board size " + s.getN() + "..."));
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
    
    <T extends Solver<T>> long getUniqueSolutions(T solver) {
	SymSolver symSolver = new SymSolver();
	symSolver.setN(solver.getN());
	symSolver.solve();
	return symSolver.getUniqueSolutionsTotal(solver.getSolutions());
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
