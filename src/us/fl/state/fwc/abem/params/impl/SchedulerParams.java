package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import us.fl.state.fwc.util.Int3D;



public final class SchedulerParams{

	
	//##################################################################
	//##################################################################
	//Run flags / params to set  
	//##################################################################
	//##################################################################

	
	//####################################
	//File output
	//####################################
	public static String runID = "_test"; 

	public static boolean outputFishSumsToFile = false; //output non-spatial fish data
	public static String outputFishSumsFilename = 
		"output/FishGlobalSums" + runID + ".txt";

	public static boolean outputFishSpatialSumsToFile = false; //output spatial fish data
	public static String outputFishSpatialSumsFilename = 
		"output/FishSpatialSums" + runID + ".nc";

	public static boolean outputGlobalFishSpawnStats = false; //output non-spatial spawn
	public static String outputGlobalFishSpawnStatsFilename = 
		"output/FishGlobalSpawnStats" + runID + ".txt";

	public static double scaleFactor =.0001;  // amount to scale output values in netCDF 
	
	
	
	
	//####################################
	//initialization
	//####################################
	public static boolean useRandomSeed = false;
	public static long seed = 10000;  


	
	
	//####################################
	// Start time parameters
	//####################################
	public static int startYear = 2005;
	public static int startMonth = 1;
	public static int startDay = 1;
	public static int startHour = 0; 
	public static int startMin = 0; 
	public static int endYear = 2005;
	public static int endMonth = 12;
	public static int endDay = 31;

	public static Calendar startTime = 
		new GregorianCalendar(startYear, startMonth-1, startDay, startHour, startMin); 
	public static Calendar endTime = 
		new GregorianCalendar(endYear, endMonth-1, endDay); 

	
	
	
	//####################################
	//Input file locations
	//####################################
	//shapefile of grid data
	public static String gridFilename = "data/ABEMGrid3_WGS.shp";
	//list of cells that can be dispersed to (specialized for a model with masks)
	public static String reachableCellsFilename = "data/ReachableCellsMap.txt";
	//mask of grid cells that are land
	public static String landMaskFilename = 	"data/TB2SB_WGS_LandMaskForGridBarrier.nc"; 
	//bathy data
	public static String bathNetCDFFileName = "data/TB2SB_WGS_Bathy.nc";


	
	
	//####################################
	//Computation flags 
	//####################################
	
	public static boolean isDispersalOn = false; 
	public static boolean isRedTideOn = false;
	public static  boolean isTrophicsOn = false; 

	public static boolean isInteractionOn = true; //do they check for dependents 

	public static  boolean isMultithreaded = false; //NOT implemented
	public static  boolean isAdultDDMortalityOn = false; //uses DD on adults via K parameter

	public static boolean limitToTampaBay = true; 	//doesn't include sarasota bay and up
																				//to anclote

	//if spawning, will move twice as far (hard-wired at 2X's; could put in a param)
	public static boolean haveExtendedSpawnMovement = true; 
	
	
	
	
	//####################################
	//Mapping flags (for debugging only with small #'ers of individuals)
	//####################################
	public static boolean drawOrganismMap = false;
	public static boolean drawFishGridMap = false;




	//####################################
	//Console Output
	//####################################
	public static  boolean outputAgentStepsToConsole = false; //output lots of stuff
	public static boolean outputFishSumsToConsole = true; //output fish tracker step data
	public static boolean outputStepTimer = false; //outputs a step timer in Scheduler


	
	
	//####################################
	//Parameters 
	//####################################
	//Set the Parameters to the proper files
	public static HashMap<String, String> getParams() {
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("Seatrout", 
				"us.fl.state.fwc.abem.params.impl.SeatroutParams");
		paramsMap.put("FishTracker", 
				"us.fl.state.fwc.abem.params.impl.FishTrackerParams");
		paramsMap.put("DisperseMatrix", 
		"us.fl.state.fwc.abem.params.impl.DisperseMatrixParams");
		if (SchedulerParams.isRedTideOn) 
			paramsMap.put("RedTideMortality", 
				"us.fl.state.fwc.abem.params.impl.RedTideMortalityParams");
		paramsMap.put("RecFishingTithe", 
				"us.fl.state.fwc.abem.params.impl.RecFishingTitheParams");
		if (SchedulerParams.drawOrganismMap) 
			paramsMap.put("MapperMonitor", 
				"us.fl.state.fwc.abem.params.impl.MapperMonitorParams"); 
		if (SchedulerParams.drawFishGridMap) 
			paramsMap.put("FishGridMapper", 
				"us.fl.state.fwc.abem.params.impl.FishGridMapperParams"); 
		return paramsMap;
	}

	
	
	
	

	//##################################################################
	//##################################################################
	//Specialized data input values below here
	//##################################################################
	//##################################################################
	
	
	//####################################
	//Land Mask values for the hydro model
	//####################################
	public static ArrayList<Integer> getLandMaskListXs() {
		ArrayList<Integer> landMaskListXs = new ArrayList<Integer>(); 
		landMaskListXs.add(84);
		landMaskListXs.add(85);
		landMaskListXs.add(86);
		landMaskListXs.add(87);
		landMaskListXs.add(88);
		landMaskListXs.add(131);
		landMaskListXs.add(130);
		landMaskListXs.add(127);
		landMaskListXs.add(126);
		landMaskListXs.add(122);
		landMaskListXs.add(121);
		landMaskListXs.add(124);
		landMaskListXs.add(125);
		return landMaskListXs;
	}


	public static HashMap<Int3D, Integer> getLandMaskList() {
		HashMap<Int3D, Integer> landMaskList = new HashMap<Int3D, Integer>();
		landMaskList.put(new Int3D(85,173,0), 1);
		landMaskList.put(new Int3D(85,172,0), 1);
		landMaskList.put(new Int3D(86,170,0), 1);
		landMaskList.put(new Int3D(87,168,0), 3);
		landMaskList.put(new Int3D(88,170,0), 1);
		landMaskList.put(new Int3D(131,64,0), 1);
		landMaskList.put(new Int3D(131,65,0), 2);
		landMaskList.put(new Int3D(127,191,0), 1);
		landMaskList.put(new Int3D(127,190,0), 1);
		landMaskList.put(new Int3D(127,189,0), 1);
		landMaskList.put(new Int3D(122,197,0), 2);
		landMaskList.put(new Int3D(121,197,0), 2);
		landMaskList.put(new Int3D(124,201,0), 2);
		landMaskList.put(new Int3D(125,201, 0), 2);
		return landMaskList;
	}



	
	
	
	
	
	
	//##################################################################
	//##################################################################
	//Constructor -- don't change below here
	//##################################################################
	//##################################################################

	public SchedulerParams(){
		System.out.println("run name: " + this.runID);
		
		endTime.setTimeZone(TimeZone.getTimeZone("GMT")); 
	}

	public static Calendar getStartTime(){
		startTime.setTimeZone(TimeZone.getTimeZone("GMT"));
		return startTime; 
	}

	public static Calendar getEndTime(){
		endTime.setTimeZone(TimeZone.getTimeZone("GMT"));
		return endTime; 
	}



}
