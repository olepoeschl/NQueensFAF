package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver.SavePoint;
import de.nqueensfaf.demo.gui.extension.SolverExtensionConfig;

record Snapshot(AppConfig appConfig, SavePoint savePoint, SolverExtensionConfig solverExtensionConfig) {
}
