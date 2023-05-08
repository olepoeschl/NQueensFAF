package de.nqueensfaf.compute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLEventCallback;
import org.lwjgl.system.MemoryStack;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.clSetEventCallback;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import static de.nqueensfaf.compute.InfoUtil.*;

import de.nqueensfaf.files.Constellation;
import de.nqueensfaf.files.DeviceConfig;
import de.nqueensfaf.files.SolverState;

public class GPUSolver extends Solver {

	private static Path tempDir;
	private static boolean openclable = true;
	// check if a OpenCL-capable device is available and block GpuSolver and print
	// an error message, if not
	static {
		int checked = checkOpenCL();
		switch (checked) {
		case 0:
			openclable = false;
			break;
		case 1:
			openclable = true;
			break;
		case -1:
			// checking for OpenCL-capable devices was not possible
			System.err.println("Unable to check for OpenCL-capable devices. Assuming that there is at least 1.");
			System.err.println(
					"To get rid of this warning, install 'clinfo' (better option) or use NQueensFAF.setIgnoreOpenCLCheck(true) (will crash the JVM if no OpenCL-capable device is found).");
			openclable = true;
			break;
		}
		if (!openclable) {
			System.err.println("No OpenCL-capable device was found. GpuSolver is not available.");
		}
	}

	// OpenCL stuff
	private ArrayList<Long> availableDevices;
	private HashMap<Long, ArrayList<Long>> devicesByPlatform;
	private HashMap<Long, String> nameByDevice;
	private ArrayList<DeviceConfig> deviceConfigs;
	private ArrayList<Long> contexts;
	private HashMap<Long, Long> contextByDevice;
	private long queue;
	private long clEvent;
	private CLEventCallback eventCB;
	private long program;
	private long kernel;
	private Long ldMem, rdMem, colMem, startijklMem, resMem;
	private ByteBuffer resPtr;
	private int globalWorkSize;
	private int workgroupSize;
	private int presetQueens;

	// calculation related stuff
	private GPUConstellationsGenerator generator;
	private ArrayList<Constellation> constellations, remainingConstellations;
	private long duration, start, end, storedDuration;
	private long solutions;
	private float progress;
	private int numberOfValidConstellations;

	// control flow variables
	private boolean restored = false;

	// non public constructor
	protected GPUSolver() {
		if(openclable) {
			contexts = new ArrayList<Long>();
			
			availableDevices = new ArrayList<Long>();
			devicesByPlatform = new HashMap<Long, ArrayList<Long>>();
			nameByDevice = new HashMap<Long, String>();
			deviceConfigs = new ArrayList<DeviceConfig>();
			contextByDevice = new HashMap<Long, Long>();
			try (MemoryStack stack = stackPush()) {
				fetchAvailableDevices(stack);
			}
		}
	}

