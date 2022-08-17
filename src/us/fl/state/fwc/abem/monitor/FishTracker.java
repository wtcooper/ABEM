package us.fl.state.fwc.abem.monitor;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.abem.dispersal.bolts.util.TimeConvert;
import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.organism.Fish;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.ibm.icu.util.Calendar;


public class FishTracker extends Monitor {


	protected DecimalFormat df = new DecimalFormat("#.####"); 

	TreeMap<String, double[]> globalSums = new TreeMap<String, double[]>(); //key: classname 
	TreeMap<String, double[]> spatialSums = new TreeMap<String, double[]>();  //key: classname_gridID.x_gridID.y
	TreeMap<String, double[]> sizeFreq = new TreeMap<String, double[]>();
	TreeMap<String, Double> sumRecruitsPerYear = new TreeMap<String, Double>(); 

	TreeMap<String, Double> sumTEPPerYear = new TreeMap<String, Double>(); //key: classname_year

	TreeMap<String, double[][]> numEggsPerAge = new TreeMap<String, double[][]>(); //key: classname, value: avgEggs[age][numberType], where numberType: 0=avgEggsPerAge, 1=totalObservations 
	TreeMap<String, double[]> numSpawnsPerSeasonPerAge = new TreeMap<String, double[]>(); //key: classname_age_year, value: numSpawns[indivualHashCode] 

	ArrayList<ABEMCell> activeCells; 

	PrintWriter outGlobalSumsFile;
	PrintWriter outSettlersPerCell;
	PrintWriter outGlobalSpawnStatsFile;

	NetcdfFileWriteable ncFile; 

	final String abundVarName = "abundance";
	final String biomVarName = "biomass";
	final String SSBVarName = "SSB";
	final String TEPVarName = "TEP";
	final String recruitVarName = "recruitment"; 
	final String settlerVarName = "settlers"; 
	final String adultVarName = "adults"; 

	//final String SSB2TEPVarName = "SSB:TEP";
	final String latVarName = "lat";
	final String lonVarName = "lon";
	final String timeVarName = "time";

	int timeIndex = 0; //keeps track of timeIndex

	int yearOfLastRun = 0; //sets equal to current year at end of run method to 

	int[] origin = new int[3];
	int[] shape = {1,1,1};

	long lastOutput;

	double[] buncesMovers = new double[2];


	public FishTracker() {

		//create the Global Sums file outputter to text file
		if (SchedulerParams.outputFishSumsToFile){

			new File(SchedulerParams.outputFishSumsFilename).delete();
			try { 
				outGlobalSumsFile= new PrintWriter(
						new FileWriter(SchedulerParams.outputFishSumsFilename, true));
			} catch (IOException e) {e.printStackTrace();}
		}

		//create the Spatial Sums file outputter to netCDF file
		if (SchedulerParams.outputFishSpatialSumsToFile){
			new File(SchedulerParams.outputFishSpatialSumsFilename).delete();

			try {
				ncFile = createNetCDFFile(SchedulerParams.outputFishSpatialSumsFilename);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidRangeException e) {
				e.printStackTrace();
			} 
		}

		if (SchedulerParams.outputGlobalFishSpawnStats) {
			new File(SchedulerParams.outputGlobalFishSpawnStatsFilename).delete();
			try { 
				outGlobalSpawnStatsFile= new PrintWriter(
						new FileWriter(SchedulerParams.outputGlobalFishSpawnStatsFilename, true));
			} catch (IOException e) {e.printStackTrace();}

		}

		lastOutput = System.currentTimeMillis();
	}





