package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Turbulent velocity interface.
 * 
 * @author Johnathan Kool
 *
 */

public interface TurbVar {

	/**
	 * This method changes the properties of the particle object accordingly.
	 * 
	 * @param p - The particle to be acted upon
	 */
	
	public abstract void apply(Particle p);
	
	public abstract TurbVar clone();
	
}
