package de.nqueensfaf.demo.gui;

import java.awt.GridBagConstraints;
import java.util.List;

import javax.swing.JPanel;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.AbstractProperty;
import de.nqueensfaf.demo.gui.SolverSelectionPanel.SolverImplConfigPanel;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

class GpuSolverConfigPanel extends SolverImplConfigPanel {

    private final GpuSolver solver = new GpuSolver();
    
    private final PropertyGroupConfigUi propConfigUi;
    
    public GpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 4, 1);
	propConfigUi.addPropertyChangeListener(
		"prequeens", e -> solver.setPresetQueens((int) e.getNewValue()));
	// TODO
    }
    
    @Override
    AbstractSolver getConfiguredSolver() {
	return solver;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	propConfigUi.setEnabled(enabled);
    }
    
    class GpuSelectionProperty extends AbstractProperty<List<Gpu>> {

	GpuSelectionProperty(String name, String title, List<Gpu> value) {
	    super(name, title, value);
	}

	@Override
	protected void installConfigUi(JPanel panel, GridBagConstraints constraints) {
	    // TODO Auto-generated method stub
	}

	@Override
	void setEnabled(boolean enabled) {
	    // TODO Auto-generated method stub
	}
	
    }

}
