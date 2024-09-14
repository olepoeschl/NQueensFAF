package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

import de.nqueensfaf.demo.Main;
import de.nqueensfaf.demo.gui.MainModel.SolverListener;
import de.nqueensfaf.demo.gui.util.Dialog;
import de.nqueensfaf.demo.gui.util.QuickGBC;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.IntProperty;

import static de.nqueensfaf.demo.gui.util.QuickGBC.*;

public class MainFrame extends JFrame {
    
    private final MainModel model = new MainModel();

    public MainFrame() {
	createAndShowUi();
	
	Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
	    Dialog.error(e.getMessage());
	    e.printStackTrace();
	});
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
	var resultsPanel = new ResultsPanel(model);
	return resultsPanel;
    }

    private JMenuBar createAndGetMenuBar() {
	final var openFileChooser = new JFileChooser();
	openFileChooser.setFileFilter(new FileFilter() {
	    @Override
	    public String getDescription() {
		return "NQueensFAF files";
	    }
	    @Override
	    public boolean accept(File f) {
		return f.isDirectory() || f.getName().endsWith(".faf");
	    }
	});
	openFileChooser.setMultiSelectionEnabled(false);
	openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

	final var saveFileChooser = new JFileChooser();
	saveFileChooser.setFileFilter(new FileFilter() {
	    @Override
	    public String getDescription() {
		return "NQueensFAF files";
	    }
	    @Override
	    public boolean accept(File f) {
		return f.isDirectory() || f.getName().endsWith(".faf");
	    }
	});
	saveFileChooser.setMultiSelectionEnabled(false);
	saveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	
	var openItem = new JMenuItem(new AbstractAction("Open") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		openFileChooser.showOpenDialog(MainFrame.this);
		File selectedFile = openFileChooser.getSelectedFile();
		
		if(selectedFile != null)
		    openFile(selectedFile.getAbsolutePath());
	    }
	});
	
	var saveItem = new JMenuItem(new AbstractAction("Save") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		saveFileChooser.showOpenDialog(MainFrame.this);
		File selectedFile = saveFileChooser.getSelectedFile();
		
		if(selectedFile != null) {
		    String targetPath = selectedFile.getAbsolutePath();
		    if(!targetPath.endsWith(".faf"))
			targetPath += ".faf";
		    saveToFile(targetPath);
		}
	    }
	});
	saveItem.setEnabled(false);
	
	var settingsItem = new JMenuItem(new AbstractAction("Settings") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("File.Settings");
	    }
	});
	var fileMenu = new JMenu("File");
	fileMenu.add(openItem);
	fileMenu.add(saveItem);
	fileMenu.add(settingsItem);
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		openItem.setEnabled(false);
		saveItem.setEnabled(true);
	    }
	    @Override
	    public void solverTerminated() {
		openItem.setEnabled(true);
		saveItem.setEnabled(false);
	    }
	});
	
	model.addPropertyChangeListener("fileOpened", e -> {
	    boolean fileOpened = (boolean) e.getNewValue();
	    openItem.setEnabled(!fileOpened);
	});

	var aboutMenu = new JMenu("About");
	aboutMenu.add(new JMenuItem(new AbstractAction("Website") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
		    try {
			desktop.browse(URI.create("https://github.com/olepoeschl/NQueensFAF"));
		    } catch (Exception ex) {
			Dialog.error("could not open link: " + ex.getMessage());
		    }
		}
	    }
	}));
	aboutMenu.add(new JMenuItem(new AbstractAction("Version") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Dialog.info("<html>Version: <i>" + Main.VERSION + "</i><br>Version Date: <i>" + Main.VERSION_DATE + "</i>");
	    }
	}));

	var menuBar = new JMenuBar();
	menuBar.add(fileMenu);
	menuBar.add(aboutMenu);
	
	return menuBar;
    }

    private JPanel createAndGetNConfigPanel() {
	var nConfigUi = new PropertyGroupConfigUi();
	nConfigUi.addIntProperty("n", "Board Size N", 1, 31, model.getN(), 1);
	nConfigUi.addPropertyChangeListener("n", e -> model.setN((int) e.getNewValue()));
	
	model.addPropertyChangeListener("fileOpened", e -> {
	    boolean fileOpened = (boolean) e.getNewValue();
	    nConfigUi.setEnabled(!fileOpened);
	    
	    if(fileOpened)
		((IntProperty) nConfigUi.getProperty("n")).setValue(model.getN());
	});
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		nConfigUi.setEnabled(false);
	    }
	    @Override
	    public void solverTerminated() {
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
	    
	    var solverImplWithConfig = ((SolverImplConfigPanel) solverSelectionPanel.getSelectedComponent()).getModel();
	    model.setSelectedSolverImplWithConfig(solverImplWithConfig);
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

	model.addPropertyChangeListener("fileOpened", e -> {
	    boolean fileOpened = (boolean) e.getNewValue();
	    for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
		if(i == solverSelectionPanel.getSelectedIndex())
		    continue;
		solverSelectionPanel.setEnabledAt(i, !fileOpened);
	    }
	});
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		((JPanel) solverSelectionPanel.getSelectedComponent()).setEnabled(false);
		for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
		    if(i == solverSelectionPanel.getSelectedIndex())
			continue;
		    solverSelectionPanel.setEnabledAt(i, false);
		}
	    }
	    @Override
	    public void solverTerminated() {
		((JPanel) solverSelectionPanel.getSelectedComponent()).setEnabled(true);
		for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
		    if(i == solverSelectionPanel.getSelectedIndex())
			continue;
		    solverSelectionPanel.setEnabledAt(i, true);
		}
	    }
	});
	
	return solverSelectionPanel;
    }
    
    private JPanel createAndGetSolverControlPanel() {
	var startButton = new JButton("Start");
	startButton.addActionListener(e -> model.startSolver());
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		startButton.setEnabled(false);
	    }
	    @Override
	    public void solverTerminated() {
		startButton.setEnabled(true);
	    }
	});

	var solverControlPanel = new JPanel(new GridBagLayout());
	solverControlPanel.add(startButton, new QuickGBC(0, 0).weight(1, 1).fill());
	
	return solverControlPanel;
    }    
    
    private JProgressBar createAndGetProgressBar() {
	var progressBar = new JProgressBar(0, 100);
	progressBar.setStringPainted(true);
	progressBar.setString("0,000 %");
	progressBar.setValue(0);

	model.addPropertyChangeListener("progress", e -> {
	    float progress = ((float) e.getNewValue()) * 100;
	    progressBar.setValue((int) progress);
	    progressBar.setString(String.format("%3.3f %%", progress));
	});
	
	return progressBar;
    }

    private void openFile(String path) {
	try {
	    model.openFile(path);
	} catch (IOException e) {
	    Dialog.error("could not open file: " + e.getMessage());
	}
    }
    
    private void saveToFile(String path) {
	try {
	    model.saveToFile(path);
	} catch (IOException e) {
	    Dialog.error("could not save to file: " + e.getMessage());
	}
    }
}
