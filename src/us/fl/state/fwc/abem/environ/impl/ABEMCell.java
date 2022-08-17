package us.fl.state.fwc.abem.environ.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import us.fl.state.fwc.abem.environ.EnviroCell;
import us.fl.state.fwc.abem.monitor.FishTracker;
import us.fl.state.fwc.abem.organism.Fish;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.algorithm.CentroidArea;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class ABEMCell implements EnviroCell {

	private ABEMGrid grid; 
	private Geometry geom; 
	private boolean activeCell = false; 

	private double genericQuality=1; //generic environmental quality term for testing/theoretical
	
	//####################
	//enviro/habitat properties
	//####################
	private double rugosity;
	private double depth; 
	private double roughness;
	private int habType;
	private Int3D index;
	private int numLayers; 
	
	
	//####################
	//LTL concentration variables
	//####################
	private double nitrogen; //concentration per mL
	private double phosphorous; //concentration per mL
	private double silica; //concentration per mL
	private double phytoplankton; //density per mL
	private double zooplankton; //density per mL

	
	
	//####################
	//data storage
	//####################
	//stores the total number of recruits
	private TreeMap<String, Double> numRecruits; 
	//stores the yearly average for abundance, biomass, and SSB
	private TreeMap<String, double[]> avgNums;
	//stores the current adult abundance, biomass, and SSB (indices 0,1,2) and num settlers (3)
	private TreeMap<String, double[]> currentNums;
	//stores the number of recruits
	private TreeMap<String, Double> TEP; 
	
	//holds all organisms in the cell, mapped by className
	private HashMap<String, ArrayList<Organism>> agentList;

	private HashMap<String, Organism> maleMergers;
	private HashMap<String, Organism>  femaleMergers; 
	
	//hashmap of year as the key, and double as percent cover
	private HashMap<Integer, Double> savCov; // = new HashMap<Integer, Double>(); 
	private HashMap<Short, ArrayList<ABEMCell>> reachableCells;
	
	public ABEMCell(ABEMGrid grid){
		this.grid = grid;
	}

	
	public Geometry getGeom() {
		return geom;
	}
	public void setGeom(Geometry geom) {
		this.geom = geom;
	}
	public double getRugosity() {
		return rugosity;
	}
	public void setRugosity(double rugosity) {
		this.rugosity = rugosity;
	}
	public double getDepth() {
		return depth;
	}
	
	public void setDepth(double depth) {
		this.depth = depth;
	}
	
	/**Returns the layer number for a given depth
	 * Note: depth should be provided as positive down
	 * 
	 * @param depth
	 * @return
	 */
	public int getDepthLayer(double depth){
		return (int) (depth/(this.depth/numLayers)); 
	}
	
	/**Returns the depth (positive down) at the centroid of a given layer
	 * 
	 * @param layer
	 * @return
	 */
	public double getLayerCentroid(int layer){
		double layerThickness = depth/(double) numLayers; 
		return layerThickness*layer + layerThickness/2.0; 
	}
	
	public double getRoughness() {
		return roughness;
	}
	
	public void setRoughness(double roughness) {
		this.roughness = roughness;
	}

	public Int3D getIndex() {
		return index;
	}
	
	public void setIndex(Int3D index) {
		this.index = index;
	}

	public Coordinate getCentroidCoord(){
	      CentroidArea cent = new CentroidArea();
	      cent.add(geom);
	      Coordinate centPt = cent.getCentroid();
	      centPt.z = 0;
		return centPt; 
	}
	
	/**Sets the number of vertical layers used in the model
	 * 
	 * @param numLayers
	 */
	public void setNumLayers(int numLayers){
		this.numLayers = numLayers;
	}
	public int getHabType() {
		return habType;
	}
	public void setHabType(int habType) {
		this.habType = habType;
	}
	public HashMap<Integer, Double> getSavCov() {
		
		
		return savCov;
	}
	public void setSavCov(HashMap<Integer, Double> savCov) {
		this.savCov = savCov;
	}

	@Override
	public ArrayList<EnviroCell> getReachableCells(int searchRadius) {
		
		ArrayList<EnviroCell> cellList = new ArrayList<EnviroCell>();
		Set<Short> keys = reachableCells.keySet();
		Iterator<Short> it = keys.iterator();
		while (it.hasNext()){
			short radiusIndex = it.next();
			if ( radiusIndex<= searchRadius ) {
				ArrayList<ABEMCell> cellsInNeigh = reachableCells.get(radiusIndex);
		
				for (int i=0; i<cellsInNeigh.size(); i++){
					cellList.add(cellsInNeigh.get(i));
				}
			}
		}
		return cellList;
	}


	
	
	/**Returns the reachable cells map
	 * 
	 * @return
	 */
	public HashMap<Short, ArrayList<ABEMCell>> getReachableCellsMap(){
		return this.reachableCells;
	}
	
	
	
	

	/**Sets the reachable cells map
	 * 
	 * @param reachableCells
	 */
	public void setReachableCells(HashMap<Short, ArrayList<ABEMCell>> reachableCells) {
		this.reachableCells = reachableCells;
	}
	
	
	
	
	
	
	
	@Override
	public void setAvgNumbers(String classname, int year, double[] newNums) {

		
		/*Here, 
		 * data[0]=avgAbund 
		 * data[1]=avgTotalBiomass
		 * data[2]=avgSSB
		 * data[3]=numObservations	
		 */
		if (avgNums == null) avgNums = new TreeMap<String, double[]>(); 
		if (currentNums == null) currentNums = new TreeMap<String, double[]>(); 

		String key = classname + "_" + year;

		if (avgNums.containsKey(key)) {

			//set the avg numbers
			double[] data = avgNums.get(key);
			data[3]++; //increment numObservations

			//average existing numbers
			data[0] = (data[0]*(data[3]-1)+newNums[0])/data[3]; 
			data[1] = (data[1]*(data[3]-1)+newNums[1])/data[3]; 
			data[2] = (data[2]*(data[3]-1)+newNums[2])/data[3]; 
			
			if (grid.getMaxAbundance() < data[0]) grid.setMaxAbundance(data[0]);
			if (grid.getMaxBiomass() < data[1]) grid.setMaxBiomass(data[1]);
			if (grid.getMaxSSB() < data[2]) grid.setMaxSSB(data[2]);

			//set the current numbers
			double[] currentData = currentNums.get(classname);
			currentData[0] = newNums[0];
			currentData[1] = newNums[1];
			currentData[2] = newNums[2];
			currentData[3] = newNums[3];
			currentData[4] = newNums[4];
			//NOTE: don't do newNums[3] since this is set in addSettlers
			
			
		}
		else {

			//set avg numbers
			if (grid.getMaxAbundance() < newNums[0]) grid.setMaxAbundance(newNums[0]);
			if (grid.getMaxBiomass() < newNums[1]) grid.setMaxBiomass(newNums[1]);
			if (grid.getMaxSSB() < newNums[2]) grid.setMaxSSB(newNums[2]);

			// add new map key value with appropriate biomass
			avgNums.put(key, new double[]{newNums[0], newNums[1], newNums[2], 1}); 

			//set the current numbers:
			//[0] adult abund, [1] adult biomasss, [2] adult SSB , [3] num settlers
			double[] currentData = currentNums.get(classname);
			if (currentData == null) {
				currentData =new double[5]; 
				currentNums.put(classname, currentData);
			}
			currentData[0] = newNums[0];
			currentData[1] = newNums[1];
			currentData[2] = newNums[2];
			currentData[3] = newNums[3];
			currentData[4] = newNums[4];
		}
		
	}

	
	
	
	
	@Override
	public void setNumRecruits(String classname, int year, double numNewRecruits) { 

		//numNewRecruits *= SchedulerParams.scaleFactor;
		
		if (numRecruits == null) numRecruits = new TreeMap<String, Double>(); 

		
		String key = classname + "_" + year;
		if (numRecruits.containsKey(key)) {
			double val = numRecruits.get(key).doubleValue();
			val +=numNewRecruits;
			numRecruits.put(key, val);
			
			if (grid.getMaxRecruitment() < val) grid.setMaxRecruitment(val);
		}
		else {
			// add new map key value with appropriate biomass
			numRecruits.put(key, numNewRecruits); 

			if (grid.getMaxRecruitment() < numNewRecruits) 
				grid.setMaxRecruitment(numNewRecruits);
		}
		
		if (!activeCell) {
			FishTracker tracker = (FishTracker) grid.scheduler.getMonitors().get("FishTracker");
			tracker.addActiveCell(this);
			activeCell = true;
		}
	}


	
	
	
	
	
	
	@Override
	public void setTEP(String classname, int year, double newTEP) {
		//newTEP *= SchedulerParams.scaleFactor;
		
		if (TEP == null) TEP = new TreeMap<String, Double>(); 

		String key = classname + "_" + year;
		if (TEP.containsKey(key)) {
			double val = TEP.get(key).doubleValue();
			val +=newTEP;
			TEP.put(key, val);
			
			if (grid.getMaxTEP() < val) grid.setMaxTEP(val);
		}
		else {
			// add new map key value with appropriate biomass
			TEP.put(key, newTEP); 

			if (grid.getMaxTEP() < newTEP) grid.setMaxTEP(newTEP);
		}
		
		if (!activeCell) {
			FishTracker tracker = (FishTracker) grid.scheduler.getMonitors().get("FishTracker");
			tracker.addActiveCell(this);
			activeCell = true;
		}
	}

	
	
	@Override
	public double getNumRecruits(String classname, int year) {
		if (numRecruits == null) return 0; 
		
		Double val = numRecruits.get(classname + "_" + year); 
		if (val == null) return 0;
		else return val.doubleValue();
	}


	@Override
	public double getTEP(String classname, int year) {
		if (TEP == null) return 0;
		
		Double val = TEP.get(classname + "_" + year);
		if (val == null) return 0; 
		else return val.doubleValue();
	}


	@Override
	public double getAvgAbundance(String classname, int year) {
		if (avgNums == null) return (double) 0;
		return avgNums.get(classname+ "_" + year)[0];
	}


	@Override
	public double getAvgBiomass(String classname, int year) {
		if (avgNums == null) return (double) 0;
		return avgNums.get(classname+"_" + year)[1];
	}


	@Override
	public double getAvgSSB(String classname, int year) {
		if (avgNums == null) return (double) 0;
		return avgNums.get(classname+"_" + year)[2];
	}

	
	@Override
	public double getAbundance(String classname) {
		if (currentNums == null) return (double) 0;
		return currentNums.get(classname)[0];
	}


	@Override
	public double getBiomass(String classname) {
		if (currentNums == null) return (double) 0;
		return currentNums.get(classname)[1];
	}


	@Override
	public double getSSB(String classname) {
		if (currentNums == null) return (double) 0;
		return currentNums.get(classname)[2];
	}

	@Override
	public double getNumSettlers(String classname) {
		if (currentNums == null) return (double) 0;
		return currentNums.get(classname)[3];
	}
	
	@Override
	public double getNumAdults(String classname) {
		if (currentNums == null) return (double) 0;
		return currentNums.get(classname)[4];
	}

	
	@Override
	public boolean isActive() {
		return activeCell;
	}


	@Override
	public void setIsActive(boolean isActive) {
		this.activeCell = isActive;
	}
	
	//#############################################
	//Methods to handle agent list
	//#############################################
	
	public void addAgent(Organism agent){
		if (agentList == null) agentList = new HashMap<String, ArrayList<Organism>>(); 
		ArrayList<Organism> list = agentList.get(agent.getClassName());
		if (list == null){
			list = new ArrayList<Organism>();
			agentList.put(agent.getClassName(), list);
		}
		list.add(agent);
	}
	
	public void removeAgent(Organism agent){
		if (agentList == null) return; 
		ArrayList<Organism> list = agentList.get(agent.getClassName());
		if (list == null || list.isEmpty()) return;
		else list.remove(agent);
	}
	
	public ArrayList<Organism> getAgents(String className){
		if (agentList == null) return null; 
		return agentList.get(className);
	}
	
	
	
	//#############################################
	//Methods to handle settlers list
	//#############################################
	
/*	public void addSettlers(String classname, long num){
		if (currentNums == null) currentNums = new TreeMap<String, double[]>(); 
		double[] nums;
		if (currentNums.containsKey(classname)) nums = currentNums.get(classname);
		else  {
			nums = new double[4];  
			currentNums.put(classname, nums); //only add if new
		}
		nums[3] += num;
		
	}
	
	public void removeSettlers(String classname, long num){
		if (currentNums == null) return; 
		if (currentNums.containsKey(classname)){
			double[] nums = currentNums.get(classname);
			nums[3] -= num;
			currentNums.put(classname, nums);
		}
		//else return
	}

	
	public long getNumSettlers(String classname){
		if (currentNums == null) return 0;  
		double[] nums =currentNums.get(classname);
		if (nums == null) return 0;
		else return (long) nums[3];
	}
*/

	
	//#############################################
	//Methods to handle merging settlers into existing settler agents
	//#############################################


	/**
	 * Return the existing agent to be merged into.
	 * 
	 * @param classname
	 * @param sex
	 */
	public Organism getMerger(String classname, int sex){
		if (sex == 0) {
			if (femaleMergers == null) return null;
			Organism femaleMerger = femaleMergers.get(classname);
			if (femaleMerger == null) return null;
			else return femaleMerger;
		}
		else {
			if (maleMergers == null) return null;
			Organism maleMerger = maleMergers.get(classname);
			if (maleMerger == null) return null;
			else return maleMerger;
		}
	}
	

	/**
	 * Replace the existing agent to be merged into with the newbee.
	 * 
	 * @param classname
	 * @param newbee
	 */
	public void setMerger(String classname, Organism newbee) {
		
		if (newbee.getGroupSex() == 0) {
			if (femaleMergers == null)  femaleMergers = new HashMap<String, Organism>();  
			femaleMergers.put(classname, newbee);
		}
		else {
			if (maleMergers == null)  maleMergers = new HashMap<String, Organism>();  
			maleMergers.put(classname, newbee);
		}
	}


	public double getNitrogen() {
		return nitrogen;
	}


	public void setNitrogen(double nitrogen) {
		this.nitrogen = nitrogen;
	}


	public double getPhosphorous() {
		return phosphorous;
	}


	public void setPhosphorous(double phosphorous) {
		this.phosphorous = phosphorous;
	}


	public double getSilica() {
		return silica;
	}


	public void setSilica(double silica) {
		this.silica = silica;
	}


	public double getPhytoplankton() {
		return phytoplankton;
	}


	public void setPhytoplankton(double phytoplankton) {
		this.phytoplankton = phytoplankton;
	}


	public double getZooplankton() {
		return zooplankton;
	}


	public void setZooplankton(double zooplankton) {
		this.zooplankton = zooplankton;
	}


	public double getGenericQuality() {
		return genericQuality;
	}


	public void setGenericQuality(double genericQuality) {
		this.genericQuality = genericQuality;
	}
	
	
	
}
