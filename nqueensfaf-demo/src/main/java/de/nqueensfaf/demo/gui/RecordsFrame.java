package de.nqueensfaf.demo.gui;

import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicArrowButton;

@SuppressWarnings("serial")
class RecordsFrame extends JFrame {

    private final Records records;
    
    private int n;
    
    private JLabel nLbl;
    private JScrollPane dataPanel = new JScrollPane();
    
    public RecordsFrame(Records records, int initialN) {
	super("Records");
	
	this.records = records;
	n = initialN;
	createUi();
	
	records.addRecordListener(n -> update());
    }
    
    private void createUi() {
	var contentPane = new JPanel(new GridBagLayout());
	contentPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
	setContentPane(contentPane);
	
	// n configuration ui
	var nEqLbl = new JLabel("N=");
	nEqLbl.setFont(MainFrame.CAPTION_FONT);
	nEqLbl.setHorizontalAlignment(JLabel.CENTER);
	
	var prevNBtn = new BasicArrowButton(BasicArrowButton.WEST);
	prevNBtn.addActionListener(e -> left());
	prevNBtn.setFocusable(false);
	
	var nextNBtn = new BasicArrowButton(BasicArrowButton.EAST);
	nextNBtn.addActionListener(e -> right());
	nextNBtn.setFocusable(false);
	
	nLbl = new JLabel(Integer.toString(n));
	nLbl.setFont(MainFrame.HIGHLIGHT_FONT);
	nLbl.setHorizontalAlignment(JLabel.CENTER);
	
	add(nEqLbl, new QuickGBC(0, 0).size(3, 1).anchor(QuickGBC.ANCHOR_CENTER));
	add(prevNBtn, new QuickGBC(0, 1).fill().weight(0.25, 0));
	add(nLbl, new QuickGBC(1, 1).weight(0.5, 0).anchor(QuickGBC.ANCHOR_CENTER).left(20).right(20));
	add(nextNBtn, new QuickGBC(2, 1).fill().weight(0.25, 0));
	
	update();
	
	pack();
	addKeyListener(new KeyListener() {
	    @Override
	    public void keyTyped(KeyEvent e) {
	    }
	    @Override
	    public void keyReleased(KeyEvent e) {
	    }
	    @Override
	    public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_LEFT)
		    left();
		else if(e.getKeyCode() == KeyEvent.VK_RIGHT)
		    right();
	    }
	});
    }
    
    private void left() {
	int newN = n-1;
	if(newN < 1)
	    newN = 1;
	setN(newN);
    }
    
    private void right() {
	int newN = n+1;
	if(newN > 31)
	    newN = 31;
	setN(newN);
    }
    
    void setN(int n) {
	this.n = n;
	nLbl.setText(Integer.toString(n));
	update();
	refresh();
    }
    
    void update() {
	var panel = new JPanel(new GridBagLayout());
	panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
	
	var recordsByN = records.getRecordsByN(n);
	if(recordsByN == null) {
	    remove(dataPanel);
	    dataPanel = new JScrollPane(panel);
	    dataPanel.setBorder(null);
	    add(dataPanel, new QuickGBC(0, 3).size(3, 1).weight(1, 1));
	    refresh();
	    return;
	}
	
	final var recordsSortedByDuration = new ArrayList<>(recordsByN.entrySet());
	recordsSortedByDuration.sort(Entry.comparingByValue());
	
	int y = 0;
	for(var record : recordsSortedByDuration) {
	    String device = record.getKey();
	    long duration = record.getValue();
	    
	    var deviceLbl = new JLabel(device + ":");
	    deviceLbl.setFont(MainFrame.CAPTION_FONT);
	    deviceLbl.setHorizontalAlignment(JLabel.LEFT);
	    
	    var durationLbl = new JLabel(ResultsPanel.getDurationString(duration));
	    durationLbl.setFont(MainFrame.CAPTION_FONT);
	    durationLbl.setHorizontalAlignment(JLabel.RIGHT);
	    
	    int topGap = 5;
	    panel.add(deviceLbl, new QuickGBC(0, y).top(topGap).weight(0.5, 0).fillx().anchor(QuickGBC.ANCHOR_NORTHEAST));
	    panel.add(durationLbl, new QuickGBC(1, y).top(topGap).left(20).weight(0.5, 0).fillx().anchor(QuickGBC.ANCHOR_NORTHEAST));
	    
	    y++;
	}
	panel.add(new JPanel(), new QuickGBC(0, y).weight(1, 1).fill());

	remove(dataPanel);
	dataPanel = new JScrollPane(panel);
	dataPanel.setBorder(null);
	add(dataPanel, new QuickGBC(0, 3).size(3, 1).weight(1, 1).fill());
	refresh();
    }
    
    // refresh data panel and frame
    private void refresh() {
	getContentPane().revalidate();
	getContentPane().repaint();
	setSize(getWidth(), getPreferredSize().height);
    }
}
