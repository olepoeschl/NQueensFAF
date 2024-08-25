# NQueensFAF
Highly optimized Solvers for the N queens problem, one for GPUs (definitely try
this one) and one for CPUs. Also provides useful utilities for implementing
custom N queens problem solving algorithms.<br>
Features are:
1) supports GPU-computing and also CPU-Multithreading<br> 
2) distribution among multiple GPUs (soon done automatically in proportion to performance)<br>
3) auto progress save and continuing from save file<br>
4) some advanced settings, see explanation of the command line usage<br>

Can run on Windows, Linux and also Mac (for download and installation see
below).<br> 
The standard version uses a command line interface, which is easy to use
(examples below). However we also linked a GUI program of an older version. <br>
Built with Java 17.<br>
__NOTE:__ currently only works for NVIDIA and integrated Intel GPUs.

# 1 Distributed Computing 
We recently started a distributed computing project for solving the N Queens
problem! The goals are: 
1) Solve N=27 and confirm the results of the TU Dresden. (currently in progress)<br>
You can check the current progress on the 27 queens problem [here](http://nqueensfaf.de:14772/progress). 
2) Solve N=28 and set the new world record.

## 1.1 How to contribute
<b> The project is currently offline due to further development! <b><br>
Although the client is working just fine, we are still developing it.<br> 
__PLEASE CHECK FOR UPDATES__ from time to time, until updates will be fetched
automatically (coming soon).<br>
Latest version: 0.0.1 (link below).
### Windows 
Download and install the [Windows Client](https://github.com/olepoeschl/NQueensFAF-GUI/releases/download/1.17/nqueens-client-0.0.1.exe).
After that, just double click the desktop icon - the rest is self
explanatory. Alternatively you may follow the same installation process as for
Linux and Mac, see below.<br> 

__NOTE:__ Your anti virus program may tell you that the program is not safe
(although it is). In this case you must manually ignore the warning or even add the
program as an exception in the anti virus software.<br>
### Linux and Mac (and also Windows)
You first have to install the [Java JDK](https://www.oracle.com/de/java/technologies/downloads/#java21) and also
OpenCL. In Windows and Mac the OpenCL Libraries should be included in the graphics
driver, in Linux you may have to install the missing libraries manually. After
that, [download the jar](https://github.com/olepoeschl/NQueensFAF-GUI/releases/download/1.17/nqueens-client-0.0.1.jar) and run it by executing the  command<br>
`java -jar nqueens-client.jar`<br>
in the directory where the jar is located. (nqueensfaf.de is the distributing server) <br>

__NOTE:__ Eventually you have to adapt the name 'nqueens-client.jar' to the actual name of the jar
you downloaded. 

# 2 Local Computing
## 2.1 Download and Installation
The following sections always refer to the most recent version of NQueensFAF. In
case you want to try out older versions, just visit the
[Releases](https://github.com/olepoeschl/NQueensFAF/releases) section. The
installation process remains the same as described below. <br>
### The simplest Way
1) Choose the 
[Latest Stable Release](https://github.com/olepoeschl/NQueensFAF/releases/latest) 
or the 
[Latest Nightly Build](https://github.com/olepoeschl/NQueensFAF/releases/tag/nightly) 
and download the zip file that fits your Operating System. 
2) Unpack the zip file and open a console in the unpacked directory. 
3) Run the application by typing `./nqueensfaf-cli -n 16` (Linux)
or `nqueensfaf-cli -n 16` (Windows).
4) Check out section __5 Usage__ for an overview of all possible
commands including examples. <br>
__NOTE:__ If you can not run the program try the command `chmod +x nqueensfaf-cli`
between steps 2 and 3. 
### The Java Way
0) First [install Java](https://www.oracle.com/de/java/technologies/downloads/#java21) for your OS. 
1) Choose the 
[Latest Stable Release](https://github.com/olepoeschl/NQueensFAF/releases/latest) 
or the 
[Latest Nightly Build](https://github.com/olepoeschl/NQueensFAF/releases/tag/nightly) 
and download the file "nqueensfaf-cli-***.jar" (NOTE the -cli suffix). 
2) open a console in the directory where the jar is located.  
3) run the jar by typing `java -jar nqueensfaf-cli.jar -n 16 cpu` (same command for
all operating systems). 
4) Check out the section [Usage]('docs/5 Usage) for an overview of all possible
commands including examples.
### A GUI Version
In case you are a Windows User and prefer a graphical user interface you
can downlad the following [Windows Installer with GUI](https://github.com/olepoeschl/NQueensFAF-GUI/releases/download/1.17/nqueensfaf.exe).<br>

# 3 News
- We are currently developing a new solver which is based on a completely new method.
  Solving N=22 on the 12600k (single-threaded) takes only 2h25min, which corresponds to a speedup factor of more than 40 compared to the present solver.
  Additionally, the method possesses much better scaling. At N=24 the speedup factor is already 100. 
  This is still a work in progress and there are lots of optimizations that have to be implemented, so lets see how far we can go.
  The new solver will be included in the repository as soon as it is finished. However, this may take some time.
  
## Older News
- The distributed computing for solving the 27 Queens problem has begun.<br>
Download the client and get started (see __1 Distributed Computing__)!  
- We are excited to announce that we have successfully verified the number of solutions for the **26-Queens problem**. <br> 
The computation was performed using 3 GPUs (2x3070, 1x3060ti) and it took slightly more than 3 weeks to finish. <br>
27 - Here we come! <br>

# 4 Current Benchmarks
During the time we have spent developing NQueensFAF, we have been able to
continuously expand our available hardware. Especially the newer graphics cards
show the potential of our program.

<b>GPUs</b>
|      Board size N     |   18    |     19    |      20      |      21      |      22      |       23       |       24       |   25 |
|      :----------:     |   :-:   |    :-:    |      :-:     |      :-:     |      :-:     |       :-:      |      :-:       |  :-: |
|      RTX 3080 FE      |  0.03s  |   0.77s   |     5.85s    |     0:48m    |      6:56m   |      1:02h     |      9:45h     | 4d 7h |
|     RTX 3060 Ti FE    |  0.10s  |   1.26s   |    10.18s    |     1:23m    |     12:10m   |      1:49h     |     17:50h     | 7d 2h |
|      GTX 1650 Ti      |  0.40s  |   3.62s   |    29.08s    |     4:02m    |     35:21m   |  not measured  |  not measured  | not measured |
|     Intel UHD 770     |  4.71s  |  32.98s   |     4:18m    |    36:13m    | not measured |  not measured  |  not measured  | not measured | 
|       RX 6650 XT      |  0.28s  |   2.00s   |    16.60s    |     2:13m    |     19:14m   |  3:03h  |  not measured  | not measured |

<b>CPUs</b>
|      Board size N     |        16       |     17    |     18    |     19    |      20      |      21      |      22      |
|      :----------:     |       :-:       |    :-:    |    :-:    |    :-:    |      :-:     |      :-:     |      :-:     |
|  i5 - 12600k single   |      1.12s      |   7.04s   |   49.92s  |   6:21m   |    57:47m    | not measured | not measured |
|  i5 - 12600k multi    |      0.203s     |   0.79s   |   4.91s   |   37.1s   |     4:59m    |    42:20m    |     6:09h    |
|   i5 - 9300h single   |      1.32s      |   8.95s   |   1:05m   |   8:20m   |     1:10h    | not measured | not measured |
|   i5 - 9300h multi    |      0.25s      |   1.75s   |   12.5s   |   1:35m   |    13:05m    |     1:52h    |     16:18h   |
|   Ryzen 5800X single  |      0.91s      |   6.09s   |   44.3s   |   5:38m   |    45:24m    | mot measured | not measured |
|   Ryzen 5800X multi   |      0.28s      |   0.70s   |   4.06s   |   30.3s   |     4:04m    |    33:53m    | not measured |

Single stands for single-threaded and multi for Multi-threaded. 
The CPUs and the GPUs are used with stock settings. 

Attention: Your graphics card may go into another power state when running the program. To check this and to avoid this, you can use a tool such as "nvidiainfo".

# 5 Usage
Show the general help message by using `nqueensfaf-cli -h`and the device
specific help messages by using either `nqueensfaf-cli -n 20 gpu -h` or
`nqueensfaf-cli -n 20 cpu -h`.<br>
If you just want to get started maybe read __5.2 Extended Explanation with Examples__ first. 
## 5.1 Compact Explanation
The command format reads as follows:<br>
`nqueensfaf-cli [-u=<update-interval>] [-s=<auto-save-interval>] (-n=<N> |
-r=<path-to-save-file>) (cpu | gpu) [<extra device options>] [-h]`
The symbol "|" means that either the first or second option (exclusively, not both) can be specified. <br> 

Explanation of the Options:
- `-s=<value>` ⟶ auto-save interval as a decimal, for example -s=0.05 for
  auto-saving in 5% intervals
- `-u=<value>` ⟶ update time and solution and progress after \<value\> milliseconds
- `-n=<N>` ⟶ substitute the board size for starting a new computation OR
- `-r=<path-to-save-file>` ⟶ path to a save-file to continue a computation from the last checkpoint, for example `./20-queens.faf`
- `cpu` | `gpu` ⟶ write cpu for choosing cpu and gpu for choosing gpu (device
  specific options see below)
- `-h`  ⟶ print device specific help message<br>
__NOTE:__ You must enable auto-saving again each time you resume from a save-file.

Device options for the CPU: `nqueensfaf [...] 20 cpu [-t=<threadcount>] [-p=<pre-queens>] [-h] `
- `-t=<value>` ⟶ use \<value\> threads<br>
- `-p=<value>` ⟶ default is 6. A higher number means more but smaller tasks by setting
  additional queens before sending to the solver device. Most of the time 6 is
  the best option.
- `-h`  ⟶ print CPU specific help message

Device options for GPUs: `nqueensfaf [...] 20 gpu [-p=<pre-queens>] [-h]` 
- `-h`  ⟶ print GPU specific help message

## 5.2 Extended Explanation with Examples
Depending on your way of installation you start the command with<br>
- `nqueensfaf-cli` (Windows)
- `./nqueensfaf-cli` (Linux and Mac)
- `java -jar nqueensfaf-cli.jar` (Java). 
Here we always use `nqueensfaf-cli`.<br>
The board size (N) and the device (cpu or gpu) must always be specified.<br>
### Explanation for CPU
- N=16 on CPU with 1 thread<br>
`nqueensfaf-cli -n=16 cpu`
- N=18 on CPU with 8 threads<br>
`nqueensfaf-cli -n=18 cpu -t=8`
- N=20 with 8 threads and auto-saves in 5% steps<br>
`nqueensfaf-cli -n=20 -s=0.05 cpu -t=8`
- continue the solution of the 20 queens problem from the save-file
20-queens.faf<br>
`nqueensfaf-cli -s=0.05 -r=./20-queens.faf cpu -t=8`
### Explanation for GPUs
- compute N=20 on the default GPU <br>
`nqueensfaf-cli -n=20 gpu`<br>

#### Selecting GPUs
When choosing GPU mode, you will see a list of all available GPUs provided with indices.
You can select which GPUs should be used by entering their indices, separated by commata.
For example: `0,1,3` if you have minimum 4 GPUs available and just don't want to use the third one.

Multiple GPU option flags can be set, separated by `:`. Possible flags are 
  - `ws` ⟶ workgroup size on the GPU, standard option 64 is best for NVIDIA GPUs.
  Only set it to 24 for integrated Intel GPUs. (also automatically set)
  - `bm` ⟶ represents the benchmark and is required, but only takes effect if multiple GPUs are used, each one with its own benchmark score.
  A lower score shifts more work towards a GPU.<br>
  __NOTE:__ A good way to choose the `bm` value is to solve the same board size with all wanted GPUs
            and use the rounded time as the benchmark value.<br>

Some Examples:
- For GPU with index 0 (default GPU) with the workgroup size 128, use<br>
`0:ws128`
- In case you have 3 GPUs and all should contribute equally, use<br>
`0,1,2` 
- In case you have 2 GPUs with different performance and the one with index 0 should get twice as much
work as the other one, use<br> 
  `0:bm1, 1:bm2`

## 5.3 Java usage
```
CpuSolver cs = new CpuSolver();
cs.onInit(() -> System.out.println("Starting Solver for board size " + cs.getN() + "..."))
cs.onFinish(() -> System.out.println("Found " + cs.getSolutions() + " solutions in " + cs.getDuration() + " ms"))
cs.setN(16)
cs.solve();

GpuSolver gs = new GpuSolver();
List<Gpu> availableGpus = gs.getAvailableGpus();
gs.gpuSelection().add(availableGpus.get(0).getId());
gs.setN(18);
gs.solve();
```

## 5.4 Implement your own algorithm
The abstract class Solver provides a good structure and handy features for your own N Queens Problem solution algorithm. Just extend it and fill the abstract methods with your code.
<br>The method names are self explanatory.

# 6 References

This solution is based on three ideas, especially the first two:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://github.com/preusser/q27">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

- GPU: remember board-leaving diagonals when going to the next row, so that they can be reinserted when we go backwards. This has also been done in Ping Che Chen's implementation (https://forum.beyond3d.com/threads/n-queen-solver-for-opencl.47785/) of the N Queens Problem for GPU's. 

# 7 Contact
If you have a comment, question, idea or whatever, we will be happy to answer!
Mail: olepoeschl.developing@gmail.com
