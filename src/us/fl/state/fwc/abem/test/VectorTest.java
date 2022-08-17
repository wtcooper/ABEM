package us.fl.state.fwc.abem.test;

import org.apache.commons.math.geometry.Vector3D;

import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;


public class VectorTest {

	public double gridXDim = 500;
	public double gridYDim = 500; 

	public static void main(String[] args) {
		VectorTest test = new VectorTest();
		//test.rotateVector();
		test.vectorBounce();
		//test.vectorReflect();
		//test.test(); 
		//test.test2();
	}


	public void test(){

		Coordinate startPoint = new Coordinate(200, -100); 
		Coordinate endPoint = new Coordinate(-startPoint.x, -startPoint.y);
		Coordinate intersection = new Coordinate(0, 0);
		Coordinate wallStart = new Coordinate(-1,0); 
		Coordinate wallEnd = new Coordinate(1,0); 

		double dist = intersection.distance(endPoint); 

		//double ref_angle = 0;
		double ref_angle = angle(wallStart, wallEnd);
		System.out.println("ref angle: " + ref_angle); 
		// Determine the angle of incidence

		double vec_angle = angle(intersection, startPoint);
		System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 

		double wall_angle = angle(intersection, wallStart);
		System.out.println("wall angle: " + Math.toDegrees(wall_angle)); 
		double i_angle = vec_angle-wall_angle;

		System.out.println("incidence angle: " + Math.toDegrees(i_angle)); 

		// Subtract from the reference angle to get the angle of reflection

		double r_angle = ref_angle-i_angle;
		System.out.println("reflection angle: " + Math.toDegrees(r_angle)); 

		// Decompose the differential into x and y components.

		double xp1 = Math.cos(r_angle) * dist;
		double yp1 = Math.sin(r_angle) * dist;

		// Update the position of the particle

		double output1 = intersection.x + xp1;
		double output2 = intersection.y + yp1;
		System.out.println("new coordinate: (" + output1 + ", " + output2 + ")"); 


	}

	public void test2(){
		Coordinate startPoint = new Coordinate(5, 5); 
		Coordinate endPoint = new Coordinate(0, 4.9);
		
		double vec_angle = angle(startPoint,endPoint);
		System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 


	}

	public void vectorReflect(){
		Vector3D vec = new Vector3D(3,3,0);
		Vector3D floor = new Vector3D(1,0,0); 

		//new_vector=old_vector-wall_normal*wall_normal.dot( old_vector )*2;
		Vector3D bounce = vec.subtract(floor.scalarMultiply(Vector3D.dotProduct(vec, floor)*2));
		printVector(bounce); 

		double dotProduct = (vec.getX()*floor.getX() + vec.getY()*floor.getY())* 2; 

		Vector3D bounce2 = new Vector3D( vec.getX()- (floor.getX()*dotProduct), vec.getY()-(floor.getY()*dotProduct), 0); 
		printVector(bounce2); 

	}

	
	public void vectorBounce(){

		Coordinate startPoint = new Coordinate(750,750); 
		Coordinate endPoint = new Coordinate(400, 1200);
		Coordinate finalPoint = new Coordinate(endPoint.x, endPoint.y); 
		int[][] landMask = { //NOTE: this is row-major order [y][x], so need to call appropriately below 
				{1, 1, 1},
				{1, 0, 1},
				{1, 1, 1}
		};

		Coordinate pointOfIntercept = new Coordinate(startPoint.x, startPoint.y);
		double dist = 0; 

		int counter = 0; 
		//if the end point is on land
		while (landMask[(int) Math.round(getXYGridID(finalPoint).x)][(int) Math.round(getXYGridID(finalPoint).y)] == 1 ){
			
			if (counter>0){
				endPoint.x = finalPoint.x;
				endPoint.y = finalPoint.y;
				startPoint.x = pointOfIntercept.x;
				startPoint.y = pointOfIntercept.y; 
			}
			
			
			System.out.println("Have encountered land, entering the bounce house");

			double vecSlope = (endPoint.y-startPoint.y)/(endPoint.x-startPoint.x);
			double vecIntercept = (startPoint.y)-(vecSlope*startPoint.x);
			//PointLoc currentGridID = getXYGridID(startPoint); 
			//PointLoc nextGridID = new PointLoc();
			Coordinate currentGridID = getXYGridID(startPoint);
			Coordinate nextGridID = new Coordinate(); 

			//boolean boxIsWater = true;
			int moveType = 0;  //0=still in water and haven't moved; 1=still in water but have moved; 2=have moved and on land
			//follow boxes until find one that is land
			while (moveType == 0){
				// check which box it passes through
				
				//check N boundary
				if ( ((endPoint.y-startPoint.y) > 0) && ((( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim+gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim+gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim) ) ) < 0) ) {
					
					nextGridID.y = currentGridID.y + 1;
					nextGridID.x = currentGridID.x;


					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){
						System.out.println("Am moving north...land aho!");

						finalPoint.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) + 
							Math.cos(0f-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0f)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );
						finalPoint.y = (nextGridID.y*gridYDim) + 
							Math.sin(0f-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0f)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );

