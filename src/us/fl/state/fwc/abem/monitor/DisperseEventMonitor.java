package us.fl.state.fwc.abem.monitor;

import java.io.PrintWriter;
import java.util.TreeMap;

import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.util.Int3D;

import com.ibm.icu.util.Calendar;


public class DisperseEventMonitor  extends Monitor {

	/**Records the recruits added to the system (@ 3 months of age), both 
	 * and their origination source
	 */

	private final String keySep = "_"; 
	
	//map of the total recruits for each release site
	//here, the key mapping is "className_releaseID"
	
	//map of total num
	TreeMap<String, Long> sumRecruitsPerYear = new TreeMap<String, Long>(); 
	TreeMap<String, Long> sumTEPPerYear = new TreeMap<String, Long>(); 
	
	
	
	PrintWriter outReleaseSiteSumsFile;
	

	
	
	
	@Override
	public void run() {
		// Outputs the total recruitment
		//This should run at the end of the year, and total up the SSB/
		
	}

	
	
	
	
	public void addRecruit(OrgDatagram data){
		
		
		//(1) Set for total recruitSums
		int year = scheduler.getCurrentDate().get(Calendar.YEAR)  ;

		String key = data.getClassName()+ keySep + year;
		if (sumRecruitsPerYear.containsKey(key)) {
			Long val = sumRecruitsPerYear.get(key);
			val += data.getGroupAbundance();
		}
		else {
			// add new map key value with appropriate biomass
			sumRecruitsPerYear.put(key, (long) data.getGroupAbundance()); 
		}
		

		//(2) set the number of recruits in the ABEMCell so have spatial data on recruitment
		scheduler.getGrid().getGridCell(data.getGridCellIndex()).setNumRecruits(data.getClassName(), year, data.getGroupAbundance());
	}





	public void addTEP(String classname, int numParts, Int3D gridIndex) {

		//(1) Set total TEP
		int year = scheduler.getCurrentDate().get(Calendar.YEAR)  ;
		String key = classname + keySep + year;
		if (sumTEPPerYear.containsKey(key)) {
			Long val = sumTEPPerYear.get(key);
			val += numParts;
		}
		else {
			// add new map key value with appropriate biomass
			sumTEPPerYear.put(key, (long) numParts); 
		}
		
		//(2) set the number of recruits in the ABEMCell so have spatial data on recruitment
		scheduler.getGrid().getGridCell(gridIndex).setTEP(classname, year, numParts);

		
	}
	
	
	
	
}
