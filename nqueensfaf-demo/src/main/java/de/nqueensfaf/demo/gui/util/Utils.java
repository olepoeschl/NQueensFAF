package de.nqueensfaf.demo.gui.util;

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
public class Utils {
    
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
    
    public static class LoadingWindow extends JWindow {
	
	private JProgressBar progressBar;
	
	public LoadingWindow(JFrame context, String loadingText) {
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
	
	public JProgressBar getProgressBar() {
	    return progressBar;
	}
    }

    public static String getDurationUnitlessString(long time) {
	long h = time / 1000 / 60 / 60;
	long m = time / 1000 / 60 % 60;
	long s = time / 1000 % 60;
	long ms = time % 1000;

	String strh, strm, strs, strms;
	// hours
	if (h == 0) {
	    strh = "00";
	} else if ((h + "").toString().length() == 3) {
	    strh = "" + h;
	} else if ((h + "").toString().length() == 2) {
	    strh = "0" + h;
	} else {
	    strh = "00" + h;
	}
	// minutes
	if ((m + "").toString().length() == 2) {
	    strm = "" + m;
	} else {
	    strm = "0" + m;
	}
	// seconds
	if ((s + "").toString().length() == 2) {
	    strs = "" + s;
	} else {
	    strs = "0" + s;
	}
	// milliseconds
	if ((ms + "").toString().length() == 3) {
	    strms = "" + ms;
	} else if ((ms + "").toString().length() == 2) {
	    strms = "0" + ms;
	} else {
	    strms = "00" + ms;
	}
	
	String durationStr;
	if(h > 0)
	    durationStr = strh + ":" + strm + ":" + strs + "." + strms;
	else if(m > 0)
	    durationStr = strm + ":" + strs + "." + strms;
	else
	    durationStr = strs + "." + strms;

	return durationStr.startsWith("0") ? durationStr.substring(1) : durationStr;
    }
    
    public static String getDurationUnitString(long duration) {
	if(duration >= 60 * 60 * 1000)
	    return "hours";
	else if(duration >= 60 * 1000)
	    return "minutes";
	else
	    return "seconds";
    }
    
    public static String getDurationString(long time) {
	long h = time / 1000 / 60 / 60;
	long m = time / 1000 / 60 % 60;
	long s = time / 1000 % 60;
	long ms = time % 1000;

	String strh, strm, strs, strms;
	// hours
	if (h == 0) {
	    strh = "00";
	} else if ((h + "").toString().length() == 3) {
	    strh = "" + h;
	} else if ((h + "").toString().length() == 2) {
	    strh = "0" + h;
	} else {
	    strh = "00" + h;
	}
	// minutes
	if ((m + "").toString().length() == 2) {
	    strm = "" + m;
	} else {
	    strm = "0" + m;
	}
	// seconds
	if ((s + "").toString().length() == 2) {
	    strs = "" + s;
	} else {
	    strs = "0" + s;
	}
	// milliseconds
	if ((ms + "").toString().length() == 3) {
	    strms = "" + ms;
	} else if ((ms + "").toString().length() == 2) {
	    strms = "0" + ms;
	} else {
	    strms = "00" + ms;
	}
	
	String durationStr = strh + ":" + strm + ":" + strs + "." + strms;
	return durationStr;
    }
    
    public static String getSolutionsString(long solutions) {
	StringBuilder sb = new StringBuilder(Long.toString(solutions));
	for (int i = sb.length() - 3; i >= 0; i -= 3) {
	    if (i <= 0)
		break;
	    sb.insert(i, ".");
	}
	return sb.toString();
    }
}
