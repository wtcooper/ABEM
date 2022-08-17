package us.fl.state.fwc.abem.organism;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import us.fl.state.fwc.abem.Agent;
import us.fl.state.fwc.abem.environ.EnviroCell;
import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**	Main animal behavioral decision tree methods (DD mortality, merging, hunt/evade, 
 * movement, natural mortality, growth, spawning; in that order)
 * 
 * @author Wade.Cooper; based loosely on descriptions in Gray et al. 2006 
 */



public abstract class Fish extends Organism {


	//************* Unique Variables to each Fish ***************
	protected FishTracker sumsMonitor; 

	protected double sizeAtMaturity=Double.MAX_VALUE; 

	protected boolean exitStep = false; 

	protected double habitatQuality; 

	protected long lastSpawn=0; // in seconds
	protected boolean spawnThisStep = false; 
	protected long nextSpawnTime; 
	protected int numSpawnsInSeason=0; 

	protected boolean accuteStressFlag = false; 

	protected Coordinate homeRangeCoord; 

	protected int releaseID; //holds the release site ID for connectivity purposes



	//################################################################
	@Override
	public void run() {

		//check if running for first time as a settler and if so, reset isSettler
		if (isSettler == true) isSettler = false;


		yearClass = (int) (getAgeInDays()/365.25); 

		
		//add the temperature measurement to organism's memory
		mem.addTemp(
				scheduler.getTemp().getCurrentTemp(scheduler.getCurrentDate(), gridCellIndex));
		

		//run density dependent mortality if setup to do so
		if ( (SchedulerParams.isAdultDDMortalityOn)  
				&&  (!SchedulerParams.isTrophicsOn) 
				&& ((sumsMonitor.getTotalBiomass(this.getClassName()))> 
				params.getCarryCapacity()) ){
			densityDepMortality(); 
			if (groupAbundance < 1){
				scheduler.getOrganismFactory().recycleAgent(this); 
				return; 
			}
		}

		// if are individuals in neighborhood, then attempt to merge
		//here, check all agents in list to see if they are less than group threshold,
		// else will only potentially merge 1/2 of possible mergers depending on 
		// who runs first at a time step
		if (!dependentsList.isEmpty()) mergeSearch(); 

		// if merged into bigger group, kick out of this run() method
		if (exitStep){
			exitStep = false; // reset to false
			return;
		}

		// if time to spawn, then determine movement appropriately
		if (timeToSpawn() ) {
			spawnThisStep = true; 

			/*check if should move to a spawn aggregation, and if so,
			 *  move in the moveToSpawnAgg() method.  If doesn't move, then
			 *  return false, and move to an appropriate location for spawning 
			 *  (default is to base decision on same parameters 
			 */
			if (!moveToSpawnAgg()) {
				directedMove("spawnMove"); 
				if (spawnSite == null) spawnSite = new Coordinate(0,0,0);
				spawnSite.x = coord.x;  
				spawnSite.y = coord.y; 
				spawnSite.z = coord.z; 

				processRates();
				return;

			}
		}

		// if not spawning, then move based on regular habitat quality
		else {
			directedMove("normalMove"); 
			processRates();
			return;
		}
	}


	//################################################################
	/** This method steps through the population rates for each agent, i.e., mortality, growth, 
	 * and spawning (if appropriate) 
	 * 
	 */
	public void processRates(){

		naturalMortality();
		if (groupAbundance < 1){
			scheduler.getOrganismFactory().recycleAgent(this); 
			if (SchedulerParams.outputAgentStepsToConsole) 
				System.out.println("\n\tdying --" +getDescriptor() + "\n"); 		
			return; 
		}
		growth();
		if ((groupSex == 0) && spawnThisStep ) {
			spawn();
			spawnThisStep = false; 
		}
	}


	//################################################################

