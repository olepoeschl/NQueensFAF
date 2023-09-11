package de.nqueensfaf.impl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.lwjgl.cuda.NVRTC.*;
import static org.lwjgl.cuda.CU.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import de.nqueensfaf.Config;
import de.nqueensfaf.Constants;
import de.nqueensfaf.Solver;
import de.nqueensfaf.SolverException;
import de.nqueensfaf.persistence.Constellation;
import de.nqueensfaf.persistence.SolverState;

public class GPUSolverCUDA extends Solver {

    private ArrayList<Device> devices, availableDevices;
    private GPUConstellationsGenerator generator;
    private ArrayList<Constellation> constellations;
    private int workloadSize;
    private int weightSum;
    private long duration, start, end, storedDuration;
    private boolean injected;
    
    private GPUSolverConfig config;
    private SolverUtils utils;

    public GPUSolverCUDA() {
	config = new GPUSolverConfig();
	utils = new SolverUtils();
	devices = new ArrayList<Device>();
	availableDevices = new ArrayList<Device>();
	constellations = new ArrayList<Constellation>();
	injected = false;
	try (MemoryStack stack = stackPush()) {
	    check(cuInit(0));
	    fetchAvailableDevices(stack);
	    setDeviceConfigs(config.deviceConfigs);
	} catch(IllegalStateException e) {
	    throw new SolverException("gpu solver is not available", e);
	} catch(Exception e) {
	    throw new SolverException("gpu solver is not available", e);
	}
    }
    
