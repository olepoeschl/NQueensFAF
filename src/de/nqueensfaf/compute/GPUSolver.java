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
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
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

import de.nqueensfaf.NQueensFAF;
import de.nqueensfaf.Solver;
import de.nqueensfaf.files.Config;
import de.nqueensfaf.files.Constellation;
import de.nqueensfaf.files.SolverState;

public class GPUSolver extends Solver {

	private static Path tempDir;
	private static boolean openclable = true;
	// check if a OpenCL-capable device is available and block GpuSolver and print an error message, if not 
	static {
		int checked = checkOpenCL();
		switch(checked) {
		case 0:
			openclable = false;
			break;
		case 1:
			openclable = true;
			break;
		case -1:
			// checking for OpenCL-capable devices was not possible
			if(NQueensFAF.getIgnoreOpenCLCheck()) {
				openclable = true;
				break;
			}
			System.err.println("Unable to check for OpenCL-capable devices.");
			System.err.println("To get rid of this warning, install 'clinfo' (better option) or use NQueensFAF.setIgnoreOpenCLCheck(true) (will crash the JVM if no OpenCL-capable device is found).");
			openclable = false;
			break;
		}
		if(!openclable) {
			System.err.println("No OpenCL-capable device was found. GpuSolver is not available.");
		}
	}

	// OpenCL stuff
	private IntBuffer errBuf;
	private ByteBuffer resBuf;
	private PointerBuffer ctxProps;
	private long context;
	private CLContextCallback contextCB;
	private long platform;
	private HashMap<Long, Long> platformByDevice;
	private List<Long> devices;
	private long device = Config.getDefaultConfig().getGPUDevice();
	private long xqueue, memqueue;
	private long clEvent;
	private long program;
	private long kernel;
	private Long ldMem, rdMem, colMem, startijklMem, resMem;
	private int globalWorkSize;
	private final int MAX_GLOBAL_WORKSIZE = 500_000;
	private int workgroupSize = Config.getDefaultConfig().getGPUWorkgroupSize();
	private int presetQueens = Config.getDefaultConfig().getGPUPresetQueens();

	// calculation related stuff
	private GPUConstellationsGenerator generator;
	private ArrayList<Constellation> constellations, remainingConstellations, workloadConstellations;
	private long duration, start, end, storedDuration;
	private long solutions;
	private float progress;
	private int numberOfValidConstellations;
	
	// control flow variables
	private boolean restored = false;
	
	public GPUSolver() {
		if(openclable)
			getAvailableDevices();		// fill the devices list with all available devices
	}
	
