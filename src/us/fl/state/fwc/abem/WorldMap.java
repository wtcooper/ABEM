package us.fl.state.fwc.abem;

import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;

/**	WorldMap class which keeps a record of all individuals within a 10x10m area
 * 
 * @author Wade.Cooper
 *
 */
public class WorldMap {


	//private SpatialIndex spatialIndex = new Quadtree(); 
	//GeometryFactory gf = new GeometryFactory();	

	HashMap<Long, Organism> organismMap;  
	private long agentIDCounter = 0; 

	//keeps track of the total number of organisms in simulation
	private long 	orgCounter = 0;

	Scheduler scheduler;



	public WorldMap(Scheduler scheduler){
		this.scheduler = scheduler;
		if (SchedulerParams.drawOrganismMap)  organismMap = new HashMap<Long, Organism>();
	}


	public synchronized void addToMap(Organism agent){
		orgCounter++;
		agent.setID(agentIDCounter++); 
		//Coordinate coords = agent.getCoords();
		//Point location = gf.createPoint(coords);
		//spatialIndex.insert(location.getEnvelopeInternal(), agent);
		if (SchedulerParams.drawOrganismMap) {
			organismMap.put(agent.getID(), agent); 
		}
	}




	/**Get's a list of dependents for the agent's current position.
	 * Based upon the scaleOfPerception of the agent
	 * 
	 * @param thisAgent
	 * @return
	 */
	public ArrayList<Agent> getDependents(Organism thisAgent){
		int searchRadius =(int) 
		Math.round(thisAgent.params.getScaleOfPerception(thisAgent.getYearClass())/2.0);

		ArrayList<Agent> dependents = thisAgent.getDependentsList();
		if (dependents == null) dependents = new ArrayList<Agent>();
		else dependents.clear();


		//loop through all cells in search radius
		int x = thisAgent.getGridIndex().x;
		int y = thisAgent.getGridIndex().y;
		for (int i=x-searchRadius; i<=x+searchRadius; i++) {
			for (int j=y-searchRadius; j<=y+searchRadius; j++) {

				ABEMCell cell = (ABEMCell) scheduler.getGrid().getGridCell(new Int3D(i,j,0));

				//loop through all agent types in dependentsTypeList 
				ArrayList<String> typeList = thisAgent.params.getDependentsTypeList();

				for(String className: typeList){

					ArrayList<Organism> agentList = cell.getAgents(className);
					if (agentList == null) return dependents;
					//loop through each individual in the AbemCell of appropriate type and check it's time,
					//to make sure it hasn't already stepped to far in future, 

					for (Organism thatAgent: agentList){
						if (thatAgent.equals(thisAgent)) continue;

						/*check if the other agent isn't too far ahead in time,
						 * where too far ahead is defined as a interactionTick (aka timeStep)
						 */
						if ((thatAgent.getCurrentTime()-thisAgent.getCurrentTime())
								<=thisAgent.getInteractionTick() ) {
							dependents.add(thatAgent);
						}
					}
				}
			}
		}

		return dependents;

	}


	/**	Moves the Organism and resets the spatial index 
	 * 
	 * @param agent
	 * @param toCoords
	 */
	public void moveTo(Organism agent, Int3D newCellIndex, Coordinate toCoords){

		//(1) remove the agent from the grid cell at the old location
		Int3D oldCellIndex = agent.getGridIndex();
		ABEMCell cell = (ABEMCell) scheduler.getGrid().getGridCell(oldCellIndex);
		cell.removeAgent(agent);

		//(2) reset the grid cell index and coordinates to the new position
		cell = (ABEMCell) scheduler.getGrid().getGridCell(newCellIndex);
		agent.setGridCellIndex(newCellIndex);
		agent.setCoords(toCoords.x, toCoords.y, toCoords.z);
		cell.addAgent(agent);
	}



	/**	Removes the agent from the spatial index and agent map
	 * 
	 * @param agent
	 */
	public void removeAgent(Organism agent){
		orgCounter--;

		//(1) remove the agent from the grid cell at the old location
		Int3D oldCellIndex = agent.getGridIndex();
		ABEMCell cell = (ABEMCell) scheduler.getGrid().getGridCell(oldCellIndex);
		cell.removeAgent(agent);

		if (SchedulerParams.drawOrganismMap) scheduler.getMap().remove(agent);  
	}


	public HashMap<Long, Organism> getOrganismMap(){
		return organismMap; 
	}


	/**Returns the total number of organisms remaining in the simulation
	 * 
	 * @return
	 */
	public long getOrgCounter() {
		return orgCounter;
	}


}
