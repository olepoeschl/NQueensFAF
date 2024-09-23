package de.nqueensfaf.demo.gui;

import static de.nqueensfaf.demo.gui.QuickGBC.*;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;

import de.nqueensfaf.demo.gui.MainModel.SolverListener;

@SuppressWarnings("serial")
class ResultsPanel extends JPanel {

    private final MainModel mainModel;
    
    private JLabel lblN;
    private JLabel lblSolverName;
    private JLabel lblDuration;
    private JLabel lblDurationCaption;
    private JLabel lblSolutions;
    private JLabel lblUniqueSolutions;
    
    ResultsPanel(MainModel mainModel) {
	this.mainModel = mainModel;
	initUi();
    }
    
    private void initUi() {
	// used config
	lblN = new JLabel("...");
	lblN.setFont(MainFrame.CAPTION_FONT);
	lblN.setHorizontalAlignment(JLabel.RIGHT);
	
	lblSolverName = new JLabel();
	lblSolverName.setFont(MainFrame.CAPTION_FONT);
	lblSolverName.setHorizontalAlignment(JLabel.LEFT);
	
	var usedConfigs = new JPanel(new GridBagLayout());
	usedConfigs.setBorder(new CompoundBorder(
		BorderFactory.createLineBorder(MainFrame.ACCENT_COLOR, 10),
		BorderFactory.createEmptyBorder(2, 5, 2, 5)));
	usedConfigs.add(lblN, new QuickGBC(0, 0).weight(1, 0).fillx().anchor(QuickGBC.ANCHOR_CENTER));
	usedConfigs.add(lblSolverName, new QuickGBC(1, 0).weight(1, 0).fillx().anchor(QuickGBC.ANCHOR_CENTER));
	
	// result labels
	lblDuration = new JLabel("00.000");
	lblDuration.setFont(MainFrame.HIGHLIGHT_FONT);
	
	lblDurationCaption = new JLabel("seconds");
	lblDurationCaption.setFont(MainFrame.CAPTION_FONT);

	lblSolutions = new JLabel("0");
	lblSolutions.setFont(MainFrame.HIGHLIGHT_FONT);
	
	JLabel lblSolutionsCaption = new JLabel("solutions");
	lblSolutionsCaption.setFont(MainFrame.CAPTION_FONT);
	
	lblUniqueSolutions = new JLabel("0");
	lblUniqueSolutions.setFont(MainFrame.HIGHLIGHT_FONT);
	
	JLabel lblUniqueSolutionsCaption = new JLabel("unique solutions");
	lblUniqueSolutionsCaption.setFont(MainFrame.CAPTION_FONT);

	// update labels from model listeners
	mainModel.addPropertyChangeListener("duration", e -> {
	    updateDuration((long) e.getNewValue());
	});
	mainModel.addPropertyChangeListener("solutions", e -> {
	    updateSolutions((long) e.getNewValue());
	});
	mainModel.addPropertyChangeListener("uniqueSolutions", e -> {
	    updateUniqueSolutions((long) e.getNewValue());
	});
	
	// result panels
	JPanel pnlDuration = new JPanel(new GridBagLayout());
	JPanel pnlSolutions = new JPanel(new GridBagLayout());
	JPanel pnlUniqueSolutions = new JPanel(new GridBagLayout());

	pnlDuration.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	pnlSolutions.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	pnlUniqueSolutions.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	
	pnlDuration.add(lblDuration, new QuickGBC(0, 0).anchor(ANCHOR_CENTER));
	pnlDuration.add(lblDurationCaption, new QuickGBC(0, 1).anchor(ANCHOR_CENTER));
	pnlSolutions.add(lblSolutions, new QuickGBC(0, 0).anchor(ANCHOR_CENTER));
	pnlSolutions.add(lblSolutionsCaption, new QuickGBC(0, 1).anchor(ANCHOR_CENTER));
	pnlUniqueSolutions.add(lblUniqueSolutions, new QuickGBC(0, 0).anchor(ANCHOR_CENTER));
	pnlUniqueSolutions.add(lblUniqueSolutionsCaption, new QuickGBC(0, 1).anchor(ANCHOR_CENTER));

	var resultsPanel = new JPanel(new GridLayout(3, 1, 0, 7));
	resultsPanel.setBorder(BorderFactory.createEmptyBorder(0, 7, 7, 7));
	resultsPanel.setBackground(MainFrame.ACCENT_COLOR);
	resultsPanel.add(pnlDuration);
	resultsPanel.add(pnlSolutions);
	resultsPanel.add(pnlUniqueSolutions);
	
	setLayout(new BorderLayout());
	add(usedConfigs, BorderLayout.NORTH);
	add(resultsPanel, BorderLayout.CENTER);
	
	mainModel.addSolverListener(new SolverListener() {
	    @Override
	    public void solverStarted() {
		updateUsedN(mainModel.getN());
		updateUsedSolverImplName(mainModel.getSelectedSolverImplWithConfig().getName());
		
		if(!mainModel.isFileOpened()) {
		    updateDuration(0);
		    updateSolutions(0);
		    updateUniqueSolutions(0);
		}
	    }
	    
	    @Override
	    public void solverTerminated() {
		updateDuration(mainModel.getDuration());
		final var solutions = mainModel.getSolutions();
		updateSolutions(solutions);
		updateUniqueSolutions(mainModel.getUniqueSolutions(solutions));
	    }
	});
    }
    
    private void updateUsedN(int n) {
	lblN.setText("N=" + n);
    }
    
    private void updateUsedSolverImplName(String solverImplName) {
	lblSolverName.setText("," + solverImplName);
    }
    
    private void updateDuration(long duration) {
	lblDuration.setText(getDurationUnitlessString(duration));
	lblDurationCaption.setText(getDurationUnitString(duration));
    }
    
    private void updateSolutions(long solutions) {
	lblSolutions.setText(getSolutionsString(solutions));
    }
    
    private void updateUniqueSolutions(long uniqueSolutions) {
	lblUniqueSolutions.setText(getSolutionsString(uniqueSolutions));
    }

    private static String getSolutionsString(long solutions) {
	StringBuilder sb = new StringBuilder(Long.toString(solutions));
	for (int i = sb.length() - 3; i >= 0; i -= 3) {
	    if (i <= 0)
		break;
	    sb.insert(i, ".");
	}
	return sb.toString();
    }

    static String getDurationUnitlessString(long time) {
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
	
	String durationStr;
	if(h > 0)
	    durationStr = strh + ":" + strm + ":" + strs + "." + strms;
	else if(m > 0)
	    durationStr = strm + ":" + strs + "." + strms;
	else
	    durationStr = strs + "." + strms;

	return durationStr.startsWith("0") ? durationStr.substring(1) : durationStr;
    }
    
    static String getDurationUnitString(long duration) {
	if(duration >= 60 * 60 * 1000)
	    return "hours";
	else if(duration >= 60 * 1000)
	    return "minutes";
	else
	    return "seconds";
    }
    
    static String getDurationString(long time) {
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
	
	String durationStr = strh + ":" + strm + ":" + strs + "." + strms;
	return durationStr;
    }
    
}
