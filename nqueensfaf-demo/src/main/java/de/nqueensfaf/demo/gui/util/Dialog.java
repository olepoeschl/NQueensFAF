package de.nqueensfaf.demo.gui.util;

import java.awt.EventQueue;

import javax.swing.JOptionPane;

public class Dialog {
    
    public static void error(String message, Runnable action) {
	JOptionPane.showMessageDialog(null, message);
	EventQueue.invokeLater(action);
    }
    
    public static void error(String message) {
	error(message, null);
    }
    
}
