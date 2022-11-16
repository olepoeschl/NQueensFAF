
//	//	//	//	//	//	//	//	//
//	  Explosion Boost 9000		//
//	//	//	//	//	//	//	//	//

__kernel void run(global int *ld_arr, global int *rd_arr, global int *col_arr, global int *start_jkl_arr, global long *result, global int *progress) {

// gpu intern indice
	int g_id = get_global_id(0);							// global thread id 
	int l_id = get_local_id(0);								// local thread id within workgroup
	
// variables												
	// start_jkl_arr contains [11 queens free][5 queens for start][5 queens for j][5 queens for k][5 queens for l] 
	int start = start_jkl_arr[g_id] >> 15;		
	if(start == 69) {										// if we have a pseudo constellation we do nothing 
		progress[g_id] = -1;
		return;
	}
	int j = (start_jkl_arr[g_id] >> 10) & 31;				// queen in last row at position j
	int k = (start_jkl_arr[g_id] >> 5) & 31;				// in row k queen at left border, in row l queen at right border
	int l = start_jkl_arr[g_id] & 31;
	
	// describe the occupancy of the board 
	uint ld = ld_arr[g_id];									// left diagonals, 1 means occupied
	uint rd = rd_arr[g_id];									// right diagonals, 1 means occupied 
	uint col = ~((1 << N) - 1) | col_arr[g_id];				// columns, 1 means occupied 
	// for memorizing board-leaving diagonals 
	uint ld_mem = 0;
	uint rd_mem = 0;
	// queen at the left border of the board ( right border is represented by 1) 
	uint L = 1 << (N-1);	
	
	// jkl_queens occupies the diagoals, that go from bottom row to upper right and upper left 
	local uint jkl_queens[BLOCK_SIZE][N];
	// the diagonals from queen j and k with respect to the last row
	uint rdiag = (L >> j) | (L >> (N-1-k));
	// the diagonals from queen j and l with respect to the last row
	uint ldiag = (L >> j) | (L >> l);
	for(int a = N-1;a > 0; a--){							// we also occupy the left and right border 
		jkl_queens[l_id][a] = (ldiag >> N-1-a) | (rdiag << (N-1-a)) | L | 1;
	}
	ldiag = L >> k;											// ld from queen l with respect to the first row 
	rdiag = 1 << l;											// ld from queen k with respect to the first row 
	for(int a = 0;a < N; a++){
		jkl_queens[l_id][a] |= (ldiag << a) | (rdiag >> a);
	}
	jkl_queens[l_id][k] = ~L;
	jkl_queens[l_id][l] = ~1; 

	// initialize current row as start and solvecounter as 0
	int row = start;
	uint solvecounter = 0;	
	// calculate the first queen that will be set in the first row 
	uint free = ~(ld | rd | col | jkl_queens[l_id][row]);	// free is 1 if a queen can be set at the queens location 
	// bit is the queen that will be set in the current row 
	uint queen = -free & free;	
	// each row of queens contains the queens of the board of one workitem 
	// local arrays are faster 
	local uint queens[BLOCK_SIZE][N];						// for remembering the queens for all rows for all boards in the work-group 
	queens[l_id][start] = queen;							// we already calculated the first queen in the start row 
	// going forward or backward? 										
	int direction = 1;
	
// iterative loop representing the recursive setqueen-function 
// this is the actual solver (via backtracking with Jeff Somers Bit method
// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed 
	while(row >= start) {									// while we havent tried everything 
		if(free) {											// if there are free slots in the current row 
			direction = 1;									// we are going forwards 
			queen = -free & free;							// this is the next free slot for a queen (searching from the right border) in the current row
			queens[l_id][row] = queen;						// remember the queen 
			row++;											// increase row counter 

			ld_mem = ld_mem << 1 | ld >> 31;				// place the queen in the diagonals and shift them and remember the diagonals leaving the board 
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | queen) << 1;							
			rd = (rd | queen) >> 1;	
		}
		else{												// if the row is completely occupied 
			direction = 0;									// we are going backwards 
			row--;											// decrease row counter 
			queen = queens[l_id][row];						// recover the queen in order to remove it 
																									
			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;		// shift diagonals one back, remove the queen and insert the diagonals that had left the board 
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;						
		}
		free = ~(jkl_queens[l_id][row] | ld | rd | col);	// calculate the occupancy of the next row
		free &= ~(queen + direction-1);						// aoccupy all bits right from the last queen in order to not place the same queen again 
		col ^= queen;										// free up the column AFTER calculating notfree in order to not place the same queen again		

		solvecounter += (row == N-1);						// increase the solvecounter, if we are in the last row 
	}
	result[g_id] = solvecounter;							// number of solutions of the work item 
	progress[g_id] = 1;										// progress 1 if done, 0 if not 
}
