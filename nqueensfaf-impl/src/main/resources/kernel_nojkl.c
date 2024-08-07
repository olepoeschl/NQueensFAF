
void printBits(size_t const size, void const * const ptr)
{
    unsigned char *b = (unsigned char*) ptr;
    unsigned char byte;
    int i, j;

    for (i = size-1; i >= 0; i--) {
        for (j = 7; j >= 0; j--) {
            byte = (b[i] >> j) & 1;
            printf("%u ", byte);
        }
    }
    printf("\n");
}

struct constellation {
    int ld;
    int rd;
    int col;
    int start_ijkl;
};

kernel void nqfaf(global struct constellation *constellation_arr, global long *result) {
        const int l_id = get_local_id(0);  			// local thread id within workgroup
    
        const struct constellation c = constellation_arr[get_global_id(0)];
    
	// variables
	uint L = 1 << (N-1);				// queen at the left border of the board (right border is represented by 1)
	// start_jkl_arr contains [6 queens free][5 queens for start][5 queens for i][5 queens for j][5 queens for k][5 queens for l]
	int start_jkl = c.start_ijkl;
	int start = (start_jkl >> 20) & 31;
	if(start == 69) {				// if we have a pseudo constellation we do nothing
		return;
	}
	int j = (start_jkl >> 10) & 31;	// queen in last row at position j
	int k = (start_jkl >> 5) & 31;	// in row k queen at left border, in row l queen at right border
	int l = start_jkl & 31;

	uint ld = c.ld;
	uint rd = c.rd;
	uint col = ~(L-2) ^ c.col;

	// the part that is different from the default kernel
	local uint jkl_queens[N];
	uint ldiagbot = (L >> j) | (L >> l);
	uint rdiagbot = (L >> j) | (L >> (N-1-k));
	uint ldiagtop = L >> k;
	uint rdiagtop = 1 << l;
	for(int a = 0;a < N; a++){
		jkl_queens[a] = (a==k)*(~L) + (a==l)*(~1) + (a!=k&&a!=l)*((ldiagbot >> (N-1-a)) | (rdiagbot << (N-1-a)) | (ldiagtop << a) | (rdiagtop >> a) | L | 1);
	}

	int row = start;
	ulong solutions = 0;

	uint free = ~(col | jkl_queens[row] | ld | rd);

	ulong queen2 = -free & free;
	uint queen = queen2;

	ld <<= 20;
	rd <<= 20;

	ulong LL = 1L << (20 + N - 1);

//	topdiags from k and l
	ld |= (1L << (N-1-k+start+20));
	rd |= (LL >> (N-1-l+start));

//	botdiags from j
	ld |= (LL >> (j + N - 1 - start));
	rd |= (1L << (20 + N - 1 - j + N - 1 - start));

	local uint2 queens[WORKGROUP_SIZE][N];
	queens[l_id][start].x = queen;
	queens[l_id][start].y = free;

	// iterative loop representing the recursive setqueen-function
	// this is the actual solver (via backtracking with Jeff Somers Bit method)
	// the structure is slightly complicated since we have to take into account the queens at the border, that have already been placed
	while(row >= start) {				// while we haven't tried everything
		if(free) {					// if there are free slots in the current row
			if(row == N-2)	{				// increase the solutions, if we are in the last row
				solutions++;
				free = 0;
			}
			else{
				queen = queen2 = -free & free;				// this is the next free slot for a queen (searching from the right border) in the current row
				queens[l_id][row].x = queen;
				queens[l_id][row].y = free ^ queen;			// remember the queen
				row++;						// increase row counter

				ld = (ld | (queen2 << 20)) << 1;
				rd = (rd | (queen2 << 20)) >> 1;
				col ^= queen;

				if (row == k)
					free = L;
				else if(row == l && row < N-1)
					free = 1;
				else
					free = ~(jkl_queens[row] | col | (ld >> 20) | (rd >> 20));
			}
		}
		else {						// if the row is completely occupied
			row--;						// decrease row counter
			queen = queen2 = queens[l_id][row].x;			// recover the queen in order to remove it
			free = queens[l_id][row].y;

			ld = (ld >> 1) ^ (queen2 << 20);
			rd = (rd << 1) ^ (queen2 << 20);

			if(row != k && row != l)
				col ^= queen;
		}
	}
	result[get_global_id(0)] = solutions;			// number of solutions of the work item
}
