package de.nqueensfaf.compute;

import java.util.ArrayList;

import de.nqueensfaf.data.Constellation;

class CPUSolverThread extends Thread {

	private final int N, N3, N4, L, L3, L4;				// boardsize
	private long tempcounter = 0;					// tempcounter is #(unique solutions) of current start constellation, solvecounter is #(all solutions)

	// mark1 and mark2 mark the lines k-1 and l-1 (not necessarily in this order), 
	// because in from this line we will directly shift everything to the next free row 
	// endmark marks the row of the last free row 
	// jmark marks the row j+1, where the diagonal jr from j has to be set 
	private int mark1, mark2, endmark, jmark;
 
	// list of uncalculated starting positions, their indices
	private ArrayList<Constellation> constellations;

	CPUSolverThread(int N, ArrayList<Constellation> constellations) {
		this.N = N;
		N3 = N - 3;
		N4 = N - 4;
		L = 1 << (N-1);
		L3 = 1 << N3;
		L4 = 1 << N4;
		this.constellations = constellations;
	}

	// Recursive functions for Placing the Queens
	
	// IMPORTANT: since the left and right col are occupied by the startConstalletaion, we only deal with the bits in between, 
	//     hence N-2 bits for a board of size N
	// the functions recursively call themselves and travel through the board row-wise 
	// the occupancy of each row is represented with integers in binary representation (1 occupied, 0 free) 
	// there are different recursive functions for different arrangements of the queens i,j,k,l on the border 
	// in order to reduce the amount of different cases we rotate and mirror the board in such a way, 
	//     that the queen j in the last row is as close to the right corner as possible 
	// this is done by the function jasmin (j as min) 
	// we call this distance to the corner d and distinguish between d=0,d=1,d=2,d <small enough> and d <big> 
	// for d <small enough> the diagonal jl from queen j going upwards to the left can already be set
	//     in the first row of the start constellation 
	// for d <big> we have to explicitly set occupy this diagonal in some row before we can continue 
	
	// NOTATION: 
	// SQ stands for SetQueens and is the prefix of any of the following solver functions 
	// B stand for block and describes a block of free rows, where nothing special has to be done 
	// Blocks B are separated by the rows k and l for d<=2 
	//     and additionally by row jr for d <small enough> and additionally by row jl for d <big>
	// jl is always first and jr is always last, k and l are in between in no fixed order 
	// (the last fact is a consequence of jasmin) 
	
	// in the last function of every case, respectively, we check check in both next rows, if there are free spaces in the row 
	
	// of course, when traveling over row k or l or both or jl or jr, we have to shift ld and rd by 2 or 3 rows at once 
	// after skipping these rows we have to occupy the corresponding diagonals 
	
	// for d = 0
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

	// for d = 1
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
	
	// for d = 2
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

	// for d>2 but d <small enough> 
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
	
	// for d <big> 
	private void SQBjlBkBlBjrB(int ld, int rd, int col, int row, int free) {
		if(row == N-1-jmark) {
			rd |= L;
			free &= ~L;
			SQBkBlBjrB(ld, rd, col, row, free);
			return; 
		}
		
		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBjlBkBlBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBjlBlBkBjrB(int ld, int rd, int col, int row, int free) {
		if(row == N-1-jmark) {
			rd |= L;
			free &= ~L;
			SQBlBkBjrB(ld, rd, col, row, free);
			return; 
		}
		
		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBjlBlBkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBjlBklBjrB(int ld, int rd, int col, int row, int free) {
		if(row == N-1-jmark) {
			rd |= L;
			free &= ~L;
			SQBklBjrB(ld, rd, col, row, free);
			return; 
		}
		
		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBjlBklBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	private void SQBjlBlkBjrB(int ld, int rd, int col, int row, int free) {
		if(row == N-1-jmark) {
			rd |= L;
			free &= ~L;
			SQBlkBjrB(ld, rd, col, row, free);
			return; 
		}
		
		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit));
			if(nextfree > 0)
				SQBjlBlkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}
	
	@Override
	public void run() {
		int j, k, l, ijkl, ld, rd, col, startIjkl, start, free, LD;
		final int N = this.N; 
		final int smallmask = (1 << (N-2)) - 1;
		
		for(Constellation constellation : constellations) {
			startIjkl = constellation.getStartijkl();
			start = startIjkl >> 20;
			ijkl = startIjkl & ((1 << 20) - 1);
			j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
			
			// IMPORTANT NOTE: we shift ld and rd one to the right, because the right 
			// column does not matter (always occupied by queen l)
			// add occupation of ld from queens j and l from the bottom row upwards 
			LD = (L >>> j) | (L >>> l);
			ld = constellation.getLd() >>> 1;
			ld |= LD >>> (N-start);
			// add occupation of rd from queens j and k from the bottom row upwards 
			rd = constellation.getRd() >>> 1;
			if(start > k)
				rd |= (L >>> (start-k+1)); 
			if(j >= 2*N-33-start)	// only add the rd from queen j if it does not 
				rd |= (L >>> j) << (N-2-start);		// occupy the sign bit! 
			
			// also occupy col and then calculate free 
			col = (constellation.getCol() >>> 1) | (~smallmask);
			free = ~(ld | rd | col);
			
		// big case distinction for deciding which soling algorithm to use 
			
			// if queen j is more than 2 columns away from the corner 
			if(j < N - 3) {
				jmark = j + 1; 
				endmark = N - 2;
				// if the queen j is more than 2 columns away from the corner but the rd from the j-queen can be set right at start 
				if(j < 2*N - 34 - start) {
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
								SQBkBjrB(ld, rd, col, start, free); 
							}
						}
						// if both l and k already came before start 
						else {
							SQBjrB(ld, rd, col, start, free); 
						}
					}
				}
				// if we have to set some queens first in order to reach the row N-1-jmark where the rd from queen j
				// can be set 
				else {
					// k < l 
					if(k < l) {
						mark1 = k - 1;
						mark2 = l - 1; 
						// there is at least one free row between rows k and l 
						if(l != k+1) {
							SQBjlBkBlBjrB(ld, rd, col, start, free);
						}
						// if l comes right after k 
						else {
							SQBjlBklBjrB(ld, rd, col, start, free); 
						}
					}
					// l < k 
					else {
						mark1 = l - 1; 
						mark2 = k - 1; 
						// there is at least on efree row between rows l and k 
						if(k != l+1) {
							SQBjlBlBkBjrB(ld, rd, col, start, free); 
						}
						// if k comes right after l 
						else {
							SQBjlBlkBjrB(ld, rd, col, start, free); 
						}
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
			else{
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

			// for saving and loading progress remove the finished starting constellation
			constellation.setSolutions(tempcounter * symmetry(ijkl));
			tempcounter = 0;
		}
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
