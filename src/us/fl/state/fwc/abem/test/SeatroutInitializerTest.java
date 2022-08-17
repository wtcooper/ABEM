package us.fl.state.fwc.abem.test;

import java.util.HashMap;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.fishing.RecFishingTithe;
import us.fl.state.fwc.abem.monitor.AnimalBiomass;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.AnimalBiomassParams;
import us.fl.state.fwc.abem.params.impl.RecFishingTitheParams;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;




public class SeatroutInitializerTest {

	private static Scheduler scheduler; 




	public Scheduler initialize() {
		initializeImplementations(); 
		return scheduler; 
	}




	public void initializeImplementations(){

		scheduler = new Scheduler(); 

		// the ThingFactory is the factory method which makes new agents or returns recycled ones, instead of constantly creating and destoying agents
		OrganismFactory organFac = new OrganismFactory(scheduler); 
		scheduler.setOrganismFactory(organFac);

		// here, add a 'AgentParams()' for each agent type in model
		HashMap<String, Parameters>paramMap = new HashMap<String, Parameters>();  
		SeatroutParams params = new SeatroutParams();
		params.initialize(scheduler);
		paramMap.put("Seatrout", params);
		paramMap.put("AnimalBiomass", new AnimalBiomassParams());
		paramMap.put("RecFishingTithe", new RecFishingTitheParams());
		scheduler.setParamMap(paramMap); 



		EnviroGrid grid = new ABEMGrid(SchedulerParams.gridFilename, true);
		scheduler.setGrid(grid); 


		AnimalBiomass fb = new AnimalBiomass(); 
		fb.initialize(scheduler); 

		RecFishingTithe rec = new RecFishingTithe();
		rec.initialize(scheduler); 


	}






}
