![14x14-chessboard-logo](https://github.com/user-attachments/assets/c678aff5-babf-47ce-bc31-73cf13895870)
<h1></h1>

NQueensFAF (**F**ast **A**nd **F**un) is an independent research project dedicated to the development of highly
optimized or generally new algorithms for solving the N-Queens problem. It is platform independent and the included
solvers can be tested through the GUI or CLI of the demo application or embedded in your project (
🔗[Installation](#installation)).
Built with Java 24.

Included:

* a solver for CPUs using Java Threads, supports multi-threading
* a solver for GPUs using OpenCL, supports automated distribution among multiple GPUs (multi-GPU)
* a simple, recursive, single-threaded solver for comparison purposes.

Currently work in progress:

* a completely new solving method with outstanding performance and excellent scaling (🔗[News](#news))
* a server-client system for distributed computing on heterogeneous GPUs and CPUs (
  🔗[Distributed Computing](#distributed-computing))

# Features

| Description                                                                          | GUI | CLI |
|:-------------------------------------------------------------------------------------|:---:|:---:|
| adjust the number of CPU threads                                                     |  ✓  |  ✓  |
| adjust the number of pre-placed queens                                               |  ✓  |  ✓  |
| view, select and configure available GPUs                                            |  ✓  |  ✓  |
| automatically determine the weight of all available GPUs<br>(for multi-GPU)          |  ✓  |     |
| save&restore the progress of a solver run (includes auto-save)                       |  ✓  |  ✓  |
| save/load solver configurations to/from files                                        |  ✓  |     |
| see a history of all finished runs during the current session                        |  ✓  |     |
| see the records for a certain N <br>(record = shortest duration for finishing a run) |  ✓  |     |
| copy the solver configuration of a record or history entry                           |  ✓  |     |

__Note:__ While the GPU-Solver is tested successfully for a range of NVIDIA and Intel Integrated GPUs, it remains not
working on AMD GPUs and untested for Intel Arc GPUs. If you happen to have an Intel Arc GPU, feel free to test it and
let us know if it worked or not :)

# Benchmarks

During the time we have spent developing NQueensFAF, we have been able to
continuously expand our available hardware. Especially the newer graphics cards
show the potential of our program.

<b>GPUs</b>
| Board size N | 18 | 19 | 20 | 21 | 22 | 23 | 24 | 25 |
|      :----------:     |   :-:   |    :-:    |      :-:     |      :-:     |      :-:     |       :-:      |      :-:       |  :-: |
| RTX 3080 FE | 0.03s | 0.77s | 5.85s | 0:48m | 6:56m | 1:02h | 9:45h | 4d 7h |
| RTX 3060 Ti FE | 0.10s | 1.26s | 10.18s | 1:23m | 12:10m | 1:49h | 17:50h | 7d 2h |
| GTX 1650 Ti | 0.40s | 3.62s | 29.08s | 4:02m | 35:21m | not measured | not measured | not measured |
| Intel UHD 770 | 4.71s | 32.98s | 4:18m | 36:13m | not measured | not measured | not measured | not measured |
| RX 6650 XT | 0.28s | 2.00s | 16.60s | 2:13m | 19:14m | 3:03h | not measured | not measured |

<b>CPUs</b>
| Board size N | 16 | 17 | 18 | 19 | 20 | 21 | 22 |
|      :----------:     |       :-:       |    :-:    |    :-:    |    :-:    |      :-:     |      :-:     |      :-:     |
| i5 - 12600k single | 1.12s | 7.04s | 49.92s | 6:21m | 57:47m | not measured | not measured |
| i5 - 12600k multi | 0.203s | 0.79s | 4.91s | 37.1s | 4:59m | 42:20m | 6:09h |
| i5 - 9300h single | 1.32s | 8.95s | 1:05m | 8:20m | 1:10h | not measured | not measured |
| i5 - 9300h multi | 0.25s | 1.75s | 12.5s | 1:35m | 13:05m | 1:52h | 16:18h |
| Ryzen 5800X single | 0.91s | 6.09s | 44.3s | 5:38m | 45:24m | mot measured | not measured |
| Ryzen 5800X multi | 0.28s | 0.70s | 4.06s | 30.3s | 4:04m | 33:53m | not measured |

Single stands for single-threaded and multi for multi-threaded with the maximum number of threads.
The CPUs and the GPUs are used with stock settings.

__Note:__ Your graphics card may go into another power state when running the program. To check this and to avoid this,
you can use a tool such as "nvidiainfo".

# Installation

## Requirements

Java24 (or a newer version) needs to be installed on your computer.

## Demo

The demo application can be downloaded from the [Releases](https://github.com/olepoeschl/NQueensFAF/releases) page.

## Code

There are two artifacts potentially useful for external projects:

* `nqueensfaf-impl`: contains the classes representing the solvers; depends on `nqueensfaf-core`
* `nqueensfaf-core`: simplifies the implementation of a new solver algorithm and its usage

Their jar's can be downloaded from the [Releases](https://github.com/olepoeschl/NQueensFAF/releases) page to be added to
the classpath of your project.

# Usage

## GUI

The GUI is self-explanatory. If you do have a question though, feel free to ask.

## CLI

Command format:<br>
`nqueensfaf-demo (-n=<N> | -r=<path_to_save_file>) [<general_options>] <solver_command>`

If you just want to get started, take a look at the examples in the 🔗[CPU-Solver](#cpu-solver-command) section and the
🔗[GPU-Solver](#gpu-solver-command) section.

### Required Parameters

Specify exactly one of them:

* `-n=<N>` ⟶ specify the board size to start a new computation
* `-r=<path_to_save_file>` ⟶ specify the path to a save-file generated using_auto-save to continue an old computation

### General Options

* `-s=<percentage>` ⟶ auto-save percentage interval in decimal, for example -s=0.05 for auto-saving in 5% intervals
* `-u=<interval>` ⟶ update duration, solutions and progress after each \<interval\> milliseconds
* `-h` ⟶ print device specific help message

__Note:__ You must re-enable auto-saving again each time you resume from a save-file.

### CPU-Solver-Command

Command format: <br>
`cpu [-t=<threadcount>] [-p=<pre_queens>] [-h]`

* `-t=<threads>` ⟶ number of threads that should be used
* `-p=<pre_queens>` ⟶ number of pre-placed queens, default is 6. A higher number means more but smaller tasks by placing
  additional queens before starting the worker threads. Most of the time 6 is
  the best option.
* `-h` ⟶ print CPU-Solver specific help message

#### Examples

* N=16 on CPU with 1 thread<br>
  `nqueensfaf-demo -n=16 cpu`
* N=18 on CPU with 8 threads<br>
  `nqueensfaf-demo -n=18 cpu -t=8`
* N=20 with 8 threads and auto-saves in 5% steps<br>
  `nqueensfaf-demo -n=20 -s=0.05 cpu -t=8`
* continue the solution of the 20 queens problem from the save-file 20-queens.faf and auto-save in 5% steps <br>
  `nqueensfaf-demo -s=0.05 -r=./20-queens.faf cpu -t=8`

### GPU-Solver-Command

Command format: <br>
`gpu [-p=<pre_queens>] [-h]`

* `-0` ⟶ use the default GPU
* `-p=<pre_queens>` ⟶ see 🔗[CPU-Solver](#cpu-solver-command)
* `-h` ⟶ print GPU-Solver specific help message

When `-0` is not specified, the 🔗[selection of GPUs](#selecting-gpus) is done interactively when executing the command.

#### Examples

- compute N=18 on the default GPU <br>
  `nqueensfaf-demo -n=18 gpu -0`<br>
- compute N=20 and select the GPUs interactively <br>
  `nqueensfaf-demo -n=20 gpu`<br>
- compute N=20, select the GPUs interactively and do auto-saves each 10% <br>
  `nqueensfaf-demo -n=20 -s=0.1 gpu`<br>

### Selecting GPUs

When starting the GPU-Solver, you will see a list of all available GPUs provided with indices.
To select a GPU, you simply enter its index and its configuration, separated by commata. Multiple GPUs are selected one
after another.

Selection / Configuration format: <br>
`<index>[,wg=<workgroup_size>][,mw=<weight>]`

* `wg` ⟶ workgroup size on the GPU, default value 64 is best for NVIDIA GPUs. A value of 24 has proven best for
  Integrated Intel GPUs.
* `mw` ⟶ the weight, for multi-GPU, represents the portion of the workload that the respective GPU should process.
  In other words, it ideally should represent the GPU's performance relative to the other selected GPUs. Default value
  is 1.

#### Examples

* select GPU 0 (default GPU) and set its workgroup size to 128 <br>
  `0,wg=128`
* select GPUs 0 and 1, GPU 0 should use workgroup size 24 and GPU 1 should do twice the work of GPU 0 <br>
  `0,mw=1,wg=24`⏎ <br>
  `1,mw=2` <br>
* select GPUs 0, 1 and 2 and let them all contribute equally (use default weight 1) <br>
  `0`⏎ <br>
  `1`⏎ <br>
  `2`

## Code

```
CpuSolver cs = new CpuSolver();
cs.onInit(() -> System.out.println("Starting Solver for board size " + cs.getN() + "..."))
cs.onFinish(() -> System.out.println("Found " + cs.getSolutions() + " solutions in " + cs.getDuration() + " ms"))
cs.setN(16)
cs.solve();

GpuSolver gs = new GpuSolver();
List<Gpu> availableGpus = gs.getAvailableGpus();
gs.gpuSelection().choose(availableGpus.get(0));
gs.setN(18);
gs.solve();
```

For more examples, take a look at
the [examples module](https://github.com/olepoeschl/NQueensFAF/tree/readme3.0.0/nqueensfaf-examples/src/main/java/de/nqueensfaf/examples).

### Implement your own algorithm

The abstract class `AbstractSolver` provides a good structure and handy features for implementing your own solver. Just
extend it and fill the abstract methods with your code.
The documentation of `nqueensfaf-core` can help you here.

# Distributed Computing

A subproject of NQueensFAF is the development and administration of a distributed computing system in the context of the
N-Queens problem. We aim to deploy an easy-to-use client program that supports Windows and Linux as well as macOS, while
also keeping the setup-process to a minimum, so that anybody with a computer can contribute.

The goals are:

1) Solve N=27 and confirm the results of the TU Dresden.
2) Solve N=28 and set the new world record.

Further updates on this are expected for summer 2026.

# News

- We are currently developing a new solver which is based on a completely new method.
  Solving N=22 on the 12600k (single-threaded) takes only 2h25min, which corresponds to a speedup factor of more than 40
  compared to the present solver.
  Additionally, the method possesses much better scaling. At N=24 the speedup factor is already 100.
  This is still a work in progress and there are lots of optimizations that have to be implemented, so lets see how far
  we can go.
  The new solver will be included in the repository as soon as it is finished. However, this may take some time.

## Older News

- We are excited to announce that we have successfully verified the number of solutions for the **26-Queens problem
  **. <br>
  The computation was performed using 3 GPUs (2x3070, 1x3060ti) and it took slightly more than 3 weeks to finish. <br>

# References

The CPU-Solver and the GPU-Solver are based on following concepts:

* using bits to represent the occupancy of the board; based on
  the [implementation by Jeff Somers](http://users.rcn.com/liusomers/nqueen_demo/nqueens.html)
* calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on
  the [implementation by the TU Dresden](https://github.com/preusser/q27) (
  click [here](http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html) for a very good description fo this method)
* GPU: remember board-leaving diagonals when going to the next row, so that they can be reinserted when we go backwards.
  This has also been done
  in [Ping Che Chen's implementation](https://forum.beyond3d.com/threads/n-queen-solver-for-opencl.47785/) of the N
  Queens Problem for GPU's

# Special Thanks

* Volker Berndt, for introducing us to the chess world
* Vincent Hindriksen, for mentioning NQueensFAF in
  his [blog at StreamHPC](https://streamhpc.com/blog/2023-08-10/ancient-n-queens-project/)
