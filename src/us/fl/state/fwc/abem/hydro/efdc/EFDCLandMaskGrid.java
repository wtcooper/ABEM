package us.fl.state.fwc.abem.hydro.efdc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.Shapefile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.index.SpatialIndex;

public class EFDCLandMaskGrid {

	String landFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleUTM.shp"; // Name of the land mask file

	String directory = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";

	String cornersInFile = directory + "corners.inp"; 
	private HashMap <Int3D, EFDCCell> gridCells; //  = new FastMap <PointLoc, EFDCCell>(); 
	private HashMap<Integer, Int3D> gridIndexes; 
	String outFile = directory + "TB2SB_WGS_LandMaskForGridBarrier.nc"; 	
	String lonVarName = "lon"; 
	String latVarName = "lat"; 
	String landMask = "landInRange"; 

	NetCDFFile EFDCFile; //use this for EFDC
	String EFDCFileName = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\TB2SB_WGS_070110-071010.nc";
	private String latName = "lat";
	private String lonName = "lon";
	private String kName = "depth";
	private String tName = "time";
	private String uName = "water_u";
	private String vName= "water_v";
	private String landVarName = "landMask";
	GeometryFactory gf; 




	public static void main(String[] args){
		EFDCLandMaskGrid e = new EFDCLandMaskGrid();
		//e.getLandForActiveCells();
		//e.getLandGridForInactiveCells();
		try {
			e.getLandGridForLargerArea();
		} catch (IOException e1) {
			e1.printStackTrace();
		} 

	}


