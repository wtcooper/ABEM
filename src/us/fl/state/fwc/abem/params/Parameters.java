package us.fl.state.fwc.abem.params;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import us.fl.state.fwc.abem.Scheduler;
import cern.jet.random.Normal;

import com.vividsolutions.jts.geom.Coordinate;


public interface Parameters {

	public void initialize(Scheduler sched); 
	
	//*************************** FOR AGENT METHODS ****************************


	
	/**If agent has set time steps (e.g., known calendar dates to run at), then set to true. */ 
	public  boolean isSetRunTimes();
	
	/**Set times steps in Calendar dates at which agent should run*/
	public ArrayList<Calendar> getSetRunTimes(); 

	/**If agent interacts with others, it's reactive. */ 
	public  boolean isReactive();

	/**If agent runs in the runQueue, then is runnable*/ 
	public  boolean isRunnable();

	/**The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing*/
	public  String getQueueType();

	/**The normal tick length when no interactions are present*/
	public  long getNormalTick();

	/** The priority as to what should run first; here, monitors have higher priority than organisms*/
	public  int getRunPriority();
	
	/**Map of all the dependent agent types with the preferred interaction tick for that dependent*/
	public HashMap<String, Long> getInteractionTicks();

	/**Other agents which this agent interacts with*/
	public ArrayList<String> getDependentsTypeList();

	
	
	
	//*************************** FOR THING METHODS ****************************
	/**Area over which individuals can sense their surroundings, in units of the coordinate
	 * system*/
	public  double getScaleOfPerception(int yearClass);


	
	//*************************** FOR FISH METHODS ****************************


	/**Average amount of time between reproductive events (normal curve), including gametogenesis*/
	public  double getFallowPeriod(int yearClass);

	/**Standard deviation of amount of time between reproductive events (normal curve), including gametogenesis*/
	public  double getFallowPeriodSD();

	/**Start of breeding season in days since start of new year*/
	public  double getStartOfBreeding();

	/**End of breeding season in days since start of new year*/
	public  double getEndOfBreeding();

	/**If agent has known spawning aggregations*/
	public  boolean haveSpawnAggregations();
	
	public ArrayList<Coordinate> getSpawnAggregationList(); 
	
	public double getSpawnAggregationSpecificity(int yearClass); 
	
	public double getHomeRanges(int yearClass); 
	
	public double getSpawnAggregationWeight(int yearClass); 
	
	/**If an agent spawns multiple times in a single breeding season*/
	public  boolean isMultipleSpawner();
	
	/**gets a list of normal distributions used to calculate the seasonal probability of spawning*/
	public ArrayList<Normal> getSeasonNormals(int yearClass);

	/**gets the peak days of the season at which individuals spawn for each life yearClass; if multiple peaks, then will have >1 entry in the ArrayList for each life yearClass*/
	public ArrayList<Integer>  getSeasonPeaks(int yearClass); 
	
	/**gets the peak sizes for the normal seasonal distibutions, relative to the largest peak which is set to 1, for each life yearClass*/
	public ArrayList<Double>  getSeasonPeakSizes(int yearClass); 

	/**gets a list of normal distributions used to calculate the lunar probability of spawning*/
	public ArrayList<Normal> getLunarNormals(int yearClass); 

	/**gets the peak days of the lunar period at which individuals spawn for each life yearClass; if multiple peaks, then will have >1 entry in the ArrayList for each life yearClass*/
	public ArrayList<Integer>  getLunarPeaks(int yearClass); 

	/**gets the peak sizes for the normal lunar distibutions, relative to the largest peak which is set to 1*/
	public ArrayList<Double>  getLunarPeakSizes(int yearClass); 

	/**gets the shift in the peak lunar day from the full/new moon cycle.  E.g., if peaks at 2 days after new and full moon, peakShift=2.  If only 1 peak, then set the other lunarProbPeak to 0.*/   
	public int getLunarPeakShift(int yearClass); 

	/**The proportion of females in population; here, 0.5 is 1:1 ratio of females:males*/
	public  double getSexRatio(int yearClass);
	
	/**A scaler which adjusts the batch fecundity to reflect size-dependent changes in fecundity; here, higher the value, greater size dependency*/
	public  double getFecunditySizeScaler(int yearClass);

	/**Total number of eggs that a female produces in a batch*/
	public  double getBaseFecundity(int yearClass, double mass);
	
	public double getFertilizationRtAvg(int yearClass);
	
	public double getFertilizationRtSD();
	
	/**Max age in years*/
	public  int getAgeMax();

	/** returns the age at recruitment (in years) */
	public int getAgeAtRecruitment(); 

