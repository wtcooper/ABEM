package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.AbstractParams;
import cern.jet.random.Normal;

import com.vividsolutions.jts.geom.Coordinate;

public class SnookParams extends AbstractParams {


	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// COLLECTIONS INSTANTIATIONS -- DO NOT ADJUST

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList = new ArrayList<String>(); 	//Other agents which this agent interacts with
	private HashMap<String, Long>interactionTicks = new HashMap<String, Long>();  	//Map of all the dependent agent types with the preferred interaction tick for that dependent

	// THING.CLASS COLLECTIONS 
	private HashMap<Integer, Double> scaleOfPerception = new HashMap<Integer, Double>(); 						//TODO Area over which individuals can sense their surroundings; this is in units of 10m, so a scaleOfPerception=1 reflects a 10m perception
	
	//ANIMAL.CLASS COLLECTIONS
	// reproduction parameters
	private ArrayList<Coordinate> spawnAggregationList = new ArrayList<Coordinate>();  // waypoint list of known spawn aggregations
	private HashMap<Integer, Double> spawnAggregationSpecificity = new HashMap<Integer, Double>(); //TODO The probability that any given individual will move towards a specific aggregation, versus spawn in other suitable habitat
	private HashMap<Integer, ArrayList<Normal>> seasonNormals = new HashMap<Integer, ArrayList<Normal>> ();	//list of the seasonal normal distributions, set below, for each stage 
	private HashMap<Integer, ArrayList<Integer>> seasonPeaks = new HashMap<Integer, ArrayList<Integer>>();  // fast map of season peaks (i.e., days of year) for each stage, set below
	private HashMap<Integer, ArrayList<Double>> seasonPeakSizes = new HashMap<Integer, ArrayList<Double>>();  //fast map of seasonal peak size for each stage, set below
	private HashMap<Integer, ArrayList<Normal>>  lunarNormals = new HashMap<Integer, ArrayList<Normal>> ();  	// list of the lunar normal distributions
	private HashMap<Integer, ArrayList<Integer>> lunarPeaks = new HashMap<Integer, ArrayList<Integer>>();  // fast map of lunar peaks (days of month, where 0=new moon, 14=full moon) size for each stage, set below
	private HashMap<Integer, ArrayList<Double>> lunarPeakSizes = new HashMap<Integer, ArrayList<Double>>();  // fast map of lunar peak size for each stage, set below
	private HashMap<Integer, Integer> lunarPeakShift = new HashMap<Integer, Integer>();  // fast map of lunar peak size for each stage, set below
	private HashMap<Integer, Double>  sexRatio= new HashMap<Integer, Double>(); 							//TODO The proportion of females in population; here, 0.5 is 1:1 ratio of females:males
	private HashMap<Integer, Double>  fecunditySizeScaler= new HashMap<Integer, Double>(); 						//TODO A scaler which adjusts the batch fecundity to reflect size-dependent changes in fecundity; here, higher the value, greater size dependency
	//private HashMap<Integer, Double>  baseFecundity= new HashMap<Integer, Double>(); 				//TODO Total number of eggs that a female produces in a batch; i.e., Batch Fecundity 
	private HashMap<Integer, Double>  natInstMortality= new HashMap<Integer, Double>(); 						//TODO Base mortality rate, M/year
	private HashMap<Integer, Double>  searchRadiusExponent= new HashMap<Integer, Double>(); 			//Determines the width between consecuative spirals in the search pattern (see Gray et al. 2006)
	private HashMap<Integer,Double> suitableHabitats = new HashMap<Integer,Double>(); 	//Maximum distance at which a school looking to merge can detect other schools in vicinity (m)
	private ArrayList<Integer> suitableHabitatList = new ArrayList<Integer>(); 	//Maximum distance at which a school looking to merge can detect other schools in vicinity (m)
	private HashMap<Integer, Double>  minDepth= new HashMap<Integer, Double>(); 							//TODO Minimal depth (m)
	private HashMap<Integer, Double>  maxDepth= new HashMap<Integer, Double>(); 							//TODO Maximum depth (m)
	private HashMap<Integer, Double>  preferredDepth= new HashMap<Integer, Double>(); 					//TODO Preferred depth (m)
	private HashMap<Integer, Double>  cruiseSpeed= new HashMap<Integer, Double>();						//TODO Normal traveling speed (m/s)
	private HashMap<Integer, Double>  maxSpeed= new HashMap<Integer, Double>(); 							//TODO Max sprint speed (m/s)

