package us.fl.state.fwc.abem.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class MaturityOptimize extends FitnessFunction {

	static int POP_SIZE = 10000;
	static int GENERATIONS = 200; 

	private static int loopCounter = 1; 
	static long seed = System.currentTimeMillis(); 
	static MersenneTwister m = new MersenneTwister((int) seed); 
	static Uniform uniform = new Uniform(m); 
	static Normal normal = new Normal(0,1,m); 

	double avgSize = 26.8002; 

	public static void main(String[] args) throws InvalidConfigurationException {


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
			
			FitnessFunction myFunc = new MaturityOptimize();
			
			
			conf.setFitnessFunction( myFunc );


			/*	Variables to optimize:
			 * 	(1) fallow period avg and fallow period stdev
			 * 	(2) lunar specificity
			 * 	(3) peak1 stdev
			 * 	(4) peak2 stdev
			 * 	(5) peak1 size, but just for smaller peak; leave bigger peak = 1
			 */
			Gene[] sampleGenes = new Gene[ 1 ];

			sampleGenes[0] = new DoubleGene(conf, 0, 50);  // size standard deviation

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


				Double stdevClass = (Double) bestSolutionSoFar.getGene(0).getAllele();
				double stdev = stdevClass.doubleValue(); 



				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						+"\t"+ "avg fitness: \t" + avgFitness + "\tstdev: " + stdev);

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

		int totalFish=10000; 
		List<Double> sizes = new ArrayList<Double>();  
		double[] sizeAtProp = new double[100]; // size at each 1% proportion 
		double[] realProp = new double[100]; // size at each 1% proportion 
		
		double fitness = 0; 


		Double stdevClass = (Double) a_subject.getGene(0).getAllele();
		double stdev = stdevClass.doubleValue(); 

		normal.setState(avgSize, stdev); 
		
		for (int i=0; i< totalFish; i++){
			sizes.add( normal.nextDouble() );
		}
		
		Collections.sort(sizes); 
		
		
		int counter=0;
		for (int i=100; i<sizes.size()-1; i=i+100){
			sizeAtProp[counter] =sizes.get(i); 
			double props = (double) i/ (double) totalFish; 
//			System.out.println("props: " + props); 
			realProp[counter] = (0.7794*26.8002 - Math.log(-((props-1)/props)))/0.7794;
			counter++; 
		}

		for (int i=0; i<counter; i++){
		fitness += (1-Math.abs(( (sizeAtProp[i]  - realProp[i]  )/ realProp[i] )));  
		}
		
		fitness = fitness/100 + 1000000; 
		
		return fitness; 

	} // end evaluate()






}
