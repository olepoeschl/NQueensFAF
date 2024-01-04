package de.nqueensfaf;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;

import de.nqueensfaf.impl.Constellation;
import de.nqueensfaf.persistence.SolverState;

public class Constants {

    public static final Kryo kryo = new Kryo();
    static {
	kryo.register(SolverState.class);
	kryo.register(Constellation.class);
	kryo.register(ArrayList.class);
    }
    
}
