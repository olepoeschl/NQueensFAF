//	//	//	//	//	//	//	//	//
// kernel for nqueens-solving	//
//	//	//	//	//	//	//	//	//


// not cancelable, faster
__kernel void run(global int *ld_arr, global int *rd_arr, global int *col_mask_arr, global int *LD_arr, global int *RD_arr, global int *kl_arr, global int *start_arr, global uint *result, global int *progress) {
	
// gpu intern indice
	const int g_id = get_global_id(0);												// global thread id
	const short l_id = get_local_id(0);												// local thread id within workgroup
	
// variables	
	// for the board
	uint ld = ld_arr[g_id];															// left diagonal
	uint rd = rd_arr[g_id];															// right diagonal
	uint col_mask = ~((1 << N) - 1) | 1;														// col_maskumn and mask
	
	// k and l - row indice where a queen is already set and we have to go to the next row
	const short k = kl_arr[g_id] >> 8;
	const short l = kl_arr[g_id] & 255;
	
	// LD and RD - occupancy of board-entering diagonals due to the queens from the start constellation
	const uint jdiag = LD_arr[g_id] & RD_arr[g_id];
	
	// wir shiften das ja in der zeile immer (im solver), aslo muss es hier einfach in der 0-ten zeile die diagonale der dame belegen EASY
	const uint L = 1 << (N-1);
	
	// init col_mask
	col_mask |= col_mask_arr[g_id] | L | 1;
	
	// start index
	const short start = start_arr[g_id];
	
	// to memorize diagonals leaving the board at a certain row
	uint ld_mem = 0;															
	uint rd_mem = 0;
	
	// initialize current row and solvecounter as 0
	int row = start;
	uint solvecounter = 0;
	
	ld |= (L >> k) << row;
	rd |= (1 << l) >> row;
	
	// init klguard
	uint notfree = ld | rd | col_mask | (jdiag >> N-1-row) | (jdiag << (N-1-row));
	if(row == k)
		notfree = ~L;
	else if (row == l)
		notfree = ~1U;
	
	// temp variable
	uint temp = (notfree + 1) & ~notfree;		// for reducing array reads
	
	// local (faster) array containing positions of the queens of each row 
	// for all boards of the workgroup
	local uint bits[N][BLOCK_SIZE];													// is '1', where a queen will be set; one integer for each line 
	bits[start][l_id] = temp;							 			// initialize bit as rightmost free space ('0' in notfree)
	
	// other variables											
	uint diff = 1;
	int direction = 1;
	
	// iterative loop representing the recursive setqueen-function
	while(row >= start){
		direction = (temp != 0);
		row += (direction) ? 1 : -1;
		if(direction) {																	// if bit is on board
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | temp) << 1;													// shift diagonals to next line
			rd = (rd | temp) >> 1;
		}
		else {
			temp = bits[row][l_id];													// this saves 2 reads from local array
			temp *= (row != k && row != l);
			ld = ((ld >> 1) | (ld_mem << 31)) & ~temp;								// shift diagonals one row up
			rd = ((rd << 1) | (rd_mem >> 31)) & ~temp;								// if there was a diagonal leaving the board in the line before, occupy it again
			ld_mem >>= 1;															// shift those as well
			rd_mem <<= 1;
		}
		solvecounter += (row == N-1);
		
		diff = (direction) ? 1 : temp;
		col_mask |= temp;
		notfree = (jdiag >> N-1-row) | (jdiag << (N-1-row)) | ld | rd | col_mask;							// calculate occupancy of next row
		col_mask = (direction) ? col_mask : col_mask & ~temp;
		
		temp = (row == k || row == l) ? direction : ((notfree + diff) & ~notfree);
		temp = (row == k && direction) ? L : temp;

		bits[row][l_id] = temp;
		// unroll 1 iteration
		if(row < start)
			break;
		direction = (temp != 0);
		row += (direction) ? 1 : -1;
		if(direction) {																	// if bit is on board
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | temp) << 1;													// shift diagonals to next line
			rd = (rd | temp) >> 1;
		}
		else {
			temp = bits[row][l_id];													// this saves 2 reads from local array
			temp *= (row != k && row != l);
			ld = ((ld >> 1) | (ld_mem << 31)) & ~temp;								// shift diagonals one row up
			rd = ((rd << 1) | (rd_mem >> 31)) & ~temp;								// if there was a diagonal leaving the board in the line before, occupy it again
			ld_mem >>= 1;															// shift those as well
			rd_mem <<= 1;
		}
		solvecounter += (row == N-1);
		
		diff = (direction) ? 1 : temp;
		col_mask |= temp;
		notfree = (jdiag >> N-1-row) | (jdiag << (N-1-row)) | ld | rd | col_mask;							// calculate occupancy of next row
		col_mask = (direction) ? col_mask : col_mask & ~temp;

		temp = (row == k || row == l) ? direction : ((notfree + diff) & ~notfree);
		temp = (row == k && direction) ? L : temp;

		bits[row][l_id] = temp;
	}
	result[g_id] = solvecounter;
	progress[g_id] = 1;
}

