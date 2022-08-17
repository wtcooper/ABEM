package us.fl.state.fwc.abem.organism;

import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.monitor.Monitor;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.geo.CoordinateUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class Seatrout extends Fish {


	Coordinate bunces = new Coordinate(-82.7455, 27.6518, 0); 
	FishTracker fishTracker; 
	int distScaleFactor = 2; //see below for usage
	double fertScaler = 1;
	/* Overrides the default Fish implementation, by adding a specific movement for Bunces
	 * Here, first checks if Bunces is close, and if so, if the fish is of large size to move to 
	 * Bunces.  If doesn't move ot Bunces, then attempts to move to a spawn aggregation. 
	 * If fails, then return false.  
	 * 
	 * Note: if they move to spawn, the go to the spawn site, spawn, and return to their
	 * home spot (last position) immediately.
	 * 
	 */
	@Override
	public boolean moveToSpawnAgg(){

		SeatroutParams params = (SeatroutParams) this.params; 

		if (fishTracker == null) 
			fishTracker = 
				(FishTracker) scheduler.getMonitors().get("FishTracker"); 

		fishTracker.getBuncesMovers()[0]++; 

		double distToBunces = CoordinateUtils.getDistance(this.coord, bunces);

		//Check if Bunces is within home range, and if so, have larger adults go there to spawn
		if (distToBunces < .06 
				&& uniform.nextDoubleFromTo(0, 1) 
				< (params.getHomeRanges(yearClass)) / (distToBunces*distScaleFactor)){

			fishTracker.getBuncesMovers()[1]++; 



			if (spawnSite == null) spawnSite = new Coordinate(0,0,0);
			spawnSite.x = bunces.x;  
			spawnSite.y = bunces.y; 
			spawnSite.z = bunces.z; 

			//don't move -- assume go to spawn site and then return

			processRates();

			return true; 
		}

		//return if no spawn aggregations
		else if (params.getSpawnAggregationList().isEmpty()) 
			return false;


		for (int i = 0; i < params.getSpawnAggregationList().size(); i++){

			Coordinate tempCoord = params.getSpawnAggregationList().get(i); 

			double tempDistance = 
				CoordinateUtils.getDistance(this.coord, tempCoord );  

			//add in an extra check here, so are 50% less likley to go than bunces, irrespective
			//of size
			if (uniform.nextDoubleFromTo(0, 1)
					< 0.5
					&& uniform.nextDoubleFromTo(0, 1)
					< (params.getHomeRanges(yearClass)) / (tempDistance*distScaleFactor)){



				if (spawnSite == null) spawnSite = new Coordinate(0,0,0);
				spawnSite.x = tempCoord.x;  
				spawnSite.y = tempCoord.y; 
				spawnSite.z = tempCoord.z; 

				//don't move -- assume go to spawn site and then return

				processRates();
				return true; 


			}
		}

		// if no aggregation within home range, then return false
		return false; 

	}


	@Override
	public void registerWithMonitors() {

		Monitor m = scheduler.getMonitors().get("FishTracker"); 
		m.addMonitoree(this); 
		this.addToMonitorList(m); 
		sumsMonitor = (FishTracker) m; // set the biomassMonitor explicitly
		sumsMonitor.setSumMap(this); //"Seatrout", this.groupBiomass, this.groupAbundance);


		Monitor m2 = scheduler.getMonitors().get("RecFishingTithe"); 
		m2.addMonitoree(this);
		this.addToMonitorList(m2); 


		if (SchedulerParams.isRedTideOn) {
			Monitor m3 = scheduler.getMonitors().get("RedTideMortality"); 
			m3.addMonitoree(this);
			this.addToMonitorList(m3); 
		}


	}
	
	@Override
	public double getFecundityScaler() {
		return fertScaler;
	}





}
