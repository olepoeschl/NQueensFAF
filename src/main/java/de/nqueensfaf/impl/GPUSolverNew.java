package de.nqueensfaf.impl;

import static de.nqueensfaf.impl.InfoUtil.checkCLError;
import static de.nqueensfaf.impl.InfoUtil.getDeviceInfoStringUTF8;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NOT_FOUND;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_DEVICE_VENDOR;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import de.nqueensfaf.Solver;

public class GPUSolverNew extends Solver {

    private ArrayList<GPU> availableGpus;
    private GPUSelection gpuSelection;
    private ArrayList<Constellation> constellations;
    private int presetQueens = 6;
    private MultiGPULoadBalancing multiGpuLoadBalancingMode = MultiGPULoadBalancing.DYNAMIC;
    
    private long start, duration, storedDuration;
    private boolean stateLoaded;
    
    public GPUSolverNew() {
	availableGpus = new ArrayList<GPU>();
	
	fetchAvailableGpus();
	gpuSelection = new GPUSelection();
    }
    
    private void fetchAvailableGpus() {
	availableGpus.clear();
	
	try (MemoryStack stack = MemoryStack.stackPush()) {
	    IntBuffer clCountBuf = stack.mallocInt(1);
	    checkCLError(clGetPlatformIDs(null, clCountBuf));
	    if (clCountBuf.get(0) == 0) {
		return; // no gpus found
	    }

	    PointerBuffer platforms = stack.mallocPointer(clCountBuf.get(0));
	    checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));

	    for (int p = 0; p < platforms.capacity(); p++) {
		long platform = platforms.get(p);
		int error = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, clCountBuf);
		if (error == CL_DEVICE_NOT_FOUND) {
		    continue; // no gpus found for this platform
		}
		
