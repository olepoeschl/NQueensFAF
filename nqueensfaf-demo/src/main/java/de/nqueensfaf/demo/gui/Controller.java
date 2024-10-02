package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.event.EventListenerList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
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
	// TODO: auto save
    };
    private final Runnable onStart = this::fireSolverStarted;
    private final Runnable onFinish = () -> {
	fireSolverFinished();
	fireSolverTerminated();
    };
    private final Consumer<Exception> onCancel = e -> fireSolverTerminated();
    
    public Controller() {
	view = new View(this, model);
	kryo.setRegistrationRequired(false);
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
	
	final var symSolver = model.configureAndGetSymSolver();
	Thread.ofVirtual().start(() -> symSolver.start());
	try {
	    solver.start();
	} catch(Exception e) {
	    symSolver.cancel();
	    // TODO: tell the View to show an error message
	}
    }
    
    public void save(File file) {
	var solverExtensionConfig = new HashMap<String, Object>();
	model.getSelectedSolverExtension().getConfig(solverExtensionConfig);
	
	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(file)))) {
	    
	    var snapshot = new Snapshot(model.getSelectedSolverExtension().getSolver().getSavePoint(),
		    solverExtensionConfig, model.getAutoSaveInterval()); // TODO
	    kryo.writeClassAndObject(output, snapshot);
	    
	    fireSolverSaved();
	    
	} catch (KryoException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    public void restore(File file) {
	try (Input input = new Input(new GZIPInputStream(new FileInputStream(file)))) {
	    
	    var snapshot = (Snapshot) kryo.readClassAndObject(input);
	    model.getSelectedSolverExtension().getSolver().loadSavePoint(snapshot.savePoint());
	    model.setN(snapshot.savePoint().getN()); // TODO
	    model.getSelectedSolverExtension().setConfig(snapshot.solverExtensionConfig());
	    model.setAutoSaveInterval(snapshot.autoSaveInterval()); // TODO

	    model.updateSolverProgress(
		    snapshot.savePoint().getProgress(), 
		    snapshot.savePoint().getSolutions(), 
		    0, 
		    snapshot.savePoint().getDuration()
		    );
	    
	    fireSolverRestored();
	    
	} catch (KryoException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    public void reset() {
	model.getSelectedSolverExtension().getSolver().reset();
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
