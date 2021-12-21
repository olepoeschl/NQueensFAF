//	//	//	//	//	//	//	//	//
//	  Explosion Boost 9000		//
//	//	//	//	//	//	//	//	//
__kernel void run(global int *ld_arr, global int *rd_arr, global int *col_mask_arr, global int *start_jkl_arr, global uint *result, global int *progress) {
	
// gpu intern indice
	int g_id = get_global_id(0);												// global thread id
	int l_id = get_local_id(0);												// local thread id within workgroup
	
// variables	
	// for the board
	uint ld = ld_arr[g_id];															// left diagonal
	uint rd = rd_arr[g_id];															// right diagonal
	uint col_mask = ~((1 << N) - 1) | 1;											// column and mask
	
	// wir shiften das ja in der zeile immer (im solver), also muss es hier einfach in der 0-ten zeile die diagonale der dame belegen EASY
	uint L = 1 << (N-1);
	
	// start index
	int start = start_jkl_arr[g_id] >> 15;
	if(start == 69) {
		progress[g_id] = -1;
		return;
	}
	// LD and RD - occupancy of board-entering diagonals due to the queens from the start constellation
	// uint jdiag = L >> ((start_jkl_arr[g_id] >> 10) & 31);
	
	// k and l - row indice where a queen is already set and we have to go to the next row
	int k = (start_jkl_arr[g_id] >> 5) & 31;
	int l = start_jkl_arr[g_id] & 31;
	int j = (start_jkl_arr[g_id] >> 10) & 31;
	uint jdiag = (L >> j);
	local uint jqueen[N];
	for(int a = N-1;a > 0; a--){
		jqueen[a] = (jdiag >> N-1-a) | (jdiag << (N-1-a));
	}
	// init col_mask
	col_mask |= col_mask_arr[g_id] | L | 1;
	
	// to memorize diagonals leaving the board at a certain row
	uint ld_mem = 0;															
	uint rd_mem = 0;
	
	// initialize current row and solvecounter as 0
	int row = start;
	uint solvecounter = 0;
	
	ld |= (L >> k) << row;
	rd |= (1 << l) >> row;
	
	// init klguard
	uint notfree = ld | rd | col_mask | jqueen[row];
	if(row == k)
		notfree = ~L;
	else if (row == l)
		notfree = ~1U;
	
	// temp variable
	uint temp = (notfree + 1) & ~notfree;		// for reducing array reads
	
	// local (faster) array containing positions of the queens of each row 
	// for all boards of the workgroup
	local uint bits[BLOCK_SIZE][N];									// is '1', where a queen will be set; one integer for each line 
	bits[l_id][start] = temp;							 			// initialize bit as rightmost free space ('0' in notfree)
	
	// other variables											
	uint old_queen = 1;
	int direction = 1;
	
	// iterative loop representing the recursive setqueen-function
	while(row >= start) {
		if(temp) {																	// if bit is on board
			col_mask |= temp;															// new col
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | temp) << 1;													// shift diagonals to next line
			rd = (rd | temp) >> 1;													
				
			row++;
			old_queen = direction = 1;
		}
		else {
			row--;																	// one row back
			temp = bits[l_id][row];													// this saves 2 reads from local array
			temp *= (row != k && row != l);
			ld = ((ld >> 1) | (ld_mem << 31)) & ~temp;								// shift diagonals one row up
			rd = ((rd << 1) | (rd_mem >> 31)) & ~temp;								// if there was a diagonal leaving the board in the line before, occupy it again
			ld_mem >>= 1;															// shift those as well
			rd_mem <<= 1;
			
			direction = 0;
			old_queen = temp;
		}
		solvecounter += (row == N-1);
		
		notfree = jqueen[row] | ld | rd | col_mask;							// calculate occupancy of next row
		if(!direction)
			col_mask &= ~temp;
		
		temp = (notfree + old_queen) & ~notfree;
		if(row == k)
			temp = L * direction;
		if(row == l)
			temp = direction;
			
		bits[l_id][row] = temp;
	}
	result[g_id] = solvecounter;
	progress[g_id] = 1;
}
