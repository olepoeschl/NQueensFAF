
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
	queens[l_id][row] = queen;
	local uint lds[WORKGROUP_SIZE][N];
	lds[l_id][start] = ld;
	local uint rds[WORKGROUP_SIZE][N];
	rds[l_id][start] = rd;

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
				ld = lds[l_id][row];
				rd = rds[l_id][row];
				queen = queens[l_id][row];			// recover the queen in order to remove it
			}
			else{
				direction = 1;					// we are going forwards
				queen = -free & free;				// this is the next free slot for a queen (searching from the right border) in the current row
				queens[l_id][row] = queen;			// remember the queen
				row++;						// increase row counter

				ld = (ld | queen) << 1;
				rd = (rd | queen) >> 1;
				lds[l_id][row] = ld;
				rds[l_id][row] = rd;
			}
		}
		else {						// if the row is completely occupied
			direction = 0;					// we are going backwards
			row--;						// decrease row counter
			ld = lds[l_id][row];
			rd = rds[l_id][row];
			queen = queens[l_id][row];			// recover the queen in order to remove it

		}
		free = ~(ld | rd | col | jkl_queens[row]);	// calculate the occupancy of the next row
		free &= ~(queen + direction-1);			// occupy all bits right from the last queen in order to not place the same queen again
		col ^= queen;					// free up the column AFTER calculating free in order to not place the same queen again
	}
	result[get_global_id(0)] = solutions;			// number of solutions of the work item
}
