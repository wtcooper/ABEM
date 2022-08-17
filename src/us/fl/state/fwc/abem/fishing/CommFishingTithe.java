package us.fl.state.fwc.abem.fishing;

import java.util.Calendar;

import us.fl.state.fwc.abem.monitor.Monitor;
import us.fl.state.fwc.abem.organism.Fish;
import us.fl.state.fwc.util.TimeUtils;

public class CommFishingTithe extends Monitor {

	@Override
	public void run() {


		for (int i=0; i<monitorees.size(); i++){
			
			Fish fish = (Fish) monitorees.get(i); 
			if (!fish.getParams().isCommOpenSeason(scheduler.getCurrentDate())) return;  // return if its not the appropriate season to fish; could set this up to not run until season begins
			
			int year = scheduler.getCurrentDate().get(Calendar.YEAR); 
			int groupAbundance = fish.getGroupAbundance(); 
			double fishLength = 
				fish.getParams().getLengthAtMass((double) fish.getGroupBiomass() 
						/ (double) groupAbundance, fish.getGroupSex());  
			int  fishYearClass = fish.getYearClass(); 
			int fishSex = fish.getGroupSex(); 


			/*
			 * The commercial catch rate represents the average instantaneous rate of getting harvested, where  instant. rate is on a unit basis defined in the fish's parameters class 
			 * (e.g., for seatrout, is on monthly basis since some can get caught by rec fisherman multiple times a year).
			 * Note: here, need to deal with open season length, since input instCatchRate is on average rate for the entire year (what most data is presented in).      
			 */
			double instCatchRate = fish.getParams().getCommInstMortality(year, fishLength, fishYearClass, fishSex);
			int seasonDays =fish.getParams().getCommOpenSeasonNumDays(year) ;  
			if (seasonDays < 365.25){
				
				//TODO -- need to finish up this train of thought from months and months ago...
				//double propDiePerYear = (1-)
			}
			
			//Here, probOfCatch is the time-
			double probOfCatch = 1- Math.exp(-(instCatchRate/((double) TimeUtils.SECS_PER_YEAR / (double) timeStep))  ) ;

			// if they're caught, then assess their mortality
			if (uniform.nextDoubleFromTo(0, 1) < probOfCatch){

				if (groupAbundance == 1) scheduler.getOrganismFactory().recycleAgent(fish); 
				else{ // if larger group
					int numToDie = (int) Math.round( (double) groupAbundance * probOfCatch) ;
					groupAbundance -= numToDie; 
					if (groupAbundance == 0)  scheduler.getOrganismFactory().recycleAgent(fish);
					else fish.setGroupAbundance(groupAbundance); 
				}
			} // end of if caught
		} // end of monitorees loop

	}


}
