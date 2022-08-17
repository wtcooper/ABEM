package us.fl.state.fwc.abem.environ.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.SimpleMapper;
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
 * Creates grid cells with set geometries for an EFDC Grid, using the corners.inp file 
 * to define the geometry.  
 * Can then use these geometries for calculating various other properties for other
 * files.  E.g., can get bathymetry, roughness, veg type for the dxdy.inp file from netCDF data 
 * on these other properies.  See DXDYFormatter.java.
 *  
 * @author wade.cooper
 *
 */
public class ABEMGrid implements EnviroGrid {

	static final double ABEMCellSize_m2 = 175050.3321; //avg size in m^2 (used to get density on m2 

	private double gridWidth = 0.004, gridHeight = 0.004;
	private int seed;
	private MersenneTwister m; 
	private Uniform uniform; 

	private GeometryFactory gf;
	private STRtree spatialIndex;

	private HashMap <Int3D, ABEMCell> gridCells; 


	String filename; 

	public Scheduler scheduler; 

	private int numLayers; 

	private double 
	minLon = -83.10519999216366, 
	maxLon = -82.19719999216255 - .004, 
	minLat = 27.0720000176368, 
	maxLat = 28.220000017637183 + .004;

	private int xDim = 228, yDim = 288;

	private double maxAbundance = 0, maxBiomass = 0, maxSSB = 0, maxRecruitment = 0,
	maxTEP = 0;

	int[] yearsArray; 


	//=====================================
	//Constructors
	//=====================================

	public ABEMGrid(){
		//empty constructor
	}

	public ABEMGrid(String filename, Scheduler scheduler) {
		this.scheduler = scheduler;
		initialize(filename);
	}

	public ABEMGrid(String filename, boolean autoInitialize){
		this.filename = filename; 
		if (autoInitialize) initialize(filename);
	}


	//initialize method for ABEM model, to keep standard with rest of instantiations
	public void initialize(Scheduler sched) {
		this.scheduler = sched;
		sched.setGrid(this);
		initialize(SchedulerParams.gridFilename);
		setReachableCells(SchedulerParams.reachableCellsFilename);

	}



	//=====================================
	//Initialize
	//=====================================

	public void initialize(String filename) {

		if (scheduler == null){
			seed = (int) (System.currentTimeMillis()/1000d);
			m= new MersenneTwister(seed); 
			uniform = new Uniform(0,1,m); 
		}
		else {
			uniform = scheduler.getUniform();
		}

		gf = new GeometryFactory();
		spatialIndex = new STRtree();

		gridCells = new HashMap <Int3D, ABEMCell> (); 
		//private Scheduler scheduler; 

		try {
			File file = new File(filename);
			FileDataStore store = FileDataStoreFinder.getDataStore(file);
			FeatureSource<SimpleFeatureType, SimpleFeature> fSource = store.getFeatureSource(); 

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
			FeatureIterator<SimpleFeature> iterator = features.features();
			try {
				while (iterator.hasNext()) {
					SimpleFeature simpleFeature = iterator.next();
					Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
					int latIndex = 
						(int) ((Double) simpleFeature.getAttribute("LATINDEX")).doubleValue();
					int lonIndex = 
						(int) ((Double) simpleFeature.getAttribute("LONINDEX")).doubleValue();
					double bath = Math.abs( (Double) simpleFeature.getAttribute("BATHYM"));
					//double hab = (Double) simpleFeature.getAttribute("HABTYPE");

					//TODO -- set the attributes coorectly
					double sav88 = (Double) simpleFeature.getAttribute("SAVCOV88"); 
					double sav90 = (Double) simpleFeature.getAttribute("SAVCOV90"); 
					double sav92 = (Double) simpleFeature.getAttribute("SAVCOV92"); 
					double sav94 = (Double) simpleFeature.getAttribute("SAVCOV94"); 
					double sav96 = (Double) simpleFeature.getAttribute("SAVCOV96"); 
					double sav99 = (Double) simpleFeature.getAttribute("SAVCOV99"); 
					double sav01 = (Double) simpleFeature.getAttribute("SAVCOV01"); 
					double sav04 = (Double) simpleFeature.getAttribute("SAVCOV04"); 
					double sav06 = (Double) simpleFeature.getAttribute("SAVCOV06"); 
					double sav08 = (Double) simpleFeature.getAttribute("SAVCOV08"); 

					Int3D index = new Int3D(lonIndex, latIndex, 0); 
					ABEMCell cell = new ABEMCell(this); 
					cell.setGeom(geom); 
					cell.setIndex(index);
					cell.setDepth(bath);
					//cell.setHabType((int) hab); 
					HashMap<Integer, Double> sav = new HashMap<Integer, Double>();
					sav.put(1988, sav88);
					sav.put(1990, sav90);
					sav.put(1992, sav92);
					sav.put(1994, sav94);
					sav.put(1996, sav96);
					sav.put(1999, sav99);
					sav.put(2001, sav01);
					sav.put(2004, sav04);
					sav.put(2006, sav06);
					sav.put(2008, sav08);
					cell.setSavCov(sav);
					gridCells.put(index, cell); 

					Envelope bounds = geom.getEnvelopeInternal();
					spatialIndex.insert( bounds, index);
				}
			} finally {
				iterator.close(); // IMPORTANT
			}
		} catch (IOException e) {
			e.printStackTrace();
		}



	}