	// inherited functions
	@Override
	protected void run() {
		if (start != 0) {
			throw new IllegalStateException(
					"You first have to call reset() when calling solve() multiple times on the same object");
		}
		if (!openclable) {
			throw new IllegalStateException("No OpenCL-capable device was found. GpuSolver is not available.");
		}
		if (N <= 6) { // if N is very small, use the simple Solver from the parent class
			// prepare simulating progress = 100
			start = System.currentTimeMillis();
			solutions = solveSmallBoard();
			end = System.currentTimeMillis();
			progress = 1;
			return;
		}

		try {
			
			try (MemoryStack stack = stackPush()) {
				createContexts(stack);
				for(var dvcCfg : deviceConfigs) {
					
					init(stack, dvcCfg);
					
					genConstellations();
					
				}
			}
			
			
//			globalWorkSize = remainingConstellations.size();
//
//			try (MemoryStack stack = stackPush()) {
//				init(stack);
//				transferDataToDevice();
//				setKernelArgs(stack);
//				explosionBoost9000();
//				readResults();
//				releaseCLObjects();
//			}

			restored = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void store_(String filepath) throws IOException {
		// if Solver was not even started yet or is already done, throw exception
		if (constellations.size() == 0) {
			throw new IllegalStateException("Nothing to be saved");
		}
		ArrayList<Constellation> tmpConstellations = new ArrayList<Constellation>();
		for (var c : constellations) {
			if (c.getStartijkl() >> 20 != 69)
				tmpConstellations.add(c);
		}
		ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
		out.writeValue(new File(filepath),
				new SolverState(N, System.currentTimeMillis() - start + storedDuration, tmpConstellations));
	}

	@Override
	public void restore_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
		if (!isIdle()) {
			throw new IllegalStateException("Cannot restore while the Solver is running");
		}
		ObjectMapper mapper = new ObjectMapper();
		SolverState state = mapper.readValue(new File(filepath), SolverState.class);
		setN(state.getN());
		storedDuration = state.getStoredDuration();
		constellations = state.getConstellations();
		numberOfValidConstellations = constellations.size();

		generator = new GPUConstellationsGenerator();
		generator.sortConstellations(constellations);
		remainingConstellations = new ArrayList<Constellation>();
		int currentIjkl = constellations.get(0).getStartijkl() & ((1 << 20) - 1);
		for (var c : constellations) {
			// iterate through constellations, add each remaining constellations and fill up
			// each group of ijkl till its dividable by workgroup-size
			if (c.getSolutions() >= 0)
				continue;

			if ((c.getStartijkl() & ((1 << 20) - 1)) != currentIjkl) { // check if new ijkl is found
				while (remainingConstellations.size() % workgroupSize != 0) {
					generator.addTrashConstellation(remainingConstellations, currentIjkl);
				}
				currentIjkl = c.getStartijkl() & ((1 << 20) - 1);
			}
			remainingConstellations.add(c);
		}
		while (remainingConstellations.size() % workgroupSize != 0) {
			generator.addTrashConstellation(remainingConstellations, currentIjkl);
		}

		restored = true;
	}

	@Override
	public boolean isRestored() {
		return restored;
	}

	@Override
	public void reset() {
		duration = 0;
		start = 0;
		end = 0;
		solutions = 0;
		progress = 0;
		globalWorkSize = 0;
		numberOfValidConstellations = 0;
		restored = false;
	}

	@Override
	public long getDuration() {
		if (end == 0)
			if (isRunning() && start != 0) {
				return System.currentTimeMillis() - start + storedDuration;
			}
		return duration;
	}

	@Override
	public float getProgress() {
		return progress;
	}

	@Override
	public long getSolutions() {
		return solutions;
	}

	// own functions
	private void createContexts(MemoryStack stack) {
		IntBuffer errBuf = stack.callocInt(1);
		devicesByPlatform.keySet().stream().distinct().forEach(platform -> {
			PointerBuffer ctxProps = stack.mallocPointer(3);
			ctxProps
				.put(CL_CONTEXT_PLATFORM)
				.put(platform)
				.put(NULL)
				.flip();
			PointerBuffer devices = stack.mallocPointer(devicesByPlatform.get(platform).size());
			for(long device : devicesByPlatform.get(platform)) {
				if(deviceConfigs.stream().anyMatch(dvcCfg -> device == dvcCfg.getId()))	// if the device shall be used
					devices.put(device);
			}
			if(devices.capacity() > 0) {	// if at least one device of this platform is used, create the context
				long context = clCreateContext(ctxProps, devices, null, NULL, errBuf);
				checkCLError(errBuf.get(0));
				contexts.add(context);
				// 
				devices.position(0);
				while(devices.hasRemaining()) {
					contextByDevice.put(devices.get(), context);
				}
			}
		});
	}
	
	private void init(MemoryStack stack, DeviceConfig deviceConfig) {
		IntBuffer errBuf = stack.callocInt(1);
		long deviceId = deviceConfig.getId();
		long context = contextByDevice.get(deviceId);
		
		queue = clCreateCommandQueue(context, deviceId, CL_QUEUE_PROFILING_ENABLE, errBuf);
		checkCLError(errBuf.get(0));

		program = clCreateProgramWithSource(context, getKernelSourceAsString("de/nqueensfaf/res/kernels.c"), null);
		String options = "-D N=" + N + " -D BLOCK_SIZE=" + workgroupSize + " -cl-mad-enable";
		int error = clBuildProgram(program, device, options, null, 0);
		checkCLError(error);

		// determine which kernel to use
		if (getDeviceInfoStringUTF8(device, CL_DEVICE_VENDOR).toLowerCase().contains("intel")) {
			kernel = clCreateKernel(program, "nqfaf_intel", errBuf);
		} else {
			kernel = clCreateKernel(program, "nqfaf_default", errBuf);
		}
	}

	private void genConstellations() {
		if (!restored) {
			generator = new GPUConstellationsGenerator();
			generator.genConstellations(N, workgroupSize, presetQueens);

			constellations = generator.getConstellations();
			remainingConstellations = constellations;
			numberOfValidConstellations = generator.getNumberOfValidConstellations();
		}
	}

	private void transferDataToDevice() {
		// OpenCL-Memory Objects to be passed to the kernel
		// ld
		ldMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize * 4, errBuf);
		ByteBuffer ldPtr = clEnqueueMapBuffer(queue, ldMem, true, CL_MAP_WRITE, 0, globalWorkSize * 4, null, null,
				errBuf, null);
		checkCLError(errBuf.get(0));
		for (int i = 0; i < globalWorkSize; i++) {
			ldPtr.putInt(i * 4, remainingConstellations.get(i).getLd());
		}
		checkCLError(clEnqueueUnmapMemObject(queue, ldMem, ldPtr, null, null));

		// rd
		rdMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize * 4, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer rdPtr = clEnqueueMapBuffer(queue, rdMem, true, CL_MAP_WRITE, 0, globalWorkSize * 4, null, null, errBuf,
				null);
		checkCLError(errBuf.get(0));
		for (int i = 0; i < globalWorkSize; i++) {
			rdPtr.putInt(i * 4, remainingConstellations.get(i).getRd());
		}
		checkCLError(clEnqueueUnmapMemObject(queue, rdMem, rdPtr, null, null));

		// col
		colMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize * 4, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer colPtr = clEnqueueMapBuffer(queue, colMem, true, CL_MAP_WRITE, 0, globalWorkSize * 4, null, null, errBuf,
				null);
		checkCLError(errBuf.get(0));
		for (int i = 0; i < globalWorkSize; i++) {
			colPtr.putInt(i * 4, remainingConstellations.get(i).getCol());
		}
		checkCLError(clEnqueueUnmapMemObject(queue, colMem, colPtr, null, null));

		// startijkl
		startijklMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize * 4, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer startijklPtr = clEnqueueMapBuffer(queue, startijklMem, true, CL_MAP_WRITE, 0, globalWorkSize * 4, null, null,
				errBuf, null);
		checkCLError(errBuf.get(0));
		for (int i = 0; i < globalWorkSize; i++) {
			startijklPtr.putInt(i * 4, remainingConstellations.get(i).getStartijkl());
		}
		checkCLError(clEnqueueUnmapMemObject(queue, startijklMem, startijklPtr, null, null));

		// result memory
		resMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize * 8, errBuf);
		checkCLError(errBuf.get(0));
		resPtr = clEnqueueMapBuffer(queue, resMem, true, CL_MAP_WRITE, 0, globalWorkSize * 8, null, null, errBuf,
				null);
		checkCLError(errBuf.get(0));
		for (int i = 0; i < globalWorkSize; i++) {
			resPtr.putLong(i * 8, remainingConstellations.get(i).getSolutions());
		}
		checkCLError(clEnqueueUnmapMemObject(queue, resMem, resPtr, null, null));

