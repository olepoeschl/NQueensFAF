package de.nqueensfaf.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

class GpuConstellationsGenerator {

	private int N, preQueens = 5, L, mask, LD, RD, counter;
	private int kbit, lbit; 					
	private HashSet<Integer> startConstellations;
	private ArrayList<Integer> jklList, startList;
	ArrayList<Integer> ldList, rdList, colList, startjklList, symList;
	int startConstCount;

	// generate starting constellations 
	void genConstellations(int N, int WORKGROUP_SIZE) {
		// the name says it all
		ldList = new ArrayList<Integer>();
		rdList = new ArrayList<Integer>();
		colList = new ArrayList<Integer>();
		// jklList contains [17 free bits][5 bits for j][5 bits for k][5 bits for l] 
		jklList = new ArrayList<Integer>();
		// sym is or 2 or 4 or 8
		symList = new ArrayList<Integer>();
		// starting (first empty) row 
		startList = new ArrayList<Integer>();

		int ld, rd, col, jkl;
		// queen at left border 
		L = (1 << (N-1));
		// marks the board 
		mask = (L << 1) - 1;

		// set N, halfN half of N rounded up, collection of startConstellations
		this.N = N;
		final int halfN = (N + 1) / 2;
		startConstellations = new HashSet<Integer>();

		// calculating start constellations with the first Queen on square (0,0) (corner) 
		for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
			for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
				// always add the constellation, we can not accidently get symmetric ones 
				startConstellations.add(toijkl(0, j, 0, l));
				
				// empty ld
				ld = 0;
				// rd occupied from queen at top left corner (main diagonal) or l-queen at right border 
				rd = (L >>> 1) | (1 << (l-1));
				// col occupied a t the borders and from queen j in last row 
				col = 1 | L | (L >>> j);
				// diagonals, that are occupied in the last row by the queen j or l 
				// we are going to shift them upwards the board later 
				LD = (L >>> j) | (L >>> l);
				RD = (L >>> j) | 1;
				
				// counter of subconstellations, that arise from setting extra queens 
				counter = 0;
				// this is the queen in row k (0) and l 
				// their diagonals have to be occupied later 
				// we can not do this right now, because in row k, the queen k has to be actually set 
				kbit = (1 << (N-0-1));
				lbit = (1 << l);
				// generate all subconstellations with 5 queens 
				setPreQueens(ld, rd, col, 0, l, 1, 3);
				// jam j and k and l together into one integer 
				jkl = (j << 10) | (0 << 5) | l;
				// jkl and sym are the same for all subconstellations 
				for(int a = 0; a < counter; a++) {
					jklList.add(jkl);
					symList.add(8);
				}
			}
			// j has to be the same value for all workitems within the same workgroup 
			// thus add trash constellations with same j, until workgroup is full 
			while(ldList.size() % WORKGROUP_SIZE != 0) {
				addTrashConstellation(j);
				startConstCount--;
			}
		}

		// calculate starting constellations for no Queens in corners
		// have a look in the loop above for missing explanations 
		for(int j = 1; j < halfN; j++) {						// go through last row
			for(int l = j+1; l < N-1; l++) {					// go through last col
				for(int i = j+1; i < N-1; i++) {				// go through first row
					if(i == N-1-l)								// skip if occupied
						continue;
					for(int k = N-j-2; k > 0; k--) {			// go through first col 
						if(k==i || l == k)						// skip if occupied 
							continue;
						// check, if we already found a symmetric constellation 
						if(!checkRotations(i, j, k, l)) {	
							startConstellations.add(toijkl(i, j, k, l));
							
							// occupy the board corresponding to the queens on the borders of the board 
							ld = (L >>> (i-1)) | (1 << (N-k));
							rd = (L >>> (i+1)) | (1 << (l-1));
							col = 1 | L | (L >>> j) | (L >>> i);
							// occupy diagonals of the queens j k l in the last row 
							// later we are going to shift them upwards the board 
							LD = (L >>> j) | (L >>> l);
							RD = (L >>> j) | (1 << k);
							// this is the queen in row k and l 
							// their diagonals have to be occupied later 
							// we can not do this right now, because in row k, the queen k has to be actually set 
							kbit = (1 << (N-k-1));
							lbit = (1 << l);
							
							// counts all subconstellations 
							counter = 0;
							// generate all subconstellations 
							setPreQueens(ld, rd, col, k, l, 1, 4);
							// jam j and k and l into one integer 
							jkl = (j << 10) | (k << 5) | l;
							// jkl and sym and start are the same for all subconstellations 
							for(int a = 0; a < counter; a++) {
								jklList.add(jkl);
								symList.add(symmetry(toijkl(i, j , k, l)));
							}
						}
					}
				}
			}
			// fill up the workgroup 
			while(ldList.size() % WORKGROUP_SIZE != 0) {
				addTrashConstellation(j);
				startConstCount--;
			}
		}
		// number of constellations (workitems) for the gpu 
		startConstCount += ldList.size();
		// sort them by j and k and l (little bit faster) 
		// we could also directly generate constellations in fitting order 
		sortConstellations();
		startjklList = new ArrayList<Integer>(ldList.size());
		for(int i = 0; i < ldList.size(); i++) {
			startjklList.add((startList.get(i) << 15) | jklList.get(i));
		}
		// for the trash
		jklList = null;
		startList = null;
	}

	// generate subconstellations for each starting constellation with 3 or 4 queens 
	private void setPreQueens(int ld, int rd, int col, int k, int l, int row, int queens) {
		// in row k and l just go further 
		if(row == k || row == l) {
			setPreQueens(ld<<1, rd>>>1, col, k, l, row+1, queens);
			return;
		}
		// add queens until we have preQueens queens 
		// this should be variable for the distributed version and different N 
		if(queens == preQueens) {
			// occupy diagonals from queen k and l, that will end in the left or right border 
			// the following 2 lines may be TRASH 
			ld &= ~(kbit << row);
			rd &= ~(lbit >>> row);
			// make left and right col free 
			col &= ~(1 | L);
			// if k already came, then occupy it on the board 
			if(k < row) {
				rd |= (L >> (row-k));
				col |= L;
			}
			// same for l 
			if(l < row) {
				ld |= (1 << (row-l));
				col |= 1;
			}
			// ad the subconstellations to the list 
			ldList.add(ld);
			rdList.add(rd);
			colList.add(col);
			startList.add(row);
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

	// sort constellations so that as many workgroups as possible have solutions with less divergent branches
	// this can also be done by directly generating the constellations in a different order 
	void sortConstellations() {
		record BoardProperties(int ld, int rd, int col, int start, int jkl, int sym) {
			BoardProperties(int ld, int rd, int col, int start, int jkl, int sym) {
				this.ld = ld;
				this.rd = rd;
				this.col = col;
				this.start = start;
				this.jkl = jkl;
				this.sym = sym;
			}
		}

		int len = ldList.size();
		ArrayList<BoardProperties> list = new ArrayList<BoardProperties>(len);
		for(int i = 0; i < len; i++) {
			list.add(new BoardProperties(ldList.get(i), rdList.get(i), colList.get(i), startList.get(i), jklList.get(i), symList.get(i)));
		}
		Collections.sort(list, new Comparator<BoardProperties>() {
			@Override
			public int compare(BoardProperties o1, BoardProperties o2) {
				int j1 = o1.jkl >> 10, j2 = o2.jkl >> 10;
				int k1 = (o1.jkl >> 5) & 0b00011111, k2 = (o2.jkl >> 5) & 0b00011111;
				int l1 = o1.jkl & 0b00011111, l2 = o2.jkl & 0b00011111;
				if(j1 > j2) {
					return 1;
				} else if(j1 < j2) {
					return -1;
				} else {
					if(k1 > k2) {
						return 1;
					} else if(k1 < k2) {
						return -1;
					} else {
						if(l1 > l2) {
							return 1;
						} else if(l1 < l2) {
							return -1;
						} else {
							return 0;
						}
					}
				}
			}
		});
		// clear the unsorted lists 
		ldList.clear();
		rdList.clear();
		colList.clear();
		startList.clear();
		jklList.clear();
		symList.clear();
		// make the sorted list (same elements in new sorted order) 
		for(int i = 0; i < len; i++) {
			ldList.add(list.get(i).ld);
			rdList.add(list.get(i).rd);
			colList.add(list.get(i).col);
			startList.add(list.get(i).start);
			jklList.add(list.get(i).jkl);
			symList.add(list.get(i).sym);
		}
	}
	
	// create trash constellation to fill up workgroups 
	private void addTrashConstellation(int j) {
		ldList.add((1 << N) - 1);
		rdList.add((1 << N) - 1);
		colList.add((1 << N) - 1);
		startList.add(69);
		jklList.add(j << 10);
		symList.add(0);
	}
	
	public void addTrashConstellation(int j, ArrayList<Integer> ldList, ArrayList<Integer> rdList, ArrayList<Integer> colList, 
			ArrayList<Integer> startjklList, ArrayList<Integer> symList) {
		ldList.add((1 << N) - 1);
		rdList.add((1 << N) - 1);
		colList.add((1 << N) - 1);
		startjklList.add((69 << 15) | (j << 10));
		symList.add(0);
	}

	// helper functions
	// true, if starting constellation rotated by any angle has already been found
	private boolean checkRotations(int i, int j, int k, int l) {
		// rot90
		if(startConstellations.contains(((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i)) 
			return true;

		// rot180
		if(startConstellations.contains(((N-1-j)<<24) + ((N-1-i)<<16) + ((N-1-l)<<8) + N-1-k)) 
			return true;

		// rot270
		if(startConstellations.contains((l<<24) + (k<<16) + ((N-1-i)<<8) + N-1-j)) 
			return true;

		return false;
	}

	// wrap i, j, k and l to one integer using bitwise movement
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}

	// how often does a found solution count for this start constellation
	private int symmetry(int ijkl) {
		if(geti(ijkl) == N-1-getj(ijkl) && getk(ijkl) == N-1-getl(ijkl))		// starting constellation symmetric by rot180?
			if(symmetry90(ijkl))		// even by rot90?
				return 2;
			else
				return 4;
		else
			return 8;					// none of the above?
	}

	private int geti(int ijkl) {
		return ijkl >>> 24;
	}
	private int getj(int ijkl) {
		return (ijkl >>> 16) & 255;
	}
	private int getk(int ijkl) {
		return (ijkl >>> 8) & 255;
	}
	private int getl(int ijkl) {
		return ijkl & 255;
	}

	// true, if starting constellation is symmetric for rot90
	private boolean symmetry90(int ijkl) {
		if(((geti(ijkl) << 24) + (getj(ijkl) << 16) + (getk(ijkl) << 8) + getl(ijkl)) == (((N-1-getk(ijkl))<<24) + ((N-1-getl(ijkl))<<16) + (getj(ijkl)<<8) + geti(ijkl)))
			return true;
		return false;
	}
}