	@Override
	public void run() {

		// clear at the beginning of step since will recalculate each
		globalSums.clear();  
		spatialSums.clear();
		sizeFreq.clear(); 

		double avgSchoolSize = 0; 





		//####################################
		// loop through each monitor and set sums
		//####################################

		for (int i=0; i<monitorees.size(); i++){
			Fish animal = (Fish) monitorees.get(i); 
			avgSchoolSize += animal.getGroupAbundance();


			setSumMap(animal);  
			setSizeFreqMap(animal); 


			// for each monitoree, add a new run time to their queue which 
			//is the next time this monitor will fire
			monitorees.get(i).addTimeToRunQueue(this.timesToRunQueue.get(0)); 
		}





		//####################################
		//prep NetCDF spatial sums file
		//####################################
		//write the time variable to file
		if (SchedulerParams.outputFishSpatialSumsToFile){
			int[] shape = new int[]{1}; 
			double[] timeValue = new double[] {
					TimeUtils.getHoursSinceTidalEpoch(scheduler.getCurrentDate())};
			Array array = Array.factory(double.class, shape, timeValue); 
			int[] origin = new int[]{timeIndex}; 
			try {
				ncFile.write(timeVarName, origin, array);
			} catch (IOException e) {e.printStackTrace();} 
			catch (InvalidRangeException e) {e.printStackTrace();} 
		}






		//####################################
		//copy the spatial data to ABEMCells
		//####################################

		for(String key : spatialSums.keySet()) { 
			double[] data = spatialSums.get(key);


			String[] tokens = key.split("_"); 
			Int3D index = new Int3D(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));

			ABEMCell cell = (ABEMCell) scheduler.getGrid().getGridCell(index);

			int year = scheduler.getCurrentDate().get(Calendar.YEAR);
			cell.setAvgNumbers(tokens[0], year, data);

			if (SchedulerParams.outputFishSpatialSumsToFile && !cell.isActive()) {
				if (activeCells == null ) activeCells = new ArrayList<ABEMCell>();
				if (!activeCells.contains(cell)) activeCells.add(cell);
				cell.setIsActive(true);
			}


		} //end of for loop over all abem cells





		//####################################
		//average the size frequencies
		//####################################

		for(String className : sizeFreq.keySet()) { 
			double[] sizeFreqData = sizeFreq.get(className);
			double totalNum = globalSums.get(className)[0];

			for (int i=0; i<sizeFreqData.length; i++) {
				sizeFreqData[i] = sizeFreqData[i]/totalNum;
			}
		}





		//####################################
		//Global sums file output
		//####################################

		if (SchedulerParams.outputFishSumsToFile){
			//DATE   ABUND   BIOMASS   SSB   TEP   RECRUIT  SF[0]    ….  SF[MAX AGE CLASS]

			int year = scheduler.getCurrentDate().get(Calendar.YEAR);

			for (String key: globalSums.keySet()){
				outGlobalSumsFile.print(new Date(this.currentTime).toString() 
						+ "\t" + key + "\t" + ((int) globalSums.get(key)[0]) 
						+ "\t" + df.format(globalSums.get(key)[1])
						+ "\t" + df.format(globalSums.get(key)[2]) 
						+ "\t");

				String yearlyKey = key + "_" + year;

				outGlobalSumsFile.print(this.sumTEPPerYear.get(yearlyKey) 
						+ "\t" + this.sumRecruitsPerYear.get(yearlyKey) 
						+ "\t");

				//print out the size frequency distribution
				double[] sizeFreqData = sizeFreq.get(key);
				for (int i=0; i<sizeFreqData.length; i++) {
					outGlobalSumsFile.print(df.format(sizeFreqData[i]) 
							+ "\t"); 
				}
				outGlobalSumsFile.println(); 
			}
		} //end of if (outputFishSum)






		/*TODO -- need to set up NetCDF to accomodate more species
		 * 	-- will need to create a set of variables for each species, e.g.,
		 * 		SeatroutSSB
		 * 		SeatroutBiom
		 * 		SeatroutTEP 
		 * 		....
		 * 		RedfishSSB
		 * 		...
		 */





