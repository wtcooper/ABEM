package us.fl.state.fwc.abem.spawn;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.TreeMap;

import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import cern.jet.random.Distributions;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;

public class SeatroutBuilderNonSpatial {

	//use seperate locks so a thread can be distributing individuals while another finished thread
	//is recycling it old individuals
	Object lock1 = new Object(); //lock for build method
	Object lock2 = new Object(); //lock for seatrout recycling methods

	public boolean femalesOnly = true; 

	//private HashMap<ABEMCell, Double> gridCellsWithSeagrass;
	//private ArrayList<ABEMCell> gridCellArray; 
	private EmpiricalWalker ewSizeSelect;
	private EmpiricalWalker ewMonthSelect;

	double[] probs; 

	Calendar birthday= Calendar.getInstance(TimeZone.getTimeZone("GMT"));;


	//empirical probability distribution for the month that an individual was 
	//spawned as function of age
	double[] monthProbs = {0, 0.027027027, 0.571428571, 0.869565217, 0.891304348, 
			0.790322581, 0.719512195, 0.792682927, 0.433333333, 0, 0, 0};


	//recycler for all trout
	ArrayList<SpawnSeatrout> recycledTroutList = new ArrayList<SpawnSeatrout>(); 


	/**
	 * Builds the population and returns a TreeMap<ageClass, ArrayListOfSpawnSeatrout>
	 * 
	 * @param model
	 * @return
	 */
	public TreeMap<Integer, ArrayList<SpawnSeatrout>>  build(SpawnModelNonSpatial model) {

		synchronized (lock1) {


			Thread t = Thread.currentThread();
			//System.out.println("\t"+t.getName() + ": in build method with " + recycledTroutList.size() + " recycled trout" );

			TreeMap<Integer, ArrayList<SpawnSeatrout>> fishList = 
					new TreeMap<Integer, ArrayList<SpawnSeatrout>>();

			SeatroutParams params = model.params;


			//=========================================
			//=========================================
			//set up the total number to distribute for each age class
			//=========================================

			//distribute just females
			double[] sizeFreqArray = params.getSizeFreqArray(model.sizeFreqYear, 0);

			//initialize the fish list -- put entry for each age class, even if will be empty -- avoid null in model
			for (int i=0; i<sizeFreqArray.length; i++){
				ArrayList<SpawnSeatrout> list = new ArrayList<SpawnSeatrout>(); 
				fishList.put(i, list);
			}


			double totalToDistribute = 0; 
			double[] numToDistribute = new double[sizeFreqArray.length]; 
			int totalDistributed = 0; 


			totalToDistribute = Math.round(params.getPopulationAbundance(model.abundYear, 0) 
					/ params.getSchoolSize());
			ewSizeSelect = 
					new EmpiricalWalker(sizeFreqArray, Empirical.NO_INTERPOLATION, model.m);

			while (totalDistributed < totalToDistribute) {
				numToDistribute[ewSizeSelect.nextInt()]++;
				totalDistributed++;
			}



			//=========================================
			//=========================================
			//distribute out the seatrout
			//=========================================

			Uniform uni = model.uniform;
			Normal norm = model.normal;

			//discrete empirical distribution to choose month of birth of each individual
			ewMonthSelect = 
					new EmpiricalWalker(monthProbs, Empirical.NO_INTERPOLATION, model.m);


			//get a list of Seatrout, using as many recycled individuals as possible
			//to reach the quota of totalDistributed 
			ArrayList<SpawnSeatrout> recycledTrout = getRecycledTrout(totalDistributed);


			for (int ageClass=0; ageClass<sizeFreqArray.length; ageClass++){
				totalToDistribute = numToDistribute[ageClass]; 
				totalDistributed = 0; 

				//System.out.println("\t"+t.getName() + ": in build method, starting age class " + ageClass);

				while (totalDistributed < totalToDistribute) {

					SpawnSeatrout trout = null;
					if (recycledTrout.isEmpty()) trout = new SpawnSeatrout(); 
					else {
						//REMOVE FROM END!!! Else if remove index 0 will box down system BADLY
						trout = recycledTrout.remove(recycledTrout.size()-1); 
					}
					trout.initialize(model);

					//Here, create some individuals that are not yet born -- these are year 0 
					//individuals that will be born in upcoming year, and possibly a few early birthers 
					//could spawn late in season

					int yearOfBirth = model.currentDate.get(Calendar.YEAR) -ageClass; // -(ageClass+1); 
					int monthOfBirth = ewMonthSelect.nextInt(); //see note below
					//set to 1st day of month so can get max day next
					birthday.set(yearOfBirth, monthOfBirth, 1); 
					int day = uni.nextIntFromTo(1, birthday.getActualMaximum(Calendar.DAY_OF_MONTH));
					birthday.set(yearOfBirth, monthOfBirth, day);

					//note: ewMonthSelect is 0-11 indexes, so pass this straight to calendar

					//trout.birthdate = birthday;
					//set the birthday to Java millisec time
					trout.birthday = birthday.getTimeInMillis();

					trout.yearClass = ageClass; 

					int sex = 0;
					if (!femalesOnly) sex = (int) Math.round(uni.nextIntFromTo(0, 1));
					trout.groupSex = sex;

					//set the size at maturity -- get a random num from logistic distribution L(0,1)
					double rand = Distributions.nextLogistic(model.m);
					//scale to the appropriate mean and SD
					trout.sizeAtMaturity =params.getSizeAtMaturityAvg() + params.getSizeAtMaturitySD()*rand;


					//need to set this so won't have individuals born in the future, which can happen for age 0 individuals
					//need to set the biomass, based on estimated age from birthday
					double ageInDays = (model.currentDate.getTimeInMillis()-birthday.getTimeInMillis())/(1000*60*60*24);


					//assign age in days for the future-born as 0, so biomass won't end up NaN
					if (ageInDays<0) {
						trout.ageInDays = 0;
						trout.nominalBiomass = 0;
						trout.groupBiomass = 0;
					
						if (model.runner.useResourceDepGrowth){
							System.out.println("need to set up Linf for resource dep growth");
							System.exit(0);
						}
						else if (model.runner.useAssessmentGrowth){
							
							/*Here, the Linf individual variability is based on the assessment model
							 * von bertalannfy fit (which is a truncated to account for commercial catch
							 * not keeping those under minimum/maximum size).  The st deviation for
							 * this Linf is based on the empirical relationship between 
							 */
							if (model.runner.useVariableSize)
								trout.Linf = model.normal.nextDouble(params.getLinf_Assess(sex), params.getLinfStDev_Assess(0));
							else
								trout.Linf = params.getLinf_Assess(sex);
						}
						else {

							//set the trouts Linfinity value for individual variability
							if (model.runner.useVariableSize)
								trout.Linf = model.normal.nextDouble(params.getLinf(0), params.getLinfStDev(0));
							else
								trout.Linf = params.getLinf(0);
						}
						
					}
					
					//If have already been born, then set appropriate values
					else {
						trout.ageInDays = ageInDays;


						if (model.runner.useResourceDepGrowth){
							//initially set the TL to equal the average biomass per area, 
							trout.TL = params.getDDLengthAtAge((ageInDays)/365.25, sex, 
									model.resource, params.avgBiomassPerArea[0]);
							trout.nominalBiomass = params.getMassAtLength(trout.TL, sex) * trout.groupAbundance;
							trout.groupBiomass = trout.nominalBiomass;

							if (trout.TL <0)  {
								trout.TL = 0;
								trout.nominalBiomass = 0.0;
								trout.groupBiomass = 0.0;
							}
						}


						//if using assessment growth
						else if (model.runner.useAssessmentGrowth){
							
							/*Here, the Linf individual variability is based on the assessment model
							 * von bertalannfy fit (which is a truncated to account for commercial catch
							 * not keeping those under minimum/maximum size).  The st deviation for
							 * this Linf is based on the empirical relationship between 
							 */
							if (model.runner.useVariableSize)
								trout.Linf = model.normal.nextDouble(params.getLinf_Assess(sex), params.getLinfStDev_Assess(0));
							else
								trout.Linf = params.getLinf_Assess(sex);
							
							trout.TL = params.getLengthAtAge_Assess(trout.Linf, (ageInDays)/365.25, sex);
							trout.nominalBiomass = params.getMassAtLength_Assess(trout.TL, sex)* trout.groupAbundance;  
							trout.groupBiomass = trout.nominalBiomass;

							if (trout.TL < 0) {
								trout.TL =0;
								trout.nominalBiomass = 0.0;
								trout.groupBiomass = 0.0;
							}

						}



						//else, if use old way of doing growth
						else {

							//set the trouts Linfinity value for individual variability
							if (model.runner.useVariableSize)
								trout.Linf = model.normal.nextDouble(params.getLinf(0), params.getLinfStDev(0));
							else
								trout.Linf = params.getLinf(0);

							trout.TL = params.getLengthAtAge(trout.Linf, (ageInDays)/365.25, sex);
							trout.nominalBiomass = params.getMassAtLength(trout.TL, sex)* trout.groupAbundance;  
							trout.groupBiomass = trout.nominalBiomass;


							if (trout.TL < 0) {
								trout.TL =0;
								trout.nominalBiomass = 0.0;
								trout.groupBiomass = 0.0;
							}
							//calculate length at age based on double von Bert, then get nominal (i.e., expected) biomass
							// assign a random group biomass based on increasing variance with mass -- estimated stDev is ~10% of expected mass at length
						}



					}



					totalDistributed++; 

					//after finishing assigning properties, then add to the list to return
					ArrayList<SpawnSeatrout> list = fishList.get(ageClass);
					list.add(trout);

				} 
			}

			return fishList;
		}
	}


	public ArrayList<SpawnSeatrout> getRecycledTrout(int numToDistribute){
		synchronized (lock2) {
			ArrayList<SpawnSeatrout> list = new ArrayList<SpawnSeatrout>();

			for (int i=0; i<numToDistribute; i++){
				SpawnSeatrout trout = null;
				if (recycledTroutList.isEmpty()) trout = new SpawnSeatrout(); 
				else {
					trout = recycledTroutList.remove(recycledTroutList.size()-1);
				}
				list.add(trout);
			}
			return list;
		}
	}


	public  void recycleTrout(TreeMap<Integer, ArrayList<SpawnSeatrout>> fishList){

		synchronized (lock2) {
			//if recycling list, then recycle them all
			for (ArrayList<SpawnSeatrout> troutList: fishList.values()){
				for (SpawnSeatrout trout: troutList) {
					recycledTroutList.add(trout);
				}
			}
		}
	}


	/**
	 * Only add females to the simulation if set to true.  
	 * 
	 * @param femalesOnly
	 */
	public void setFemalesOnly(boolean femalesOnly) {
		this.femalesOnly = femalesOnly;
	}

}

