package de.nqueensfaf.demo.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

@SuppressWarnings("serial")
class Utils {
    
    static void error(Component parent, String message, Runnable action) {
	JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
	if(action != null)
	    EventQueue.invokeLater(action);
    }
    
    static void error(Component parent, String message) {
	error(parent, message, null);
    }
    
    static void info(Component parent, String message, String title) {
	JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    static void loadingCursor(JFrame frame) {
	frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
    }
    
    static void defaultCursor(JFrame frame) {
	frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    
    static class LoadingWindow extends JWindow {
	
	private JProgressBar progressBar;
	
	LoadingWindow(JFrame context, String loadingText) {
	    super(context);
	    createUi(context, loadingText);
	}
	
	private void createUi(JFrame context, String loadingText) {
	    progressBar = new JProgressBar(0, 100);
	    progressBar.setStringPainted(true);
	    
	    var lbl = new JLabel(loadingText);
	    
	    var panel = new JPanel(new GridBagLayout());
	    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    panel.add(lbl, new QuickGBC(0, 0));
	    panel.add(progressBar, new QuickGBC(0, 1).top(2));
	    
	    add(panel);
	    pack();
	    setLocationRelativeTo(context);
	    
	    context.addComponentListener(new ComponentAdapter() {
		@Override
		public void componentMoved(ComponentEvent e) {
		    setLocationRelativeTo(context);
		}
	    });
	}
	
	JProgressBar getProgressBar() {
	    return progressBar;
	}
    }
}
