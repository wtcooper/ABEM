package us.fl.state.fwc.abem.organism;


/** Old animal methods
 * Also see specific testing classes in ....jinvitro.test for different methods which hold some of old methods
 * @author Wade.Cooper
 *
 */
public class Fish_OldMethods {

	
	/** Primary mortality method where removes individuals randomly dependent on their age; i.e., older individuals have higher probability of removal.
	 * 
	 */
	public void mortality(){

		// TODO first check for mortality due to starvation: if mass is less than 2/3rds of nominal mass, then it dies


		/*

		double proportionToDie = 0; 
		Double  currentMortality = new Double(0); 
		double amod = 0; 

		// use 50% maturity for this
		amod = ((params.getAgeMax() + params.getAgeAtRecruitment())/2) - params.getAgeAtRecruitment(); 

		currentMortality  = Math.min(   1,   Math.max(0,   ( (  1-Math.sqrt(   (1-params.getBaseMortality(stage))  *  (1-params.getBaseMortality(stage))  *  (1-(   ((groupAge-amod)*(groupAge-amod))  /  (  ((params.getAgeMax()+params.getAgeAtRecruitment())/2) * ((params.getAgeMax()+params.getAgeAtRecruitment())/2)  ) ) )  ) )  * (( (double) runTime)/((double)TimeUtils.SECS_PER_YEAR))  ) ) )  ;


		if ( (uniform.nextDoubleFromTo(0, 1) < currentMortality  || currentMortality.isNaN())) {
			if (groupAge < params.getAgeMax()) proportionToDie = 0.001*(uniform.nextDoubleFromTo(0, 1)); 
			else proportionToDie = 1;

			// here, mortaltiy rate
			double mortalityRate = Math.max(proportionToDie*groupAbundance, 1); 

			int counter = 0; 
			while ( (groupAbundance > 0) && (counter < mortalityRate)  ) {
				int index = uniform.nextIntFromTo(0, individualBiomass.size()); 
				groupBiomass -= individualBiomass.get(index); 
				groupAbundance--; 
				individualBiomass.remove(index); 
				individualAges.remove(index); 
				counter++; 
			}

			//NOTE: if groupSize==0, will remove this agent in the processRates() method and then return out of method; since processRates() is last method, will return from step
		}



*/
	}
	
	
	
	
	
