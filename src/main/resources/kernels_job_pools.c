// Explosion Boost 9000

struct constellation {
    uint ld;
    uint rd;
    uint col;
    uint start_ijkl;
};

// Nvidia kernel
kernel void nqfaf_nvidia(global struct constellation *constellation_arr, global uint* jkl_queens_arr, volatile global uint* next_job, const uint max_job_index, global long *result) {
    const int l_id = get_local_id(0); // local thread id within workgroup

    // local pointer to next job in global mem
    local uint job_index;
    if(l_id == 0)
	job_index = get_global_id(0);
    barrier(CLK_LOCAL_MEM_FENCE);
    
    // task content
    struct constellation c;
    local uint jkl_queens[N];
    uint old_jkl = 0;
    
    const uint L = 1 << (N-1); // queen at the left border of the board (right border is represented by 1) 
    
    local uint queens[WORKGROUP_SIZE][N]; // for remembering the queens for all rows for all boards in the work-group 
    
    int start, row, direction;
    uint ld, rd, col, ld_mem, rd_mem, free, queen;
    ulong solutions;
    
    while (job_index <= max_job_index) {
	// init job
	c = constellation_arr[job_index + l_id];
	ld = c.ld;
	rd = c.rd;
	col = ~(L-2) ^ c.col;

	if(l_id == 0) {
	    if((c.start_ijkl & 0b111111111111111) != old_jkl) {
		for(int i = 0; i < N; i++) {
		    jkl_queens[i] = jkl_queens_arr[job_index / WORKGROUP_SIZE * N + i];
		}
	    }
	}
	barrier(CLK_LOCAL_MEM_FENCE);

	row = start = c.start_ijkl >> 20 & 31;
	solutions = 0;

	if(start != 69) {
	    ld &= ~(L >> (((c.start_ijkl >> 5) & 31) - start)); // remove queen k from ld 
	    if((c.start_ijkl & 31) != N-1) 
		/* only remove queen k from rd, if no queen in corner (N-1,N-1),
		 * otherwise we continue in row N-1 and find too many solutions
		 */
		rd &= ~(1 << ((c.start_ijkl & 31) - start));

	    free = ~(ld | rd | col | jkl_queens[row]);
	    queens[l_id][start] = queen = -free & free;

	    // solve
	    // iterative loop representing the recursive setqueen-function 
	    // this is the actual solver (via backtracking with Jeff Somers Bit method) 
	    // the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed
	    while(row >= start) {
		// while we haven't tried everything 
		if(free) {
		    /* if there are free slots in the current row,
		     * place a queen and go to the next row
		     */
		    if(row == N-2){
			// increase the solutions, if we are in the last row
			solutions++;
			// then go one row back
			direction = 0;
			row--;
			queen = queens[l_id][row];
			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;
		    }
		    else {
			direction = 1;
			queen = -free & free;
			queens[l_id][row] = queen;
			row++;

			// remember the diagonals leaving the board
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | queen) << 1;							
			rd = (rd | queen) >> 1;	
		    }
		} 
		else {
		    /* if there aren't any free slots in the current row,
		     * go one row back, remove the last queen and continue with the next free slot
		     */
		    direction = 0;
		    row--;
		    queen = queens[l_id][row]; // recover the queen in order to remove it from ld, rd and col

		    // recover the diagonals that previously left the board
		    ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
		    rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
		    ld_mem >>= 1;
		    rd_mem <<= 1;						
		}
		/* calculate the occupancy of the next row
		 * (free is 1 if a queen can be set at the queens location)
		 * free slots are searched and occupied from right to left
		 */
		free = ~(jkl_queens[row] | ld | rd | col);
		free &= ~(queen + direction-1); 
		col ^= queen;
		
		// unroll 1 iteration
		if(row < start)
		    break;

		// while we haven't tried everything 
		if(free) {
		    /* if there are free slots in the current row,
		     * place a queen and go to the next row
		     */
		    if(row == N-2){
			// increase the solutions, if we are in the last row
			solutions++;
			// then go one row back
			direction = 0;
			row--;
			queen = queens[l_id][row];
			ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
			rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
			ld_mem >>= 1;
			rd_mem <<= 1;
		    }
		    else {
			direction = 1;
			queen = -free & free;
			queens[l_id][row] = queen;
			row++;

			// remember the diagonals leaving the board
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | queen) << 1;							
			rd = (rd | queen) >> 1;	
		    }
		} 
		else {
		    /* if there aren't any free slots in the current row,
		     * go one row back, remove the last queen and continue with the next free slot
		     */
		    direction = 0;
		    row--;
		    queen = queens[l_id][row]; // recover the queen in order to remove it from ld, rd and col

		    // recover the diagonals that previously left the board
		    ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
		    rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
		    ld_mem >>= 1;
		    rd_mem <<= 1;						
		}
		/* calculate the occupancy of the next row
		 * (free is 1 if a queen can be set at the queens location)
		 * free slots are searched and occupied from right to left
		 */
		free = ~(jkl_queens[row] | ld | rd | col);
		free &= ~(queen + direction-1); 
		col ^= queen;
	    }

	    // write the number of solutions for this job back to global memory
	    result[job_index + l_id] = solutions;
	}

	barrier(CLK_LOCAL_MEM_FENCE); // wait for each item of the workgroup to finish
	
	// fetch next job
	if(l_id == 0) {
	    job_index = atomic_add(next_job, WORKGROUP_SIZE);
	}
	barrier(CLK_LOCAL_MEM_FENCE);
	
	old_jkl = c.start_ijkl & 0b111111111111111;
    }
}

