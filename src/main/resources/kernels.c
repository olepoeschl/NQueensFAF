// Explosion Boost 9000

// null kernel as a workaround for AMD GPUs to ensure they return the correct results
kernel void null() {
	
}

// Nvidia kernel
kernel void nqfaf_nvidia(global int *ld_arr, global int *rd_arr, global int *col_arr, global int *start_jkl_arr, global long *result) {
	// gpu intern indice
	int g_id = get_global_id(0);			// global thread id 
	int l_id = get_local_id(0);  			// local thread id within workgroup

	// variables		
	uint L = 1 << (N-1);				// queen at the left border of the board (right border is represented by 1) 										
	// start_jkl_arr contains [6 queens free][5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l] 
	int start = start_jkl_arr[g_id] >> 20;		
	if(start == 69) {				// if we have a pseudo constellation we do nothing 
		return;
	}
	int j = (start_jkl_arr[g_id] >> 10) & 31;	// queen in last row at position j
	int k = (start_jkl_arr[g_id] >> 5) & 31;	// in row k queen at left border, in row l queen at right border
	int l = start_jkl_arr[g_id] & 31;

	// describe the occupancy of the board 
	uint ld = ld_arr[g_id];				// left diagonals, 1 means occupied
	uint rd = rd_arr[g_id];				// right diagonals, 1 means occupied 
	uint col = ~(L-2) ^ col_arr[g_id];		// columns, 1 means occupied 
	uint ld_mem = 0;				// for memorizing board-leaving diagonals 
	uint rd_mem = 0;

	// jkl_queens occupies the diagonals, that go from bottom row to upper right and upper left 
	// and also the left and right column 
	// in row k only L is free and in row l only 1 is free 
	local uint jkl_queens[N];
	uint rdiag = (L >> j) | (L >> (N-1-k));		// the rd from queen j and k with respect to the last row
	uint ldiag = (L >> j) | (L >> l);		// the ld from queen j and l with respect to the last row
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
	barrier(CLK_LOCAL_MEM_FENCE);			// avoid corrupt memory behavior 

	ld &= ~(ldiag << start);			// remove queen k from ld 
	if(l != N-1)					// only remove queen k from rd, if no queen in corner (N-1,N-1)
		rd &= ~(rdiag >> start);			// otherwise we continue in row N-1 and find too many solutions 

	// initialize current row as start and solutions as 0
	int row = start;
	ulong solutions = 0;

	// calculate the occupancy of the first row
	uint free = ~(ld | rd | col | jkl_queens[row]);	// free is 1 if a queen can be set at the queens location
	uint queen = -free & free;			// the queen that will be set in the current row
	// each row of queens contains the queens of the board of one workitem 
	// local arrays are faster 
	local uint queens[WORKGROUP_SIZE][N];		// for remembering the queens for all rows for all boards in the work-group 
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
	result[g_id] = solutions;			// number of solutions of the work item 
}

