package us.fl.state.fwc.abem.dispersal.bolts;


public interface Barrier extends Habitat {
	
	public void checkReflect(Particle p);
	public Barrier clone();
	public void closeConnections();
}

