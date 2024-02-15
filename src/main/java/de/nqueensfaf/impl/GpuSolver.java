package de.nqueensfaf.impl;

import static de.nqueensfaf.impl.InfoUtil.checkCLError;
import static de.nqueensfaf.impl.InfoUtil.getDeviceInfoStringUTF8;
import static de.nqueensfaf.impl.InfoUtil.getProgramBuildInfoStringASCII;
import static de.nqueensfaf.impl.Utils.getJkl;
import static de.nqueensfaf.impl.Utils.symmetry;
import static org.lwjgl.opencl.CL12.CL_COMPLETE;
import static org.lwjgl.opencl.CL12.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL12.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL12.CL_DEVICE_NOT_FOUND;
import static org.lwjgl.opencl.CL12.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL12.CL_DEVICE_VENDOR;
import static org.lwjgl.opencl.CL12.CL_EVENT_COMMAND_EXECUTION_STATUS;
import static org.lwjgl.opencl.CL12.CL_MAP_WRITE;
import static org.lwjgl.opencl.CL12.CL_MEM_ALLOC_HOST_PTR;
import static org.lwjgl.opencl.CL12.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL12.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL12.CL_PROFILING_COMMAND_END;
import static org.lwjgl.opencl.CL12.CL_PROFILING_COMMAND_START;
import static org.lwjgl.opencl.CL12.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL12.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL12.clBuildProgram;
import static org.lwjgl.opencl.CL12.clCreateBuffer;
import static org.lwjgl.opencl.CL12.clCreateCommandQueue;
import static org.lwjgl.opencl.CL12.clCreateContext;
import static org.lwjgl.opencl.CL12.clCreateKernel;
import static org.lwjgl.opencl.CL12.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL12.clEnqueueMapBuffer;
import static org.lwjgl.opencl.CL12.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL12.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL12.clEnqueueUnmapMemObject;
import static org.lwjgl.opencl.CL12.clFinish;
import static org.lwjgl.opencl.CL12.clFlush;
import static org.lwjgl.opencl.CL12.clGetDeviceIDs;
import static org.lwjgl.opencl.CL12.clGetEventInfo;
import static org.lwjgl.opencl.CL12.clGetEventProfilingInfo;
import static org.lwjgl.opencl.CL12.clGetPlatformIDs;
import static org.lwjgl.opencl.CL12.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL12.clReleaseContext;
import static org.lwjgl.opencl.CL12.clReleaseEvent;
import static org.lwjgl.opencl.CL12.clReleaseKernel;
import static org.lwjgl.opencl.CL12.clReleaseMemObject;
import static org.lwjgl.opencl.CL12.clReleaseProgram;
import static org.lwjgl.opencl.CL12.clSetKernelArg;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.opencl.NVCreateBuffer.clCreateBufferNV;
import static org.lwjgl.opencl.NVCreateBuffer.CL_MEM_PINNED_NV;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import de.nqueensfaf.Solver;

public class GpuSolver extends Solver<GpuSolver> implements Stateful {

    private ArrayList<Gpu> availableGpus = new ArrayList<Gpu>();
    private GPUSelection gpuSelection = new GPUSelection();
    private ArrayList<Constellation> constellations = new ArrayList<Constellation>();
    private int presetQueens = 6;
    
    private long start, duration, storedDuration;
    private boolean stateLoaded, ready = true;
    
    public GpuSolver() {
	fetchAvailableGpus();
    }

    // getters and setters
    public int getPresetQueens() {
	return presetQueens;
    }
    
    public GpuSolver setPresetQueens(int presetQueens) {
	this.presetQueens = presetQueens;
	return this;
    }
    
    public void reset() {
	constellations.clear();
	start = duration = storedDuration = 0;
	for(var gpu : gpuSelection.get())
	    gpu.duration = 0;
	stateLoaded = false;
	ready = true;
    }
    
    @Override
    public SolverState getState() {
	return new SolverState(getN(), getDuration(), constellations);
    }

