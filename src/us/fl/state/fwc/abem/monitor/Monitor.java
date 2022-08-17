package us.fl.state.fwc.abem.monitor;

import java.util.ArrayList;

import us.fl.state.fwc.abem.Agent;
import us.fl.state.fwc.abem.Scheduler;

public abstract class Monitor extends Agent {

	protected ArrayList<Agent> monitorees = new ArrayList<Agent>(); 

	
	public void initialize(Scheduler s){
		scheduler = s; 
		scheduler.addMonitor(this); 

		super.superInitialize(0); 
		
	}
	

	@Override
	public void registerWithMonitors() {
		// nothing here now since not planning on having monitors of monitors, but this could happen 
	}
	
	public void addMonitoree(Agent agent){
		monitorees.add(agent);
	}
	
	public void removeMonitoree(Agent agent){
		monitorees.remove(agent); 
	}
	
	
	@Override
	public void setDependentsList() {
		// won't set dependents in agent.getNextRunTime, but will use this to get dependents for "step" method 
	}

	
	
	
}
