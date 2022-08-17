package us.fl.state.fwc.abem.dispersal.bolts.impl;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import cern.jet.random.Uniform;

/**
 * Implements mortality using an exponential function.
 * 
 * @author Johnathan Kool
 */

public class TBRateMortality implements us.fl.state.fwc.abem.dispersal.bolts.Mortality{

	private double mrate;
	//private String units;
	
	public TBRateMortality(double mrate){
		this.mrate = mrate;
	}

	/**
	 * Applies probabilistic mortality the given particle
	 */
	
	public synchronized void apply(Particle p) {

		if (Uniform.staticNextDouble() > Math.exp(-1.0 * mrate)) {
			p.setDead(true);
		}
	//	this.notifyAll();
	}
	
	/**
	 * Retrieves the mortality rate
	 * 
	 * @return
	 */

	public double getMrate() {
		return mrate;
	}

	/**
	 * Sets the mortality rate
	 * 
	 * @param mrate
	 */
	
	public void setMrate(double mrate) {
		this.mrate = mrate;
	}

}

