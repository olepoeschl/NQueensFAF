package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class DialogUtils {
    
    private static JFrame jframe;
    
    public static void setJFrame(JFrame context) {
	jframe = context;
    }
    
    public static void error(String message, Runnable action) {
	JOptionPane.showMessageDialog(jframe, message, "Error", JOptionPane.ERROR_MESSAGE);
	if(action != null)
	    EventQueue.invokeLater(action);
    }
    
    public static void error(String message) {
	error(message, null);
    }
    
    public static void info(String message, String title) {
	JOptionPane.showMessageDialog(jframe, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
}
