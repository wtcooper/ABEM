package us.fl.state.fwc.abem.dispersal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;

import us.fl.state.fwc.abem.Agent;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.organism.Fish;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.organism.SettlerGrid;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.DisperseMatrixParams;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.GeometryUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;

/**
 * Singleton dispersal class that runs as an agent at daily time steps.  Sums up all eggs released 
 * from an environment cell (release site) during a day, applies mortality, and distributes them
 * to an arrival site using an offline connectivity matrix.  Mortality includes density independent
 * dependent rates, where mortality is adjusted based on the incoming cohort size and the 
 * juvenile/adult abundances at an arrival site, thereby simulating both prey limitation and 
 * adult cannabalism.
 * 
 * @author Wade.Cooper
 *
 */
public class DisperseMatrix extends Agent implements Dispersal {

	String releaseSiteName = "releaseSiteID";
	String arrivalSiteName = "arrivalSiteID";
	String latName = "lat";
	String lonName = "lon";
	String probName = "disperseProb";
	String pldName = "avgPLD"; 

	double[][] coords; //1st dimension is the site ID, 2nd dimension is the lat [0] / lon [1] 

	SimpleShapefile shp;
	SpatialIndex spatialIndex;

	ABEMGrid seagrassGrid; 

	SettlerGrid settlerGrid; 


	int currentYear; //holds the currentYear for the run method
	int oldYear = -99;
	int[] yearsArray; 

	FishTracker fishTracker; 

	HashMap<String, Long> numPartsPerSpecies ;	//map is "Classname_releaseSite"

	//holds the probability data for release sites
	HashMap<Integer, ReleaseSiteProbData> releaseSiteData; //key is releaseSiteID

	//holds the probability data for the arrival sites
	HashMap<Integer, ArrivalSiteProbData> arrivalSiteData; //key is releaseSiteID


	//holds the number of settlers per environment cell
	//key = cellIndex.x_cellIndex.y
	//value[0] = num settlers / num observations, value[1] =weighted mean PLD
	//	HashMap<String, double[]> sumSettlersPerCell = 
	//		new HashMap<String, double[]>();





	/**
	 * Initialize method.  Reads in needed habitat data and sets empirical distribution for 
	 * connectivity matrix.
	 * 
	 * @param sched
	 */
	public void initialize(Scheduler sched){
		this.scheduler = sched;
		sched.setDispersal(this);

		//initialize shapefile to get releaseSiteIDs
		if (SchedulerParams.limitToTampaBay)
			shp = new SimpleShapefile(DisperseMatrixParams.shpFileName_TB);	
		else
			shp = new SimpleShapefile(DisperseMatrixParams.shpFileName);	
		shp.buildSpatialIndex();
		spatialIndex = shp.getSpatialIndex();

		if (seagrassGrid == null) {
			if (SchedulerParams.limitToTampaBay)
				seagrassGrid = new ABEMGrid(
						DisperseMatrixParams.seagrassABEMGridName_TB, scheduler);
			else 
				seagrassGrid = new ABEMGrid(
						DisperseMatrixParams.seagrassABEMGridName, scheduler);
		}

		//set the settlerGrid
		settlerGrid = scheduler.getSGrid(); 



		//set's the dispersal matrix -- reads in a netCDF connectivity matrix
		//and sets the transition probabilities for each release site to an arrival site
		setReleaseSiteProbs(); 


		super.superInitialize(0); 
	}






	/**
	 * Stores the number of released particles in a HashMap based on the release site, 
	 * and these release sites are then iterated through during the agents run method 
	 * (e.g., at the end of every day)
	 */
	@Override
	public void disperse(Organism t, long numParts) {

		if (numPartsPerSpecies == null) 
			numPartsPerSpecies = new HashMap<String,Long>();	

		if (fishTracker == null) 
			fishTracker = (FishTracker) scheduler.getMonitors().get("FishTracker"); 

		fishTracker.addTEP(t, (double) numParts, 
				scheduler.getGrid().getGridIndex(t.getSpawnSite())); 

		String key = t.getClassName() + "_" + getReleaseSite(t.getSpawnSite());

		//TODO -- set the condition bin here based on general condition factor
		
		Long mappedParts = numPartsPerSpecies.get(key);
		if (mappedParts == null){
			mappedParts = new Long(numParts);
		}
		else mappedParts += numParts;

		numPartsPerSpecies.put(key, mappedParts);

	}






