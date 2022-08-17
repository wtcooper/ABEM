package us.fl.state.fwc.abem.organism;

import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;

public class OrgDatagram {

	private String className;
	private double groupBiomass; // refers to total biomass of Animal (i.e., if individual or group);   ||||||||||||||||||| IN GRAMS |||||||||||||||||||  
	private double nominalBiomass; // refers to nominal, or "healthy" biomass that a fish should be at
	private int groupAbundance; // total numbers of group
	private int groupSex; //Female=0, Male=1; put as int because will serve as index for some FastMap's related to sex
	//private double sizeAtMaturity; //size at which this organism will reach maturity -- set ahead of time to match data 
	private Coordinate coord;  
	private int yearClass; // this is the stage of the animal, if they have different life stages (e.g., larval, juvenile, adult, and any additions in between)
	private Int3D gridCellIndex; //the location of the agent in the enviroGrid
	private long birthday;
	private long firstRunTime; //this is first run time in case pre-filters larval / EPSS for a period of time to cut down on larger nums of individuals
	private int releaseID; 
	private boolean isSettler = false; //default is false
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public double getGroupBiomass() {
		return groupBiomass;
	}
	public void setGroupBiomass(double groupBiomass) {
		this.groupBiomass = groupBiomass;
	}
	public double getNominalBiomass() {
		return nominalBiomass;
	}
	public void setNominalBiomass(double nominalBiomass) {
		this.nominalBiomass = nominalBiomass;
	}
	public int getGroupAbundance() {
		return groupAbundance;
	}
	public void setGroupAbundance(int groupAbundance) {
		this.groupAbundance = groupAbundance;
	}
	public int getGroupSex() {
		return groupSex;
	}
	public void setGroupSex(int groupSex) {
		this.groupSex = groupSex;
	}
	public Coordinate getCoord() {
		return coord;
	}
	public void setCoord(Coordinate coord) {
		this.coord = coord;
	}
	public int getYearClass() {
		return yearClass;
	}
	public void setYearClass(int yearClass) {
		this.yearClass = yearClass;
	}
	public Int3D getGridCellIndex() {
		return gridCellIndex;
	}
	public void setGridCellIndex(Int3D gridCellIndex) {
		this.gridCellIndex = gridCellIndex;
	}
	public long getBirthday() {
		return birthday;
	}
	public void setBirthday(long birthday) {
		this.birthday = birthday;
	}
	public long getFirstRunTime() {
		return firstRunTime;
	}
	public void setFirstRunTime(long firstRunTime) {
		this.firstRunTime = firstRunTime;
	}
	public int getReleaseID() {
		return releaseID;
	}
	public void setReleaseID(int releaseID) {
		this.releaseID = releaseID;
	}
	public boolean isSettler() {
		return isSettler;
	}
	public void setIsSettler(boolean isSettler) {
		this.isSettler = isSettler;
	}

	
	
	
}