	/** Growth method.  If trophics are not included (i.e., no explicit feeding), then do a false metabolism as in Gray et al. 2006. If trophics are on, then growth is dependent
	 * on the injested food and metabolism.
	 * 
	 */
	public void growth(){
		

		// do false metabolism if trophics, i.e., explicit feeding/hunting, is not modeled explicitly, but food intake is considered adequate if in suitable habitat 
		/* here, set growth to simulate the growth rates typical from a fitted von Bertalanffy growth estimates on fork length of the fish species
		 * because growth is in mass, the lenght-weight relationship (W = aL^b) must first be used to derive Weight-Age from the Lenght-Age vonB relationship
		 * then, the Weight-Age is fit to a generalized Michalis-Menton functin (Lopez et al. 2000, J. Anim Science 78:1816-1828) to get parameters K (growth) and c (functional shape)
		 * this MM function fits a sigmoidal shape to Weight-Age, which may be more appropriate than a saturating von Bertalanffy fit to Weight-Age as done in Gray
		 * In Gray et al., used a schema to calculate new growth based on the difference between biomass and nominal biomass; this approach is adopted here in the getNewGrowth() function,
		 * so it simulates Gray et al's false metabolism approach, but the underlying functional fit between growth and age is different and more appropriate for the species here.
		 */ 

		/*

		
		if (!scheduler.isTrophicsOn()) {

			// do false growth as per Gray et al. 
			double newGrowth, newGrowthNom; 
			double tPrime, tPrimeNom; 
			double runTimePrime = ((double) runTime) / (60*60*24*365.25); 
			double tempMass = 0, tempNomMass = 0; 
			double weight; 

			groupBiomass = 0; // set group 
			nominalBiomass = 0; 
			
			double avgBiomass = groupBiomass / (double) groupAbundance; 
			double avgNominalBiomass = nominalBiomass / (double) groupAbundance;
			
			for (int i = 0; i < individualBiomass.size(); i++){

				double individualMass = individualBiomass.get(i); 
				double individualNomMass = individualNominalBiomass.get(i); 

				if (individualMass >= params.getMaxMass()) tempMass = params.getMaxMass()-0.0001; // here, set this so that new growth rate will equal growth rate for the maximum mass.  If over this, will return NaN
				else tempMass = individualMass; 

				if (individualNomMass >= params.getMaxMass()	) tempNomMass = params.getMaxMass()-0.0001; 
				else tempNomMass = individualNomMass; 

				tPrime = Math.pow(((params.getIniMass()*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)))/(tempMass-params.getMaxMass()) - (tempMass*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)))/(tempMass-params.getMaxMass())),(1/params.getGrowth_c(stage))); 
				tPrimeNom = Math.pow(((params.getIniMass()*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)))/(tempNomMass-params.getMaxMass()) - (tempNomMass*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)))/(tempNomMass-params.getMaxMass())),(1/params.getGrowth_c(stage))); 

				newGrowth = ((params.getIniMass()*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + params.getMaxMass()*Math.pow(tPrime+runTimePrime, params.getGrowth_c(stage)))/(Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + Math.pow(tPrime+runTimePrime,params.getGrowth_c(stage)))) - ((params.getIniMass()*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + params.getMaxMass()*Math.pow(tPrime, params.getGrowth_c(stage)))/(Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + Math.pow(tPrime,params.getGrowth_c(stage)))); 
				newGrowthNom = ((params.getIniMass()*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + params.getMaxMass()*Math.pow(tPrimeNom+runTimePrime, params.getGrowth_c(stage)))/(Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + Math.pow(tPrimeNom+runTimePrime,params.getGrowth_c(stage)))) - ((params.getIniMass()*Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + params.getMaxMass()*Math.pow(tPrimeNom, params.getGrowth_c(stage)))/(Math.pow(params.getGrowth_K(stage),params.getGrowth_c(stage)) + Math.pow(tPrimeNom,params.getGrowth_c(stage)))); 

				individualMass+=newGrowth;
				individualNomMass+=newGrowthNom; 

				if (runTime > (((double)TimeUtils.SECS_PER_YEAR)/2)) {
					weight = 1; 
				}
				else {
					weight = runTime / (((double)TimeUtils.SECS_PER_YEAR)/params.getMassCatchUpRate());  // this will scale how quickly the mass catches up to nominal mass 
				}
				individualMass = (1-weight)*individualMass+(weight)*individualNomMass;
				groupBiomass += individualMass; // reset the biomass
				nominalBiomass += individualNomMass; // reset the nominal biomass
				individualBiomass.set(i, individualMass); 
				individualNominalBiomass.set(i, individualNomMass); 
			}
		}

		/*
		else {
			// TODO do explicit feeding growth here if included
		}
		*/

	}
	
	
	
	
	
	
	/**	Determines if the agent (either individual or all individuals in the group as whole) will spawn during this or a future time step.  Here, if agents are grouped, then will consider all individuals
	 * to be of same age, and the average groupBiomass and average group nominalBiomass will determine if they spawn or not.  
	 * 
	 * @return
	 */
	public boolean timeToSpawnOld(){
		
		/*
		spawnThisStep = false; // set here to false as default

		// if a previous request to spawn at a set time wasn't already performed (i.e., still in the future), then check if appropriate time to spawn based on spawn season and lunar periodicity 
		if (currentTime > nextSpawnTime) {
			if ( (groupBiomass-0.9*nominalBiomass) > 0	) {
				if ( (groupAge-params.getAgeMaturity()) > 0 ) {
					if ( ((currentTime-lastSpawn) > params.getFallowPeriod()) || (currentTime<params.getFallowPeriod()) ) {
						if ( (TimeUtils.getDate(currentTime, scheduler).get(Calendar.DAY_OF_YEAR) > params.getStartOfBreeding()) && (TimeUtils.getDate(currentTime, scheduler).get(Calendar.DAY_OF_YEAR) < params.getEndOfBreeding())) {

							int lunarPhase = TimeUtils.getMoonPhase(currentTime, scheduler);
							int adjustedPhase = lunarPhase; 
							int j = adjustedPhase ; 

							// here, reset the normal distribution so it reflects an individual's spawning specificity -- a lower value has high specificity, because reflects the SD; see notes below for reasoning
							// Note: the median is set to 15 because need to rescale the values from 0-29 with peak at 15 in order get the proper probability for the peakLunarSpawnTime
							// 			* see notes below for explanation -- this was simply one procedure that works for getting a normal curve of probabilities around the peak spawning day
							normal.setState(15, params.getLunarSpecificity());  

							// if peak day isn't equal to 15, then need to scale the peak day to equal 15
							if (params.getPeakLunarSpawnTime() != 15) {
								int dayDiff = 15 - params.getPeakLunarSpawnTime(); // get difference between peak day and the midpoint re-scaled peak day (15) 
								adjustedPhase = lunarPhase+dayDiff;  // rescale the lunar phase to an adjusted value
								if (adjustedPhase < 0) adjustedPhase = 30+adjustedPhase;  // make sure everything is positive so have seamless normal distribution around peak day
								if (adjustedPhase> 15) j = (adjustedPhase-(adjustedPhase-15)*2); // for rescaled values greater than 15, make them decrease equally on both sides of this median day
								else j = adjustedPhase;  
								if (j < 0) j=-j; //make negative values positive; after this, all values are consecutive from 0-29, with the peak day equal to 15
							}

							// here, the probability of spawning is based on the normal distribution PDF, but scaled to the mean value (15) so the mean has probability of 1.0
							if ( uniform.nextDoubleFromTo(0, 1) < (normal.pdf(j)/normal.pdf(15)) ) {
								spawnThisStep = true; 
							}
							// here, if they don't spawn this time step, then look forward in time to find the next day that they should spawn, using same approach as above (with rescaling)
							else {
								long numDaysInSeconds; 
								boolean dayFound = false; 
								int i = 1; 
								while (!dayFound) {
									numDaysInSeconds = i*TimeUtils.SECS_PER_DAY; 
									lunarPhase = TimeUtils.getMoonPhase(currentTime+numDaysInSeconds, scheduler);
									normal.setState(15, params.getLunarSpecificity()); 
									adjustedPhase = lunarPhase; 
									j = adjustedPhase ; 
									if (params.getPeakLunarSpawnTime() != 15) {
										int dayDiff = 15 - params.getPeakLunarSpawnTime(); 
										adjustedPhase = lunarPhase+dayDiff; 
										if (adjustedPhase < 0) adjustedPhase = 30+adjustedPhase; 
										if (adjustedPhase> 15) j = (adjustedPhase-(adjustedPhase-15)*2);
										else j = adjustedPhase;  
										if (j < 0) j=-j; 
									}

									if ( uniform.nextDoubleFromTo(0, 1) < (normal.pdf(j)/normal.pdf(15)) ) {
										dayFound = true; 
										nextSpawnTime = currentTime + numDaysInSeconds; 
										addTimeToRunQueue(nextSpawnTime); // once a day is found, then add that day to the queue so the animal will step then, and have them spawn on that day
									}
									i++; 
								} // end of while(!dayFound) loop
							}// end of else statement to spawn this step or find a future time
						} // end of check of breeding season
					} // end of check of fallow period 
				} // end of check of maturity
			} // end of check of biomass 
		} // end if(currentTime > nextSpawnTime) check
		else if (currentTime == nextSpawnTime){
			spawnThisStep = true;
		}
		else {		// else if currentTime < nextSpawnTime, then don't spawn, since is already a future spawn time set
			spawnThisStep = false; 
		}
		return spawnThisStep;
		
		*/
		return false; 
	}

	
	
	
	



