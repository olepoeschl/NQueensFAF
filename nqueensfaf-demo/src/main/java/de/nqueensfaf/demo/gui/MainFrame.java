package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.SolverModel.SolverListener;
import de.nqueensfaf.demo.gui.util.Dialog;
import de.nqueensfaf.demo.gui.util.QuickGBC;

import static de.nqueensfaf.demo.gui.util.QuickGBC.*;

public class MainFrame extends JFrame {

    private final SolverModel solverModel = new SolverModel();

    public MainFrame() {
	createAndShowUi();
    }

    private void createAndShowUi() {
	// main container
	var container = new JPanel(new BorderLayout(10, 10));
	container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setContentPane(container);

	// MenuBar
	setJMenuBar(createAndGetMenuBar());

	// solver configs and controls
	var configAndControlPanel = new JPanel(new GridBagLayout());
	
	var nConfigPanel = createAndGetNConfigPanel();
	var solverSelectionPanel = createAndGetSolverSelectionPanel();
	var solverControlPanel = createAndGetSolverControlPanel();
	
	configAndControlPanel.add(nConfigPanel, new QuickGBC(0, 0).weight(1, 0).anchor(ANCHOR_NORTH).fillx());
	configAndControlPanel.add(solverSelectionPanel, new QuickGBC(0, 1).weight(1, 0.5).anchor(ANCHOR_NORTH).fill().top(5));
	configAndControlPanel.add(solverControlPanel, new QuickGBC(0, 2).weight(1, 0.5).fill().top(5));

	// solver results
	var resultsPanel = createAndGetResultsPanel();

	// display as configAndControlPanel and resultsPanel in split pane
	var mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configAndControlPanel, resultsPanel);
	mainSplitPane.setResizeWeight(0.5);

	// progress bar
	var progressBar = createAndGetProgressBar();

	// add all initialized components to main container
	add(mainSplitPane, BorderLayout.CENTER);
	add(progressBar, BorderLayout.SOUTH);
	
	// finalize frame initialization
	pack();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	setLocation(screenSize.width / 2 - getPreferredSize().width / 2, screenSize.height / 2 - getPreferredSize().height / 2);

	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	setVisible(true);
	resultsPanel.requestFocus();
    }

    private JPanel createAndGetResultsPanel() {
	var resultsPanel = new ResultsPanel(solverModel);
	return resultsPanel;
    }

    private JMenuBar createAndGetMenuBar() {
	var fileMenu = new JMenu("File");
	fileMenu.add(new JMenuItem(new AbstractAction("Open") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("File.Open");
	    }
	}));
	fileMenu.add(new JMenuItem(new AbstractAction("Save") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("File.Save");
	    }
	}));
	fileMenu.add(new JMenuItem(new AbstractAction("Settings") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("File.Settings");
	    }
	}));

	var aboutMenu = new JMenu("About");
	aboutMenu.add(new JMenuItem(new AbstractAction("Website") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("About.Website");
	    }
	}));
	aboutMenu.add(new JMenuItem(new AbstractAction("Version") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("About.Version");
	    }
	}));

	var menuBar = new JMenuBar();
	menuBar.add(fileMenu);
	menuBar.add(aboutMenu);
	
	return menuBar;
    }

    private JPanel createAndGetNConfigPanel() {
	var nConfigUi = new PropertyGroupConfigUi();
	nConfigUi.addIntProperty("n", "Board Size N", 1, 31, solverModel.getN(), 1);
	nConfigUi.addPropertyChangeListener("n", e -> solverModel.setN((int) e.getNewValue()));
	
	solverModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		nConfigUi.setEnabled(false);
	    }

	    @Override
	    public void solverFinished() {
		nConfigUi.setEnabled(true);
	    }
	});
	
	return nConfigUi.getUi();
    }
    
    private JTabbedPane createAndGetSolverSelectionPanel() {
	var solverSelectionPanel = new JTabbedPane();
	
	final Color tabColor = new Color(235, 235, 235);
	final Color systemDefaultTabColor = solverSelectionPanel.getBackground();
	solverSelectionPanel.addChangeListener(e -> {
	    for(int i = 0; i < solverSelectionPanel.getTabCount(); i++)
		solverSelectionPanel.setBackgroundAt(i, systemDefaultTabColor);
	    solverSelectionPanel.setBackgroundAt(solverSelectionPanel.getSelectedIndex(), tabColor);
	    
	    var solverConfig = ((SolverImplConfigPanel) solverSelectionPanel.getSelectedComponent()).getModel();
	    solverModel.setSelectedSolverConfig(solverConfig);
	    solverModel.setSelectedSolver(solverConfig.getConfiguredSolver());
	});

	solverModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		for(var component : ((JPanel) solverSelectionPanel.getSelectedComponent()).getComponents())
		    component.setEnabled(false);
		for(int i = 0; i < solverSelectionPanel.getTabCount(); i++)
		    if(i != solverSelectionPanel.getSelectedIndex())
			solverSelectionPanel.setEnabledAt(i, false);
	    }
	    
	    @Override
	    public void solverFinished() {
		for(var component : ((JPanel) solverSelectionPanel.getSelectedComponent()).getComponents())
		    component.setEnabled(true);
		for(int i = 0; i < solverSelectionPanel.getTabCount(); i++)
		    if(i != solverSelectionPanel.getSelectedIndex())
			solverSelectionPanel.setEnabledAt(i, true);
	    }
	});
	
	// add Solver implementations' tabs
	var cpuPanel = new CpuSolverConfigPanel();
	var gpuPanel = new GpuSolverConfigPanel();

	solverSelectionPanel.addTab("CPU", cpuPanel);
	solverSelectionPanel.addTab("GPU", gpuPanel);
	
	for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
	    var component = solverSelectionPanel.getComponentAt(i);
	    component.setBackground(tabColor);
	    ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
	}
	
	return solverSelectionPanel;
    }
    
    private JPanel createAndGetSolverControlPanel() {
	var startButton = new JButton("Start");
	startButton.addActionListener(e -> startSolver());
	
	solverModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		startButton.setEnabled(false);
	    }
	    @Override
	    public void solverFinished() {
		startButton.setEnabled(true);
	    }
	});

	var solverControlPanel = new JPanel(new GridBagLayout());
	solverControlPanel.add(startButton, new QuickGBC(0, 0).weight(1, 1).fill());
	
	return solverControlPanel;
    }

    private void startSolver() {
	solverModel.applyCallbacks();
	var solver = solverModel.getSelectedSolver();
	
	solver.setN(solverModel.getN());
	solver.setUpdateInterval(100);
	
	String errorMessage = solverModel.getSelectedSolverConfig().checkValid();
	if(errorMessage.length() > 0) {
	    Dialog.error(errorMessage);
	    return;
	}

	Thread.ofVirtual().start(() -> solverModel.startSymSolver());
	Thread.ofVirtual().start(() -> {
	    try {
		solver.start();
	    } catch (Exception e) {
		Dialog.error(e.getMessage());
	    }
	});
    }
    
    private JProgressBar createAndGetProgressBar() {
	var progressBar = new JProgressBar(0, 100);
	progressBar.setStringPainted(true);
	progressBar.setString("0,000 %");
	progressBar.setValue(0);

	solverModel.addPropertyChangeListener("progress", e -> {
	    float progress = ((float) e.getNewValue()) * 100;
	    progressBar.setValue((int) progress);
	    progressBar.setString(String.format("%3.3f %%", progress));
	});
	
	return progressBar;
    }
}