	/** If group size of agents is less than groupThreshold, then search for other agents to 
	 * merge with within the scale of perception of Animal.  This is doneby searching through the 
	 * dependents list for this time step and looking for conspecifics, then choosing the closest 
	 * one to merge into.  
	 */
	public void mergeSearch(){

		/* Here, if there's a group of same species within dependents list, merge with them.  
		 * Don't use Gray et al approach -- gives very large numbers as done below,
		 * e.g., up to 1km search radius if runTime is 1minute, and 50km is runTime is 1 day
		 */
		//double 	searchRadius = params.getMergeRadius()*((-1+Math.sqrt(1+4*
		//(Math.pow(params.getDirectionalVariability(),2))*params.getCruiseSpeed()*runTime))/
		//(2*Math.pow(params.getDirectionalVariability(),2)));

		Fish closest = null; 
		double closestDistance = params.getScaleOfPerception(yearClass);

		for (int i=0; i<dependentsList.size(); i++) {
			Agent a = dependentsList.get(0);
			//merge if parameterized to do so

			// 1st make sure same sex and the other agent isn't ahead of this one in time
			if ( (a.getCurrentTime() <= this.currentTime) && a.getClass().equals(this.getClass()) ) { 
				Fish agent = (Fish) a; 

				if (agent.groupSex != this.groupSex) continue; //continue to next

				//check if this other are less than the desired school size, and if so, then merge
				if ( (this.groupAbundance < params.getSchoolSize()*.75) 
						|| agent.groupAbundance < params.getSchoolSize()*.75){

					// make sure it's a school of the correct life stage, then checks it's distance away
					if ( (agent.getYearClass()==this.getYearClass()) 
							&& (CoordinateUtils.getDistance(this.coord, 
									agent.getCoords())<closestDistance)  ) { 
						closestDistance = CoordinateUtils.getDistance(this.coord, agent.getCoords());
						closest = agent; 
					}
				}
			}
		}
		if (closest!=null){
			// merge smaller into larger; if equal, then randomly choose one to merge into other
			if (closest.groupAbundance > this.groupAbundance) merge(this, closest);  
			else if (closest.groupAbundance < this.groupAbundance) merge(closest,this);
			else if (closest.groupAbundance == this.groupAbundance){
				if (uniform.nextIntFromTo(0, 1) == 0) merge(closest,this);
				else merge(this,closest);
			}
		}

	}

	//################################################################
	/** Method which actually does the merging:
	 * 	(1) adds appropriate parameters of smallGroup to largeGroup
	 * 	(2) removes the smallGroup from simulation
	 *
	 * @param smallGroup
	 * @param largeGroup
	 */
	public void merge(Fish smallGroup, Fish largeGroup) {

		//System.out.println("\t"+smallGroup.getDescriptor() + " merging into: " + largeGroup.getDescriptor() );
		//if (consoleOutput ) System.out.println("\t"+smallGroup.getDescriptor() + " merging into: " + largeGroup.getDescriptor() ); 

		largeGroup.groupBiomass += smallGroup.groupBiomass; 
		largeGroup.groupAbundance += smallGroup.groupAbundance; 
		largeGroup.nominalBiomass += smallGroup.nominalBiomass;
		//largeGroup.sizeAtMaturity = (largeGroup.sizeAtMaturity + smallGroup.sizeAtMaturity)/2;

		scheduler.getOrganismFactory().recycleAgent(smallGroup); 

		if (smallGroup.equals(this)) exitStep = true; 
	}




