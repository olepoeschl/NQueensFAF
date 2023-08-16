package de.nqueensfaf.impl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLEventCallback;
import org.lwjgl.system.MemoryStack;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static de.nqueensfaf.impl.InfoUtil.*;
import static org.lwjgl.opencl.CL12.*;
import static org.lwjgl.opencl.CL12.clSetEventCallback;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import de.nqueensfaf.Config;
import de.nqueensfaf.Constants;
import de.nqueensfaf.Solver;
import de.nqueensfaf.SolverException;
import de.nqueensfaf.persistence.Constellation;
import de.nqueensfaf.persistence.SolverState;

public class GPUSolver extends Solver {

    private long[] contexts, programs;
    private ArrayList<Device> devices, availableDevices;
    private GPUConstellationsGenerator generator;
    private ArrayList<Constellation> constellations;
    private int workloadSize;
    private int weightSum;
    private long duration, start, end, storedDuration;
    private boolean injected;
    
    private GPUSolverConfig config;
    private SolverUtils utils;

    public GPUSolver() {
	config = new GPUSolverConfig();
	utils = new SolverUtils();
	devices = new ArrayList<Device>();
	availableDevices = new ArrayList<Device>();
	constellations = new ArrayList<Constellation>();
	injected = false;
	try (MemoryStack stack = stackPush()) {
	    fetchAvailableDevices(stack);
	    setDeviceConfigs(config.deviceConfigs);
	} catch(IllegalStateException e) {
	    throw new SolverException("gpu solver is not available", e);
	}
    }
    
    @Override
    protected void run() {
	if (N <= 6) { // if N is very small, use the simple Solver from the parent class
	    start = System.currentTimeMillis();
	    devices.add(new Device(0, 0, "", ""));
	    constellations.add(new Constellation());
	    constellations.get(0).setSolutions(solveSmallBoard());
	    end = System.currentTimeMillis();
	    return;
	}
	
	if(devices.size() == 0)
	    throw new IllegalStateException("no devices selected");

	try (MemoryStack stack = stackPush()) {
	    utils.setN(N);
	    if (!injected)
		genConstellations(); // generate constellations
	    var remainingConstellations = constellations.stream().filter(c -> c.getSolutions() < 0)
		    .collect(Collectors.toList());
	    workloadSize = remainingConstellations.size();

	    IntBuffer errBuf = stack.callocInt(1);

	    createContextsAndPrograms(stack, errBuf);
	    int workloadBeginPtr = 0;
	    for (Device device : devices) {
		// build program
		String options = "-cl-std=CL1.2 -D N=" + N + " -D WORKGROUP_SIZE=" + device.config.workgroupSize + " -Werror";
		int error = clBuildProgram(device.program, device.id, options, null, 0);
		if(error != 0) {
		    String buildLog = getProgramBuildInfoStringASCII(device.program, device.id, CL_PROGRAM_BUILD_LOG);
		    String msg = String.format("OpenCL error [%d]: failed to build program: %s", error, buildLog);
		    throw new RuntimeException(msg);
		}
		// create kernel
		long kernel;
		if (device.vendor.toLowerCase().contains("intel")) {
		    kernel = clCreateKernel(device.program, "nqfaf_intel", errBuf);
		} else if (device.vendor.toLowerCase().contains("nvidia")) {
		    kernel = clCreateKernel(device.program, "nqfaf_nvidia", errBuf);
		} else if (device.vendor.toLowerCase().contains("amd")
			|| device.vendor.toLowerCase().contains("advanced micro devices")) {
		    kernel = clCreateKernel(device.program, "nqfaf_amd", errBuf);
		} else {
		    kernel = clCreateKernel(device.program, "nqfaf_nvidia", errBuf);
		}
		checkCLError(errBuf);
		device.kernel = kernel;
		// create command queues
		long xqueue = clCreateCommandQueue(device.context, device.id, CL_QUEUE_PROFILING_ENABLE, errBuf);
		checkCLError(errBuf);
		device.xqueue = xqueue;
		long memqueue = clCreateCommandQueue(device.context, device.id, CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE,
			errBuf);
		checkCLError(errBuf);
		device.memqueue = memqueue;
		// calculate workload size for device
		int deviceWorkloadSize = (device.config.weight * workloadSize) / weightSum;
		if (devices.indexOf(device) == devices.size() - 1) {
		    deviceWorkloadSize = workloadSize - workloadBeginPtr;
		}
		device.constellations = fillWithTrash(
			remainingConstellations.subList(workloadBeginPtr, workloadBeginPtr + deviceWorkloadSize),
			device.config.workgroupSize);
		workloadBeginPtr += deviceWorkloadSize;
		if (device.constellations.size() == 0) {
		    throw new IllegalArgumentException("weight " + device.config.weight + " is too low");
		}

		new Thread(() -> runDevice(stack, errBuf, device)).start();
	    }

	    // execute device kernels
	    for (Device device : devices)
		while (!device.finished)
		    Thread.sleep(50);

	    // release remaining OpenCL objects
	    for (int i = 0; i < contexts.length; i++) {
		checkCLError(clReleaseProgram(programs[i]));
		checkCLError(clReleaseContext(contexts[i]));
	    }
	} catch (InterruptedException e) {
	    Thread.currentThread().interrupt();
	} catch (IOException e) {
	    throw new SolverException("unexpected error while executing solver", e);
	}
    }

