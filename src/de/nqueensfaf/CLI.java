package de.nqueensfaf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.compute.Solver;
import de.nqueensfaf.compute.SymSolver;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(mixinStandardHelpOptions = true)
public class CLI implements Runnable {

	@Spec CommandSpec spec;
	
	@Option(names = { "-d", "--show-devices" }, description = "Show a list of all available OpenCL devices")
	private boolean showAvailableDevices;

	private File configFile;
	@Option(names = { "-c",
			"--config-file" }, paramLabel = "FILE", required = false, description = "Absolute path to the file containing the run configuration")
	public void setConfigFile(File configFile) {
		if (!configFile.exists()) {
			throw new ParameterException(spec.commandLine(), "Invalid value '%s' for option '--config-file': file not found");
		}
		this.configFile = configFile;
	}

	private File taskFile;
	@Option(names = { "-t",
			"--task-file" }, paramLabel = "FILE", required = false, description = "Absolute path to the file containing the task")
	public void setTaskFile(File taskFile) {
		if (!configFile.exists()) {
			throw new ParameterException(spec.commandLine(), "Invalid value '%s' for option '--task-file': file not found");
		}
		this.taskFile = taskFile;
	}
	
	@Option(names = { "-N", "--board-size" }, paramLabel = "INT", required = false, description = "Set the board size")
	private int N = -69;
	
	// for printing the progress in the progress callback
	private final String progressStringFormat = "\r%c\tprogress: %1.10f\tsolutions: %18d\tduration: %12s";
	// for showing the loading animation in the progress callback
	private final char[] loadingChars = new char[] {'-', '\\', '|', '/'};
	private char loadingCharIdx = 0;

	@Override
	public void run() {
		if (showAvailableDevices) {
			var devices = Solver.createGPUSolver().getAvailableDevices();
			System.out.println(AsciiTable.getTable(AsciiTable.BASIC_ASCII, devices,
					Arrays.asList(new Column().header("Index").headerAlign(HorizontalAlign.CENTER)
							.dataAlign(HorizontalAlign.CENTER).with(device -> Integer.toString(device.getIndex())),
							new Column().header("Vendor").headerAlign(HorizontalAlign.CENTER)
									.dataAlign(HorizontalAlign.CENTER).with(device -> device.getVendor()),
							new Column().header("Device Name").headerAlign(HorizontalAlign.CENTER)
									.dataAlign(HorizontalAlign.CENTER).with(device -> device.getName()))));
			return;
		}
		
		// validate board size
		if (taskFile == null) {
			if(N == -69) {
				System.err.println("Missing required option: '--board-size=INT'");
				CommandLine.usage(this, System.err);
				return;
			}
			if(N <= 0 || N >= 32) {
				System.err.println("Invalid board size! Must be a number N with N > 0 and N < 32.");
				return;
			}
		}
		
		try {
			// initialize solver
			Solver solver;
			if(configFile != null)
				solver = Solver.createSolverWithConfig(configFile);
			else
				solver = Solver.createCPUSolver();
			
			// set callbacks
			solver.setInitializationCallback((self) -> {
				System.out.println("Starting solver...");
			});
			solver.setOnProgressUpdateCallback((progress, solutions, duration) -> {
				if(loadingCharIdx == loadingChars.length)
					loadingCharIdx = 0;
				System.out.format(progressStringFormat, loadingChars[loadingCharIdx++], progress, solutions, getDurationPrettyString(duration));
			});
			solver.setTerminationCallback((self) -> {
				System.out.println();
				System.out.println("found " + self.getSolutions() + " solutions in " + getDurationPrettyString(self.getDuration()) + " for board size " + self.getN());
			});
			
			// start
			if(taskFile != null) {
				solver.inject(taskFile);
			} else {
				solver.setN(N);
				solver.solve();
			}
			
			// calculate unique solutions
			SymSolver symSolver = new SymSolver();
			symSolver.setTerminationCallback((self) -> {
				System.out.println("(" + symSolver.getUniqueSolutionsTotal(solver.getSolutions()) + " unique solutions)");
			});
			symSolver.setN(N);
			symSolver.solve();
		} catch (IOException | ClassNotFoundException | ClassCastException | IllegalArgumentException e) {
			System.err.println("Unexpected error!");
			e.printStackTrace();
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
