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
    
    private JLabel lblN;
    private JLabel lblSolverName;
    private JLabel lblDuration;
    private JLabel lblDurationCaption;
    private JLabel lblSolutions;
    private JLabel lblUniqueSolutions;
    
    ResultsPanel() {
	createUi();
    }
    
    private void createUi() {
	// used config
	lblN = new JLabel("...");
	lblN.setFont(View.CAPTION_FONT);
	lblN.setHorizontalAlignment(JLabel.RIGHT);
	
	lblSolverName = new JLabel();
	lblSolverName.setFont(View.CAPTION_FONT);
	lblSolverName.setHorizontalAlignment(JLabel.LEFT);
	
	var usedConfigs = new JPanel(new GridBagLayout());
	usedConfigs.setBorder(new CompoundBorder(
		BorderFactory.createLineBorder(View.ACCENT_COLOR, 10),
		BorderFactory.createEmptyBorder(2, 5, 2, 5)));
	usedConfigs.add(lblN, new QuickGBC(0, 0).weight(1, 0).fillx().anchor(QuickGBC.ANCHOR_CENTER));
	usedConfigs.add(lblSolverName, new QuickGBC(1, 0).weight(1, 0).fillx().anchor(QuickGBC.ANCHOR_CENTER));
	
	// result labels
	lblDuration = new JLabel("00.000");
	lblDuration.setFont(View.HIGHLIGHT_FONT);
	
	lblDurationCaption = new JLabel("seconds");
	lblDurationCaption.setFont(View.CAPTION_FONT);

	lblSolutions = new JLabel("0");
	lblSolutions.setFont(View.HIGHLIGHT_FONT);
	
	JLabel lblSolutionsCaption = new JLabel("solutions");
	lblSolutionsCaption.setFont(View.CAPTION_FONT);
	
	lblUniqueSolutions = new JLabel("0");
	lblUniqueSolutions.setFont(View.HIGHLIGHT_FONT);
	
	JLabel lblUniqueSolutionsCaption = new JLabel("unique solutions");
	lblUniqueSolutionsCaption.setFont(View.CAPTION_FONT);

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
	resultsPanel.setBackground(View.ACCENT_COLOR);
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
		updateUsedSolverImplName(mainModel.getSelectedSolverExtension().getName());
		
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
    
    void updateUsedN(int n) {
	lblN.setText("N=" + n);
    }
    
    void updateUsedSolverImplName(String solverImplName) {
	lblSolverName.setText("," + solverImplName);
    }
    
    void updateDuration(long duration) {
	lblDuration.setText(Utils.getDurationUnitlessString(duration));
	lblDurationCaption.setText(Utils.getDurationUnitString(duration));
    }
    
    void updateSolutions(long solutions) {
	lblSolutions.setText(Utils.getSolutionsString(solutions));
    }
    
    void updateUniqueSolutions(long uniqueSolutions) {
	lblUniqueSolutions.setText(Utils.getSolutionsString(uniqueSolutions));
    }
}
