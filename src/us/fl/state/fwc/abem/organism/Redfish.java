package us.fl.state.fwc.abem.organism;

import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.monitor.Monitor;

public class Redfish extends Fish {


	@Override
	public void registerWithMonitors() {

		Monitor m = scheduler.getMonitors().get("FishTracker"); 
		m.addMonitoree(this); 
		this.addToMonitorList(m); 
		sumsMonitor = (FishTracker) m; // set the biomassMonitor explicitly
		sumsMonitor.setSumMap(this); //"Redfish", this.groupBiomass, this.groupAbundance);

		Monitor m2 = scheduler.getMonitors().get("RecFishingTithe"); 
		m2.addMonitoree(this);
		this.addToMonitorList(m2); 

		Monitor m3 = scheduler.getMonitors().get("CommFishingTithe"); 
		m3.addMonitoree(this);
		this.addToMonitorList(m3); 

	}



}
