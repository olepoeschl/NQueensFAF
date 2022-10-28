package de.nqueensfaf.compute;

import java.util.ArrayDeque;

class CpuSolverThread extends Thread {

	private final int N, N3, N4, L3, L4;			// boardsize
	private long tempcounter = 0, solvecounter = 0;			// tempcounter is #(unique solutions) of current start constellation, solvecounter is #(all solutions)
	private int done = 0;						// #(done start constellations)

	private int mark1, mark2, endmark, jmark;
 
	// list of uncalculated starting positions, their indices
	private ArrayDeque<Integer> startConstellations, ldList, rdList, colList, startQueensIjklList;
	
	// for pausing or cancelling the run
	private boolean cancel = false, running = false;
	private int pause = 0;
	private CpuSolver caller;

	CpuSolverThread(CpuSolver caller, int N, ArrayDeque<Integer> startConstellations, ArrayDeque<Integer> ldList, 
			ArrayDeque<Integer> rdList, ArrayDeque<Integer> colList, ArrayDeque<Integer> startQueensIjklList) {
		this.caller = caller;
		this.N = N;
		N3 = N - 3;
		N4 = N - 4;
		L3 = 1 << N3;
		L4 = 1 << N4;
		this.startConstellations = startConstellations;
		this.ldList = ldList;
		this.rdList = rdList;
		this.colList = colList;
		this.startQueensIjklList = startQueensIjklList;
	}

	// Recursive functions for Placing the Queens

