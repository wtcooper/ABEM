package us.fl.state.fwc.abem.dispersal.bolts;

import java.util.HashSet;
import java.util.Set;

import us.fl.state.fwc.abem.dispersal.bolts.impl.BOLTSParams;

/**
 * Generic particle class.
 * 
 * @author Johnathan Kool
 * 
 */

public class Particle {

	private BOLTSParams params; 
	private double x, y, z;
	private double px, py, pz; // Previous values - useful for debugging and
								// intersections
	
	private double u = 0d, v = 0d;
	private long t;
	private long birthday = 0;
	private boolean settled = false;
	private boolean dead = false;
	private boolean lost = false;
	private boolean nodata = false;
	private boolean nearNoData = false;
	private boolean hadNoData = false;
	private double distance = 0.0d;
	private long id;
	private String source;
	private String destination;
	private Set<Long> visited = new HashSet<Long>();
	private boolean incomingTide; 

	public Particle(){}
	
	public Particle(BOLTSParams params){
		this.params = params; 
	}
	
	
	public Set<Long> getVisited() {
		return visited;
	}

	public void setVisited(Set<Long> visited) {
		this.visited = visited;
	}

	public long getAge(){
		return this.t-this.birthday;
	}
	
	public double getAgeInDays(){
		return ((double)getAge()/(1000*60*60*24));
	}

	public long getBirthday() {
		return birthday;
	}

	/**
	 * Retrieve the current coordinates of the particle
	 * 
	 * @return
	 */

	public double[] getCoords() {

		return new double[] { x, y };
	}
	
	public String getDestination() {
		return destination;
	}
	
	public double getDistance() {
		return distance;
	}
	
	/**
	 * Retrieve the unique identifier of the particle
	 * 
	 * @return
	 */

	public long getID() {
		return id;
	}

	public boolean getNodata() {
		return nodata;
	}

	/**
	 * Retrieve the Previous X Coordinate of the particle
	 * 
	 * @return
	 */

	public double getPX() {
		return px;
	}

	/**
	 * Retrieve the Previous Y Coordinate of the particle
	 * 
	 * @return
	 */
	
	public double getPY() {
		return py;
	}
	
	/**
	 * Retrieve the Previous Z Coordinate of the particle
	 * 
	 * @return
	 */
	
	public double getPZ() {
		return pz;
	}
	
	public String getSource() {
		return source;
	}

	/**
	 * Retrieve the Time Coordinate of the particle
	 * 
	 * @return
	 */
	
	public long getT() {
		return t;
	}
	
	/**
	 * Retrieve the last E-W velocity of the particle
	 * 
	 * @return
	 */

	public double getU() {
		return u;
	}

	/**
	 * Retrieve the last N-S velocity of the particle
	 * 
	 * @return
	 */

	public double getV() {
		return v;
	}
	
	/**
	 * Retrieve the X coordinate of the particle
	 * 
	 * @return
	 */

	public double getX() {
		return x;
	}
	
	/**
	 * Retrieve the Y coordinate of the particle
	 * 
	 * @return
	 */

	public double getY() {
		return y;
	}

	/**
	 * Retrieve the Z Coordinate of the particle
	 * 
	 * @return
	 */

	public double getZ() {
		return z;
	}

	public boolean isDead() {
		return dead;
	}

	public boolean isLost() {
		return lost;
	}

	public boolean isNearNoData() {
		return nearNoData;
	}

	public boolean isSettled() {
		return settled;
	}

	public void setBirthday(long birthday) {
		this.birthday = birthday;
	}

	public void setDead(boolean dead) {
		this.dead = dead;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	/**
	 * Sets the unique ID of the particle
	 * 
	 * @param id
	 */
	
	public void setID(long id) {
		this.id = id;
		if (params != null && params.drawParticles) {
			params.addToMap(this); 
		}
	}

	public void setLost(boolean lost) {
		this.lost = lost;
		if (params != null && params.drawParticles) {
			params.removeFromMap(this);
			params.updateMap(this); 
		}
	}

	public void setNearNoData(boolean nearNoData) {
		this.nearNoData = nearNoData;
	}

	public void setNodata(boolean nodata) {
		this.nodata = nodata;
		if(nodata){this.hadNoData = true;}
	}

	public void setPX(double px) {
		this.px = px;
	}

	public void setPY(double py) {
		this.py = py;
	}

	public void setPZ(double pz) {
		this.pz = pz;
	}

	public void setSettled(boolean settled) {
		this.settled = settled;
	}
	
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Sets the T coordinate of the particle
	 * 
	 * @param t
	 */

	public void setT(long t) {
		this.t = t;
	}

	public void setU(double u) {
		this.u = u;
	}

	public void setV(double v) {
		this.v = v;
	}

	/**
	 * Sets the X coordinate of the particle
	 * 
	 * @param x
	 */

	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Sets the Y coordinate of the particle
	 * 
	 * @param y
	 */
	
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * Sets the Z coordinate of the particle
	 * 
	 * @param z
	 */

	public void setZ(double z) {
		this.pz = this.z;
		this.z = z;
	}

	public boolean wasNoData() {
		return hadNoData;
	}

	public boolean isIncomingTide() {
		return incomingTide;
	}

	public void setIncomingTide(boolean incomingTide) {
		this.incomingTide = incomingTide;
	}
}
