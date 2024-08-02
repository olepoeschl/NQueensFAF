package de.nqueensfaf.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static de.nqueensfaf.impl.Utils.*;

public class ConstellationsGenerator {

    private ArrayList<Constellation> subConstellations;
    private int n, presetQueens;
    private int L, mask;
    private HashSet<Integer> ijklList;
    
    public ConstellationsGenerator(int n) {
	this.n = n;
	
	L = (1 << (n - 1));
	mask = (L << 1) - 1;
    }
    
    public ArrayList<ImmutableConstellation> generate(int presetQueens){
	var constellations = new ArrayList<ImmutableConstellation>();
	generate(presetQueens, constellation -> {
	    constellations.add(constellation);
	    return true;
	});
	return constellations;
    }
    
    public void generate(int presetQueens, Predicate<ImmutableConstellation> constellationConsumer){
	if(presetQueens < 4)
	    throw new IllegalArgumentException("could not initialize ConstellationsGenerator: presetQueens must be >=4");
	this.presetQueens = presetQueens;
	
	generateIjkls();
	
	subConstellations = new ArrayList<Constellation>();

	int i, j, k, l, ld, rd, col;
	for (int ijkl : ijklList) {
	    ijkl = jAsMin(n, ijkl);
	    i = geti(ijkl);
	    j = getj(ijkl);
	    k = getk(ijkl);
	    l = getl(ijkl);

	    // fill up the board with preQueens queens and generate corresponding variables
	    // ld, rd, col, start_queens_ijkl for each constellation
	    // occupy the board corresponding to the queens on the borders of the board
	    // we are starting in the first row that can be free, namely row 1
	    ld = (L >>> (i - 1)) | (1 << (n - k));
	    rd = (L >>> (i + 1)) | (1 << (l - 1));
	    col = 1 | L | (L >>> i) | (L >>> j);
	    
	    // LD and RD (see placePresetQueens()) are used to
	    // occupy diagonals of the queens j k l in the last row
	    // later we are going to shift them upwards the board

	    // generate all subconstellations
	    placePresetQueens(ijkl, ld, rd, col, 1, j == n - 1 ? 3 : 4);
	    
	    // jkl and sym and start are the same for all subconstellations
	    for (int a = 0; a < subConstellations.size(); a++) {
		var c = subConstellations.get(a);
		c.setStartIjkl(c.getStartIjkl() | ijkl);

		if( ! constellationConsumer.test((ImmutableConstellation) c))
		    return; // stop generating if something goes wrong in the callback
	    }
	    
	    subConstellations.clear();
	}
    }

    public ArrayList<ImmutableConstellation> generateSubConstellations(List<ImmutableConstellation> baseConstellations, int extraQueens){
	if(baseConstellations.size() == 0)
	    throw new IllegalArgumentException("could not generate sub constellations: base constellations must contain min 1 constellation");
	if(extraQueens <= 0)
	    throw new IllegalArgumentException("could not generate sub constellations: extraQueens must be >0");
	
	// number of currently placed queens is the same for all base constellations
	var c0 = baseConstellations.get(0);
	int queens = c0.getStart();
	if(getk(c0.getStartIjkl()) < c0.getStart())
	    queens--;
	if(getl(c0.getStartIjkl()) < c0.getStart())
	    queens--;
	presetQueens = queens + extraQueens;

	var constellations = new ArrayList<ImmutableConstellation>();
	subConstellations = new ArrayList<Constellation>();
	
	for(var bc : baseConstellations) {
	    queens = bc.getStart();
	    if(getk(bc.getStartIjkl()) < bc.getStart())
		queens--;
	    if(getl(bc.getStartIjkl()) < bc.getStart())
		queens--;
	    
	    placePresetQueens(bc.getIjkl(), bc.getLd(), bc.getRd(), bc.getCol(), bc.getStart(), queens); // from row start to row presetQueens
	    
	    // jkl and sym and start are the same for all sub constellations
	    for (int a = 0; a < subConstellations.size(); a++) {
		var c = subConstellations.get(a);
		c.setStartIjkl(c.getStartIjkl() | bc.getIjkl());
		constellations.add(c);
	    }
	    
	    subConstellations.clear();
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
    private void placePresetQueens(int ijkl, int ld, int rd, int col, int row, int queens) {
	// in row k and l just go further
	if (row == getk(ijkl) || row == getl(ijkl)) {
	    placePresetQueens(ijkl, ld << 1, rd >>> 1, col, row + 1, queens);
	    return;
	}
	// add queens until we have preQueens queens
	if (queens == presetQueens) {
	    // add the subconstellations to the list
	    subConstellations.add(new Constellation(ld, rd, col, row << 20));
	    return;
	}
	// if not done or row k or l, just place queens and occupy the board and go
	// further
	else {
	    int free = (~(ld | rd | col | (getLD(ijkl, L) >>> (n - 1 - row)) | (getRD(ijkl, L) << (n - 1 - row)))) & mask;
	    int bit;

	    while (free > 0) {
		bit = free & (-free);
		free -= bit;
		placePresetQueens(ijkl, (ld | bit) << 1, (rd | bit) >>> 1, col | bit, row + 1, queens + 1);
	    }
	}
    }
}
