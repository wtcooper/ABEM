package us.fl.state.fwc.abem.spawn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.stat.StatUtils;

import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.TextFileIO;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnModelNonSpatial implements Runnable {

	DecimalFormat twoDForm = new DecimalFormat("#.##");

	public int startYear;

	private Calendar tempDate; 
	public Calendar currentDate; 

	public MersenneTwister64 m; 
	public Uniform uniform; 
	public Normal normal; 

	//map with key = age class and list of trout
	TreeMap<Integer, ArrayList<SpawnSeatrout>> fishList;

	public GAMModel_2Stage gam; 
	public GAMModel_1Stage gam1Stage;

	public SeatroutParams params; 

	double[][] numSpawners;
	double[][] numSurvivors;
	double[][] tep;
	double[][] ssb ;
	double[][] tepSize;
	double[][] ssbSize;


	//boolean to print outputs
	public boolean outPSpawn;
	public boolean outSSB ;
	public boolean outTEP ;

	String pSpawnOutFile ;
	String ssbOutFile ;
	String tepOutFile ;
	String rawOutFile; 
	String capProbOutFile; 
	String actProbOutFile; 
	String seasonLengthOutFile; 
	String spawnIntervalOutFile; 
	String avgWeightOutFile; 
	String avgMaturityOutFile; 
	String ssbSizeOutFile;
	String tepSizeOutFile;

	TextFileIO pSpawnOut; 
	TextFileIO ssbOut; 
	TextFileIO tepOut; 
	TextFileIO rawOut;
	TextFileIO capProbOut;
	TextFileIO actProbOut;
	TextFileIO seasonLengthOut;
	TextFileIO spawnIntervalOut;
	TextFileIO avgWeightOut;
	TextFileIO avgMaturityOut;
	TextFileIO ssbSizeOut;
	TextFileIO tepSizeOut;

	SeatroutBuilderNonSpatial builder; 

	int sizeFreqYear; 
	int fishMortYear;
	int abundYear;

	//synchronized output for global averages
	TextFileIO globalAvgTEPOut;
	TextFileIO globalAvgNumSpawnsOut;
	String[] idTokens; 

	String ID ;

	SpawnModelBatchRunner runner;

	boolean isMortOn; 

	public double probMult = 1;

	double resource=0; 
	double biomassPerArea=0;

	//String gamSizeFunc;

	public void batchInitialize(SpawnModelDatagram datagram){

		params = new SeatroutParams(); //datagram.getParams();
		builder = datagram.getBuilder(); 
		globalAvgTEPOut = datagram.getGlobalAvgTEPOut();
		globalAvgNumSpawnsOut = datagram.getGlobalAvgNumSpawnsOut();
		startYear = datagram.getStartYear();
		sizeFreqYear = datagram.getSizeFreqYear();
		abundYear = datagram.getAbundYear();
		fishMortYear = datagram.getFishMortYear();
		outPSpawn = datagram.isOutputPSpawn();
		outSSB = datagram.isOutputSSB();
		outTEP = datagram.isOutputTEP();
		if (runner.useResourceDepGrowth) resource = datagram.getResource();

		if (datagram.getMortOn() ==1) isMortOn = true;
		else isMortOn = false;

		String gamSizeFunc = datagram.getGamSizeFunc();
		double gamInterceptOffset = datagram.getGamInterceptOffset(); 

		ID = startYear+"_"+sizeFreqYear+"_"+abundYear+"_"+fishMortYear
				+"_"+datagram.getMortOn() + "_" + gamSizeFunc+"_"+gamInterceptOffset;
		if (runner.useResourceDepGrowth) ID += "_" + resource;

		idTokens = ID.split("_"); 

		//if offset is negative, then replace with 'neg' so can write the file
		if (gamInterceptOffset < 0) {
			ID.replace("-", "neg");
		}

		pSpawnOutFile = "output/SpawnModel/ProbSpawn/SpawnModel_pSpawn_" + ID;
		ssbOutFile = "output/SpawnModel/SSB/SpawnModel_SSB_" + ID;
		tepOutFile = "output/SpawnModel/TEP/SpawnModel_TEP_" + ID;
		ssbSizeOutFile = "output/SpawnModel/SSB/SpawnModel_SSB_Size_" + ID;
		tepSizeOutFile = "output/SpawnModel/TEP/SpawnModel_TEP_Size_" + ID;

		if (runner.outputRaw) rawOutFile = "output/SpawnModel/Raw/SpawnModel_Raw_" + ID;
		if (runner.outputCapProb) {
			capProbOutFile = "output/SpawnModel/Raw/SpawnModel_CapProb_" + ID;
			actProbOutFile = "output/SpawnModel/Raw/SpawnModel_ActProb_" + ID;
		}
		if (runner.outputSeasonLength){
			seasonLengthOutFile = "output/SpawnModel/Raw/SpawnModel_SeasonLength_" + ID;
		}
		if (runner.outputSpawnIntervalAvg){
			spawnIntervalOutFile="output/SpawnModel/Raw/SpawnModel_SpawnInterval_" + ID;
		}
		if (runner.outputAvgWeight) {
			avgWeightOutFile = "output/SpawnModel/Raw/SpawnModel_AvgWeight_" + ID;
		}
		if (runner.outputAvgMaturity) {
			avgMaturityOutFile = "output/SpawnModel/Raw/SpawnModel_AvgMaturity_" + ID;
		}

		//delete any existing
		new File(pSpawnOutFile).delete();
		new File(ssbOutFile).delete();
		new File(tepOutFile).delete();
		new File(ssbSizeOutFile).delete();
		new File(tepSizeOutFile).delete();


		//make new print writers
		if (outPSpawn) pSpawnOut= new TextFileIO(pSpawnOutFile);
		if (outSSB) {
			ssbOut= new TextFileIO(ssbOutFile);
			ssbSizeOut= new TextFileIO(ssbSizeOutFile);
		}
		if (outTEP) {
			tepOut= new TextFileIO(tepOutFile);
			tepSizeOut= new TextFileIO(tepSizeOutFile);
		}
		if (runner.outputRaw) rawOut = new TextFileIO(rawOutFile);
		if (runner.outputCapProb) {
			capProbOut = new TextFileIO(capProbOutFile);
			actProbOut = new TextFileIO(actProbOutFile);
		}
		if (runner.outputSeasonLength){
			seasonLengthOut= new TextFileIO(seasonLengthOutFile);
		}
		if (runner.outputSpawnIntervalAvg){
			spawnIntervalOut= new TextFileIO(spawnIntervalOutFile);
		}
		if (runner.outputAvgWeight) {
			avgWeightOut = new TextFileIO(avgWeightOutFile);
		}
		if (runner.outputAvgMaturity) {
			avgMaturityOut = new TextFileIO(avgMaturityOutFile);
		}

		int seed = datagram.getSeed();
		m = new MersenneTwister64((int) seed); 
		uniform= new Uniform(m); 
		normal= new Normal(0,1,m); 


		/*##########################################
		 * NOTE: using the SeatroutGAMModel JRI interface does
		 * not work inline -- threads don't spawn out for some reason.
		 * Also had problems with JRI botching up relative paths, where
		 * they read fine for instantiating a new File, but not for a 
		 * PrintWriter.  e.g., can do File file = new File("relative/Path"), then do
		 * TextFileIO out = new TextFileIO(file.getAbsolutePath())
		 */

		if (runner.useSingleGAM){
			gam1Stage = new GAMModel_1Stage();

			gam1Stage.monthInFile = "data/GAM_Month.txt";
			if (runner.useFixedDFs) gam1Stage.monthInFile = "data/GAM_MonthFixedDFs.txt";
			if (runner.useNoZone5) gam1Stage.monthInFile = "data/GAM_MonthNoZone5.txt";

			if (gamSizeFunc.equals("Mean") ){
				gam1Stage.TLInFile= "data/GAM_SizeEst.txt";
				if (runner.useFixedDFs) gam1Stage.TLInFile= "data/GAM_SizeFixedDFsEst.txt";
				if (runner.useNoZone5) gam1Stage.TLInFile = "data/GAM_SizeNoZone5Est.txt";
			}
			else if (gamSizeFunc.equals("Upper")){
				gam1Stage.TLInFile= "data/GAM_SizeHigh.txt";
				if (runner.useFixedDFs) gam1Stage.TLInFile= "data/GAM_SizeFixedDFsHigh.txt";
				if (runner.useNoZone5) gam1Stage.TLInFile = "data/GAM_SizeNoZone5High.txt";
			}
			else if (gamSizeFunc.equals("Lower")){
				gam1Stage.TLInFile = "data/GAM_SizeLow.txt";
				if (runner.useFixedDFs) gam1Stage.TLInFile = "data/GAM_SizeFixedDFsLow.txt"; 
				if (runner.useNoZone5) gam1Stage.TLInFile = "data/GAM_SizeNoZone5Low.txt";
			}
			else if (gamSizeFunc.equals("None")){
				gam1Stage.TLInFile = "data/GAM_SizeNone.txt";
			}
			else if (gamSizeFunc.equals("Asymptote")){
				gam1Stage.TLInFile = "data/GAM_SizeNoZone5_Asymptote.txt";
			}
			else if (gamSizeFunc.equals("Decrease")){
				gam1Stage.TLInFile = "data/GAM_SizeNoZone5_Decrease.txt";
			}
			
			
			else {
				System.out.println("INvalid gam size function!!");
				System.exit(0);
			}

			gam1Stage.initialize();

			if (runner.useProbMult) probMult = gamInterceptOffset;
			else gam1Stage.intercept += gamInterceptOffset;

		}

		else {

			gam = new GAMModel_2Stage(); 

			//set the funcation relationship for size and probability of spawning
			if (gamSizeFunc.equals("SuesData") || gamSizeFunc.equals("Mean")){
				//do nothing -- default is Sue's data fit
			}
			//		else if (gamSizeFunc.equals("Flat")){
			//			gam.capTLInFile = "data/GAM_capTL_flat.txt";
			//			gam.actTLInFile = "data/GAM_actTL_flat.txt";
			//		}
			else if (gamSizeFunc.equals("Peak")){
				gam.capTLInFile = "data/GAM_capTL_peak.txt";
				gam.actTLInFile = "data/GAM_actTL_peak.txt";
			}
			else if (gamSizeFunc.equals("Asymptote")){
				gam.capTLInFile = "data/GAM_capTL_asympt.txt";
				gam.actTLInFile = "data/GAM_actTL_asympt.txt";
			}
			else if (gamSizeFunc.equals("Upper")){
				gam.capTLInFile = "data/GAM_capTL_upper.txt";
				gam.actTLInFile = "data/GAM_actTL_upper.txt";
			}
			else if (gamSizeFunc.equals("Lower")){
				gam.capTLInFile = "data/GAM_capTL_lower.txt";
				gam.actTLInFile = "data/GAM_actTL_lower.txt";
			}
			else if (gamSizeFunc.equals("negLinear")){
				gam.capTLInFile = "data/GAM_capTL_negLinear.txt";
				gam.actTLInFile = "data/GAM_actTL_negLinear.txt";
			}
			else if (gamSizeFunc.equals("posLinear")){
				gam.capTLInFile = "data/GAM_capTL_posLinear.txt";
				gam.actTLInFile = "data/GAM_actTL_posLinear.txt";
			}
			else {
				System.out.println("INvalid gam size function!!");
				System.exit(0);
			}

			gam.initialize();

			//set the intercept for prob of spawning offset to account different spawning intervals

			//don't want to set spawning capable -- don't adjust spawn season -- just set 
			//those actually spawning -- will do the same thing, but do this so more proper
			//	gam.capIntercept += gamInterceptOffset;

			if (runner.useProbMult) probMult = gamInterceptOffset;
			else gam.actIntercept += gamInterceptOffset;

		}



		currentDate = new GregorianCalendar(startYear, 0, 1); 
		tempDate  = (Calendar) currentDate.clone(); 


	}








	public void run() {
		Thread t = Thread.currentThread();
		System.out.println(t.getName() + ": start " + ID );

		//long startTime = System.currentTimeMillis();




		//System.out.println("\t"+t.getName() + ": getting read to enter build" );
		fishList = builder.build(this); 
		//System.out.println("\t"+t.getName() + ": build complete.");

		//System.out.println("initialize complete");

		//number of days of simulation

		numSpawners = new double[fishList.size()][365];  //number spawning in an age class per day
		numSurvivors = new double[fishList.size()][365];  //number of survivors in an age class per day
		tep = new double[fishList.size()][365];  //number of survivors in an age class per day
		ssb = new double[fishList.size()][365];  //number of survivors in an age class per day
		//tepSize = new double[ ][365];  //number of survivors in an age class per day
		//ssbSize = new double[  ][365];  //number of survivors in an age class per day



		// Get the initial biomass for the entire population
		biomassPerArea = 0;
		for(int ageClass = 0; ageClass<fishList.size(); ageClass++){ 
			ArrayList<SpawnSeatrout> aList = fishList.get(ageClass);
			for (SpawnSeatrout trout: aList){
				biomassPerArea += trout.groupBiomass;
			}
		}

		if (builder.femalesOnly) biomassPerArea /= 0.595238237; //this is to add in some male biomass too -- ratio is based on DD calcs at 2003-2007 avgs
		//divide the total biomass by the total area to get per area, and by 1000 to convert to kg
		biomassPerArea /= 1000*params.getNumHectares();
		System.out.println(ID + "\tinitial biomass/ha:\t" + biomassPerArea);



		//#############################################
		//Model Run
		//#############################################

		//variables to get average capable probability for each day

		//step through each day
		for (int day=0; day<364; day++){

			double capProbAvg = 0;
			long capProbCount= 0;
			double[] capProbAgeAvg = new double[fishList.size()];
			long[] capProbAgeCount =  new long[fishList.size()];
			double[] actProbAgeAvg = new double[fishList.size()];
			long[] actProbAgeCount =  new long[fishList.size()];

			if (outPSpawn) pSpawnOut.print( (day+1) + "\t");
			if (outSSB) ssbOut.print( (day+1)  + "\t");
			if (outTEP) tepOut.print( (day+1) + "\t");
			if (outSSB) ssbSizeOut.print( (day+1)  + "\t");
			if (outTEP) tepSizeOut.print( (day+1) + "\t");

			//loop through age classes
			for(int ageClass = 0; ageClass<fishList.size(); ageClass++){ 

				ArrayList<SpawnSeatrout> aList = fishList.get(ageClass);

				//use iterator so can safely remove if trout dies
				Iterator<SpawnSeatrout> it = aList.iterator();
				while (it.hasNext()) {
					SpawnSeatrout trout = it.next();

					//Double mass = trout.groupBiomass;


					trout.setCurrentTime(currentDate);

					//only step trout that have been born already, OF COURSE
					if (trout.birthday > trout.currentTime) continue;


					//if dead, continue to next fish.
					//note: dont remove from list, because want to get data from dead ones 
					//too to get the avg tep
					if (trout.isDead) continue;

					//apply growth 1st since size is used in mortality and spawning 
					trout.growth();


					//if should apply mortality, apply mortality 
					if (isMortOn) trout.suffersMortality(); 




					tempDate.setTimeInMillis(trout.birthday);
					int yearOfBirth = tempDate.get(Calendar.YEAR);
					int cohort = currentDate.get(Calendar.YEAR)-yearOfBirth;

					if (trout.timeToSpawn(currentDate)) {

						//trout.lastSpawn = currentTime.getTimeInMillis();
						numSpawners[cohort][day]++; 

						tep[cohort][day] += trout.newNumSpawned; 
					}

					//increment the number of survivors for that day
					numSurvivors[cohort][day]+= trout.groupAbundance; 


					//ONly calculate capable prob for mature fish
					if (trout.mature){
						//SSB is used here as anything mature
						ssb[cohort][day] += trout.groupBiomass; 
						capProbAvg += trout.capableProb;
						capProbCount++;
						capProbAgeAvg[cohort] += trout.capableProb;
						capProbAgeCount[cohort]++;
						actProbAgeAvg[cohort] += trout.activeProb;
						actProbAgeCount[cohort]++;

					}

				} //end loop through each fish in an age class

				if (outPSpawn) pSpawnOut.print( (double)numSpawners[ageClass][day]/ (double) numSurvivors[ageClass][day] + "\t");
				if (outSSB) ssbOut.print( ssb[ageClass][day] + "\t");
				if (outTEP) tepOut.print( tep[ageClass][day] + "\t");
				//System.out.print( (double)numSpawners[j][i]/ (double) numSurvivors[j][i] + "\t");

			} //end loop through age classes

			if (outPSpawn) pSpawnOut.print("\n");
			if (outSSB) ssbOut.print("\n");
			if (outTEP) tepOut.print("\n");



			//######################
			//Reset biomass per area 
			//######################
			biomassPerArea = 0;
			for (int i=0; i<ssb.length; i++){
				biomassPerArea += ssb[i][day];
			}
			if (builder.femalesOnly) biomassPerArea /= 0.595238237; //this is to add in some male biomass too -- ratio is based on DD calcs at 2003-2007 avgs
			//divide the total biomass by the total area to get per area, and by 1000 to convert to kg
			biomassPerArea /= 1000*params.getNumHectares();




			//######################
			//Output Capable Prob and Active Probs 
			//######################


			//Output the capable prob is supposed to
			if (runner.outputCapProb ){
				String capProb=null;
				String actProb=null;
				capProbAvg /= (double) capProbCount;
				for (int i=0; i<fishList.size(); i++){
					capProbAgeAvg[i] /= (double) capProbAgeCount[i]; 
					capProb+=capProbAgeAvg[i] + "\t";
					actProbAgeAvg[i] /= (double) actProbAgeCount[i]; 
					actProb+=actProbAgeAvg[i] + "\t";
				}
				capProb+= capProbAvg;

				capProbOut.println(day + "\t" + capProb); 
				actProbOut.println(day + "\t" + actProb); 
			}



			//######################
			// Output raw data if needed
			//######################
			if (runner.outputRaw) {

				double month = currentDate.get(Calendar.MONTH)+1;
				month += (double) currentDate.get(Calendar.DAY_OF_MONTH) /
						(double) currentDate.getActualMaximum(Calendar.DAY_OF_MONTH);

				for(int ageClass = 0; ageClass<fishList.size(); ageClass++){ 

					ArrayList<SpawnSeatrout> aList = fishList.get(ageClass);

					int sampleSize = 20;
					if (aList.size() < sampleSize) sampleSize = aList.size();

					//pull samples at set intervals through list -- list has no specific order
					//so this doesn't need to be randomized
					int stride = (int) ((double) aList.size()/ (double) sampleSize);

					//get random individual with replacement
					for (int i=0; i<aList.size(); i += stride){

						SpawnSeatrout trout = aList.get(i);
						//only use trout that have been born already
						int counter = 0;
						while ( trout.birthday > trout.currentTime || trout.isDead) {
							trout =aList.get(uniform.nextIntFromTo(0, aList.size()-1));
							counter++;
							if (counter>50) break; //need to break for early on when all age 0's not born yet
						}

						int active = 0;
						if (trout.markerOn) active=1;

						//print out the proportion of active spawners in those sampled per day
						//ONLY print out those realistic individuals
						if (trout.TL > 0) rawOut.println(month + "\t" + trout.getAgeInDays()+"\t" + trout.TL + "\t" + trout.capableProb + "\t" + active);

					}

				}
			} //end output section




			//######################
			// Output average weight and/or maturity for each age class if needed
			//######################
			//if output the avg weight and it's the middle of the year, then output
			if ((runner.outputAvgWeight || runner.outputAvgMaturity) && currentDate.get(Calendar.DAY_OF_YEAR) == 182) {
				if (runner.outputAvgWeight) avgWeightOut.println("Age"+"\tAvgWeight" + "\tAvgTL");
				if (runner.outputAvgMaturity) avgMaturityOut.println("Age"+"\tAvgMaturity");

				for(int ageClass = 0; ageClass<fishList.size(); ageClass++){ 
					double averageWt=0;
					double averageTL=0;
					double averageMaturity=0;
					double averageAgeInDays=0;

					ArrayList<SpawnSeatrout> aList = fishList.get(ageClass);

					int counter=  0; 
					int counterMat=  0; 

					for (SpawnSeatrout trout: aList) {
						//this should be everything, because TL should be >= 0
						//junst need to make sure don't count those not born yet
						if (trout.birthday <= trout.currentTime){
							counter++;
							averageWt+= trout.groupBiomass;
							averageTL += trout.TL;
							averageAgeInDays+=trout.getAgeInDays();

							if (trout.mature) {
								counterMat++;
							}
						}

					}

					averageWt /= (double) counter;
					averageTL /= (double) counter;
					averageMaturity = (double) counterMat / (double) counter;
					averageAgeInDays /= (double) counter;
					if (runner.outputAvgWeight) avgWeightOut.println(ageClass + "\t" + averageWt + "\t" + averageTL);
					if (runner.outputAvgMaturity) avgMaturityOut.println(ageClass + "\t" + averageMaturity+ "\t" + averageAgeInDays + "\t"+averageTL);
				}
			}




			currentDate.add(Calendar.DAY_OF_YEAR, 1); 

		}//end while loop over days




		//#############################################
		//End-of-Season Model Output
		//#############################################
		//reloop through all age classes and fish, and calculate the 
		//average TEP per age class for Mike's model
		//loop through age classes
		double[] avgTEP = new double[fishList.size()];
		double[] avgNumSpawns = new double[fishList.size()];
		int[] cohortSize = new int[fishList.size()];

		//holds mean and st error on the season length per age class
		//double[][] seasonLengthStats = new double[fishList.size()][2]; 
		//double[] seasonLengthSum = new double[fishList.size()];
		double totalNumSpawns = 0; 
		double totalNumOfSpawners = 0; 
		double totalTEP = 0; 

		for(int j = 0; j<fishList.size(); j++){ 


			ArrayList<SpawnSeatrout> aList = fishList.get(j);

			ArrayList<Double> seasonLengths = new ArrayList<Double>();
			ArrayList<Double> spawnIntervals = new ArrayList<Double>();

			//use iterator so can safely remove if trout dies
			Iterator<SpawnSeatrout> it = aList.iterator();
			while (it.hasNext()) {
				SpawnSeatrout trout = it.next();
				//tempDate.setTimeInMillis(trout.birthday);
				//int yearOfBirth = tempDate.get(Calendar.YEAR);
				int cohort = j; //currentDate.get(Calendar.YEAR)-yearOfBirth;
				avgTEP[cohort] += trout.sumTEP;
				avgNumSpawns[cohort] += trout.numSpawns;
				if (trout.mature) cohortSize[cohort] ++;

				//get season length

				if (runner.outputSeasonLength && trout.mature && trout.numSpawns>1){
					seasonLengths.add((double) (trout.lastDayOfSpawn - trout.firstDayOfSpawn) );						
				}
				if (runner.outputSpawnIntervalAvg && trout.mature && trout.numSpawns>1){
					spawnIntervals.add((double) (trout.spawnIntervalSum/(double)(trout.numSpawns-1))); 						
				}

				totalTEP += trout.sumTEP;
				if (trout.mature) totalNumSpawns += trout.numSpawns;
				if (trout.mature) totalNumOfSpawners++;
			} //end loop over all fish in a cohort

			//if output season lengths, get the mean and variance for each cohort
			if (runner.outputSeasonLength ) {
				//Calculate season length statistics
				double[] seasonLengthsArray=ArrayUtils.toPrimitive(seasonLengths.toArray(new Double[seasonLengths.size()])); 
				double mean = StatUtils.mean(seasonLengthsArray);
				double variance = StatUtils.variance(seasonLengthsArray);

				//output mean + count + variance + stdev + sterr
				seasonLengthOut.println(j + "\t" + mean +"\t" + seasonLengthsArray.length + "\t" + variance + "\t" + Math.sqrt(variance) + "\t" + Math.sqrt(variance)/((double) seasonLengthsArray.length));
			}

			//if output spawn interval, get the mean and variance for each cohort
			if (runner.outputSpawnIntervalAvg) {
				//Calculate season length statistics
				double[] spawnIntervalArray=ArrayUtils.toPrimitive(spawnIntervals.toArray(new Double[spawnIntervals.size()])); 
				double mean = StatUtils.mean(spawnIntervalArray);
				double variance = StatUtils.variance(spawnIntervalArray);

				//output mean + count + variance + stdev + sterr
				spawnIntervalOut.println(j + "\t" + mean +"\t" + spawnIntervalArray.length + "\t" + variance + "\t" + Math.sqrt(variance) + "\t" + Math.sqrt(variance)/((double) spawnIntervalArray.length));
			}


		} // end loop over all cohorts

		//save to string first, then print all at once since is syncrhonized global printer

		String avgTEPString = null;
		for (String token: idTokens){
			if (avgTEPString == null) {
				avgTEPString = token + "\t";
				continue;
			}
			avgTEPString += token + "\t";
		}

		for(int j = 0; j<avgTEP.length; j++){ 
			avgTEP[j] /= cohortSize[j];
			avgTEPString += avgTEP[j] + "\t";
		}


		avgTEPString += totalTEP/totalNumOfSpawners + "\t"; //put sum total of all TEP at end

		//print out num spawners last
		avgTEPString += totalNumOfSpawners;

		globalAvgTEPOut.println(avgTEPString);



		String avgNumSpawnsString = null;
		for (String token: idTokens){
			if (avgNumSpawnsString == null) {
				avgNumSpawnsString = token + "\t";
				continue;
			}
			avgNumSpawnsString += token + "\t";
		}

		for(int j = 0; j<avgTEP.length; j++){ 
			avgNumSpawns[j] /= cohortSize[j];
			avgNumSpawnsString += twoDForm.format(avgNumSpawns[j]) + "\t";
		}


		totalNumSpawns /= totalNumOfSpawners;

		avgNumSpawnsString += twoDForm.format(totalNumSpawns) + "\t"; 

		//print out total num spawners last
		avgNumSpawnsString += totalNumOfSpawners;
		globalAvgNumSpawnsOut.println(avgNumSpawnsString); 

		System.out.println(avgNumSpawnsString);




		if (outPSpawn) pSpawnOut.close();
		if (outSSB) ssbOut.close();
		if (outTEP) tepOut.close();
		if (runner.outputRaw) rawOut.close();
		if (runner.outputCapProb) {
			capProbOut.close();
			actProbOut.close();
		}
		if (runner.outputSeasonLength) seasonLengthOut.close();
		if (runner.outputSpawnIntervalAvg) spawnIntervalOut.close();
		if (runner.outputAvgWeight) avgWeightOut.close();
		if (runner.outputAvgMaturity) avgMaturityOut.close();



		//double runTime = (double) (System.currentTimeMillis() - startTime ) / (1000*60);
		//System.out.println("Finished " + ID + ".  Runtime (min): " + twoDForm.format(runTime));



		//recycle the trout
		builder.recycleTrout(fishList);

		if (runner != null) runner.recycleModel(this);

	} 





	public void setRunner(SpawnModelBatchRunner runner) {
		this.runner = runner;
	}






	public static void main(String[] args){
		SpawnModelNonSpatial model = new SpawnModelNonSpatial();
		model.run();
	}


}


