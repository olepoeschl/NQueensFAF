package de.nqueensfaf.demo.gui;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;

import javax.swing.event.EventListenerList;

import de.nqueensfaf.core.AbstractSolver;
import de.nqueensfaf.core.AbstractSolver.OnProgressUpdateConsumer;

class SolverController {
        
    private final SolverModel model;
    
    private final EventListenerList listenerList = new EventListenerList();
    private final OnProgressUpdateConsumer onProgressUpdate = (progress, solutions, duration) -> {
	fireSolverProgressUpdate(new SolverProgressUpdateEvent(progress, solutions, duration));
    };
    private final Runnable onStart = () -> fireSolverStart(new SolverStartEvent());
    private final Runnable onFinish = () -> fireSolverFinish(new SolverFinishEvent());
    
    public SolverController(SolverModel model) {
	this.model = model;
    }
    
    SolverModel getModel() {
	return model;
    }
    
    void applySolverConfig(AbstractSolver solver) {
	solver.onProgressUpdate(onProgressUpdate);
	solver.onStart(onStart);
	solver.onFinish(onFinish);
	solver.setN(model.getN());
    }

    void addSolverProgressUpdateListener(SolverProgressUpdateListener l) {
	listenerList.add(SolverProgressUpdateListener.class, l);
    }

    void removeSolverProgressUpdateListener(SolverProgressUpdateListener l) {
	listenerList.remove(SolverProgressUpdateListener.class, l);
    }

    void addSolverStartListener(SolverStartListener l) {
	listenerList.add(SolverStartListener.class, l);
    }

    void removeSolverStartListener(SolverStartListener l) {
	listenerList.remove(SolverStartListener.class, l);
    }

    void addSolverFinishListener(SolverFinishListener l) {
	listenerList.add(SolverFinishListener.class, l);
    }

    void removeSolverFinishListener(SolverFinishListener l) {
	listenerList.remove(SolverFinishListener.class, l);
    }

    void fireSolverProgressUpdate(SolverProgressUpdateEvent e) {
	for (var l : listenerList.getListeners(SolverProgressUpdateListener.class)) {
	    try {
		EventQueue.invokeAndWait(() -> l.handleEvent(e));
	    } catch (InvocationTargetException e1) {
		e1.printStackTrace(); // TODO
	    } catch (InterruptedException e1) {
		e1.printStackTrace(); // TODO
	    }
	}
    }

    void fireSolverStart(SolverStartEvent e) {
	for (var l : listenerList.getListeners(SolverStartListener.class)) {
	    try {
		EventQueue.invokeAndWait(() -> l.handleEvent(e));
	    } catch (InvocationTargetException e1) {
		e1.printStackTrace(); // TODO
	    } catch (InterruptedException e1) {
		e1.printStackTrace(); // TODO
	    }
	}
    }

    void fireSolverFinish(SolverFinishEvent e) {
	for (var l : listenerList.getListeners(SolverFinishListener.class)) {
	    try {
		EventQueue.invokeAndWait(() -> l.handleEvent(e));
	    } catch (InvocationTargetException e1) {
		e1.printStackTrace(); // TODO
	    } catch (InterruptedException e1) {
		e1.printStackTrace(); // TODO
	    }
	}
    }

    static class SolverProgressUpdateEvent {
	final float progress;
	final long solutions;
	final long duration;

	SolverProgressUpdateEvent(float progress, long solutions, long duration) {
	    this.progress = progress;
	    this.solutions = solutions;
	    this.duration = duration;
	}
	
	float getProgress() {
	    return progress;
	}
	
	long getSolutions() {
	    return solutions;
	}
	
	long getDuration() {
	    return duration;
	}
    }

    static interface SolverProgressUpdateListener extends EventListener {
	void handleEvent(SolverProgressUpdateEvent e);
    }

    static class SolverStartEvent {
	SolverStartEvent(){
	}
    }

    static interface SolverStartListener extends EventListener {
	void handleEvent(SolverStartEvent e);
    }

    static class SolverFinishEvent {
	SolverFinishEvent(){
	}
    }

    static interface SolverFinishListener extends EventListener {
	void handleEvent(SolverFinishEvent e);
    }
}
