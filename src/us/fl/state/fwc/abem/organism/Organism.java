package us.fl.state.fwc.abem.organism;

import us.fl.state.fwc.abem.Agent;
import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.WorldMap;
import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;




/**Sub-models that are associated with discrete locations are typically derived from the Thing 
 * agent type. Thing agents (like Environments ...) use the asynchronous handling of time that is 
 * standard in NWS-InVitro... Thing agents can interact with all other agent types and are used 
 * to represent highly mobile entities such as animals, boats, and drift buoys."  Gray et al. (2006)
 * 
 * As in Gray et al. (2006), need to institute the major generic processes common to things here, 
 * including:
 * 		(1) decomposition & floating
 * 		(2) movement, which includes direction vector combined with current influence, 
 * 			and random movement when not searching for preferred habitat; 
 * 			here, the size and speed of individuals will determine the relative contribution 
 * 			of their own movement versus hydrodynamic forcing
 * 		(3) searching behavior, which includes searching for preferred habitat, prey items, 
 * 			schools to merge with, etc
 * 		(4) growth
 * 		(5) reproduction
 * 		(6) mortality (natural and from predation)
 * 
 * 		* attempt to follow Gray et al. as closely as possible
 */

public abstract class Organism extends Agent  {

	// main thing that Things have are location and means to assess location
	protected Coordinate coord;  
	protected Coordinate spawnSite; 
	
	protected double groupBiomass; // in grams  
	protected double nominalBiomass; // refers to "healthy" biomass that a fish should be at
	protected int groupAbundance; // total numbers
	protected int groupSex; //Female=0, Male=1
//	protected double avgTL; //average total length of the school, in cm
	
	protected WorldMap world; 
	protected int yearClass; // this is the stage of the animal, if they have different life stages 
	protected Int3D gridCellIndex; //the location of the agent in the enviroGrid
	protected long birthday = 0;
	
	protected boolean isSettler = false;

	protected double condition = 1; //general condition value, initially set to 1
	
	protected OrgMemory mem; //holds data from 'memory' (e.g., location, temp time series)
	
	
	
	
	public void initialize(Scheduler s, OrgDatagram data){

		mem = new OrgMemory(this);
		
		//set properties
		birthday = data.getBirthday();
		isSettler	 = data.isSettler();
		
		//clone or make new ones
		coord = new Coordinate(data.getCoord().x, data.getCoord().y, data.getCoord().z);
		gridCellIndex = data.getGridCellIndex().clone();

		world = s.getWorld(); 
		world.addToMap(this); 
		scheduler = s; 
		super.superInitialize(data.getFirstRunTime()); 

		// sets the agent-specific characteristics: age, biomass, etc
		setCharacteristics(data); 
		registerWithMonitors();
	}

	
	




	/**	 This finds and sets the dependents for a particular agent based on the 
	 * neighborhood of the agent, and the current time of the potential dependents.
	 * 
	 */
	@Override
	public void setDependentsList() {

		interactionTick = params.getNormalTick(); 

		if (dependentsList != null) dependentsList.clear(); 
		
		//if interactions are turned off, then no dependents
		dependentsList = world.getDependents(this) ;
		
	}





	public void setCoords(double x, double y, double z) {
		this.coord.x = x;
		this.coord.y = y; 
		this.coord.z = z; 
	}

	
	public void setGridCellIndex(Int3D gridCellIndex){
		this.gridCellIndex = gridCellIndex;
	}
	
	
	public Coordinate getCoords() {
		return coord;
	}
	
	
	public Coordinate getSpawnSite() {
		return spawnSite;
	}



	public double getCondition() {
		return condition;
	}



	public WorldMap getWorld() {
		return world;
	}

	public void setWorld(WorldMap world) {
		this.world = world;
	}

	public int getYearClass() {
		return yearClass;
	}

	public void setYearClass(int yearClass) {
		this.yearClass = yearClass;
	}

	
	public double getAgeInDays(){
		return (this.currentTime-this.birthday)/(1000*60*60*24);
	}
	
	public double getAgeInDaysAfterTimeStep(){
		return ((currentTime+timeStep*1000)-birthday)/(1000*60*60*24);
		
	}

	
	public void setBirthday(long birthday){
		this.birthday = birthday; 
	}

	public Int3D getGridIndex(){
		return gridCellIndex;
	}
	

	public double getGroupBiomass() {
		return groupBiomass;
	}

	public void setGroupBiomass(double biom){
		this.groupBiomass = biom;
	}

	public double getNominalBiomass() {
		return nominalBiomass;
	}

	public void setNominalBiomass(double biom){
		this.nominalBiomass = biom;
	}


	public int getGroupAbundance() {
		return groupAbundance;
	}

	public void setGroupAbundance(int abund) {
		this.groupAbundance = abund; 
	}

	public void setGroupSex(int groupSex) {
		this.groupSex = groupSex;
	}

	public int getGroupSex() {
		return groupSex;
	}
	
	//**************** ABSTRACT METHODS *****************

	//	public abstract void setScaleOfPerception(); 
	
	public abstract void setCharacteristics(OrgDatagram data);







	public boolean isSettler() {
		return isSettler;
	}







	public void setSettler(boolean isSettler) {
		this.isSettler = isSettler;
	} 


}
