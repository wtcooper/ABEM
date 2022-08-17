package us.fl.state.fwc.abem.params.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;	

import us.fl.state.fwc.abem.params.AbstractParams;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class SeatroutParams extends AbstractParams {





	//###############################################################
	//###############################################################
	// SET ALL VARIABLE ASSIGNMENTS HERE
	//###############################################################
	// AGENT.CLASS VARIABLES
	private boolean isSetRunTimes = false;
	private boolean isReactive=true; 						//If agent interacts with others, it's reactive. 
	private boolean isRunnable=true;						//If agent changes over time, it's dynamic.  E.g., Bathymetry would not be dynamic
	private String queueType="runQueue";				//The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing
	private long normalTick=24l /*hour*/ * (60*60*1000); 								//The normal tick length when no interactions are present
	private int runPriority=3; 									//The priority as to what should run first; here, monitors have higher priority than organisms


	//******************************************************
	//ANIMAL.CLASS VARIABLES 
	// FEMALES ARE INDEX 0; MALES ARE INDEX 1
	//******************************************************

	//Population variables
	public int popnAbundance= 1000000; //2000000; 			//6260925;  
	private int schoolSize=1;							//smallest school after which will merge 

	//Spawning variables
	private double startOfBreeding= 0; //32; //59 is 1st day in Sue's data 
	private double endOfBreeding=365; //287; //266 is last day in Sue's data 
	private boolean haveSpawnAggregations=true;	//If agent has known spawning aggregations
	private boolean isMultipleSpawner=true; 			//TODO If an agent spawns multiple times in a single breeding season
	private int ageMax=12; 								//TODO Max age in years
	private double sizeAtMaturityAvg=26.15; // From SizeAtMaturity R code maximum likelihood //not sure where this came from - 26.8002; 	
	private double propSetSizeAtMat = .75; 
	private double sizeAtMaturitySD=1.291303; //From SizeAtMaturity R code maximum likelihood  //not sure where this value came from -- 2.196391585;
	private int avgPLD = 18; 								// actual PLD's are shorter (most settled in Peebles and Tolly by age 10, but mort rate in said cite is up through 18 days, and Reefbase uses 19 days for LD 
	private int avgEPSD = 90-avgPLD; 								//this is for age 10-90 days, using Powell et al. 2004 growth/mort rates
	private int ageAtRecruitment = 1; 
	public String spawnAggFilename = "data/HydroSurveys_largeAggs.shp";


	//Growth params 
	//DOUBLE von Bert FEMALES = 0, MALES = 1
	//	private double[] tp = {1.261873109, 1.198195743};
	//	private double[] vonBertLinf = {68.1123, 67.9246};
	//	private double[][] vonBertK = {{0.5536, 0.1728},{0.4895, 0.0878}};
	//	private double[][] vonBertT0= {{.0957, -2.4742}, {.0745, -5.0666}};
	//	private double[] lorenzenM1 = {16.70964746, 15.31451011}; // from calculations in Lorenzen.xlsx

	//Porch smooth function versus double von bert -- here, fit to Mike's data, including commercial fisheries 
	//	private double[] vonBertLinf = {104.6327711, 134.76058661};
	//	private double[] vonBertK0 = {0.06611822, 0.02578988};
	//	private double[] vonBertK1 = {0.59501913, 0.44850093};
	//	private double[] vonBertT0= {0.1290456, 0.11813809};
	//	private double[] vonBertLambda1= {1.5856164, 1.62922482};
	//	private double[] LinfStDev = {13.43803629, 13.43803629}; //this is last 10 generations of genetic algorithm optimization

	//Porch smooth function versus double von bert -- here, fit to Sues data for all >1yr old and McMicheal and Peter's data on juveniles 
	//############# NOTE ################
	//  Using Sue's data because are otilith age'd, and original fecundity vector used these same data
	//Damped version (B2=0) of Porch (unstable optimization -- wants to maximize Linf to ~3K+
	//	private double[] vonBertLinf = {188.77310610, 0};
	//	private double[] vonBertK0 = {0.02456212, 0};
	//	private double[] vonBertK1 = {0.34912796, 0};
	//	private double[] vonBertT0= {0.13287334, 0};
	//	private double[] vonBertLambda1= {1.55576918, 0};
	//	private double[] LinfStDev = {20.79520982, 20.79520982}; //this is last 10 generations of genetic algorithm optimization

	private double[] vonBertLinf = {73.8541802945, 0};
	private double[] vonBertK0 = {0.137847473645, 0};
	private double[] vonBertK1 = {2.60713468477, 0};
	private double[] vonBertT0= {0.0829245086880, 0};
	private double[] vonBertLambda1= {2.69984113009, 0};
	private double[] vonBertK2 = {4.93480759872, 0};
	private double[] vonBertTs= {0.524912054675, 0};
	private double[] vonBertLambda2= {5.61020041942, 0};
	private double[] LinfStDev = {10.182, 10.182}; //this is from ADMB .std file

	private double[] lorenzenM1 = {17.09157484, 15.38526581}; // from calculations in Lorenzen.xlsx

	//need to make a lookup table for the ages so can  get an expected age from size
	//need to do this because is not a solution for age from the Porch equation that I could find
	private double[] lookupAgeTable;


	//Growth params for density- and resource-dependent growth
	private double[] vonBert_r = {25, 25}; // this is resource-dependent coefficient, in cm units (max range of +/- 10cm impact on Linf
	private double[] vonBert_g = {0.204052635, 0.204052635}; // this is DD competition coefficient, in cm*kg/ha biomass units
	public double[] avgBiomassPerArea = {16.17229786, 16.17229786}; //this is expected average biomass in kg/ha, derived from Ecopath input -- ages 0-6mo: 0.0251, ages 6-18mo: 0.2548, ages18+ 1.5028, in t/km2, converted to kg/ha for Lorenzen and Enberg equation

	private double[] avgResource = {.5, .5}; //this is average resource 
	public final double numHectaresSlope = 0.0285; //0.032481572; //this is derived from size freq from F rates from 2003-2007 (assessment) times average wt at age (double von bert), assumming 2,000,000 individuals and the avg biomass per hectare from Ecopath model 
	public final double femaleToMaleWeight = 1.5; //this is estimated ratio of female to male weight -- this will change, but is fudge factor to get an estimate of the weight of males out there  

	private double[] a = {0.0112364 , 0.0112364 }; //set the same - no diff b/w males/females
	private double[] b = {2.9360025, 2.9360025}; 
	private double massStDevScalar = .09; //this is optimized amount to multiply by expected mass to get the stDev of the mass estimate -- then can assign a random mass as normal.nextDouble(estMass, estMass*stDevScalar)


	//early post-settlement DD mortality rates -- set maxMOffsets to 0,0 to turn of DD mortality
	private double[] epsM = {.5, .0585}; //{.5, .0585}; //early post-settlement M for initial period (18 days) and longer EPS period (3 months)
	private double[] epsMAdultVmax = {.1,.02}; //max offset in M due to adult density for initial period (18 days) and longer EPS period (3 months)
	private double[] epsMSettlerVmax = {.1, .02}; //max offset in M due to settler density for initial period (18 days) and longer EPS period (3 months)
	private double[] epsMAdultKm = {.05, .05}; //strength of influence / rate of change in offset in M due to adult density for initial period (18 days) and longer EPS period (3 months)
	private double[] epsMSettlerKm = {.001, .001}; //strength of influence / rate of change in offset in M due to settler density for initial period (18 days) and longer EPS period (3 months)
	private double[] epsMAdultExpon = {1, 1}; //average density of adults per m2
	private double[] epsMSettlerExpon = {2, 2}; //average density of settlers per m2 

	private String habitatSearchComplexity = "1st tier"; 
	private boolean hasHomeRange = true; 

	private boolean areFishingParamsSet = false;



	//###############################################################
	//###############################################################
	//COLLECTIONS
	//###############################################################

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList; 	//Other agents which this agent interacts with
	private HashMap<String, Long>interactionTicks;  	//Map of all the dependent agent types with the preferred interaction tick for that dependent

	// THING.CLASS COLLECTIONS 
	private HashMap<Integer, Double> scaleOfPerception ; 	 //Area over which individuals can sense their surroundings

	//ANIMAL.CLASS COLLECTIONS
	private ArrayList<Coordinate> spawnAggregationList;
	private ArrayList<Integer> spawnAggSize; 						
	private HashMap<Integer, Double>  homeRanges ;
	private HashMap<Integer, double[]>  natInstMortality; 				// M/year
	private HashMap<Integer, double[][]>  fishInstMortByYear;
	private HashMap<Integer, double[][]>  propReleased;
	private HashMap<Integer, double[]>  FMult; 				// M/year
	private HashMap<Integer, double[][]>  sizeFreqByYear ;
	private HashMap<Integer, int[]>  fishAbundByYear ;

	private HashMap<Integer, Double>recMinSizeLimit; 
	private HashMap<Integer, Double> recMaxSizeLimit;
	private HashMap<Integer, int[][]> recOpenSeasons;
	private HashMap<Integer, Integer>recBagLimit; 
	private HashMap<Integer, double[][]> recInstCatchRate; 
	private HashMap<Integer, double[][]>  recReleaseMortality;  
	private HashMap<Integer, double[][]> probCatchAndRelease;
	private HashMap<Integer, Double>commMinSizeLimit; 
	private HashMap<Integer, Double> commMaxSizeLimit;
	private HashMap<Integer, int[][]> commOpenSeasons; 
	private HashMap<Integer, Integer>commBagLimit; 
	private HashMap<Integer, double[][]> commInstMortality;





	//###############################################################
	//###############################################################
	//Methods
	//###############################################################
	public void setPopAbundance(int abund){
		popnAbundance = abund;
	}


	public void setSchoolSize(int size){
		schoolSize = size;
	}


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

		// from 2006 trout stock assessment, citing Barbieri 2004
		//the .95 is ratio scaling from total weight to somatic weight, which is what Sue's equation
		//is based on -- I got this avg number from her data measuring both TW and SW
		return (669.6*mass -43139d)*.95 ; 

		//espential from Sue's MEPS fecundity data 
		//return 114.7* Math.pow(mass, 1.2314); 
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

		//This below is size-dependent values from Peebles/Tolly for recent recruits, and Mike's calculations using Kai's formula

		//TODO -- could also put in Kai's formula direct

		//synchronize this so will check first
		synchronized (this) {
			if (natInstMortality == null) {
				natInstMortality = new HashMap<Integer, double[]>();
				natInstMortality.put(-2,new double[] {.5,.5}); // this is average daily value from Peebles and Tolly 1998, also the value from FishBase based on Houde prediction, for larvae 6-18 days of age, here assummed for those dispersing
				natInstMortality.put(-1,new double[] {0.0585, 0.0585}); //this is daily value from Powell et al. 2004, for juveniles 40-90 days of age
				natInstMortality.put(0,new double[] {0.777289306, 0.664486101	});
				natInstMortality.put(1,new double[] {0.5461384, 0.481163507	});
				natInstMortality.put(2,new double[] {0.387951272,	0.3589465	});
				natInstMortality.put(3,new double[] {0.328162725,	0.315312881	});
				natInstMortality.put(4,new double[] {0.298804798,	0.295454597	});
				natInstMortality.put(5,new double[] {0.282623863,	0.285471738	});
				natInstMortality.put(6,new double[] {0.273143147,	0.280205932	});
				natInstMortality.put(7,new double[] {0.267389532,	0.277358147	});
				natInstMortality.put(8,new double[] {0.263823334,	0.275797312	});
				natInstMortality.put(9,new double[] {0.261584011,	0.274935575	});
				natInstMortality.put(10,new double[] {0.26016639,	0.274457894	});
				natInstMortality.put(11,new double[] {0.259264331,	0.274192515	});
				natInstMortality.put(12,new double[] {0.258688458,	0.274044899	});
			}
		}

		return natInstMortality.get(yearClass)[sex];
	}






	@Override
	public double getSizeDepNatMort(double size, double expectedAge, int sex){
		//TODO -- need to fix this up for full Porch model
		//return the Lorenzen natural mortality at size as a constant function of size
		//this formulation based on M. Murphy calculations for spotted seatrout, 2010 assessment
		//but using the growth formulation in Porch et al 2002
		double k=vonBertK0[sex]+vonBertK1[sex]*Math.exp(-vonBertLambda1[sex]*expectedAge);
		return  
				-Math.log(size/(size+vonBertLinf[sex]
						*(Math.exp(k)-1)))
						*(lorenzenM1[sex]/(vonBertLinf[sex]
								*k));

	}



	public double getExpectedAge(double size, int sex){
		synchronized (this) {
			if (lookupAgeTable == null) setLookupAgeTable(); 
		}

		double expAgeAtLength =0;
		int idx = Arrays.binarySearch(lookupAgeTable, size);
		if (idx >= 0) expAgeAtLength = lookupAgeTable[idx];

		// is less than lowest value, so set idx to 2nd value so that can get slope
		//between second and first value to interpolate backwards
		else if (idx == -1) idx = 1; 	 
		//if greater than highest value, set idx to highest value
		else if (-(idx + 1) >= lookupAgeTable.length)  idx = lookupAgeTable.length-1; 
		else idx = -(idx + 1);
		//interpolate between the two values
		double slope = (0.05) / (lookupAgeTable[idx] - lookupAgeTable[idx-1]);
		//		double fracChange = (value- valArray[idx-1])/(valArray[idx] - valArray[idx-1]);
		double unitChange = size- lookupAgeTable[idx-1];
		expAgeAtLength =  ((idx-1)+1)*0.05 + unitChange*slope;

		return expAgeAtLength;
	}



	@Override
	public double getSizeDepFishMort(int year, double size, double expAgeAtLength, int sex)	 {
		/*returns size-based F, where use rearranged von bert to convert from length to 
		 * expected age, then extrapolate between ages (assumming each age 
		 * represents midpoint of year)
		 */

		/*get age as continuous, using single von bert
		 *here, we use the assume that the mortality rates in nature due to fishing are size-based,
		 *since fishing regulations are based on size.  Thus, in the assessment when they present
		 *F for different ages, in reality those ages are probabbly representing sizes.  Therefore,
		 *use the size at age VBG that was used in the assessment to calculate the age for a 
		 *given size, thus representing the size that was represented in the assessment.
		 */

		// vonBertLinf[sex]*(1-Math.exp(-vonBertK[sex][period]*(age-vonBertT0[sex][period])));




		//FOR DOUBLE VON BERT
		/*		double expAgeAtLength = (vonBertK[sex][0]*vonBertT0[sex][0]
				+Math.log(vonBertLinf[sex]/(vonBertLinf[sex]-size)))
				/vonBertK[sex][0];

		double expAgeAtLength2 = (vonBertK[sex][1]*vonBertT0[sex][1]
				+Math.log(vonBertLinf[sex]/(vonBertLinf[sex]-size)))
				/vonBertK[sex][1];

		if (expAgeAtLength2>expAgeAtLength) expAgeAtLength= expAgeAtLength2; 
		//if the age-at-length is not a number (i.e. when L>Linf) 
		if (new Double(expAgeAtLength).isNaN() || expAgeAtLength>12) expAgeAtLength=12;
		 */

		//set the hashmaps is not already done so
		synchronized (this) {
			if (FMult == null) setFMult();
			if (propReleased == null) setPropReleased();
		}

		//add 0.5 to age to make it representative of an age class, which is what the 
		//selectivity function and proportion released data are based on
		expAgeAtLength += 0.5; 
		//get commercial F
		double F=FMult.get(year)[0]*getSelectivity(year, 0, sex, expAgeAtLength);

		int propRelIndex = (int) expAgeAtLength;
		int propRelIndexPlus1 = propRelIndex+1;

		if (propRelIndex >= propReleased.get(year)[sex].length) {
			propRelIndex = propReleased.get(year)[sex].length-1;
			propRelIndexPlus1 = propRelIndex;
		}
		else 	if (propRelIndexPlus1>= propReleased.get(year)[sex].length) {
			propRelIndexPlus1 = propRelIndex;
		}

		//get rec F with release mortality
		double propRelease = propReleased.get(year)[sex][propRelIndex];
		//get next vale

		//else interpolate
		if ( (int) expAgeAtLength < 12){
			//here, get interpolated value as:
			// newProp = lowerProp + slope*dAge  (slope denom = 1yr so ignore) 
			propRelease += (expAgeAtLength-(int) expAgeAtLength)
					*(propReleased.get(year)[sex][propRelIndexPlus1]-propRelease);
		}

		//Add in rec F with 8% release mortality
		F += (1-propRelease)*FMult.get(year)[1]*getSelectivity(year, 1, sex, expAgeAtLength) 
				+ propRelease*0.08*FMult.get(year)[1]*getSelectivity(year, 1, sex, expAgeAtLength);


		return F;

		//This approach is based on interpolating between the output F rates
		// Gives very peaky values
		/*
		//call this to instantiate map
		if (fishInstMortByYear == null) getFishInstMort(year, 0, sex);  


		//if it's older than 12.5yrs, or if size is greater than Linf, then return the last year 
		if ( age > 12.5 || new Double(age).isNaN()) return getFishInstMort(year, 12, sex);

		//else, interpolate between the two bounding years
		else {
			double F=0, slope=0;
			double lowAge = (int) (age - 0.5);
			double highAge = lowAge+1;
			double lowF=0, highF=0;

			//if the fish is younger than 0.5yrs, then bound between 0 and F at 0.5yrs
			if (age<0.5) {
				lowAge=0;
				lowF=0;
				highF = getFishInstMort(year, 0, sex);
				slope = (highF-lowF)/.5;

				F= slope*age;
			}
			else {
				lowF = getFishInstMort(year, (int) lowAge, sex);
				highF = getFishInstMort(year, (int) highAge, sex);
				slope = (highF-lowF);
				F=slope*(age-(lowAge+.5)) + lowF; 
			}

			return F;
		}
		 */

	}


	public void setLookupAgeTable() {

		//242 items takes it up to 12 years at 0.05yr intervals (~half month)
		lookupAgeTable = new double[242];
		for (int i=0; i<242; i++) 
			lookupAgeTable[i] = getLengthAtAge((i+1)*.05, 0);
	}




	public double getSelectivity(int year, int fleet, int sex, double age){
		double gamma=0, beta=0, age50=0;

		//commercial
		if (fleet==1) {
			if (year<1996	) { //1st block
				if (sex==1){ gamma=1.99E-08; beta=2.1192; age50=2.5754; }
				else { gamma=0.039153; beta=2.136; age50=1.9694; }
			}
			else if (year<2001){
				if (sex==1){ gamma=0.04818; beta=2.1156; age50=	2.4567; }
				else { gamma=0.034175; beta=2.059 ; age50=	2.0704; }
			}
			else {
				if (sex==1){ gamma= 3.62E-08; beta= 2.0591; age50= 2.2434; }
				else { gamma= 0.055123; beta=2.0299 ; age50=1.8724; }
			}
		}
		//recreational
		else {
			if (year<1990) { //1st block
				if (sex==1){ gamma=7.93E-08; beta= 1.9804; age50= 2.0511; }
				else { gamma=0.088059; beta= 1.9449; age50= 1.511; }
			}
			else if (year<2001){
				if (sex==1){ gamma=0.079544; beta= 2.0313; age50=	2.1246 ; }
				else { gamma=0.06071; beta=  1.9341; age50=	1.576 ; }
			}
			else {
				if (sex==1){ gamma= 0.17682 ; beta=  2.2601; age50= 2.0408 ; }
				else { gamma= 0.069877 ; beta= 1.9526 ; age50= 1.5014; }
			}
		}

		//return an exponential-logistic selectivity based on ADMB output
		return 
				(1/(1-gamma))
				*Math.pow((1-gamma)/gamma,gamma)
				*(Math.exp(beta*gamma*(age50-age))
						/(1+Math.exp(beta*(age50-age))));
	}



	public void setFMult() {

		FMult = new HashMap<Integer, double[]>();

		FMult.put(	1950	, new double[] {	0.87232781	,	0.052110	});
		FMult.put(	1951	, new double[] {	0.87582411	,	0.063634	});
		FMult.put(	1952	, new double[] {	0.95571550	,	0.074430	});
		FMult.put(	1953	, new double[] {	1.02712537	,	0.100641	});
		FMult.put(	1954	, new double[] {	0.82477059	,	0.088002	});
		FMult.put(	1955	, new double[] {	0.64847618	,	0.117926	});
		FMult.put(	1956	, new double[] {	0.57727299	,	0.104633	});
		FMult.put(	1957	, new double[] {	0.69107287	,	0.085546	});
		FMult.put(	1958	, new double[] {	0.86160358	,	0.128093	});
		FMult.put(	1959	, new double[] {	0.87574529	,	0.132298	});
		FMult.put(	1960	, new double[] {	0.90932838	,	0.162773	});
		FMult.put(	1961	, new double[] {	0.92052787	,	0.251528	});
		FMult.put(	1962	, new double[] {	0.97917294	,	0.408440	});
		FMult.put(	1963	, new double[] {	0.88578590	,	0.377785	});
		FMult.put(	1964	, new double[] {	1.02662631	,	0.213696	});
		FMult.put(	1965	, new double[] {	1.36401151	,	0.229122	});
		FMult.put(	1966	, new double[] {	1.70255489	,	0.309437	});
		FMult.put(	1967	, new double[] {	1.54348061	,	0.380207	});
		FMult.put(	1968	, new double[] {	2.01693695	,	0.523415	});
		FMult.put(	1969	, new double[] {	1.52270750	,	0.466168	});
		FMult.put(	1970	, new double[] {	1.67717361	,	0.954031	});
		FMult.put(	1971	, new double[] {	1.11048849	,	0.787990	});
		FMult.put(	1972	, new double[] {	0.98029180	,	0.666403	});
		FMult.put(	1973	, new double[] {	0.39727760	,	1.061837	});
		FMult.put(	1974	, new double[] {	0.89061771	,	0.425185	});
		FMult.put(	1975	, new double[] {	0.71342355	,	0.375600	});
		FMult.put(	1976	, new double[] {	0.71425160	,	0.363528	});
		FMult.put(	1977	, new double[] {	0.54233575	,	0.343283	});
		FMult.put(	1978	, new double[] {	0.51051768	,	0.278149	});
		FMult.put(	1979	, new double[] {	0.50587787	,	0.643071	});
		FMult.put(	1980	, new double[] {	0.47309929	,	0.333704	});
		FMult.put(	1981	, new double[] {	0.48740007	,	0.548751	});
		FMult.put(	1982	, new double[] {	0.73696123	,	0.641427	});
		FMult.put(	1983	, new double[] {	0.48043660	,	0.912586	});
		FMult.put(	1984	, new double[] {	0.42568740	,	1.239329	});
		FMult.put(	1985	, new double[] {	0.32138998	,	0.590444	});
		FMult.put(	1986	, new double[] {	0.29264354	,	0.925720	});
		FMult.put(	1987	, new double[] {	0.23957231	,	0.667484	});
		FMult.put(	1988	, new double[] {	0.20321312	,	0.885201	});
		FMult.put(	1989	, new double[] {	0.11305284	,	1.070786	});
		FMult.put(	1990	, new double[] {	0.05232401	,	0.791433	});
		FMult.put(	1991	, new double[] {	0.04804583	,	0.870385	});
		FMult.put(	1992	, new double[] {	0.05944522	,	0.863208	});
		FMult.put(	1993	, new double[] {	0.03713111	,	0.816123	});
		FMult.put(	1994	, new double[] {	0.03362451	,	0.745053	});
		FMult.put(	1995	, new double[] {	0.02589255	,	0.707378	});
		FMult.put(	1996	, new double[] {	0.00222322	,	0.670769	});
		FMult.put(	1997	, new double[] {	0.00297414	,	0.962268	});
		FMult.put(	1998	, new double[] {	0.00335333	,	0.963144	});
		FMult.put(	1999	, new double[] {	0.00197576	,	0.775242	});
		FMult.put(	2000	, new double[] {	0.00151415	,	0.814770	});
		FMult.put(	2001	, new double[] {	0.00081404	,	0.780360	});
		FMult.put(	2002	, new double[] {	0.00102322	,	0.950491	});
		FMult.put(	2003	, new double[] {	0.00126182	,	1.052555	});
		FMult.put(	2004	, new double[] {	0.00057554	,	1.024333	});
		FMult.put(	2005	, new double[] {	0.00126435	,	1.113925	});
		FMult.put(	2006	, new double[] {	0.00036960	,	0.798564	});
		FMult.put(	2007	, new double[] {	0.00025832	,	0.916921	});
		FMult.put(	2008	, new double[] {	0.00054327	,	1.039020	});
		FMult.put(	2009	, new double[] {	0.00128384	,	0.933470	});

	}

	public void setPropReleased() {
		propReleased = new HashMap<Integer, double[][]>();

		propReleased.put(	1950	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1951	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1952	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1953	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1954	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1955	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1956	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1957	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1958	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1959	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1960	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1961	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1962	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1963	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1964	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1965	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1966	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1967	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1968	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1969	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1970	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1971	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1972	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1973	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1974	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1975	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1976	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1977	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1978	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1979	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1980	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1981	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1982	, new double[][] { {	0.69628,0.50112,0.33843,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732,0.23732	}, {	0.68279,0.73488,0.55921,0.46812,0.24298,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029,0.21029	} });
		propReleased.put(	1983	, new double[][] { {	0.64992,0.51569,0.3653,0.28095,0.28095,0.28095,0.28095,0.28095,0.28095,0.28095,0.28095,0.28095,0.28095	}, {	0.83293,0.55938,0.54535,0.48333,0.36142,0.33723,0.33723,0.33723,0.33723,0.33723,0.33723,0.33723,0.33723	} });
		propReleased.put(	1984	, new double[][] { {	0.625,0.70335,0.61479,0.59442,0.59442,0.59442,0.59442,0.59442,0.59442,0.59442,0.59442,0.59442,0.59442	}, {	0.52576,0.68503,0.69168,0.70178,0.64318,0.62632,0.62632,0.62632,0.62632,0.62632,0.62632,0.62632,0.62632	} });
		propReleased.put(	1985	, new double[][] { {	0.47791,0.5611,0.51045,0.52686,0.52686,0.52686,0.52686,0.52686,0.52686,0.52686,0.52686,0.52686,0.52686	}, {	0.53047,0.51556,0.55231,0.55578,0.54112,0.54458,0.54458,0.54458,0.54458,0.54458,0.54458,0.54458,0.54458	} });
		propReleased.put(	1986	, new double[][] { {	0.9485,0.46394,0.42777,0.38564,0.38564,0.38564,0.38564,0.38564,0.38564,0.38564,0.38564,0.38564,0.38564	}, {	0.9353,0.58547,0.48206,0.43385,0.3855,0.40967,0.40967,0.40967,0.40967,0.40967,0.40967,0.40967,0.40967	} });
		propReleased.put(	1987	, new double[][] { {	0.7499,0.68305,0.55197,0.457,0.457,0.457,0.457,0.457,0.457,0.457,0.457,0.457,0.457	}, {	0.72847,0.74244,0.71046,0.65864,0.56085,0.47517,0.23054,0.23054,0.23054,0.23054,0.23054,0.23054,0.23054	} });
		propReleased.put(	1988	, new double[][] { {	0.86017,0.66989,0.52367,0.36949,0.05045,0.05045,0.05045,0.05045,0.05045,0.05045,0.05045,0.05045,0.05045	}, {	0.89941,0.79843,0.69451,0.61152,0.48359,0.40717,0.15868,0.15868,0.15868,0.15868,0.15868,0.15868,0.15868	} });
		propReleased.put(	1989	, new double[][] { {	0.96311,0.84184,0.54573,0.62475,0.44879,0.44879,0.44879,0.44879,0.44879,0.44879,0.44879,0.44879,0.44879	}, {	0.95742,0.90774,0.80048,0.66975,0.6316,0.6316,0.72535,0.738,0.738,0.738,0.738,0.738,0.738	} });
		propReleased.put(	1990	, new double[][] { {	0.98641,0.96221,0.82344,0.79756,0.6986,0.45438,0.37763,0.99058,0.99058,0.99058,0.99058,0.99058,0.99058	}, {	0.9829,0.96591,0.92153,0.86463,0.82574,0.75092,0.8862,0.82859,0.82859,0.82859,0.82859,0.82859,0.82859	} });
		propReleased.put(	1991	, new double[][] { {	0.99096,0.95374,0.78865,0.74931,0.55211,0.11173,0.05324,1,1,1,1,1,1	}, {	0.99097,0.97722,0.91903,0.84004,0.80499,0.64988,0.80814,0.79587,0.79587,0.79587,0.79587,0.79587,0.79587	} });
		propReleased.put(	1992	, new double[][] { {	0.97494,0.9434,0.78384,0.74015,0.59626,0.36751,0.58825,0.9745,0.9745,0.9745,0.9745,0.9745,0.9745	}, {	0.97494,0.96615,0.91617,0.83899,0.79214,0.59788,0.81466,0.70798,0.70798,0.70798,0.70798,0.70798,0.70798	} });
		propReleased.put(	1993	, new double[][] { {	0.97577,0.9377,0.81857,0.76449,0.64792,0.52976,0.51023,0.45276,0.45276,0.45276,0.45276,0.45276,0.45276	}, {	0.97578,0.96576,0.91832,0.84748,0.80728,0.60978,0.81262,0.76683,0.76683,0.76683,0.76683,0.76683,0.76683	} });
		propReleased.put(	1994	, new double[][] { {	0.97229,0.94619,0.85234,0.80203,0.70872,0.50357,0.45136,0.45644,0.45644,0.45644,0.45644,0.45644,0.45644	}, {	0.97229,0.96534,0.92886,0.88445,0.85067,0.72754,0.88223,0.80862,0.80862,0.80862,0.80862,0.80862,0.80862	} });
		propReleased.put(	1995	, new double[][] { {	0.93794,0.92154,0.83537,0.80542,0.78438,0.8281,0.8355,0.85695,0.85695,0.85695,0.85695,0.85695,0.85695	}, {	0.93225,0.94011,0.90412,0.85221,0.82087,0.78561,0.87339,0.86155,0.86155,0.86155,0.86155,0.86155,0.86155	} });
		propReleased.put(	1996	, new double[][] { {	0.95353,0.94905,0.91447,0.85741,0.8205,0.81129,0.81878,0.81758,0.81758,0.81758,0.81758,0.81758,0.81758	}, {	0.9642,0.96196,0.93971,0.90853,0.87731,0.78614,0.77921,0.81337,0.81337,0.81337,0.81337,0.81337,0.81337	} });
		propReleased.put(	1997	, new double[][] { {	0.95542,0.94835,0.90446,0.84325,0.80808,0.8235,0.80288,0.81905,0.81905,0.81905,0.81905,0.81905,0.81905	}, {	0.96952,0.96681,0.94658,0.91748,0.88379,0.77906,0.77278,0.81629,0.81629,0.81629,0.81629,0.81629,0.81629	} });
		propReleased.put(	1998	, new double[][] { {	0.97723,0.95409,0.86257,0.8044,0.6964,0.73109,0.75078,0.76918,0.76918,0.76918,0.76918,0.76918,0.76918	}, {	0.97842,0.97406,0.94683,0.91043,0.87554,0.73738,0.74656,0.74797,0.74797,0.74797,0.74797,0.74797,0.74797	} });
		propReleased.put(	1999	, new double[][] { {	0.87148,0.96319,0.90634,0.80435,0.8087,0.8151,0.75388,0.85493,0.85493,0.85493,0.85493,0.85493,0.85493	}, {	0.98316,0.98131,0.95593,0.91924,0.88927,0.77538,0.77971,0.81914,0.81914,0.81914,0.81914,0.81914,0.81914	} });
		propReleased.put(	2000	, new double[][] { {	0.97943,0.96514,0.92657,0.86745,0.76253,0.77338,0.84668,0.88166,0.88166,0.88166,0.88166,0.88166,0.88166	}, {	0.97945,0.97534,0.95879,0.95091,0.87998,0.85367,0.75964,0.77571,0.77571,0.77571,0.77571,0.77571,0.77571	} });
		propReleased.put(	2001	, new double[][] { {	0.98445,0.97928,0.93707,0.89021,0.81365,0.80383,0.82615,0.84164,0.84164,0.84164,0.84164,0.84164,0.84164	}, {	0.98661,0.98367,0.98065,0.96786,0.9484,0.84257,0.83356,0.82988,0.82988,0.82988,0.82988,0.82988,0.82988	} });
		propReleased.put(	2002	, new double[][] { {	0.98932,0.9835,0.93462,0.80209,0.75258,0.75514,0.81448,0.8973,0.8973,0.8973,0.8973,0.8973,0.8973	}, {	0.98971,0.98721,0.97232,0.93755,0.93158,0.85112,0.77478,0.86024,0.86024,0.86024,0.86024,0.86024,0.86024	} });
		propReleased.put(	2003	, new double[][] { {	0.98268,0.96446,0.88181,0.80079,0.78752,0.79618,0.86688,0.90847,0.90847,0.90847,0.90847,0.90847,0.90847	}, {	0.98339,0.98098,0.96721,0.94513,0.93173,0.86396,0.83083,0.81334,0.81334,0.81334,0.81334,0.81334,0.81334	} });
		propReleased.put(	2004	, new double[][] { {	0.98035,0.95274,0.85881,0.73888,0.75278,0.81504,0.79534,0.8837,0.8837,0.8837,0.8837,0.8837,0.8837	}, {	0.98315,0.9774,0.94701,0.91285,0.89774,0.81801,0.79432,0.81197,0.81197,0.81197,0.81197,0.81197,0.81197	} });
		propReleased.put(	2005	, new double[][] { {	0.99004,0.96373,0.87962,0.85207,0.83522,0.84897,0.8815,0.93545,0.93545,0.93545,0.93545,0.93545,0.93545	}, {	0.99051,0.98292,0.94874,0.91359,0.89864,0.84498,0.8438,0.85235,0.85235,0.85235,0.85235,0.85235,0.85235	} });
		propReleased.put(	2006	, new double[][] { {	0.98913,0.97172,0.88011,0.79168,0.72446,0.76219,0.78275,0.79914,0.79914,0.79914,0.79914,0.79914,0.79914	}, {	0.98918,0.98686,0.9611,0.92722,0.9097,0.80286,0.75507,0.73031,0.73031,0.73031,0.73031,0.73031,0.73031	} });
		propReleased.put(	2007	, new double[][] { {	0.98081,0.96708,0.85628,0.78195,0.87135,0.91747,0.94792,0.96578,0.96578,0.96578,0.96578,0.96578,0.96578	}, {	0.98251,0.97865,0.96437,0.94486,0.93076,0.86249,0.84643,0.89224,0.89224,0.89224,0.89224,0.89224,0.89224	} });
		propReleased.put(	2008	, new double[][] { {	0.98515,0.96026,0.86654,0.82561,0.79119,0.67112,0.79328,0.88945,0.88945,0.88945,0.88945,0.88945,0.88945	}, {	0.9849,0.97873,0.93487,0.89673,0.89056,0.85127,0.83828,0.79466,0.79466,0.79466,0.79466,0.79466,0.79466	} });
		propReleased.put(	2009	, new double[][] { {	0.98586,0.97337,0.85519,0.68193,0.74701,0.81009,0.855,0.855,0.855,0.855,0.855,0.855,0.855	}, {	0.98571,0.98315,0.95186,0.87902,0.83493,0.72776,0.71651,0.74223,0.74223,0.74223,0.74223,0.74223,0.74223	} });


	}




	@Override
	public synchronized double getFishInstMort(int year, int yearClass, int sex) {
		if (fishInstMortByYear == null) {
			fishInstMortByYear  = new HashMap<Integer, double[][]>();
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
			fishInstMortByYear.put(	19700	, new double[][] { {	0.05569625,0.3848475,1.4504,2.20205,2.163625,1.97985,1.7969,1.7969,1.7969,1.7969,1.7969,1.7969,1.7969	}, {	0.01146285,0.082987,0.551745,1.588525,2.1689,2.23725,2.21755,2.21755,2.21755,2.21755,2.21755,2.21755,2.21755	} }); //average of 1967-1970
			fishInstMortByYear.put(	19850	, new double[][] { {	0.04321975,0.2281175,0.7251275,0.9839475,0.92778,0.82626,0.731475,0.731475,0.731475,0.731475,0.731475,0.731475,0.731475	}, {	0.008197125,0.05658575,0.298175,0.7205425,0.946795,0.9585975,0.9316925,0.9316925,0.9316925,0.9316925,0.9316925,0.9316925,0.9316925	} });
			fishInstMortByYear.put(	20090	, new double[][] { {	0.00694955,0.03822725,0.165105,0.2664625,0.2319525,0.202495,0.1428185,0.12217125,0.12217125,0.12217125,0.12217125,0.12217125,0.12217125	}, {	0.0030645,0.018836,0.088428,0.1454925,0.119877,0.11368825,0.08228775,0.0822175,0.0822175,0.0822175,0.0822175,0.0822175,0.0822175	} }); //average of 2006-2009
			fishInstMortByYear.put(	0	, new double[][] { {0,0,0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0,0,0} }); //zero fishing moratlity

		}
		return fishInstMortByYear.get(year)[sex][yearClass];
	}




	@Override
	public double getLengthAtAge(double age, int sex) {

		//	=Linf*(1-EXP((k1_/lambda1*(EXP(-lambda1*P3)-EXP(-lambda1*t0)))-k0*(P3-t0)))

		double TL =  vonBertLinf[sex]
				*(1-Math.exp(
				//Beta1 section
				vonBertK1[sex]/vonBertLambda1[sex]
						*(Math.exp(-vonBertLambda1[sex]*age)
								-Math.exp(-vonBertLambda1[sex]*vonBertT0[sex]))

								//Beta2 section
								+(vonBertK2[sex]/(4*Math.PI*Math.PI+vonBertLambda2[sex]*vonBertLambda2[sex]))
								*(Math.exp(-vonBertLambda2[sex]*age)
										*(2*Math.PI*Math.cos(2*Math.PI*(vonBertTs[sex]-age))
												-vonBertLambda2[sex]*Math.sin(2*Math.PI*(vonBertTs[sex]-age)))
												-Math.exp(-vonBertLambda2[sex]*vonBertT0[sex])
												*(2*Math.PI*Math.cos(2*Math.PI*(vonBertTs[sex]-vonBertT0[sex]))
														-vonBertLambda2[sex]
																*Math.sin(2*Math.PI*(vonBertTs[sex]-vonBertT0[sex]))))
								
								//Normal von bert section
								-vonBertK0[sex]*(age-vonBertT0[sex])));

		if (TL > 2.0) return TL;
		//For DOUBLE VON BERT
		//transition age for double von bert by sex
		//int period = 0; // this is double von bert period
		//if (age > tp[sex]) period=1;


		//age is in years!
		//return vonBertLinf[sex]*(1-Math.exp(-vonBertK[sex][period]*(age-vonBertT0[sex][period])));

		//else, return the McMichael and Peters 1989 quadratic fit to age in days
		else return (0.448*(age*365.25)+0.0002*((age*365.25)*(age*365.25)))/10; 

	}



	/**
	 * Get's the expected length at age for the given L-infinity 
	 * @param Linf
	 * @param age
	 * @param sex
	 * @return
	 */
	public double getLengthAtAge(double Linf, double age, int sex) {

		//	=Linf*(1-EXP((k1_/lambda1*(EXP(-lambda1*P3)-EXP(-lambda1*t0)))-k0*(P3-t0)))
		double TL = Linf
				*(1-Math.exp(
				//Beta1 section
				vonBertK1[sex]/vonBertLambda1[sex]
						*(Math.exp(-vonBertLambda1[sex]*age)
								-Math.exp(-vonBertLambda1[sex]*vonBertT0[sex]))

								//Beta2 section
								+(vonBertK2[sex]/(4*Math.PI*Math.PI+vonBertLambda2[sex]*vonBertLambda2[sex]))
								*(Math.exp(-vonBertLambda2[sex]*age)
										*(2*Math.PI*Math.cos(2*Math.PI*(vonBertTs[sex]-age))
												-vonBertLambda2[sex]*Math.sin(2*Math.PI*(vonBertTs[sex]-age)))
												-Math.exp(-vonBertLambda2[sex]*vonBertT0[sex])
												*(2*Math.PI*Math.cos(2*Math.PI*(vonBertTs[sex]-vonBertT0[sex]))
														-vonBertLambda2[sex]
																*Math.sin(2*Math.PI*(vonBertTs[sex]-vonBertT0[sex]))))
								
								//Normal von bert section
								-vonBertK0[sex]*(age-vonBertT0[sex])));

		if (TL > 2.0) return TL;
		//For DOUBLE VON BERT
		//transition age for double von bert by sex
		//int period = 0; // this is double von bert period
		//if (age > tp[sex]) period=1;


		//age is in years!
		//return vonBertLinf[sex]*(1-Math.exp(-vonBertK[sex][period]*(age-vonBertT0[sex][period])));

		//else, return the McMichael and Peters 1989 quadratic fit to age in days
		else return (0.448*(age*365.25)+0.0002*((age*365.25)*(age*365.25)))/10; 

	}

	@Override
	public double getDDLengthAtAge(double age, int sex, double resource, double biomass) {

		double Linf = vonBertLinf[sex] - vonBert_r[sex]*(avgResource[sex] - resource) 
				- vonBert_g[sex]*(biomass-avgBiomassPerArea[sex]);

		double TL =  Linf
				*(1-Math.exp(
				//Beta1 section
				vonBertK1[sex]/vonBertLambda1[sex]
						*(Math.exp(-vonBertLambda1[sex]*age)
								-Math.exp(-vonBertLambda1[sex]*vonBertT0[sex]))

								//Beta2 section
								+(vonBertK2[sex]/(4*Math.PI*Math.PI+vonBertLambda2[sex]*vonBertLambda2[sex]))
								*(Math.exp(-vonBertLambda2[sex]*age)
										*(2*Math.PI*Math.cos(2*Math.PI*(vonBertTs[sex]-age))
												-vonBertLambda2[sex]*Math.sin(2*Math.PI*(vonBertTs[sex]-age)))
												-Math.exp(-vonBertLambda2[sex]*vonBertT0[sex])
												*(2*Math.PI*Math.cos(2*Math.PI*(vonBertTs[sex]-vonBertT0[sex]))
														-vonBertLambda2[sex]
																*Math.sin(2*Math.PI*(vonBertTs[sex]-vonBertT0[sex]))))
								
								//Normal von bert section
								-vonBertK0[sex]*(age-vonBertT0[sex])));

		if (TL > 2.0) return TL;
		//For DOUBLE VON BERT
		//transition age for double von bert by sex
		//int period = 0; // this is double von bert period
		//if (age > tp[sex]) period=1;


		//age is in years!
		//return vonBertLinf[sex]*(1-Math.exp(-vonBertK[sex][period]*(age-vonBertT0[sex][period])));

		//else, return the McMichael and Peters 1989 quadratic fit to age in days
		else return (0.448*(age*365.25)+0.0002*((age*365.25)*(age*365.25)))/10; 


	}


	/**
	 * Returns the Linf for the given sex
	 * 
	 * @param sex
	 * @return
	 */
	public double getLinf(int sex) {
		return vonBertLinf[sex] ;
	}


	/*	SW							SW				
	Linf	456.785	a	-7.88501				Linf	641.567	a	-7.85313
males	K	0.585347	b	2.94379			females	K	0.442982	b	2.92976
	 */

	public double getLinf_Assess(int sex) {
		if (sex==0) return 64.1567;
		else return 45.6785;
	}


	public double getLengthAtAge_Assess(double Linf, double age, int sex) {

		if (sex==0) 
			return (Linf*(1-Math.exp(-0.442982*age)));
		else 
			return (Linf*(1-Math.exp(-0.585347*age)));
	}


	public double getMassAtLength_Assess(double TL, int sex){
		double TL_inches = TL/2.54;
		double a=0,b=0;
		if (sex==0){
			a=-7.85313;
			b=2.92976;
		}
		else {
			a=-7.88501;
			b=2.94379;
		}

		double mass_lbs = Math.exp(a+Math.log(TL_inches+0.001)*b);
		//convert back to grams
		return 453.59237*mass_lbs;
	}

	/**
	 * Returns the st deviation of Linf in cm from Mike's 2010 sst assessment data, 
	 * where Linf sigma	 is calculated from the linear relationship between sigma and age 
	 * (sigma(mm)=2.9862*age+40.342), assumming Linf is at max
	 * age of 12 (so likely slight underestimate of individual variability). 
	 * 
	 * @param sex
	 * @return
	 */
	public double getLinfStDev_Assess(int sex) {
		return 76.1764/10.0;
	}


	public double getLinfStDev(int sex) {
		return LinfStDev[sex];
	}


	/**
	 * Return the number of hectares in the model based on the initial starting population
	 * abundance. 
	 * 
	 * @return
	 */
	public double getNumHectares(){
		// note: here we use the unweight popnAbundance (versus getPopulationAbundance(year, sex)
		//because this relationship was fit to this overarching popn abundance for simplicity
		//this should give the number of hectares where 2003-2007 produces an average density
		//that is close to the overall average density, using start-of-year abundances
		return numHectaresSlope*popnAbundance;
	}



	public synchronized ArrayList<Integer> getSpawnAggSize() {
		if (spawnAggSize == null) setSpawnAggList();
		return spawnAggSize;
	}


	public void setSpawnAggList(){

		spawnAggSize = new ArrayList<Integer>();  
		spawnAggregationList = new ArrayList<Coordinate>(); 
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
	public int getAgeMax() {
		return ageMax;
	}




	@Override
	public synchronized  ArrayList<String> getDependentsTypeList() {
		if (dependentsTypeList == null){
			dependentsTypeList = new ArrayList<String>();
			dependentsTypeList.add("Seatrout"); 
		}
		return dependentsTypeList;
	}



	@Override
	public double getEndOfBreeding() {
		return endOfBreeding;
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
	public synchronized HashMap<String, Long> getInteractionTicks() {
		if (interactionTicks == null){
			interactionTicks = new HashMap<String, Long>();
			interactionTicks.put("Seatrout", 1l /*hour*/ * (60*60*1000)); 
		}
		return interactionTicks;
	}




	@Override
	public long getNormalTick() {
		return normalTick;
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
	public synchronized  double getScaleOfPerception(int yearClass) {
		if (scaleOfPerception == null) {
			scaleOfPerception = new HashMap<Integer, Double>();
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
		}
		return scaleOfPerception.get(yearClass);
	}


	@Override
	public boolean hasHomeRange(){
		return hasHomeRange; 
	}

	@Override
	public synchronized double getHomeRanges(int yearClass){
		if (homeRanges == null) {
			homeRanges = new HashMap<Integer, Double>(); 
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
		}
		return homeRanges.get(yearClass); 
	}


	@Override 
	public String getHabitatSearchComplexity(){
		return habitatSearchComplexity; 
	}


	@Override
	public double getStartOfBreeding() {
		return startOfBreeding;
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
	public synchronized ArrayList<Coordinate> getSpawnAggregationList() {
		if (spawnAggregationList == null) setSpawnAggList();
		return spawnAggregationList;
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
	public synchronized int getCommBagLimit(int year) {
		if (!areFishingParamsSet) setFishingParams();
		return commBagLimit.get(year);
	}

	@Override
	public synchronized int getRecBagLimit(int year) {
		if (!areFishingParamsSet) setFishingParams();
		return recBagLimit.get(year);
	}

	@Override
	public synchronized double getCommInstMortality(int year, double length, int yearClass, int sex) {
		if (!areFishingParamsSet) setFishingParams();
		return commInstMortality.get(year)[sex][yearClass];
	}

	@Override
	public synchronized double getRecInstCatchRate(int year, double length, int yearClass, int sex) {
		if (!areFishingParamsSet) setFishingParams();
		return this.recInstCatchRate.get(year)[sex][yearClass];
	}

	//TODO -- need to make sure convert here since data is in inches for prob of cathc and relase
	@Override
	public synchronized double getProbOfCatchAndRelease(int year, double length, int yearClass, int sex) {
		if (!areFishingParamsSet) setFishingParams();
		length = Math.round(length/2.54)-1; //convert to inches 
		if (length>=33) length = 32;
		if (length<0) length = 0;

		return probCatchAndRelease.get(year)[sex][(int)length];
	}

	@Override
	public synchronized double getRecReleaseMortality(int year, double length, int yearClass, int sex) {
		if (recReleaseMortality == null) {
			recReleaseMortality= new HashMap<Integer, double[][]>();		
			//release mortality taken from 2006 stock assessment
			double[][] releaseMort = { {0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08}, {0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08,0.08} }; 
			for (int i = 1990; i<2007; i++){ // up until regulation update in 1996
				recReleaseMortality.put(i, releaseMort); 
			}
		}
		return recReleaseMortality.get(year)[sex][yearClass];
	}

	@Override
	public synchronized boolean isCommLegalSize(int year, double length) {
		if (!areFishingParamsSet) setFishingParams();
		double minSize = this.commMinSizeLimit.get(year); 
		double maxSize = this.commMaxSizeLimit.get(year); 
		if ( (length >= minSize) && (length <= maxSize)) return true;
		else return false;
	}

	@Override
	public synchronized boolean isRecLeagalSize(int year, double length) {
		if (!areFishingParamsSet) setFishingParams();
		double minSize = this.recMinSizeLimit.get(year); 
		double maxSize = this.recMaxSizeLimit.get(year); 
		if ( (length >= minSize) && (length <= maxSize)) return true;
		else return false;
	}


	@Override
	public synchronized boolean isCommOpenSeason(Calendar currentDate) {
		if (!areFishingParamsSet) setFishingParams();
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
	public synchronized boolean isRecOpenSeason(Calendar currentDate) {
		if (!areFishingParamsSet) setFishingParams();
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
	public synchronized int getCommOpenSeasonNumDays(int year) {
		if (!areFishingParamsSet) setFishingParams();
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
	public synchronized int getRecOpenSeasonNumDays(int year) {
		if (!areFishingParamsSet) setFishingParams();
		int[][] thisYearsSeasons = recOpenSeasons.get(year);
		int totalDays = 0; 
		for (int i=0; i< thisYearsSeasons.length; i++){
			int startDay = thisYearsSeasons[i][0]; 
			int endDay = thisYearsSeasons[i][1];
			totalDays += endDay-startDay; 
		}
		return totalDays;
	}


	public void setFishingParams(){

		areFishingParamsSet = true;

		recMinSizeLimit = new HashMap<Integer, Double>(); // here, the Integer is the Year of the simulation 
		recMaxSizeLimit = new HashMap<Integer, Double>(); //here the Integer index is the Year of the simulation
		recOpenSeasons = new HashMap<Integer, int[][]>(); // index is year, and int[number of open season within a year][0=startday, 1=endday]
		recBagLimit = new HashMap<Integer, Integer>(); //4; 
		recInstCatchRate = new HashMap<Integer, double[][]>(); // here, Integer index is year, double[2][maxAge] 
		probCatchAndRelease = new HashMap<Integer, double[][]>(); // here index is year and double[sex,2][integerOfLength]
		commMinSizeLimit = new HashMap<Integer, Double>(); // here, the Integer is the Year of the simulation 
		commMaxSizeLimit = new HashMap<Integer, Double>(); //here the Integer index is the Year of the simulation
		commOpenSeasons = new HashMap<Integer, int[][]>(); // index is year, and int[number of open season within a year][0=startday, 1=endday] 
		commBagLimit = new HashMap<Integer, Integer>(); //4; 
		commInstMortality= new HashMap<Integer, double[][]>(); // here index is year and double[sex,2][age, maxAge]

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





	public synchronized double[] getSizeFreqArray(int year, int sex) {

		if (sizeFreqByYear == null) {
			sizeFreqByYear = new HashMap<Integer, double[][]>();
			//females then males
			//these SF are based on age-specific fishing and natural mortality rates from assessment
			sizeFreqByYear.put(	1950	, new double[][] { {	0.52588234,0.2486135,0.14283348,0.05716258,0.01731667,0.00533351,0.00180538,0.00066013,0.00024625,0.00009278,0.00003496,0.0000133,0.00000511	}, {	0.48657912,0.23354938,0.14868032,0.08402955,0.03233753,0.01016447,0.00318002,0.00100298,0.00032273,0.00010489,0.00003409,0.00001119,0.00000371	} });
			sizeFreqByYear.put(	1951	, new double[][] { {	0.52776619,0.24933226,0.14257332,0.05612977,0.01660405,0.00499844,0.00165743,0.00059494,0.00021787,0.00008059,0.00002981,0.00001114,0.0000042	}, {	0.48776463,0.23408555,0.14888105,0.0836179,0.03163764,0.00971322,0.00296657,0.00091385,0.0002872,0.00009116,0.00002894,0.00000928,0.000003	} });
			sizeFreqByYear.put(	1952	, new double[][] { {	0.53389167,0.25169926,0.14184501,0.05266468,0.01421027,0.00390269,0.00118892,0.00039491,0.00013382,0.0000458,0.00001568,0.00000542,0.00000189	}, {	0.49189975,0.23596751,0.14957787,0.08216235,0.02907641,0.00815603,0.00226813,0.00063626,0.00018209,0.00005264,0.00001522,0.00000444,0.00000131	} });
			sizeFreqByYear.put(	1953	, new double[][] { {	0.54029449,0.2540709,0.14068641,0.04887438,0.01191923,0.00296374,0.00082428,0.00025206,0.00007864,0.00002478,0.00000781,0.00000249,0.0000008	}, {	0.49607301,0.23784484,0.15019561,0.08044136,0.02646356,0.0067192,0.00168602,0.00042715,0.0001104,0.00002882,0.00000752,0.00000198,0.00000053	} });
			sizeFreqByYear.put(	1954	, new double[][] { {	0.52725474,0.24906597,0.14236125,0.05639598,0.01694729,0.00519646,0.00175535,0.00064168,0.00023931,0.00009014,0.00003396,0.00001292,0.00000496	}, {	0.48714455,0.2337854,0.14873833,0.08372508,0.03207505,0.00999564,0.00310377,0.00097342,0.00031145,0.00010065,0.00003253,0.00001062,0.0000035	} });
			sizeFreqByYear.put(	1955	, new double[][] { {	0.51634672,0.24456655,0.14249081,0.0618392,0.02167626,0.00778803,0.0030542,0.0012828,0.00054968,0.0002379,0.00010297,0.00004501,0.00001987	}, {	0.47919167,0.23010057,0.14714853,0.08572911,0.03680054,0.01332794,0.00484604,0.0017839,0.00066995,0.00025413,0.0000964,0.00003693,0.00001429	} });
			sizeFreqByYear.put(	1956	, new double[][] { {	0.50878081,0.24144732,0.14256832,0.06522947,0.02482227,0.0096768,0.00409133,0.00184058,0.00084475,0.00039161,0.00018154,0.000085,0.0000402	}, {	0.47403185,0.22771374,0.14606158,0.08682011,0.03955762,0.01553116,0.00613971,0.00245662,0.0010028,0.00041346,0.00017047,0.00007099,0.00002986	} });
			sizeFreqByYear.put(	1957	, new double[][] { {	0.51704394,0.24494185,0.14284225,0.06160055,0.02121632,0.00746444,0.00286538,0.0011783,0.00049433,0.00020947,0.00008876,0.00003799,0.00001642	}, {	0.48004258,0.2305152,0.14736622,0.08566298,0.03625803,0.01291528,0.00460955,0.00166265,0.00061182,0.0002274,0.00008452,0.00003173,0.00001203	} });
			sizeFreqByYear.put(	1958	, new double[][] { {	0.53280144,0.25114208,0.14144611,0.05331453,0.01492681,0.00427896,0.00136082,0.00047145,0.00016663,0.00005949,0.00002124,0.00000766,0.00000279	}, {	0.49057863,0.23533023,0.1492921,0.08245435,0.03004105,0.00872781,0.00252317,0.00073806,0.00022025,0.00006639,0.00002001,0.00000609,0.00000187	} });
			sizeFreqByYear.put(	1959	, new double[][] { {	0.53429178,0.2517081,0.14123287,0.05246625,0.01436436,0.00402768,0.00125514,0.00042687,0.00014811,0.00005191,0.00001819,0.00000644,0.0000023	}, {	0.49156964,0.23577921,0.14945283,0.08208874,0.02942754,0.00836353,0.00236359,0.00067589,0.00019718,0.0000581,0.00001712,0.0000051,0.00000153	} });
			sizeFreqByYear.put(	1960	, new double[][] { {	0.53864233,0.25329701,0.14037014,0.04995301,0.01285307,0.00339561,0.0010029,0.00032509,0.00010751,0.00003591,0.00001199,0.00000405,0.00000138	}, {	0.49428983,0.23699637,0.14985315,0.08096896,0.02776971,0.00742198,0.00196963,0.00052964,0.0001453,0.00004026,0.00001116,0.00000312,0.00000088	} });
			sizeFreqByYear.put(	1961	, new double[][] { {	0.54511943,0.25550396,0.13850797,0.0461916,0.01092876,0.002677,0.00074074,0.00022705,0.000071,0.00002242,0.00000708,0.00000226,0.00000073	}, {	0.49793832,0.23858981,0.15029016,0.07920091,0.02563319,0.00629898,0.00153709,0.0003817,0.0000967,0.00002475,0.00000633,0.00000164,0.00000043	} });
			sizeFreqByYear.put(	1962	, new double[][] { {	0.55716642,0.25943987,0.13453104,0.03908212,0.00767881,0.00158465,0.00037725,0.00010141,0.00002781,0.0000077,0.00000213,0.0000006,0.00000017	}, {	0.50495546,0.24163341,0.15099198,0.07546634,0.0214509,0.00438095,0.00088716,0.00018418,0.00003901,0.00000835,0.00000179,0.00000039,0.00000008	} });
			sizeFreqByYear.put(	1963	, new double[][] { {	0.55115527,0.25740084,0.13622999,0.04275365,0.00944754,0.00218815,0.000579,0.00017132,0.00005172,0.00001577,0.00000481,0.00000148,0.00000046	}, {	0.50097038,0.23987246,0.15055544,0.07749174,0.02397451,0.00549639,0.00125407,0.00029305,0.00006986,0.00001682,0.00000405,0.00000099,0.00000024	} });
			sizeFreqByYear.put(	1964	, new double[][] { {	0.54870064,0.25689052,0.13809336,0.04389718,0.00953005,0.00213219,0.0005411,0.00015289,0.00004407,0.00001283,0.00000374,0.0000011,0.00000033	}, {	0.50080308,0.23990175,0.15071256,0.07800824,0.02367464,0.00535165,0.00119531,0.00027101,0.00006269,0.00001465,0.00000342,0.00000081,0.00000019	} });
			sizeFreqByYear.put(	1965	, new double[][] { {	0.56443236,0.2622076,0.13347398,0.03380493,0.00510646,0.00079343,0.00014355,0.00002972,0.00000628,0.00000134,0.00000029,0.00000006,0.00000001	}, {	0.51159804,0.24467442,0.15173877,0.0720601,0.01678357,0.0026586,0.00041012,0.00006416,0.00001024,0.00000165,0.00000027,0.00000004,0.00000001	} });
			sizeFreqByYear.put(	1966	, new double[][] { {	0.57974321,0.26661302,0.12652985,0.0244072,0.00242245,0.00024835,0.00003066,0.00000448,0.00000067,0.0000001,0.00000002,0,0	}, {	0.52153992,0.24890869,0.15198337,0.0650822,0.01118251,0.00117193,0.00011792,0.00001206,0.00000126,0.00000013,0.00000001,0,0	} });
			sizeFreqByYear.put(	1967	, new double[][] { {	0.57763965,0.26591594,0.12720758,0.02597723,0.00287376,0.00033115,0.00004577,0.00000745,0.00000124,0.00000021,0.00000003,0.00000001,0	}, {	0.51956881,0.24802783,0.15192393,0.06645599,0.01239682,0.00144302,0.00016244,0.00001867,0.00000219,0.00000026,0.00000003,0,0	} });
			sizeFreqByYear.put(	1968	, new double[][] { {	0.59613566,0.27032698,0.1165141,0.01599238,0.00096436,0.00006111,0.00000488,0.00000048,0.00000005,0,0,0,0	}, {	0.53141128,0.25289447,0.15142543,0.05694941,0.00685216,0.00043883,0.00002666,0.00000166,0.00000011,0.00000001,0,0,0	} });
			sizeFreqByYear.put(	1969	, new double[][] { {	0.58044783,0.26655722,0.12546652,0.0245949,0.00259903,0.00028847,0.00003869,0.00000615,0.000001,0.00000016,0.00000003,0,0	}, {	0.52089553,0.24854214,0.15187038,0.0653853,0.01183306,0.00131455,0.00014145,0.0000156,0.00000176,0.0000002,0.00000002,0,0	} });
			sizeFreqByYear.put(	1970	, new double[][] { {	0.60187015,0.27098778,0.11163267,0.01455033,0.00089298,0.00006022,0.00000523,0.00000057,0.00000006,0.00000001,0,0,0	}, {	0.53274681,0.25319357,0.15108329,0.05560935,0.00689193,0.00044533,0.00002778,0.00000182,0.00000012,0.00000001,0,0,0	} });
			sizeFreqByYear.put(	1971	, new double[][] { {	0.58046074,0.26602736,0.12391169,0.02581793,0.00324647,0.000444,0.0000737,0.00001447,0.0000029,0.00000059,0.00000012,0.00000002,0	}, {	0.51845911,0.24730667,0.15158131,0.06668648,0.01387796,0.00181666,0.00023499,0.00003173,0.00000437,0.00000061,0.00000008,0.00000001,0	} });
			sizeFreqByYear.put(	1972	, new double[][] { {	0.57049698,0.26328731,0.12860502,0.0315239,0.00500454,0.00085526,0.00017343,0.00004071,0.00000975,0.00000236,0.00000057,0.00000014,0.00000003	}, {	0.51223016,0.24468079,0.15138261,0.07086314,0.01740662,0.00287011,0.00046989,0.00007989,0.00001386,0.00000243,0.00000043,0.00000008,0.00000001	} });
			sizeFreqByYear.put(	1973	, new double[][] { {	0.55783568,0.25969099,0.13442581,0.03862819,0.00743934,0.00150288,0.00035056,0.00009244,0.00002487,0.00000676,0.00000184,0.0000005,0.00000014	}, {	0.50552606,0.24189366,0.15106303,0.07519443,0.02105435,0.00421735,0.00083653,0.00017001,0.00003525,0.00000738,0.00000155,0.00000033,0.00000007	} });
			sizeFreqByYear.put(	1974	, new double[][] { {	0.55408387,0.25833367,0.13518876,0.04105539,0.00869825,0.00194026,0.00049709,0.00014308,0.00004202,0.00001246,0.0000037,0.00000111,0.00000034	}, {	0.50261001,0.24057698,0.15070799,0.07659441,0.02302071,0.00506014,0.00110704,0.00024863,0.00005697,0.00001318,0.00000305,0.00000071,0.00000017	} });
			sizeFreqByYear.put(	1975	, new double[][] { {	0.54141173,0.25382652,0.13802034,0.04854596,0.01287917,0.00358652,0.00112631,0.00039015,0.00013788,0.00004921,0.00001757,0.00000633,0.00000231	}, {	0.49407644,0.2367626,0.14956857,0.08037996,0.02842375,0.00779611,0.0021439,0.00060425,0.00017374,0.00005046,0.00001465,0.0000043,0.00000127	} });
			sizeFreqByYear.put(	1976	, new double[][] { {	0.54033216,0.25345063,0.13829361,0.0491506,0.01321945,0.00372724,0.0011832,0.00041369,0.00014756,0.00005316,0.00001915,0.00000697,0.00000256	}, {	0.49344594,0.23648472,0.14948359,0.08065628,0.02879255,0.00800556,0.00223164,0.00063721,0.00018562,0.00005461,0.00001607,0.00000478,0.00000143	} });
			sizeFreqByYear.put(	1977	, new double[][] { {	0.52702745,0.24830247,0.13971402,0.0562096,0.01836906,0.00629098,0.00239025,0.00098522,0.00041429,0.00017596,0.00007474,0.00003206,0.00001389	}, {	0.48420947,0.23227107,0.14786033,0.08362212,0.03439489,0.01157929,0.00393676,0.00137074,0.00048692,0.0001747,0.00006268,0.00002272,0.00000831	} });
			sizeFreqByYear.put(	1978	, new double[][] { {	0.51863126,0.24506639,0.14066893,0.06030585,0.02150056,0.00798615,0.00326001,0.00143115,0.00064097,0.00028996,0.00013117,0.00005993,0.00002766	}, {	0.47899231,0.22990442,0.14689362,0.08512312,0.03722121,0.01366433,0.005071,0.00192149,0.00074279,0.00029003,0.00011324,0.00004466,0.00001779	} });
			sizeFreqByYear.put(	1979	, new double[][] { {	0.54532657,0.25466573,0.13511845,0.04652695,0.01266246,0.00371419,0.00124253,0.00046166,0.000175,0.000067,0.00002565,0.00000992,0.00000387	}, {	0.49425681,0.23667868,0.14929041,0.07949495,0.02892232,0.00809279,0.00229569,0.00067671,0.00020351,0.00006182,0.00001878,0.00000576,0.00000179	} });
			sizeFreqByYear.put(	1980	, new double[][] { {	0.5199165,0.24544435,0.1400487,0.05955976,0.02122416,0.00792276,0.00325919,0.00144458,0.00065322,0.00029835,0.00013626,0.00006286,0.00002929	}, {	0.47929262,0.23000826,0.14688386,0.08483873,0.03715923,0.01362072,0.00505765,0.00192293,0.00074587,0.00029222,0.00011449,0.0000453,0.00001811	} });
			sizeFreqByYear.put(	1981	, new double[][] { {	0.53763462,0.25196626,0.13691841,0.0506346,0.01512706,0.00482869,0.00173819,0.00068795,0.00027778,0.00011329,0.0000462,0.00001903,0.00000792	}, {	0.48965691,0.23464217,0.14862282,0.08134193,0.03157014,0.00969637,0.00301977,0.0009729,0.00031977,0.00010616,0.00003524,0.00001182,0.000004	} });
			sizeFreqByYear.put(	1982	, new double[][] { {	0.55699228,0.25889676,0.13291095,0.03976907,0.00863039,0.00201413,0.00054465,0.00016634,0.00005183,0.00001631,0.00000513,0.00000163,0.00000052	}, {	0.50276405,0.24051563,0.15050617,0.0759917,0.02342688,0.00525082,0.00118197,0.00027615,0.00006582,0.00001585,0.00000381,0.00000093,0.00000023	} });
			sizeFreqByYear.put(	1983	, new double[][] { {	0.55948312,0.25818551,0.13021388,0.03935127,0.00926208,0.0023999,0.00072449,0.00024739,0.00008618,0.00003033,0.00001067,0.00000379,0.00000136	}, {	0.50359372,0.2412838,0.14839533,0.07412181,0.02414675,0.00621055,0.00162783,0.0004464,0.00012489,0.00003529,0.00000997,0.00000285,0.00000082	} });
			sizeFreqByYear.put(	1984	, new double[][] { {	0.5551214,0.25276819,0.13077559,0.04348008,0.01211607,0.00366755,0.00126888,0.00048797,0.00019145,0.00007587,0.00003006,0.00001203,0.00000486	}, {	0.50156206,0.23858493,0.1467808,0.07444647,0.02688586,0.00809163,0.00247824,0.0007894,0.00025653,0.0000842,0.00002764,0.00000916,0.00000307	} });
			sizeFreqByYear.put(	1985	, new double[][] { {	0.52168482,0.24234337,0.13538541,0.05867628,0.02329912,0.00975623,0.00448064,0.00220363,0.00110566,0.00056034,0.00028397,0.00014536,0.00007516	}, {	0.48131606,0.23041212,0.14463113,0.08127972,0.03632501,0.01487224,0.00628764,0.00272821,0.00120769,0.00053997,0.00024143,0.00010903,0.00004973	} });
			sizeFreqByYear.put(	1986	, new double[][] { {	0.54352251,0.25757994,0.13072555,0.04614876,0.01414781,0.00473845,0.00180608,0.00076101,0.00032714,0.00014204,0.00006167,0.00002705,0.00001198	}, {	0.49795792,0.23912874,0.14829686,0.07512746,0.02681298,0.00837722,0.00279222,0.00097166,0.00034495,0.0001237,0.00004436,0.00001606,0.00000588	} });
			sizeFreqByYear.put(	1987	, new double[][] { {	0.51297201,0.24139203,0.13851274,0.06220667,0.02468096,0.01043789,0.00486775,0.00243989,0.00124766,0.00064442,0.00033284,0.00017364,0.0000915	}, {	0.47268582,0.22668268,0.14426345,0.08502464,0.04123433,0.01778403,0.00755803,0.00290226,0.00113697,0.00044989,0.00017802,0.00007115,0.00002872	} });
			sizeFreqByYear.put(	1988	, new double[][] { {	0.52778432,0.24905798,0.13841213,0.05659698,0.01920298,0.00569168,0.0019713,0.00077148,0.00030802,0.00012422,0.00005009,0.0000204,0.00000839	}, {	0.48034176,0.23066288,0.146603,0.08345756,0.03710302,0.01404222,0.00523752,0.00170417,0.0005657,0.00018967,0.00006359,0.00002154,0.00000737	} });
			sizeFreqByYear.put(	1989	, new double[][] { {	0.51318297,0.24400981,0.14299483,0.05784161,0.02434529,0.00950272,0.00418593,0.00201885,0.00099335,0.00049368,0.00024535,0.00012316,0.00006244	}, {	0.47017973,0.2259702,0.14501557,0.08586799,0.03988224,0.01718971,0.00775619,0.00393685,0.00206116,0.00108998,0.0005764,0.00030788,0.0001661	} });
			sizeFreqByYear.put(	1990	, new double[][] { {	0.45881563,0.21976448,0.13906041,0.080202,0.04533813,0.02476542,0.01239618,0.00629527,0.00454241,0.00331056,0.00241277,0.00177613,0.00132061	}, {	0.43368441,0.20855995,0.13502791,0.08671306,0.0523224,0.03110581,0.01862872,0.01243089,0.00821839,0.005488,0.00366473,0.00247179,0.00168394	} });
			sizeFreqByYear.put(	1991	, new double[][] { {	0.47195856,0.22602361,0.14236106,0.07919605,0.04238169,0.02031189,0.00781861,0.00316444,0.00229279,0.00167794,0.00122797,0.00090769,0.0006777	}, {	0.44000373,0.21159742,0.13702145,0.08736236,0.05107037,0.02948331,0.01627116,0.01033509,0.00665145,0.00432376,0.00281065,0.00184542,0.00122384	} });
			sizeFreqByYear.put(	1992	, new double[][] { {	0.47036015,0.22502018,0.14115902,0.07787828,0.04101702,0.02015288,0.00914372,0.00504954,0.00357143,0.00255139,0.00182268,0.00131518,0.00095853	}, {	0.4432887,0.21310356,0.13778069,0.08756184,0.05081823,0.02879654,0.01522921,0.00961184,0.00583384,0.00357639,0.00219248,0.0013576,0.00084908	} });
			sizeFreqByYear.put(	1993	, new double[][] { {	0.46262134,0.22150133,0.1394834,0.08057074,0.04482901,0.02380952,0.01252574,0.00685882,0.00371098,0.00202801,0.00110829,0.00061175,0.00034107	}, {	0.43662957,0.20995062,0.13594527,0.0873013,0.05234809,0.03100397,0.01718515,0.01117138,0.0072333,0.00473052,0.00309372,0.0020436,0.0013635	} });
			sizeFreqByYear.put(	1994	, new double[][] { {	0.45404605,0.21748412,0.13773407,0.08245682,0.04822631,0.02744145,0.01470878,0.00806291,0.00452078,0.00256022,0.00144991,0.00082937,0.00047918	}, {	0.42722656,0.20545708,0.13320112,0.08653225,0.05413696,0.03366629,0.02052322,0.01401583,0.00942765,0.00640518,0.0043517,0.00298628,0.00206987	} });
			sizeFreqByYear.put(	1995	, new double[][] { {	0.44161077,0.2112939,0.13346471,0.08008248,0.04776041,0.02901657,0.01899123,0.01281885,0.00891912,0.00626812,0.00440507,0.00312688,0.00224189	}, {	0.42621895,0.2048678,0.13261682,0.08576851,0.05322732,0.0329942,0.0211194,0.01454841,0.01016959,0.00718016,0.0050695,0.00361525,0.00260409	} });
			sizeFreqByYear.put(	1996	, new double[][] { {	0.4261943,0.20422059,0.13040666,0.08322708,0.05293066,0.03392027,0.02265301,0.0155754,0.01091942,0.00773219,0.00547528,0.00391609,0.00282906	}, {	0.41558194,0.19989564,0.12984539,0.08581359,0.05643488,0.0372871,0.02464804,0.01678419,0.01183282,0.00842596,0.00599999,0.00431543,0.00313503	} });
			sizeFreqByYear.put(	1997	, new double[][] { {	0.44460094,0.21253939,0.13402205,0.08169676,0.04824317,0.02861595,0.01811996,0.01168194,0.00776537,0.00521378,0.0035006,0.00237397,0.00162612	}, {	0.42776662,0.20561232,0.13298129,0.08615573,0.0544649,0.03428461,0.0210929,0.0134982,0.00905237,0.00613185,0.00415356,0.0028418,0.00196385	} });
			sizeFreqByYear.put(	1998	, new double[][] { {	0.45401328,0.21732392,0.13725932,0.08111938,0.04626945,0.02502041,0.01479149,0.00921076,0.00592258,0.00384653,0.0024982,0.00163881,0.00108586	}, {	0.43019556,0.20681881,0.13388284,0.0867341,0.05447365,0.03403594,0.02030581,0.01277956,0.0082125,0.00533062,0.00346003,0.00226843,0.00150215	} });
			sizeFreqByYear.put(	1999	, new double[][] { {	0.43596651,0.20788342,0.13261026,0.08309582,0.04981093,0.03093581,0.02025992,0.01320855,0.00926732,0.00656744,0.00465413,0.00333137,0.00240852	}, {	0.4185757,0.20134464,0.13082297,0.08633612,0.05626755,0.03678014,0.02360145,0.01575075,0.0109362,0.00766963,0.00537876,0.00381007,0.00272601	} });
			sizeFreqByYear.put(	2000	, new double[][] { {	0.43338377,0.20769311,0.13236279,0.08355178,0.05197987,0.03098293,0.01961582,0.01335027,0.00945143,0.00675847,0.0048328,0.00349054,0.00254642	}, {	0.41836005,0.20120921,0.13057924,0.08605643,0.0570794,0.03683223,0.02457927,0.01611263,0.01086659,0.00740224,0.00504235,0.00346934,0.00241103	} });
			sizeFreqByYear.put(	2001	, new double[][] { {	0.42837329,0.20521654,0.13094439,0.08313749,0.05296152,0.03315739,0.0217383,0.01485811,0.01044095,0.00741071,0.00525992,0.00377087,0.00273053	}, {	0.40599615,0.19515168,0.12623283,0.08300867,0.05599745,0.0389889,0.02758602,0.02023106,0.01512379,0.01141946,0.00862245,0.00657595,0.00506558	} });
			sizeFreqByYear.put(	2002	, new double[][] { {	0.44354973,0.21228468,0.13477857,0.0836719,0.04788361,0.02747409,0.0168078,0.01108678,0.00784521,0.00560721,0.00400764,0.00289317,0.00210961	}, {	0.41475978,0.19927172,0.12854221,0.082975,0.05356626,0.03632907,0.02531782,0.01802097,0.01340887,0.01007741,0.00757366,0.00574917,0.00440807	} });
			sizeFreqByYear.put(	2003	, new double[][] { {	0.4518357,0.21597843,0.13567041,0.07954783,0.04426651,0.02544982,0.01567298,0.01051843,0.00740576,0.00526662,0.00374536,0.00269029,0.00195185	}, {	0.41856922,0.20099266,0.12923137,0.08240823,0.05284671,0.03546426,0.02459309,0.01764403,0.01284316,0.00944254,0.00694234,0.00515544,0.00386695	} });
			sizeFreqByYear.put(	2004	, new double[][] { {	0.45709469,0.2185063,0.13687127,0.07915523,0.04190733,0.02357109,0.0148199,0.00953435,0.00662991,0.00465658,0.00327059,0.00232022,0.00166255	}, {	0.42315333,0.20321302,0.13064841,0.08231553,0.05146489,0.03387407,0.02308262,0.01642256,0.01198494,0.00883433,0.00651196,0.00484834,0.003646	} });
			sizeFreqByYear.put(	2005	, new double[][] { {	0.4493904,0.21483818,0.13456564,0.07795737,0.04497794,0.02661838,0.01689119,0.01135629,0.00809383,0.00582659,0.00419445,0.00304985,0.00223988	}, {	0.42508044,0.20412989,0.13113844,0.08198477,0.05055812,0.03290793,0.02246871,0.01611392,0.01182359,0.00876275,0.00649428,0.00486144,0.00367572	} });
			sizeFreqByYear.put(	2006	, new double[][] { {	0.44131327,0.21144808,0.1345595,0.08222147,0.0486123,0.02857741,0.01822468,0.01215179,0.00833552,0.00577522,0.00400133,0.00280016,0.00197927	}, {	0.41289081,0.19846816,0.12839901,0.0834032,0.05457231,0.03718553,0.02590043,0.01862405,0.01358166,0.01000403,0.0073688,0.00548228,0.00411973	} });
			sizeFreqByYear.put(	2007	, new double[][] { {	0.44011319,0.2105748,0.13317658,0.07841349,0.04454349,0.02826561,0.01944959,0.01396078,0.01033077,0.00772145,0.00577118,0.00435686,0.0033222	}, {	0.41271956,0.19827131,0.12779243,0.08231253,0.05373064,0.03657454,0.0257177,0.01874261,0.01411183,0.01073197,0.00816161,0.00626924,0.00486404	} });
			sizeFreqByYear.put(	2008	, new double[][] { {	0.45378415,0.21697834,0.1361894,0.07905601,0.04523027,0.02619154,0.01471926,0.00943066,0.00657035,0.00462357,0.00325361,0.00231259,0.00166025	}, {	0.42522906,0.2042118,0.1312845,0.08185887,0.05030461,0.03288796,0.02271912,0.01636593,0.01186521,0.00868867,0.00636255,0.004706,0.00351573	} });
			sizeFreqByYear.put(	2009	, new double[][] { {	0.45362671,0.21708024,0.13744819,0.08055877,0.04178749,0.02397945,0.01528936,0.01035995,0.00716163,0.00500045,0.00349145,0.00246233,0.001754	}, {	0.42522671,0.20428606,0.13172063,0.08398862,0.05174414,0.03311649,0.02195536,0.01539513,0.01109274,0.00807305,0.00587538,0.00431895,0.00320673	} });


			//This is calculated by applying F+M to a starting value of 1
			sizeFreqByYear.put(	19700	, new double[][] { {	0.589646124, 0.268761016, 0.12018384, 0.019660717, 0.00159444, 0.000137096, 1.45964E-05, 	1.88491E-06, 2.48324E-07, 3.30438E-08, 4.39705E-09, 5.90984E-10, 8.02293E-11}, {0.589646124, 0.268761016, 0.12018384, 0.019660717, 0.00159444, 0.000137096, 1.45964E-05, 	1.88491E-06, 2.48324E-07, 3.30438E-08, 4.39705E-09, 5.90984E-10, 8.02293E-11	} }); //average of 1967-1970
			sizeFreqByYear.put(	19850	, new double[][] { {	0.550346648, 0.25399856, 0.132848728, 0.044884099, 0.012306605, 0.003641356, 0.001228871, 0.000460529, 0.000176073, 6.79945E-05, 2.62575E-05, 1.02418E-05, 4.03498E-06}, { 0.550346648, 0.25399856, 0.132848728, 0.044884099, 0.012306605, 0.003641356, 0.001228871, 0.000460529, 0.000176073, 6.79945E-05, 2.62575E-05, 1.02418E-05, 4.03498E-06 }});
			sizeFreqByYear.put(	20090	, new double[][] { {	0.447592132, 0.214215475, 0.13547436, 0.080132571, 0.045023402, 0.026713856, 0.016821913, 0.011357546, 0.007985939, 0.005671663, 0.00402805, 0.002889497, 0.002093594}, {0.447592132, 0.214215475, 0.13547436, 0.080132571, 0.045023402, 0.026713856, 0.016821913, 0.011357546, 0.007985939, 0.005671663, 0.00402805, 0.002889497, 0.002093594 } }); //average of 2006-2009

			//equal size freq per size class -- useful for estimating fecundity vectors so aren't having small sample size at older ages
			sizeFreqByYear.put(	0, new double[][] { {	0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077 }, {	0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077,0.076923077	} }); //equal size freq


			//virgin SizeFreq -- calculate by applying F+M to starting value of 1
			sizeFreqByYear.put(	1, new double[][] { {0.375172118, 0.180798816, 0.118793287, 0.082879264, 0.060787544, 0.045485105, 0.035071362, 0.027313604, 0.021701576, 0.017415923, 0.013976606, 0.011329216, 0.009275578	 },{	 0.375172118, 0.180798816, 0.118793287, 0.082879264, 0.060787544, 0.045485105, 0.035071362, 0.027313604, 0.021701576, 0.017415923, 0.013976606, 0.011329216, 0.009275578 } }); //virgin size freq -- just applied nat mortality to each age

		}
		return sizeFreqByYear.get(year)[sex];
	}


	@Override
	public synchronized int getPopulationAbundance(int year, int sex) {


		if (fishAbundByYear == null) {
			fishAbundByYear = new HashMap<Integer, int[]>(); 
			//females then males
			//These abundances are based on the age-specific fishing + natural mortlaity rates applied across all age classes, then compared
			//to what would be the total abundance of a virgin population
			fishAbundByYear.put(	1950	, new int[] { (int) Math.round(	0.713414559	* popnAbundance), (int) Math.round(	0.77104031	* popnAbundance) });
			fishAbundByYear.put(	1951	, new int[] { (int) Math.round(	0.710868036	* popnAbundance), (int) Math.round(	0.769166309	* popnAbundance) });
			fishAbundByYear.put(	1952	, new int[] { (int) Math.round(	0.702712069	* popnAbundance), (int) Math.round(	0.76270037	* popnAbundance) });
			fishAbundByYear.put(	1953	, new int[] { (int) Math.round(	0.694384494	* popnAbundance), (int) Math.round(	0.75628407	* popnAbundance) });
			fishAbundByYear.put(	1954	, new int[] { (int) Math.round(	0.711557599	* popnAbundance), (int) Math.round(	0.770145367	* popnAbundance) });
			fishAbundByYear.put(	1955	, new int[] { (int) Math.round(	0.726589524	* popnAbundance), (int) Math.round(	0.78292704	* popnAbundance) });
			fishAbundByYear.put(	1956	, new int[] { (int) Math.round(	0.737394394	* popnAbundance), (int) Math.round(	0.791449172	* popnAbundance) });
			fishAbundByYear.put(	1957	, new int[] { (int) Math.round(	0.725609744	* popnAbundance), (int) Math.round(	0.781539254	* popnAbundance) });
			fishAbundByYear.put(	1958	, new int[] { (int) Math.round(	0.704149965	* popnAbundance), (int) Math.round(	0.764754309	* popnAbundance) });
			fishAbundByYear.put(	1959	, new int[] { (int) Math.round(	0.70218583	* popnAbundance), (int) Math.round(	0.763212551	* popnAbundance) });
			fishAbundByYear.put(	1960	, new int[] { (int) Math.round(	0.69651436	* popnAbundance), (int) Math.round(	0.759012417	* popnAbundance) });
			fishAbundByYear.put(	1961	, new int[] { (int) Math.round(	0.688238394	* popnAbundance), (int) Math.round(	0.753450987	* popnAbundance) });
			fishAbundByYear.put(	1962	, new int[] { (int) Math.round(	0.673357378	* popnAbundance), (int) Math.round(	0.74298061	* popnAbundance) });
			fishAbundByYear.put(	1963	, new int[] { (int) Math.round(	0.680701316	* popnAbundance), (int) Math.round(	0.748890826	* popnAbundance) });
			fishAbundByYear.put(	1964	, new int[] { (int) Math.round(	0.683746454	* popnAbundance), (int) Math.round(	0.749140994	* popnAbundance) });
			fishAbundByYear.put(	1965	, new int[] { (int) Math.round(	0.66468924	* popnAbundance), (int) Math.round(	0.73333376	* popnAbundance) });
			fishAbundByYear.put(	1966	, new int[] { (int) Math.round(	0.647134997	* popnAbundance), (int) Math.round(	0.719354562	* popnAbundance) });
			fishAbundByYear.put(	1967	, new int[] { (int) Math.round(	0.649491628	* popnAbundance), (int) Math.round(	0.722083597	* popnAbundance) });
			fishAbundByYear.put(	1968	, new int[] { (int) Math.round(	0.629340173	* popnAbundance), (int) Math.round(	0.705992004	* popnAbundance) });
			fishAbundByYear.put(	1969	, new int[] { (int) Math.round(	0.646349415	* popnAbundance), (int) Math.round(	0.720244455	* popnAbundance) });
			fishAbundByYear.put(	1970	, new int[] { (int) Math.round(	0.623343949	* popnAbundance), (int) Math.round(	0.704222178	* popnAbundance) });
			fishAbundByYear.put(	1971	, new int[] { (int) Math.round(	0.646335043	* popnAbundance), (int) Math.round(	0.723629135	* popnAbundance) });
			fishAbundByYear.put(	1972	, new int[] { (int) Math.round(	0.65762332	* popnAbundance), (int) Math.round(	0.732428795	* popnAbundance) });
			fishAbundByYear.put(	1973	, new int[] { (int) Math.round(	0.672549519	* popnAbundance), (int) Math.round(	0.742141992	* popnAbundance) });
			fishAbundByYear.put(	1974	, new int[] { (int) Math.round(	0.677103486	* popnAbundance), (int) Math.round(	0.746447764	* popnAbundance) });
			fishAbundByYear.put(	1975	, new int[] { (int) Math.round(	0.692951587	* popnAbundance), (int) Math.round(	0.759340231	* popnAbundance) });
			fishAbundByYear.put(	1976	, new int[] { (int) Math.round(	0.694336086	* popnAbundance), (int) Math.round(	0.760310471	* popnAbundance) });
			fishAbundByYear.put(	1977	, new int[] { (int) Math.round(	0.711864478	* popnAbundance), (int) Math.round(	0.774813676	* popnAbundance) });
			fishAbundByYear.put(	1978	, new int[] { (int) Math.round(	0.723388941	* popnAbundance), (int) Math.round(	0.783252901	* popnAbundance) });
			fishAbundByYear.put(	1979	, new int[] { (int) Math.round(	0.687976968	* popnAbundance), (int) Math.round(	0.759063121	* popnAbundance) });
			fishAbundByYear.put(	1980	, new int[] { (int) Math.round(	0.721600711	* popnAbundance), (int) Math.round(	0.782762144	* popnAbundance) });
			fishAbundByYear.put(	1981	, new int[] { (int) Math.round(	0.697819862	* popnAbundance), (int) Math.round(	0.766193863	* popnAbundance) });
			fishAbundByYear.put(	1982	, new int[] { (int) Math.round(	0.673567896	* popnAbundance), (int) Math.round(	0.746219064	* popnAbundance) });
			fishAbundByYear.put(	1983	, new int[] { (int) Math.round(	0.670569141	* popnAbundance), (int) Math.round(	0.74498967	* popnAbundance) });
			fishAbundByYear.put(	1984	, new int[] { (int) Math.round(	0.675837966	* popnAbundance), (int) Math.round(	0.74800737	* popnAbundance) });
			fishAbundByYear.put(	1985	, new int[] { (int) Math.round(	0.719154748	* popnAbundance), (int) Math.round(	0.77947143	* popnAbundance) });
			fishAbundByYear.put(	1986	, new int[] { (int) Math.round(	0.690260496	* popnAbundance), (int) Math.round(	0.753421326	* popnAbundance) });
			fishAbundByYear.put(	1987	, new int[] { (int) Math.round(	0.73136957	* popnAbundance), (int) Math.round(	0.79370293	* popnAbundance) });
			fishAbundByYear.put(	1988	, new int[] { (int) Math.round(	0.710843623	* popnAbundance), (int) Math.round(	0.781052464	* popnAbundance) });
			fishAbundByYear.put(	1989	, new int[] { (int) Math.round(	0.731068916	* popnAbundance), (int) Math.round(	0.797933422	* popnAbundance) });
			fishAbundByYear.put(	1990	, new int[] { (int) Math.round(	0.817696902	* popnAbundance), (int) Math.round(	0.865080937	* popnAbundance) });
			fishAbundByYear.put(	1991	, new int[] { (int) Math.round(	0.794925978	* popnAbundance), (int) Math.round(	0.852656679	* popnAbundance) });
			fishAbundByYear.put(	1992	, new int[] { (int) Math.round(	0.797627344	* popnAbundance), (int) Math.round(	0.846338108	* popnAbundance) });
			fishAbundByYear.put(	1993	, new int[] { (int) Math.round(	0.810970197	* popnAbundance), (int) Math.round(	0.859245785	* popnAbundance) });
			fishAbundByYear.put(	1994	, new int[] { (int) Math.round(	0.826286498	* popnAbundance), (int) Math.round(	0.878157289	* popnAbundance) });
			fishAbundByYear.put(	1995	, new int[] { (int) Math.round(	0.849553832	* popnAbundance), (int) Math.round(	0.880233307	* popnAbundance) });
			fishAbundByYear.put(	1996	, new int[] { (int) Math.round(	0.880284225	* popnAbundance), (int) Math.round(	0.902763279	* popnAbundance) });
			fishAbundByYear.put(	1997	, new int[] { (int) Math.round(	0.843840133	* popnAbundance), (int) Math.round(	0.877048607	* popnAbundance) });
			fishAbundByYear.put(	1998	, new int[] { (int) Math.round(	0.826346125	* popnAbundance), (int) Math.round(	0.872096672	* popnAbundance) });
			fishAbundByYear.put(	1999	, new int[] { (int) Math.round(	0.860552613	* popnAbundance), (int) Math.round(	0.896306484	* popnAbundance) });
			fishAbundByYear.put(	2000	, new int[] { (int) Math.round(	0.865681053	* popnAbundance), (int) Math.round(	0.89676851	* popnAbundance) });
			fishAbundByYear.put(	2001	, new int[] { (int) Math.round(	0.87580651	* popnAbundance), (int) Math.round(	0.92407801	* popnAbundance) });
			fishAbundByYear.put(	2002	, new int[] { (int) Math.round(	0.845840039	* popnAbundance), (int) Math.round(	0.904552792	* popnAbundance) });
			fishAbundByYear.put(	2003	, new int[] { (int) Math.round(	0.830328629	* popnAbundance), (int) Math.round(	0.896320378	* popnAbundance) });
			fishAbundByYear.put(	2004	, new int[] { (int) Math.round(	0.820775498	* popnAbundance), (int) Math.round(	0.886610333	* popnAbundance) });
			fishAbundByYear.put(	2005	, new int[] { (int) Math.round(	0.834846748	* popnAbundance), (int) Math.round(	0.88259087	* popnAbundance) });
			fishAbundByYear.put(	2006	, new int[] { (int) Math.round(	0.850126522	* popnAbundance), (int) Math.round(	0.908647303	* popnAbundance) });
			fishAbundByYear.put(	2007	, new int[] { (int) Math.round(	0.852444607	* popnAbundance), (int) Math.round(	0.909024317	* popnAbundance) });
			fishAbundByYear.put(	2008	, new int[] { (int) Math.round(	0.826763372	* popnAbundance), (int) Math.round(	0.8822824	* popnAbundance) });
			fishAbundByYear.put(	2009	, new int[] { (int) Math.round(	0.827050321	* popnAbundance), (int) Math.round(	0.882287272	* popnAbundance) });


			//These values are calculated by applying year specific F+M to each age class and then summing total  
			fishAbundByYear.put(	19700	, new int[] {	(int) Math.round(0.636266571 * popnAbundance)	,	(int) Math.round(0.636266571 * popnAbundance)	}); //avg of 1967-1970
			fishAbundByYear.put(	19850	, new int[] {	(int) Math.round(0.681701468 * popnAbundance),	(int) Math.round(0.681701468 * popnAbundance)	});
			fishAbundByYear.put(	20090	, new int[] {	(int) Math.round(0.838200877 * popnAbundance),	(int) Math.round(0.838200877 * popnAbundance)	}); //avg of 2006-2009
			fishAbundByYear.put(	0 , new int[] {	popnAbundance,	popnAbundance }); 

		}
		return fishAbundByYear.get(year)[sex];
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