	//*****************TODO -- see if can make the InstCatchRate for both recreational and commercial dependent on their size, so can have gear selectivity in there
	private HashMap<Integer, Double>recMinSizeLimit = new HashMap<Integer, Double>(); // here, the Integer is the Year of the simulation 
	private HashMap<Integer, Double> recMaxSizeLimit = new HashMap<Integer, Double>(); //here the Integer index is the Year of the simulation
	private HashMap<Integer, int[][]> recOpenSeasons = new HashMap<Integer, int[][]>(); // index is year, and int[number of open season within a year][0=startday, 1=endday]
	private HashMap<Integer, Integer>recBagLimit = new HashMap<Integer, Integer>(); //4; 
	private HashMap<Integer, double[][]> recInstCatchRate = new HashMap<Integer, double[][]>(); // here, Integer index is year, double[2][maxAge] 
	private HashMap<Integer, double[][]>  recReleaseMortality= new HashMap<Integer, double[][]>();		//Catch and release mortality rate where index is year and double[sex,2][age, maxAge]  
	private HashMap<Integer, double[][]> probCatchAndRelease = new HashMap<Integer, double[][]>(); // here index is year and double[sex,2][integerOfLength]
	private HashMap<Integer, Double>commMinSizeLimit = new HashMap<Integer, Double>(); // here, the Integer is the Year of the simulation 
	private HashMap<Integer, Double> commMaxSizeLimit = new HashMap<Integer, Double>(); //here the Integer index is the Year of the simulation
	private HashMap<Integer, int[][]> commOpenSeasons = new HashMap<Integer, int[][]>(); // index is year, and int[number of open season within a year][0=startday, 1=endday] 
	private HashMap<Integer, Integer>commBagLimit = new HashMap<Integer, Integer>(); //4; 
	private HashMap<Integer, double[][]> commInstMortality= new HashMap<Integer, double[][]>(); // here index is year and double[sex,2][age, maxAge]

	// AGENT.CLASS PARAMETERS 
	private boolean isSetRunTimes = false;
	private boolean isReactive=true; 						//If agent interacts with others, it's reactive. 
	private boolean isRunnable=true;						//If agent changes over time, it's dynamic.  E.g., Bathymetry would not be dynamic
	private String queueType="runQueue";				//The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing
	private long normalTick=10; 								//TODO The normal tick length when no interactions are present
	private int runPriority=3; 									//The priority as to what should run first; here, monitors have higher priority than organisms
	

	//ANIMAL.CLASS PARAMETERS 
	// reproduction parameters
	private double fallowPeriod=30;	 					//TODO Amount of time between reproductive events, including gametogenesis
	double[] fallowPeriods = {12, 11, 10, 9, 8, 7.5, 7.25, 7, 6.8, 6.65, 6.55, 6.5, 6.475, 6.5};
	private double fallowPeriodSD = 2; 
	private double startOfBreeding=120; 					//TODO Start of breeding season in days since start of new year 
	private double endOfBreeding=210;  					//TODO End of breeding season in days since start of new year 
	private boolean haveSpawnAggregations=false;	//If agent has known spawning aggregations
	private boolean isMultipleSpawner=true; 			//TODO If an agent spawns multiple times in a single breeding season
	private int ageMax=15; 								//TODO Max age in years
	private double sizeAtMaturityAvg=26.8002; 	
	private double sizeAtMaturitySD=2.196391585;
	private double fertRtAvg = .8;
	private double[] fertRtAvgs = {.3, .35, .4, .45, .5, .55, .6, .65, .7, .75, .8, .85, .9, .9};

	private double fertRtSD = .1;
	private int ageAtRecruitment = 1; 

	//!!!! TODO -- need to fix --these are for seatrout!!
	private double[]  vonBertK= {.585347, .442982};  			//K in vonBert age-length relationship
	private double[]  vonBertLinf= {45.6785, 64.1567};  	//L infinity in vonBert age-length relationship, in centimeters
	private double[]  vonBertT0= {0,0};  	//t0 in von Bert age-length relationship
	private double[] a = {.0115, .011}; //0.0116;									// 'a' length-weight conversion factor, where lenght = a*(weight^b); HERE, weight is structural weight (no gonad or stomach content)
	private double[] b = {2.9298, 2.9438}; //2.9204;								// 'b' length-weight conversion factor

