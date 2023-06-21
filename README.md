# NQueensFAF
Insanely fast Solvers for the n queens problem, one for GPUs (definitely try this one) and one for CPUs. Also provides useful utilities for implementing custom N queens problem solving algorithms. <br>
Comes with a command line interface.<br>
Uses Java 17.

### Download
[Latest Release](https://github.com/olepoeschl/NQueensFAF/releases/latest)<br>
[Latest Nightly Build](https://github.com/olepoeschl/NQueensFAF/releases/tag/nightly)<br>
For more releases visit the "Releases" section. <br>

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
|       RX 6650 XT      |  0.28s  |   2.00s   |    16.60s    |     2:13m    |     19:14m   |  3:03h  |  not measured  | not measured |

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

# Usage
## Try the command line interface
_Documentation is in progress._

## Use it in your project
_Documentation is in progress._

## Implement your own algorithm
The abstract class Solver provides a good structure and handy features for your own N Queens Problem solution algorithm. Just extend it and fill the abstract methods with your code.
<br>The method names are self explanatory.

# General

This solution is based on three ideas, especially the first two:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://github.com/preusser/q27">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

- GPU: remember board-leaving diagonals when going to the next row, so that they can be reinserted when we go backwards. This has also been done in Ping Che Chen's implementation (https://forum.beyond3d.com/threads/n-queen-solver-for-opencl.47785/) of the N Queens Problem for GPU's. 

The GPU solver does support NVIDIA, AMD and also integrated Intel GPU's.
For NDIDIA and AMD GPU's we recommend a workgroupsize of 64, for the integrated intel graphics we recommend 24 for optimal performance. 

# Contact
If you have a comment, question, idea or whatever, we will be happy to answer!
Mail: olepoeschl.developing@gmail.com
