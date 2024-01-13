package de.nqueensfaf.impl;

import static de.nqueensfaf.impl.InfoUtil.checkCLError;
import static de.nqueensfaf.impl.InfoUtil.getDeviceInfoStringUTF8;
import static de.nqueensfaf.impl.InfoUtil.getProgramBuildInfoStringASCII;
import static de.nqueensfaf.impl.SolverUtils.getJkl;
import static de.nqueensfaf.impl.SolverUtils.symmetry;
import static org.lwjgl.opencl.CL10.CL_COMPLETE;
import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NOT_FOUND;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_DEVICE_VENDOR;
import static org.lwjgl.opencl.CL10.CL_EVENT_COMMAND_EXECUTION_STATUS;
import static org.lwjgl.opencl.CL10.CL_MAP_WRITE;
import static org.lwjgl.opencl.CL10.CL_MEM_ALLOC_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_END;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_START;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueMapBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueUnmapMemObject;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clFlush;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetEventInfo;
import static org.lwjgl.opencl.CL10.clGetEventProfilingInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseEvent;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import de.nqueensfaf.Solver;

public class GPUSolver extends Solver implements Stateful {

    private ArrayList<GPU> availableGpus = new ArrayList<GPU>();
    private GPUSelection gpuSelection = new GPUSelection();
    private ArrayList<Constellation> constellations = new ArrayList<Constellation>();
    private int presetQueens = 6;
    private MultiGPULoadBalancing multiGpuLoadBalancingMode = MultiGPULoadBalancing.STATIC;
    
    private long start, duration, storedDuration;
    private boolean stateLoaded;
    
    public GPUSolver() {
	fetchAvailableGpus();
    }
    
    @Override
    public SolverState getState() {
	return new SolverState(getN(), getDuration(), constellations);
    }

    @Override
    public void setState(SolverState state) {
	setN(state.getN());
	storedDuration = state.getStoredDuration();
	constellations = state.getConstellations();
	stateLoaded = true;
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
	if(constellations.size() == 0)
	    return 0;
	
	int solvedConstellations = 0;
	for (var c : constellations) {
	    if (c.extractStart() == 69) // start=69 is for pseudo constellations
		continue;
	    if (c.getSolutions() >= 0) {
		solvedConstellations++;
	    }
	}
	return (float) solvedConstellations / constellations.size();
    }

    @Override
    public long getSolutions() {
	if(constellations.size() == 0)
	    return 0;
	
	return constellations.stream().filter(c -> c.getSolutions() >= 0).map(c -> c.getSolutions())
		.reduce(0l, (cAcc, c) -> cAcc + c);
    }

