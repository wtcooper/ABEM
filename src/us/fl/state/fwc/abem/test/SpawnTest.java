package us.fl.state.fwc.abem.test;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javolution.util.FastTable;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnTest {

	double groupBiomass=1000; // refers to total biomass of Animal (i.e., if individual or group) 
	int groupAbundance =1; 
	protected double groupAge=5;

	protected DecimalFormat df = new DecimalFormat("#.####"); 
	protected DecimalFormat df2 = new DecimalFormat("#"); 

	long seed = System.currentTimeMillis(); 
	MersenneTwister64 m; // = new MersenneTwister64((int) seed); 
	Uniform uniform; // = new Uniform(m); 
	Normal normal; // = new Normal(0,1,m); 
	Binomial bernoulli; // = new Binomial(1,0.5,m);
	double alpha=2, beta=10; 

	Beta betaDist = new Beta(2, 10, m); 


	long secsPerDay = 60*60*24;
	double biomass = 5;
	double nominalBiomass = 5; 
	double age = 5; // in year
	double ageMaturity = 2; 
	double spawningPeriod = 30; // total amount of time to attain readiness and go through fallow period 

	long runTime = secsPerDay; //secsPerDay*30; // 1 year in seconds 
	long currentTime = 0*secsPerDay + secsPerDay*365; 
	Calendar currentDate; 
	long stepTime = secsPerDay; 

	double fallowPeriod = TimeUtils.SECS_PER_DAY*14; 
	double fallowPeriodSD = 2; 
	long lastSpawn=0; 
	double spawnOffset = 0;
	double startOfBreeding = 59; 
	double endOfBreeding = 272; 
	int peaks[] = {129, 221};
	double peaksSD[] = {25, 20}; 
	double peakSize[] = {0.6, 1}; // relative 

	double lunarSpecificity = 10; // 1 = only spawn on lunar cycle, while 0 = no lunar specificity
	int peakLunarSpawnTime = 0; // 0 = full moon; 4 = new moon
	boolean spawnThisStep = false; 
	long nextSpawnTime = 0; 

	Scheduler schedule = new Scheduler(); 

	SeatroutParams params = new SeatroutParams(); 

	boolean isMature = false; 

	public static void main(String[] args) {

		SpawnTest mt = new SpawnTest();

		mt.step(); 
	}


	public void step(){
		params.initialize(schedule);
		m = schedule.getM();
		uniform = schedule.getUniform();
		normal = schedule.getNormal();
		bernoulli = schedule.getBernoulli(); 
		
		currentDate = new GregorianCalendar(2009, 0, 1);
		currentDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

		System.out.println(); 
		//		System.out.println("current time: " + currentTime + "\tcurrent date: " + TimeUtils.getDate(currentTime, schedule).getTime());
		//	System.out.println("ending run date: " + GeneralUtils.getDate(currentTime+runTime, scheduler).getTime());
		//		System.out.println("start of breeding season: " + TimeUtils.getDate((long)startOfBreeding*secsPerDay, schedule).getTime());
		//		System.out.println("end of breeding season: " + TimeUtils.getDate((long)(endOfBreeding*secsPerDay), schedule).getTime());
		//		System.out.println(); 

		for (int i = 0; i<365; i ++) {


			//System.out.println("time step: " + i); 
			System.out.println("current time: " + currentTime + "\tcurrent date: " + currentDate.getTime() + "\t" + "current day of year: " + i);
			System.out.println("\tcurrent lunar phase: " + TimeUtils.getMoonPhase(currentDate)); 

			/* NOTE: this is not done as in Gray et al. 2006 -- their formulation using a sine wave is difficult to interpret and when coded as interpreted in the document,
			 * it led to strange results not reflective of what an animal would actually do.  May have mis-interpreted, description 
			 * 
			 */

			if (i == 150){
				System.out.println();
			}
			if (timeToSpawn()) {
				System.out.println("*****SPAWN!*****");
				//lastSpawn = currentTime; // here, make sure the currentTime is already set

			}

			System.out.println(); 
			currentTime+=secsPerDay;
			currentDate.add(Calendar.DAY_OF_YEAR, 1); 

		} // END OF IF STATEMENT TO DO LOOP OVER DAYS

	}




	public boolean timeToSpawn(){

		int yearClass =(int) Math.round(age); 

		int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR);

		double daysInRunTime = ((double) runTime)/((double) TimeUtils.SECS_PER_DAY); //runTime/TimeUtils.secPerDay; //set the total number of days this animal will be stepping for 
		double probScaler = 1; 
		if (daysInRunTime < 1) probScaler = daysInRunTime; // do this so that can scale the probability if less than a day so that if taking short time steps (e.g., seconds/minutes if evading predator or hunting) then won't spawn each time

		// if proper age to spawn
		if ( ((params.getLengthAtMass(groupBiomass/groupAbundance, 0)) - params.getSizeAtMaturityAvg()) > 0  ) { // average length of fish in a group is less than the size at maturity for this group, then proceed
			// if not low weight from previous spawning or other growth issue
			if ( (groupBiomass-0.9*nominalBiomass) > 0	) {

				//should roughly be spawning every 14 days, according to Sue's telemetry data
				if ( (dayOfYear-lastSpawn) > 10) {

					// if in spawning season; here, spawning season is calculated as probability with normal curve-like shape,  
					double seasonalProbOfSpawn = 0; 

					//multi-model distribution, where peaks and stdev of peaks are parameterized
					for (int i=0; i<params.getSeasonPeakSizes(yearClass).size(); i++) {
						double pdf = params.getSeasonNormals(yearClass).get(i).pdf(dayOfYear);
						double peakSize = params.getSeasonPeakSizes(yearClass).get(i);
						double pdf2 = params.getSeasonNormals(yearClass).get(i).pdf(params.getSeasonPeaks(yearClass).get(i));
						double probtmp =(params.getSeasonNormals(yearClass).get(i).pdf(dayOfYear)*params.getSeasonPeakSizes(yearClass).get(i) ) /params.getSeasonNormals(yearClass).get(i).pdf(params.getSeasonPeaks(yearClass).get(i) );  
						if (probtmp>1) probtmp=1; 
						if (i == 0) seasonalProbOfSpawn = probtmp;
						else if ( probtmp > seasonalProbOfSpawn) seasonalProbOfSpawn = probtmp; 
					}

					if ( uniform.nextDoubleFromTo(0, 1) < seasonalProbOfSpawn  ) { // if in the season to spawn, then check if it's the right lunar period; here lunar period check serves as additional probability decrease

						double lunarProb = 0; 
						//int lunarPeaks[] = {0, 14}; // don't change this -- this needs to stay as 0 and 14;  
						//double lunarPeaksSD[] = {5.071560681, 5.071560681}; 
						//double lunarPeakSize[] = {1, 1}; // relative 
						//int peakShift = 2; 

						int lunarPhase = TimeUtils.getMoonPhase(schedule.getCurrentDate());
						int lunarAdjust = lunarPhase - params.getLunarPeakShift(yearClass);
						if (lunarAdjust < 0) lunarAdjust = 28+lunarAdjust; 
						if (lunarAdjust > 14) lunarAdjust =28-lunarAdjust; 

						FastTable<Double> lunarProbs = FastTable.newInstance();

						for (int i=0; i<params.getLunarPeakSizes(i).size(); i++) {
							lunarProbs.add((params.getLunarNormals(yearClass).get(i).pdf(lunarAdjust)/params.getLunarNormals(yearClass).get(i).pdf(params.getLunarPeaks(yearClass).get(i)))*params.getLunarPeakSizes(yearClass).get(i) );
							if (i == 0) lunarProb = lunarProbs.get(i);
							else if ( lunarProbs.get(i) > lunarProb) lunarProb = lunarProbs.get(i); 
						}

						FastTable.recycle(lunarProbs); 

						if ( uniform.nextDoubleFromTo(0, 1)  < lunarProb  ) {
							lastSpawn = dayOfYear; 
							return true; 
						}


					} // end of season check
				} // end of fallow period check
			} // end of age Maturity check
		} // end of mass check

		return false;
	}




	public boolean timeToSpawnOld(){



		spawnThisStep = false; // set here to false as default
		double daysInRunTime = runTime/TimeUtils.SECS_PER_DAY; //set the total number of days this animal will be stepping for 
		double probScaler = 1; 
		if (daysInRunTime < 1) probScaler = daysInRunTime; // do this so that can scale the probability if less than a day so that if taking short time steps (e.g., seconds/minutes if evading predator or hunting) then won't spawn each time

		// if proper age to spawn
		if ( checkMaturity() ) {
			// if not low weight from previous spawning or other growth issue
			if ( (biomass-0.9*nominalBiomass) > 0	) {
				// if not fallow from spawning recently
				double fallow = normal.nextDouble(fallowPeriod, fallowPeriodSD); 
				if ( ((currentTime-lastSpawn) > fallow) || (currentTime<fallow) ) {

					//betaDist.setState(alpha, beta);

					// if in spawning season; here, spawning season is calculated as probability with normal curve-like shape,  
					int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR);
					double seasonalProbOfSpawn = 0; 

					//multi-model distribution, where peaks and stdev of peaks are parameterized
					for (int i = 0; i<peaks.length; i++){
						normal.setState(peaks[i], peaksSD[i]	);
						double scaleFactor = 1/normal.pdf(peaks[i]); 
						seasonalProbOfSpawn += normal.pdf(dayOfYear)*scaleFactor*peakSize[i];
					}

					double randSeasonProb = uniform.nextDoubleFromTo(0, 1); 
					System.out.println("\tseasonal probOfSpawing: " + seasonalProbOfSpawn + ",  seasonal randProb: " + randSeasonProb); 
					if ( randSeasonProb < seasonalProbOfSpawn  ) {

						normal.setState(15, lunarSpecificity); 

						System.out.println("\tIn breeding season."); 


						long numDaysInSeconds; 
						boolean dayFound = false; 
						int i = 1; 
						int lunarPhase=0; 

						while (!dayFound && (i<=daysInRunTime) ) {
							numDaysInSeconds = i*TimeUtils.SECS_PER_DAY; 
							lunarPhase = TimeUtils.getMoonPhase(currentDate);

							int adjustedPhase = lunarPhase; 
							int j = adjustedPhase ; 

							// if peak day isn't equal to 15, then need to scale the peak day to equal 15
							if (peakLunarSpawnTime != 15) {
								int dayDiff = 15 - peakLunarSpawnTime; 
								adjustedPhase = lunarPhase+dayDiff; 
								if (adjustedPhase < 0) adjustedPhase = 30+adjustedPhase; 
								if (adjustedPhase> 15) j = (adjustedPhase-(adjustedPhase-15)*2);
								else j = adjustedPhase;  
								if (j < 0) j=-j; 
							}

							double probOfSpawning =probScaler*(normal.pdf(j)/normal.pdf(15)) ; 
							double randProb =uniform.nextDoubleFromTo(0, 1) ;  
							System.out.println("\tprobOfSpawing: " + probOfSpawning + ",  randProb: " + randProb); 

							if ( randProb < (normal.pdf(j)/normal.pdf(15)) ) {
								spawnThisStep = true; 
								dayFound= true; 
								System.out.println("****SPAWN THIS STEP****"); 
							}

							i++; 

						} // end of while loop


					} // end of season check
					else System.out.println("\tNot in breeding season."); 
				} // end of fallow period check
				else System.out.println("\t****Fallow Period****"); 
			} // end of age Maturity check
		} // end of mass check

		return spawnThisStep; 
	}


	public boolean checkMaturity(){
		if (isMature) return true; 
		//else if ( uniform.nextDoubleFromTo(0, 1) < params.getMaturityProb(params.getLength(groupBiomass/groupAbundance))) {
		isMature = true; 
		return true; 
		//		}
		//	else return false; 
	}


	public boolean timeToSpawnOldVersion(){
		spawnThisStep = false; // set here to false as default

		// if a previous request to spawn at a set time wasn't already performed (i.e., still in the future), then check if appropriate time to spawn based on spawn season and lunar periodicity 
		if (currentTime > nextSpawnTime) {
			// if not low weight from previous spawning or other growth issue
			if ( (biomass-0.9*nominalBiomass) > 0	) {
				// if proper age
				if ( (age-ageMaturity) > 0 ) {
					// if not fallow from spawning recently
					if ( ((currentTime-lastSpawn) > fallowPeriod) || (currentTime<fallowPeriod) ) {

						// if in spawning season; here, spawning season is calculated as probability with normal curve-like shape,  
						int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR);
						double seasonalProbOfSpawn = 0; 
						if ( (dayOfYear>=startOfBreeding) && dayOfYear<=endOfBreeding ) {
							seasonalProbOfSpawn = (dayOfYear-startOfBreeding)/( ((endOfBreeding+startOfBreeding)/2)-startOfBreeding);
							if (seasonalProbOfSpawn>=1) seasonalProbOfSpawn= 1-(seasonalProbOfSpawn-1);
						}

						if ( uniform.nextDoubleFromTo(0, 1) < betaDist.cdf(seasonalProbOfSpawn) ) {

							System.out.println("\tIn breeding season."); 


							int lunarPhase = TimeUtils.getMoonPhase(currentDate);
							normal.setState(15, lunarSpecificity); 

							int adjustedPhase = lunarPhase; 
							int j = adjustedPhase ; 

							// if peak day isn't equal to 15, then need to scale the peak day to equal 15
							if (peakLunarSpawnTime != 15) {
								int dayDiff = 15 - peakLunarSpawnTime; 
								adjustedPhase = lunarPhase+dayDiff; 
								if (adjustedPhase < 0) adjustedPhase = 30+adjustedPhase; 
								if (adjustedPhase> 15) j = (adjustedPhase-(adjustedPhase-15)*2);
								else j = adjustedPhase;  
								if (j < 0) j=-j; 
							}

							double probOfSpawning =(normal.pdf(j)/normal.pdf(15)); 
							double randProb =uniform.nextDoubleFromTo(0, 1) ;  
							System.out.println("\tprobOfSpawing: " + probOfSpawning + ",  randProb: " + randProb); 

							if ( randProb < (normal.pdf(j)/normal.pdf(15)) ) {
								spawnThisStep = true; 
								System.out.println("****SPAWN THIS STEP****"); 
							}




							else {
								System.out.println("****DONT SPAWN THIS STEP****"); 

								long numDaysInSeconds; 
								boolean dayFound = false; 
								int i = 1; 
								while (!dayFound) {
									numDaysInSeconds = i*TimeUtils.SECS_PER_DAY; 
									lunarPhase = TimeUtils.getMoonPhase(currentDate);

									// here, reset the normal distribution so it reflects an individuals spawning specificity
									// Note: the median is set to 15 because need to rescale the values from 0-29 with peak at 15 in order get the proper probability for the peakLunarSpawnTime
									// 			* see notes below for explanation -- this was simply one procedure that works for getting a normal curve of probabilities around the peak spawning day
									normal.setState(15, lunarSpecificity); 

									adjustedPhase = lunarPhase; 
									j = adjustedPhase ; 

									// if peak day isn't equal to 15, then need to scale the peak day to equal 15
									if (peakLunarSpawnTime != 15) {
										int dayDiff = 15 - peakLunarSpawnTime; 
										adjustedPhase = lunarPhase+dayDiff; 
										if (adjustedPhase < 0) adjustedPhase = 30+adjustedPhase; 
										if (adjustedPhase> 15) j = (adjustedPhase-(adjustedPhase-15)*2);
										else j = adjustedPhase;  
										if (j < 0) j=-j; 
									}

									probOfSpawning =(normal.pdf(j)/normal.pdf(15)); 
									System.out.println("\tFuture day " + i + " has lunar phase of " + lunarPhase + " with prob of spawning " + probOfSpawning); 

									if ( uniform.nextDoubleFromTo(0, 1) < (normal.pdf(j)/normal.pdf(15)) ) {
										dayFound = true; 
										nextSpawnTime = currentTime + numDaysInSeconds;
										//addTimesToRunQueue(nextSpawnTime); 
										System.out.println("\tNext time to spawn: " + nextSpawnTime); 
									}


									i++; 
								}
							}

						}
						else System.out.println("\tNot in breeding season."); 
					}
					else System.out.println("\t****Fallow Period****"); 
				}
			}

		} // end if (!spawnNextStep) 
		else if (currentTime == nextSpawnTime){
			spawnThisStep = true;
		}
		// else if currentTime < nextSpawnTime, then don't spawn, since is already a future spawn time set
		else {
			System.out.println("\t****Waiting to spawn til lunar cycle****");
			spawnThisStep = false; 
		}

		return spawnThisStep; 
	}



	public boolean timeToSpawnOldOldVersion(){
		spawnThisStep = false; // set here to false as default
		//if (consoleOutput ) System.out.println("\t" + this.getClassName(this.getClass())+ " spawning...."  ); 

		// if a previous request to spawn at a set time wasn't set to this current time, then check if appropriate time to spawn based on spawn season and lunar periodicity 
		if (currentTime > nextSpawnTime) {
			if ( (biomass-0.9*nominalBiomass) > 0	) {
				if ( (age-ageMaturity) > 0 ) {
					if ( ((currentTime-lastSpawn) > fallowPeriod) || (currentTime<fallowPeriod) ) {
						if ( (currentDate.get(Calendar.DAY_OF_YEAR) > startOfBreeding) && (currentDate.get(Calendar.DAY_OF_YEAR) < endOfBreeding)) {

							System.out.println("\tIn breeding season."); 

							double probSpawn = 0; 
							double expiredDays1 = currentDate.get(Calendar.DAY_OF_YEAR);
							double probSpawn1 = ((1-spawnOffset)*Math.sin(Math.PI*((expiredDays1 - startOfBreeding)/(endOfBreeding-startOfBreeding))) + spawnOffset); 

							System.out.println("\tprobSpawn1: " + probSpawn1); 

							double expiredDays2 = currentDate.get(Calendar.DAY_OF_YEAR) + runTime/TimeUtils.SECS_PER_DAY;
							double probSpawn2 = ((1-spawnOffset)*Math.sin(Math.PI*((expiredDays2 - startOfBreeding)/(endOfBreeding-startOfBreeding))) + spawnOffset); 

							System.out.println("\tprobSpawn2: " + probSpawn2); 

							if (probSpawn1 < 0) probSpawn = 0;
							else if ( ((currentDate.get(Calendar.YEAR)-1900) != (currentDate.get(Calendar.YEAR)-1900)) && (probSpawn1 > 0) && (probSpawn2 < 0)) probSpawn = 1;
							else probSpawn = probSpawn1;

							double prob =uniform.nextDoubleFromTo(0, 1) ; 

							System.out.println("\tprobSpawn of: " + probSpawn + "\trandom prob: " + prob); 

							if (prob < probSpawn) {

								System.out.println("\tPassed probability test."); 

								// since passed probability, will spawn no matter what, either this step or the next lunar peak period.
								// first check if species has lunarSpecificity, and how strong it is (0-1, with 1 being ONLY spawns on lunar phase)
								if (uniform.nextDoubleFromTo(0, 1) < lunarSpecificity) {

									int lunarPhase = TimeUtils.getMoonPhase(currentDate);

									long difference = peakLunarSpawnTime - lunarPhase;
									if (Math.abs(difference) <= 0){
										spawnThisStep = true;


									}
									else {
										spawnThisStep = false; 

										long numDays = TimeUtils.getDaysTilPeakLunarPhase(currentDate, peakLunarSpawnTime); 
										System.out.println("number of days until spawn next: " + numDays); 
										nextSpawnTime = currentTime + numDays*TimeUtils.SECS_PER_DAY;  
										//timesToRunQueue.add(nextSpawnTime);
										System.out.println("next time to spawn: " + nextSpawnTime); 
									}

								}
								else {
									// this animal is not concerned with the lunar phase, so will spawn this timestep
									spawnThisStep = true; 
								}
							}
						}
					}
					else System.out.println("\t****Fallow Period****"); 
				}
			}

		} // end if (!spawnNextStep) 
		else if (currentTime == nextSpawnTime){
			spawnThisStep = true;
		}
		// else if currentTime < nextSpawnTime, then don't spawn, since is already a future spawn time set
		else {
			System.out.println("\t****Waiting to spawn til lunar cycle****");
			spawnThisStep = false; 
		}

		return spawnThisStep; 
	}


}
