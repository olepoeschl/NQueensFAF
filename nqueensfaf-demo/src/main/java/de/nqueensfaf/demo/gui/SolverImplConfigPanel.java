package de.nqueensfaf.demo.gui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import de.nqueensfaf.core.AbstractSolver;

abstract class SolverImplConfigPanel extends JPanel {
    
    public static Color BACKGROUND_COLOR = new Color(235, 235, 235);
    
    abstract AbstractSolver getConfiguredSolver();
    
    public SolverImplConfigPanel() {
	setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setBackground(BACKGROUND_COLOR);
    }
}
