package us.fl.state.fwc.abem.organism;

import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.monitor.Monitor;

public class Snook extends Fish {



	@Override
	public void registerWithMonitors() {

		Monitor m = scheduler.getMonitors().get("FishTracker"); 
		m.addMonitoree(this); 
		this.addToMonitorList(m); 
		sumsMonitor = (FishTracker) m; // set the biomassMonitor explicitly
		sumsMonitor.setSumMap(this); //"Snook", this.groupBiomass, this.groupAbundance);

		Monitor m2 = scheduler.getMonitors().get("RecFishingTithe"); 
		m2.addMonitoree(this);
		this.addToMonitorList(m2); 

		Monitor m3 = scheduler.getMonitors().get("CommFishingTithe"); 
		m3.addMonitoree(this);
		this.addToMonitorList(m3); 

	}

	/*

	@Override
	public void setScaleOfPerception() {
		scaleOfPerception =10;
	}

	@Override
	public void setReactivity() {
		isReactive = true; 
	}

	@Override
	public void setTickTimes() {
		normalTick =10; // normal tick is 1 minute; will need to crank this up for real model runs to something like 1 day  
	}


	@Override
	public void setPriority() {
		runPriority=3;
	}


	@Override
	public void setGroupSizeAndThreshold() {
		groupSize = 1;
		groupThreshold = 5; 
	}

	@Override
	public void setCarryCapacity() {
		carryCapacity = 5000000; // this is in g
		cullPeriod = 2419200;
	}


	@Override
	public void setSuitableHabitats() {
		suitableHabitats.put(9116, 2.0); //continuous seagrass
		suitableHabitats.put(9113, 1.5); // discountinuous seagrass
		suitableHabitats.put(9121, 1.); // algal beds
		suitableHabitats.put(5700, 1.); // tidal flats
		suitableHabitats.put(0, 0.); 
		suitableHabitats.put(2, 0.); 

		suitableHabitatList.add(9116); 
		suitableHabitatList.add(9113); 
		suitableHabitatList.add(9121); 
		suitableHabitatList.add(5700); 
	}

	@Override
	public  void setDependentsTypeList(){

		//(1) set dependentsTypeList
		dependentsTypeList.add("Snook"); 
		dependentsTypeList.add("RecFisherman"); 

		//(2) set interactionTicks
		interactionTicks.put("Snook", 5l); 
		interactionTicks.put("RecFisherman", 30l); 
	}







	@Override
	public void setDepthPreference() {
		preferredDepth = 5;
		maxDepth = 200; 
		minDepth = 0; 
	}



	@Override
	public void setSpeeds() {
		cruiseSpeed = 0.7; // roughly based on Fulton et al 2006b where are typical values for small lutjanids 
		maxSpeed = 1.8;   // roughly based on Fulton et al 2006b where are typical values for small lutjanids
	}



	@Override
	public void setHabitatSearchParameters() {
		searchRadiusExponent = 2;
		directionalVariability = 0.5; 
	}


	@Override
	public void setMergeRadius() {
		mergeRadius=10;
	}


	@Override
	public void setPopulationParameters() {
		baseMortality = 0.25; 
		ageMax = 15;  // need to get snook max age estimated
		ageMaturity = 1; 
		fallowPeriod = TimeUtils.secPerDay * 30; // period of time that can't spawn again 
		startOfBreeding = 120;
		endOfBreeding = 210;
		lunarSpecificity = 1; // 1 = only spawn on lunar cycle, while 0 = no lunar specificity
		peakLunarSpawnTime = 0; // 0 = full moon; 4 = new moon
		isMultipleSpawner = true; 

		// set all growth parameters
		c = 2.479; // c parameter defines the shape of the weight as function of age relationship: c <=1 is saturating; c>1 is sigmoidal
		K = 7.6941; // K is constant which defines the growth rate, where increasing K is faster growth rate;  here, K is in per year units, calculated from vonBertalanffy length curve
		iniMass = 0.001; // will need to get accurate estimate of initial mass -- this should be weight at 
		maxMass = 8.045; // maximum mass
		massCatchUpRate = 6; 



	}

	 */

}
