package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.abem.params.AbstractParams;

public class RecFishingTitheParams extends AbstractParams {



	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// COLLECTIONS INSTANTIATIONS -- DO NOT ADJUST

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList = new ArrayList<String>(); 	//Other agents which this agent interacts with
	private HashMap<String, Long>interactionTicks = new HashMap<String, Long>();  	//Map of all the dependent agent types with the preferred interaction tick for that dependent


	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// SET ALL VARIABLE ASSIGNMENTS HERE
	// AGENT.CLASS VARIABLES

	private boolean isSetRunTimes = false;
	private boolean isReactive=false; 						//If agent interacts with others, it's reactive. 
	private boolean isRunnable=true; 						//If agent changes over time, it's dynamic.  E.g., Bathymetry would not be dynamic
	private String queueType="runQueue"; 				//The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing
	private long normalTick=1l /*week*/ * (7*24*60*60*1000); 							//The normal tick length when no interactions are present
	private int runPriority=2; 								//The priority as to what should run first; here, monitors have higher priority than organisms
	
	


	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//RETURN METHODS FOR STATIC ASSIGNEMENTS -- IN GENERAL, DO NOT CHANGE BELOW THIS 
	
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