	/**	Returns the index of the grid, just x and y as a point.  
	 * When getting data, will need to use the Agent's depth to get appropriate netCDF location
	 * 
	 */

	@SuppressWarnings("unchecked")
	public Int3D getGridIndex(Coordinate coord) {

		Geometry newGeometry = (Geometry) gf.createPoint(coord); 
		List<Int3D> hits = spatialIndex.query(newGeometry.getEnvelopeInternal());

		if (hits.size() == 0) {
			return null; 			
		}
		Int3D index = null; 
		for (int i = 0; i < hits.size(); i++) {
			index = hits.get(i);
			Geometry geom = (Geometry) gridCells.get(index).getGeom(); 
			if (newGeometry.intersects(geom)) { 
				return index;
			}
		}	
		// if didn't intersect, return null
		return null; 

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

		// in radians, this is angle between each polygon point of circle, 
		//based on the total number of circle points defined in Sampler
		double angleIncrement = (Math.PI*2)/coords.length; 
		double angle = angleIncrement; // start at first increment

		for (int i=1; i<coords.length; i++){
			coords[i].x = midCoord.x + (Math.cos(angle)*radius); 
			coords[i].y = midCoord.y + (Math.sin(angle)*radius); 
			angle += angleIncrement; 
		}

		coords[12].x = coords[0].x;
		coords[12].y = coords[0].y;	

		Geometry newGeometry = (Geometry) 
		gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 

		return newGeometry; 
	}



	/**	Get's all the environment cells within the search buffer.  
	 * This represents the 1st tier of habitat selection where an animal chooses 
	 * its preferred environment (i.e., based on temperature, salinity, seagrass cover, etc). 
	 */

	@SuppressWarnings("unchecked")
	public ArrayList<Int3D> getCellsWithinRange(Geometry searchGeometry) {

		ArrayList<Int3D> cellsWithinRange = new ArrayList<Int3D>(); 


		//	(2) Get all the grid cells within that 
		List<Int3D> hits = spatialIndex.query(searchGeometry.getEnvelopeInternal());

		if (hits.size() == 0) return null; 

		/*	{
			System.out.println("you passed an invalid Coordinate in FLEMGrid.getGridIndex()"); 
			System.exit(1);
		}
		 */
		Int3D index = null; 
		for (int i = 0; i < hits.size(); i++) {
			index = hits.get(i);
			Geometry geom = (Geometry) gridCells.get(index).getGeom(); 
			if (searchGeometry.intersects(geom)) { 
				cellsWithinRange.add(index); 
			}
		}	

		if (cellsWithinRange.isEmpty()) return null;
		
		else return cellsWithinRange;
	}