    private void fetchAvailableGpus() {
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
    
    public List<GPUInfo> getAvailableGpus() {
	ArrayList<GPUInfo> infos = new ArrayList<GPUInfo>(availableGpus.size());
	for(int i = 0; i < availableGpus.size(); i++)
	    infos.add(availableGpus.get(i).info);
	return infos;
    }

    public GPUSelection gpuSelection() {
	return gpuSelection;
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
	
	// sort selected GPUs by descending benchmark (the ones with higher scores come first)
	Collections.sort(gpuSelection.get(), (g1, g2) -> {
	    return Integer.compare(g2.benchmark, g1.benchmark);
	});
	
	if(!stateLoaded)
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);
	
	var remainingConstellations = constellations.stream().filter(c -> c.getSolutions() < 0)
		.collect(Collectors.toList());
	
	createOpenClObjects();
	start = System.currentTimeMillis();
	
	if(gpuSelection.get().size() == 1) {
	    singleGpu(gpuSelection.get().get(0), remainingConstellations);
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
	
	if(gpuSelection.get().size() == 1)
	    duration = gpuSelection.get().get(0).duration;
	else
	    duration = System.currentTimeMillis() - start;
	releaseOpenClObjects();
    }
    
    private void createOpenClObjects() {
	try (MemoryStack stack = MemoryStack.stackPush()) {
	    IntBuffer errBuf = stack.callocInt(1);
	    gpuSelection.get().stream().mapToLong(g1 -> g1.platform()).forEach(platform -> {
		var platformGpusList = gpuSelection.get().stream().filter(g2 -> g2.platform == platform).toList();
		PointerBuffer platformGpus = MemoryStack.stackPush().mallocPointer(platformGpusList.size());
		for (int i = 0; i < platformGpus.capacity(); i++) {
		    platformGpus.put(i, platformGpusList.get(i).info.id());
		}
		
		// create context
		PointerBuffer ctxProps = stack.mallocPointer(3);
		ctxProps.put(CL_CONTEXT_PLATFORM)
        		.put(platform)
        		.put(NULL).flip();
		long context = clCreateContext(ctxProps, platformGpus, null, NULL, errBuf);
		checkCLError(errBuf);
		
		// create program
		long program;
		try {
		    program = clCreateProgramWithSource(context, readKernelSource("kernels.c"), errBuf);
		    checkCLError(errBuf);
		} catch (IOException e) {
		    throw new RuntimeException("could not read OpenCL kernel source file: " + e.getMessage());
		}
		
		for(var gpu : platformGpusList) {
		    gpu.context = context;
		    gpu.program = program;
		    
		    // build program
		    String options = "-cl-std=CL1.2"
			    + " -D N=" + getN()
			    + " -D WORKGROUP_SIZE=" + gpu.workgroupSize
			    + " -Werror";
		    int error = clBuildProgram(program, platformGpus, options, null, NULL);
		    if (error != 0) {
			String buildLog = getProgramBuildInfoStringASCII(program, gpu.info.id(), CL_PROGRAM_BUILD_LOG);
			String msg = String.format("could not build OpenCL program: %s", error, buildLog);
			throw new RuntimeException(msg);
		    }
		    // create kernel
		    long kernel;
		    if (gpu.info.vendor().toLowerCase().contains("intel")) {
			kernel = clCreateKernel(program, "nqfaf_intel", errBuf);
		    } else if (gpu.info.vendor().toLowerCase().contains("nvidia")) {
			kernel = clCreateKernel(program, "nqfaf_nvidia", errBuf);
		    } else if (gpu.info.vendor().toLowerCase().contains("amd")
			    || gpu.info.vendor().toLowerCase().contains("advanced micro devices")) {
			kernel = clCreateKernel(program, "nqfaf_amd", errBuf);
		    } else {
			kernel = clCreateKernel(program, "nqfaf_nvidia", errBuf);
		    }
		    checkCLError(errBuf);
		    gpu.kernel = kernel;
		    
		    // create command queues
		    long xQueue = clCreateCommandQueue(context, gpu.info.id(), CL_QUEUE_PROFILING_ENABLE, errBuf);
		    checkCLError(errBuf);
		    gpu.xQueue = xQueue;
		    long memQueue = clCreateCommandQueue(context, gpu.info.id(), CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE,
			    errBuf);
		    checkCLError(errBuf);
		    gpu.memQueue = memQueue;
		}
	    });
	}
    }
    
    private void releaseOpenClObjects() {
	var released = new HashSet<Long>();
	for(var gpu : gpuSelection.get()) {
	    checkCLError(clReleaseCommandQueue(gpu.xQueue));
	    checkCLError(clReleaseCommandQueue(gpu.memQueue));
	    checkCLError(clReleaseKernel(gpu.kernel));
	    
	    if(!released.contains(gpu.program)) {
		checkCLError(clReleaseProgram(gpu.program));
		released.add(gpu.program);
	    }
	    
	    if(!released.contains(gpu.context)) {
		checkCLError(clReleaseContext(gpu.context));
		released.add(gpu.context);
	    }
	}
    }
    
    private void singleGpu(GPU gpu, List<Constellation> constellations) {
	sortConstellationsByJkl(constellations);
	constellations = fillWithPseudoConstellations(constellations, gpu.workgroupSize);
	
	try (MemoryStack stack = MemoryStack.stackPush()) {
	    IntBuffer errBuf = stack.callocInt(1);
	    
	    // create buffers and write them to the GPU
	    long constellationsMem = clCreateBuffer(gpu.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
		    constellations.size() * (4 + 4 + 4 + 4), errBuf);
	    checkCLError(errBuf);

	    ByteBuffer constellationPtr = clEnqueueMapBuffer(gpu.memQueue, constellationsMem, true, CL_MAP_WRITE,
		    0, constellations.size() * (4 + 4 + 4 + 4), null, null, errBuf, null);
	    checkCLError(errBuf);
	    for (int i = 0; i < constellations.size(); i++) {
		constellationPtr.putInt(i*(4+4+4+4), constellations.get(i).getLd());
		constellationPtr.putInt(i*(4+4+4+4)+4, constellations.get(i).getRd());
		constellationPtr.putInt(i*(4+4+4+4)+4+4, constellations.get(i).getCol());
		constellationPtr.putInt(i*(4+4+4+4)+4+4+4, constellations.get(i).getStartIjkl());
	    }
	    checkCLError(clEnqueueUnmapMemObject(gpu.memQueue, constellationsMem, constellationPtr, null, null));

	    long resMem = clCreateBuffer(gpu.context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
		    constellations.size() * 8, errBuf);
	    checkCLError(errBuf);
	    ByteBuffer resPtr = clEnqueueMapBuffer(gpu.memQueue, resMem, true, CL_MAP_WRITE, 0, constellations.size() * 8,
		    null, null, errBuf, null);
	    checkCLError(errBuf);
	    for (int i = 0; i < constellations.size(); i++) {
		resPtr.putLong(i * 8, constellations.get(i).getSolutions());
	    }
	    checkCLError(clEnqueueUnmapMemObject(gpu.memQueue, resMem, resPtr, null, null));

	    checkCLError(clFlush(gpu.memQueue));
	    checkCLError(clFinish(gpu.memQueue));

	    // set kernel args
	    LongBuffer constellationsArg = stack.mallocLong(1);
	    constellationsArg.put(0, constellationsMem);
	    checkCLError(clSetKernelArg(gpu.kernel, 0, constellationsArg));

	    LongBuffer resArg = stack.mallocLong(1);
	    resArg.put(0, resMem);
	    checkCLError(clSetKernelArg(gpu.kernel, 1, resArg));

	    // define kernel dimensions
	    final int dimensions = 1;
	    PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
	    globalWorkSize.put(0, constellations.size());
	    PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
	    localWorkSize.put(0, gpu.workgroupSize);

	    // run kernel
	    final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1);
	    checkCLError(clEnqueueNDRangeKernel(gpu.xQueue, gpu.kernel, dimensions, null, globalWorkSize,
		    localWorkSize, null, xEventBuf));
	    checkCLError(clFlush(gpu.xQueue));

	    // read start and end times using an event
	    long xEvent = xEventBuf.get(0);
	    
	    // wait for kernel to finish and continuously read results from gpu
	    IntBuffer eventStatusBuf = stack.mallocInt(1);
	    while (true) {
		if(getUpdateInterval() > 0)
		    readResults(gpu.memQueue, resMem, resPtr, constellations);

		checkCLError(clGetEventInfo(xEvent, CL_EVENT_COMMAND_EXECUTION_STATUS, eventStatusBuf, null));
		if (eventStatusBuf.get(0) == CL_COMPLETE)
		    break;
		
		try {
		    Thread.sleep(50);
		} catch (InterruptedException e) {
		    // ignore
		}
	    }
	    
	    // read final results
	    readResults(gpu.memQueue, resMem, resPtr, constellations);

	    // read gpu kernel profiled time
	    LongBuffer startBuf = BufferUtils.createLongBuffer(1), endBuf = BufferUtils.createLongBuffer(1);
	    int err = clGetEventProfilingInfo(xEvent, CL_PROFILING_COMMAND_START, startBuf, null);
	    checkCLError(err);
	    err = clGetEventProfilingInfo(xEvent, CL_PROFILING_COMMAND_END, endBuf, null);
	    checkCLError(err);
	    gpu.duration += (endBuf.get(0) - startBuf.get(0)) / 1000000; // convert nanoseconds to ms
	    
	    // release memory and event
	    checkCLError(clReleaseMemObject(constellationsMem));
	    checkCLError(clReleaseMemObject(resMem));
	    checkCLError(clReleaseEvent(xEvent));
	}
    }
    
