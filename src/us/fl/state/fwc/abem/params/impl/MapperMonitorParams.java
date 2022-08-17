package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.abem.params.AbstractParams;

public class MapperMonitorParams extends AbstractParams {



	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// COLLECTIONS INSTANTIATIONS -- DO NOT ADJUST

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList; // = new ArrayList<String>(); 	//Other agents which this agent interacts with
	private HashMap<String, Long>interactionTicks; // = new HashMap<String, Long>();  	//Map of all the dependent agent types with the preferred interaction tick for that dependent


	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// SET ALL VARIABLE ASSIGNMENTS HERE
	// AGENT.CLASS VARIABLES

	private boolean isSetRunTimes = false;
	private boolean isReactive=false; 						//If agent interacts with others, it's reactive. 
	private boolean isRunnable=true; 						//If agent changes over time, it's dynamic.  E.g., Bathymetry would not be dynamic
	private String queueType="runQueue"; 				//The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing
	private long normalTick=1l /*day*/ * (24*60*60*1000); 								//The normal tick length when no interactions are present
	private int runPriority=2; 								//The priority as to what should run first; here, monitors have higher priority than organisms
	
	public static double displayLag = 0.0; 
	
	
	//===========================
	//Display variables
	//===========================
	public static final boolean drawIndividuals = true; 
	public static final boolean drawDensityMap = false; 

	public static final boolean drawLand = true;
	public static final boolean drawHabitat = true;
	public static final boolean drawGrid = true;
	public static final boolean drawBathy = false; 
	public static final int frameWidth = 650; 
	public static final int frameHeight = 1150; 
	
	public static final String EFDCDirectory = "c:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	public static final String bathFileName = EFDCDirectory + "TB2SB_WGS_Bathy.nc";
	public static final String displayLandFilename =   "c:\\work\\data\\GISData\\BaseMaps\\FloridaBaseLayers\\fl_40k_nowater_TBClip_simpleWGS_test.shp";
	public static final String displayBathyFilename = bathFileName; // "C:\\work\\data\\GISData\\BaseMaps\\FloridaBaseLayers\\Bathymetry\\nearshore_fl_arc.shp";
	public static final String habitatFileName = "c:\\work\\data\\GISData\\Habitat\\Seagrass\\JustSeagrass_WGS84_TBClip2_poly.shp";
	public static final String seagrassABEMGridName = "C:\\work\\data\\GISData\\BaseMaps\\ABEMGrids\\ABEMGrid2_WGS_seagrassCells.shp";


	
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
