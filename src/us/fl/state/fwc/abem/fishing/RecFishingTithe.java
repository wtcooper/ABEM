package us.fl.state.fwc.abem.fishing;

import java.util.Calendar;

import us.fl.state.fwc.abem.monitor.Monitor;
import us.fl.state.fwc.abem.organism.Fish;
import us.fl.state.fwc.util.TimeUtils;

public class RecFishingTithe extends Monitor {




	@Override
	public void run() {


		for (int i=0; i<monitorees.size(); i++){
			Fish fish = (Fish) monitorees.get(i); 
			int year = scheduler.getCurrentDate().get(Calendar.YEAR); 
			int groupAbundance = fish.getGroupAbundance(); 

		
			double fishLength = 
				fish.getParams().getLengthAtMass((double) fish.getGroupBiomass() 
						/ (double) groupAbundance, fish.getGroupSex());  
			int  fishYearClass = fish.getYearClass(); 
			int fishSex = fish.getGroupSex(); 

			// the catchRate represents the average instantanous rate of getting caught, adjusted by environmental scalers (vary from 0-infinity, with 1 as average) 
			/*
			 * Note: don't need to worry about the length of recreational fishing season (i.e., when they can keep fish), if assumming fisherman fish as much out of season as in season, but they simple
			 * catch and releaes when not in season.  However, can have a seasonal scaler, which scales the instantaneous rate dependent on the season around the average monthly rate  

			int numToDie=0;
			for (int j=0; j<groupAbundance; j++){
				if ( uniform.nextDoubleFromTo(0, 1) < probOfKeep ) numToDie++; 
			}

			groupAbundance -= numToDie; 

			 */


			double instCatchRate = fish.getParams().getRecInstCatchRate(year, fishLength, fishYearClass, fishSex) * getSpatialScaler() * getSeasonalScaler() * getEnvironmentalScaler() ;
			//Here, probOfCatch is the time-
			double probOfCatch = 1- Math.exp(-(instCatchRate * (timeStep/(double) TimeUtils.SECS_PER_YEAR))); 
			double probOfKeep = 1- fish.getParams().getProbOfCatchAndRelease(year, fishLength, fishYearClass, fishSex);
			boolean isLegalSize = fish.getParams().isRecLeagalSize(year, fishLength);
			boolean isRecOpenSeason = fish.getParams().isRecOpenSeason(scheduler.getCurrentDate());
			double probOfReleaseMortality = fish.getParams().getRecReleaseMortality(year, fishLength, fishYearClass, fishSex); 


			int numToDie = 0;
			for (int j=0; j<groupAbundance; j++){


				// if they're caught, then assess their mortality
				if (uniform.nextDoubleFromTo(0, 1) < probOfCatch){

					//Fish is caught; now see if is kept or released

					//NOTE: assumes 100% compliance
					
					if (  isLegalSize && isRecOpenSeason &&  (uniform.nextDoubleFromTo(0, 1) < probOfKeep)) 
						numToDie++;
					else {
						//Fish is released, so check release mortality
						if (uniform.nextDoubleFromTo(0, 1) < probOfReleaseMortality) 
							numToDie++;
					}

				}
			}

			groupAbundance -= numToDie;
			if (groupAbundance == 0)  scheduler.getOrganismFactory().recycleAgent(fish);
			else {
				fish.setGroupAbundance(groupAbundance);
				//reset the groupBiomass
				double groupBiomass = 
					fish.getParams().getMassAtLength(fishLength, fish.getGroupSex())*groupAbundance;
				fish.setGroupBiomass(groupBiomass);
				fish.setNominalBiomass(groupBiomass);
			}

			
		} //end of loop over all monitorees

		
		
	}



	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	/** Adjustment to the base catch rate to account for differences in spatial effort of fishing ((vary from 0-infinity, with 1 as average)*/
	public double getSpatialScaler(){
		return 1; 
	}

	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	/** Adjustment to the base catch rate to account for seasonal variability in fishing effort ((vary from 0-infinity, with 1 as average)*/
	public double getSeasonalScaler(){
		return 1; 
	}

	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	/** Adjustment to the base catch rate to account for environmental influences on fishing effort ((vary from 0-infinity, with 1 as average)*/
	public double getEnvironmentalScaler(){
		return 1; 
	}			




}
