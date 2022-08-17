package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Horizontal migration interface.
 * 
 * @author Wade Cooper
 *
 */

public interface HorizontalMigration {

	/**
	 * This method changes the properties of the particle object accordingly.
	 * 
	 * @param p - The particle to be acted upon
	 */
	
	public void apply(Particle p);
	public HorizontalMigration clone();
	
	public void closeConnections(); 
}

