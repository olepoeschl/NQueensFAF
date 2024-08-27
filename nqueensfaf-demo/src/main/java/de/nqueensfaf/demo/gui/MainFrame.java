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
    private final SolverController solverController = new SolverController(solverModel);
    
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
	configUiN.addIntProperty("n", "Board Size N", 1, 31, solverModel.getN(), 1);
	configUiN.addPropertyChangeListener("n", e -> solverModel.setN((int) e.getNewValue()));
	pnlConfigAndControl.add(configUiN.getUi(), constraints);
	solverController.addSolverStartListener(e -> configUiN.setEnabled(false));
	solverController.addSolverFinishListener(e -> configUiN.setEnabled(true));
	
	constraints.gridy++;
	constraints.insets.top = 5;
	var solverSelectionPanel = new SolverSelectionPanel(solverController);
	pnlConfigAndControl.add(solverSelectionPanel, constraints);
	solverController.addSolverStartListener(e -> solverSelectionPanel.setEnabled(false));
	solverController.addSolverFinishListener(e -> solverSelectionPanel.setEnabled(true));

	constraints.gridy++;
	constraints.weighty = 1;
	constraints.fill = GridBagConstraints.BOTH;
	var solverControlPanel = new SolverControlPanel(solverController);
	pnlConfigAndControl.add(solverControlPanel, constraints);
	solverController.addSolverStartListener(e -> solverControlPanel.setEnabled(false));
	solverController.addSolverFinishListener(e -> solverControlPanel.setEnabled(true));

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
	
	solverController.addSolverProgressUpdateListener(e -> {
	    progressBar.setValue((int) (e.getProgress() * 100));
	    System.out.println(e.getSolutions());
	});
    }
}
