package us.fl.state.fwc.abem.environ;

import java.util.ArrayList;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


/**	This is the interface for whatever grid is being used for the environment variables (e.g., currents, salinity, temperature, seagrass area, etc); 
 * the grid will usually be the grid of the hydrodynamic model, since the input data will be in that shape.
 * 
 * The interface will read in a shapefile at initialization of the grid boundaries, store them at JTS geometry features, and convert an agent's coordinate to the grid index number so that can look up values.   
 * 
 * @author wade.cooper
 *
 */
public interface EnviroGrid {

	
	/**	Initializes the grid
	 * 
	 * @param s
	 * @param filename
	 */
	public abstract void initialize(Scheduler scheduler); 

	
	/**
	 * Get's the double value for a particular variable of a grid cell
	 * @param variable 
	 * @param cellIndex
	 * @return
	 */
	public double getValue(String variable, Int3D cellIndex); 

	/**
	 * Returns the grid cell index for a cell at a given coordinate (note: can be no overlap of cells)
	 * @param coord
	 * @return PointLoc
	 */
	public abstract Int3D getGridIndex(Coordinate coord); 
	
	/**
	 * Returns a list of PointLoc's (i.e., cell index's) for all cells that intersect a given geometry
	 * @param searchGeometry
	 * @return
	 */
	public abstract ArrayList<Int3D> getCellsWithinRange(Geometry searchGeometry); 
	
	/**
	 * Returns a Dodecagon geometry (12-sided polygon) with a midpoint at midCoord and a given radius
	 * @param midCoord
	 * @param radius
	 * @return FastTable<PointLoc>
	 */
	public abstract Geometry getSearchBuffer(Coordinate midCoord, Double radius); 
	
	/**
	 * Returns the geometry of the interesection of two geometrys
	 * @param geom1
	 * @param geom2
	 * @return Geometry of intersection
	 */
	public abstract Geometry getSearchIntersection(Geometry geom1, Geometry geom2);
	
	/**
	 * Get's a FastTable list of random Coordinates within a geometry search buffer 
	 * @param numPoints
	 * @param searchBuffer
	 * @param cellIndex
	 * @return FastTable list
	 */
	public abstract ArrayList<Coordinate> getRandomPoints(int numPoints, Geometry searchBuffer, Int3D cellIndex); 
	
	/**
	 * Get's a random Coordinate within a grid cell
	 * @param cellIndex
	 * @return Coordinate
	 */
	public abstract Coordinate getRandomPoint(Int3D cellIndex); 

	
	/**
	 * Get's the geometry of the grid cell at the index 
	 * @param point: PointLoc girdcell index
	 * @return Geometry  
	 */
	public abstract Geometry getGridCellGeometry(Int3D index);


	/**
	 * Iterates across all features in shapefile and updates the grid cells to the appropriate year
	 * @param year
	 */
	public abstract void updateCells(String year); 
	
	/**Returns an environment cell given the cell index
	 * 
	 * @param cellIndex
	 * @return
	 */
	public abstract EnviroCell getGridCell(Int3D cellIndex); 
	
	/**Returns the width of a grid cell
	 * 
	 * @return
	 */
	public abstract double getCellWidth();
	
	
	/**Returns the height of a grid cell
	 * 
	 * @return
	 */
	public abstract double getCellHeight();
	
	/**
	 * Returns the area of the grid cell
	 * @return
	 */
	public abstract double getCellArea();
	
	
	/**Set's the reachable cells lookup table
	 * 
	 * @return
	 */
	public abstract void setReachableCells(String filename);
	
	/**Returns a GridCoverage2D for display, given the variable name (i.e., 
	 * abundance, biomass, SSB, recruitment, TEP), the classname, and the year
	 *  
	 * @param variable
	 * @param classname
	 * @param year
	 * @return
	 */
	public Object[] getGridCov(String variable, String classname, int year);
	
	public double getMaxAbundance();

	public double getMaxBiomass() ;

	public double getMaxSSB();
	
	public double getMaxRecruitment();

	public double getMaxTEP();

}
