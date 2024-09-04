package de.nqueensfaf.demo.gui;

import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.AbstractProperty;
import de.nqueensfaf.demo.gui.SolverSelectionPanel.SolverImplConfigPanel;
import de.nqueensfaf.demo.gui.util.QuickGBC;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

import static de.nqueensfaf.demo.gui.util.QuickGBC.*;

class GpuSolverConfigPanel extends SolverImplConfigPanel {

    private final GpuSolver solver = new GpuSolver();

    private final PropertyGroupConfigUi propConfigUi;

    public GpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 4, 1);
	propConfigUi.addPropertyChangeListener("prequeens", e -> solver.setPresetQueens((int) e.getNewValue()));
	
	final var gpus = solver.getAvailableGpus();
	propConfigUi.addProperty(
		new GpuSelectionProperty("gpus", gpus.size() > 0 ? List.of(gpus.get(0)) : List.of(), gpus));
    }

    @SuppressWarnings("unchecked")
    @Override
    AbstractSolver getConfiguredSolver() {
	solver.gpuSelection().reset();
	for (var gpu : (List<Gpu>) propConfigUi.getProperty("gpus")) {
	    solver.gpuSelection().add(gpu);
	}
	return solver;
    }

    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	propConfigUi.setEnabled(enabled);
    }

    class GpuSelectionProperty extends AbstractProperty<List<Gpu>> {

	private final List<Gpu> availableGpus;

	GpuSelectionProperty(String name, List<Gpu> value, List<Gpu> availableGpus) {
	    super(name, "GPU Selection", value);
	    this.availableGpus = availableGpus;
	}

	@Override
	protected void createConfigUi() {
	    // TODO Auto-generated method stub
	    var columns = new Object[] { "Vendor", "Name", "Weight", "Work Group Size", "X"};
	    
	    var data = new Object[availableGpus.size()][columns.length];
	    for(int i = 0; i < data.length; i++) {
		var gpu = availableGpus.get(i);
		data[i][0] = gpu.getInfo().vendor();
		data[i][1] = gpu.getInfo().name();
		data[i][2] = gpu.getConfig().getBenchmark(); // TODO: rename to "weight"
		data[i][3] = gpu.getConfig().getWorkgroupSize();
		data[i][4] = false;
	    }
	    data[0][4] = true; // default gpu is selected
	    
	    var model = new DefaultTableModel(data, columns);
	    var table = new JTable(model) {
		@Override
		public Class getColumnClass(int column) {
		    switch(column) {
		    case 0: return String.class;
		    case 1: return String.class;
		    case 2: return Double.class;
		    case 3: return Integer.class;
		    default: return Boolean.class;
		    }
		}
	    };
	    table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
	    table.setPreferredScrollableViewportSize(table.getPreferredSize());
	    add(new JScrollPane(table), new QuickGBC(0, 1).fill().size(4, 1).weight(1, 1));
	}

	@Override
	void setEnabled(boolean enabled) {
	    // TODO Auto-generated method stub
	}

    }

}
