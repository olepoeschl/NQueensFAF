package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver.SavePoint;
import de.nqueensfaf.demo.gui.extension.SolverExtensionConfig;

record Snapshot(SavePoint savePoint, SolverExtensionConfig solverExtensionConfig, AppConfig appConfig) {
}
