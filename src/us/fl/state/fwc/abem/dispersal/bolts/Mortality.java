package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Mortality interface.
 * 
 * @author Johnathan Kool
 *
 */

public interface Mortality {
	
	/**
	 * This method changes the properties of the particle object accordingly.
	 * 
	 * @param p - The particle to be acted upon
	 */
	
	public void apply(Particle p);

}