	//###############################################################
	/**Sets the next waypoint to move towards, for either spawning or normal processes (e.g., mortality, growth). 
	 *Only in 2D space right now, and not along z-axis (depth), since most processes will be in shallow estuary
	 * 
	 * @param typeOfMove -- either "spawn" or "normal" depending on how to assess surrounding habitat
	 */
	public void directedMove(String typeOfMove){


		//ArrayList<Int3D> gridTestSites = new ArrayList<Int3D>(); 

		/*	******UPDATE*****
		 *For this loop, use a different method where is 2-tier habitat selection -- first select general environment where they want to be (salinity, temperature, density, overall seagrass cover), 
		 *	then select a specific habitat feature where they want to be (e.g., on seagrass versus mud
		 *
		 *TODO -- need to set this up so do a bioenergetics approach -- do similar to Ault et al. but addd in prey density; where growth can then be dictated by gut contents
		 *	(1) calculate maximum searchRadius using same function based on the runTime and their arc; here, set directionVariability high for seatrout since are relatively stationary
		 *		*** make this a circle geometry
		 *	(2) get a list of each FLEM cell within the search radius.  Here, create a circle defined by the radius, and get a spatial query of all polygon cells within that radius
		 *		TODO -- may need to deal with possible issues where can move outside of EnvironmentGrid cells due to grid cells near coast where don't fit (e.g., Luther model)
		 *
		 *	(3) Add each cell index to testSites, and base decision about which cell to to move to on environmental charactierstics
		 *	(4) Do 2nd tier of habitat selection, where then choose an area within FLEM grid based on benthic habitat map; 
		 */			


		//*******************1ST TIER SELECTION (environment)*********************
		double searchRadius = 
			(1+Math.pow(6,params.getSearchRadiusExponent(yearClass)))
			*((-1+Math.sqrt(1+4*(Math.pow(params.getDirectionalVariability(),2))
					*params.getCruiseSpeed(yearClass)*timeStep))
					/(2*Math.pow(params.getDirectionalVariability(),2)));

		//if spawning, then have ability to move 2x's as far
		if (SchedulerParams.haveExtendedSpawnMovement &&  typeOfMove.equals("spawnMove")) 
			searchRadius *= 2; 

		//convert searchRadius to degrees 
		Coordinate temp = CoordinateUtils.getNewCoordAlongSphere(coord, 0, searchRadius);  
		searchRadius = CoordinateUtils.getDistance(coord, temp); 

		EnviroCell currentCell = scheduler.getGrid().getGridCell(this.gridCellIndex);
		int cellSearchRadius = (int) Math.round(searchRadius/scheduler.getGrid().getCellWidth());
		if (cellSearchRadius == 0) cellSearchRadius = 1;
		else if (cellSearchRadius >5) cellSearchRadius = 5;

		ArrayList<EnviroCell> reachableCellsInRange = currentCell.getReachableCells(cellSearchRadius);

		//Geometry searchBuffer = GeometryUtils.getSearchBuffer(coord, searchRadius); //scheduler.getGrid().getSearchBuffer(coord, searchRadius); 
		//gridTestSites = scheduler.getGrid().getCellsWithinRange(searchBuffer); 
		Int3D bestCell = null; 
		double bestValue = -Double.MAX_VALUE;  
		for (int i = reachableCellsInRange.size()-1; i>=0; i--){ // count these down backwards so closest points will have preference if some cells have same habQuality value

			// here, get a habQuality value depending on if it's for regular habitat (e.g., growth, mortality) or spawning
			double habQuality = 0;
			//			if (hasMovementBarrier(searchRadius, gridTestSites.get(i))){
			habQuality = -Double.MAX_VALUE;
			//			}
			//			else {
			habQuality = getEnvironmentHabitatQuality(typeOfMove, reachableCellsInRange.get(i)); 
			//			}

			if (habQuality >= bestValue) {
				bestValue = habQuality;
				bestCell = reachableCellsInRange.get(i).getIndex(); 
			}
		}

		Coordinate bestSpot = null; 
		//if only doing first tier selection based solely on the environment grid,
		//then choose a random point within the grid cell and move there
		if (params.getHabitatSearchComplexity().equals("1st tier")) {
			bestSpot = scheduler.getGrid().getRandomPoint(bestCell); 
		}



		//*******************2ND TIER SELECTION (specific location)*******************

		//else, if also choosing a preferred location within the environment cell based on a 
		//shapefile layer, query the shapefile for best location based on a random
		//number of points
		else if (params.getHabitatSearchComplexity().equals("2nd tier")) {
			ArrayList<Coordinate> pointTestSites = new ArrayList<Coordinate>(); 

			Geometry searchBuffer = GeometryUtils.getSearchBuffer(coord, searchRadius);  
			pointTestSites = scheduler.getGrid().getRandomPoints(params.getNumTestSites(), scheduler.getGrid().getSearchIntersection(searchBuffer, scheduler.getGrid().getGridCellGeometry(bestCell)), bestCell); 

			int size = pointTestSites.size();

			for (int i = size-1; i>=0; i--){ // count these down backwards so closest points will have preference if some cells have same habQuality value

				// here, get a habQuality value depending on if it's for regular habitat (e.g., growth, mortality) or spawning
				double habQuality = getPointHabitatQuality(typeOfMove, pointTestSites.get(i)); 

				if (habQuality >= bestValue) {
					bestValue = habQuality;
					bestSpot = pointTestSites.get(i); 
				}
			}
		}
		// else if (params.getHabitatSearchComplexity().equals("3rd tier")) { } and etc....

		// Set the new coordinates after movement accounting for currents, wind, and behavioral movement 
		this.coord.x = bestSpot.x;  
		this.coord.y = bestSpot.y; 
		this.coord.z = bestSpot.z; 
		//gridCellIndex = bestCell; 
		world.moveTo(this, bestCell, coord); 

	}




