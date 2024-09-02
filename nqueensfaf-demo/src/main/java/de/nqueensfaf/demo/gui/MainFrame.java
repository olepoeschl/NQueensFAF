package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;

import de.nqueensfaf.demo.gui.SolverModel.SolverListener;

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

	// left
	JPanel pnlConfigAndControl = new JPanel();
	pnlConfigAndControl.setLayout(new GridBagLayout());

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
	solverModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		configUiN.setEnabled(false);
	    }

	    @Override
	    public void solverFinished() {
		configUiN.setEnabled(true);
	    }
	});

	constraints.gridy++;
	constraints.insets.top = 5;
	var solverSelectionPanel = new SolverSelectionPanel(solverModel);
	pnlConfigAndControl.add(solverSelectionPanel, constraints);
	solverModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		solverSelectionPanel.setEnabled(false);
	    }

	    @Override
	    public void solverFinished() {
		solverSelectionPanel.setEnabled(true);
	    }
	});

	constraints.gridy++;
	constraints.weighty = 1;
	constraints.fill = GridBagConstraints.BOTH;
	var solverControlPanel = new SolverControlPanel(solverModel);
	pnlConfigAndControl.add(solverControlPanel, constraints);
	solverModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		solverControlPanel.setEnabled(false);
	    }

	    @Override
	    public void solverFinished() {
		solverControlPanel.setEnabled(true);
	    }
	});

	// right
	ResultPanel pnlResults = new ResultPanel(solverModel);

	// add split pane to container
	JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlConfigAndControl, pnlResults);
	mainSplitPane.setResizeWeight(0.5);
	mainSplitPane.setDividerLocation(0.5);
	add(mainSplitPane, BorderLayout.CENTER);

	// south
	addProgressBar();

	final Dimension preferredSize = new Dimension(500, 300);
	setPreferredSize(preferredSize);
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	setLocation(screenSize.width / 2 - preferredSize.width / 2, 
		screenSize.height / 2 - preferredSize.height / 2);
	pack();
	setVisible(true);
    }

    private void addProgressBar() {
	JProgressBar progressBar = new JProgressBar(0, 100);
	progressBar.setStringPainted(true);
	progressBar.setValue(0);
	add(progressBar, BorderLayout.SOUTH);

	solverModel.addPropertyChangeListener("progress", e -> {
	    progressBar.setValue((int) (((float) e.getNewValue()) * 100));
	});
    }
}
