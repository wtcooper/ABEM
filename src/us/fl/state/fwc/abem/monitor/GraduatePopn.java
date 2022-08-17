package us.fl.state.fwc.abem.monitor;

/** This class will serve as a graduating agent for population agents (see InVitro pg 39 "Ageing" section)
 * 	Purpose is to force all population agents to converge at set time (i.e., 1 year) to advance the age structure forward
 *	via matrix model approach (or similar) 
 * 
 */

public class GraduatePopn extends Monitor {

	


	@Override
	public void run() {

		for (int i=0; i<monitorees.size(); i++){
			System.out.println("\t" + this.getClassName()+ " monitoring: " + monitorees.get(i).getDescriptor()); 
			
			// for each monitoree, add a new run time to their queue which is the next time this monitor will fire
			monitorees.get(i).addTimeToRunQueue(this.timesToRunQueue.get(0)); 
		}
	}

}
