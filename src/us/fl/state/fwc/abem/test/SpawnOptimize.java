package us.fl.state.fwc.abem.test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javolution.util.FastTable;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnOptimize extends FitnessFunction {

	static int POP_SIZE = 10000;
	static int GENERATIONS = 100; 

	private static int loopCounter = 1; 
	static long seed = System.currentTimeMillis(); 
	static MersenneTwister64 m; // = new MersenneTwister64((int) seed); 
	static Uniform uniform; // = new Uniform(m); 
	static Normal normal; // = new Normal(0,1,m); 

	static Scheduler sched = new Scheduler(); 
	
	static  int numFish = 1000; 
	static  FastTable<SpawnOptiTrout> fishList = new FastTable<SpawnOptiTrout>(); 


	static  double[] numFishPerAge = new double[9]; 

	// size frequency for ages 0,1,2,3,4,5,6,7,8
	static public double[][] sizeFreq = { {0.02967033,	0.226373626,	0.323076923,	0.22967033,	0.110989011,	0.064835165,	0.00989011,	0.003296703,	0.002197802},
			{0.038565022,	0.234080717,	0.277130045,	0.219730942,	0.137219731,	0.069955157,	0.015246637,	0.007174888,	0.000896861} };


	static  public double[][] avgSizeAtAge = {	{24.52222222,	28.19757282,	31.18843537,	34.0215311,	36.36534653,	40.07627119,	44.5,	50.56666667,	48.75},
			{24.66046512,	30.31609195,	35.68640777,	40.76530612,	45.69542484,	50.16923077,	51.22941176,	54.675,	55.8}};

	static public double[][]stdSizeAtAge = {	{2.404536311,	3.107417394,	2.465904482,	2.586608551,	3.772411315,	4.012495095,	5.281571736,	2.122105872,	3.889087297},
			{2.383089185,	4.031115714,	3.327591712,	4.08389234,	4.571555935,	6.782891153,	4.944790782,	7.201140783,	0.}};



	//this is from Sue's MEPS master data
	//Observed data: 0=new, 1=1st, 2=full, 3=3rd
	static double[] propLunar = {0.228121927, 0.210914454,	0.322025565,	0.238938053	};

	//this is the proportion spawning per month, grouping all ages, from Sue's MEPS master data
	//static double[] propSpawnAgeAvg = {0,	0,	0.130952381,	0.298850575,	0.323308271,	0.24137931,	0.394366197,	0.679738562,	0.395833333,	0,	0,	0}; // this is from calculating the data on my own
	static double[] propSpawnAgeAvg = {0,	0,	0.124026457,	0.281983254,	0.310701546,	0.225854637,	0.338114348,	0.62924346,	0.315930935,	0,	0,	0}; // this is from digitizing Sue's data
	
	
	//i'm assumming this is from Sue's master data too, but I didn't see my calcs
	// first row: age, 2nd: month so is propSpawn[9][12]; only ages 1, 2, 3, 4, and 5 had enough data to be useful
	static double [][] propSpawn = {
			{0,0,0,0,0,0,0,0,0,0,0, 0}, // age0
			{ 0,	0,	0,	0,	0.136363636,	0.175,	0,	0.125, 	0.090909091,	0,	0,	0 }, //age1
			{0,	0,	0.102564103,	0.285714286,	0.320754717,	0.380952381,	0.391304348,	0.605263158,	0.285714286,	0,	0,	0 }, //age2
			{0,	0,	0.083333333,	0.333333333,	0.424242424,	0.153846154,	0.583333333,	0.714285714,	0.875	, 0,	0,	0 },//age3
			{0,	0,	0.25,	0.388888889,	0.461538462,	0.166666667,	0.692307692,	0.804878049,	1,	0,	0,	0 },//age4
			{0,	0,	0.5,	0.222222222,	0.333333333,	0,	0.4,	0.894736842,	0.5,	0,	0,	0}, //age5
			{0,0,0,0,0,0,0,0,0,0,0, 0},//age6
			{0,0,0,0,0,0,0,0,0,0,0, 0},//age7
			{0,0,0,0,0,0,0,0,0,0,0, 0}};//age8





	public static void main(String[] args) throws InvalidConfigurationException {

		SeatroutParams params = new SeatroutParams(); 
		
		m = sched.getM(); 
		uniform = sched.getUniform();
		normal = sched.getNormal(); 
		
		int numFemales = numFish; //(int) (numFish*propFemales);

		double[][] freq = new double[2][9]; 
		freq[1][0] = sizeFreq[1][0]; 
		freq[1][1] = freq[1][0] + sizeFreq[1][1]; 
		freq[1][2] = freq[1][1] + sizeFreq[1][2]; 
		freq[1][3] = freq[1][2] + sizeFreq[1][3]; 
		freq[1][4] = freq[1][3] + sizeFreq[1][4]; 
		freq[1][5] = freq[1][4] + sizeFreq[1][5]; 
		freq[1][6] = freq[1][5] + sizeFreq[1][6]; 
		freq[1][7] = freq[1][6] + sizeFreq[1][7]; 
		freq[1][8] = freq[1][7] + sizeFreq[1][8]; 
		
		
		for (int i =0; i<numFemales; i++){
			double prob = uniform.nextDoubleFromTo(0, 1); 
			int age=0; 
			if (prob < freq[1][0]) age=0;
			else if ( (prob >= freq[1][0] ) && (prob < freq[1][1])) age =1; 
			else if ( (prob >= freq[1][1] ) && (prob < freq[1][2])) age =2; 
			else if ( (prob >= freq[1][2] ) && (prob < freq[1][3])) age =3; 
			else if ( (prob >= freq[1][3] ) && (prob < freq[1][4])) age =4; 
			else if ( (prob >= freq[1][4] ) && (prob < freq[1][5])) age =5; 
			else if ( (prob >= freq[1][5] ) && (prob < freq[1][6])) age =6; 
			else if ( (prob >= freq[1][6] ) && (prob < freq[1][7])) age =7; 
			else  if (prob >=freq[1][7]) age = 8; 

			SpawnOptiTrout trout = new SpawnOptiTrout(sched, params); 
			double size = normal.nextDouble(avgSizeAtAge[1][age], stdSizeAtAge[1][age]); 
			double sizeMaturity = normal.nextDouble(trout.params.getSizeAtMaturityAvg(), trout.params.getSizeAtMaturitySD());
			trout.setAge(age);
			trout.setSizeMaturity(sizeMaturity);
			trout.setSex(true); 
			trout.setSize(size);
			trout.setBiomass(trout.params.getMassAtLength(size, 0)); 
			trout.setNominalBiomass(trout.params.getMassAtLength(size, 0)); 
			fishList.add(trout); 
			numFishPerAge[age]++; 
		}

		System.out.println("initialize complete");
//		for (int i=0; i< numFishPerAge.length; i++){
//			System.out.println("number of fish age " + i + ":\t" + numFishPerAge[i]); 
//		}

		{ // **** JGAP CODE BLOCK
			Configuration conf = new DefaultConfiguration();
			
			conf.setThreaded(true); // tells the system to run as multi-threaded; this should run CPUs+1 threads during fitness calculations; speed up per thread is minimal due to JGAP construction and my possible coding errors
			conf.getBestChromosomesSelector().setDoubletteChromosomesAllowed(false); // turns off allowing double values; in DefaultConfig is set to 'true'; Pop size isn't kept constant but it isn't evolving to a single population of clones either
			//conf.setOutputIndividuals(true); 
			conf.setPreservFittestIndividual(true);
			conf.setKeepPopulationSizeConstant(true);


			// For setting to non-default settings -- this had very rapid evoltion to avg fitness (~10 gen's)
			/*            conf.getGeneticOperators().clear();
            CrossoverOperator _xOver = new CrossoverOperator(conf,2,true);
            MutationOperator _mutation = new MutationOperator(conf,100); 
            conf.addGeneticOperator(_mutation);
            conf.addGeneticOperator(_xOver);
			 */

			//conf.setAlwaysCaculateFitness(true); 

			FitnessFunction myFunc = new SpawnOptimize();
			
			
			conf.setFitnessFunction( myFunc );


			/*	Variables to optimize:
			 * (1) peak1 time
			 * (2) peak2 time
			 * 	(3) peak1 stdev
			 * 	(4) peak2 stdev
			 * 	(5) peak1 size, but just for smaller peak; leave bigger peak = 1
			 */
			Gene[] sampleGenes = new Gene[ 5 ];
			
//			sampleGenes[0] = new IntegerGene(conf, 0, 29);  // lunar specificy
//			sampleGenes[1] = new DoubleGene(conf, 0, 100);  // lunar specificy
//			sampleGenes[2] = new IntegerGene(conf, 1, 15 );  // fallow period, avg
//			sampleGenes[3] = new DoubleGene(conf, 1, 5 );  // fallow period, stdev
			sampleGenes[0] = new IntegerGene(conf, 120, 150);  // peak1 time (from May1-May30)
			sampleGenes[1] = new IntegerGene(conf, 212, 242);  // peak2 time (from Aug1-Aug31)
			sampleGenes[2] = new DoubleGene(conf, 10, 50);  // peak1 stdev
			sampleGenes[3] = new DoubleGene(conf, 10, 50);  // peak2 stdev
			sampleGenes[4] = new DoubleGene(conf, 0.25, .9 );  // peak1 size relative to peak2

			Chromosome sampleChromosome = new Chromosome(conf, sampleGenes );

			conf.setSampleChromosome( sampleChromosome );

			// Finally, we need to tell the Configuration object how many Chromosomes we want in our population. The more Chromosomes,
			// the larger the number of potential solutions (which is good for finding the answer), but the longer it will take to evolve
			// the population each round. 
			conf.setPopulationSize( POP_SIZE );

			Genotype population = Genotype.randomInitialGenotype( conf );

			for( int i = 0; i < GENERATIONS; i++ ) {

				double avgFitness = 0; 

				population.evolve(); // method to do evolution

				// below code to get average fitness values for each generation to see how it increases
				Population pop = population.getPopulation(); 
				int size = pop.size(); 

				for (int j=0; j < size; j++){
					IChromosome eachChromosome = pop.getChromosome(j);
					avgFitness += eachChromosome.getFitnessValue(); 
				}

				avgFitness = avgFitness/((double) pop.size()); 

				IChromosome bestSolutionSoFar = population.getFittestChromosome();


				Integer peak1Class = (Integer) bestSolutionSoFar.getGene(0).getAllele();
				int peak1 = peak1Class.intValue(); 

				Integer peak2Class = (Integer) bestSolutionSoFar.getGene(1).getAllele();
				int peak2 = peak2Class.intValue(); 

				Double peak1SDClass  = (Double) bestSolutionSoFar.getGene(2).getAllele();
				double peak1SD = peak1SDClass.intValue(); 

				Double peak2SDClass  = (Double) bestSolutionSoFar.getGene(3).getAllele();
				double peak2SD = peak2SDClass.intValue(); 

				Double peak1SizeClass = (Double) bestSolutionSoFar.getGene(4).getAllele();
				double peak1Size = peak1SizeClass.doubleValue(); 


				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						+"\t"+ "avg fitness: \t" + avgFitness + "\tpeak1:\t" + peak1 +"\tpeak2:\t" + peak2+"\tpeak1SD:\t" + peak1SD + "\tpeak2SD:\t" + peak2SD + "\tpeak1Size:\t" + peak1Size);

				loopCounter++; 

			} // END EVOLVE loop through all generations

		} // **** END JGAP CODE BLOCK

	}


	/**Method for performing fitness evaluation.
	 * Needs to be full model for a single year, where it tests the fit to the data at the end of a year.
	 * Here, the size frequency distribution integrates across the full year, so can assume no growth 
	 * 		(i.e., at month 3, would actually be smaller sizes than at month 9; month 6 should be correct) 
	 * 
	 */
	public double evaluate( IChromosome a_subject ) {

		Calendar startDate  = new GregorianCalendar(2001, 0, 1) ; 
		Calendar currentDate = (Calendar) startDate.clone(); 

		int  peaks[] = new int[2]; 
		double peaksSD[] = new double[2]; 
		double peakSize[] = new double[2]; // relative 

		double[][] numSpawners = new double[9][365];  //% spawning per day of year out of total females
		double[] numLunarSpawners = new double[4]; // % spawning on each of 4 moon phases, out of the total spawners

//		System.out.println("fish list size: " + fishList.size())	; 
		double fitness = 0; 
		double fitPropSpawn = 0; 
		double fitLunarSpawn = 0; 


		Integer peak1Class = (Integer) a_subject.getGene(0).getAllele();
		int peak1 = peak1Class.intValue(); 

		Integer peak2Class = (Integer) a_subject.getGene(1).getAllele();
		int peak2 = peak2Class.intValue(); 

		Double peak1SDClass  = (Double) a_subject.getGene(2).getAllele();
		double peak1SD = peak1SDClass.intValue(); 

		Double peak2SDClass  = (Double) a_subject.getGene(3).getAllele();
		double peak2SD = peak2SDClass.intValue(); 

		Double peak1SizeClass = (Double) a_subject.getGene(4).getAllele();
		double peak1Size = peak1SizeClass.doubleValue(); 
		
		
		peaks[0] = peak1;
		peaks[1] = peak2; 
		peaksSD[0] = peak1SD; 
		peaksSD[1] = peak2SD; 
		peakSize[0] = peak1Size;
		peakSize[1] = 1;
		
		
		//set parameters for each fish
		for(int j = 0; j<fishList.size(); j++){ 
			SpawnOptiTrout trout = fishList.get(j); 
			trout.setParameters(peaks, peaksSD, peakSize) ;
		}

		
		//step through each day
		for (int i = 0; i<365; i ++) {
			
			for(int j = 0; j<fishList.size(); j++){ 
				
				SpawnOptiTrout trout = fishList.get(j); 
				
				if (trout.timeToSpawn(currentDate)) {

					numSpawners[(int) trout.getAge()][i]++; 
					//numLunarSpawners[trout.getMoonPhase()]++; 

				}
			}

			currentDate.add(Calendar.DAY_OF_YEAR, 1); 

		}

		//double[] propLunarSpawn = new double[4]; 
		double[][] propSpawnPerDay = new double[9][365]; 
//		double[][] propSpawnPerMonth = new double[9][12]; 
		double[] propSpawnPerMonthAgeAvg = new double[12]; 

		double[] numDaysPerMonth = new double[12]; 
		Calendar newDate = (GregorianCalendar) startDate.clone(); 
//		Calendar tempDate = null; 

		//int totalNumOfSpawnEvents = 0; 
		
		// sum total number spawning per month for each age group 
		for (int i=0; i<365; i++){
			for (int j=0; j<9; j++){
				if (numFishPerAge[j] == 0) propSpawnPerDay[j][i] = 0;  
				else propSpawnPerDay[j][i] = numSpawners[j][i]/numFishPerAge[j]; 
//				propSpawnPerMonth[j][ newDate.get(Calendar.MONTH) ] += propSpawnPerDay[j][i]	;
				propSpawnPerMonthAgeAvg[ newDate.get(Calendar.MONTH) ] += propSpawnPerDay[j][i]	;

				// for lunar calculation
				//totalNumOfSpawnEvents += numSpawners[j][i]; 
			}
			propSpawnPerMonthAgeAvg[newDate.get(Calendar.MONTH)] =propSpawnPerMonthAgeAvg[newDate.get(Calendar.MONTH)]/9;  //gets the average for each age
			numDaysPerMonth[newDate.get(Calendar.MONTH)] ++; 
			
//			tempDate = (GregorianCalendar) newDate.clone(); 
//			tempDate.add(Calendar.DAY_OF_YEAR, 1); 
//			if (tempDate.get(Calendar.MONTH) > newDate.get(Calendar.MONTH)) System.out.println("month: " + newDate.get(Calendar.MONTH) + "\tpropSpawnPerMonthAvgAge: " + propSpawnPerMonthAgeAvg[newDate.get(Calendar.MONTH)]	);

			newDate.add(Calendar.DAY_OF_YEAR, 1);

		}

		
		for (int i=0; i<12; i++){
			propSpawnPerMonthAgeAvg[i] = propSpawnPerMonthAgeAvg[i]/numDaysPerMonth[i];  // this would be average total per day
		}

		
		// calculate average % spawning per day for each month and age group
//		for (int i=0; i<12; i++){
//			for (int j=0; j<9; j++){
//				propSpawnPerMonth[j][i] = propSpawnPerMonth[j][i]/numDaysPerMonth[i];  // this would be average total per day
//			}
//		}
		
		
		//propLunarSpawn[0] = numLunarSpawners[0]/totalNumOfSpawnEvents;
	//	propLunarSpawn[1] = numLunarSpawners[1]/totalNumOfSpawnEvents;
	//	propLunarSpawn[2] = numLunarSpawners[2]/totalNumOfSpawnEvents;
	//	propLunarSpawn[3] = numLunarSpawners[3]/totalNumOfSpawnEvents;
		
//		for (int i=0; i<12; i++){
//			for (int j=1; j<6; j++){
//				fitPropSpawn += 1-Math.abs( propSpawnPerMonth[j][i] - propSpawn[j][i]) ; // do unweighted absolute difference since all between 0-1; don't want to standardize to observed 
//			}
//		}
//		fitPropSpawn = fitPropSpawn/(12*6);

		// max would be 12
		for (int i=0; i<12; i++){
			fitPropSpawn += 1-Math.abs( propSpawnPerMonthAgeAvg[i] - propSpawnAgeAvg[i]) ; // do unweighted absolute difference since all between 0-1; don't want to standardize to observed 
		}
		//fitPropSpawn = fitPropSpawn/12; 
		
//		for (int i=0; i<4; i++){
//				fitLunarSpawn += 1- Math.abs( propLunarSpawn[i] - propLunar[i]) ;
//		}		

//		fitLunarSpawn = fitLunarSpawn/4; 
		
//		fitness = (0.75*fitPropSpawn) + (0.25*fitLunarSpawn); // add a large constant to make positive 
		
		//System.out.println("fitPropSpawn: " + fitPropSpawn + "\tfitLunarSpawn: " + fitLunarSpawn + "\tfitness: " + fitness);
		return fitPropSpawn; 

	} // end evaluate()






}
