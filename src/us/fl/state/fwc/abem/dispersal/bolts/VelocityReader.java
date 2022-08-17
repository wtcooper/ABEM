package us.fl.state.fwc.abem.dispersal.bolts;

/**
 * Retrieves velocity values from a designated source
 * 
 * @author Johnathan Kool
 * 
 */

public interface VelocityReader {

	public double[] getNODATA();

	/**
	 * Retrieves the velocity values at the given coordinates
	 * 
	 * @param time -
	 *            Time
	 * @param z -
	 *            Depth
	 * @param lon -
	 *            Longitude
	 * @param lat -
	 *            Latitude
	 * @return - Coordinate pair containing u and v velocity values
	 */

	public abstract double[] getUV(long time, double z, double lon, double lat);

	
	/**
	 * Returns true if the tide is currently incoming, given a time.
	 * Since only inputs a time, must hard-code a single location to check if
	 * tide is incoming.
	 *  
	 * @param time
	 * @return
	 */
	public abstract boolean getIncomingTide(long time);
	
	
	/**
	 * Retrieves the average velocity values across time at the given
	 * coordinates
	 * 
	 * @param z -
	 *            Depth
	 * @param lon -
	 *            Longitude
	 * @param lat -
	 *            Latitude
	 * @return - Coordinate pair containing average u and v velocity values
	 */

	public abstract double[] getUVmean(double z, double lon, double lat);

	/**
	 * Retrieves the units of the velocity field (e.g. meters per second).
	 * 
	 * @return
	 */

	public abstract String getUnits();

	/**
	 * Explicitly sets the units of the velocity field
	 * 
	 * @param units
	 */

	public abstract void setUnits(String units);

	public abstract int[][] getShape();
	
	public abstract boolean isNearNoData();
	
	public abstract VelocityReader clone();
	
	public abstract void closeConnections(); 

}
