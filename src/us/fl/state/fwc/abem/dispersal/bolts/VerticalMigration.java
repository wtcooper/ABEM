package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Vertical migration interface.
 * 
 * @author Johnathan Kool
 *
 */

public interface VerticalMigration {

	/**
	 * This method changes the properties of the particle object accordingly.
	 * 
	 * @param p - The particle to be acted upon
	 */
	
	public void apply(Particle p);
	public VerticalMigration clone();
	
	public void closeConnections(); 
}

