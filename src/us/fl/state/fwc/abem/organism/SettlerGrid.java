package us.fl.state.fwc.abem.organism;

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.icu.util.Calendar;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import us.fl.state.fwc.abem.Agent;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.DisperseMatrixParams;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;

public class SettlerGrid extends Agent {

	
	/*	TODO --  NOTE: TRACKING RELEASE SITES
	 *
	 *See notes in SettlerCell/Settler classes below
	 */

	
	//key is cell index, SettlerCell contains a hashmap of num settlers per week of birth
	HashMap<String, SettlerCell> settlerCells = new HashMap<String, SettlerCell>(); 
	FishTracker fishTracker; 
	double cellSize;

	public void initialize(Scheduler sched) {
		this.scheduler = sched;
		sched.setSGrid(this);
		cellSize = sched.getGrid().getCellArea();
		fishTracker = (FishTracker) scheduler.getMonitors().get("FishTracker"); 
	}


	
	
	/**
	 * Sets the new number of settlers 
	 * @param cell
	 * @param numSettlers
	 * @param avgPLD
	 */
	public void addSettlers(String className, int releaseSite, Int3D arrivalCell, 
			int numSettlers, double avgPLD){
	
		//TODO -- need to fix this up
		
		
		int weekOfBirth = scheduler.getCurrentDate().get(Calendar.WEEK_OF_YEAR);
		
		String cellKey = className +"_"+ arrivalCell.x +"_"+ arrivalCell.y +"_"+ arrivalCell.z;
		SettlerCell sCell = settlerCells.get(cellKey);
		if (sCell == null){
			sCell = new SettlerCell(weekOfBirth, className, releaseSite, arrivalCell, numSettlers, avgPLD);
		}

		//TODO -- uncomment this and fix up
		
/*		double[] data = sumSettlersPerCell.get(settlerKey);
		if (data == null) {
			data = new double[2];
			data[0] = num;
			data[1] = avgPLD;
			sumSettlersPerCell.put(settlerKey, data);
		}
		else {
			data[1] = (data[1]*data[0] + avgPLD*num)/(data[0]+num); //compute new weighted mean PLD
			data[0] += num; //add in new number after use it to compute w. mean PLD
		}
*/
		
	}
	
	
	
	
	@Override
	public void run() {

		//return if no settlers  -- if dispersal is turned off, this will always be empty
		if (settlerCells == null || settlerCells.isEmpty()) return;

		
		OrganismFactory fac = scheduler.getOrganismFactory();
		Normal norm = scheduler.getNormal();
		Uniform uni = scheduler.getUniform();
		Parameters params =null;


		//!!!!!!!!!!!!!!!!!!! IMPORTANT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//TODO -- will need to add in the number of current settlers
		//into the ABEMCell, versus having the FishTracker monitor them
		//FishTracker will only pick up agents, and the settlers don't run as 
		//seperate agents, so need to manually add settlers to the currentNums array
		//in ABEMCell, and remove addition from FishTracker->setAvgNums()
		
		//else, query the totalSetters in each settler cell, making sure to increment/decrement
		//each step
		//!!!!!!!!!!!!!!!!!!! IMPORTANT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!



		//end loop over numPartsPerSpecies so can sum up sumSettlersPerCell just once, then iteratate over sumSettlersPerCell

		//=========================================
		//=========================================
		//Iterate through setters per cell, apply mort, and add to simulation
		//=========================================

		double M = 0; //nat mortality
		double timestep = 0 ;
		double parts = 0, mort = 0;
		String className; 

		//loop through all settlement locations from a single release site, apply mortality, 
		// and add to simulation
		for (String settlerKey: settlerCells.keySet()) {

			SettlerCell sCell = settlerCells.get(settlerKey);

			String[] tokens = settlerKey.split("_");
			Int3D cellIndex = new Int3D(
					Integer.parseInt(tokens[1]), 
					Integer.parseInt(tokens[2]), 
					Integer.parseInt(tokens[3]));
			className = tokens[0];

			
			for (Integer weekOfYear: sCell.settlers.keySet()){

				
				double[] settlerData = sCell.settlers.get(weekOfYear);

				parts = settlerData[0];

				//this is weighted avgPLD for all settlers, based on the avgPLD 
				//from a release to an arrival site -- since group all arrivers and don't keep 
				//track of release site (impossible to do so if merging individuals), need to use an
				//average value
				double avgPLD = settlerData[1]; 


				//=========================================
				/*For DD Mortality, use simple approach where have offsets to M rate
				 * due to the density of existing settlers and the density of adults.  
				 * To get density of settlers, first need to calculate the total number 
				 * that  would survive of the incoming cohort using only the 
				 * existing settler density as a DD factor, so as not to artifically
				 * inflate the DD effect due to how often the DipserseMatrix is run, then
				 * re-calculate the total surviving based on the sum of the existing
				 * and the expected total surviving of the incoming cohort.  
				 * This way, avoids using the incoming cohort size as a direct DD 
				 * factor, which can naturally inflate the M rate simply based on how 
				 * often the DisperseMatrix is run.  E.g., if run once per week,
				 * will have a high incoming cohort size due to summing up over many 
				 * days 
				 * 
				 * Nt = Math.exp(-M*timestep) * N0
				 * 
				 * M = M_DI + (M_DDA + M_DDS)
				 * 
				 * M_DI = nat inst mort rate
				 * M_DDA = increase in M due to density of adults
				 * M_DDS = increase in M due to density of settlers
				 * 
				 * M_DDA/M_DDS offsets follow Type II/TypeIII responses, respectively,
				 * based on theoretical functional shapes of the foraging agena theory.  
				 */
				//=========================================


				//=========================================
				//1st run through DD mort to get predicted settler density
				//=========================================

				double tempParts = parts; 

				//-------------------------------------------------------------
				//Apply 1st post-settlement DD mortality (1-2 weeks)
				//-------------------------------------------------------------

				//get the cell holding environment data
				ABEMCell cell = (ABEMCell) scheduler.getGrid().getGridCell(cellIndex);

				double settlerDensity = cell.getNumSettlers(className) / cellSize;

				//this is density / sq meter
				double adultDensity = 
					cell.getNumAdults(className)/cellSize;

				M = params.getEPSM(0); //nat inst mortality on per day rate
				timestep = params.getAvgPLD()-avgPLD;

				//add in DD mortality due to adults:
				M += (params.getMAdultVmax(0)
						*Math.pow(adultDensity,params.getMAdultExpon(0)))
						/ ((params.getMAdultKm(0)
								+Math.pow(adultDensity,params.getMAdultExpon(0))));

				//add in DD mortality due to settlers:
				M += (params.getMSettlerVmax(0)
						*Math.pow(settlerDensity,params.getMSettlerExpon(0)))
						/ ((params.getMSettlerKm(0) 
								+Math.pow(settlerDensity,params.getMSettlerExpon(0))));

				mort = Math.exp(-M*timestep);
				tempParts *= mort; 


				//-------------------------------------------------------------
				//Apply 2nd post-settlement DD mortality (1-3 months)
				//-------------------------------------------------------------

				//apply 3rd mortality to juveniles for a later post-settlement phase -- 
				//		seatrout use 90days following rates in Powell et al. 2004
				M = params.getEPSM(1); //nat inst mortality on per day rate
				timestep = params.getAvgEPSD();

				//add in DD mortality due to adults:
				M += (params.getMAdultVmax(1)
						*Math.pow(adultDensity,params.getMAdultExpon(1)))
						/ ((params.getMAdultKm(1)
								+Math.pow(adultDensity,params.getMAdultExpon(1))));

				//add in DD mortality due to settlers:
				M += (params.getMSettlerVmax(1)
						*Math.pow(settlerDensity,params.getMSettlerExpon(1)))
						/ ((params.getMSettlerKm(1) 
								+Math.pow(settlerDensity,params.getMSettlerExpon(1))));

				mort = Math.exp(-M*timestep);					
				tempParts *= mort; 




				//=========================================
				//Now use predicted value to get real value
				//=========================================

				//add in predicted amount and recalculate
				settlerDensity += tempParts / cellSize;

				//-------------------------------------------------------------
				//Apply 1st post-settlement DD mortality (1-2 weeks)
				//-------------------------------------------------------------

				M = params.getEPSM(0); //nat inst mortality on per day rate
				timestep = params.getAvgPLD()-avgPLD;

				//add in DD mortality due to adults:
				M += (params.getMAdultVmax(0)
						*Math.pow(adultDensity,params.getMAdultExpon(0)))
						/ ((params.getMAdultKm(0)
								+Math.pow(adultDensity,params.getMAdultExpon(0))));

				//add in DD mortality due to settlers:
				M += (params.getMSettlerVmax(0)
						*Math.pow(settlerDensity,params.getMSettlerExpon(0)))
						/ ((params.getMSettlerKm(0) 
								+Math.pow(settlerDensity,params.getMSettlerExpon(0))));

				mort = Math.exp(-M*timestep);
				parts *= mort; 


				//-------------------------------------------------------------
				//Apply 2nd post-settlement DD mortality (1-3 months)
				//-------------------------------------------------------------

				//apply 3rd mortality to juveniles for a later post-settlement phase -- 
				//		seatrout use 90days following rates in Powell et al. 2004
				M = params.getEPSM(1); //nat inst mortality on per day rate
				timestep = params.getAvgEPSD();

				//add in DD mortality due to adults:
				M += (params.getMAdultVmax(1)
						*Math.pow(adultDensity,params.getMAdultExpon(1)))
						/ ((params.getMAdultKm(1)
								+Math.pow(adultDensity,params.getMAdultExpon(1))));

				//add in DD mortality due to settlers:
				M += (params.getMSettlerVmax(1)
						*Math.pow(settlerDensity,params.getMSettlerExpon(1)))
						/ ((params.getMSettlerKm(1) 
								+Math.pow(settlerDensity,params.getMSettlerExpon(1))));

				mort = Math.exp(-M*timestep);					
				parts *= mort; 


				
				
				//=========================================
				//Check if time to settle
				//=========================================
				
				/*TODO -- ascertain whether the individuals are old enough to 
				 * settle, and if so:
				 * 
				 *	(1) settle them as below
				 *	(2) remove them from SettlerCell hashmap.  
				 *
				 *If not, then:
				 * 	(1) update the SettlerCell hashmap
				 * 	(2) update the SettlerCell totalSettlers  
				 */



				//=========================================
				//Calculate the number to distribute based on school size
				//=========================================

				double modGroupSize = parts % (double) params.getSchoolSize();
				double fractPart = modGroupSize - (long) modGroupSize;
				modGroupSize = (long) modGroupSize;

				//catch fractional part to test for adding extra individual -- if high mortality and 
				//well dispersed settlers, will need to do this to get some survivors
				if (uni.nextDoubleFromTo(0, 1) < fractPart) modGroupSize++;

				long numSchools = (long) parts / params.getSchoolSize(); 

				//if not any individuals to distribute, i.e., all died, then return
				if (modGroupSize == 0 && numSchools == 0) continue; 


				int totalToDistribute = (int) (1 + numSchools);
				//if no remainder (i.e., multiple of school size), then set equal to numParts
				if (modGroupSize == 0) totalToDistribute--;  
				int totalDistributed = 0; 


				//age at which will be added to scheduler
				int ageInDays = params.getAvgPLD() + params.getAvgEPSD();
				long firstRunTime = scheduler.getCurrentTime() 
				+ ageInDays*TimeUtils.MILLISECS_PER_DAY;



				//=========================================
				//create new agents with characteristics (size, age, etc)
				//=========================================

				boolean distributedModulusGroup =false;

				while (totalDistributed < totalToDistribute) {			

					//====================================
					// Set the individual properties
					//====================================

					//get a new datagram to store
					OrgDatagram data = fac.getDatagram();
					data.setClassName(className);


					//set the release site ID   
					//data.setReleaseID(releaseSite); 


					data.setGridCellIndex(cellIndex); 
					data.setCoord(scheduler.getGrid().getGridCell(data.getGridCellIndex()).getCentroidCoord()); 

					//set the birthday to Java millisec time
					data.setBirthday(scheduler.getCurrentTime());
					data.setYearClass(0); 

					//set the group abundance to start at the school size, aka group threshold
					//if haven't already distributed the first group which is the remainder of numParts
					///schoolSize, then set that one first
					if (distributedModulusGroup == false && modGroupSize > 0){
						data.setGroupAbundance((int) modGroupSize);
						distributedModulusGroup = true;
					}
					else data.setGroupAbundance(params.getSchoolSize());

					//int sex = (int) Math.round(uni.nextIntFromTo(0, 1));
					data.setGroupSex((int) Math.round(uni.nextIntFromTo(0, 1)));

					data.setFirstRunTime(firstRunTime); 
					data.setIsSettler(true);  //signals that it's a settler and hasn't run yet

					//TODO -- move this species specific stuff to SeatroutParams

					/*From Peebles and Tolly 1988:
					 * I = otolith increment number and L = standard length.
					 * L = 0.405I + 0.116
					 * From Powell et al 2004: 
					 * Growth of larvae and juveniles (<80 mm SL) was best described 
					 * by the equation loge standard length = –1.31 + 1.2162 (loge age).
					 * 
					 * Growth in length of juveniles (12–80 mm SL) was best described 
					 * by the equation standard length = –7.50 + 0.8417 (age). 
					 * 
					 * Growth in wet weight of juveniles (15–69 mm SL) was best described 
					 * by the equation loge wet-weight = –4.44 + 0.0748 (age).
					 */
					//calculate length at age based on von Bert, then get group biomass
					// assign nominal biomass as groupbiomass


					//only record mass, not length of individuals
					//double length = .8417*ageInDays - 7.5; //from Powell et al. 2004
					//double logeMass = 0.0748*ageInDays - 4.44; //from Powell et al. 2004; 
					//double mass = Math.exp(logeMass); 
					// = params.getMass(params.getLengthAtAge(ageInDays/365.25, sex));
					data.setGroupBiomass((Math.exp(0.0748*ageInDays - 4.44))
							*data.getGroupAbundance());
					data.setNominalBiomass(data.getGroupBiomass());  

					//add the total recruitment to the repro monitor
					fishTracker.addRecruit(data);

					//cell.setIsActive(true);
					//fishTracker.addActiveCell(cell); //make cell active in fishTracker

					//add the number of settlers to the cell counter
					//this is done in FishTracker through isSettler boolean flag in Fish
					//cell.addSettlers(data.getClassName(), data.getGroupAbundance());

					//====================================
					//Merge / add new agent
					//====================================

					/*Note: with merging, simply add abundance/weight into existing agent,
					 * and don't adjust sizeAtMaturity or firstRunTime -- generalize this so don't
					 * have to tinker around in the runTimeBag to reset the run time every time
					 * an agent is merged
					 */

					boolean createNew = true;

					//if this distributee isn't full or almost full
					if (data.getGroupAbundance()< params.getSchoolSize()*.75){
						Fish merger = (Fish) cell.getMerger(data.getClassName(), data.getGroupSex());

						// if there's an existing agent to merge into, and that merger isn't already 
						//full
						if ( merger != null 
								&& merger.getGroupAbundance() < params.getSchoolSize()) {   

							if ( (data.getFirstRunTime()-merger.getCurrentTime()) 
									< 30*TimeUtils.SECS_PER_DAY) { // if close enough in time

								//merge agent
								merger.setGroupAbundance(
										merger.getGroupAbundance() + data.getGroupAbundance());
								merger.setGroupBiomass(
										merger.getGroupBiomass() + data.getGroupBiomass());
								merger.setNominalBiomass(
										merger.getNominalBiomass() + data.getNominalBiomass());

								createNew = false;
							}
						}
					}

					if (createNew) {
						Organism newbee = fac.newAgent(data);
						cell.setMerger(data.getClassName(), newbee);
					}

					totalDistributed++; 

				} //end of while loop over totalNumToDistribute
			}//end of loop over setters for each WEEK_OF_YEAR
		}//end for loop over settlerCells with settlers to apply

	}


	public void setSettlers(Int3D index, double[] data, int weekOfYear){

	}


	@Override
	public void setDependentsList() {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerWithMonitors() {
		// TODO Auto-generated method stub

	}


}


/**
 * Data class holding the settlers
 * @author Wade.Cooper
 *
 */
class SettlerCell {

	public long totalSettlers; //this holds current num of all settlers across weeks of birth
	
	/*key is the week of the year in which they were born -- thereby can group
	*individuals by the week of the year, and repeat each year
	*double[] is numParts=[0], and avgPLD=[1]
	*/
	public HashMap<Integer, double[]> settlers = new HashMap<Integer, double[]>();


	public SettlerCell(int weekOfBirth, String className, int releaseSite, Int3D arrivalCell,
			int numSettlers, double avgPLD) {
		
	}



	
	
}




/**
 * Data storage for an individual settler
 * @author Wade.Cooper
 *
 */
class Settler {
	public int releaseSite; //site of release, to track spatial position of where came from
	public double condition; //factor representing their condition from maternal carry-over effect
}

