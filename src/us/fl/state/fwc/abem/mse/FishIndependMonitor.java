package us.fl.state.fwc.abem.mse;

public interface FishIndependMonitor {

	/**
	 * Main method to collect FIM data.
	 */
	public void collectFIMData();

	/**
	 * Returns the FIM data
	 * 
	 * @return FIMDatagram
	 */
	public FIMDatagram getFIMData();
	
}
