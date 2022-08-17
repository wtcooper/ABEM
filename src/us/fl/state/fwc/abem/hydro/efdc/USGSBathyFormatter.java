package us.fl.state.fwc.abem.hydro.efdc;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleMapper;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class USGSBathyFormatter {


	int latSize; 
	int lonSize;
	double gridXDim = 0.00049;
	double gridYDim = 0.00044; 
	Coordinate originNode = new Coordinate(0, 0, 0);

	String inFileName = "data/USGSBathUTMClip_latFlipped.nc";
	String outFileName = "data/USGSBathWGSClip.nc"; 	

	String bathName = "water_depth"; 
	String lonVarName = "lon"; 
	String latVarName = "lat"; 



	public void writeFile() throws IOException, InvalidRangeException {

		NetCDFFile inFile = null;
		try {
			inFile = new NetCDFFile(inFileName);
		} catch (IOException e) {}

		inFile.setInterpolationAxes("lat", "lon");
		inFile.setVariables("lat", "lon", "water_depth"); 

		//bring it in 2 indexes so can do interpolation
		Coordinate origin = new Coordinate(inFile.getValue("lon", new int[] {0}).doubleValue(), inFile.getValue("lat", new int[] {0}).doubleValue(), 0);
		CoordinateUtils.convertUTMToLatLon(origin, 17, false); 

		int latSize = inFile.getSingleDimension("lat");
		int lonSize = inFile.getSingleDimension("lon"); 

		//Coordinate maxBounds = new Coordinate(inFile.getValue("x", new int[] {lonSize-1}).doubleValue(), inFile.getValue("y", new int[] {latSize-1}).doubleValue(), 0);

		//delete existing file
		new File(outFileName).delete();

		NetcdfFileWriteable ncFile =NetcdfFileWriteable.createNew(outFileName);
		//ncFile.setFill(true);	// this will fill in with blank values, as below
		//ncFile.setLargeFile(true); //NOTE: for whatever reason, when I set this = true, I cant open file in ncBrowse!!!

		String latUnits = "degrees_north";
		String lonUnits = "degrees_east";
		//String bathUniits = "meters"; 

		Dimension[] dim = new Dimension[2];

		dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latSize, false);
		dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonSize, false);

		ncFile.addVariable(bathName, DataType.FLOAT, dim);

		ncFile.addVariableAttribute(bathName, "units", "meters");
		//ncFile.addVariableAttribute(bathName, "_FillValue", -9999);
		ncFile.addVariableAttribute(bathName, "missing_value", inFile.getMissingValue("water_depth").floatValue());

		//ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(latVarName, "missing_value", inFile.getMissingValue("water_depth").floatValue());

		//ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(lonVarName, "missing_value", inFile.getMissingValue("water_depth").floatValue());

		ncFile.create();



		// write the lat and lon index values out (here, leave as index values since grid is rotated and hence not regular and parallel with UTM lines
		ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
		ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());



		double lonSum = origin.x;
		double latSum = origin.y; 

		for (int i=0; i<dim[0].getLength(); i++) {
			lonSum = origin.x;
			for (int j=0; j<dim[1].getLength(); j++) {
				
				//get the bathymetry data interpolated
				float[] floatStorage = new float[1]; 
				Coordinate temp = new Coordinate(lonSum, latSum, 0);
				CoordinateUtils.convertLatLonToUTM(temp, 17); 
				System.out.println(i + "\t" + j + "\t" + temp.y + "\t" + temp.x);

				Float tempValue =null; 
				try {
					tempValue =  (Float) inFile.getValue("water_depth", new double[]{temp.y, temp.x}, new boolean[]{false, false}, true); 
				} catch(ArrayIndexOutOfBoundsException e){}
				
				if (tempValue == null || tempValue.isNaN() ) {
					floatStorage[0] = inFile.getMissingValue("water_depth").floatValue();
				}
				else floatStorage[0] = tempValue.floatValue();


				//write the water depth data
				int[] shape = new int[]{1, 1};
				Array floatArray = Array.factory(float.class, shape, floatStorage);
				int[] orig = new int[]{i, j};
				ncFile.write(bathName, orig, floatArray);

				// write the lon data
				dataX.set(j,  lonSum); // save in
				lonSum += gridXDim; 
			}
			System.out.println("done with lat" + latSum); 
			//write the lat data
			dataY.set(i, latSum);
			latSum += gridYDim; 
		}

		ncFile.write(lonVarName, dataX);
		ncFile.write(latVarName, dataY);




		ncFile.close();


	}

	
	public void exportShapefiles() throws IOException, InvalidRangeException{
		
		ArrayList<Coordinate[]> utmPoints = new ArrayList<Coordinate[]>(); 
		ArrayList<Coordinate[]> wgsPoints = new ArrayList<Coordinate[]>(); 

		
		NetCDFFile inFile = null;
		try {
			inFile = new NetCDFFile(inFileName);
		} catch (IOException e) {}

		inFile.setVariables("lat", "lon", "water_depth"); 

		//bring it in 2 indexes so can do interpolation
		Coordinate origin = new Coordinate(inFile.getValue("lon", new int[] {0}).doubleValue(), inFile.getValue("lat", new int[] {0}).doubleValue(), 0);
		CoordinateUtils.convertUTMToLatLon(origin, 17, false); 

		int latSize = inFile.getSingleDimension("lat");
		int lonSize = inFile.getSingleDimension("lon"); 
		//Coordinate maxBounds = new Coordinate(inFile.getValue("x", new int[] {lonSize-1}).doubleValue(), inFile.getValue("y", new int[] {latSize-1}).doubleValue(), 0);

		//delete existing file

		double lonSum = origin.x;
		double latSum = origin.y; 
		double utmVal = 0; 
		for (int i=0; i<latSize; i++) {
			lonSum = origin.x;
			for (int j=0; j<lonSize; j++) {
				
				if (i<3 || i>(latSize-4)){
				Coordinate wgs = new Coordinate(lonSum, latSum, 0);
				CoordinateUtils.convertLatLonToUTM(wgs, 17);
				wgsPoints.add(new Coordinate[]{wgs});
				
				Coordinate utm = new Coordinate(inFile.getValue("lon", new int[] {j}).doubleValue(), inFile.getValue("lat", new int[] {i}).doubleValue(), 0);
				utmPoints.add(new Coordinate[]{utm});
				utmVal = utm.y;
				}
				// write the lon data
				lonSum += .00049; //gridXDim; 
			}
			System.out.println("i: " + i +"\tdone with lat " + latSum + "\tdone with utm y " + utmVal); 
			//write the lat data
			latSum += .00044; //gridYDim; 
		}
		
		SimpleShapefile utmShape = new SimpleShapefile("dataTest/utmPoints.shp");
		utmShape.createShapefile(Point.class, utmPoints, "UTM");
		
		SimpleShapefile wgsShape = new SimpleShapefile("dataTest/wgsPoints.shp");
		wgsShape.createShapefile(Point.class, wgsPoints, "UTM");


	}

	
	public void map(){
		SimpleMapper map = new SimpleMapper();
		map.drawShapefileLayers(new String[]{"dataTest/wgsPoints.shp", "dataTest/utmPoints.shp" }, null);
	}
	
	
	public void flipDataSet() throws IOException, InvalidRangeException{



		NetCDFFile inFile = new NetCDFFile(inFileName);

		inFile.setInterpolationAxes("y", "x");
		inFile.setVariables("y", "x", "depth"); 

		new File(outFileName).delete();

		NetcdfFileWriteable ncFile =NetcdfFileWriteable.createNew("data/USGSBathWGSClip_temp.nc" 	);
		ncFile.setFill(true);	// this will fill in with blank values, as below
		//ncFile.setLargeFile(true); //NOTE: for whatever reason, when I set this = true, I cant open file in ncBrowse!!!

		String latUnits = "degrees_north";
		String lonUnits = "degrees_east";
		//String bathUniits = "meters"; 
		int latSize = inFile.getSingleDimension("y");
		int lonSize = inFile.getSingleDimension("x"); 

		Dimension[] dim = new Dimension[2];

		dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latSize, false);
		dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonSize, false);

		ncFile.addVariable(bathName, DataType.FLOAT, dim);

		ncFile.addVariableAttribute(bathName, "units", "meters");
		ncFile.addVariableAttribute(bathName, "_FillValue", -3.4028235E38f);
		ncFile.addVariableAttribute(bathName, "missing_value", -3.4028235E38f);

		ncFile.addVariableAttribute(latVarName, "_FillValue", -3.4028235E38f);
		ncFile.addVariableAttribute(latVarName, "missing_value", -3.4028235E38f);

		ncFile.addVariableAttribute(lonVarName, "_FillValue", -3.4028235E38f);
		ncFile.addVariableAttribute(lonVarName, "missing_value", -3.4028235E38f);

		ncFile.create();



		// write the lat and lon index values out (here, leave as index values since grid is rotated and hence not regular and parallel with UTM lines
		ArrayDouble.D1 dataY = (D1) inFile.getArray("y", new int[]{0}, new int[]{dim[0].getLength()}); //new ArrayDouble.D1(dim[0].getLength());
		ArrayDouble.D1 dataX = (D1) inFile.getArray("x", new int[]{0}, new int[]{dim[1].getLength()}); //new ArrayDouble.D1(dim[1].getLength());

		double[] dataYJava = (double[]) dataY.copyTo1DJavaArray();

		int counter = 0; 
		for (int i=dataYJava.length-1; i>=0 ; i--){
			dataY.set(counter, dataYJava[i]);
			counter++;
		}

		ncFile.write(lonVarName, dataX);
		ncFile.write(latVarName, dataY);

		counter = 0; 
		for (int i=dim[0].getLength()-1; i>=0; i--) {

			//get the bathymetry data interpolated
			//float[] floatStorage = new float[dim[1].getLength()]; 
			Array floatArray = inFile.getArray("depth", new int[]{counter, 0}, new int[]{1, dim[1].getLength()}, false); 


			int[] orig = new int[]{i, 0};
			ncFile.write(bathName, orig, floatArray);
			counter++;
		}

		ncFile.close();



	}


	private static Dimension setDimension(NetcdfFileWriteable ncFile,
			DataType type, String name, String units, int length, boolean isUnlimited) {

		Dimension dimension; 
		if (isUnlimited) {
			dimension = ncFile.addUnlimitedDimension(name);
		}
		else{
			dimension = ncFile.addDimension(name, length);
		}
		ncFile.addVariable(name, type, new Dimension[]{dimension});
		ncFile.addVariableAttribute(name, "units", units);


		return dimension;
	}


	public static void main(String[] args) {
		USGSBathyFormatter a = new USGSBathyFormatter();
		try {
			a.writeFile();
			//a.flipDataSet();
			// a.exportShapefiles();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
		
		
	}

}