	public Geometry getSearchIntersection(Geometry geom1, Geometry geom2){
		return geom1.intersection(geom2); 
	}


	/**	Gets a list of random point coordinates at the intersection the search radius and the 
	 * environment grid cell in which to search.  These points represent the 2nd tier of 
	 * habitat selection, where an animal will choose a specific habitat type within a 
	 * preferred environment.  
	 * 
	 * @param numPoints
	 * @param cellIndex
	 * @return
	 */
	public ArrayList<Coordinate> getRandomPoints(int numPoints, 
			Geometry searchBuffer, Int3D cellIndex){
		
		ArrayList<Coordinate> randomPoints = new ArrayList<Coordinate>();

		Geometry searchGeometry = getSearchIntersection(searchBuffer, 
				gridCells.get(cellIndex).getGeom()); 
		Envelope env = searchGeometry.getEnvelopeInternal();

		int n = 0;
		while (n < numPoints) {
			Coordinate c = new Coordinate();
			c.x = uniform.nextDoubleFromTo(0, 1)* env.getWidth() + env.getMinX();
			c.y = uniform.nextDoubleFromTo(0, 1)* env.getHeight() + env.getMinY();
			c.z = 0; 
			Point p = gf.createPoint(c);

			if (searchGeometry.contains(p)) {
				randomPoints.add(c); 
				n++; 
			}
		}
		return randomPoints; 
	}

	/**	Gets a list of random point coordinates at the intersection the search radius and the 
	 * environment grid cell in which to search.  These points represent the 2nd tier of habitat 
	 * selection, where an animal will choose a specific habitat type within a preferred 
	 * environment.  
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
			c.z = 0; 
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
				Coordinate good = new Coordinate(p.getX(), p.getY(), 0);
				return good; 
			}
		}
		return null; 
	}



	/**Gets the cell geometry for the given Point3D index `
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
	public HashMap <Int3D, ABEMCell> getGridCells(){
		return gridCells; 
	}



	/**Gets the EFDCCell for the given Point3D index
	 * 
	 * @param index
	 * @return EFDCCell
	 */
	public ABEMCell getGridCell(Int3D index){
		ABEMCell cell =gridCells.get(index); 
		return cell;
	}

	/**Gets the EFDCCell for the given coordinate
	 * 
	 * @param index
	 * @return EFDCCell
	 */
	public ABEMCell getGridCell(Coordinate coord){
		ABEMCell cell =gridCells.get(getGridIndex(coord)); 
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
	public ABEMGrid clone(){
		ABEMGrid grid = new ABEMGrid(filename, false); 
		grid.seed = (int) System.currentTimeMillis();
		grid.m= new MersenneTwister(seed); 
		grid.uniform = new Uniform(0,1,m); 

		grid.gf = new GeometryFactory();

		grid.gridCells = (HashMap<Int3D, ABEMCell>) this.gridCells.clone(); 


		//clone index tree
		STRtree tree = new STRtree(); 
		Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {

			Int3D index = it.next();
			ABEMCell cell = grid.gridCells.get(index);
			tree.insert(cell.getGeom().getEnvelopeInternal(), index);
		}

		grid.spatialIndex = tree; 
		return grid; 
	}



	@Override
	public double getValue(String variable, Int3D cellIndex) {

		if (variable.equals("SAVCover")){
			if (yearsArray == null){
				Set<Integer> keys = 
					getGridCell(cellIndex).getSavCov().keySet();
				yearsArray = new int[keys.size()];
				Iterator<Integer> it = keys.iterator();
				int counter = 0; 
				while (it.hasNext()){
					Integer year = it.next();
					yearsArray[counter++] = year; 
				}
				Arrays.sort(yearsArray);
			}

			int currentYear = yearsArray[locate(scheduler.getCurrentDate().get(Calendar.YEAR))]; 
			return  getGridCell(cellIndex).getSavCov().get(currentYear).doubleValue();
		}
		
		else if (variable.equals("depth")){
			return getGridCell(cellIndex).getDepth(); 
		}

		else if (variable.equals("nitrogen")){
			return getGridCell(cellIndex).getNitrogen(); 
		}
		else if (variable.equals("phosphorous")){
			return getGridCell(cellIndex).getPhosphorous(); 
		}
		else if (variable.equals("silica")){
			return getGridCell(cellIndex).getSilica(); 
		}
		else if (variable.equals("phytoplankton")){
			return getGridCell(cellIndex).getPhytoplankton(); 
		}
		else if (variable.equals("zooplankton")){
			return getGridCell(cellIndex).getZooplankton(); 
		}

		else if (variable.equals("genericQuality")){
			return getGridCell(cellIndex).getGenericQuality(); 
		}
		
		
		//return null
		return 0;
	}


	@Override
	public void updateCells(String year) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getCellHeight() {
		return gridHeight;
	}

	@Override
	public double getCellWidth() {
		return gridWidth;
	}

	@Override
	public void setReachableCells(String filename) {

		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));


			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				// this is a "greedy qualifier regular expression in java 
				//String tokens[] = line.split("\\x20+"); 
				String tokens[] = line.split("\t"); 

