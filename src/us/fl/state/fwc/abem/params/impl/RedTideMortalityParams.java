package us.fl.state.fwc.abem.params.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import us.fl.state.fwc.abem.params.AbstractParams;

import com.vividsolutions.jts.geom.Coordinate;

public class RedTideMortalityParams extends AbstractParams {

	
	public static boolean usePointMortality = false;
	
	public static Coordinate[] focalPoints = {
		new Coordinate(-82.7455, 27.6518, 0), 
		new Coordinate(-82.7248, 27.5350, 0)
		};

	//  polygon based on figure from Flaherty and Landsberg
	
	public static Coordinate[][] polygons = {
		{
			new Coordinate(-82.7718937148742, 27.75520040898662, 0), 
			new Coordinate(-82.62614863304552, 27.716569182477812, 0),
			new Coordinate(-82.55181278809675, 27.634038834936273, 0),
			new Coordinate(-82.55195281254012, 27.69066948351595, 0),
			new Coordinate(-82.56468986359968, 27.61589386490941, 0),
			new Coordinate(-82.60039448203965, 27.600090181337624, 0),
			new Coordinate(-82.71980009124869, 27.50058550699676, 0),
			new Coordinate(-82.77423500132927, 27.55502041707735, 0),
			new Coordinate(-82.7718937148742, 27.75520040898662, 0),
		}
	};
	
	
	public static double polygonMortRt = 0.9; 
	
	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// COLLECTIONS INSTANTIATIONS -- DO NOT ADJUST

	// AGENT.CLASS COLLECTIONS 
	private ArrayList<String> dependentsTypeList = new ArrayList<String>(); 	//Other agents which this agent interacts with
	private HashMap<String, Long>interactionTicks = new HashMap<String, Long>();  	//Map of all the dependent agent types with the preferred interaction tick for that dependent


	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// SET ALL VARIABLE ASSIGNMENTS HERE
	// AGENT.CLASS VARIABLES

	private boolean isSetRunTimes = true;
	private boolean isReactive=false; 						//If agent interacts with others, it's reactive. 
	private boolean isRunnable=true; 						//If agent changes over time, it's dynamic.  E.g., Bathymetry would not be dynamic
	private String queueType="runQueue"; 				//The queue that this agent will belong to -- those with interactions should be in runQueue, while those without should be in standing
	private long normalTick=7l /*day*/ * (24*60*60*1000); 								//The normal tick length when no interactions are present
	private int runPriority=1; 								//The priority as to what should run first; here, monitors have higher priority than organisms
	
	ArrayList<Calendar> setRunTimes;


	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//RETURN METHODS FOR STATIC ASSIGNEMENTS -- IN GENERAL, DO NOT CHANGE BELOW THIS 

	@Override
	public ArrayList<Calendar> getSetRunTimes(){
		if (setRunTimes == null) {
			setRunTimes = new ArrayList<Calendar>(); 
			GregorianCalendar runTime = new GregorianCalendar(2005, 6, 1);
			runTime.setTimeZone(TimeZone.getTimeZone("GMT"));
			setRunTimes.add(runTime);
		}
		return setRunTimes;

	}
	
	
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
