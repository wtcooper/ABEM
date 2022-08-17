package us.fl.state.fwc.abem.monitor;


import java.util.HashMap;

import us.fl.state.fwc.abem.organism.Fish;


public class AnimalBiomass extends Monitor {

	HashMap<String, Double > biomassTotals = new HashMap<String,Double>(); 
	


	@Override
	public void run() {

		biomassTotals.clear(); // clear at the beginning of step to set 
		for (int i=0; i<monitorees.size(); i++){
			Fish animal = (Fish) monitorees.get(i); 
			//System.out.println("\t" + this.getClassName()+ " monitoring: " + animal.getDescriptor()); 
			setBiomassTotalsMap(animal.getClassName(), animal.getGroupBiomass()); 
			// for each monitoree, add a new run time to their queue which is the next time this monitor will fire
			monitorees.get(i).addTimeToRunQueue(this.timesToRunQueue.get(0)); 
		}

/*		Set<String> keys = biomassTotals.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()){
			String key = it.next();	
	          //System.out.println("\t" + key + " class has a total biomass of " + biomassTotals.get(key)); // No typecast necessary.
			
		}
*/		
	}
	
	

	
	public void setBiomassTotalsMap(String className, double biomass){
		if (biomassTotals.containsKey(className)) {
			// update existing biomass value for className class 
			double tempBiomass = biomassTotals.get(className) ;
			tempBiomass += biomass;
			biomassTotals.put(className, tempBiomass); 
		}
		else {
			// add new map key value with appropriate biomass
			biomassTotals.put(className, biomass); 
		}
	}


	
	public double getTotalBiomass(String className){
		return biomassTotals.get(className); 
	}
	
	
	/**	Returns the total biomass of all monitoree agents of the same class type that this monitor is monitoring.  
	 * 
	 * @param className of the Agent type
	 * @return
	 */
/*	public double getTotalBiomass(String className){
		double totBiomass = 0;
		for (int i=0; i<monitorees.size(); i++){
			if ( (className.equals(monitorees.get(i).getClassName(monitorees.getClass())))   &&   (monitorees.get(i) instanceof Animal)  ){
				Animal animal = (Animal) monitorees.get(i); 
				totBiomass += animal.getBiomass(); 
			}
		}
		return totBiomass; 
		
	}
*/
	
}
