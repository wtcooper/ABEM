package us.fl.state.fwc.abem.test;

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

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

/**
 * This will calculate a probability of natural mortality and fishing moratlity for a set interval of time (divisorOfYear), which corresponds to instantanous rates of mortality with annual time units.  
 * Reason for this is that if want to get a conditional probability of a fish dying over a small unit of time, simply cannot take (1-exp(-M)) / divisorOfYear, where divisorOfYear corresponds to runtime
 *    
 * @author wade.cooper
 *
 */
public class MortalityOptimize extends FitnessFunction {

	
	//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| VARIABLES TO SET |||||||||||||||||||||||||||||||||||||||||||||||||||
	static final double A = 1.2;  				//the annual proportion of events.  Note, A>1 is plausible, e.g., if a fish is caught more than once per year
	static final int divisorOfYear = 365; 		//the number of time units in a year (must be integer).  For example, 365 would be day, and 12 would be month.  Here, this is the Z/timeUnit unit (e.g.,Z/day, Z/month)
	
	
	
	
	
	static int POP_SIZE = 100;
	static int GENERATIONS = 10; 

	private static int loopCounter = 1; 
	static long seed = System.currentTimeMillis(); 
	static MersenneTwister m = new MersenneTwister((int) seed); 
	static Uniform uniform = new Uniform(m); 
	static Normal normal = new Normal(0,1,m); 


	public static void main(String[] args) throws InvalidConfigurationException {


		{ // **** JGAP CODE BLOCK
			Configuration conf = new DefaultConfiguration();

			conf.setThreaded(true); // tells the system to run as multi-threaded; this should run CPUs+1 threads during fitness calculations; speed up per thread is minimal due to JGAP construction and my possible coding errors
			conf.getBestChromosomesSelector().setDoubletteChromosomesAllowed(false); // turns off allowing double values; in DefaultConfig is set to 'true'; Pop size isn't kept constant but it isn't evolving to a single population of clones either
			//conf.setOutputIndividuals(true); 
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
			
			FitnessFunction myFunc = new MortalityOptimize();
			
			
			conf.setFitnessFunction( myFunc );

			Gene[] sampleGenes = new Gene[ 1 ];

			sampleGenes[0] = new DoubleGene(conf, 0.0001, 0.05);  // estimate for Z, or instantaneous mortality on monthly time unit

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


				Double ZClass = (Double) bestSolutionSoFar.getGene(0).getAllele();
				double Z = ZClass.doubleValue(); 



				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						+"\t"+ "avg fitness: \t" + avgFitness + "\tZ: " + Z);

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

		int totalFish=100000;
		int numFishCaught = 0; 
		
		double fitness = 0; 

		Double ZClass = (Double) a_subject.getGene(0).getAllele();
		double Z = ZClass.doubleValue(); 

		
		for (int i=0; i< totalFish; i++){
			for (int j=0; j<divisorOfYear; j++){
				
				double probOfCatch = 1- Math.exp(-Z);
				if (uniform.nextDoubleFromTo(0, 1) < probOfCatch){
					numFishCaught++; 
				}

			}
			
		}
		
		double propCaught = (double) numFishCaught / (double) totalFish; 
		
		fitness = (1-Math.abs(( propCaught  - A  )/ A )) + 1000;   
		
		return fitness; 

	} // end evaluate()






}
