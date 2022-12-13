package de.nqueensfaf.compute;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
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

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.clSetEventCallback;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import static de.nqueensfaf.compute.InfoUtil.*;

import de.nqueensfaf.NQueensFAF;
import de.nqueensfaf.Solver;

public class GpuSolver extends Solver {

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
		} else {
	        CL.getFunctionProvider();
		}
	}

	// OpenCL stuff
	private IntBuffer errBuf, progressBuf;
	private ByteBuffer resBuf;
	private PointerBuffer ctxProps;
	private long context;
	private CLContextCallback contextCB;
	private long platform;
	private HashMap<Long, Long> platformByDevice;
	private List<Long> devices;
	private long device;
	private long xqueue, memqueue;
	private long clEvent;
	private long program;
	private long kernel;
	private Long ldMem, rdMem, colMem, startjklMem, resMem, progressMem;
	private final Object resLock = new Object(), progressLock = new Object();
	private int WORKGROUP_SIZE = 64;
	private int PRE_QUEENS = 6;
	private int globalWorkSize;

	// calculation related stuff
	private GpuConstellationsGenerator generator;
	private int startConstCount;
	private ArrayList<Integer> ldList, rdList, colList, symList, startjklList;
	private long solutions, savedSolutions;
	private long duration, start, end, savedDuration;
	private float progress;
	private int savedSolvedConstellations;
	
	// control flow variables
	private boolean gpuDone = false;
	private boolean restored = false;
	
	public GpuSolver() {
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
			progress = 1f;
			gpuDone = true;
			start = System.currentTimeMillis();
			solutions = solveSmallBoard();
			end = System.currentTimeMillis();
			// simulate progress = 100
			startConstCount = 1;
			return;
		}
		if(device == 0) {
			throw new IllegalStateException("You have to choose a device by calling setDevice() before starting the Solver. See all available devices using getAvailableDevices()");
		}
		// if init fails, do not proceed
		try (
			MemoryStack stack = stackPush();
		) {
			init(stack);
			transferDataToDevice(stack);
			explosionBoost9000(stack);
			readResults(stack);
			terminate();
			restored = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void store_(String filepath) throws IOException {
		// if Solver was not even started yet or is already done, throw exception
		if(start == 0 || gpuDone) {
			throw new IllegalStateException("Nothing to be saved");
		}
		long solutions = savedSolutions;
		ArrayList<Integer> 
		ldListTmp = new ArrayList<Integer>(),
		rdListTmp = new ArrayList<Integer>(),
		colListTmp = new ArrayList<Integer>(),
		startjklListTmp = new ArrayList<Integer>(),
		symListTmp = new ArrayList<Integer>();
		synchronized(resLock) {
			clEnqueueReadBuffer(memqueue, resMem, true, 0, resBuf, null, null);
			for(int i = 0; i < globalWorkSize; i++) {
				if(resBuf.getLong(i*8) > 1) {
					solutions += resBuf.getLong(i*8) * symList.get(i);
				} else if(resBuf.getLong(i*8) <= 1 && startjklList.get(i) >> 15 != 69) {
					ldListTmp.add(ldList.get(i));
					rdListTmp.add(rdList.get(i));
					colListTmp.add(colList.get(i));
					startjklListTmp.add(startjklList.get(i));
					symListTmp.add(symList.get(i));
				}
			}
		}
		RestorationInformation resInfo = new RestorationInformation(N, getDuration(), solutions, startConstCount, ldListTmp, rdListTmp, colListTmp, startjklListTmp, symListTmp);

		try (
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				RandomAccessFile raf = new RandomAccessFile(filepath, "rw");
				FileOutputStream fos = new FileOutputStream(raf.getFD());
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				) {
			oos.writeObject(resInfo);
			oos.flush();
			var resInfoByteArray = baos.toByteArray();
			int bytes = 0;
			int bufsize = 1024;
			for(int i = 0; i < resInfoByteArray.length/bufsize; i++) {
				bytes = i*bufsize;
				bos.write(resInfoByteArray, bytes, bufsize);
				bos.flush();
				bytes += bufsize;
			}
			if(bytes < resInfoByteArray.length) {
				bos.write(resInfoByteArray, bytes, resInfoByteArray.length-bytes);
				bos.flush();
			}
		}
	}

	@Override
	public void restore_(String filepath) throws IOException, ClassNotFoundException, ClassCastException {
		if(!isIdle()) {
			throw new IllegalStateException("Cannot restore while the Solver is running");
		}

		byte[] resInfoByteArray = Files.readAllBytes(Path.of(filepath));
		try (
				ByteArrayInputStream bais = new ByteArrayInputStream(resInfoByteArray);
				ObjectInputStream ois = new ObjectInputStream (bais);
				) {
			RestorationInformation resInfo = (RestorationInformation) ois.readObject();

			reset();
			N = resInfo.N;
			savedDuration = resInfo.duration;
			savedSolutions = resInfo.solutions;
			startConstCount = resInfo.startConstCount;
			savedSolvedConstellations = startConstCount - resInfo.ldList.size();
			progress = (float) savedSolvedConstellations / startConstCount;

			ldList = new ArrayList<Integer>();
			rdList = new ArrayList<Integer>();
			colList = new ArrayList<Integer>();
			startjklList = new ArrayList<Integer>();
			symList = new ArrayList<Integer>();

			generator = new GpuConstellationsGenerator();
			generator.sortConstellations(ldList, rdList, colList, startjklList, symList);
			int currentJKL = resInfo.startjklList.get(0) & ((1 << 15)-1);
			for(int i = 0; i < resInfo.ldList.size(); i++) {
				if((resInfo.startjklList.get(i) & ((1 << 15)-1)) != currentJKL) {	// check if new jkl is found
					while(ldList.size() % WORKGROUP_SIZE != 0) {
						generator.addTrashConstellation(currentJKL, ldList, rdList, colList, startjklList, symList);
					}
					currentJKL = resInfo.startjklList.get(i) & ((1 << 15)-1);
				}
				ldList.add(resInfo.ldList.get(i));
				rdList.add(resInfo.rdList.get(i));
				colList.add(resInfo.colList.get(i));
				startjklList.add(resInfo.startjklList.get(i));
				symList.add(resInfo.symList.get(i));
			}
			while(ldList.size() % WORKGROUP_SIZE != 0) {
				generator.addTrashConstellation(currentJKL, ldList, rdList, colList, startjklList, symList);
			}
			restored = true;
		}
	}

	@Override
	public boolean isRestored() {
		return restored;
	}
	
	@Override
	public void reset() {
		globalWorkSize = 0;
		startConstCount = 0;
		progress = 0;
		savedSolvedConstellations = 0;
		solutions = 0;
		savedSolutions = 0;
		duration = 0;
		savedDuration = 0;
		start = 0;
		end = 0;
		gpuDone = false;
		restored = false;
	}

	@Override
	public long getDuration() {
		if(restored && isIdle())
			duration = savedDuration;
		else if(start != 0 && end == 0)
			duration = savedDuration + System.currentTimeMillis() - start;
		else if(end != 0)
			duration = savedDuration + end - start;
		return duration;
	}

	@Override
	public float getProgress() {
		if(startConstCount == 0)
			return 0;
		if(gpuDone || (restored && isIdle()) || start == 0)
			return progress;
		if(getDuration() == 0 || progressMem == null)
			return ((float) savedSolvedConstellations) / startConstCount;		// either has a value, is still 0 or is 0 because of reset
		
		int solvedConstellations = savedSolvedConstellations;
		synchronized(progressLock) {
			clEnqueueReadBuffer(memqueue, progressMem, true, 0, progressBuf, null, null);
			for(int i = 0; i < globalWorkSize; i++) {
				if(progressBuf.get(i) == 1) {
					solvedConstellations++;
				}
			}
			progress = ((float) solvedConstellations) / startConstCount;
		}
		
		return progress;
	}

	@Override
	public long getSolutions() {
		if(gpuDone)
			return solutions;
		if(resMem == null || start == 0)
			return savedSolutions;		// either has a value, is still 0 or is 0 because of reset
		
		long solutions = 0;
		synchronized(resLock) {
			clEnqueueReadBuffer(memqueue, resMem, true, 0, resBuf, null, null);
			for(int i = 0; i < globalWorkSize; i++) {
				solutions += resBuf.getLong(i*8) * symList.get(i);
			}
			this.solutions = solutions;
		}
		return savedSolutions + solutions;
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
		String options = "-D N="+N + " -D BLOCK_SIZE="+WORKGROUP_SIZE + " -cl-mad-enable";
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

	private void transferDataToDevice(MemoryStack stack) {
		if(savedDuration == 0) {		// if duration is 0, then restore() was not called
			generator = new GpuConstellationsGenerator();
			generator.genConstellations(N, WORKGROUP_SIZE, PRE_QUEENS);
			
			ldList = generator.ldList;
			rdList = generator.rdList;
			colList = generator.colList;
			startjklList = generator.startjklList;
			symList = generator.symList;

			startConstCount = generator.startConstCount;
		}
		globalWorkSize = ldList.size();

		// OpenCL-Memory Objects to be passed to the kernel
		// ld
		ldMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer paramPtr = clEnqueueMapBuffer(memqueue, ldMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, ldList.get(i));
		}
		clEnqueueUnmapMemObject(memqueue, ldMem, paramPtr, null, null);
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);
		
		// rd
		rdMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		paramPtr = clEnqueueMapBuffer(memqueue, rdMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, rdList.get(i));
		}
		clEnqueueUnmapMemObject(memqueue, rdMem, paramPtr, null, null);
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);
		
		// col
		colMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		paramPtr = clEnqueueMapBuffer(memqueue, colMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, colList.get(i));
		}
		clEnqueueUnmapMemObject(memqueue, colMem, paramPtr, null, null);
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);
		
		// startjkl
		startjklMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		paramPtr = clEnqueueMapBuffer(memqueue, startjklMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, startjklList.get(i));
		}
		clEnqueueUnmapMemObject(memqueue, startjklMem, paramPtr, null, null);
