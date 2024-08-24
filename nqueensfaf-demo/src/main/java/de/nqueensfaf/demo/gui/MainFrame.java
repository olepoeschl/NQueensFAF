package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatLightLaf;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    public MainFrame() {
	FlatLightLaf.setup();
	init();
    }

    private void init() {
	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	JPanel container = new JPanel();
	container.setLayout(new BorderLayout());
	container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setContentPane(container);

	JPanel configPanel = new JPanel();
	configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
	add(configPanel, BorderLayout.WEST);
	
	JPanel panelN = new IntValueConfigPanel("Board size N", 1, 31, 16, 1);
	configPanel.add(panelN, BorderLayout.NORTH);

	JPanel panelUpdateInterval = new IntValueConfigPanel("Update Interval in ms", 0, 10000, 200, 100);
	configPanel.add(panelUpdateInterval, BorderLayout.NORTH);

	pack();
	setVisible(true);
    }
}
