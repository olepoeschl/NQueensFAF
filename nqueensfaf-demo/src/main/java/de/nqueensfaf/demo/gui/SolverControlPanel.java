package de.nqueensfaf.demo.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

class SolverControlPanel extends JPanel {
    
    private final SolverModel solverModel;
    private JButton btnStart;
    
    SolverControlPanel(SolverModel solverModel) {
	this.solverModel = solverModel;
	
	setLayout(new GridBagLayout());
	
	initUi();
    }
    
    private void initUi() {
	var constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.weightx = 1;
	constraints.weighty = 1;
	constraints.fill = GridBagConstraints.BOTH;
	
	btnStart = new JButton("Start");
	btnStart.addActionListener(e -> {
	    var solver = solverModel.getSelectedSolver();
	    solverModel.applySolverConfig(solver);
	    
	    Thread.ofVirtual().start(() -> solverModel.startSymSolver(solver));
	    Thread.ofVirtual().start(() -> solver.start());
	});
	add(btnStart, constraints);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	btnStart.setEnabled(enabled);
    }
}
