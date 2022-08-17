package us.fl.state.fwc.abem.test;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;

import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.fractal.FractalBuilder;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleMapper;
import us.fl.state.fwc.util.geo.SimpleShapefile;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class RKMoveTester {

	long seed = System.currentTimeMillis(); 
	MersenneTwister m = new MersenneTwister((int) seed); 
	Uniform uniform = new Uniform(m); 
	Normal normal = new Normal(0,1,m); 
	double[] probabilities = {0.3, 0.3, 0.3, 0.3}; 
	EmpiricalWalker ew = new EmpiricalWalker(probabilities, Empirical.NO_INTERPOLATION, m);

	protected DecimalFormat df = new DecimalFormat("#.#"); 
	protected DecimalFormat df2 = new DecimalFormat("#"); 

	double[] cashKarpParams = 
	{ 0.2f, 																															//B21 
			3.0f/40.0f, 9.0f/40.0f, 																										//B31-B32 
			0.3f, -0.9f, 1.2f, 																												//B41-B43
			-11.0f/54.0f, 2.5f, -70.0f/27.0f, 35.0f/27.0f, 																		//B51-B54
			1631.0f/55296.0f, 175.0f/512.0f, 575.0f/13824.0f, 44275.0f/110592.0f, 253.0f/4096.0f,			//B61-B65 
			37.0f/378.0f, 250.0f/621.0f, 125.0f/594.0f, 512.0f/1771.0f };													//C1-C6
	int threadCount = 1; 
	double[] aku1 = new double[threadCount];
	double[] aku2= new double[threadCount];
	double[] aku3= new double[threadCount];
	double[] aku4= new double[threadCount];
	double[] aku5= new double[threadCount];
	double[] aku6= new double[threadCount];
	double[] akv1= new double[threadCount];
	double[] akv2= new double[threadCount];
	double[] akv3= new double[threadCount];
	double[] akv4= new double[threadCount];
	double[] akv5= new double[threadCount];
	double[] akv6= new double[threadCount];
	int gridXDim; 
	int gridYDim;  
	int gridXSize;  
	int gridYSize; 
	int[][] landGrid; 
	double[][][] u;   
	double[][][] v;   
	int threadID = 0; 
	int timeStep; 
	Coordinate pos;  
	Coordinate tempPos = new Coordinate(0, 0, 0); 
	Coordinate physicalData = new Coordinate(0, 0, 0); 
	Int3D gridID= new Int3D(0, 0, 0); 
	Int3D tempGridID = new Int3D(0, 0, 0); 

	double[][] points; 

	double maxVelocity = 1.5;
	int landSearchRadius; 
	
	NetCDFFile uvel; // = new NetCDFFile("dataTest/uvel_norotate.nc"); 
	NetCDFFile vvel; // = new NetCDFFile("dataTest/vvel_norotate.nc"); 

	//=================================================================================
	//====== Setup Method
	//=================================================================================

	public void setup(){

		landGrid = getEFDCCellFile("dataTest/cell.inp");
		try {
			uvel = new NetCDFFile("dataTest/uvel_norotate.nc");
			vvel = new NetCDFFile("dataTest/vvel_norotate.nc"); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		gridXDim = landGrid[0].length; //each cell 100x100m
		gridYDim = landGrid.length; 
		gridXSize = 500; 
		gridYSize = 500; 
		double roughness = .8; // low numbers (e.g., .1) -> high spatial aggregation, versus high numbers (~2) -> low spatial aggregation

		double xStart = 13684;//((double)gridXSize*(double)gridXDim)/2.0 + Math.random()*(double)gridXSize;
		double yStart = 24765;//((double)gridYSize*(double)gridYDim)/2.0 + Math.random()*(double)gridYSize;
		timeStep = 3600;
		 
		landSearchRadius = (int) ((maxVelocity*timeStep)/gridXSize) + 1;//this is 
		int numTimeSteps = 24; 
		pos = new Coordinate(xStart, yStart, 0);

		points = new double[2][numTimeSteps+1]; 
		points[0][0] = pos.x;
		points[1][0] = pos.y;
		System.out.println("seed: " + seed); 
		FractalBuilder fb = new FractalBuilder(gridXDim,gridYDim,roughness, seed); 
		int[][] tempLand = fb.getCategories(new double[]{.5, .5});

		u = new double[numTimeSteps][gridYDim][gridXDim];  
		v = new double[numTimeSteps][gridYDim][gridXDim];  

		//set up u/v data

/*		for (int i=0; i<gridXDim; i++){
			for (int j=0; j<gridYDim; j++){
				if ( (i==0) || (j==0) || (i==(gridXDim-1)) || (j==(gridYDim-1))){
					landGrid[i][j] = 0;
				}
				else landGrid[i][j] = tempLand[i][j]; // water
			}
		}
		
		landGrid[(int) yStart/gridYSize][(int) xStart/gridXSize] = 1; //set the start position to water
		landGrid[(int) yStart/gridYSize-1][(int) xStart/gridXSize] = 1; //set the start position to water
		landGrid[(int) yStart/gridYSize-1][(int) xStart/gridXSize-1] = 1; //set the start position to water
		landGrid[(int) yStart/gridYSize][(int) xStart/gridXSize-1] = 1; //set the start position to water
*/

		for (int h=0; h<numTimeSteps; h++){
		for (int i=0; i<gridYDim; i++){
			for (int j=0; j<gridXDim; j++){
				if (landGrid[i][j] == 0){
					u[h][i][j] = 0;
					v[h][i][j] = 0; 
				}
				else{
					u[h][i][j] = normal.nextDouble(.2, 0.5);
					v[h][i][j] = normal.nextDouble(0, 0.25);
				}
			}
		}
		}
		/*		System.out.println("====== U velocities =====");
		for (int i=0; i<gridXDim; i++){
			System.out.println(Arrays.toString(u[i]));
		}

		System.out.println("\n====== V velocities =====");
		for (int i=0; i<gridXDim; i++){
			System.out.println(Arrays.toString(u[i]));
		}
		 */


		//create barrier shapefile for later display
/*		String barrierShpName = "data/barrier.shp"; 
		Coordinate[][] polyCoords = {  {new Coordinate(barrierMinX*gridXSize,barrierMinY*gridYSize), new Coordinate(barrierMaxX*gridXSize,barrierMinY*gridYSize), 
			new Coordinate(barrierMaxX*gridXSize,barrierMaxY*gridYSize), new Coordinate(barrierMinX*gridXSize,barrierMaxY*gridYSize), new Coordinate(barrierMinX*gridXSize,barrierMinY*gridYSize)}  }; 
		SimpleShapefile barrier = new SimpleShapefile(barrierShpName);
		barrier.createShapefile(Polygon.class, polyCoords);

		//create boundary shapefile for later display
		String boundShpName = "data/bound.shp"; 
		Coordinate[][] boundCoords = {  {new Coordinate(0,0), new Coordinate(gridXDim*gridXSize,0), 
			new Coordinate(gridXDim*gridXSize,gridYDim*gridYSize), new Coordinate(0,gridYDim*gridYSize), new Coordinate(0,0)}  }; 
		SimpleShapefile bound = new SimpleShapefile(boundShpName);
		bound.createShapefile(Polygon.class, boundCoords);
*/
		//create dynamic shapefile, which will add new points to each time step
		String pointsShpName = "dataTest/Points.shp"; 
		SimpleShapefile pointsShp = new SimpleShapefile(pointsShpName);
		Coordinate[][] pointCoords = {  {new Coordinate(pos.x,pos.y)}  }; 
		pointsShp.createShapefile(Point.class, pointCoords);

		//this will store bounce vectors
		String vectorsShpName = "dataTest/Vectors.shp"; 
		SimpleShapefile vectorsShp = new SimpleShapefile(vectorsShpName);
		vectorsShp.createShapefile(LineString.class, "UTM"); 

		String gridShpName = "dataTest/Grid.shp"; 
		SimpleShapefile gridShp = new SimpleShapefile(gridShpName); 
		Coordinate[][] gridCoords = new Coordinate[gridXDim+gridYDim][2]; 
		int counter=0; 
		for (int i=0; i<gridXDim; i++){
			gridCoords[counter][0] = new Coordinate(i*gridXSize,0);
			gridCoords[counter][1] = new Coordinate(i*gridXSize,gridYDim*gridYSize); 
			counter++;
		}
		for (int j=0; j<gridYDim; j++){
			gridCoords[counter][0] = new Coordinate(0,j*gridYSize);
			gridCoords[counter][1] = new Coordinate(gridXDim*gridXSize, j*gridXSize); 
			counter++;
		}
		gridShp.createShapefile(LineString.class, gridCoords); 

		//Create Map
		Toolkit toolkit =  Toolkit.getDefaultToolkit ();
        Dimension dim = toolkit.getScreenSize();
		SimpleMapper map = new SimpleMapper("map test", dim.width-100, dim.height-100); 
		String[] layers = {pointsShpName, vectorsShpName, gridShpName};
		Style[] layerStyles = {	//SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), .5f),
				//SLD.createPolygonStyle(null, new Color(255, 0, 255), 0.01f), 
				SLD.createPointStyle("circle", new Color(150,0,150), new Color(150,0,150), 1.0f, 5f),
				SLD.createLineStyle(new Color(150,0,150), 1f),
				SLD.createLineStyle(new Color(0, 0, 0), 1f)};
		ArrayList<float[][]> rasters = new ArrayList<float[][]>();
		rasters.add(map.int2float(map.reflectArray(landGrid))); 
		map.drawMixedLayers(layers, layerStyles, rasters, new ReferencedEnvelope[]{map.getRasterBounds(0, 0, gridXDim*gridXSize, gridYDim*gridYSize, null)}, null); 
//		map.drawShapefileLayers(layers, layerStyles);


		Coordinate pointCoord = new Coordinate(); 
		for (int i = 1; i<numTimeSteps+1; i++) {
			System.out.println("=============== Time Step " + i + " ===============");
			System.out.println("Starting Position:\t(" + df.format(pos.x) + ", " + df.format(pos.y) + ")"); 
			ArrayList<Coordinate[]> bounceVectors = RKStep(i-1); 
			points[0][i] = pos.x;
			points[1][i] = pos.y;
			pointCoord.x = pos.x;
			pointCoord.y = pos.y; 

			map.addLag(1.5);
			Coordinate[][] tempCoord = {{pointCoord}};
			//String[] tempCoordName = { "point" };
			pointsShp.addFeatures(Point.class, tempCoord, null);
			vectorsShp.addFeatures(LineString.class, bounceVectors, null);
			map.repaint();
		}

		map.deleteFiles(pointsShpName, vectorsShpName, gridShpName); 
	}



	//=================================================================================
	//====== RK Step Method
	//=================================================================================

	public ArrayList<Coordinate[]> RKStep(int time){
		ArrayList<Coordinate[]> vectors = new ArrayList<Coordinate[]>(); // for drawing the movement vector 
		
		gridID.x = (int) (pos.x/(double) gridXSize);
		gridID.y = (int) (pos.y/(double) gridYSize);

		//runge kutta steps using cash-karp pararmeters
		//|||||||||||||||||||||||||||| CALC 1 ||||||||||||||||||||||||||||
		if ( (gridID.x < 0) || (gridID.y < 0) || (gridID.x > (gridXDim-1) ) || (gridID.y > (gridYDim-1)) ) {
			System.out.println("particle lost."); 
			return vectors; 
		}
		else {
			physicalData.x = uvel.getValue("uvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); // new Point3D(gridID.x, gridID.y, 0), time); //u[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			physicalData.y = vvel.getValue("vvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); //new Point3D(gridID.x, gridID.y, 0), time); ; //v[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			if (physicalData.x == -9999) physicalData.x = 0;
			if (physicalData.y == -9999) physicalData.y = 0;
		}
		//System.out.println("\tu:\t" + physicalData.x + "\tv:\t" + physicalData.y); 

		aku1[threadID] = physicalData.x;
		akv1[threadID] = physicalData.y;
		//		dx = cashKarpParams[0] * timeStep * aku1[threadID];
		//		dy = cashKarpParams[0] * timeStep * akv1[threadID];
		tempPos.x = pos.x+cashKarpParams[0] * timeStep * aku1[threadID];
		tempPos.y = pos.y+cashKarpParams[0] * timeStep * akv1[threadID];
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 

		//getGridID(tempPos, tempGridID);
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);

		//|||||||||||||||||||||||||||| CALC 2 ||||||||||||||||||||||||||||
		if ( (gridID.x < 0) || (gridID.y < 0) || (gridID.x > (gridXDim-1) ) || (gridID.y > (gridYDim-1))){
			System.out.println("particle lost."); 
			return vectors; 
		}
		else {
			physicalData.x = uvel.getValue("uvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); // new Point3D(gridID.x, gridID.y, 0), time); //u[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			physicalData.y = vvel.getValue("vvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); //new Point3D(gridID.x, gridID.y, 0), time); ; //v[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			if (physicalData.x == -9999) physicalData.x = 0;
			if (physicalData.y == -9999) physicalData.y = 0;
		}
		//System.out.println("\tu:\t" + physicalData.x + "\tv:\t" + physicalData.y); 

		if ((physicalData.x == 9999) || (physicalData.y == 9999) ) {
			aku2[threadID] = aku1[threadID] ;
			akv2[threadID] = akv1[threadID] ;
		}
		else {
			aku2[threadID] = physicalData.x;
			akv2[threadID] = physicalData.y;
		}
		tempPos.x = pos.x+timeStep * (cashKarpParams[1]  * aku1[threadID] + cashKarpParams[2] * aku2[threadID]);
		tempPos.y = pos.y+timeStep * (cashKarpParams[1]  * akv1[threadID] + cashKarpParams[2] * akv2[threadID]);
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 

		//getGridID(tempPos, tempGridID);
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);



		//|||||||||||||||||||||||||||| CALC 3 ||||||||||||||||||||||||||||
		if ( (gridID.x < 0) || (gridID.y < 0) || (gridID.x > (gridXDim-1) ) || (gridID.y > (gridYDim-1))){
			System.out.println("particle lost."); 
			return vectors; 
		}
		else {
			physicalData.x = uvel.getValue("uvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); // new Point3D(gridID.x, gridID.y, 0), time); //u[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			physicalData.y = vvel.getValue("vvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); //new Point3D(gridID.x, gridID.y, 0), time); ; //v[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			if (physicalData.x == -9999) physicalData.x = 0;
			if (physicalData.y == -9999) physicalData.y = 0;
		}
		//System.out.println("\tu:\t" + physicalData.x + "\tv:\t" + physicalData.y); 

		if ((physicalData.x == 9999) || (physicalData.y == 9999) ) {
			aku3[threadID] = aku2[threadID] ;
			akv3[threadID] = akv2[threadID] ;
		}
		else {
			aku3[threadID] = physicalData.x;
			akv3[threadID] = physicalData.y;
		}
		tempPos.x = pos.x+timeStep * (cashKarpParams[3]  * aku1[threadID]
		                                                        + cashKarpParams[4] * aku2[threadID]
		                                                                                   + cashKarpParams[5] * aku3[threadID]);
		tempPos.y = pos.y+timeStep * (cashKarpParams[3]  * akv1[threadID]
		                                                        + cashKarpParams[4] * akv2[threadID]
		                                                                                   + cashKarpParams[5] * akv3[threadID]);
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 

		//getGridID(tempPos, tempGridID);
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);



		//|||||||||||||||||||||||||||| CALC 4 ||||||||||||||||||||||||||||
		if ( (gridID.x < 0) || (gridID.y < 0) || (gridID.x > (gridXDim-1) ) || (gridID.y > (gridYDim-1))){
			System.out.println("particle lost."); 
			return vectors; 
		}
		else {
			physicalData.x = uvel.getValue("uvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); // new Point3D(gridID.x, gridID.y, 0), time); //u[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			physicalData.y = vvel.getValue("vvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); //new Point3D(gridID.x, gridID.y, 0), time); ; //v[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			if (physicalData.x == -9999) physicalData.x = 0;
			if (physicalData.y == -9999) physicalData.y = 0;
		}
		//System.out.println("\tu:\t" + physicalData.x + "\tv:\t" + physicalData.y); 

		if ((physicalData.x == 9999) || (physicalData.y == 9999) ) {
			aku4[threadID] = aku3[threadID] ;
			akv4[threadID] = akv3[threadID] ;
		}
		else {
			aku4[threadID] = physicalData.x;
			akv4[threadID] = physicalData.y;
		}
		tempPos.x = pos.x+timeStep * (cashKarpParams[6]  * aku1[threadID]
		                                                        + cashKarpParams[7] * aku2[threadID]
		                                                                                   + cashKarpParams[8] * aku3[threadID]
		                                                                                                              + cashKarpParams[9] * aku4[threadID]);
		tempPos.y = pos.y+timeStep * (cashKarpParams[6]  * akv1[threadID]
		                                                        + cashKarpParams[7] * akv2[threadID]
		                                                                                   + cashKarpParams[8] * akv3[threadID]
		                                                                                                              + cashKarpParams[9] * akv4[threadID]);
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 

		//getGridID(tempPos, tempGridID);
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);



		//|||||||||||||||||||||||||||| CALC 5 ||||||||||||||||||||||||||||
		if ( (gridID.x < 0) || (gridID.y < 0) || (gridID.x > (gridXDim-1) ) || (gridID.y > (gridYDim-1))){
			System.out.println("particle lost."); 
			return vectors; 
		}
		else {
			physicalData.x = uvel.getValue("uvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); // new Point3D(gridID.x, gridID.y, 0), time); //u[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			physicalData.y = vvel.getValue("vvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); //new Point3D(gridID.x, gridID.y, 0), time); ; //v[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			if (physicalData.x == -9999) physicalData.x = 0;
			if (physicalData.y == -9999) physicalData.y = 0;
		}
		//System.out.println("\tu:\t" + physicalData.x + "\tv:\t" + physicalData.y); 

		if ((physicalData.x == 9999) || (physicalData.y == 9999) ) {
			aku5[threadID] = aku4[threadID] ;
			akv5[threadID] = akv4[threadID] ;
		}
		else {
			aku5[threadID] = physicalData.x;
			akv5[threadID] = physicalData.y;
		}
		tempPos.x = pos.x+timeStep * (cashKarpParams[10]  * aku1[threadID]
		                                                         + cashKarpParams[11] * aku2[threadID]
		                                                                                     + cashKarpParams[12] * aku3[threadID]
		                                                                                                                 + cashKarpParams[13] * aku4[threadID]
		                                                                                                                                             + cashKarpParams[14] * aku5[threadID]);
		tempPos.y = pos.y+timeStep * (cashKarpParams[10]  * akv1[threadID]
		                                                         + cashKarpParams[11] * akv2[threadID]
		                                                                                     + cashKarpParams[12] * akv3[threadID]
		                                                                                                                 + cashKarpParams[13] * akv4[threadID]
		                                                                                                                                             + cashKarpParams[14] * akv5[threadID]);
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 

		//getGridID(tempPos, tempGridID);
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);




		//|||||||||||||||||||||||||||| CALC 6 ||||||||||||||||||||||||||||
		if ( (gridID.x < 0) || (gridID.y < 0) || (gridID.x > (gridXDim-1) ) || (gridID.y > (gridYDim-1))){
			System.out.println("particle lost."); 
			return vectors; 
		}
		else {
			physicalData.x = uvel.getValue("uvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); // new Point3D(gridID.x, gridID.y, 0), time); //u[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			physicalData.y = vvel.getValue("vvel", new int[] {time, 0, gridID.y, gridID.x}).doubleValue(); //new Point3D(gridID.x, gridID.y, 0), time); ; //v[time][gridID.y][gridID.x]; 	//0.1; physicalData.y=0.1; physicalData.z=0.1;
			if (physicalData.x == -9999) physicalData.x = 0;
			if (physicalData.y == -9999) physicalData.y = 0;
		}
		//System.out.println("\tu:\t" + physicalData.x + "\tv:\t" + physicalData.y); 

		if ((physicalData.x == 9999) || (physicalData.y == 9999) ) {
			aku6[threadID] = aku5[threadID] ;
			akv6[threadID] = akv5[threadID] ;
		}
		else {
			aku6[threadID] = physicalData.x;
			akv6[threadID] = physicalData.y;
		}
		tempPos.x = pos.x+timeStep * (cashKarpParams[15]  * aku1[threadID]
		                                                         + cashKarpParams[16] * aku3[threadID]
		                                                                                     + cashKarpParams[17] * aku4[threadID]
		                                                                                                                 + cashKarpParams[18] * aku6[threadID]);
		tempPos.y = pos.y+timeStep * (cashKarpParams[15]  * akv1[threadID]
		                                                         + cashKarpParams[16] * akv3[threadID]
		                                                                                     + cashKarpParams[17] * akv4[threadID]
		                                                                                                                 + cashKarpParams[18] * akv6[threadID]);
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 

		//getGridID(tempPos, tempGridID);
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);


		// Set the gridID after movement is done
		vectors = checkLandBounce(pos, tempPos,  gridID); 

		//if end position (i.e., tempPos) is suitable, then set pos = tempPos
		pos.x = tempPos.x; //set the position
		pos.y = tempPos.y;
		System.out.println("Position:\t(" + df.format(tempPos.x) + ", " + df.format(tempPos.y) + ")"); 
		//getGridID(pos, gridID); //set the gridID

		 return vectors; 
	}




	//=================================================================================
	//====== check land bounce -- using graphical line following approach
	//=================================================================================

	public ArrayList<Coordinate[]> checkLandBounce(Coordinate startPoint, Coordinate endPoint, Int3D gridID){
		ArrayList<Coordinate[]> vectors = new ArrayList<Coordinate[]>(); 
		
		//int[] gridID = new int[2];
		gridID.x = (int) (startPoint.x/(double) gridXSize); //= getXYGridID(startPoint); 
		gridID.y = (int) (startPoint.y/(double) gridYSize); //= getXYGridID(startPoint);

		// if the end point is in the same grid cell as the start point, then don't go through this set of steps
		if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
			Coordinate[] vec = {
					new Coordinate(startPoint.x,startPoint.y), 
					new Coordinate(endPoint.x,endPoint.y)}; // point of intercept
			vectors.add(vec);
			return vectors; 
		}
		
		//checks to see if particle is near land -- if it is, will have to check every box it passes through to make sure it doesn't 'jump over' a sliver of land during movement
		boolean nearLand = false; 
		for (int i=gridID.y-landSearchRadius; i<gridID.y+landSearchRadius; i++ ){
			for (int j=gridID.x-landSearchRadius; j<gridID.x+landSearchRadius; j++ ){
				if ( (i>=0) && (j>=0) && (i<landGrid.length) && (j<landGrid[0].length) && (landGrid[i][j] == 0)) nearLand = true;  
			}
		}
		
		
		//while the particle's end point is either on land, or if the particle is in the vicinity of land, then loop through
		while ( (landGrid[(int) (endPoint.y/(double) gridYSize)][(int) (endPoint.x/(double) gridXSize)] == 0)  || nearLand ){
			double vecSlope = (endPoint.y-startPoint.y)/(endPoint.x-startPoint.x);
			double vecIntercept = (startPoint.y)-(vecSlope*startPoint.x);


			int moveType = 0;  //0=still in water and haven't moved; 1=still in water but have moved; 2=have moved and on land

			//follow boxes until find one that is land
			while (moveType == 0){

				//========================================
				//check N boundary
				if ( ((endPoint.y-startPoint.y) > 0) && ((( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize+gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize+gridXSize)) + vecIntercept) - (gridID.y*gridYSize + gridYSize) ) ) < 0) ) {

					gridID.y = gridID.y + 1;
					gridID.x = gridID.x;

					if (landGrid[gridID.y][gridID.x] == 0 ){

						//add a vector from the start position to point of intercept on land
						Coordinate[] vec = {
								new Coordinate(startPoint.x,startPoint.y), 
								new Coordinate((((gridID.y*gridYSize) - vecIntercept)/vecSlope),(gridID.y*gridYSize))}; // point of intercept
						vectors.add(vec);

						//simple calculation for uniform nonrotated grid 
						endPoint.y = 2*(gridID.y*gridYSize)-endPoint.y; 
						
						//reset startPoint to the point of intercept where particle hits land
						startPoint.y = (gridID.y*gridYSize);
						startPoint.x = (((gridID.y*gridYSize) - vecIntercept)/vecSlope); 

						moveType = 2;
						nearLand=false; 
					}
					
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					else if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
						moveType=2;
						nearLand=false; 
					}
					
					else {
						System.out.println("Am moving north...alas am wallowing in water");
						//currentGridID[0] = nextGridID[0];
						//currentGridID[1] = nextGridID[1]; 

						moveType = 1; 
					}
				}


				//========================================
				//check E boundary

				if ( (moveType < 1) && ((endPoint.x-startPoint.x) > 0) &&  (( (vecSlope*((gridID.x*gridXSize+gridXSize)) + vecIntercept) - (gridID.y*gridYSize + gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize + gridXSize)) + vecIntercept) - (gridID.y*gridYSize) ) ) < 0 ) {

					gridID.x = gridID.x + 1;
					gridID.y = gridID.y; 

					if (landGrid[gridID.y][gridID.x] == 0 ){

						//add a vector from the start position to point of intercept on land
						Coordinate[] vec = {
								new Coordinate(startPoint.x,startPoint.y), 
								new Coordinate((gridID.x*gridXSize),(vecSlope*((gridID.x*gridXSize)) + vecIntercept))}; // point of intercept
						vectors.add(vec);

						//simple calculation for uniform nonrotated grid 
						endPoint.x = 2*(gridID.x*gridXSize)-endPoint.x;  
						startPoint.x = (gridID.x*gridXSize);  
						startPoint.y = (vecSlope*((gridID.x*gridXSize)) + vecIntercept);

						moveType = 2; 
						nearLand=false; 

					}
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					else if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
						moveType=2;
						nearLand=false; 
					}
					
					else {
						System.out.println("Am moving east...alas am wallowing in water");

						moveType = 1; 
					}
				}


				//========================================
				//check S boundary
				if ( (moveType < 1) && ((endPoint.y-startPoint.y) < 0) &&  (( (vecSlope*((gridID.x*gridXSize + gridXSize)) + vecIntercept) - (gridID.y*gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize) ) ) < 0 ) {

					gridID.y = gridID.y - 1;
					gridID.x = gridID.x; 

					if (landGrid[gridID.y][gridID.x] == 0 ){

						//add a vector from the start position to point of intercept on land
						Coordinate[] vec = {
								new Coordinate(startPoint.x,startPoint.y), 
								new Coordinate( (((gridID.y*gridYSize + gridYSize ) - vecIntercept)/vecSlope), (gridID.y*gridYSize + gridYSize) )  }; // point of intercept
						vectors.add(vec);

						//simple calculation for uniform nonrotated grid 
						endPoint.y = 2*(gridID.y*gridYSize+gridYSize)-endPoint.y;  
						startPoint.y = (gridID.y*gridYSize + gridYSize); 
						startPoint.x = (((gridID.y*gridYSize + gridYSize) - vecIntercept)/vecSlope); 

						moveType = 2; 
						nearLand=false; 

					}
					
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					else if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
						moveType=2;
						nearLand=false; 
					}
					
					else {
						System.out.println("Am moving south...alas am wallowing in water");

						moveType = 1; 
					}
				}


				//========================================
				//check W boundary
				if ( (moveType < 1) && ((endPoint.x-startPoint.x) < 0) &&  (( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize + gridYSize) ) ) < 0 ) {

					gridID.x = gridID.x - 1;
					gridID.y = gridID.y; 

					if (landGrid[gridID.y][gridID.x] == 0 ){

						//add a vector from the start position to point of intercept on land
						Coordinate[] vec = {
								new Coordinate(startPoint.x,startPoint.y), 
								new Coordinate( (gridID.x*gridXSize + gridXSize),(vecSlope*((gridID.x*gridXSize+ gridXSize)) + vecIntercept)  )  }; // point of intercept
						vectors.add(vec);

						//simple calculation for uniform nonrotated grid 
						endPoint.x = 2*(gridID.x*gridXSize+gridXSize)-endPoint.x;  
						startPoint.x = (gridID.x*gridXSize + gridXSize);  
						startPoint.y = (vecSlope*((gridID.x*gridXSize + gridXSize)) + vecIntercept); //HERE'S WHERE IS MESSED UP

						moveType = 2; 
						nearLand=false; 

					}
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					else if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
						moveType=2;
						nearLand=false; 
					}

					else  System.out.println("Am moving west...alas am wallowing in water");

				}

				if (moveType ==1 ) moveType = 0; //reset to 0 so will continue loop, but if it's set to 2 (has encountered land), then will break out

			} // end while (boxIsWater)

		}//end while (end point is on land)
		
		//add a vector from the start to end point
		// NOTE: if it was on land and went into while loop, the startPoint was recently set to point of intercept with land
		Coordinate[] vec = {
				new Coordinate(startPoint.x,startPoint.y), 
				new Coordinate(endPoint.x,endPoint.y)}; // point of intercept
		vectors.add(vec);


	return vectors; 
	}

	
	
		
	
	//=================================================================================
	//====== inputs the land grid from an EFDC Cell.inp file 
	//=================================================================================

	public int[][] getEFDCCellFile(String filename){
		
		int[][] landGrid = new int[131][113];
		int headerLines = 4; 
		int numColumns = 113; 
		File fFile = new File(filename);  
		try {
			Scanner scanner = new Scanner(fFile);
			for (int i=0; i<headerLines; i++){
				scanner.nextLine();
			}
			int counter=0; 
			while ( scanner.hasNextLine() ){
				Scanner lineScanner = new Scanner(scanner.nextLine()); 

				int row = lineScanner.nextInt(); 
				String columns = lineScanner.next();
				
				String[] tokens = columns.split(""); 
				for (int i=0; i<numColumns; i++){
					int value = Integer.parseInt(tokens[i+1]);
					if (value == 5) landGrid[row-1][i] =1; 
					else landGrid[row-1][i] =0; 
				}
				
				lineScanner.close();
			} // end file scanner
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}
		
/*		for (int i=0; i<landGrid.length; i++){
			for (int j=0; j<landGrid[0].length; j++){
				System.out.print(landGrid[i][j]);
			}
			System.out.println();
		}
*/		
		return landGrid;
	}

	
	
	public static int getLineNumber() {
		return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}

	
	
	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

	
	
	public static String getClassName() {
		return Thread.currentThread().getStackTrace()[2].getClassName();
	}


	
	
	public void getGridID(Coordinate pos, Int3D gridID) {
		gridID.x = (int) (tempPos.x/(double) gridXSize);
		gridID.y = (int) (tempPos.y/(double) gridYSize);
	}


	
	//=================================================================================
	//====== main method
	//=================================================================================

	public static void main(String[] args) {

		RKMoveTester mt = new RKMoveTester(); 
		mt.setup(); 

	}

}
