package de.nqueensfaf.demo.gui.extension;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.demo.gui.util.QuickGBC;
import de.nqueensfaf.demo.gui.util.Utils.LoadingWindow;
import de.nqueensfaf.impl.GpuSolver;

public class GpuSolverExtension implements SolverExtension {

    private final GpuSolver solver = new GpuSolver();
    private JPanel configUi;
    
    public GpuSolverExtension() {
	createConfigUi();
    }
    
    private void createConfigUi() {
	var propConfigUi = new PropertyGroupConfigUi();
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 6, 1);
	propConfigUi.getProperty("prequeens").addChangeListener(e -> solver.setPresetQueens((int) e.getNewValue()));
	
	int gridy = propConfigUi.getNextFreeY();
	var table = createGpuSelectionTable();
	propConfigUi.add(new JScrollPane(table), new QuickGBC(0, gridy).fill().size(4, 1).weight(1, 1).top(5).bottom(5));
	gridy++;
	
	configUi = propConfigUi;
	
	if(solver.getAvailableGpus().size() > 1) {
	    var autoWeightButton = new JButton("Auto-Configure GPU Weights");
	    autoWeightButton.addActionListener(e -> autoWeight((DefaultTableModel) table.getModel()));
	    propConfigUi.add(autoWeightButton, new QuickGBC(0, gridy).size(4, 1).weight(1, 0).anchor(QuickGBC.ANCHOR_CENTER).bottom(5));
	}
    }
    
    @SuppressWarnings("serial")
    private JTable createGpuSelectionTable() {
	var availableGpus = solver.getAvailableGpus();
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

	var tableModel = new DefaultTableModel(data, columns);
	tableModel.addTableModelListener(e -> {
	    int row = e.getFirstRow();
	    int col = e.getColumn();

	    switch(col) {
	    case 2:
		int weight = (int) tableModel.getValueAt(row, col);
		if(weight <= 0) {
		    tableModel.setValueAt(1, row, col);
		    break;
		}
		availableGpus.get(row).getConfig().setWeight(weight);
		break;
	    case 3:
		int workgroupSize = (int) tableModel.getValueAt(row, col);
		if(workgroupSize <= 0) {
		    tableModel.setValueAt(1, row, col);
		    break;
		}
		availableGpus.get(row).getConfig().setWorkgroupSize(workgroupSize);
		break;
	    case 4:
		boolean gpuSelected = (boolean) tableModel.getValueAt(row, col);
		if(gpuSelected)
		    solver.gpuSelection().add(availableGpus.get(row));
		else
		    solver.gpuSelection().remove(availableGpus.get(row));
	    }
	});

	var table = new JTable(tableModel) {
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
		if(solver.getExecutionState().isBusy())
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
	
	return table;
    }

    private void autoWeight(DefaultTableModel tableModel) {
	var threads = new Thread[solver.getAvailableGpus().size()];
	var solvers = new GpuSolver[solver.getAvailableGpus().size()];
	
	int index = 0;
	for(var gpu : solver.getAvailableGpus()) {
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
	    var mainFrame = (JFrame) SwingUtilities.getWindowAncestor(configUi);
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
		
		for(int i = 0; i < solver.getAvailableGpus().size(); i++) {
		    final int finalI = i;
		    var gpu = solver.getAvailableGpus().get(i);
		    if(gpu.getId() == solver.gpuSelection().get().get(0).getId()) {
			gpu.getConfig().setWeight(portion);
			EventQueue.invokeLater(() -> tableModel.setValueAt(portion, finalI, 2));
			break;
		    }
		}
	    }

	    loadingWindow.setVisible(false);
	    loadingWindow.dispose();
	    for(int i = 0; i < mainFrame.getJMenuBar().getMenuCount(); i++)
		mainFrame.getJMenuBar().getMenu(i).setEnabled(true);
	    mainFrame.getGlassPane().setVisible(false);
	});
    }
    
    @Override
    public AbstractSolver getSolver() {
	return solver;
    }

    @Override
    public String getName() {
	return "GPU";
    }
    
    @Override
    public String getCurrentRecordCategory() {
	if(solver.gpuSelection().get().size() == 1)
	    return "GPU: " + solver.gpuSelection().get().get(0).getInfo().name();
	return "GPU: Multi-GPU";
    }
    
    @Override
    public JComponent getConfigUi() {
	return configUi;
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
