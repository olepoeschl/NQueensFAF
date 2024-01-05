package de.nqueensfaf.impl;

import java.util.HashSet;

class SolverUtils {

    private int n;
    
    SolverUtils() {
    }
    
    void setN(int n) {
	this.n = n;
    }
    
    // true, if starting constellation rotated by any angle has already been found
    boolean checkRotations(HashSet<Integer> ijklList, int i, int j, int k, int l) {
	// rot90
	if (ijklList.contains(((n - 1 - k) << 15) + ((n - 1 - l) << 10) + (j << 5) + i))
	    return true;

	// rot180
	if (ijklList.contains(((n - 1 - j) << 15) + ((n - 1 - i) << 10) + ((n - 1 - l) << 5) + n - 1 - k))
	    return true;

	// rot270
	if (ijklList.contains((l << 15) + (k << 10) + ((n - 1 - i) << 5) + n - 1 - j))
	    return true;

	return false;
    }

    // i, j, k, l to ijkl and functions to get specific entry
    int toijkl(int i, int j, int k, int l) {
	return (i << 15) + (j << 10) + (k << 5) + l;
    }

    int geti(int ijkl) {
	return ijkl >> 15;
    }

    int getj(int ijkl) {
	return (ijkl >> 10) & 31;
    }

    int getk(int ijkl) {
	return (ijkl >> 5) & 31;
    }

    int getl(int ijkl) {
	return ijkl & 31;
    }
    
    int getjkl(int ijkl) {
	return ijkl & 0b111111111111111;
    }

    // rotate and mirror board, so that the queen closest to a corner is on the
    // right side of the last row
    int jasmin(int ijkl) {
	int min = Math.min(getj(ijkl), n - 1 - getj(ijkl)), arg = 0;

	if (Math.min(geti(ijkl), n - 1 - geti(ijkl)) < min) {
	    arg = 2;
	    min = Math.min(geti(ijkl), n - 1 - geti(ijkl));
	}
	if (Math.min(getk(ijkl), n - 1 - getk(ijkl)) < min) {
	    arg = 3;
	    min = Math.min(getk(ijkl), n - 1 - getk(ijkl));
	}
	if (Math.min(getl(ijkl), n - 1 - getl(ijkl)) < min) {
	    arg = 1;
	    min = Math.min(getl(ijkl), n - 1 - getl(ijkl));
	}

	for (int i = 0; i < arg; i++) {
	    ijkl = rot90(ijkl);
	}

	if (getj(ijkl) < n - 1 - getj(ijkl))
	    ijkl = mirvert(ijkl);

	return ijkl;
    }

    // mirror left-right
    int mirvert(int ijkl) {
	return toijkl(n - 1 - geti(ijkl), n - 1 - getj(ijkl), getl(ijkl), getk(ijkl));
    }

    // rotate 90 degrees clockwise
    int rot90(int ijkl) {
	return ((n - 1 - getk(ijkl)) << 15) + ((n - 1 - getl(ijkl)) << 10) + (getj(ijkl) << 5) + geti(ijkl);
    }

    // helper functions for doing the math
    // for symmetry stuff and working with ijkl
    // true, if starting constellation is symmetric for rot90
    boolean symmetry90(int ijkl) {
	if (((geti(ijkl) << 15) + (getj(ijkl) << 10) + (getk(ijkl) << 5) + getl(ijkl)) == (((n - 1 - getk(ijkl)) << 15)
		+ ((n - 1 - getl(ijkl)) << 10) + (getj(ijkl) << 5) + geti(ijkl)))
	    return true;
	return false;
    }

    // how often does a found solution count for this start constellation
    int symmetry(int ijkl) {
	if (geti(ijkl) == n - 1 - getj(ijkl) && getk(ijkl) == n - 1 - getl(ijkl)) // starting constellation symmetric by rot180?
	    if (symmetry90(ijkl)) // even by rot90?
		return 2;
	    else
		return 4;
	else
	    return 8; // none of the above?
    }
}