	private double[] iniMass= {0.001, 0.001}; 							//TODO Initial mass, i.e. mass of an egg 
	private double[] maxMass={8.045, 8.045}; 					//TODO Maximum mass per individuals for this species
	private int massCatchUpRate=6; 						//TODO Rate at which true mass will catch up to nominal mass, e.g., if a fish is starved and has now found steady food, or after a reproduction event
	private double cullPeriod=1000000; 					//TODO Cull period is the time frame over which density dependent mortality occurs when a species is above carrying capacity; see Gray et al. 2006
	private int carryCapacity=1000000; 					//TODO Carrying capacity in mass (kg) for the entire domain; note, a biomass monitor will keep track of the running total of biomass
	private int schoolSize=1;							//TODO Smallest size of school after which a school will want to merge with another school
	private int mergeRadius=100;							//TODO Maximum distance at which a school looking to merge can detect other schools in vicinity (m
	private int numTestSites=36;								//Number of test sites to use when assessing habitat suitability in the surrounding area
	private double directionalVariability=0.5; 			//The amount of "wandering" an agent will do while traveling along a path towards a waypoint; from Gray et al. 2006 algorithm 
	private int numPointsPerSpiral=6;						//Number of points to use in a spiral for the habitat search algorithm; see Gray et al. 2006 for description 
	private String habitatSearchComplexity = "1st tier"; 
	private boolean hasHomeRange = true; 

	
	
/*	private double sexRatio=0.5; 							//TODO The proportion of females in population; here, 0.5 is 1:1 ratio of females:males
	private double fecundityRate=0; 						//TODO A scaler which adjusts the batch fecundity to reflect size-dependent changes in fecundity; here, higher the value, greater size dependency
	private double baseFecundity=10000; 				//TODO Total number of eggs that a female produces in a batch; i.e., Batch Fecundity 
	private double ageMaturity=1; 							//TODO Age of maturity in years
	// growth parameters
	private double c=2.479; 									//TODO c parameter defines the shape of the weight as function of age relationship: c <=1 is saturating; c>1 is sigmoidal
	private double K=7.6941; 									//TODO K is constant which defines the growth rate, where increasing K is faster growth rate;  here, K is in per year units, calculated from vonBertalanffy length curve
	private double iniMass=0.001; 							//TODO Initial mass, i.e. mass of an egg 
	private double maxMass=8.045; 						//TODO Maximum mass per individuals for this species
	private int massCatchUpRate=6; 						//TODO Rate at which true mass will catch up to nominal mass, e.g., if a fish is starved and has now found steady food, or after a reproduction event
	// mortality parameters
	private double baseMortality=0.25;					//TODO Base mortality rate, M/year
	private double catchMortality=0.05;					//TODO Catch and release mortality rate; likelihood of dying after capture 
	private double cullPeriod=1000000; 					//TODO Cull period is the time frame over which density dependent mortality occurs when a species is above carrying capacity; see Gray et al. 2006
	private int carryCapacity=1000000; 					//TODO Carrying capacity in mass (kg) for the entire domain; note, a biomass monitor will keep track of the running total of biomass
	// merge parameters
	private int groupThreshold=3;							//TODO Smallest size of school after which a school will want to merge with another school
	private int mergeRadius=100;							//TODO Maximum distance at which a school looking to merge can detect other schools in vicinity (m
	// habitat suitability parameters
	private int numTestSites=36;								//Number of test sites to use when assessing habitat suitability in the surrounding area
	private double searchRadiusExponent=2; 			//Determines the width between consecuative spirals in the search pattern (see Gray et al. 2006)
	private double directionalVariability=0.5; 			//The amount of "wandering" an agent will do while traveling along a path towards a waypoint; from Gray et al. 2006 algorithm 
	private int numPointsPerSpiral=6;						//Number of points to use in a spiral for the habitat search algorithm; see Gray et al. 2006 for description 
	private HashMap<Integer,Double> suitableHabitats = new HashMap<Integer,Double>(); 	//Maximum distance at which a school looking to merge can detect other schools in vicinity (m)
	private ArrayList<Integer> suitableHabitatList = new ArrayList<Integer>(); 	//Maximum distance at which a school looking to merge can detect other schools in vicinity (m)
	private double minDepth=0.1; 							//TODO Minimal depth (m)
	private double maxDepth=250; 							//TODO Maximum depth (m)
	private double preferredDepth=2; 					//TODO Preferred depth (m)
	// movement parameters
	private double cruiseSpeed=0.7;						//TODO Normal traveling speed (m/s)
	private double maxSpeed=0.7; 							//TODO Max sprint speed (m/s)
	*/
	
	
	@Override
	public void initialize(Scheduler sched){

		int numYearClasses = 15; 
		/*	**DEFINE STAGES**
		 * 		-2: eggs/larvae
		 * 		-1: recently settled, age 0
		 * 		0: juveniles, age 0 up to age 1
		 * 		1: age 1
		 * 		2: age 2
		 * 		etc., up to max age, 12
		 * 
		 *  	Here, do each age since want high resolution for spawning seasonality ages
		 */


		//*********************************************************************************************************************************************************************************************************
		//Movement and habitat search

		scaleOfPerception.put(-2, .0001); 
		scaleOfPerception.put(-1, .0001); 
		scaleOfPerception.put(0, .002); 
		scaleOfPerception.put(1, .004); 
		scaleOfPerception.put(2, .004); 
		scaleOfPerception.put(3, .004); 
		scaleOfPerception.put(4, .004); 
		scaleOfPerception.put(5, .004); 
		scaleOfPerception.put(6, .004); 
		scaleOfPerception.put(7, .004); 
		scaleOfPerception.put(8, .004); 
		scaleOfPerception.put(9, .004); 
		scaleOfPerception.put(10, .004); 
		scaleOfPerception.put(11, .004); 
		scaleOfPerception.put(12, .004); 


		dependentsTypeList.add("Snook"); 
		dependentsTypeList.add("RecFisherman"); 
		interactionTicks.put("Snook", 5l); 
		interactionTicks.put("RecFisherman", 30l); 

		suitableHabitats.put(9116, 2.0); //continuous seagrass
		suitableHabitats.put(9113, 1.5); // discountinuous seagrass
		suitableHabitats.put(9121, 1.); // algal beds
		suitableHabitats.put(5700, 1.); // tidal flats
		suitableHabitats.put(0, 0.); 
		suitableHabitats.put(2, 0.); 
		suitableHabitatList.add(9116); 
		suitableHabitatList.add(9113); 
		suitableHabitatList.add(9121); 
		suitableHabitatList.add(5700);

		for (int i=-2; i<numYearClasses-2; i++) searchRadiusExponent.put(i,0.5); 

		for (int i=-2; i<numYearClasses-2; i++) minDepth.put(i,0.1);

		for (int i=-2; i<numYearClasses-2; i++) maxDepth.put(i,250.);

		for (int i=-2; i<numYearClasses-2; i++) preferredDepth.put(i,2.);

		for (int i=-2; i<numYearClasses-2; i++) cruiseSpeed.put(i,0.7);

		for (int i=-2; i<numYearClasses-2; i++) maxSpeed.put(i, 0.7);





		//*********************************************************************************************************************************************************************************************************
		//Reprodction Collections

		// Parameters currently without stage differences
		for (int i=-2; i<numYearClasses-2; i++) sexRatio.put(i,.5);

		spawnAggregationList.add(new Coordinate(10, 10)); 
		// TODO ....etc....

		spawnAggregationSpecificity.put(-2, 0.);
		spawnAggregationSpecificity.put(-1, 0.);
		spawnAggregationSpecificity.put(0, 0.);
		spawnAggregationSpecificity.put(1, .3);
		spawnAggregationSpecificity.put(2, .4);
		spawnAggregationSpecificity.put(3, .5);
		spawnAggregationSpecificity.put(4, .6);
		spawnAggregationSpecificity.put(5, .7);
		spawnAggregationSpecificity.put(6, .7);
		spawnAggregationSpecificity.put(7, .7);
		spawnAggregationSpecificity.put(8, .7);
		spawnAggregationSpecificity.put(9, .7);
		spawnAggregationSpecificity.put(10, .7);
		spawnAggregationSpecificity.put(11, .7);
		spawnAggregationSpecificity.put(12, .7);

		fecunditySizeScaler.put(-2,0.);
		fecunditySizeScaler.put(-1,0.);
		fecunditySizeScaler.put(0,0.);
		fecunditySizeScaler.put(1,0.);
		fecunditySizeScaler.put(2,0.);
		fecunditySizeScaler.put(3,0.);
		fecunditySizeScaler.put(4,0.);
		fecunditySizeScaler.put(5,0.);
		fecunditySizeScaler.put(6,0.);
		fecunditySizeScaler.put(7,0.);
		fecunditySizeScaler.put(8,0.);
		fecunditySizeScaler.put(9,0.);
		fecunditySizeScaler.put(10,0.);
		fecunditySizeScaler.put(11,0.);
		fecunditySizeScaler.put(12,0.);

		// From 2006 Stock Assessment (using Sue Lowerre-Barbieri's data), here, this is for total per adult per season
		// Annual fecundities were: age-0, 0 eggs; age-1, 1,919,700 eggs; age-2, 5,854,464 eggs; age-3, 9,224,781 eggs; 
		//age-4, 15,701,400 eggs and; ages 5 and older, 18,048,054 eggs.
/*		baseFecundity.put(-2,0.);
		baseFecundity.put(-1,0.);
		baseFecundity.put(0,0.);
		baseFecundity.put(1,1919700.);
		baseFecundity.put(2,5854464.);
		baseFecundity.put(3,9224781.);
		baseFecundity.put(4,15701400.);
		baseFecundity.put(5,18048054.);
		baseFecundity.put(6,18048054.);
		baseFecundity.put(7,18048054.);
		baseFecundity.put(8,18048054.);
		baseFecundity.put(9,18048054.);
		baseFecundity.put(10,18048054.);
		baseFecundity.put(11,18048054.);
		baseFecundity.put(12,18048054.);
*/

		for (int i=-2; i<numYearClasses-2; i++) {
			ArrayList<Normal> seasonNormalsTemp = new ArrayList<Normal>();  
			// set appropriate values here: add a normal curve for each peak, with Normal(mean, SD, scheduler.getM())
			seasonNormalsTemp.add(new Normal(138, 34.5, sched.getM())); //- set from SpawnOptimize.java
			seasonNormalsTemp.add(new Normal(227, 32.12, sched.getM())); //- set from SpawnOptimize.java
			seasonNormals.put(i, seasonNormalsTemp); 
		}

		for (int i=-2; i<numYearClasses-2; i++) {
			ArrayList<Integer> seasonPeaksTmp = new ArrayList<Integer>(); 
			seasonPeaksTmp.add(138);//- set from SpawnOptimize.java
			seasonPeaksTmp.add(227); //- set from SpawnOptimize.java
			seasonPeaks.put(i, seasonPeaksTmp); 
		}

		for (int i=-2; i<numYearClasses-2; i++) {
			ArrayList<Double> seasonPeakSizesTmp = new ArrayList<Double>(); 
			seasonPeakSizesTmp.add(0.644602);//- set from SpawnOptimize.java
			seasonPeakSizesTmp.add(1.); //- set from SpawnOptimize.java
			seasonPeakSizes.put(i, seasonPeakSizesTmp); 
		}

		for (int i=-2; i<numYearClasses-2; i++) {
			ArrayList<Normal> lunarNormalsTemp = new ArrayList<Normal>();  
			// set appropriate values here: add a normal curve for each peak, with Normal(mean, SD, m)
			lunarNormalsTemp.add(new Normal(0, 5.071560681, sched.getM()));	// set from SpawnLunarOptimize.java
			lunarNormalsTemp.add(new Normal(14, 5.071560681, sched.getM()));	// set from SpawnLunarOptimize.java
			lunarNormals.put(i, lunarNormalsTemp); 
		}

		for (int i=-2; i<numYearClasses-2; i++) {
			ArrayList<Integer> lunarPeaksTmp = new ArrayList<Integer>(); 
			lunarPeaksTmp.add(0);
			lunarPeaksTmp.add(14); 
			lunarPeaks.put(i, lunarPeaksTmp); 
		}

		for (int i=-2; i<numYearClasses-2; i++) {
			ArrayList<Double> lunarPeakSizesTmp = new ArrayList<Double>(); 
			lunarPeakSizesTmp.add(1.);
			lunarPeakSizesTmp.add(1.);
			lunarPeakSizes.put(i, lunarPeakSizesTmp); 
		}

		for (int i=-2; i<numYearClasses-2; i++) lunarPeakShift.put(i, 2);



		//*********************************************************************************************************************************************************************************************************
		//Mortalities and Fishing Management Collections

		//from 2006 seatrout stock assessment, cited as Lorenzen 1996, where this leads to cumulative 0.3 natural mortality
		// note, this is instantaneous mortality rate , M (-yr)
		natInstMortality.put(-2,.99);
		natInstMortality.put(-1,.9);
		natInstMortality.put(0,.73);
		natInstMortality.put(1,.42);
		natInstMortality.put(2,0.36);
		natInstMortality.put(3,0.31);
		natInstMortality.put(4,0.29);
		natInstMortality.put(5,0.26);
		natInstMortality.put(6,0.25);
		natInstMortality.put(7,0.23);
		natInstMortality.put(8,0.22);
		natInstMortality.put(9,0.22);
		natInstMortality.put(10,0.21);
		natInstMortality.put(11,0.20);
		natInstMortality.put(12,0.20);

		double[][] releaseMort = { {0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08}, {0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08} }; 
		for (int i = 1990; i<2007; i++){ // up until regulation update in 1996
			recReleaseMortality.put(i, releaseMort); 
		}

		
		for (int i = 1990; i<1996; i++){ // up until regulation update in 1996
			recMinSizeLimit.put(i, 14d*2.54); 
			recMaxSizeLimit.put(i, 24d*2.54);
			commMinSizeLimit.put(i, 0d); 
			commMaxSizeLimit.put(i, Double.MAX_VALUE);
			recBagLimit.put(i, 10); 
			commBagLimit.put(i, Integer.MAX_VALUE); // is a limit based on poundage, so may need to redo scheme to include at some point 
			int[][] openSeason = {{0, 365}}; 
			recOpenSeasons.put(i, openSeason); 
			commOpenSeasons.put(i, openSeason); 
			double[][]	F_recCatch2	=	{{	0.918849744	,	0.787571038	,	0.515530193	,	0.396091366	,	0.300335628	,	0.249771462	,	0.18879025	,	0.11384297	,	0.11384297	,	0.11384297	,	0.11384297	,	0.11384297	,	0.11384297	},{	0.115474805	,	0.209259726	,	0.288398616	,	0.353376011	,	0.473377621	,	0.657022214	,	0.738927138	,	0.518198556	,	0.518198556	,	0.518198556	,	0.518198556	,	0.518198556	,	0.518198556	}};
			recInstCatchRate.put(i, F_recCatch2);																																																							
			double[][]	F_comm2	=	{{	0.013102755	,	0.010253616	,	0.035676619	,	0.042251383	,	0.041438902	,	0.029497485	,	0.024662583	,	0.010162478	,	0.010162478	,	0.010162478	,	0.010162478	,	0.010162478	,	0.010162478	},{	0.001579426	,	0.000707183	,	0.006306405	,	0.026789467	,	0.051500128	,	0.048731413	,	0.074491025	,	0.062504014	,	0.062504014	,	0.062504014	,	0.062504014	,	0.062504014	,	0.062504014	}};
			commInstMortality.put(i, F_comm2);																																																							
			double[][]	PropReleased2	=	{{	1	,	1	,	1	,	0.976074621	,	0.976120562	,	0.976095359	,	0.969170257	,	0.976103895	,	0.974930908	,	0.971830335	,	0.973733365	,	0.964250408	,	0.915648098	,	0.55357819	,	0.506469641	,	0.509815972	,	0.638949544	,	0.134253373	,	0.71309777	,	0.702255578	,	0.823377199	,	0.670335377	,	0.568088588	,	0.721228664	,	0.784341293	,	0.904754044	,	0.976064696	,	1	,	1	,	1	,	1	,	0.833333333	,	0.833333333	},{	1	,	1	,	1	,	0.976074621	,	0.976120562	,	0.976095359	,	0.969170257	,	0.976103895	,	0.974930908	,	0.971830335	,	0.973733365	,	0.964250408	,	0.915648098	,	0.55357819	,	0.506469641	,	0.509815972	,	0.638949544	,	0.134253373	,	0.71309777	,	0.702255578	,	0.823377199	,	0.670335377	,	0.568088588	,	0.721228664	,	0.784341293	,	0.904754044	,	0.976064696	,	1	,	1	,	1	,	1	,	0.833333333	,	0.833333333	}};
			probCatchAndRelease.put(i, PropReleased2);																																																																																																																																							
		}		

		for (int i = 1996; i<2000; i++){ // up until regulation update in 2000
			recMinSizeLimit.put(i, 15d*2.54); 
			recMaxSizeLimit.put(i, 20d*2.54);
			commMinSizeLimit.put(i, 15d*2.54); 
			commMaxSizeLimit.put(i, 24d*2.54);
			recBagLimit.put(i, 7); 
			commBagLimit.put(i, 75); 
			int[][] recSeason = {{0, 304}}; //Jan1-Oct31
			recOpenSeasons.put(i, recSeason); 
			int[][] commSeason = {{151, 242}}; // June,July, and August
			commOpenSeasons.put(i, commSeason);// TODO 
			double[][]	F_recCatch3	=	{{	0.058253595	,	0.336094518	,	0.841026336	,	0.917429487	,	0.813619776	,	0.711741341	,	0.62444302	,	0.559423975	,	0.559423975	,	0.559423975	,	0.559423975	,	0.559423975	,	0.559423975	},{	0.141670574	,	0.191314553	,	0.246924551	,	0.312992601	,	0.385761894	,	0.464512562	,	0.542634043	,	0.543829	,	0.543829	,	0.543829	,	0.543829	,	0.543829	,	0.543829	}};
			recInstCatchRate.put(i, F_recCatch3);																																																							
			double[][]	F_comm3	=	{{	0.000100651	,	0.000147135	,	0.002487313	,	0.007795138	,	0.009255725	,	0.00571888	,	0.003116345	,	0.000562927	,	0.000562927	,	0.000562927	,	0.000562927	,	0.000562927	,	0.000562927	},{	0.000188917	,	1.88595E-05	,	0.000225683	,	0.000877154	,	0.002334028	,	0.003860236	,	0.005648842	,	0.005152759	,	0.005152759	,	0.005152759	,	0.005152759	,	0.005152759	,	0.005152759	}};
			commInstMortality.put(i, F_comm3);				
			double[][]	PropReleased3	=	{{	1	,	1	,	1	,	1	,	0.975609381	,	0.975632388	,	0.97218398	,	0.975619798	,	0.974167221	,	0.975015223	,	0.974674216	,	0.9752831	,	0.969094823	,	0.92742428	,	0.661431328	,	0.614167862	,	0.574662	,	0.678789986	,	0.199043285	,	0.750587346	,	0.747047605	,	0.853305367	,	0.827819438	,	0.761456778	,	0.801159622	,	0.739470946	,	0.975535334	,	0.975535334	,	1	,	1	,	1	,	1	,	1	},{	1	,	1	,	1	,	1	,	0.975609381	,	0.975632388	,	0.97218398	,	0.975619798	,	0.974167221	,	0.975015223	,	0.974674216	,	0.9752831	,	0.969094823	,	0.92742428	,	0.661431328	,	0.614167862	,	0.574662	,	0.678789986	,	0.199043285	,	0.750587346	,	0.747047605	,	0.853305367	,	0.827819438	,	0.761456778	,	0.801159622	,	0.739470946	,	0.975535334	,	0.975535334	,	1	,	1	,	1	,	1	,	1	}};
			probCatchAndRelease.put(i, PropReleased3);			
			

		}		

		for (int i = 2000; i<2007; i++){ // up until 2006
			recMinSizeLimit.put(i, 15d*2.54); 
			recMaxSizeLimit.put(i, 20d*2.54);
			commMinSizeLimit.put(i, 15d*2.54); 
			commMaxSizeLimit.put(i, 24d*2.54);
			recBagLimit.put(i, 4); 
			commBagLimit.put(i, 75); 
			int[][] recSeason = {{0, 304}}; //Jan1-Oct31
			recOpenSeasons.put(i, recSeason); 
			int[][] commSeason = {{151, 242}}; // June,July, and August
			commOpenSeasons.put(i, commSeason);// TODO 
			double[][]	F_recCatch4	=	{{	0.068573661	,	0.400217495	,	1.006661231	,	1.110390675	,	0.990148016	,	0.858288855	,	0.743268831	,	0.748024487	,	0.748024487	,	0.748024487	,	0.748024487	,	0.748024487	,	0.748024487	},{	0.170436355	,	0.227023516	,	0.294122683	,	0.377594963	,	0.46797583	,	0.561796213	,	0.656340139	,	0.636848681	,	0.636848681	,	0.636848681	,	0.636848681	,	0.636848681	,	0.636848681	}};
			recInstCatchRate.put(i, F_recCatch4);																																																							
			double[][]	F_comm4	=	{{	3.07379E-05	,	9.25526E-05	,	0.000883558	,	0.002523288	,	0.002906165	,	0.002387748	,	0.001472953	,	0.000996691	,	0.000996691	,	0.000996691	,	0.000996691	,	0.000996691	,	0.000996691	},{	0.000157646	,	1.80967E-05	,	0.000112332	,	0.000281653	,	0.000591351	,	0.001353902	,	0.001911264	,	0.001772451	,	0.001772451	,	0.001772451	,	0.001772451	,	0.001772451	,	0.001772451	}};
			commInstMortality.put(i, F_comm4);
			double[][]	PropReleased4	=	{{	1	,	1	,	0.997232962	,	0.998484848	,	0.992375132	,	0.989582568	,	0.987912632	,	0.985958912	,	0.98415752	,	0.984426996	,	0.981982238	,	0.982897517	,	0.976089623	,	0.951017567	,	0.811786893	,	0.68614396	,	0.679524278	,	0.726681974	,	0.646370919	,	0.784061891	,	0.664614032	,	0.723169411	,	0.921719874	,	0.644529285	,	0.656499505	,	0.443002408	,	0.82833767	,	0.827244919	,	1	,	1	,	1	,	1	,	1	},{	1	,	1	,	0.997232962	,	0.998484848	,	0.992375132	,	0.989582568	,	0.987912632	,	0.985958912	,	0.98415752	,	0.984426996	,	0.981982238	,	0.982897517	,	0.976089623	,	0.951017567	,	0.811786893	,	0.68614396	,	0.679524278	,	0.726681974	,	0.646370919	,	0.784061891	,	0.664614032	,	0.723169411	,	0.921719874	,	0.644529285	,	0.656499505	,	0.443002408	,	0.82833767	,	0.827244919	,	1	,	1	,	1	,	1	,	1	}};
			probCatchAndRelease.put(i, PropReleased4);																																																																																																																																							

		}		

		
	}





	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// FUNCTIONAL RELATIONSHIPS -- SET FOR EACH FISH TYPE

