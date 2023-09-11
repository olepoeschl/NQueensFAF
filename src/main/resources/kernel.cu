// Explosion Boost 9000

extern "C" __global__ void nqfaf(int *ld_arr, int *rd_arr, int *col_arr, int *start_jkl_arr, long *result) {
	// gpu intern indice
	int g_id = blockIdx.x * blockDim.x + threadIdx.x;		// global thread id 
	int l_id = threadIdx.x;  								// local thread id within block
	printf("g_id: %d, l_id: %d\n", g_id, l_id);
	// variables		
	unsigned int L = 1 << (N-1);				// queen at the left border of the board (right border is represented by 1) 										
	// start_jkl_arr contains [6 queens free][5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l] 
	int start = start_jkl_arr[g_id] >> 20;		
	if(start == 69) {				// if we have a pseudo constellation we do nothing 
		return;
	}
	// printf("[%d] N: %d, startjkl: %d\n", g_id, N, start_jkl_arr[g_id]);
	int j = (start_jkl_arr[g_id] >> 10) & 31;	// queen in last row at position j
	int k = (start_jkl_arr[g_id] >> 5) & 31;	// in row k queen at left border, in row l queen at right border
	int l = start_jkl_arr[g_id] & 31;

	// describe the occupancy of the board 
	unsigned int ld = ld_arr[g_id];				// left diagonals, 1 means occupied
	unsigned int rd = rd_arr[g_id];				// right diagonals, 1 means occupied 
	unsigned int col = ~(L-2) ^ col_arr[g_id];		// columns, 1 means occupied 
	unsigned int ld_mem = 0;				// for memorizing board-leaving diagonals 
	unsigned int rd_mem = 0;

	// jkl_queens occupies the diagonals, that go from bottom row to upper right and upper left 
	// and also the left and right column 
	// in row k only L is free and in row l only 1 is free 
	__shared__ unsigned int jkl_queens[N];
	unsigned int rdiag = (L >> j) | (L >> (N-1-k));		// the rd from queen j and k with respect to the last row
	unsigned int ldiag = (L >> j) | (L >> l);		// the ld from queen j and l with respect to the last row
	if(l_id == 0) {
		for(int a = 0;a < N; a++){			// we also occupy the left and right border 
			jkl_queens[N-1-a] = (ldiag >> a) | (rdiag << a) | L | 1;
		}
	}
	ldiag = L >> k;					// ld from queen l with respect to the first row 
	rdiag = 1 << l;					// ld from queen k with respect to the first row 
	if(l_id == 0) {
		for(int a = 0;a < N; a++){
			jkl_queens[a] |= (ldiag << a) | (rdiag >> a);
		}
		jkl_queens[k] = ~L;
		jkl_queens[l] = ~1; 
	}
	__syncthreads();			// avoid corrupt memory behavior 

	ld &= ~(ldiag << start);			// remove queen k from ld 
	if(l != N-1)					// only remove queen k from rd, if no queen in corner (N-1,N-1)
		rd &= ~(rdiag >> start);			// otherwise we continue in row N-1 and find too many solutions 

	// initialize current row as start and solutions as 0
	int row = start;
	unsigned long solutions = 0;

	// calculate the occupancy of the first row
	unsigned int free = ~(ld | rd | col | jkl_queens[row]);	// free is 1 if a queen can be set at the queens location
	unsigned int queen = -free & free;			// the queen that will be set in the current row
	// each row of queens contains the queens of the board of one workitem 
	// local arrays are faster 
	__shared__ unsigned int queens[WORKGROUP_SIZE][N];		// for remembering the queens for all rows for all boards in the work-group 
	queens[l_id][start] = queen;			// we already calculated the first queen in the start row 

	// going forward (setting a queen) or backward (removing a queen)? 										
	int direction = 0;

	// iterative loop representing the recursive setqueen-function 
	// this is the actual solver (via backtracking with Jeff Somers Bit method) 
	// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed 
	while(row >= start) {				// while we haven't tried everything 
		if(free) {					// if there are free slots in the current row 
			direction = 1;					// we are going forwards 
			queen = -free & free;				// this is the next free slot for a queen (searching from the right border) in the current row
			queens[l_id][row] = queen;			// remember the queen 
			row++;						// increase row counter

			ld_mem = ld_mem << 1 | ld >> 31;		// place the queen in the diagonals and shift them and remember the diagonals leaving the board 
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | queen) << 1;							
			rd = (rd | queen) >> 1;	
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

		if(row == N-1)					// increase the solutions, if we are in the last row 
			solutions++;
	}
	printf("solutions: %d\n", solutions);
	result[g_id] = solutions;			// number of solutions of the work item 
}
