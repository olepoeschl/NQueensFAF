package de.nqueensfaf.demo.gui.util;

import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import de.nqueensfaf.demo.gui.View;

@SuppressWarnings("serial")
public class Utils {
    
    private static ImageIcon saveIcon, openIcon, pasteIcon, copyIcon;
    
    static {
	try {
	    var saveBufImg = ImageIO.read(Utils.class.getClassLoader().getResourceAsStream("save.png"));
	    var openBufImg = ImageIO.read(Utils.class.getClassLoader().getResourceAsStream("open.png"));
	    var pasteBufImg = ImageIO.read(Utils.class.getClassLoader().getResourceAsStream("paste.png"));
	    var copyBufImg = ImageIO.read(Utils.class.getClassLoader().getResourceAsStream("copy.png"));

	    saveIcon = new ImageIcon(saveBufImg.getScaledInstance(20, 20, java.awt.Image.SCALE_AREA_AVERAGING));
	    openIcon = new ImageIcon(openBufImg.getScaledInstance(20, 20, java.awt.Image.SCALE_AREA_AVERAGING));
	    pasteIcon = new ImageIcon(pasteBufImg.getScaledInstance(20, 20, java.awt.Image.SCALE_AREA_AVERAGING));
	    copyIcon = new ImageIcon(copyBufImg.getScaledInstance(20, 20, java.awt.Image.SCALE_AREA_AVERAGING));
	} catch (IOException e) {
	    View.error(null, "could not read image resource: " + e.getMessage());
	}
    }
    
    public static ImageIcon getSaveIcon() {
	return saveIcon;
    }
    
    public static ImageIcon getOpenIcon() {
	return openIcon;
    }
    
    public static ImageIcon getPasteIcon() {
	return pasteIcon;
    }
    
    public static ImageIcon getCopyIcon() {
	return copyIcon;
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
