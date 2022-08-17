package us.fl.state.fwc.abem.dispersal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.DisperseMatrixParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import com.vividsolutions.jts.geom.Coordinate;

/**Randomly distributes a given number of larvae to a preferred habitat within the domain.  
 * Here, that habitat is hard-coded to seagrass, but can easily adapt to include other types
 * of habitats.  The number of particles dispersed are pre-filtered with average mortlaity
 * rates to decrease the total number of particles through the pelagic and early post-settlement 
 * phase (e.g., a few months after settlement) when mortality is high.
 * 
 * @author Wade.Cooper
 *
 */

public class DisperseRandom implements Dispersal {

	long numLarvae; 
	//DispersalRandomParams params = new DispersalRandomParams(); 
	//HashMap<ABEMCell, Double> gridCellsWithSeagrass; 
	//ArrayList<ABEMCell> gridCellArray; 
	Scheduler scheduler; 
	int currentYear; 
	int oldYear=0;
	int[] yearsArray; 

	private EmpiricalWalker ewCellSelect;
	ABEMGrid seagrassGrid; 
	double[] probs; 
	ArrayList<Int3D> indices = new ArrayList<Int3D>(); 

	FishTracker fishTracker; 



	public void initialize(Scheduler sched){
		this.scheduler = sched;
		sched.setDispersal(this);
		
		if (seagrassGrid == null) seagrassGrid = new ABEMGrid(DisperseMatrixParams.seagrassABEMGridName, scheduler);

		//instantiate here with a dummy array, and then use the .setState2(probArray[]) down below
		ewCellSelect = new EmpiricalWalker(new double[] {1.0, 0.0}, Empirical.NO_INTERPOLATION, scheduler.getM());
	}