    @Override
    public void setState(SolverState state) {
	if(!ready)
	    throw new IllegalStateException("could not set solver state: solver was already used and must be reset first");
	reset();
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
		    GpuInfo gpuInfo = new GpuInfo(gpuId, getDeviceInfoStringUTF8(gpuId, CL_DEVICE_VENDOR), 
			    getDeviceInfoStringUTF8(gpuId, CL_DEVICE_NAME));
			    
		    Gpu gpu = new Gpu();
		    gpu.info = gpuInfo;
		    gpu.platform = platform;
		    
		    availableGpus.add(gpu);
		}
	    }
	}
    }
    
    public List<GpuInfo> getAvailableGpus() {
	ArrayList<GpuInfo> infos = new ArrayList<GpuInfo>(availableGpus.size());
	for(int i = 0; i < availableGpus.size(); i++)
	    infos.add(availableGpus.get(i).info);
	return infos;
    }

    public GPUSelection gpuSelection() {
	return gpuSelection;
    }

    @Override
    protected void run() {
	ready = false;
	
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
	    sortConstellationsByJkl(remainingConstellations);
	    remainingConstellations = new ArrayList<>(fillWithPseudoConstellations(remainingConstellations, gpuSelection.get().get(0).workgroupSize));
	    
	    createBuffers(gpuSelection.get().get(0), remainingConstellations.size());
	    singleGpu(gpuSelection.get().get(0), remainingConstellations);
	    releaseBuffers(gpuSelection.get().get(0));
	} else {
	    multiGpu(remainingConstellations);
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
	    gpuSelection.get().stream().mapToLong(g1 -> g1.platform()).distinct().forEach(platform -> {		var platformGpusList = gpuSelection.get().stream().filter(g2 -> g2.platform == platform).toList();
		PointerBuffer platformGpus = MemoryStack.stackPush().mallocPointer(platformGpusList.size());
		for (int i = 0; i < platformGpus.capacity(); i++) {
		    platformGpus.put(i, platformGpusList.get(i).info.id());
		}

		for(var gpu : platformGpusList) {
		    // create context
		    PointerBuffer ctxProps = stack.mallocPointer(3);
		    ctxProps.put(CL_CONTEXT_PLATFORM)
        		    .put(platform)
        		    .put(NULL).flip();
		    long context = clCreateContext(ctxProps, gpu.info.id, null, NULL, errBuf);
		    checkCLError(errBuf);
		    gpu.context = context;
			
		    // create program
		    long program;
		    try {
			program = clCreateProgramWithSource(context, readKernelSource("kernels.c"), errBuf);
			checkCLError(errBuf);
		    } catch (IOException e) {
			throw new RuntimeException("could not read OpenCL kernel source file: " + e.getMessage(), e);
		    }
		    // build program
		    String options = "" // "-cl-std=CL1.2"
			    + " -D N=" + getN()
			    + " -D WORKGROUP_SIZE=" + gpu.workgroupSize
			    + " -Werror";
		    int error = clBuildProgram(program, gpu.info.id(), options, null, NULL);
		    if (error != 0) {
			String buildLog = getProgramBuildInfoStringASCII(program, gpu.info.id(), CL_PROGRAM_BUILD_LOG);
			String msg = String.format("could not build OpenCL program: %s", error, buildLog);
			throw new RuntimeException(msg);
		    }
		    gpu.program = program;

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
		    long memQueue = clCreateCommandQueue(context, gpu.info.id(), 0,
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
    
    private void createBuffers(Gpu gpu, int maxNumOfConstellations) {
	try (MemoryStack stack = MemoryStack.stackPush()) {
	    IntBuffer errBuf = stack.callocInt(1);
	    
	    if(gpu.info.vendor.toLowerCase().contains("nvidia"))
		gpu.constellationsMem = clCreateBufferNV(gpu.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, 
			CL_MEM_PINNED_NV, maxNumOfConstellations * (4 + 4 + 4 + 4), errBuf);
	    else
		gpu.constellationsMem = clCreateBuffer(gpu.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, 
			maxNumOfConstellations * (4 + 4 + 4 + 4), errBuf);
	    checkCLError(errBuf);
	    
	    if(gpu.info.vendor.toLowerCase().contains("nvidia"))
		gpu.resMem = clCreateBufferNV(gpu.context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
			CL_MEM_PINNED_NV, maxNumOfConstellations * 8, errBuf);
	    else
		gpu.resMem = clCreateBuffer(gpu.context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
			maxNumOfConstellations * 8, errBuf);
	    checkCLError(errBuf);

	    checkCLError(clFlush(gpu.memQueue));
	    checkCLError(clFinish(gpu.memQueue));
	}
    }
    
    private void releaseBuffers(Gpu gpu) {
	checkCLError(clReleaseMemObject(gpu.constellationsMem));
	checkCLError(clReleaseMemObject(gpu.resMem));
    }
    
    private void singleGpu(Gpu gpu, List<Constellation> constellations) {
	try (MemoryStack stack = MemoryStack.stackPush()) {
	    IntBuffer errBuf = stack.callocInt(1);
	    
	    // write data GPU buffers
	    ByteBuffer constellationPtr = clEnqueueMapBuffer(gpu.memQueue, gpu.constellationsMem, true, CL_MAP_WRITE,
		    0, constellations.size() * (4 + 4 + 4 + 4), null, null, errBuf, null);
	    checkCLError(errBuf);
	    for (int i = 0; i < constellations.size(); i++) {
		constellationPtr.putInt(i*(4+4+4+4), constellations.get(i).getLd());
		constellationPtr.putInt(i*(4+4+4+4)+4, constellations.get(i).getRd());
		constellationPtr.putInt(i*(4+4+4+4)+4+4, constellations.get(i).getCol());
		constellationPtr.putInt(i*(4+4+4+4)+4+4+4, constellations.get(i).getStartIjkl());
	    }
	    checkCLError(clEnqueueUnmapMemObject(gpu.memQueue, gpu.constellationsMem, constellationPtr, null, null));

	    ByteBuffer resPtr = clEnqueueMapBuffer(gpu.memQueue, gpu.resMem, true, CL_MAP_WRITE, 0, constellations.size() * 8,
		    null, null, errBuf, null);
	    checkCLError(errBuf);
	    for (int i = 0; i < constellations.size(); i++) {
		resPtr.putLong(i * 8, constellations.get(i).getSolutions());
	    }
	    checkCLError(clEnqueueUnmapMemObject(gpu.memQueue, gpu.resMem, resPtr, null, null));

	    checkCLError(clFlush(gpu.memQueue));
	    checkCLError(clFinish(gpu.memQueue));

	    // set kernel args
	    LongBuffer constellationsArg = stack.mallocLong(1);
	    constellationsArg.put(0, gpu.constellationsMem);
	    checkCLError(clSetKernelArg(gpu.kernel, 0, constellationsArg));

	    LongBuffer resArg = stack.mallocLong(1);
	    resArg.put(0, gpu.resMem);
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
		    readResults(gpu.memQueue, gpu.resMem, resPtr, constellations);

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
	    readResults(gpu.memQueue, gpu.resMem, resPtr, constellations);

	    // read gpu kernel profiled time
	    LongBuffer startBuf = BufferUtils.createLongBuffer(1), endBuf = BufferUtils.createLongBuffer(1);
	    int err = clGetEventProfilingInfo(xEvent, CL_PROFILING_COMMAND_START, startBuf, null);
	    checkCLError(err);
	    err = clGetEventProfilingInfo(xEvent, CL_PROFILING_COMMAND_END, endBuf, null);
	    checkCLError(err);
	    gpu.duration += (endBuf.get(0) - startBuf.get(0)) / 1000000; // convert nanoseconds to ms
	    
	    // release memory and event
	    checkCLError(clReleaseEvent(xEvent));
	}
    }
    
    private void multiGpu(List<Constellation> constellations) {
	sortConstellationsByJkl(constellations);
	var selectedGpus = gpuSelection.get();
	
	int firstWorkloadToIndex = (int) (constellations.size() * 0.6);
	
	var firstWorkload = constellations.subList(0, findNextJklChangeIndex(constellations, firstWorkloadToIndex));
	
	var benchmarkRatioFromFirstGpu = new float[selectedGpus.size()];
	float factor = 0; // for solving c1 + c2 + c... + cx = total number of constellations
	// c1 (constellations for gpu 1) is calculated using this formula
	// then the other c's are calculated based on c1 and its ratio to them
	
	for(int i = 0; i < selectedGpus.size(); i++) {
	    benchmarkRatioFromFirstGpu[i] = (float) selectedGpus.get(0).benchmark / selectedGpus.get(i).benchmark;
	    factor += benchmarkRatioFromFirstGpu[i];
	}
	int numConstellationsFirstGpu = (int) (firstWorkload.size() / factor);
	
	final float finalFactor = factor;
	
	int fromIndex = 0;
	HashMap<Gpu, List<Constellation>> gpuConstellations = new HashMap<Gpu, List<Constellation>>();
	
	for(int i = 0; i < selectedGpus.size(); i++) {
	    var gpu = selectedGpus.get(i);
	    
	    int toIndex = (int) (fromIndex + numConstellationsFirstGpu * benchmarkRatioFromFirstGpu[i]);
	    
	    if(toIndex < firstWorkload.size() && i < selectedGpus.size() - 1)
		toIndex = findNextJklChangeIndex(firstWorkload, toIndex);
	    else
		toIndex = firstWorkload.size();
	    
	    var gpuWorkload = firstWorkload.subList(fromIndex, toIndex);
	    gpuWorkload = fillWithPseudoConstellations(gpuWorkload, gpu.workgroupSize);
		    
	    gpuConstellations.put(gpu, gpuWorkload);
	    
	    createBuffers(gpu, gpuWorkload.size());
	    
	    fromIndex = toIndex;
	}
	
	var queue = new ConcurrentLinkedQueue<>(constellations.subList(fromIndex, constellations.size()));
	var executor = Executors.newFixedThreadPool(selectedGpus.size());
	
	for(int gpuIdx = 0; gpuIdx < selectedGpus.size(); gpuIdx++) {
	    final int finalGpuIdx = gpuIdx;
	    final var gpu = selectedGpus.get(gpuIdx);
	    
	    executor.execute(() -> {
		// first workload (the biggest one)
		if(gpuConstellations.get(gpu).size() > 0)
		    singleGpu(gpu, gpuConstellations.get(gpu));
		
		var workload = new ArrayList<Constellation>();

		int remaining;
		while((remaining = queue.size()) > 0) {
		    int workloadSize = (int) (remaining / finalFactor * benchmarkRatioFromFirstGpu[finalGpuIdx]);
		    if(workloadSize < 512)
			workloadSize = 512;
		    
		    for(int i = 0; i < workloadSize; i++) {
			synchronized(queue) {
			    if(queue.isEmpty())
				break;
			    workload.add(queue.remove());
			}
		    }

		    if(workload.size() > 0) {
			workload = new ArrayList<>(fillWithPseudoConstellations(workload, gpu.workgroupSize));
			singleGpu(gpu, workload);
		    }
		}
	    });
	}
	
	executor.shutdown();
	try {
	    executor.awaitTermination(10000, TimeUnit.DAYS);
	} catch (InterruptedException e) {
	    throw new RuntimeException("could not wait for termination of GpuSolver: " + e.getMessage());
	}
	
	for(var gpu : selectedGpus) {
	    releaseBuffers(gpu);
	}
    }
    
    // utils
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

    private String readKernelSource(String filepath) throws IOException {
	String resultString = null;
	try (InputStream clSourceFile = GpuSolver.class.getClassLoader().getResourceAsStream(filepath);
		BufferedReader br = new BufferedReader(new InputStreamReader(clSourceFile));) {
	    String line = null;
	    StringBuilder result = new StringBuilder();
	    while ((line = br.readLine()) != null) {
		result.append(line);
		result.append("\n");
	    }
	    resultString = result.toString();
	} catch (IOException e) {
	    throw new IOException("could not read kernel source file: " + e.getMessage(), e); // should not happen
	}
	return resultString;
    }
    
    private int findNextJklChangeIndex(List<Constellation> constellations, int fromIndex) {
	int currentIjkl = getJkl(constellations.get(fromIndex).extractIjkl());
	for(int i = fromIndex; i < constellations.size(); i++) {
	    if(getJkl(constellations.get(i).extractIjkl()) != currentIjkl)
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
    
    public class GPUSelection {
	private ArrayList<Gpu> selectedGpus = new ArrayList<Gpu>();
	private boolean chosen = false;
	
	private GPUSelection() {}
	
	public void choose(long gpuId) {
	    add(gpuId, 1, 64);
	    chosen = true;
	}
	
	public void add(long gpuId, int benchmark, int workgroupSize) {
	    if(chosen)
		throw new IllegalStateException("unable to add more GPU's after choosing one");
	    
	    if(benchmark <= 0)
		throw new IllegalArgumentException("benchmark must not be <= 0");
	    
	    if(selectedGpus.stream().anyMatch(gpu -> gpu.info.id() == gpuId))
		throw new IllegalArgumentException("GPU with id " + gpuId + " was already added");
		
	    try {
		Gpu gpu = availableGpus.stream().filter(g -> g.info.id() == gpuId).findFirst().get();
		
		if(benchmark != 0)
		    gpu.benchmark = benchmark;
		
		if(workgroupSize != 0)
		    gpu.workgroupSize = workgroupSize;
		
		selectedGpus.add(gpu);
	    } catch (NoSuchElementException e) {
		throw new IllegalArgumentException("no GPU found for id " + gpuId);
	    }
	}
	
	private ArrayList<Gpu> get(){
	    return selectedGpus;
	}
    }
    
    public static record GpuInfo(long id, String vendor, String name) {}
    
    private class Gpu {
	private GpuInfo info;
	private int benchmark = 1;
	private int workgroupSize = 64;
	
	// measured kernel duration
	private long duration;
	
	// related opencl objects
	private long platform, context, program, kernel, xQueue, memQueue, constellationsMem, resMem;
	
	private Gpu(){}
	
	private long platform() {
	    return platform;
	}
    }
}
