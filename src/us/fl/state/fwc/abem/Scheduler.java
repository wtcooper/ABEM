package us.fl.state.fwc.abem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

import us.fl.state.fwc.abem.dispersal.Dispersal;
import us.fl.state.fwc.abem.dispersal.bolts.util.TimeConvert;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.environ.HabitatShpFile;
import us.fl.state.fwc.abem.environ.Temperature;
import us.fl.state.fwc.abem.environ.impl.LandMask;
import us.fl.state.fwc.abem.monitor.FishGridMapper;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.monitor.MapperMonitor;
import us.fl.state.fwc.abem.monitor.Monitor;
import us.fl.state.fwc.abem.organism.SettlerGrid;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;
import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;


public  class Scheduler {




	/* Notes:
	 * 
	 * 'currentTime' is the current time of the simulation, in quanta of seconds.  
	 * Note that this is the time at which the current agent to run (head of the runQueue) runs at, 
	 * and in effect represents the last agent to run at a given time. I.e., agents run for a 
	 * preferred time step, and thus move ahead into the future, and their next time to run will 
	 * be the time at which they moved ahead to.  So once all other agents in the system have 
	 * caught up to them (or surpassed them, since most agents do not have the same time 
	 * steps), and therefore the current time has caught up to them , they will step again.  
	 * Thus, an agent's subjective time, or here the headCurrentTime, is the first entry in an 
	 * agent's timesToRun queue
	 * 
	 * See the diagram 'stepping.jpg' for overview of how asynchronous stepping occurs
	 * 
	 * An agent can get ahead in time if they have already run and are then requested to 
	 * interact by a slower agent.  In such cases, an agent can't go backwards, but will simply 
	 * not run, and interactions will assume they are synchronous enough IF close enough in time.  
	 * This will only be a major problem for slow-moving agents or agents with slow dynamics, 
	 * and thus assumptions should be OK. Done as in Gray et al (2006) when discussing 
	 * evolution of a sponge bed over time which is much slower than fast-moving agents
	 *    
	 */

	//NOTE: because of leap year
	private int seed; 
	private MersenneTwister64 m; 
	private Uniform uniform; 
	private Normal normal; 
	private Binomial bernoulli;  
	private Beta beta; 

	private Calendar currentDate; 
	private Calendar endDate; 
	private long startTime = System.currentTimeMillis();
	private long currentTime = 0;
	private  Queue<RunTimeBag> runQueue = new PriorityQueue<RunTimeBag>();  

	protected boolean isWindup = true;
	private boolean simulationRunning = true;
	private Agent queueHead; 
	private RunTimeBag currentRTBag;
	private int loopCounter = 0; 

	private WorldMap world; 
	private OrganismFactory organismFactory; 
	private EnviroGrid grid; 
	private HabitatShpFile habitat; 
	private Dispersal disp; 
	private SettlerGrid sGrid;
	private Temperature temp;
	private LandMask landMask; //a land mask for the enviroGrid 
	public ArrayList<Integer> landMaskListXs; 
	public HashMap<Int3D, Integer> landMaskList; 

	private ThreadService threadService; 

	// the paramMap holds a list of all the different species-specific Parameter files 
	private HashMap<String, Parameters>paramMap;  
	private HashMap<String, Monitor> monitors = new HashMap<String, Monitor>(); 
	private MapperMonitor map; 
	private RunTimeBagFactory bagFac;  



	public Scheduler(){
		int seed = 0;
		/*divide by 1000 since need to truncate to int -- 
		 * if using currentTimeMillis(), will provide new RN seed every second
		*/
		if (SchedulerParams.useRandomSeed) seed = (int) (System.currentTimeMillis()/1000d); 
		else seed = (int) (SchedulerParams.seed/1000d);
		m= new MersenneTwister64(seed); 
		uniform = new Uniform(0,1,m); 
		normal = new Normal(0, 1, m); 
		bernoulli = new Binomial(1,0.5,m); 
		beta = new Beta(1.5, 10, m); 
		world = new WorldMap(this);    
		landMask = new LandMask(SchedulerParams.landMaskFilename); 
		landMaskList = SchedulerParams.getLandMaskList();
		landMaskListXs = SchedulerParams.getLandMaskListXs();
		bagFac = new RunTimeBagFactory(this);

		runQueue.clear();
		currentDate = (GregorianCalendar) SchedulerParams.getStartTime().clone(); 
		endDate = SchedulerParams.getEndTime(); 
		currentDate.setTimeZone(currentDate.getTimeZone()); 
		currentTime = currentDate.getTimeInMillis(); 

		if (SchedulerParams.isMultithreaded) {
			threadService = new ThreadService();
			//TODO
		}
		
		//System.out.println("run name: " + SchedulerParams.runID);
	}


