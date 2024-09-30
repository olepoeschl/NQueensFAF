package de.nqueensfaf.demo.gui.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Test1 {
    
    public static void main(String[] args) {

	final Kryo kryo = new Kryo();
	kryo.register(Man.class);
	kryo.register(Woman.class);
	kryo.register(AgedHuman.class);
	
//	writeSeparate(kryo);
//	readSeparate(kryo);

	writeHumanRecord(kryo);
	readHumanRecord(kryo);
    }
    
    private static void writeSeparate(Kryo kryo) {
	try (
		Output output1 = new Output(new FileOutputStream("man"));
		Output output2 = new Output(new FileOutputStream("woman"));
		) {
	    kryo.writeClassAndObject(output1, new Man("Eric"));
	    kryo.writeClassAndObject(output2, new Woman("Perry"));
	} catch (KryoException | FileNotFoundException e) {
	    e.printStackTrace();
	}
    }
    
    private static void readSeparate(Kryo kryo) {
	try (
		Input input1 = new Input(new FileInputStream("man"));
		Input input2 = new Input(new FileInputStream("woman"));
		) {
	    Human human1 = (Human) kryo.readClassAndObject(input1);
	    Human human2 = (Human) kryo.readClassAndObject(input2);
	    human1.greet();
	    human2.greet();
	    
	    System.out.println("---");
	    
	    Man man = (Man) human1;
	    Woman woman = (Woman) human2;
	    man.greet();
	    woman.greet();
	} catch (KryoException | FileNotFoundException e) {
	    e.printStackTrace();
	}
    }

    private static void writeHumanRecord(Kryo kryo) {
	try (
		Output output = new Output(new FileOutputStream("aged_human"));
		) {
	    kryo.writeClassAndObject(output, new AgedHuman(new Man("Tom"), 27));
	} catch (KryoException | FileNotFoundException e) {
	    e.printStackTrace();
	}
    }
    
    private static void readHumanRecord(Kryo kryo) {
	try (
		Input input = new Input(new FileInputStream("aged_human"));
		) {
	    AgedHuman agedHuman = (AgedHuman) kryo.readClassAndObject(input);
	    agedHuman.human().greet();
	    
	    System.out.println("---");
	    
	    Man man = (Man) agedHuman.human();
	    man.greet();
	} catch (KryoException | FileNotFoundException e) {
	    e.printStackTrace();
	}
    }
    
    public static interface Human {
	abstract void greet();
    }
    
    public static class Man implements Human {
	private String name;
	public Man() {}
	public Man(String name) {
	    this.name = name;
	}
	@Override
	public void greet() {
	    System.out.println("Hello from Man! name=" + name);
	}
    }
    
    public static class Woman implements Human {
	private String surname;
	public Woman() {}
	public Woman(String surname) {
	    this.surname = surname;
	}
	@Override
	public void greet() {
	    System.out.println("Hello from Woman! surname=" + surname);
	}
    }
    
    public static record AgedHuman(Human human, int age) {}
}
