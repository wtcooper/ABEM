package us.fl.state.fwc.abem.optimize;

import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.stat.regression.SimpleRegression;
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

import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.DataTable;
import us.fl.state.fwc.util.TextFileIO;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

/**
 * This is change around since original optimization for weight variability (weight at length); 
 * now variability is in length at age
 * 
 * 
 * @author Wade.Cooper
 *
 */
public class SSTBuilderSizeVarianceOptimize extends FitnessFunction {

	static private EmpiricalWalker ewSizeSelect;

	static boolean powerFunction = true;

	private static final long serialVersionUID = 1L;
	static int POP_SIZE = 500;
	static int GENERATIONS = 50; 

	static private  int loopCounter = 1; 
	static long seed = System.currentTimeMillis(); 
	static public MersenneTwister64 m = new MersenneTwister64((int) seed); 
	static public Uniform uniform= new Uniform(m); 
	static public Normal normal= new Normal(0,1,m); 

//	static DataTable assessLAData = new DataTable("data/SuesLengthAtAge", true, "\t");
	static DataTable assessLAData = new DataTable("data/sst2010_LengthAtAge_female_JustFI", true, "\t");

	static double outlierCutoff = 10000;;

	static double sse = 0; 
	static double LM = 0;
	static double goodSlope = 0;
	static int numFish ;
	static int numTopFitness = 10;
	static SimpleRegression regress; 

	static double[] sizeFreqs; 
	static int[] sizeClasses;

	//static ArrayList<double[]> topVals = new ArrayList<double[]>();

	static TextFileIO fitOut = null; 
	//static PrintWriter out = null; 

	static double[] age; 
	static double[] TL;



	public static void main(String[] args) throws InvalidConfigurationException {

		TreeMap<Integer, Integer> sizeFreqTemp = new TreeMap<Integer, Integer>();

		SeatroutParams params = new SeatroutParams();

		//Initialize the model
		//Need to calculate the LM of the actual data
		age = ArrayUtils.toPrimitive((Double[]) assessLAData.getColumn("age") );
		TL = ArrayUtils.toPrimitive((Double[]) assessLAData.getColumn("TL") );
		
		
		int n = TL.length;
		numFish = n;


		for (int i=0; i<n;i++){

			//add to size frequency probabilities by keeping track of total number per cm size class
			//get size class in cm
			int TLTemp = (int) Math.round((TL[i])/10);
			Integer counter = sizeFreqTemp.get(TLTemp);
			if (counter == null) counter = 0;
			counter++;
			sizeFreqTemp.put(TLTemp, counter);

		}

		double sum = 0;
		//compute size freq size class
		for (Integer cmSizeClass: sizeFreqTemp.keySet()){
			sum += sizeFreqTemp.get(cmSizeClass);
		}

		sizeFreqs = new double[sizeFreqTemp.size()];
		sizeClasses = new int[sizeFreqTemp.size()];

		int counter = 0;
		for (Integer cmSizeClass: sizeFreqTemp.keySet()){
			double freq = ((double) sizeFreqTemp.get(cmSizeClass))/sum;
			sizeFreqs[counter] = freq; 
			sizeClasses[counter] = cmSizeClass;
			counter++;
		}



		for (int i=0; i<n;i++){
			//claculate sum squared errors
			double indTL = TL[i];
			double exp = params.getLengthAtAge(age[i], 0);
			
			sse += Math.pow(exp - indTL, 2);
		}

		//get st dev from variance
		sse = Math.sqrt(sse);
		//get st error
		sse /= Math.sqrt(numFish);
		
/*
 * 		//loop through num of fish
		for (int i=0; i<numFish; i++){

				double obsAge = age[i];
				//loop through each day to get stdev correct
					expTL = params.getLengthAtAge(obsAge, 0);
					TLOffset = normal.nextDouble(params.getLinf(0), stDev);
					realTL = params.getLengthAtAge(TLOffset, obsAge, 0);

					newSSE += Math.pow(expTL - realTL, 2);
		}

		//get st dev from variance
		newSSE = Math.sqrt(newSSE);
		//get st error
		newSSE /= Math.sqrt(numFish);

 */
		
		
		{ // **** JGAP CODE BLOCK
			Configuration conf = new DefaultConfiguration();

			conf.setThreaded(true); // tells the system to run as multi-threaded; this should run CPUs+1 threads during fitness calculations; speed up per thread is minimal due to JGAP construction and my possible coding errors
			conf.getBestChromosomesSelector().setDoubletteChromosomesAllowed(true); // turns off allowing double values; in DefaultConfig is set to 'true'; Pop size isn't kept constant but it isn't evolving to a single population of clones either
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

			conf.setAlwaysCaculateFitness(true); 

			FitnessFunction myFunc = new SSTBuilderSizeVarianceOptimize();

			conf.setFitnessFunction( myFunc );


			//For power function
			//Gene[] sampleGenes = new Gene[ 1 ];
			//sampleGenes[0] = new DoubleGene(conf, 0.00001, .1);  // coefficient for StDev =e^(b1*TL) 


			//For SIMPLE logSW ~ logTL linear fit
			Gene[] sampleGenes = new Gene[ 1 ];

			sampleGenes[0] = new DoubleGene(conf, 10, 40);  // st dev 

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

				if (i == GENERATIONS-1) {
					fitOut = new TextFileIO("dataTest/fishLWFitValues.txt");
				}


				for (int j=0; j < size; j++){
					IChromosome eachChromosome = pop.getChromosome(j);
					avgFitness += eachChromosome.getFitnessValue(); 


					//if last loop, print out fitness values
					if (i == GENERATIONS-1) {
						fitOut.println(eachChromosome.getFitnessValue() 
								+ "\t" + eachChromosome.getGene(0).getAllele() );
					}


				}

				avgFitness = avgFitness/((double) pop.size()); 

				IChromosome bestSolutionSoFar = population.getFittestChromosome();

				Double aClass  = (Double) bestSolutionSoFar.getGene(0).getAllele();
				double a = aClass.doubleValue(); 

				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						+"\t"+ "avg fitness: \t" + avgFitness + "\tst dev scalar value:\t" + a );


				loopCounter++; 




			} // END EVOLVE loop through all generations

		} // **** END JGAP CODE BLOCK

		fitOut.close();
	}









	/**Method for performing fitness evaluation.
	 */
	public double evaluate( IChromosome a_subject ) {

		SeatroutParams params = new SeatroutParams();
		
		double fitness = 0;

		Double aClass  = (Double) a_subject.getGene(0).getAllele();
		double stDev = aClass.doubleValue(); 


		ewSizeSelect = 
				new EmpiricalWalker(sizeFreqs, Empirical.NO_INTERPOLATION, m);


		double newSSE = 0;
		//Assign fish weights/lengths
		double expTL=0;
		double realTL = 0 ;
		double Linf = 0;

		//loop through num of fish
		for (int i=0; i<numFish; i++){

				double obsAge = age[i];
				//loop through each day to get stdev correct
					expTL = params.getLengthAtAge(obsAge, 0);
					Linf = normal.nextDouble(params.getLinf(0), stDev);
					realTL = params.getLengthAtAge(Linf, obsAge, 0);

					newSSE += Math.pow(expTL - realTL, 2);
		}

		//get st dev from variance
		newSSE = Math.sqrt(newSSE);
		//get st error
		newSSE /= Math.sqrt(numFish);


		fitness = 10000 - (Math.abs(sse - newSSE));

		return fitness; 

	} // end evaluate()







}
