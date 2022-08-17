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

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class GrowthParamOptimize extends FitnessFunction {

	static int POP_SIZE = 500;
	static int GENERATIONS = 100; 
	
	private static int loopCounter = 1; 
	long seed = System.currentTimeMillis(); 
	MersenneTwister m = new MersenneTwister((int) seed); 
	Uniform uniform = new Uniform(m); 

	
	//Seatrout
/*	double wtMax = 4.9804;
	double obsK = 0.1057;
	double t0 = 1.408; 
	double wtAge0 = 0.00217;
*/
	
	// Redfish
/*	double wtMax = 11.848;
	double obsK = 0.2646;
	double t0 = 1.0117; 
	double wtAge0 = 0.00307;
*/
	
	//Snook
	double wtMax = 10.0233;
	double obsK = 0.0774;
	double t0 = 1.1735; 
	double wtAge0 = 0.0632;
	/**/
	
	
	
	public static void main(String[] args) throws InvalidConfigurationException {

		{ // **** JGAP CODE BLOCK
			Configuration conf = new DefaultConfiguration();

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

			FitnessFunction myFunc = new GrowthParamOptimize();

			conf.setFitnessFunction( myFunc );


			Gene[] sampleGenes = new Gene[ 1 ];

			/** For first optimization run to get which best disp dist was, used expOffset, crypOffset, larMortality, and dispDist
			 *  Did not use siteOffset because this co-varied with larvMortality
			 *  
			 *  For 2nd optimzation run to get best fit at single disp distance, just did expOffset, crypOffset, and larvMortality
			 */
			
			sampleGenes[0] = new DoubleGene(conf, 0, 1 );  // Exp offset

			
			
			Chromosome sampleChromosome = new Chromosome(conf, sampleGenes );

			conf.setSampleChromosome( sampleChromosome );

			// Finally, we need to tell the Configuration object how many
			// Chromosomes we want in our population. The more Chromosomes,
			// the larger the number of potential solutions (which is good
			// for finding the answer), but the longer it will take to evolve
			// the population each round. We'll set the population size to
			// 500 here.
			// --------------------------------------------------------------
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


				Double kClass = (Double) bestSolutionSoFar.getGene(0).getAllele();
				double k = kClass.doubleValue(); 

				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						 +"\t"+ "avg fitness: \t" + avgFitness +"\t"+ "k value:\t" + k);
				
				loopCounter++; 
			
			} // END EVOLVE loop through all generations

		} // **** END JGAP CODE BLOCK

	}


	/**
	 * Method for performing fitness evaluation. 
	 * 
	 */
	public double evaluate( IChromosome a_subject ) {

		double fitness = 0; 


		Double kClass = (Double) a_subject.getGene(0).getAllele();
		double k = kClass.doubleValue(); 


		// here, compute a fitness value and then return the fitness

		int maxAge = 25;
		double wtDiffSum = 0;  
		
		for (int i=1; i<maxAge; i++){
			
			double obsWeight =wtMax*(1-Math.exp((-obsK*(i-t0))));  
			
			// to calculate expected
			double tm = wtMax - wtAge0;

			double Lm = Math.exp(-k * i);

			tm = tm * Lm;
			double estWeight = wtMax - (tm+tm)/2.0;

			wtDiffSum += Math.abs(obsWeight-estWeight); 
		}
		
		fitness = 1/wtDiffSum; 
		
		return fitness; 

	} // end evaluate()


}