	//TODO -- need to fill this in appropriately from Sven's paper; and make my own weightings for homeRange, density, and rugosity 

	@Override
	public double getHabQualFunction(String string, int yearClass, double value) { 
		if (string.equals("salinity")) return 0; 
		else if (string.equals("SAVCover")) return 0;
		else if (string.equals("density")) return 0;
		else if (string.equals("depth")) return 0;
		else if (string.equals("homeRange")) return 0; // will have to make this myself -- set to ~average partial effect value as others, but will have to calibrate; here, make - to + as with others, where 3km is 0
		else if (string.equals("temperature")) return 0;
		else if (string.equals("rugosity")) return 0;
		else return 0; 
	}

	@Override
	public double getBaseFecundity(int yearClass, double mass) {
		return 669.6*mass -43139d ; // from 2006 trout stock assessment, citing Barbieri 2004
		// return 637.3*mass - 53905d; // from Sue's MEPS data, similar to Barbieri 2004 but only 148 records
	//	return baseFecundity.get(yearClass);
	}
	
	
	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//RETURN METHODS FOR STATIC ASSIGNEMENTS -- IN GENERAL, DO NOT CHANGE BELOW THIS 


	@Override
	public int getAgeMax() {
		return ageMax;
	}



	@Override
	public int getCarryCapacity() {
		return carryCapacity;
	}

