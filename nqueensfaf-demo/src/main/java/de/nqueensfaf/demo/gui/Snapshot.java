package de.nqueensfaf.demo.gui;

import java.util.Map;

import de.nqueensfaf.core.AbstractSolver.SavePoint;

record Snapshot(SavePoint savePoint, Map<String, Object> solverExtensionConfig, Settings settings) {
}
