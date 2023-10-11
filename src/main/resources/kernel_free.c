kernel void nqfaf_intel(global int *ld_arr, global int *rd_arr, global int *col_arr, global int *start_jkl_arr, global long *result) {
	// gpu intern indice
	int g_id = get_global_id(0);			// global thread id
	int l_id = get_local_id(0);  			// local thread id within workgroup

	// variables
	uint L = 1 << (N-1);				// queen at the left border of the board (right border is represented by 1)
	// start_jkl_arr contains [6 queens free][5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l]
	int start_jkl = start_jkl_arr[g_id];
	int start = (start_jkl >> 20) & 31;
	if(start == 69) {				// if we have a pseudo constellation we do nothing
		return;
	}
	int j = (start_jkl >> 10) & 31;	// queen in last row at position j
	int k = (start_jkl >> 5) & 31;	// in row k queen at left border, in row l queen at right border
	int l = start_jkl & 31;

	uint ld = ld_arr[g_id];
	uint rd = rd_arr[g_id];
	uint col = ~(L-2) ^ col_arr[g_id];
	uint ld_mem = 0;
	uint rd_mem = 0;

	// the part that is different from the default kernel
	local uint jkl_queens[N];
	uint ldiagbot = (L >> j) | (L >> l);
	uint rdiagbot = (L >> j) | (L >> (N-1-k));
	uint ldiagtop = L >> k;
	uint rdiagtop = 1 << l;
	for(int a = 0;a < N; a++){
		jkl_queens[a] = (a==k)*(~L) + (a==l)*(~1) + (a!=k&&a!=l)*((ldiagbot >> (N-1-a)) | (rdiagbot << (N-1-a)) | (ldiagtop << a) | (rdiagtop >> a) | L | 1);
	}
	// -----

	ld &= ~(ldiagtop << start);
	if(l != N-1)
		rd &= ~(rdiagtop >> start);

	int row = start;
	ulong solutions = 0;
	uint free = ~(ld | rd | col | jkl_queens[row]);
	uint queen = -free & free;

	local uint2 queens[WORKGROUP_SIZE][N];
	queens[l_id][start].x = queen;
	queens[l_id][start].y = free;

	int direction = 0;

	// iterative loop representing the recursive setqueen-function
	// this is the actual solver (via backtracking with Jeff Somers Bit method)
	// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed
	while(row >= start) {				// while we haven't tried everything
		if(free) {					// if there are free slots in the current row
			if(row == N-2){					// increase the solutions, if we are in the last row
				solutions++;
				free = 0;
			}
			else{
				queen = -free & free;				// this is the next free slot for a queen (searching from the right border) in the current row
				queens[l_id][row].x = queen;
				queens[l_id][row].y = free ^ queen;			// remember the queen
				row++;						// increase row counter

				ld_mem = ld_mem << 1 | ld >> 31;		// place the queen in the diagonals and shift them and remember the diagonals leaving the board
				rd_mem = rd_mem >> 1 | rd << 31;
				ld = (ld | queen) << 1;
				rd = (rd | queen) >> 1;

				col ^= queen;
				free = ~(jkl_queens[row] | ld | rd | col);
			}
		}
		else {						// if the row is completely occupied
			row--;						// decrease row counter
			queen = queens[l_id][row].x;			// recover the queen in order to remove it
			free = queens[l_id][row].y;

			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;	// shift diagonals one back, remove the queen and insert the diagonals that had left the board
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;

			col ^= queen;
		}
	}
	result[g_id] = solutions;			// number of solutions of the work item
}