	//################################################################
	/**	Get's the habitat suitability for a given location and for a particular life stage.  
	 * Note, this is default implementation -- if animal has unique habitat quality choices (most animals should), then needto override this method in [species]Class.java and set appropriately.  
	 * This may require new parameter/data needs; if so, restrict species-specific requirements to the [species]Class.java.
	 * 
	 * @param coord
	 * @return
	 */
	public double getEnvironmentHabitatQuality(String typeOfSearch, EnviroCell cell){
		double habQuality= 0; 

		if (typeOfSearch.equals("spawnMove")){
			habQuality = 0 
			+ params.getHabQualFunction("SAVCover", yearClass, scheduler.getGrid().getValue("SAVCover", cell.getIndex()))
			+	params.getHabQualFunction("homeRange", yearClass, CoordinateUtils.getDistance(homeRangeCoord, (cell.getCentroidCoord()))) 
			+ params.getHabQualFunction("depth_spawn", yearClass, scheduler.getGrid().getValue("depth", cell.getIndex()))
			//					+ params.getHabQualScaler("salinity", yearClass)*scheduler.getGrid().getValue("salinity", cellIndex, currentTime) 
			//					+ params.getHabQualScaler("temperature")*scheduler.getGrid().getValue("temperature", cellIndex) /// TODO -- change this so not grided, just based on time
			//					+	params.getHabQualScaler("rugosity")*scheduler.getGrid().getValue("rugosity", cellIndex) +
			//					+	params.getHabQualScaler("density", yearClass)*scheduler.getGrid().getValue("density", cellIndex, currentTime) +	//TODO change this so gets appropriate data from Monitor
			// need to get the cell from FLEMGrid, then getCentroid from the cell, then compute distance between homeRangeCoord and this centroid coordinate, and weight it appropriately
			//+ scheduler.getNormal().nextDouble(0, .01)	//add in some randomness
			; // end of habQuality

			//habQuality = ((Math.min(0, (params.getMaxDepth()-depth)/2)) + (Math.min(0, (depth - params.getMinDepth())/2)) + 
			//		Math.abs((params.getPreferredDepth()-depth)/2) + 2*params.getMaxDepth()*params.getSuitableHabitats().get(scheduler.getHabitat().getValue(currentTime, xCoord, yCoord))); 
		}
		else if (typeOfSearch.equals("normalMove")){
			habQuality = 0 + 
			+ params.getHabQualFunction("SAVCover", yearClass, scheduler.getGrid().getValue("SAVCover", cell.getIndex()))
			+	params.getHabQualFunction("homeRange", yearClass, CoordinateUtils.getDistance(homeRangeCoord, (cell.getCentroidCoord()))) 
			//					+ params.getHabQualScaler("salinity", yearClass)*scheduler.getGrid().getValue("salinity", cellIndex, currentTime) 
			//					+ params.getHabQualScaler("temperature")*scheduler.getGrid().getValue("temperature", cellIndex) /// TODO -- change this so not grided, just based on time
			//					+	params.getHabQualScaler("rugosity")*scheduler.getGrid().getValue("rugosity", cellIndex) +
			//					+	params.getHabQualScaler("density", yearClass)*scheduler.getGrid().getValue("density", cellIndex, currentTime) +	//TODO change this so gets appropriate data from Monitor
			//+ params.getHabQualFunction("depth", yearClass, scheduler.getGrid().getValue("depth", cell.getIndex()))
			// need to get the cell from FLEMGrid, then getCentroid from the cell, then compute distance between homeRangeCoord and this centroid coordinate, and weight it appropriately
			//+ scheduler.getNormal().nextDouble(0, .01)	//add in some randomness
			; // end of habQuality
		}

		//add to this in future -- right now just base habitat quality on SAV cover
		else if (typeOfSearch.equals("growth")){
			habQuality = 0 + 
			+ params.getHabQualFunction("SAVCover", yearClass, scheduler.getGrid().getValue("genericQuality", cell.getIndex()))
			//+	params.getHabQualFunction("homeRange", yearClass, CoordinateUtils.getDistance(homeRangeCoord, (cell.getCentroidCoord()))) 
			//					+ params.getHabQualScaler("salinity", yearClass)*scheduler.getGrid().getValue("salinity", cellIndex, currentTime) 
			//					+ params.getHabQualScaler("temperature")*scheduler.getGrid().getValue("temperature", cellIndex) /// TODO -- change this so not grided, just based on time
			//					+	params.getHabQualScaler("rugosity")*scheduler.getGrid().getValue("rugosity", cellIndex) +
			//					+	params.getHabQualScaler("density", yearClass)*scheduler.getGrid().getValue("density", cellIndex, currentTime) +	//TODO change this so gets appropriate data from Monitor
			//+ params.getHabQualFunction("depth", yearClass, scheduler.getGrid().getValue("depth", cell.getIndex()))
			// need to get the cell from FLEMGrid, then getCentroid from the cell, then compute distance between homeRangeCoord and this centroid coordinate, and weight it appropriately
			//+ scheduler.getNormal().nextDouble(0, .01)	//add in some randomness
			; // end of habQuality

		}


		return habQuality; 
	}








