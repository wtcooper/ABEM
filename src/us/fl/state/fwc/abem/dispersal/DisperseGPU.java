package us.fl.state.fwc.abem.dispersal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.organism.Organism;

/**	Will disperse particles via the CUDA API and jCuda bindings on a CUDA-enabled GPU.
 * 
 * Considerations:
 * 
 *	(1) need to deal with different species which have different parameters
 *		- stored as base parameter constants on the GPU, and reset between doing
 *			different species
 *		- base params: 
 *   		comp periods (pre and comp), behaviors, mortality rates, preferred habitats
 *   		 
 *	(2) a particle set has a given time of release in a day so can pick up tidal influences
 * 
 * 	
 * 
 * @author wade.cooper
 *
 */
public class DisperseGPU implements Dispersal {

	public boolean partsExist = false; //this flag is set to false if no particles exist to disperse
	
	
	public DisperseGPU(){

		/*TODO
		 * (1) copy over the habitat/bathymetry raster as a texture to GPU -- this will remain constant
		 * 		unless update the habitat over time
		 */

	}

	
	//will hold a list of releases keyed to an ID (long), where map value is a double array, 
	//containing: lat, lon, numParticles, hr of release, age, status (0=in transit, 1=settled, 2=lost)
	HashMap<String, ArrayList<double[]>> releases = new HashMap<String, ArrayList<double[]>>(); 
	int numParts; 

	
	
	
	

	
	
	
	
	@Override
	public synchronized void disperse(Organism t, long numParts) {
		ArrayList<double[]> releaseList = releases.get(t.getClassName()); 
		releaseList.add(new double[] {t.getCoords().y, t.getCoords().x, numParts}); 

		
		
	}

	
	
	public void step(long runTime) {

		if (partsExist){
			//copy the appropriate hydro data over to the GPU for the time length of a timeStep (e.g., 24 or 48hrs)
				// - need to copy over both NCOM and EFDC data
				//-scale as shorts to speed up copy to and from


			//loop through each species type
			Set<String> keys = releases.keySet(); 
			Iterator<String> it = keys.iterator();
			while (it.hasNext()){
				String key = it.next();
				ArrayList<double[]> list = releases.get(key); 

				//if are some releases for this species
				if (list != null){

					//(1) Reset the base params as constants on the GPU

					//(2) Copy over the double[] array data, and run the particles for the a timeStep
						// - scale as shorts to speed up
					
					
					//(3) Once done running, copy the particle data back, which includes lat/lon, numParts, age, status
					
				}

				//(4) Check if any existing particles, and if none left, then set partsExist = false

			}

		}
	}



	@Override
	public void setScheduler(Scheduler sched) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public EnviroGrid getSeagrassGrid() {
		// TODO Auto-generated method stub
		return null;
	}




}
