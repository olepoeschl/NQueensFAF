package de.nqueensfaf.demo.gui.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;

public class QuickGBC extends GridBagConstraints {
    
    /**
     * Put the component in the center of its display area.
     */
    public static final int ANCHOR_CENTER = 10;

    /**
     * Put the component at the top of its display area,
     * centered horizontally.
     */
    public static final int ANCHOR_NORTH = 11;

    /**
     * Put the component at the top-right corner of its display area.
     */
    public static final int ANCHOR_NORTHEAST = 12;

    /**
     * Put the component on the right side of its display area,
     * centered vertically.
     */
    public static final int ANCHOR_EAST = 13;

    /**
     * Put the component at the bottom-right corner of its display area.
     */
    public static final int ANCHOR_SOUTHEAST = 14;

    /**
     * Put the component at the bottom of its display area, centered
     * horizontally.
     */
    public static final int ANCHOR_SOUTH = 15;

    /**
     * Put the component at the bottom-left corner of its display area.
     */
    public static final int ANCHOR_SOUTHWEST = 16;

    /**
     * Put the component on the left side of its display area,
     * centered vertically.
     */
    public static final int ANCHOR_WEST = 17;

    /**
     * Put the component at the top-left corner of its display area.
     */
    public static final int ANCHOR_NORTHWEST = 18;
    
    /**
     * Do not resize the component.
     */
    public static final int FILL_NONE = 0;

    /**
     * Resize the component both horizontally and vertically.
     */
    public static final int FILL_BOTH = 1;

    /**
     * Resize the component horizontally but not vertically.
     */
    public static final int FILL_HORIZONTAL = 2;

    /**
     * Resize the component vertically but not horizontally.
     */
    public static final int FILL_VERTICAL = 3;
    
    public QuickGBC(int gridx, int gridy) {
	this.gridx = gridx;
	this.gridy = gridy;
	
        gridwidth = 1;
        gridheight = 1;

        weightx = 0;
        weighty = 0;
        anchor = CENTER;
        fill = NONE;

        insets = new Insets(0, 0, 0, 0);
        ipadx = 0;
        ipady = 0;
    }
    
    public QuickGBC() {
	this(0, 0);
    }
    
    public QuickGBC pos(int gridx, int gridy) {
	this.gridx = gridx;
	this.gridy = gridy;
	return this;
    }
    
    public QuickGBC size(int gridwidth, int gridheight) {
	this.gridwidth = gridwidth;
	this.gridheight = gridheight;
	return this;
    }
    
    public QuickGBC weight(int weightx, int weighty) {
	this.weightx = weightx;
	this.weighty = weighty;
	return this;
    }
    
    public QuickGBC anchor(int anchor) {
	this.anchor = anchor;
	return this;
    }
    
    public QuickGBC fill(int fill) {
	this.fill = fill;
	return this;
    }
    
    public QuickGBC insets(int top, int left, int bottom, int right) {
	insets = new Insets(top, left, bottom, right);
	return this;
    }
    
    public QuickGBC ipad(int ipadx, int ipady) {
	this.ipadx = ipadx;
	this.ipady = ipady;
	return this;
    }
}
