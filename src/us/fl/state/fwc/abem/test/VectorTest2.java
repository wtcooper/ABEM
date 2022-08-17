package us.fl.state.fwc.abem.test;

import java.awt.Color;
import java.util.ArrayList;

import org.apache.commons.math.geometry.Vector3D;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;

import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.SimpleMapper;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class VectorTest2 {
	float gridXDim = 500;
	float gridYDim = 500; 

	boolean outputMap = true;


	public static void main(String[] args) {
		VectorTest2 test = new VectorTest2();
//		test.vectorBounce();
		System.out.println("===================vector bounce unform================="); 
		test.vectorBounceUniform();

	}


	/**Vector Bounce algorithm designed to run in C and for a uniform, unrotated grid
	 * 
	 */
	public void vectorBounceUniform(){

		float[] startPoint = {750f, 750f}; 
		float[] endPoint = {700f, 400f};

		int[][] landMask = { //NOTE: this is row-major order [y][x], so need to call appropriately below --> rotate it visually 90 degrees counter clockwise 
				{1, 1, 1}, 	//  (0,0)   (0,1)   (0,2)	
				{1, 0, 1}, 	//  (1,0)   (1,1)   (1,2)
				{1, 1, 1}  	//  (2,0)   (2,1)   (2,2)
		};

		float vecSlope =0; 
		float vecIntercept =0; 
		int[] gridID = new int[2];
		//int[] nextGridID = new int[2];

		
		//========================================
		//Mapping variables
		//========================================

		ArrayList<Coordinate[]>	boxList = new ArrayList<Coordinate[]>(); 
		final String pointsFilename = "dataTest/points.shp"; 
		final String boxFilename = "dataTest/boxes.shp"; 
		final String vecFilename = "dataTest/vectors.shp"; 
		SimpleShapefile points = new SimpleShapefile(pointsFilename); 
		SimpleShapefile boxes = new SimpleShapefile(boxFilename); 
		SimpleShapefile vectors = new SimpleShapefile(vecFilename); 

		Coordinate[][] pointCoords = { 
				{new Coordinate(startPoint[0],startPoint[1])},
				{new Coordinate(endPoint[0],endPoint[1])}  };
		points.createShapefile(Point.class, pointCoords); 
		Coordinate[][] vecCoords = { {
			new Coordinate(startPoint[0],startPoint[1]), 
			new Coordinate(endPoint[0],endPoint[1])}  };
		vectors.createShapefile(LineString.class, vecCoords); 

		if (outputMap){
			for (int i=0; i<landMask.length; i++){
				for (int j=0; j<landMask[i].length; j++){
					if (landMask[i][j] == 1){
						Coordinate[] array = {
								new Coordinate(i*gridXDim,j*gridYDim), 
								new Coordinate(i*gridXDim+gridXDim,j*gridYDim), 
								new Coordinate(i*gridXDim+gridXDim,j*gridYDim+gridYDim), 
								new Coordinate(i*gridXDim,j*gridYDim+gridYDim), 
								new Coordinate(i*gridXDim,j*gridYDim)};
						boxList.add(array);
					}
				}
			}

			Coordinate[][] boxArray = new Coordinate[boxList.size()][5]; 
			for (int i=0; i<boxArray.length; i++){
				for (int j=0; j<boxList.get(i).length; j++){
					boxArray[i][j] = boxList.get(i)[j]; 
				}
			}
			boxes.createShapefile(Polygon.class, boxArray); 
		}		
		
		Style pointsStyle = SLD.createPointStyle("circle", new Color(150,0,150), new Color(150,0,150), 1.0f, 5f); 
		Style boxStyle = SLD.createPolygonStyle(new Color(0, 125, 0), new Color(0, 125, 0), .5f);
		Style vecStyle = SLD.createLineStyle(new Color(255, 0, 255), 2f); //.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), 1f);
		Style[] mapStyles = {pointsStyle, boxStyle, vecStyle};
		String[] mapLayers = {pointsFilename, boxFilename, vecFilename}; 
		SimpleMapper map = new SimpleMapper();
		map.drawShapefileLayers(mapLayers, mapStyles); 
		//float[] pointOfIntercept = new float[2]; // = new Coordinate(startPoint.x, startPoint.y);
		//pointOfIntercept[0] = startPoint[0]; 
		//pointOfIntercept[1] = startPoint[1]; 

		//int counter = 0; //if multiple reflections needed


		//========================================
		//if the end point is on land
		//========================================

		while (landMask[(int) (endPoint[0]/(double) gridXDim )][(int) (endPoint[1]/(double) gridYDim )] == 1 ){


			System.out.println("Have encountered land, entering the bounce house");

			vecSlope = (endPoint[1]-startPoint[1])/(endPoint[0]-startPoint[0]);
			vecIntercept = (startPoint[1])-(vecSlope*startPoint[0]);
			gridID[0] = (int) (startPoint[0]/(double) gridXDim ); //= getXYGridID(startPoint); 
			gridID[1] = (int) (startPoint[1]/(double) gridYDim ); //= getXYGridID(startPoint);

			//boolean boxIsWater = true;
			int moveType = 0;  //0=still in water and haven't moved; 1=still in water but have moved; 2=have moved and on land
			//follow boxes until find one that is land
			while (moveType == 0){
				// check which box it passes through

				
				//========================================
				//check N boundary

				if ( ((endPoint[1]-startPoint[1]) > 0) && ((( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim+gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim+gridXDim)) + vecIntercept) - (gridID[1]*gridYDim + gridYDim) ) ) < 0) ) {

					gridID[1] = gridID[1] + 1;
					gridID[0] = gridID[0];


					if (landMask[gridID[0]][gridID[1]] == 1 ){
						System.out.println("Am moving north...land aho!");

						//simple calculation for uniform nonrotated grid 
						endPoint[1] = 2*(gridID[1]*gridYDim)-endPoint[1]; 

						moveType = 2;

						//reset start point to this point of intercept
						startPoint[1] = (gridID[1]*gridYDim);
						startPoint[0] = (((gridID[1]*gridYDim) - vecIntercept)/vecSlope); 
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

				if ( (moveType < 1) && ((endPoint[0]-startPoint[0]) > 0) &&  (( (vecSlope*((gridID[0]*gridXDim+gridXDim)) + vecIntercept) - (gridID[1]*gridYDim + gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim + gridXDim)) + vecIntercept) - (gridID[1]*gridYDim) ) ) < 0 ) {



					gridID[0] = gridID[0] + 1;
					gridID[1] = gridID[1]; 

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						System.out.println("Am moving east...land aho!");

						//simple calculation for uniform nonrotated grid 
						endPoint[0] = 2*(gridID[0]*gridXDim)-endPoint[0];  

						moveType = 2; 

						startPoint[0] = (gridID[0]*gridXDim);  
						startPoint[1] = (vecSlope*((gridID[0]*gridXDim)) + vecIntercept);

					}
					else {
						System.out.println("Am moving east...alas am wallowing in water");

						moveType = 1; 
					}

				}



				//========================================
				//check S boundary

				if ( (moveType < 1) && ((endPoint[1]-startPoint[1]) < 0) &&  (( (vecSlope*((gridID[0]*gridXDim + gridXDim)) + vecIntercept) - (gridID[1]*gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim) ) ) < 0 ) {

					gridID[1] = gridID[1] - 1;
					gridID[0] = gridID[0]; 

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						System.out.println("Am moving south...land aho!");

						//simple calculation for uniform nonrotated grid 
						endPoint[1] = 2*(gridID[1]*gridYDim+gridYDim)-endPoint[1];  

						moveType = 2; 

						startPoint[1] = (gridID[1]*gridYDim + gridYDim); 
						startPoint[0] = (((gridID[1]*gridYDim + gridYDim) - vecIntercept)/vecSlope); 
					}
					else {
						System.out.println("Am moving south...alas am wallowing in water");

						moveType = 1; 
					}
				}



				//========================================
				//check W boundary

				if ( (moveType < 1) && ((endPoint[0]-startPoint[0]) < 0) &&  (( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim + gridYDim) ) ) < 0 ) {

					gridID[0] = gridID[0] - 1;
					gridID[1] = gridID[1]; 

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						System.out.println("Am moving west...land aho!");

						//simple calculation for uniform nonrotated grid 
						endPoint[0] = 2*(gridID[0]*gridXDim+gridXDim)-endPoint[0];  
						moveType = 2; 

						startPoint[0] = (gridID[0]*gridXDim + gridXDim);  
						startPoint[1] = (vecSlope*((gridID[0]*gridXDim + gridXDim)) + vecIntercept); //HERE'S WHERE IS MESSED UP
					}
					//else {
					//	System.out.println("Am moving west...alas am wallowing in water");

					//	moveType = 1; 
					//}
				}

				if (moveType ==1 ) moveType = 0; //reset to 0 so will continue loop, but if it's set to 2 (has encountered land), then will break out


			} // end while (boxIsWater)

			System.out.println("point of intersection: (" + startPoint[0] + ", " + startPoint[1] + ")");
			System.out.println("final location after bounce: (" + endPoint[0] + ", " + endPoint[1]+ ")");
			if (landMask[(int) (endPoint[0]/(double) gridXDim )][(int) (endPoint[1]/(double) gridYDim )]== 0) System.out.println("i've ended up on water!!!");
			else if (landMask[(int) (endPoint[0]/(double) gridXDim )][(int) (endPoint[1]/(double) gridYDim )]== 1) System.out.println("i've ended up on land!!!");

			//counter++; 
			System.out.println(); 

			map.addLag(1);
			Coordinate[][] newPointCoords = { 
					{new Coordinate(startPoint[0],startPoint[1])},
					{new Coordinate(endPoint[0],endPoint[1])}  };
			points.addFeatures(Point.class, newPointCoords, null);
			Coordinate[][] newVecCoords = { {
				new Coordinate(startPoint[0],startPoint[1]), 
				new Coordinate(endPoint[0],endPoint[1])}  };
			vectors.addFeatures(LineString.class, newVecCoords, null); 
			map.repaint(); 

		} //end check on land

		map.deleteFiles(pointsFilename, boxFilename, vecFilename); 
	}

	/**Vector Bounce algorithm designed to run in C and for a uniform, unrotated grid
	 * 
	 */
	public void vectorBounceGPU(){

		float[] startPoint = {750f, 750f}; 
		float[] endPoint = {700f, 400f};

		int[][] landMask = {  
				{1, 1, 1}, 		
				{1, 0, 1}, 	
				{1, 1, 1}  	
		};

//		float vecSlope =0; 
//		float vecIntercept =0; 
//		


		//========================================
		//if the end point is on land
		//========================================

		while (landMask[(int) (endPoint[0]/(double) gridXDim )][(int) (endPoint[1]/(double) gridYDim )] == 1 ){


			float vecSlope = (endPoint[1]-startPoint[1])/(endPoint[0]-startPoint[0]);
			float vecIntercept = (startPoint[1])-(vecSlope*startPoint[0]);
			int[] gridID = new int[2];
			gridID[0] = (int) (startPoint[0]/(double) gridXDim ); //= getXYGridID(startPoint); 
			gridID[1] = (int) (startPoint[1]/(double) gridYDim ); //= getXYGridID(startPoint);

			int moveType = 0;  //0=still in water and haven't moved; 1=still in water but have moved; 2=have moved and on land

			//follow boxes until find one that is land
			while (moveType == 0){
				
				//========================================
				//check N boundary
				if ( ((endPoint[1]-startPoint[1]) > 0) && ((( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim+gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim+gridXDim)) + vecIntercept) - (gridID[1]*gridYDim + gridYDim) ) ) < 0) ) {

					gridID[1] = gridID[1] + 1;
					gridID[0] = gridID[0];

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						//simple calculation for uniform nonrotated grid 
						endPoint[1] = 2*(gridID[1]*gridYDim)-endPoint[1]; 
						startPoint[1] = (gridID[1]*gridYDim);
						startPoint[0] = (((gridID[1]*gridYDim) - vecIntercept)/vecSlope); 

						moveType = 2;
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

				if ( (moveType < 1) && ((endPoint[0]-startPoint[0]) > 0) &&  (( (vecSlope*((gridID[0]*gridXDim+gridXDim)) + vecIntercept) - (gridID[1]*gridYDim + gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim + gridXDim)) + vecIntercept) - (gridID[1]*gridYDim) ) ) < 0 ) {

					gridID[0] = gridID[0] + 1;
					gridID[1] = gridID[1]; 

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						//simple calculation for uniform nonrotated grid 
						endPoint[0] = 2*(gridID[0]*gridXDim)-endPoint[0];  
						startPoint[0] = (gridID[0]*gridXDim);  
						startPoint[1] = (vecSlope*((gridID[0]*gridXDim)) + vecIntercept);

						moveType = 2; 
					}
					else {
						moveType = 1; 
					}
				}


				//========================================
				//check S boundary

				if ( (moveType < 1) && ((endPoint[1]-startPoint[1]) < 0) &&  (( (vecSlope*((gridID[0]*gridXDim + gridXDim)) + vecIntercept) - (gridID[1]*gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim) ) ) < 0 ) {

					gridID[1] = gridID[1] - 1;
					gridID[0] = gridID[0]; 

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						//simple calculation for uniform nonrotated grid 
						endPoint[1] = 2*(gridID[1]*gridYDim+gridYDim)-endPoint[1];  
						startPoint[1] = (gridID[1]*gridYDim + gridYDim); 
						startPoint[0] = (((gridID[1]*gridYDim + gridYDim) - vecIntercept)/vecSlope); 

						moveType = 2; 
					}
					else {
						moveType = 1; 
					}
				}


				//========================================
				//check W boundary

				if ( (moveType < 1) && ((endPoint[0]-startPoint[0]) < 0) &&  (( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim)  ) * ( (vecSlope*((gridID[0]*gridXDim)) + vecIntercept) - (gridID[1]*gridYDim + gridYDim) ) ) < 0 ) {

					gridID[0] = gridID[0] - 1;
					gridID[1] = gridID[1]; 

					if (landMask[gridID[0]][gridID[1]] == 1 ){

						//simple calculation for uniform nonrotated grid 
						endPoint[0] = 2*(gridID[0]*gridXDim+gridXDim)-endPoint[0];  
						startPoint[0] = (gridID[0]*gridXDim + gridXDim);  
						startPoint[1] = (vecSlope*((gridID[0]*gridXDim + gridXDim)) + vecIntercept); //HERE'S WHERE IS MESSED UP

						moveType = 2; 
					}
				}

				if (moveType ==1 ) moveType = 0; //reset to 0 so will continue loop, but if it's set to 2 (has encountered land), then will break out

			} // end while (boxIsWater)

		} //end check on land
	}



	/**This function only works for uniform grids that have been rotated and origin set to zero -- it curvelinear, will need to devise mapping methods */
	public Int3D getXYGridID(Coordinate coord){
		Int3D loc = new Int3D(); 
		loc.x = (int) (coord.x/(double) gridXDim );
		loc.y = (int) (coord.y/(double) gridYDim );
		return loc; 
	}

	/**This function only works for uniform grids that have been rotated and origin set to zero -- it curvelinear, will need to devise mapping methods */
	public int[] getXYGridID(float[] coord){
		int[] loc = new int[2]; 
		loc[0] = (int) (coord[0]/(double) gridXDim );
		loc[1] = (int) (coord[1]/(double) gridYDim );
		return loc; 
	}

	/**
	 * Returns the angle of the vector from p0 to p1,
	 * relative to the positive X-axis.
	 * The angle is normalized to be in the range [ -Pi, Pi ].
	 *
	 * @return the normalized angle (in radians) that p0-p1 makes with the positive x-axis.
	 */

	private static double angle(Coordinate p0, Coordinate p1) {
		return Math.atan2(p1.y - p0.y, p1.x - p0.x);
	}



	public void printVector(Vector3D vec){
		System.out.println("vector: (" + vec.getX() + ", " + vec.getY() + ", " + vec.getZ() + ")"); 
	}

}
