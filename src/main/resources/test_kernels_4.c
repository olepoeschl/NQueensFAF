struct constellation {
    uint ld;
    uint rd;
    uint col;
    uint start_ijkl;
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
	uint queen;

	local uint stack[4*N][WORKGROUP_SIZE];
	stack[4*start][l_id] = ld;
	stack[4*start+1][l_id] = rd;
	stack[4*start+2][l_id] = col;
	stack[4*start+3][l_id] = free;

	// iterative loop representing the recursive setqueen-function
	// this is the actual solver (via backtracking with Jeff Somers Bit method)
	// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed
	while(row >= start) {				// while we haven't tried everything
		solutions += (row == N-1);
		if(free){
			queen = free & -free;
			stack[4*row+3][l_id] = free ^ queen;
			ld = (ld | queen) << 1;
			rd = (rd | queen) >> 1;
			col |= queen;
			row++;
			stack[4*row][l_id] = ld;
			stack[4*row+1][l_id] = rd;
			stack[4*row+2][l_id] = col;
			stack[4*row+3][l_id] = free;
			free = ~(ld | rd | col | jkl_queens[row]);
		}
		else{
			row--;
			ld = stack[4*row][l_id];
			rd = stack[4*row+1][l_id];
			col = stack[4*row+2][l_id];
			free = stack[4*row+3][l_id];
		}
	}
	result[get_global_id(0)] = solutions;			// number of solutions of the work item
}
