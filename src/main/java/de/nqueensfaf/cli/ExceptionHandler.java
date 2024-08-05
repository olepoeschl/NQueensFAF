package de.nqueensfaf.cli;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

public class ExceptionHandler implements IParameterExceptionHandler, IExecutionExceptionHandler {

	public int handleParseException(ParameterException ex, String[] args) {
		CommandLine cmd = ex.getCommandLine();
		PrintWriter err = cmd.getErr();
		CommandSpec spec = cmd.getCommandSpec();

		Throwable cause;
		if (ex.getCause() != null)
			cause = ex.getCause();
		else
			cause = ex;

		err.println(cmd.getColorScheme().errorText(cause.toString())); // bold red
		err.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

		return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
				: spec.exitCodeOnInvalidInput();
	}

	@Override
	public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
		PrintWriter err = cmd.getErr();
		CommandSpec spec = cmd.getCommandSpec();

		Throwable cause;
		if (ex.getCause() != null)
			cause = ex.getCause();
		else
			cause = ex;

		err.println(cmd.getColorScheme().errorText(cause.toString())); // bold red
		err.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

		return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
				: spec.exitCodeOnInvalidInput();
	}
}
