package de.nqueensfaf.demo;

import com.formdev.flatlaf.FlatLightLaf;

import de.nqueensfaf.demo.cli.BaseCommand;
import de.nqueensfaf.demo.cli.ExceptionHandler;
import de.nqueensfaf.demo.gui.Controller;
import picocli.CommandLine;

public class Main {
    
    public static final String VERSION = "3.0.0-SNAPSHOT";
    public static final String VERSION_DATE = "???";

    public static void main(String[] args) {
	if (args.length == 0) {
	    // show gui
	    FlatLightLaf.setup();
	    var controller = new Controller();
	    controller.createAndShowView();
	} else {
	    // handle command line arguments
	    ExceptionHandler exceptionHandler = new ExceptionHandler();
	    CommandLine commandLine = new CommandLine(new BaseCommand()).setParameterExceptionHandler(exceptionHandler)
		    .setExecutionExceptionHandler(exceptionHandler);
	    commandLine.execute(args);
	}
    }

}