// AMD kernel
kernel void nqfaf_amd(constant struct constellation *constellation_arr, global uint* jkl_queens_arr, global long *result) {
    const struct constellation c = constellation_arr[get_global_id(0)]; // task for this work item									
    
    // start_jkl_arr contains [6 queens free][5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l] 
    const int start = c.start_ijkl >> 20 & 31;		
    if(start == 69) {
	// if we have a pseudo constellation we do nothing 
	return;
    }

    const int l_id = get_local_id(0); // local thread id within workgroup
    const uint L = 1 << (N-1); // queen at the left border of the board (right border is represented by 1) 	

    // describe the occupancy of the board 
    uint ld = c.ld; // left diagonals, 1 means occupied
    uint rd = c.rd; // right diagonals, 1 means occupied 
    uint col = ~(L-2) ^ c.col; // columns, 1 means occupied
    // for memorizing board-leaving diagonals 
    uint ld_mem = 0;
    uint rd_mem = 0;

    // jkl_queens occupies the diagonals, that go from bottom row to upper right and upper left 
    // and also the left and right column 
    // in row k only L is free and in row l only 1 is free 
    local uint jkl_queens[N];
    if(l_id == 0) {
	for(int i = 0; i < N; i++) {
	    jkl_queens[i] = jkl_queens_arr[get_group_id(0) * N + i];
	}
    }
    barrier(CLK_LOCAL_MEM_FENCE);

    ld &= ~(L >> (((c.start_ijkl >> 5) & 31) - start)); // remove queen k from ld 
    if((c.start_ijkl & 31) != N-1) 
	/* only remove queen k from rd, if no queen in corner (N-1,N-1),
	 * otherwise we continue in row N-1 and find too many solutions
	 */
	rd &= ~(1 << ((c.start_ijkl & 31) - start));

    int row = start;
    ulong solutions = 0;

    /* calculate the occupancy of the first row
     * and place a queen in the first free slot
     * (read the comments in the loop for more information)
     */
    uint free = ~(ld | rd | col | jkl_queens[row]);
    uint queen = -free & free;
    
    // all rows of queens in total contain the queens of the board of one workitem
    local uint queens[WORKGROUP_SIZE][N]; // for remembering the queens for all rows for all boards in the work-group 
    queens[l_id][start] = queen;

    // going forward (setting a queen) or backward (removing a queen)? 										
    int direction = 0;

    // iterative loop representing the recursive setqueen-function 
    // this is the actual solver (via backtracking with Jeff Somers Bit method) 
    // the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed 
    while(row >= start) {
	// while we haven't tried everything 
	if(free) {
	    /* if there are free slots in the current row,
	     * place a queen and go to the next row
	     */
	    if(row == N-2){
		// increase the solutions, if we are in the last row
		solutions++;
		// then go one row back
		direction = 0;
		row--;
		queen = queens[l_id][row];
		ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
		rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
		ld_mem >>= 1;
		rd_mem <<= 1;
	    }
	    else {
		direction = 1;
		queen = -free & free;
		queens[l_id][row] = queen;
		row++;

		// remember the diagonals leaving the board
		ld_mem = ld_mem << 1 | ld >> 31;
		rd_mem = rd_mem >> 1 | rd << 31;
		ld = (ld | queen) << 1;							
		rd = (rd | queen) >> 1;	
	    }
	} 
	else {
	    /* if there aren't any free slots in the current row,
	     * go one row back, remove the last queen and continue with the next free slot
	     */
	    direction = 0;
	    row--;
	    queen = queens[l_id][row]; // recover the queen in order to remove it from ld, rd and col

	    // recover the diagonals that previously left the board
	    ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
	    rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
	    ld_mem >>= 1;
	    rd_mem <<= 1;						
	}
	/* calculate the occupancy of the next row
	 * (free is 1 if a queen can be set at the queens location)
	 * free slots are searched and occupied from right to left
	 */
	free = ~(jkl_queens[row] | ld | rd | col);
	free &= ~(queen + direction-1); 
	col ^= queen;

	// unroll 1 iteration
	if(row < start)
	    break;

	// while we haven't tried everything 
	if(free) {
	    /* if there are free slots in the current row,
	     * place a queen and go to the next row
	     */
	    if(row == N-2){
		// increase the solutions, if we are in the last row
		solutions++;
		// then go one row back
		direction = 0;
		row--;
		queen = queens[l_id][row];
		ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
		rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
		ld_mem >>= 1;
		rd_mem <<= 1;
	    }
	    else {
		direction = 1;
		queen = -free & free;
		queens[l_id][row] = queen;
		row++;

		// remember the diagonals leaving the board
		ld_mem = ld_mem << 1 | ld >> 31;
		rd_mem = rd_mem >> 1 | rd << 31;
		ld = (ld | queen) << 1;							
		rd = (rd | queen) >> 1;	
	    }
	} 
	else {
	    /* if there aren't any free slots in the current row,
	     * go one row back, remove the last queen and continue with the next free slot
	     */
	    direction = 0;
	    row--;
	    queen = queens[l_id][row]; // recover the queen in order to remove it from ld, rd and col

	    // recover the diagonals that previously left the board
	    ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
	    rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
	    ld_mem >>= 1;
	    rd_mem <<= 1;						
	}
	/* calculate the occupancy of the next row
	 * (free is 1 if a queen can be set at the queens location)
	 * free slots are searched and occupied from right to left
	 */
	free = ~(jkl_queens[row] | ld | rd | col);
	free &= ~(queen + direction-1); 
	col ^= queen;
    }
    // write the number of solutions for this work item back to global memory
    result[get_global_id(0)] = solutions;
}

