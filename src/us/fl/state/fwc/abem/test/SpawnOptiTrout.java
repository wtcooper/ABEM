package us.fl.state.fwc.abem.test;

import java.util.Calendar;

import javolution.util.FastMap;
import javolution.util.FastTable;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnOptiTrout {

	private final int seed = (int) System.currentTimeMillis();
	private MersenneTwister64 m; 

	int moonPhase; 
	boolean mature = false; // false = immature, true=mature
	double sizeMaturity; 
	boolean sex; // false = male, true = female
	double age; 
	int spawningCounter=0; //if spawn, does a 2 day count -- one day for prepping, and 1 day post-spawn, to record individuals as the "spawning" category  
	double size; 
	int lastSpawn=0; 
	double biomass;
	double nominalBiomass; 
	double groupSize; 
	//Uniform uniform; 
	Normal normal; 

	boolean spawnThisStep; 

	int peakLunarSpawnTime;
	double lunarSpecificity; 
	int fallowPeriod; 
	double fallowPeriodSD; 

	int peaks[];
	double peaksSD[] = new double[2]; 
	double peakSize[]; // relative 

	double[] fallowPeriods = {12, 11, 10, 9, 8, 7.5, 7.25, 7, 6.8, 6.65, 6.55, 6.5, 6.475, 6.5};
	SeatroutParams params; // = new SeatroutParams(); 


	public SpawnOptiTrout(Scheduler sched, SeatroutParams params) {
		this.params = params;
		//this.uniform = new Uniform(m);
		params.initialize(sched);
		this.m = sched.getM(); 

	}

	public void setParameters(int[] peaks, double[]peaksSD, double[] peakSize){
		this.peaks = peaks; 
		this.peaksSD[0] = peaksSD[0];
		this.peaksSD[1] = peaksSD[1];

		this.peakSize=peakSize;
	}

	
	
	public boolean timeToSpawn(Calendar currentDate){
		 Uniform uniform = new Uniform(m); //need to set here because Uniform is not threadsafe -- will throw an error if running multiple threads over same object 
		 
		if (spawningCounter > 0) spawningCounter--; 
			
		int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR);

		double daysInRunTime = 1; //runTime/TimeUtils.secPerDay; //set the total number of days this animal will be stepping for 
		double probScaler = 1; 
		if (daysInRunTime < 1) probScaler = daysInRunTime; // do this so that can scale the probability if less than a day so that if taking short time steps (e.g., seconds/minutes if evading predator or hunting) then won't spawn each time

		// if proper age to spawn
		if ( (size-sizeMaturity) > 0  ) {
			// if not low weight from previous spawning or other growth issue
			if ( (biomass-0.9*nominalBiomass) > 0	) {
				// if not fallow from spawning recently
				//double fallow = normal.nextDouble(fallowPeriod, fallowPeriodSD); 
				//if ( ((dayOfYear-lastSpawn) > fallow) || (dayOfYear<fallow) ) {
				//should roughly be spawning every 14 days, according to Sue's telemetry data
				if ( (dayOfYear-lastSpawn) > fallowPeriods[(int) age]) {

					//betaDist.setState(alpha, beta);

					// if in spawning season; here, spawning season is calculated as probability with normal curve-like shape,  
					double seasonalProbOfSpawn = 0; 

					FastMap<Integer, FastTable<Normal>> seasonNormals = new FastMap<Integer, FastTable<Normal>> (); 
						FastTable<Normal> seasonNormal = new FastTable<Normal>();  
						for (int j=0; j<peaks.length; j++){
							seasonNormal.add(new Normal(peaks[j], peaksSD[j], m));
						}
						seasonNormals.put(0, seasonNormal); 
					
					
					//multi-model distribution, where peaks and stdev of peaks are parameterized
					for (int i=0; i<peaks.length; i++) {
						double probtmp =((seasonNormals.get(0).get(i).pdf(dayOfYear)*peakSize[i])/seasonNormals.get(0).get(i).pdf(peaks[i]));  
						if (probtmp>1) probtmp=1; 
						if (i == 0) seasonalProbOfSpawn = probtmp;
						else if ( probtmp > seasonalProbOfSpawn) seasonalProbOfSpawn = probtmp; 
					}

					if ( uniform.nextDoubleFromTo(0, 1) < seasonalProbOfSpawn  ) {

						double lunarProb = 0; 
						int lunarPeaks[] = {0, 14}; // don't change this -- this needs to stay as 0 and 14;  
						double lunarPeaksSD[] = {5.071560681, 5.071560681}; 
						double lunarPeakSize[] = {1, 1}; // relative 
						int peakShift = 2; 
						
						FastTable<Normal> lunarNormals = new FastTable<Normal>();  
						for (int i=0; i<lunarPeaks.length; i++){
							lunarNormals.add(new Normal(lunarPeaks[i], lunarPeaksSD[i], m));
						}

						//		currentDate.add(Calendar.DAY_OF_YEAR, 0); 

						int lunarPhase = TimeUtils.getMoonPhase(currentDate);
						int lunarAdjust = lunarPhase - peakShift;
						if (lunarAdjust < 0) lunarAdjust = 28+lunarAdjust; 
						if (lunarAdjust > 14) lunarAdjust =28-lunarAdjust; 
						

						FastTable<Double> lunarProbs = FastTable.newInstance();

						for (int i=0; i<lunarPeaks.length; i++) {
							lunarProbs.add((lunarNormals.get(i).pdf(lunarAdjust)/lunarNormals.get(i).pdf(lunarPeaks[i]))*lunarPeakSize[i] );
							if (i == 0) lunarProb = lunarProbs.get(i);
							else if ( lunarProbs.get(i) > lunarProb) lunarProb = lunarProbs.get(i); 
						}

							FastTable.recycle(lunarProbs); 

						if ( uniform.nextDoubleFromTo(0, 1)  < lunarProb  ) {
							// **SPAWN**
							spawningCounter = 2; // set to 2 days to signify fact that when trout are sampled, are recorded as "spawning" if within 2 days of actual spawn
							lastSpawn = dayOfYear; 
							moonPhase=TimeUtils.getMoonPhase_8(currentDate); 
							if (moonPhase==0 || moonPhase==1) moonPhase=0; //new
							else if (moonPhase==2 || moonPhase==3) moonPhase=1; //1st quarter
							else if (moonPhase==4 || moonPhase==5) moonPhase=2; //full
							else if (moonPhase==6 || moonPhase==7) moonPhase=3; //3rd quarter
						}


					} // end of season check
				} // end of fallow period check
			} // end of age Maturity check
		} // end of mass check

		if (spawningCounter > 0) return true;
		else return false;
		
	}


/*	public boolean checkMaturity(long timeStep){
		
		if (mature) return true; 
		else if ( uniform.nextDoubleFromTo(0, 1) < (timeStep*Math.pow((params.getMaturityProb(params.getLength(biomass))), (1/(365.25*60*60*24)))) ) {
			mature = true; 
			return true; 
		}
		else return false; 
	}
*/


	public void setSex(boolean sex) {
		this.sex = sex;
	}
	public void setAge(double age) {
		this.age = age;
	}
	public double getAge(){
		return age; 
	}
	
	public void setSize(double size) {
		this.size = size;
	}
	public void setBiomass(double biomass) {
		this.biomass = biomass;
	}

	public void setNominalBiomass(double nominalBiomass) {
		this.nominalBiomass = nominalBiomass;
	}

	public int getLastSpawn() {
		return lastSpawn;
	}
	public void setLastSpawn(int lastSpawn) {
		this.lastSpawn = lastSpawn;
	}

	public int getMoonPhase() {
		return moonPhase;
	}

	public void setSizeMaturity(double sizeMaturity) {
		this.sizeMaturity = sizeMaturity;
	}

	public double getSize() {
		return size;
	}



}
