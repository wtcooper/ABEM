package us.fl.state.fwc.abem.hydro.ncom;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.geometry.Vector3D;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;

import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.SimpleMapper;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class NetCDFMapperTester {

	DecimalFormat twoDForm = new DecimalFormat("#.##");

	PrintWriter outWriter= null; //need Time 	Speed	Direction, in meters per second
	String outFile = "output/SalinityZeroValues10.1.09-01.30.10.txt";
	int numLayers = 8; 
	String filename= "c:\\work\\data\\NCOM_AS\\ncom_relo_amseas_2010052800.nc"; //"http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/amseas/ncom_relo_amseas_2010072900/ncom_relo_amseas_2010072900_t096.nc"; //"c:\\work\\temp\\ncom_relo_amseas_2010072500_t084.nc" ; //"dataTest/v3d_2010051500.nc"; 
	String varNameNCOM_AS = "water_v"; 
	String latVarNameNCOM_AS = "lat";
	String lonVarNameNCOM_AS = "lon";



	final static double R_EARTH = 6372795.477598f; // Quadratic mean radius of
	// the earth (meters)
	final static double REINV = 1d / R_EARTH;// Inverse Radius of the Earth

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NetCDFMapperTester main = new NetCDFMapperTester();

		main.netCDFTest();


	}


	public void netCDFTest(){
		double oldLat=0, oldLon=0;
		int latCounter=0, lonCounter=0; 

		
		NetcdfFile uDataFile = null;

		try {
			uDataFile = NetcdfDataset.openFile(filename, null);    



			//create data shapefile


			Variable uVar = uDataFile.findVariable(varNameNCOM_AS ); 

			//Print the attributes
			List<Attribute> list = uVar.getAttributes();
			for (int i=0; i<list.size(); i++){
				Attribute att = list.get(i);
				System.out.print(att.getName() + "\t\t" + att.getDataType());
				if (att.getDataType().toString().equals("String")) System.out.println("\t\t" + att.getStringValue());
				else System.out.println("\t\t" +att.getNumericValue()); 
			}
			
			
			ArrayShort.D4 uArray;
			int time = 0;
			int depth = 0; 

			Variable latVar = uDataFile.findVariable(latVarNameNCOM_AS ); 
			Variable lonVar = uDataFile.findVariable(lonVarNameNCOM_AS ); 
			ArrayDouble.D1 latArray = (ArrayDouble.D1) latVar.read(); 
			ArrayDouble.D1 lonArray = (ArrayDouble.D1) lonVar.read(); 
			int[] latVarShape = latVar.getShape();
			int[] lonVarShape = lonVar.getShape();

			ArrayList<Coordinate[]> coords = new ArrayList<Coordinate[]>(); 
			
			double lat=0, lon=0;
			boolean trigger1 = false;
			boolean trigger2 = true; 
			for (int i = 0; i < latVarShape[0]; i++){
				for (int j = 0; j < lonVarShape[0]; j++){
					//outFile.println(latArray.get(i,j) + "\t" + lonArray.get(i,j)); 
					lat = latArray.get(i);
					lon = lonArray.get(j);// - 360.0; 
					if ( (lat > 23.5) && (lat < 30.7) && (lon>-87.6) && (lon < -78.5)){
						trigger1=true; 
						//count the lat and lon to get dimensions
						if (lat!=oldLat){
							oldLat = lat;
							latCounter++;
						}
						if (trigger2 && (lon!=oldLon)){
							oldLon = lon;
							lonCounter++;
						}
						
						uArray = (ArrayShort.D4) uVar.read(time+":"+time+":1,"+  depth+":"+depth+":1,"+  i+":"+i+":1,"+j+":"+j+":1"); //read("0:0:1, 0:29:1, 0:480:1, 0:600:1 ");
						short value = uArray.get(0,0,0,0); //*scaleFactor;
						if (value > -1000){
							//coords.add(new Coordinate[]{new Coordinate(lon, lat)}); 

							double[] utmcoords = CoordinateUtils.convertLatLonToUTM(lat, lon, 17);
							coords.add(new Coordinate[]{new Coordinate(utmcoords[1], utmcoords[0])});
							//if ( (Math.round(utmcoords[0]) == Math.round(336805.6674)) &&  (Math.round(utmcoords[1]) == Math.round(3056865.3824)) ){
							if ( (utmcoords[1] > 336700) && (utmcoords[1] < 336900) &&  (utmcoords[0] > 3056765) && (utmcoords[0] < 3056965) ){
								System.out.println("index at sunshine skyway (lat,lon): " + i + ", " + j);
							}
							//else System.out.println("can't find point!");
						}

					}
				}
				if (trigger1) trigger2=false; //turn of recording of latCounter
			}

			System.out.println("lat dimensions: " + latCounter + "\tlon dimensions" + lonCounter);
			
/*	*/		String pointsShpName = "dataTest/NCOMActiveCells.shp"; 
			SimpleShapefile pointsShp = new SimpleShapefile(pointsShpName);
			pointsShp.createShapefile(Point.class, coords, "UTM17N");

				//do mapping
				SimpleMapper map = new SimpleMapper("NCOM datapoints", 1000, 800); 

				String flLand = "dataTest/fl_40k_nowater_poly_UTM.shp";
				String EFDCGrid = "c:\\work\\temp\\EFDCGrid_Final.shp";
				String[] layers = {pointsShpName, flLand, EFDCGrid};
				Style[] layerStyles = {	 
						SLD.createPointStyle("circle", new Color(0,125,125), new Color(0,125,125), 1.0f, 3f),
						SLD.createPolygonStyle(new Color(125, 50, 125), new Color(125, 50, 125), .3f),
					SLD.createPolygonStyle(new Color(125, 175, 125), new Color(125, 175, 125), .3f)};
				map.drawShapefileLayers(layers, layerStyles);

			//	map.deleteFiles(pointsShpName); 



			} catch (java.io.IOException e) {
				System.out.println(" fail = "+e);
				e.printStackTrace();
			} catch (InvalidRangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (uDataFile != null )
					try {
						uDataFile.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
			}

		}

	

		public void step() {

			double[] coords = {27.6530227,-82.767852}; 
			double[] coords2 = {27.6530227,-82.739379};

			double[] utmcoords1 = CoordinateUtils.convertLatLonToUTM(coords[0], coords[1], 17); 
			double[] utmcoords2 = CoordinateUtils.convertLatLonToUTM(coords2[0], coords2[1], 17); 


			//double dx = utmcoords2[1]-utmcoords1[1];
			//double dy = utmcoords2[0]-utmcoords1[0];

			System.out.println("coords1 in utm, lat: " + utmcoords1[0] + "\tlon: " + utmcoords1[1]);
			System.out.println("coords2 in utm, lat: " + utmcoords2[0] + "\tlon: " + utmcoords2[1]);

			double dist = CoordinateUtils.getDistance(utmcoords1, utmcoords2); 

			System.out.println("back calculation of distance: " + dist); 

			double[] newCoords = latLon(coords, 0, dist);
			System.out.println("new coords lat: " + newCoords[0] + "\tlon: " + newCoords[1]);
			System.out.println();

			dist = NetCDFMapperTester.distance_Sphere(coords[1], coords[0], coords2[1], coords2[0]);
			System.out.println("main distance_Sphere method: " + dist);

			dist = NetCDFMapperTester.distance_Sphere2(coords[1], coords[0], coords2[1], coords2[0]);
			System.out.println("alternate distance_Sphere method: " + dist);
		}


		/**
		 * Executes a change in position within a spherical coordinate system.
		 * 
		 * @param coords -
		 *            Coordinates, latitude first, then longitude
		 * @param dy -
		 *            Change in the y direction (latitude) in meters
		 * @param dx -
		 *            Change in the x direction (longitude) in meters
		 * @return - The new position, latitude then longitude.
		 */

		public double[] latLon(double[] coords, double dy, double dx) {

			double rlat2, rlon2;
			double dlon, rln1, rlt1;

			rln1 = Math.toRadians(coords[1]); // Convert longitude to radians
			rlt1 = Math.toRadians(coords[0]); // Convert latitude to radians
			rlat2 = rlt1 + dy * REINV; // Convert distance to radians
			rlat2 = Math.asin(Math.sin(rlat2) * Math.cos(dx * REINV)); // Trigonometry
			// magic!
			dlon = Math.atan2(Math.sin(dx * REINV) * Math.cos(rlt1), (Math.cos(dx
					* REINV) - Math.sin(rlt1) * Math.sin(rlat2)));
			rlon2 = Math.toDegrees(rln1 + dlon); // Convert back
			rlat2 = Math.toDegrees(rlat2); // same

			return new double[] { rlat2, rlon2 };

		}


		/**
		 * Calculates the distance traveled along a sphere (great circle distance)
		 * 
		 * @param rlon1 -
		 *            The longitude of origin
		 * @param rlat1 -
		 *            The latitude of origin
		 * @param rlon2 -
		 *            The destination longitude
		 * @param rlat2 -
		 *            The destination latitude.
		 * @return - Distance traveled in meters.
		 */

		public static double distance_Sphere(double rlon1, double rlat1,
				double rlon2, double rlat2) {

			double rln1, rlt1, rln2, rlt2;
			double dist;

			rln1 = Math.toRadians(rlon1);
			rlt1 = Math.toRadians(rlat1);
			rln2 = Math.toRadians(rlon2);
			rlt2 = Math.toRadians(rlat2);

			double d_lambda = Math.abs(rln1 - rln2);

			// Simple great circle distance

			// dist = Math.acos(Math.cos(rlt1) * Math.cos(rlt2) * Math.cos(rln2 -
			// rln1)
			// + Math.sin(rlt1) * Math.sin(rlt2));

			// More complex great circle distance formula to reduce error due to
			// rounding.

			double n1 = Math.pow(Math.cos(rlt2) * Math.sin(d_lambda), 2);
			double n2 = Math.pow(Math.cos(rlt1) * Math.sin(rlt2) - Math.sin(rlt1)
					* Math.cos(rlt2) * Math.cos(d_lambda), 2);
			double numerator = Math.sqrt(n1 + n2);
			double denominator = Math.sin(rlt1) * Math.sin(rlt2) + Math.cos(rlt1)
			* Math.cos(rlt2) * Math.cos(d_lambda);
			dist = Math.atan2(numerator, denominator);

			return R_EARTH * Math.toDegrees(dist) / 360;
		}

		public static double distance_Sphere2(double rlon1, double rlat1,
				double rlon2, double rlat2) {

			double rln1, rlt1, rln2, rlt2;
			double dist;

			rln1 = Math.toRadians(rlon1);
			rlt1 = Math.toRadians(rlat1);
			rln2 = Math.toRadians(rlon2);
			rlt2 = Math.toRadians(rlat2);

			//double d_lambda = Math.abs(rln1 - rln2);

			// Simple great circle distance

			dist = Math.acos(Math.cos(rlt1) * Math.cos(rlt2) * Math.cos(rln2 -
					rln1) + Math.sin(rlt1) * Math.sin(rlt2));

			// More complex great circle distance formula to reduce error due to
			// rounding.

			/*		double n1 = Math.pow(Math.cos(rlt2) * Math.sin(d_lambda), 2);
		double n2 = Math.pow(Math.cos(rlt1) * Math.sin(rlt2) - Math.sin(rlt1)
			 * Math.cos(rlt2) * Math.cos(d_lambda), 2);
		double numerator = Math.sqrt(n1 + n2);
		double denominator = Math.sin(rlt1) * Math.sin(rlt2) + Math.cos(rlt1)
			 * Math.cos(rlt2) * Math.cos(d_lambda);
		dist = Math.atan2(numerator, denominator);
			 */
			return R_EARTH * Math.toDegrees(dist) / 360;
		}

		public void step2(){

			double x = 2;
			double y = 1; 

			Vector3D vector1 = new Vector3D(x,0,0);
			Vector3D vector2 = new Vector3D(0,y,0);

			Vector3D vector3 = vector1.add(vector2); 
			//NOTE: getNorm() is the new vector magnitude from the addition, while getAlpha is the angle (in radians)
			System.out.println(vector3.getX() + "\t" + vector3.getY() + "\t" + vector3.getZ() + "\t" + vector3.getNorm() + "\t" + Math.toDegrees(vector3.getAlpha()));

			double angle = 60; //Math.toDegrees(Math.atan(y/x)) ; //getAlpha(); 
			double hyp = 0.898 * 0.514444444; //vector3.getNorm();  //vector3.getNorm(); 

			double u = Math.cos(Math.toRadians(angle))* hyp; //x
			double v= Math.sin(Math.toRadians(angle)) * hyp; //y
			System.out.println(u + "\t" + v); 

		}

	}
