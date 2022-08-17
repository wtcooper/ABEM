package us.fl.state.fwc.abem.organism.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.params.impl.DisperseMatrixParams;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.Int3D;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;

import com.vividsolutions.jts.geom.Coordinate;

public class SeatroutBuilder {

	//private HashMap<ABEMCell, Double> gridCellsWithSeagrass;
	//private ArrayList<ABEMCell> gridCellArray; 
	private EmpiricalWalker ewCellSelect;
	ABEMGrid seagrassGrid; 
	double[] probs; 
	ArrayList<Int3D> indices = new ArrayList<Int3D>(); 
	Scheduler scheduler;

	public void build() {

		if (seagrassGrid == null) {
			if (SchedulerParams.limitToTampaBay)
				seagrassGrid = new ABEMGrid(
						DisperseMatrixParams.seagrassABEMGridName_TB, scheduler);
			else 
				seagrassGrid = new ABEMGrid(
						DisperseMatrixParams.seagrassABEMGridName, scheduler);
		}

		SeatroutParams params = (SeatroutParams) scheduler.getParamClass("Seatrout");

		int currentYear; 
		int[] yearsArray; 



		//=========================================
		//=========================================
		//set up the total number to distribute for each age class
		//=========================================

		
		/*
		 * TODO -- need to fix up so distributes males then females
		 */
		
		int yr =-9999; //dummy yr
		int sx = 2;
		double[] sizeFreqArray = params.getSizeFreqArray(yr, sx);
		double totalToDistribute = 0; 
		double[] numToDistribute = new double[sizeFreqArray.length]; 
		int totalDistributed = 0; 


			totalToDistribute = Math.round(params.getPopulationAbundance(yr, sx) 
					/ params.getSchoolSize());
			ewCellSelect = 
				new EmpiricalWalker(sizeFreqArray, Empirical.NO_INTERPOLATION, 
						scheduler.getM());

			while (totalDistributed < totalToDistribute) {
				numToDistribute[ewCellSelect.nextInt()]++;
				totalDistributed++;
			}
		
		//else distribute based on initial mass
		
		//TODO -- need to fix this so is female and male masses
		
		/*
		else {
			double[] massPerFish = new double[sizeFreqArray.length];
			double[] numFishPerAge = new double[sizeFreqArray.length];
			double totalMass= 0; 
			for (int i=0; i<sizeFreqArray.length; i++){
				massPerFish[i] = params.getMass(params.getLengthAtAge(i+0.3485, 0));
				numFishPerAge[i] =10000*sizeFreqArray[i];
				totalMass += numFishPerAge[i]*massPerFish[i];
			}
			
			double scaler = params.getPopulationMass()/totalMass;
			
			for (int i=0; i<sizeFreqArray.length; i++){
				numFishPerAge[i] *= scaler;
				numToDistribute[i] = numFishPerAge[i]/params.getSchoolSize(); 
			}
		}
 	*/




		//=========================================
		//=========================================
		//get the current year for the seagrass
		//=========================================

		Set<Integer> keys = seagrassGrid.getGridCell(new Coordinate(-82.7094,27.6679,0)).getSavCov().keySet();
		yearsArray = new int[keys.size()];
		Iterator<Integer> it = keys.iterator();
		int counter = 0; 
		while (it.hasNext()){
			Integer year = it.next();
			yearsArray[counter++] = year; 
		}

		//need to sort as the keys may not be in order
		Arrays.sort(yearsArray);
		currentYear = yearsArray[locate(yearsArray, scheduler.getCurrentDate().get(Calendar.YEAR))]; 




		//=========================================
		//=========================================
		//Iterate through all cells, and create a probability array
		//for the EmpiricalWalker
		//=========================================

		//create a collection of the indices that are ordered

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

		ewCellSelect = new EmpiricalWalker(probs, Empirical.NO_INTERPOLATION, scheduler.getM());


		//=========================================
		//=========================================
		//distribute out the seatrout
		//=========================================

		Uniform uni = scheduler.getUniform();
		Normal norm = scheduler.getNormal();
		OrganismFactory fac = scheduler.getOrganismFactory();
		OrgDatagram data = fac.getDatagram();
		GregorianCalendar tempDate = null; 

		data.setClassName("Seatrout"); 

		for (int ageClass=0; ageClass<sizeFreqArray.length; ageClass++){
			totalToDistribute = numToDistribute[ageClass]; 
			totalDistributed = 0; 

			counter = 0; 

			while (totalDistributed < totalToDistribute) {

				//get a random from 0-1, and check if it's less than a SAV cover (from 0-1) for a random ABEMCell
				//if it is, then assign a fish to that location
				//This may take a long time to go assign the fish, but by doing this way, should
				//be able to assign all the fish to cells based on the SAV cover of the cell, so the more 
				//SAV cover, the higher number of fish assigned to that cell

				Int3D index = indices.get(ewCellSelect.nextInt()); 

				data.setGridCellIndex(index);

				data.setCoord(seagrassGrid.getRandomPoint(index));

				//data.setSizeAtMaturity(norm.nextDouble(params.getSizeAtMaturityAvg(), params.getSizeAtMaturitySD()));

				//loop through seasonal peak sizes
				double sum = 0;
				int peak = 0;
				for (int j=0; j< params.getSeasonPeakSizes(ageClass).size(); j++){
					sum += params.getSeasonPeakSizes(ageClass).get(j);
				}

				double prob = uni.nextDoubleFromTo(0, 1); 

				double probSum = 0; 
				for (int j=0; j< params.getSeasonPeakSizes(ageClass).size(); j++){
					probSum += params.getSeasonPeakSizes(ageClass).get(j)/sum; 
					if (prob < probSum) {
						peak = j; 
						continue; 
					}
				}

				int dayOfBirth = (int) Math.round(params.getSeasonNormals(ageClass).get(peak).nextDouble()); 
				int yearOfBirth = scheduler.getCurrentDate().get(Calendar.YEAR) - (ageClass+1); 

				tempDate = new GregorianCalendar(yearOfBirth, 0, 1);
				tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
				tempDate.add(Calendar.DAY_OF_YEAR, dayOfBirth); 
				//set the birthday to Java millisec time
				data.setBirthday(tempDate.getTimeInMillis());

				data.setYearClass(ageClass); 

				//if we're down to the remainder, then set the groupAbundance to remainder
				if (totalToDistribute - totalDistributed < 1){
					data.setGroupAbundance((int) 
							(params.getSchoolSize()*(totalToDistribute-totalDistributed)	));
				}
				
				else data.setGroupAbundance(params.getSchoolSize());

				int sex = (int) Math.round(uni.nextIntFromTo(0, 1));
				data.setGroupSex(sex);

				//need to set this so won't have individuals born in the future, which can happen for age 0 individuals
				//need to set the biomass, based on estimated age from birthday
				double ageInDays = (scheduler.getCurrentDate().getTimeInMillis()-tempDate.getTimeInMillis())/(1000*60*60*24);

				//calculate length at age based on von Bert, then get group biomass
				// assign nominal biomass as groupbiomass
				double mass = params.getMassAtLength(params.getLengthAtAge(ageInDays/365.25, sex), sex);
				data.setGroupBiomass(mass*data.getGroupAbundance());
				data.setNominalBiomass(data.getGroupBiomass()); 


				//set the release site ID -- HERE make it default null value since are distributed randomly 
				data.setReleaseID(Integer.MIN_VALUE); 

				//set the new agent 
				fac.newAgent(data);

				totalDistributed++; 

			} 

			//System.out.println("age class: " + ageClass + "\ttotal distributed: " + totalDistributed + "\ttotal to distribute: " + totalToDistribute); 



		}
	}




	public int locate(int[] ja, int val){
		int idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// if -1, then is smaller than smallest value
			if (idx == -1) {
				return 0;
			}


			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= ja.length) {
				return ja.length-1;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;
			if (val < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}




	public ABEMGrid getSeagrassGrid() {
		return seagrassGrid;
	}




	public void setSeagrassGrid(ABEMGrid seagrassGrid) {
		this.seagrassGrid = seagrassGrid;
	}




	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
}