    private void genConstellations() {
	generator = new GPUConstellationsGenerator();
	generator.genConstellations(N, config.presetQueens);

	constellations = generator.getConstellations();
    }

    private ArrayList<Constellation> fillWithTrash(List<Constellation> subConstellations, int workgroupSize) {
	sortConstellations(subConstellations);
	ArrayList<Constellation> newConstellations = new ArrayList<Constellation>();
	int currentJkl = subConstellations.get(0).getStartijkl() & ((1 << 15) - 1);
	for (var c : subConstellations) {
	    // iterate through constellations, add each remaining constellations and fill up
	    // each group of ijkl till its dividable by workgroup-size
	    if (c.getSolutions() >= 0)
		continue;

	    if ((c.getStartijkl() & ((1 << 15) - 1)) != currentJkl) { // check if new ijkl is found
		while (newConstellations.size() % workgroupSize != 0) {
		    addTrashConstellation(newConstellations, currentJkl);
		}
		currentJkl = c.getStartijkl() & ((1 << 15) - 1);
	    }
	    newConstellations.add(c);
	}
	while (newConstellations.size() % workgroupSize != 0) {
	    addTrashConstellation(newConstellations, currentJkl);
	}
	return newConstellations;
    }

    private void addTrashConstellation(ArrayList<Constellation> constellations, int ijkl) {
	constellations.add(new Constellation(-1, (1 << N) - 1, (1 << N) - 1, (1 << N) - 1, (69 << 20) | ijkl, -2));
    }

    void sortConstellations(List<Constellation> constellations) {
	Collections.sort(constellations, new Comparator<Constellation>() {
	    @Override
	    public int compare(Constellation o1, Constellation o2) {
		int o1ijkl = o1.getStartijkl() & ((1 << 20) - 1);
		int o2ijkl = o2.getStartijkl() & ((1 << 20) - 1);
		return Integer.compare(utils.getjkl(o1ijkl), utils.getjkl(o2ijkl));
	    }
	});
    }

