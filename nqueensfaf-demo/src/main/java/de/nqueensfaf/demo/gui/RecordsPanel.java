package de.nqueensfaf.demo.gui;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class RecordsPanel extends JPanel {

    private final Records records;
    
    public RecordsPanel(Records records) {
	this.records = records;
	createUi();
    }
    
    private void createUi() {
	setLayout(new GridBagLayout());
	setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
	update(16); // default n is 16
    }

    private void update(int n) {
	removeAll();
	revalidate();
	repaint();
	
	addNConfigUi(n);
	
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

	revalidate();
	repaint();
    }
    
    private void addNConfigUi(int n) {
	var nConfigUi = new PropertyGroupConfigUi(this);
	nConfigUi.addIntProperty("n", "Board Size N", 1, 31, n, 1);
	nConfigUi.addPropertyChangeListener("n", e -> {
	    update((int) e.getNewValue());
	});
    }
}