		//####################################
		//Spatial netCDF output
		//####################################
		if (SchedulerParams.outputFishSpatialSumsToFile){

			//only iterate over the active cells that have has some values set or previously set 
			for (ABEMCell cell: activeCells){

				int year = scheduler.getCurrentDate().get(Calendar.YEAR);

				Array abundArray = null; 
				double abund = 
					cell.getAbundance("Seatrout");
				//				if (abund != 0){
				abundArray = 
					Array.factory(double.class, shape, 
							new double[]{abund});
				//				}

				Array biomArray = null;
				double biom = 
					cell.getBiomass("Seatrout");
				//				if (biom != 0){
				biomArray = 
					Array.factory(double.class, shape, 
							new double[]{biom});
				//				}

				Array ssbArray = null;
				double ssb = 
					cell.getSSB("Seatrout");
				//				if (ssb != 0){
				ssbArray = 
					Array.factory(double.class, shape, 
							new double[]{ssb});
				//				}

				Array tepArray = null;
				double tep = 
					cell.getTEP("Seatrout", year); 
				//				if (tep != 0){
				tepArray = 
					Array.factory(double.class, shape, 
							new double[]{tep});
				//				}

				Array recruitArray = null;
				double recruit = 
					cell.getNumRecruits("Seatrout", year);
				//				if (recruit != 0) {
				recruitArray = 
					Array.factory(double.class, shape, 
							new double[]{recruit});
				//				}

				Array settlerArray = null;
				double settlers = 
					cell.getNumSettlers("Seatrout");
				//				if (settlers != 0) {
				settlerArray = 
					Array.factory(double.class, shape, 
							new double[]{settlers});
				//				}

				Array adultArray = null;
				double adults = 
					cell.getNumAdults("Seatrout");
				//					if (settlers != 0) {
				adultArray = 
					Array.factory(double.class, shape, 
							new double[]{adults});
				//					}

				Array time = Array.factory(double.class, new int[] {1}, 
						new double[] {TimeUtils.getHoursSinceTidalEpoch(scheduler.getCurrentDate())});

				origin[0] = timeIndex;
				origin[1] = cell.getIndex().y;
				origin[2] = cell.getIndex().x;

				try {

					//write the time
					ncFile.write(timeVarName, new int[] {timeIndex}, time); 

					//write the data
					if (abundArray != null) ncFile.write(abundVarName, origin, abundArray);
					if (biomArray != null) ncFile.write(biomVarName, origin, biomArray);
					if (ssbArray != null) ncFile.write(SSBVarName, origin, ssbArray);
					if (tepArray != null) ncFile.write(TEPVarName, origin, tepArray);
					if (recruitArray != null) ncFile.write(recruitVarName, origin, recruitArray);
					if (settlerArray != null) {
						ncFile.write(settlerVarName, origin, settlerArray);
					}
					if (adultArray != null) {
						ncFile.write(adultVarName, origin, adultArray);
					}
				} catch (IOException e) {e.printStackTrace();} 
				catch (InvalidRangeException e) {e.printStackTrace();}

			} //end of loop over all ABEMCells

			timeIndex++; 

		} //end of check for outputing fish spatial data


		//####################################
		//console output
		//####################################

		avgSchoolSize = avgSchoolSize/monitorees.size();

		if (SchedulerParams.outputFishSumsToConsole){


			for (String key: globalSums.keySet()){
				System.out.println(new Date(this.currentTime).toString() 
						+ "\t" + key + " sums (abund, biomass, SSB):\t" 
						+ ((int) globalSums.get(key)[0]) + "\t" + df.format(globalSums.get(key)[1])  
						+ "\t" + df.format(globalSums.get(key)[2])  + "\t\tnum agents: " 
						+ monitorees.size() + "\tavg school size: " + df.format(avgSchoolSize) 
						+ "\t\trunTime: " + TimeConvert.millisToString(System.currentTimeMillis()-lastOutput)
				);
			}
			lastOutput = System.currentTimeMillis();
		} //end of if (outputFishSum)


