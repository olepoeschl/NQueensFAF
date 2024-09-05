package de.nqueensfaf.demo.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import de.nqueensfaf.demo.gui.util.Dialog;
import de.nqueensfaf.demo.gui.util.QuickGBC;

class SolverControlPanel extends JPanel {
    
    private final SolverModel solverModel;
    private JButton btnStart;
    
    SolverControlPanel(SolverModel solverModel) {
	this.solverModel = solverModel;
	
	setLayout(new GridBagLayout());
	
	initUi();
    }
    
    private void initUi() {
	btnStart = new JButton("Start");
	btnStart.addActionListener(e -> {
	    var solver = solverModel.getSelectedSolver();
	    String errorMessage = solverModel.checkStartingConditions(solver);
	    if(errorMessage.length() > 0) {
		Dialog.error(errorMessage);
		return;
	    }
	    
	    Thread.ofVirtual().start(() -> solverModel.startSymSolver(solver));
	    Thread.ofVirtual().start(() -> solver.start());
	});
	
	add(btnStart, new QuickGBC(0, 0).weight(1, 1).fill());
    }
    
    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	btnStart.setEnabled(enabled);
    }
}