		PointerBuffer gpusBuf = stack.mallocPointer(clCountBuf.get(0));
		checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, gpusBuf, (IntBuffer) null));
		for (int g = 0; g < gpusBuf.capacity(); g++) {
		    long gpuId = gpusBuf.get(g);
		    GPUInfo gpuInfo = new GPUInfo(gpuId, getDeviceInfoStringUTF8(gpuId, CL_DEVICE_VENDOR), 
			    getDeviceInfoStringUTF8(gpuId, CL_DEVICE_NAME));
			    
		    GPU gpu = new GPU();
		    gpu.info = gpuInfo;
		    gpu.platform = platform;
		    
		    availableGpus.add(gpu);
		}
	    }
	}
    }
    
    public GPUInfo[] getAvailableGpus() {
	GPUInfo[] infos = new GPUInfo[availableGpus.size()];
	for(int i = 0; i < availableGpus.size(); i++)
	    infos[i] = availableGpus.get(i).info;
	return infos;
    }

    public GPUSelection gpuSelection() {
	return gpuSelection;
    }
    
    @Override
    public long getDuration() {
	if (isRunning() && start != 0) {
	    return System.currentTimeMillis() - start + storedDuration;
	}
	return duration;
    }

    @Override
    public float getProgress() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public long getSolutions() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    protected void run() {
	if (getN() <= 6) { // if n is very small, use the simple Solver from the parent class
	    start = System.currentTimeMillis();
	    constellations.add(new Constellation());
	    constellations.get(0).setSolutions(solveSmallBoard());
	    duration = System.currentTimeMillis() - start;
	    return;
	}

	if (gpuSelection.get().size() == 0)
	    throw new IllegalStateException("could not run GPUSolver: no GPUs selected");
	
	if(!stateLoaded)
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);
	
	var remainingConstellations = constellations.stream().filter(c -> c.getSolutions() < 0)
		.collect(Collectors.toList());
	
	if(gpuSelection.get().size() == 1) {
	    singleGpu(remainingConstellations);
	} else {
	    switch(multiGpuLoadBalancingMode) {
	    case STATIC:
		multiGpuStaticLoadBalancing(remainingConstellations);
		break;
	    case DYNAMIC:
		multiGpuDynamicLoadBalancing(remainingConstellations);
		break;
	    }
	}
	
	
	// set duration when solver is finished
	duration = 1;
    }
    
    private void singleGpu(List<Constellation> remainingConstellations) {
	
    }
    
    private void multiGpuStaticLoadBalancing(List<Constellation> remainingConstellations) {
	
    }
    
    private void multiGpuDynamicLoadBalancing(List<Constellation> remainingConstellations) {
	final int minConstellationsPerIterationPerGpu = 10_000;
	final int minConstellationsPerIteration = gpuSelection.get().size() * minConstellationsPerIterationPerGpu;
	int constellationPtr = 0;

	// create contexts, compile programs, create kernels
	// TODO
	
	// initialize: distribute gpu weights equally if one or more gpu's weights are set to 0
	if(gpuSelection.get().stream().anyMatch(gpu -> gpu.weightPercentage == 0f))
	    for(var gpu : gpuSelection.get())
		gpu.weightPercentage = 1f / gpuSelection.get().size();
	
	ExecutorService executor = Executors.newFixedThreadPool(gpuSelection.get().size());
	while(constellationPtr < remainingConstellations.size()) {
	    int endOfConstellationRange = findNextIjklChangeIndex(remainingConstellations, constellationPtr + minConstellationsPerIteration - 1);
	    if(endOfConstellationRange == 0) // ijkl does not change anymore from this index on
		endOfConstellationRange = remainingConstellations.size() - 1; // so solve all remaining constellations

	    var constellationsForIteration = remainingConstellations.subList(constellationPtr, endOfConstellationRange);
	    
	    // now distribute those constellations to all selected GPUs
	    gpuSelection.get().parallelStream().forEach(gpu -> {
		// TODO
	    });
	    
	    // TODO
	}
    }
    
    private int findNextIjklChangeIndex(List<Constellation> constellations, int fromIndex) {
	int currentIjkl = constellations.get(fromIndex).extractIjkl();
	for(int i = fromIndex; i < constellations.size(); i++) {
	    if(constellations.get(i).extractIjkl() != currentIjkl)
		return i;
	}
	return 0;
    }
    
    public void reset() {
	gpuSelection = new GPUSelection();
	constellations = null;
	start = 0;
	duration = 0;
	storedDuration = 0;
	presetQueens = 6;
    }

    public GPUSolverNew setPresetQueens(int presetQueens) {
	this.presetQueens = presetQueens;
	return this;
    }
    
    public int getPresetQueens() {
	return presetQueens;
    }
    
    public GPUSolverNew setMultiGpuLoadBalancingMode(MultiGPULoadBalancing mode) {
	this.multiGpuLoadBalancingMode = mode;
	return this;
    }
    
    public MultiGPULoadBalancing getMultiGpuLoadBalancingMode() {
	return multiGpuLoadBalancingMode;
    }
    
    public static enum MultiGPULoadBalancing {
	STATIC, DYNAMIC
    }
    
    public class GPUSelection {
	private ArrayList<GPU> selectedGpus;
	
	public GPUSelection() {
	    selectedGpus = new ArrayList<GPU>();
	}
	
	public void add(long gpuId) {
	    add(gpuId, 0f, 64);
	}
	
	public void add(long gpuId, float weightPercentage, int workgroupSize) {
	    try {
		GPU gpu = availableGpus.stream().filter(g -> g.info.id == gpuId).findFirst().get();
		gpu.weightPercentage = weightPercentage;
		gpu.workgroupSize = workgroupSize;
		selectedGpus.add(gpu);
	    } catch (NoSuchElementException e) {
		throw new IllegalArgumentException("invalid gpu id");
	    }
	}
	
	public ArrayList<GPU> get(){
	    return selectedGpus;
	}
    }
    
    public static record GPUInfo(long id, String vendor, String name) {}
    
    private class GPU {
	GPUInfo info;
	float weightPercentage;
	int workgroupSize;
	
	// related opencl objects
	long platform, context, program, kernel, xQueue, memQueue;
	Long constellationsMem, resMem;
	ByteBuffer resPtr;
	long xEvent;
	
	GPU(){
	}
    }

}
