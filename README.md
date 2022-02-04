# NQueensFAF library
A Java library for easily implementing custom solvers (algorithms) for the N Queens problem. Contains a super fast Solver for CPU using pure Java and for GPU using OpenCL. The built in Solvers allow you to save their state for continuing the execution at some time later.
<br>Initially created for [NQueensFAF](https://github.com/olepoeschl/NQueensFAF). For more information about the built in Solvers, have a look at [NQueensFAF](https://github.com/olepoeschl/NQueensFAF).

### Download
See the "Releases" section.

# Getting Started
The library uses Java 17.
<br>As stated below, this project depends on [LWJGL 3](http://www.lwjgl.org/), so make sure you include it in your project setup as well as its native jars.

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
cpuSolver.setTimeUpdateDelay(50).setProgressUpdateDelay(50);
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
gpuSolver.setTimeUpdateDelay(69).setProgressUpdateDelay(420);
gpuSolver.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("Progress: " + progress + " (" + solutions + " solutions)"));
gpuSolver.addTerminationCallback(() -> System.out.println("GPU finished after " + gpuSolver.getDuration() + " ms!"));
gpuSolver.solve();
```


# Dependencies
  The library uses [LWJGL 3](http://www.lwjgl.org/) to enable the use of OpenCL for the GPU solver.
