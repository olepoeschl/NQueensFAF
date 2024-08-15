package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private final GeneralSolverConfig model = new GeneralSolverConfig(16, 200);

    public MainFrame() {
	init();
    }

    private void init() {
	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setLayout(new BorderLayout());
    }

}
