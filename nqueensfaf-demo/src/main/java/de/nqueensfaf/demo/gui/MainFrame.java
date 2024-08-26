package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.core.AbstractSolver.OnUpdateConsumer;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPaneSolverSelection;
    
    private PropertyGroupConfigUi commonSolverConfigs;
    
    private JProgressBar progressBar;

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

	addLeftPanel();

//	addRightPanel();

	addProgressBar();

	pack();
	setResizable(false);
	setVisible(true);
    }

    private void addLeftPanel() {
	var pnlWest = new JPanel();
	pnlWest.setLayout(new GridBagLayout());
	add(pnlWest, BorderLayout.CENTER);

	var constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.weightx = 1;
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.anchor = GridBagConstraints.NORTH;
	addCommonSolverConfigPanel(pnlWest, constraints);

	constraints.insets.top = 5;
	constraints.gridy++;
	addSolverSelectionPanel(pnlWest, constraints);

	constraints.gridy++;
	addControlPanel(pnlWest, constraints);
    }

    private void addSolverSelectionPanel(JPanel parent, GridBagConstraints constraints) {
	tabbedPaneSolverSelection = new JTabbedPane();
	tabbedPaneSolverSelection.addTab("CPU", new CpuSolverConfigPanel());
	tabbedPaneSolverSelection.addTab("GPU", new GpuSolverConfigPanel());
	final Color defaultColor = tabbedPaneSolverSelection.getBackgroundAt(0);
	tabbedPaneSolverSelection.addChangeListener(e -> {
	    tabbedPaneSolverSelection.setBackgroundAt(tabbedPaneSolverSelection.getSelectedIndex(),
		    SolverImplConfigPanel.BACKGROUND_COLOR);
	    tabbedPaneSolverSelection.setBackgroundAt(1 - tabbedPaneSolverSelection.getSelectedIndex(), defaultColor);
	});
	parent.add(tabbedPaneSolverSelection, constraints);
    }

    private void addCommonSolverConfigPanel(JPanel parent, GridBagConstraints constraints) {
	commonSolverConfigs = new PropertyGroupConfigUi();
	commonSolverConfigs.addIntProperty("Board Size N", 1, 31, 16, 1);
	commonSolverConfigs.addIntProperty("Update Interval in ms", 0, 5000, 200);
	commonSolverConfigs.addIntProperty("Auto Save Interval in Percentage", 0, 100, 0, 5);
	parent.add(commonSolverConfigs.getUi(), constraints);
    }

    private void addControlPanel(JPanel parent, GridBagConstraints constraints) {
	var btnStart = new JButton("Start");
	btnStart.addActionListener(e -> {
	    AbstractSolver solver = ((SolverImplConfigPanel) tabbedPaneSolverSelection.getSelectedComponent())
		    .getConfiguredSolver();
	    solver.setN((int) commonSolverConfigs.getProperty("Board Size N"));
	    solver.setUpdateInterval((int) commonSolverConfigs.getProperty("Update Interval in ms"));
	    solver.onUpdate(onSolverProgress());
	    new Thread().ofVirtual().start(() -> solver.start());
	    // TODO: auto save percentage step ...
	});
	parent.add(btnStart, constraints);
    }

    private void addRightPanel() {
	JTextArea txtArea = new JTextArea();
	txtArea.setBackground(Color.BLACK);
	txtArea.setForeground(Color.GREEN);
	add(new JScrollPane(txtArea), BorderLayout.EAST);
    }

    private void addProgressBar() {
	progressBar = new JProgressBar(0, 100);
	progressBar.setStringPainted(true);
	progressBar.setValue(0);
	add(progressBar, BorderLayout.SOUTH);
    }
    
    private OnUpdateConsumer onSolverProgress() {
	return (progress, solutions, duration) -> {
	    EventQueue.invokeLater(() -> progressBar.setValue((int) (progress * 100)));
	    System.out.println(progress + " - " + solutions);
	};
    }
}
