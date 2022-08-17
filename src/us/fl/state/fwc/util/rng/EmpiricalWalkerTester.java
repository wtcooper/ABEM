package us.fl.state.fwc.util.rng;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class EmpiricalWalkerTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		double[] pdf = {.8, .1, .05, .05, 0};
		EmpiricalWalker test = new EmpiricalWalker(pdf);
		Uniform uni = new Uniform(new MersenneTwister((int) System.currentTimeMillis())); 
		
		
		for (int i=0; i<100; i++){
			System.out.println(test.nextInt(uni.nextDouble()));
		}
	}

}