//		clEnqueueWriteBuffer(memqueue, context, true, 0, paramPtr, null, null);

		// result memory
		resMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*8, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer resWritePtr = clEnqueueMapBuffer(memqueue, resMem, true, CL_MAP_WRITE, 0, globalWorkSize*8, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			resWritePtr.putLong(i*8, 0);
		}
		clEnqueueUnmapMemObject(memqueue, resMem, resWritePtr, null, null);
//		clEnqueueWriteBuffer(memqueue, context, true, 0, resWritePtr, null, null);
		resBuf = BufferUtils.createByteBuffer(globalWorkSize*8);

		// progress indicator memory
		progressMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errBuf);
		checkCLError(errBuf.get(0));
		ByteBuffer progressWritePtr = clEnqueueMapBuffer(memqueue, progressMem, true, CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errBuf, null);
		checkCLError(errBuf.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			progressWritePtr.putInt(i*4, 0);
		}
		clEnqueueUnmapMemObject(memqueue, progressMem, progressWritePtr, null, null);
//		clEnqueueWriteBuffer(memqueue, context, true, 0, progressWritePtr, null, null);
		progressBuf = BufferUtils.createIntBuffer(globalWorkSize);
		
		clFlush(memqueue);

		// set kernel parameters
		// ld
		LongBuffer ldArg = BufferUtils.createLongBuffer(1);
		ldArg.put(0, ldMem);
		clSetKernelArg(kernel, 0, ldArg);
		// rd
		LongBuffer rdArg = BufferUtils.createLongBuffer(1);
		rdArg.put(0, rdMem);
		clSetKernelArg(kernel, 1, rdArg);
		// col
		LongBuffer colArg = BufferUtils.createLongBuffer(1);
		colArg.put(0, colMem);
		clSetKernelArg(kernel, 2, colArg);
		// startjkl
		LongBuffer startjklArg = BufferUtils.createLongBuffer(1);
		startjklArg.put(0, startjklMem);
		clSetKernelArg(kernel, 3, startjklArg);
		// res
		LongBuffer resArg = BufferUtils.createLongBuffer(1);
		resArg.put(0, resMem);
		clSetKernelArg(kernel, 4, resArg);
		// progress
		LongBuffer progressArg = BufferUtils.createLongBuffer(1);
		progressArg.put(0, progressMem);
		clSetKernelArg(kernel, 5, progressArg);
	}
	
	private void explosionBoost9000(MemoryStack stack) {
		// create buffer of pointers defining the multi-dimensional size of the number of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkers = BufferUtils.createPointerBuffer(dimensions);
		globalWorkers.put(0, globalWorkSize);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, WORKGROUP_SIZE);

		// wait for all memory operations to finish
		clFinish(memqueue);
		
		// run kernel and profile time
		final PointerBuffer xEventBuf = BufferUtils.createPointerBuffer(1);		// buffer for event that is used for measuring the execution time
		clEnqueueNDRangeKernel(xqueue, kernel, dimensions, null, globalWorkers, localWorkSize, null, xEventBuf);
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
        
		// wait for the gpu computation to finish
		clFinish(xqueue);

		start = startBuf.get(0);
		end = endBuf.get(0);
		
		// indicator for getProgress() and getSolutions() to not read from gpu memory any more, but just return the variable
		gpuDone = true;
	}
	
	private void readResults(MemoryStack stack) {
		// read result and progress memory buffers
		synchronized(resLock) {
			synchronized(progressLock) {
				clEnqueueReadBuffer(memqueue, resMem, true, 0, resBuf, null, null);
				clEnqueueReadBuffer(memqueue, progressMem, true, 0, progressBuf, null, null);
				solutions = savedSolutions;
				int solvedConstellations = savedSolvedConstellations;
				for(int i = 0; i < globalWorkSize; i++) {
					if(progressBuf.get(i) == 1) {
						solutions += resBuf.getLong(i*8) *  symList.get(i);
						solvedConstellations++;
					}
				}
				progress = ((float) solvedConstellations) / startConstCount;
			}
		}
	}
	
	private void terminate() {
		// release all CL-objects
		clReleaseEvent(clEvent);
		//eventCB.free();
		clReleaseCommandQueue(xqueue);
		clReleaseCommandQueue(memqueue);
		clReleaseKernel(kernel);
		clReleaseProgram(program);
		
		clReleaseMemObject(ldMem);
		clReleaseMemObject(rdMem);
		clReleaseMemObject(colMem);
		clReleaseMemObject(startjklMem);
		clReleaseMemObject(resMem);
		clReleaseMemObject(progressMem);
		
		int errcode = clReleaseContext(context);
        checkCLError(errcode);
		contextCB.free();
	}
	
	// detect operating system and use corresponding native library file if available
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
			InputStream in = GpuSolver.class.getClassLoader().getResourceAsStream("de/nqueensfaf/res/clinfo/clinfo.exe");
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
			throw new IllegalStateException("No OpenCL-capable device was found. GpuSolver is not available.");
		}
		if(idx < 0 || idx >= devices.size()) {
			throw new IllegalArgumentException("Invalid index value: " + idx + " (size:" + devices.size() + ")");
		}
		device = devices.get(idx);
		platform = platformByDevice.get(device);
	}

	private String getKernelSourceAsString(String filepath) {
		String resultString = null;
		try (
			InputStream clSourceFile = GpuSolver.class.getClassLoader().getResourceAsStream(filepath);
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

	// getters and setters
	
	// returns the total number of enqueued work-items 
	public int getGlobalWorkSize() {
		return globalWorkSize;
	}
	
	// gets WORKGROUP_SIZE
	public int getWorkgroupSize() {
		return WORKGROUP_SIZE;
	}

	// sets WORKGROUP_SIZE
	public void setWorkgroupSize(int s) {
		if(device == 0) {
			throw new IllegalStateException("Choose a device first");
		}
		long maxWorkgroupSize = getDeviceInfoPointer(device, CL_DEVICE_MAX_WORK_GROUP_SIZE);
		if(s <= 0 || s > maxWorkgroupSize) {
			throw new IllegalArgumentException("WorkgroupSize must be between 0 and " + maxWorkgroupSize + " (=max for this device)");
		}
		WORKGROUP_SIZE = s;
	}

	// gets PRE_QUEENS
	public int getNumberOfPresetQueens() {
		return PRE_QUEENS;
	}
	
	// sets PRE_QUEENS
	public void setNumberOfPresetQueens(int pq) {
		if(pq < 4 || pq > 10) {
			throw new IllegalArgumentException("Number of preset queens must be between 4 and 10");
		}
		PRE_QUEENS = pq;
	}
	
	// record class for saving and restoring
	private record RestorationInformation(int N, long duration, long solutions, int startConstCount, 
			ArrayList<Integer> ldList, ArrayList<Integer> rdList, ArrayList<Integer> colList, ArrayList<Integer> startjklList, ArrayList<Integer> symList) implements Serializable {
		RestorationInformation(int N, long duration, long solutions, int startConstCount, 
				ArrayList<Integer> ldList, ArrayList<Integer> rdList, ArrayList<Integer> colList, ArrayList<Integer> startjklList, ArrayList<Integer> symList) {
			this.N = N;
			this.duration = duration;
			this.solutions = solutions;
			this.startConstCount = startConstCount;
			this.ldList = ldList;
			this.rdList = rdList;
			this.colList = colList;
			this.startjklList = startjklList;
			this.symList = symList;
		}
	}
}
