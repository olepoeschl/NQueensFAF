package de.nqueensfaf.demo.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ResultPanel extends JPanel {
    
    private static final Font labelFont = new Font("Serif", Font.PLAIN, 14);
    private static final Font highlightFont = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    private static final Font highlightCaptionFont = new Font(Font.MONOSPACED, Font.PLAIN, 16);
	
    private final SolverModel solverModel;
    
    private JLabel lblSolutions;
    private JLabel lblDuration;
    
    ResultPanel(SolverModel solverModel) {
	this.solverModel = solverModel;
	
	setLayout(new GridBagLayout());
	initUi();
    }
    
    private void initUi() {
	var constraints = new GridBagConstraints();
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	
	JLabel lblAboveSolutions = new JLabel("A total of");
	lblAboveSolutions.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblAboveSolutions.setFont(labelFont);
	add(lblAboveSolutions, constraints);

	lblSolutions = new JLabel("0");
	lblSolutions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
	lblSolutions.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblSolutions.setFont(highlightFont);
	solverModel.addPropertyChangeListener("solutions", e -> {
	    lblSolutions.setText(Long.toString((long) e.getNewValue()));
	});
	add(lblSolutions, constraints);

	JLabel lblSolutionsCaption = new JLabel("solutions");
	lblSolutionsCaption.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
	lblSolutionsCaption.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblSolutionsCaption.setFont(highlightCaptionFont);
	add(lblSolutionsCaption, constraints);

	JLabel lblBelowSolutions = new JLabel("was found");
	lblBelowSolutions.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblBelowSolutions.setFont(labelFont);
	add(lblBelowSolutions, constraints);

	JLabel lblAboveDuration = new JLabel("in");
	lblAboveDuration.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblAboveDuration.setFont(labelFont);
	add(lblAboveDuration, constraints);

	lblDuration = new JLabel("00:00:00.000");
	lblSolutions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
	lblDuration.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblDuration.setFont(highlightFont);
	solverModel.addPropertyChangeListener("duration", e -> {
	    lblDuration.setText(getDurationPrettyString((long) e.getNewValue()));
	});
	add(lblDuration, constraints);

	JLabel lblDurationCaption = new JLabel("(HH:mm:ss.SSS)");
	lblDurationCaption.setAlignmentX(JLabel.CENTER_ALIGNMENT);
	lblDurationCaption.setFont(highlightCaptionFont);
	add(lblDurationCaption, constraints);
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
