package us.fl.state.fwc.abem.test;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnLunarTest {

	double groupBiomass=5; // refers to total biomass of Animal (i.e., if individual or group) 
	int groupAbundance =1; 
	protected double groupAge=5;

	protected DecimalFormat df = new DecimalFormat("#.####"); 
	protected DecimalFormat df2 = new DecimalFormat("#"); 

//	long seed = System.currentTimeMillis(); 
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
	long lastSpawn=0; 
	double spawnOffset = 0;
	double startOfBreeding = 59; 
	double endOfBreeding = 272; 

	double lunarSpecificity = 100; // low number, e..g, 0 or 1 = only spawn on lunar cycle, while high number, e.g., 100 = no lunar specificity
	int peakLunarSpawnTime = 0; // 0 = full moon; 4 = new moon
	boolean spawnThisStep = false; 
	long nextSpawnTime = 0; 

	Scheduler schedule = new Scheduler(); 

	SeatroutParams params = new SeatroutParams(); 

	boolean isMature = false; 

	public static void main(String[] args) {

		SpawnLunarTest mt = new SpawnLunarTest();

		mt.step(); 
	}


	public void step(){

		params.initialize(schedule);
		m =schedule.getM();
		uniform = schedule.getUniform();
		normal = schedule.getNormal();
		bernoulli = schedule.getBernoulli(); 

		for (int i = 0; i<365; i ++) {

			currentDate = new GregorianCalendar(2009, 0, 1);
			currentDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
			/* NOTE: this is not done as in Gray et al. 2006 -- their formulation using a sine wave is difficult to interpret and when coded as interpreted in the document,
			 * it led to strange results not reflective of what an animal would actually do.  May have mis-interpreted, description 
			 * 
			 */
			if (timeToSpawn(i)) {
//				System.out.println("*****SPAWN!*****");
				lastSpawn = currentTime; // here, make sure the currentTime is already set

			}

//			System.out.println(); 
			currentDate.add(Calendar.DAY_OF_YEAR, 1); 
			currentTime+=secsPerDay;


		} // END OF IF STATEMENT TO DO LOOP OVER DAYS

	}








	public boolean timeToSpawn(int day){



		spawnThisStep = false; // set here to false as default
		double daysInRunTime = runTime/TimeUtils.SECS_PER_DAY; //set the total number of days this animal will be stepping for 
		double probScaler = 1; 
		if (daysInRunTime < 1) probScaler = daysInRunTime; // do this so that can scale the probability if less than a day so that if taking short time steps (e.g., seconds/minutes if evading predator or hunting) then won't spawn each time

						normal.setState(15, lunarSpecificity); 

						//System.out.println("\tIn breeding season."); 


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
//							System.out.println("\tprobOfSpawing: " + probOfSpawning + "\t  randProb: " + randProb); 
							System.out.println(day + "\t" + probOfSpawning ); 


							i++; 

						} // end of while loop

		return spawnThisStep; 
	}



}
