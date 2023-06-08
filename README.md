# NQueensFAF
Insanely fast Solvers for the n queens problem, one for GPUs (definitely try this one) and one for CPUs. Also provides useful utilities for implementing custom N queens problem solving algorithms. <br>
Comes with a command line interface.<br>
Uses Java 17.

### Download
[Latest Release](https://github.com/olepoeschl/NQueensFAF/releases/latest)<br>
[Latest Nightly Build](https://github.com/olepoeschl/NQueensFAF/releases/tag/nightly)<br>
For more releases visit the "Releases" section. <br>

# Usage
## Implementing an own Solver
The abstract class Solver provides a good structure for your own N Queens Solver. Just extend it and fill the abstract methods with code.
But the Solver class also comes with many nice utilities, for example setting / adding callbacks for certain events. 


## Using one of the builtin Solvers
```
GpuSolver gpuSolver = new GpuSolver();
gpuSolver.setN(19);
gpuSolver.setDevice(0);
gpuSolver.setOnProgressUpdateCallback((progress, solutions) -> System.out.println("Progress: " + progress + " (" + solutions + " solutions)"));
gpuSolver.addTerminationCallback(() -> System.out.println("GPU finished after " + gpuSolver.getDuration() + " ms!"));
gpuSolver.solve();
```

# Current Benchmarks
During the time we have spent developing this program, we have been able to continuously expand our available hardware. 
Especially the newer graphics cards show the potential of our program. 

<b>GPUs</b>
|      Board size N     |   18    |     19    |      20      |      21      |      22      |       23       |       24       |   25 |
|      :----------:     |   :-:   |    :-:    |      :-:     |      :-:     |      :-:     |       :-:      |      :-:       |  :-: |
|      RTX 3080 FE      |  0.03s  |   0.77s   |     5.85s    |     0:48m    |      6:56m   |      1:02h     |      9:45h     | 4d 7h |
|     RTX 3060 Ti FE    |  0.10s  |   1.26s   |    10.18s    |     1:23m    |     12:10m   |      1:49h     |     17:50h     | 7d 2h |
|      GTX 1650 Ti      |  0.40s  |   3.62s   |    29.08s    |     4:02m    |     35:21m   |  not measured  |  not measured  | not measured |
|     Intel UHD 770     |  4.71s  |  32.98s   |     4:18m    |    36:13m    | not measured |  not measured  |  not measured  | not measured | 

<!-- |       RX 6650 XT      |  0.27s  |   1.99s   |    15.80s    |   not measured    |    not measured   |  not measured  |  not measured  | not measured | -->

<b>CPUs</b>
|      Board size N     |        16       |     17    |     18    |     19    |      20      |      21      |      22      |
|      :----------:     |       :-:       |    :-:    |    :-:    |    :-:    |      :-:     |      :-:     |      :-:     |
|  i5 - 12600k single   |      1.12s      |   7.04s   |   49.92s  |   6:21m   |    57:47m    | not measured | not measured |
|  i5 - 12600k multi    |      0.203s     |   0.79s   |   4.91s   |   37.1s   |     4:59m    |    42:20m    |     6:09h    |
|   i5 - 9300h single   |      1.32s      |   8.95s   |   1:05m   |   8:20m   |     1:10h    | not measured | not measured |
|   i5 - 9300h multi    |      0.25s      |   1.75s   |   12.5s   |   1:35m   |    13:05m    |     1:52h    |     16:18h   |

Single stands for single-threaded and multi for Multi-threaded. 
The CPU's and the GPU's are used with stock settings. 

Attention: Your graphics card may go into another power state when running the program. To check this and to avoid this, you can use a tool such as "nvidiainfo".

# General

This solution is based on three ideas, especially the first two:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://github.com/preusser/q27">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

- GPU: remember board-leaving diagonals when going to the next row, so that they can be reinserted when we go backwards. This has also been done in Ping Che Chen's implementation (https://forum.beyond3d.com/threads/n-queen-solver-for-opencl.47785/) of the N Queens Problem for GPU's. 

The GPU solver does support NVIDIA, AMD and also integrated Intel GPU's.
However, for AMD the solver currently breaks for N=23 and larger boards. 
This will hopefully be fixed soon. 
For NDIDIA and AMD GPU's we recommend a workgroupsize of 64, for the integrated intel graphics we recommend 24 for optimal performance. 

# Versions
This section shows the improvements we made from version to version related to performance.
Unless stated otherwise, the following times are referring to the i5-9300h mentioned above in *single-threaded* mode.

## Distributed (yet to come):
      - we already had a working version, but it was only for cpu and due to new ideas and requirements, we started again from zero and reprogram the whole application
     
      - currently developing a system for distributing the workloads as efficent as possible, so that CPU's and GPU's of all classes can contribute usefully

      - we aim to have a working prototype this summer
   
      - there will be a website with a ranking of all participants and the current progress of the project when it runs
      
      - the goal is to first verify the solution of the 27 queens problem and afterwards approach the 28 queens problem 
     
We are very excited!

## 1.18 (latest):
      - implemented a beautiful command line interface (see Releases page)
      - now it's possible to use multiple GPUs at the same time
      - easily configure, how the workload should be distributed to all used GPUs
      - configuration made simple by using a json file or creating a Config-Object in code
      - storing and restoring now much cleaner implemented, also using json format

## 1.17:
      - GPU speed up of approximately 35% 
      - prepared the CPU Solver for the distributed version (works now for board size up to 31 again)
      - saving and restoring is now 10 times faster 
      - number of preset queens is now configurable 
      - now also showing the passed time in the command line version 
## 1.16:
      - implemented SymSolver for finding solutions that are symmetric with respect to 90 or 180 degree rotation 
      - enable counting of unique solutions 
      - extend capabilities of the command line version, for example auto saves, unique solution counter and config file 
## 1.15:
      - migrated from LWJGL 2 to LWJGL 3 -> much less overhead when starting the GpuSolver
      - for low board sizes, noticeably faster times
## 1.14:
      - command line support
## 1.13:
      - bim performance improvement in the GPU-Solver (approximately 30%)
      
      - swapped j with k in GpuConstellationsGenerator
      
      - group constellations by j, putting them into the same OpenCL workgroup
      
      - reduced overhead of starting the GPU-Solver by using a better method of filling the workgroups with "pseudo" constellations
## 1.12:
      - the OpenCL workgroup size used by the GpuSolver is now editable
      
      - some small changes to the Gpu Solver with little improvement
      
      - some new Gui features
## 1.11:
      - splitted into the Gui program (NQueensFAF-GUI) and the NQueensFAF library (this repo) for the computation part 
      
      - the GPU solver now rounds the global work size up to the next matching number of constellations 
        and solves all constellations using GPU instead of solving remaining constellations using CPU
        
      - code (especially of the Gui class) is much cleaner now
## 1.10:
      - included support for GPU's using OpenCL through lwjgl
      
      - insanely fast thanks to optimized parallel programming
      
      - realtime progress updates using OpenCL read operations
## 1.9:
      - big performance improvement (~35%)

      - implemented case distinction for the different start Constellations in order to get rid of arrays
      
      - now only 0 - 3 class variables (int) per start constellation instead of int[N-3] plus 5 int
      
      - currently only works for board sizes up to N=23, this will be updated soon
      
      - N = 16 in ~1.35 sec --> broke the 2-second-Barrier!
## 1.8:
      - better handling of the different start constellation cases
      
      - optimization of the recursion functions
      
      - many further little optimizations

      - N = 16 in ~2.1 sec
## 1.7:
      - many minor changes to reduce cache misses and also the use of memory
      
      - ability to save progress and continue later
      
      - use newest java jdk 15
      
      - N = 16 in ~2.5 sec
## 1.6:
      - bit representation of integers for modelling the board
      
      - rest of the program stays the same
      
      - has a gui now
      
      - N = 16 in ~ 4 sec
## 1.5:
      - better use of symmetry of solutions by using starting constellations
      
      - set Queens on the outer rows and cols
      
      - multithreading by distributing the starting positions to the threads
      
      - N = 16 in ~ 1 min     
## 1.4:
      - multithreading by setting Queen in the first row on different places
## 1.3:
      - represent the board with diagonals and cols 
      
      - N = 16 in ~2-3 min
## 1.2:
      - reduce to half by only going to the half of the first row
      
      - N = 16 in ~ 5 min
## 1.1 (actually 1.0): 
      - board as NxN-boolean
      
      - occupy each square individually
      
      - single threading only
      
      - N = 16 in ~ 10 minutes
      
# Contact
We're happy about every comment, question, idea or whatever - if you have such a thought or need help running the program, you can use the issue templates, the discussion section or reach out to us directly!
Mail: olepoeschl.developing@gmail.com
