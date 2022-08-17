package us.fl.state.fwc.abem.spawn;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import us.fl.state.fwc.abem.ThreadService;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.TextFileIO;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnModelBatchRunner {

	Object lock1 = new Object();

	DecimalFormat twoDForm = new DecimalFormat("#.##");

	long seed = System.currentTimeMillis(); 
	public MersenneTwister m = new MersenneTwister((int) seed); 
	public Uniform uniform= new Uniform(m); 


	SeatroutParams params = new SeatroutParams(); 
	SeatroutBuilderNonSpatial builder = new SeatroutBuilderNonSpatial() ; 

	String batchFileName = "data/SpawnModel_batchTest.txt";
//	String batchFileName = "data/SpawnModel_batchAllNew.txt";
//	String batchFileName = "data/SpawnModel_batchAll_1Stage.txt";
//	String batchFileName = "data/SpawnModel_batchShort_1Stage.txt";
//	String batchFileName = "data/SpawnModel_Resource.txt";
	
	
	//builder flags
	public boolean useVariableSize = true; // this controls if fish get a variable weight when first assigned
	public boolean useAssessmentGrowth = false;
	public boolean useResourceDepGrowth = false;
	public boolean useSizeDepMortality = false;
	
	//this uses a multiplier instead of a GAM intercept offset to adjust the probability
	public boolean useProbMult = false;				//false = defualt
	public boolean useSingleGAM = true;				//true = default
	public boolean useFixedDFs = false;				//false = defualt
	public boolean useNoZone5 = true;				//true = default run for the publication
	
	
	//output raw data
	public boolean outputRaw = false;
	public boolean outputCapProb = false; //this is to get spawning season 
	public boolean outputSeasonLength = true; 
	public boolean outputSpawnIntervalAvg = true; 
	public boolean outputAvgWeight = true;
	public boolean outputAvgMaturity= true;
	
	
	
	String globalAvgTEPOutName = "output/SpawnModel/GlobalTEPAvgs.txt";
	String globalAvgNumSpawnsOutName = "output/SpawnModel/GlobalNumSpawnsAvgs.txt";

	ThreadService service;



	ArrayList<SpawnModelNonSpatial> recycledModels = new ArrayList<SpawnModelNonSpatial>();

	/**
	 * Initializes the simulation to read in correct batch parameters
	 * @throws IOException
	 */
	public void runBatch() {

		String version = System.getProperty("java.version");
	    System.out.println("JDK version "+ version + " found");
	    
	    
		//run the list through ThreadService
		service = new ThreadService(8);

		long startTime = System.currentTimeMillis();

		Calendar currentTime = new GregorianCalendar(); 
		currentTime.setTimeInMillis(System.currentTimeMillis());
		System.out.println("Model start time: " + currentTime.getTime().toString());

		TextFileIO globalAvgTEPOut = new TextFileIO(globalAvgTEPOutName);
		TextFileIO globalAvgNumSpawnsOut = new TextFileIO(globalAvgNumSpawnsOutName);
		TextFileIO batch = new TextFileIO(batchFileName);

		//Print out headers
		globalAvgTEPOut.print("startYr"+"\t"+"SFYr"+"\t"+"abundYr"+"\t"+"FYr"+"\t"+"M" + "\t" + "gamSize"+"\t"+"gamInt"+"\t");
		globalAvgNumSpawnsOut.print("startYr"+"\t"+"SFYr"+"\t"+"abundYr"+"\t"+"FYr"+"\t"+"M" + "\t" + "gamSize"+"\t"+"gamInt"+"\t");

		if (useResourceDepGrowth) {
			globalAvgTEPOut.print("resource"+"\t");
			globalAvgNumSpawnsOut.print("resource"+"\t");
		}
		
		for (int ageClass=0; ageClass<=params.getAgeMax(); ageClass++){
			globalAvgTEPOut.print("age" + ageClass + "\t");
			globalAvgNumSpawnsOut.print("age" + ageClass + "\t");
		}

		globalAvgTEPOut.print("allAvg" + "\t" + "numSpawners");
		globalAvgNumSpawnsOut.print("allAvg"+ "\t" + "numSpawners");

		globalAvgTEPOut.print("\n");
		globalAvgNumSpawnsOut.print("\n");


		SpawnModelDatagram datagram = new SpawnModelDatagram();  
		//datagram.setM(m);
		//datagram.setParams(params);
		datagram.setBuilder(builder);
		datagram.setGlobalAvgTEPOut(globalAvgTEPOut);
		datagram.setGlobalAvgNumSpawnsOut(globalAvgNumSpawnsOut);

		try {

			//list or thread runnables to run through ThreadService
			List<Runnable> list = new ArrayList<Runnable>(); 

			int counter = 0; 

			for (String line = batch.readLine(); line != null; line = batch.readLine()) { 
				if (line.isEmpty() || line.contains("#") || line.contains("//")) continue; //ignore lines with comments
				String[] tokens = line.split("\t"); 

				datagram.setSeed(uniform.nextIntFromTo(0, Integer.MAX_VALUE));
				datagram.setStartYear(Integer.parseInt(tokens[0]));
				datagram.setSizeFreqYear(Integer.parseInt(tokens[1]));
				datagram.setAbundYear(Integer.parseInt(tokens[2]));
				datagram.setFishMortYear(Integer.parseInt(tokens[3]));
				datagram.setMortOn(Integer.parseInt(tokens[4]));
				datagram.setGamSizeFunc(tokens[5]);
				datagram.setGamInterceptOffset(Double.parseDouble(tokens[6]));
				datagram.setOutputPSpawn(Boolean.parseBoolean(tokens[7]));
				datagram.setOutputTEP(Boolean.parseBoolean(tokens[8]));
				datagram.setOutputSSB(Boolean.parseBoolean(tokens[9]));

				if (useResourceDepGrowth) datagram.setResource(Double.parseDouble(tokens[10]));

				
				SpawnModelNonSpatial model = recycleModel(null); 

				model.setRunner(this); //set the batch runner so can recycle model after run
				model.batchInitialize(datagram);

				//should bottleneck here on addTask waiting for Semaphore
				service.addTask(model);

				if (++counter % 8 == 0) System.gc();
			}



			/*
			//set the latch, i.e., total number to run
			service.setLatch(list.size());

			//run all batches
			for (Runnable runner: list){
				service.addTask(runner);
			}

			//await termination
			service.await(); 
			 */			

		} catch (NumberFormatException e) {
			e.printStackTrace();
		} finally {

			//this will await termination of threads
			service.shutdown();

			batch.close();
			globalAvgNumSpawnsOut.close();
			globalAvgTEPOut.close();

		}

		double runTime = (double) (System.currentTimeMillis() - startTime ) / (1000*60);
		System.out.println("\nTotal model run time (min): " + twoDForm.format(runTime));

	}


	public  SpawnModelNonSpatial recycleModel(SpawnModelNonSpatial model){
		synchronized (lock1) {
			if (model == null) {
				if (recycledModels.isEmpty()) return new SpawnModelNonSpatial();
				else return recycledModels.remove(0);
			}
			else {
				recycledModels.add(model);
			}

			return null;
		}
	}



	public static void main(String[] args){
		SpawnModelBatchRunner model = new SpawnModelBatchRunner();
		model.runBatch();
	}




}
