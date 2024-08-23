package de.nqueensfaf.demo.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.text.NumberFormatter;

class IntValueConfigPanel extends JPanel {

    private final String name;
    private final int min, max, step;
    private int value;
    private JFormattedTextField txtValue;
    private JSlider slider;

    public IntValueConfigPanel(String name, int min, int max, int value, int step) {
	this.name = name;
	this.min = min;
	this.max = max;
	this.value = value;
	this.step = step;
	init();
    }

    private void init() {
	setLayout(new GridBagLayout());
	GridBagConstraints constraints = new GridBagConstraints();

	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = 0;
	constraints.gridy = 0;
	JLabel lblName = new JLabel(name);
	add(lblName, constraints);

	constraints.fill = GridBagConstraints.NONE;
	constraints.gridx = 0;
	constraints.gridy = 1;
	constraints.insets.top = 2;
	NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance()) {
	    @Override
	    public void install(final JFormattedTextField ftf) {
	        int prevLen = ftf.getDocument().getLength();
	        int savedCaretPos = ftf.getCaretPosition();
	        super.install(ftf);
	        if (ftf.getDocument().getLength() == prevLen) {
	            ftf.setCaretPosition(savedCaretPos);
	        }
	    }
	};
	formatter.setMinimum(min);
	formatter.setMaximum(max);
	formatter.setAllowsInvalid(true);
	formatter.setCommitsOnValidEdit(true);
	txtValue = new JFormattedTextField(formatter);
	txtValue.setText(Integer.toString(value));
	txtValue.addPropertyChangeListener("value", e -> {
	    value = (int) e.getNewValue();
	    valueChanged();
	    txtValue.setCaretPosition(txtValue.getText().length());
	});
	add(txtValue, constraints);

	constraints.gridx = 1;
	constraints.insets.left = 5;
	JButton btnMinus = new JButton("-");
	btnMinus.addActionListener(e -> {
	    value -= step;
	    valueChanged();
	});
	add(btnMinus, constraints);

	constraints.gridx = 2;
	constraints.insets.left = 0;
	slider = new JSlider(min, max, value);
	slider.addChangeListener(e -> {
	    value = slider.getValue();
	    valueChanged();
	});
	add(slider, constraints);

	constraints.gridx = 3;
	JButton btnPlus = new JButton("+");
	btnPlus.addActionListener(e -> {
	    value += step;
	    valueChanged();
	});
	add(btnPlus, constraints);
    }

    private void valueChanged() {
	if (value < min)
	    value = min;
	if (value > max)
	    value = max;
	txtValue.setText(Integer.toString(value));
	slider.setValue(value);
    }
}
