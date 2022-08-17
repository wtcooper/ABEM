package us.fl.state.fwc.abem.mse;

public interface FishDependMonitor {

	/**
	 * Main method to collect FDM data.
	 */
	public void collectFDMData();

	/**
	 * Returns the FDM data
	 * 
	 * @return FDMDatagram
	 */
	public FDMDatagram getFDMData();
	

}