						moveType = 2;
						
						//test output
						pointOfIntercept.y = (nextGridID.y*gridYDim);
						pointOfIntercept.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope); 
						dist = Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );

					}
					else {
						System.out.println("Am moving north...alas am wallowing in water");
						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}
					
				}

				
				
				//check E boundary
				if ( (moveType != 1) && ((endPoint.x-startPoint.x) > 0) &&  (( (vecSlope*((currentGridID.x*gridXDim+gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim) ) ) < 0 ) {


					
					nextGridID.x = currentGridID.x + 1;
					nextGridID.y = currentGridID.y; 

					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){

						System.out.println("Am moving east...land aho!");

						finalPoint.x = (nextGridID.x*gridXDim) + 
							Math.cos((Math.PI/2f)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2f))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
						finalPoint.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) + 
							Math.sin((Math.PI/2f)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2f))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
						
						moveType = 2; 
						
						pointOfIntercept.x = (nextGridID.x*gridXDim);  
						pointOfIntercept.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept);
						dist = Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
						
					}
					else {
						System.out.println("Am moving east...alas am wallowing in water");

						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}

				}

				
				
				//check S boundary
				if ( (moveType != 1) && ((endPoint.y-startPoint.y) < 0) &&  (( (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim) ) ) < 0 ) {

					nextGridID.y = currentGridID.y - 1;
					nextGridID.x = currentGridID.x; 

					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){

						System.out.println("Am moving south...land aho!");

						finalPoint.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) + 
							Math.cos(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );
						finalPoint.y = (nextGridID.y*gridYDim + gridYDim) + 
							Math.sin(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );

						moveType = 2; 

						pointOfIntercept.y = (nextGridID.y*gridYDim + gridYDim); 
						pointOfIntercept.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope); 
						dist = Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );
					}
					else {

						System.out.println("Am moving south...alas am wallowing in water");

						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}
				}

				
				
				//check W boundary
				if ( (moveType != 1) && ((endPoint.x-startPoint.x) < 0) &&  (( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim) ) ) < 0 ) {

					nextGridID.x = currentGridID.x - 1;
					nextGridID.y = currentGridID.y; 
					
					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){

						System.out.println("Am moving west...land aho!");

						finalPoint.x = (nextGridID.x*gridXDim + gridXDim) + 
							Math.cos((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );
						finalPoint.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) + 
							Math.sin((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );

						moveType = 2; 

						pointOfIntercept.x = (nextGridID.x*gridXDim + gridXDim);  
						pointOfIntercept.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept);
						dist = Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );
					}
					else {
						System.out.println("Am moving west...alas am wallowing in water");

						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}
				}
				
				if (moveType ==1 ) moveType = 0; //reset to 0 so will continue loop, but if it's set to 2 (has encountered land), then will break out
				
			} // end while (boxIsWater)

			System.out.println("point of intersection: (" + pointOfIntercept.x + ", " + pointOfIntercept.y + ")");
			System.out.println("distance of bounce: " + dist); 
			System.out.println("final location after bounce: (" + finalPoint.x + ", " + finalPoint.y + ")");
			if (landMask[(int) Math.round(getXYGridID(finalPoint).x)][(int) Math.round(getXYGridID(finalPoint).y)] == 0) System.out.println("i've ended up on water!!!");
			else if (landMask[(int) Math.round(getXYGridID(finalPoint).x)][(int) Math.round(getXYGridID(finalPoint).y)] == 1) System.out.println("i've ended up on land!!!");

			counter++; 
			System.out.println(); 
			
		} //end check on land

	}

	
	
	
	
	
	

	public void vectorBounceDebug(){

		Coordinate startPoint = new Coordinate(750,750); 
		Coordinate endPoint = new Coordinate(700, 900);
		Coordinate finalPoint = new Coordinate(); 
		
		int[][] landMask = { //NOTE: this is row-major order [y][x], so need to call appropriately below 
				{1, 1, 1},
				{1, 0, 1},
				{1, 1, 1}
		};

		Coordinate pointOfIntercept = new Coordinate(startPoint.x, startPoint.y);
		double dist = 0; 
		
		//if the end point is on land
		if (landMask[(int) Math.round(getXYGridID(endPoint).x)][(int) Math.round(getXYGridID(endPoint).y)] == 1 ){
			
			System.out.println("Have encountered land, entering the bounce house");

			double vecSlope = (endPoint.y-startPoint.y)/(endPoint.x-startPoint.x);
			double vecIntercept = (startPoint.y)-(vecSlope*startPoint.x);
			//PointLoc currentGridID = getXYGridID(startPoint); 
			//PointLoc nextGridID = new PointLoc();
			Coordinate currentGridID = getXYGridID(startPoint);
			Coordinate nextGridID = new Coordinate(); 

			//boolean boxIsWater = true;
			int moveType = 0;  //0=still in water and haven't moved; 1=still in water but have moved; 2=have moved and on land
			//follow boxes until find one that is land
			while (moveType == 0){
				// check which box it passes through

//				new PointLoc(currentGrid.x*gridXDim, currentGrid.y*gridYDim+gridYDim),//UPPER LEFT POINT
//				new PointLoc(currentGrid.x*gridXDim+gridYDim, currentGrid.y*gridYDim+gridYDim), //UPPER RIGHT POINT
//				new PointLoc(currentGrid.x*gridXDim+gridYDim, currentGrid.y*gridYDim),//UPPER RIGHT POINT
//				new PointLoc(currentGrid.x*gridXDim, currentGrid.y*gridYDim)  }; //LOWER LEFT POINT
				
				
				
				double dx1, dx2;
				
				//check N boundary
				dx1 =  (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim+gridYDim)  ;
				dx2 =  (vecSlope*((currentGridID.x*gridXDim+gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim) ; 

				if ( ((endPoint.y-startPoint.y) > 0) && (( dx1*dx2) < 0) ) {
					
					nextGridID.y = currentGridID.y + 1;
					nextGridID.x = currentGridID.x;

					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){
						System.out.println("Am moving north...land aho!");

						pointOfIntercept.y = (nextGridID.y*gridYDim);
						pointOfIntercept.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope); 
						dist = Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );

						Vector3D vec = new Vector3D(endPoint.x-startPoint.x, endPoint.y-startPoint.y, 0); 
						System.out.println("vector angle: " + Math.toDegrees(vec.getAlpha()));
						
						double distCheck = pointOfIntercept.distance(endPoint); 
						double ref_angle = 0;
						System.out.println("ref angle: " + Math.toDegrees(ref_angle)); 
						double vec_angle = angle(startPoint, pointOfIntercept); //angle(startPoint,endPoint);
						System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 

						double i_angle = vec_angle-ref_angle;
						System.out.println("incidence angle: " + Math.toDegrees(i_angle)); 
						double r_angle = ref_angle-i_angle;
						System.out.println("refelction angle: " + Math.toDegrees(r_angle)); 
						double xp1 = Math.cos(r_angle) * dist;
						double yp1 = Math.sin(r_angle) * dist;

						double output1 = pointOfIntercept.x + xp1;
						double output2 = pointOfIntercept.y + yp1;

						
						finalPoint.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) + 
							Math.cos(0f-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0f)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );
						finalPoint.y = (nextGridID.y*gridYDim) + 
							Math.sin(0f-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0f)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );

						moveType = 2;
						
						//test output

					}
					else {
						System.out.println("Am moving north...alas am wallowing in water");
						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}
					
				}

				
				
				//check E boundary
				dx1 = (vecSlope*((currentGridID.x*gridXDim+gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim); 
				dx2 = (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim) ; 
				
				if ( (moveType != 1) && ((endPoint.x-startPoint.x) > 0) &&  ((dx1*dx2) < 0) ) {


					
					nextGridID.x = currentGridID.x + 1;
					nextGridID.y = currentGridID.y; 

					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){

						System.out.println("Am moving east...land aho!");

						pointOfIntercept.x = (nextGridID.x*gridXDim);  
						pointOfIntercept.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept);
						dist = Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );

						Vector3D vec = new Vector3D(endPoint.x-startPoint.x, endPoint.y-startPoint.y, 0); 
						System.out.println("vector angle: " + Math.toDegrees(vec.getAlpha()));
						
						double distCheck = pointOfIntercept.distance(endPoint); 
						double ref_angle = Math.PI/2f;
						System.out.println("ref angle: " + Math.toDegrees(ref_angle)); 
						double vec_angle = angle(startPoint, pointOfIntercept); //angle(startPoint,endPoint);
						System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 

						double i_angle = vec_angle-ref_angle;
						System.out.println("incidence angle: " + Math.toDegrees(i_angle)); 
						double r_angle = ref_angle-i_angle;
						System.out.println("refelction angle: " + Math.toDegrees(r_angle)); 
						double xp1 = Math.cos(r_angle) * dist;
						double yp1 = Math.sin(r_angle) * dist;

						double output1 = pointOfIntercept.x + xp1;
						double output2 = pointOfIntercept.y + yp1;

						
						finalPoint.x = (nextGridID.x*gridXDim) + 
							Math.cos((Math.PI/2f)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2f))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
						finalPoint.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) + 
							Math.sin((Math.PI/2f)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2f))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
						
						moveType = 2; 
						
						
					}
					else {
						System.out.println("Am moving east...alas am wallowing in water");

						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}

				}

				
				
				//check S boundary
				dx1 = (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim);
				dx2 = (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim) ; 
				
				if ( (moveType != 1) && ((endPoint.y-startPoint.y) < 0) &&  (( dx1*dx2) < 0) ) {

					nextGridID.y = currentGridID.y - 1;
					nextGridID.x = currentGridID.x; 

					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){

						System.out.println("Am moving south...land aho!");

						pointOfIntercept.y = (nextGridID.y*gridYDim + gridYDim); 
						pointOfIntercept.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope); 
						dist = Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );

						Vector3D vec = new Vector3D(endPoint.x-startPoint.x, endPoint.y-startPoint.y, 0); 
						System.out.println("vector angle: " + Math.toDegrees(vec.getAlpha()));
						
						double distCheck = pointOfIntercept.distance(endPoint); 
						double ref_angle = 0; 
						System.out.println("ref angle: " + Math.toDegrees(ref_angle)); 
						double vec_angle = angle(startPoint, pointOfIntercept); //angle(startPoint,endPoint);
						System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 

						double i_angle = vec_angle-ref_angle;
						System.out.println("incidence angle: " + Math.toDegrees(i_angle)); 
						double r_angle = ref_angle-i_angle;
						System.out.println("refelction angle: " + Math.toDegrees(r_angle)); 
						double xp1 = Math.cos(r_angle) * dist;
						double yp1 = Math.sin(r_angle) * dist;

						double output1 = pointOfIntercept.x + xp1;
						double output2 = pointOfIntercept.y + yp1;

						
						finalPoint.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) + 
							Math.cos(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );
						finalPoint.y = (nextGridID.y*gridYDim + gridYDim) + 
							Math.sin(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );

						moveType = 2; 

					}
					else {

						System.out.println("Am moving south...alas am wallowing in water");

						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}
				}

				
				
				//check W boundary
				dx1 = (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim)  ;
				dx2 = (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim) ; 
				if ( (moveType != 1) && ((endPoint.x-startPoint.x) < 0) &&  (( dx1*dx2) < 0 )) {

					nextGridID.x = currentGridID.x - 1;
					nextGridID.y = currentGridID.y; 
					
					if (landMask[(int) Math.round(nextGridID.x)][(int) Math.round(nextGridID.y)] == 1 ){

						System.out.println("Am moving west...land aho!");

						pointOfIntercept.x = (nextGridID.x*gridXDim + gridXDim);  
						pointOfIntercept.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept);
						dist = Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );

						Vector3D vec = new Vector3D(endPoint.x-startPoint.x, endPoint.y-startPoint.y, 0); 
						System.out.println("vector angle: " + Math.toDegrees(vec.getAlpha()));
						
						double distCheck = pointOfIntercept.distance(endPoint); 
						double ref_angle = Math.PI/2f;
						System.out.println("ref angle: " + Math.toDegrees(ref_angle)); 
						double vec_angle = angle(startPoint, pointOfIntercept); //angle(startPoint,endPoint);
						System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 

						double i_angle = vec_angle-ref_angle;
						System.out.println("incidence angle: " + Math.toDegrees(i_angle)); 
						double r_angle = ref_angle-i_angle;
						System.out.println("refelction angle: " + Math.toDegrees(r_angle)); 
						double xp1 = Math.cos(r_angle) * dist;
						double yp1 = Math.sin(r_angle) * dist;

						double output1 = pointOfIntercept.x + xp1;
						double output2 = pointOfIntercept.y + yp1;

						finalPoint.x = (nextGridID.x*gridXDim + gridXDim) + 
							Math.cos((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );
						finalPoint.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) + 
							Math.sin((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );

						moveType = 2; 

					}
					else {
						System.out.println("Am moving west...alas am wallowing in water");

						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 

						moveType = 1; 
					}
				}
				
				if (moveType ==1 ) moveType = 0; //reset to 0 so will continue loop, but if it's set to 2 (has encountered land), then will break out
				
			} // end while (boxIsWater)

			System.out.println("point of intersection: (" + pointOfIntercept.x + ", " + pointOfIntercept.y + ")");
			System.out.println("distance of bounce: " + dist); 
			System.out.println("final location after bounce: (" + finalPoint.x + ", " + finalPoint.y + ")");

		} //end check on land

	}

	
	
	

	public void vectorBounceSlop(){

		/*	1) calcualte the equation for the vector line:
		 * 		a. get slope, m from the 2 points: m=(y2-y1)/(x2-x1)
		 * 		b. get where crosses origin: y1(or y2) = m*(x1 or x2) + b, so b=y1/(m*x1) 
		 * 	
		 * 2) using the equation, check which surrounding box faces it passes through:
		 * 		a. get end points of box face
		 * 		b. check if end points are split between the line, i.e., one is above and one is below the line
		 * 			- substitute the end point x into the vector line equation, and see if the y value is < or > the real value
		 * 			e.g., end points are (-2,1) and (2,1) and equation is y=1*x + 0
		 * 					substitute end point 1:		y=1*(-2) + 0 		-> 	y=-2			y is less than real y
		 * 					substitute end point 2:		y=1*(2) + 0		->		y=2			y is greater than real y
		 * 					** if calculate as dx1 and dx2, can then multiple together to get if + or - to test
		 * 					- therefore, since 1 is < and 1 is >, that means the line goes through the endpoints and therefore intersects
		 * 
		 * 	3) check the box it intersects with to see if it is land -- if so, check if that box is where end particle point is located
		 * 
		 * 	4)	if end point is in another box, keep on checking box faces to see where it passes through    
		 * 
		 * 	5) once have the box where the end point is located, then need to get the point of intersection
		 * 		a. Get the two equations for the lines into slope-intercept form. That is, have them in this form: y = mx + b. 
		 * 		b. Set the two equations for y equal to each other.
		 * 		c. Solve for x. This will be the x-coordinate for the point of intersection.
		 * 		d. Use this x-coordinate and plug it into either of the original equations for the lines and solve for y. This will be the y-coordinate of the point of intersection. 
		 * 
		 * 	6) After get the point of intersection, follow JKool's code below
		 */

		double[] origin = {0,0}; 
		Coordinate startPoint = new Coordinate(100,100); 
		Coordinate endPoint = new Coordinate(700, 900);
		int[][] landMask = { 
				{0, 0, 1},
				{0, 1, 1},
				{1, 1, 1}
		};

		//if the FINAL point is on land
		if (landMask[(int) Math.round(getXYGridID(endPoint).x)][(int) Math.round(getXYGridID(endPoint).y)] == 1 ){

			double vecSlope = (endPoint.y-startPoint.y)/(endPoint.x-startPoint.x);
			double vecIntercept = (startPoint.y)/(vecSlope*startPoint.x);
			Coordinate temp = getXYGridID(startPoint);
			Int3D currentGridID =  new Int3D();
			currentGridID.x = (int) temp.x;
			currentGridID.y = (int) temp.y; 
			Int3D nextGridID = new Int3D(); 

			boolean boxIsWater = true;
			//follow boxes until find one that is land
			while (boxIsWater){
				// check which box it passes through
				//nextGridID = getNextGridID(currentGridID, vecSlope, vecIntercept);

				
				
				
				//PointLoc[] cornerPoints = { 
				//		new PointLoc(currentGrid.x*gridXDim, currentGrid.y*gridYDim), 
				//		new PointLoc(currentGrid.x*gridXDim, currentGrid.y*gridYDim+gridYDim),
				//		new PointLoc(currentGrid.x*gridXDim+gridYDim, currentGrid.y*gridYDim+gridYDim),
				//		new PointLoc(currentGrid.x*gridXDim+gridYDim, currentGrid.y*gridYDim) };


				// ****************************************************
				// THIS FORMULATION BELOW IS WRONG -- I messed up and started with West facing boundary and not North
				// ****************************************************
				
				/*check N boundary
				 * first check if the point is moving northwards so that don't check both N and S boundary during any given step 
				 * -- this will avoid situations where have two potential box boundaries that are checked
				 */

				if ( ((endPoint.y-startPoint.y) > 0) && (( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim) ) ) < 0 ) {
					nextGridID.y = currentGridID.y + 1;
					nextGridID.x = currentGridID.x;

					if (landMask[nextGridID.x][nextGridID.y] == 1 ){
						boxIsWater = false; 

						endPoint.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) + 
							Math.cos(0-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );
						endPoint.y = (nextGridID.y*gridYDim) + 
							Math.sin(0-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );

					}
					else {
						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 
					}
					
				}

				//check E boundary
				if (((endPoint.x-startPoint.x) > 0) &&  (( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim) ) ) < 0 ) {
					nextGridID.x = currentGridID.x + 1;
					nextGridID.y = currentGridID.y; 

					if (landMask[nextGridID.x][nextGridID.y] == 1 ){
						boxIsWater = false; 

						endPoint.x = (nextGridID.x*gridXDim) + 
							Math.cos((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
						endPoint.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) + 
							Math.sin((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );

					}
					else {
						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 
					}

				}

				//check S boundary
				if (((endPoint.y-startPoint.y) < 0) &&  (( (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim + gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim) ) ) < 0 ) {
					nextGridID.y = currentGridID.y - 1;
					nextGridID.x = currentGridID.x; 

					if (landMask[nextGridID.x][nextGridID.y] == 1 ){
						boxIsWater = false; 

						endPoint.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) + 
							Math.cos(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );
						endPoint.y = (nextGridID.y*gridYDim + gridYDim) + 
							Math.sin(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
							Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );

					}
					else {
						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 
					}
				}

				//check W boundary
				if (((endPoint.x-startPoint.x) < 0) &&  (( (vecSlope*((currentGridID.x*gridXDim + gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim)  ) * ( (vecSlope*((currentGridID.x*gridXDim)) + vecIntercept) - (currentGridID.y*gridYDim) ) ) < 0 ) {
					nextGridID.x = currentGridID.x - 1;
					nextGridID.y = currentGridID.y; 
					
					if (landMask[nextGridID.x][nextGridID.y] == 1 ){
						boxIsWater = false; 

						endPoint.x = (nextGridID.x*gridXDim + gridXDim) + 
							Math.cos((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );
						endPoint.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) + 
							Math.sin((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
							Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );

					}
					else {
						currentGridID.x = nextGridID.x;
						currentGridID.y = nextGridID.y; 
					}
				}

				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				if (landMask[nextGridID.x][nextGridID.y] == 1 ){
					boxIsWater = false; 
					//should be able to inline remaining here


				}
				else {
					currentGridID.x = nextGridID.x;
					currentGridID.y = nextGridID.y; 
				}

			} // end while (boxIsWater)


			//get the point of intersection 

			//Coordinate pointOfIntercept = new Coordinate(); 
			//double ref_angle =0; 
			// is N Boundary
			if ( nextGridID.y == (currentGridID.y+1) ){ 
				// since we know the y-value where intersects, solve for x
				//pointOfIntercept.y = (nextGridID.y*gridYDim);
				//pointOfIntercept.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope); 

				endPoint.x = (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) + 
				Math.cos(0-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
				Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );
				endPoint.y = (nextGridID.y*gridYDim) + 
				Math.sin(0-(Math.atan2((nextGridID.y*gridYDim) - startPoint.y, (((nextGridID.y*gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
				Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim))*(endPoint.y-(nextGridID.y*gridYDim)) );
			}

			// is E Boundary
			else if( nextGridID.x == (currentGridID.x+1) ){ 
				//since we know the x-value where intersects, solve for y
				//pointOfIntercept.x = (nextGridID.x*gridXDim);  
				//pointOfIntercept.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept);
				//ref_angle=Math.PI/2;

				endPoint.x = (nextGridID.x*gridXDim) + 
				Math.cos((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2))) * 
				Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );
				endPoint.y = (vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) + 
				Math.sin((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim) - startPoint.x)-(Math.PI/2))) * 
				Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim))*(endPoint.x-(nextGridID.x*gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim)) + vecIntercept)) );

			}

			// is S Boundary
			else if (nextGridID.y == (currentGridID.y-1)) { 
				//pointOfIntercept.y = (nextGridID.y*gridYDim + gridYDim); 
				//pointOfIntercept.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope); 
				//ref_angle=0;

				endPoint.x = (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) + 
				Math.cos(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
				Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );
				endPoint.y = (nextGridID.y*gridYDim + gridYDim) + 
				Math.sin(0-(Math.atan2((nextGridID.y*gridYDim + gridYDim) - startPoint.y, (((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope) - startPoint.x)-0)) * 
				Math.sqrt( (endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope))*(endPoint.x-(((nextGridID.y*gridYDim + gridYDim) - vecIntercept)/vecSlope)) + (endPoint.y-(nextGridID.y*gridYDim + gridYDim))*(endPoint.y-(nextGridID.y*gridYDim + gridYDim)) );

			}

			// is W Boundary
			else if (nextGridID.x == (currentGridID.x-1)) { 
				//pointOfIntercept.x = (nextGridID.x*gridXDim + gridXDim);  
				//pointOfIntercept.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept);
				//ref_angle=Math.PI/2;

				endPoint.x = (nextGridID.x*gridXDim + gridXDim) + 
				Math.cos((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
				Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );
				endPoint.y = (vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) + 
				Math.sin((Math.PI/2)-(Math.atan2((vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept) - startPoint.y, (nextGridID.x*gridXDim + gridXDim) - startPoint.x)-(Math.PI/2))) * 
				Math.sqrt( (endPoint.x-(nextGridID.x*gridXDim + gridXDim))*(endPoint.x-(nextGridID.x*gridXDim + gridXDim)) + (endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept))*(endPoint.y-(vecSlope*((nextGridID.x*gridXDim + gridXDim)) + vecIntercept)) );

			}


			//calculate distance from end point to point of intersection
			//double dist = Math.sqrt( (endPoint.x-pointOfIntercept.x)*(endPoint.x-pointOfIntercept.x) + (endPoint.y-pointOfIntercept.y)*(endPoint.y-pointOfIntercept.y) ); 

			//now follow JKool's code
			//double vec_angle = Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x); //angle(startPoint,intersection);
			//System.out.println("vec angle: " + Math.toDegrees(vec_angle)); 

			//double wall_angle = angle(wallStart, intersection);
			//System.out.println("wall angle: " + Math.toDegrees(wall_angle)); 
			//double i_angle = Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x)-ref_angle;

			//System.out.println("incidence angle: " + Math.toDegrees(i_angle)); 

			// Subtract from the reference angle to get the angle of reflection

			//double r_angle = ref_angle-(Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x)-ref_angle);
			//System.out.println("reflection angle: " + Math.toDegrees(r_angle)); 

			// Decompose the differential into x and y components.

			// angle function: Math.atan2(p1.y - p0.y, p1.x - p0.x);
			//double xp1 = Math.cos(ref_angle-(Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x)-ref_angle)) * 
			//Math.sqrt( (endPoint.x-pointOfIntercept.x)*(endPoint.x-pointOfIntercept.x) + (endPoint.y-pointOfIntercept.y)*(endPoint.y-pointOfIntercept.y) );
			//double yp1 = Math.sin(ref_angle-(Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x)-ref_angle)) * 
			//Math.sqrt( (endPoint.x-pointOfIntercept.x)*(endPoint.x-pointOfIntercept.x) + (endPoint.y-pointOfIntercept.y)*(endPoint.y-pointOfIntercept.y) );

			// Update the position of the particle

			//endPoint.x = pointOfIntercept.x + 
			//Math.cos(ref_angle-(Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x)-ref_angle)) * 
			//Math.sqrt( (endPoint.x-pointOfIntercept.x)*(endPoint.x-pointOfIntercept.x) + (endPoint.y-pointOfIntercept.y)*(endPoint.y-pointOfIntercept.y) );
			//endPoint.y = pointOfIntercept.y + 
			//Math.sin(ref_angle-(Math.atan2(pointOfIntercept.y - startPoint.y, pointOfIntercept.x - startPoint.x)-ref_angle)) * 
			//Math.sqrt( (endPoint.x-pointOfIntercept.x)*(endPoint.x-pointOfIntercept.x) + (endPoint.y-pointOfIntercept.y)*(endPoint.y-pointOfIntercept.y) );



		}

	}


	public Int3D getNextGridID(Int3D currentGrid, double vecSlope, double vecIntercept){
		Int3D nextGrid = new Int3D(); 
		//check N box
		//get 4 corner points of current grid box
		Int3D[] cornerPoints = { new Int3D(0,0),new Int3D(0,0),new Int3D(0,0),new Int3D(0,0)};  
//				new PointLoc((int)currentGrid.x*gridXDim, (int)currentGrid.y*gridYDim+gridYDim),//UPPER LEFT POINT
//				new PointLoc(currentGrid.x*gridXDim+gridYDim, currentGrid.y*gridYDim+gridYDim), //UPPER RIGHT POINT
//				new PointLoc(currentGrid.x*gridXDim+gridYDim, currentGrid.y*gridYDim),//UPPER RIGHT POINT
//				new PointLoc(currentGrid.x*gridXDim, currentGrid.y*gridYDim)  }; //LOWER LEFT POINT

		/*
		 * 		b. check if end points are split between the line, i.e., one is above and one is below the line
		 * 			- substitute the end point x into the vector line equation, and see if the y value is < or > the real value
		 * 			e.g., end points are (-2,1) and (2,1) and equation is y=1*x + 0
		 * 					substitute end point 1:		y=1*(-2) + 0 		-> 	y=-2			y is less than real y
		 * 					substitute end point 2:		y=1*(2) + 0		->		y=2			y is greater than real y
		 * 					** if calculate as dx1 and dx2, can then multiple together to get if + or - to test
		 * 					- therefore, since 1 is < and 1 is >, that means the line goes through the endpoints and therefore intersects
		 */

		//check N boundary
		if ( (( (vecSlope*(cornerPoints[0].x) + vecIntercept) - cornerPoints[0].y  ) * ( (vecSlope*(cornerPoints[0].x) + vecIntercept) - cornerPoints[1].y ) ) < 0 ) {
			nextGrid.y = currentGrid.y + 1;
			nextGrid.x = currentGrid.x;
			return nextGrid;
		}

		//check E boundary
		if ( (( (vecSlope*(cornerPoints[1].x) + vecIntercept) - cornerPoints[1].y  ) * ( (vecSlope*(cornerPoints[2].x) + vecIntercept) - cornerPoints[2].y ) ) < 0 ) {
			nextGrid.x = currentGrid.x + 1;
			nextGrid.y = currentGrid.y; 
			return nextGrid;
		}

		//check S boundary
		if ( (( (vecSlope*(cornerPoints[2].x) + vecIntercept) - cornerPoints[2].y  ) * ( (vecSlope*(cornerPoints[3].x) + vecIntercept) - cornerPoints[3].y ) ) < 0 ) {
			nextGrid.y = currentGrid.y - 1;
			nextGrid.x = currentGrid.x; 
			return nextGrid;
		}

		//check W boundary
		if ( (( (vecSlope*(cornerPoints[3].x) + vecIntercept) - cornerPoints[3].y  ) * ( (vecSlope*(cornerPoints[0].x) + vecIntercept) - cornerPoints[0].y ) ) < 0 ) {
			nextGrid.x = currentGrid.x - 1;
			nextGrid.y = currentGrid.y; 
			return nextGrid;
		}

		return nextGrid; 
	}

	/**This function only works for uniform grids that have been rotated and origin set to zero -- it curvelinear, will need to devise mapping methods */
	public Coordinate getXYGridID(Coordinate coord){
		Coordinate loc = new Coordinate(); 
		loc.x = (int) (coord.x/(double) gridXDim );
		loc.y = (int) (coord.y/(double) gridYDim );
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

	public void rotateVector(){

		double x = 1;
		double y = 1; 
		double rotation = 45; 

		Vector3D vector1 = new Vector3D(x,0,0);
		Vector3D vector2 = new Vector3D(0,y,0);
		Vector3D vector3 = vector1.add(vector2); 
		//NOTE: getNorm() is the new vector magnitude from the addition, while getAlpha is the angle (in radians)
		System.out.println(vector3.getX() + "\t" + vector3.getY() + "\t" + vector3.getZ() + "\t" + vector3.getNorm() + "\t" + Math.toDegrees(vector3.getAlpha()));

		double angle = Math.toDegrees(Math.atan(y/x)) - rotation ; //getAlpha(); 
		double hyp = vector3.getNorm();  //vector3.getNorm(); 

		double u = Math.cos(Math.toRadians(angle))* hyp; //x
		double v= Math.sin(Math.toRadians(angle)) * hyp; //y
		System.out.println(u + "\t" + v); 

	}
}
