package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.util.Arrays;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import us.fl.state.fwc.abem.dispersal.bolts.Bathymetry;
import us.fl.state.fwc.util.TestingUtils;

public class NetCDFBathymetry implements Bathymetry{
	
	private NetcdfFile bathymetry;
	private Variable var;
	//private String variableName = "water_depth";
	//private String latName = "lat";
	//private String lonName = "lon";
	//private boolean neglon = true;
	private double[] lats,lons;
	private Array bth;
	private Index ind;
	private BOLTSParams params; 
	
	public NetCDFBathymetry(BOLTSParams params, String filename) throws IOException{
		this.params = params; 
		bathymetry = NetcdfFile.open(filename);
		var = bathymetry.findVariable(params.bathVarName);
		bth = var.read();
		ind = bth.getIndex();
	}
	
	public NetCDFBathymetry(BOLTSParams params, String filename, String latname, String lonname) throws IOException{
		this.params = params; 
		bathymetry = NetcdfFile.open(filename);
		var = bathymetry.findVariable(params.bathVarName);
		bth = var.read();
		ind = bth.getIndex();
		lats = toJArray(bathymetry.findVariable(params.latVarName).read());
		lons = toJArray(bathymetry.findVariable(params.lonVarName).read());

	}
	
	public void initialize() throws IOException{
		
	lats = toJArray(bathymetry.findVariable(params.latVarName).read());
	lons = toJArray(bathymetry.findVariable(params.lonVarName).read());
	}
	
	public double getDepth(double x, double y){
		
		if(params.negBathymCoord){
			x = (x+180)%360-180;
		}

		ind.set(locate(lats,y),locate(lons,x));
		return bth.getDouble(ind);
	}
	
	public int locate(double[] array, double val){
		int idx = Arrays.binarySearch(array, val);

		if (idx < 0) {

			// Error check

			if (idx == -1) {
				throw new IllegalArgumentException(var.getName() + " value "
						+ val + " does not fall in the range " + array[0] + " : "
						+ array[array.length - 1] + ".");
			}
		
			return -(idx+2);
		}

		return idx;
	}
	
	public int locate(float[] array, float val){
		int idx = Arrays.binarySearch(array, val);

		if (idx < 0) {

			// Error check

			if (idx == -1) {
				throw new IllegalArgumentException(var.getName() + " value "
						+ val + " does not fall in the range " + array[0] + " : "
						+ array[array.length - 1] + ".");
			}
		
			return -(idx+2);
		}

		return idx;
	}

	private double[] toJArray(Array arr){
		double[] ja;
		if (arr.getElementType() == Float.TYPE) {

			float[] fa = (float[]) arr.copyTo1DJavaArray();
			ja = new double[fa.length];
			for (int i = 0; i < ja.length; i++) {
				ja[i] = fa[i];
			}
		return ja;
		} else {

			return (double[]) arr.copyTo1DJavaArray();
		}
	}


	
	public NetCDFBathymetry clone() {
		NetCDFBathymetry ncb;
		try {
			ncb = new NetCDFBathymetry(this.params, bathymetry.getLocation());
			double[] tlats = new double[lats.length];
			for(int i = 0; i < lats.length; i++){
				tlats[i] = lats[i];
			}
			double[] tlons = new double[lons.length];
			for(int i = 0; i < lons.length; i++){
				tlons[i] = lons[i];
			}
			ncb.lats = tlats;
			ncb.lons = tlons;
			return ncb;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		}
		return null;
	}

	public void setParams(BOLTSParams params) {
		this.params = params;
	}
	
	public BOLTSParams getParams() {
		return params ;
	}	
	
	public String getFilename(){
		return bathymetry.getLocation(); 
	}
}

