package de.nqueensfaf.compute;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.stream.Collectors;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLEventCallback;
import org.lwjgl.system.MemoryStack;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import static org.lwjgl.opencl.CL12.*;
import static org.lwjgl.opencl.CL12.clSetEventCallback;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import static de.nqueensfaf.compute.InfoUtil.*;

import de.nqueensfaf.config.Config;
import de.nqueensfaf.config.DeviceConfig;
import de.nqueensfaf.data.Constellation;
import de.nqueensfaf.data.SolverState;

public class GPUSolver extends Solver {

	// OpenCL stuff
	private ArrayList<Device> devices, availableDevices;
	private long[] contexts, programs;

	// calculation related stuff
	private GPUConstellationsGenerator generator;
	private ArrayList<Constellation> constellations;
	private int workloadSize;
	
	// config stuff
	private int presetQueens;
	private int weightSum;

	// user interface
	private long duration, start, end, storedDuration;

	// control flow
	private boolean injected = false;

	protected GPUSolver() {
		devices = new ArrayList<Device>();
		availableDevices = new ArrayList<Device>();
		constellations = new ArrayList<Constellation>();
		try (MemoryStack stack = stackPush()) {
			fetchAvailableDevices(stack);
		}
	}

	// --------------------------------------------------------
	// ----------------- Solver main method -----------------
	// --------------------------------------------------------