// AMD kernel
kernel void nqfaf_amd(global int *ld_arr, global int *rd_arr, global int *col_arr, global int *start_jkl_arr, global long *result) {
	// gpu intern indice
	int g_id = get_global_id(0);			// global thread id 
	short l_id = get_local_id(0);			// local thread id within workgroup

	// variables		
	uint L = 1 << (N-1);				// queen at the left border of the board (right border is represented by 1) 										
	// start_jkl_arr contains [6 queens free][5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l] 
	char start = start_jkl_arr[g_id] >> 20;		
	if(start == 69) {				// if we have a pseudo constellation we do nothing 
		return;
	}
	char j = (start_jkl_arr[g_id] >> 10) & 31;	// queen in last row at position j
	char k = (start_jkl_arr[g_id] >> 5) & 31;	// in row k queen at left border, in row l queen at right border
	char l = start_jkl_arr[g_id] & 31;

	// describe the occupancy of the board 
	uint ld = ld_arr[g_id];				// left diagonals, 1 means occupied
	uint rd = rd_arr[g_id];				// right diagonals, 1 means occupied 
	uint col = ~(L-2) ^ col_arr[g_id];		// columns, 1 means occupied 
	uint ld_mem = 0;				// for memorizing board-leaving diagonals 
	uint rd_mem = 0;

	// jkl_queens occupies the diagonals, that go from bottom row to upper right and upper left 
	// and also the left and right column 
	// in row k only L is free and in row l only 1 is free 
	local uint jkl_queens[N];
	uint rdiag = (L >> j) | (L >> (N-1-k));		// the rd from queen j and k with respect to the last row
	uint ldiag = (L >> j) | (L >> l);		// the ld from queen j and l with respect to the last row
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
	barrier(CLK_LOCAL_MEM_FENCE);			// avoid corrupt memory behavior 

	ld &= ~((L >> k) << start);			// remove queen k from ld 
	if(l != N-1)					// only remove queen k from rd, if no queen in corner (N-1,N-1)
		rd &= ~((1 << l) >> start);			// otherwise we continue in row N-1 and find too many solutions 

	// initialize current row as start and solutions as 0
	char row = start;
	ulong solutions = 0;

	// calculate the occupancy of the first row
	uint free = ~(ld | rd | col | jkl_queens[row]);	// free is 1 if a queen can be set at the queens location
	uint queen = -free & free;			// the queen that will be set in the current row
	// each row of queens contains the queens of the board of one workitem 
	// local arrays are faster 
	local uint queens[WORKGROUP_SIZE][N];		// for remembering the queens for all rows for all boards in the work-group 
	queens[l_id][start] = queen;			// we already calculated the first queen in the start row 

	// going forward (setting a queen) or backward (removing a queen)? 										
	bool direction = 0;

	// iterative loop representing the recursive setqueen-function 
	// this is the actual solver (via backtracking with Jeff Somers Bit method) 
	// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed 
	while(row >= start) {				// while we haven't tried everything 
		direction = (free > 0);				// forwards or backwards?
		if(direction) {					// if there are free slots in the current row
			queen = -free & free;				// this is the next free slot for a queen (searching from the right border) in the current row
			queens[l_id][row] = queen;			// remember the queen 
			row++;						// increase row counter 

			ld_mem = ld_mem << 1 | ld >> 31;		// place the queen in the diagonals and shift them and remember the diagonals leaving the board 
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | queen) << 1;							
			rd = (rd | queen) >> 1;	
		}
		else {						// if the row is completely occupied
			row--;						// decrease row counter 
			queen = queens[l_id][row];			// recover the queen in order to remove it 

			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;	// shift diagonals one back, remove the queen and insert the diagonals that had left the board 
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;						
		}
		barrier(CLK_LOCAL_MEM_FENCE);
		free = ~(jkl_queens[row] | ld | rd | col); 	// calculate the occupancy of the next row
		free &= ~(queen + direction-1);			// occupy all bits right from the last queen in order to not place the same queen again 
		col ^= queen;					// free up the column AFTER calculating free in order to not place the same queen again		

		solutions += (row == N-1);			// increase the solutions, if we are in the last row 
	}
	result[g_id] = solutions;			// number of solutions of the work item 
	barrier(CLK_GLOBAL_MEM_FENCE);
}

// Intel kernel
// Same as default kernel, but jkl_queens is initialized using only 1 loop instead of 2.
// It's a bit slower, but the barrier, that caused problems on Intel GPU's, is not needed.
kernel void nqfaf_intel(global int *ld_arr, global int *rd_arr, global int *col_arr, global int *start_jkl_arr, global long *result) {
	int g_id = get_global_id(0);
	int l_id = get_local_id(0);

	uint L = 1 << (N-1);
	int start = start_jkl_arr[g_id] >> 20;		
	if(start == 69) {
		return;
	}
	int j = (start_jkl_arr[g_id] >> 10) & 31;
	int k = (start_jkl_arr[g_id] >> 5) & 31;
	int l = start_jkl_arr[g_id] & 31;

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
	local uint queens[WORKGROUP_SIZE][N];
	queens[l_id][start] = queen;

	int direction = 0;

	while(row >= start) {	
		if(free) {
			direction = 1;
			queen = -free & free;
			queens[l_id][row] = queen;	
			row++;

			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
		ld = (ld | queen) << 1;							
		rd = (rd | queen) >> 1;	
		}
		else {
			direction = 0;
			row--;
			queen = queens[l_id][row];

			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;						
		}
		free = ~(jkl_queens[row] | ld | rd | col);
		free &= ~(queen + direction-1);
		col ^= queen;

		if(row == N-1)
			solutions++;
	}
	result[g_id] = solutions;
}
