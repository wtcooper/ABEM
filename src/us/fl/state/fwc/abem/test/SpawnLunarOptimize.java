package us.fl.state.fwc.abem.test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.TimeZone;

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

import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class SpawnLunarOptimize extends FitnessFunction {

	static int POP_SIZE = 10000;
	static int GENERATIONS = 250; 

	static private Calendar startDate  = new GregorianCalendar(TimeZone.getTimeZone("GMT")) ; 
	static private Calendar currentDate ; 


	private static int loopCounter = 1; 
	static long seed = System.currentTimeMillis(); 
	static MersenneTwister m = new MersenneTwister((int) seed); 
	static Uniform uniform = new Uniform(m); 
	static Normal normal = new Normal(0,1,m); 

	//	static int[] days = new int[90]; 
	static float[] probs = new float[366]; 


	public static void main(String[] args) throws InvalidConfigurationException {

		inputData();

		{ // **** JGAP CODE BLOCK
			Configuration conf = new DefaultConfiguration();

			conf.setPreservFittestIndividual(true);
			conf.setKeepPopulationSizeConstant(true);


			// For setting to non-default settings -- this had very rapid evoltion to avg fitness (~10 gen's)
			/*			 conf.getGeneticOperators().clear();
            CrossoverOperator _xOver = new CrossoverOperator(conf,2,true);
            MutationOperator _mutation = new MutationOperator(conf,100); 
            conf.addGeneticOperator(_mutation);
            conf.addGeneticOperator(_xOver);
			 */
			//conf.setAlwaysCaculateFitness(true); 

			FitnessFunction myFunc = new SpawnLunarOptimize();


			conf.setFitnessFunction( myFunc );


			/*	Variables to optimize:
			 * 	(1) peak spawn1
			 * 	(2) peak spawn2
			 * 	(2) peak SD (both)
			 */
			Gene[] sampleGenes = new Gene[ 1 ];

			sampleGenes[0] = new DoubleGene(conf, 0, 7);  // peak spawn SD
//			sampleGenes[1] = new IntegerGene(conf, 0, 14);  // peak spawn 1 (new moon)
	//		sampleGenes[2] = new IntegerGene(conf, 0, 14);  // peak spawn 1 (new moon)


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


				Double peakSDClass = (Double) bestSolutionSoFar.getGene(0).getAllele();
				double peakSD = peakSDClass.doubleValue(); 

				/*				Integer peak1Class = (Integer) bestSolutionSoFar.getGene(1).getAllele();
				int peak1 = peak1Class.intValue(); 

				Integer peak2Class = (Integer) bestSolutionSoFar.getGene(2).getAllele();
				int peak2 = peak2Class.intValue(); 
*/



				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						+"\t"+ "avg fitness: \t" + avgFitness + /* "\tpeak1: " + peak1 + "\tpeak2: " + peak2 + */ "\tpeakSD: " + peakSD);

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

		startDate.set(2006, 0, 1, 0, 0, 0);  
		currentDate = (GregorianCalendar) startDate.clone(); 

		double fitness = 0; 


		Double peakSDClass = (Double) a_subject.getGene(0).getAllele();
		double peakSD = peakSDClass.doubleValue(); 

/*		Integer peak1Class = (Integer) a_subject.getGene(1).getAllele();
		int peak1 = peak1Class.intValue(); 

		Integer peak2Class = (Integer) a_subject.getGene(2).getAllele();
		int peak2 = peak2Class.intValue(); 
*/

		double[] simProbs = new double[365];   
		//step through each day

		for (int i = 1; i<366; i ++) {

			if (probs[i] != 0){ // only do if there's a measurement for that day

				simProbs[i] =bimodalLunarGood(peakSD); 
		//		System.out.println("day of year: " + currentDate.get(Calendar.DAY_OF_YEAR) + "\tlunar phase: " + TimeUtils.getMoonPhase(currentDate) + "\tlunar prob: " + simProbs[i]	); 
/*					if (bimodalLunarGood(peakSD)) {
						simProbs[i]++; // add 1 to the observedProbs; will later devide by 1000
					}
				simProbs[i] = simProbs[i]/1000; //devide by 1000 to get probability 
*/
				fitness += (1-Math.abs(( (simProbs[i]  - probs[i]  )/ probs[i] )));  

			}
			currentDate.add(Calendar.DAY_OF_YEAR, 1); 
		}	

		// calculate the fitness as the difference between the calculated daily lunar prob of spawning and the measured prob of spawning

		return fitness; 

	} // end evaluate()




	/**	Here, this will calculate a bimodal probability of spawning.  It can only handle 2 peaks in spawning based on the lunar period, and they need to be evenly spaced 
	 * (i.e., on full and new moon, or a singular shift in days from either). If not on full/new, then need to adjust the "peakShift" parameter which is the shift in days from the 
	 * new/full on which the peaks occur.  I.e., a peakShift=2 will lead to peak spawning on 2 days after both the full and new moon.  This is what the data in seatrout support.   
	 * 
	 * @param peakSD
	 * @return
	 */
	public double bimodalLunarGood(/*int peak1, int peak2, */double peakSD){

		
		
		double lunarProb=0; 
		Uniform uniform = new Uniform(m); 

		int lunarPeaks[] = {0, 14}; // don't change this -- this needs to stay as 0 and 14;  
		double lunarPeaksSD[] = {peakSD, peakSD}; 
		double lunarPeakSize[] = {1, 1}; // relative 
		int peakShift = 2; 

		FastTable<Normal> lunarNormals = new FastTable<Normal>();  
		for (int i=0; i<lunarPeaks.length; i++){
			lunarNormals.add(new Normal(lunarPeaks[i], lunarPeaksSD[i], m));
		}

		//		currentDate.add(Calendar.DAY_OF_YEAR, 0); 

		int lunarPhase = TimeUtils.getMoonPhase(currentDate);
		int lunarAdjust = lunarPhase - peakShift;
		if (lunarAdjust < 0) lunarAdjust = 28+lunarAdjust; 
		if (lunarAdjust > 14) lunarAdjust =28-lunarAdjust; 
		

		FastTable<Double> lunarProbs = FastTable.newInstance();

		for (int i=0; i<lunarPeaks.length; i++) {
			lunarProbs.add((lunarNormals.get(i).pdf(lunarAdjust)/lunarNormals.get(i).pdf(lunarPeaks[i]))*lunarPeakSize[i] );
			if (i == 0) lunarProb = lunarProbs.get(i);
			else if ( lunarProbs.get(i) > lunarProb) lunarProb = lunarProbs.get(i); 
		}

			FastTable.recycle(lunarProbs); 
		
		return lunarProb;  
	}



	public static void inputData(){
		File fFile=new File("dataTest/SueLunarSpawn_correctExport.txt"); 
		if (fFile.exists()){
			try {

				//first use a Scanner to get each line
				Scanner scanner = new Scanner(fFile);
				while ( scanner.hasNextLine() ){
					Scanner lineScanner = new Scanner(scanner.nextLine()); 
					if ( lineScanner.hasNext() ){
						int day = lineScanner.nextInt();
						float prob = lineScanner.nextFloat(); 
						probs[day] = prob;
					}// end lineScanner 
					lineScanner.close();
				} // end file scanner
				scanner.close();
			}
			catch (IOException ex){
				System.out.println(" fail = "+ex);
			}
		}
	}



}