	/**Gets a netCDF land grid, where the land is any inactive EFDC cells that are over land
	 * Note: the other method (getLandForActiveCells) will return a land grid where any active cell that has land in it will be marked as such --
	 * this is useful for running a ShapefileBarrier to get the nearNoData flag
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void getLandGridForInactiveCells(){


		gf = JTSFactoryFinder.getGeometryFactory(null);

		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid

		try {

			new File(outFile).delete();

			NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outFile);

			SimpleShapefile landShp = new SimpleShapefile(landFileName);
			landShp.buildSpatialIndex();
			SpatialIndex spatialIndex = landShp.getSpatialIndex(); 


			try {
				EFDCFile = new NetCDFFile(EFDCFileName);
			} catch (IOException e) {}

			EFDCFile.setInterpolationAxes(latName, lonName);
			EFDCFile.setVariables(tName, kName, latName, lonName, uName, vName); 


			String latUnits = "degrees_north";
			String lonUnits = "degrees_east";
			//String landUnits = "1=land in range, 0=no land in range"; 

			Dimension[] dim = new Dimension[2];


			dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, EFDCFile.getSingleDimension(latName), false);
			dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, EFDCFile.getSingleDimension(lonName), false);


			ncFile.addVariable(landVarName, DataType.SHORT, dim);
			ncFile.addVariableAttribute(landVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(landVarName, "missing_value", -9999);


			ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(latVarName, "missing_value", -9999);

			ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(lonVarName, "missing_value", -9999);


			ncFile.create();

			ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());
			ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
			//ArrayShort.D2 land = new ArrayShort.D2(dim[0].getLength(), dim[1].getLength());

			double lat = 0;
			double lon = 0; 
			for (int i=0; i<dim[0].getLength(); i++) {

				for (int j=0; j<dim[1].getLength(); j++) {



					lat = EFDCFile.getValue(latVarName, new int[]{i}).doubleValue();
					lon = EFDCFile.getValue(lonVarName, new int[]{j}).doubleValue();

					Coordinate wgscoord = new Coordinate(lon, lat, 0);
					Coordinate coord = new Coordinate(lon, lat, 0);
					CoordinateUtils.convertLatLonToUTM(coord, 17);
					System.out.println("writing file at " + wgscoord.x + "\t" + wgscoord.y + "\tfor index lat: " + i +" and lon: " + j);

					EFDCCell cell = grid.getGridCell(coord);

					short val = 0; 

					//if active cells, set them to 0, i.e., they don't contain land
					if (cell != null){
						val = 0; 
						short[] intArray = {val}; 
						Array value = Array.factory(short.class, new int[]{1,1}, intArray);
						ncFile.write(landVarName, new int[]{i,j}, value);
						//val = 0; 
						//land.set(i,j,val); 
					}

					//else, if it's an inactive cell, need to check if it's water or land
					else {
						double tempLat = -1*lon -55.3036;

						//if it's to the west of the boundary in open water, set to 0
						if ( 	( (lon < -82.8376) && (lat<27.8249) ) 				||   //checks to see if west of EFDC western boundary, plus 3 rows to avoid boundary layer 
								( (lat  < tempLat) && 	(lon < -82.578) ) 			||
								( lon < -82.8552) ) { 	//check the S sections

							val = 0; 
							short[] intArray = {val}; 
							Array value = Array.factory(short.class, new int[]{1,1}, intArray);
							ncFile.write(landVarName, new int[]{i,j}, value);
							//val = 0; 
							//land.set(i,j,val); 
						}
						else{
							//check if it's in the NW & SW corners where is some water and land, and run through 
							//shapefile test if so

							if ( 	( (lat < 27.278) && (lon<-82.4106) ) 				||   //checks to see if west of EFDC western boundary, plus 3 rows to avoid boundary layer 
									( (lon < -82.7545) && 	(lat > 27.82) ) /*			||
									( (lon < -82.7848) && 	(lat > 27.838) ) */	 ) { 	//check the S sections

								//check shapefile
								//first set to 0, i.e., that's it doesn't contain land
								val = 0; 
								short[] intArray = {val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, intArray);
								ncFile.write(landVarName, new int[]{i,j}, value);
								//val = 0; 
								//land.set(i,j,val); 

								//create a diamond just a tad smaller than the grid to check if there's an intersection with the shapefile
								Coordinate[] coords = new Coordinate[5]; 
								coords[0] = new Coordinate(lon+0.002, lat, 0);
								CoordinateUtils.convertLatLonToUTM(coords[0], 17);
								coords[1] = new Coordinate(lon, lat-0.002, 0);
								CoordinateUtils.convertLatLonToUTM(coords[1], 17);
								coords[2] = new Coordinate(lon-0.002, lat, 0);
								CoordinateUtils.convertLatLonToUTM(coords[2], 17);
								coords[3] = new Coordinate(lon, lat+0.002, 0);
								CoordinateUtils.convertLatLonToUTM(coords[3], 17);
								coords[4] = new Coordinate(lon+0.002, lat, 0);
								CoordinateUtils.convertLatLonToUTM(coords[4], 17);

								Polygon check = gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null);

								Geometry geom = check.getGeometryN(0);  
								List<SimpleFeature > fl = spatialIndex.query(geom.getEnvelopeInternal());


								for (int k = 0; k < fl.size(); k++) {

									// Retrieve the feature from the list of intersecting features
									SimpleFeature f = fl.get(k);

									// Get the geometric object
									Geometry landGeom = (Geometry) f.getAttribute(0);

									// Does the Line Segment cross into the Polygon?
									if (geom.intersects(landGeom)) {
										val = 1; 
										intArray[0] = val; 
										value = Array.factory(short.class, new int[]{1,1}, intArray);
										ncFile.write(landVarName, new int[]{i,j}, value);
										//val = 0; 
										//land.set(i,j,val); 
									}

								}

							}
							else {
								val = 1; 
								short[] intArray = {val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, intArray);
								ncFile.write(landVarName, new int[]{i,j}, value);
								//val = 0; 
								//land.set(i,j,val); 
							}
						}

					}//end check if the EFDC cell is null

					dataX.set(j,  lon); // save in

				}

				dataY.set(i, lat);
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
	@SuppressWarnings("unchecked")
	public void getLandGridForLargerArea() throws IOException{

		String shpLandFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file
		String outFile = directory + "TB2SB_WGS_LandMaskForGridBarrier.nc"; 	

		double lonSize = 0.004; 
		double latSize = 0.004; 


		//for extra area
		String extraShapeCoords = "dataTest/SarasotaBayClip.shp";
		Shapefile extraShp = new Shapefile(extraShapeCoords, false);
		extraShp.openShapefile();
		SimpleFeature feature = extraShp.getFeature(new Coordinate(-82.4685, 27.1837,0)); 
		MultiPolygon extraShpGeom = (MultiPolygon) feature.getDefaultGeometry();

		//for extra area
		String extraShapeCoords2 = "dataTest/AncloteAreaClip.shp";
		Shapefile extraShp2 = new Shapefile(extraShapeCoords2, false);
		extraShp2.openShapefile();
		SpatialIndex extraShp2SpatialIndex = extraShp2.getSpatialIndex(); 

		

		//Get minimum lat/lon
		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid
		gridCells =grid.getGridCells(); 
		gridIndexes = grid.getGridIndexMap();  // returns a HashMap of the I,J's indexed on the L value
		if (gridIndexes == null){
			System.out.println("gridIndexes is null");
			System.exit(1); 
		}

		// loop through and get the min lat and lon centroid
		//because UTM is shifted compared to WGS, need to store the minlon and minLat as coordiantes, 
		// then convert, to get a singular originNode 
		Coordinate minLat = new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, 0);
		Coordinate minLon =new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, 0);
		Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {

			Int3D index = it.next();
			//Geometry geom = gridCells.get(index).getGeom();
			Coordinate centroid = gridCells.get(index).getCentroidCoord(); 
			if (centroid.x < minLon.x) minLon = centroid;
			if (centroid.y < minLat.y) minLat = centroid; 
		}

		CoordinateUtils.convertUTMToLatLon(minLon, 17, false);
		CoordinateUtils.convertUTMToLatLon(minLat, 17, false);

		//Coordinate origin = new Coordinate(minLon.x, minLat.y, 0);


		gf = JTSFactoryFinder.getGeometryFactory(null);


		try {
			new File(outFile).delete();

			NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outFile);

			SimpleShapefile landShp = new SimpleShapefile(shpLandFileName);
			landShp.buildSpatialIndex();
			//SpatialIndex spatialIndex = landShp.getSpatialIndex(); 
			FeatureSource featureSource = landShp.getFeatureSource(); 
			ReferencedEnvelope env = featureSource.getBounds(); 
			double startLon = env.getMinX()-lonSize;
			double startLat = env.getMinY()-latSize; 
			double endLon = env.getMaxX()+lonSize;
			double endLat = env.getMaxY()+latSize; 

			int lonDim = (int) ( (endLon-startLon)/lonSize); 
			int latDim = (int) ( (endLat-startLat)/latSize);

			String latUnits = "degrees_north";
			String lonUnits = "degrees_east";
			//String landUnits = "1=land in range, 0=no land in range"; 

			Dimension[] dim = new Dimension[2];


			dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latDim, false);
			dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonDim, false);


			ncFile.addVariable(landVarName, DataType.SHORT, dim);
			ncFile.addVariableAttribute(landVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(landVarName, "missing_value", -9999);


			ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(latVarName, "missing_value", -9999);

			ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
			ncFile.addVariableAttribute(lonVarName, "missing_value", -9999);


			ncFile.create();

			ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());
			ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
			//ArrayShort.D2 land = new ArrayShort.D2(dim[0].getLength(), dim[1].getLength());

			int lonDiff = (int) Math.round((minLon.x - startLon)/0.004);  
			int  latDiff = (int) Math.round((minLat.y - startLat)/0.004); 


			double lat = minLat.y-latDiff*0.004; //startLat; 
			double lon = minLon.x-lonDiff*0.004; //startLon; 
			//double maxLat = lat + dim[0].getLength()*0.004;
			//double maxLon = lon + dim[1].getLength()*0.004;

			for (int i=0; i<dim[0].getLength(); i++) {

				lon = minLon.x-lonDiff*0.004; //startLon; 
				for (int j=0; j<dim[1].getLength(); j++) {




					//first check if it's in the EFDC bounds so that will base the 
					Coordinate coord = new Coordinate(lon, lat, 0);
					CoordinateUtils.convertLatLonToUTM(coord, 17);

					EFDCCell cell = grid.getGridCell(coord);

					short val = 0; 

					//=====================================================
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//if cell is inactive, then need to do some specialty checking here
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//=====================================================
					//if active cells, set them to 0, i.e., they don't contain land
					if (cell != null){
						val = 0; 
						short[] intArray = {val}; 
						Array value = Array.factory(short.class, new int[]{1,1}, intArray);
						ncFile.write(landVarName, new int[]{i,j}, value);
						//val = 0; 
						//land.set(i,j,val); 
					}


					//=====================================================
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//if cell is inactive, then need to do some specialty checking here
					//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					//=====================================================
					else {

						double tempLat = -1*lon -55.3036; // this is for first check, if to west of EFDC boundary 
						double tempLat2 = (-1.583214794)*lon -103.3909233;  //this if for 2nd check, if west of sarasota bay area 

						//=======================================
						// West boundary : then check to see if it's land or water through 3-way check
						//=======================================
						//if it's to the west of the boundary in open water, set to 0
						if ( 	( (lon < -82.8376) && (lat<27.8249) ) 				||   //checks to see if west of EFDC western boundary, plus 3 rows to avoid boundary layer 
								( (lat  < tempLat) && 	(lon < -82.578) ) 			||
								( lon < -82.8552) ) { 	//check the S sections

							val = 0; 
							short[] intArray = {val}; 
							Array value = Array.factory(short.class, new int[]{1,1}, intArray);
							ncFile.write(landVarName, new int[]{i,j}, value);
							//val = 0; 
							//land.set(i,j,val); 
						}




						//=======================================
						// SW corner: then check to see if it's land or water through 3-way check
						//=======================================
						else	if ( (lat < 27.278) && (lon<-82.4106) ){
							Coordinate extraCoord = new Coordinate(lon, lat, 0);
							Point extraPoint = gf.createPoint(extraCoord); 
							Geometry extraGeom = extraPoint.getGeometryN(0);

							if (extraGeom.intersects(extraShpGeom)) {
								val =1; 
								short[] intArray = {val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, intArray);
								ncFile.write(landVarName, new int[]{i,j}, value);

							}

							//else if to the west of the polygon around sarasota bay
							else if (lat  < tempLat2){
								val =0; 
								short[] intArray = {val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, intArray);
								ncFile.write(landVarName, new int[]{i,j}, value);
							}

							//else if to the east of the polygon around sarasota bay
							else if (lat>tempLat2){
								val =1; 
								short[] intArray = {val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, intArray);
								ncFile.write(landVarName, new int[]{i,j}, value);
							}
						}


						//=======================================
						// NW corner: then check to see if it's land or water through 3-way check
						//=======================================

						else if ( 	(lon < -82.7545) && 	(lat > 27.886)  	||
								(lon < -82.8307) && 	(lat > 27.8259)  			||
								(lon < -82.7831) && 	(lat > 27.8374)  ) {//NW corner

							
							//create polygon of the grid cell
							Coordinate[] coords = new Coordinate[5]; 
							coords[0] = new Coordinate(lon-lonSize/2, lat-latSize/2, 0);
							coords[1] = new Coordinate(lon+lonSize/2, lat-latSize/2, 0);
							coords[2] = new Coordinate(lon+lonSize/2, lat+latSize/2, 0);
							coords[3] = new Coordinate(lon-lonSize/2, lat+latSize/2, 0);
							coords[4] = new Coordinate(lon-lonSize/2, lat-latSize/2, 0);
							Polygon cell1 = gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null);


							List<Coordinate> randomPoints = new ArrayList<Coordinate>(); 

							Geometry searchGeometry = cell1.getGeometryN(0); 
							Envelope bb = searchGeometry.getEnvelopeInternal();

							int n = 0;
							int numRandPoints = 20; 
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

							Geometry geom ;
							List<SimpleFeature > fl ; 
							Point point ; 

							int landIntersectCounter = 0; 
							//loop through all random points and see if they intersect land
							for (int l = 0; l<randomPoints.size(); l++){

								point = gf.createPoint(randomPoints.get(l)); 
								geom = point.getGeometryN(0);  
								fl = extraShp2SpatialIndex.query(geom.getEnvelopeInternal());

								for (int k = 0; k < fl.size(); k++) {

									// Retrieve the feature from the list of intersecting features
									SimpleFeature f = fl.get(k);

									// Get the geometric object
									Geometry landGeom = (Geometry) f.getAttribute(0);

									// Does the Line Segment cross into the Polygon?
									if (geom.intersects(landGeom)) {
										landIntersectCounter++; 
									}

								}

								val = 0;
								if (landIntersectCounter > (numRandPoints/2)) val = 1; 
								short[] intArray = {val}; 
								Array value = Array.factory(short.class, new int[]{1,1}, intArray);
								ncFile.write(landVarName, new int[]{i,j}, value);
							}
						}//end of else{} if in NW or SW corner where is land/water interface, where needs to check

						
						//=======================================
						// EASTERN Areas: then check to see if it's land or water through 3-way check
						//=======================================
						else {
							val = 1;
							short[] intArray = {val}; 
							Array value = Array.factory(short.class, new int[]{1,1}, intArray);
							ncFile.write(landVarName, new int[]{i,j}, value);
						}

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






@SuppressWarnings("unchecked")
public void getLandForActiveCells(){
	EFDCGrid grid = new EFDCGrid();
	grid.initialize(cornersInFile); // set up the grid

	try {

		new File(outFile).delete();

		NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outFile);

		SimpleShapefile landShp = new SimpleShapefile(landFileName);
		landShp.buildSpatialIndex();
		SpatialIndex spatialIndex = landShp.getSpatialIndex(); 


		try {
			EFDCFile = new NetCDFFile(EFDCFileName);
		} catch (IOException e) {}

		EFDCFile.setInterpolationAxes(latName, lonName);
		EFDCFile.setVariables(tName, kName, latName, lonName, uName, vName); 


		String latUnits = "degrees_north";
		String lonUnits = "degrees_east";
		//String landUnits = "1=land in range, 0=no land in range"; 

		Dimension[] dim = new Dimension[2];


		dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, EFDCFile.getSingleDimension(latName), false);
		dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, EFDCFile.getSingleDimension(lonName), false);


		ncFile.addVariable(landVarName, DataType.SHORT, dim);
		ncFile.addVariableAttribute(landVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(landVarName, "missing_value", -9999);


		ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(latVarName, "missing_value", -9999);

		ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(lonVarName, "missing_value", -9999);


		ncFile.create();

		ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());
		ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
		ArrayShort.D2 land = new ArrayShort.D2(dim[0].getLength(), dim[1].getLength());

		double lat = 0;
		double lon = 0; 
		for (int i=0; i<dim[0].getLength(); i++) {

			for (int j=0; j<dim[1].getLength(); j++) {

				lat = EFDCFile.getValue(latVarName, new int[]{i}).doubleValue();
				lon = EFDCFile.getValue(lonVarName, new int[]{j}).doubleValue();

				Coordinate coord = new Coordinate(lon, lat, 0);
				CoordinateUtils.convertLatLonToUTM(coord, 17);
				System.out.println("writing file at " + coord.x + "\t" + coord.y);

				EFDCCell cell = grid.getGridCell(coord);

				Short val = 0; 

				//if inactive cell, set the value to 1, i.e., is near no data / land
				if (cell == null){
					val = 1; 
					land.set(i,j,val); 
				}

				//else, if it's an active cell 
				else {

					//first set to 0, i.e., that's it doesn't contain land
					val = 0; 
					land.set(i, j, val); 

					Geometry cellGeom = cell.getGeom(); 
					List<SimpleFeature > fl = spatialIndex.query(cellGeom.getEnvelopeInternal());


					for (int k = 0; k < fl.size(); k++) {

						// Retrieve the feature from the list of intersecting features
						SimpleFeature f = fl.get(k);

						// Get the geometric object
						Geometry landGeom = (Geometry) f.getAttribute(0);

						// Does the Line Segment cross into the Polygon?
						if (cellGeom.intersects(landGeom)) {
							val = 1; 
							land.set(i, j, val); 
						}

					}
				}//end check if the EFDC cell is null

				dataX.set(j,  lon); // save in

			}

			dataY.set(i, lat);
		}

		ncFile.write(lonVarName, dataX);
		ncFile.write(latVarName, dataY);
		ncFile.write(landVarName, land); 

		ncFile.close(); 

	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (InvalidRangeException e) {
		// TODO Auto-generated catch block
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