    private void readResults(long memQueue, long resMem, ByteBuffer resPtr, List<Constellation> constellations) {
	// read result and progress memory buffers
	checkCLError(clEnqueueReadBuffer(memQueue, resMem, true, 0, resPtr, null, null));
	for (int i = 0; i < constellations.size(); i++) {
	    if (constellations.get(i).extractStart() == 69) // start=69 is for trash constellations
		continue;
	    long solutionsForConstellation = resPtr.getLong(i * 8)
		    * symmetry(getN(), constellations.get(i).extractIjkl());
	    if (solutionsForConstellation >= 0)
		// synchronize with the list of constellations on the RAM
		constellations.get(i).setSolutions(solutionsForConstellation);
	}
    }

    private void multiGpuStaticLoadBalancing(List<Constellation> constellations) {
	int benchmarkSum = gpuSelection.get().stream().map(gpu -> gpu.benchmark()).reduce(0, (wAcc, benchmark) -> wAcc + benchmark);
	
	int fromIndex = 0;
	HashMap<GPU, List<Constellation>> gpuConstellations = new HashMap<GPU, List<Constellation>>();
	var iterator = gpuSelection.get().iterator();
	while(iterator.hasNext()) {
	    var gpu = iterator.next();
	    int portionPercentage = (gpu.benchmark * 100) / benchmarkSum;
	    int toIndex = fromIndex + (portionPercentage * constellations.size()) / 100;
	    if(toIndex < constellations.size() && iterator.hasNext())
		toIndex = findNextIjklChangeIndex(constellations, toIndex);
	    else
		toIndex = constellations.size();
	    gpuConstellations.put(gpu, constellations.subList(fromIndex, toIndex));
	    fromIndex = toIndex;
	}
	
	gpuSelection.get().stream().parallel().forEach(gpu -> {
	    if(gpuConstellations.get(gpu).size() != 0)
		singleGpu(gpu, gpuConstellations.get(gpu));
	});
    }
    
