package de.nqueensfaf.demo.gui;

import java.awt.Cursor;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
    
    static void loading(boolean loading) {
	if(loading)
	    jframe.setCursor(new Cursor(Cursor.WAIT_CURSOR));
	else
	    jframe.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
}