	/**
	 * Agent run method: loops through all particles that need dispersed, disperses them,
	 * applies DD mortality, and assigns survivors as new agents to model. 
	 */
	@Override
	public void run() {

		//if dispersal is turned off or no existing particles, then exit
		if (!SchedulerParams.isDispersalOn || numPartsPerSpecies == null || numPartsPerSpecies.isEmpty() ) return;





		//=========================================
		//=========================================
		//get the current year -- used for arrivalSiteProbs based on seagrass
		//=========================================
		//=========================================

		if (yearsArray == null){
			Set<Integer> keys = 
				seagrassGrid.getGridCell(new Coordinate(-82.7094,27.6679,0)).getSavCov().keySet();
			yearsArray = new int[keys.size()];
			Iterator<Integer> it = keys.iterator();
			int counter = 0; 
			while (it.hasNext()){
				Integer year = it.next();
				yearsArray[counter++] = year; 
			}

			//need to sort as the keys may not be in order
			Arrays.sort(yearsArray);
		}

		currentYear = yearsArray[locate(scheduler.getCurrentDate().get(Calendar.YEAR))]; 

		//do this each time the currentYear changes
		if (currentYear != oldYear) {
			setArrivalSiteProbs();
			oldYear = currentYear;
		}




		//=========================================
		//=========================================
		//Iterate through all release sites where num of particles are summed up for the day
		//=========================================
		//=========================================

		for (String key : numPartsPerSpecies.keySet()){
			String[] tokens = key.split("_"); 
			String className = tokens[0];
			int releaseSite = Integer.parseInt(tokens[1]);

			// TODO -- get this working
			//int conditionBin = Integer.parseInt(tokens[2]);

			double parts = numPartsPerSpecies.get(key);

			//get the parameters
			params = scheduler.getParamClass(className);

			ReleaseSiteProbData rsData = releaseSiteData.get(releaseSite);



			//=========================================
			//=========================================
			//get the number of recruits to distribute
			//=========================================

			/*Get initial DI mortality due to loss from system and mortality during dispersal
			 * Note: this is done as 1-probSum, because the dispersal model accounts 
			 * for mortality during dispersal phase, and as such, the probabilities in the transition
			 * matrix are relative to the total amount of particles that were release at a site prior
			 * to any mortality taking place (hence very low probabilities).  Therefore, 1-probSum
			 * represents the total number lost from the system during the dispersal phase.
			 */
			double mort = 1-rsData.probSum; 
			parts -= (double) Math.round(mort*parts);

			double avgPLD = rsData.pldSum/rsData.probSum;



			//=========================================
			//loop through all particles and assign to settlement location (Enviro cell)
			//=========================================
			while (parts>0) {

				//group so not looping through until eternity
				int num = DisperseMatrixParams.planktonSchoolSize;
				if (parts < num) num = (int) parts;


				//(1) Draw arrival site from release site empirical walker
				int arrivalSite = rsData.siteIDs[rsData.ewSiteSelect.nextInt()];
				ArrivalSiteProbData asData = arrivalSiteData.get(arrivalSite);

				//if arrival site doesn't have enviro cells with seagrass currently in vicinity, then 
				//continue looping -- 
				if (asData.noSeagrass == true) continue;

				//(2) Draw seagrass cell from arrival site empircal walker
				Int3D cell = asData.siteIndices.get(asData.ewCellSelect.nextInt());


				
				//(3) Add settlers to SettlerGrid
				settlerGrid.addSettlers(className, releaseSite, cell, num, avgPLD);
				

				parts = parts - num;
			}

		} //end for loop over release sites to distribute for the run period

		//clear out map after run through day
		numPartsPerSpecies.clear();






		//BREAK IN METHODS











	} //end run method