	@Override
	public double getCruiseSpeed(int yearClass) {
		return cruiseSpeed.get(yearClass);
	}

	@Override
	public double getCullPeriod() {
		return cullPeriod;
	}

	@Override
	public ArrayList<String> getDependentsTypeList() {
		return dependentsTypeList;
	}

	@Override
	public double getDirectionalVariability() {
		return directionalVariability;
	}

	@Override
	public double getEndOfBreeding() {
		return endOfBreeding;
	}

	@Override
	public double getFallowPeriod(int yearClass) {
		return fallowPeriods[yearClass];
	}

	@Override
	public double getFallowPeriodSD() {
		return fallowPeriodSD;
	}

	@Override
	public double getFecunditySizeScaler(int yearClass) {
		return fecunditySizeScaler.get(yearClass);
	}

	@Override
	public int getSchoolSize() {
		return schoolSize;
	}

	@Override
	public double getSizeAtMaturityAvg(){
		return sizeAtMaturityAvg; 
	}

	@Override
	public double getSizeAtMaturitySD(){
		return sizeAtMaturitySD;
	}
	
	@Override
	public double getFertilizationRtAvg(int yearClass) {
		return fertRtAvgs[yearClass];
	}
	
	@Override
	public double getFertilizationRtSD() {
		return fertRtSD;
	}

