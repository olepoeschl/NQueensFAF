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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import de.nqueensfaf.AbstractSolver;
import de.nqueensfaf.SolverStatus;

public class GpuSolver extends AbstractSolver implements Stateful {

    private List<Gpu> availableGpus;
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

    public void setPresetQueens(int presetQueens) {
	this.presetQueens = presetQueens;
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
	if (getStatus().isBefore(SolverStatus.FINISHED) && start != 0) {
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
	var gpuListTemp = new ArrayList<Gpu>();
	
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
		    long id = gpusBuf.get(g);
		    GpuInfo info = new GpuInfo(getDeviceInfoStringUTF8(id, CL_DEVICE_VENDOR),
			    getDeviceInfoStringUTF8(id, CL_DEVICE_NAME));
		    
		    Gpu gpu = new Gpu(id, platform, info);
		    
		    gpuListTemp.add(gpu);
		}
	    }
	}
	
	availableGpus = List.copyOf(gpuListTemp);
    }

    public List<Gpu> getAvailableGpus() {
	return availableGpus;
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

	start = System.currentTimeMillis();

	if (!stateLoaded)
	    constellations = new ConstellationsGenerator(getN()).generate(presetQueens);

	var remainingConstellations = constellations.stream().filter(c -> c.getSolutions() < 0)
		.collect(Collectors.toList());
	if (remainingConstellations.size() == 0)
	    return; // nothing to do

	for (var gpu : gpuSelection.get()) {
	    gpu.setN(getN());
	    gpu.createOpenClObjects();
	}
	
	L = 1 << (getN() - 1);

	if (gpuSelection.get().size() == 1) {
	    singleGpu(gpuSelection.get().get(0), remainingConstellations);
	} else {
	    multiGpu(remainingConstellations);
	}
	
	duration = System.currentTimeMillis() - start + storedDuration;

	for (var gpu : gpuSelection.get())
	    gpu.releaseOpenClObjects();
    }

    private void singleGpu(Gpu gpu, List<Constellation> constellations) {
	constellations = new ArrayList<>(
		fillWithPseudoConstellations(constellations, gpuSelection.get().get(0).getConfig().getWorkgroupSize()));

	gpu.createBuffers(constellations.size());
	gpu.executeWorkload(constellations);
	gpu.releaseBuffers();
    }

    private void multiGpu(List<Constellation> constellations) {
	sortConstellationsByJkl(constellations);
	var selectedGpus = gpuSelection.get();

	// calculate workload percentage for each gpu. the lower the benchmark, the bigger the workload
	float benchmarkSum = selectedGpus.stream().map(gpu -> gpu.getConfig().getBenchmark()).reduce(0f, Float::sum);
	float[] gpuPortions = new float[selectedGpus.size()];
	float gpuPortionSum = 0f;
	for (int i = 0; i < selectedGpus.size(); i++) {
	    gpuPortions[i] = (benchmarkSum / selectedGpus.get(i).getConfig().getBenchmark());
	    gpuPortionSum += gpuPortions[i];
	}
	for (int i = 0; i < selectedGpus.size(); i++) {
	    gpuPortions[i] /= gpuPortionSum;
	}

	// if very few constellations, enqueue all at once
	final float firstPortion = (constellations.size() < 10_000 * selectedGpus.size()) ? 1f : 0.3f;

	// first workload is the biggest one, then they shrink with each iteration
	var firstWorkloads = new ArrayList<List<Constellation>>(selectedGpus.size());

	int firstWorkloadSize = (int) (constellations.size() * firstPortion);
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

	    var gpuFirstWorkload = fillWithPseudoConstellations(gpuFirstWork, selectedGpus.get(gpuIdx).getConfig().getWorkgroupSize());
	    firstWorkloads.add(gpuFirstWorkload);

	    var gpu = selectedGpus.get(gpuIdx);
	    gpu.createBuffers((int) (constellations.size() * 0.7)); // max size of workload, because 30% are already executed in the first iteration

	    fromIndex = toIndex;
	}

	var queue = new ArrayDeque<>(constellations.subList(fromIndex, constellations.size()));
	var executor = Executors.newFixedThreadPool(selectedGpus.size());
	final var iterationSum = new AtomicInteger(0);
	final int minGpuWorkloadSize = 1024;

	for (int idx = 0; idx < selectedGpus.size(); idx++) {
	    final int gpuIdx = idx;
	    final var gpu = selectedGpus.get(idx);
	    final var gpuFirstWorkloadSize = firstWorkloads.get(idx).size();

	    if (firstWorkloads.get(gpuIdx).size() == 0)
		continue;

	    executor.execute(() -> {
		var workload = firstWorkloads.get(gpuIdx);
		int iteration = 0;
		float adaptive = 1;

		do {
		    gpu.executeWorkload(workload);

		    if (queue.isEmpty())
			break;
		    
		    workload = new ArrayList<Constellation>(workload.size());

		    iteration++;
		    
		    // if the other gpus in average completed already more workloads, make the own workload size a bit smaller to catch up and vice versa
		    float cumulatedIterationProgressAvg = iterationSum.incrementAndGet() - iteration;
		    for(var otherGpu : selectedGpus) {
			if(otherGpu.getId() == gpu.getId())
			    continue;
			cumulatedIterationProgressAvg += otherGpu.getProgress();
		    }
		    cumulatedIterationProgressAvg /= (selectedGpus.size() - 1);
		    if(cumulatedIterationProgressAvg == 0f) // prevent division by 0
			cumulatedIterationProgressAvg = 0.00001f;
		    adaptive *= iteration / cumulatedIterationProgressAvg;
		    
		    int workloadSize = (int) (gpuFirstWorkloadSize * adaptive);
		    if(getProgress() > 0.8)
			workloadSize *= 0.3;
		    else if(getProgress() > 0.5)
			workloadSize *= 0.5;
		    if (workloadSize < minGpuWorkloadSize)
			workloadSize = minGpuWorkloadSize;
		    
		    while (workload.size() < workloadSize && !queue.isEmpty()) {
			synchronized(queue) {
			    for (int i = 0; i < gpu.getConfig().getWorkgroupSize() && workload.size() < workloadSize
				    && !queue.isEmpty(); i++) {
				workload.add(queue.remove());
			    }
			}
		    }

		    if (workload.isEmpty())
			break;

		    workload = fillWithPseudoConstellations(workload, gpu.getConfig().getWorkgroupSize());
		    
		    // if workload too big, give some work back to the queue
		    while (workload.size() > gpu.maxNumOfConstellationsPerRun) {
			synchronized(queue) {
			    for (int i = 0; i < gpu.getConfig().getWorkgroupSize(); i++) {
				var c = workload.remove(workload.size() - 1);
				if (c.extractStart() != 69)
				    queue.add(c);
			    }
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

	for (int gpuIdx = 0; gpuIdx < selectedGpus.size(); gpuIdx++) {
	    if (firstWorkloads.get(gpuIdx).size() == 0)
		continue;
	    selectedGpus.get(gpuIdx).releaseBuffers();
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
	
	private List<Gpu> selectedGpus = new ArrayList<Gpu>();
	private boolean chosen = false;

	private GpuSelection() {
	}

	public void choose(Gpu gpu) {
	    add(gpu);
	    chosen = true;
	}

	public void add(Gpu gpu) {
	    if (chosen)
		throw new IllegalStateException("unable to add more GPU's after choosing one");

	    if(!availableGpus.contains(gpu))
		throw new IllegalArgumentException("no GPU found for id " + gpu.getId() + " ('" + gpu.getInfo().name() + "')");
	    
	    if (selectedGpus.contains(gpu))
		throw new IllegalArgumentException("GPU with id " + gpu.getId() + " was already added");

	    selectedGpus.add(gpu);
	}

	public List<Gpu> get() {
	    return List.copyOf(selectedGpus);
	}
	
	public void reset() {
	    selectedGpus.clear();
	}
    }

    public static record GpuInfo(String vendor, String name) {
	@Override
	public String toString() {
	    return name;
	}
    }

    public static class GpuConfig {
	
	private float benchmark;
	private int workgroupSize;
	
	public GpuConfig() {
	    this(1, 64);
	}
	
	public GpuConfig(float benchmark, int workgroupSize) {
	    this.benchmark = benchmark;
	    this.workgroupSize = workgroupSize;
	}
	
	public GpuConfig(GpuConfig config) {
	    benchmark = config.getBenchmark();
	    workgroupSize = config.getWorkgroupSize();
	}

	public float getBenchmark() {
	    return benchmark;
	}

	public void setBenchmark(float benchmark) {
	    if(benchmark <= 0)
		throw new IllegalStateException("benchmark was " + benchmark + " but expected >0");
	    this.benchmark = benchmark;
	}

	public int getWorkgroupSize() {
	    return workgroupSize;
	}

	public void setWorkgroupSize(int workgroupSize) {
	    if(workgroupSize <= 0)
		throw new IllegalStateException("workgroup size was " + workgroupSize + " but expected >0");
	    this.workgroupSize = workgroupSize;
	}
    }
    
    public class Gpu {
	
	private final long id; // OpenCL device id
	private final long platform;
	private GpuInfo info;
	private GpuConfig config = new GpuConfig();

	// for creating the buffers with sufficient size to be reused in all workloads (for multi gpu)
	private int maxNumOfConstellationsPerRun, maxNumOfJklQueensArrays;

	// related opencl objects
	private long context;
	private long program;
	private long kernel;
	private long xQueue, memQueue;
	private long constellationsMem, jklQueensMem, resMem;
	
	// other variables
	private int n;
	private float progress;
	
	private Gpu(long id, long platform, GpuInfo info) {
	    this.id = id;
	    this.platform = platform;
	    this.info = info;
	}
	
	public long getId() {
	    return id;
	}

	public GpuInfo getInfo() {
	    return info;
	}
	
	public GpuConfig getConfig() {
	    return config;
	}
	
	public void setConfig(GpuConfig config) {
	    this.config = config;
	}
	
	@Override
	public String toString() {
	    return info.name();
	}

	private void reset() {
	    duration = 0;
	    maxNumOfConstellationsPerRun = 0;
	    context = program = kernel = xQueue = memQueue = constellationsMem = jklQueensMem = resMem = 0;
	    progress = 0;
	}

	private void setN(int n) {
	    this.n = n; 
	}
	
	private void createOpenClObjects() {
	    try (MemoryStack stack = MemoryStack.stackPush()) {
		IntBuffer errBuf = stack.callocInt(1);

		// create context
		PointerBuffer ctxProps = stack.mallocPointer(3);
		ctxProps.put(CL_CONTEXT_PLATFORM).put(platform).put(NULL).flip();
		long context = clCreateContext(ctxProps, id, null, NULL, errBuf);
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
			+ " -D N=" + n + " -D WORKGROUP_SIZE=" + config.getWorkgroupSize() + " -Werror";
		int error = clBuildProgram(program, id, options, null, NULL);
		if (error != 0) {
		    String buildLog = getProgramBuildInfoStringASCII(program, id, CL_PROGRAM_BUILD_LOG);
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
		long xQueue = clCreateCommandQueue(context, id, CL_QUEUE_PROFILING_ENABLE, errBuf);
		checkCLError(errBuf);
		this.xQueue = xQueue;
		long memQueue = clCreateCommandQueue(context, id, 0, errBuf);
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
	    maxNumOfJklQueensArrays = maxNumOfConstellationsPerRun / config.getWorkgroupSize(); // 1 jkl queens array per workgroup

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
		    jklQueensMem = clCreateBufferNV(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    CL_MEM_PINNED_NV, maxNumOfJklQueensArrays * n * 4, errBuf);
		else
		    jklQueensMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
			    maxNumOfJklQueensArrays * n * 4, errBuf);
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

		int numOfJklQueensArrays = constellations.size() / config.getWorkgroupSize();
		ByteBuffer jklQueensPtr = clEnqueueMapBuffer(memQueue, jklQueensMem, true, CL_MAP_WRITE, 0,
			numOfJklQueensArrays * n * 4, null, null, errBuf, null);
		checkCLError(errBuf);
		for (int wgIdx = 0; wgIdx < numOfJklQueensArrays; wgIdx ++) {
		    var ijkl = constellations.get(wgIdx * config.getWorkgroupSize()).extractIjkl();
		    int j = getj(ijkl);
		    int k = getk(ijkl);
		    int l = getl(ijkl);
		    // the rd from queen j and k with respect to the last row
		    int rdiag = (L >> j) | (L >> (n - 1 - k));
		    // the ld from queen j and l with respect to the last row
		    int ldiag = (L >> j) | (L >> l);
		    for (int row = 0; row < n; row++) {
			jklQueensPtr
				.putInt(wgIdx * n * 4 + ((n - 1 - row) * 4), (ldiag >> row) | (rdiag << row) | L | 1);
		    }
		    ldiag = L >> k;
		    rdiag = 1 << l;
		    for(int row = 0; row < n; row++){
			int idx = wgIdx * n * 4 + (row * 4);
			jklQueensPtr.putInt(idx, jklQueensPtr.getInt(idx) | (ldiag << row) | (rdiag >> row));
		    }
		    jklQueensPtr.putInt(wgIdx * n * 4 + (k * 4), ~L);
		    jklQueensPtr.putInt(wgIdx * n * 4 + (l * 4), ~1);
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
		localWorkSize.put(0, config.getWorkgroupSize());

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

		// release memory and event
		checkCLError(clReleaseEvent(xEvent));
	    }
	}

	private void readResults(ByteBuffer resPtr, List<Constellation> constellations) {
	    int solvedConstellations = 0;
	    
	    // read result and progress memory buffers
	    checkCLError(clEnqueueReadBuffer(memQueue, resMem, true, 0, resPtr, null, null));
	    for (int i = 0; i < constellations.size(); i++) {
		if (constellations.get(i).extractStart() == 69) // start=69 is for trash constellations
		    continue;
		long solutionsForConstellation = resPtr.getLong(i * 8)
			* symmetry(n, constellations.get(i).extractIjkl());
		if (solutionsForConstellation >= 0) {
		    // synchronize with the list of constellations on the RAM
		    constellations.get(i).setSolutions(solutionsForConstellation);
		    
		    solvedConstellations++;
		}
	    }
	    
	    progress = (float) solvedConstellations / constellations.size();
	}

	private float getProgress() {
	    return progress;
	}
    }
}
