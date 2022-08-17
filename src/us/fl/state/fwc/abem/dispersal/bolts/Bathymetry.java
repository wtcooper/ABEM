package us.fl.state.fwc.abem.dispersal.bolts;

public interface Bathymetry {
	
	public double getDepth(double x, double y);
	public Bathymetry clone();

}

