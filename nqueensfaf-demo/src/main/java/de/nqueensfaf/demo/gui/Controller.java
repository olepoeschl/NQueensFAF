package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.io.File;
import java.util.EventListener;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.swing.event.EventListenerList;

import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;

public class Controller {

    private final EventListenerList listeners = new EventListenerList();
    
    private final Model model = new Model();
    private final View view;
    
    // solver instance callbacks
    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	model.setProgress(progress);
	model.setSolutions(solutions);
	model.setUniqueSolutions(model.getCurrentSymSolver().getUniqueSolutionsTotal(solutions));
	model.setDuration(duration);
	
	// TODO: auto save
    };
    private final Runnable onStart = this::fireSolverStarted;
    private final Runnable onFinish = () -> {
	fireSolverFinished();
	fireSolverTerminated();
    };
    private final Consumer<Exception> onCancel = e -> fireSolverTerminated();
    
    public Controller() {
	view = new View(model);
    }

    public void createAndShowView() {
	EventQueue.invokeLater(view::createAndShowUi);
    }
    
    public void start() {
	var solver = model.getSelectedSolverExtension().getSolver();
	solver.onStart(onStart);
	solver.onProgressUpdate(onProgressUpdate);
	solver.onFinish(onFinish);
	solver.onCancel(onCancel);
	solver.setUpdateInterval(model.getSettings().getUpdateInterval());
	
	final var symSolver = model.getConfiguredSymSolver();
	Thread.ofVirtual().start(() -> symSolver.start());
	try {
	    solver.start();
	} catch(Exception e) {
	    symSolver.cancel();
	    // TODO: tell the View to show an error message
	}
    }
    
    public void save() {
	var solverExtensionConfig = new HashMap<String, Object>();
	model.getSelectedSolverExtension().getCurrentConfig(solverExtensionConfig);
	var snapshot = new Snapshot(model.getSelectedSolverExtension().getSolver().getSavePoint(), solverExtensionConfig, model.getSettings());
	
	// TODO: write snapshot to file
	
	fireSolverSaved();
    }
    
    public void restore(File file) {
	// TODO
	
	fireSolverRestored();
    }
    
    public void reset() {
	// TODO
	
	fireSolverReset();
    }
    
    // solver event listeners
    public void addSolverListener(SolverAdapter a) {
	listeners.add(SolverAdapter.class, a);
    }
    
    private void fireSolverStarted() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    EventQueue.invokeLater(() -> listener.solverStarted());
	}
    }

    private void fireSolverFinished() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    EventQueue.invokeLater(() -> listener.solverFinished());
	}
    }
    
    private void fireSolverTerminated() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    EventQueue.invokeLater(() -> listener.solverTerminated());
	}
    }

    private void fireSolverSaved() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    EventQueue.invokeLater(() -> listener.solverSaved());
	}
    }

    private void fireSolverRestored() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    EventQueue.invokeLater(() -> listener.solverRestored());
	}
    }

    private void fireSolverReset() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    EventQueue.invokeLater(() -> listener.solverReset());
	}
    }
    
    // classes and types
    public static interface SolverAdapter extends EventListener {
	default void solverStarted() {}
	default void solverFinished() {}
	default void solverTerminated() {}
	default void solverSaved() {}
	default void solverRestored() {}
	default void solverReset() {}
    }
}