	public void run() {

		
		while (simulationRunning && world.getOrgCounter() > 0) {
			loopCounter++;

			if (currentDate.after(new GregorianCalendar(2004, 5, 11))) {
				hashCode();
			}
			
			currentRTBag = runQueue.poll(); 

			while (!currentRTBag.isEmpty()){
				
				long startTime = System.currentTimeMillis();
				
				queueHead = currentRTBag.poll();

				queueHead.isQueueHead = true; 
				if (SchedulerParams.outputAgentStepsToConsole && 
						queueHead.timeToDisplay == true) 
					System.out.println(queueHead.getDescriptor()); 


				/* check's agent's current time against it's next time in timesToRunQueue -- 
				 * if agent is ahead of the model current time (i.e., another agent inserted a 
				 * timeToRunQueue entry into this agent) then don't run and clear out
				 * all entries into timesToRunQueue up to the agents current time
				 */
				if (queueHead.currentTime > queueHead.timesToRunQueue.get(0)){
					queueHead.cleanToCurrentTime();
				}

				else {
					currentDate.add(Calendar.SECOND, 
							(int) ((queueHead.timesToRunQueue.get(0)-currentTime)/1000 )); 
					/* set the simulation time to the new timesToRunQueue value; 
					 * this is what current tick to run at value is					 
					 */
					currentTime = queueHead.timesToRunQueue.remove(0);

					if (currentDate.getTimeInMillis() != currentTime){
						System.out.println("times not matching up!");
					}
					
					runAgent(queueHead);

					if (SchedulerParams.outputStepTimer == true) 
						System.out.println("step run time: " + 
								(System.currentTimeMillis()-startTime) + "ms");
						
					if (currentDate.after(endDate)){
						simulationRunning = false;
					}
				}
				queueHead.isQueueHead = false; 
				
			} //end while loop over all agents in currentBag
			
			//recycle the current bag for future use
			bagFac.recycleBag(currentRTBag);
			
		} // end while loop
		endSimulation(); 
	} // end step method


	/** Cycles through an agent by stepping the agent, adding to it's timeToRunQueue, 
	 * and reinserting it into the runQueue.  Note: the methods to determine the agent's 
	 * next time step are left up to the agent to figure out.   
	 * 
	 * @param agent
	 */
	private  void runAgent(Agent agent) {
		// set the next run time based on if have dependents or not
		agent.setNextRunTime(); 
		// gets the next timeToRun and calculate dt (aka runTime), only if a next time exists
		if (!agent.timesToRunQueue.isEmpty())
			agent.setTimeStep(agent.timesToRunQueue.get(0) - currentTime);  
		agent.run(); 	

		if (agent.isInRecycleBin() || agent.timesToRunQueue.isEmpty()) return;  

		// after an agent is run for a set amount of time, set it's current time
		agent.currentTime = agent.timesToRunQueue.get(0);	 

	}





	/** Program termination method; makes sure all output is wrapped up by end of method
	 * 
	 */
	private void endSimulation(){
		// ensure all output is finished

		if (SchedulerParams.isMultithreaded) threadService.shutdown();  

		if (this.monitors.containsKey("FishTracker")) {
			FishTracker track = (FishTracker) monitors.get("FishTracker");
			track.closeFiles(); // need to close the outfile writers, if open
		}
		
		// program terminates at end of this method
		System.out.println("Model run time: " 
				+ TimeConvert.millisToString(System.currentTimeMillis()-startTime));
		
		if (monitors.containsKey("FishGridMapper")){
			FishGridMapper map = (FishGridMapper) monitors.get("FishGridMapper"); 
			map.drawMap();
			
		}
	}




	public void addBagToRunQueue(RunTimeBag bag){
		runQueue.add(bag);
	}
	
	public void removeBagFromRunQueue(RunTimeBag bag){
		runQueue.remove(bag);
	}

	
	public void addAgentToRunQueue(Agent agent){
		bagFac.addToBag(agent); 	
		//need to sort each time if using arraylist
		//Collections.sort(runQueue); 
	}

	public void removeAgentFromRunQueue(Agent agent){
		//note: check first so that aren't removing is not already there
		bagFac.removeFromAllBags(agent);
	}


//	public void pollRunQueue(){
//		runQueue.poll(); //.remove(0);  
//	}


	public MersenneTwister64 getM() {
		return m;
	}

	public Uniform getUniform() {
		return uniform;
	}

	public Normal getNormal() {
		return normal;
	}

	public Binomial getBernoulli() {
		return bernoulli;
	}

	public Beta getBeta(){
		return beta; 
	}

	public void addMonitor(Monitor m){
		monitors.put(m.getClassName(), m); 
	}

	public HashMap<String, Monitor> getMonitors() {
		return monitors;
	}




	public MapperMonitor getMap() {
		return map;
	}


	public void setMap(MapperMonitor map) {
		this.map = map;
	}



	public OrganismFactory getOrganismFactory() {
		return organismFactory;
	}


	public void setOrganismFactory(OrganismFactory orgFactory) {
		this.organismFactory = orgFactory;
	}


	public Parameters getParamClass(String className) {
		return paramMap.get(className);
	}


	public void setParamMap(HashMap<String, Parameters> paramMap) {
		this.paramMap = paramMap;
	}


	/**Returns the current time of simulation in milliseconds*/
	public long getCurrentTime() {
		return currentTime;
	}


	public Calendar getCurrentDate() {
		return currentDate;
	}


	public boolean isWindup() {
		return isWindup;
	}


	public void setGrid(EnviroGrid grid) {
		this.grid = grid;
	}


	public EnviroGrid getGrid() {
		return grid;
	}


	public SettlerGrid getSGrid() {
		return sGrid;
	}


	public Temperature getTemp() {
		return temp;
	}


	public void setTemp(Temperature temp) {
		this.temp = temp;
	}


	public void setSGrid(SettlerGrid sGrid) {
		this.sGrid = sGrid;
	}


	public LandMask getLandMask() {
		return landMask;
	}


	public ArrayList<Integer> getLandMaskListXs() {
		return landMaskListXs;
	}


	public HashMap<Int3D, Integer> getLandMaskList() {
		return landMaskList;
	}


	public HabitatShpFile getHabitat() {
		return habitat;
	}


	public void setHabitat(HabitatShpFile habitat) {
		this.habitat = habitat;
	}


	public Dispersal getDispersal() {
		return disp;
	}


	public void setDispersal(Dispersal disp) {
		this.disp = disp;
	}


	public ThreadService getThreadService() {
		return threadService;
	}


	public WorldMap getWorld() {
		return world;
	}




}
