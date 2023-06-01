package de.nqueensfaf.compute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.nqueensfaf.data.Constellation;

class GPUConstellationsGenerator {

	private int N, preQueens, L, mask, LD, RD, counter;		
	private HashSet<Integer> ijklList;
	private ArrayList<Constellation> constellations;
	
	// generates all tasks
	public void genConstellations(int N, int preQueens) {
		int ld, rd, col, ijkl, currentSize;
		// queen at left border 
		L = (1 << (N-1));
		// marks the board 
		mask = (L << 1) - 1;

		// set N, halfN half of N rounded up, collection of startConstellations
		this.N = N;
		final int halfN = (N + 1) / 2;
		ijklList = new HashSet<Integer>();
		constellations = new ArrayList<Constellation>();
		
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
				ijkl = toijkl(i, N-1, k, N-1);
				
				currentSize = constellations.size();
				
				// ijkl and sym are the same for all subconstellations 
				for(int a = 0; a < counter; a++) {
					int start = constellations.get(currentSize - a - 1).getStartijkl();
					constellations.get(currentSize - a - 1).setStartijkl(start | ijkl);
				}
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
								int start = constellations.get(currentSize - a - 1).getStartijkl();
								constellations.get(currentSize - a - 1).setStartijkl(start | ijkl);
							}
						}
					}
				}
			}
		}
	}
	
	// TODO
	public List<Constellation> setMorePreQueens(List<Constellation> constellations, int moreQueens) {
		return null;
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
			constellations.add(new Constellation(-1, ld, rd, col, row << 20, -1));
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

	// helper functions
	// true, if starting constellation rotated by any angle has already been found
	private boolean checkRotations(int i, int j, int k, int l) {
		// rot90
		if(ijklList.contains(((N-1-k)<<15) + ((N-1-l)<<10) + (j<<5) + i)) 
			return true;

		// rot180
		if(ijklList.contains(((N-1-j)<<15) + ((N-1-i)<<10) + ((N-1-l)<<5) + N-1-k)) 
			return true;

		// rot270
		if(ijklList.contains((l<<15) + (k<<10) + ((N-1-i)<<5) + N-1-j)) 
			return true;

		return false;
	}

	// wrap i, j, k and l to one integer using bitwise movement
	private int toijkl(int i, int j, int k, int l) {
		return (i<<15) + (j<<10) + (k<<5) + l;
	}

	// getters and setters
	ArrayList<Constellation> getConstellations(){
		return constellations;
	}
}