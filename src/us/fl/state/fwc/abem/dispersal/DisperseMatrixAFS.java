package us.fl.state.fwc.abem.dispersal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;

import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
//import us.fl.state.fwc.abem.monitor.DisperseEventMonitor;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.organism.Organism;
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

public class DisperseMatrixAFS implements Dispersal {

	Scheduler scheduler; 
	String releaseSiteName = "releaseSiteID";
	String arrivalSiteName = "arrivalSiteID";
	String latName = "lat";
	String lonName = "lon";
	String probName = "disperseProb";
	String pldName = "avgPLD"; 

	double[][] probs; //1st dimension is release site ID, 2nd dimension is arrival site ID
	double[][] avgPLDs; //1st dimension is release site ID, 2nd dimension is arrival site ID
	double[][] coords; //1st dimension is the site ID, 2nd dimension is the lat [0] / lon [1] 

	SimpleShapefile shp;
	SpatialIndex spatialIndex;
	private GeometryFactory gf = new GeometryFactory();
	private EmpiricalWalker ewSiteSelect;
	private EmpiricalWalker ewCellSelect;

	ArrayList<Double> posProbs = new ArrayList<Double>();
	ArrayList<Integer> posSiteIDs = new ArrayList<Integer>(); 
	Coordinate arrivalCoord = new Coordinate();
	ABEMGrid seagrassGrid; 

	int currentYear; 
	int[] yearsArray; 

	FishTracker fishTracker; 






	public void initialize(Scheduler sched){
		this.scheduler = sched;
		sched.setDispersal(this);

		//set's the dispersal matrix -- reads in a netCDF connectivity matrix
		//and sets the probs[] array
		setMatrix(); 

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

		ewSiteSelect = new EmpiricalWalker(new double[] {1.0, 0.0},
				Empirical.NO_INTERPOLATION, scheduler.getM());
		ewCellSelect = new EmpiricalWalker(new double[] {1.0, 0.0}, 
				Empirical.NO_INTERPOLATION, scheduler.getM());

	}









