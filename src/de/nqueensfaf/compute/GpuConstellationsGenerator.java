package de.nqueensfaf.compute;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

class GpuConstellationsGenerator {

	private int N, L, mask, LD, RD, counter, k;
	private int kbit, lbit; 	// belegt de diagoanle auf der später k bzw. l dame stehen soll
	private int[] bits;
	private int[][] klcounter;
	private HashSet<Integer> startConstellations;
	ArrayDeque<Integer> ldList, rdList, colList, LDList, RDList, klList, symList, startList;

	// calculate occupancy of starting row
	void genConstellations(int N) {
		ldList = new ArrayDeque<Integer>();
		rdList = new ArrayDeque<Integer>();
		colList = new ArrayDeque<Integer>();
		LDList = new ArrayDeque<Integer>();
		RDList = new ArrayDeque<Integer>();
		klList = new ArrayDeque<Integer>();
		symList = new ArrayDeque<Integer>();
		startList = new ArrayDeque<Integer>();

		int ld, rd, col, kl;
		L = (1 << (N-1));
		mask = (L << 1) - 1;
		bits = new int[N];
		klcounter = new int[N][N];

		// set N, halfN half of N rounded up, collection of startConstellations
		this.N = N;
		final int halfN = (N + 1) / 2;
		startConstellations = new HashSet<Integer>();

		// calculating start constellations with the first Queen on square (0,0)
		for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
			for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
				startConstellations.add(toijkl(0, j, 0, l));

				ld = 0;
				rd = (L >>> 1) | (1 << (l-1));
				col = 1 | L | (L >>> j);
				LD = (L >>> j) | (L >>> l);
				RD = (L >>> j) | 1;

				bits[0] = L;
				bits[l] = 1;
				bits[N-1] = L >>> j;

				counter = 0;
				kbit = (1 << (N-0-1));
				lbit = (1 << l);
				sq5(ld, rd, col, 0, l, 1, 3);
				kl = (k << 8) | l;

				LD = (L >>> j);
				RD = (L >>> j);

				for(int a = 0; a < counter; a++) {
					LDList.add(LD);
					RDList.add(RD);
					klList.add(kl);
					symList.add(8);
					klcounter[k][l]++;
				}
			}
		}

		// calculate starting constellations for no Queens in corners
		// look above for if missing explanation
		for(int k = 1; k < halfN; k++) {						// go through first col
			for(int l = k+1; l < N-1; l++) {					// go through last col
				for(int i = k+1; i < N-1; i++) {				// go through first row
					if(i == N-1-l)								// skip if occupied
						continue;
					for(int j = N-k-2; j > 0; j--) {			// go through last row
						if(j==i || l == j)
							continue;

						if(!checkRotations(i, j, k, l)) {		// if no rotation-symmetric starting constellation already found
							startConstellations.add(toijkl(i, j, k, l));

							ld = (L >>> (i-1)) | (1 << (N-k));
							rd = (L >>> (i+1)) | (1 << (l-1));
							col = 1 | L | (L >>> j) | (L >>> i);
							LD = (L >>> j) | (L >>> l);
							RD = (L >>> j) | (1 << k);

							bits[0] = L >>> i;
							bits[N-1] = L >>> j;
							bits[k] = L;
							bits[l] = 1;

							kbit = (1 << (N-k-1));
							lbit = (1 << l);

							counter = 0;
							sq5(ld, rd, col, k, l, 1, 4);
							kl = (k << 8) | l;

							RD = (L >>> j);
							LD = (L >>> j);

							for(int a = 0; a < counter; a++) {
								LDList.add(LD);
								RDList.add(RD);
								klList.add(kl);
								symList.add(symmetry(toijkl(i, j , k, l)));
								klcounter[k][l]++;
							}
						}
					}
				}
			}
		}
		sortConstellations();
	}

	// presolver
	private void sq5(int ld, int rd, int col, int k, int l, int row, int queens) {
		if(queens == 5) {
			ld &= ~(kbit << row);
			rd &= ~(lbit >>> row);
			col &= ~(1 | L);
			if(k < row) {
				rd |= (L >> (row-k));
				col |= L;
			}
			if(l < row) {
				ld |= (1 << (row-l));
				col |= 1;
			}
			ldList.add(ld);
			rdList.add(rd);
			colList.add(col);
			startList.add(row);
			counter++;
			return;
		}
		if(row == k || row == l) {
			sq5(ld<<1, rd>>>1, col, k, l, row+1, queens);
			return;
		}
		else {
			int free = ~(ld | rd | col | (LD >>> (N-1-row)) | (RD << (N-1-row))) & mask;
			int bit;

			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				sq5((ld|bit) << 1, (rd|bit) >>> 1, col|bit, k, l, row+1, queens+1);
			}
		}
	}

	// sort constellations so that as many workgroups as possible have solutions with less divergent branches
	void sortConstellations() {
		record BoardProperties(int ld, int rd, int col, int start, int kl, int LD, int RD, int sym) {
			BoardProperties(int ld, int rd, int col, int start, int kl, int LD, int RD, int sym) {
				this.ld = ld;
				this.rd = rd;
				this.col = col;
				this.start = start;
				this.kl = kl;
				this.LD = LD;
				this.RD = RD;
				this.sym = sym;
			}
		}

		int len = ldList.size();
		ArrayList<BoardProperties> list = new ArrayList<BoardProperties>(len);
		for(int i = 0; i < len; i++) {
			list.add(new BoardProperties(ldList.removeFirst(), rdList.removeFirst(), colList.removeFirst(), startList.removeFirst(), klList.removeFirst(), LDList.removeFirst(), RDList.removeFirst(), symList.removeFirst()));
		}
		Collections.sort(list, new Comparator<BoardProperties>() {
			@Override
			public int compare(BoardProperties o1, BoardProperties o2) {
				if(o1.start > o2.start) {
					return 1;
				} else if(o1.start < o2.start) {
					return -1;
				} else {
					if((o1.kl >> 8) > (o2.kl >> 8)) {
						return 1;
					} else if((o1.kl >> 8) < (o2.kl >> 8)) {
						return -1;
					}
					return 0;
				}
			}
		});
		for(int i = 0; i < len; i++) {
			ldList.add(list.get(i).ld);
			rdList.add(list.get(i).rd);
			colList.add(list.get(i).col);
			startList.add(list.get(i).start);
			klList.add(list.get(i).kl);
			LDList.add(list.get(i).LD);
			RDList.add(list.get(i).RD);
			symList.add(list.get(i).sym);
		}
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
	
	// getters
	int getStartConstCount() {
		return ldList.size();
	}
}
