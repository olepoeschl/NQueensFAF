package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    
    private final SolverModel solverModel = new SolverModel();
    
    public MainFrame() {
	createAndShowUi();
    }
    
    private void createAndShowUi() {
	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	JPanel container = new JPanel();
	var layout = new BorderLayout(10, 10);
	container.setLayout(layout);
	container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setContentPane(container);
	
	// west
	JPanel pnlConfigAndControl = new JPanel();
	pnlConfigAndControl.setLayout(new GridBagLayout());
	add(pnlConfigAndControl, BorderLayout.CENTER);
	
	var constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.weightx = 1;
	constraints.weighty = 0;
	constraints.fill = GridBagConstraints.BOTH;
	constraints.anchor = GridBagConstraints.NORTH;
	// add n slider
	var configUiN = new PropertyGroupConfigUi();
	configUiN.addIntProperty("n", "Board Size N", 1, 31, 16, 1);
	configUiN.addPropertyChangeListener("n", e -> solverModel.setN((int) e.getNewValue()));
	pnlConfigAndControl.add(configUiN.getUi(), constraints);
	
	constraints.gridy++;
	constraints.insets.top = 5;
	var solverSelectionPanel = new SolverSelectionPanel(solverModel);
	pnlConfigAndControl.add(solverSelectionPanel, constraints);

	constraints.gridy++;
	constraints.weighty = 1;
	constraints.fill = GridBagConstraints.BOTH;
	var solverControlPanel = new SolverControlPanel(solverModel);
	pnlConfigAndControl.add(solverControlPanel, constraints);

	// south
	addProgressBar();
	
	pack();
//	setResizable(false);
	setVisible(true);
    }
    
    private void addProgressBar() {
	JProgressBar progressBar = new JProgressBar(0, 100);
	progressBar.setStringPainted(true);
	progressBar.setValue(0);
	add(progressBar, BorderLayout.SOUTH);
	
	solverModel.addSolverProgressUpdateListener(e -> {
	    progressBar.setValue((int) (e.getProgress() * 100));
	    System.out.println(e.getSolutions());
	});
    }
}
