package de.nqueensfaf.demo.gui;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class RecordsPanel extends JPanel {

    private final Records records;
    
    private int n = 16;
    
    private JLabel nLbl;
    private JPanel dataPanel = new JPanel(new GridBagLayout());
    
    public RecordsPanel(Records records) {
	this.records = records;
	createUi();
    }
    
    private void createUi() {
	setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setLayout(new GridBagLayout());
	
	// n configuration ui
	var nEqLbl = new JLabel("N = ");
	nEqLbl.setHorizontalAlignment(JLabel.CENTER);
	
	var prevNBtn = new JButton("<");
	prevNBtn.addActionListener(e -> {
	    int newN = n-1;
	    if(newN < 1)
		newN = 1;
	    setN(newN);
	});
	
	var nextNBtn = new JButton(">");
	nextNBtn.addActionListener(e -> {
	    int newN = n+1;
	    if(newN > 31)
		newN = 31;
	    setN(newN);
	});
	
	nLbl = new JLabel(Integer.toString(n));
	nLbl.setHorizontalAlignment(JLabel.CENTER);
	
	add(nEqLbl, new QuickGBC(0, 0).size(3, 1).anchor(QuickGBC.ANCHOR_CENTER).bottom(2));
	add(prevNBtn, new QuickGBC(0, 1).fill().weight(0.25, 0));
	add(nLbl, new QuickGBC(1, 1).weight(0.5, 0).anchor(QuickGBC.ANCHOR_CENTER).left(20).right(20));
	add(nextNBtn, new QuickGBC(2, 1).fill().weight(0.25, 0));
	
	refreshDataPanel();
    }
    
    private void setN(int n) {
	this.n = n;
	nLbl.setText(Integer.toString(n));
	refreshDataPanel();
    }
    
    private void refreshDataPanel() {
	JPanel panel = new JPanel(new GridBagLayout());
	panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	
	var recordsByN = records.getRecordsByN(n);
	if(recordsByN == null) {
	    remove(dataPanel);
	    add(panel, new QuickGBC(0, 3).size(3, 1).weight(1, 1));
	    revalidate();
	    repaint();
	    dataPanel = panel;
	    return;
	}
	
	int y = 0;
	for(var record : recordsByN.entrySet()) {
	    String device = record.getKey();
	    long duration = record.getValue();
	    
	    var deviceLbl = new JLabel(device + ":");
	    deviceLbl.setHorizontalAlignment(JLabel.LEFT);
	    
	    var durationLbl = new JLabel(ResultsPanel.getDurationString(duration));
	    durationLbl.setHorizontalAlignment(JLabel.RIGHT);
	    
	    int topGap = 5;
	    panel.add(deviceLbl, new QuickGBC(0, y).anchor(QuickGBC.ANCHOR_WEST).top(topGap));
	    panel.add(durationLbl, new QuickGBC(1, y).size(1, 1).anchor(QuickGBC.ANCHOR_EAST).top(topGap).left(10));
	    
	    y++;
	}
	
	remove(dataPanel);
	add(panel, new QuickGBC(0, 3).size(3, 1).weight(1, 1));
	revalidate();
	repaint();
	dataPanel = panel;
    }
    
    private void update(int n) {
	var recordsByN = records.getRecordsByN(n);
	if(recordsByN == null)
	    return;
	
	int y = 2;
	for(var record : recordsByN.entrySet()) {
	    String device = record.getKey();
	    long duration = record.getValue();
	    
	    var deviceLbl = new JLabel(device + ":");
	    deviceLbl.setHorizontalAlignment(JLabel.LEFT);
	    
	    var durationLbl = new JLabel(ResultsPanel.getDurationString(duration));
	    durationLbl.setHorizontalAlignment(JLabel.RIGHT);
	    
	    int topGap = y == 2 ? 10 : 5;
	    add(deviceLbl, new QuickGBC(0, y).anchor(QuickGBC.ANCHOR_WEST).top(topGap));
	    add(durationLbl, new QuickGBC(1, y).size(1, 1).anchor(QuickGBC.ANCHOR_EAST).top(topGap).left(10));
	    y++;
	}
    }
    
}