// Intel kernel
kernel void nqfaf_intel(global struct constellation *constellation_arr, global uint* jkl_queens_arr, global long *result) {
    const struct constellation c = constellation_arr[get_global_id(0)]; // task for this work item									
    
    // start_ijkl contains [5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l] 
    const int start = c.start_ijkl >> 20 & 31;		
    if(start == 69) {
	// if we have a pseudo constellation we do nothing 
	return;
    }

    const int l_id = get_local_id(0); // local thread id within workgroup
    const uint L = 1 << (N-1); // queen at the left border of the board (right border is represented by 1) 	

    // describe the occupancy of the board 
    uint ld = c.ld; // left diagonals, 1 means occupied
    uint rd = c.rd; // right diagonals, 1 means occupied 
    uint col = ~(L-2) ^ c.col; // columns, 1 means occupied
    // for memorizing board-leaving diagonals 
    uint ld_mem = 0;
    uint rd_mem = 0;

    // jkl_queens occupies the diagonals, that go from bottom row to upper right and upper left 
    // and also the left and right column 
    // in row k only L is free and in row l only 1 is free 
    local uint jkl_queens[N];
    jkl_queens[l_id % N] = jkl_queens_arr[get_group_id(0) * N + l_id % N];
    barrier(CLK_LOCAL_MEM_FENCE);

    ld &= ~(L >> (((c.start_ijkl >> 5) & 31) - start)); // remove queen k from ld 
    if((c.start_ijkl & 31) != N-1) 
	/* only remove queen k from rd, if no queen in corner (N-1,N-1),
	 * otherwise we continue in row N-1 and find too many solutions
	 */
	rd &= ~(1 << ((c.start_ijkl & 31) - start));

    int row = start;
    ulong solutions = 0;

    /* calculate the occupancy of the first row
     * and place a queen in the first free slot
     * (read the comments in the loop for more information)
     */
    uint free = ~(ld | rd | col | jkl_queens[row]);
    uint queen = -free & free;
    
    // all rows of queens in total contain the queens of the board of one workitem
    local uint queens[WORKGROUP_SIZE][N]; // for remembering the queens for all rows for all boards in the work-group 
    queens[l_id][start] = queen;

    // going forward (setting a queen) or backward (removing a queen)? 										
    int direction = 0;

    // iterative loop representing the recursive setqueen-function 
    // this is the actual solver (via backtracking with Jeff Somers Bit method) 
    // the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed 
    while(row >= start) {
	// while we haven't tried everything 
	if(free) {
	    /* if there are free slots in the current row,
	     * place a queen and go to the next row
	     */
	    if(row == N-2){
		// increase the solutions, if we are in the last row
		solutions++;
		// then go one row back
		direction = 0;
		row--;
		queen = queens[l_id][row];
		ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
		rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
		ld_mem >>= 1;
		rd_mem <<= 1;
	    }
	    else {
		direction = 1;
		queen = -free & free;
		queens[l_id][row] = queen;
		row++;

		// remember the diagonals leaving the board
		ld_mem = ld_mem << 1 | ld >> 31;
		rd_mem = rd_mem >> 1 | rd << 31;
		ld = (ld | queen) << 1;							
		rd = (rd | queen) >> 1;	
	    }
	} 
	else {
	    /* if there aren't any free slots in the current row,
	     * go one row back, remove the last queen and continue with the next free slot
	     */
	    direction = 0;
	    row--;
	    queen = queens[l_id][row]; // recover the queen in order to remove it from ld, rd and col

	    // recover the diagonals that previously left the board
	    ld = ((ld >> 1) | (ld_mem << 31)) & ~queen;
	    rd = ((rd << 1) | (rd_mem >> 31)) & ~queen;
	    ld_mem >>= 1;
	    rd_mem <<= 1;						
	}
	/* calculate the occupancy of the next row
	 * (free is 1 if a queen can be set at the queens location)
	 * free slots are searched and occupied from right to left
	 */
	free = ~(jkl_queens[row] | ld | rd | col);
	free &= ~(queen + direction-1); 
	col ^= queen;
    }
    // write the number of solutions for this work item back to global memory
    result[get_global_id(0)] = solutions;
}
