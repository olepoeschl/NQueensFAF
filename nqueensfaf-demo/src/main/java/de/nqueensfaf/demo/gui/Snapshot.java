package de.nqueensfaf.demo.gui;

import de.nqueensfaf.core.AbstractSolver.SavePoint;

record Snapshot(SavePoint savePoint, int autoSaveInterval) {
}
