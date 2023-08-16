
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

## 1.5.0 (latest):
      - implemented a beautiful command line interface (see Releases page)
      - now it's possible to use multiple GPUs at the same time
      - easily configure, how the workload should be distributed to all used GPUs
      - configuration made simple by using a json file or creating a Config-Object in code
      - storing and restoring now much cleaner implemented, also using json format

## 1.4.0:
      - GPU speed up of approximately 35% 
      - prepared the CPU Solver for the distributed version (works now for board size up to 31 again)
      - saving and restoring is now 10 times faster 
      - number of preset queens is now configurable 
      - now also showing the passed time in the command line version 
      - converted to Maven project
## 1.3.0:
      - implemented SymSolver for finding solutions that are symmetric with respect to 90 or 180 degree rotation 
      - enable counting of unique solutions 
      - extend capabilities of the command line version, for example auto saves, unique solution counter and config file 
## 1.2.0:
      - migrated from LWJGL 2 to LWJGL 3 -> much less overhead when starting the GpuSolver
      - for low board sizes, noticeably faster times
## 1.1.1:
      - command line support
## 1.1.0:
      - big performance improvement in the GPU-Solver (approximately 30%)
      
      - swapped j with k in GpuConstellationsGenerator
      
      - group constellations by j, putting them into the same OpenCL workgroup
      
      - reduced overhead of starting the GPU-Solver by using a better method of filling the workgroups with "pseudo" constellations
## 1.0.1:
      - the OpenCL workgroup size used by the GpuSolver is now editable
      
      - some small changes to the Gpu Solver with little improvement
      
      - some new Gui features
## 1.0.0:
      - splitted into the Gui program (NQueensFAF-GUI) and the NQueensFAF library (this repo) for the computation part 
      
      - the GPU solver now rounds the global work size up to the next matching number of constellations 
        and solves all constellations using GPU instead of solving remaining constellations using CPU
        
      - code (especially of the Gui class) is much cleaner now
## 0.10:
      - included support for GPU's using OpenCL through lwjgl
      
      - insanely fast thanks to optimized parallel programming
      
      - realtime progress updates using OpenCL read operations
## 0.9:
      - big performance improvement (~35%)

      - implemented case distinction for the different start Constellations in order to get rid of arrays
      
      - now only 0 - 3 class variables (int) per start constellation instead of int[N-3] plus 5 int
      
      - currently only works for board sizes up to N=23, this will be updated soon
      
      - N = 16 in ~1.35 sec --> broke the 2-second-Barrier!
## 0.8:
      - better handling of the different start constellation cases
      
      - optimization of the recursion functions
      
      - many further little optimizations

      - N = 16 in ~2.1 sec
## 0.7:
      - many minor changes to reduce cache misses and also the use of memory
      
      - ability to save progress and continue later
      
      - use newest java jdk 15
      
      - N = 16 in ~2.5 sec
## 0.6:
      - bit representation of integers for modelling the board
      
      - rest of the program stays the same
      
      - has a gui now
      
      - N = 16 in ~ 4 sec
## 0.5:
      - better use of symmetry of solutions by using starting constellations
      
      - set Queens on the outer rows and cols
      
      - multithreading by distributing the starting positions to the threads
      
      - N = 16 in ~ 1 min     
## 0.4:
      - multithreading by setting Queen in the first row on different places
## 0.3:
      - represent the board with diagonals and cols 
      
      - N = 16 in ~2-3 min
## 0.2:
      - reduce to half by only going to the half of the first row
      
      - N = 16 in ~ 5 min
## 0.1: 
      - board as NxN-boolean
      
      - occupy each square individually
      
      - single threading only
      
      - N = 16 in ~ 10 minutes
      