    private void createContextsAndPrograms(MemoryStack stack, IntBuffer errBuf) throws IOException {
	// list of platforms to be used
	ArrayList<Long> platforms = new ArrayList<Long>();
	for (Device device : devices) {
	    if (!platforms.contains(device.platform))
		platforms.add(device.platform);
	}
	// create one context for each platform
	contexts = new long[platforms.size()];
	programs = new long[platforms.size()];
	int idx = 0;
	for (long platform : platforms) {
	    List<Device> platformDevices = devices.stream().filter(device -> device.platform == platform)
		    .collect(Collectors.toList());
	    PointerBuffer ctxDevices = stack.mallocPointer(platformDevices.size());
	    for (int i = 0; i < ctxDevices.capacity(); i++) {
		ctxDevices.put(i, platformDevices.get(i).id);
	    }
	    PointerBuffer ctxPlatform = stack.mallocPointer(3);
	    ctxPlatform.put(CL_CONTEXT_PLATFORM).put(platform).put(NULL).flip();

	    long context = clCreateContext(ctxPlatform, ctxDevices, null, NULL, errBuf);
	    checkCLError(errBuf);
	    contexts[idx] = context;
	    long program;
	    try {
		program = clCreateProgramWithSource(context, getKernelSourceAsString("kernels.c"), errBuf);
	    } catch (IOException e) {
		throw new IOException("error while creating program", e);
	    }
	    checkCLError(errBuf);
	    programs[idx] = program;

	    for (Device device : devices) {
		if (device.platform == platform) {
		    device.context = context;
		    device.program = program;
		}
	    }

	    idx++;
	}
    }

