package us.fl.state.fwc.abem.environ.impl;

import java.io.IOException;

import ucar.ma2.Array;
import us.fl.state.fwc.util.geo.NetCDFFile;

public class LandMask {

	protected NetCDFFile ncFile; 
	private double[] lats,lons;
	short[][] landGrid; 
	
	
	/**Sets the data -- reads in a netCDF land mask, and stores the landMask variable
	 * and lats/lons as Java arrays for fast access
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public LandMask (String filename) {

		try {
			ncFile = new NetCDFFile(filename);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		ncFile.setVariables("lat", "lon", "landMask"); 
		
		landGrid = new short[ncFile.getSingleDimension("lat")][ncFile.getSingleDimension("lon")];
		
		for (int i=0; i<ncFile.getSingleDimension("lat"); i++ ){
			for (int j=0; j<ncFile.getSingleDimension("lon"); j++ ){
				Number val = ncFile.getValue("landMask", new int[]{i,j});
				landGrid[i][j] = ((Short) val).shortValue(); 
			}
		}
		
		//store the lats and lons as permanent for speed lookup
		try {
			lats = toJArray(ncFile.getVariable("lat").read());
			lons = toJArray(ncFile.getVariable("lon").read());

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	
	/**Copies netCDF arrays to java arrays
	 * 
	 * @param arr
	 * @return
	 */
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

	
	
}
