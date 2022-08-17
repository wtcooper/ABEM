package us.fl.state.fwc.abem.hydro.efdc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class EFDCBathy2NetCDF {

	static boolean doAsScaledShort = true; 
	double scaleFactor = 0.01;
	
	String directory = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	String outFile = directory + "TB2SB_WGS_Bathy_scaled.nc"; 	

	String landFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleUTM.shp"; // Name of the land mask file
	
	String USGSBathy = "c:\\work\\data\\BOLTs\\USGSBathWGSClip.nc";
	String cornersInFile = directory + "corners.inp"; 
	String dxdyInFile = directory + "dxdy.inp"; 
	//private HashMap <Int3D, EFDCCell> gridCells; //  = new FastMap <PointLoc, EFDCCell>(); 
	private HashMap<Integer, Int3D> gridIndexes; 
	String lonVarName = "lon"; 
	String latVarName = "lat"; 

	private String latName = "lat";
	private String lonName = "lon";
	private String landVarName = "landMask";
	private String bathVarName = "water_depth";
	GeometryFactory gf; 




	public static void main(String[] args){
		EFDCBathy2NetCDF e = new EFDCBathy2NetCDF();
		try {
			if (doAsScaledShort) e.bathy2NetCDF_ScaledShort();
			else e.bathy2NetCDF();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} 

	}




	/**Gets a netCDF land grid, where the land is any inactive EFDC cells that are over land
	 * Note: the other method (getLandForActiveCells) will return a land grid where any active cell that has land in it will be marked as such --
	 * this is useful for running a ShapefileBarrier to get the nearNoData flag
	 * @throws IOException 
	 * 
	 */
	public void bathy2NetCDF() throws IOException{


		//Manual cell index list
		ArrayList<Int3D> makeLandList = new ArrayList<Int3D>(); 
		makeLandList.add(new Int3D(204, 3, 0)); 
//		makeLandList.add(new Int3D(206, 4, 0)); 
//		makeLandList.add(new Int3D(208, 5, 0)); 
//		makeLandList.add(new Int3D(214, 5, 0)); 
//		makeLandList.add(new Int3D(215, 5, 0)); 



		NetCDFFile bathInFile = new NetCDFFile(USGSBathy); 
		NetCDFFile landInFile = new NetCDFFile(directory + "TB2SB_WGS_LandMaskForGridBarrier.nc"); 

		bathInFile.setInterpolationAxes(latName, lonName);
		bathInFile.setVariables(latName, lonName, bathVarName); 

		landInFile.setInterpolationAxes(latName, lonName);
		landInFile.setVariables(latName, lonName, landVarName); 


		double lonSize = 0.004; 
		double latSize = 0.004; 




		//Get minimum lat/lon
		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid
		//gridCells =grid.getGridCells(); 
		gridIndexes = grid.getGridIndexMap();  // returns a HashMap of the I,J's indexed on the L value
		if (gridIndexes == null){
			System.out.println("gridIndexes is null");
			System.exit(1); 
		}
		grid.setCellSizeAndDepth(dxdyInFile, 8);



		gf = JTSFactoryFinder.getGeometryFactory(null);


		try {
			new File(outFile).delete();

			NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outFile);

			double lonMinUSGS = bathInFile.getValue("lon", new int[] {0}).doubleValue(); 
			double lonMinLandMask = landInFile.getValue("lon", new int[] {0}).doubleValue();
			int numExtraLonCells = Math.abs(Math.round((int) ((lonMinLandMask - lonMinUSGS)/lonSize))); 
			
			
			int lonDim = landInFile.getSingleDimension(lonVarName) + numExtraLonCells; 
			int latDim = landInFile.getSingleDimension(latVarName);

			String latUnits = "degrees_north";
			String lonUnits = "degrees_east";
			//String landUnits = "depth in meters"; 

			Dimension[] dim = new Dimension[2];


			dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latDim, false);
			dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonDim, false);


			ncFile.addVariable(bathVarName, DataType.FLOAT, dim);
			ncFile.addVariableAttribute(bathVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(bathVarName, "missing_value", -9999);


			ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(latVarName, "missing_value", -9999);

			ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(lonVarName, "missing_value", -9999);


			ncFile.create();

			ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());
			ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
			//ArrayShort.D2 land = new ArrayShort.D2(dim[0].getLength(), dim[1].getLength());

			double lat = landInFile.getValue(latVarName, new int[] {0}).doubleValue(); //0;  
			double lon = landInFile.getValue(lonVarName, new int[] {0}).doubleValue() - (numExtraLonCells*lonSize); //0; 
			double startLon = lon; 

			for (int i=0; i<dim[0].getLength(); i++) {
				lon = startLon; //reset here so goes back to beginning on lon
				for (int j=0; j<dim[1].getLength(); j++) {
					//lat = landInFile.getValue(latVarName, new int[] {i}).doubleValue(); 
					//lon = landInFile.getValue(lonVarName, new int[] {j}).doubleValue(); 


					//first check if it's in the EFDC bounds so that will base the 
					Coordinate coord = new Coordinate(lon, lat, 0);
					CoordinateUtils.convertLatLonToUTM(coord, 17);

					EFDCCell cell = grid.getGridCell(coord);

					float val = 0; 

					//=====================================================
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//if active cell, then set to the value from EFDC model (which is interpolated)
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//=====================================================
					//if active cells, set them to 0, i.e., they don't contain land
					if (cell != null){
						val = (float) cell.getDepth(); ; 
						float[] dataArray = {-val}; 
						Array value = Array.factory(float.class, new int[]{1,1}, dataArray);
						ncFile.write(bathVarName, new int[]{i,j}, value);
					}


					//=====================================================
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//if cell is inactive and on land, then set to 1 to record as positive value
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//=====================================================
					else {
						
						Short landNum  = (Short) landInFile.getValue(landVarName, new double[] {lat, lon}, new boolean[] {false, false}, true); //.getValue(landVarName, new int[]{i,j}).shortValue();
						

						//=======================================
						// inactive and on land
						//=======================================
						if (landNum != null && landNum.shortValue() == 1){
							val = 1.0f; 
							float[] dataArray = {val}; 
							Array value = Array.factory(float.class, new int[]{1,1}, dataArray);
							ncFile.write(bathVarName, new int[]{i,j}, value);
						}


						//=======================================
						// inactive and on water
						//=======================================
						else { //if not active cell and 
							//create polygon of the grid cell
							Coordinate[] coords = new Coordinate[5]; 
							coords[0] = new Coordinate(lon-lonSize/2, lat-latSize/2, 0);
							coords[1] = new Coordinate(lon+lonSize/2, lat-latSize/2, 0);
							coords[2] = new Coordinate(lon+lonSize/2, lat+latSize/2, 0);
							coords[3] = new Coordinate(lon-lonSize/2, lat+latSize/2, 0);
							coords[4] = new Coordinate(lon-lonSize/2, lat-latSize/2, 0);
							Polygon cell1 = gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null);

							//create a list of random points to check if >50% of random points intersect land

							List<Coordinate> randomPoints = new ArrayList<Coordinate>(); 

							Geometry searchGeometry = cell1.getGeometryN(0); 
							Envelope bb = searchGeometry.getEnvelopeInternal();

							int n = 0;
							int numRandPoints = 50; 
							while (n < numRandPoints) {
								Coordinate c = new Coordinate();
								c.x = Math.random()*bb.getWidth() + bb.getMinX();
								c.y = Math.random()*bb.getHeight() + bb.getMinY();
								Point p = gf.createPoint(c);
								if (searchGeometry.contains(p)) {
									randomPoints.add(c); 
									n++; 
								}
							}

							float depthAvg=0;
							int count = 0; 
							for (int l = 0; l<randomPoints.size(); l++){
								Coordinate c = randomPoints.get(l); 
								Float depth = (Float) bathInFile.getValue(bathVarName, new double[]{c.y, c.x}, new boolean[]{false, false}, true); 
								if (depth != null && !depth.isNaN() ){
									depthAvg += depth.floatValue();
									count++; 
								}
							}

							if (count == 0 ) {

/*								Int3D index = new Int3D(i,j,0); 
								boolean include = false;
								for (int l = 0; l<makeLandList.size(); l++){

									if (makeLandList.get(l).equals(index)) {
										include = true;
									}
								}
								if (include){
									float[] dataArray = {1.0f}; 
									Array value = Array.factory(float.class, new int[]{1,1}, dataArray);
									ncFile.write(bathVarName, new int[]{i,j}, value);
								}
								else {
*/									System.out.println("cell num " + i + "\t" + j +"\tdidn't have any depth measurements from USGS file! adding in missing value");
									float[] dataArray = {-9999f}; 
									Array value = Array.factory(float.class, new int[]{1,1}, dataArray);
									ncFile.write(bathVarName, new int[]{i,j}, value);								
//									}

							}
							else {
								depthAvg = depthAvg/(float)count; 

								float[] dataArray = {-depthAvg}; 
								Array value = Array.factory(float.class, new int[]{1,1}, dataArray);
								ncFile.write(bathVarName, new int[]{i,j}, value);
							}
						} //end of else statement where is inactive cell over water

					}//end of else{} if not active cell

					dataX.set(j,  lon); // save in
					lon += lonSize;
					System.out.println(i + "\t" + j + "\t" + lon + "\t" + lat); 
				}

				dataY.set(i, lat);
				lat += latSize;
			}

			ncFile.write(lonVarName, dataX);
			ncFile.write(latVarName, dataY);
			//ncFile.write(landVarName, land); 

			ncFile.close(); 


		} catch (IOException e1) {
			System.out.println("catching exception");
			e1.printStackTrace();
		} catch (InvalidRangeException e) {
			System.out.println("catching exception");
			e.printStackTrace();
		}

	}


	/**Gets a netCDF land grid, where the land is any inactive EFDC cells that are over land
	 * Note: the other method (getLandForActiveCells) will return a land grid where any active cell that has land in it will be marked as such --
	 * this is useful for running a ShapefileBarrier to get the nearNoData flag
	 * @throws IOException 
	 * 
	 */
	public void bathy2NetCDF_ScaledShort() throws IOException{


		//Manual cell index list
		ArrayList<Int3D> makeLandList = new ArrayList<Int3D>(); 
		makeLandList.add(new Int3D(204, 3, 0)); 
//		makeLandList.add(new Int3D(206, 4, 0)); 
//		makeLandList.add(new Int3D(208, 5, 0)); 
//		makeLandList.add(new Int3D(214, 5, 0)); 
//		makeLandList.add(new Int3D(215, 5, 0)); 



		NetCDFFile bathInFile = new NetCDFFile(USGSBathy); 
		NetCDFFile landInFile = new NetCDFFile(directory + "TB2SB_WGS_LandMaskForGridBarrier.nc"); 

		bathInFile.setInterpolationAxes(latName, lonName);
		bathInFile.setVariables(latName, lonName, bathVarName); 

		landInFile.setInterpolationAxes(latName, lonName);
		landInFile.setVariables(latName, lonName, landVarName); 


		double lonSize = 0.004; 
		double latSize = 0.004; 




		//Get minimum lat/lon
		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid
		//gridCells =grid.getGridCells(); 
		gridIndexes = grid.getGridIndexMap();  // returns a HashMap of the I,J's indexed on the L value
		if (gridIndexes == null){
			System.out.println("gridIndexes is null");
			System.exit(1); 
		}
		grid.setCellSizeAndDepth(dxdyInFile, 8);



		gf = JTSFactoryFinder.getGeometryFactory(null);


		try {
			new File(outFile).delete();

			NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outFile);

			double lonMinUSGS = bathInFile.getValue("lon", new int[] {0}).doubleValue(); 
			double lonMinLandMask = landInFile.getValue("lon", new int[] {0}).doubleValue();
			int numExtraLonCells = Math.abs(Math.round((int) ((lonMinLandMask - lonMinUSGS)/lonSize))); 
			
			
			int lonDim = landInFile.getSingleDimension(lonVarName) + numExtraLonCells; 
			int latDim = landInFile.getSingleDimension(latVarName);

			String latUnits = "degrees_north";
			String lonUnits = "degrees_east";
			//String landUnits = "depth in meters"; 

			Dimension[] dim = new Dimension[2];


			dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latDim, false);
			dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonDim, false);

			short missingVal = -9999; 
			ncFile.addVariable(bathVarName, DataType.SHORT, dim);
			//ncFile.addVariableAttribute(bathVarName, "_FillValue", missingVal);
			ncFile.addVariableAttribute(bathVarName, "missing_value", missingVal);


			//ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(latVarName, "missing_value", -9999.0);

			//ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(lonVarName, "missing_value", -9999.0);


			ncFile.create();

			ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());
			ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
			//ArrayShort.D2 land = new ArrayShort.D2(dim[0].getLength(), dim[1].getLength());

			double lat = landInFile.getValue(latVarName, new int[] {0}).doubleValue(); //0;  
			double lon = landInFile.getValue(lonVarName, new int[] {0}).doubleValue() - (numExtraLonCells*lonSize); //0; 
			double startLon = lon; 

			for (int i=0; i<dim[0].getLength(); i++) {
				lon = startLon; //reset here so goes back to beginning on lon
				for (int j=0; j<dim[1].getLength(); j++) {
					//lat = landInFile.getValue(latVarName, new int[] {i}).doubleValue(); 
					//lon = landInFile.getValue(lonVarName, new int[] {j}).doubleValue(); 


					//first check if it's in the EFDC bounds so that will base the 
					Coordinate coord = new Coordinate(lon, lat, 0);
					CoordinateUtils.convertLatLonToUTM(coord, 17);

					EFDCCell cell = grid.getGridCell(coord);

					short val = 0; 

					//=====================================================
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//if active cell, then set to the value from EFDC model (which is interpolated)
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//=====================================================
					//if active cells, set them to 0, i.e., they don't contain land
					if (cell != null){
						val = (short) (Math.round((cell.getDepth()* (1/scaleFactor))));  //(float) cell.getDepth(); ; 
						short[] dataArray = {(short) -val}; 
						Array value = Array.factory(short.class, new int[]{1,1}, dataArray);
						ncFile.write(bathVarName, new int[]{i,j}, value);
					}


					//=====================================================
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//if cell is inactive and on land, then set to 1 to record as positive value
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//=====================================================
					else {
						
						Short landNum  = (Short) landInFile.getValue(landVarName, new double[] {lat, lon}, new boolean[] {false, false}, true); //.getValue(landVarName, new int[]{i,j}).shortValue();
						

						//=======================================
						// inactive and on land
						//=======================================
						if (landNum != null && landNum.shortValue() == 1){
							val = (short) 100; //1.0f; 
							short [] dataArray = {val}; 
							Array value = Array.factory(short.class, new int[]{1,1}, dataArray);
							ncFile.write(bathVarName, new int[]{i,j}, value);
						}


						//=======================================
						// inactive and on water
						//=======================================
						else { //if not active cell and 
							//create polygon of the grid cell
							Coordinate[] coords = new Coordinate[5]; 
							coords[0] = new Coordinate(lon-lonSize/2, lat-latSize/2, 0);
							coords[1] = new Coordinate(lon+lonSize/2, lat-latSize/2, 0);
							coords[2] = new Coordinate(lon+lonSize/2, lat+latSize/2, 0);
							coords[3] = new Coordinate(lon-lonSize/2, lat+latSize/2, 0);
							coords[4] = new Coordinate(lon-lonSize/2, lat-latSize/2, 0);
							Polygon cell1 = gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null);

							//create a list of random points to check if >50% of random points intersect land

							List<Coordinate> randomPoints = new ArrayList<Coordinate>(); 

							Geometry searchGeometry = cell1.getGeometryN(0); 
							Envelope bb = searchGeometry.getEnvelopeInternal();

							int n = 0;
							int numRandPoints = 50; 
							while (n < numRandPoints) {
								Coordinate c = new Coordinate();
								c.x = Math.random()*bb.getWidth() + bb.getMinX();
								c.y = Math.random()*bb.getHeight() + bb.getMinY();
								Point p = gf.createPoint(c);
								if (searchGeometry.contains(p)) {
									randomPoints.add(c); 
									n++; 
								}
							}

							float depthAvg=0;
							int count = 0; 
							for (int l = 0; l<randomPoints.size(); l++){
								Coordinate c = randomPoints.get(l); 
								Float depth = (Float) bathInFile.getValue(bathVarName, new double[]{c.y, c.x}, new boolean[]{false, false}, true); 
								if (depth != null && !depth.isNaN() ){
									depthAvg += depth.floatValue();
									count++; 
								}
							}

							if (count == 0 ) {

/*								Int3D index = new Int3D(i,j,0); 
								boolean include = false;
								for (int l = 0; l<makeLandList.size(); l++){

									if (makeLandList.get(l).equals(index)) {
										include = true;
									}
								}
								if (include){
									float[] dataArray = {1.0f}; 
									Array value = Array.factory(float.class, new int[]{1,1}, dataArray);
									ncFile.write(bathVarName, new int[]{i,j}, value);
								}
								else {
*/									System.out.println("cell num " + i + "\t" + j +"\tdidn't have any depth measurements from USGS file! adding in missing value");
									short[] dataArray = {missingVal}; 
									Array value = Array.factory(short.class, new int[]{1,1}, dataArray);
									ncFile.write(bathVarName, new int[]{i,j}, value);								
//									}

							}
							else {
								depthAvg = depthAvg/(float)count; 
								val = (short) (Math.round((depthAvg* (1/scaleFactor))));  //(float) cell.getDepth(); ; 
								short[] dataArray = {(short) -val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, dataArray);
								ncFile.write(bathVarName, new int[]{i,j}, value);
							}
						} //end of else statement where is inactive cell over water

					}//end of else{} if not active cell

					dataX.set(j,  lon); // save in
					lon += lonSize;
					System.out.println(i + "\t" + j + "\t" + lon + "\t" + lat); 
				}

				dataY.set(i, lat);
				lat += latSize;
			}

			ncFile.write(lonVarName, dataX);
			ncFile.write(latVarName, dataY);
			//ncFile.write(landVarName, land); 

			ncFile.close(); 


		} catch (IOException e1) {
			System.out.println("catching exception");
			e1.printStackTrace();
		} catch (InvalidRangeException e) {
			System.out.println("catching exception");
			e.printStackTrace();
		}

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



}
