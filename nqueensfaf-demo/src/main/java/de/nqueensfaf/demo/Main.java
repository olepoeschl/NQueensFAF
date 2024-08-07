package de.nqueensfaf.demo;

import de.nqueensfaf.demo.cli.BaseCommand;
import de.nqueensfaf.demo.cli.ExceptionHandler;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
	ExceptionHandler exceptionHandler = new ExceptionHandler();
	CommandLine commandLine = new CommandLine(new BaseCommand()).setParameterExceptionHandler(exceptionHandler)
		.setExecutionExceptionHandler(exceptionHandler);
	commandLine.execute(args);
    }

}
