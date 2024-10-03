package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.PropertyGroupConfigUi.AbstractProperty;
import de.nqueensfaf.demo.gui.Utils.LoadingWindow;
import de.nqueensfaf.impl.GpuSolver;
import de.nqueensfaf.impl.GpuSolver.Gpu;

@SuppressWarnings("serial")
class GpuSolverConfigPanel extends SolverImplConfigPanel {

    private final GpuSolverWithConfig model = new GpuSolverWithConfig();

    private final PropertyGroupConfigUi propConfigUi;
//    private final GpuSelectionProperty gpuSelectionProp;

    public GpuSolverConfigPanel() {
	propConfigUi = new PropertyGroupConfigUi(this);
	
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 6, 1);
	propConfigUi.addPropertyChangeListener("prequeens", e -> model.setPresetQueens((int) e.getNewValue()));
	
	final var gpus = model.getAvailableGpus();
	gpuSelectionProp = new GpuSelectionProperty("gpus", gpus.size() > 0 ? List.of(gpus.get(0)) : List.of(), gpus);
	propConfigUi.addProperty(gpuSelectionProp);
	
	if(model.getAvailableGpus().size() > 1) {
	    var autoWeightButton = new JButton("Auto-Configure GPU Weights");
	    autoWeightButton.addActionListener(e -> autoWeight());
	    add(autoWeightButton, new QuickGBC(0, 5).size(4, 1).weight(1, 0).anchor(QuickGBC.ANCHOR_CENTER).bottom(5));
	}
    }

    @Override
    SolverImplWithConfig getModel() {
	return model;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
	propConfigUi.setEnabled(enabled);
    }
    
    private void autoWeight() {
	var threads = new Thread[model.getAvailableGpus().size()];
	var solvers = new GpuSolver[model.getAvailableGpus().size()];
	
	int index = 0;
	for(var gpu : model.getAvailableGpus()) {
	    final var solver = new GpuSolver();
	    var selectedGpu = solver.getAvailableGpus().stream()
		    .filter(g -> g.getInfo().name().equals(gpu.getInfo().name())).findFirst().get();
	    selectedGpu.getConfig().setWorkgroupSize(64);
	    solver.gpuSelection().choose(selectedGpu);
	    solver.setPresetQueens(6);
	    solver.setN(19);
	    
	    var thread = Thread.ofVirtual().start(() -> solver.start());
	    
	    threads[index] = thread;
	    solvers[index] = solver;
	    index++;
	}
	
	Thread.ofVirtual().start(() -> {
	    var mainFrame = (JFrame) SwingUtilities.getWindowAncestor(GpuSolverConfigPanel.this);
	    var loadingWindow = new LoadingWindow(mainFrame, "Determining GPU weight distribution");
	    var progressBar = loadingWindow.getProgressBar();
	    
	    mainFrame.getGlassPane().setVisible(true);
	    for(int i = 0; i < mainFrame.getJMenuBar().getMenuCount(); i++)
		mainFrame.getJMenuBar().getMenu(i).setEnabled(false);
	    loadingWindow.setVisible(true);
	    
	    float progress = 0;
	    while(progress < 1) {
		progress = 0;
		for(var solver : solvers)
		    progress += solver.getProgress();
		progress /= solvers.length;
		final int progressVal = (int) (progress * 100);
		EventQueue.invokeLater(() -> progressBar.setValue(progressVal));
		try {
		    Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	    }
	    
	    long durationSum = 0;
	    for(var solver : solvers)
		durationSum += solver.getDuration();
	    for (var solver : solvers) {
		int portion = (int) ((1 - ((float) solver.getDuration() / durationSum)) * 100);
		gpuSelectionProp.setWeightForGpu(solver.gpuSelection().get().get(0).getId(), portion);
	    }

	    loadingWindow.setVisible(false);
	    loadingWindow.dispose();
	    for(int i = 0; i < mainFrame.getJMenuBar().getMenuCount(); i++)
		mainFrame.getJMenuBar().getMenu(i).setEnabled(true);
	    mainFrame.getGlassPane().setVisible(false);
	});
    }
    
    class GpuSolverWithConfig implements SolverImplWithConfig {
	
	private final GpuSolver solver = new GpuSolver();
	
	@Override
	public AbstractSolver getSolver() {
	    return solver;
	}

	@Override
	public String checkConfigValid() {
	    if(solver.getN() <= 6)
		return "This solver is only applicable for N >= 6";
	    
	    if(solver.gpuSelection().get().size() == 0)
		return "No GPUs selected";
	    if(solver.getPresetQueens() >= solver.getN() - 1)
		return "Number of pre placed queens must be lower than N - 1";
	    return "";
	}
	
	@Override
	public String getName() {
	    return "GPU";
	}
	
	@Override
	public String toString() {
	    int usedGpusSize = solver.gpuSelection().get().size();
	    switch(usedGpusSize) {
	    case 0:
		return "GPU";
	    case 1:
		return "GPU: " + solver.gpuSelection().get().get(0).getInfo().name();
	    default:
		return "GPU: Multi-GPU";
	    }
	}
	
	@Override
	public String getDiscipline() {
	    int usedGpusSize = solver.gpuSelection().get().size();
	    switch(usedGpusSize) {
	    case 0:
		return "GPU";
	    case 1:
		return "GPU: " + solver.gpuSelection().get().get(0).getInfo().name();
	    default:
		return "GPU: Multi-GPU";
	    }
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

	@Override
	public void loaded() {
	    propConfigUi.getProperty("prequeens").setEnabled(false);
	}
    }

    class GpuSelectionProperty extends AbstractProperty<List<Gpu>> {

	private final List<Gpu> availableGpus;
	
	private JTable table;
	private DefaultTableModel tableModel;

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
		data[i][2] = gpu.getConfig().getWeight();
		data[i][3] = gpu.getConfig().getWorkgroupSize();
		data[i][4] = false;
	    }
	    
	    tableModel = new DefaultTableModel(data, columns);
	    tableModel.addTableModelListener(e -> {
		int row = e.getFirstRow();
		int col = e.getColumn();
		
		switch(col) {
		case 2:
		    int weight = (int) tableModel.getValueAt(row, col);
		    if(weight <= 0) {
			Utils.error(GpuSolverConfigPanel.this, "GPU weight must be >= 1");
			tableModel.setValueAt(1, row, col);
			break;
		    }
		    availableGpus.get(row).getConfig().setWeight(weight);
		    break;
		case 3:
		    int workgroupSize = (int) tableModel.getValueAt(row, col);
		    if(workgroupSize <= 0) {
			Utils.error(GpuSolverConfigPanel.this, "GPU workgroup size must be >= 1");
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
	    
	    table = new JTable(tableModel) {
		@Override
		public Class<?> getColumnClass(int column) {
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
		    if(model.getSolver().getExecutionState().isBusy())
			return false;
		    
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
	    column.setPreferredWidth(70);
	    column = table.getColumnModel().getColumn(0);
	    column.setPreferredWidth(30);
	    
	    table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
	    table.setPreferredScrollableViewportSize(table.getPreferredSize());
	    
	    add(new JScrollPane(table), new QuickGBC(0, 1).fill().size(4, 1).weight(1, 1).bottom(5));
	}

	@Override
	void setEnabled(boolean enabled) {}

	@Override
	void updateUi(List<Gpu> value) {
	    // not needed
	}
	
	void setWeightForGpu(long gpuId, int weight) {
	    for(int i = 0; i < availableGpus.size(); i++) {
		final int finalI = i;
		var gpu = availableGpus.get(i);
		if(gpu.getId() == gpuId) {
		    gpu.getConfig().setWeight(weight);
		    EventQueue.invokeLater(() -> tableModel.setValueAt(weight, finalI, 2));
		    break;
		}
	    }
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
