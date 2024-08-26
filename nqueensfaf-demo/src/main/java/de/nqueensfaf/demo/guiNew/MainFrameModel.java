package de.nqueensfaf.demo.guiNew;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

class MainFrameModel {
    
    private int n;
    private int progress;
    
    private PropertyChangeSupport prop = new PropertyChangeSupport(this);
    
    MainFrameModel() {
	n = 16;
	progress = 0;
    }

    void setN(int n) {
	int oldN = this.n;
	this.n = n;
	prop.firePropertyChange("n", oldN, n);
    }

    void setProgress(int progress) {
	int oldProgress = this.progress;
	this.progress = progress;
	prop.firePropertyChange("progress", oldProgress, progress);
    }
    
    void addPropertyChangeListener(PropertyChangeListener l) {
	prop.addPropertyChangeListener(l);
    }
    
    void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
	prop.addPropertyChangeListener(propertyName, l);
    }

    void removePropertyChangeListener(PropertyChangeListener l) {
	prop.removePropertyChangeListener(l);
    }
}
