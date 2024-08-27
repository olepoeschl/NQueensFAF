package de.nqueensfaf.demo.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.text.NumberFormatter;

class PropertyGroupConfigUi {
    
    private GridBagLayout layout = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();

    private HashMap<String, AbstractProperty<?>> properties = new HashMap<String, AbstractProperty<?>>();

    private final JPanel panel;
    
    private PropertyChangeSupport prop = new PropertyChangeSupport(this);
    
    PropertyGroupConfigUi() {
	this(new JPanel());
    }
    
    PropertyGroupConfigUi(JPanel panel) {
	this.panel = panel;
	panel.setLayout(layout);
	constraints.gridx = 0;
	constraints.gridy = 0;
    }
    
    JPanel getUi() {
	return panel;
    }

    Object getProperty(String name) {
	return properties.get(name).getValue();
    }
    
    void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
	prop.addPropertyChangeListener(propertyName, l);
	prop.firePropertyChange(
		propertyName, null, getProperty(propertyName));
    }
    
    void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
	prop.removePropertyChangeListener(propertyName, l);
    }
    
    void setEnabled(boolean enabled) {
	for(var prop : properties.values()) {
	    prop.setEnabled(enabled);
	}
    }
    
    // only text input
    void addIntProperty(String name, String title, int min, int max, int value) {
	addIntProperty(name, title, min, max, value, 0);
    }

    // text input, slider, + and - buttons
    void addIntProperty(String name, String title, int min, int max, int value, int step) {
	var prop = new IntProperty(name, title, min, max, value, step);
	properties.put(name, prop);
	
	prop.installConfigUi();
	
	resetConstraintsToNextRow();
    }
    
    private abstract class AbstractProperty<T> {
	
	private final String name;
	private final String title;
	private T value;
	
	AbstractProperty(String name, String title, T value) {
	    this.name = name;
	    this.title = title;
	    this.value = value;
	}
	
	final String getTitle() {
	    return title;
	}
	
	final T getValue() {
	    return value;
	}
	
	protected final void setValue(T value){
	    T oldValue = this.value;
	    this.value = value;
	    prop.firePropertyChange(name, oldValue, value);
	}
	
	abstract void installConfigUi();
	abstract void setEnabled(boolean enabled);
    }
    
    private class IntProperty extends AbstractProperty<Integer> {
	
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
	    if(step == 0)
		textInputOnly = true;
	}

	@Override
	public void installConfigUi() {
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    constraints.anchor = GridBagConstraints.NORTHWEST;
	    JLabel lblTitle = new JLabel(getTitle());
	    panel.add(lblTitle, constraints);

	    constraints.fill = GridBagConstraints.NONE;
	    constraints.gridx = 0;
	    constraints.gridy++;
	    constraints.insets.top = 2;
	    constraints.weighty = 1.0;
	    NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance());
	    formatter.setValueClass(Integer.class);
	    formatter.setMinimum(min);
	    formatter.setMaximum(max);
	    formatter.setAllowsInvalid(true);
	    formatter.setCommitsOnValidEdit(false);
	    txtValue = new JFormattedTextField(formatter);
	    txtValue.setText(Integer.toString(getValue()));
	    txtValue.addPropertyChangeListener("value", e -> {
		int newValue = (int) e.getNewValue();
		valueChanged(newValue);
	    });
	    panel.add(txtValue, constraints);

	    if(textInputOnly)
		return;
	    
	    constraints.gridx++;
	    constraints.insets.left = 5;
	    btnMinus = new JButton("-");
	    btnMinus.addActionListener(e -> {
		int newValue = getValue() - step;
		valueChanged(newValue);
	    });
	    panel.add(btnMinus, constraints);

	    constraints.gridx++;
	    constraints.insets.left = 0;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    constraints.weightx = 1.0;
	    slider = new JSlider(min, max, getValue());
	    slider.addChangeListener(e -> {
		int newValue = slider.getValue();
		valueChanged(newValue);
	    });
	    panel.add(slider, constraints);

	    constraints.gridx++;
	    constraints.weightx = 0;
	    btnPlus = new JButton("+");
	    btnPlus.addActionListener(e -> {
		int newValue = getValue() + step;
		valueChanged(newValue);
	    });
	    panel.add(btnPlus, constraints);
	}

	private void valueChanged(int newValue) {
	    if (newValue < min)
		newValue = min;
	    if (newValue > max)
		newValue = max;
	    txtValue.setText(Integer.toString(newValue));
	    
	    if(!textInputOnly)
		slider.setValue(newValue);
	    
	    setValue(newValue);
	}
	
	@Override
	void setEnabled(boolean enabled) {
	    txtValue.setEditable(enabled);
	    btnMinus.setEnabled(enabled);
	    btnPlus.setEnabled(enabled);
	    slider.setEnabled(enabled);
	}
    }

    private void resetConstraintsToNextRow() {
	final int oldGridY = constraints.gridy;
	constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = oldGridY + 1;
	constraints.insets.top = 5;
	constraints.insets.left = 0;
    }
}