    @Override
    protected void run() {
	if (N <= 6) { // if N is very small, use the simple Solver from the parent class
	    start = System.currentTimeMillis();
	    devices.add(new Device(0, ""));
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
	    
	    int workloadBeginPtr = 0;
	    for (Device device : devices) {
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

		new Thread(() -> runDevice(stack, device)).start();
	    }

	    // wait for execution of device kernels
	    for (Device device : devices)
		while (!device.finished)
		    Thread.sleep(50);
	    
	} catch (InterruptedException e) {
	    Thread.currentThread().interrupt();
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

    private void runDevice(MemoryStack stack, Device device) {
	PointerBuffer pb = stack.mallocPointer(1);
	
	try {
	    compileProgram(stack, device);
	} catch (IOException e) {
	    throw new SolverException("unexpected error while compiling program", e);
	}
	
	// context
	check(cuCtxCreate(pb, 0, device.id));
        device.ctx = pb.get(0);
	// streams
	check(cuStreamCreate(pb, 0));
        device.xstream = pb.get(0);
	check(cuStreamCreate(pb, 0));
        device.memstream = pb.get(0);
	check(cuStreamCreate(pb, 0));
        device.updatestream = pb.get(0);
        // events
        cuEventCreate(pb, 0);
	device.memevent = pb.get(0);
        cuEventCreate(pb, 0);
	device.startevent = pb.get(0);
        cuEventCreate(pb, 0);
	device.endevent = pb.get(0);
        cuEventCreate(pb, 0);
	device.updateevent = pb.get(0);
	// function
        check(cuModuleLoadData(pb, device.ptx));
        long module = pb.get(0);
        check(cuModuleGetFunction(pb, module, "nqfaf"));
        device.function = pb.get(0);
        
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
		allocBuffers(stack, device);
	    } else if (device.constellations.size() - deviceCurrentWorkloadSize < ptr) { 
		// last workload -> recreate the buffers
		deviceCurrentWorkloadSize = device.constellations.size() - ptr;
		device.workloadConstellations = device.constellations.subList(ptr, ptr + deviceCurrentWorkloadSize);
		ptr += deviceCurrentWorkloadSize;
		device.workloadGlobalWorkSize = device.workloadConstellations.size();

		releaseWorkloadObjects(device); // clean up memory from previous workload
		allocBuffers(stack, device);
	    } else { // regular workload -> regular iteration, nothing special
		device.workloadConstellations = device.constellations.subList(ptr, ptr + deviceCurrentWorkloadSize);
		ptr += deviceCurrentWorkloadSize;
		device.workloadGlobalWorkSize = device.workloadConstellations.size();
	    }

	    // transfer data to device
	    fillBuffers(stack, device);

	    // start timer when the first device starts computing
	    if (start == 0)
		start = System.currentTimeMillis();

	    // run
	    check(cuEventRecord(device.startevent, device.xstream));
	    enqueueKernel(stack, device);
	    // start a thread continuously reading device data
	    if(config.updateInterval > 0)
		deviceReaderThread(device).start();

	    // wait for kernel to finish
	    cuEventRecord(device.endevent, device.xstream);
	    cuEventSynchronize(device.endevent);
	    FloatBuffer fb = stack.mallocFloat(1);
	    check(cuEventElapsedTime(fb, device.startevent, device.endevent));
	    device.duration += (int) fb.get(0);

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
	releaseWorkloadObjects(device);
	releaseObjects(device);
	device.finished = true;
    }

    private void compileProgram(MemoryStack stack, Device device) throws IOException {
	PointerBuffer pb = stack.mallocPointer(1);
	checkNVRTC(nvrtcCreateProgram(pb, getKernelSourceAsString("kernel.cu"), "nqueensfaf.cu", null, null));
	long program = pb.get(0);
	
	String[] options = new String[]{"-D N=" + N + "\0", "-D BLOCK_SIZE=" + device.config.workgroupSize + "\0"};
	PointerBuffer optionsPb = stack.mallocPointer(options.length);
	optionsPb.rewind();
	for(String s : options) {
	    ByteBuffer buf = stack.malloc(s.length());
	    buf.rewind();
	    buf.put(s.getBytes());
	    buf.flip();
	    optionsPb.put(MemoryUtil.memAddress(buf));
	}
	optionsPb.flip();
	checkNVRTC(nvrtcCompileProgram(program, optionsPb));
	{
	    checkNVRTC(nvrtcGetProgramLogSize(program, pb));
	    if (1L < pb.get(0)) {
		ByteBuffer log = stack.malloc((int) pb.get(0) - 1);

		checkNVRTC(nvrtcGetProgramLog(program, log));
		System.err.println("Compilation log:");
		System.err.println("----------------");
		System.err.println(memASCII(log));
	    }
	}
	checkNVRTC(nvrtcGetPTXSize(program, pb));
	device.ptx = memAlloc((int)pb.get(0));
	checkNVRTC(nvrtcGetPTX(program, device.ptx));
    }

    private void allocBuffers(MemoryStack stack, Device device) {
	PointerBuffer pb = stack.mallocPointer(1);

	device.hostLdMem = memAllocInt(device.workloadGlobalWorkSize);
	check(cuMemAllocAsync(pb, Integer.BYTES * device.workloadGlobalWorkSize, device.memstream));
        device.deviceLdMem = pb.get(0);

	device.hostRdMem = memAllocInt(device.workloadGlobalWorkSize);
	check(cuMemAllocAsync(pb, Integer.BYTES * device.workloadGlobalWorkSize, device.memstream));
        device.deviceRdMem = pb.get(0);

	device.hostColMem = memAllocInt(device.workloadGlobalWorkSize);
	check(cuMemAllocAsync(pb, Integer.BYTES * device.workloadGlobalWorkSize, device.memstream));
        device.deviceColMem = pb.get(0);

	device.hostStartijklMem = memAllocInt(device.workloadGlobalWorkSize);
	check(cuMemAllocAsync(pb, Integer.BYTES * device.workloadGlobalWorkSize, device.memstream));
        device.deviceStartijklMem = pb.get(0);

	device.hostResMem = memAllocLong(device.workloadGlobalWorkSize);
	check(cuMemAllocAsync(pb, Long.BYTES * device.workloadGlobalWorkSize, device.memstream));
        device.deviceResMem = pb.get(0);
        
        check(cuEventRecord(device.memevent, device.memstream));
        check(cuEventSynchronize(device.memevent));
    }

    private void fillBuffers(MemoryStack stack, Device device) {
	List<Constellation> workloadConstellations = device.workloadConstellations;
	int globalWorkSize = device.workloadGlobalWorkSize;

	// fill buffers
	for(int i = 0; i < globalWorkSize; i++) {
	    device.hostLdMem.put(i, workloadConstellations.get(i).getLd());
	    device.hostRdMem.put(i, workloadConstellations.get(i).getRd());
	    device.hostColMem.put(i, workloadConstellations.get(i).getCol());
	    device.hostStartijklMem.put(i, workloadConstellations.get(i).getStartijkl());
	    device.hostResMem.put(i, -1); // mark each constellation as unsolved
	}
	
	// copy buffers to device
        check(cuMemcpyHtoDAsync(device.deviceLdMem, device.hostLdMem, device.memstream));
        check(cuMemcpyHtoDAsync(device.deviceRdMem, device.hostRdMem, device.memstream));
        check(cuMemcpyHtoDAsync(device.deviceColMem, device.hostColMem, device.memstream));
        check(cuMemcpyHtoDAsync(device.deviceStartijklMem, device.hostStartijklMem, device.memstream));
        check(cuMemcpyHtoDAsync(device.deviceResMem, device.hostResMem, device.memstream));
	check(cuEventRecord(device.memevent, device.memstream));
	check(cuEventSynchronize(device.memevent));
    }

    private void enqueueKernel(MemoryStack stack, Device device) {
	check(cuLaunchKernel(
		device.function, device.workloadGlobalWorkSize/device.config.workgroupSize, 1, 1,
		device.config.workgroupSize, 1, 1,
		0,
		device.xstream,
		stack.pointers(
			memAddress(stack.longs(device.deviceLdMem)),
			memAddress(stack.longs(device.deviceRdMem)),
			memAddress(stack.longs(device.deviceColMem)),
			memAddress(stack.longs(device.deviceStartijklMem)),
			memAddress(stack.longs(device.deviceResMem))
			),
		null
		));
    }

    private void readResults(Device device) {
	check(cuMemcpyDtoHAsync(device.hostResMem, device.deviceResMem, device.updatestream));
	check(cuEventRecord(device.updateevent, device.updatestream));
	check(cuEventSynchronize(device.updateevent));
	for (int i = 0; i < device.workloadGlobalWorkSize; i++) {
	    if (device.workloadConstellations.get(i).getStartijkl() >> 20 == 69) // start=69 is for trash constellations
		continue;
	    long solutionsForConstellation = device.hostResMem.get(i)
		    * utils.symmetry(device.workloadConstellations.get(i).getStartijkl() & 0b11111111111111111111);
	    if (solutionsForConstellation >= 0) {
		// synchronize with the list of constellations on the RAM
		device.workloadConstellations.get(i).setSolutions(solutionsForConstellation);
	    }
	}
    }

    private void releaseWorkloadObjects(Device device) {
	check(cuMemFreeAsync(device.deviceLdMem, device.memstream));
	check(cuMemFreeAsync(device.deviceRdMem, device.memstream));
	check(cuMemFreeAsync(device.deviceColMem, device.memstream));
	check(cuMemFreeAsync(device.deviceStartijklMem, device.memstream));
	check(cuMemFreeAsync(device.deviceResMem, device.memstream));
	check(cuEventRecord(device.memevent, device.memstream));
	check(cuEventSynchronize(device.memevent));
    }

    private void releaseObjects(Device device) {
	check(cuEventDestroy(device.memevent));
	check(cuEventDestroy(device.startevent));
	check(cuEventDestroy(device.endevent));
	check(cuStreamDestroy(device.memstream));
	check(cuStreamDestroy(device.xstream));
	check(cuCtxDetach(device.ctx));
    }

    private Thread deviceReaderThread(Device device) {
	return new Thread(() -> {
	    check(cuCtxSetCurrent(device.ctx));
	    while (device.stopReaderThread == 0) {
		readResults(device);
		try {
		    Thread.sleep(config.updateInterval);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	    }
	    device.stopReaderThread = 0;
	});
    }

    public GPUSolverCUDA config(Consumer<GPUSolverConfig> configConsumer) {
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
        IntBuffer pi = stack.mallocInt(1);
        ByteBuffer nameBuf = stack.malloc(100);
	check(cuDeviceGetCount(pi));
        if (pi.get(0) == 0) {
            throw new IllegalStateException("no devices supporting CUDA");
        }
        for(int i = 0; i < pi.get(0); i++) {
            check(cuDeviceGet(pi, i));
            check(cuDeviceGetName(nameBuf, pi.get(0)));
            Device device = new Device(pi.get(0), memASCII(memAddress(nameBuf)));
            availableDevices.add(device);
        }
    }

    public List<DeviceInfo> getAvailableDevices() {
	List<DeviceInfo> deviceInfos = new ArrayList<DeviceInfo>(availableDevices.size());
	for (int i = 0; i < availableDevices.size(); i++) {
	    deviceInfos.add(new DeviceInfo(i, availableDevices.get(i).name));
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
	return devices.stream().map(device -> new DeviceInfo(device.config.index, device.name)).toList();
    }
    
    public List<DeviceInfoWithConfig> getDevicesWithConfig(){
	return devices.stream().map(device -> new DeviceInfoWithConfig(device.config, device.name)).toList();
    }

    private String getKernelSourceAsString(String filepath) throws IOException {
	String resultString = null;
	try (InputStream clSourceFile = GPUSolverCUDA.class.getClassLoader().getResourceAsStream(filepath);
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
    
    private void checkNVRTC(int err) {
        if (err != NVRTC_SUCCESS) {
            throw new IllegalStateException(nvrtcGetErrorString(err));
        }
    }
    
    private void check(int err) {
        if (err != CUDA_SUCCESS) {
            throw new IllegalStateException(Integer.toString(err));
        }
    }
    
    private void check(int err, long ctx) {
        if (err != CUDA_SUCCESS) {
            if (ctx != NULL) {
                cuCtxDetach(ctx);
                ctx = NULL;
            }
            throw new IllegalStateException(Integer.toString(err));
        }
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

    public record DeviceInfo(int index, String name) {}
    public record DeviceInfoWithConfig(DeviceConfig config, String name) {}
    
    // a class holding all OpenCL bindings needed for an OpenCL device to operate
    private class Device {
	int id;
	String name;
	DeviceConfig config;
	
	// cuda
	ByteBuffer ptx;
	long function, ctx, xstream, memstream, updatestream, memevent, startevent, endevent, updateevent;
	IntBuffer hostLdMem, hostRdMem, hostColMem, hostStartijklMem;
	LongBuffer hostResMem;
	long deviceLdMem, deviceRdMem, deviceColMem, deviceStartijklMem, deviceResMem;
	
	// results
	List<Constellation> constellations, workloadConstellations;
	int workloadGlobalWorkSize;
	long duration = 0;
	
	// control flow
	int stopReaderThread = 0;
	boolean finished = false;

	public Device(int id, String name) {
	    this.id = id;
	    this.name = name;
	}
    }
}
