package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.VerticalMigration;
import us.fl.state.fwc.abem.dispersal.bolts.util.TimeConvert;
import us.fl.state.fwc.util.geo.NetCDFFile;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;


//TODO -- set up so that have timeInterval, bins[], and vmtz[][] in BOLTParams
public class TBTextVerticalMigration implements VerticalMigration,Cloneable {

	private long timeInterval = 1;
	private String timeIntervalUnits = "Days";

	//private NetCDFBathymetry bathym;
	private NetCDFFile bathFile; 
	private String filename; 
	private BOLTSParams params; 
	
//	private MatrixUtilities mu = new MatrixUtilities();
	private RandomEngine re = new MersenneTwister64(new Date(System.currentTimeMillis()));
	private EmpiricalWalker ew;

	/*
	 * Depth bins are:	0: 0-2m		(1m) 
	 * 					1: 2-5m 		(3.5m) 
	 * 					2: 5-10m 		(7.5m) 
	 * 					3: 10-20m 		(15m) 
	 * 					4: 20-50m 		(30.5m) 
	 * 					5: 50-100m 		(75m) 
	 */
	private double[] bins = {1, 3.5, 7.5, 15, 30.5, 75};
	//22 columns for age of individuals by 5 depth bins
	private double[][] vmtx = {
			{1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0, 	1.0}, 
			{0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0}, 
			{0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0}, 
			{0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0}, 
			{0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0}, 
			{0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0, 	0.0}, 
	};
	

	public TBTextVerticalMigration(BOLTSParams params, String filename){

		this.params = params;
		this.filename = filename; 
		
		try {
			bathFile = new NetCDFFile(filename); 
		} catch (IOException e) {}
		bathFile.setInterpolationAxes(params.latVarName, params.lonVarName);
		bathFile.setVariables(params.latVarName, params.lonVarName, params.bathVarName); 
	}
	
	
	
	// *** What if we are in a shallower area than the depth range?
	
	public void apply(Particle p) {

				float blocktime = TimeConvert.convertToMillis(timeIntervalUnits, timeInterval);
				
				// Truncation is OK
				
				int column = (int) (p.getAge()/blocktime);
				
				// If age extends beyond the matrix dimensions, use the values
				// of the last column.
				
				if(column >= vmtx[0].length){
					column = vmtx[0].length-1;
				}

				//double maxDepth = bathym.getDepth(p.getX(), p.getY());
				double lon = p.getX();
				double lat = p.getY();
				if(lon>180){lon = -(360-lon);}

				Float maxDepth = (Float) bathFile.getValue(params.bathVarName, new double[]{lat, lon}, new boolean[]{false, false}, true); 
				if (maxDepth == null || maxDepth.isNaN() ){
					p.setLost(true); 
					return; 
				}
				
				float depth = Math.abs(maxDepth.floatValue()); 
				
				double[] probabilities = new double[vmtx.length];

				for (int i = 0; i < probabilities.length; i++) {
					probabilities[i] = vmtx[i][column];
				}

				ew = new EmpiricalWalker(probabilities,Empirical.NO_INTERPOLATION, re);
				int select = ew.nextInt();
				int val;
				
				//if the selected depth is greater than the max depth, then set
				// depth equal to maxDepth
				if (depth <= bins[select]){
					val = Arrays.binarySearch(bins, maxDepth);
					if(val<0){val = -(val+1);}
					p.setZ(bins[Math.max(0,val-1)]);
					
				}
				
				//else, if the current depth is greater than the selected depth, have them move up one
				else if(p.getZ()>bins[select]){
					
					val = Arrays.binarySearch(bins, p.getZ());
					if(val<0){val = -(val+1);}
					p.setZ(bins[Math.max(0,val-1)]);
				}
				
				//else, have them move down one
				else if(p.getZ()<bins[select]){
					val = Arrays.binarySearch(bins, p.getZ());
					if(val<0){val = -(val+1);}
					p.setZ(bins[Math.min(bins.length-1, val+1)]);
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

	public double[][] getVmtx() {
		return vmtx;
	}

	public void setVmtx(double[][] vmtx) {
		this.vmtx = vmtx;
	}

	public double[] getBins() {
		return bins;
	}

	public void setBins(double[] bins) {
		this.bins = bins;
	}
	
	public TBTextVerticalMigration clone(){
		TBTextVerticalMigration tvm = new TBTextVerticalMigration(params, filename);
		return tvm;
	}



	@Override
	public void closeConnections() {
		bathFile.closeFile();		
	}
}