	@Override
	protected void run() {
		if (start != 0) {
			throw new IllegalStateException(
					"You first have to call reset() when calling solve() multiple times on the same object");
		}
		if (N <= 6) { // if N is very small, use the simple Solver from the parent class
			// prepare simulating progress = 100
			start = System.currentTimeMillis();
			devices.add(new Device(0, 0, "", ""));
			constellations.add(new Constellation());
			constellations.get(0).setSolutions(solveSmallBoard());
			end = System.currentTimeMillis();
			return;
		}

		try (MemoryStack stack = stackPush()) {
			// if only 1 device is used, set presetQueens to the value specified in the
			// devices config
			if (devices.size() == 1)
				presetQueens = devices.get(0).config.getPresetQueens();
			
			if (!injected)
				genConstellations(); // generate constellations
			var remainingConstellations = constellations.stream().filter(c -> c.getSolutions() < 0).collect(Collectors.toList());
			workloadSize = remainingConstellations.size();

			IntBuffer errBuf = stack.callocInt(1);

			// prepare OpenCL stuff, generate workload and transfer to device
			createContextsAndPrograms(stack, errBuf);
			int workloadBeginPtr = 0;
			for (Device device : devices) {
				// build program
				String options = "-D N=" + N + " -D WORKGROUP_SIZE=" + device.config.getWorkgroupSize();
				int error = clBuildProgram(device.program, device.id, options, null, 0);
				checkCLError(error);
				// create kernel
				long kernel;
				if (device.vendor.toLowerCase().contains("intel")) {
					kernel = clCreateKernel(device.program, "nqfaf_intel", errBuf);
				} else if (device.vendor.toLowerCase().contains("nvidia")) {
					kernel = clCreateKernel(device.program, "nqfaf_nvidia", errBuf);
				} else if (device.vendor.toLowerCase().contains("amd") || device.vendor.toLowerCase().contains("advanced micro devices")) {
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
				// create buffers and fill with trash constellations
				int deviceWorkloadSize = (device.config.getWeight() * workloadSize) / weightSum;
				if (devices.indexOf(device) == devices.size() - 1) {
					deviceWorkloadSize = workloadSize - workloadBeginPtr;
				}
				device.constellations = fillWithTrash(
						remainingConstellations.subList(workloadBeginPtr, workloadBeginPtr + deviceWorkloadSize),
						device.config.getWorkgroupSize());
				workloadBeginPtr += deviceWorkloadSize;
				if (device.constellations.size() == 0) {
					throw new IllegalArgumentException("Weight " + device.config.getWeight() + " is too low");
				}

				new Thread(() -> runDevice(stack, errBuf, device)).start();
			}
			

			// execute device kernels
			for(Device device : devices)
				while(!device.finished)
					Thread.sleep(50);
			
			// release remaining OpenCL objects
			for (int i = 0; i < contexts.length; i++) {
				checkCLError(clReleaseProgram(programs[i]));
				checkCLError(clReleaseContext(contexts[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// --------------------------------------------------------
	// --------------- prepare total workload ---------------
	// --------------------------------------------------------

	private void genConstellations() {
		generator = new GPUConstellationsGenerator();
		generator.genConstellations(N, presetQueens);

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

	// sort constellations so that as many workgroups as possible have solutions
	// with less divergent branches
	// this can also be done by directly generating the constellations in a
	// different order
	void sortConstellations(List<Constellation> constellations) {
		Collections.sort(constellations, new Comparator<Constellation>() {
			@Override
			public int compare(Constellation o1, Constellation o2) {
				int o1ijkl = o1.getStartijkl() & ((1 << 20) - 1);
				int o2ijkl = o2.getStartijkl() & ((1 << 20) - 1);
				return Integer.compare(getjkl(o1ijkl), getjkl(o2ijkl));
			}
		});
	}

	// --------------------------------------------------------
	// -------------------- OpenCL stuff --------------------
	// --------------------------------------------------------

	private void createContextsAndPrograms(MemoryStack stack, IntBuffer errBuf) {
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
			long program = clCreateProgramWithSource(context,
					getKernelSourceAsString("kernels.c"), errBuf);
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
		int deviceCurrentWorkloadSize = device.config.getMaxGlobalWorkSize() / device.config.getWorkgroupSize() * device.config.getWorkgroupSize();
		if(device.constellations.size() - deviceCurrentWorkloadSize < ptr) // is it the one and only device workload?
			deviceCurrentWorkloadSize = device.constellations.size() - ptr;
		
		while(ptr < device.constellations.size()) {
			if(ptr == 0) {	// first workload -> create buffers
				device.workloadConstellations = device.constellations.subList(ptr, ptr + deviceCurrentWorkloadSize);
				ptr += deviceCurrentWorkloadSize;
				device.workloadGlobalWorkSize = device.workloadConstellations.size();
				// create buffers once at the beginning and once at the end
				// because their size the same for all workloads except for the last
				createBuffers(errBuf, device);
			} else if(device.constellations.size() - deviceCurrentWorkloadSize < ptr) {	// last workload -> create the buffers new
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
			if(start == 0)
				start = System.currentTimeMillis();
			
			// run
			explosionBoost9000(errBuf, device);
			// start a thread continuously reading device data
			deviceReaderThread(device).start();
			
			// wait for kernel to finish
			clFinish(device.xqueue);
			
			// stop timer when the last device is finished computing
			if(ptr >= device.constellations.size()) {
				if(end != devices.size() - 1)
					end++;
				else {
					// calculate needed time
					if(devices.size() > 1) {
						end = System.currentTimeMillis();
						duration = end - start + storedDuration;
					} else {
						while(device.duration == 0)
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								// ignore
							}
						duration = device.duration;
					}
				}
			}
			
			device.stopReaderThread = 1; // stop the devices reader thread
			while(device.stopReaderThread != 0) {	// wait until the thread has terminated
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// ignore
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
		device.ldMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, device.workloadGlobalWorkSize * 4,
				errBuf);
		checkCLError(errBuf);
		
		device.rdMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, device.workloadGlobalWorkSize * 4,
				errBuf);
		checkCLError(errBuf);
		
		device.colMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, device.workloadGlobalWorkSize * 4,
				errBuf);
		checkCLError(errBuf);
		
		device.startijklMem = clCreateBuffer(device.context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR,
				device.workloadGlobalWorkSize * 4, errBuf);
		checkCLError(errBuf);
		
		device.resMem = clCreateBuffer(device.context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, device.workloadGlobalWorkSize * 8,
				errBuf);
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

	private void explosionBoost9000(IntBuffer errBuf, Device device) {
		// create buffer of pointers defining the multi-dimensional size of the number
		// of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, device.workloadGlobalWorkSize);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, device.config.getWorkgroupSize());

		// run kernel
		final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1); // buffer for event that is used for
																			// measuring the execution time
		checkCLError(clEnqueueNDRangeKernel(device.xqueue, device.kernel, dimensions, null, globalWorkSize,
				localWorkSize, null, xEventBuf));
		
		// workaround for AMD GPUs so that they always return the correct results back to the host
		if(device.vendor.toLowerCase().contains("advanced micro devices") || device.vendor.toLowerCase().contains("amd")) {
			long nullKernel = clCreateKernel(device.program, "null", errBuf);
			checkCLError(errBuf);
			PointerBuffer globalWorkSizeNullKernel = BufferUtils.createPointerBuffer(dimensions);
			globalWorkSizeNullKernel.put(0, device.workloadGlobalWorkSize);
			PointerBuffer localWorkSizeNullKernel = BufferUtils.createPointerBuffer(dimensions);
			localWorkSizeNullKernel.put(0, device.config.getWorkgroupSize());
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
					device.duration += (endBuf.get(0) - startBuf.get(0)) / 1000000;	// convert nanoseconds to milliseconds
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
					* symmetry(device.workloadConstellations.get(i).getStartijkl() & 0b11111111111111111111);
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
					// ignore
				}
			}
			device.stopReaderThread = 0;
		});
	}

	// --------------------------------------------------------
	// --------- (re-)storing, real time analytics ----------
	// --------------------------------------------------------

	@Override
	protected void store_(String filepath) throws IOException {
		// if Solver was not even started yet or is already done, throw exception
		if (constellations.size() == 0) {
			throw new IllegalStateException("Nothing to be saved");
		}
		ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
		out.writeValue(new File(filepath),
				new SolverState(N, System.currentTimeMillis() - start + storedDuration, constellations));
	}

	@Override
	protected void inject_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
		if (!isIdle()) {
			throw new IllegalStateException("Cannot inject while the Solver is running");
		}
		ObjectMapper mapper = new ObjectMapper();
		SolverState state = mapper.readValue(new File(filepath), SolverState.class);
		setN(state.getN());
		storedDuration = state.getStoredDuration();
		constellations = state.getConstellations();
		injected = true;
	}

	@Override
	public boolean isInjected() {
		return injected;
	}

	@Override
	public void reset() {
		devices.clear();
		contexts = null;
		programs = null;
		constellations.clear();
		duration = 0;
		storedDuration = 0;
		start = 0;
		end = 0;
		workloadSize = 0;
		weightSum = 0;
		injected = false;
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
		if(deviceIndex < 0 || deviceIndex >= availableDevices.size())
			throw new IllegalArgumentException("Invalid valule! Device index must be a number >= 0 and < [number of available devices]");
		return availableDevices.get(deviceIndex).duration;
	}

	// --------------------------------------------------------
	// --------------------- devices ------------------------
	// --------------------------------------------------------

	private void fetchAvailableDevices(MemoryStack stack) {
		IntBuffer entityCountBuf = stack.mallocInt(1);
		checkCLError(clGetPlatformIDs(null, entityCountBuf));
		if (entityCountBuf.get(0) == 0) {
			throw new RuntimeException("No OpenCL platforms found.");
		}
		PointerBuffer platforms = stack.mallocPointer(entityCountBuf.get(0));
		checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));

		for (int p = 0; p < platforms.capacity(); p++) {
			long platform = platforms.get(p);
			checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, entityCountBuf));
			if (entityCountBuf.get(0) == 0) {
				throw new RuntimeException("No OpenCL devices found.");
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

	public void setDeviceConfigs(DeviceConfig... deviceConfigsInput) {
		devices.clear();
		weightSum = 0;

		if (deviceConfigsInput[0].equals(DeviceConfig.ALL_DEVICES)) {
			for (Device device : availableDevices) {
				devices.add(device);
				device.config = Config.getDefaultConfig().getGPUDeviceConfigs()[0];
				weightSum += device.config.getWeight();
			}
			return;
		}

		ArrayList<DeviceConfig> deviceConfigsTmp = new ArrayList<DeviceConfig>();
		for (DeviceConfig deviceConfig : deviceConfigsInput) {
			if (deviceConfig.getWeight() == 0)
				continue;
			if (deviceConfigsTmp.stream().anyMatch(dvcCfg -> deviceConfig.getIndex() == dvcCfg.getIndex())) // check for duplicates
				continue;
			if (deviceConfig.getIndex() >= 0 && deviceConfig.getIndex() < availableDevices.size()) {
				deviceConfigsTmp.add(deviceConfig);
				Device device = availableDevices.get(deviceConfig.getIndex());
				device.config = deviceConfig;
				devices.add(device);
				weightSum += deviceConfig.getWeight();
			}
		}
	}

	public List<DeviceInfo> getDevices() {
		List<DeviceInfo> deviceInfos = new ArrayList<DeviceInfo>(devices.size());
		for (int i = 0; i < devices.size(); i++) {
			deviceInfos.add(new DeviceInfo(i, devices.get(i).vendor, devices.get(i).name));
		}
		return deviceInfos;
	}

	// --------------------------------------------------------
	// ------------ further run configurations --------------
	// --------------------------------------------------------

	public void setPresetQueens(int presetQueens) {
		this.presetQueens = presetQueens;
	}

	// --------------------------------------------------------
	// ------------------ utility methods -------------------
	// --------------------------------------------------------

	private String getKernelSourceAsString(String filepath) {
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
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultString;
	}

	private boolean symmetry90(int ijkl) {
		if (((geti(ijkl) << 15) + (getj(ijkl) << 10) + (getk(ijkl) << 5) + getl(ijkl)) == (((N - 1 - getk(ijkl)) << 15)
				+ ((N - 1 - getl(ijkl)) << 10) + (getj(ijkl) << 5) + geti(ijkl)))
			return true;
		return false;
	}

	// how often does a found solution count for this start constellation
	private int symmetry(int ijkl) {
		if (geti(ijkl) == N - 1 - getj(ijkl) && getk(ijkl) == N - 1 - getl(ijkl)) // starting constellation symmetric by
																					// rot180?
			if (symmetry90(ijkl)) // even by rot90?
				return 2;
			else
				return 4;
		else
			return 8; // none of the above?
	}

	private int geti(int ijkl) {
		return ijkl >>> 15;
	}

	private int getj(int ijkl) {
		return (ijkl >>> 10) & 31;
	}

	private int getk(int ijkl) {
		return (ijkl >>> 5) & 31;
	}

	private int getl(int ijkl) {
		return ijkl & 31;
	}

	private int getjkl(int ijkl) {
		return ijkl & 0b111111111111111;
	}

	// a class holding all OpenCL bindings needed for a single OpenCL device to
	// operate
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