	// inherited functions
	@Override
	protected void run() {
		if(start != 0) {
			throw new IllegalStateException("You first have to call reset() when calling solve() multiple times on the same object");
		}
		if(!openclable) {
			throw new IllegalStateException("No OpenCL-capable device was found. GpuSolver is not available.");
		}
		if(N <= 6) {	// if N is very small, use the simple Solver from the parent class
			// prepare simulating progress = 100
			start = System.currentTimeMillis();
			solutions = solveSmallBoard();
			end = System.currentTimeMillis();
			progress = 1;
			return;
		}
		
		// if init fails, do not proceed
		try (
			MemoryStack stack = stackPush();
		) {
			init(stack);
			
			genConstellations();
			
			// split huge global work sizes into multiple smaller workloads
			int workloadSize = MAX_GLOBAL_WORKSIZE / workgroupSize * workgroupSize;
			int numberOfWorkloads = remainingConstellations.size() / workloadSize + 1;
			workloadConstellations = new ArrayList<Constellation>();
			for(int i = 0; i < numberOfWorkloads; i++) {
				int toIdx = i * workloadSize + workloadSize;
				if(toIdx > remainingConstellations.size())
					toIdx = remainingConstellations.size();
				workloadConstellations.clear();
				workloadConstellations.addAll(remainingConstellations.subList(i * workloadSize, toIdx));
				globalWorkSize = workloadConstellations.size();
				
				transferDataToDevice(stack);
				explosionBoost9000(stack);
				readResults(stack);
				
				storedDuration = duration;
				resetBetweenWorkloads();
			}
			
			terminate();
			restored = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void store_(String filepath) throws IOException {
		// if Solver was not even started yet or is already done, throw exception
		if(constellations.size() == 0) {
			throw new IllegalStateException("Nothing to be saved");
		}
		ArrayList<Constellation> tmpConstellations = new ArrayList<Constellation>();
		for(var c : constellations) {
			if(c.getStartijkl() >> 20 != 69)
				tmpConstellations.add(c);
		}
		ObjectWriter out = new ObjectMapper().writer(new DefaultPrettyPrinter());
		out.writeValue(new File(filepath), new SolverState(N, System.currentTimeMillis() - start + storedDuration, tmpConstellations));
	}

	@Override
	public void restore_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
		if(!isIdle()) {
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
			// iterate through constellations, add each remaining constellations and fill up each group of ijkl till its dividable by workgroup-size
			if(c.getSolutions() >= 0)
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
	
	private void resetBetweenWorkloads() {
		start = 0;
		end = 0;
	}

	@Override
	public long getDuration() {
		if(end == 0)
			if(isRunning() && start != 0) {
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
	private void init(MemoryStack stack) {
		try {
            IntBuffer pi = stack.mallocInt(1);
            checkCLError(clGetPlatformIDs(null, pi));
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	            CL.destroy();
            }));
        } catch (Throwable t) {
        	t.printStackTrace();
            openclable = false;
        }
		
		// intbuffer for containing error information if needed
		errBuf = stack.callocInt(1);

		ctxProps = stack.mallocPointer(3);
        ctxProps
            .put(0, CL_CONTEXT_PLATFORM)
            .put(1, platform)
            .put(2, 0);
        
        context = clCreateContext(ctxProps, device, contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) -> {
            System.err.println("[LWJGL] cl_context_callback");
            System.err.println("\tInfo: " + memUTF8(errinfo));
        }), NULL, errBuf);
        checkCLError(errBuf);
		
		xqueue = clCreateCommandQueue(context, device, CL_QUEUE_PROFILING_ENABLE, errBuf);
		checkCLError(errBuf.get(0));
		memqueue = clCreateCommandQueue(context, device, CL_QUEUE_PROFILING_ENABLE, errBuf);
		checkCLError(errBuf.get(0));

		program = clCreateProgramWithSource(context, getKernelSourceAsString("de/nqueensfaf/res/kernels.c"), null);
		String options = "-D N="+N + " -D BLOCK_SIZE="+workgroupSize + " -cl-mad-enable";
		PointerBuffer devicesBuf = stack.mallocPointer(1);
		devicesBuf.put(0, device);
		int error = clBuildProgram(program, devicesBuf, options, null, 0);
		checkCLError(error);
		
		// determine which kernel to use
		if(getDeviceInfoStringUTF8(device, CL_DEVICE_VENDOR).toLowerCase().contains("intel")) {
			kernel = clCreateKernel(program, "nqfaf_intel", errBuf);
		} else {
			kernel = clCreateKernel(program, "nqfaf_default", errBuf);
		}
	}

	private void genConstellations() {
		if(!restored) {
			generator = new GPUConstellationsGenerator();
			generator.genConstellations(N, workgroupSize, presetQueens);
			
			constellations = generator.getConstellations();
			remainingConstellations = constellations;
			numberOfValidConstellations = generator.getNumberOfValidConstellations();
		}
	}
	
