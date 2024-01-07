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
import de.nqueensfaf.impl.SymSolver;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(name = "nqueensfaf")
public class CLI implements Runnable {

    @Spec
    CommandSpec spec;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "show this help message")
    private boolean help;
    
    @Option(names = { "-d",
	    "--show-devices" }, required = false, description = "show a list of all available OpenCL devices")
    private boolean showAvailableDevices;

    private File configFile;

    @Option(names = { "-c",
	    "--config" }, paramLabel = "FILE", required = false, description = "absolute path to the configuration file")
    public void setConfigFile(File configFile) {
	if (!configFile.exists()) {
	    throw new ParameterException(spec.commandLine(), "invalid config file: file not found");
	}
	this.configFile = configFile;
    }

    private File taskFile;

    @Option(names = { "-t",
	    "--task" }, paramLabel = "FILE", required = false, description = "absolute path to the file containing the task")
    public void setTaskFile(File taskFile) {
	if (!taskFile.exists()) {
	    throw new ParameterException(spec.commandLine(), "invalid task file: file not found");
	}
	this.taskFile = taskFile;
    }

    @Option(names = { "-n", "--board-size" }, paramLabel = "INT", required = false, description = "board size")
    private int n = -69;

    @Option(names = { "-g", "--gpu" }, required = false, description = "execute on GPU('s)")
    private boolean executeOnGpu;

    // for printing the progress in the progress callback
    private final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %18d\tduration: %12s";
    // for showing the loading animation in the progress callback
    private final char[] loadingChars = new char[] { '-', '\\', '|', '/' };
    private char loadingCharIdx = 0;

    @Override
    public void run() {
	if (showAvailableDevices) {
	    var devices = new GPUSolver().getAvailableDevices();
	    System.out
		    .println(AsciiTable.getTable(AsciiTable.BASIC_ASCII, devices,
			    Arrays.asList(new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
				    .dataAlign(HorizontalAlign.CENTER).with(device -> Integer.toString(device.index())),
				    new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
					    .dataAlign(HorizontalAlign.CENTER).with(device -> device.vendor()),
				    new Column().header("Device Name").headerAlign(HorizontalAlign.CENTER)
					    .dataAlign(HorizontalAlign.CENTER).with(device -> device.name()))));
	    return;
	}

	// validate settings
	if (taskFile == null) {
	    if (n == -69) {
		System.err.println("Missing required option: '--board-size=INT'");
		CommandLine.usage(this, System.err);
		return;
	    }
	    if (n <= 0 || n >= 32) {
		throw new TypeConversionException("invalid board size: " + n + " is not >0 and <32");
	    }
	}

	try {
	    // initialize solver
	    Solver solver;
	    if (!executeOnGpu) {
		CPUSolver cpuSolver = new CPUSolver();
		if (configFile != null) {
		    try {
			cpuSolver.config().load(configFile);
		    } catch (IllegalAccessException | IOException e) {
			throw new IOException("could not apply config: " + e.getMessage());
		    }
		} else {
		    System.out.println("no config file provided, using default config...");
		}
		solver = cpuSolver;
	    } else {
		GPUSolver gpuSolver = new GPUSolver();
		if (configFile != null) {
		    try {
			gpuSolver.config().load(configFile);
		    } catch (IllegalArgumentException | IllegalAccessException | IOException e) {
			throw new IOException("could not apply config: " + e.getMessage());
		    }
		} else {
		    System.out.println("no config file provided, using default config...");
		}
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
			if(self.config().updateInterval > 0)
			    System.out.println();
			System.out.println("found " + self.getSolutions() + " solutions in "
				+ getDurationPrettyString(self.getDuration()));
		    });

	    // start
	    if (taskFile != null) {
		solver.load(taskFile);
	    } else {
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