    private void runDevice(MemoryStack stack, IntBuffer errBuf, Device device) {
	// workload partitioning
	int ptr = 0;

	// make the max global work size be divisible by the devices workgroup size
	int deviceCurrentWorkloadSize;
	if(device.config.maxGlobalWorkSize == 0) // if no max global work size is specified, don't limit the global work size
	    deviceCurrentWorkloadSize = device.constellations.size();
	else
	    deviceCurrentWorkloadSize = device.config.maxGlobalWorkSize / device.config.workgroupSize
		* device.config.workgroupSize;
	if (device.constellations.size() - deviceCurrentWorkloadSize < 0) // is it the one and only device workload?
	    deviceCurrentWorkloadSize = device.constellations.size() - ptr;

	while (ptr < device.constellations.size()) {
	    if (ptr == 0) { // first workload -> create buffers
		device.workloadConstellations = device.constellations.subList(ptr, ptr + deviceCurrentWorkloadSize);
		ptr += deviceCurrentWorkloadSize;
		device.workloadGlobalWorkSize = device.workloadConstellations.size();
		// create buffers once at the beginning and once at the end
		// because their size the same for all workloads except for the last
		createBuffers(errBuf, device);
	    } else if (device.constellations.size() - deviceCurrentWorkloadSize < ptr) { 
		// last workload -> recreate the buffers
		deviceCurrentWorkloadSize = device.constellations.size() - ptr;
		device.workloadConstellations = device.constellations.subList(ptr, ptr + deviceCurrentWorkloadSize);
		ptr += deviceCurrentWorkloadSize;
		device.workloadGlobalWorkSize = device.workloadConstellations.size();

		releaseWorkloadCLObjects(device); // clean up memory from previous workload
		createBuffers(errBuf, device);
	    } else { // regular workload -> regular iteration, nothing special
		device.workloadConstellations = device.constellations.subList(ptr, ptr + deviceCurrentWorkloadSize);
		ptr += deviceCurrentWorkloadSize;
		device.workloadGlobalWorkSize = device.workloadConstellations.size();
	    }

	    // transfer data to device
	    fillBuffers(errBuf, device);
	    // set kernel args
	    setKernelArgs(stack, device);

	    // start timer when the first device starts computing
	    if (start == 0)
		start = System.currentTimeMillis();

	    // run
	    enqueueKernel(errBuf, device);
	    // start a thread continuously reading device data
	    if(config.updateInterval > 0)
		deviceReaderThread(device).start();

	    // wait for kernel to finish
	    clFinish(device.xqueue);
	    checkCLError(clWaitForEvents(device.xEvent));

	    // stop timer when the last device is finished computing
	    if (ptr >= device.constellations.size()) {
		if (end != devices.size() - 1)
		    end++;
		else {
		    // calculate needed time
		    end = System.currentTimeMillis();
		    duration = end - start + storedDuration;
		}
	    }

	    if (config.updateInterval > 0) {
		device.stopReaderThread = 1; // stop the devices reader thread
		while (device.stopReaderThread != 0) { // wait until the thread has terminated
		    try {
			Thread.sleep(50);
		    } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		    }
		}
	    }

	    // read results
	    readResults(device);
	}
	releaseWorkloadCLObjects(device);
	releaseCLObjects(device);
	device.finished = true;
    }

    private void createBuffers(IntBuffer errBuf, Device device) {
	device.ldMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
		device.workloadGlobalWorkSize * 4, errBuf);
	checkCLError(errBuf);

	device.rdMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
		device.workloadGlobalWorkSize * 4, errBuf);
	checkCLError(errBuf);

	device.colMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
		device.workloadGlobalWorkSize * 4, errBuf);
	checkCLError(errBuf);

	device.startijklMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
		device.workloadGlobalWorkSize * 4, errBuf);
	checkCLError(errBuf);

	device.resMem = clCreateBuffer(device.context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR,
		device.workloadGlobalWorkSize * 8, errBuf);
	checkCLError(errBuf);
    }

    private void fillBuffers(IntBuffer errBuf, Device device) {
	List<Constellation> workloadConstellations = device.workloadConstellations;
	int globalWorkSize = device.workloadGlobalWorkSize;

	// ld
	ByteBuffer ldPtr = clEnqueueMapBuffer(device.memqueue, device.ldMem, true, CL_MAP_WRITE, 0, globalWorkSize * 4,
		null, null, errBuf, null);
	checkCLError(errBuf);
	for (int i = 0; i < globalWorkSize; i++) {
	    ldPtr.putInt(i * 4, workloadConstellations.get(i).getLd());
	}
	checkCLError(clEnqueueUnmapMemObject(device.memqueue, device.ldMem, ldPtr, null, null));

	// rd
	ByteBuffer rdPtr = clEnqueueMapBuffer(device.memqueue, device.rdMem, true, CL_MAP_WRITE, 0, globalWorkSize * 4,
		null, null, errBuf, null);
	checkCLError(errBuf);
	for (int i = 0; i < globalWorkSize; i++) {
	    rdPtr.putInt(i * 4, workloadConstellations.get(i).getRd());
	}
	checkCLError(clEnqueueUnmapMemObject(device.memqueue, device.rdMem, rdPtr, null, null));

	// col
	ByteBuffer colPtr = clEnqueueMapBuffer(device.memqueue, device.colMem, true, CL_MAP_WRITE, 0,
		globalWorkSize * 4, null, null, errBuf, null);
	checkCLError(errBuf);
	for (int i = 0; i < globalWorkSize; i++) {
	    colPtr.putInt(i * 4, workloadConstellations.get(i).getCol());
	}
	checkCLError(clEnqueueUnmapMemObject(device.memqueue, device.colMem, colPtr, null, null));

	// startijkl
	ByteBuffer startijklPtr = clEnqueueMapBuffer(device.memqueue, device.startijklMem, true, CL_MAP_WRITE, 0,
		globalWorkSize * 4, null, null, errBuf, null);
	checkCLError(errBuf);
	for (int i = 0; i < globalWorkSize; i++) {
	    startijklPtr.putInt(i * 4, workloadConstellations.get(i).getStartijkl());
	}
	checkCLError(clEnqueueUnmapMemObject(device.memqueue, device.startijklMem, startijklPtr, null, null));

	// result memory
	device.resPtr = clEnqueueMapBuffer(device.memqueue, device.resMem, true, CL_MAP_WRITE, 0, globalWorkSize * 8,
		null, null, errBuf, null);
	checkCLError(errBuf);
	for (int i = 0; i < globalWorkSize; i++) {
	    device.resPtr.putLong(i * 8, workloadConstellations.get(i).getSolutions());
	}
	checkCLError(clEnqueueUnmapMemObject(device.memqueue, device.resMem, device.resPtr, null, null));

	checkCLError(clFlush(device.memqueue));
	checkCLError(clFinish(device.memqueue));
    }

    private void setKernelArgs(MemoryStack stack, Device device) {
	// ld
	LongBuffer ldArg = stack.mallocLong(1);
	ldArg.put(0, device.ldMem);
	checkCLError(clSetKernelArg(device.kernel, 0, ldArg));
	// rd
	LongBuffer rdArg = stack.mallocLong(1);
	rdArg.put(0, device.rdMem);
	checkCLError(clSetKernelArg(device.kernel, 1, rdArg));
	// col
	LongBuffer colArg = stack.mallocLong(1);
	colArg.put(0, device.colMem);
	checkCLError(clSetKernelArg(device.kernel, 2, colArg));
	// startijkl
	LongBuffer startijklArg = stack.mallocLong(1);
	startijklArg.put(0, device.startijklMem);
	checkCLError(clSetKernelArg(device.kernel, 3, startijklArg));
	// res
	LongBuffer resArg = stack.mallocLong(1);
	resArg.put(0, device.resMem);
	checkCLError(clSetKernelArg(device.kernel, 4, resArg));
    }

    private void enqueueKernel(IntBuffer errBuf, Device device) {
	// create buffer of pointers defining the multi-dimensional size of the number
	// of work units to execute
	final int dimensions = 1;
	PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
	globalWorkSize.put(0, device.workloadGlobalWorkSize);
	PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
	localWorkSize.put(0, device.config.workgroupSize);

	// run kernel
	final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1);
	checkCLError(clEnqueueNDRangeKernel(device.xqueue, device.kernel, dimensions, null, globalWorkSize,
		localWorkSize, null, xEventBuf));

	// workaround for AMD GPUs returning not all results correctly: enqueue a no-operation kernel
	// (without this, sometimes the program finishes with progrss = 0.999...)
	if (device.vendor.toLowerCase().contains("advanced micro devices")
		|| device.vendor.toLowerCase().contains("amd")) {
	    long nullKernel = clCreateKernel(device.program, "null", errBuf);
	    checkCLError(errBuf);
	    PointerBuffer globalWorkSizeNullKernel = BufferUtils.createPointerBuffer(dimensions);
	    globalWorkSizeNullKernel.put(0, device.workloadGlobalWorkSize);
	    PointerBuffer localWorkSizeNullKernel = BufferUtils.createPointerBuffer(dimensions);
	    localWorkSizeNullKernel.put(0, device.config.workgroupSize);
	    checkCLError(clEnqueueNDRangeKernel(device.xqueue, nullKernel, dimensions, null, globalWorkSize,
		    localWorkSize, null, null));
	}

	// get exact time values using CLEvent
	device.xEvent = xEventBuf.get(0);
	checkCLError(clSetEventCallback(device.xEvent, CL_COMPLETE,
		device.profilingCB = CLEventCallback.create((event, event_command_exec_status, user_data) -> {
		    LongBuffer startBuf = BufferUtils.createLongBuffer(1), endBuf = BufferUtils.createLongBuffer(1);
		    int err = clGetEventProfilingInfo(event, CL_PROFILING_COMMAND_START, startBuf, null);
		    checkCLError(err);
		    err = clGetEventProfilingInfo(event, CL_PROFILING_COMMAND_END, endBuf, null);
		    checkCLError(err);
		    device.duration += (endBuf.get(0) - startBuf.get(0)) / 1000000; // convert nanoseconds to milliseconds
		}), NULL));

	// flush command to the device
	checkCLError(clFlush(device.xqueue));
    }

    private void readResults(Device device) {
	// read result and progress memory buffers
	checkCLError(clEnqueueReadBuffer(device.memqueue, device.resMem, true, 0, device.resPtr, null, null));
	for (int i = 0; i < device.workloadGlobalWorkSize; i++) {
	    if (device.workloadConstellations.get(i).getStartijkl() >> 20 == 69) // start=69 is for trash constellations
		continue;
	    long solutionsForConstellation = device.resPtr.getLong(i * 8)
		    * utils.symmetry(device.workloadConstellations.get(i).getStartijkl() & 0b11111111111111111111);
	    if (solutionsForConstellation >= 0)
		// synchronize with the list of constellations on the RAM
		device.workloadConstellations.get(i).setSolutions(solutionsForConstellation);
	}
    }

    private void releaseWorkloadCLObjects(Device device) {
	checkCLError(clReleaseMemObject(device.ldMem));
	checkCLError(clReleaseMemObject(device.rdMem));
	checkCLError(clReleaseMemObject(device.colMem));
	checkCLError(clReleaseMemObject(device.startijklMem));
	checkCLError(clReleaseMemObject(device.resMem));

	device.profilingCB.free();
	checkCLError(clReleaseEvent(device.xEvent));
    }

    private void releaseCLObjects(Device device) {
	checkCLError(clReleaseCommandQueue(device.xqueue));
	checkCLError(clReleaseCommandQueue(device.memqueue));
	checkCLError(clReleaseKernel(device.kernel));
    }

    private Thread deviceReaderThread(Device device) {
	return new Thread(() -> {
	    while (device.stopReaderThread == 0) {
		readResults(device);
		try {
		    Thread.sleep(50);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	    }
	    device.stopReaderThread = 0;
	});
    }

    public GPUSolver config(Consumer<GPUSolverConfig> configConsumer) {
	var tmp = new GPUSolverConfig();
	tmp.from(config);
	
	configConsumer.accept(tmp);
	try {
	    tmp.validate();
	} catch(IllegalArgumentException e) {
	    throw new IllegalArgumentException("invalid GPUSolverConfig", e);
	}
	
	config.from(tmp);
	setDeviceConfigs(config.deviceConfigs);
	return this;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public GPUSolverConfig getConfig() {
	return config;
    }
    
    @Override
    protected void store_(String filepath) throws IOException {
	// if Solver was not even started yet or is already done, throw exception
	if (constellations.size() == 0) {
	    throw new IllegalStateException("nothing to be saved");
	}
	
	Kryo kryo = Constants.kryo;
	try (Output output = new Output(new GZIPOutputStream(new FileOutputStream(filepath)))) {
	    kryo.writeObject(output,
		    new SolverState(N, System.currentTimeMillis() - start + storedDuration, constellations));
	    output.flush();
	}
    }

    @Override
    protected void inject_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
	if (!isIdle()) {
	    throw new IllegalStateException("cannot inject while the solver is running");
	}
	Kryo kryo = Constants.kryo;
	try (Input input = new Input(new GZIPInputStream(new FileInputStream(filepath)))) {
	    SolverState state = kryo.readObject(input, SolverState.class);
	    setN(state.getN());
	    storedDuration = state.getStoredDuration();
	    constellations = state.getConstellations();
	    injected = true;
	}
    }

    @Override
    public long getDuration() {
	if (duration == 0)
	    if (isRunning() && start != 0) {
		return System.currentTimeMillis() - start + storedDuration;
	    }
	return duration;
    }

    @Override
    public float getProgress() {
	int solvedConstellations = 0;
	for (var c : constellations) {
	    if (c.getStartijkl() >> 20 == 69) // start=69 is for trash constellations
		continue;
	    if (c.getSolutions() >= 0) {
		solvedConstellations++;
	    }
	}
	return constellations.size() > 0 ? (float) solvedConstellations / constellations.size() : 0f;
    }

    @Override
    public long getSolutions() {
	long tmpSolutions = 0;
	for (var c : constellations) {
	    if (c.getStartijkl() >> 20 == 69) // start=69 is for trash constellations
		continue;
	    if (c.getSolutions() >= 0) {
		tmpSolutions += c.getSolutions();
	    }
	}
	return tmpSolutions;
    }

    public long getDurationOfDevice(int deviceIndex) {
	if (deviceIndex < 0 || deviceIndex >= availableDevices.size())
	    throw new IllegalArgumentException(
		    "invalid device index: must be a number >=0 and <" + availableDevices.size() + " (number of available devices)");
	return availableDevices.get(deviceIndex).duration;
    }

    private void fetchAvailableDevices(MemoryStack stack) throws IllegalStateException {
	IntBuffer entityCountBuf = stack.mallocInt(1);
	checkCLError(clGetPlatformIDs(null, entityCountBuf));
	if (entityCountBuf.get(0) == 0) {
	    throw new IllegalStateException("no OpenCL platforms found");
	}
	PointerBuffer platforms = stack.mallocPointer(entityCountBuf.get(0));
	checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));

	for (int p = 0; p < platforms.capacity(); p++) {
	    long platform = platforms.get(p);
	    int error = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, entityCountBuf);
	    if(error == CL_DEVICE_NOT_FOUND) { // if no OpenCL GPUs are found for this platform, skip
		continue;
	    }
	    PointerBuffer devicesBuf = stack.mallocPointer(entityCountBuf.get(0));
	    checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devicesBuf, (IntBuffer) null));
	    for (int d = 0; d < devicesBuf.capacity(); d++) {
		long deviceId = devicesBuf.get(d);
		Device device = new Device(deviceId, platform, getDeviceInfoStringUTF8(deviceId, CL_DEVICE_VENDOR),
			getDeviceInfoStringUTF8(deviceId, CL_DEVICE_NAME));
		availableDevices.add(device);
	    }
	}
    }

    public List<DeviceInfo> getAvailableDevices() {
	List<DeviceInfo> deviceInfos = new ArrayList<DeviceInfo>(availableDevices.size());
	for (int i = 0; i < availableDevices.size(); i++) {
	    deviceInfos.add(new DeviceInfo(i, availableDevices.get(i).vendor, availableDevices.get(i).name));
	}
	return deviceInfos;
    }

    private void setDeviceConfigs(DeviceConfig... deviceConfigsInput) {
	devices.clear();
	weightSum = 0;

	if (deviceConfigsInput.equals(DeviceConfig.ALL_DEVICES)) {
	    int index = 0;
	    for (Device device : availableDevices) {
		devices.add(device);
		device.config = new GPUSolverConfig().deviceConfigs[0];
		device.config.index = index++;
		weightSum += device.config.weight;
	    }
	    return;
	}

	ArrayList<DeviceConfig> deviceConfigsTmp = new ArrayList<DeviceConfig>();
	for (DeviceConfig deviceConfig : deviceConfigsInput) {
	    if (deviceConfig.weight == 0)
		continue;
	    if (deviceConfigsTmp.stream().anyMatch(dvcCfg -> deviceConfig.index == dvcCfg.index)) // check for duplicates
		continue;
	    try {
		deviceConfig.validate();
	    } catch(IllegalArgumentException e) {
		throw new IllegalArgumentException("invalid device config", e);
	    }
	    if (deviceConfig.index < availableDevices.size()) {
		deviceConfigsTmp.add(deviceConfig);
		Device device = availableDevices.get(deviceConfig.index);
		device.config = deviceConfig;
		devices.add(device);
		weightSum += deviceConfig.weight;
	    }
	}
    }

    public List<DeviceInfo> getDevices() {
	return devices.stream().map(device -> new DeviceInfo(device.config.index, device.vendor, device.name)).toList();
    }
    
    public List<DeviceInfoWithConfig> getDevicesWithConfig(){
	return devices.stream().map(device -> new DeviceInfoWithConfig(device.config, device.vendor, device.name)).toList();
    }

    private String getKernelSourceAsString(String filepath) throws IOException {
	String resultString = null;
	try (InputStream clSourceFile = GPUSolver.class.getClassLoader().getResourceAsStream(filepath);
		BufferedReader br = new BufferedReader(new InputStreamReader(clSourceFile));) {
	    String line = null;
	    StringBuilder result = new StringBuilder();
	    while ((line = br.readLine()) != null) {
		result.append(line);
		result.append("\n");
	    }
	    resultString = result.toString();
	} catch (IOException e) {
	    throw new IOException("unable to read kernel source file", e);
	}
	return resultString;
    }

    public static class GPUSolverConfig extends Config {
	public DeviceConfig[] deviceConfigs;
	public int presetQueens;
	
	public GPUSolverConfig() {
	    // default values
	    super();
	    deviceConfigs = new DeviceConfig[] {
		    new DeviceConfig()
	    };
	    presetQueens = 6;
	}
	
	@Override
	public void validate() {
	    super.validate();
	    // if device configs are not specified, use default value
	    if(deviceConfigs == null || deviceConfigs.length == 0)
		deviceConfigs = new GPUSolverConfig().deviceConfigs;
	    else {
		for(var dvcCfg : deviceConfigs) {
		    dvcCfg.validate();
		}
	    }
	    if (presetQueens < 4)
		throw new IllegalArgumentException("invalid value for presetQueens: only numbers >=4 are allowed");
	}

	public void from(GPUSolverConfig config) {
	    super.from(config);
	    deviceConfigs = config.deviceConfigs;
	    presetQueens = config.presetQueens;
	}
    }

    public static class DeviceConfig {
	public static final DeviceConfig[] ALL_DEVICES = new DeviceConfig[] {new DeviceConfig(0, 9999, 1, 9999)};
	public int index;
	public int workgroupSize;
	public int weight;
	public int maxGlobalWorkSize;

	public DeviceConfig() {
	    index = 0;
	    workgroupSize = 64;
	    weight = 1;
	    maxGlobalWorkSize = 0;
	}

	@JsonCreator
	public DeviceConfig(@JsonProperty(value = "index", required = true) int index,
		@JsonProperty(value = "workgroupSize") int workgroupSize,
		@JsonProperty(value = "weight", required = true) int weight,
		@JsonProperty(value = "maxGlobalWorkSize") int maxGlobalWorkSize) {
	    this();
	    this.index = index;
	    this.workgroupSize = workgroupSize;
	    this.weight = weight;
	    this.maxGlobalWorkSize = maxGlobalWorkSize;
	}

	public void validate() {
	    if (index < 0)
		throw new IllegalArgumentException("invalid value for index: only numbers >=0 are allowed");
	    if (workgroupSize <= 0)
		throw new IllegalArgumentException("invalid value for workgroup size: only numbers >0 are allowed");
	    if (weight < 0)
		throw new IllegalArgumentException("invalid value for weight: only numbers >0 or 0 (device disabled) are allowed");
	    if (maxGlobalWorkSize != 0 && maxGlobalWorkSize < workgroupSize)
		throw new IllegalArgumentException(
			"invalid value for max global work size: only numbers >=[workgroup size] or 0 (unlimited global work size) are allowed");
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj instanceof DeviceConfig) {
		DeviceConfig dvcCfg = (DeviceConfig) obj;
		return index == dvcCfg.index && workgroupSize == dvcCfg.workgroupSize && weight == dvcCfg.weight
			&& maxGlobalWorkSize == dvcCfg.maxGlobalWorkSize;
	    }
	    return false;
	}
    }

    public record DeviceInfo(int index, String vendor, String name) {}
    public record DeviceInfoWithConfig(DeviceConfig config, String vendor, String name) {}
    
    // a class holding all OpenCL bindings needed for an OpenCL device to operate
    private class Device {
	long id;
	String vendor, name;
	DeviceConfig config;
	// OpenCL
	long platform, context, program, kernel, xqueue, memqueue;
	Long ldMem, rdMem, colMem, startijklMem, resMem;
	ByteBuffer resPtr;
	long xEvent;
	CLEventCallback profilingCB;
	// results
	List<Constellation> constellations, workloadConstellations;
	int workloadGlobalWorkSize;
	long duration = 0;
	// control flow
	int stopReaderThread = 0;
	boolean finished = false;

	public Device(long id, long platform, String vendor, String name) {
	    this.id = id;
	    this.platform = platform;
	    this.vendor = vendor;
	    this.name = name;
	}
    }
}
