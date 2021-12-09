# NQueensFAF library
A Java library for easily implementing custom solvers (algorithms) for the N Queens problem. Contains a super fast Solver for CPU using pure Java and for GPU using OpenCL. The built in Solvers allow you to save their state for continuing the execution at some time later.
<br>Initially created for [NQueensFAF](https://github.com/olepoeschl/NQueensFAF). For more information about the built in Solvers, have a look at [NQueensFAF](https://github.com/olepoeschl/NQueensFAF).

### Download
Download the latest .jar of NQueensFAF library [here](https://github.com/olepoeschl/NQueensFAF-Library/releases/download/v1.2/NQueensFAF.library.jar) (see latest release).

# Getting Started
The library uses Java 16.
<br>As stated below, this project depends on [LWJGL 2](http://legacy.lwjgl.org/), so make sure you include it in your project setup. NQueensFAF library comes with the native dynamic library files of LWJGL2  and automatically extracts the correct file depending on your operating system, so all you need is the lwjgl.jar from LWJGL 2. It also tries to automatically clear the extracted file at the end of the Java program, but that may sometimes not work when the JVM crashes or similar things happen. That's done using a self deleting script, some anti virus programs may detect it as a virus. It's not. You can [have a look into the scripts](https://github.com/olepoeschl/NQueensFAF-Library/tree/master/src/de/nqueensfaf/res) yourself, they are very short.
<br>Okay that's it for getting started.

# Usage
## Implementing an own Solver
The abstract class Solver provides a good structure for your own N Queens Solver. Just extend it and fill the abstract methods with code.
But the Solver class also comes with many nice utilities like setting / adding callbacks for certain events and a method for running your Solver asynchronous.
<br>Have a look into the Javadocs and just try it out.

## Using a Solver
To use for example the built in CpuSolver, do it like in the following code snippet:
```
CpuSolver cpuSolver = new CpuSolver();
cpuSolver.setN(16);
cpuSolver.setThreadcount(2);
cpuSolver.setTimeUpdateDelay(50).setProgressUpdateDelay(50);
cpuSolver.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("Progress: " + progress + " (" + solutions + " solutions)"));
cpuSolver.addTerminationCallback(() -> System.out.println("Solver done after " + cpuSolver.getDuration() + " ms!"));
cpuSolver.solve();
```
Using the GpuSolver or your own Solver works just analogue.


# Dependencies
  The library uses [LWJGL 2](http://legacy.lwjgl.org/) to enable the use of OpenCL for the GPU solver.
