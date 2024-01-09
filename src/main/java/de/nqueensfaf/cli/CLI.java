package de.nqueensfaf.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.Solver;
import de.nqueensfaf.impl.CPUSolver;
import de.nqueensfaf.impl.GPUSolver;
import de.nqueensfaf.impl.SolverState;
import de.nqueensfaf.impl.SymSolver;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(name = "nqueensfaf", mixinStandardHelpOptions = true)
public class CLI implements Runnable {

    @Spec
    CommandSpec spec;

    @Option(names = { "-l", "--list-gpus" }, required = false, description = "show a list of all available GPUs")
    public void listGpus() {
	var devices = new GPUSolver().getAvailableGpus();
	System.out.println(
		AsciiTable.getTable(AsciiTable.BASIC_ASCII, devices,
			Arrays.asList(
				new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(device -> Integer.toString(device.index())),
				new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(device -> device.vendor()),
				new Column().header("Device Name").headerAlign(HorizontalAlign.CENTER)
				.dataAlign(HorizontalAlign.CENTER).with(device -> device.name()))));
	System.exit(0);
    }
    
    @ArgGroup(exclusive = true, multiplicity = "1")
    private NOrState nOrState;

    private static class NOrState {
	@Parameters(description = "size of the chess board")
	private int n;

	private SolverState state;
	@Parameters(description = "path to the solver state file")
	public void pathToSolverStateFile(String input) {
	    try {
		state = SolverState.load(input);
	    } catch (IOException e) {
		throw new TypeConversionException("invalid path: " + e.getMessage());
	    }
	}
    }

    @Option(names = { "-u", "--update-interval" }, required = false, description = "delay between progress updates")
    private int updateInterval;

    private String[] gpus;
    private int[] workgroupSizes;
    @Option(names = { "-g", "--gpus" }, split = ",", required = false, description = "choose and configure GPUs for copmuting")
    public void gpuConfigs(String[] input) {
	// TODO
    }

    
    
    // for printing the progress in the progress callback
    private final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %18d\tduration: %12s";
    // for showing the loading animation in the progress callback
    private final char[] loadingChars = new char[] { '-', '\\', '|', '/' };
    private char loadingCharIdx = 0;

    @Override
    public void run() {
	try {
	    // initialize solver
	    Solver solver;
	    if (!executeOnGpu) {
		CPUSolver cpuSolver = new CPUSolver();
		// config
		
		if(taskFile != null)
		    cpuSolver.setState(SolverState.load(taskFile.getAbsolutePath()));
		
		solver = cpuSolver;
	    } else {
		GPUSolver gpuSolver = new GPUSolver();
		// config
		
		// print used devices
		var devices = gpuSolver.getDevicesWithConfig();
		System.out.println("following GPU's will be used:");
		System.out.println(AsciiTable.getTable(AsciiTable.BASIC_ASCII, devices,
			Arrays.asList(
				new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
					.dataAlign(HorizontalAlign.CENTER).with(device -> Integer.toString(device.config().index)),
				new Column().header("Device Name").headerAlign(HorizontalAlign.CENTER)
					.dataAlign(HorizontalAlign.CENTER).with(device -> device.name()),
				new Column().header("Weight").headerAlign(HorizontalAlign.CENTER)
					.dataAlign(HorizontalAlign.CENTER)
					.with(device -> Integer
						.toString(device.config().weight)),
				new Column().header("Workgroup Size").headerAlign(HorizontalAlign.CENTER)
					.dataAlign(HorizontalAlign.CENTER)
					.with(device -> Integer.toString(device.config().workgroupSize)),
				new Column().header("Max Global Work Size").headerAlign(HorizontalAlign.CENTER)
					.dataAlign(HorizontalAlign.CENTER)
					.with(device -> Integer.toString(device.config().maxGlobalWorkSize) + (device.config().maxGlobalWorkSize == 0 ? " (no limit)" : "")))));
		if(devices.stream().anyMatch(device -> device.vendor().toLowerCase().contains("advanced micro devices"))) {
		    System.err.println(
				"warning: you are using one or more AMD GPU's - those are not fully supported by nqueensfaf. \nexpect the program to crash at higher board sizes");
		}
		
		if(taskFile != null)
		    gpuSolver.setState(SolverState.load(taskFile.getAbsolutePath()));
		
		solver = gpuSolver;
	    }

	    // set callbacks
	    solver.onInit(self -> System.out.println("starting solver for board size " + self.getN() + "..."))
		    .onUpdate((self, progress, solutions, duration) -> {
			if (loadingCharIdx == loadingChars.length)
			    loadingCharIdx = 0;
			System.out.format(progressStringFormat, loadingChars[loadingCharIdx++], progress, solutions,
				getDurationPrettyString(duration));
		    }).onFinish(self -> {
			if(self.getUpdateInterval() > 0)
			    System.out.println();
			System.out.println("found " + self.getSolutions() + " solutions in "
				+ getDurationPrettyString(self.getDuration()));
		    });

	    // start
	    if (taskFile == null) {
		solver.setN(n);
		solver.solve();
	    }

	    // calculate unique solutions
	    SymSolver symSolver = new SymSolver();
	    symSolver.onFinish(self -> System.out
		    .println("(" + symSolver.getUniqueSolutionsTotal(solver.getSolutions()) + " unique solutions)"));
	    symSolver.setN(solver.getN());
	    symSolver.solve();
	    
	    // TODO: Log debug info if the user wishes so
	    /*
	     * var solPerIjkl = getSolutionsPerIjkl();
	     * for (var key : solPerIjkl.keySet()) {
	     *     System.out.printf("%d,%d,%d,%d: %d solutions\n", utils.geti(key),
	     *         utils.getj(key), utils.getk(key), utils.getl(key), solPerIjkl.get(key));
	     * }
	     */
	} catch (Exception e) {
	    System.err.println("could not create or execute solver: " + e.getMessage());
	}
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
