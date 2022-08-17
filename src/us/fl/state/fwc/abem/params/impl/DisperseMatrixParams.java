package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.abem.params.AbstractParams;

public class DisperseMatrixParams extends AbstractParams {



	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// SPECIALIZED PARAMS
	
	public static final String ncFileName = 
		"data/disperseMatrix.nc"; //transition matrix file
	public static final String shpFileName = 
		"data/BoltsRelease3.shp"; //locations of sites in transition matrix
	public static final String ncFileName_TB = 
		"data/disperseMatrixTB.nc"; //transition matrix file
	public static final String shpFileName_TB = 
		"data/BoltsRelease3_TB.shp"; //locations of sites in transition matrix

	public static final int planktonSchoolSize = 1000; //group size for dispersers -- need to 
																				//group to make computationally 
																				//efficient, else looping through millions
																				//of particles each daily step
	
	
	//TODO -- move this species specific stuff to SeatroutParams

	public static final double vmax = 0.05; //0.938;  //DD parameter -- set to 0 to make DI
	public static final double km = 0.25;  //DD parameter
	
	//This nearest neighbor distance was calcuated via ReleaseSiteAvgNND.java
	public static final double avgNND = 0.01742737929617212; 

	public static final String seagrassABEMGridName = 
		"data/ABEMGrid3_WGS_SeagrassCells.shp";

	public static final String seagrassABEMGridName_TB = 
		"data/ABEMGrid3_WGS_SeagrassCellsTB.shp";

	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// COLLECTIONS INSTANTIATIONS -- DO NOT ADJUST

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList = 
		new ArrayList<String>(); 	//Other agents which this agent interacts with
	private HashMap<String, Long>interactionTicks = 
		new HashMap<String, Long>();  	//Map of all the dependent agent types with the 
															//preferred interaction tick for that dependent


	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// SET ALL VARIABLE ASSIGNMENTS HERE
	// AGENT.CLASS VARIABLES

	private boolean isSetRunTimes = false;
	private boolean isReactive=false; 		//If agent interacts with others, it's reactive. 
	private boolean isRunnable=true; 		//If agent changes over time, it's dynamic.  
																//E.g., Bathymetry would not be dynamic
	private String queueType="runQueue"; 		//The queue that this agent will belong to -- 
																	//those with interactions should be in runQueue, 
																	//while those without should be in standing
	private long normalTick=7* 24l /*hour*/ * (60*60*1000); 	//run every day, at end of day
	private int runPriority=5; 	//run last after everything else has gone
	
	


	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//RETURN METHODS FOR STATIC ASSIGNEMENTS -- IN GENERAL, DO NOT CHANGE BELOW

	@Override
	public ArrayList<String> getDependentsTypeList() {
		return dependentsTypeList;
	}



	@Override
	public HashMap<String, Long> getInteractionTicks() {
		return interactionTicks;
	}



	@Override
	public long getNormalTick() {
		return normalTick;
	}


	@Override
	public String getQueueType() {
		return queueType;
	}

	@Override
	public int getRunPriority() {
		return runPriority;
	}



	@Override
	public boolean isRunnable() {
		return isRunnable;
	}


	@Override
	public boolean isReactive() {
		return isReactive;
	}
	

	@Override
	public boolean isSetRunTimes() {
		return isSetRunTimes;
	}


	
	
}
