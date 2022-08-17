package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.VerticalMigration;
import us.fl.state.fwc.util.geo.NetCDFFile;

/**
 * Simple verticle migration based on whether the tide is incoming or outgoing.  Need to set
 * @author wade.cooper
 *
 */
public class SeatroutVerticalMigration implements VerticalMigration,Cloneable {

	private double preferredIncomingDepth = 0; 
	private double preferredOutgoingDepth = 10; 

	private boolean followTides = false; 

	private double vertMigSpeed = 0.005; // vertical migration speed in m/s 

	private long timeInterval = 1;
	private String timeIntervalUnits = "Days";

	//private NetCDFBathymetry bathym;
	private NetCDFFile bathFile; 
	private String filename; 
	private BOLTSParams params; 



	//private double[] bins = {0, 0.25, 0.5, 0.75, 1, 1.5, 2, 3, 5, 7.5, 10, 15, 20, 30, 40, 50, 75, 100, 200, 300, 500};



	public SeatroutVerticalMigration(BOLTSParams params, String filename){

		this.params = params;
		this.filename = filename; 

		try {
			bathFile = new NetCDFFile(filename); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		bathFile.setInterpolationAxes(params.latVarName, params.lonVarName);
		bathFile.setVariables(params.latVarName, params.lonVarName, params.bathVarName); 
	}



	public void apply(Particle p) {

		double ageInHrs =p.getAge()/(60*60*1000); 

		//if less than 18hrs old, then eggs havent' hatched and are still floating
		if (ageInHrs < 18) {
			p.setZ(0.0);
			return; 
		}


		/*If set to follow the tides, then base their location on whether the
		 * tides are incoming or outgoing -- move to surface if incoming,
		 * and move to bottom if outgoing
		 */
		if (followTides){
			//double maxDepth = bathym.getDepth(p.getX(), p.getY());
			double lon = p.getX();
			double lat = p.getY();
			if(lon>180){lon = -(360-lon);}

			Float maxDepthNum = (Float) bathFile.getValue(params.bathVarName, new double[]{lat, lon}, new boolean[]{false, false}, true); 
			if (maxDepthNum == null || maxDepthNum.isNaN() ){
				p.setLost(true); 
				return; 
			}

			//take absolute value so is positive, in case units are negative in netCDF file
			float maxDepth = Math.abs(maxDepthNum.floatValue()); 

			//****This is if following tide
			//if particle was last moving east, than assume tide was going in, so they want to swim upwards
			double preferredDepth = 0; 
			if (p.getU()>0 /*.isIncomingTide()*/) preferredDepth = preferredIncomingDepth; 
			else preferredDepth = preferredOutgoingDepth;

			//move towards preferredDepth
			if (p.getZ() < preferredDepth){
				//add the total distance they can swim in the time step
				preferredDepth = Math.min(p.getZ() + (params.releaseTimeStep/1000.0)*vertMigSpeed, maxDepth);  
			}
			else if (p.getZ() > preferredDepth){
				//substract the total distance they can swim in the time step
				preferredDepth = Math.max(0, p.getZ() - (params.releaseTimeStep/1000.0)*vertMigSpeed);  
			}

			p.setZ(preferredDepth);
			return; 
		}

		/*if not following tides, then do a simple behavior where stay at 
		 * surface until 4 days old, then move downwards and look for 
		 * settlement location
		 */
		else {

			//if less than 4 days old, then stay on surface
			if (ageInHrs < 4*24){
				p.setZ(0.0);
				return;
			}

			//else, swim towards bottom
			else {

				//double maxDepth = bathym.getDepth(p.getX(), p.getY());
				double lon = p.getX();
				double lat = p.getY();
				if(lon>180){lon = -(360-lon);}

				Float maxDepthNum = (Float) bathFile.getValue(params.bathVarName, new double[]{lat, lon}, new boolean[]{false, false}, true); 
				if (maxDepthNum == null || maxDepthNum.isNaN() ){
					p.setLost(true); 
					return; 
				}

				//take absolute value so is positive, in case units are negative in netCDF file
				float maxDepth = Math.abs(maxDepthNum.floatValue()); 

				//****This is if following tide
				//if particle was last moving east, than assume tide was going in, so they want to swim upwards
				//		double preferredDepth = 0; 
				//		if (p.getU()>0 /*.isIncomingTide()*/) preferredDepth = preferredIncomingDepth; 
				//		else preferredDepth = preferredOutgoingDepth;

				//****This is if hard-coding to pefer deeper depths since are older
				double preferredDepth = preferredOutgoingDepth; 

				//move towards preferredDepth
				if (p.getZ() < preferredDepth){
					//add the total distance they can swim in the time step
					preferredDepth = Math.min(p.getZ() + (params.releaseTimeStep/1000.0)*vertMigSpeed, maxDepth);  
				}
				else if (p.getZ() > preferredDepth){
					//substract the total distance they can swim in the time step
					preferredDepth = Math.max(0, p.getZ() - (params.releaseTimeStep/1000.0)*vertMigSpeed);  
				}
				p.setZ(preferredDepth);

			}
		}
	}


	




	public long getTimeInterval() {
		return timeInterval;
	}

	public void setTimeInterval(long timeInterval) {
		this.timeInterval = timeInterval;
	}

	public String getTimeIntervalUnits() {
		return timeIntervalUnits;
	}

	public void setTimeIntervalUnits(String timeIntervalUnits) {
		this.timeIntervalUnits = timeIntervalUnits;
	}




	public double getPreferredIncomingDepth() {
		return preferredIncomingDepth;
	}



	public void setPreferredIncomingDepth(double preferredIncomingDepth) {
		this.preferredIncomingDepth = preferredIncomingDepth;
	}



	public double getPreferredOutgoingDepth() {
		return preferredOutgoingDepth;
	}



	public void setPreferredOutgoingDepth(double preferredOutgoingDepth) {
		this.preferredOutgoingDepth = preferredOutgoingDepth;
	}





	public double getVertMigSpeed() {
		return vertMigSpeed;
	}



	public void setVertMigSpeed(double vertMigSpeed) {
		this.vertMigSpeed = vertMigSpeed;
	}


	public SeatroutVerticalMigration clone(){
		SeatroutVerticalMigration tvm = new SeatroutVerticalMigration(params, filename);
		tvm.setPreferredIncomingDepth(this.preferredIncomingDepth);
		tvm.setPreferredOutgoingDepth(this.preferredOutgoingDepth);
		tvm.setVertMigSpeed(this.vertMigSpeed);
		return tvm;
	}



	@Override
	public void closeConnections() {
		bathFile.closeFile(); 
	}


}