		checkCLError(clFlush(queue));
		checkCLError(clFinish(queue));
	}

	private void setKernelArgs(MemoryStack stack) {
		// ld
		LongBuffer ldArg = stack.mallocLong(1);
		ldArg.put(0, ldMem);
		checkCLError(clSetKernelArg(kernel, 0, ldArg));
		// rd
		LongBuffer rdArg = stack.mallocLong(1);
		rdArg.put(0, rdMem);
		checkCLError(clSetKernelArg(kernel, 1, rdArg));
		// col
		LongBuffer colArg = stack.mallocLong(1);
		colArg.put(0, colMem);
		checkCLError(clSetKernelArg(kernel, 2, colArg));
		// startijkl
		LongBuffer startijklArg = stack.mallocLong(1);
		startijklArg.put(0, startijklMem);
		checkCLError(clSetKernelArg(kernel, 3, startijklArg));
		// res
		LongBuffer resArg = stack.mallocLong(1);
		resArg.put(0, resMem);
		checkCLError(clSetKernelArg(kernel, 4, resArg));
	}

	private void explosionBoost9000() {
		// create buffer of pointers defining the multi-dimensional size of the number
		// of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkers = BufferUtils.createPointerBuffer(dimensions);
		globalWorkers.put(0, globalWorkSize);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, workgroupSize);

		// run kernel
		final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1); // buffer for event that is used for
																			// measuring the execution time
		checkCLError(clEnqueueNDRangeKernel(queue, kernel, dimensions, null, globalWorkers, localWorkSize, null,
				xEventBuf));

		// set pseudo starttime
		start = System.currentTimeMillis();

		// get exact time values using CLEvent
		LongBuffer startBuf = BufferUtils.createLongBuffer(1), endBuf = BufferUtils.createLongBuffer(1);
		clEvent = xEventBuf.get(0);
		int errcode = clSetEventCallback(clEvent, CL_COMPLETE,
				eventCB = CLEventCallback.create((event, event_command_exec_status, user_data) -> {
					int err = clGetEventProfilingInfo(event, CL_PROFILING_COMMAND_START, startBuf, null);
					checkCLError(err);
					err = clGetEventProfilingInfo(event, CL_PROFILING_COMMAND_END, endBuf, null);
					checkCLError(err);
				}), NULL);
		checkCLError(errcode);

		// a thread continously reading gpu data into ram
		final StringBuilder gpuReaderThreadStopper = new StringBuilder("");
		gpuReaderThread(gpuReaderThreadStopper).start();

		// wait for the gpu computation to finish
		checkCLError(clFlush(queue));
		checkCLError(clFinish(queue));

		// stop the thread that continously reads from the GPU
		gpuReaderThreadStopper.append("STOP");
		while (!gpuReaderThreadStopper.toString().equals("STOPPED")) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// ignore
			}
		}

		// measure execution time
		while (startBuf.get(0) == 0) { // wait for event callback to be executed
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		start = startBuf.get(0);
		end = endBuf.get(0);
		duration = ((end - start) / 1000000) + storedDuration; // convert nanoseconds to milliseconds
	}

	private void readResults() {
		// read result and progress memory buffers
		checkCLError(clEnqueueReadBuffer(queue, resMem, true, 0, resPtr, null, null));

		long tmpSolutions = 0;
		int solvedConstellations = 0;
		for (int i = 0; i < globalWorkSize; i++) {
			if (remainingConstellations.get(i).getStartijkl() >> 20 == 69) // start=69 is for trash constellations
				continue;
			long solutionsForConstellation = resPtr.getLong(i * 8)
					* symmetry(remainingConstellations.get(i).getStartijkl() & 0b11111111111111111111);
			if (solutionsForConstellation >= 0)
				// synchronize with the list of constellations on the RAM
				remainingConstellations.get(i).setSolutions(solutionsForConstellation);
		}
		for (var c : constellations) {
			if (c.getStartijkl() >> 20 == 69) // start=69 is for trash constellations
				continue;
			long solutionsForConstellation = c.getSolutions();
			if (solutionsForConstellation >= 0) {
				tmpSolutions += solutionsForConstellation;
				solvedConstellations++;
			}
		}
		progress = (float) solvedConstellations / numberOfValidConstellations;
		solutions = tmpSolutions;
	}

	private void releaseCLObjects() {
		checkCLError(clReleaseMemObject(ldMem));
		checkCLError(clReleaseMemObject(rdMem));
		checkCLError(clReleaseMemObject(colMem));
		checkCLError(clReleaseMemObject(startijklMem));
		checkCLError(clReleaseMemObject(resMem));

		eventCB.free();

		checkCLError(clReleaseEvent(clEvent));

		checkCLError(clReleaseKernel(kernel));
		checkCLError(clReleaseProgram(program));
		checkCLError(clReleaseCommandQueue(queue));
		checkCLError(clReleaseContext(context));
	}

	private Thread gpuReaderThread(StringBuilder gpuReaderThreadStopper) {
		return new Thread(() -> {
			while (gpuReaderThreadStopper.toString().equals("")) {
				checkCLError(clEnqueueReadBuffer(queue, resMem, true, 0, resPtr, null, null));

				long tmpSolutions = 0;
				int solvedConstellations = 0;
				for (int i = 0; i < globalWorkSize; i++) {
					if (remainingConstellations.get(i).getStartijkl() >> 20 == 69) // start=69 is for trash
																					// constellations
						continue;
					long solutionsForConstellation = resPtr.getLong(i * 8)
							* symmetry(remainingConstellations.get(i).getStartijkl() & 0b11111111111111111111);
					if (solutionsForConstellation >= 0)
						// synchronize with the list of constellations on the RAM
						remainingConstellations.get(i).setSolutions(solutionsForConstellation);
				}
				for (var c : constellations) {
					if (c.getStartijkl() >> 20 == 69) // start=69 is for trash constellations
						continue;
					long solutionsForConstellation = c.getSolutions();
					if (solutionsForConstellation >= 0) {
						tmpSolutions += solutionsForConstellation;
						solvedConstellations++;
					}
				}
				progress = (float) solvedConstellations / numberOfValidConstellations;
				solutions = tmpSolutions;
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// bad practice, but just ignore it
				}
			}
			gpuReaderThreadStopper.append("PED");
		});
	}

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
			devicesByPlatform.put(platform, new ArrayList<Long>());
			PointerBuffer devicesBuf = stack.mallocPointer(entityCountBuf.get(0));
			checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devicesBuf, (IntBuffer) null));
			for (int d = 0; d < devicesBuf.capacity(); d++) {
				long device = devicesBuf.get(d);
				availableDevices.add(device);
				devicesByPlatform.get(platform).add(device);
				nameByDevice.put(device, getDeviceInfoStringUTF8(device, CL_DEVICE_VENDOR) + ": " + getDeviceInfoStringUTF8(device, CL_DEVICE_NAME));
			}
		}
	}
	
	public HashMap<Long, String> getNamesByDevices() {
		return nameByDevice;
	}

	public void setDeviceConfigs(DeviceConfig... deviceConfigsInput) {
		for(DeviceConfig deviceConfig : deviceConfigsInput) {
			if(deviceConfig.getId() == -69) { 	// 69 -> use default device
				deviceConfig.setId(availableDevices.get(0));
			}
			if(availableDevices.indexOf(deviceConfig.getId()) == -1)
				continue;
			if(deviceConfigs.stream().anyMatch(dvcCfg -> deviceConfig.getId() == dvcCfg.getId()))	// check for duplicates
				continue;
			deviceConfigs.add(deviceConfig);
		}
	}
	
	// utility functions
	private static String getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			// windows
			return "win";
		} else if (os.contains("mac")) {
			// mac
			return "mac";
		} else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			// unix (linux etc)
			return "unix";
		} else if (os.contains("sunos")) {
			// solaris
			return "solaris";
		} else {
			// unknown os
			return os;
		}
	}

	private static int checkOpenCL() {
		Process clinfo;
		try {
			clinfo = Runtime.getRuntime().exec(new String[] { "clinfo" });
			BufferedReader in = new BufferedReader(new InputStreamReader(clinfo.getInputStream()));
			String line;
			if ((line = in.readLine()) != null) {
				if (line.contains(" 0") || line.contains("no usable platforms")) {
					return 0;
				} else {
					return 1;
				}
			}
		} catch (IOException e) {
			// clinfo is not installed.
			switch (getOS()) {
			case "win":
				// Good that we have the windows version in our archive!
				File clinfoFile = unpackClinfo();
				if (clinfoFile == null) {
					return -1;
				}
				try {
					clinfo = Runtime.getRuntime().exec(new String[] { clinfoFile.getAbsolutePath() });
					BufferedReader in = new BufferedReader(new InputStreamReader(clinfo.getInputStream()));
					String line;
					if ((line = in.readLine()) != null) {
						if (line.contains(" 0") || line.contains("no usable platforms")) {
							return 0;
						} else {
							return 1;
						}
					}
				} catch (IOException e1) {
					return -1;
				}
				break;
			case "mac", "unix", "solaris":
				break;
			default:
				break;
			}
		}
		return -1;
	}

	private static File unpackClinfo() {
		// create temporary directory to store the clinfo file inside
		try {
			tempDir = Files.createTempDirectory("NQueensFaf");
			// copy the clinfo file from within the jar to the temporary directory
			InputStream in = GPUSolver.class.getClassLoader()
					.getResourceAsStream("de/nqueensfaf/res/clinfo/clinfo.exe");
			byte[] buffer = new byte[1024];
			int read = -1;
			File file = new File(tempDir + "/clinfo.exe");
			FileOutputStream fos = new FileOutputStream(file);
			while ((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();

			// delete the temp directory at the end of the program
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					Files.delete(Paths.get(tempDir + "/clinfo.exe"));
					Files.delete(tempDir);
				} catch (IOException e) {
					// just ignore it
				}
			}));

			return file;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}

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

	// getters and setters
	public int getGlobalWorkSize() {
		return globalWorkSize;
	}

	public int getWorkgroupSize() {
		return workgroupSize;
	}

	
	public void setWorkgroupSize(int s) {
		long maxWorkgroupSize = getDeviceInfoPointer(device, CL_DEVICE_MAX_WORK_GROUP_SIZE);
		if (s <= 0 || s > maxWorkgroupSize) {
			throw new IllegalArgumentException(
					"WorkgroupSize must be between 0 and " + maxWorkgroupSize + " (=max for this device)");
		}
		workgroupSize = s;
	}

	public int getNumberOfPresetQueens() {
		return presetQueens;
	}

	public void setNumberOfPresetQueens(int pq) {
		if (pq < 4 || pq > 10) {
			throw new IllegalArgumentException("Number of preset queens must be between 4 and 10");
		}
		presetQueens = pq;
	}
}