				//122,195	1	123,196	122,196	121,196	123,195	121,195	121,194	123,194	122,194	

				String indexTokens[] = tokens[0].split(",");

				int I = Integer.parseInt(indexTokens[0]); 
				int J = Integer.parseInt(indexTokens[1]); 
				short cellRadi = (short) Integer.parseInt(tokens[1]);

				ABEMCell cell = this.getGridCell(new Int3D(I, J, 0));
				HashMap<Short, ArrayList<ABEMCell>> reachableCells = 
					cell.getReachableCellsMap(); 

				//if haven't been set yet, then set the cell
				if (reachableCells == null) {
					reachableCells = new HashMap<Short, ArrayList<ABEMCell>>();
					cell.setReachableCells(reachableCells);
				}

				ArrayList<ABEMCell> reachables = new ArrayList<ABEMCell>();
				reachableCells.put(cellRadi, reachables);


				for (int i=2; i<tokens.length; i++){
					indexTokens = tokens[i].split(",");
					I = Integer.parseInt(indexTokens[0]); 
					J = Integer.parseInt(indexTokens[1]);
					cell = this.getGridCell(new Int3D(I, J, 0));
					reachables.add(cell);
				}

			}

			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public Object[] getGridCov(String variable, String classname, int year){

		Object[] array = new Object[3];
		array[1] = new Double(0);
		array[2] = new Double(0);

		float[][] temp = new float[yDim][xDim];  

		FishTracker fishTracker = 
			(FishTracker) scheduler.getMonitors().get("FishTracker"); 

		float val = 0; 

		for (ABEMCell cell: fishTracker.getActiveCells()){

			if (variable.equalsIgnoreCase("biomass")) {
				val =(float) cell.getAvgBiomass(classname, year); 
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[2]).doubleValue()) array[2] = new Double(val); //max val
			}

			else if (variable.equalsIgnoreCase("abundance")) { 
				val =(float) cell.getAvgAbundance(classname, year); 
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[2]).doubleValue()) array[2] = new Double(val); //max val
			}

			else if (variable.equalsIgnoreCase("SSB")) { 
				val =(float) cell.getAvgSSB(classname, year); 
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[2]).doubleValue()) array[2] = new Double(val); //max val
			}

			else if (variable.equalsIgnoreCase("TEP")) { 
				val =(float) cell.getTEP(classname, year); 
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[2]).doubleValue()) array[2] = new Double(val); //max val
			}

			else if (variable.equalsIgnoreCase("recruitment")) { 
				val =(float) cell.getNumRecruits(classname, year); 
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[2]).doubleValue()) array[2] = new Double(val); //max val
			}


			/*else if (variable.equalsIgnoreCase("SSB:TEP")) { 
				val =	(float) cell.getAvgSSB(classname, year)
				/ 
				(float) cell.getTEP(classname, year);
				if ( ((Float) val).isNaN()) val = 0; 
				if ( ((Float) val).isInfinite()) val = Float.MIN_VALUE;
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[2]).doubleValue()) array[2] = new Double(val); //max val
			}



			else if (variable.equalsIgnoreCase("TEP:SSB")) {
				val =(float) cell.getTEP(classname, year)
				/ 
				(float) cell.getAvgSSB(classname, year); 
				if ( ((Float) val).isNaN()) val = 0; 
				if ( ((Float) val).isInfinite()) val = Float.MIN_VALUE;
				temp[cell.getIndex().y][cell.getIndex().x] = val;
				if (val < ((Double) array[1]).doubleValue()) array[1] = new Double(val); //min val
				if (val > ((Double) array[1]).doubleValue()) array[2] = new Double(val); //max val
			}
*/
		}

		
		//normalize from 0-2