	//	public double getMaturityProb(double size); 
//	public double getMaturitySize(double prob);
	
	/**returns the average age at which this species matures, from a normal distribution*/ 
	public double getSizeAtMaturityAvg();
	
	/**Returns the proportion of the average size at maturity at which to set the actual size at maturity based on condition */
	public double getPropSetSizeAtMat();

	/**returns the standard deviation for the age at which a species matures*/
	public double getSizeAtMaturitySD();
	
	

	
	/**Returns the length at a given age from the a growth function (e.g., von Bertalanfy, Gompertz)*/
	public  double getLengthAtAge(double age, int sex);

	
	/**Returns the length at a given age from the a vong Bert growth function that includes
	 * resource quality and biomass to adjust the Linfinity, using:
	 * Linf = LinfL - r(Ravg-Rt) - g(Bt-Bavg)*/
	public double getDDLengthAtAge(double age, int sex, double resource, double biomass);

	
	/**Initial mass, i.e. mass of an egg, for different sexes*/
	public  double getIniMass(int sex);
	
	/**Maximum mass, for different sexes*/
	public  double getMaxMass(int sex);
	
	/**Get's the total length from length-weight conversion*/
	public double getLengthAtMass(double mass, int sex); 
	
	/**Get's the total mass from the length-weight conversion*/
	public double getMassAtLength(double getLength, int sex); 

	/**Returns the amount to multiply by expected mass to get the stDev of the mass estimate -- then can assign a random mass as normal.nextDouble(estMass, estMass*stDevScalar)*/
	public double getMassStDevScalar(int sex);

	
	/**Rate at which true mass will catch up to nominal mass, e.g., if a fish is starved and has now found steady food, or after a reproduction event*/
	public  double getMassCatchUpRate();

	/**Smallest size of school after which a school will want to merge with another school.  Note, if set = 1, then will run as individual-based model because never will attempt to merge*/
	public  int getSchoolSize();
	
	/**Initial abundance of individuals for distributing them across a landscape through a [Organism]Builder class*/
	public int getPopulationAbundance(int year, int sex); 
	
	/**Carrying capacity in mass (kg) for the entire domain; note, a biomass monitor will keep track of the running total of biomass*/
	public  int getCarryCapacity();

	/**Cull period is the time frame over which density dependent mortality occurs when a species is above carrying capacity; see Gray et al. 2006*/
	public  double getCullPeriod();
	
	/**Number of test sites to use when assessing habitat suitability in the surrounding area*/
	public  int getNumTestSites();

	/**Determines the width between consecuative spirals in the search pattern (see Gray et al. 2006)*/
	public  double getSearchRadiusExponent(int yearClass);
	
	/**Number of points to use in a spiral for the habitat search algorithm; see Gray et al. 2006 for description*/
	public  int getNumPointsPerSpiral();

	/**The amount of "wandering" an agent will do while traveling along a path towards a waypoint; from Gray et al. 2006 algorithm*/
	public  double getDirectionalVariability();

	/**Returns a scaler for a particular habitat quality variable (string) which represents the strength of the partial effect of this variable relative to the other variables*/
	public double getHabQualFunction(String string, int yearClass, double value); 
	
	/**Returns the complexity of the habitat search algorithm (e.g., 1st tier, 2nd tier, 3rd tier, etc)*/
	public String getHabitatSearchComplexity(); 
	
	/**Determines if an animal has a home range or not; if so, will be a specific coordinate associated with their home range*/
	public boolean hasHomeRange(); 
	
	/**Normal traveling speed (m/s)*/
	public  double getCruiseSpeed(int yearClass);

	/**Max sprint speed (m/s)*/
	public  double getMaxSpeed(int yearClass);
	
	/**Minimal depth (m)*/
	public  double getMinDepth(int yearClass);
	
	/**Maximum depth (m)*/
	public  double getMaxDepth(int yearClass);
	
	/**Preferred depth (m)*/
	public  double getPreferredDepth(int yearClass);

	/**Maximum distance at which a school looking to merge can detect other schools in vicinity (m)*/
	public int getMergeRadius();
	
	/**Maximum distance at which a school looking to merge can detect other schools in vicinity (m)*/
	public HashMap<Integer, Double> getSuitableHabitats();

	/**Maximum distance at which a school looking to merge can detect other schools in vicinity (m)*/
	public ArrayList<Integer> getSuitableHabitatList();

	/**Returns boolean of if the fish is of recreational legal size for keeping */
	public boolean isRecLeagalSize(int year, double length); //private HashMap<Integer, Double>recMinSizeLimt = new HashMap<Integer, Double>(); // here, the Integer is the Year of the simulation 