	//################################################################
	/**	Get's the habitat suitability for spawning; have seperate method from regular getHabitatQuality() in case animal needs to have different habitat requirements for spawning.
	 * Note, this is default implementation -- if animal has unique habitat quality choices (most animals should), then needto override this method in [species]Class.java and set appropriately.  
	 * This may require new parameter/data needs; if so, restrict species-specific requirements to the [species]Class.java.
	 * 
	 * 
	 * @param coord
	 * @return
	 */
	public double getPointHabitatQuality(String typeOfSearch, Coordinate coord){
		double habQuality= 0; 

		switch (yearClass) { // set the habitat quality criteria based on their life stage
		case 0: 
			if (typeOfSearch.equals("spawn")){
				habQuality = Math.min(0,  
						params.getSuitableHabitats().get(scheduler.getHabitat().getValue("tempString....need to create scheme to fill in!!!", new Coordinate(coord.x, coord.y)))
				); // end of habQuality
			}
			else {
				habQuality = Math.min(0,  
						params.getSuitableHabitats().get(scheduler.getHabitat().getValue("tempString....need to create scheme to fill in!!!", new Coordinate(coord.x, coord.y)))
				); // end of habQuality
			}
			break;

		case 1: 
			if (typeOfSearch.equals("spawn")){
				habQuality = Math.min(0,  
						params.getSuitableHabitats().get(scheduler.getHabitat().getValue("tempString....need to create scheme to fill in!!!", new Coordinate(coord.x, coord.y)))
				); // end of habQuality
			}
			else {
				habQuality = Math.min(0,  
						params.getSuitableHabitats().get(scheduler.getHabitat().getValue("tempString....need to create scheme to fill in!!!", new Coordinate(coord.x, coord.y)))
				); // end of habQuality
			}
			break;

		case 2: 
			if (typeOfSearch.equals("spawn")){
				habQuality = Math.min(0,  
						params.getSuitableHabitats().get(scheduler.getHabitat().getValue("tempString....need to create scheme to fill in!!!", new Coordinate(coord.x, coord.y)))
				); // end of habQuality
			}
			else {
				habQuality = Math.min(0,  
						params.getSuitableHabitats().get(scheduler.getHabitat().getValue("tempString....need to create scheme to fill in!!!", new Coordinate(coord.x, coord.y)))
				); // end of habQuality
			}
			break;

		default: 
			System.out.println(this.getDescriptor() + "got passed an invalid stage in Animal.getSpawningHabitatQuality.  Exiting program"); 
			System.exit(1); 
		}

		return habQuality; 
	}





	//################################################################
	/** Simple natural mortality function based on age-specific estimates of instantaneous natural mortality, M (-yr).  
	 * Here, probability of mortality during a given time step (in seconds), is calculated as: 
	 * ProbOfMortality (i.e., conditional mortality) =  1-(e^(-M))^(runTime/secsPerYear)
	 * Note: this formulation gives correct probability of surviving based on results from MortalityTest.mortality2() 
	 * 
	 * Implement Gislason et al. (2010) formula based on von Bert params: ln(M) = 0.55- 1.61ln(L) + 1.44ln(Linf) + ln(K)
	 */
	public void naturalMortality(){

		//if past oldest age, then remove
		if  (getAgeInDays() > params.getAgeMax()*365.25 ) {
			groupAbundance = 0;
			return;
		}

		//need to get avgLength so can reset the biomass after mortality below
		double avgLength = params.getLengthAtMass(groupBiomass/groupAbundance, groupSex);
		double avgWeight = groupBiomass/(double)groupAbundance;

		// NOTE: do not remove and recycle agents in this step: do from either step() or processRates() directly so will break out of step() method 
		double natMortality = getNatMortalityScaler() * (1- Math.exp(-(params.getNatInstMort(yearClass, avgLength, groupSex) * (timeStep/(double) TimeUtils.SECS_PER_YEAR)))); 


		int numToDie=0;
		for (int i=0; i<groupAbundance; i++){
			//testing:
			if ( uniform.nextDoubleFromTo(0, 1) < natMortality  ) numToDie++; 
		}

		groupAbundance -= numToDie; 

		//reset the groupBiomass
		groupBiomass -= numToDie*avgWeight; 
		nominalBiomass = params.getMassAtLength(params.getLengthAtAge(
				getAgeInDays()/365.25, groupSex), groupSex)
				* groupAbundance; 


		//		else{ // if larger group
		//			int numToDie = (int) Math.round( (double) groupAbundance * natMortality ) ;
		//			groupAbundance -= numToDie; 
		//		}


	}


	//################################################################
	/**	This method determines the environmental forcing on mortality at any given time step as a proportional adjustment (i.e., 1=no adjustment).  
	 * @return mortalityScaler 
	 */
	public double getNatMortalityScaler(){

		// TODO -- need methods to represent decrease in 
		return 1; 
	}


