package de.nqueensfaf.demo.gui;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.event.EventListenerList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;

public class Controller {

    private final EventListenerList listeners = new EventListenerList();
    
    private final Model model = new Model();
    private final View view;
    
    private final Kryo kryo = new Kryo();
    
    // solver instance callbacks
    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	model.updateSolverProgress(progress, solutions, model.getCurrentSymSolver().getUniqueSolutionsTotal(solutions), 
		duration);
	autoSave(progress);
    };
    private final Runnable onStart = this::fireSolverStarted;
    private final Runnable onFinish = () -> {
	fireSolverFinished();
	fireSolverTerminated();
    };
    private final Consumer<Exception> onCancel = e -> fireSolverTerminated();
    
    private boolean currentlySaving = false;
    private int lastAutoSave = 0;
    
    public Controller() {
	view = new View(this, model);
	kryo.setRegistrationRequired(false);
	
	addSolverListener(new SolverAdapter() {
	    @Override
	    public void solverStarted() {
		if(!model.isRestored())
		    model.updateSolverProgress(0, 0, 0, 0);
	    }
	    @Override
	    public void solverTerminated() {
		var solver = model.getSelectedSolverExtension().getSolver();
		model.updateSolverProgress(solver.getProgress(), solver.getSolutions(),
			model.getCurrentSymSolver().getUniqueSolutionsTotal(solver.getSolutions()),
			solver.getDuration());
		model.setRestored(false);
	    }
	});
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
	solver.setN(model.getN());
	solver.setUpdateInterval(model.getSettings().getUpdateInterval());
	
	final var symSolver = model.configureAndGetSymSolver();
	Thread.ofVirtual().start(() -> symSolver.start());
	Thread.ofVirtual().start(() -> {
	    try {
		solver.start();
	    } catch(Exception e) {
		symSolver.cancel();
		view.error(e.getMessage());
	    }
	});
    }
    
    public void manualSave(File file) {
	EventQueue.invokeLater(() -> view.setCursor(new Cursor(Cursor.WAIT_CURSOR)));
	
	try {
	    save(file);
	} catch (Exception e) {
	    view.error("could not save solver: " + e.getMessage());
	}
	
	EventQueue.invokeLater(() -> view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)));
    }

    // TODO: does the solver support this?
    private void save(File file) throws IOException {
	var solverExtensionConfig = new HashMap<String, Object>();
	model.getSelectedSolverExtension().getConfig(solverExtensionConfig);
	
	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(file)))) {
	    
	    var snapshot = new Snapshot(model.getSelectedSolverExtension().getSolver().getSavePoint(),
		    solverExtensionConfig, model.getAutoSaveInterval());
	    kryo.writeClassAndObject(output, snapshot);
	    
	    fireSolverSaved();
	}
    }

    // TODO: does the solver support this?
    public void restore(File file) {
	EventQueue.invokeLater(() -> view.setCursor(new Cursor(Cursor.WAIT_CURSOR)));
	
	try (Input input = new Input(new GZIPInputStream(new FileInputStream(file)))) {
	    
	    var snapshot = (Snapshot) kryo.readClassAndObject(input);
	    model.getSelectedSolverExtension().getSolver().restoreSavePoint(snapshot.savePoint());
	    model.setN(snapshot.savePoint().getN());
	    model.getSelectedSolverExtension().setConfig(snapshot.solverExtensionConfig());
	    model.setAutoSaveInterval(snapshot.autoSaveInterval());

	    model.updateSolverProgress(
		    snapshot.savePoint().getProgress(), 
		    snapshot.savePoint().getSolutions(), 
		    0, 
		    snapshot.savePoint().getDuration()
		    );
	    
	    model.setRestored(true);
	    
	    fireSolverRestored();
	    
	} catch (Exception e) {
	    view.error("could not restore solver: " + e.getMessage());
	}
	
	EventQueue.invokeLater(() -> view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)));
    }

    // TODO: does the solver support this?
    public void reset() {
	model.getSelectedSolverExtension().getSolver().reset();
	model.setRestored(false);
	fireSolverReset();
    }
    
    private void autoSave(float progressFloat) {
	if(model.getAutoSaveInterval() <= 0) 
	    return;
	if(currentlySaving)
	    return;
	int progress = (int) (progressFloat * 100);
	if(progress - lastAutoSave >= model.getAutoSaveInterval()) {
	    Thread.ofVirtual().start(() -> {
		try {
		    save(new File(model.getN() + "-queens.faf"));
		} catch (IOException ex) {
		    view.error(ex.getMessage());
		}
	    });
	    lastAutoSave = progress;
	}
    }
    
    // solver event listeners
    public void addSolverListener(SolverAdapter a) {
	listeners.add(SolverAdapter.class, a);
    }
    
    private void fireSolverStarted() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    listener.solverStarted();
	}
    }

    private void fireSolverFinished() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    listener.solverFinished();
	}
    }
    
    private void fireSolverTerminated() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    listener.solverTerminated();
	}
    }

    private void fireSolverSaved() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    listener.solverSaved();
	}
    }

    private void fireSolverRestored() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    listener.solverRestored();
	}
    }

    private void fireSolverReset() {
	for(var listener : listeners.getListeners(SolverAdapter.class)) {
	    listener.solverReset();
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