/*		if (variable.equalsIgnoreCase("SSB:TEP") || variable.equalsIgnoreCase("TEP:SSB")) {
			double maxVal =((Double) array[2]).doubleValue();
			
			for (ABEMCell cell: fishTracker.getActiveCells()){
				if (temp[cell.getIndex().y][cell.getIndex().x] == Float.MIN_VALUE) 
					temp[cell.getIndex().y][cell.getIndex().x] = 2; 
				else 
					temp[cell.getIndex().y][cell.getIndex().x] /= (maxVal*.5);

				if (temp[cell.getIndex().y][cell.getIndex().x] < 0 
						|| temp[cell.getIndex().y][cell.getIndex().x]  > 2) {
					System.out.println("got this botched, exiting");
					System.exit(1);
				}
				
			}

		}
*/
		
		
		float[][] values = SimpleMapper.reflectArray(temp); 
		Envelope e = new Envelope(minLon, maxLon, minLat, maxLat);  
		ReferencedEnvelope env = new ReferencedEnvelope(e, null);  
		GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
		GridCoverage2D grid = gcf.create("coverage", values, env);

		array[0] = grid;

		return array;
	}
	
	
	public Object[] getFeatureCollection(){
		Object[] array = new Object[3];
		FeatureCollection<SimpleFeatureType, SimpleFeature>  collection = null; 
		array[0] = collection;
		
		
		
		return array;
	}

	
	public int locate(int currentYear)  {
		int idx;

		idx = Arrays.binarySearch(yearsArray, currentYear);

		if (idx < 0) {

			// Error check
			if (idx == -1) {
				return 0;
			}

			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= yearsArray.length) {
				return yearsArray.length-1;
			}

			double spval = (yearsArray[-(idx + 2)] + yearsArray[-(idx + 1)]) / 2d;
			if (currentYear < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}
	
	
	public double getMaxAbundance() {
		return maxAbundance;
	}

	public void setMaxAbundance(double maxAbundance) {
		this.maxAbundance = maxAbundance;
	}

	public double getMaxBiomass() {
		return maxBiomass;
	}

	public void setMaxBiomass(double maxBiomass) {
		this.maxBiomass = maxBiomass;
	}

	public double getMaxSSB() {
		return maxSSB;
	}

	public void setMaxSSB(double maxSSB) {
		this.maxSSB = maxSSB;
	}

	public double getMaxRecruitment() {
		return maxRecruitment;
	}

	public void setMaxRecruitment(double maxRecruitment) {
		this.maxRecruitment = maxRecruitment;
	}

	public double getMaxTEP() {
		return maxTEP;
	}

	public void setMaxTEP(double maxTEP) {
		this.maxTEP = maxTEP;
	}

	@Override
	public double getCellArea() {
		return ABEMCellSize_m2;
	}





}