	//################################################################
	/**	Removed individuals randomly if the larger agent (i.e., "school") is above its carrying 
	 * capacity.  Uses NWS-InVitro implementation  
	 * 
	 */
	public void densityDepMortality(){

		// NOTE: do not remove and recycle agents in this step: do from either step() or processRates() directly so will break out of step() method 

		System.out.println("\t ************" + this.getClassName() + " is above carrying capacity.......death dealer on the prowl.....");
		// TODO SPEEDUP: may want to put biomassMonitor.getTotalBiomass as part of his step method so only does it once in a while -- probably doesn't change too and will likely rarely exceed carryCapacity
		double taxaBiomass = sumsMonitor.getTotalBiomass(this.getClassName()); 

		// calculate the total amount of biomass to remove
		double mortalityDD = Math.abs( (groupBiomass/taxaBiomass) * ((taxaBiomass - (double) params.getCarryCapacity())*(1-Math.exp(-(Math.log(.125)/params.getCullPeriod())*(double)timeStep)))); 

		int  numToDie = (int) Math.round(mortalityDD/(groupBiomass/(double)groupAbundance) ); // get the total number of individuals to remove, based on average weight 
		groupAbundance -= numToDie; 

	}




	//################################################################
	/** Growth method.  If trophics are not included (i.e., no explicit feeding), then do a false metabolism as in Gray et al. 2006. If trophics are on, then growth is dependent
	 * on the injested food and metabolism.
	 * 
	 */
	public void growth(){


		if (!SchedulerParams.isTrophicsOn) {


			//expected biomass at the end of time step for normal condition fish
			nominalBiomass = params.getMassAtLength(params.getLengthAtAge(
					getAgeInDaysAfterTimeStep()/365.25, groupSex), groupSex); 


			// Get's the avg growth in mass for a normal condition fish
			double avgGrowthForAge = nominalBiomass
			-  params.getMassAtLength(
					params.getLengthAtAge(
							getAgeInDays()/365.25, groupSex), groupSex);



			// Multiply it by the environmental habitat quality for growth -- this will control condition
			avgGrowthForAge *= 
				getEnvironmentHabitatQuality("growth", 
						scheduler.getGrid().getGridCell(gridCellIndex));  


			//reset the biomass so that reflects the groupAbundance
			groupBiomass += avgGrowthForAge*groupAbundance;
			nominalBiomass *= groupAbundance; 

			//observed weight divided expected weight (i.e., relative condition) 
			condition = groupBiomass / nominalBiomass; 


		}

		/*
		else {
			// TODO do explicit feeding growth here if included
		}
		 */

	}



	//################################################################
	/**	This method determines the environmental forcing on growth at any given time step as a proportional adjustment (i.e., 1=no adjustment).  
	 * @return growthScaler 
	 */
	public double getGrowthScaler(){

		// TODO -- need methods to represent decrease in 

		return 1; 
	}


	//################################################################
	/**	Main spawning method which goes through timeToSpawn, 
	 * 
	 */
	public void spawn() {

		if (SchedulerParams.outputAgentStepsToConsole) 
			System.out.println("\n\tspawning --" +getDescriptor() + "\n"); 		


		/*
		 * Note: add in individual variability via the fertilization rate, but this won't adjust 
		 * year to year variability, e.g., due to trophic interactions or environmental influences
		 */
		long numLarvae = (long) (groupAbundance
				*(Math.max(0, 
						//fertilization rate
						(normal.nextDouble(params.getFertilizationRtAvg(yearClass), 
								params.getFertilizationRtSD())) 
								*		
								//fecundity
								getFecundityScaler() *																								
								params.getBaseFecundity(yearClass, 
										groupBiomass/(double)groupAbundance)		
				)) 
		);  


		// pass the number of larvae to the implemented dispersal class, 
		//and have dispersal function begin
		scheduler.getDispersal().disperse(this, numLarvae); 

		lastSpawn = currentTime; // here, make sure the currentTime is already set

	}



	//################################################################
	/**	This method determines the environmental forcing on fecundity at any given time step as a proportional adjustment (i.e., 1=no adjustment).  
	 * @return fecundityScaler 
	 */
	public double getFecundityScaler(){

		// TODO -- need methods to represent decrease in 
		return 1; 
	}



