package us.fl.state.fwc.abem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import us.fl.state.fwc.abem.monitor.Monitor;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import cern.jet.random.Binomial;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


/**
 * "This class provides the basic linkages between the modelling framework (InVitro) 
 * and the sub-models which flesh out in the system. Roughly speaking these linkages 
 * fall into three main categories: those concerned with managing the agent’s behaviour 
 * in the queues, those concerned with managing the agent’s interactions with other agents, 
 * and those which deal with the bookkeeping associated with running the simulation." 
 * Gray et al. (2006)
 * 
 * @author wade.cooper (adapted from Gray et al. 2006)
 *
 */
public abstract class Agent implements Comparable<Object>, Runnable {

	protected DecimalFormat df = new DecimalFormat("#.##"); 

	protected long ID; 
	protected long interactionTick; 
	protected long nextRunTime ; 
	// this is list of times when an agent will run next, in values of the simulation run time
	protected ArrayList<Long> timesToRunQueue = new ArrayList<Long>(); 
	protected long currentTime; 
	protected ArrayList<Agent> dependentsList; 
	protected ArrayList<Monitor> monitorList = new ArrayList<Monitor>();  
	protected boolean timeToDisplay = true;
	protected Scheduler scheduler;
	protected MersenneTwister m; 
	protected Uniform uniform;
	protected Normal normal; 
	protected Binomial bernoulli; 
	protected boolean isQueueHead = false; 
	protected boolean isInRecycleBin = false; 
	protected Parameters params; 
	protected long timeStep; //in seconds
	public int runPriority;



	public void superInitialize(long firstRunTime) {
		String className = this.getClassName();
		params = scheduler.getParamClass(this.getClassName()); 
		runPriority = params.getRunPriority();

		//set the default interaction tick to a normal tick
		interactionTick = params.getNormalTick(); 
		currentTime = scheduler.getCurrentTime(); 

		timesToRunQueue.clear();

		//for new settlers being added that won't run for a period of time, add
		//a firstRunTime in the future so won't be run for the settlement period
		if (firstRunTime > currentTime){
			currentTime = firstRunTime;
			addTimeToRunQueue(firstRunTime); 
		}

		else if (params.isSetRunTimes()){
			ArrayList<Calendar> setTimeSteps = params.getSetRunTimes(); 
			for (int i=0; i < setTimeSteps.size(); i++){
				long timeToRun  =   (long) setTimeSteps.get(i).getTimeInMillis();  
				addTimeToRunQueue(timeToRun); 
			}
		}
		else if (params.isRunnable()){ //only run if runnable
			addTimeToRunQueue(scheduler.getCurrentTime()); 
		}

		m = scheduler.getM();  
		uniform = scheduler.getUniform(); 
		normal = scheduler.getNormal();    
		bernoulli = scheduler.getBernoulli();  
	}







	/** This method will compare agents to determine which should run next in runQueue
	 * Compares, in order: (1) next time to run, (2) priority, (3) then randomizes within same 
	 * priority.  This is implemented method from Comparable interface.
	 * 
	 * @param obj (other agent to compare)
	 */
	public final int compareTo(Object obj) {
		Agent other = (Agent) obj; 

		// if next scheduled run times are equal among agents,
		long thisTime = this.timesToRunQueue.get(0) ;
		long otherTime = other.timesToRunQueue.get(0);

		if (thisTime == otherTime) {  
			// then check the priority, and if they're equal,
			if (this.runPriority == other.runPriority) {  
				// randomly allocate which one goes first
				if (uniform.nextDoubleFromTo(0, 1) < 0.5) return 1; 
				else return -1;
			}
			// if run priorities aren't equal, run the one with the higher priority (lower value)
			if (this.runPriority < other.runPriority) return -1; 
			if (this.runPriority > other.runPriority) return 1;
		}
		// else if next schedule tick is not equal, run the one that comes first
		else if (thisTime < otherTime) return -1;

		else if (thisTime > otherTime) return 1;

		return 0;
	}





	/** Set's the next run time in the timesToRunQueue for a default time step run
	 * Note: this is sloppy because have multiple 
	 */
	public  final void setNextRunTime() {

		if (!params.isSetRunTimes() ){ 

			if (SchedulerParams.isInteractionOn)  setDependentsList();  

			nextRunTime = currentTime + interactionTick;  

			if (dependentsList != null && !dependentsList.isEmpty()){ 

				/* if there are some entries in timesToRunQueue, and agent already has a time 
				 * in its queue that is before the next time to run (e.g., if dependee request it),
				 * don't set a new time now, because will set it when it , but tell dependents to 
				 * match that time.  THIS formulation should account for monitors requesting time  
				 */
				if (!timesToRunQueue.isEmpty() && (timesToRunQueue.get(0) < nextRunTime)){
					/*set the next run time to head of queue, then use this value to request 
					 * dependents run then too
					 */
					nextRunTime  = timesToRunQueue.get(0); 

				}

				/* set the next run time the appropriate interactionTick (i.e., shortest interactionTick 
				 * amongst all neighboring dependents)
				 */
				else addTimeToRunQueue(nextRunTime); 

				/* iterate over dependents list and add the same next run time to their 
				 * timesToRunQueue so will be synchronous on next run
				 */
				for (int i=0; i< dependentsList.size(); i++) { 
					Agent a = dependentsList.get(i); 
					a.addTimeToRunQueue(nextRunTime); 
				}
			}
			else {
				if (timesToRunQueue.isEmpty() ||  (timesToRunQueue.get(0) > nextRunTime))  {
					addTimeToRunQueue(nextRunTime); 
				}
			}

			// remove the agent and add it to the run queue so will set appropriate time
			//THIS is main entry into the runQueue via RunTimeBags
			//scheduler.removeAgentFromRunQueue(this);
			scheduler.addAgentToRunQueue(this);

		}
	}







