package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import de.nqueensfaf.demo.gui.util.Utils;

@SuppressWarnings("serial")
class HistoryFrame extends JFrame {
    
    private DefaultTableModel tableModel;
    
    public HistoryFrame() {
	super("History");
	createUi();
    }

    private void createUi() {
	// create table
	var columns = new String[] { "N", "Solver / Description", "Duration", "Configuration" };
	tableModel = new DefaultTableModel(null, columns);
	var table = new JTable(tableModel) {
	    @Override
	    public Class<?> getColumnClass(int column) {
		switch(column) {
		case 0: return Integer.class;
		case 1: return String.class;
		case 2: return String.class;
		case 3: return JButton.class;
		default: return String.class;
		}
	    }
	    @Override
	    public boolean isCellEditable(int rowIndex, int colIndex) {
		return false;
	    }
	};
	
	// adjust column alignment
	var centerRenderer = new DefaultTableCellRenderer();
	centerRenderer.setHorizontalAlignment(JLabel.CENTER);
	table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
	table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
	table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

	var buttonRenderer = new TableCellRenderer() {
	    @Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
		    int row, int column) {
		if(value instanceof JButton)
		    return (JButton) value;
		return null;
	    }
	};
	table.getColumnModel().getColumn(3).setCellRenderer(buttonRenderer);
	// TODO: implement action when copy-Icon is clicked

	// init frame
	add(new JScrollPane(table), BorderLayout.CENTER);
	setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	pack();
    }

    void addEntry(HistoryEntry entry) {
	var copyConfigBtn = new JButton(Utils.getCopyIcon());
	copyConfigBtn.addActionListener(e -> System.out.println("hi"));
	// TODO: click not working
	
	tableModel.insertRow(0, new Object[] { entry.n(), entry.deviceName(), Utils.getDurationString(entry.duration()),
		copyConfigBtn });
    }

    record HistoryEntry(int n, String deviceName, long duration) {}
}
