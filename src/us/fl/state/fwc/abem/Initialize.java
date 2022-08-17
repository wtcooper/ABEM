package us.fl.state.fwc.abem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import us.fl.state.fwc.abem.dispersal.DisperseMatrix;
import us.fl.state.fwc.abem.dispersal.DisperseRandom;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.environ.impl.TemperatureSpline;
import us.fl.state.fwc.abem.fishing.RecFishingTithe;
import us.fl.state.fwc.abem.monitor.FishGridMapper;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.monitor.MapperMonitor;
import us.fl.state.fwc.abem.monitor.RedTideMortality;
import us.fl.state.fwc.abem.organism.SettlerGrid;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.organism.builder.SeatroutBuilder;
import us.fl.state.fwc.abem.params.AbstractParams;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;




public class Initialize {

	private static Scheduler scheduler; 




	public static void main(String[] args) {
		Initialize ini = new Initialize(); 
		try {
			ini.initializeImplementations();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		ini.buildPopulations();

		if (SchedulerParams.drawOrganismMap) scheduler.getMap().update(); 
		scheduler.run(); 

	}




	public void initializeImplementations() 
	throws InstantiationException, IllegalAccessException, ClassNotFoundException{

		scheduler = new Scheduler(); 

		// the ThingFactory is the factory method which makes new agents or returns 
		//recycled ones, instead of constantly creating and destoying agents
		OrganismFactory organFac = new OrganismFactory(scheduler); 
		scheduler.setOrganismFactory(organFac);


		// here, add a 'AgentParams()' for each agent type in model
		HashMap<String, Parameters>paramMap = new HashMap<String, Parameters>();  
		HashMap<String, String> tempMap = SchedulerParams.getParams();

		for (Iterator<Map.Entry<String,String>> i = tempMap.entrySet().iterator(); i.hasNext(); )  {
			Map.Entry<String,String> entry = i.next();
			String key = entry.getKey();
			String path = entry.getValue();

			AbstractParams params = (AbstractParams) Class.forName(path).newInstance();
			params.initialize(scheduler);
			paramMap.put(key, params); 
		}
		scheduler.setParamMap(paramMap); 

		
		//TODO -- to add in batch functionality, should adjust parameter files here
		// - can do this by adding in get/set methods for parameters



		/** Initialize other singletons here
		 */


		EnviroGrid grid = new ABEMGrid();
		grid.initialize(scheduler);

		// initialize the dispersal implementation
		DisperseMatrix disperse = new DisperseMatrix(); 
		disperse.initialize(scheduler);
		
		SettlerGrid sGrid = new SettlerGrid();
		sGrid.initialize(scheduler);

		TemperatureSpline temp = new TemperatureSpline(); 
		scheduler.setTemp(temp);
		
		
		FishTracker fb = new FishTracker(); 
		fb.initialize(scheduler); 

		if (SchedulerParams.isRedTideOn) {
			RedTideMortality rt = new RedTideMortality(); 
			rt.initialize(scheduler); 
		}

		RecFishingTithe rec = new RecFishingTithe();
		rec.initialize(scheduler); 

		if (SchedulerParams.drawFishGridMap) {
			FishGridMapper gridMap = new FishGridMapper();
			gridMap.initialize(scheduler);
		}

		if (SchedulerParams.drawOrganismMap){
			MapperMonitor map = new MapperMonitor(); 
			map.initialize(scheduler); 
			map.initiateMap(); 
			scheduler.setMap(map);
		}

		System.out.println("Implementation Initializations complete.");

	}


	public void buildPopulations() {

		SeatroutBuilder sb = new SeatroutBuilder();
		sb.setScheduler(scheduler);

		//set the seagrass grid created in the dispersal implementation so don't have 
		//multiple instances floating around
		if (scheduler.getDispersal() instanceof DisperseRandom) {
			DisperseRandom disp = (DisperseRandom) scheduler.getDispersal();
			sb.setSeagrassGrid(disp.getSeagrassGrid()); 
		}



		//TODO --- need to set this appropriately



		//get the seagrass grid if it's already been initialized so don't have to reinitialize
		if (scheduler.getDispersal() instanceof DisperseMatrix) {
			DisperseMatrix disp = (DisperseMatrix) scheduler.getDispersal();
			sb.setSeagrassGrid(disp.getSeagrassGrid()); 
		}

		sb.build(); 

		System.out.println("Initial population distribution complete.");

	}



}
