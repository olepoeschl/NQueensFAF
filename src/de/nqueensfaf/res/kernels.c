//	//	//	//	//	//	//	//	//
//	  Explosion Boost 9000		//
//	//	//	//	//	//	//	//	//
__kernel void run(global int *ld_arr, global int *rd_arr, global int *col_mask_arr, global int *start_jkl_arr, global long *result, global int *progress) {

// gpu intern indice
	int g_id = get_global_id(0);							// global thread id
	int l_id = get_local_id(0);								// local thread id within workgroup
	
// variables	

	// for the board
	ulong ld = ld_arr[g_id];									// left diagonal
	ulong rd = rd_arr[g_id];									// right diagonal
	uint col_mask = ~((1 << N) - 1) | 1;					// column and mas
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
	uint jdiag = (L >> j);
	
	// jqueen occupies the diagoals, that go from bottom row to upper right and upper left 
	local uint jqueen[N];
	for(int a = N-1;a > 0; a--){
		jqueen[a] = (jdiag >> N-1-a) | (jdiag << (N-1-a));
	}
	
	// col_mask occupies the columns 
	// left and right column are occupied by queen k (1) and l (L) 
	col_mask |= col_mask_arr[g_id] | L | 1;
	
	// initialize current row and solvecounter as 0
	int row = start;
	uint solvecounter = 0;
	
	// 
	ld |= (L >> k) << row;
	rd |= (1 << l) >> row;
	
	// notfree is 1, if queen can not be placed 
	uint notfree = ld | rd | col_mask | jqueen[row];
	
	rd <<= 32; 
	
	// temp variable for reducing reads from local array 
	// temp is the current queen in the current row 
	uint temp = (notfree + 1) & ~notfree;	
	ulong templ = temp; 	
	
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
		if(g_id == 420)
			printf("\n %i %i %i %i %i %u", row,j,k,l,start, col_mask);
		if(~notfree) {											// if there were free places for a queen in the previous loop 
			if(direction)
				old_queen = 1;
			temp = (notfree + old_queen) & ~notfree;			// this is the free slot for a queen (searching from the right border) in the current row 
			direction = 1;
			if(row==k)
				temp = L;
			if(row == l)
				temp = 1; 
				
			bits[l_id][row] = temp;								// remember the queen 
			col_mask |= temp;								// place the queen in  the column 
			templ = temp; 
			ld = (ld | templ) << 1;							// place the queen in the diagonals and shift them 
			rd = (rd | (templ<<32)) >> 1;													
				
			row++;											// increase row counter 
															// oldqueen is 1, if we go forward 
			solvecounter += (row == N-1);						// increase the solvecounter, if we are in the last row 
			notfree = jqueen[row] | ld | (rd>>32) | col_mask;			// calculate the occupancy of the next row							// we do this after calculating notfree, to not place the same queen again				
				
			if(row == k){										// in row k the queen is at the left border (L)				// it is zero, if we are going backwards (to go one row further back) 
				notfree = ~L;
			}
			if(row == l){										// same goes for row l 
				notfree = ~1;
			}
		}
		else {												// if we couldnt place a queen 
			row--;											// one row back
			temp = bits[l_id][row];							// this saves 2 reads from local array

															// in row k and l we are not allowed to do this 
			templ = temp; 
			ld = (ld >> 1) & ~templ;						// shift diagonals one back and insert the diagonals, 
			rd = (rd << 1) & ~(templ<<32);					// that left the board in this line 
			
			direction = 0;									// direction is zero, if we are removing a queen 
			old_queen = temp;								// the old queen is the one, that we just removed 
															// for row k or l, it is 0 
			notfree = jqueen[row] | ld | (rd>>32) | col_mask;			// calculate the occupancy of the next row
			
			if(row == k || row == l)
				notfree = ~0; 
			else
				col_mask &= ~temp;								// we do this after calculating notfree, to not place the same queen again		
		}
		
	}
	result[g_id] = solvecounter;							// number of solutions of the work item 
	progress[g_id] = 1;										// progress 1 if done, 0 if not 
}
