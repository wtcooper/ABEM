package us.fl.state.fwc.abem.organism.builder;

import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.abem.Agent;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.WorldMap;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.organism.Redfish;
import us.fl.state.fwc.abem.organism.Seatrout;
import us.fl.state.fwc.abem.organism.Snook;

public class OrganismFactory {

	private Scheduler scheduler; 
	private WorldMap world; 
	private HashMap<String, ArrayList<Agent>> recycledList = new HashMap<String, ArrayList<Agent>>(); 

	private ArrayList<OrgDatagram> datagrams = new ArrayList<OrgDatagram>(); 
	

	public OrganismFactory(Scheduler scheduler){
		this.scheduler = scheduler;
		this.world = scheduler.getWorld(); 
		
	}
	
	
	

	/**	returns either a new agent or a recycled agent depending on if there are any recycled agents of that type
	 * in the recycledList
	 * 
	 * @param className
	 * @return
	 */
	public Organism newAgent(OrgDatagram data){
		
		String className = data.getClassName();
		
		
		if (recycledList.containsKey(className)) {
			// if list isn't empty, return the first index in list
			if (!(recycledList.get(className).isEmpty())) {

				Organism agent =(Organism) recycledList.get(className).remove(0); 
				agent.setInRecycleBin(false); 
				
				agent.initialize(scheduler, data); 
				return agent;
 
			}
			// if list is empty, create a new
			else {
				Organism agent = createNew(className, data);
				return agent;
			}
		} // if there's no previous key set for this agent
		else {
			Organism agent = createNew(className, data);
			return agent;
		}
		
	}

	
	
	/**	Creates a new agent constructor of the className.  Note: need to call the agent's initialize method in order to add it to the world at appropriate
	 * coords and to add it to the scheduler at the appropriate times.  Also, the initialize method will set the appropriate variables for a new individual 
	 * (e.g., starting mass, size, etc)
	 * 
	 * @param className
	 * @return
	 */
	public  Organism createNew(String className, OrgDatagram data){
				if (className.equals("Redfish")) {
					Redfish agent = new Redfish();
					agent.setInRecycleBin(false); 
					agent.initialize(scheduler, data);
					return agent; 
				}
				else if (className.equals("Seatrout")) {
					Seatrout agent = new Seatrout (); 
					agent.setInRecycleBin(false); 
					agent.initialize(scheduler, data);
					return agent; 
				}
				else if (className.equals("Snook")) {
					Snook agent = new Snook (); 
					agent.setInRecycleBin(false); 
					agent.initialize(scheduler, data);
					return agent; 
				}

				
				// ....etc..... for all agents in model
				else {
					System.out.println("agent type code needs added to ThingFactory!!!!"); 
					System.exit(1); 
				}
				return null; 
	}

	

	/**	Recycles the agent by adding it to the recycledList so it's available to be used for new agents
	 * Note: recycled agents can only be used for new agents of their class type; therefore, all of the appropriate
	 * parameters are already set (e.g., isDynamic, groupThreshold, dependentTypeList, etc).  Agents still need initialized
	 * afterwards to apply any new values specific to the new agent (e.g., location). 
	 * 
	 * 
	 * @param agent
	 */
	public void recycleAgent(Agent agent){

		world.removeAgent((Organism) agent); 
		agent.removeThisAgent(); 

		agent.setInRecycleBin(true); 
		String className = agent.getClassName(); 

		// if there is already a FastMap entry for this SubClass, then just add this agent to existing list
		if (recycledList.containsKey(className)) {
			recycledList.get(className).add(agent); 
		}
		else {
			ArrayList<Agent> list = new ArrayList<Agent>();
			list.add(agent); 
			recycledList.put(className, list); 
		}

	}
	
	
	/**Get's a datagram from the datagram list.  If list is empty, will
	 * return a new datagram.
	 * 
	 * @return OrganismDatagram
	 */
	public OrgDatagram getDatagram(){
		
		if (datagrams.isEmpty()) return new OrgDatagram();
		else return datagrams.get(0); 
		
	}


	/**Recycles a datagram for future use.
	 * 
	 * @param gram
	 */
	public void recycleDatagram(OrgDatagram gram){
		datagrams.add(gram); 
	}
	
	
}
