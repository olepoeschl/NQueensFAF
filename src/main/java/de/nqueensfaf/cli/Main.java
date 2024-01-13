package de.nqueensfaf.cli;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
	ExceptionHandler exceptionHandler = new ExceptionHandler();
	CommandLine commandLine = new CommandLine(new BaseCommand())
		.setParameterExceptionHandler(exceptionHandler)
		.setExecutionExceptionHandler(exceptionHandler);
	commandLine.execute(args);
    }

}
