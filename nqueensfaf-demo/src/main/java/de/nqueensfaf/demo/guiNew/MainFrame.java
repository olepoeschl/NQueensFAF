package de.nqueensfaf.demo.guiNew;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private final MainFrameModel model = new MainFrameModel();
    private SolverImplConfigPanel solverImplConfigPanel = new SolverImplConfigPanel();
    
    public MainFrame() {
	createAndShowUi();
    }
    
    private void createAndShowUi() {
	setTitle("NQueensFAF");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	JPanel container = new JPanel();
	var layout = new BorderLayout(10, 10);
	container.setLayout(layout);
	container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	setContentPane(container);

	// south
	addProgressBar();
	
	// west
	JPanel pnlLeft = new JPanel();
	pnlLeft.setLayout(new GridBagLayout());
	
	var constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.weightx = 1;
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.anchor = GridBagConstraints.NORTH;
	// add n slider

	pack();
	setResizable(false);
	setVisible(true);
    }
    
    private void addProgressBar() {
	JProgressBar progressBar = new JProgressBar(0, 100);
	progressBar.setStringPainted(true);
	progressBar.setValue(0);
	add(progressBar, BorderLayout.SOUTH);
	
	model.addPropertyChangeListener("progress", e -> {
	    progressBar.setValue((int) e.getNewValue());
	});
    }
}