	public int getAgeAtRecruitment(){
		return ageAtRecruitment; 
	}



	@Override
	public double getIniMass(int sex) {
		return iniMass[sex];
	}

	@Override
	public double getMaxMass(int sex) {
		return maxMass[sex];
	}


	@Override
	public double getLengthAtMass(double mass, int sex) {
		return Math.exp((Math.log(mass/a[sex]))/b[sex]);
	}

	@Override
	public double getMassAtLength(double length, int sex) {
		return a[sex]*Math.pow(length, b[sex]);
	}

	
	@Override
	public double getLengthAtAge(double age, int sex) {
		return vonBertLinf[sex]*(1-Math.exp(-vonBertK[sex]*(age-vonBertT0[sex])));
	}

	@Override
	public HashMap<String, Long> getInteractionTicks() {
		return interactionTicks;
	}


	@Override
	public double getMassCatchUpRate() {
		return massCatchUpRate;
	}

	@Override
	public double getMaxDepth(int yearClass) {
		return maxDepth.get(yearClass);
	}


	@Override
	public double getMaxSpeed(int yearClass) {
		return maxSpeed.get(yearClass);
	}

	@Override
	public int getMergeRadius() {
		return mergeRadius;
	}

	@Override
	public double getMinDepth(int yearClass) {
		return minDepth.get(yearClass);
	}

