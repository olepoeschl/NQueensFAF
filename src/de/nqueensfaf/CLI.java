package de.nqueensfaf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import de.nqueensfaf.compute.Solver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public class CLI implements Runnable {

	@Option(names = { "-d", "--show-devices" }, description = "Show a list of all available OpenCL devices")
	private boolean showAvailableDevices;

	@Option(names = { "-c",
			"--config-file" }, paramLabel = "FILE", required = false, description = "Absolute path to the file containing the run configuration")
	private File configFile;

	@Option(names = { "-t",
			"--task-file" }, paramLabel = "FILE", required = false, description = "Absolute path to the file containing the task")
	private File taskFile;

	@Option(names = { "-N", "--board-size" }, defaultValue = "-1", paramLabel = "INT", required = false, description = "Set the board size")
	private int N;
	
	// for printing the progress in the progress callback
//	private final String progressStringFormat = "\r%c\tprogress: %-9.9f\tsolutions: %-22.22d\tduration: %-12s";
	private final String progressStringFormat = "\r%c\tprogress: %f\tsolutions: %d\tduration: %s";
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
		
		// validate input
		if (configFile != null && !configFile.exists()) {
			System.err.println("Specified config file does not exist!");
			return;
		}
		if (taskFile != null && !taskFile.exists()) {
			System.err.println("Specified task file does not exist!");
			return;
		} else {
			if(N <= 0 || N >= 32) {
				System.err.println("Invlalid board size! Must be a number N with N > 0 and N < 32.");
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