	/**Returns the array index of the closest release site to the given coordinate
	 * 
	 * @param coord
	 * @return
	 */
	public int getReleaseSite(Coordinate coord){
		int numCellsRadius = 0; 
		Geometry searchGeometry = null;  
		List<SimpleFeature> hits = null; 
		String name = null; 
		boolean foundPoint = false; 

		//if don't find any release sites within the specified radius, than increase radius and look again
		while (!foundPoint) {
			numCellsRadius += 5; 
			searchGeometry = GeometryUtils.getSearchBuffer(coord, 
					scheduler.getGrid().getCellArea()*numCellsRadius); 
			hits = spatialIndex.query(searchGeometry.getEnvelopeInternal());

			SimpleFeature feature = null; 
			double closestDistance = Double.MAX_VALUE; 

			for (int i = 0; i < hits.size(); i++) {
				feature = hits.get(i);
				Point point = (Point) feature.getDefaultGeometry();

				//find the closest point
				if (searchGeometry.intersects(point)) { 
					foundPoint =true; 
					double tempDistance = CoordinateUtils.getDistance(coord, 
							new Coordinate(point.getX(), point.getY(), 0));  
					if ( tempDistance < closestDistance){
						name = (String) feature.getAttribute("NAME"); 
						closestDistance = tempDistance; 
					}
				}
			}
		} //end of while(!foundPoint)

		String[] split = name.split("release");
		return Integer.parseInt(split[1]);
	}






