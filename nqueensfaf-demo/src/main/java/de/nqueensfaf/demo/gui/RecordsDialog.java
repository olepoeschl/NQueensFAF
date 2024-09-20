package de.nqueensfaf.demo.gui;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class RecordsDialog extends JDialog {

    private final Records records;
    
    private int n = 16;
    
    private JLabel nLbl;
    private JPanel dataPanel = new JPanel(new GridBagLayout());
    
    public RecordsDialog(Records records) {
	this.records = records;
	createUi();
    }
    
    private void createUi() {
	var contentPane = new JPanel(new GridBagLayout());
	contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setContentPane(contentPane);
	
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
	
	updateDataPanel();
	
	pack();
	setModal(true);
    }
    
    private void setN(int n) {
	this.n = n;
	nLbl.setText(Integer.toString(n));
	updateDataPanel();
	refresh();
    }
    
    private void updateDataPanel() {
	var panel = new JPanel(new GridBagLayout());
	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	var recordsByN = records.getRecordsByN(n);
	if(recordsByN == null) {
	    remove(dataPanel);
	    dataPanel = panel;
	    add(dataPanel, new QuickGBC(0, 3).size(3, 1).weight(1, 1));
	    refresh();
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
	    panel.add(deviceLbl, new QuickGBC(0, y).top(topGap).weight(0.5, 0).fillx().anchor(QuickGBC.ANCHOR_NORTHEAST));
	    panel.add(durationLbl, new QuickGBC(1, y).top(topGap).left(10).weight(0.5, 0).fillx().anchor(QuickGBC.ANCHOR_NORTHEAST));
	    
	    y++;
	}
	panel.add(new JPanel(), new QuickGBC(0, y).weight(1, 1).fill());

	remove(dataPanel);
	dataPanel = panel;
	add(dataPanel, new QuickGBC(0, 3).size(3, 1).weight(1, 1).fill());
	refresh();
    }
    
    // refresh data panel and frame
    void refresh() {
	revalidate();
	repaint();
    }
}
