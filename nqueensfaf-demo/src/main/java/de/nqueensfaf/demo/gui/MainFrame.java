package de.nqueensfaf.demo.gui;

import static de.nqueensfaf.demo.gui.QuickGBC.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import de.nqueensfaf.demo.gui.HistoryFrame.HistoryEntry;
import de.nqueensfaf.demo.gui.MainModel.SolverListener;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.IntProperty;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    
    static final Color ACCENT_COLOR = new Color(235, 235, 235);
    static final Font HIGHLIGHT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    static final Font CAPTION_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    
    private final MainModel model = new MainModel();
    
    private HistoryFrame historyFrame;
    private RecordsFrame recordsFrame;
    
    public MainFrame() {
	createAndShowUi();
	
	Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
	    Utils.error(this != null ? this : null, e.getMessage());
	    e.printStackTrace();
	});
    }

    private void createAndShowUi() {
	// main container
	var container = new JPanel(new BorderLayout(10, 10));
	container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setContentPane(container);
	
	// init glass pane used for greying out the content
	setGlassPane(createGlassPane());

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

	// initialize other displayable frames
	initHistoryFrame();
	initRecordsFrame();
	
	// finalize frame initialization
	pack();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	setLocation(screenSize.width / 2 - getPreferredSize().width / 2, screenSize.height / 2 - getPreferredSize().height / 2);

	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	setVisible(true);
	setSize((int) (getWidth() * 1.2), (int) (getHeight() * 1.2));
	mainSplitPane.setDividerLocation(0.6);
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
	
	var resetItem = new JMenuItem(new AbstractAction("Reset") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		model.reset();
	    }
	});
	
	var settingsItem = new JMenuItem(new AbstractAction("Settings") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		var settingsDialog = new SettingsDialog(MainFrame.this, model);
		settingsDialog.setVisible(true);
	    }
	});
	
	var fileMenu = new JMenu("File");
	fileMenu.add(openItem);
	fileMenu.add(saveItem);
	fileMenu.add(resetItem);
	fileMenu.add(settingsItem);
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		openItem.setEnabled(false);
		saveItem.setEnabled(true);
		resetItem.setEnabled(false);
		settingsItem.setEnabled(false);
	    }
	    
	    @Override
	    public void solverTerminated() {
		reset();
	    }
	    
	    @Override
	    public void solverFileOpened() {
		openItem.setEnabled(false);
	    }
	    
	    @Override
	    public void solverReset() {
		reset();
	    }
	    
	    private void reset() {
		openItem.setEnabled(true);
		saveItem.setEnabled(false);
		resetItem.setEnabled(true);
		settingsItem.setEnabled(true);
	    }
	});

	var historyItem = new JMenuItem(new AbstractAction("History") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		if(historyFrame.isVisible()) {
		    historyFrame.setExtendedState(JFrame.NORMAL);
		    historyFrame.toFront();
		    historyFrame.repaint();
		} else
		    historyFrame.setVisible(true);
	    }
	});
	
	var recordsItem = new JMenuItem(new AbstractAction("Records") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		if(recordsFrame.isVisible()) {
		    recordsFrame.setExtendedState(JFrame.NORMAL);
		    recordsFrame.toFront();
		    recordsFrame.repaint();
		} else {
		    recordsFrame.setN(model.getN());
		    recordsFrame.setVisible(true);
		}
	    } 
	});
	
	var statsMenu = new JMenu("Stats");
	statsMenu.add(historyItem);
	statsMenu.add(recordsItem);
	
	var aboutMenu = new JMenu("About");
	aboutMenu.add(new JMenuItem(new AbstractAction("Website") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
		    try {
			desktop.browse(URI.create("https://github.com/olepoeschl/NQueensFAF"));
		    } catch (Exception ex) {
			Utils.error(MainFrame.this, "could not open link: " + ex.getMessage());
		    }
		}
	    }
	}));
	aboutMenu.add(new JMenuItem(new AbstractAction("Version") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Utils.info(MainFrame.this, "<html>Version: <i>" + Main.VERSION + "</i><br>Version Date: <i>" + Main.VERSION_DATE + "</i>", "Version");
	    }
	}));

	var menuBar = new JMenuBar();
	menuBar.add(fileMenu);
	menuBar.add(statsMenu);
	menuBar.add(aboutMenu);
	
	return menuBar;
    }

    private JPanel createAndGetNConfigPanel() {
	var nConfigUi = new PropertyGroupConfigUi();
	nConfigUi.addIntProperty("n", "Board Size N", 1, 31, model.getN(), 1);
	nConfigUi.addPropertyChangeListener("n", e -> model.setN((int) e.getNewValue()));
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		nConfigUi.setEnabled(false);
	    }
	    
	    @Override
	    public void solverTerminated() {
		reset();
	    }
	    
	    @Override
	    public void solverFileOpened() {
		nConfigUi.setEnabled(false);
		((IntProperty) nConfigUi.getProperty("n")).setValue(model.getN());
	    }
	    
	    @Override
	    public void solverReset() {
		reset();
	    }
	    
	    private void reset() {
		nConfigUi.setEnabled(true);
	    }
	});
	
	return nConfigUi.getUi();
    }
    
    private JTabbedPane createAndGetSolverSelectionPanel() {
	var solverSelectionPanel = new JTabbedPane();
	
	final Color systemDefaultTabColor = solverSelectionPanel.getBackground();
	solverSelectionPanel.addChangeListener(e -> {
	    for(int i = 0; i < solverSelectionPanel.getTabCount(); i++)
		solverSelectionPanel.setBackgroundAt(i, systemDefaultTabColor);
	    solverSelectionPanel.setBackgroundAt(solverSelectionPanel.getSelectedIndex(), ACCENT_COLOR);
	    
	    var solverImplWithConfig = ((SolverImplConfigPanel) solverSelectionPanel.getSelectedComponent()).getModel();
	    model.setSelectedSolverImplWithConfig(solverImplWithConfig);
	});
	
	// add Solver implementations' tabs
	var cpuPanel = new CpuSolverConfigPanel();
	var gpuPanel = new GpuSolverConfigPanel();

	solverSelectionPanel.addTab(cpuPanel.getModel().getName(), cpuPanel);
	solverSelectionPanel.addTab(gpuPanel.getModel().getName(), gpuPanel);
	
	for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
	    var component = solverSelectionPanel.getComponentAt(i);
	    component.setBackground(ACCENT_COLOR);
	    ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
	}
	
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
		reset();
	    }
	    
	    @Override
	    public void solverFileOpened() {
		for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
		    if(i == solverSelectionPanel.getSelectedIndex())
			continue;
		    solverSelectionPanel.setEnabledAt(i, false);
		}
	    }
	    
	    @Override
	    public void solverReset() {
		reset();
	    }
	    
	    private void reset() {
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
	startButton.addActionListener(e -> {
	    Thread.ofVirtual().start(() -> {
		try {
		    model.startSolver();
		} catch (Exception ex) {
		    Utils.error(this, ex.getMessage());
		}
	    });
	});
	
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
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		if(!model.isFileOpened())
		    progressBar.setValue(0);
	    }
	    @Override
	    public void solverTerminated() {
		progressBar.setValue((int) (model.getProgress() * 100));
	    }
	});
	
	return progressBar;
    }

    private void openFile(String path) {
	Utils.loadingCursor(this);
	try {
	    model.openFile(path);
	} catch (IOException e) {
	    Utils.error(this, "could not open file: " + e.getMessage());
	}
	Utils.defaultCursor(this);
    }
    
    private void saveToFile(String path) {
	Utils.loadingCursor(this);
	try {
	    model.saveToFile(path);
	} catch (IOException e) {
	    Utils.error(this, "could not save to file: " + e.getMessage());
	}
	Utils.defaultCursor(this);
    }
    
    private JPanel createGlassPane() {
	var glassPane = new JPanel() {
	    @Override
	    public void paintComponent(Graphics g) {
		g.setColor(new Color(0, 0, 0, 140));
		g.fillRect(0, 0, getWidth(), getHeight());
	    }
	};
	glassPane.addMouseListener(new MouseAdapter() {
	    @Override
	    public void mousePressed(MouseEvent e) {
		e.consume();
	    }
	});
	glassPane.setOpaque(false);
	return glassPane;
    }

    private void initHistoryFrame() {
	historyFrame = new HistoryFrame();
	historyFrame.setSize(350, 250);
	historyFrame.setLocationRelativeTo(this);

	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
	    }
	    public void solverTerminated() {
	    }
	    @Override
	    public void solverFinished() {
		var entry = new HistoryEntry(model.getN(), model.getSelectedSolverImplWithConfig().toString(), model.getDuration());
		historyFrame.addEntry(entry);
	    }
	});
    }

    private void initRecordsFrame() {
	final String path = Records.DEFAULT_PATH;
	final Records records = new Records();
	if(new File(path).exists())
	    try {
		records.open(path);
	    } catch (Exception e) {
		Utils.error(this, "could not load saved records: " + e.getMessage());
	    }
	
	recordsFrame = new RecordsFrame(records, model.getN());
	recordsFrame.setLocationRelativeTo(this);
	
	model.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
	    }
	    @Override
	    public void solverTerminated() {
	    }
	    @Override
	    public void solverFinished() {
		var solver = model.getSelectedSolverImplWithConfig().getSolver();
		if(records.isNewRecord(solver.getDuration(), solver.getN(), model.getSelectedSolverImplWithConfig().toString()))
		    records.putRecord(solver.getDuration(), solver.getN(), model.getSelectedSolverImplWithConfig().toString());
	    }
	});

	// make records persistent on application exit
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    @Override
	    public void run() {
		try {
		    records.save(path);
		} catch (IOException e) {
		    Utils.error(null, "could not save records: " + e.getMessage());
		}
	    }
	});
    }
}