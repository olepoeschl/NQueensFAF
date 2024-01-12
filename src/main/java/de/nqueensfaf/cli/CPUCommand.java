package de.nqueensfaf.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "cpu")
public class CPUCommand implements Runnable {
    
    @ParentCommand
    private BaseCommand cli;
    
    public CPUCommand() {}

    @Option(names = { "-p", "--preset-queens" }, required = false, description = "how many queens should be placed as starting positions")
    int presetQueens;

    @Option(names = { "-t", "--threads" }, required = false, description = "how many CPU threads should be used")
    int threads;

    @Override
    public void run() {
	System.out.println("cpu solver");
    }
}
