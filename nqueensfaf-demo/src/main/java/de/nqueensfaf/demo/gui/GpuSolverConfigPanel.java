package de.nqueensfaf.demo.gui;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.AbstractProperty;
import de.nqueensfaf.demo.gui.util.Dialog;
import de.nqueensfaf.demo.gui.util.QuickGBC;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

class GpuSolverConfigPanel extends SolverImplConfigPanel {

    private final GpuSolverConfig model = new GpuSolverConfig();

    private final PropertyGroupConfigUi propConfigUi;

    public GpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 4, 1);
	propConfigUi.addPropertyChangeListener("prequeens", e -> model.setPresetQueens((int) e.getNewValue()));
	
	final var gpus = model.getAvailableGpus();
	propConfigUi.addProperty(
		new GpuSelectionProperty("gpus", gpus.size() > 0 ? List.of(gpus.get(0)) : List.of(), gpus));
    }

    @Override
    SolverImplConfig getModel() {
	return model;
    }
    
    class GpuSolverConfig implements SolverImplConfig {
	
	private final GpuSolver solver = new GpuSolver();
	
	@Override
	public AbstractSolver getConfiguredSolver() {
	    return solver;
	}

	@Override
	public String checkValid() {
	    if(solver.getPresetQueens() >= solver.getN() - 1)
		return "Number of pre placed queens must be lower than N - 1";
	    return "";
	}
	
	List<Gpu> getAvailableGpus(){
	    return solver.getAvailableGpus();
	}
	
	void addGpu(Gpu gpu) {
	    solver.gpuSelection().add(gpu);
	}
	
	void removeGpu(Gpu gpu) {
	    solver.gpuSelection().remove(gpu);
	}
	
	void setPresetQueens(int prequeens) {
	    solver.setPresetQueens(prequeens);
	}
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
	    
	    var tableModel = new DefaultTableModel(data, columns);
	    tableModel.addTableModelListener(e -> {
		int row = e.getFirstRow();
		int col = e.getColumn();
		
		switch(col) {
		case 2:
		    int weight = (int) tableModel.getValueAt(row, col);
		    if(weight <= 0) {
			Dialog.error("GPU weight must be >= 1");
			tableModel.setValueAt(1, row, col);
			break;
		    }
		    availableGpus.get(row).getConfig().setBenchmark(weight);
		    break;
		case 3:
		    int workgroupSize = (int) tableModel.getValueAt(row, col);
		    if(workgroupSize <= 0) {
			Dialog.error("GPU workgroup size must be >= 1");
			tableModel.setValueAt(1, row, col);
			break;
		    }
		    availableGpus.get(row).getConfig().setWorkgroupSize(workgroupSize);
		    break;
		case 4:
		    boolean gpuSelected = (boolean) tableModel.getValueAt(row, col);
		    if(gpuSelected)
			model.addGpu(availableGpus.get(row));
		    else
			model.removeGpu(availableGpus.get(row));
		}
	    });
	    
	    var table = new JTable(tableModel) {
		@Override
		public Class getColumnClass(int column) {
		    switch(column) {
		    case 0: return String.class;
		    case 1: return String.class;
		    case 2: return Integer.class;
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