	private void transferDataToDevice(MemoryStack stack) {
		// OpenCL-Memory Objects to be passed to the kernel
		// ld
		ldMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer paramPtr = clEnqueueMapBuffer(memqueue, ldMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, workloadConstellations.get(i).getLd());
		}
		checkCLError(clEnqueueUnmapMemObject(memqueue, ldMem, paramPtr, null, null));
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);
		
		// rd
		rdMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		paramPtr = clEnqueueMapBuffer(memqueue, rdMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, workloadConstellations.get(i).getRd());
		}
		checkCLError(clEnqueueUnmapMemObject(memqueue, rdMem, paramPtr, null, null));
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);
		
		// col
		colMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		paramPtr = clEnqueueMapBuffer(memqueue, colMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, workloadConstellations.get(i).getCol());
		}
		checkCLError(clEnqueueUnmapMemObject(memqueue, colMem, paramPtr, null, null));
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);
		
		// startijkl
		startijklMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		paramPtr = clEnqueueMapBuffer(memqueue, startijklMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, workloadConstellations.get(i).getStartijkl());
		}
		checkCLError(clEnqueueUnmapMemObject(memqueue, startijklMem, paramPtr, null, null));
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);

		// result memory
		resMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*8, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer resWritePtr = clEnqueueMapBuffer(memqueue, resMem, true, CL_MAP_WRITE, 0, globalWorkSize*8, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			resWritePtr.putLong(i*8, workloadConstellations.get(i).getSolutions());
		}
		checkCLError(clEnqueueUnmapMemObject(memqueue, resMem, resWritePtr, null, null));
