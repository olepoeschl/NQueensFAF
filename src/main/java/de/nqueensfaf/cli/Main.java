package de.nqueensfaf.cli;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
	new CommandLine(new CLI()).execute(args);
    }

}
