package us.fl.state.fwc.abem.hydro.ch3d;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import us.fl.state.fwc.abem.hydro.efdc.EFDCCell;
import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class CH3D_Corners2Shp {


	String directory = "C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\";
	String filename = directory + "corners.inp"; 
//	String filename = "C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\CH3D_TB_Corners_oriented.txt";

	
	String outFile = directory + "CH3D_TB.shp";

	String EPSG_code = "UTM17N";

	ArrayList<Coordinate[]> featureCoords = new ArrayList<Coordinate[]>(); 
	ArrayList<Object[]> featureVals = new ArrayList<Object[]>(); 


	public void run() {

		try {

			int L = 2; 
			//first use a Scanner to get each line
			File inFile = new File(filename);  
			Scanner scanner = new Scanner(inFile);
			int counter = 0; 
			while ( scanner.hasNextLine() ){
				Scanner lineScanner = new Scanner(scanner.nextLine()); 
				if ( counter>1 && lineScanner.hasNext() ){


					int I = lineScanner.nextInt() - 2; //EFDC pads with 2
					int J = lineScanner.nextInt() -2; //EFDC pads with 2
					double x1 = lineScanner.nextDouble(); 
					double y1 = lineScanner.nextDouble(); 
					double x2 = lineScanner.nextDouble(); 
					double y2 = lineScanner.nextDouble(); 
					double x3 = lineScanner.nextDouble(); 
					double y3 = lineScanner.nextDouble(); 
					double x4 = lineScanner.nextDouble(); 
					double y4 = lineScanner.nextDouble(); 

					Int2D index = new Int2D(I, J); 
					Coordinate[] coords = new Coordinate[5];
					//need to initialize the coordinates
					for (int i=0; i<coords.length; i++){
						coords[i] = new Coordinate(0,0,0); 
					}

					coords[0].x = x1;
					coords[0].y = y1; 
					coords[1].x = x2;
					coords[1].y = y2; 
					coords[2].x = x3;
					coords[2].y = y3; 
					coords[3].x = x4;
					coords[3].y = y4; 
					coords[4].x = x1; // need to close it
					coords[4].y = y1; // need to close it

					Object[] vals = {I, J};
					featureCoords.add(coords);
					featureVals.add(vals);


				}// end lineScanner 
				lineScanner.close();
				counter++; 
			} // end file scanner
			scanner.close();





		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		
		
		
		

		
		//Set up shapefile properties
		Class[] attributeTypes = new Class[3]; // {Polygon.class, Double.class}; //new Class[2];
		String[] attributeNames = new String[3]; //{"the_geom", varName}; //new String[2];
		attributeTypes[0] = Polygon.class;
		attributeNames[0] = "the_geom";
		attributeTypes[1] = Integer.class;
		attributeNames[1] = "I";
		attributeTypes[2] = Integer.class;
		attributeNames[2] = "J";


		DeleteFiles delete = new DeleteFiles();
		delete.deleteByPrefix(outFile);
		SimpleShapefile shape = new SimpleShapefile(outFile); 
		shape.createShapefile(attributeTypes, attributeNames, 
				featureCoords, featureVals, EPSG_code);  

	}




	public void readCornersFile(){

	}


	public static void main(String[] args) {
		CH3D_Corners2Shp c = new CH3D_Corners2Shp();
		c.run();
	}
}