//		clEnqueueWriteBuffer(memqueue, context, true, 0, resWritePtr, null, null);
		resBuf = BufferUtils.createByteBuffer(globalWorkSize*8);
		
		clFlush(memqueue);

		// set kernel parameters
		// ld
		LongBuffer ldArg = BufferUtils.createLongBuffer(1);
		ldArg.put(0, ldMem);
		checkCLError(clSetKernelArg(kernel, 0, ldArg));
		// rd
		LongBuffer rdArg = BufferUtils.createLongBuffer(1);
		rdArg.put(0, rdMem);
		checkCLError(clSetKernelArg(kernel, 1, rdArg));
		// col
		LongBuffer colArg = BufferUtils.createLongBuffer(1);
		colArg.put(0, colMem);
		checkCLError(clSetKernelArg(kernel, 2, colArg));
		// startijkl
		LongBuffer startijklArg = BufferUtils.createLongBuffer(1);
		startijklArg.put(0, startijklMem);
		checkCLError(clSetKernelArg(kernel, 3, startijklArg));
		// res
		LongBuffer resArg = BufferUtils.createLongBuffer(1);
		resArg.put(0, resMem);
		checkCLError(clSetKernelArg(kernel, 4, resArg));
	}
	
	private void explosionBoost9000(MemoryStack stack) {
		// create buffer of pointers defining the multi-dimensional size of the number of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkers = BufferUtils.createPointerBuffer(dimensions);
		globalWorkers.put(0, globalWorkSize);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, workgroupSize);

		// wait for all memory operations to finish
		checkCLError(clFinish(memqueue));
		
		// run kernel
		final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1);		// buffer for event that is used for measuring the execution time
		checkCLError(clEnqueueNDRangeKernel(xqueue, kernel, dimensions, null, globalWorkers, localWorkSize, null, xEventBuf));
		clFlush(xqueue);
		
		// set pseudo starttime
		start = System.currentTimeMillis();
		
		// get exact time values using CLEvent
		LongBuffer startBuf = BufferUtils.createLongBuffer(1), endBuf = BufferUtils.createLongBuffer(1);
		clEvent = xEventBuf.get(0);
        int errcode = clSetEventCallback(clEvent, CL_COMPLETE, CLEventCallback.create((event, event_command_exec_status, user_data) -> {
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
		checkCLError(clFinish(xqueue));
		
		// stop the thread that continously reads from the GPU
		gpuReaderThreadStopper.append("STOP");
		while(!gpuReaderThreadStopper.toString().equals("STOPPED")) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		// measure execution time
		while(startBuf.get(0) == 0) { // wait for event callback to be executed
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		start = startBuf.get(0);
		end = endBuf.get(0);
		duration = ((end - start) / 1000000) + storedDuration;	// convert nanoseconds to milliseconds
	}
	
	private void readResults(MemoryStack stack) {
		// read result and progress memory buffers
		checkCLError(clEnqueueReadBuffer(memqueue, resMem, true, 0, resBuf, null, null));

		long tmpSolutions = 0;
		int solvedConstellations = 0;
		for (int i = 0; i < globalWorkSize; i++) {
			if(workloadConstellations.get(i).getStartijkl() >> 20 == 69) // start=69 is for trash constellations
				continue;
			long solutionsForConstellation = resBuf.getLong(i*8) * symmetry(workloadConstellations.get(i).getStartijkl() & 0b11111111111111111111);
			if(solutionsForConstellation >= 0)
				// synchronize with the list of constellations on the RAM
				workloadConstellations.get(i).setSolutions(solutionsForConstellation);
		}
		for(var c : constellations) {
			if(c.getStartijkl() >> 20 == 69) // start=69 is for trash constellations
				continue;
			long solutionsForConstellation = c.getSolutions();
			if(solutionsForConstellation >= 0) {
				tmpSolutions += solutionsForConstellation;
				solvedConstellations++;
			}
		}
		progress = (float) solvedConstellations / numberOfValidConstellations;
		solutions = tmpSolutions;
	}
	
	private void terminate() {
		// release all CL-objects
		checkCLError(clReleaseEvent(clEvent));
		//eventCB.free();
		checkCLError(clReleaseCommandQueue(xqueue));
		checkCLError(clReleaseCommandQueue(memqueue));
		checkCLError(clReleaseKernel(kernel));
		checkCLError(clReleaseProgram(program));
		
		checkCLError(clReleaseMemObject(ldMem));
		checkCLError(clReleaseMemObject(rdMem));
		checkCLError(clReleaseMemObject(colMem));
		checkCLError(clReleaseMemObject(startijklMem));
		checkCLError(clReleaseMemObject(resMem));
		
		checkCLError(clReleaseContext(context));
		contextCB.free();
	}
	
	private Thread gpuReaderThread (StringBuilder gpuReaderThreadStopper) {
		return new Thread(() -> {
        	while(gpuReaderThreadStopper.toString().equals("")) {
        		checkCLError(clEnqueueReadBuffer(memqueue, resMem, true, 0, resBuf, null, null));

        		long tmpSolutions = 0;
        		int solvedConstellations = 0;
        		for (int i = 0; i < globalWorkSize; i++) {
        			if(workloadConstellations.get(i).getStartijkl() >> 20 == 69) // start=69 is for trash constellations
        				continue;
        			long solutionsForConstellation = resBuf.getLong(i*8) * symmetry(workloadConstellations.get(i).getStartijkl() & 0b11111111111111111111);
        			if(solutionsForConstellation >= 0)
        				// synchronize with the list of constellations on the RAM
        				workloadConstellations.get(i).setSolutions(solutionsForConstellation);
        		}
        		for(var c : constellations) {
        			if(c.getStartijkl() >> 20 == 69) // start=69 is for trash constellations
        				continue;
        			long solutionsForConstellation = c.getSolutions();
        			if(solutionsForConstellation >= 0) {
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
	
	// utility functions
	private static String getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if(os.contains("win")) {
			// windows
			return "win";
		} else if(os.contains("mac")) {
			// mac
			return "mac";
		} else if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			// unix (linux etc)
			return "unix";
		} else if(os.contains("sunos")) {
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
			clinfo = Runtime.getRuntime().exec(new String[]{"clinfo"});
			BufferedReader in = new BufferedReader(new InputStreamReader(clinfo.getInputStream()));
			String line;
			if((line = in.readLine()) != null) {
				if(line.contains(" 0") || line.contains("no usable platforms")) {
					return 0;
				} else {
					return 1;
				}
			}
		} catch (IOException e) {
			// clinfo is not installed.
			switch(getOS()) {
			case "win":
				// Good that we have the windows version in our archive!
				File clinfoFile = unpackClinfo();
				if(clinfoFile == null) {
					return -1;
				}
				try {
					clinfo = Runtime.getRuntime().exec(new String[]{clinfoFile.getAbsolutePath()});
					BufferedReader in = new BufferedReader(new InputStreamReader(clinfo.getInputStream()));
					String line;
					if((line = in.readLine()) != null) {
						if(line.contains(" 0") || line.contains("no usable platforms")) {
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
			InputStream in = GPUSolver.class.getClassLoader().getResourceAsStream("de/nqueensfaf/res/clinfo/clinfo.exe");
			byte[] buffer = new byte[1024];
			int read = -1;
			File file = new File(tempDir + "/clinfo.exe");
			FileOutputStream fos = new FileOutputStream(file);
			while((read = in.read(buffer)) != -1) {
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
		try (
			InputStream clSourceFile = GPUSolver.class.getClassLoader().getResourceAsStream(filepath);
			BufferedReader br = new BufferedReader(new InputStreamReader(clSourceFile));
		){
			String line = null;
			StringBuilder result = new StringBuilder();
			while((line = br.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}
			resultString = result.toString();
		} catch(NullPointerException e) {
			e.printStackTrace();
		} catch(IOException e) {
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
		if(geti(ijkl) == N-1-getj(ijkl) && getk(ijkl) == N-1-getl(ijkl))		// starting constellation symmetric by rot180?
			if(symmetry90(ijkl))		// even by rot90?
				return 2;
			else
				return 4;
		else
			return 8;					// none of the above?
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
	public String[] getAvailableDevices() {
		if(!openclable) {
			throw new IllegalStateException("No OpenCL-capable device was found. GpuSolver is not available.");
		}
		if(devices == null) {
			devices = new ArrayList<Long>();
			platformByDevice = new HashMap<Long, Long>();
		} else {
			devices.clear();
			platformByDevice.clear();
		}
		
		try (MemoryStack stack = stackPush()){
			ArrayList<String> deviceNames = new ArrayList<String>();
	        
	        IntBuffer pi = stack.mallocInt(1);
	        checkCLError(clGetPlatformIDs(null, pi));
	        if (pi.get(0) == 0) {
	            throw new RuntimeException("No OpenCL platforms found.");
	        }
			PointerBuffer platforms = stack.mallocPointer(pi.get(0));
	        checkCLError(clGetPlatformIDs(platforms, (IntBuffer)null));
			
			try {
				for (int p = 0; p < platforms.capacity(); p++) {
		            long platform = platforms.get(p);
		            
		            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, pi));
		            PointerBuffer devicesBuf = stack.mallocPointer(pi.get(0));
		            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devicesBuf, (IntBuffer)null));
		            for (int d = 0; d < devicesBuf.capacity(); d++) {
		                long device = devicesBuf.get(d);
						devices.add(device);
						platformByDevice.put(device, platform);
						deviceNames.add(getDeviceInfoStringUTF8(device, CL_DEVICE_NAME));
					}
				}
			} catch(NullPointerException e) {
				throw e;
			}
			String[] arr = new String[deviceNames.size()];
			for(int i = 0; i < arr.length; i++) {
				arr[i] = deviceNames.get(i);
			}
			
			return arr;
		}
	}

	public void setDevice(int idx) {
		if(!openclable) {
			throw new IllegalStateException("No OpenCL-capable device was found. GPUSolver is not available.");
		}
		if(idx < 0 || idx >= devices.size()) {
			throw new IllegalArgumentException("Invalid device index value: " + idx + " (size:" + devices.size() + ")");
		}
		device = devices.get(idx);
		platform = platformByDevice.get(device);
	}

	public int getGlobalWorkSize() {
		return globalWorkSize;
	}
	
	public int getWorkgroupSize() {
		return workgroupSize;
	}

	public void setWorkgroupSize(int s) {
		long maxWorkgroupSize = getDeviceInfoPointer(device, CL_DEVICE_MAX_WORK_GROUP_SIZE);
		if(s <= 0 || s > maxWorkgroupSize) {
			throw new IllegalArgumentException("WorkgroupSize must be between 0 and " + maxWorkgroupSize + " (=max for this device)");
		}
		workgroupSize = s;
	}

	public int getNumberOfPresetQueens() {
		return presetQueens;
	}
	
	public void setNumberOfPresetQueens(int pq) {
		if(pq < 4 || pq > 10) {
			throw new IllegalArgumentException("Number of preset queens must be between 4 and 10");
		}
		presetQueens = pq;
	}
}