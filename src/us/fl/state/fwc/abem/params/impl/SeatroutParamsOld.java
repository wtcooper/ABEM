package us.fl.state.fwc.abem.params.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.AbstractParams;
import cern.jet.random.Normal;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class SeatroutParamsOld extends AbstractParams {


	//###############################################################
	//Special properties -- will need to cast params to SeatroutParams

	public boolean useAgeTrunc = false; 
	public boolean useSizeDependentSpawnSeason = true;
	public boolean useSizeDependentFallowPeriod = true;
	public boolean useSizeDependentEggViability = true;
	public double buncesFertScaler = 1; 

	
	
	
	private boolean useSimpleF = false; //this is to use simple average F per age per sex from SA

	//old flags
	public boolean useLinearFecundity = true; 
	public boolean useSpawnAggs = false;
	public boolean useFertScaler=  true;

	//if this is false, will distribute by 
	public boolean distributeByAbundance = true;

	public boolean useYrRecDeviations = false; //turns on yearly recruit deviations (e.g., due to environmental fluctuations, trophics, etc

	//Total numbers variables
	/*To get the intitialTotalAbundance, calculated the % commercial landings (as lbs/trips) in Pinellis, Hillsborough, and Manatee
	 * relative to the total landings (lbs/trips) in SW region, and then multiplied the total abundance in SW region by
	 * this proportion.  Therefore assumming that those commercial landings in these three counties represent
	 * the proportion of total seatrout in focal area of tampa bay.
	 * 
	 * Note: this value (6,260,925) compares relatively well to an expected total abundance from Dave's TB Ecosim,
	 * where he estimated 1.75 metric tons/km for all ages of seatrout in 2004 -- this is similar in the order of 4-6million
	 * total fish, depending on the area of Tampa Bay used
	 */

	public boolean useConstantPopn= false; //starts different sims at same popnAbundance and size freq, but can be different F's 
	public boolean useYrSpecificPopn = false; //starts different simulations with year specific values of abundance and size frequency, and uses year specific F's

	private int popnAbundance = 10000000; //6260925;  
	private int popnAbundAgeTrunc = 10000000; //5041870;  

	//for 1982 abundance: 5041870
	private double popnMass = 2166228082.5384; //this is mass for 2004 size freq with 6,260,925 individuals
	private int schoolSize=1;							//School size: smallest size of school after which a school will want to merge with another school

	private double avgAge2004 = 3;
	private double avgAge1982 = 2;


	/*size freq array was calculated from 2010 stock assessment (SA2010_data.xlsx), using SW region -- this is for females but males and females are pretty much identical
	 * Note: to get prop of age 7+ (those were aggregated in SA), fit an exponential curve to 2004 data
	 * without the 7+ group, then distributed the total proportion of 7+ to each 7-12 age class using the 
	 * exponential fit
	 * 
	 * 1982-1984	0.53504275	0.26489779	0.13343873	0.04442343	0.01447095	0.00487685	0.00177951	0.00064604	0.00025010	0.00009682	0.00003748	0.00001451	0.00000562
	 * 2007-2009	0.44362352	0.21427521	0.12337937	0.07365055	0.04738421	0.03278555	0.02197748	0.01513715	0.01032070	0.00703679	0.00479777	0.00327118	0.00223034
	 * 
	 */
	//########## NORMAL AGE ############
	private double[] sizeFreqArray = {0.53504275, 0.26489779, 0.13343873, 0.04442343, 0.01447095, 0.00487685, 0.00177951, 0.00064604, 0.00025010, 0.00009682, 0.00003748, 0.00001451, 0.00000562} ;  

	//########## TRUNCATED AGE ############
	//SF distribution from 1982 as per 2006 Seatrout SA
	private double[] sizeFreqArrayTruncated = {0.44362352, 0.21427521, 0.12337937, 0.07365055, 0.04738421, 0.03278555, 0.02197748, 0.01513715, 0.01032070, 0.00703679, 0.00479777, 0.00327118, 0.00223034} ;  

	private ArrayList<Integer> spawnAggSize; // = new ArrayList<Coordinate>();  // waypoint list of known spawn aggregations
	public String spawnAggFilename = "data/HydroSurveys_largeAggs.shp";

	//###############################################################

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

	private HashMap<Integer, Double>  yrRecDeviations = new HashMap<Integer, Double>(); 						//A scaler for yearly recruit deviations
	private HashMap<Integer, Double>  homeRanges = new HashMap<Integer, Double>(); 						//TODO A scaler which adjusts the batch fecundity to reflect size-dependent changes in fecundity; here, higher the value, greater size dependency
	private HashMap<Integer, Double>  spawnAggregationWeights = new HashMap<Integer, Double>(); 						//TODO A scaler which adjusts the batch fecundity to reflect size-dependent changes in fecundity; here, higher the value, greater size dependency


	//baseFecundity is set with an equation here, based on biomass, versus age class
	//private HashMap<Integer, Double>  baseFecundity= new HashMap<Integer, Double>(); 				//TODO Total number of eggs that a female produces in a batch; i.e., Batch Fecundity 
	private HashMap<Integer, Double>  natInstMortality= new HashMap<Integer, Double>(); 						//TODO Base mortality rate, M/year
	private HashMap<Integer, double[]>  fishInstMortality= new HashMap<Integer, double[]>(); 						//TODO Base mortality rate, M/year
	private HashMap<Integer, double[][]>  fishInstMortByYear = new HashMap<Integer, double[][]>(); 						//TODO Base mortality rate, M/year

	private HashMap<Integer, double[][]>  sizeFreqByYear = new HashMap<Integer, double[][]>(); 						//TODO Base mortality rate, M/year
	private HashMap<Integer, int[]>  fishAbundByYear = new HashMap<Integer, int[]>(); 						//TODO Base mortality rate, M/year


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



		//###############################################################
		// SET ALL VARIABLE ASSIGNMENTS HERE
		// AGENT.CLASS VARIABLES
		private boolean isSetRunTimes = false;
		private boolean isReactive=true; 						//If agent interacts with others, it's reactive. 
		private boolean isRunnable=true;						//If agent changes over time, it's dynamic.  E.g., Bathymetry would not be dynamic
		private String queueType="runQueue";				//The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing
		private long normalTick=24l /*hour*/ * (60*60*1000); 								//TODO The normal tick length when no interactions are present
		private int runPriority=3; 									//The priority as to what should run first; here, monitors have higher priority than organisms

		//ANIMAL.CLASS VARIABLES 

		//******************************************************
		// FEMALES ARE INDEX 0; MALES ARE INDEX 1
		//******************************************************

		//Spawning variables
		private double fallowPeriodSD = 2; 
		private double startOfBreeding= 30; //59 is 1st day in Sue's data 
		private double endOfBreeding=287; //266 is last day in Sue's data 
		private boolean haveSpawnAggregations=true;	//If agent has known spawning aggregations
		private boolean isMultipleSpawner=true; 			//TODO If an agent spawns multiple times in a single breeding season
		private int ageMax=12; 								//TODO Max age in years
		private double sizeAtMaturityAvg=26.8002; 	
		private double propSetSizeAtMat = .75; 
		private double sizeAtMaturitySD=2.196391585;
		private double conditionInfluenceOnSpawn = 4; //determines the influence that condition will have on determining the timing of spawning: condition^condInflOnSpawn * other probs of spawning
		private double sizeInflOnSpawnIntcpt = sizeAtMaturityAvg*propSetSizeAtMat;
		private double sizeInflOnSpawnKm = sizeAtMaturityAvg*1.25;
		private double fertRtSD = .05;
		private int avgPLD = 18; 								// actual PLD's are shorter (most settled in Peebles and Tolly by age 10, but mort rate in said cite is up through 18 days, and Reefbase uses 19 days for LD 
		private int avgEPSD = 90-avgPLD; 								//this is for age 10-90 days, using Powell et al. 2004 growth/mort rates
		private int ageAtRecruitment = 1; 
		private double recruitDeviation = .1; 				//10% deviation, using beta distribution, for recruit deviations

		//crap vals -- old michalis menton K values -- fit is better but confusing and not standard
		private double[]  c= {2.0301,2.0301};  									
		private double[]  mmK= {7.1504,7.1504};  				

		//Growth params -- Good values for double von bert for both sexes: FEMALES = 0, MALES = 1
		private double[] tp = {1.261873109, 1.198195743};
		private double[] vonBertLinf = {68.1123, 67.9246};
		private double[][] vonBertK = {{0.5536, 0.1728},{0.4895, 0.0878}};
		private double[][] vonBertT0= {{.0957, -2.4742}, {.0745, -5.0666}};

		private double[] a = {0.0112364 , 0.0112364 }; //set the same - no diff b/w males/females
		private double[] b = {2.9360025, 2.9360025}; 
		private double massStDevScalar = .09; //this is optimized amount to multiply by expected mass to get the stDev of the mass estimate -- then can assign a random mass as normal.nextDouble(estMass, estMass*stDevScalar)
		//	private double weightLengthSlope = 2.922971822426165;				//slope of log10(SomaticWeight, g) = log10(TL, mm)
		//	private double weightLengthIntcept = -4.853725003139546;			//intercept of log10(SomaticWeight, g) = log10(TL, mm)
		//	private double weightLengthStDev = .0422; 									//optimized standard deviation of log10(SomaticWeight, g) = log10(TL, mm); e.g., a weight can be assigned as N(est weight at length, st dev)


		//early post-settlement DD mortality rates -- set maxMOffsets to 0,0 to turn of DD mortality
		private double[] epsM = {.5, .0585}; //{.5, .0585}; //early post-settlement M for initial period (18 days) and longer EPS period (3 months)
		private double[] epsMAdultVmax = {.1,.02}; //max offset in M due to adult density for initial period (18 days) and longer EPS period (3 months)
		private double[] epsMSettlerVmax = {.1, .02}; //max offset in M due to settler density for initial period (18 days) and longer EPS period (3 months)
		private double[] epsMAdultKm = {.05, .05}; //strength of influence / rate of change in offset in M due to adult density for initial period (18 days) and longer EPS period (3 months)
		private double[] epsMSettlerKm = {.001, .001}; //strength of influence / rate of change in offset in M due to settler density for initial period (18 days) and longer EPS period (3 months)
		private double[] epsMAdultExpon = {1, 1}; //average density of adults per m2
		private double[] epsMSettlerExpon = {2, 2}; //average density of settlers per m2 



		//mostly crap, probably can get rid of all
		private double[] iniMass= {0.001, 0.001}; 							//TODO Initial mass, i.e. mass of an egg 
		private double[] maxMass={4.569382*1000, 4.569382*1000}; 					//TODO Maximum mass per individuals for this species -- in GRAMS
		private int massCatchUpRate=6; 						//TODO Rate at which true mass will catch up to nominal mass, e.g., if a fish is starved and has now found steady food, or after a reproduction event
		private double cullPeriod=1000000; 					//TODO Cull period is the time frame over which density dependent mortality occurs when a species is above carrying capacity; see Gray et al. 2006
		private int carryCapacity=1000000; 					//TODO Carrying capacity in mass (kg) for the entire domain; note, a biomass monitor will keep track of the running total of biomass
		private int mergeRadius=100;							//TODO Maximum distance at which a school looking to merge can detect other schools in vicinity (m
		private int numTestSites=36;								//Number of test sites to use when assessing habitat suitability in the surrounding area
		private double directionalVariability=1; 			//The amount of "wandering" an agent will do while traveling along a path towards a waypoint; from Gray et al. 2006 algorithm 
		private int numPointsPerSpiral=6;						//Number of points to use in a spiral for the habitat search algorithm; see Gray et al. 2006 for description 
		private String habitatSearchComplexity = "1st tier"; 
		private boolean hasHomeRange = true; 

		//SeatroutBuilder Parameters



		//###############################################################
		// CLASS INSTANTIATION -- SET ALL COLLECTIONS ASSIGNMENTS HERE 

		public void initialize (Scheduler sched){

			//only set the age truncation abundance and size frequency if NOT useConstantPopn
			if (useAgeTrunc && !useConstantPopn) {
				sizeFreqArray = sizeFreqArrayTruncated;
				popnAbundance = popnAbundAgeTrunc;
			}
			if (useYrSpecificPopn){
				
			}

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

			homeRanges.put(-2, .001); 
			homeRanges.put(-1, .001); 
			homeRanges.put(0, .012); 
			homeRanges.put(1, .014); 
			homeRanges.put(2, .016); 
			homeRanges.put(3, .018); 
			homeRanges.put(4, .020); 
			homeRanges.put(5, .021); 
			homeRanges.put(6, .022); 
			homeRanges.put(7, .0225); 
			homeRanges.put(8, .023); 
			homeRanges.put(9, .0235); 
			homeRanges.put(10, .024); 
			homeRanges.put(11, .0245); 
			homeRanges.put(12, .025); 


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

			dependentsTypeList.add("Seatrout"); 
			interactionTicks.put("Seatrout", 1l /*hour*/ * (60*60*1000)); 

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

			//spawnAggregationList.add(new Coordinate(10, 10)); 
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

			if (useSpawnAggs) getSpawnAggList();

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


			if (!useSizeDependentSpawnSeason){

				for (int i=-2; i<numYearClasses-2; i++) {
					ArrayList<Normal> seasonNormalsAge3 = new ArrayList<Normal>();  
					seasonNormalsAge3.add(new Normal(138+9.5*2.5, 15.5, sched.getM())); 
					seasonNormalsAge3.add(new Normal(227-9.5, 15.5, sched.getM())); 
					seasonNormals.put(i, seasonNormalsAge3); 
				}

				/*			
			if (!useAgeTrunc) {
				for (int i=-2; i<numYearClasses-2; i++) {
					ArrayList<Normal> seasonNormalsAge3 = new ArrayList<Normal>();  
					seasonNormalsAge3.add(new Normal(138+9*2, 16, sched.getM())); 
					seasonNormalsAge3.add(new Normal(227-9, 16, sched.getM())); 
					seasonNormals.put(i, seasonNormalsAge3); 
				}
			}
			else {
				for (int i=-2; i<numYearClasses-2; i++) {
					ArrayList<Normal> seasonNormalsAge2 = new ArrayList<Normal>();  
					seasonNormalsAge2.add(new Normal(138+10*3, 15, sched.getM())); 
					seasonNormalsAge2.add(new Normal(227-10, 15, sched.getM())); 
					seasonNormals.put(i, seasonNormalsAge2); 
				}
			}
				 */
			}

			else {
				ArrayList<Normal> seasonNormalsAgeMin2 = new ArrayList<Normal>();  
				seasonNormalsAgeMin2.add(new Normal(138+12*4, 11, sched.getM())); 
				seasonNormalsAgeMin2.add(new Normal(227-12, 11, sched.getM())); 
				seasonNormals.put(-2, seasonNormalsAgeMin2); 

				ArrayList<Normal> seasonNormalsAgeMin1 = new ArrayList<Normal>();  
				seasonNormalsAgeMin1.add(new Normal(138+12*4, 11, sched.getM())); 
				seasonNormalsAgeMin1.add(new Normal(227-12, 11, sched.getM())); 
				seasonNormals.put(-1, seasonNormalsAgeMin1); 

				ArrayList<Normal> seasonNormalsAge0 = new ArrayList<Normal>();  
				seasonNormalsAge0.add(new Normal(138+12*4, 11, sched.getM())); 
				seasonNormalsAge0.add(new Normal(227-12, 11, sched.getM())); 
				seasonNormals.put(0, seasonNormalsAge0); 

				ArrayList<Normal> seasonNormalsAge1 = new ArrayList<Normal>();  
				seasonNormalsAge1.add(new Normal((int) (138+11*3.5), 13, sched.getM())); 
				seasonNormalsAge1.add(new Normal(227-11, 13, sched.getM())); 
				seasonNormals.put(1, seasonNormalsAge1); 

				ArrayList<Normal> seasonNormalsAge2 = new ArrayList<Normal>();  
				seasonNormalsAge2.add(new Normal(138+10*3, 15, sched.getM())); 
				seasonNormalsAge2.add(new Normal(227-10, 15, sched.getM())); 
				seasonNormals.put(2, seasonNormalsAge2); 

				ArrayList<Normal> seasonNormalsAge3 = new ArrayList<Normal>();  
				seasonNormalsAge3.add(new Normal(138+9*2, 16, sched.getM())); 
				seasonNormalsAge3.add(new Normal(227-9, 16, sched.getM())); 
				seasonNormals.put(3, seasonNormalsAge3); 

				ArrayList<Normal> seasonNormalsAge4 = new ArrayList<Normal>();  
				seasonNormalsAge4.add(new Normal(138+8*2, 18, sched.getM())); 
				seasonNormalsAge4.add(new Normal(227-8, 18, sched.getM())); 
				seasonNormals.put(4, seasonNormalsAge4); 

				ArrayList<Normal> seasonNormalsAge5 = new ArrayList<Normal>();  
				seasonNormalsAge5.add(new Normal(138+7*2, 20, sched.getM())); 
				seasonNormalsAge5.add(new Normal(227-7, 20, sched.getM())); 
				seasonNormals.put(5, seasonNormalsAge5); 

				ArrayList<Normal> seasonNormalsAge6 = new ArrayList<Normal>();  
				seasonNormalsAge6.add(new Normal(138+6*2, 21, sched.getM())); 
				seasonNormalsAge6.add(new Normal(227-6, 21, sched.getM())); 
				seasonNormals.put(6, seasonNormalsAge6); 

				ArrayList<Normal> seasonNormalsAge7 = new ArrayList<Normal>();  
				seasonNormalsAge7.add(new Normal(138+5*2, 22, sched.getM())); 
				seasonNormalsAge7.add(new Normal(227-5, 22, sched.getM())); 
				seasonNormals.put(7, seasonNormalsAge7); 

				ArrayList<Normal> seasonNormalsAge8 = new ArrayList<Normal>();  
				seasonNormalsAge8.add(new Normal(138+4*2, 23, sched.getM())); 
				seasonNormalsAge8.add(new Normal(227-4, 23, sched.getM())); 
				seasonNormals.put(8, seasonNormalsAge8); 

				ArrayList<Normal> seasonNormalsAge9 = new ArrayList<Normal>();  
				seasonNormalsAge9.add(new Normal(138+3*2, 24, sched.getM())); 
				seasonNormalsAge9.add(new Normal(227-3, 24, sched.getM())); 
				seasonNormals.put(9, seasonNormalsAge9); 

				ArrayList<Normal> seasonNormalsAge10 = new ArrayList<Normal>();  
				seasonNormalsAge10.add(new Normal(138+2*2, 25, sched.getM())); 
				seasonNormalsAge10.add(new Normal(227-2, 25, sched.getM())); 
				seasonNormals.put(10, seasonNormalsAge10); 

				ArrayList<Normal> seasonNormalsAge11 = new ArrayList<Normal>();  
				seasonNormalsAge11.add(new Normal(138+2, 26, sched.getM())); 
				seasonNormalsAge11.add(new Normal(227-1, 26, sched.getM())); 
				seasonNormals.put(11, seasonNormalsAge11); 

				ArrayList<Normal> seasonNormalsAge12 = new ArrayList<Normal>();  
				seasonNormalsAge12.add(new Normal(138, 27, sched.getM())); 
				seasonNormalsAge12.add(new Normal(227, 27, sched.getM())); 
				seasonNormals.put(12, seasonNormalsAge12); 

			}

			if (!useSizeDependentSpawnSeason){
				for (int i=-2; i<numYearClasses-2; i++) {
					if (!useAgeTrunc) {
						ArrayList<Integer> seasonPeaksTmp3 = new ArrayList<Integer>(); 
						seasonPeaksTmp3.add(138+9*2);//- set from SpawnOptimize.java
						seasonPeaksTmp3.add(227-9); //- set from SpawnOptimize.java
						seasonPeaks.put(i, seasonPeaksTmp3); 
					}
					else {
						ArrayList<Integer> seasonPeaksTmp2 = new ArrayList<Integer>(); 
						seasonPeaksTmp2.add(138+10*3);//- set from SpawnOptimize.java
						seasonPeaksTmp2.add(227-10); //- set from SpawnOptimize.java
						seasonPeaks.put(i, seasonPeaksTmp2); 
					}
				}
			}
			else {
				ArrayList<Integer> seasonPeaksTmpMin2 = new ArrayList<Integer>(); 
				seasonPeaksTmpMin2.add(138+12*4);//- set from SpawnOptimize.java
				seasonPeaksTmpMin2.add(227-12); //- set from SpawnOptimize.java
				seasonPeaks.put(-2, seasonPeaksTmpMin2); 

				ArrayList<Integer> seasonPeaksTmpMin1 = new ArrayList<Integer>(); 
				seasonPeaksTmpMin1.add(138+12*4);//- set from SpawnOptimize.java
				seasonPeaksTmpMin1.add(227-12); //- set from SpawnOptimize.java
				seasonPeaks.put(-1, seasonPeaksTmpMin1); 

				ArrayList<Integer> seasonPeaksTmp0 = new ArrayList<Integer>(); 
				seasonPeaksTmp0.add(138+12*4);//- set from SpawnOptimize.java
				seasonPeaksTmp0.add(227-12); //- set from SpawnOptimize.java
				seasonPeaks.put(0, seasonPeaksTmp0); 

				ArrayList<Integer> seasonPeaksTmp1 = new ArrayList<Integer>(); 
				seasonPeaksTmp1.add((int) (138+11*3.5));//- set from SpawnOptimize.java
				seasonPeaksTmp1.add(227-11); //- set from SpawnOptimize.java
				seasonPeaks.put(1, seasonPeaksTmp1); 

				ArrayList<Integer> seasonPeaksTmp2 = new ArrayList<Integer>(); 
				seasonPeaksTmp2.add(138+10*3);//- set from SpawnOptimize.java
				seasonPeaksTmp2.add(227-10); //- set from SpawnOptimize.java
				seasonPeaks.put(2, seasonPeaksTmp2); 

				ArrayList<Integer> seasonPeaksTmp3 = new ArrayList<Integer>(); 
				seasonPeaksTmp3.add(138+9*2);//- set from SpawnOptimize.java
				seasonPeaksTmp3.add(227-9); //- set from SpawnOptimize.java
				seasonPeaks.put(3, seasonPeaksTmp3); 

				ArrayList<Integer> seasonPeaksTmp4 = new ArrayList<Integer>(); 
				seasonPeaksTmp4.add(138+8*2);//- set from SpawnOptimize.java
				seasonPeaksTmp4.add(227-8); //- set from SpawnOptimize.java
				seasonPeaks.put(4, seasonPeaksTmp4); 

				ArrayList<Integer> seasonPeaksTmp5 = new ArrayList<Integer>(); 
				seasonPeaksTmp5.add(138+7*2);//- set from SpawnOptimize.java
				seasonPeaksTmp5.add(227-7); //- set from SpawnOptimize.java
				seasonPeaks.put(5, seasonPeaksTmp5); 

				ArrayList<Integer> seasonPeaksTmp6 = new ArrayList<Integer>(); 
				seasonPeaksTmp6.add(138+6*2);//- set from SpawnOptimize.java
				seasonPeaksTmp6.add(227-6); //- set from SpawnOptimize.java
				seasonPeaks.put(6, seasonPeaksTmp6); 

				ArrayList<Integer> seasonPeaksTmp7 = new ArrayList<Integer>(); 
				seasonPeaksTmp7.add(138+5*2);//- set from SpawnOptimize.java
				seasonPeaksTmp7.add(227-5); //- set from SpawnOptimize.java
				seasonPeaks.put(7, seasonPeaksTmp7); 

				ArrayList<Integer> seasonPeaksTmp8 = new ArrayList<Integer>(); 
				seasonPeaksTmp8.add(138+4*2);//- set from SpawnOptimize.java
				seasonPeaksTmp8.add(227-4); //- set from SpawnOptimize.java
				seasonPeaks.put(8, seasonPeaksTmp8); 

				ArrayList<Integer> seasonPeaksTmp9 = new ArrayList<Integer>(); 
				seasonPeaksTmp9.add(138+3*2);//- set from SpawnOptimize.java
				seasonPeaksTmp9.add(227-3); //- set from SpawnOptimize.java
				seasonPeaks.put(9, seasonPeaksTmp9); 

				ArrayList<Integer> seasonPeaksTmp10 = new ArrayList<Integer>(); 
				seasonPeaksTmp10.add(138+2*2);//- set from SpawnOptimize.java
				seasonPeaksTmp10.add(227-2); //- set from SpawnOptimize.java
				seasonPeaks.put(10, seasonPeaksTmp10); 

				ArrayList<Integer> seasonPeaksTmp11 = new ArrayList<Integer>(); 
				seasonPeaksTmp11.add(138+1*2);//- set from SpawnOptimize.java
				seasonPeaksTmp11.add(227-1); //- set from SpawnOptimize.java
				seasonPeaks.put(11, seasonPeaksTmp11); 

				ArrayList<Integer> seasonPeaksTmp12 = new ArrayList<Integer>(); 
				seasonPeaksTmp12.add(138);//- set from SpawnOptimize.java
				seasonPeaksTmp12.add(227); //- set from SpawnOptimize.java
				seasonPeaks.put(12, seasonPeaksTmp12); 

			}


			for (int i=-2; i<numYearClasses-2; i++) {
				ArrayList<Double> seasonPeakSizesTmp = new ArrayList<Double>(); 
				seasonPeakSizesTmp.add(0.8);//- set from SpawnOptimize.java
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
			natInstMortality.put(-2,.5); // this is average daily value from Peebles and Tolly 1998, also the value from FishBase based on Houde prediction, for larvae 6-18 days of age, here assummed for those dispersing
			natInstMortality.put(-1,0.0585); //this is daily value from Powell et al. 2004, for juveniles 40-90 days of age
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

			/*This is simple avg. F for two periods 1982-1984 and 2007-2009 for males and females
			 * seperately from 2010 SA
			 * 
			 * F82-84	M82-84	F07-09	M07-09
			 * 0.044	0.008666667	0.007333333	0.003333333
			 * 0.247333333	0.060333333	0.041333333	0.02
			 * 0.814666667	0.320666667	0.180333333	0.096
			 * 1.123333333	0.778333333	0.291333333	0.160666667
			 * 1.06	1.063666667	0.235666667	0.133333333
			 * 0.944666667	1.116	0.213	0.122333333
			 * 0.836	1.120333333	0.143	0.086333333
			 * 0.836	1.120333333	0.117666667	0.084333333
			 *
			 */
			if (!this.useAgeTrunc){ // if current, not age truncated freq distribution
				fishInstMortality.put(0, new double[] {0.007333333, 0.003333333});
				fishInstMortality.put(1,new double[] {0.041333333, 0.02});
				fishInstMortality.put(2,new double[] {0.180333333, 0.096});
				fishInstMortality.put(3,new double[] {0.291333333, 0.160666667});
				fishInstMortality.put(4,new double[] {0.235666667, 0.133333333});
				fishInstMortality.put(5,new double[] {0.213, 0.122333333});
				fishInstMortality.put(6,new double[] {0.143, 0.086333333});
				fishInstMortality.put(7,new double[] {0.117666667, 0.084333333});
				fishInstMortality.put(8,new double[] {0.117666667, 0.084333333});
				fishInstMortality.put(9,new double[] {0.117666667, 0.084333333});
				fishInstMortality.put(10,new double[] {0.117666667, 0.084333333});
				fishInstMortality.put(11,new double[] {0.117666667, 0.084333333});
				fishInstMortality.put(12,new double[] {0.117666667, 0.084333333});
			}
			else {  // if historical highly age truncated distribution
				fishInstMortality.put(0, new double[] {0.044, 0.008666667});
				fishInstMortality.put(1,new double[] {0.247333333, 0.060333333});
				fishInstMortality.put(2,new double[] {0.814666667, 0.320666667});
				fishInstMortality.put(3,new double[] {1.123333333, 0.778333333});
				fishInstMortality.put(4,new double[] {1.06, 1.063666667});
				fishInstMortality.put(5,new double[] {0.944666667, 1.116});
				fishInstMortality.put(6,new double[] {0.836, 1.120333333});
				fishInstMortality.put(7,new double[] {0.836, 1.120333333});
				fishInstMortality.put(8,new double[] {0.836, 1.120333333});
				fishInstMortality.put(9,new double[] {0.836, 1.120333333});
				fishInstMortality.put(10,new double[] {0.836, 1.120333333});
				fishInstMortality.put(11,new double[] {0.836, 1.120333333});
				fishInstMortality.put(12,new double[] {0.836, 1.120333333});
			}

			//females then males
			fishInstMortByYear.put(	1950	, new double[][] { {	0.019178,0.13422,0.55578,0.88423,0.88766,0.82324,0.75609,0.75609,0.75609,0.75609,0.75609,0.75609,0.75609	}, {	0.004006,0.031595,0.21063,0.64494,0.86733,0.90201,0.90391,0.90391,0.90391,0.90391,0.90391,0.90391,0.90391	} });
			fishInstMortByYear.put(	1951	, new double[][] { {	0.019867,0.13893,0.57219,0.90802,0.91052,0.84386,0.77456,0.77456,0.77456,0.77456,0.77456,0.77456,0.77456	}, {	0.0041463,0.032539,0.21689,0.66191,0.89086,0.92608,0.9275,0.9275,0.9275,0.9275,0.9275,0.9275,0.9275	} });
			fishInstMortByYear.put(	1952	, new double[][] { {	0.021958,0.1535,0.63079,0.99998,1.0023,0.92862,0.85215,0.85215,0.85215,0.85215,0.85215,0.85215,0.85215	}, {	0.0045808,0.035877,0.23912,0.72877,0.98117,1.0198,1.0211,1.0211,1.0211,1.0211,1.0211,1.0211,1.0211	} });
			fishInstMortByYear.put(	1953	, new double[][] { {	0.024501,0.17108,0.69728,1.1011,1.1017,1.0197,0.93484,0.93484,0.93484,0.93484,0.93484,0.93484,0.93484	}, {	0.0051046,0.03968,0.26441,0.80176,1.0808,1.1226,1.123,1.123,1.123,1.123,1.123,1.123,1.123	} });
			fishInstMortByYear.put(	1954	, new double[][] { {	0.019966,0.13935,0.56597,0.89229,0.89213,0.82531,0.75634,0.75634,0.75634,0.75634,0.75634,0.75634,0.75634	}, {	0.0041573,0.032215,0.21465,0.64946,0.87593,0.90953,0.90956,0.90956,0.90956,0.90956,0.90956,0.90956,0.90956	} });
			fishInstMortByYear.put(	1955	, new double[][] { {	0.017291,0.12021,0.47474,0.73832,0.73363,0.67607,0.61747,0.61747,0.61747,0.61747,0.61747,0.61747,0.61747	}, {	0.0035842,0.027074,0.18025,0.53568,0.72565,0.7517,0.74936,0.74936,0.74936,0.74936,0.74936,0.74936,0.74936	} });
			fishInstMortByYear.put(	1956	, new double[][] { {	0.015366,0.10683,0.42191,0.65617,0.65201,0.60086,0.54879,0.54879,0.54879,0.54879,0.54879,0.54879,0.54879	}, {	0.0031852,0.024061,0.16019,0.47608,0.64491,0.66807,0.66599,0.66599,0.66599,0.66599,0.66599,0.66599,0.66599	} });
			fishInstMortByYear.put(	1957	, new double[][] { {	0.017107,0.11928,0.48107,0.7559,0.75462,0.69745,0.63863,0.63863,0.63863,0.63863,0.63863,0.63863,0.63863	}, {	0.003558,0.027396,0.1825,0.54976,0.74225,0.77028,0.76972,0.76972,0.76972,0.76972,0.76972,0.76972,0.76972	} });
			fishInstMortByYear.put(	1958	, new double[][] { {	0.02213,0.1541,0.61571,0.96305,0.95945,0.88562,0.81003,0.81003,0.81003,0.81003,0.81003,0.81003,0.81003	}, {	0.0045958,0.035085,0.23366,0.69968,0.94605,0.981,0.97925,0.97925,0.97925,0.97925,0.97925,0.97925,0.97925	} });
			fishInstMortByYear.put(	1959	, new double[][] { {	0.022672,0.15786,0.63024,0.98542,0.98156,0.90594,0.82853,0.82853,0.82853,0.82853,0.82853,0.82853,0.82853	}, {	0.0047078,0.035915,0.23918,0.71587,0.96805,1.0037,1.0019,1.0019,1.0019,1.0019,1.0019,1.0019,1.0019	} });
			fishInstMortByYear.put(	1960	, new double[][] { {	0.024489,0.17028,0.6732,1.0475,1.0411,0.95959,0.87654,0.87654,0.87654,0.87654,0.87654,0.87654,0.87654	}, {	0.0050772,0.038389,0.25559,0.76012,1.0295,1.0666,1.0634,1.0634,1.0634,1.0634,1.0634,1.0634,1.0634	} });
			fishInstMortByYear.put(	1961	, new double[][] { {	0.027767,0.19231,0.73813,1.1314,1.1167,1.0248,0.9325,0.9325,0.9325,0.9325,0.9325,0.9325,0.9325	}, {	0.0057304,0.042178,0.28058,0.8181,1.1135,1.1505,1.143,1.143,1.143,1.143,1.143,1.143,1.143	} });
			fishInstMortByYear.put(	1962	, new double[][] { {	0.034339,0.23673,0.87613,1.3172,1.2881,1.1752,1.0637,1.0637,1.0637,1.0637,1.0637,1.0637,1.0637	}, {	0.0070485,0.050195,0.33354,0.94792,1.2985,1.337,1.3221,1.3221,1.3221,1.3221,1.3221,1.3221,1.3221	} });
			fishInstMortByYear.put(	1963	, new double[][] { {	0.031382,0.21629,0.79889,1.1997,1.1727,1.0695,0.96777,0.96777,0.96777,0.96777,0.96777,0.96777,0.96777	}, {	0.0064396,0.045776,0.30416,0.86318,1.1829,1.2177,1.2038,1.2038,1.2038,1.2038,1.2038,1.2038,1.2038	} });
			fishInstMortByYear.put(	1964	, new double[][] { {	0.028903,0.20072,0.78608,1.2174,1.2073,1.1113,1.0139,1.0139,1.0139,1.0139,1.0139,1.0139,1.0139	}, {	0.0059835,0.044855,0.29856,0.88241,1.197,1.239,1.234,1.234,1.234,1.234,1.234,1.234,1.234	} });
			fishInstMortByYear.put(	1965	, new double[][] { {	0.036684,0.25523,1.0133,1.5801,1.5719,1.4497,1.3249,1.3249,1.3249,1.3249,1.3249,1.3249,1.3249	}, {	0.0076108,0.057768,0.38466,1.1471,1.5526,1.6091,1.6051,1.6051,1.6051,1.6051,1.6051,1.6051,1.6051	} });
			fishInstMortByYear.put(	1966	, new double[][] { {	0.046787,0.32532,1.2856,2.0001,1.9877,1.832,1.6733,1.6733,1.6733,1.6733,1.6733,1.6733,1.6733	}, {	0.0096997,0.073315,0.48812,1.4513,1.9657,2.0364,2.0303,2.0303,2.0303,2.0303,2.0303,2.0303,2.0303	} });
			fishInstMortByYear.put(	1967	, new double[][] { {	0.04577,0.31736,1.2286,1.8916,1.8708,1.719,1.566,1.566,1.566,1.566,1.566,1.566,1.566	}, {	0.0094583,0.070161,0.46684,1.3691,1.8607,1.9242,1.9136,1.9136,1.9136,1.9136,1.9136,1.9136,1.9136	} });
			fishInstMortByYear.put(	1968	, new double[][] { {	0.060836,0.42162,1.6259,2.4984,2.4688,2.2672,2.0643,2.0643,2.0643,2.0643,2.0643,2.0643,2.0643	}, {	0.012564,0.092879,0.61793,1.8076,2.4582,2.5411,2.526,2.526,2.526,2.526,2.526,2.526,2.526	} });
			fishInstMortByYear.put(	1969	, new double[][] { {	0.048211,0.33355,1.2695,1.9374,1.9083,1.749,1.5896,1.5896,1.5896,1.5896,1.5896,1.5896,1.5896	}, {	0.0099371,0.072585,0.48273,1.3994,1.9074,1.9693,1.9544,1.9544,1.9544,1.9544,1.9544,1.9544,1.9544	} });
			fishInstMortByYear.put(	1970	, new double[][] { {	0.067968,0.46686,1.6776,2.4808,2.4066,2.1842,1.9677,1.9677,1.9677,1.9677,1.9677,1.9677,1.9677	}, {	0.013892,0.096323,0.63948,1.778,2.4493,2.5144,2.4762,2.4762,2.4762,2.4762,2.4762,2.4762,2.4762	} });
			fishInstMortByYear.put(	1971	, new double[][] { {	0.050223,0.34403,1.2085,1.7635,1.6995,1.5358,1.3781,1.3781,1.3781,1.3781,1.3781,1.3781,1.3781	}, {	0.010232,0.069507,0.46112,1.2597,1.7433,1.7852,1.7522,1.7522,1.7522,1.7522,1.7522,1.7522,1.7522	} });
			fishInstMortByYear.put(	1972	, new double[][] { {	0.043262,0.2965,1.046,1.5304,1.4767,1.3356,1.1994,1.1994,1.1994,1.1994,1.1994,1.1994,1.1994	}, {	0.0088196,0.060144,0.39906,1.0939,1.5125,1.5496,1.5219,1.5219,1.5219,1.5219,1.5219,1.5219,1.5219	} });
			fishInstMortByYear.put(	1973	, new double[][] { {	0.034572,0.23848,0.88703,1.3372,1.3094,1.1956,1.083,1.083,1.083,1.083,1.083,1.083,1.083	}, {	0.0071014,0.050801,0.33762,0.96297,1.3179,1.3577,1.3434,1.3434,1.3434,1.3434,1.3434,1.3434,1.3434	} });
			fishInstMortByYear.put(	1974	, new double[][] { {	0.033064,0.22758,0.83175,1.2418,1.2103,1.1018,0.99535,0.99535,0.99535,0.99535,0.99535,0.99535,0.99535	}, {	0.0067744,0.047696,0.31682,0.89213,1.225,1.2597,1.2435,1.2435,1.2435,1.2435,1.2435,1.2435,1.2435	} });
			fishInstMortByYear.put(	1975	, new double[][] { {	0.027529,0.18925,0.68489,1.0169,0.98843,0.89823,0.81017,0.81017,0.81017,0.81017,0.81017,0.81017,0.81017	}, {	0.0056323,0.039303,0.26099,0.72954,1.0036,1.031,1.0164,1.0164,1.0164,1.0164,1.0164,1.0164,1.0164	} });
			fishInstMortByYear.put(	1976	, new double[][] { {	0.027015,0.18579,0.67449,1.0032,0.97602,0.88745,0.80086,0.80086,0.80086,0.80086,0.80086,0.80086,0.80086	}, {	0.0055297,0.038697,0.25699,0.72008,0.98998,1.0174,1.0034,1.0034,1.0034,1.0034,1.0034,1.0034,1.0034	} });
			fishInstMortByYear.put(	1977	, new double[][] { {	0.022605,0.15505,0.55051,0.80842,0.78155,0.70772,0.63629,0.63629,0.63629,0.63629,0.63629,0.63629,0.63629	}, {	0.0046125,0.031637,0.20996,0.5784,0.79869,0.81886,0.80501,0.80501,0.80501,0.80501,0.80501,0.80501,0.80501	} });
			fishInstMortByYear.put(	1978	, new double[][] { {	0.019664,0.13512,0.48698,0.72135,0.70037,0.63598,0.57325,0.57325,0.57325,0.57325,0.57325,0.57325,0.57325	}, {	0.0040209,0.027955,0.18561,0.51722,0.71209,0.73125,0.72044,0.72044,0.72044,0.72044,0.72044,0.72044,0.72044	} });
			fishInstMortByYear.put(	1979	, new double[][] { {	0.031433,0.2138,0.70612,0.99139,0.93648,0.83501,0.74007,0.74007,0.74007,0.74007,0.74007,0.74007,0.74007	}, {	0.0063518,0.04081,0.2702,0.70108,0.98364,0.99994,0.97154,0.97154,0.97154,0.97154,0.97154,0.97154,0.97154	} });
			fishInstMortByYear.put(	1980	, new double[][] { {	0.020598,0.14108,0.49501,0.72184,0.6954,0.62826,0.56366,0.56366,0.56366,0.56366,0.56366,0.56366,0.56366	}, {	0.0041961,0.028473,0.18889,0.51554,0.71362,0.73069,0.71705,0.71705,0.71705,0.71705,0.71705,0.71705,0.71705	} });
			fishInstMortByYear.put(	1981	, new double[][] { {	0.027884,0.18991,0.63475,0.89815,0.85191,0.76173,0.67689,0.67689,0.67689,0.67689,0.67689,0.67689,0.67689	}, {	0.0056433,0.03665,0.24275,0.63645,0.89046,0.90657,0.88266,0.88266,0.88266,0.88266,0.88266,0.88266,0.88266	} });
			fishInstMortByYear.put(	1982	, new double[][] { {	0.036122,0.24675,0.84659,1.2178,1.1651,1.0478,0.93611,0.93611,0.93611,0.93611,0.93611,0.93611,0.93611	}, {	0.0073359,0.048781,0.32338,0.86674,1.2055,1.2312,1.204,1.204,1.204,1.204,1.204,1.204,1.204	} });
			fishInstMortByYear.put(	1983	, new double[][] { {	0.043335,0.2645,0.83665,1.1366,1.0605,0.93771,0.82449,0.82449,0.82449,0.82449,0.82449,0.82449,0.82449	}, {	0.005796,0.066094,0.33417,0.81156,1.0679,1.079,1.0438,1.0438,1.0438,1.0438,1.0438,1.0438,1.0438	} });
			fishInstMortByYear.put(	1984	, new double[][] { {	0.056714,0.23899,0.74118,0.96777,0.90501,0.80139,0.70564,0.70564,0.70564,0.70564,0.70564,0.70564,0.70564	}, {	0.013002,0.065785,0.31886,0.70848,0.91077,0.92328,0.89403,0.89403,0.89403,0.89403,0.89403,0.89403,0.89403	} });
			fishInstMortByYear.put(	1985	, new double[][] { {	0.036708,0.16223,0.47609,0.61362,0.58051,0.51814,0.45966,0.45966,0.45966,0.45966,0.45966,0.45966,0.45966	}, {	0.0066546,0.045683,0.21629,0.49539,0.60301,0.60091,0.58494,0.58494,0.58494,0.58494,0.58494,0.58494,0.58494	} });
			fishInstMortByYear.put(	1986	, new double[][] { {	0.016741,0.25823,0.68123,0.87231,0.80385,0.70455,0.61427,0.61427,0.61427,0.61427,0.61427,0.61427,0.61427	}, {	0.0035135,0.057786,0.32003,0.7203,0.87337,0.83868,0.80559,0.80559,0.80559,0.80559,0.80559,0.80559,0.80559	} });
			fishInstMortByYear.put(	1987	, new double[][] { {	0.023799,0.13546,0.4405,0.61443,0.57059,0.50281,0.44068,0.44068,0.44068,0.44068,0.44068,0.44068,0.44068	}, {	0.0048798,0.03191,0.1687,0.41367,0.55097,0.59569,0.70712,0.70712,0.70712,0.70712,0.70712,0.70712,0.70712	} });
			fishInstMortByYear.put(	1988	, new double[][] { {	0.021002,0.16745,0.53428,0.77089,0.92606,0.80031,0.68814,0.68814,0.68814,0.68814,0.68814,0.68814,0.68814	}, {	0.0035406,0.033229,0.20339,0.50064,0.68163,0.72622,0.87277,0.87277,0.87277,0.87277,0.87277,0.87277,0.87277	} });
			fishInstMortByYear.put(	1989	, new double[][] { {	0.013424,0.1144,0.5451,0.55537,0.65076,0.55985,0.4792,0.4792,0.4792,0.4792,0.4792,0.4792,0.4792	}, {	0.0027119,0.023562,0.16403,0.45688,0.55162,0.53582,0.42811,0.41711,0.41711,0.41711,0.41711,0.41711,0.41711	} });
			fishInstMortByYear.put(	1990	, new double[][] { {	0.006092,0.037648,0.19036,0.2604,0.3147,0.43206,0.42759,0.096341,0.096341,0.096341,0.096341,0.096341,0.096341	}, {	0.0020906,0.014745,0.082877,0.19518,0.23003,0.25269,0.15452,0.18381,0.18381,0.18381,0.18381,0.18381,0.18381	} });
			fishInstMortByYear.put(	1991	, new double[][] { {	0.0062517,0.042273,0.22644,0.31521,0.44551,0.6947,0.65453,0.092206,0.092206,0.092206,0.092206,0.092206,0.092206	}, {	0.0020977,0.014548,0.090073,0.22686,0.25938,0.33443,0.20385,0.21071,0.21071,0.21071,0.21071,0.21071,0.21071	} });
			fishInstMortByYear.put(	1992	, new double[][] { {	0.0073086,0.046303,0.23474,0.33116,0.42064,0.53028,0.34377,0.11633,0.11633,0.11633,0.11633,0.11633,0.11633	}, {	0.002443,0.016115,0.093318,0.23409,0.278,0.37704,0.21022,0.26932,0.26932,0.26932,0.26932,0.26932,0.26932	} });
			fishInstMortByYear.put(	1993	, new double[][] { {	0.0064803,0.042483,0.18881,0.27628,0.34277,0.3823,0.35225,0.38424,0.38424,0.38424,0.38424,0.38424,0.38424	}, {	0.0022128,0.01462,0.082887,0.20145,0.2338,0.33007,0.18069,0.20466,0.20466,0.20466,0.20466,0.20466,0.20466	} });
			fishInstMortByYear.put(	1994	, new double[][] { {	0.0060728,0.036801,0.15305,0.22637,0.27385,0.36361,0.35117,0.34859,0.34859,0.34859,0.34859,0.34859,0.34859	}, {	0.0020773,0.013377,0.071343,0.159,0.18502,0.23494,0.13137,0.16654,0.16654,0.16654,0.16654,0.16654,0.16654	} });
			fishInstMortByYear.put(	1995	, new double[][] { {	0.0071788,0.039413,0.15078,0.20686,0.20833,0.16389,0.14306,0.13272,0.13272,0.13272,0.13272,0.13272,0.13272	}, {	0.0025883,0.014901,0.075812,0.16708,0.18824,0.18614,0.12271,0.12808,0.12808,0.12808,0.12808,0.12808,0.12808	} });
			fishInstMortByYear.put(	1996	, new double[][] { {	0.0056946,0.028543,0.089085,0.14259,0.15497,0.14372,0.1246,0.12515,0.12515,0.12515,0.12515,0.12515,0.12515	}, {	0.0018844,0.011451,0.054167,0.10909,0.12444,0.15395,0.13426,0.11956,0.11956,0.11956,0.11956,0.11956,0.11956	} });
			fishInstMortByYear.put(	1997	, new double[][] { {	0.0080498,0.041123,0.13499,0.21676,0.23229,0.19695,0.18897,0.17837,0.17837,0.17837,0.17837,0.17837,0.17837	}, {	0.0025853,0.015784,0.074052,0.1486,0.17286,0.22576,0.19638,0.16953,0.16953,0.16953,0.16953,0.16953,0.16953	} });
			fishInstMortByYear.put(	1998	, new double[][] { {	0.0067375,0.039517,0.16595,0.25144,0.32479,0.26564,0.22368,0.2116,0.2116,0.2116,0.2116,0.2116,0.2116	}, {	0.0023968,0.014878,0.074118,0.15513,0.1803,0.25651,0.21306,0.21219,0.21219,0.21219,0.21219,0.21219,0.21219	} });
			fishInstMortByYear.put(	1999	, new double[][] { {	0.010588,0.029563,0.10742,0.20176,0.18632,0.16327,0.17778,0.12437,0.12437,0.12437,0.12437,0.12437,0.12437	}, {	0.0018397,0.011173,0.055597,0.11813,0.13516,0.18365,0.15442,0.13481,0.13481,0.13481,0.13481,0.13481,0.13481	} });
			fishInstMortByYear.put(	2000	, new double[][] { {	0.0055621,0.030515,0.10008,0.16461,0.22742,0.1971,0.1348,0.11537,0.11537,0.11537,0.11537,0.11537,0.11537	}, {	0.0019972,0.012365,0.056977,0.10056,0.14807,0.14447,0.1723,0.16391,0.16391,0.16391,0.16391,0.16391,0.16391	} });
			fishInstMortByYear.put(	2001	, new double[][] { {	0.0059293,0.029293,0.094277,0.14093,0.1783,0.16219,0.13053,0.12281,0.12281,0.12281,0.12281,0.12281,0.12281	}, {	0.0025666,0.015649,0.059183,0.083639,0.072029,0.085968,0.06009,0.06095,0.06095,0.06095,0.06095,0.06095,0.06095	} });
			fishInstMortByYear.put(	2002	, new double[][] { {	0.0068817,0.034295,0.11673,0.24813,0.26553,0.2314,0.16609,0.11585,0.11585,0.11585,0.11585,0.11585,0.11585	}, {	0.0030302,0.018412,0.077718,0.12762,0.098301,0.10111,0.089972,0.06562,0.06562,0.06562,0.06562,0.06562,0.06562	} });
			fishInstMortByYear.put(	2003	, new double[][] { {	0.0081401,0.04495,0.17387,0.27613,0.26352,0.22477,0.14881,0.12087,0.12087,0.12087,0.12087,0.12087,0.12087	}, {	0.0035739,0.021664,0.089919,0.13429,0.10887,0.10606,0.082068,0.087586,0.087586,0.087586,0.087586,0.087586,0.087586	} });
			fishInstMortByYear.put(	2004	, new double[][] { {	0.0080757,0.047774,0.18763,0.32595,0.28544,0.20405,0.19107,0.13331,0.13331,0.13331,0.13331,0.13331,0.13331	}, {	0.0034798,0.021745,0.10195,0.15966,0.12825,0.12357,0.090424,0.085005,0.085005,0.085005,0.085005,0.085005,0.085005	} });
			fishInstMortByYear.put(	2005	, new double[][] { {	0.0080069,0.047833,0.18589,0.23999,0.23457,0.19481,0.14702,0.10867,0.10867,0.10867,0.10867,0.10867,0.10867	}, {	0.0035219,0.022503,0.10972,0.17341,0.13941,0.12159,0.08244,0.079587,0.079587,0.079587,0.079587,0.079587,0.079587	} });
			fishInstMortByYear.put(	2006	, new double[][] { {	0.0057755,0.031973,0.13259,0.21554,0.24126,0.18984,0.1553,0.14695,0.14695,0.14695,0.14695,0.14695,0.14695	}, {	0.0025545,0.015486,0.071456,0.11416,0.093607,0.10166,0.079806,0.085733,0.085733,0.085733,0.085733,0.085733,0.085733	} });
			fishInstMortByYear.put(	2007	, new double[][] { {	0.007191,0.038165,0.16968,0.25553,0.16482,0.11382,0.081574,0.071125,0.071125,0.071125,0.071125,0.071125,0.071125	}, {	0.003132,0.019229,0.079884,0.11654,0.094631,0.092173,0.06638,0.053786,0.053786,0.053786,0.053786,0.053786,0.053786	} });
			fishInstMortByYear.put(	2008	, new double[][] { {	0.0078241,0.045751,0.18389,0.24839,0.25633,0.31628,0.19519,0.1314,0.1314,0.1314,0.1314,0.1314,0.1314	}, {	0.0034703,0.021791,0.11237,0.1769,0.13499,0.1099,0.078005,0.091591,0.091591,0.091591,0.091591,0.091591,0.091591	} });
			fishInstMortByYear.put(	2009	, new double[][] { {	0.0070076,0.03702,0.17426,0.34639,0.2654,0.19004,0.13921,0.13921,0.13921,0.13921,0.13921,0.13921,0.13921	}, {	0.0031012,0.018838,0.090002,0.17437,0.15628,0.15102,0.10496,0.09776,0.09776,0.09776,0.09776,0.09776,0.09776	} });
			fishInstMortByYear.put(	19700	, new double[][] { {	0.05569625,0.3848475,1.4504,2.20205,2.163625,1.97985,1.7969,1.7969,1.7969,1.7969,1.7969,1.7969,1.7969	}, {	0.01146285,0.082987,0.551745,1.588525,2.1689,2.23725,2.21755,2.21755,2.21755,2.21755,2.21755,2.21755,2.21755	} });
			fishInstMortByYear.put(	20090	, new double[][] { {	0.00694955,0.03822725,0.165105,0.2664625,0.2319525,0.202495,0.1428185,0.12217125,0.12217125,0.12217125,0.12217125,0.12217125,0.12217125	}, {	0.0030645,0.018836,0.088428,0.1454925,0.119877,0.11368825,0.08228775,0.0822175,0.0822175,0.0822175,0.0822175,0.0822175,0.0822175	} });

			//females then males
			sizeFreqByYear.put(	1950	, new double[][] { {	0.553540,0.249665,0.126463,0.049214,0.014643,0.004470,0.001479,0.000332,0.000122,0.000045,0.000017,0.000006,0.000006	}, {	0.478655,0.245420,0.146993,0.083161,0.031843,0.009959,0.003039,0.000521,0.000228,0.000100,0.000044,0.000019,0.000019	} });
			sizeFreqByYear.put(	1951	, new double[][] { {	0.639922,0.201273,0.101951,0.039675,0.011804,0.003603,0.001192,0.000372,0.000132,0.000047,0.000017,0.000006,0.000006	}, {	0.568194,0.203159,0.121681,0.068840,0.026360,0.008244,0.002516,0.000579,0.000244,0.000103,0.000043,0.000018,0.000018	} });
			sizeFreqByYear.put(	1952	, new double[][] { {	0.581029,0.271094,0.095369,0.036682,0.010834,0.003310,0.001097,0.000381,0.000132,0.000045,0.000016,0.000005,0.000005	}, {	0.514127,0.272833,0.113862,0.064074,0.024274,0.007542,0.002300,0.000579,0.000238,0.000097,0.000040,0.000016,0.000016	} });
			sizeFreqByYear.put(	1953	, new double[][] { {	0.579803,0.246965,0.127274,0.032537,0.009186,0.002787,0.000931,0.000344,0.000114,0.000038,0.000013,0.000004,0.000004	}, {	0.510279,0.247613,0.152927,0.058842,0.021206,0.006368,0.001923,0.000507,0.000200,0.000079,0.000031,0.000012,0.000012	} });
			sizeFreqByYear.put(	1954	, new double[][] { {	0.584873,0.248285,0.115070,0.041036,0.007438,0.002161,0.000723,0.000282,0.000089,0.000028,0.000009,0.000003,0.000003	}, {	0.510408,0.247348,0.139231,0.077594,0.018229,0.005071,0.001475,0.000399,0.000150,0.000057,0.000021,0.000008,0.000008	} });
			sizeFreqByYear.put(	1955	, new double[][] { {	0.567293,0.254301,0.120699,0.042762,0.011684,0.002181,0.000688,0.000266,0.000085,0.000027,0.000009,0.000003,0.000003	}, {	0.494030,0.251906,0.142523,0.075525,0.028475,0.005442,0.001478,0.000380,0.000146,0.000056,0.000022,0.000008,0.000008	} });
			sizeFreqByYear.put(	1956	, new double[][] { {	0.548109,0.252897,0.128856,0.050247,0.014523,0.004104,0.000824,0.000287,0.000098,0.000034,0.000012,0.000004,0.000004	}, {	0.475696,0.249142,0.149003,0.081720,0.031714,0.010088,0.001897,0.000429,0.000178,0.000074,0.000031,0.000013,0.000013	} });
			sizeFreqByYear.put(	1957	, new double[][] { {	0.541040,0.245585,0.130279,0.056730,0.018584,0.005553,0.001678,0.000338,0.000130,0.000050,0.000019,0.000007,0.000007	}, {	0.468897,0.241214,0.148571,0.087609,0.036611,0.012243,0.003844,0.000545,0.000250,0.000115,0.000053,0.000024,0.000024	} });
			sizeFreqByYear.put(	1958	, new double[][] { {	0.544846,0.245117,0.126558,0.054760,0.019236,0.006495,0.002088,0.000539,0.000214,0.000085,0.000034,0.000013,0.000013	}, {	0.471569,0.240775,0.145227,0.086537,0.036935,0.012989,0.004266,0.000905,0.000422,0.000197,0.000092,0.000043,0.000043	} });
			sizeFreqByYear.put(	1959	, new double[][] { {	0.558348,0.247816,0.123098,0.046915,0.015230,0.005527,0.002042,0.000624,0.000242,0.000094,0.000036,0.000014,0.000014	}, {	0.482220,0.243870,0.145033,0.081033,0.031661,0.010776,0.003696,0.000938,0.000422,0.000190,0.000085,0.000038,0.000038	} });
			sizeFreqByYear.put(	1960	, new double[][] { {	0.562284,0.251049,0.122629,0.044480,0.012619,0.004233,0.001684,0.000641,0.000237,0.000088,0.000032,0.000012,0.000012	}, {	0.485445,0.247066,0.145426,0.079735,0.028903,0.008953,0.002970,0.000850,0.000367,0.000158,0.000068,0.000029,0.000029	} });
			sizeFreqByYear.put(	1961	, new double[][] { {	0.568976,0.250549,0.121816,0.042146,0.011164,0.003281,0.001214,0.000553,0.000193,0.000067,0.000023,0.000008,0.000008	}, {	0.491037,0.246844,0.145920,0.078093,0.027015,0.007631,0.002301,0.000677,0.000280,0.000116,0.000048,0.000020,0.000020	} });
			sizeFreqByYear.put(	1962	, new double[][] { {	0.574238,0.253286,0.119204,0.039324,0.009749,0.002698,0.000883,0.000412,0.000136,0.000045,0.000015,0.000005,0.000005	}, {	0.494446,0.249631,0.145299,0.076453,0.024978,0.006561,0.001804,0.000497,0.000197,0.000078,0.000031,0.000012,0.000012	} });
			sizeFreqByYear.put(	1963	, new double[][] { {	0.577060,0.259875,0.117959,0.034302,0.007731,0.002031,0.000639,0.000277,0.000086,0.000027,0.000008,0.000003,0.000003	}, {	0.493446,0.255687,0.148469,0.073540,0.021876,0.005135,0.001311,0.000335,0.000125,0.000047,0.000018,0.000007,0.000007	} });
			sizeFreqByYear.put(	1964	, new double[][] { {	0.568176,0.261608,0.123371,0.036624,0.007574,0.001805,0.000534,0.000214,0.000064,0.000019,0.000006,0.000002,0.000002	}, {	0.484019,0.255816,0.153045,0.077532,0.022946,0.005058,0.001158,0.000267,0.000099,0.000036,0.000013,0.000005,0.000005	} });
			sizeFreqByYear.put(	1965	, new double[][] { {	0.572450,0.254671,0.124408,0.038267,0.007836,0.001685,0.000449,0.000164,0.000048,0.000014,0.000004,0.000001,0.000001	}, {	0.488822,0.249138,0.152083,0.079756,0.023550,0.005191,0.001108,0.000220,0.000081,0.000030,0.000011,0.000004,0.000004	} });
			sizeFreqByYear.put(	1966	, new double[][] { {	0.579323,0.262916,0.118429,0.031747,0.005883,0.001250,0.000309,0.000104,0.000028,0.000008,0.000002,0.000001,0.000001	}, {	0.493198,0.257987,0.150164,0.074687,0.019094,0.003835,0.000807,0.000149,0.000052,0.000018,0.000006,0.000002,0.000002	} });
			sizeFreqByYear.put(	1967	, new double[][] { {	0.601144,0.259754,0.112409,0.022699,0.003162,0.000611,0.000154,0.000050,0.000012,0.000003,0.000001,0.000000,0.000000	}, {	0.510998,0.256559,0.151216,0.065679,0.013029,0.002032,0.000384,0.000073,0.000022,0.000007,0.000002,0.000001,0.000001	} });
			sizeFreqByYear.put(	1968	, new double[][] { {	0.595262,0.267931,0.111167,0.022652,0.002503,0.000366,0.000084,0.000028,0.000006,0.000001,0.000000,0.000000,0.000000	}, {	0.505070,0.263945,0.149757,0.067071,0.012348,0.001528,0.000226,0.000040,0.000011,0.000003,0.000001,0.000000,0.000000	} });
			sizeFreqByYear.put(	1969	, new double[][] { {	0.615481,0.263565,0.104196,0.015184,0.001373,0.000161,0.000029,0.000010,0.000002,0.000000,0.000000,0.000000,0.000000	}, {	0.520151,0.261714,0.151568,0.057470,0.008185,0.000802,0.000092,0.000013,0.000003,0.000001,0.000000,0.000000,0.000000	} });
			sizeFreqByYear.put(	1970	, new double[][] { {	0.616896,0.257857,0.104579,0.018991,0.001507,0.000144,0.000020,0.000005,0.000001,0.000000,0.000000,0.000000,0.000000	}, {	0.525539,0.255537,0.145024,0.062961,0.009975,0.000872,0.000081,0.000009,0.000002,0.000000,0.000000,0.000000,0.000000	} });
			sizeFreqByYear.put(	1971	, new double[][] { {	0.624801,0.266445,0.094159,0.013327,0.001151,0.000101,0.000012,0.000002,0.000000,0.000000,0.000000,0.000000,0.000000	}, {	0.528558,0.266401,0.143240,0.053350,0.007752,0.000640,0.000053,0.000005,0.000001,0.000000,0.000000,0.000000,0.000000	} });
			sizeFreqByYear.put(	1972	, new double[][] { {	0.610981,0.263387,0.105484,0.018392,0.001587,0.000150,0.000016,0.000002,0.000000,0.000000,0.000000,0.000000,0.000000	}, {	0.518356,0.260389,0.148525,0.060987,0.010681,0.000976,0.000078,0.000006,0.000001,0.000000,0.000000,0.000000,0.000000	} });
			sizeFreqByYear.put(	1973	, new double[][] { {	0.604639,0.258942,0.109170,0.024198,0.002760,0.000259,0.000029,0.000003,0.000001,0.000000,0.000000,0.000000,0.000000	}, {	0.514706,0.255447,0.146383,0.067211,0.014396,0.001692,0.000151,0.000010,0.000003,0.000001,0.000000,0.000000,0.000000	} });
			sizeFreqByYear.put(	1974	, new double[][] { {	0.595384,0.257235,0.113185,0.029218,0.004384,0.000529,0.000056,0.000006,0.000001,0.000000,0.000000,0.000000,0.000000	}, {	0.510163,0.253649,0.144701,0.070321,0.018054,0.002766,0.000316,0.000022,0.000007,0.000002,0.000001,0.000000,0.000000	} });
			sizeFreqByYear.put(	1975	, new double[][] { {	0.598760,0.250539,0.112269,0.031620,0.005752,0.000917,0.000125,0.000013,0.000003,0.000001,0.000000,0.000000,0.000000	}, {	0.515558,0.247973,0.142110,0.069980,0.019991,0.003754,0.000561,0.000048,0.000016,0.000005,0.000002,0.000001,0.000001	} });
			sizeFreqByYear.put(	1976	, new double[][] { {	0.584176,0.255145,0.114423,0.036580,0.007849,0.001512,0.000268,0.000033,0.000009,0.000003,0.000001,0.000000,0.000000	}, {	0.504292,0.252067,0.140766,0.073020,0.023518,0.005211,0.000962,0.000105,0.000038,0.000014,0.000005,0.000002,0.000002	} });
			sizeFreqByYear.put(	1977	, new double[][] { {	0.591215,0.245034,0.115036,0.037062,0.009057,0.002056,0.000440,0.000070,0.000021,0.000006,0.000002,0.000001,0.000001	}, {	0.512351,0.242915,0.141048,0.071534,0.024404,0.006122,0.001333,0.000181,0.000069,0.000026,0.000010,0.000004,0.000004	} });
			sizeFreqByYear.put(	1978	, new double[][] { {	0.617358,0.226855,0.103757,0.038415,0.010154,0.002624,0.000652,0.000126,0.000040,0.000013,0.000004,0.000001,0.000001	}, {	0.542810,0.227377,0.125992,0.069152,0.025355,0.007080,0.001758,0.000285,0.000114,0.000045,0.000018,0.000007,0.000007	} });
			sizeFreqByYear.put(	1979	, new double[][] { {	0.528723,0.288299,0.118911,0.044804,0.013934,0.003872,0.001084,0.000241,0.000084,0.000030,0.000010,0.000004,0.000004	}, {	0.458133,0.284106,0.139528,0.074610,0.030713,0.009454,0.002616,0.000479,0.000204,0.000087,0.000037,0.000016,0.000016	} });
			sizeFreqByYear.put(	1980	, new double[][] { {	0.498433,0.276023,0.158010,0.046652,0.014032,0.004746,0.001483,0.000391,0.000144,0.000053,0.000019,0.000007,0.000007	}, {	0.416459,0.264771,0.190495,0.084024,0.030516,0.009661,0.002955,0.000628,0.000273,0.000119,0.000052,0.000023,0.000023	} });
			sizeFreqByYear.put(	1981	, new double[][] { {	0.518185,0.238760,0.147674,0.069494,0.017366,0.005521,0.002029,0.000588,0.000230,0.000090,0.000035,0.000014,0.000014	}, {	0.431909,0.226500,0.168776,0.116848,0.038851,0.011808,0.003712,0.000856,0.000395,0.000182,0.000084,0.000039,0.000039	} });
			sizeFreqByYear.put(	1982	, new double[][] { {	0.585166,0.224481,0.110819,0.051449,0.019755,0.005322,0.001882,0.000685,0.000266,0.000104,0.000040,0.000016,0.000016	}, {	0.496378,0.217990,0.133089,0.091169,0.044493,0.011707,0.003536,0.000882,0.000405,0.000186,0.000086,0.000039,0.000039	} });
			sizeFreqByYear.put(	1983	, new double[][] { {	0.494762,0.318863,0.124836,0.039618,0.013473,0.005614,0.001728,0.000685,0.000259,0.000098,0.000037,0.000014,0.000014	}, {	0.561934,0.226361,0.114528,0.060025,0.024956,0.008855,0.002294,0.000608,0.000253,0.000106,0.000044,0.000018,0.000018	} });
			sizeFreqByYear.put(	1984	, new double[][] { {	0.525997,0.250916,0.163300,0.042254,0.010549,0.003985,0.001908,0.000685,0.000253,0.000093,0.000035,0.000013,0.000013	}, {	0.488860,0.291189,0.132615,0.057977,0.019700,0.006467,0.002292,0.000538,0.000215,0.000086,0.000035,0.000014,0.000014	} });
			sizeFreqByYear.put(	1985	, new double[][] { {	0.603769,0.219254,0.109811,0.050657,0.011096,0.003037,0.001293,0.000700,0.000246,0.000086,0.000030,0.000011,0.000011	}, {	0.510744,0.236482,0.160452,0.064093,0.019833,0.005616,0.001839,0.000572,0.000222,0.000086,0.000034,0.000013,0.000013	} });
			sizeFreqByYear.put(	1986	, new double[][] { {	0.521642,0.285265,0.115115,0.049335,0.021061,0.004909,0.001453,0.000760,0.000285,0.000107,0.000040,0.000015,0.000015	}, {	0.472923,0.259173,0.138578,0.089566,0.028283,0.008017,0.002298,0.000675,0.000281,0.000117,0.000049,0.000020,0.000020	} });
			sizeFreqByYear.put(	1987	, new double[][] { {	0.661023,0.186899,0.101134,0.031310,0.011770,0.005539,0.001449,0.000545,0.000204,0.000076,0.000028,0.000011,0.000011	}, {	0.514248,0.231767,0.144456,0.067136,0.030386,0.008399,0.002490,0.000643,0.000271,0.000114,0.000048,0.000020,0.000020	} });
			sizeFreqByYear.put(	1988	, new double[][] { {	0.638857,0.234893,0.074832,0.034956,0.009657,0.003905,0.001998,0.000563,0.000210,0.000079,0.000029,0.000011,0.000011	}, {	0.519509,0.235412,0.124005,0.076158,0.028950,0.011652,0.003111,0.000667,0.000295,0.000131,0.000058,0.000026,0.000026	} });
			sizeFreqByYear.put(	1989	, new double[][] { {	0.632821,0.235011,0.094033,0.024311,0.009518,0.002318,0.001080,0.000602,0.000201,0.000067,0.000023,0.000008,0.000008	}, {	0.472170,0.266347,0.140694,0.070625,0.033670,0.010896,0.004237,0.000738,0.000337,0.000154,0.000070,0.000032,0.000032	} });
			sizeFreqByYear.put(	1990	, new double[][] { {	0.566634,0.269784,0.114100,0.034757,0.009444,0.003460,0.000938,0.000582,0.000197,0.000066,0.000022,0.000008,0.000008	}, {	0.456928,0.243222,0.161355,0.083670,0.032749,0.014489,0.004811,0.001456,0.000691,0.000327,0.000155,0.000074,0.000074	} });
			sizeFreqByYear.put(	1991	, new double[][] { {	0.424775,0.297542,0.172925,0.073525,0.022174,0.005874,0.001945,0.000748,0.000295,0.000116,0.000046,0.000018,0.000018	}, {	0.412318,0.242208,0.152868,0.107028,0.051835,0.019989,0.008732,0.002360,0.001257,0.000669,0.000356,0.000190,0.000190	} });
			sizeFreqByYear.put(	1992	, new double[][] { {	0.419736,0.222624,0.189518,0.107302,0.044329,0.012081,0.002535,0.001034,0.000461,0.000206,0.000092,0.000041,0.000041	}, {	0.403678,0.222300,0.154869,0.102393,0.065335,0.031249,0.011292,0.003842,0.002208,0.001269,0.000729,0.000419,0.000419	} });
			sizeFreqByYear.put(	1993	, new double[][] { {	0.482766,0.197605,0.126996,0.104864,0.057251,0.022264,0.005525,0.001326,0.000683,0.000352,0.000181,0.000093,0.000093	}, {	0.417918,0.213504,0.139261,0.101459,0.060897,0.037939,0.016600,0.005026,0.003052,0.001853,0.001125,0.000683,0.000683	} });
			sizeFreqByYear.put(	1994	, new double[][] { {	0.491798,0.222006,0.110434,0.071808,0.057688,0.030335,0.011522,0.001937,0.001098,0.000622,0.000352,0.000200,0.000200	}, {	0.407726,0.223315,0.135306,0.093129,0.062977,0.037334,0.021337,0.007369,0.004601,0.002873,0.001794,0.001120,0.001120	} });
			sizeFreqByYear.put(	1995	, new double[][] { {	0.552310,0.196820,0.108550,0.056299,0.036124,0.028488,0.013916,0.003307,0.001866,0.001053,0.000594,0.000336,0.000336	}, {	0.445066,0.200730,0.130539,0.084324,0.055560,0.037346,0.021273,0.009887,0.006142,0.003816,0.002371,0.001473,0.001473	} });
			sizeFreqByYear.put(	1996	, new double[][] { {	0.604712,0.194571,0.084585,0.048876,0.025449,0.016785,0.014062,0.005102,0.002742,0.001473,0.000792,0.000426,0.000426	}, {	0.459991,0.214267,0.114626,0.079237,0.048821,0.032131,0.021860,0.011659,0.007128,0.004358,0.002665,0.001629,0.001629	} });
			sizeFreqByYear.put(	1997	, new double[][] { {	0.496963,0.271094,0.107414,0.051474,0.029937,0.015849,0.010742,0.007996,0.004137,0.002140,0.001107,0.000573,0.000573	}, {	0.384540,0.250033,0.138525,0.080219,0.054851,0.033953,0.021914,0.014260,0.008798,0.005427,0.003348,0.002066,0.002066	} });
			sizeFreqByYear.put(	1998	, new double[][] { {	0.532121,0.205665,0.136744,0.057769,0.027088,0.015968,0.008899,0.007769,0.003940,0.001998,0.001013,0.000514,0.000514	}, {	0.406258,0.201696,0.155418,0.091768,0.051545,0.035095,0.020812,0.014833,0.009151,0.005645,0.003483,0.002149,0.002149	} });
			sizeFreqByYear.put(	1999	, new double[][] { {	0.429757,0.273115,0.128705,0.088317,0.036371,0.016315,0.010368,0.008136,0.004269,0.002240,0.001175,0.000616,0.000616	}, {	0.380930,0.223571,0.131629,0.107998,0.061453,0.034338,0.021882,0.014834,0.009299,0.005829,0.003654,0.002291,0.002291	} });
			sizeFreqByYear.put(	2000	, new double[][] { {	0.465119,0.197132,0.154870,0.079068,0.052426,0.022572,0.010528,0.008315,0.004568,0.002509,0.001379,0.000757,0.000757	}, {	0.377049,0.206549,0.144218,0.091762,0.073905,0.042179,0.022679,0.015581,0.010045,0.006476,0.004175,0.002691,0.002691	} });
			sizeFreqByYear.put(	2001	, new double[][] { {	0.494204,0.202494,0.105460,0.090511,0.046001,0.029487,0.013297,0.008028,0.004609,0.002646,0.001519,0.000872,0.000872	}, {	0.456086,0.178081,0.115937,0.087462,0.055672,0.043621,0.025237,0.014138,0.009133,0.005899,0.003811,0.002462,0.002462	} });
			sizeFreqByYear.put(	2002	, new double[][] { {	0.514723,0.206664,0.104217,0.059568,0.051811,0.026113,0.017285,0.008305,0.004859,0.002843,0.001663,0.000973,0.000973	}, {	0.392268,0.240713,0.111398,0.078441,0.060344,0.039645,0.030941,0.016919,0.011083,0.007260,0.004756,0.003116,0.003116	} });
			sizeFreqByYear.put(	2003	, new double[][] { {	0.467286,0.242319,0.119264,0.064863,0.034518,0.030374,0.016096,0.010931,0.006281,0.003609,0.002074,0.001192,0.001192	}, {	0.408336,0.201324,0.146092,0.071980,0.050389,0.040723,0.026947,0.020385,0.013090,0.008405,0.005397,0.003465,0.003465	} });
			sizeFreqByYear.put(	2004	, new double[][] { {	0.377702,0.256807,0.161714,0.081938,0.042720,0.023701,0.022029,0.013900,0.008247,0.004893,0.002903,0.001723,0.001723	}, {	0.374976,0.222638,0.129451,0.099120,0.048818,0.035764,0.029276,0.022424,0.014457,0.009320,0.006008,0.003873,0.003873	} });
			sizeFreqByYear.put(	2005	, new double[][] { {	0.431169,0.187636,0.154471,0.099056,0.046410,0.025938,0.015862,0.016745,0.009777,0.005709,0.003334,0.001947,0.001947	}, {	0.366900,0.207107,0.144986,0.087899,0.066388,0.034423,0.025590,0.025018,0.016096,0.010356,0.006663,0.004287,0.004287	} });
			sizeFreqByYear.put(	2006	, new double[][] { {	0.444755,0.208288,0.109740,0.092163,0.059449,0.028828,0.017036,0.016363,0.009796,0.005865,0.003511,0.002102,0.002102	}, {	0.375898,0.199606,0.132750,0.096224,0.057200,0.045599,0.024310,0.025237,0.016434,0.010701,0.006968,0.004537,0.004537	} });
			sizeFreqByYear.put(	2007	, new double[][] { {	0.421037,0.222575,0.127921,0.071382,0.058589,0.037916,0.019669,0.016221,0.010007,0.006174,0.003809,0.002350,0.002350	}, {	0.443478,0.179691,0.113105,0.080357,0.058321,0.036104,0.028837,0.022234,0.014449,0.009390,0.006102,0.003966,0.003966	} });
			sizeFreqByYear.put(	2008	, new double[][] { {	0.457590,0.194978,0.125895,0.074302,0.040401,0.037377,0.025865,0.017329,0.010669,0.006569,0.004045,0.002490,0.002490	}, {	0.512967,0.188026,0.090023,0.060250,0.043121,0.032635,0.020457,0.021200,0.012897,0.007846,0.004773,0.002904,0.002904	} });
			sizeFreqByYear.put(	2009	, new double[][] { {	0.447332,0.222453,0.114973,0.075725,0.044491,0.024707,0.021875,0.019971,0.011945,0.007144,0.004273,0.002556,0.002556	}, {	0.362472,0.294033,0.127068,0.062781,0.041164,0.031343,0.024568,0.022604,0.013861,0.008500,0.005213,0.003197,0.003197	} });
			sizeFreqByYear.put(	19700	, new double[][] { {	0.607195692,0.262276848,0.108087619,0.019881257,0.002136239,0.000320617,7.18194E-05,2.33418E-05,5.0852E-06,1.12024E-06,2.49136E-07,5.58535E-08,5.58535E-08	}, {	0.515439445,0.259438756,0.149391269,0.063294982,0.010883994,0.001308513,0.000195825,3.37287E-05,9.54361E-06,2.71791E-06,7.7849E-07,2.24117E-07,2.24117E-07	} });
			sizeFreqByYear.put(	20090	, new double[][] { {	0.442678632,0.212073352,0.119632389,0.078392924,0.050732195,0.032207181,0.021111346,0.01747108,0.010604481,0.006438043,0.003909428,0.002374474,0.002374474	}, {	0.423703621,0.215338688,0.115736534,0.074902715,0.049951365,0.036420246,0.024543007,0.022818744,0.014410261,0.009109257,0.00576398,0.00365079,0.00365079	} });

			//females then males
			fishAbundByYear.put(	1950	, new int[] {	4073059	,	4710286	});
			fishAbundByYear.put(	1951	, new int[] {	5052333	,	5690131	});
			fishAbundByYear.put(	1952	, new int[] {	5375635	,	6075148	});
			fishAbundByYear.put(	1953	, new int[] {	5688658	,	6463717	});
			fishAbundByYear.put(	1954	, new int[] {	5960096	,	6829640	});
			fishAbundByYear.put(	1955	, new int[] {	6178116	,	7094310	});
			fishAbundByYear.put(	1956	, new int[] {	6262627	,	7215951	});
			fishAbundByYear.put(	1957	, new int[] {	6328555	,	7302241	});
			fishAbundByYear.put(	1958	, new int[] {	6313708	,	7294791	});
			fishAbundByYear.put(	1959	, new int[] {	6242527	,	7228032	});
			fishAbundByYear.put(	1960	, new int[] {	6240614	,	7228420	});
			fishAbundByYear.put(	1961	, new int[] {	6283392	,	7280715	});
			fishAbundByYear.put(	1962	, new int[] {	6311847	,	7330425	});
			fishAbundByYear.put(	1963	, new int[] {	6196062	,	7245979	});
			fishAbundByYear.put(	1964	, new int[] {	6090018	,	7148888	});
			fishAbundByYear.put(	1965	, new int[] {	6069000	,	7107296	});
			fishAbundByYear.put(	1966	, new int[] {	5857007	,	6879798	});
			fishAbundByYear.put(	1967	, new int[] {	5731568	,	6742692	});
			fishAbundByYear.put(	1968	, new int[] {	5648101	,	6656700	});
			fishAbundByYear.put(	1969	, new int[] {	5518939	,	6530406	});
			fishAbundByYear.put(	1970	, new int[] {	5771802	,	6775145	});
			fishAbundByYear.put(	1971	, new int[] {	5740386	,	6785626	});
			fishAbundByYear.put(	1972	, new int[] {	5954355	,	7018344	});
			fishAbundByYear.put(	1973	, new int[] {	6186339	,	7267252	});
			fishAbundByYear.put(	1974	, new int[] {	6458693	,	7537596	});
			fishAbundByYear.put(	1975	, new int[] {	6827272	,	7929073	});
			fishAbundByYear.put(	1976	, new int[] {	7166500	,	8301746	});
			fishAbundByYear.put(	1977	, new int[] {	7646289	,	8823250	});
			fishAbundByYear.put(	1978	, new int[] {	8957695	,	10187910	});
			fishAbundByYear.put(	1979	, new int[] {	8647632	,	9980068	});
			fishAbundByYear.put(	1980	, new int[] {	7380535	,	8833285	});
			fishAbundByYear.put(	1981	, new int[] {	6939603	,	8325834	});
			fishAbundByYear.put(	1982	, new int[] {	7162759	,	8443975	});
			fishAbundByYear.put(	1983	, new int[] {	5829469	,	9462326	});
			fishAbundByYear.put(	1984	, new int[] {	5061060	,	9345827	});
			fishAbundByYear.put(	1985	, new int[] {	5274702	,	9817249	});
			fishAbundByYear.put(	1986	, new int[] {	4948029	,	9893374	});
			fishAbundByYear.put(	1987	, new int[] {	6244559	,	10356092	});
			fishAbundByYear.put(	1988	, new int[] {	7889565	,	11589029	});
			fishAbundByYear.put(	1989	, new int[] {	9656134	,	11595402	});
			fishAbundByYear.put(	1990	, new int[] {	10275240	,	11556528	});
			fishAbundByYear.put(	1991	, new int[] {	8942613	,	11199856	});
			fishAbundByYear.put(	1992	, new int[] {	7796094	,	10671631	});
			fishAbundByYear.put(	1993	, new int[] {	7558521	,	10361850	});
			fishAbundByYear.put(	1994	, new int[] {	7508368	,	9960370	});
			fishAbundByYear.put(	1995	, new int[] {	8573803	,	10393070	});
			fishAbundByYear.put(	1996	, new int[] {	11110090	,	11084780	});
			fishAbundByYear.put(	1997	, new int[] {	11330010	,	10478230	});
			fishAbundByYear.put(	1998	, new int[] {	12486830	,	10257530	});
			fishAbundByYear.put(	1999	, new int[] {	11110700	,	9572360	});
			fishAbundByYear.put(	2000	, new int[] {	11019540	,	9070960	});
			fishAbundByYear.put(	2001	, new int[] {	11573160	,	9867440	});
			fishAbundByYear.put(	2002	, new int[] {	12649510	,	9599810	});
			fishAbundByYear.put(	2003	, new int[] {	12269370	,	9599440	});
			fishAbundByYear.put(	2004	, new int[] {	10181580	,	9031240	});
			fishAbundByYear.put(	2005	, new int[] {	9347370	,	8388400	});
			fishAbundByYear.put(	2006	, new int[] {	8825760	,	7909600	});
			fishAbundByYear.put(	2007	, new int[] {	8061990	,	8496250	});
			fishAbundByYear.put(	2008	, new int[] {	7947070	,	10283710	});
			fishAbundByYear.put(	2009	, new int[] {	7457770	,	9203740	});
			fishAbundByYear.put(	19700	, new int[] {	5667602	,	6676236	}); //avg of 1967-1970
			fishAbundByYear.put(	20090	, new int[] {	8073148	,	8973325	}); //avg of 2006-2009

			//release mortality taken from 2006 stock assessment
			double[][] releaseMort = { {0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08}, {0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08} }; 
			for (int i = 1990; i<2007; i++){ // up until regulation update in 1996
				recReleaseMortality.put(i, releaseMort); 
			}


			//************************************************************************
			// Calculations below are done in the spreadsheet SeatroutSA06_data
			//All done from 2006 stock assessment
			//************************************************************************

			/*Format: 
			 * 	Rec and Commercial catch rates (per year unit): per age class for males and females seperately
			 *	Rec probCatchAndRelease: per size class (in INCHES!!!!!! -- NEED TO CONVERT) for males
			 *		and females seperately , although they are the same because didn't have seperate data   
			 * 
			 * These are 
			 */

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

			for (int i = 1996; i<2000; i++){ // up until regulation update in 2000 -- bag limit change
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

			for (int i = 2000; i<2010; i++){ // up until 2006
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
		// FUNCTIONAL/UNIQUE RELATIONSHIPS -- SET FOR EACH FISH TYPE

		//TODO -- need to fill this in appropriately from Sven's paper; and make my own weightings for homeRange, density, and rugosity 
		@Override
		public double getHabQualFunction(String string, int yearClass, double value) { 


			if (string.equals("SAVCover")) {
				/*returns from a logistic curve with r=5, adjusted where
				 * origin is at 20% seagrass cover, and values range -1 to +1 
				 */
				return (1/(1+Math.exp(-5*(value-0.2)))-0.5)*2; 
			}

			else if (string.equals("homeRange")) {
				//here, distance is in degrees, so 0.004 is one grid cell
				//Seatrout generally stay within 3km, which is a radius of 1.5km, or roughly 4 cells (0.016degrees)
				//made this an inverse logistic curve so have a sharp transition from around their max home range, where max = 0 
				double val = -(1/(1+Math.exp(-1000*(value-homeRanges.get(yearClass).doubleValue())))-0.5)*2;
				if (val>0) return val;
				else return -(1/homeRanges.get(yearClass).doubleValue())*value;
				//return -(1/(1+Math.exp(-1000*(value-homeRanges.get(yearClass).doubleValue())))-0.5)*2; //linear version: -(1/homeRanges.get(yearClass).doubleValue())*value+1;
			}

			else if (string.equals("depth_spawn")){
				if (value > 1 && value <2) return 1;
				else return (-1)*(1/(1+Math.exp(-1*(Math.abs(1.5-value)-0.5)))-0.5)*2;
			}

			else if (string.equals("salinity")) return 0;
			else if (string.equals("density")) return 0;
			else if (string.equals("temperature")) return 0;
			else if (string.equals("rugosity")) return 0;
			else return 0; 
		}

		@Override
		public double getBaseFecundity(int yearClass, double mass) {

			//Note: R^2 value is higher for linear than power function

			// from 2006 trout stock assessment, citing Barbieri 2004
			//the .95 is ratio scaling from total weight to somatic weight, which is what Sue's equation
			//is based on -- I got this avg number from her data measuring both TW and SW
			if (this.useLinearFecundity) return (669.6*mass -43139d)*.95 ; 


			//from Sue's MEPS fecundity data 
			else return 114.7* Math.pow(mass, 1.2314); 

			// from Sue's MEPS data, similar to Barbieri 2004 but only 148 records
			// return 637.3*mass - 53905d; 
			//	return baseFecundity.get(yearClass);
		}



		@Override
		public double getNatInstMort(int yearClass, double size, int sex) {

			//use size dependent nat mort for year class >=0
			//		if (this.useSizeDependentM && (yearClass>=0)) 
			//from Gislason et al 2010: ln(M) = 0.55 - 1.61ln(L) + 1.44ln(Linf) + ln(K)
			// -->  M=K*e^(-(1.61*log(L))+(1.44*log(Linf))+.55)
			//			return (vonBertK[sex][period] * Math.exp(.55 - 1.61*Math.log(size)+1.44*Math.log(vonBertLinf[sex])));


			return natInstMortality.get(yearClass);
		}

		@Override
		public double getFishInstMort(int year, int yearClass, int sex) {
			return fishInstMortByYear.get(year)[sex][yearClass];
		}


		@Override
		public double getLengthAtAge(double age, int sex) {

			//transition age for double von bert by sex
			int period = 0; // this is double von bert period
			if (age > tp[sex]) period=1;


			//age is in years!
			return vonBertLinf[sex]*(1-Math.exp(-vonBertK[sex][period]*(age-vonBertT0[sex][period])));

			//else, return the McMichael and Peters 1989 quadratic fit to age in days
			//else return (0.448*(age*365.25)+0.0002*((age*365.25)*(age*365.25)))/10; 

		}



		public void getSpawnAggList(){

			spawnAggSize = new ArrayList<Integer>();  

			try {
				File file = new File(spawnAggFilename);
				FileDataStore store = FileDataStoreFinder.getDataStore(file);
				FeatureSource<SimpleFeatureType, SimpleFeature> fSource = store.getFeatureSource(); 

				FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
				FeatureIterator<SimpleFeature> iterator = features.features();
				try {
					while (iterator.hasNext()) {
						SimpleFeature simpleFeature = iterator.next();
						Point point = (Point) simpleFeature.getDefaultGeometry();
						spawnAggregationList.add(new Coordinate(point.getX(), point.getY(), 0));

						int aggSize = 
								(int) ((Long) simpleFeature.getAttribute("LabEstNumT")).longValue();

						spawnAggSize.add(aggSize);
					}
				} finally {
					iterator.close(); // IMPORTANT
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		@Override
		public double getFallowPeriod(int yearClass) {
			///=12.914*POWER(F6+1,-0.271)

			if (useSizeDependentFallowPeriod) return 12.914*Math.pow(yearClass+1,-0.271);
			else if (!useAgeTrunc) return 12.914*Math.pow(avgAge2004+1,-0.271);
			else return 12.914*Math.pow(avgAge1982+1,-0.271);

			//old read from vector
			//		if (this.useSizeDependentFallowPeriod) return fallowPeriods[yearClass];
			//		else if (!useAgeTrunc) return fallowPeriods[avgAge2004];
			//		else return fallowPeriods[avgAge1982];
		}


		@Override
		public double getFertilizationRtAvg(int yearClass) {
			///=0.1951*LN(F6+1)+0.3927

			if (useSizeDependentEggViability) return 0.1951*Math.log(yearClass+1)+0.3927;

			else if (!useAgeTrunc) return 0.1951*Math.log(avgAge2004+1)+0.3927;
			else return 0.1951*Math.log(avgAge1982+1)+0.3927;

			//old read from vector
			//		if (useSizeDependentEggViability) return fertRtAvgs[yearClass];
			//		else if (!useAgeTrunc) return fertRtAvgs[avgAge2004];
			//		else return fertRtAvgs[avgAge1982];
		}

		//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
		//RETURN METHODS FOR STATIC ASSIGNEMENTS -- IN GENERAL, DO NOT CHANGE BELOW THIS 


		public ArrayList<Integer> getSpawnAggSize() {
			return spawnAggSize;
		}





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
		public double getPropSetSizeAtMat(){
			return propSetSizeAtMat;
		}

		@Override
		public double getCondInflOnSpawn() {
			return conditionInfluenceOnSpawn;
		}

		@Override
		public double getSizeInflOnSpawnIntcpt() {
			return sizeInflOnSpawnIntcpt;
		}

		@Override
		public double getSizeInflOnSpawnKm() {
			return sizeInflOnSpawnKm;
		}

		@Override
		public double getSizeAtMaturitySD(){
			return sizeAtMaturitySD;
		}

		public int getAgeAtRecruitment(){
			return ageAtRecruitment; 
		}

		@Override
		public int getAvgPLD() {
			return avgPLD;
		}

		@Override
		public int getAvgEPSD() {
			return avgEPSD;
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
			//return Math.pow(10, weightLengthSlope*Math.log10(length) + weightLengthIntcept);
		}


		@Override
		public double getMassStDevScalar(int sex) {
			return massStDevScalar;
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
		public double getHomeRanges(int yearClass){
			return homeRanges.get(yearClass); 
		}

		@Override 
		public double getSpawnAggregationWeight(int yearClass){
			return spawnAggregationWeights.get(yearClass); 
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
		public double getFertilizationRtSD() {
			return fertRtSD;
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
		public double getCommInstMortality(int year, double length, int yearClass, int sex) {
			return commInstMortality.get(year)[sex][yearClass];
		}

		@Override
		public double getRecInstCatchRate(int year, double length, int yearClass, int sex) {
			return this.recInstCatchRate.get(year)[sex][yearClass];
		}

		//TODO -- need to make sure convert here since data is in inches for prob of cathc and relase
		@Override
		public double getProbOfCatchAndRelease(int year, double length, int yearClass, int sex) {
			length = Math.round(length/2.54)-1; //convert to inches 
			if (length>=33) length = 32;
			if (length<0) length = 0;

			return probCatchAndRelease.get(year)[sex][(int)length];
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





		public double[] getSizeFreqArray(int year, int sex) {
			return sizeFreqByYear.get(year)[sex];
		}


		@Override
		public int getPopulationAbundance(int year, int sex) {
			return fishAbundByYear.get(year)[sex];
		}


		public double getPopulationMass() {
			return popnMass;
		}


		@Override
		public double getMAdultVmax(int index) {
			return epsMAdultVmax[index];
		}

		@Override
		public double getMSettlerVmax(int index) {
			return epsMSettlerVmax[index];
		}

		@Override
		public double getMAdultKm(int index) {
			return epsMAdultKm[index];
		}

		@Override
		public double getMSettlerKm(int index) {
			return epsMSettlerKm[index];
		}

		@Override
		public double getMAdultExpon(int index) {
			return epsMAdultExpon[index];
		}

		@Override
		public double getMSettlerExpon(int index) {
			return epsMSettlerExpon[index];
		}


		@Override
		public double getEPSM(int index) {
			return epsM[index];
		}

}
