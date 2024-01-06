package de.nqueensfaf.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Consumer;

import static de.nqueensfaf.impl.SolverUtils.*;

public class ConstellationsGenerator {

    private ArrayList<Constellation> constellations;
    private int n, presetQueens;
    private int L, mask, LD, RD, subConstellationsCounter;
    private HashSet<Integer> ijklList;
    
    public ConstellationsGenerator(int n, int presetQueens) {
	this.n = n;
	this.presetQueens = presetQueens;
	
	L = (1 << (n - 1));
	mask = (L << 1) - 1;
    }
    
    public ArrayList<Constellation> generate(){
	return generate(null);
    }
    
    public ArrayList<Constellation> generate(Consumer<Constellation> constellationConsumer){
	generateIjkls();
	
	constellations = new ArrayList<Constellation>();

	int i, j, k, l, ld, rd, col, currentSize = 0;
	for (int ijkl : ijklList) {
	    ijkl = jAsMin(n, ijkl);
	    i = geti(ijkl);
	    j = getj(ijkl);
	    k = getk(ijkl);
	    l = getl(ijkl);

	    // counts all subconstellations
	    subConstellationsCounter = 0;

	    // fill up the board with preQueens queens and generate corresponding variables
	    // ld, rd, col, start_queens_ijkl for each constellation
	    // occupy the board corresponding to the queens on the borders of the board
	    // we are starting in the first row that can be free, namely row 1
	    ld = (L >>> (i - 1)) | (1 << (n - k));
	    rd = (L >>> (i + 1)) | (1 << (l - 1));
	    col = 1 | L | (L >>> i) | (L >>> j);
	    // occupy diagonals of the queens j k l in the last row
	    // later we are going to shift them upwards the board
	    LD = (L >>> j) | (L >>> l);
	    RD = (L >>> j) | (1 << k);

	    // generate all subconstellations
	    placePresetQueens(ld, rd, col, k, l, 1, j == n - 1 ? 3 : 4);
	    currentSize = constellations.size();
	    
	    // jkl and sym and start are the same for all subconstellations
	    for (int a = 0; a < subConstellationsCounter; a++) {
		var c = constellations.get(currentSize - a - 1);
		c.setStartIjkl(c.getStartIjkl() | ijkl);
		c.setId(currentSize - a - 1);

		if(constellationConsumer != null)
		    constellationConsumer.accept(constellations.get(currentSize - a - 1));
	    }
	}

	return constellations;
    }

    public ArrayList<Constellation> generateSubConstellations(ArrayList<Constellation> baseConstellations){
	constellations = new ArrayList<Constellation>();
	
	int currentSize;
	
	for(var bc : baseConstellations) {
	    // counts all subconstellations
	    subConstellationsCounter = 0;
	    
	    placePresetQueens(bc.getLd(), bc.getRd(), bc.getCol(), getk(bc.getIjkl()), getl(bc.getIjkl()), 
		    bc.getStartIjkl() >> 20, bc.getStartIjkl() >> 20); // from row start to row presetQueens
	    currentSize = constellations.size();
	    
	    // jkl and sym and start are the same for all subconstellations
	    for (int a = 0; a < subConstellationsCounter; a++) {
		var c = constellations.get(currentSize - a - 1);
		c.setStartIjkl(c.getStartIjkl() | bc.getIjkl());
		c.setId(bc.getId());
	    }
	}
	
	return constellations;
    }
    
    private void generateIjkls() {
	ijklList = new HashSet<Integer>();

	// half of n rounded up
	final int halfN = (n + 1) / 2;

	// calculate starting constellations for no Queens in corners
	for (int j = 1; j < halfN; j++) { // go through last row
	    for (int l = j + 1; l < n - 1; l++) { // go through last col
		for (int k = n - j - 2; k > 0; k--) { // go through first col
		    if (k == l) // skip if occupied
			continue;
		    for (int i = j + 1; i < n - 1; i++) { // go through first row
			if (i == n - 1 - l || i == k) // skip if occupied
			    continue;

			if (!checkRotations(n, ijklList, i, j, k, l)) { 
			    // if no rotation-symmetric starting constellation is found
			    ijklList.add(toIjkl(i, j, k, l));
			}
		    }
		}
	    }
	}
	
	// calculating start constellations with the first Queen on the corner square
	// (0,0)
	for (int k = 1; k < n - 2; k++) { // j is idx of Queen in last row
	    for (int i = k + 1; i < n - 1; i++) { // l is idx of Queen in last col
		// always add the constellation, we can not accidently get symmetric ones
		ijklList.add(toIjkl(i, n - 1, k, n - 1));
	    }
	}
    }

    // generate sub constellations for each starting constellation
    private void placePresetQueens(int ld, int rd, int col, int k, int l, int row, int queens) {
	// in row k and l just go further
	if (row == k || row == l) {
	    placePresetQueens(ld << 1, rd >>> 1, col, k, l, row + 1, queens);
	    return;
	}
	// add queens until we have preQueens queens
	if (queens == presetQueens) {
	    // add the subconstellations to the list
	    constellations.add(new Constellation(0, ld, rd, col, row << 20, -1));
	    subConstellationsCounter++;
	    return;
	}
	// if not done or row k or l, just place queens and occupy the board and go
	// further
	else {
	    int free = (~(ld | rd | col | (LD >>> (n - 1 - row)) | (RD << (n - 1 - row)))) & mask;
	    int bit;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		placePresetQueens((ld | bit) << 1, (rd | bit) >>> 1, col | bit, k, l, row + 1, queens + 1);
	    }
	}
    }
}