	/**
	 * Reads in connectivity matrix data (probabilities, avg. PLD, and coordinates) from netCDF
	 * and stores in memory for fast access.
	 */
	public void setReleaseSiteProbs(){

		releaseSiteData = new HashMap<Integer, ReleaseSiteProbData>(); 

		/*
		 * Read in connectivity matrix from netCDF and store probs
		 */
		NetCDFFile ncFile = null;
		try {
			if (SchedulerParams.limitToTampaBay)
				ncFile = new NetCDFFile(DisperseMatrixParams.ncFileName_TB);
			else
				ncFile = new NetCDFFile(DisperseMatrixParams.ncFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ncFile.setVariables(releaseSiteName, arrivalSiteName, 
				probName, pldName, latName, lonName); 

		coords = new double[ncFile.getSingleDimension(releaseSiteName)][2];

		//could have used a hashmap here:
		ArrayList<Double> posProbs = new ArrayList<Double>(); //holds probs > 0
		ArrayList<Integer> posSiteIDs = new ArrayList<Integer>(); //holds index of posProbs


		//loop through all elements of the ncFile to set the connectivity matrix
		for (int i=0; i<ncFile.getSingleDimension(releaseSiteName); i++){

			double probSum = 0; 
			double pldSum = 0;
			double prob = 0;
			double avgPLD = 0;

			//clear the probs for the next release site
			posProbs.clear();
			posSiteIDs.clear();

			for (int j=0; j<ncFile.getSingleDimension(arrivalSiteName); j++){
				//set the probability array

				prob = ncFile.getValue(probName, new int[] {i,j}).doubleValue();
				avgPLD = ncFile.getValue(pldName, new int[] {i,j}).doubleValue();

				//only record the ones that have a value so EmpiricalWalker size is minimal
				if (prob > 0) {
					probSum += prob;
					posProbs.add(prob);
					posSiteIDs.add(j); 	//note: it's oK to use j vs. j-1 here because the posSiteIDs
					//are only used to pull the coords, so everything is 


					/*Get a weighted sum of the average plds; do weighted sum because the 
					 * avgPLDs matrix is avg PLD for that release site (e.g., remote sites have higher
					 * avgPLD because travel further to get to a site), and when represent dispersal
					 * during post-settlement phase below 
					 * 
					 */
					pldSum += avgPLD*prob;
				}
			}

			//set the values for a new ReleaseSiteProbData object
			ReleaseSiteProbData r = new ReleaseSiteProbData(); 
			double[] probabilities = new double[posProbs.size()];
			int[] siteIDs = new int[posProbs.size()];

			r.probSum = probSum;
			r.pldSum = pldSum;

			for (int k=0; k<posProbs.size(); k++) {
				probabilities[k] = posProbs.get(k).doubleValue()/probSum; //standardize here
				siteIDs[k] = posSiteIDs.get(k).intValue();
			}

			EmpiricalWalker ewSiteSelect = new EmpiricalWalker(probabilities,
					Empirical.NO_INTERPOLATION, scheduler.getM());

			r.probabilities = probabilities; 
			r.siteIDs = siteIDs;
			r.ewSiteSelect = ewSiteSelect;


			/*note: use i as the map for releaseSiteID (index starts at 0), because
			 * getReleaseSite(Coord) returns [releaseSiteID from shapefile] - 1 so indexed to 0 
			 */
			releaseSiteData.put(i, r);

			//set the coords for the site
			coords[i][0] = ncFile.getValue(latName, new int[]{i}).floatValue();
			coords[i][1] = ncFile.getValue(lonName, new int[]{i}).floatValue();
		}
	}





	/**
	 * Sets the probabilities for an individual being assigned to a seagrass cell in the vicinity
	 * of an arrival site. Probability is based on seagrass cover  in vicinity, and as such, this 
	 * simulates random dispersal to seagrass areas surrounding an arrival site.
	 */
	public void setArrivalSiteProbs() {

		//reset the arrivalSiteData 
		if (arrivalSiteData == null) 
			arrivalSiteData =new HashMap<Integer, ArrivalSiteProbData>();
		else arrivalSiteData.clear();

		ArrayList<Double> posProbs = new ArrayList<Double>(); //holds probs > 0


		//loop through all release sites
		for (int arrivalSite = 0; arrivalSite<coords.length  ; arrivalSite++){ 

			posProbs.clear(); //this is recycled every time

			//create new one each round since ArrayList will be stored in each arrivalSiteData
			ArrayList<Int3D> posSiteIDs = new ArrayList<Int3D>(); //holds index of posProbs

			double cov = 0; 
			double covSum = 0; 

			//System.out.println("arrival site: " + arrivalSite);

			ArrivalSiteProbData a = new ArrivalSiteProbData();

			Coordinate arrivalCoord = 
				new Coordinate(coords[arrivalSite][1], coords[arrivalSite][0], 0);




			ArrayList<Int3D> cellsInRange = null; 
			double dist =DisperseMatrixParams.avgNND/2; 

			while (cellsInRange == null) {
				cellsInRange = seagrassGrid.getCellsWithinRange(GeometryUtils.getSearchBuffer(arrivalCoord, 
						dist));
				dist *= 1.5; //increment the dist by a multiplier until get some seagrass cells in range
			}


			//loop through all cells in range, and sum up the total proportion of seagrass cover 
			//	to standardize, only recording those cells with a positive cover for that year
			for (int i =0; i<cellsInRange.size(); i++){
				cov = seagrassGrid.getGridCell(cellsInRange.get(i)).getSavCov().get(currentYear);

				if (cov > 0) {
					covSum += cov;
					posProbs.add(cov);
					posSiteIDs.add(cellsInRange.get(i).clone()); 	
				}

			}

			//standardize to proportion
			double[] cellProbs = new double[posProbs.size()];
			for (int i = 0; i < cellProbs.length; i++) {
				cellProbs[i] = posProbs.get(i) / covSum;
			}

			if (posProbs.size() == 0) {
				a.noSeagrass = true;
			}
			else {
				a.noSeagrass = false;
				EmpiricalWalker ewCellSelect = new EmpiricalWalker(cellProbs, 
						Empirical.NO_INTERPOLATION, scheduler.getM());

				a.siteIndices = posSiteIDs;
				a.probabilities = cellProbs;
				a.ewCellSelect = ewCellSelect;
			}
			arrivalSiteData.put(arrivalSite, a);

		}
	}





	/**
	 * Uses binary search to find the closest year of seagrass data to the current year.
	 * 
	 * @param currentYear
	 * @return
	 */
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




	@Override
	public void setScheduler(Scheduler sched) {
		this.scheduler = sched; 
	}

	public void setSeagrassGrid(ABEMGrid seagrassGrid) {
		this.seagrassGrid = seagrassGrid;
	}

	@Override
	public ABEMGrid getSeagrassGrid() {
		return seagrassGrid;
	}

	@Override
	public void setDependentsList() {
	}

	@Override
	public void registerWithMonitors() {
	}

} //end of DisperseMatirixAgent class'





//###########################################################
//Supporting classes
//###########################################################


/**
 * Holds the data for a release site (probabilities of dispersing to other sites, their 
 * corresponding siteID, the sum of probabilities, and the pldSum).
 * 
 * @author Wade.Cooper
 *
 */
class ReleaseSiteProbData {

	public double probSum = 0;
	public double pldSum = 0; 
	public double[] probabilities;
	public int[] siteIDs;
	public EmpiricalWalker ewSiteSelect;

}



/**
 * Holds the data for an arrival site (probabilities of surrounding environment cells and 
 * the Index of those cells).
 * 
 * @author Wade.Cooper
 *
 */
class ArrivalSiteProbData {
	public double[] probabilities;
	public ArrayList<Int3D>siteIndices;
	public EmpiricalWalker ewCellSelect;
	public boolean noSeagrass = false;

}



