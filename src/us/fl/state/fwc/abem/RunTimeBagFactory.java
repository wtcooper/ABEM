package us.fl.state.fwc.abem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Factory / collections class for RunTimeBag, which handles add/remove agents to/from  bags, 
 * create/recycle bags, and create/recycle lists of agents used in bags.  
 * 
 * Holds current bags in the scheduler, a pointer to the bag an agent resides in, 
 * and recycled collections to avoid excessive creation / garbage collection of objects.  
 * 
 * 
 * @author Wade.Cooper
 *
 */
public class RunTimeBagFactory {

	//=========================
	// RECYCLED LISTS
	//=========================
	private ArrayList<RunTimeBag> bagRecycledList = new ArrayList<RunTimeBag>(); 
	private ArrayList<ArrayList<Agent>> agentRecycledList = new ArrayList<ArrayList<Agent>>(); 
	private ArrayList<ArrayList<RunTimeBag>> bagListRecycledList = new ArrayList<ArrayList<RunTimeBag>>(); 



	//Key is the runtime so don't have to loop through each bag looking at runtime
	private HashMap<Long, RunTimeBag> currentBags = new HashMap<Long, RunTimeBag>();

	//points to the bag that an agent resides in, so easily can find and remove
	private HashMap<Agent, ArrayList<RunTimeBag>> bagPointer = new HashMap<Agent, ArrayList<RunTimeBag>>();

	private Scheduler scheduler;





	public RunTimeBagFactory(Scheduler sched){
		this.scheduler = sched;
	}






	public void addToBag(Agent agent){
		//TODO -- need to add to a bag and to the bagPointer
		long runTime = agent.timesToRunQueue.get(0);
		RunTimeBag bag = currentBags.get(runTime); 
		if (bag == null) {
			bag = getNewBag();
			bag.runTime = runTime;
			currentBags.put(runTime, bag);
			scheduler.addBagToRunQueue(bag);
		}
		bag.addAgent(agent);

		//check to see if the bag already contains the agent
		//shouldn't need this contains method -- the only time an agent will be 
		//added to the bag is when either 
		// (1) it's requested through it's setNextRunTime() method, or
		// (2) an interacting agent requests an interaction which is before
		//			the previously set timesToRunQueue.get(0)
		
		//if (bag.contains(agent)) return;

		//add the agent's bag to the bagPointer mapping -- useful for removing all 
		ArrayList<RunTimeBag> bagList = bagPointer.get(agent);
		if (bagList == null) {
			bagList = getNewBagList();
			bagPointer.put(agent, bagList);
		}
		bagList.add(bag);
	}




	/**Removes and agent from any bag that it resides in, and if that
	 * bag is empty, then removes that bag from the 
	 * @param agent
	 */
	public void removeFromAllBags(Agent agent) {
		ArrayList<RunTimeBag> bagList = bagPointer.get(agent);

		bagPointer.remove(agent);
		
		if (bagList == null) return;

		for (RunTimeBag bag: bagList){

			TreeMap<Integer, ArrayList<Agent>> agentsToRun = bag.getAgentsToRun();

			for (Iterator<Integer> it = agentsToRun.keySet().iterator(); it.hasNext(); )  
			{  
			    Integer key = it.next();  
				ArrayList<Agent> agents = agentsToRun.get(key); 
				if (agents.contains(agent)){
					agents.remove(agent);
				}

				if (agents.isEmpty()){
					recycleAgentList(agents);
					it.remove();  
				}  

			} 

			if (agentsToRun.isEmpty()) {
				scheduler.removeBagFromRunQueue(bag);
				recycleBag(bag);
			}

		}
	}


	
	/**Removes the given bag from the bagPointer list for that agent.  
	 * If the bagPointer list is empty, then recycle the bagList and remove the 
	 * agent's key from the bagPointer list. 
	 * @param agent
	 */
	public void removeFromBagPointer(Agent agent, RunTimeBag bag) {
		ArrayList<RunTimeBag> bagList = bagPointer.get(agent);
		
		bagList.remove(bag); 
		
		if (bagList.isEmpty()){
			this.recycleBagList(bagList);
			bagPointer.remove(agent);
		}
		
	}



	//=============================================
	//RECYCLING METHODS
	//=============================================

	/**	returns a new bag, either from the recycled list or a new one entirely
	 * 
	 * @return bag
	 */
	public RunTimeBag getNewBag(){
		if (bagRecycledList.isEmpty()) return new RunTimeBag(scheduler, this); 
		else return bagRecycledList.remove(0);
	}



	/**	Recycles the bag by adding to the recycledList
	 * 
	 * @param bag
	 */
	public void recycleBag(RunTimeBag bag){
		bagRecycledList.add(bag);
		while (currentBags.containsKey(bag.runTime)) {
			currentBags.remove(bag.runTime);
		}
	}



	/**Returns a new arraylist of agents, either from the recycled list
	 * or a new one entirely
	 * 
	 * @return ArrayList<Agent>
	 */
	public ArrayList<Agent> getNewAgentList(){
		if (agentRecycledList.isEmpty()) return new ArrayList<Agent>(); 
		else return agentRecycledList.remove(0);
	}




	/**Recycles a list of agents
	 * 
	 * @param list
	 */
	public void recycleAgentList(ArrayList<Agent> list){
		agentRecycledList.add(list);
	}



	/**Returns a new arraylist of bags, either from the recycled list
	 * or a new one entirely
	 * 
	 * @return ArrayList<RunTimeBag>
	 */
	public ArrayList<RunTimeBag> getNewBagList(){
		if (bagListRecycledList.isEmpty()) return new ArrayList<RunTimeBag>(); 
		else return bagListRecycledList.remove(0);
	}




	/**Recycles a list of bags
	 * 
	 * @param list
	 */
	public void recycleBagList(ArrayList<RunTimeBag> list){
		bagListRecycledList.add(list);
	}


}