	/**Returns boolean of if the current date is in the recreational open season*/
	public boolean isRecOpenSeason(Calendar currentDate); //private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> recOpenSeasons = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>(); 

	/**Returns the total number of days that the season is open in the year; used to adjust the instantaneous mortality rate to reflect actual day rates*/
	public int getRecOpenSeasonNumDays(int year); 

	/**Returns the recreational bag limit per day*/
	public int getRecBagLimit(int year);  //= new HashMap<Integer, Integer>(); //4; 
	
	/**Returns the instananeous rate of the likelihood of catching a fish */
	public double getRecInstCatchRate(int year, double length, int yearClass, int sex); // = new HashMap<Integer, Double>(); // here, Integer index is year, and double is the base instantaneous catch rate for the species; for seatrout, this has increased over time

	/**Returns the liklihood of an individual dying after released from recreational fishing capture */
	public double getRecReleaseMortality(int year, double length, int yearClass, int sex); //private HashMap<Integer, Double>  recCatchMortality= new HashMap<Integer, Double>();					//TODO Catch and release mortality rate; likelihood of dying after capture; could make Index a size or age index; was a study from Texas that had release mortality rates based on age -- do Google search to find and see if should bring in 

	/**Returns the liklihood that an individual fish will be kept after its captured.  This is difficult to parameterize because is a behavioral choice of humans, but can base on actual survey data*/
	public double getProbOfCatchAndRelease(int year, double length, int yearClass, int sex); //	private HashMap<Integer, Double[] > propCatchAndRelease = new HashMap<Integer, Double[]>(); // here, Double[] is a double array of the proportion of released fish for each siz, starting from 0-

	/**Returns a boolean of if the fish is commercial legal size for keeping*/
	public boolean isCommLegalSize(int year, double length); 
	
	/**Returns boolean of if the current date is in the commercial open season*/
	public boolean isCommOpenSeason(Calendar currentDate); 

	/**Returns the total number of days that the season is open in the year; used to adjust the instantaneous mortality rate to reflect actual day rates*/
	public int getCommOpenSeasonNumDays(int year); 

	/**Returns the commercial bag limit per day*/
	public int getCommBagLimit(int year);   

	/**Returns the instaneous rate of mortality due to commercial fishing*/
	public double getCommInstMortality(int year, double length, int yearClass, int sex); 

	/**Base mortality rate, M/year*/
	public  double getNatInstMort(int yearClass, double length, int sex);

	/**The average pelagic larval duration, PLD*/
	public int getAvgPLD(); 
	
	/**The average early-post-settlement duration (EPSD) if considering early post-settlement different from juvenile mortality*/
	public int getAvgEPSD();
	
	/**Return the instantaneous early post-settlement natural mortality rate for initial period (18 days) and longer EPS period (3 months)*/
	public double getEPSM(int index);

	/**Return the max increase in M due to density of adults for initial period (18 days) and longer EPS period (3 months)*/
	public double getMAdultVmax(int index) ;

	/**Return the max increase in M due to density of settlers for initial period (18 days) and longer EPS period (3 months)*/
	public double getMSettlerVmax(int index) ;

	/**Return the density to the power of an exponent (controlling functional response shape) at 1/2 the max increase in M due to density of adults for initial period (18 days) and longer EPS period (3 months)*/
	public double getMAdultKm(int index) ;

	/**Return the density to the power of an exponent (controlling functional response shape) at 1/2 the max increase in M due to density of settlers for initial period (18 days) and longer EPS period (3 months)*/
	public double getMSettlerKm(int index) ;

	/**Return the exponent controlling the shape of the function response (x<=1 is Type II, x>1 is Type III) for increase in M due to density of adults for initial period (18 days) and longer EPS period (3 months)*/
	public double getMAdultExpon(int index) ;

	/**Return the exponent controlling the shape of the function response (x<=1 is Type II, x>1 is Type III) for increase in M due to density of settlers for initial period (18 days) and longer EPS period (3 months)*/
	public double getMSettlerExpon(int index) ;

	/**Returns the influence of condition on spawn, set as condition to the power of cond influence for spawn*/
	public double getCondInflOnSpawn();

	double getSizeInflOnSpawnIntcpt();

	double getSizeInflOnSpawnKm();

	double getFishInstMort(int year, int yearClass, int sex);

	/**Returns F (yr-1) for a given year, size, and sex*/ 
	double getSizeDepFishMort(int year, double size, double expectedAge, int sex);

	/**Returns M (yr-1) for a given size and sex*/
	double getSizeDepNatMort(double size, double expectedAge, int sex);



	
}
