package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
	JPanel pnlResults = new JPanel();
	pnlResults.setLayout(new BoxLayout(pnlResults, BoxLayout.Y_AXIS));

	final Font labelFont = new Font("Serif", Font.PLAIN, 14);
	final Font highlightFont = new Font("Arial", Font.ITALIC, 20);
	final Font highlightCaptionFont = new Font("Arial", Font.ITALIC, 16);

	JLabel lblAboveSolutions = new JLabel("A total of");
	lblAboveSolutions.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblAboveSolutions.setFont(labelFont);
	pnlResults.add(lblAboveSolutions);

	JLabel lblSolutions = new JLabel("0");
	lblSolutions.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
	lblSolutions.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblSolutions.setFont(highlightFont);
	solverModel.addPropertyChangeListener("solutions", e -> {
	    lblSolutions.setText(Long.toString((long) e.getNewValue()));
	});
	pnlResults.add(lblSolutions);

	JLabel lblSolutionsCaption = new JLabel("solutions");
	lblSolutionsCaption.setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 0));
	lblSolutionsCaption.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblSolutionsCaption.setFont(highlightCaptionFont);
	pnlResults.add(lblSolutionsCaption);

	JLabel lblBelowSolutions = new JLabel("was found");
	lblBelowSolutions.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblBelowSolutions.setFont(labelFont);
	pnlResults.add(lblBelowSolutions);

	JLabel lblAboveDuration = new JLabel("in");
	lblAboveDuration.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
	lblAboveDuration.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblAboveDuration.setFont(labelFont);
	pnlResults.add(lblAboveDuration);

	JLabel lblDuration = new JLabel("00:00:00.000");
	lblSolutions.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
	lblDuration.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblDuration.setFont(highlightFont);
	solverModel.addPropertyChangeListener("duration", e -> {
	    lblDuration.setText(getDurationPrettyString((long) e.getNewValue()));
	});
	pnlResults.add(lblDuration);

	JLabel lblDurationCaption = new JLabel("(HH:mm:ss.SSS)");
	lblDurationCaption.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblDurationCaption.setFont(highlightCaptionFont);
	pnlResults.add(lblDurationCaption);

	// add split pane to container
	JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlConfigAndControl, pnlResults);
	mainSplitPane.setResizeWeight(0.5);
	add(mainSplitPane, BorderLayout.CENTER);

	// south
	addProgressBar();

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

    private static String getDurationPrettyString(long time) {
	long h = time / 1000 / 60 / 60;
	long m = time / 1000 / 60 % 60;
	long s = time / 1000 % 60;
	long ms = time % 1000;

	String strh, strm, strs, strms;
	// hours
	if (h == 0) {
	    strh = "00";
	} else if ((h + "").toString().length() == 3) {
	    strh = "" + h;
	} else if ((h + "").toString().length() == 2) {
	    strh = "0" + h;
	} else {
	    strh = "00" + h;
	}
	// minutes
	if ((m + "").toString().length() == 2) {
	    strm = "" + m;
	} else {
	    strm = "0" + m;
	}
	// seconds
	if ((s + "").toString().length() == 2) {
	    strs = "" + s;
	} else {
	    strs = "0" + s;
	}
	// milliseconds
	if ((ms + "").toString().length() == 3) {
	    strms = "" + ms;
	} else if ((ms + "").toString().length() == 2) {
	    strms = "0" + ms;
	} else {
	    strms = "00" + ms;
	}

	return strh + ":" + strm + ":" + strs + "." + strms;
    }
}