		yearOfLastRun = scheduler.getCurrentDate().get(Calendar.YEAR);

	} //end or run() method






	/**Sets the sum map.
	 * 
	 * @param className
	 * @param biomass
	 * @param abundance
	 */
	public void setSumMap(Fish animal) { 
		boolean spawner = false; 
		boolean isSettler = animal.isSettler();


		//Note: SSB IS NOT the biomass of only those spawning; it is the biomass of individuals
		//that are in the age class at which 50% are mature
		//there, all individuals in age class 1 will be considered spawners
		/*		if (animal.getParams().getLength(animal.getGroupBiomass()
				/(double)animal.getGroupAbundance()) - animal.getSizeAtMaturity() > 0 
				&& animal.getGroupSex() == 0){
		 */
		if (animal.getYearClass() > 0)
			spawner = true;


		//(1): Set the non-spatial sums
		if (globalSums.containsKey(animal.getClassName())) {
			// update existing biomass value for className class 
			double[] data = globalSums.get(animal.getClassName());
			data[0] += animal.getGroupAbundance(); 
			data[1] += animal.getGroupBiomass(); 
			if (spawner) data[2]+= animal.getGroupBiomass(); 
			if (isSettler) data[3] +=animal.getGroupAbundance();
			else data[4] +=animal.getGroupAbundance();
		}
		else {
			// add new map key value with appropriate biomass
			double[] data = new double[5];
			data[0] = animal.getGroupAbundance(); 
			data[1] = animal.getGroupBiomass(); 
			if (spawner) data[2] = animal.getGroupBiomass();
			if (isSettler) data[3] =animal.getGroupAbundance();
			else data[4] =animal.getGroupAbundance();

			globalSums.put(animal.getClassName(), data); 
		}



		//(2): Set the spatial sums
		Int3D index = animal.getGridIndex(); 
		String key = animal.getClassName() + "_" + index.x + "_" + index.y;
		if (spatialSums.containsKey(key) ) {
			// update existing biomass value for className class 
			double[] data = spatialSums.get(key);
			data[0] += animal.getGroupAbundance(); 
			data[1] += animal.getGroupBiomass(); 
			if (spawner) data[2]+= animal.getGroupBiomass(); 
			if (isSettler) data[3] +=animal.getGroupAbundance();
			else data[4] +=animal.getGroupAbundance();

		}
		else {
			// add new map key value with appropriate biomass
			/*
			 * data[0] = abund, data[1] = biom, data[2] = ssb
			 * data[3] = numSettlers, data[4] = numAdults
			 */
			double[] data = new double[5];
			data[0] = animal.getGroupAbundance(); 
			data[1] = animal.getGroupBiomass(); 
			if (spawner) data[2] = animal.getGroupBiomass(); 
			if (isSettler) data[3] =animal.getGroupAbundance();
			else data[4] =animal.getGroupAbundance();
			spatialSums.put(key, data); 
		}


	}






	/**Sets the size frequency map.
	 * 
	 * @param className
	 * @param biomass
	 * @param abundance
	 */
	public void setSizeFreqMap(Fish animal) { 
		if (sizeFreq.containsKey(animal.getClassName())) {
			// update existing biomass value for className class 
			sizeFreq.get(animal.getClassName())[animal.getYearClass()] += 
				animal.getGroupAbundance();
		}
		else {
			// add new map key value with appropriate biomass
			//here, add 1 to include 0-agemax (+1)
			double[] data = new double[animal.getParams().getAgeMax() + 1];  
			sizeFreq.put(animal.getClassName(), data);
			sizeFreq.get(animal.getClassName())[animal.getYearClass()] += 
				animal.getGroupAbundance();
		}
	}







	public void addRecruit(OrgDatagram data){


		//(1) Set for total recruitSums
		int year = scheduler.getCurrentDate().get(Calendar.YEAR)  ;

		String key = data.getClassName()+ "_" + year;
		if (sumRecruitsPerYear.containsKey(key)) {
			double val = sumRecruitsPerYear.get(key).doubleValue() + data.getGroupAbundance();
			sumRecruitsPerYear.put(key, val);
		}
		else {
			// add new map key value with appropriate biomass
			sumRecruitsPerYear.put(key, (double) data.getGroupAbundance()); 
		}


		//(2) set the number of recruits in the ABEMCell so have spatial data on recruitment
		scheduler.getGrid().getGridCell(data.getGridCellIndex()).setNumRecruits(
				data.getClassName(), year, data.getGroupAbundance());
	}





	public void addTEP(Organism t, double numParts, Int3D gridIndex) {


		String classname = t.getClassName();
		//(1) Set total TEP
		int year = scheduler.getCurrentDate().get(Calendar.YEAR)  ;

		String key = classname + "_" + year;
		if (sumTEPPerYear.containsKey(key)) {
			double val = sumTEPPerYear.get(key) + numParts;
			sumTEPPerYear.put(key, val); 
		}
		else {
			// add new map key value with appropriate biomass
			sumTEPPerYear.put(key, numParts); 
		}

		//(2) set the number of recruits in the ABEMCell so have spatial data on recruitment
		scheduler.getGrid().getGridCell(gridIndex).setTEP(classname, year, numParts);


		//(3) set the average spatial stats -- num eggs per age class, and number of spawns per season
		//		TreeMap<String, double[][]> numEggsPerAge = new TreeMap<String, double[][]>(); //key: classname, value: avgEggs[age][numberType], where numberType: 0=avgEggsPerAge, 1=totalObservations 
		//		TreeMap<String, double[]> numSpawnsPerSeasonPerAge = new TreeMap<String, double[]>(); //key: classname_age_year, value: numSpawns[indivualHashCode]
		String numEggsKey = classname;
		if (numEggsPerAge.containsKey(numEggsKey)) {
			double[][] numEggs = numEggsPerAge.get(numEggsKey); 
			numEggs[t.getYearClass()][0] += numParts;
			numEggs[t.getYearClass()][1] ++;
		}
		else {
			double[][] numEggs = new double[t.getParams().getAgeMax() + 1][2];
			numEggs[t.getYearClass()][0] = numParts;
			numEggs[t.getYearClass()][1] = 1;
			// add new map key value with appropriate biomass
			numEggsPerAge.put(numEggsKey, numEggs); 
		}



		String numSpawnsKey = classname + "_" + t.getYearClass() + "_" + year;

		if (numSpawnsPerSeasonPerAge.containsKey(numSpawnsKey)) {
			double[][] numEggs = numEggsPerAge.get(numEggsKey); 
			numEggs[t.getYearClass()][0] += numParts;
			numEggs[t.getYearClass()][1] ++;
		}
		else {
			double[][] numEggs = new double[t.getParams().getAgeMax() + 1][2];
			numEggs[t.getYearClass()][0] = numParts;
			numEggs[t.getYearClass()][1] = 1;
			// add new map key value with appropriate biomass
			numEggsPerAge.put(numEggsKey, numEggs); 
		}


	}







	public NetcdfFileWriteable createNetCDFFile(String ncFilename) 
	throws IOException, InvalidRangeException{

		new File(ncFilename).delete();

		NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(ncFilename);

		NetCDFFile ncInput = new NetCDFFile(SchedulerParams.bathNetCDFFileName); 
		ncInput.setVariables("lat", "lon"); 


		int latSize = ncInput.getSingleDimension(latVarName);
		int lonSize = ncInput.getSingleDimension(lonVarName);
		int timeSize = 1; //temporary -- is unlimited variable


		Dimension[] dim = new Dimension[3];

		String timeUnits = "hours since tidal epoch 1983-01-01 00:00:0.0 GMT";
		String latUnits = "degrees_north";
		String lonUnits = "degrees_east";

		short missingVal = -9999; 

		dim[0] = setDimension(ncFile, DataType.DOUBLE, timeVarName, timeUnits, timeSize, true);
		dim[1] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latSize, false);
		dim[2] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonSize, false);

		ncFile.addVariableAttribute(timeVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(timeVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(latVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(latVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(lonVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(lonVarName, "missing_value", (double) missingVal);

		ncFile.addVariable(abundVarName, DataType.DOUBLE, dim);
		ncFile.addVariable(biomVarName, DataType.DOUBLE, dim);
		ncFile.addVariable(SSBVarName, DataType.DOUBLE, dim);
		ncFile.addVariable(TEPVarName, DataType.DOUBLE, dim);
		ncFile.addVariable(recruitVarName, DataType.DOUBLE, dim);
		ncFile.addVariable(settlerVarName, DataType.DOUBLE, dim);
		ncFile.addVariable(adultVarName, DataType.DOUBLE, dim);

		ncFile.addVariableAttribute(abundVarName, "units", "number of current individuals");
		ncFile.addVariableAttribute(biomVarName, "units", "grams of current biomass");
		ncFile.addVariableAttribute(SSBVarName, "units", "grams of current SSB");
		ncFile.addVariableAttribute(TEPVarName, "units", "sum of viable eggs per cell");
		ncFile.addVariableAttribute(recruitVarName, "units", "sum of recruits per cell");
		ncFile.addVariableAttribute(settlerVarName, "units", "number of current settlers");
		ncFile.addVariableAttribute(adultVarName, "units", "number of current adults");


		//NOTE: DON'T USE SCALE FACTOR!!!!!! -- scale factor is supposed to be multipled 
		//by the value when data is accessed from netCDF, and this is done in the NetCDFFile 
		//utility class
		ncFile.addVariableAttribute(abundVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(abundVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(biomVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(biomVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(SSBVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(SSBVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(TEPVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(TEPVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(recruitVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(recruitVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(settlerVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(settlerVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(adultVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(adultVarName, "missing_value", (double) missingVal);

		ncFile.create();

		//write out the lon/lat dimensions
		ArrayDouble.D1 dataLon = (ArrayDouble.D1) ncInput.getArray(lonVarName); 
		ArrayDouble.D1 dataLat = (ArrayDouble.D1) ncInput.getArray(latVarName); 


		ncFile.write(lonVarName, dataLon);
		ncFile.write(latVarName, dataLat);

		return ncFile; 
	}






	private static Dimension setDimension(NetcdfFileWriteable ncFile,
			DataType type, String name, String units, int length, boolean isUnlimited) {

		Dimension dimension; 
		if (isUnlimited) {
			dimension = ncFile.addUnlimitedDimension(name);
		}
		else{
			dimension = ncFile.addDimension(name, length);
		}
		ncFile.addVariable(name, type, new Dimension[]{dimension});
		ncFile.addVariableAttribute(name, "units", units);


		return dimension;
	}







	/**Returns the total biomass of the given class
	 * in the model domain.
	 * 
	 * @param className
	 * @return
	 */

	public double getTotalBiomass(String className){
		return globalSums.get(className)[1]; 
	}







	/**Returns the total abundance of the given class
	 * in the model domain.
	 * 
	 * @param className
	 * @return
	 */
	public int getTotalAbundance(String className) {
		return (int) globalSums.get(className)[0];
	}





	/**Adds an ABEMCell to the active cell list
	 * 
	 * @param cell
	 */
	public void addActiveCell(ABEMCell cell){
		if (activeCells == null) activeCells = new ArrayList<ABEMCell>(); 
		if (!activeCells.contains(cell)) activeCells.add(cell);
	}



	/**Returns the list of active ABEM cells -- i.e., those that have some data recording
	 * values (e.g., recruitment, TEP, SSB, biomass) associated with them
	 * 
	 * @return active cells list
	 */
	public ArrayList<ABEMCell> getActiveCells(){
		return activeCells;	
	}


	public double[] getBuncesMovers() {
		return buncesMovers;
	}





	public void setBuncesMovers(double[] buncesMovers) {
		this.buncesMovers = buncesMovers;
	}





	public void closeFiles() {

		//TODO -- need to print all data files that are total summation of years



		System.out.println("% of movements to Bunces: " + buncesMovers[1]/buncesMovers[0]);

		if (outGlobalSumsFile != null) outGlobalSumsFile.close();

		if (outGlobalSpawnStatsFile != null) outGlobalSpawnStatsFile.close();

		if (ncFile != null)
			try {
				ncFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}



}