    private void multiGpuDynamicLoadBalancing(List<Constellation> constellations) {
	/*
	 * final int minConstellationsPerIterationPerGpu = 10_000; final int
	 * minConstellationsPerIteration = gpuSelection.get().size() *
	 * minConstellationsPerIterationPerGpu; int constellationPtr = 0;
	 * 
	 * // create contexts, compile programs, create kernels // TODO
	 * 
	 * // initialize: distribute gpu benchmarks equally if one or more gpu's benchmarks
	 * are set to 0 if(gpuSelection.get().stream().anyMatch(gpu -> gpu.benchmark ==
	 * 0f)) for(var gpu : gpuSelection.get()) gpu.benchmark = 1f /
	 * gpuSelection.get().size();
	 * 
	 * ExecutorService executor =
	 * Executors.newFixedThreadPool(gpuSelection.get().size());
	 * while(constellationPtr < constellations.size()) { int
	 * endOfConstellationRange = findNextIjklChangeIndex(constellations,
	 * constellationPtr + minConstellationsPerIteration - 1);
	 * if(endOfConstellationRange == 0) // ijkl does not change anymore from this
	 * index on endOfConstellationRange = constellations.size() - 1; // so
	 * solve all constellations
	 * 
	 * var constellationsForIteration =
	 * constellations.subList(constellationPtr, endOfConstellationRange);
	 * 
	 * // now distribute those constellations to all selected GPUs
	 * gpuSelection.get().parallelStream().forEach(gpu -> { // TODO });
	 * 
	 * // TODO }
	 */
    }
    
