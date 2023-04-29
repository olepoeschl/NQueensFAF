# NQueensFAF
A Java framework containing an insanely fast Solver for CPU using pure Java and for GPU using OpenCL. Also provides useful utilities for implementing custom n queens problem solving algorithms.
This proces uses Java 17. <br>

### Download
See the "Releases" section.

# Documentation
Only the core package `de.nqueensfaf` is documented.
<br>You can access the javadocs [here](https://olepoeschl.github.io/NQueensFAF-Library/).

# Usage
## Implementing an own Solver
The abstract class Solver provides a good structure for your own N Queens Solver. Just extend it and fill the abstract methods with code.
But the Solver class also comes with many nice utilities like setting / adding callbacks for certain events and a method for running your Solver asynchronous.
<br>Have a look into the Javadocs and just try it out.

## Using the CpuSolver
To use for example the built in CpuSolver, do it like in the following code snippet:
```
CpuSolver cpuSolver = new CpuSolver();
cpuSolver.setN(16);
cpuSolver.setThreadcount(2);
cpuSolver.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("Progress: " + progress + " (" + solutions + " solutions)"));
cpuSolver.addTerminationCallback(() -> System.out.println("CPU finished after " + cpuSolver.getDuration() + " ms!"));
cpuSolver.solve();
```
## Using the GpuSolver
To use for example the built in CpuSolver, do it like in the following code snippet:
```
GpuSolver gpuSolver = new GpuSolver();
gpuSolver.setN(19);
gpuSolver.setDevice(0);
gpuSolver.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("Progress: " + progress + " (" + solutions + " solutions)"));
gpuSolver.addTerminationCallback(() -> System.out.println("GPU finished after " + gpuSolver.getDuration() + " ms!"));
gpuSolver.solve();
```


# Dependencies
  The library uses [LWJGL 3](http://www.lwjgl.org/) to enable the use of OpenCL for the GPU solver.
