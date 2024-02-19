package de.nqueensfaf.impl;

import static de.nqueensfaf.impl.InfoUtil.checkCLError;
import static de.nqueensfaf.impl.InfoUtil.getDeviceInfoStringUTF8;
import static de.nqueensfaf.impl.InfoUtil.getProgramBuildInfoStringASCII;
import static de.nqueensfaf.impl.Utils.getJkl;
import static de.nqueensfaf.impl.Utils.getj;
import static de.nqueensfaf.impl.Utils.getk;
import static de.nqueensfaf.impl.Utils.getl;
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
    private GpuSelection gpuSelection = new GpuSelection();
    private ArrayList<Constellation> constellations = new ArrayList<Constellation>();
    private int presetQueens = 6;

    private long start, duration, storedDuration;
    private boolean stateLoaded, ready = true;

    private int L;

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
	for (var gpu : gpuSelection.get())
	    gpu.reset();
	stateLoaded = false;
	ready = true;
    }

    @Override
    public SolverState getState() {
	return new SolverState(getN(), getDuration(), constellations);
    }

    @Override
    public void setState(SolverState state) {
	if (!ready)
	    throw new IllegalStateException(
		    "could not set solver state: solver was already used and must be reset first");
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
	if (constellations.size() == 0)
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
	if (constellations.size() == 0)
	    return 0;

	return constellations.stream().filter(c -> c.getSolutions() >= 0).map(c -> c.getSolutions()).reduce(0l,
		(cAcc, c) -> cAcc + c);
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
	for (int i = 0; i < availableGpus.size(); i++)
	    infos.add(availableGpus.get(i).info);
	return infos;
    }

    public GpuSelection gpuSelection() {
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

	// sort selected GPUs by descending benchmark (the ones with better benchmarks
	// come first)
	Collections.sort(gpuSelection.get(), (g1, g2) -> {
	    return Float.compare(g1.benchmark, g2.benchmark);
	});

	if (!stateLoaded)
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);

	var remainingConstellations = constellations.stream().filter(c -> c.getSolutions() < 0)
		.collect(Collectors.toList());
	if (remainingConstellations.size() == 0)
	    return; // nothing to do

	for (var gpu : gpuSelection.get())
	    gpu.createOpenClObjects();

	start = System.currentTimeMillis();
	
	L = 1 << (getN() - 1);

	if (gpuSelection.get().size() == 1) {
	    singleGpu(gpuSelection.get().get(0), remainingConstellations);
	} else {
	    multiGpu(remainingConstellations);
	}

	if (gpuSelection.get().size() == 1)
	    duration = gpuSelection.get().get(0).duration;
	else
	    duration = System.currentTimeMillis() - start;

	for (var gpu : gpuSelection.get())
	    gpu.releaseOpenClObjects();
    }

    private void singleGpu(Gpu gpu, List<Constellation> constellations) {
	constellations = new ArrayList<>(
		fillWithPseudoConstellations(constellations, gpuSelection.get().get(0).workgroupSize));

	gpu.createBuffers(constellations.size());
	gpu.executeWorkload(constellations);
	gpu.releaseBuffers();
    }

    private void multiGpu(List<Constellation> constellations) {
	sortConstellationsByJkl(constellations);
	var selectedGpus = gpuSelection.get();

	float benchmarkSum = selectedGpus.stream().map(Gpu::getBenchmark).reduce(0f, Float::sum);
	float[] gpuPortions = new float[selectedGpus.size()];
	float gpuPortionSum = 0f;
	for (int i = 0; i < selectedGpus.size(); i++) {
	    gpuPortions[i] = (benchmarkSum / selectedGpus.get(i).benchmark);
	    gpuPortionSum += gpuPortions[i];
	}
	for (int i = 0; i < selectedGpus.size(); i++) {
	    gpuPortions[i] /= gpuPortionSum;
	}

	// if very few constellations, enqueue all at once
	final float portionPerIteration = (constellations.size() < 10_000 * selectedGpus.size()) ? 1f : 0.6f;
	final int minGpuWorkloadSize = 1024;

	// first workload is the biggest one, then they shrink with each iteration
	var firstWorkloads = new ArrayList<List<Constellation>>(selectedGpus.size());

	int firstWorkloadSize = (int) (constellations.size() * portionPerIteration);
	int fromIndex = 0;
	for (int gpuIdx = 0; gpuIdx < selectedGpus.size(); gpuIdx++) {
	    int toIndex = fromIndex + (int) (firstWorkloadSize * gpuPortions[gpuIdx]);
	    if (gpuIdx == selectedGpus.size() - 1)
		toIndex = firstWorkloadSize;

	    var gpuFirstWork = constellations.subList(fromIndex, toIndex);
	    if (gpuFirstWork.size() == 0) {
		firstWorkloads.add(new ArrayList<Constellation>());
		continue;
	    }

	    var gpuFirstWorkload = fillWithPseudoConstellations(gpuFirstWork, selectedGpus.get(gpuIdx).workgroupSize);
	    firstWorkloads.add(gpuFirstWorkload);

	    var gpu = selectedGpus.get(gpuIdx);
	    gpu.createBuffers(gpuFirstWorkload.size());

	    fromIndex = toIndex;
	}

	var queue = new ConcurrentLinkedQueue<>(constellations.subList(fromIndex, constellations.size()));
	var executor = Executors.newFixedThreadPool(selectedGpus.size());

	for (int idx = 0; idx < selectedGpus.size(); idx++) {
	    final int gpuIdx = idx;
	    final var gpu = selectedGpus.get(idx);

	    if (firstWorkloads.get(gpuIdx).size() == 0)
		continue;

	    executor.execute(() -> {
		var workload = firstWorkloads.get(gpuIdx);
		int gpuFirstWorkloadSize = workload.size();
		int iteration = 0;

		do {
		    gpu.executeWorkload(workload);
		    workload.clear();

		    if (queue.isEmpty())
			break;

		    iteration++;

		    int workloadSize = (int) (gpuFirstWorkloadSize * Math.pow(portionPerIteration, iteration) * 0.9f);
		    if (workloadSize < minGpuWorkloadSize)
			workloadSize = minGpuWorkloadSize;
		    while (workload.size() < workloadSize && !queue.isEmpty()) {
			synchronized (queue) {
			    for (int i = 0; i < gpu.workgroupSize && workload.size() < workloadSize
				    && !queue.isEmpty(); i++) {
				workload.add(queue.remove());
			    }
			}
		    }

		    if (workload.isEmpty())
			break;

		    workload = fillWithPseudoConstellations(workload, gpu.workgroupSize);

		    while (workload.size() > gpu.maxNumOfConstellationsPerRun) { // if workload too big, give some work
										 // back to the queue
			for (int i = 0; i < gpu.workgroupSize; i++) {
			    var c = workload.remove(workload.size() - 1);
			    if (c.extractStart() != 69)
				queue.add(c);
			}
		    }
		} while (true);
	    });
	}

	executor.shutdown();
	try {
	    executor.awaitTermination(10000, TimeUnit.DAYS);
	} catch (InterruptedException e) {
	    throw new RuntimeException("could not wait for termination of GpuSolver: " + e.getMessage());
	}

	for (var gpu : selectedGpus) {
	    if (gpu.maxNumOfConstellationsPerRun == 0)
		continue;
	    gpu.releaseBuffers();
	}
    }

    // utils
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
	int currentJkl = getJkl(constellations.get(fromIndex).extractIjkl());
	for (int i = fromIndex; i < constellations.size(); i++) {
	    if (getJkl(constellations.get(i).extractIjkl()) != currentJkl)
		return i;
	}
	return constellations.size();
    }

    private void sortConstellationsByJkl(List<Constellation> constellations) {
	Collections.sort(constellations, new Comparator<Constellation>() {
	    @Override
	    public int compare(Constellation o1, Constellation o2) {
		return Integer.compare(getJkl(o1.extractIjkl()), getJkl(o2.extractIjkl()));
	    }
	});
    }

    private ArrayList<Constellation> fillWithPseudoConstellations(List<Constellation> constellations,
	    int workgroupSize) {
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
	constellations
		.add(new Constellation(-1, (1 << getN()) - 1, (1 << getN()) - 1, (1 << getN()) - 1, (69 << 20), -2));
    }

    public class GpuSelection {
	private ArrayList<Gpu> selectedGpus = new ArrayList<Gpu>();
	private boolean chosen = false;

	private GpuSelection() {
	}

	public void choose(long gpuId) {
	    add(gpuId, 1, 64);
	    chosen = true;
	}

	public GpuSelection add(long gpuId, int benchmark, int workgroupSize) {
	    if (chosen)
		throw new IllegalStateException("unable to add more GPU's after choosing one");

	    if (benchmark <= 0)
		throw new IllegalArgumentException("benchmark must not be <= 0");

	    if (selectedGpus.stream().anyMatch(gpu -> gpu.info.id() == gpuId))
		throw new IllegalArgumentException("GPU with id " + gpuId + " was already added");

	    try {
		Gpu gpu = availableGpus.stream().filter(g -> g.info.id() == gpuId).findFirst().get();

		if (benchmark != 0)
		    gpu.benchmark = benchmark;

		if (workgroupSize != 0)
		    gpu.workgroupSize = workgroupSize;

		selectedGpus.add(gpu);

		return this;
	    } catch (NoSuchElementException e) {
		throw new IllegalArgumentException("no GPU found for id " + gpuId);
	    }
	}

	private ArrayList<Gpu> get() {
	    return selectedGpus;
	}
    }

    public static record GpuInfo(long id, String vendor, String name) {
    }

    private class Gpu {
	private GpuInfo info;
	private float benchmark = 1;
	private int workgroupSize = 64;

	// measured kernel duration
	private long duration;
	private int maxNumOfConstellationsPerRun, maxNumOfJklQueensArrays;

	// related opencl objects
	private long platform;
	private long context;
	private long program;
	private long kernel;
	private long xQueue, memQueue;
	private long constellationsMem, jklQueensMem, resMem;

	private Gpu() {
	}

	private float getBenchmark() {
	    return benchmark;
	}

	private void reset() {
	    duration = 0;
	    maxNumOfConstellationsPerRun = 0;
	    context = program = kernel = xQueue = memQueue = constellationsMem = jklQueensMem = resMem = 0;
	}

	private void createOpenClObjects() {
	    try (MemoryStack stack = MemoryStack.stackPush()) {
		IntBuffer errBuf = stack.callocInt(1);

		// create context
		PointerBuffer ctxProps = stack.mallocPointer(3);
		ctxProps.put(CL_CONTEXT_PLATFORM).put(platform).put(NULL).flip();
		long context = clCreateContext(ctxProps, info.id, null, NULL, errBuf);
		checkCLError(errBuf);
		this.context = context;

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
			+ " -D N=" + getN() + " -D WORKGROUP_SIZE=" + workgroupSize + " -Werror";
		int error = clBuildProgram(program, info.id(), options, null, NULL);
		if (error != 0) {
		    String buildLog = getProgramBuildInfoStringASCII(program, info.id(), CL_PROGRAM_BUILD_LOG);
		    String msg = String.format("could not build OpenCL program: %s", buildLog);
		    throw new RuntimeException(msg);
		}
		this.program = program;

		// create kernel
		long kernel;
		if (info.vendor().toLowerCase().contains("intel")) {
		    kernel = clCreateKernel(program, "nqfaf_intel", errBuf);
		} else if (info.vendor().toLowerCase().contains("nvidia")) {
		    kernel = clCreateKernel(program, "nqfaf_nvidia", errBuf);
		} else if (info.vendor().toLowerCase().contains("amd")
			|| info.vendor().toLowerCase().contains("advanced micro devices")) {
		    kernel = clCreateKernel(program, "nqfaf_amd", errBuf);
		} else {
		    kernel = clCreateKernel(program, "nqfaf_nvidia", errBuf);
		}
		checkCLError(errBuf);
		this.kernel = kernel;

		// create command queues
		long xQueue = clCreateCommandQueue(context, info.id(), CL_QUEUE_PROFILING_ENABLE, errBuf);
		checkCLError(errBuf);
		this.xQueue = xQueue;
		long memQueue = clCreateCommandQueue(context, info.id(), 0, errBuf);
		checkCLError(errBuf);
		this.memQueue = memQueue;
	    }
	}

	private void releaseOpenClObjects() {
	    checkCLError(clReleaseCommandQueue(xQueue));
	    checkCLError(clReleaseCommandQueue(memQueue));
	    checkCLError(clReleaseKernel(kernel));
	    checkCLError(clReleaseProgram(program));
	    checkCLError(clReleaseContext(context));
	}

	private void createBuffers(int maxNumOfConstellationsPerRun) {
	    this.maxNumOfConstellationsPerRun = maxNumOfConstellationsPerRun;
	    maxNumOfJklQueensArrays = maxNumOfConstellationsPerRun / workgroupSize; // 1 jkl queens array per workgroup

	    try (MemoryStack stack = MemoryStack.stackPush()) {
		IntBuffer errBuf = stack.callocInt(1);

		if (info.vendor.toLowerCase().contains("nvidia"))
		    constellationsMem = clCreateBufferNV(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    CL_MEM_PINNED_NV, maxNumOfConstellationsPerRun * (4 + 4 + 4 + 4), errBuf);
		else
		    constellationsMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    maxNumOfConstellationsPerRun * (4 + 4 + 4 + 4), errBuf);
		checkCLError(errBuf);

		if (info.vendor.toLowerCase().contains("nvidia"))
		    jklQueensMem = clCreateBufferNV(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    CL_MEM_PINNED_NV, maxNumOfJklQueensArrays * getN() * 4, errBuf);
		else
		    jklQueensMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    maxNumOfJklQueensArrays * getN() * 4, errBuf);
		checkCLError(errBuf);

		if (info.vendor.toLowerCase().contains("nvidia"))
		    resMem = clCreateBufferNV(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, CL_MEM_PINNED_NV,
			    maxNumOfConstellationsPerRun * 8, errBuf);
		else
		    resMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    maxNumOfConstellationsPerRun * 8, errBuf);
		checkCLError(errBuf);

		checkCLError(clFlush(memQueue));
		checkCLError(clFinish(memQueue));

		// set kernel args
		LongBuffer constellationsArg = stack.mallocLong(1);
		constellationsArg.put(0, constellationsMem);
		checkCLError(clSetKernelArg(kernel, 0, constellationsArg));

		LongBuffer jklQueensArg = stack.mallocLong(1);
		jklQueensArg.put(0, jklQueensMem);
		checkCLError(clSetKernelArg(kernel, 1, jklQueensArg));

		LongBuffer resArg = stack.mallocLong(1);
		resArg.put(0, resMem);
		checkCLError(clSetKernelArg(kernel, 2, resArg));
	    }
	}

	private void releaseBuffers() {
	    checkCLError(clReleaseMemObject(constellationsMem));
	    checkCLError(clReleaseMemObject(jklQueensMem));
	    checkCLError(clReleaseMemObject(resMem));
	}

	private void executeWorkload(List<Constellation> constellations) {
	    // TODO
	    // fill jkl queens buffer
	    try (MemoryStack stack = MemoryStack.stackPush()) {
		IntBuffer errBuf = stack.callocInt(1);

		// write data GPU buffers
		ByteBuffer constellationPtr = clEnqueueMapBuffer(memQueue, constellationsMem, true, CL_MAP_WRITE, 0,
			constellations.size() * (4 + 4 + 4 + 4), null, null, errBuf, null);
		checkCLError(errBuf);
		for (int i = 0; i < constellations.size(); i++) {
		    constellationPtr.putInt(i * (4 + 4 + 4 + 4), constellations.get(i).getLd());
		    constellationPtr.putInt(i * (4 + 4 + 4 + 4) + 4, constellations.get(i).getRd());
		    constellationPtr.putInt(i * (4 + 4 + 4 + 4) + 4 + 4, constellations.get(i).getCol());
		    constellationPtr.putInt(i * (4 + 4 + 4 + 4) + 4 + 4 + 4, constellations.get(i).getStartIjkl());
		}
		checkCLError(clEnqueueUnmapMemObject(memQueue, constellationsMem, constellationPtr, null, null));

		int numOfJklQueensArrays = constellations.size() / workgroupSize;
		ByteBuffer jklQueensPtr = clEnqueueMapBuffer(memQueue, jklQueensMem, true, CL_MAP_WRITE, 0,
			numOfJklQueensArrays * getN() * 4, null, null, errBuf, null);
		checkCLError(errBuf);
		for (int wgIdx = 0; wgIdx < numOfJklQueensArrays; wgIdx ++) {
		    var ijkl = constellations.get(wgIdx * workgroupSize).extractIjkl();
		    int j = getj(ijkl);
		    int k = getk(ijkl);
		    int l = getl(ijkl);
		    // the rd from queen j and k with respect to the last row
		    int rdiag = (L >> j) | (L >> (getN() - 1 - k));
		    // the ld from queen j and l with respect to the last row
		    int ldiag = (L >> j) | (L >> l);
		    for (int row = 0; row < getN(); row++) {
			jklQueensPtr
				.putInt(wgIdx * getN() * 4 + ((getN() - 1 - row) * 4), (ldiag >> row) | (rdiag << row) | L | 1);
		    }
		    ldiag = L >> k;
		    rdiag = 1 << l;
		    for(int row = 0; row < getN(); row++){
			int idx = wgIdx * getN() * 4 + (row * 4);
			jklQueensPtr.putInt(idx, jklQueensPtr.getInt(idx) | (ldiag << row) | (rdiag >> row));
		    }
		    jklQueensPtr.putInt(wgIdx * getN() * 4 + (k * 4), ~L);
		    jklQueensPtr.putInt(wgIdx * getN() * 4 + (l * 4), ~1);
		}
		checkCLError(clEnqueueUnmapMemObject(memQueue, jklQueensMem, jklQueensPtr, null, null));

		ByteBuffer resPtr = clEnqueueMapBuffer(memQueue, resMem, true, CL_MAP_WRITE, 0,
			constellations.size() * 8, null, null, errBuf, null);
		checkCLError(errBuf);
		for (int i = 0; i < constellations.size(); i++) {
		    resPtr.putLong(i * 8, constellations.get(i).getSolutions());
		}
		checkCLError(clEnqueueUnmapMemObject(memQueue, resMem, resPtr, null, null));

		checkCLError(clFlush(memQueue));
		checkCLError(clFinish(memQueue));

		// define kernel dimensions
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, constellations.size());
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, workgroupSize);

		// run kernel
		final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1);
		checkCLError(clEnqueueNDRangeKernel(xQueue, kernel, dimensions, null, globalWorkSize, localWorkSize,
			null, xEventBuf));
		checkCLError(clFlush(xQueue));

		// read start and end times using an event
		long xEvent = xEventBuf.get(0);

		// wait for kernel to finish and continuously read results from gpu
		IntBuffer eventStatusBuf = stack.mallocInt(1);
		while (true) {
		    if (getUpdateInterval() > 0)
			readResults(resPtr, constellations);

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
		readResults(resPtr, constellations);

		// read gpu kernel profiled time
		LongBuffer startBuf = BufferUtils.createLongBuffer(1), endBuf = BufferUtils.createLongBuffer(1);
		int err = clGetEventProfilingInfo(xEvent, CL_PROFILING_COMMAND_START, startBuf, null);
		checkCLError(err);
		err = clGetEventProfilingInfo(xEvent, CL_PROFILING_COMMAND_END, endBuf, null);
		checkCLError(err);
		duration += (endBuf.get(0) - startBuf.get(0)) / 1000000; // convert nanoseconds to ms

		// release memory and event
		checkCLError(clReleaseEvent(xEvent));
	    }
	}

	private void readResults(ByteBuffer resPtr, List<Constellation> constellations) {
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
    }
}