	@Override
	public long getNormalTick() {
		return normalTick;
	}

	@Override
	public int getNumPointsPerSpiral() {
		return numPointsPerSpiral;
	}

	@Override
	public int getNumTestSites() {
		return numTestSites;
	}


	@Override
	public double getPreferredDepth(int yearClass) {
		return preferredDepth.get(yearClass);
	}

	@Override
	public String getQueueType() {
		return queueType;
	}

	@Override
	public int getRunPriority() {
		return runPriority;
	}

	@Override
	public double getScaleOfPerception(int yearClass) {
		return scaleOfPerception.get(yearClass);
	}

	@Override
	public double getSearchRadiusExponent(int yearClass) {
		return searchRadiusExponent.get(yearClass);
	}

	@Override
	public boolean hasHomeRange(){
		return hasHomeRange; 
	}

	@Override 
	public String getHabitatSearchComplexity(){
		return habitatSearchComplexity; 
	}

	@Override
	public double getSexRatio(int yearClass) {
		return sexRatio.get(yearClass);
	}

	@Override
	public double getStartOfBreeding() {
		return startOfBreeding;
	}

	@Override
	public ArrayList<Integer> getSuitableHabitatList() {
		return suitableHabitatList;
	}

	@Override
	public HashMap<Integer, Double> getSuitableHabitats() {
		return suitableHabitats;
	}