    // utils
    private String readKernelSource(String filepath) throws IOException {
	String resultString = null;
	try (InputStream clSourceFile = GPUSolverOld.class.getClassLoader().getResourceAsStream(filepath);
		BufferedReader br = new BufferedReader(new InputStreamReader(clSourceFile));) {
	    String line = null;
	    StringBuilder result = new StringBuilder();
	    while ((line = br.readLine()) != null) {
		result.append(line);
		result.append("\n");
	    }
	    resultString = result.toString();
	} catch (IOException e) {
	    throw new IOException("could not read kernel source file: " + e.getMessage()); // should not happen
	}
	return resultString;
    }
    
    private int findNextIjklChangeIndex(List<Constellation> constellations, int fromIndex) {
	int currentIjkl = constellations.get(fromIndex).extractIjkl();
	for(int i = fromIndex; i < constellations.size(); i++) {
	    if(constellations.get(i).extractIjkl() != currentIjkl)
		return i;
	}
	return 0;
    }

    private void sortConstellationsByJkl(List<Constellation> constellations) {
	Collections.sort(constellations, new Comparator<Constellation>() {
	    @Override
	    public int compare(Constellation o1, Constellation o2) {
		return Integer.compare(getJkl(o1.extractIjkl()), getJkl(o2.extractIjkl()));
	    }
	});
    }
    
    private List<Constellation> fillWithPseudoConstellations(List<Constellation> constellations, int workgroupSize) {
	sortConstellationsByJkl(constellations);
	
	ArrayList<Constellation> newConstellations = new ArrayList<Constellation>();
	int currentJkl = constellations.get(0).getStartIjkl() & ((1 << 15) - 1);
	for (var c : constellations) {
	    // iterate through constellations, add each remaining constellations and fill up
	    // each group of ijkl till its dividable by workgroup-size
	    if (c.getSolutions() >= 0)
		continue;

	    if ((c.getStartIjkl() & ((1 << 15) - 1)) != currentJkl) { // check if new ijkl is found
		while (newConstellations.size() % workgroupSize != 0) {
		    addPseudoConstellation(newConstellations);
		}
		currentJkl = c.getStartIjkl() & ((1 << 15) - 1);
	    }
	    newConstellations.add(c);
	}
	while (newConstellations.size() % workgroupSize != 0) {
	    addPseudoConstellation(newConstellations);
	}
	
	return newConstellations;
    }

    private void addPseudoConstellation(List<Constellation> constellations) {
	constellations.add(new Constellation(-1, (1 << getN()) - 1, (1 << getN()) - 1, (1 << getN()) - 1, (69 << 20), -2));
    }
    
    // setters and getters
    public GPUSolver setPresetQueens(int presetQueens) {
	this.presetQueens = presetQueens;
	return this;
    }
    
    public int getPresetQueens() {
	return presetQueens;
    }
    
    public GPUSolver setMultiGpuLoadBalancingMode(MultiGPULoadBalancing mode) {
	if(mode == MultiGPULoadBalancing.DYNAMIC)
	    throw new IllegalStateException("could not apply dynamic multi gpu load balancing: not implemented yet");
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
	
	public void add(long gpuId, int benchmark, int workgroupSize) {
	    try {
		GPU gpu = availableGpus.stream().filter(g -> g.info.id() == gpuId).findFirst().get();
		
		if(benchmark != 0)
		    gpu.benchmark = benchmark;
		
		if(workgroupSize != 0)
		    gpu.workgroupSize = workgroupSize;
		
		selectedGpus.add(gpu);
	    } catch (NoSuchElementException e) {
		throw new IllegalArgumentException("invalid gpu id");
	    }
	}
	
	private ArrayList<GPU> get(){
	    return selectedGpus;
	}
    }
    
    public static record GPUInfo(long id, String vendor, String name) {}
    
    private class GPU {
	private GPUInfo info;
	private int benchmark = 1;
	private int workgroupSize = 64;
	
	// measured kernel duration
	private long duration;
	
	// related opencl objects
	private long platform, context, program, kernel, xQueue, memQueue;
	
	private GPU(){
	}
	
	private int benchmark() {
	    return benchmark;
	}
	
	private long platform() {
	    return platform;
	}
    }
}
