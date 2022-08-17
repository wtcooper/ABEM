package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import org.apache.commons.math.geometry.Vector3D;

import com.vividsolutions.jts.geom.Coordinate;

public class EFDCRotation {

	DecimalFormat twoDForm = new DecimalFormat("#.###");

	double[] origin = {319731.373767, 3032472.653897}; 
	double rotationAngle = 0.21998025172381408; // this is in radians; degrees=12.60394; //official EFDC rotation used
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EFDCRotation main = new EFDCRotation();
		main.checkCudaRotation();
		//main.rotate2DPoint();
	}


	public void checkCudaRotation(){
		Coordinate coord = new Coordinate(origin[0]+1, origin[1]+1, 0);

		System.out.println(coord.x + "\t" + coord.y);

		rotateToFakeSpace(coord);
		
		System.out.println(coord.x + "\t" + coord.y);
		
		rotateToRealSpace(coord);
		
		System.out.println(coord.x + "\t" + coord.y);
		
	}
	
	
	public void rotateToFakeSpace(Coordinate pos){
		double angle;
		if (pos.x < origin[0]) angle = Math.atan( (pos.y-origin[1])/ (pos.x-origin[0]) ) + 3.141592653589793f - rotationAngle; // here, need to adjust if the point is in quadrant 2 by adding Pi (180 degrees)
		else angle = Math.atan( (pos.y-origin[1])/ (pos.x-origin[0]) )  - rotationAngle;

		pos.x = Math.cos(angle) * Math.sqrt((pos.x-origin[0])*(pos.x-origin[0]) + (pos.y-origin[1])*(pos.y-origin[1])); //  + origin[0];
		pos.y = Math.sin(angle) * Math.sqrt((pos.x-origin[0])*(pos.x-origin[0]) + (pos.y-origin[1])*(pos.y-origin[1])); // + origin[1];
	}



	/* Converts a position from the rotated space back to real world space
	 * Here: pos.x = cos(angle+rotation)*distance + origin
	 * 			pos.y = sin(angle+rotation)*distance + origin
	 * */
	public void rotateToRealSpace(Coordinate pos){
		pos.x = Math.cos(Math.atan( (pos.y-origin[1]) / (pos.x-origin[0]) ) + rotationAngle) * Math.sqrt((pos.x-origin[0])*(pos.x-origin[0]) + (pos.y-origin[1])*(pos.y-origin[1]))  + origin[0];
		pos.y = Math.sin(Math.atan( (pos.y-origin[1]) / (pos.x-origin[0]) ) + rotationAngle) * Math.sqrt((pos.x-origin[0])*(pos.x-origin[0]) + (pos.y-origin[1])*(pos.y-origin[1])) + origin[1];
	}

	
	
	
	public void rotateVector(){

	//	double[] x = {-0.1331, -.2712, -.2339};
	//	double[] y = {0.0, 0.0, -.04378}; 
		double[] x = {0.02891};
		double[] y = {-0.06441}; 
		//double rotationDegs = 12.60394; //official EFDC rotation used

		for (int i=0; i< x.length; i++){
		Vector3D vector1 = new Vector3D(x[i],0,0);
		Vector3D vector2 = new Vector3D(0,y[i],0);
		Vector3D vector3 = vector1.add(vector2); 
		
		double magnitude = vector3.getNorm();
		double angle = vector3.getAlpha();
		System.out.print("pre-rotation:\t");
		System.out.println("angle: " + twoDForm.format(Math.toDegrees(angle)) ); 

		double rotation = Math.toRadians(12.604);//.60394); 
		angle += rotation;


		double u = Math.cos(angle)* magnitude; //x
		double v= Math.sin(angle) * magnitude; //y

		System.out.print("post-rotation:\t");
		System.out.println("angle: " + twoDForm.format(Math.toDegrees(angle)) ); 
		System.out.println("L " + (i+2) + ": " +twoDForm.format(u) + "\t\t" + twoDForm.format(v) + "\t\tmagnitude: " +twoDForm.format(vector3.getNorm())); 
		System.out.println();
		}		
		
/*		Vector3D vector1 = new Vector3D(x,0,0);
		Vector3D vector2 = new Vector3D(0,y,0);
		Vector3D vector3 = vector1.add(vector2); 
		//NOTE: getNorm() is the new vector magnitude from the addition, while getAlpha is the angle (in radians)
		System.out.println(vector3.getX() + "\t" + vector3.getY() + "\t" + vector3.getZ() + "\t" + vector3.getNorm() + "\t" + Math.toDegrees(vector3.getAlpha()));

		double angle = Math.toDegrees(Math.atan(y/x)) + rotationDegs ; //getAlpha(); 
		double hyp = vector3.getNorm();  //vector3.getNorm(); 
		
		double u = Math.cos(Math.toRadians(angle))* hyp; //x
		double v= Math.sin(Math.toRadians(angle)) * hyp; //y
		System.out.println(u + "\t" + v); 
	*/	
	}
	
	public void rotate2DPoint(){

		//Parameters to input
		//20	3	328784.2	3035521.5
		//61	129	335043	3101476.7
		//21 92 319561.8	3079058.3

		
		double x = 319561.8;
		double y = 3079058.3; 
		int gridI=21;
		int gridJ=92;

		double originX = 319731.373767; 
		double originY=3032472.653897; 
		double rotationDegs = 12.60394; //official EFDC rotation used
		
		double expXDiff = (gridI-1)*500;
		double expYDiff = (gridJ-1)*500; 
		
		double rotationRads = Math.toRadians(rotationDegs); 
		System.out.println("angle of " + rotationDegs + " degrees is " + rotationRads + " radians"); 
		
		//first, get distance and anble
		double distance = Math.sqrt((x-originX)*(x-originX) + (y-originY)*(y-originY)); 
		double angle = 0; 
		if (x < originX) angle = Math.atan( (y-originY)/ (x-originX) ) + Math.PI - rotationRads; // here, need to adjust if the point to rotate is in quadrant 2 by adding Pi (180 degrees)
		else angle = Math.atan( (y-originY)/ (x-originX) ) - rotationRads; 
		
		System.out.println("distance: " + distance + "\tangle: " + Math.toDegrees(angle));
		
		double xRotated = Math.cos(angle)*distance + originX;
		double yRotated = Math.sin(angle)*distance + originY; 

		double xDiff = (xRotated-originX) - expXDiff;
		double yDiff = (yRotated-originY) - expYDiff; 
		
		
		System.out.println("Original x/y: (" + x + "," + y + ")\tNew x/y: (" + twoDForm.format(xRotated) + "," + twoDForm.format(yRotated)+ ")");
		System.out.println("\nx diff: " + twoDForm.format(xDiff) + "m\ty diff: " + twoDForm.format(yDiff) + "m"); 

		
		//de-rotate
		angle = Math.atan( (yRotated-originY) / (xRotated-originX) ) + rotationRads;
		xRotated = Math.cos(angle)*distance + originX;
		yRotated = Math.sin(angle)*distance + originY; 
		
		System.out.println("Original x/y: (" + x + "," + y + ")\tNew x/y: (" + twoDForm.format(xRotated) + "," + twoDForm.format(yRotated) + ")");
	}

	
	public void testRotatePoint(){
		PrintWriter outFile= null; //need Time 	Speed	Direction, in meters per second
		try { 
			outFile= new PrintWriter(new FileWriter("dataTest/rotationError.txt", true));
		} catch (IOException e) {e.printStackTrace();}

		File file = new File("dataTest/EFDC_id_and_cornerPt2.txt"); 
		BufferedReader reader = null; 
		int[] gridI = new int[5750]; 
		int[] gridJ = new int[5750]; 
		double[] x= new double[5750]; 
		double[] y = new double[5750]; 
		double originX = 319731.373767; 
		double originY=3032472.653897; 
		double rotationDegs = 12.60394; //official EFDC rotation used
		
		
		try {
			reader = new BufferedReader(new FileReader(file));

			int counter = 0; 
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); 

					gridI[counter] = Integer.parseInt(tokens[0]);
					gridJ[counter]= Integer.parseInt(tokens[1]);
					x[counter] = Double.parseDouble(tokens[2]); 
					y[counter] = Double.parseDouble(tokens[3]); 

					counter++; 
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // end finally
	
		for (int i=0; i<x.length; i++){
			double expXDiff = (gridI[i]-1)*500;
			double expYDiff = (gridJ[i]-1)*500; 
			
			double rotationRads = Math.toRadians(rotationDegs); 
//			System.out.println("angle of " + rotationDegs + " degrees is " + rotationRads + " radians"); 
			
			//first, get distance and anble
			double distance = Math.sqrt((x[i]-originX)*(x[i]-originX) + (y[i]-originY)*(y[i]-originY)); 

			double angle = 0; 
			if (x[i] < originX) angle = Math.atan( (y[i]-originY)/ (x[i]-originX) ) + Math.PI - rotationRads; // here, need to adjust if the point to rotate is in quadrant 2 by adding Pi (180 degrees)
			else angle = Math.atan( (y[i]-originY)/ (x[i]-originX) ) - rotationRads; 

			
			double xRotated = Math.cos(angle)*distance + originX;
			double yRotated = Math.sin(angle)*distance + originY; 

			double xDiff = (xRotated-originX) - expXDiff;
			double yDiff = (yRotated-originY) - expYDiff; 
			
			double reAngle = Math.atan( (yRotated-originY) / (xRotated-originX) ) + rotationRads;
			double reXRotated = Math.cos(reAngle)*distance + originX;
			double reYRotated = Math.sin(reAngle)*distance + originY; 

			double xDiff2 = reXRotated - x[i];
			double yDiff2 = reYRotated - y[i]; 
			
			outFile.println(gridI[i] + "\t" + gridJ[i] + "\tx diff:\t" + twoDForm.format(xDiff) + "\ty diff:\t" + twoDForm.format(yDiff) + "\tx rerotate diff:\t" + twoDForm.format(xDiff2) + "\ty rerotate diff:\t" + twoDForm.format(yDiff2)); 
		}
		
	
		
		outFile.close(); 
		
	}

}
