package de.nqueensfaf.demo.gui;

import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import de.nqueensfaf.demo.gui.util.QuickGBC;

import static de.nqueensfaf.demo.gui.util.QuickGBC.*;

class ResultPanel extends JPanel {
    
    private static final Font highlightFont = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    private static final Font highlightCaptionFont = new Font(Font.MONOSPACED, Font.PLAIN, 16);
	
    private final SolverModel solverModel;
    
    ResultPanel(SolverModel solverModel) {
	this.solverModel = solverModel;
	
	setLayout(new GridBagLayout());
	initUi();
    }
    
    private void initUi() {
	JLabel lblSolutions = new JLabel("0");
	lblSolutions.setFont(highlightFont);
	solverModel.addPropertyChangeListener("solutions", e -> {
	    lblSolutions.setText(getSolutionsPrettyString((long) e.getNewValue()));
	});
	
	JLabel lblSolutionsCaption = new JLabel("solutions");
	lblSolutionsCaption.setFont(highlightCaptionFont);
	
	JLabel lblDuration = new JLabel("00.000");
	lblDuration.setFont(highlightFont);
	solverModel.addPropertyChangeListener("duration", e -> {
	    lblDuration.setText(getDurationPrettyString((long) e.getNewValue()));
	});
	
	add(new JLabel("A total of"), new QuickGBC(0, 0).anchor(ANCHOR_CENTER));
	add(lblSolutions, new QuickGBC(0, 1).anchor(ANCHOR_CENTER).insets(8, 0, 0, 0));
	add(lblSolutionsCaption, new QuickGBC(0, 2).anchor(ANCHOR_CENTER).insets(0, 0, 8, 0));
	add(new JLabel("were found"), new QuickGBC(0, 3).anchor(ANCHOR_CENTER));
	add(new JLabel("in"), new QuickGBC(0, 4).anchor(ANCHOR_CENTER));
	add(lblDuration, new QuickGBC(0, 5).anchor(ANCHOR_CENTER).insets(8, 0, 8, 0));
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
