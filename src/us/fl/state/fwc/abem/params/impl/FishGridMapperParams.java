package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.abem.params.AbstractParams;

public class FishGridMapperParams extends AbstractParams {



	
	//################################################################
	// COLLECTIONS INSTANTIATIONS -- DO NOT ADJUST

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList; 
	private HashMap<String, Long>interactionTicks; 


	//################################################################
	// SET ALL VARIABLE ASSIGNMENTS HERE
	// AGENT.CLASS VARIABLES

	private boolean isSetRunTimes = false;
	private boolean isReactive=false; 						 
	private boolean isRunnable=false; 						
	private String queueType="runQueue"; 				
	private long normalTick=1l /*day*/ * (24*60*60*1000); 								
	private int runPriority=2; 								
	
	public static double displayLag = 0.0; 
	
	
	//===========================
	//Display variables
	//===========================
	
	public static final boolean drawAbundance = true;
	public static final boolean drawBiomass = true;
	public static final boolean drawSSB = true;
	public static final boolean drawRecruitment = true;
	public static final boolean drawTEP = true;
//	public static final boolean drawSSB2TEPRatio= true;


	
	
	public static final boolean drawLand = true;
	public static final boolean drawHabitat = true;
	public static final boolean drawBathy = false; 
	public static final int frameWidth = 1200; 
	public static final int frameHeight = 1150; 
	
	public static final String bathFileName = 
		"data/TB2SB_WGS_Bathy.nc";
	public static final String displayLandFilename =  
		"maps/fl_40k_nowater_TBClip_simpleWGS_test.shp";
	public static final String habitatFileName = 
		"c:\\work\\data\\GISData\\Habitat\\Seagrass\\JustSeagrass_WGS84_TBClip2_poly.shp";

	
	//################################################################
	//RETURN METHODS FOR STATIC ASSIGNEMENTS -- IN GENERAL, DO NOT CHANGE  

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
