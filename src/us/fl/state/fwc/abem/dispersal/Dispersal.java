package us.fl.state.fwc.abem.dispersal;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.organism.Organism;


/**	This interface defines the type of dispersal to be represented, whether it be non-physical (e.g., random allocation to locations), connectivity matrix approach, full hydrodynamic with particle tracking, or 
 * polynomial pseudo-hydrodynamic with particle tracking (e.g., as in Gray et al. 2006)
 * 
 * @author wade.cooper
 *
 */
public interface Dispersal {

	
	public void disperse(Organism t, long numParts); // this will perform actions of dispersal  
	
	public void setScheduler(Scheduler sched); 

	public EnviroGrid getSeagrassGrid();
}