	@Override
	public boolean haveSpawnAggregations() {
		return haveSpawnAggregations;
	}



	@Override
	public boolean isRunnable() {
		return isRunnable;
	}

	@Override
	public boolean isMultipleSpawner() {
		return isMultipleSpawner;
	}

	@Override
	public boolean isReactive() {
		return isReactive;
	}



	@Override
	public ArrayList<Coordinate> getSpawnAggregationList() {
		return spawnAggregationList;
	}

	@Override
	public double getSpawnAggregationSpecificity(int yearClass) {
		return spawnAggregationSpecificity.get(yearClass); 
	}


	@Override
	public ArrayList<Calendar> getSetRunTimes() {
		return null; 
	}

	@Override
	public boolean isSetRunTimes() {
		return isSetRunTimes;
	}


	@Override
	public ArrayList<Normal> getLunarNormals(int yearClass) {
		return lunarNormals.get(yearClass);
	}


	@Override
	public int getLunarPeakShift(int yearClass) {
		return lunarPeakShift.get(yearClass); 
	}


	@Override
	public ArrayList<Integer>  getLunarPeaks(int yearClass) {
		return lunarPeaks.get(yearClass);
	}

	@Override
	public ArrayList<Double>  getLunarPeakSizes(int yearClass) {
		return lunarPeakSizes.get(yearClass);
	}

	@Override
	public ArrayList<Normal> getSeasonNormals(int yearClass) {
		return seasonNormals.get(yearClass);
	}


	@Override
	public ArrayList<Integer> getSeasonPeaks(int yearClass) {
		return seasonPeaks.get(yearClass);
	}

	@Override
	public ArrayList<Double> getSeasonPeakSizes(int yearClass) {
		return seasonPeakSizes.get(yearClass);
	}




	@Override
	public int getCommBagLimit(int year) {
		return commBagLimit.get(year);
	}
	@Override
	public int getRecBagLimit(int year) {
		return recBagLimit.get(year);
	}

	@Override
	public double getNatInstMort(int yearClass, double size, int sex) {
		return natInstMortality.get(yearClass);
	}

	@Override
	public double getCommInstMortality(int year, double length, int yearClass, int sex) {
		return commInstMortality.get(year)[sex][yearClass];
	}

	@Override
	public double getRecInstCatchRate(int year, double length, int yearClass, int sex) {
		return this.recInstCatchRate.get(year)[sex][yearClass];
	}

	@Override
	public double getProbOfCatchAndRelease(int year, double length, int yearClass, int sex) {
		return probCatchAndRelease.get(year)[sex][(int)Math.round(length)];
	}

	@Override
	public double getRecReleaseMortality(int year, double length, int yearClass, int sex) {
		return recReleaseMortality.get(year)[sex][yearClass];
	}

	@Override
	public boolean isCommLegalSize(int year, double length) {
		double minSize = this.commMinSizeLimit.get(year); 
		double maxSize = this.commMaxSizeLimit.get(year); 
		if ( (length >= minSize) && (length <= maxSize)) return true;
		else return false;
	}

	@Override
	public boolean isRecLeagalSize(int year, double length) {
		double minSize = this.recMinSizeLimit.get(year); 
		double maxSize = this.recMaxSizeLimit.get(year); 
		if ( (length >= minSize) && (length <= maxSize)) return true;
		else return false;
	}


	@Override
	public boolean isCommOpenSeason(Calendar currentDate) {
		int year = currentDate.get(Calendar.YEAR); 
		int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR); 
		int[][] thisYearsSeasons = commOpenSeasons.get(year);  
		for (int i=0; i< thisYearsSeasons.length; i++){
			int startDay = thisYearsSeasons[i][0]; 
			int endDay = thisYearsSeasons[i][1]; 
			if ((dayOfYear >= startDay) && (dayOfYear <= endDay)) return true; 
		}
		return false;
	}


	@Override
	public boolean isRecOpenSeason(Calendar currentDate) {
		int year = currentDate.get(Calendar.YEAR); 
		int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR); 
		int[][] thisYearsSeasons = recOpenSeasons.get(year);  
		for (int i=0; i< thisYearsSeasons.length; i++){
			int startDay = thisYearsSeasons[i][0]; 
			int endDay = thisYearsSeasons[i][1]; 
			if ((dayOfYear >= startDay) && (dayOfYear <= endDay)) return true; 
		}
		return false;
	}

	@Override
	public int getCommOpenSeasonNumDays(int year) {
		int[][] thisYearsSeasons = commOpenSeasons.get(year);
		int totalDays = 0; 
		for (int i=0; i< thisYearsSeasons.length; i++){
			int startDay = thisYearsSeasons[i][0]; 
			int endDay = thisYearsSeasons[i][1];
			totalDays += endDay-startDay; 
		}
		return totalDays;
	}


	@Override
	public int getRecOpenSeasonNumDays(int year) {
		int[][] thisYearsSeasons = recOpenSeasons.get(year);
		int totalDays = 0; 
		for (int i=0; i< thisYearsSeasons.length; i++){
			int startDay = thisYearsSeasons[i][0]; 
			int endDay = thisYearsSeasons[i][1];
			totalDays += endDay-startDay; 
		}
		return totalDays;
	}

}
