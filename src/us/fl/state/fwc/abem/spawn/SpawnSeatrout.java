package us.fl.state.fwc.abem.spawn;

import java.util.ArrayList;
import java.util.Calendar;

import javolution.util.FastMap;
import javolution.util.FastTable;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.organism.OrgMemory;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Binomial;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnSeatrout {

	protected MersenneTwister m; 
	protected Uniform uniform;
	protected Normal normal; 

	SeatroutParams params; 
	SpawnModelNonSpatial model; 

	//public Calendar birthdate;
	public boolean isDead = false;
	public long birthday;
	public int yearClass;
	public int groupAbundance = 1;
	public int groupSex;
	public double groupBiomass;
	public double nominalBiomass;
	public double ageInDays; 
	public boolean mature = false;
	public  double sizeAtMaturity=Double.MAX_VALUE; 
	//public  long lastSpawn=0; // in seconds
	public  long nextSpawnTime; 
	//public  int numSpawnsInSeason=0; 

	public long currentTime; 

	public double relCond; //relative condition
	public double fulCond; //fulton's condition

	//protected OrgMemory mem; 

	long timeStep = 1*TimeUtils.SECS_PER_DAY;


	public double sumTEP;
	public double newNumSpawned ;
	public  int numSpawns ; 

	public double lastSpawnTime;
	public boolean markerOn = false;
	private final double markerTime = 2.583;

	public double capableProb=0;
	public double activeProb =0;
	public double TL; 
	public double Linf; //use this if use variable length

	//holds the first and last day of spawn for the year 
	public int firstDayOfSpawn = -99;
	public int lastDayOfSpawn = -99;

	//sum of the days between spawns -- can then devide by (numSpawns-1) to get 
	//average spawn interval over hte course of the year
	public double spawnIntervalSum = 0;


	public void initialize(SpawnModelNonSpatial model) {
		this.params = model.params;
		this.model = model;
		m = model.m;  
		uniform = model.uniform; 
		normal = model.normal;

		sumTEP = 0;
		newNumSpawned = 0;
		numSpawns = 0; 
		isDead = false;
		groupAbundance = 1;

		lastSpawnTime =0;
		markerOn = false;
		mature = false;

		firstDayOfSpawn = -99;
		lastDayOfSpawn = -99;

		spawnIntervalSum=0;
	}


	/**
	 * Set the current time for the model and the age in days for the fish.
	 * 
	 * @param currentDate
	 */
	public void setCurrentTime(Calendar currentDate){
		currentTime = currentDate.getTimeInMillis();
		ageInDays=getAgeInDays();
		if (ageInDays<0) ageInDays=0;
	}








	/**
	 * Determine if should spawn during this given time step
	 * @param currentDate
	 * @return
	 */
	public boolean timeToSpawn(Calendar currentDate){

		//reset actively spawning indicator marker if needed
		if (currentTime-lastSpawnTime > markerTime*TimeUtils.MILLISECS_PER_DAY)
			markerOn = false;



		int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR);
		int dayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH);
		double month = currentDate.get(Calendar.MONTH) + 1;
		int maxDays = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH);

		//convert month decimal with days of month
		month += (double) (dayOfMonth-1) /(double) maxDays;
		//get current lenght of individual

		//convert to mm so input to GAM
		//TL = params.getLengthAtAge(ageInDays/365.25, groupSex)*10.0;
		double TL_mm = TL*10.0;
		//########################################
		//check and set maturity 
		//########################################


		//if not mature, then check if it grew enough to be mature
		if (!mature)
			if (TL_mm > sizeAtMaturity*10.0) mature = true;


		//only possible to spawn if between start and end of season
		if (dayOfYear >= params.getStartOfBreeding() 
				&& dayOfYear <= params.getEndOfBreeding() 
				&& mature){


			//if using tte 1-stage GAM, then get appropriate probs
			if (model.gam1Stage != null) {
				capableProb = 1; //always 1 since activeProb has all of effects
				activeProb = model.gam1Stage.getProbOfSpawn(month, TL_mm, 0);
			}

			else {
				//use 'average' zone, which is average of all partial effects of zones
				capableProb = model.gam.getCapableSpawnProb(month, TL_mm, 0);
				activeProb = model.gam.getActiveSpawnProb(month, TL_mm, 0);
			}

			//get instantaneous rate for active spawners, since they spawning is an event, but the
			//detection of the event occurs over a few days period
			//moron, this formulation is specific to mortality, not a general conversion to an instananeous rate!
			//			double instCapableProb= Math.log(-1/(capableProb-1));
			//			double instActiveProb= Math.log(-1/(activeProb-1));

			//compute prob per time step (day) using the time units of the 
			//spawning indicator (i.e., spawning indicator was -14hr -> +2 days)
			//note: don't do for capable prob, since they're capable for a long period of time
			//and it's assummed that a change in their capability would be detected within a day

			//don't adjust for spawning marker time -- sampling regimes eliminates this issue
			//activeProb /= markerTime;

			//multiple this by the prob mult -- default is 1 unless set differently
			activeProb *= model.probMult;

			if ( uniform.nextDoubleFromTo(0, 1)  < capableProb*activeProb) {
				//lastSpawn = dayOfYear; 
				spawn();
				numSpawns++;
				lastSpawnTime = currentTime;
				markerOn = true;

				//set the first and last day of spawn, used to get individual variability in season length
				if (firstDayOfSpawn == -99) firstDayOfSpawn = dayOfYear;

				else spawnIntervalSum += dayOfYear-lastDayOfSpawn;

				lastDayOfSpawn = dayOfYear;

				return true; 
			}



		}//end check if in breeding season

		return false;
	}




	private void spawn() {
		//set TEP -- not including fertilization or scalers
		long numLarvae = (long) (groupAbundance
				*(																								
						params.getBaseFecundity((int) (ageInDays/365.25), 
								groupBiomass/(double)groupAbundance)		
						) 
				);  


		//can be negative using Sue's function if < ~19cm TL, and sometimes fish this small
		//will spawn based on probability

		if (numLarvae < 0) numLarvae = 0;

		newNumSpawned = numLarvae;
		sumTEP += numLarvae;
	}





	/**
	 * Determines if a fish lives or dies during this time step, using simple nat mortality
	 * as a function of age, and fishing mortality based on estimate from 2010 SA.
	 * 
	 * @return
	 */
	public boolean suffersMortality(){

		//if past oldest age, then remove -- add an extra year so goes to end of oldest age
		if  (ageInDays >= (params.getAgeMax()+1)*365.25 ) 
			return true;


		//need to get avgLength so can reset the biomass after mortality below
		double avgWeight = groupBiomass/(double)groupAbundance;
		double avgNominalBiomass = nominalBiomass/(double)groupAbundance;

		double Z = 0;
		if (model.runner.useSizeDepMortality) {
			
			//expected age is the expected age at length (using lookup table since no solution
			//to Porch et al. 2002 exists solving for age) 
			double expectedAge = params.getExpectedAge(TL, groupSex);

			//get natural mortality
			Z = params.getSizeDepNatMort(TL, expectedAge, groupSex);

			//if there's fishing mortality
			if (model.fishMortYear != 0) 
				Z +=params.getSizeDepFishMort(model.fishMortYear, TL, expectedAge, groupSex);
		}

		//else, if based on year class
		else {
			Z = params.getNatInstMort(yearClass, TL, groupSex);
			if (model.fishMortYear != 0) 
					Z += params.getFishInstMort(model.fishMortYear, yearClass, groupSex);
		}

		double mort = (1- Math.exp(-(Z * (timeStep/(double) TimeUtils.SECS_PER_YEAR)))); 


		// NOTE: do not remove and recycle agents in this step: do from either step() or processRates() directly so will break out of step() method 
		int numToDie=0;
		for (int i=0; i<groupAbundance; i++){
			//testing:
			if ( uniform.nextDoubleFromTo(0, 1) < mort ) {
				numToDie++; 
			}
		}

		groupAbundance -= numToDie; 

		//reset the groupBiomass
		groupBiomass -= numToDie*avgWeight; 
		if (TL < 0) nominalBiomass = 0.0; 
		else nominalBiomass -= numToDie*avgNominalBiomass; 


		if (groupAbundance <= 0) {
			isDead = true;
			return true;
		}
		else return false;

	}





	/**
	 * Grows a fish each time step, where new growth is the expected amount of new growth
	 * for an average weighted fish of the fish's size.  This assumes that amount grown 
	 * each time step is a function of size rather than weight 
	 */
	public void growth(){

		double timeStep = 1; 


		/*if use the DDGrowth formuatlion, then the length is variable
		 * depending on biomass and resource availability, 
		 * and the weight is set to average length since so not variable
		 */
		if (model.runner.useResourceDepGrowth){

			//get new growth to add as the expected size at the current age, minus the 
			//expected size at their last age based on current resources/density;
			// as such, will grow based on how big they should be at their age, conditional
			// on the environmental resources and density dependence
			double newGrowth = params.getDDLengthAtAge((ageInDays)/365.25, groupSex, 
					model.resource, model.biomassPerArea) 
					- params.getDDLengthAtAge((ageInDays-1)/365.25, groupSex, 
							model.resource, model.biomassPerArea);

			//if the growth is less than zero, set equal to zero since can't get smaller in length;
			//won't shrink in length
			if (newGrowth < 0) newGrowth = 0.0;

			//add in the new growth
			TL += newGrowth;

			//set these the same since not varying

			//TODO -- could get fancy here, where group and nominal depart based on expected
			//growth, but would need to create a bounding logistic mechanism so can't get too fat, 
			//or if get too skinny then die
			//in this situation, basing the GAM on weight versus length would be beneficial

			groupBiomass = params.getMassAtLength(TL, groupSex) * groupAbundance;
			nominalBiomass = params.getMassAtLength(TL, groupSex) * groupAbundance;
		}

		
		//if following assessment growth rates
		else if (model.runner.useAssessmentGrowth){
			TL = params.getLengthAtAge_Assess(Linf, (ageInDays)/365.25, groupSex);
			nominalBiomass = params.getMassAtLength_Assess(TL, groupSex)* groupAbundance;  
			groupBiomass = nominalBiomass;

			if (TL < 0) {
				TL =0;
				nominalBiomass = 0.0;
				groupBiomass = 0.0;
			}
		}

		
		/*Else the length is set to average, and the weight can be variable
		 * This should be the same as from before, but changed around so TL is set 
		 * first, and everything else is then based upon that
		 */
		else {

			//new TL, where Linf can be variable (assigned at initialization
			TL = params.getLengthAtAge(Linf, (ageInDays)/365.25, groupSex);
			nominalBiomass = params.getMassAtLength(TL, groupSex) * groupAbundance;
			groupBiomass = nominalBiomass;
 
			//if the von bert is making it smaller than 0, then need to set everything to 0.0
			if (TL < 0) {
				TL =0;
				nominalBiomass = 0.0;
				groupBiomass = 0.0;
			}
			
			//old way
			/*
			double avgGrowthForAge = 0;
			double lastTL =TL;

			else {
				//nominal biomass per individual at next time step
				nominalBiomass = params.getMassAtLength(TL, groupSex) ; 

				//only calculate if the lengthTemp > 0, else will have some NaN's percolating through
				if (TL > 0) 

					// Get's the avg growth in mass for a normal condition fish
					avgGrowthForAge = nominalBiomass
					-  params.getMassAtLength(lastTL, groupSex);

				// Multiply it by the environmental habitat quality for growth -- this will control condition
				//avgGrowthForAge *= normal.nextDouble(1, .01);  

				//reset the biomass so that reflects the groupAbundance
				groupBiomass += avgGrowthForAge*groupAbundance;
				nominalBiomass *= groupAbundance; 

			}
			*/
		}



		//reset the new condition

		//observed weight divided expected weight (i.e., relative condition) 
		if (groupBiomass > 0) {
			relCond = groupBiomass / nominalBiomass; 

			//observed weight divided by TL^3 as measure of expected weight (Fulton's condition factor)
			fulCond = 100000.0*( (groupBiomass/groupAbundance)
					/(Math.pow(params.getLengthAtAge(Linf, (ageInDays+timeStep)/365.25, groupSex), 3)));
		}


	}


	public double getAgeInDays(){
		return (double) (this.currentTime-this.birthday)/(1000*60*60*24);
	}


}