	//##################################################################
	public boolean timeToSpawn(){

		int dayOfYear = scheduler.getCurrentDate().get(Calendar.DAY_OF_YEAR);


		/*
		 * 
		 * Need to redo completely to make time to spawn mechanistic, i.e., reliant on condition 
		 * and possibly other factors
		 * 
		 * (1) Maturity: based on condition and length
		 * 		sizeMature = avgSizeMaturity + mat_p*(1-condition)*avgSizeMaturity
		 * 		- where mat_p is a parameter, and is optimized to lead to roughly same amount
		 * 			of error as measured in maturity data (see avgMatSD)
		 * 
		 * (2) Likelihood of spawning on a given day function of length, temperature, condition, and lunar cycle
		 * 
		 * 		~ (t_weight * tempFunction) * condition * (length/~avgLength) * lunarProb
		 * 
		 * 		- try to set up some temp function based on positiveness of temperature increase,
		 * 			e.g., (numDaysWithPosTempSlope/~45), but that will then stop once plateau's 
		 *			and turns negative ~250 days
		 *
		 *		- also think about how to bring in weighting scheme to this so that can add weight factors
		 *			to the different factors
		 * 
		 * 		- this likelihood function should therefore capture lunar dynamics, condition, and 
		 * 				length so will lead to longer spawning season and more frequent spawns by
		 * 				older individuals
		 * 
		 * 		- use Sue's MEPs paper to optimize for this function via genetic algorithm-- 
		 * 			compare prop females spawning, lunar periodicity, 
		 * 
		 *  
		 * (3) Batch fecundity function of condition, weight
		 * 		- add in simple scaler for fecundity -- since don't know, will have to make up and explore as function of analyses
		 * 
		 * 
		 * 
		 * NEED to make this reliant on time step, so that if stepping short periods, not spawning all the time
		 * 
		 * 
		 * 
		 * 
		 */






		//get current lenght of individual
		double TL = params.getLengthAtAge(getAgeInDays()/365.25, groupSex);

		//this is a fraction of the average size at maturity (e.g., .75*sizeMature)
		double propSizeMature = params.getPropSetSizeAtMat()*params.getSizeAtMaturityAvg();
		//########################################
		//check and set maturity 
		//########################################

		if (sizeAtMaturity ==Double.MAX_VALUE){ //hasn't been set yet
			//if it's getting close to maturing, then it sets its actual sizeAtMaturity based on condition
			//here, assumes it takes some time to actually ramp up egg production
			if (TL > propSizeMature){
				sizeAtMaturity = params.getSizeAtMaturityAvg()*condition;	
			}
		}

		
		//only possible to spawn if between start and end of season
		if (dayOfYear > params.getStartOfBreeding() 
				&& dayOfYear < params.getEndOfBreeding()){

			// if proper age to spawn
			if ( TL > sizeAtMaturity  ) { // average length of fish in a group is less than the size at maturity for this group, then proceed

				//check fallow period -- minimum amount of time needed between spawns to replenish egg stash
				if ( (currentTime-lastSpawn) > params.getFallowPeriod(yearClass)*TimeUtils.SECS_PER_DAY*1000) {

					double prob = 0; 


					//########################################
					//Calculate lunar prob
					//########################################

					double lunarProb = 0; 

					int lunarPhase = TimeUtils.getMoonPhase(scheduler.getCurrentDate());
					int lunarAdjust = lunarPhase - params.getLunarPeakShift(yearClass);
					if (lunarAdjust < 0) lunarAdjust = 28+lunarAdjust; 
					if (lunarAdjust > 14) lunarAdjust =28-lunarAdjust; 

					ArrayList<Double> lunarProbs = new ArrayList<Double>(); 

					/*This lunar prob routine takes the pdf of multiple normal distributions
					 * of spawning as a function of day of year, standardizes the prob to the 
					 * peak prob of 1 (so always < 1) and then takes the highest probability
					 * for the different peaks.  E.g., if have two normal distributions together,
					 * in between the peaks where they interesect, simply takes the highest value
					 * for any given day, so will have a pointed transition in between the peaks
					 * 
					 */
					for (int i=0; i<params.getLunarPeakSizes(i).size(); i++) {
						lunarProbs.add((params.getLunarNormals(yearClass).get(i).pdf(lunarAdjust)
								/params.getLunarNormals(yearClass).get(i).pdf(
										params.getLunarPeaks(yearClass).get(i)))
										*params.getLunarPeakSizes(yearClass).get(i) );

						if (i == 0) lunarProb = lunarProbs.get(i);
						else if ( lunarProbs.get(i) > lunarProb) lunarProb = lunarProbs.get(i); 
					}

					
					
					//########################################
					//Calculate prob based on temperature signal
					//########################################

					//calculate via a simple linear equation, where max slope is ~.1
					double tempProb = mem.getTempSlope()*10; 


					
					//########################################
					//Calculate prob based on size (length) of fish 
					//########################################

					/*TODO -- see notes in notebook -- use michalis-menton where
					 * x axis is scaled so propSetSizeAtMat*avgSizeMaturity = 0, then Km is the 
					 * average size of 1st spawners
					 * 
					 * This way some spawning potential for those that just became mature,
					 * and the 1st year spawners are about 50% as likely as elders
					 */
					
					double sizeProb = (TL-params.getSizeInflOnSpawnIntcpt()) 
					/ (params.getSizeInflOnSpawnKm()+ (TL- params.getSizeInflOnSpawnIntcpt()));
					
					
					//########################################
					//Calculate total prob, also dependent on the time step
					//########################################

					
					prob = (Math.pow(condition, params.getCondInflOnSpawn()) * lunarProb 
							* tempProb * sizeProb) * (double) timeStep/TimeUtils.SECS_PER_DAY;  
					

					if ( uniform.nextDoubleFromTo(0, 1)  < prob  ) {
						//lastSpawn = dayOfYear; 
						return true; 
					}


				} // end of fallow period check
			} // end of age Maturity check
		}//end check if in breeding season

		return false;
	}


	
	
	
	//##################################################################
	/**	Attempts to move to a spawn aggregation. If fails, then return false.  
	 * 
	 */
	public boolean moveToSpawnAgg(){
		//Check if Bunces is within home range, and if so, have larger adults go there to spawn

		if (params.getSpawnAggregationSpecificity(yearClass) > 0){
			//else, Check to see if there's an aggregation w/in home range, and if so, go to it
			Coordinate closestSpawnSite = null; 
			double closestDistance = Double.MAX_VALUE; 
			for (int i = 0; i < params.getSpawnAggregationList().size(); i++){
				double tempDistance = 
					CoordinateUtils.getDistance(this.coord, params.getSpawnAggregationList().get(i));  
				if ( tempDistance < closestDistance){
					closestSpawnSite = params.getSpawnAggregationList().get(i);
					closestDistance = tempDistance; 
				}
			}

			if (closestDistance < params.getHomeRanges(yearClass)) {
				// proceed to aggregation
				// move the agent to closestSpawnSite
				spawnSite.x = closestSpawnSite.x;  
				spawnSite.y = closestSpawnSite.y; 
				spawnSite.z = closestSpawnSite.z; 
				//gridCellIndex = scheduler.getGrid().getGridIndex(spawnSite); 
				world.moveTo(this, scheduler.getGrid().getGridIndex(spawnSite), spawnSite); 
				processRates(); 
				return true; 
			}
			// if no aggregation within home range, then return false
			else return false; 

		}
		//if doesn't use spawn aggs, then return false
		else return false; 
	}