	// for N-1-j = 0
	private void SQd0B(int ld, int rd, int col, int row, int free) {
		if(row == endmark) {
			tempcounter++;
			return;
		}

		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			int next_ld = ((ld|bit)<<1);
			int next_rd = ((rd|bit)>>1);
			int next_col = (col|bit);
			nextfree = ~(next_ld | next_rd | next_col);
			if(nextfree > 0)
				if(row < endmark-1) {
					if(~((next_ld<<1) | (next_rd>>1) | (next_col)) > 0)
						SQd0B(next_ld, next_rd, next_col, row+1, nextfree);
				} else {
					SQd0B(next_ld, next_rd, next_col, row+1, nextfree);
				}
		}
	}

	private void SQd0BkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | L3);
				if(nextfree > 0)
					SQd0B((ld|bit)<<2, ((rd|bit)>>2) | L3, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd0BkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	// for N-1-j = 1
	private void SQd1BklB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | 1 | L4);
				if(nextfree > 0)
					SQd1B(((ld|bit)<<3) | 1, ((rd|bit)>>3) | L4, col|bit, row+3, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd1BklB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd1B(int ld, int rd, int col, int row, int free) {
		if(row == endmark) {
			tempcounter++;
			return;
		}

		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			int next_ld = ((ld|bit)<<1);
			int next_rd = ((rd|bit)>>1);
			int next_col = (col|bit);
			nextfree = ~(next_ld | next_rd | next_col);
			if(nextfree > 0)
				if(row+1 < endmark) {
					if(~((next_ld<<1) | (next_rd>>1) | (next_col)) > 0)
						SQd1B(next_ld, next_rd, next_col, row+1, nextfree);
				} else {
					SQd1B(next_ld, next_rd, next_col, row+1, nextfree);
				}
		}
	}

	private void SQd1BkBlB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | L3);
				if(nextfree > 0)
					SQd1BlB(((ld|bit)<<2), ((rd|bit)>>2) | L3, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd1BkBlB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd1BlB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				
				int next_ld = ((ld|bit)<<2) | 1;
				int next_rd = ((rd|bit)>>2);
				int next_col = (col|bit);
				nextfree = ~(next_ld | next_rd | next_col);
				if(nextfree > 0)
					if(row+2 < endmark) {
						if(~((next_ld<<1) | (next_rd>>1) | (next_col)) > 0)
							SQd1B(next_ld, next_rd, next_col, row+2, nextfree);
					} else {
						SQd1B(next_ld, next_rd, next_col, row+2, nextfree);
					}
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd1BlB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd1BlkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | 2 | L3);
				if(nextfree > 0)
					SQd1B(((ld|bit)<<3) | 2, ((rd|bit)>>3) | L3, col|bit, row+3, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd1BlkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd1BlBkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1);
				if(nextfree > 0)
					SQd1BkB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd1BlBkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd1BkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | L3);
				if(nextfree > 0)
					SQd1B(((ld|bit)<<2), ((rd|bit)>>2) | L3, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd1BkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	// all following SQ functions for N-1-j > 2
	private void SQBkBlBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | (1 << (N3)));
				if(nextfree > 0)
					SQBlBjrB(((ld|bit)<<2), ((rd|bit)>>2) | (1 << (N3)), col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBkBlBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBlBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1);
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBlBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == jmark) {
			free &= (~1);
			ld |= 1;
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
				if(nextfree > 0)
					SQB(((ld|bit)<<1), (rd|bit)>>1, col|bit, row+1, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQB(int ld, int rd, int col, int row, int free) {
		if(row == endmark) {
			tempcounter++;
			return;
		}

		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			int next_ld = ((ld|bit)<<1);
			int next_rd = ((rd|bit)>>1);
			int next_col = (col|bit);
			nextfree = ~(next_ld | next_rd | next_col);
			if(nextfree > 0)
				if(row < endmark-1) {
					if(~((next_ld<<1) | (next_rd>>1) | (next_col)) > 0)
						SQB(next_ld, next_rd, next_col, row+1, nextfree);
				} else {
					SQB(next_ld, next_rd, next_col, row+1, nextfree);
				}
		}
	}

	private void SQBlBkBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1);
				if(nextfree > 0)
					SQBkBjrB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBlBkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBkBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | L3);
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<2), ((rd|bit)>>2) | L3, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBklBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | L4 | 1);
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<3) | 1, ((rd|bit)>>3) | L4, col|bit, row+3, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBklBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBlkBjrB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | L3 | 2);
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<3) | 2, ((rd|bit)>>3) | L3, col|bit, row+3, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBlkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	// for N-1-j = 2
	private void SQd2BlkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | L3 | 2);
				if(nextfree > 0)
					SQd2B(((ld|bit)<<3) | 2, ((rd|bit)>>3) | L3, col|bit, row+3, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd2BlkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd2BklB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | L4 | 1);
				if(nextfree > 0)
					SQd2B(((ld|bit)<<3) | 1, ((rd|bit)>>3) | L4, col|bit, row+3, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd2BklB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd2BlBkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1);
				if(nextfree > 0)
					SQd2BkB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd2BlBkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd2BkBlB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | (1 << (N3)));
				if(nextfree > 0)
					SQd2BlB(((ld|bit)<<2), ((rd|bit)>>2) | (1 << (N3)), col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd2BkBlB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd2BlB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1);
				if(nextfree > 0)
					SQd2B(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd2BlB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd2BkB(int ld, int rd, int col, int row, int free) {
		int bit;
		int nextfree;

		if(row == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | L3);
				if(nextfree > 0)
					SQd2B(((ld|bit)<<2), ((rd|bit)>>2) | L3, col|bit, row+2, nextfree);
			}
			return;
		}

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQd2BkB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQd2B(int ld, int rd, int col, int row, int free) {
		if(row == endmark) {
			if((free & (~1)) > 0) 
				tempcounter++;
			return;
		}

		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			int next_ld = ((ld|bit)<<1);
			int next_rd = ((rd|bit)>>1);
			int next_col = (col|bit);
			nextfree = ~(next_ld | next_rd | next_col);
			if(nextfree > 0)
				if(row < endmark-1) {
					if(~((next_ld<<1) | (next_rd>>1) | (next_col)) > 0)
						SQd2B(next_ld, next_rd, next_col, row+1, nextfree);
				} else {
					SQd2B(next_ld, next_rd, next_col, row+1, nextfree);
				}
		}
	}

	
	@Override
	public void run() {
		running = true;
	
		int i, j, k, l, ijkl, ld, rd, col, startQueensIjkl, start, free; 
		final int N = this.N; 
		final int smallmask = (1 << (N-2)) - 1, listsize = startQueensIjklList.size();
		
		for(int idx = 0; idx < listsize; idx++) {
			
			startQueensIjkl = startQueensIjklList.getFirst();
			start = startQueensIjkl >> 25;
			ijkl = startQueensIjkl & ((1 << 20) - 1);
			i = geti(ijkl); j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
			ld = ldList.getFirst() >>> 1;
			rd = rdList.getFirst() >>> 1;
			col = (colList.getFirst() >>> 1) | (~smallmask);
			free = ~(ld | rd | col);
			
			// if the queen j is more than 2 columns away from the corner 
			if(j < N - 3) {
				jmark = j + 1; 
				endmark = N - 2;
				// k < l 
				if(k < l) {
					mark1 = k - 1; 
					mark2 = l - 1; 
					// if at least l is yet to come 
					if(start < l) {
						// if also k is yet to come 
						if(start < k) {
							// if there are free rows between k and l 
							if(l != k + 1) {
								SQBkBlBjrB(ld, rd, col, start, free);
							}
							// if there are no free rows between k and l 
							else { 
								SQBklBjrB(ld, rd, col, start, free); 
							}
						}
						// if k already came before start and only l is left 
						else {
							SQBlBjrB(ld, rd, col, start, free); 
						}
					}
					// if both k and l already came before start 
					else {
						SQBjrB(ld, rd, col, start, free); 
					}
				}
				// l < k 
				else {
					mark1 = l - 1; 
					mark2 = k - 1; 
					// if at least k is yet to come 
					if(start < k) {
						// if also l is yet to come 
						if(start < l) {
							// if there is at least one free row between l and k 
							if(k != l + 1) {
								SQBlBkBjrB(ld, rd, col, start, free); 
							}
							// if there is no free row between l and k 
							else {
								SQBlkBjrB(ld, rd, col, start, free); 
							}
						}
						// if l already came and only k is yet to come 
						else {
							SQBlBjrB(ld, rd, col, start, free); 
						}
					}
					// if both l and k already came before start 
					else {
						SQBjrB(ld, rd, col, start, free); 
					}
				}
			}
			// if the queen j is exactly 2 columns away from the corner 
			else if(j == N - 3) {
				// this means that the last row will always be row N-2 
				endmark = N - 2; 
				// k < l 
				if(k < l) {
					mark1 = k - 1; 
					mark2 = l - 1; 
					// if at least l is yet to come 
					if(start < l) {
						// if k is yet to come too 
						if(start < k) {
							// if there are free rows between k and l 
							if(l != k+1) {
								SQd2BkBlB(ld, rd, col, start, free); 
							}
							else {
								SQd2BklB(ld, rd, col, start, free); 
							}
						}
						// if k was set before start 
						else {
							mark2 = l - 1;
							SQd2BlB(ld, rd, col, start, free); 
						}
					}
					// if k and l already came before start 
					else {
						SQd2B(ld, rd, col, start, free);
					}
				}
				// l < k 
				else {
					mark1 = l - 1; 
					mark2 = k - 1; 
					endmark = N - 2; 
					// if at least k is yet to come 
					if(start < k) {
						// if also l is yet to come 
						if(start < l) {
							// if there are free rows between l and k 
							if(k != l+1) {
								SQd2BlBkB(ld, rd, col, start, free);
							}
							// if there are no free rows between l and k 
							else {
								SQd2BlkB(ld, rd, col, start, free);
							}
						}
						// if l came before start 
						else {
							mark2 = k - 1; 
							SQd2BkB(ld, rd, col, start, free);
						}
					}
					// if both l and k already came before start 
					else {
						SQd2B(ld, rd, col, start, free); 
					}
				}
			}
			// if the queen j is exactly 1 column away from the corner 
			else if(j == N - 2) {
				// k < l 
				if(k < l) {
					// k can not be first, l can not be last due to queen placement 
					// thus always end in line N-2 
					endmark = N - 2;
					// if at least l is yet to come 
					if(start < l) {
						// if k is yet to come too 
						if(start < k) {
							mark1 = k - 1;
							// if k and l are next to each other 
							if(l != k+1) {
								mark2 = l - 1;
								SQd1BkBlB(ld, rd, col, start, free);
							}
							// 
							else {
								SQd1BklB(ld, rd, col, start, free);
							}
						}
						// if only l is yet to come 
						else{
							mark2 = l - 1;
							SQd1BlB(ld, rd, col, start, free); 
						}
					}
					// if k and l already came 
					else {
						SQd1B(ld, rd, col, start, free);
					}
				}
				// l < k 
				else {
					// if at least k is yet to come 
					if(start < k) {
						// if also l is yet to come 
						if(start < l) {
							// if k is not at the end 
							if(k < N-2) {
								mark1 = l - 1;
								endmark = N - 2;
								// if there are free rows between l and k 
								if(k != l+1) {
									mark2 = k - 1;
									SQd1BlBkB(ld, rd, col, start, free);
								}
								// if there are no free rows between l and k 
								else {
									SQd1BlkB(ld, rd, col, start, free);
								}
							}
							// if k is at the end 
							else {
								// if l is not right before k 
								if(l != N-3) {
									mark2 = l - 1;
									endmark = N - 3;
									SQd1BlB(ld, rd, col, start, free);
								}
								// if l is right before k 
								else {
									endmark = N - 4;
									SQd1B(ld, rd, col, start, free);
								}
							}
						}
						// if only k is yet to come 
						else{
							// if k is not at the end 
							if(k != N-2) {
								mark2 = k - 1;
								endmark = N - 2;
								SQd1BkB(ld, rd, col, start, free);
							}
							else {
								// if k is at the end 
								endmark = N - 3;
								SQd1B(ld, rd, col, start, free); 
							}
						}
					}
					// k and l came before start 
					else {
						endmark = N - 2; 
						SQd1B(ld, rd, col, start, free); 
					}
				}
			}
			// if the queen j is placed in the corner 
			else if(j == N-1) {
				endmark = N - 2;
				if(start > k) {
					SQd0B(ld, rd, col, start, free);
				}
				// k can not be in the last row due to the way we construct start constellations with a queen in the corner and 
				// due to the way we apply jasmin 
				else {
					mark1 = k - 1;
					SQd0BkB(ld, rd, col, start, free);
				}
			}
			
			
			
			// sum up solutions
			solvecounter += tempcounter * symmetry(ijkl);

			// get occupancy of the board for each starting constellation and the hops and max from board Properties 
			tempcounter = 0;								// set counter of solutions for this starting constellation to 0

			// for saving and loading progress remove the finished starting constellation
			startQueensIjklList.removeFirst();
			ldList.removeFirst();
			rdList.removeFirst();
			colList.removeFirst();
			
			// update the current startconstellation-index
			done++;
			
			// check for pausing
			if(pause == 1) {
				pause = 2;
				caller.onPauseStart();
				while(pause == 2) {
					if(cancel)
						break;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			// check for cancelling
			if(cancel) {
				break;
			}
			
		}
		running = false;
	}

	// for user interaction
	void pauseThread() {
		pause = 1;
	}
	
	void cancelThread() {
		cancel = true;
	}
	
	void resumeThread() {
		pause = 0;
		cancel = false;
	}
	
	boolean isPaused() {
		return pause == 2;
	}
	
	boolean wasCanceled() {
		return !running && cancel;
	}
	
	// getters and setters
	int getDone() {
		return done;
	}
	
	long getSolutions() {
		return solvecounter;
	}
	
	ArrayDeque<Integer> getRemainingConstellations() {
		return startConstellations;
	}
	
	// helper functions for doing the math
	// for symmetry stuff and working with ijkl
	// true, if starting constellation is symmetric for rot90
	private boolean symmetry90(int ijkl) {
		if(((geti(ijkl) << 15) + (getj(ijkl) << 10) + (getk(ijkl) << 5) + getl(ijkl)) == (((N-1-getk(ijkl))<<15) + ((N-1-getl(ijkl))<<10) + (getj(ijkl)<<5) + geti(ijkl)))
			return true;
		return false;
	}
	// how often does a found solution count for this start constellation
	private int symmetry(int ijkl) {
		if(geti(ijkl) == N-1-getj(ijkl) && getk(ijkl) == N-1-getl(ijkl))		// starting constellation symmetric by rot180?
			if(symmetry90(ijkl))		// even by rot90?
				return 2;
			else
				return 4;
		else
			return 8;					// none of the above?
	}
	
	private int geti(int ijkl) {
		return ijkl >> 15;
	}
	private int getj(int ijkl) {
		return (ijkl >> 10) & 31;
	}
	private int getk(int ijkl) {
		return (ijkl >> 5) & 31;
	}
	private int getl(int ijkl) {
		return ijkl & 31;
	}
}