	/**	Check for suitable spawn location 
	 * 
	 * @return
	 */
	public boolean suitableSpawnLocation(){
		
		/*
		boolean atSpawnLocation = false; 

		// TODO -- need to somehow make this appropriate for species like seatrout that will spawn in high concentrations in certain areas (Bunces), but that will also 
		// spawn in other areas too if in appropriate habitat.  

		// what if have a singular getSpawningHabitatQuality function that determines quality, but where the search mechanism is different than getHabitatQuality, and can 
		// assess over larger distances where some areas, like Bunces, are very high in quality.  Could even have a map of spawning habitat quality,

		if (params.haveSpawnAggregations() ){

			//check to see if they're at an aggregation site already 
			for (int i = 0; i < params.getSpawnAggregationList().size(); i++){
				// if within 10 meters of a spawn aggregation site, then consider them at an aggregation
				if (CoordinateUtils.getDistance(this.coords, params.getSpawnAggregationList().get(i)) < 10){
					atSpawnLocation = true; 
				}
			}
		}

		// else if not an aggregator, check to make sure are in good habitat which is considered suitable for spawning
		else {
			if (getPointHabitatQuality("spawn", coords) > 0) {
				atSpawnLocation = true; 
			}
		}

		return atSpawnLocation; 
	*/
		return false;
	}
	
	
	
}
