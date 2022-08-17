package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Settlement interface.
 * 
 * @author Johnathan Kool
 *
 */

public interface Settlement {
	
	/**
	 * This method changes the properties of the particle object accordingly.
	 * 
	 * @param p - The particle to be acted upon
	 */
	
	public void apply(Particle p);

}

