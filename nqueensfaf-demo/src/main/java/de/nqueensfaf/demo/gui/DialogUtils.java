package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class DialogUtils {
    
    private static JFrame jframe;
    
    static void setJFrame(JFrame context) {
	jframe = context;
    }
    
    static void error(String message, Runnable action) {
	JOptionPane.showMessageDialog(jframe, message, "Error", JOptionPane.ERROR_MESSAGE);
	if(action != null)
	    EventQueue.invokeLater(action);
    }
    
    static void error(String message) {
	error(message, null);
    }
    
    static void info(String message, String title) {
	JOptionPane.showMessageDialog(jframe, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    static JDialog loading(String title) {
	String loadingText = "Please wait...";
	var lbl = new JLabel(loadingText);
	
	var panel = new JPanel(new GridBagLayout());
	panel.add(lbl, new QuickGBC(0, 0).anchor(QuickGBC.ANCHOR_CENTER).insets(5, 5, 5, 5));
	
	var dialog = new JDialog(jframe, title, false);
	dialog.setContentPane(panel);
	dialog.pack();
	dialog.setLocationRelativeTo(jframe);
	
	return dialog;
    }
}
