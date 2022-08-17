package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Movement interface.
 * 
 * @author Johnathan Kool
 *
 */

public interface Movement {
	
	/**
	 * This method changes the properties of the particle object accordingly.
	 * 
	 * @param p - The particle to be acted upon
	 */
	
	public void apply(Particle p);
	
	/**
	 * Indicates whether the particle being moved is in proximity to NoData
	 * (used in efficiently applying the land detection routine).
	 * 
	 * @return
	 */
	
	public boolean isNearNoData();
	public Movement clone();
	
	public void closeConnections();
	
}