	@Override
	public synchronized void disperse(Organism t, long numParts) {

		if (fishTracker == null) fishTracker = (FishTracker) 
		scheduler.getMonitors().get("FishTracker"); 
		fishTracker.addTEP(t, numParts, t.getGridIndex()); 

		Parameters params = t.getParams(); 


		//=========================================
		//=========================================
		//get the number of recruits to distribute
		//=========================================

		//convert to double before doing mortality multiplication
		double parts = numParts; 

		//Pre-filter mortality through early post-settlement mortality for computational efficiency
		//apply mortality to larvae for the average PLD
		double mrate = 1-Math.exp(- ( params.getNatInstMort(-2,0,0) / ((double) (TimeUtils.SECS_PER_DAY*1000)/ (double) (params.getAvgPLD()*24*60*60*1000)))) ;	
		parts -= (mrate*parts);

		//apply mortality to juveniles for an early post-settlement phase (EPSD) -- seatrout use 90days following rates in Powell et al. 2004
		mrate = 1-Math.exp(- ( params.getNatInstMort(-1,0,0) / ((double) (TimeUtils.SECS_PER_DAY*1000)/ (double) (params.getAvgEPSD()*24*60*60*1000)))) ;	
		parts -= (mrate*parts);

		numParts = (int) Math.round(parts); 

		//age at which will be added to scheduler
		int ageInDays = params.getAvgPLD() + params.getAvgEPSD();
		long firstRunTime = scheduler.getCurrentTime() + ageInDays*TimeUtils.MILLISECS_PER_DAY;

		//need to catch remainder and set the initial one to distribute to remainder versus school size
		int modGroupSize = (int) (numParts%params.getSchoolSize() );

		//set the number to distribute to the total number divided by the group threshold (i.e., school size)
		numParts = numParts/params.getSchoolSize(); 
		
		//if not any individuals to distribute, i.e., all died, then return
		if (modGroupSize == 0 && numParts == 0) return; 



		//=========================================
		//=========================================
		//get the current year for the seagrass
		//=========================================

		//set the yearsArray -- this will be used for binary search to find closest year
		if (yearsArray == null){
			Set<Integer> keys = seagrassGrid.getGridCell(new Coordinate(-82.7094,27.6679,0)).getSavCov().keySet();
			yearsArray = new int[keys.size()];
			Iterator<Integer> it = keys.iterator();
			int counter = 0; 
			while (it.hasNext()){
				Integer year = it.next();
				yearsArray[counter++] = year; 
			}

			Arrays.sort(yearsArray);
		}

		currentYear = yearsArray[locate(scheduler.getCurrentDate().get(Calendar.YEAR))]; 






		//=========================================
		//=========================================
		//Iterate through all cells, and create a probability array
		//for the EmpiricalWalker
		//=========================================

		//create a collection of the indices that are ordered

		if (currentYear != oldYear){
			indices.clear(); 
			double cellSAVSum=0;
			Set<Int3D> keys2 = seagrassGrid.getGridCells().keySet(); 
			probs = new double[keys2.size()];

			Iterator<Int3D> it2 = keys2.iterator();
			while (it2.hasNext()){
				Int3D index = it2.next();
				indices.add(index);
				cellSAVSum += seagrassGrid.getGridCell(index).getSavCov().get(currentYear); 
			}

			//loop through again and standardize
			for (int i=0; i< indices.size(); i++){
				probs[i] =  seagrassGrid.getGridCell(indices.get(i)).getSavCov().get(currentYear)/cellSAVSum; 
			}

			ewCellSelect.setState2(probs); 

		}


		//=========================================
		//=========================================
		//distribute out the seatrout
		//=========================================

		Uniform uni = scheduler.getUniform();
		Normal norm = scheduler.getNormal();
		OrganismFactory fac = scheduler.getOrganismFactory();
		OrgDatagram data = fac.getDatagram();

		data.setClassName(t.getClassName());
		
		int totalToDistribute = (int) (1 + numParts);
		//if no remainder (i.e., multiple of school size), then set equal to numParts
		if (modGroupSize == 0) totalToDistribute =(int) numParts;  
		int totalDistributed = 0; 

		boolean distributedModulusGroup =false;

		while (totalDistributed < totalToDistribute) {

			//TODO -- could also do this with empirical walker distribution as via DisperseMatrix

			//get a random from 0-1, and check if it's less than a SAV cover (from 0-1) for a random ABEMCell
			//if it is, then assign a fish to that location
			//This may take a long time to go assign the fish, but by doing this way, should
			//be able to assign all the fish to cells based on the SAV cover of the cell, so the more 
			//SAV cover, the higher number of fish assigned to that cell
			Int3D index = indices.get(ewCellSelect.nextInt()); 

			data.setGridCellIndex(index);

			data.setCoord(seagrassGrid.getRandomPoint(index));

			//data.setSizeAtMaturity(norm.nextDouble(params.getSizeAtMaturityAvg(), params.getSizeAtMaturitySD()));

			//set the birthday to Java millisec time
			data.setBirthday(scheduler.getCurrentDate().getTimeInMillis());
			data.setYearClass(0); 

			//set the group abundance to start at the school size, aka group threshold
			//if haven't already distributed the first group which is the remainder of numParts/schoolSize,
			//then set that one first
			if (distributedModulusGroup == false){
				data.setGroupAbundance(modGroupSize);
				distributedModulusGroup = true;
			}
			else data.setGroupAbundance(params.getSchoolSize());

			data.setGroupSex((int) Math.round(uni.nextIntFromTo(0, 1)));

			data.setFirstRunTime(firstRunTime); 

			
			//set the release site ID -- HERE make it default null value since are distributed randomly 
			//set it equal to max value here, to signify that's a new recruit and distinguish it from
			//the Builder class where it's Integer.MIN_VALUE
			data.setReleaseID(Integer.MAX_VALUE); 
			
			
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
			//double mass = Math.exp(logeMass); // = params.getMass(params.getLengthAtAge(ageInDays/365.25, sex));
			data.setGroupBiomass((Math.exp(0.0748*ageInDays - 4.44))*data.getGroupAbundance());
			data.setNominalBiomass(data.getGroupBiomass());  

			//add the total recruitment to the repro monitor
			fishTracker.addRecruit(data);

			//set the new agent 
			fac.newAgent(data);

			totalDistributed++; 

		} 

		//System.out.println("age class: " + ageClass + "\ttotal distributed: " + totalDistributed + "\ttotal to distribute: " + totalToDistribute); 


	}


	public int locate(int currentYear)  {
		int idx;

		idx = Arrays.binarySearch(yearsArray, currentYear);

		if (idx < 0) {

			// Error check
			if (idx == -1) {
				return 0;
			}

			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= yearsArray.length) {
				return yearsArray.length-1;
			}

			double spval = (yearsArray[-(idx + 2)] + yearsArray[-(idx + 1)]) / 2d;
			if (currentYear < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}






	@Override
	public void setScheduler(Scheduler sched) {
		this.scheduler = sched; 
	}



	public void setSeagrassGrid(ABEMGrid seagrassGrid) {
		this.seagrassGrid = seagrassGrid;
	}


	@Override
	public ABEMGrid getSeagrassGrid() {
		return seagrassGrid;
	}





}
