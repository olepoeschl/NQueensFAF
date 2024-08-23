package de.nqueensfaf.demo.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private final AbstractSolverConfig model = new AbstractSolverConfig(16, 200, 0);

    public MainFrame() {
	FlatLightLaf.setup();
	init();
    }

    private void init() {
	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setLayout(new BorderLayout());
	
	addNConfigurationPanel();

	pack();
	setVisible(true);
    }

    private void addNConfigurationPanel() {
	JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
	add(p, BorderLayout.WEST);
	
	JButton btnMinus = new JButton("-");
	JSlider sliderN = new JSlider(1, 31, 16);
	JButton btnPlus = new JButton("+");
	JTextField txtN = new JTextField("16");
	
	p.add(btnMinus);
	p.add(sliderN);
	p.add(btnPlus);
	p.add(txtN);
    }
}
