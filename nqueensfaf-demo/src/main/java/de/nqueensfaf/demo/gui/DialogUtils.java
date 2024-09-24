package de.nqueensfaf.demo.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

@SuppressWarnings("serial")
class DialogUtils {
    
    private static JFrame jframe;
    
    private static JWindow loadingWindow;
    private static JPanel loadingGlass;
    
    static void setJFrame(JFrame context) {
	jframe = context;
	jframe.addComponentListener(new ComponentListener() {
	    @Override
	    public void componentShown(ComponentEvent e) {
	    }
	    @Override
	    public void componentResized(ComponentEvent e) {
	    }
	    @Override
	    public void componentMoved(ComponentEvent e) {
		if(loadingWindow != null)
		    loadingWindow.setLocationRelativeTo(jframe);
	    }
	    @Override
	    public void componentHidden(ComponentEvent e) {
	    }
	});
	loadingGlass = new JPanel() {
	    @Override
	    public void paintComponent(Graphics g) {
		g.setColor(new Color(0, 0, 0, 140));
		g.fillRect(0, 0, getWidth(), getHeight());
	    }
	};
	loadingGlass.addMouseListener(new MouseAdapter() {
	    @Override
	    public void mousePressed(MouseEvent me) {
		me.consume();
	    }
	});
	loadingGlass.setOpaque(false);
	jframe.setGlassPane(loadingGlass);
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
    
    static void loadingCursor(boolean loading) {
	if(loading)
	    jframe.setCursor(new Cursor(Cursor.WAIT_CURSOR));
	else
	    jframe.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    
    static JProgressBar loadingWindow(boolean visible) {
	if(visible) {
	    loadingGlass.setVisible(true);
	    
	    loadingWindow = new JWindow(jframe);
	    var progressBar = new JProgressBar(0, 100);
	    loadingWindow.add(progressBar);
	    loadingWindow.pack();
	    loadingWindow.setLocationRelativeTo(jframe);
	    loadingWindow.setVisible(true);
	    return progressBar;
	} else {
	    loadingWindow.setVisible(false);
	    loadingWindow.dispose();

	    loadingGlass.setVisible(false);
	    
	    return null;
	}
    }
}