	@Override
	public void disperse(Organism t, long numParts) {

		if (fishTracker == null) 
			fishTracker = 
				(FishTracker) scheduler.getMonitors().get("FishTracker"); 
		fishTracker.addTEP(t, (double) numParts, scheduler.getGrid().getGridIndex(t.getSpawnSite())); 
		
		Parameters params = t.getParams(); 

		//convert to double before doing mortality multiplication
		double parts = numParts; 

		int releaseSite = getReleaseSite(t.getSpawnSite()); 

		//instantiations for 
		OrganismFactory fac = scheduler.getOrganismFactory();
		OrgDatagram data = fac.getDatagram();
		Normal norm = scheduler.getNormal();
		Uniform uni = scheduler.getUniform();

		data.setClassName(t.getClassName());


		/*New method:
		 * 	(1) Pre-filter the total lost during the dispersal phase, which is 1-sum(probs of all cells)
		 * 	(2) During summation step, calculate a weighted avgPLD, where the weight is raw 
		 * 			probability
		 * 	(3) Take second mortality step for the early post-settlement duration 
		 * 			(e.g., up to 1-2 weeks following settlement when mort is higher) 
		 * 			- this should be realistic since use weighted avgPLD
		 * 
		 * (4) Take 3rd mortality step for period EPSD to recruitment 
		 * (e.g., up to 3 months as per Powell et al. 2004 data) when mortality is higher
		 * 
		 */



		//=========================================
		//=========================================
		//Standardize the dispersal probabilities
		//=========================================
		/* choose an arrival site by creating a discrete probability distribution
		 * based on the probability of arrival from the dispersal matrix, using
		 * a discrete empirical distribution
		 */

		/*TODO -- save the posProbs and posSiteIDs in memory
		 * 			so can re-use without having to do this everytime, particularly
		 * 			for sites that get a lot of releases
		 */

		posProbs.clear();
		posSiteIDs.clear();

		//loop through all arrival sites for the given release site, sum
		//up the total probability to standardize, and add the probability
		//to the collection
		double probSum = 0; 
		double pldSum = 0;
		for (int i =0; i<probs[releaseSite].length; i++){
			if (probs[releaseSite][i] > 0){
				probSum += probs[releaseSite][i];
				posProbs.add(probs[releaseSite][i]);
				posSiteIDs.add(i);

				//get a weighted sum of the average plds
				pldSum += avgPLDs[releaseSite][i]*probs[releaseSite][i];
			}
		}


		/*TODO -- ****NOTE****
		 * 
		 * This mortality formulation below will not work for small school sizes, since the
		 * average individual doesn't produce an egg during a given spawn event
		 * (i.e., should be more like 1 juvenile per season for each adult)
		 * As such, will need to eventually build in some form of variability in EPSS 
		 * so that can have some surviving each round, versus the lack of any survivors as 
		 * is currently done
		 */

		//=========================================
		//=========================================
		//get the number of recruits to distribute
		//=========================================

		//get initial mortality due to loss from system and mortality during dispersal
		double pldMortality = 1-probSum; 
		parts -= (pldMortality*parts);


		
		/*TODO -- add in DD here in post-settlement mortality

		 */
		
		
		
		
		
		
		
		
		
		//get 2nd mortality which represents an early post-settlement mortality for first 1-2 weeks 
		double avgWeightedPLD = pldSum/probSum;

		double mrate = 1-Math.exp(- ( params.getNatInstMort(-2,0,0) //set dummy vals of 0,0 for size and sex in M parameter
				/ ((double) (TimeUtils.SECS_PER_DAY*1000)
						/ (double) ( (params.getAvgPLD()-avgWeightedPLD)*24*60*60*1000)))) ;	
		parts -= (mrate*parts);

		//apply 3rd mortality to juveniles for a later post-settlement phase -- 
		//		seatrout use 90days following rates in Powell et al. 2004
		mrate = 1-Math.exp(- ( params.getNatInstMort(-1,0,0) 
				/ ((double) (TimeUtils.SECS_PER_DAY*1000)
						/ (double) (params.getAvgEPSD()*24*60*60*1000)))) ;	
		parts -= (mrate*parts);

		numParts = (int) Math.round(parts); 

		//age at which will be added to scheduler
		int ageInDays = params.getAvgPLD() + params.getAvgEPSD();
		long firstRunTime = scheduler.getCurrentTime() 
		+ ageInDays*TimeUtils.MILLISECS_PER_DAY;

		//need to catch remainder and set the initial to distribute to remainder versus school size
		int modGroupSize = (int) numParts%params.getSchoolSize();

		//set the number to distribute to the total number divided by the school size
		numParts = numParts/params.getSchoolSize(); 

		//if not any individuals to distribute, i.e., all died, then return
		if (modGroupSize == 0 && numParts == 0) return; 


		int totalToDistribute = (int) (1 + numParts);
		//if no remainder (i.e., multiple of school size), then set equal to numParts
		if (modGroupSize == 0) totalToDistribute = (int) numParts;  
		int totalDistributed = 0; 




		//=========================================
		//=========================================
		//get the current year for the seagrass
		//=========================================

		//set the yearsArray -- this will be used for binary search to find closest year
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





		//=========================================
		//=========================================
		//distribute
		//=========================================


		//standardize the positive probabilities to choose where remaining particles 
		//should be distributed too
		double[] probabilities = new double[posProbs.size()];
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] = posProbs.get(i)/probSum;
		}

		//set the empirical walker distribution to quickly pull the arrival site based on
		//probabilities below
		ewSiteSelect.setState2(probabilities); 


		int arrivalSite;


		boolean distributedModulusGroup =false;

		while (totalDistributed < totalToDistribute) {			

			arrivalSite = posSiteIDs.get(ewSiteSelect.nextInt());

			//set the release site ID   
			data.setReleaseID(releaseSite); 


			arrivalCoord.x = coords[arrivalSite][1];
			arrivalCoord.y = coords[arrivalSite][0];
			arrivalCoord.z = 0;

			//TODO -- need to assign to a grid cell in the vicinity based on seagrass cover
			// base this on a search radius which is 1/2 the average nearest neighbor 
			//distance between points

			ArrayList<Int3D> cellsInRange = 
				seagrassGrid.getCellsWithinRange(GeometryUtils.getSearchBuffer(arrivalCoord, 
						DisperseMatrixParams.avgNND/2));

			//if aren't any in range, skip to the start of the while loop and find another arrival site
			if (cellsInRange == null) continue; 



			//=====================================================
			/* after an arrival site is selected based on a discrete
			 * empirical distribution, then use a 2nd discrete empirical
			 * distribution to choose a grid cell surrounding the arrival
			 * site, using the cover of seagrass as the settlement 
			 * probability
			 *///===================================================



			//loop through all cells in range, and sum up the total proportion of seagrass cover 
			//	to standardize
			double cellSumCells = 0; 
			for (int i =0; i<cellsInRange.size(); i++){
				cellSumCells += 
					seagrassGrid.getGridCell(cellsInRange.get(i)).getSavCov().get(currentYear);
			}

			double[] cellProbs = new double[cellsInRange.size()];
			for (int i = 0; i < cellProbs.length; i++) {
				cellProbs[i] = 
					seagrassGrid.getGridCell(cellsInRange.get(i)).getSavCov().get(currentYear)
					/cellSumCells;
			}

			//set the empirical walker distribution to quickly pull the arrival site based on
			//probabilities below
			ewCellSelect.setState2(cellProbs); 


			//====================================
			// Set the individual properties
			//====================================

			data.setGridCellIndex(cellsInRange.get(ewCellSelect.nextInt())); 
			data.setCoord(seagrassGrid.getGridCell(data.getGridCellIndex()).getCentroidCoord()); 
//			data.setSizeAtMaturity(norm.nextDouble(params.getSizeAtMaturityAvg(), 
//					params.getSizeAtMaturitySD()));

			//set the birthday to Java millisec time
			data.setBirthday(scheduler.getCurrentTime());
			data.setYearClass(0); 

			//set the group abundance to start at the school size, aka group threshold
			//if haven't already distributed the first group which is the remainder of numParts
			///schoolSize, then set that one first
			if (distributedModulusGroup == false && modGroupSize > 0){
				data.setGroupAbundance(modGroupSize);
				distributedModulusGroup = true;
			}
			else data.setGroupAbundance(params.getSchoolSize());

			//int sex = (int) Math.round(uni.nextIntFromTo(0, 1));
			data.setGroupSex((int) Math.round(uni.nextIntFromTo(0, 1)));

			data.setFirstRunTime(firstRunTime); 

			/*From Peebles and Tolly 1988:
			 * I = otolith increment number and L = standard length.
			 * L = 0.405I + 0.116
			 * From Powell et al 2004: 
			 * Growth of larvae and juveniles (<80 mm SL) was best described 
			 * by the equation loge standard length = –1.31 + 1.2162 (loge age).
			 * 
			 * Growth in length of juveniles (12–80 mm SL) was best described 
			 * by the equation standard length = –7.50 + 0.8417 (age). 
			 * 
			 * Growth in wet weight of juveniles (15–69 mm SL) was best described 
			 * by the equation loge wet-weight = –4.44 + 0.0748 (age).
			 */
			//calculate length at age based on von Bert, then get group biomass
			// assign nominal biomass as groupbiomass


			//only record mass, not length of individuals
			//double length = .8417*ageInDays - 7.5; //from Powell et al. 2004
			//double logeMass = 0.0748*ageInDays - 4.44; //from Powell et al. 2004; 
			//double mass = Math.exp(logeMass); 
			// = params.getMass(params.getLengthAtAge(ageInDays/365.25, sex));
			data.setGroupBiomass((Math.exp(0.0748*ageInDays - 4.44))
					*data.getGroupAbundance());
			data.setNominalBiomass(data.getGroupBiomass());  

			//add the total recruitment to the repro monitor
			fishTracker.addRecruit(data);


			//set the new agent 
			fac.newAgent(data);

			totalDistributed++; 

		}


	}


	/**Returns the array index of the closest release site to the given coordinate
	 * 
	 * @param coord
	 * @return
	 */
	public int getReleaseSite(Coordinate coord){
		int numCellsRadius = 5; 
		Geometry searchGeometry = GeometryUtils.getSearchBuffer(coord, 
				scheduler.getGrid().getCellArea()*numCellsRadius); 
		List<SimpleFeature> hits = spatialIndex.query(searchGeometry.getEnvelopeInternal());
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

		if (name.equals("Bunces")){
			//subtract 1 to make it an array index start of 0
			return 72 - 1; 
		}
		else {
			String[] split = name.split("release");
			return Integer.parseInt(split[1]) - 1;
		}
	}




	/**Set's the matrix to memory for faster access
	 * 
	 */
	public void setMatrix(){
		NetCDFFile ncFile = null;
		try {
			ncFile = new NetCDFFile(DisperseMatrixParams.ncFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ncFile.setVariables(releaseSiteName, arrivalSiteName, 
				probName, pldName, latName, lonName); 

		probs = new double[ncFile.getSingleDimension(releaseSiteName)]
		                   [ncFile.getSingleDimension(releaseSiteName)];
		avgPLDs = new double[ncFile.getSingleDimension(releaseSiteName)]
		                     [ncFile.getSingleDimension(releaseSiteName)];

		coords = new double[ncFile.getSingleDimension(releaseSiteName)][2];
		//loop through all elements of the ncFile
		for (int i=0; i<ncFile.getSingleDimension(releaseSiteName); i++){
			for (int j=0; j<ncFile.getSingleDimension(arrivalSiteName); j++){
				//set the probability array
				probs[i][j] = ncFile.getValue(probName, new int[] {i,j}).doubleValue();
				avgPLDs[i][j] = ncFile.getValue(pldName, new int[] {i,j}).doubleValue();
			}
			//set the coords for the site
			coords[i][0] = ncFile.getValue(latName, new int[]{i}).floatValue();
			coords[i][1] = ncFile.getValue(lonName, new int[]{i}).floatValue();
		}

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

}
