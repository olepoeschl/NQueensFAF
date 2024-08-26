package de.nqueensfaf.demo.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

    private HashMap<String, Property<?>> properties = new HashMap<String, Property<?>>();

    private final JPanel panel;
    
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
    
    // only text input
    void addIntProperty(String name, int min, int max, int value) {
	addIntProperty(name, min, max, value, 0);
    }

    // text input, slider, + and - buttons
    void addIntProperty(String name, int min, int max, int value, int step) {
	var prop = new IntProperty(name, min, max, value, step);
	prop.installConfigUi();
	
	resetConstraintsToNextRow();
	
	properties.put(name, prop);
    }

    private interface Property<T> {
	T getValue();
	void installConfigUi();
    }

    private class IntProperty implements Property<Integer> {
	
	final String name;
	final int min, max, step;
	int value;
	
	boolean textInputOnly = false;
	
	JFormattedTextField txtValue;
	JSlider slider;

	IntProperty(String name, int min, int max, int value, int step) {
	    this.name = name;
	    this.min = min;
	    this.max = max;
	    this.value = value;
	    
	    this.step = step;
	    if(step == 0)
		textInputOnly = true;
	}

	@Override
	public Integer getValue() {
	    return value;
	}

	@Override
	public void installConfigUi() {
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    constraints.anchor = GridBagConstraints.WEST;
	    JLabel lblName = new JLabel(name);
	    panel.add(lblName, constraints);

	    constraints.fill = GridBagConstraints.NONE;
	    constraints.gridx = 0;
	    constraints.gridy++;
	    constraints.insets.top = 2;
	    NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance());
	    formatter.setValueClass(Integer.class);
	    formatter.setMinimum(min);
	    formatter.setMaximum(max);
	    formatter.setAllowsInvalid(true);
	    formatter.setCommitsOnValidEdit(true);
	    txtValue = new JFormattedTextField(formatter);
	    txtValue.setText(Integer.toString(value));
	    txtValue.addPropertyChangeListener("value", e -> {
		value = (int) e.getNewValue();
		valueChanged();
	    });
	    panel.add(txtValue, constraints);

	    if(textInputOnly)
		return;
	    
	    constraints.gridx++;
	    constraints.insets.left = 5;
	    JButton btnMinus = new JButton("-");
	    btnMinus.addActionListener(e -> {
		value -= step;
		valueChanged();
	    });
	    panel.add(btnMinus, constraints);

	    constraints.gridx++;
	    constraints.insets.left = 0;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    constraints.weightx = 1.0;
	    slider = new JSlider(min, max, value);
	    slider.addChangeListener(e -> {
		value = slider.getValue();
		valueChanged();
	    });
	    panel.add(slider, constraints);

	    constraints.gridx++;
	    constraints.weightx = 0;
	    JButton btnPlus = new JButton("+");
	    btnPlus.addActionListener(e -> {
		value += step;
		valueChanged();
	    });
	    panel.add(btnPlus, constraints);
	}

	private void valueChanged() {
	    if (value < min)
		value = min;
	    if (value > max)
		value = max;
	    txtValue.setText(Integer.toString(value));
	    
	    if(!textInputOnly)
		slider.setValue(value);
	}
    }

    private void resetConstraintsToNextRow() {
	final int oldGridY = constraints.gridy;
	constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = oldGridY + 1;
	constraints.insets.top = 5;
    }
}
