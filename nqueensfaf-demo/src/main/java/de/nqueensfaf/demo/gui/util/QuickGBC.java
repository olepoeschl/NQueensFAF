package de.nqueensfaf.demo.gui.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;

@SuppressWarnings("serial")
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
    
    public QuickGBC weight(double weightx, double weighty) {
	this.weightx = weightx;
	this.weighty = weighty;
	return this;
    }
    
    public QuickGBC anchor(int anchor) {
	this.anchor = anchor;
	return this;
    }
    
    public QuickGBC fill() {
	this.fill = GridBagConstraints.BOTH;
	return this;
    }
    
    public QuickGBC fillx() {
	this.fill |= GridBagConstraints.HORIZONTAL;
	return this;
    }
    
    public QuickGBC filly() {
	this.fill |= GridBagConstraints.VERTICAL;
	return this;
    }
    
    public QuickGBC insets(int top, int left, int bottom, int right) {
	insets = new Insets(top, left, bottom, right);
	return this;
    }
    
    public QuickGBC top(int top) {
	insets.top = top;
	return this;
    }
    
    public QuickGBC left(int left) {
	insets.left = left;
	return this;
    }
    
    public QuickGBC bottom(int bottom) {
	insets.bottom = bottom;
	return this;
    }
    
    public QuickGBC right(int right) {
	insets.right = right;
	return this;
    }
    
    public QuickGBC ipad(int ipadx, int ipady) {
	this.ipadx = ipadx;
	this.ipady = ipady;
	return this;
    }
}
