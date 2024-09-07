package de.nqueensfaf.demo.gui;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.AbstractProperty;
import de.nqueensfaf.demo.gui.util.QuickGBC;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

class GpuSolverConfigPanel extends de.nqueensfaf.demo.gui.MainFrame.SolverImplConfigPanel {

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

    @Override
    AbstractSolver getConfiguredSolver() {
	return solver;
    }

    @Override
    String isValidConfiguration() {
	// TODO
	return null;
    }

    class GpuSelectionProperty extends AbstractProperty<List<Gpu>> {

	private final List<Gpu> availableGpus;

	GpuSelectionProperty(String name, List<Gpu> value, List<Gpu> availableGpus) {
	    super(name, "GPU Selection", value);
	    this.availableGpus = availableGpus;
	}

	@Override
	protected void createConfigUi() {
	    var columns = new String[] { "Vendor", "Name", "Weight", "WGS", "X"};
	    
	    var data = new Object[availableGpus.size()][columns.length];
	    for(int i = 0; i < data.length; i++) {
		var gpu = availableGpus.get(i);
		data[i][0] = getShortNameOfGpuVendor(gpu.getInfo().vendor());
		data[i][1] = gpu.getInfo().name();
		data[i][2] = gpu.getConfig().getBenchmark(); // TODO: rename to "weight"
		data[i][3] = gpu.getConfig().getWorkgroupSize();
		data[i][4] = false;
	    }
	    
	    var model = new DefaultTableModel(data, columns);
	    model.addTableModelListener(e -> {
		int row = e.getFirstRow();
		int col = e.getColumn();
		
		switch(col) {
		case 2:
		    availableGpus.get(row).getConfig().setBenchmark((int) model.getValueAt(row, col));
		    break;
		case 3:
		    availableGpus.get(row).getConfig().setWorkgroupSize((int) model.getValueAt(row, col));
		    break;
		case 4:
		    boolean gpuSelected = (boolean) model.getValueAt(row, col);
		    if(gpuSelected)
			solver.gpuSelection().add(availableGpus.get(row));
		    else
			solver.gpuSelection().remove(availableGpus.get(row));
		}
	    });
	    
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
		
		@Override
		public boolean isCellEditable(int rowIndex, int colIndex) {
		    switch(colIndex) {
		    case 0:
		    case 1:
			return false;
		    default:
			return true;
		    }
		}
		
		@Override
		protected JTableHeader createDefaultTableHeader() {
		    return new JTableHeader(columnModel) {
			public String getToolTipText(MouseEvent e) {
			    String tip = null;
			    java.awt.Point p = e.getPoint();
			    int index = columnModel.getColumnIndexAtX(p.x);
			    int realIndex = 
				    columnModel.getColumn(index).getModelIndex();
			    switch(realIndex) {
			    case 3:
				return "Work Group Size";
			    case 4:
				return "Selected";
			    default:
				return columns[realIndex];
			    }
			}
		    };
		}
	    };
	    
	    // adjust column widths
	    var column = table.getColumnModel().getColumn(4);
	    column.setPreferredWidth(column.getMinWidth());
	    column = table.getColumnModel().getColumn(3);
	    column.setPreferredWidth(column.getMinWidth());
	    column = table.getColumnModel().getColumn(2);
	    column.setPreferredWidth(column.getMinWidth());
	    column = table.getColumnModel().getColumn(1);
	    column.setPreferredWidth(50);
	    column = table.getColumnModel().getColumn(0);
	    column.setPreferredWidth(50);
	    
	    table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
	    table.setPreferredScrollableViewportSize(table.getPreferredSize());
	    
	    add(new JScrollPane(table), new QuickGBC(0, 1).fill().size(4, 1).weight(1, 1).bottom(5));
	}

	@Override
	void setEnabled(boolean enabled) {
	    // TODO
	}

    }

    private static final String getShortNameOfGpuVendor(String vendor) {
	String tmp = vendor.toLowerCase();
	if(tmp.contains("advanced"))
	    return "AMD";
	else if(tmp.contains("nvidia"))
	    return "Nvidia";
	else if(tmp.contains("intel"))
	    return "Intel";
	else
	    return vendor;
    }
}
