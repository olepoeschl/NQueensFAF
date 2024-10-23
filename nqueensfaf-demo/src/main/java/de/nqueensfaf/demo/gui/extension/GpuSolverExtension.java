package de.nqueensfaf.demo.gui.extension;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
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
    
    private PropertyGroupConfigUi configUi;
    private JTable gpuSelectionTable;
    
    public GpuSolverExtension() {
	createConfigUi();
    }
    
    private void createConfigUi() {
	var propConfigUi = new PropertyGroupConfigUi();
	propConfigUi.addIntProperty("prequeens", "Pre-placed Queens", 4, 8, 6, 1);
	propConfigUi.getProperty("prequeens").addChangeListener(e -> solver.setPresetQueens((int) e.getNewValue()));
	
	int gridy = propConfigUi.getNextFreeY();
	gpuSelectionTable = createGpuSelectionTable();
	propConfigUi.add(new JScrollPane(gpuSelectionTable), new QuickGBC(0, gridy).fill().size(4, 1).weight(1, 1).top(5).bottom(5));
	gridy++;
	
	configUi = propConfigUi;
	
	if(solver.getAvailableGpus().size() > 1) {
	    var autoWeightButton = new JButton("Auto-Configure GPU Weights");
	    autoWeightButton.addActionListener(e -> autoWeight((DefaultTableModel) gpuSelectionTable.getModel()));
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
    
    @Override
    public void setConfig(Map<String, Object> configMap) {
	if(!configMap.containsKey("gpu"))
	    throw new IllegalArgumentException("invalid config for this solver");
	
	for(var key : configMap.keySet()) {
	    switch(key) {
	    case "prequeens":
		configUi.getProperty(key).setValue(configMap.get(key));
		break;
	    case "gpuSelection":
		@SuppressWarnings("unchecked")
		var gpuSelectionList = (ArrayList<String>) configMap.get(key);
		
		for(var entry : gpuSelectionList) {
		    var properties = entry.split("[0-9a-zA-Z]*,");
		    
		    String name;
		    boolean selected;
		    int weight, workgroupSize;
		    
		    for(var prop : properties) {
			if(prop.startsWith("name="))
			    name = prop.substring(5);
			else if(prop.startsWith("selected="))
			    selected = Boolean.parseBoolean(prop.substring(9));
			else if(prop.startsWith("weight="))
			    weight = Integer.parseInt(prop.substring(7));
			else if(prop.startsWith("workgroupSize="))
			    workgroupSize = Integer.parseInt(prop.substring(14));
		    }
		    
		    // TODO: update table ui and solver data
		}
		break;
	    }
	}
    }
    
    @Override
    public Map<String, Object> getConfig() {
	var tableModel = gpuSelectionTable.getModel();
	
	int toggleSelectionIndex = gpuSelectionTable.getColumnModel().getColumnIndex("X");
	int nameIndex = gpuSelectionTable.getColumnModel().getColumnIndex("Name");
	int weightIndex = gpuSelectionTable.getColumnModel().getColumnIndex("Weight");
	int workgroupSizeIndex = gpuSelectionTable.getColumnModel().getColumnIndex("WGS");
	
	var configMap = new HashMap<String, Object>();
	configMap.put("prequeens", configUi.getProperty("prequeens").getValue());
	
	var gpuSelectionList = new ArrayList<String>();
	for(int row = 0; row < gpuSelectionTable.getRowCount(); row++) {
	    var name = tableModel.getValueAt(row, nameIndex);
	    var selected = (Boolean) tableModel.getValueAt(row, toggleSelectionIndex);
	    var weight = (Integer) tableModel.getValueAt(row, weightIndex);
	    var workgroupSize = (Integer) tableModel.getValueAt(row, workgroupSizeIndex);
	    
	    var stringBuilder = new StringBuilder();
	    stringBuilder.append("name=").append(name);
	    stringBuilder.append(',').append("selected=").append(selected);
	    stringBuilder.append(',').append("weight=").append(weight);
	    stringBuilder.append(',').append("workgroupSize=").append(workgroupSize);
	    
	    gpuSelectionList.add(stringBuilder.toString());
	}
	configMap.put("gpuSelection", gpuSelectionList);
	
	return configMap;
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
