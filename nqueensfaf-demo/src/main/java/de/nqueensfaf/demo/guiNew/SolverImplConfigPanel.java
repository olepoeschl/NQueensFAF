package de.nqueensfaf.demo.guiNew;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import de.nqueensfaf.core.AbstractSolver;

class SolverImplConfigPanel extends JPanel {
    
    static Color BACKGROUND_COLOR = new Color(235, 235, 235);
    
    SolverImplConfigPanel() {
	setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    
    AbstractSolver getConfiguredSolver() {
	return null;
    }
}
