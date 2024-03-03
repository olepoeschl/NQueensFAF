
struct constellation {
    int ld;
    int rd;
    int col;
    int start_ijkl;
};

kernel void nqfaf_nvidia(global struct constellation *constellation_arr, global uint* jkl_queens_arr, global long *result) {
	const struct constellation c = constellation_arr[get_global_id(0)];
    
	int start = (c.start_ijkl >> 20) & 31;
	if(start == 69) {				// if we have a pseudo constellation we do nothing
		return;
	}

	const int l_id = get_local_id(0);
	const uint L = 1 << (N-1);

	uint ld = c.ld;
	uint rd = c.rd;
	uint col = ~(L-2) ^ c.col;
	uint ld_mem = 0;
	uint rd_mem = 0;

	local uint jkl_queens[N];
	jkl_queens[l_id % N] = jkl_queens_arr[get_group_id(0) * N + l_id % N];
	barrier(CLK_LOCAL_MEM_FENCE);

    ld &= ~(L >> (((c.start_ijkl >> 5) & 31) - start));
    if((c.start_ijkl & 31) != N-1)
    	rd &= ~(1 << ((c.start_ijkl & 31) - start));

    int row = start;
	ulong solutions = 0;

	uint free = ~(ld | rd | col | jkl_queens[row]);
	uint queen = -free & free;

	local uint queens[WORKGROUP_SIZE][N];
	queens[l_id][start] = queen;

	int direction = 0;

	// iterative loop representing the recursive setqueen-function
	// this is the actual solver (via backtracking with Jeff Somers Bit method)
	// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed
	while(row >= start) {				// while we haven't tried everything
		if(free) {					// if there are free slots in the current row
			if(row == N-2){					// increase the solutions, if we are in the last row
				solutions++;
				direction = 0;
				row--;						// decrease row counter
				queen = queens[l_id][row];			// recover the queen in order to remove it
				ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;	// shift diagonals one back, remove the queen and insert the diagonals that had left the board
				rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
				ld_mem >>= 1;
				rd_mem <<= 1;
			}
			else{
				direction = 1;					// we are going forwards
				queen = -free & free;				// this is the next free slot for a queen (searching from the right border) in the current row
				queens[l_id][row] = queen;			// remember the queen
				row++;						// increase row counter

				ld_mem = ld_mem << 1 | ld >> 31;		// place the queen in the diagonals and shift them and remember the diagonals leaving the board
				rd_mem = rd_mem >> 1 | rd << 31;
				ld = (ld | queen) << 1;
				rd = (rd | queen) >> 1;
			}
		}
		else {						// if the row is completely occupied
			direction = 0;					// we are going backwards
			row--;						// decrease row counter
			queen = queens[l_id][row];			// recover the queen in order to remove it

			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;	// shift diagonals one back, remove the queen and insert the diagonals that had left the board
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;
		}
		free = ~(jkl_queens[row] | ld | rd | col);	// calculate the occupancy of the next row
		free &= ~(queen + direction-1);			// occupy all bits right from the last queen in order to not place the same queen again
		col ^= queen;					// free up the column AFTER calculating free in order to not place the same queen again
	}
	result[get_global_id(0)] = solutions;			// number of solutions of the work item
}
