package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javolution.util.FastTable;
import us.fl.state.fwc.util.Int3D;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.index.strtree.STRtree;

/**   
 * Creates grid cells with set geometries for an EFDC Grid, using the corners.inp file to define the geometry.  
 * Can then use these geometries for calculating various other properties for other
 * files.  E.g., can get bathymetry, roughness, veg type for the dxdy.inp file from netCDF data 
 * on these other properies.  See DXDYFormatter.java.
 *  
 * @author wade.cooper
 *
 */
public class EFDCGrid {

	private int seed;
	private MersenneTwister m; 
	private Uniform uniform; 

	private GeometryFactory gf;
	private STRtree spatialIndex;

	private HashMap <Int3D, EFDCCell> gridCells; 
	//private Scheduler scheduler; 

	private FastTable<Int3D> cellsWithinRange; // = new FastTable<Point3D>(); 
	private FastTable<Coordinate> randomPoints;// = new FastTable<Coordinate>();

	String filename; 

	private int numLayers; 
	
	private File inFile; // input file reader

	public EFDCGrid(){
		//empty constructor
	}

	public EFDCGrid(String filename, boolean autoInitialize){
		this.filename = filename; 
		if (autoInitialize) initialize(filename);
	}


	public void initialize(String filename) {

		seed = (int) System.currentTimeMillis();
		m= new MersenneTwister(seed); 
		uniform = new Uniform(0,1,m); 

		gf = new GeometryFactory();
		spatialIndex = new STRtree();

		gridCells = new HashMap <Int3D, EFDCCell> (); 
		//private Scheduler scheduler; 


		
		try {

			int L = 2; 
			//first use a Scanner to get each line
			inFile = new File(filename);  
			Scanner scanner = new Scanner(inFile);
			int counter = 0; 
			while ( scanner.hasNextLine() ){
				Scanner lineScanner = new Scanner(scanner.nextLine()); 
				//lineScanner.useDelimiter(" *");
				if ( counter>1 && lineScanner.hasNext() ){

					//I    J        DX        DY      DEPTH     ELEV     ZROUGH      TYPE

					int I = lineScanner.nextInt();
					int J = lineScanner.nextInt();
					double x1 = lineScanner.nextDouble(); 
					double y1 = lineScanner.nextDouble(); 
					double x2 = lineScanner.nextDouble(); 
					double y2 = lineScanner.nextDouble(); 
					double x3 = lineScanner.nextDouble(); 
					double y3 = lineScanner.nextDouble(); 
					double x4 = lineScanner.nextDouble(); 
					double y4 = lineScanner.nextDouble(); 

					Int3D index = new Int3D(I, J); 
					Coordinate[] coords = new Coordinate[5];
					for (int i=0; i<coords.length; i++){
						coords[i] = new Coordinate(0,0,0); 
					}
					coords[0].x = x1;
					coords[0].y = y1; 
					coords[1].x = x2;
					coords[1].y = y2; 
					coords[2].x = x3;
					coords[2].y = y3; 
					coords[3].x = x4;
					coords[3].y = y4; 
					coords[4].x = x1; // need to close it
					coords[4].y = y1; // need to close it


					Geometry newGeometry = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 

					EFDCCell cell = new EFDCCell(); 
					cell.setGeom(newGeometry); 
					cell.setIndex(index);
					cell.setL(L); 
					
					spatialIndex.insert(newGeometry.getEnvelopeInternal(), index);
					
					gridCells.put(index, cell); // a fast map of the geometries for each PointLoc index.  Note: I use both the spatialIndex and gridCells to avoid having to deal with the SimpleFeature's directly
					L++; 
					//System.out.println("inputting EFDC Grid, " + I + "\t" + J); 
				}// end lineScanner 
				lineScanner.close();
				counter++; 
			} // end file scanner
			scanner.close();


			System.out.println("done initiazlizing cells.  No of cells: " + gridCells.size()); 



		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	
	
	
	public void setOpenBoundCells(String boundCellFilename){
		//Read in an ASCI file which simply has I \t J values for the boundary cells
		try {
			
		BufferedReader reader = new BufferedReader(new FileReader(new File(boundCellFilename)));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			String tokens[] = line.split("\t"); //split("[ ]+"); 
			Int3D index = new Int3D();
			index.x = Integer.parseInt(tokens[0]);
			index.y = Integer.parseInt(tokens[1]);
			
			gridCells.get(index).setOpenBoundCell(true); 
		}

		reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	/**Sets the cell depth for each of the EFDC cells.
	 *	Note: accepts the location of the dxdy.inp as the input file,
	 *and the number of depth layers used in the model.
	 * 
	 * @param file
	 * @param numDepthLayers
	 */
	public void setCellSizeAndDepth(String file, int numDepthLayers){

		this.numLayers = numDepthLayers; 
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			/* First line of the data file is the header */
			for (int i=0; i<4; i++){
				reader.readLine();
			}

			//int counter = 0; 
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				//String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				int I = Integer.parseInt(tokens[1]); 
				int J = Integer.parseInt(tokens[2]); 
				double dx = Double.parseDouble(tokens[3]);
				double dy = Double.parseDouble(tokens[4]); 
				double depth = Double.parseDouble(tokens[5]);
				
				Int3D index = new Int3D(I, J); 
				gridCells.get(index).setDx(dx);
				gridCells.get(index).setDy(dy); 
				gridCells.get(index).setDepth(depth);
				gridCells.get(index).setNumLayers(numDepthLayers); 
				
			}
			
			System.out.println("done setting dx, dy, and depth for EFDC cells.  No of cells: " + gridCells.size()); 
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}






	/**	Returns the index of the grid, just x and y as a point.  When getting data, will need to use the Agent's depth to get appropriate netCDF location
	 * 
	 */

	@SuppressWarnings("unchecked")
	public Int3D getGridIndex(Coordinate coord) {

		Geometry newGeometry = (Geometry) gf.createPoint(coord); 
		List<Int3D> hits = spatialIndex.query(newGeometry.getEnvelopeInternal());

		if (hits.size() == 0) {
			return null; 			
			//System.out.println("you passed an invalid Coordinate in FLEMGrid.getGridIndex(); exiting program"); 
			//System.exit(1);
		}
		Int3D index = null; 
		for (int i = 0; i < hits.size(); i++) {
			index = hits.get(i);
			Geometry geom = (Geometry) gridCells.get(index).getGeom(); 
			if (newGeometry.intersects(geom)) { 
				return index;
			}
/*			else {
				System.out.println("spatial query didn't work -- no geometries intersected coordinate"); 
				System.exit(1);
			}
			
*/
		}	
		// if didn't intersect, return null
		return null; 

	}


	/**
	 * Will return a HashMap of PointLoc's (I,J) indexed on the L index key, i.e., <L, PointLoc(I,J)>
	 * @return HashMap<Integer, PointLoc>
	 */
	public HashMap<Integer, Int3D> getGridIndexMap(){
		HashMap<Integer, Int3D> gridIndexes = new HashMap<Integer, Int3D>(); 

		Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {
			Int3D index = it.next();
			EFDCCell cell = gridCells.get(index); 
			int L = cell.getL(); 
			gridIndexes.put(L, index); 
		}
		return gridIndexes; 
	}



	public Geometry getSearchBuffer(Coordinate midCoord, Double radius){
		// (1) Get a polygon for a circle within search radius
		Coordinate[] coords = new Coordinate[13]; 
		for (int i=0; i<coords.length; i++){
			coords[i] = new Coordinate(0,0,0); 
		}
		// set the first coordinate in the coords array to the point at 0 angle
		coords[0].x = midCoord.x+radius;
		coords[0].y = midCoord.y; 

		double angleIncrement = (Math.PI*2)/coords.length; // in radians, this is angle between each polygon point of circle, based on the total number of circle points defined in Sampler
		double angle = angleIncrement; // start at first increment

		for (int i=1; i<coords.length; i++){
			coords[i].x = midCoord.x + (Math.cos(angle)*radius); 
			coords[i].y = midCoord.y + (Math.sin(angle)*radius); 
			angle += angleIncrement; 
		}

		coords[12].x = coords[0].x;
		coords[12].y = coords[0].y;


		Geometry newGeometry = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 

		return newGeometry; 
	}



	/**	Get's all the environment cells within the search buffer.  This represents the 1st tier of habitat selection where an animal chooses its preferred environment (i.e., based on temperature, salinity, seagrass cover, etc). 
	 * 
	 */

	@SuppressWarnings("unchecked")
	public FastTable<Int3D> getCellsWithinRange(Geometry searchGeometry) {

		cellsWithinRange.clear();  


		//	(2) Get all the grid cells within that 
		List<Int3D> hits = spatialIndex.query(searchGeometry.getEnvelopeInternal());

		if (hits.size() == 0) {
			System.out.println("you passed an invalid Coordinate in FLEMGrid.getGridIndex(); exiting program"); 
			System.exit(1);
		}
		Int3D index = null; 
		for (int i = 0; i < hits.size(); i++) {
			index = hits.get(i);
			Geometry geom = (Geometry) gridCells.get(index).getGeom(); 
			if (searchGeometry.intersects(geom)) { 
				cellsWithinRange.add(index); 
			}
		}	

		return cellsWithinRange;
	}


	public Geometry getSearchIntersection(Geometry geom1, Geometry geom2){
		return geom1.intersection(geom2); 
	}


	/**	Gets a list of random point coordinates at the intersection the search radius and the environment grid cell in which to search.  
	 * These points represent the 2nd tier of habitat selection, where an animal will choose a specific habitat type within a preferred environment.  
	 * 
	 * @param numPoints
	 * @param cellIndex
	 * @return
	 */
	public FastTable<Coordinate> getRandomPoints(int numPoints, Geometry searchBuffer, Int3D cellIndex){
		randomPoints.clear();

		Geometry searchGeometry = getSearchIntersection(searchBuffer, gridCells.get(cellIndex).getGeom()); 
		Envelope env = searchGeometry.getEnvelopeInternal();

		int n = 0;
		while (n < numPoints) {
			Coordinate c = new Coordinate();
			c.x = uniform.nextDoubleFromTo(0, 1)* env.getWidth() + env.getMinX();
			c.y = uniform.nextDoubleFromTo(0, 1)* env.getHeight() + env.getMinY();
			Point p = gf.createPoint(c);

			if (searchGeometry.contains(p)) {
				randomPoints.add(c); 
				n++; 
			}
		}
		return randomPoints; 
	}

	/**	Gets a list of random point coordinates at the intersection the search radius and the environment grid cell in which to search.  
	 * These points represent the 2nd tier of habitat selection, where an animal will choose a specific habitat type within a preferred environment.  
	 * 
	 * @param numPoints
	 * @param cellIndex
	 * @return
	 */
	public List<Coordinate> getRandomPointsWithinCell(int numPoints, Int3D cellIndex){
		List<Coordinate> randomPoints = new ArrayList<Coordinate>(); 

		Geometry searchGeometry = gridCells.get(cellIndex).getGeom(); 
		Envelope env = searchGeometry.getEnvelopeInternal();

		int n = 0;
		while (n < numPoints) {
			Coordinate c = new Coordinate();
			c.x = uniform.nextDoubleFromTo(0, 1)* env.getWidth() + env.getMinX();
			c.y = uniform.nextDoubleFromTo(0, 1)* env.getHeight() + env.getMinY();
			Point p = gf.createPoint(c);

			if (searchGeometry.contains(p)) {
				randomPoints.add(c); 
				n++; 
			}
		}
		return randomPoints; 
	}


	/**	Gets a random point coordinate within a given cell  
	 * 
	 * @param cellIndex
	 * @return random coordinate
	 */
	public Coordinate getRandomPoint(Int3D cellIndex){

		Geometry geom = gridCells.get(cellIndex).getGeom(); 
		Envelope env = geom.getEnvelopeInternal();

		boolean pointFound = false; 
		while (!pointFound) {
			Coordinate c = new Coordinate();
			c.x = uniform.nextDoubleFromTo(0, 1)* env.getWidth() + env.getMinX();
			c.y = uniform.nextDoubleFromTo(0, 1)* env.getHeight() + env.getMinY();
			Point p = gf.createPoint(c);

			if (geom.contains(p)) {
				Coordinate good = new Coordinate(p.getX(), p.getY());
				return good; 
			}
		}
		return null; 
	}



	/**Gets the cell geometry for the given Point3D index 
	 * 
	 * @param index
	 * @return JTS Geometry
	 */
	public Geometry getGridCellGeometry(Int3D index) {
		return gridCells.get(index).getGeom();
	}

	/**Get's the hashmap of EFDCCell's
	 * 
	 * @return Hashmap of EFDC grid cells 
	 */
	public HashMap <Int3D, EFDCCell> getGridCells(){
		return gridCells; 
	}

	/**Gets the EFDCCell for the given Point3D index
	 * 
	 * @param index
	 * @return EFDCCell
	 */
	public EFDCCell getGridCell(Int3D index){
		EFDCCell cell =gridCells.get(index); 
		return cell;
	}
	
	/**Gets the EFDCCell for the given coordinate
	 * 
	 * @param index
	 * @return EFDCCell
	 */
	public EFDCCell getGridCell(Coordinate coord){
		EFDCCell cell =gridCells.get(getGridIndex(coord)); 
		return cell;
	}
	
	
	public int getNumDepthLayers(){
		return numLayers;
	}

	
	
	/**Explicit clone.  Need to iterate the gridCells and reset the STRtree since it 
	 * isn't clonable.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public EFDCGrid clone(){
		EFDCGrid grid = new EFDCGrid(filename, false); 
		grid.seed = (int) System.currentTimeMillis();
		grid.m= new MersenneTwister(seed); 
		grid.uniform = new Uniform(0,1,m); 

		grid.gf = new GeometryFactory();

		grid.gridCells = (HashMap<Int3D, EFDCCell>) this.gridCells.clone(); 

		
		//clone index tree
		STRtree tree = new STRtree(); 
		Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {
			
			Int3D index = it.next();
			EFDCCell cell = grid.gridCells.get(index);
			tree.insert(cell.getGeom().getEnvelopeInternal(), index);
		}
		
		grid.spatialIndex = tree; 
		return grid; 
	}


}