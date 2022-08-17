package us.fl.state.fwc.abem.mse.assess;

import us.fl.state.fwc.abem.mse.AssessmentResults;
import us.fl.state.fwc.util.TextFileIO;

/**
 * An ADMB report file specific for the the ADMB seatrout 
 * catch-at-age model developed by M. Murphy (FWCC). 
 * 
 * @author Wade.Cooper
 *
 */
public class SeatroutADMBReport implements AssessmentResults {

	String fileName;
	TextFileIO repFile;
	
	public SeatroutADMBReport(String fileName){
		this.fileName = fileName;
	}

	
	public void open() {
		if (repFile != null) repFile.close();
		repFile = new TextFileIO(fileName);
		
		//read in the repFile to memory:
		
	}
	
	
	
	public void close(){
		repFile.close();
	}
	
	
	
}
