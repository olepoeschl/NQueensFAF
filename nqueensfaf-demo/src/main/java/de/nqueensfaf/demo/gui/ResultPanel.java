package de.nqueensfaf.demo.gui;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import de.nqueensfaf.demo.gui.util.QuickGBC;

import static de.nqueensfaf.demo.gui.util.QuickGBC.*;

class ResultPanel extends JPanel {
    
    private static final Font highlightFont = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    private static final Font highlightCaptionFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);
	
    private final SolverModel solverModel;
    
    ResultPanel(SolverModel solverModel) {
	this.solverModel = solverModel;
	
	setLayout(new GridLayout(3, 1, 0, 5));
	initUi();
    }
    
    private void initUi() {
	// labels
	JLabel lblDuration = new JLabel("00.000");
	lblDuration.setFont(highlightFont);
	solverModel.addPropertyChangeListener("duration", e -> {
	    lblDuration.setText(getDurationPrettyString((long) e.getNewValue()));
	});
	
	JLabel lblDurationCaption = new JLabel("seconds");
	lblDurationCaption.setFont(highlightCaptionFont);
	solverModel.addPropertyChangeListener("duration", e -> {
	    String unit;
	    long duration = (long) e.getNewValue();
	    
	    if(duration >= 60 * 60 * 1000)
		unit = "hours";
	    else if(duration >= 60 * 1000)
		unit = "minutes";
	    else
		unit = "seconds";
	    
	    lblDurationCaption.setText(unit);
	});

	JLabel lblSolutions = new JLabel("0");
	lblSolutions.setFont(highlightFont);
	solverModel.addPropertyChangeListener("solutions", e -> {
	    lblSolutions.setText(getSolutionsPrettyString((long) e.getNewValue()));
	});
	
	JLabel lblSolutionsCaption = new JLabel("solutions");
	lblSolutionsCaption.setFont(highlightCaptionFont);
	
	JLabel lblUniqueSolutions = new JLabel("0");
	lblUniqueSolutions.setFont(highlightFont);
	solverModel.addPropertyChangeListener("unique_solutions", e -> {
	    lblUniqueSolutions.setText(getSolutionsPrettyString((long) e.getNewValue()));
	});
	
	JLabel lblUniqueSolutionsCaption = new JLabel("unique solutions");
	lblUniqueSolutionsCaption.setFont(highlightCaptionFont);

	// panels
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
	
	add(pnlDuration);
	add(pnlSolutions);
	add(pnlUniqueSolutions);
    }

    private static String getSolutionsPrettyString(long solutions) {
	StringBuilder sb = new StringBuilder(Long.toString(solutions));
	for (int i = sb.length() - 3; i >= 0; i -= 3) {
	    if (i <= 0)
		break;
	    sb.insert(i, ".");
	}
	return sb.toString();
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

	var builder = new StringBuilder();
	if(h > 0)
	    return strh + ":" + strm + ":" + strs + "." + strms;
	else if(m > 0)
	    return strm + ":" + strs + "." + strms;
	else
	    return strs + "." + strms;
    }
}
