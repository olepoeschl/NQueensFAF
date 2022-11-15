//	//	//	//	//	//	//	//	//
//	  Explosion Boost 9000		//
//	//	//	//	//	//	//	//	//
__kernel void run(global int *ld_arr, global int *rd_arr, global int *col_mask_arr, global int *start_jkl_arr, global long *result, global int *progress) {

// gpu intern indice
	int g_id = get_global_id(0);							// global thread id
	int l_id = get_local_id(0);								// local thread id within workgroup
	
// variables	

	// for the board
	uint ld = ld_arr[g_id];									// left diagonal
	uint rd = rd_arr[g_id];									// right diagonal
	uint col_mask = ~((1 << N) - 1);					// column and mas
	uint L = 1 << (N-1);									// queen at left border
	
	// start row index - start_jkl_arr contains [11 bits free][5 bits for start][5 bits for j][5 bits for k][5 bits for l]
	int start = start_jkl_arr[g_id] >> 15;
	
	// if we have a pseudo constellation 
	if(start == 69) {
		progress[g_id] = -1;
		return;
	}
	
	// in row k queen at left border, in row l queen at right border
	int k = (start_jkl_arr[g_id] >> 5) & 31;
	int l = start_jkl_arr[g_id] & 31;
	
	// queen in last row at position j
	int j = (start_jkl_arr[g_id] >> 10) & 31;
	
	// jdiag is queen in last row
	uint rdiag = (L >> j) | (L >> (N-1-k));
	uint ldiag = (L >> j) | (L >> l);
	
	// jqueen occupies the diagoals, that go from bottom row to upper right and upper left 
	local uint jqueen[BLOCK_SIZE][N];
	for(int a = N-1;a > 0; a--){
		jqueen[l_id][a] = (ldiag >> N-1-a) | (rdiag << (N-1-a)) | L | 1;
	}
	ldiag = L >> k;
	rdiag = 1 << l;
	for(int a = 0;a < N; a++){
		jqueen[l_id][a] |= (ldiag << a) | (rdiag >> a);
	}
	jqueen[l_id][k] = ~L;
	jqueen[l_id][l] = ~1; 
	
	// col_mask occupies the columns 
	// left and right column are occupied by queen k (1) and l (L) 
	col_mask |= col_mask_arr[g_id];
	
	// initialize current row and solvecounter as 0
	int row = start;
	uint solvecounter = 0;
	
	uint ld_mem = 0;
	uint rd_mem = 0;
	
	// notfree is 1, if queen can not be placed 
	uint free = ~(ld | rd | col_mask | jqueen[l_id][row]);
	
	// temp variable for reducing reads from local array 
	// temp is the current queen in the current row 
	uint temp = -free & free;		
	
	// each row of bits contains the queens of the board of one workitem 
	// local arrays are faster 
	local uint bits[BLOCK_SIZE][N];							// is '1', where a queen will be set; one integer for each line 
	bits[l_id][start] = temp;								// initialize bit as rightmost free space ('0' in notfree)
	
	// other variables										
	uint old_queen = 1;										// previously set queen in solver 
	int direction = 1;										// going forward or backward? 
	
// iterative loop representing the recursive setqueen-function
// this is the actual solver 
	while(row >= start) {									// while we havent tried everything 
		if(free) {											// if there are free slots in the current row 
			direction = 1;
			temp = -free & free;							// this is the next free slot for a queen (searching from the right border) in the current row
			bits[l_id][row] = temp;							// remember the queen 
			row++;											// go to next row 

			ld_mem = ld_mem << 1 | ld >> 31;				// place the queen in the diagonals and shift them 
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | temp) << 1;							
			rd = (rd | temp) >> 1;	
		}
		else {											
			direction = 0;									// if the row is occupied 
			row--;											// one row back
			temp = bits[l_id][row];							// this saves 2 reads from local array	
																									
			ld = ((ld >> 1) | (ld_mem << 31)) & ~temp;		// shift diagonals one back and insert the leaving diagonals 
			rd = ((rd << 1) | (rd_mem >> 31)) & ~temp;
			ld_mem >>= 1;
			rd_mem <<= 1;						
		}
		free = ~(jqueen[l_id][row] | ld | rd | col_mask);	// calculate the occupancy of the next row
		free &= ~(temp + direction-1);
		col_mask ^= temp;									// we do this after calculating notfree in order to not place the same queen again		

		solvecounter += (row == N-1);						// increase the solvecounter, if we are in the last row 
	}
	result[g_id] = solvecounter;							// number of solutions of the work item 
	progress[g_id] = 1;										// progress 1 if done, 0 if not 
}
