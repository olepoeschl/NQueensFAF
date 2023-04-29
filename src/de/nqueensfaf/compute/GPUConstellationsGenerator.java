package de.nqueensfaf.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import de.nqueensfaf.files.Constellation;

class GPUConstellationsGenerator {

	private int N, preQueens, L, mask, LD, RD, counter, numberOfValidConstellations = 0;		
	private HashSet<Integer> ijklList;
	private ArrayList<Constellation> constellations;
	
	// generate starting constellations 
	void genConstellations(int N, int WORKGROUP_SIZE, int preQueens) {
		int ld, rd, col, ijkl, currentSize;
		// queen at left border 
		L = (1 << (N-1));
		// marks the board 
		mask = (L << 1) - 1;

		// set N, halfN half of N rounded up, collection of startConstellations
		this.N = N;
		final int halfN = (N + 1) / 2;
		ijklList = new HashSet<Integer>();
		
		// set number of preset queens
		this.preQueens = preQueens;

		// calculating start constellations with one Queen on the corner square (N-1,N-1)
		for(int k = 1; k < N-2; k++) {						// j is idx of Queen in last row				
			for(int i = k+1; i < N-1; i++) {				// l is idx of Queen in last col
				// always add the constellation, we can not accidently get symmetric ones 
				ijklList.add(toijkl(i, N-1, k, N-1));
				
				// occupation of ld, rd according to row 1 
				// queens i and k
				ld = (L >>> (k-1)) | (L >>> (i-1));
				// queens i and l
				rd = (L >>> (i+1)) | (L >>> 1);
				// left border from k, right border from l, also bits i and j from the corresponding queens
				col = 1 | L | (L >>> i);
				
				// diagonals, that are occupied in the last row by the queen j or l 
				// we are going to shift them upwards the board later 
				// from queen j and l (same, since queen is in the corner) 
				LD = 1;
				// from queen k and l 
				RD = 1 | (1 << k);
				
				// counter of subconstellations, that arise from setting extra queens 
				counter = 0;
				
				// generate all subconstellations with 5 queens 
				setPreQueens(ld, rd, col, k, 0, 1, 3);
				// jam j and k and l together into one integer 
				ijkl = toijkl(i, (N-1) << 10, k, N-1);
				
				currentSize = constellations.size();
				
				// ijkl and sym are the same for all subconstellations 
				for(int a = 0; a < counter; a++) {
					constellations.get(currentSize - a - 1).setStartijkl(constellations.get(currentSize - a - 1).getStartijkl() | ijkl);
				}
			}
			// j has to be the same value for all workitems within the same workgroup 
			// thus add trash constellations with same j, until workgroup is full 
			while(constellations.size() % WORKGROUP_SIZE != 0) {
				addTrashConstellation(toijkl(N, N, N, N));
				numberOfValidConstellations--;
			}
		}

		// calculate starting constellations for no Queens in corners
		// have a look in the loop above for missing explanations 
		for(int j = 1; j < halfN; j++) {						// go through last row
			for(int l = j+1; l < N-1; l++) {					// go through last col
				for(int k = N-j-2; k > 0; k--) {			// go through first col 
					if(k == l)						// skip if occupied 
						continue;
					for(int i = j+1; i < N-1; i++) {				// go through first row
						if(i == N-1-l || i == k)								// skip if occupied
							continue;
						// check, if we already found a symmetric constellation 
						if(!checkRotations(i, j, k, l)) {	
							ijklList.add(toijkl(i, j, k, l));
							
							// occupy the board corresponding to the queens on the borders of the board 
							ld = (L >>> (i-1)) | (1 << (N-k));
							rd = (L >>> (i+1)) | (1 << (l-1));
							col = 1 | L | (L >>> j) | (L >>> i);
							// occupy diagonals of the queens j k l in the last row 
							// later we are going to shift them upwards the board 
							LD = (L >>> j) | (L >>> l);
							RD = (L >>> j) | (1 << k);
							
							// counts all subconstellations 
							counter = 0;
							// generate all subconstellations 
							setPreQueens(ld, rd, col, k, l, 1, 4);
							// jam j and k and l into one integer 
							ijkl = toijkl(i, j, k, l);
							
							currentSize = constellations.size();
							
							// jkl and sym and start are the same for all subconstellations 
							for(int a = 0; a < counter; a++) {
								constellations.get(currentSize - a - 1).setStartijkl(constellations.get(currentSize - a - 1).getStartijkl() | ijkl);
							}
						}
					}
					// fill up the workgroup 
					while(constellations.size() % WORKGROUP_SIZE != 0) {
						addTrashConstellation(toijkl(N, N, N, N));
						numberOfValidConstellations--;
					}
				}
			}
		}
		numberOfValidConstellations += constellations.size();
	}

	// generate subconstellations for each starting constellation with 3 or 4 queens 
	private void setPreQueens(int ld, int rd, int col, int k, int l, int row, int queens) {
		// in row k and l just go further 
		if(row == k || row == l) {
			setPreQueens(ld<<1, rd>>>1, col, k, l, row+1, queens);
			return;
		}
		// add queens until we have preQueens queens 
		if(queens == preQueens) {		
			// add the subconstellations to the list
			constellations.add(new Constellation(ld, rd, col, row << 20, -1));
			counter++;
			return;
		}
		// if not done or row k or l, just place queens and occupy the board and go further 
		else {
			int free = ~(ld | rd | col | (LD >>> (N-1-row)) | (RD << (N-1-row))) & mask;
			int bit;

			while(free > 0) {
				bit = free & (-free);
				free -= bit; 
				setPreQueens((ld|bit) << 1, (rd|bit) >>> 1, col|bit, k, l, row+1, queens+1);
			}
		}
	}

	// create trash constellation to fill up workgroups 
	void addTrashConstellation(int ijkl) {
		constellations.add(new Constellation((1 << N) - 1, (1 << N) - 1, (1 << N) - 1, (69 << 20) | ijkl, -2));
	}
	
	// helper functions
	// true, if starting constellation rotated by any angle has already been found
	private boolean checkRotations(int i, int j, int k, int l) {
		// rot90
		if(ijklList.contains(((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i)) 
			return true;

		// rot180
		if(ijklList.contains(((N-1-j)<<24) + ((N-1-i)<<16) + ((N-1-l)<<8) + N-1-k)) 
			return true;

		// rot270
		if(ijklList.contains((l<<24) + (k<<16) + ((N-1-i)<<8) + N-1-j)) 
			return true;

		return false;
	}

	// wrap i, j, k and l to one integer using bitwise movement
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}

	// true, if starting constellation is symmetric for rot90

	// sort constellations so that as many workgroups as possible have solutions
	// with less divergent branches
	// this can also be done by directly generating the constellations in a
	// different order
	void sortConstellations(ArrayList<Constellation> constellations) {
		Collections.sort(constellations, new Comparator<Constellation>() {
			@Override
			public int compare(Constellation o1, Constellation o2) {
				int o1jkl = o1.getStartijkl() & ((1 << 15) - 1);
				int o2jkl = o2.getStartijkl() & ((1 << 15) - 1);
				if (o1jkl > o2jkl)
					return 1;
				else if (o1jkl < o2jkl)
					return -1;
				else
					return 0;
			}
		});
	}

	// getters and setters
	ArrayList<Constellation> getConstellations(){
		return constellations;
	}
	
	int getNumberOfValidConstellations() {
		return numberOfValidConstellations;
	}
}