// cancelable, slower
__kernel void runCancelable(global int *ld_arr, global int *rd_arr, global int *col_mask_arr, global int *LD_arr, global int *RD_arr, global int *kl_arr, global int *start_arr, global uint *result, global int *progress, global int *signal) {

// gpu intern indice
	const int g_id = get_global_id(0);												// global thread id
	const short l_id = get_local_id(0);												// local thread id within workgroup

// variables
	// for the board
	uint ld = ld_arr[g_id];															// left diagonal
	uint rd = rd_arr[g_id];															// right diagonal
	uint col_mask = ~((1 << N) - 1) | 1;														// col_maskumn and mask

	// k and l - row indice where a queen is already set and we have to go to the next row
	const short k = kl_arr[g_id] >> 8;
	const short l = kl_arr[g_id] & 255;

	// LD and RD - occupancy of board-entering diagonals due to the queens from the start constellation
	const uint jdiag = LD_arr[g_id] & RD_arr[g_id];

	// wir shiften das ja in der zeile immer (im solver), aslo muss es hier einfach in der 0-ten zeile die diagonale der dame belegen EASY
	const uint L = 1 << (N-1);

	// init col_mask
	col_mask |= col_mask_arr[g_id] | L | 1;

	// start index
	const short start = start_arr[g_id];

	// to memorize diagonals leaving the board at a certain row
	uint ld_mem = 0;
	uint rd_mem = 0;

	// initialize current row and solvecounter as 0
	int row = start;
	uint solvecounter = 0;

	ld |= (L >> k) << row;
	rd |= (1 << l) >> row;

	// init klguard
	uint notfree = ld | rd | col_mask | (jdiag >> N-1-row) | (jdiag << (N-1-row));
	if(row == k)
		notfree = ~L;
	else if (row == l)
		notfree = ~1U;

	// temp variable
	uint temp = (notfree + 1) & ~notfree;		// for reducing array reads

	// local (faster) array containing positions of the queens of each row
	// for all boards of the workgroup
	local uint bits[N][BLOCK_SIZE];													// is '1', where a queen will be set; one integer for each line
	bits[start][l_id] = temp;							 			// initialize bit as rightmost free space ('0' in notfree)

	// other variables
	uint diff = 1;
	int direction = 1;
	int tmpSignal = 0;

	// iterative loop representing the recursive setqueen-function
	while(row >= start && tmpSignal == 0){
		direction = (temp != 0);
		row += (direction) ? 1 : -1;
		if(direction) {																	// if bit is on board
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | temp) << 1;													// shift diagonals to next line
			rd = (rd | temp) >> 1;
		}
		else {
			temp = bits[row][l_id];													// this saves 2 reads from local array
			temp *= (row != k && row != l);
			ld = ((ld >> 1) | (ld_mem << 31)) & ~temp;								// shift diagonals one row up
			rd = ((rd << 1) | (rd_mem >> 31)) & ~temp;								// if there was a diagonal leaving the board in the line before, occupy it again
			ld_mem >>= 1;															// shift those as well
			rd_mem <<= 1;
		}
		solvecounter += (row == N-1);

		diff = (direction) ? 1 : temp;
		col_mask |= temp;
		notfree = (jdiag >> N-1-row) | (jdiag << (N-1-row)) | ld | rd | col_mask;							// calculate occupancy of next row
		col_mask = (direction) ? col_mask : col_mask & ~temp;

		temp = (row == k || row == l) ? direction : ((notfree + diff) & ~notfree);
		temp = (row == k && direction) ? L : temp;

		bits[row][l_id] = temp;
		// unroll 1 iteration
		if(row < start)
			break;
		direction = (temp != 0);
		row += (direction) ? 1 : -1;
		if(direction) {																	// if bit is on board
			ld_mem = ld_mem << 1 | ld >> 31;
			rd_mem = rd_mem >> 1 | rd << 31;
			ld = (ld | temp) << 1;													// shift diagonals to next line
			rd = (rd | temp) >> 1;
		}
		else {
			temp = bits[row][l_id];													// this saves 2 reads from local array
			temp *= (row != k && row != l);
			ld = ((ld >> 1) | (ld_mem << 31)) & ~temp;								// shift diagonals one row up
			rd = ((rd << 1) | (rd_mem >> 31)) & ~temp;								// if there was a diagonal leaving the board in the line before, occupy it again
			ld_mem >>= 1;															// shift those as well
			rd_mem <<= 1;
		}
		solvecounter += (row == N-1);

		diff = (direction) ? 1 : temp;
		col_mask |= temp;
		notfree = (jdiag >> N-1-row) | (jdiag << (N-1-row)) | ld | rd | col_mask;							// calculate occupancy of next row
		col_mask = (direction) ? col_mask : col_mask & ~temp;

		temp = (row == k || row == l) ? direction : ((notfree + diff) & ~notfree);
		temp = (row == k && direction) ? L : temp;

		bits[row][l_id] = temp;

		// check for signal
		tmpSignal = *signal;
	}
	result[g_id] = solvecounter;

	// dont set the progress indicator if the process got canceled
	if(tmpSignal == 0)
		progress[g_id] = 1;
}
