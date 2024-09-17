package de.nqueensfaf.demo.gui;

import static de.nqueensfaf.demo.gui.QuickGBC.*;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.text.NumberFormatter;

class PropertyGroupConfigUi {

    private GridBagLayout layout = new GridBagLayout();
    private int gridy = 0;

    private Map<String, AbstractProperty<?>> properties = new HashMap<String, AbstractProperty<?>>();

    private final JPanel panel;

    PropertyGroupConfigUi() {
	this(new JPanel());
    }

    PropertyGroupConfigUi(JPanel panel) {
	this.panel = panel;
	panel.setLayout(layout);
    }

    JPanel getUi() {
	return panel;
    }

    AbstractProperty<?> getProperty(String propertyName) {
	return properties.get(propertyName);
    }

    void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
	properties.get(propertyName).addPropertyChangeListener(propertyName, l);
    }

    void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
	properties.get(propertyName).removePropertyChangeListener(propertyName, l);
    }

    void setEnabled(boolean enabled) {
	for (var prop : properties.values()) {
	    prop.setEnabled(enabled);
	}
    }

    <T extends AbstractProperty<?>> void addProperty(T property) {
	property.createUi();
	installPropertyUi(property);
	properties.put(property.getName(), property);
    }
    
    void installPropertyUi(AbstractProperty<?> property) {
	if(properties.size() > 0)
	    panel.add(Box.createVerticalStrut(5), new QuickGBC(0, gridy++));
	
	int maxGridy = 0;
	
	for(var entry : property.getComponentsWithConstraints()) {
	    entry.getValue().gridy += gridy;
	    if(entry.getValue().gridy > maxGridy)
		maxGridy = entry.getValue().gridy;
	    
	    panel.add(entry.getKey(), entry.getValue());
	}
	
	gridy = maxGridy + 1;
    }

    // only text input
    void addIntProperty(String name, String title, int min, int max, int value) {
	addIntProperty(name, title, min, max, value, 0);
    }

    // text input, slider, + and - buttons
    void addIntProperty(String name, String title, int min, int max, int value, int step) {
	addProperty(new IntProperty(name, title, min, max, value, step));
    }

    static abstract class AbstractProperty<T> {

	private final PropertyChangeSupport prop = new PropertyChangeSupport(this);

	private final List<Map.Entry<JComponent, GridBagConstraints>> componentsWithConstraints = new ArrayList<Entry<JComponent, GridBagConstraints>>();

	private final String name;
	private final String title;
	private T value;

	AbstractProperty(String name, String title, T value) {
	    this.name = name;
	    this.title = title;
	    this.value = value;
	}

	final void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
	    prop.addPropertyChangeListener(propertyName, l);
	}

	final void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
	    prop.removePropertyChangeListener(propertyName, l);
	}

	final void add(JComponent component, GridBagConstraints constraints) {
	    componentsWithConstraints
		    .add(new AbstractMap.SimpleEntry<JComponent, GridBagConstraints>(component, constraints));
	}
	
	final List<Map.Entry<JComponent, GridBagConstraints>> getComponentsWithConstraints(){
	    return componentsWithConstraints;
	}

	final String getName() {
	    return name;
	}

	final String getTitle() {
	    return title;
	}

	final T getValue() {
	    return value;
	}

	final void setValue(T value) {
	    T oldValue = this.value;
	    this.value = value;
	    updateUi(value);
	    prop.firePropertyChange(name, oldValue, value);
	}

	void createUi() {
	    JLabel lblTitle = new JLabel(getTitle());
	    add(lblTitle, new QuickGBC(0, 0).fillx().anchor(ANCHOR_WEST));

	    createConfigUi();
	}
	
	abstract void updateUi(T value);

	abstract protected void createConfigUi();

	abstract void setEnabled(boolean enabled);
    }

    static class IntProperty extends AbstractProperty<Integer> {

	final int min, max, step;

	boolean textInputOnly = false;

	JFormattedTextField txtValue;
	JButton btnMinus, btnPlus;
	JSlider slider;

	IntProperty(String name, String title, int min, int max, int value, int step) {
	    super(name, title, value);

	    this.min = min;
	    this.max = max;
	    this.step = step;
	    if (step == 0)
		textInputOnly = true;
	}

	@Override
	public void createConfigUi() {
	    int gridy = 1; // title is on y=0 so we start at y=1
	    
	    NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance());
	    formatter.setValueClass(Integer.class);
	    formatter.setMinimum(min);
	    formatter.setMaximum(max);
	    formatter.setAllowsInvalid(true);
	    formatter.setCommitsOnValidEdit(false);
	    txtValue = new JFormattedTextField(formatter);
	    txtValue.setText(Integer.toString(getValue()));
	    txtValue.addPropertyChangeListener("value", e -> {
		int newValue = e.getNewValue() != null ? (int) e.getNewValue() : getValue();
		setValueIfValid(newValue);
	    });
	    add(txtValue, new QuickGBC(0, gridy).anchor(ANCHOR_NORTH).top(2).weight(0, 0).fillx());

	    if (textInputOnly)
		return;

	    btnMinus = new JButton("-");
	    btnMinus.addActionListener(e -> {
		int newValue = getValue() - step;
		setValueIfValid(newValue);
	    });
	    add(btnMinus, new QuickGBC(1, gridy).anchor(ANCHOR_NORTH).top(2).weight(0, 0).fillx().left(5));

	    slider = new JSlider(min, max, getValue());
	    slider.addChangeListener(e -> {
		int newValue = slider.getValue();
		setValueIfValid(newValue);
	    });
	    add(slider, new QuickGBC(2, gridy).anchor(ANCHOR_NORTH).top(2).weight(1, 0).fillx().left(5));

	    btnPlus = new JButton("+");
	    btnPlus.addActionListener(e -> {
		int newValue = getValue() + step;
		setValueIfValid(newValue);
	    });
	    add(btnPlus, new QuickGBC(3, gridy).anchor(ANCHOR_NORTH).top(2).weight(0, 0).fillx().left(5));
	}
	
	void setValueIfValid(int newValue){
	    if (newValue < min)
		newValue = min;
	    if (newValue > max)
		newValue = max;
	    setValue(newValue);
	}
	
	@Override
	void updateUi(Integer newValue) {
	    txtValue.setText(Integer.toString(newValue));
	    if (!textInputOnly)
		slider.setValue(newValue);
	}

	@Override
	void setEnabled(boolean enabled) {
	    txtValue.setEditable(enabled);
	    btnMinus.setEnabled(enabled);
	    btnPlus.setEnabled(enabled);
	    slider.setEnabled(enabled);
	}
    }

    public void fillRemainingVerticalSpace() {
	panel.add(Box.createVerticalGlue(), new QuickGBC(0, gridy).weight(0, 1).filly());
    }
}
