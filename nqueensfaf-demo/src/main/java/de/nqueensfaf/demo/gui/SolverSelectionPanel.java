package de.nqueensfaf.demo.gui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.nqueensfaf.core.AbstractSolver;

class SolverSelectionPanel extends JTabbedPane {
    
    private static final Color SELECTED_BACKGROUND_COLOR = new Color(235, 235, 235);
    private final Color defaultTabColor;
    
    SolverSelectionPanel(SolverModel solverModel) {
	defaultTabColor = getBackground();
	addChangeListener(e -> {
	    for(int i = 0; i < getTabCount(); i++)
		setBackgroundAt(i, defaultTabColor);
	    setBackgroundAt(getSelectedIndex(), SELECTED_BACKGROUND_COLOR);
	    
	    solverModel.setSelectedSolver(
		    ((SolverImplConfigPanel) getSelectedComponent()).getConfiguredSolver());
	});
	addTab("CPU", new CpuSolverConfigPanel());
	addTab("GPU", new GpuSolverConfigPanel());
    }
    
    static abstract class SolverImplConfigPanel extends JPanel {
	public SolverImplConfigPanel() {
	    setBackground(SELECTED_BACKGROUND_COLOR);
	    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}
	
	abstract AbstractSolver getConfiguredSolver();
    }
}
