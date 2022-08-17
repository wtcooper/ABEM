package us.fl.state.fwc.abem.test;

import us.fl.state.fwc.abem.Scheduler;
import cern.jet.random.Binomial;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class FecundityTest {

	long seed = System.currentTimeMillis(); 
	MersenneTwister m = new MersenneTwister((int) seed); 
	Uniform uniform = new Uniform(m); 
	Normal normal = new Normal(0,1,m); 
	Binomial bernoulli = new Binomial(1,0.5,m); 

	Scheduler scheduler = new Scheduler(); 

	
	double iniMass  = 0.001; // will need to get accurate estimate of initial mass -- this should be weight at 
	double maxMass = 4.569382; // maximum mass per individuals for this species
	int groupSize = 5; 
	double groupBiomass = (maxMass-1) * groupSize; 
	int numLarvae; 
	double sexRatio = 0.5; // the typical ratio of sexes in this species
	double fecundityRate = 0; 
	double baseFecundity = 100000; // total number of individuals produced per female per spawn 
	
	
	public static void main(String[] args) {

		FecundityTest mt = new FecundityTest();
		mt.step(); 
	}

	
	public void step(){

		System.out.println("baseFecundity - fecundityRate*(maxMass-(groupBiomass/groupSize)))   ))) /iniMass: " + ((baseFecundity - fecundityRate*(maxMass-(groupBiomass/groupSize)))   /iniMass) );
		System.out.println("baseFecundity/iniMass: " + (baseFecundity /iniMass) );

		// here, am assumming the 0.5 is referring to fact of equal sexes
		numLarvae = (int) (sexRatio*groupSize*( (Math.max(0, (getFecundityForcingWeight()*(baseFecundity - fecundityRate*(maxMass-(groupBiomass/groupSize)))   ))) /iniMass)) ; 
		
		System.out.println("num larvae released: " + numLarvae); 
		
	}

	
	/**	This method determines the environmental forcing on fecundity at any given time step.  This can vary depending on salinity, temperature, nutrients, habitat quality, etc
	 * 	Default implementation is to return a value of 1, or average fecundity.  Values can range above and below this, depending on the reference level of the external factors (see Gray et al.)
	 * @return fecundityWeight
	 */
	public double getFecundityForcingWeight(){
		
		// TODO -- need methods to represent decrease in 

		//default implementation: return 1, or average fecundity -- note, this could be great
		return 1; 
	}
	
	
}
