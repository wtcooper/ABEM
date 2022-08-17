package us.fl.state.fwc.abem.mse.assess;

import us.fl.state.fwc.util.TextFileIO;

/**
 * Holds the parameter values and data for the ADMB seatrout 
 * catch-at-age model developed by M. Murphy (FWCC). 
 * 
 * @author Wade.Cooper
 *
 */
public class SeatroutADMBData {

	String fileName;
	TextFileIO datFile;
	
	public SeatroutADMBData(String fileName){
		this.fileName = fileName;
	}
	
	
	/**
	 * Writes a new data file in place of the old one
	 */
	public void writeDatFile() {
		if (datFile != null) datFile.close();
		datFile = new TextFileIO(fileName);

		//write the data file in the proper format
		datFile.println("# ******** GENERAL DIMENSION DEFINITIONS ********");
		datFile.println("# number of fisheries (fleets) -com, rec h&l");
		datFile.println("");
		
		
		datFile.close();

	}
	
	
	public void readOldFile() {
		TextFileIO oldFile = new TextFileIO(fileName);
		//read in all data and set the params to old vals
		//use DataTable.toIntVector(String line) 
		
		oldFile.close();
	}
	
	//######################################
	//Parameters / data with get/set methods
	//######################################

	public int numFisheries = 2;
	public int prefirstYear = 1950;
	public int[] endingYearByFleet = {1985, 1981};
	
	
	


}
