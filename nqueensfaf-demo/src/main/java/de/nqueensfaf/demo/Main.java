package de.nqueensfaf.demo;

import java.awt.EventQueue;

import com.formdev.flatlaf.FlatLightLaf;

import de.nqueensfaf.demo.cli.BaseCommand;
import de.nqueensfaf.demo.cli.ExceptionHandler;
import de.nqueensfaf.demo.gui.MainFrame;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
	if (args.length == 0) {
	    // show gui
	    FlatLightLaf.setup();
	    EventQueue.invokeLater(MainFrame::new);
	} else {
	    // handle command line arguments
	    ExceptionHandler exceptionHandler = new ExceptionHandler();
	    CommandLine commandLine = new CommandLine(new BaseCommand()).setParameterExceptionHandler(exceptionHandler)
		    .setExecutionExceptionHandler(exceptionHandler);
	    commandLine.execute(args);
	}
    }

}
