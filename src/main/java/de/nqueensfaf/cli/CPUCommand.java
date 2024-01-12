package de.nqueensfaf.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "cpu", description = "use CPU")
public class CPUCommand implements Runnable {
    
    @ParentCommand
    BaseCommand cli;
    
    public CPUCommand() {}

    @Option(names = { "-p", "--preset-queens" }, required = false, description = "How many queens should be placed for a start constellation")
    int presetQueens;

    @Option(names = { "-t", "--threads" }, required = false, description = "How many CPU threads should be used")
    int threads;

    @Override
    public void run() {
	System.out.println("cpu solver");
    }
}
