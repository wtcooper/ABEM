package us.fl.state.fwc.abem;

import java.util.ArrayList;
import java.util.TreeMap;

/**A collection of Agents to run at a single time, comparable based
 * on the collections scheduled  time to run.  Each agent is grouped by its priority, \
 * where poll() will remove a random agent from the highest priority in
 * the collection.   
 * 
 * @author wade.cooper
 *
 */
public class RunTimeBag implements Comparable<RunTimeBag> {

	public long runTime;
	Scheduler scheduler;
	RunTimeBagFactory fac; 
	
	//Key is priority (sorted), ArrayList are runners

	//Don't need to recycle this, since it belongs to the RunTimeBag nad 
	//the bags are recycled
	private TreeMap<Integer, ArrayList<Agent>> agentsToRun = new TreeMap<Integer, ArrayList<Agent>>();; 

	
	
	/**Constructor for a RunTimeBag
	 * 
	 * @param scheduler
	 */
	public RunTimeBag(Scheduler scheduler, RunTimeBagFactory fac){
		this.scheduler = scheduler;
		this.fac = fac;
	}



	@Override
	public int compareTo(RunTimeBag bag) {
		return (int) Math.signum(this.runTime-bag.runTime);
		//return (int) (this.runTime - bag.runTime);
	}



	/**Adds the agent to a list of other agents of the same priority, 
	 * set to run at the same time
	 * 
	 * @param agent
	 */
	public void addAgent(Agent agent){
		ArrayList<Agent> bagList = agentsToRun.get(agent.runPriority);
		if (bagList == null) {
			bagList = fac.getNewAgentList();
			agentsToRun.put(agent.runPriority, bagList);
		}
		//double check that the agent isn't already in the bag list -- should n
//		if (!bagList.contains(agent)) 
			bagList.add(agent);
	}



	/**Returns the next random agent with the highest priority
	 * 
	 * @return
	 */
	public Agent poll(){

		Integer topPriorityKey = agentsToRun.firstKey();
		ArrayList<Agent> agents = agentsToRun.get(topPriorityKey); 

		Agent agent = agents.remove(scheduler.getUniform().nextIntFromTo(0, agents.size()-1));

		//check if empty after removal, and if so, remove that priority key
		if (agents.isEmpty()) {
			fac.recycleAgentList(agents);
			agentsToRun.remove(topPriorityKey);
		}

		//remove the bag from the agent's bagPointer list
		fac.removeFromBagPointer(agent, this);
		
		return agent;
	}



	public boolean contains(Agent agent) {
		ArrayList<Agent> agents = agentsToRun.get(agent.runPriority); 
		if (agents == null || !agents.contains(agent)) return false;
		return true;
	}
	
	
	/**Checks if the list of agents to run at this time is empty
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return agentsToRun.isEmpty();
	}



	public TreeMap<Integer, ArrayList<Agent>> getAgentsToRun() {
		return agentsToRun;
	}




}
