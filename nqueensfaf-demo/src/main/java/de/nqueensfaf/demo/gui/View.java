package de.nqueensfaf.demo.gui;

import static de.nqueensfaf.demo.gui.util.QuickGBC.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import de.nqueensfaf.demo.Main;
import de.nqueensfaf.demo.gui.Controller.SolverAdapter;
import de.nqueensfaf.demo.gui.HistoryFrame.HistoryEntry;
import de.nqueensfaf.demo.gui.extension.PropertyGroupConfigUi;
import de.nqueensfaf.demo.gui.extension.PropertyGroupConfigUi.IntProperty;
import de.nqueensfaf.demo.gui.util.QuickGBC;
import de.nqueensfaf.demo.gui.util.Utils;

@SuppressWarnings("serial")
public class View extends JFrame {
    
    static final Color ACCENT_COLOR = new Color(235, 235, 235);
    static final Font HIGHLIGHT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    static final Font CAPTION_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    
    private final Controller controller;
    private final Model model;
    
    private HistoryFrame historyFrame;
    private RecordsFrame recordsFrame;
    
    public View(Controller controller, Model model) {
	this.controller = controller;
	this.model = model;
	Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
	    error(this != null ? this : null, e.getMessage());
	    e.printStackTrace();
	});
    }

    public void createAndShowUi() {
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
	var solverExtensionToolBar = createSolverExtensionToolBar();
	var solverControlPanel = createAndGetSolverControlPanel();
	
	configAndControlPanel.add(nConfigPanel, new QuickGBC(0, 0).weight(1, 0).anchor(ANCHOR_NORTH).fillx());
	configAndControlPanel.add(solverSelectionPanel, new QuickGBC(0, 1).weight(1, 0.5).anchor(ANCHOR_NORTH).fill().top(5));
	configAndControlPanel.add(solverExtensionToolBar, new QuickGBC(0, 2).weight(0, 0).anchor(ANCHOR_NORTH));
	configAndControlPanel.add(solverControlPanel, new QuickGBC(0, 3).weight(1, 0.5).fill().top(5));

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

    private JPanel createAndGetResultsPanel() {
	var resultsPanel = new ResultsPanel();
	
	model.addPropertyChangeListener("duration", e -> {
	    EventQueue.invokeLater(() -> resultsPanel.updateDuration((long) e.getNewValue()));
	});
	model.addPropertyChangeListener("solutions", e -> {
	    EventQueue.invokeLater(() -> resultsPanel.updateSolutions((long) e.getNewValue()));
	});
	model.addPropertyChangeListener("uniqueSolutions", e -> {
	    EventQueue.invokeLater(() -> resultsPanel.updateUniqueSolutions((long) e.getNewValue()));
	});
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		EventQueue.invokeLater(() -> {
		    resultsPanel.updateUsedN(model.getN());
		    resultsPanel.updateUsedSolverImplName(model.getSelectedSolverExtension().getName());
		});
	    }
	});
	
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
		openFileChooser.showOpenDialog(View.this);
		File selectedFile = openFileChooser.getSelectedFile();
		
		if(selectedFile != null)
		    controller.restore(selectedFile);
	    }
	});
	
	var saveItem = new JMenuItem(new AbstractAction("Save") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		saveFileChooser.showOpenDialog(View.this);
		File selectedFile = saveFileChooser.getSelectedFile();
		
		if(selectedFile != null)
		    controller.manualSave(selectedFile);
	    }
	});
	saveItem.setEnabled(false);
	
	var resetItem = new JMenuItem(new AbstractAction("Reset") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		controller.reset();
	    }
	});
	
	var settingsItem = new JMenuItem(new AbstractAction("Settings") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		var settingsDialog = new SettingsDialog(View.this, model.getSettings().getUpdateInterval(), model.getAutoSaveInterval());
		settingsDialog.addPropertyChangeListener("updateInterval", ev -> model.getSettings().setUpdateInterval((int) ev.getNewValue()));
		settingsDialog.addPropertyChangeListener("autoSaveInterval", ev -> model.setAutoSaveInterval((int) ev.getNewValue()));
		settingsDialog.setVisible(true);
	    }
	});
	
	var fileMenu = new JMenu("File");
	fileMenu.add(openItem);
	fileMenu.add(saveItem);
	fileMenu.add(resetItem);
	fileMenu.add(settingsItem);
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		EventQueue.invokeLater(() -> {
		    openItem.setEnabled(false);
		    saveItem.setEnabled(true);
		    resetItem.setEnabled(false);
		    settingsItem.setEnabled(false);
		});
	    }
	    
	    @Override
	    public void solverTerminated() {
		EventQueue.invokeLater(() -> reset());
	    }
	    
	    @Override
	    public void solverRestored() {
		EventQueue.invokeLater(() -> openItem.setEnabled(false));
	    }
	    
	    @Override
	    public void solverReset() {
		EventQueue.invokeLater(() -> reset());
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
			error("could not open link: " + ex.getMessage());
		    }
		}
	    }
	}));
	aboutMenu.add(new JMenuItem(new AbstractAction("Version") {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		info("<html>Version: <i>" + Main.VERSION + "</i><br>Version Date: <i>" + Main.VERSION_DATE + "</i>", "Version");
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
	nConfigUi.getProperty("n").addChangeListener(e -> model.setN((int) e.getNewValue()));
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		EventQueue.invokeLater(() -> nConfigUi.setEnabled(false));
	    }
	    
	    @Override
	    public void solverTerminated() {
		EventQueue.invokeLater(() -> reset());
	    }
	    
	    @Override
	    public void solverRestored() {
		EventQueue.invokeLater(() -> {
		    nConfigUi.setEnabled(false);
		    ((IntProperty) nConfigUi.getProperty("n")).setValue(model.getN());
		});
	    }
	    
	    @Override
	    public void solverReset() {
		EventQueue.invokeLater(() -> reset());
	    }
	    
	    private void reset() {
		nConfigUi.setEnabled(true);
	    }
	});
	
	return nConfigUi;
    }
    
    private JTabbedPane createAndGetSolverSelectionPanel() {
	var solverSelectionPanel = new JTabbedPane();
	
	final Color systemDefaultTabColor = solverSelectionPanel.getBackground();
	solverSelectionPanel.addChangeListener(e -> {
	    for(int i = 0; i < solverSelectionPanel.getTabCount(); i++)
		solverSelectionPanel.setBackgroundAt(i, systemDefaultTabColor);
	    solverSelectionPanel.setBackgroundAt(solverSelectionPanel.getSelectedIndex(), ACCENT_COLOR);
	    model.setSelectedSolverExtension(solverSelectionPanel.getSelectedIndex());
	});
	
	for(var solverExtension : model.getSolverExtensions())
	    solverSelectionPanel.addTab(solverExtension.getName(), solverExtension.getConfigUi());
	
	for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
	    var component = solverSelectionPanel.getComponentAt(i);
	    component.setBackground(ACCENT_COLOR);
	    ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
	}
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		EventQueue.invokeLater(() -> {
		    solverSelectionPanel.getSelectedComponent().setEnabled(false);
		    for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
			if(i == solverSelectionPanel.getSelectedIndex())
			    continue;
			solverSelectionPanel.setEnabledAt(i, false);
		    }
		});
	    }
	    
	    @Override
	    public void solverTerminated() {
		EventQueue.invokeLater(() -> reset());
	    }
	    
	    @Override
	    public void solverRestored() {
		EventQueue.invokeLater(() -> {
		    for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
			if(i == solverSelectionPanel.getSelectedIndex())
			    continue;
			solverSelectionPanel.setEnabledAt(i, false);
		    }
		});
	    }
	    
	    @Override
	    public void solverReset() {
		EventQueue.invokeLater(() -> reset());
	    }
	    
	    private void reset() {
		solverSelectionPanel.getSelectedComponent().setEnabled(true);
		for(int i = 0; i < solverSelectionPanel.getTabCount(); i++) {
		    if(i == solverSelectionPanel.getSelectedIndex())
			continue;
		    solverSelectionPanel.setEnabledAt(i, true);
		}
	    }
	});
	
	return solverSelectionPanel;
    }
    
    private JToolBar createSolverExtensionToolBar() {
	var saveConfigBtn = new JButton(Utils.getSaveIcon());
	saveConfigBtn.addActionListener(e -> controller.saveCurrentSolverExtensionConfig(new File("")));
	
	var openConfigBtn = new JButton(Utils.getOpenIcon());
	openConfigBtn.addActionListener(e -> controller.loadSolverExtensionConfig(new File("")));
	
	var pasteConfigBtn = new JButton(Utils.getPasteIcon());
	pasteConfigBtn.addActionListener(e -> controller.pasteSolverExtensionConfig());
	
	var toolBar = new JToolBar();
	toolBar.setBackground(ACCENT_COLOR);
	
	toolBar.add(saveConfigBtn);
	toolBar.add(openConfigBtn);
	toolBar.add(pasteConfigBtn);
	
	return toolBar;
    }
    
    private JPanel createAndGetSolverControlPanel() {
	var startButton = new JButton("Start");
	startButton.addActionListener(e -> {
	    Thread.ofVirtual().start(() -> {
		try {
		    controller.start();
		} catch (Exception ex) {
		    error(ex.getMessage());
		}
	    });
	});
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		EventQueue.invokeLater(() -> startButton.setEnabled(false));
	    }
	    @Override
	    public void solverTerminated() {
		EventQueue.invokeLater(() -> startButton.setEnabled(true));
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
	    EventQueue.invokeLater(() -> {
		float progress = ((float) e.getNewValue()) * 100;
		progressBar.setValue((int) progress);
		progressBar.setString(String.format("%3.3f %%", progress));
	    });
	});
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		EventQueue.invokeLater(() -> {
		    if(!model.isRestored())
			progressBar.setValue(0);
		});
	    }
	    @Override
	    public void solverTerminated() {
		EventQueue.invokeLater(() -> progressBar.setValue((int) (model.getProgress() * 100)));
	    }
	});
	
	return progressBar;
    }

    private void initHistoryFrame() {
	historyFrame = new HistoryFrame();
	historyFrame.setSize(350, 250);
	historyFrame.setLocationRelativeTo(this);

	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverFinished() {
		EventQueue.invokeLater(() -> {
		    var entry = new HistoryEntry(model.getN(), model.getSelectedSolverExtension().getName(),
			    model.getSelectedSolverExtension().getSolver().getDuration());
		    historyFrame.addEntry(entry);
		});
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
		error("could not load saved records: " + e.getMessage());
	    }
	
	recordsFrame = new RecordsFrame(records, model.getN());
	recordsFrame.setLocationRelativeTo(this);
	
	controller.addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverFinished() {
		EventQueue.invokeLater(() -> {
		    var solver = model.getSelectedSolverExtension().getSolver();
		    if (records.isNewRecord(model.getSelectedSolverExtension().getSolver().getDuration(), solver.getN(),
			    model.getSelectedSolverExtension().getCurrentRecordCategory()))
			records.putRecord(model.getSelectedSolverExtension().getSolver().getDuration(), solver.getN(),
				model.getSelectedSolverExtension().getCurrentRecordCategory());
		});
	    }
	});

	// make records persistent on application exit
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    @Override
	    public void run() {
		try {
		    records.save(path);
		} catch (IOException e) {
		    error("could not save records: " + e.getMessage());
		}
	    }
	});
    }
    
    public void error(String message) {
	error(this, message);
    }
    
    public static void error(Component parent, String message) {
	JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void info(String message, String title) {
	JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}