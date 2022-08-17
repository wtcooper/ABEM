package us.fl.state.fwc.abem.environ;

import java.util.ArrayList;

import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public interface EnviroCell {

	public abstract Geometry getGeom(); 
	public abstract void setGeom(Geometry geom);
	public abstract double getRugosity();
	public abstract void setRugosity(double rugosity);
	public abstract double getDepth();
	public abstract void setDepth(double depth);
	public abstract int getHabType();
	public abstract void setHabType(int habType);
	public abstract int getDepthLayer(double depth);
	
	public abstract double getLayerCentroid(int layer);
	
	public abstract double getRoughness();
	
	public abstract void setRoughness(double roughness);

	public abstract Int3D getIndex();
	
	public abstract void setIndex(Int3D index);

	public abstract Coordinate getCentroidCoord();
	
	public abstract void setNumLayers(int numLayers);
	
	/**Returns a lookup table of cells within the neighborhood that can be moved
	 * to, where the index key is a
	 * @return
	 */
	public abstract ArrayList<EnviroCell> getReachableCells(int searchRadius);
	
	public abstract void setNumRecruits(String classname, int year, double numRecruits); 
	
	/**Holds the average numbers (abundance, total biomass, and SSB) for this grid cell
	 * 
	 * @param classname
	 * @param year
	 * @param SSB
	 */
	public abstract void setAvgNumbers(String classname, int year, double[] data);
	
	public abstract double getAvgAbundance(String classname, int year);

	public abstract double getAvgBiomass(String classname, int year);

	public abstract double getAvgSSB(String classname, int year);

	public abstract double getAbundance(String classname);
	
	public abstract double getBiomass(String classname);

	public abstract double getSSB(String classname);

	public double getNumSettlers(String classname);

	public double getNumAdults(String classname);

	public abstract void setTEP(String classname, int year, double TEP); 
	
	
	public abstract double getNumRecruits(String classname, int year);

	public abstract double getTEP(String classname, int year);

	public abstract boolean isActive();
	
	public abstract void setIsActive(boolean hasFish);
	
}