	//##################################################################
	/**	Set's the initial characteristics of the agents, e.g., their group size, sex, masses and ages.  
	 * 
	 */
	public void setCharacteristics(OrgDatagram data){

		this.groupAbundance = data.getGroupAbundance();
		this.groupBiomass = data.getGroupBiomass();
		this.nominalBiomass = data.getNominalBiomass();
		this.groupSex = data.getGroupSex();
		this.yearClass = data.getYearClass();
		this.releaseID = data.getReleaseID();

		this.homeRangeCoord = (Coordinate) coord.clone(); 

		//reset the maturity
		sizeAtMaturity = Double.MAX_VALUE;
	}


	//##################################################################
	@ Override 
	public String getDescriptor(){

		if (scheduler.getCurrentDate().getTime().after(new Date(this.currentTime))) {
			System.out.println();
		}

		String descriptor = this.getClassName() 
		+" ---[age: "
		+df.format(this.getAgeInDays()/365.25) 
		+ "yr, mass: " + df.format(groupBiomass) 
		+ "grams, length: " 
		+ df.format(params.getLengthAtMass(groupBiomass/(double)groupAbundance, groupSex)) 
		+ "cm, location: (" + df.format(this.coord.x)+", " 
		+ df.format(this.coord.y) +", " + df.format(this.coord.z)+"), " 
		+ "time: "+new Date(this.currentTime).toString() 
		+  ", groupSize: " + groupAbundance+"]" + "---" + this; 
		return descriptor; 
	}



	//##################################################################




	public int getYearClass() {
		return yearClass;
	}



	public void setHomeRangeCoord(Coordinate homeRangeCoord) {
		this.homeRangeCoord = homeRangeCoord;
	}



	public double getSizeAtMaturity() {
		return sizeAtMaturity;
	}

	public void setSizeAtMaturity(double sizeAtMaturity) {
		this.sizeAtMaturity = sizeAtMaturity;
	}

	public int getReleaseID() {
		return releaseID;
	}






}
