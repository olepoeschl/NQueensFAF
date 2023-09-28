# NQueensFAF
Insanely fast Solvers for the n queens problem, one for GPUs (definitely try this one) and one for CPUs. Also provides useful utilities for implementing custom N queens problem solving algorithms. <br>
Comes with a command line interface.<br>
Built with Java 17.

### Download
[Latest Release](https://github.com/olepoeschl/NQueensFAF/releases/latest)<br>
[Latest Nightly Build](https://github.com/olepoeschl/NQueensFAF/releases/tag/nightly)<br>
For more releases visit the "Releases" section. <br>

# News
We are excited to announce that we have succesfully verified the number of solutions for the **26-Queens problem**. <br> 
The computation was performed using 3 GPUs (2x3070, 1x3060ti) and it took slightly more than 3 weeks to finish. <br>
27 - Here we come! <br>

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

<b> This section applies to version 2.0.0 and does currently work for the nightly release, not for the 1.5.0 release! </b>

## command line interface
```
Usage: nqueensfaf [-dgh] [-c=FILE] [-N=INT] [-t=FILE]
  -c, --config=FILE      absolute path to the configuration file
  -d, --show-devices     show a list of all available OpenCL devices
  -g, --gpu              execute on GPU('s)
  -h, --help             show this help message
  -N, --board-size=INT   board size
  -t, --task=FILE        absolute path to the file containing the task
```
For example, to run board size 18 on your default GPU with default settings, you would execute:
`nqueensfaf -N 18 -g`

### config files
With `-c` you can pass a config file. Config files are written in json format and may have attributes from the following table.<br>
<table>
  <thead>
    <tr>
      <th>name</th>
      <th>type</th>
      <th>allowed values</th>
      <th>default value</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td colspan="4" align="center"><i>General configs</i></td>
    </tr>
    <tr>
      <td>updateInterval</td>
      <td>int</td>
      <td>>0, =0 (no progress updates) </td>
      <td>128</td>
    </tr>
    <tr>
      <td>autoSaveEnabled</td>
      <td>boolean</td>
      <td>true, false</td>
      <td>false</td>
    </tr>
    <tr>
      <td>autoDeleteEnabled</td>
      <td>boolean</td>
      <td>true, false</td>
      <td>false</td>
    </tr>
    <tr>
      <td>autoSavePercentageStep</td>
      <td>float</td>
      <td>>0 && <100</td>
      <td>10</td>
    </tr>
    <tr>
      <td>autoSavePath</td>
      <td>String</td>
      <td>valid file system path</td>
      <td>nqueensfaf{N}.dat</td>
    </tr>
    <tr>
      <td colspan="4" align="center"><i>CPU configs</i></td>
    </tr>
    <tr>
      <td>threadcount</td>
      <td>int</td>
      <td>>0</td>
      <td>1</td>
    </tr>
    <tr>
      <td>presetQueens</td>
      <td>int</td>
      <td>>=4</td>
      <td>4</td>
    </tr>
    <tr>
      <td colspan="4" align="center"><i>GPU configs</i></td>
    </tr>
    <tr>
      <td>deviceConfigs</td>
      <td>deviceConfig[]</td>
      <td>see deviceConfig section</td>
      <td>[{0, 64, 1, 0}]</td>
    </tr>
    <tr>
      <td>presetQueens</td>
      <td>int</td>
      <td>>=4</td>
      <td>6</td>
    </tr>
    <tr>
      <td colspan="4" align="center"><i>deviceConfig attributes</i></td>
    </tr>
    <tr>
      <td>index</td>
      <td>int</td>
      <td>>=0</td>
      <td>-</td>
    </tr>
    <tr>
      <td>workgroupSize</td>
      <td>int</td>
      <td>>0</td>
      <td>64</td>
    </tr>
    <tr>
      <td>weight</td>
      <td>int</td>
      <td>>0, =0 (device disabled) </td>
      <td>-</td>
    </tr>
    <tr>
      <td>maxGlobalWorkSize</td>
      <td>int</td>
      <td>>=workgroupSize, =0 (unlimited global work size) </td>
      <td>0</td>
    </tr>
  </tbody>
</table>

Execute `nqueensfaf -d` to see your available devices and their respective indexes.

An example config file would look something like this:
```
{
"updateInterval": 200,
"deviceConfigs": [
    {"index": 0, "weight": 1, "workgroupSize": 64}
]
}
```

## Java usage
```
CPUSolver s = new CPUSolver()
      .config(config -> {
            config.threadcount = 1;
            config.updateInterval = 200;
      })
      .onInit(self -> System.out.println("Starting Solver for board size " + self.getN() + "..."))
      .onUpdate((self, progress, solutions, duration) -> System.out.println("progress: " + progress + " solutions: " + solutions + " duration: " + duration))
      .onFinish(self -> System.out.println("Found " + self.getSolutions() + " solutions in " + self.getDuration() + " ms"))
      .setN(16)
      .solve();
```

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