	/**	Adds a time to the addTimeToRunQueue
	 * 
	 * @param time
	 */
	public void addTimeToRunQueue(long time){
		if ( time >= currentTime && !timesToRunQueue.contains(time)) {

			timesToRunQueue.add(time); // only adds the time if it's not already in the linkedlist
			if (timesToRunQueue.size() > 1) Collections.sort(timesToRunQueue);

			// need to remove and add agent to run queue so is updated with priority
			//schedule.removeAgentFromRunQueue(this);
			//schedule.addAgentToRunQueue(this);



			/*If another agent requests an interaction, then this agent isn't the queueHead, 
			 * so will need to run the scheduler methods to enter the runQueue since doesn't 
			 * happen in setNextRunTime() above 
			 */
			if (!this.isQueueHead){ 

				/*only add the agent to the run queue if the requested time is the next in the 
				 * run queue (i.e., before future requested time) 
				 * If so, need to first remove the agent from any bags in the runTimeBag's so 
				 * that won't be multiple instances bouncing around
				 */
				if (time == timesToRunQueue.get(0)) {
					scheduler.removeAgentFromRunQueue(this);
					scheduler.addAgentToRunQueue(this);
				}
			}
		}
	}



	/**	Similar to method from InVitro, where cleans out the timesToRunQueue up to a 
	 * particular point.  This is needed in case other agents request an interaction time into 
	 * an agent's timesToRunQueue
	 * 
	 * @param time
	 */
	public final void cleanToCurrentTime(){
		while (timesToRunQueue.get(0) < currentTime) {
			// will remove all entries in timesToRunQueue up to current time
			timesToRunQueue.remove(0); 
		}
		scheduler.removeAgentFromRunQueue(this);
		scheduler.addAgentToRunQueue(this);
	}





	/**	Returns the next run time in the timesToRunQueue
	 * 
	 * @return
	 */
	public long getNextRunTime(){
		if (!(this.timesToRunQueue.get(0) == null)) return this.timesToRunQueue.get(0) ;
		else return this.currentTime; 
	}





	/**	Gets the class name without the package.  Takes in class argument, e.g., object.getClass(), 
	 * and returns a string
	 * 
	 * @param c
	 * @return string -- the class name without package
	 */
	public String getClassName() {
		String FQClassName = this.getClass().getName();
		int firstChar;
		firstChar = FQClassName.lastIndexOf ('.') + 1;
		if ( firstChar > 0 ) {
			FQClassName = FQClassName.substring ( firstChar );
		}
		return FQClassName;
	}



	/**	Resets the agent as needed to prepare it for recycling
	 * 
	 */
	public void removeThisAgent(){
		// set all the appropriate variables to zero
		timesToRunQueue.clear(); 
		scheduler.removeAgentFromRunQueue(this);

		for (int i=0; i<monitorList.size(); i++) {
			Monitor m = monitorList.get(i); 
			m.removeMonitoree(this); 
		}
		
		monitorList.clear();

	}




	/** Returns a string describing the agent (e.g, hashcode, current time). Is overridden by 
	 * subclasses to encorporate more detail where necessary (e.g., position, group size, etc)
	 * 
	 * @return
	 */
	public String getDescriptor(){
		String descriptor = 
			this.getClassName() +" (time: "+new Date(this.currentTime).toString() + ")"; 
		return descriptor; 
	}

	
	
	/**Returns the current time of simulation in milliseconds*/
	public long getCurrentTime() {
		return currentTime;
	}

	
	
	public long getInteractionTick() {
		return interactionTick;
	}







	public boolean isInRecycleBin() {
		return isInRecycleBin;
	}

	public void setInRecycleBin(boolean isInRecycleBin) {
		this.isInRecycleBin = isInRecycleBin;
	}

	public void addToMonitorList(Monitor monitor) {
		this.monitorList.add(monitor); 
	} 

	public Parameters getParams(){
		return params; 
	}




	//####################################
	//ABSTRACT METHODS
	//####################################


	/** Set's all neighboring agents to interact with, and adds them too Scheduler.stadingQueue
	 * Note: the order at which agents are cycled through the standingQueue will be the same as
	 * the order to which they're atted to FastList dependents -- if want random, need to 
	 * randomly sort this list
	 */
	public abstract void setDependentsList(); 

	public abstract void registerWithMonitors(); 
	/*
	public abstract void setTickTimes(); 

	public abstract void setReactivity(); 

	public abstract void setIsDynamic(); 

	public abstract void setPriority(); 


	public abstract void setQueueType();

	public  abstract void setDependentsTypeList();
	 */







	public long getID() {
		return ID;
	}


	/**Converts milliseconds of the Java clock to seconds which is 
	 * base time unit for model
	 * 
	 * @param timeStep
	 */
	public void setTimeStep(long timeStep){
		this.timeStep = Math.round(timeStep/1000d);
	}


	
	public ArrayList<Agent> getDependentsList(){
		return dependentsList;
	}



	public void setID(long iD) {
		ID = iD;
	}



}
