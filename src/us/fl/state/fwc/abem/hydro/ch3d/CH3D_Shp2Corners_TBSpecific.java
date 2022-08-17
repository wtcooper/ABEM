package us.fl.state.fwc.abem.hydro.ch3d;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class CH3D_Shp2Corners_TBSpecific {


	String shpFilename ="C:\\work\\data\\GISData\\BaseMaps\\HydroModels\\CH3D_TB_edit_UTM17.shp"; 
	String outFile = "C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\CH3D_TB_Corners_oriented.txt";

	String EPSG_code = "UTM17N";

	PrintWriter out= null; 

	GeometryFactory gf =  new GeometryFactory();


	HashMap<Int2D, Coordinate[]> featureMap = new HashMap<Int2D, Coordinate[]>(); 
	HashMap<Int2D, Coordinate[]> stretchedMap = new HashMap<Int2D, Coordinate[]>(); 

	int iMax = 0;
	int jMax = 0;

	int[] head = 	{1,8,14,20,25,30,34,38,41,44,46,48};
	int[] tail = 		{7,13,19,24,29,33,37,40,43,45,47,49};

	Coordinate[][] corners = null;



	public void run() throws IOException {


		//instantiate the out file printer
		new File(outFile).delete();
		out= new PrintWriter(new FileWriter(outFile, true));


		
		
		/**
		 * Open up the shapefile, iterate through features, format the coordinates so that they
		 * are in the correct orientation, where on a structured grid without rotation, the lower left
		 * coordinate is index zero and printed out first, followed by printing of the corners out 
		 * int counter-clockwise rotation  
		 */
		
		SimpleShapefile shp = new SimpleShapefile(shpFilename);
		FeatureSource fSource = shp.getFeatureSource();

		FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
		features.accepts( new FeatureVisitor(){
			int counter = 0; 

			public void visit(Feature feature) {
				SimpleFeature simpleFeature = (SimpleFeature) feature;
				Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
				Coordinate[] coords = geom.getCoordinates().clone(); 
				Double iIndex = (Double) simpleFeature.getAttribute("IGRID");
				Double jIndex = (Double) simpleFeature.getAttribute("JGRID");

				if (iIndex.intValue() > iMax) iMax = iIndex.intValue();
				if (jIndex.intValue() > jMax) jMax = jIndex.intValue();


				Int2D index = new Int2D(iIndex.intValue(), jIndex.intValue());

				//scale the coordinates so output as 5 decimmal places to cooincide with
				//original netCDF formatting
				for (int i=0; i<coords.length; i++){
					//Coordinate newCoord = new Coordinate(coords[i].x, coords[i].y, 0);
					//CoordinateUtils.convertLatLonToUTM(newCoord, 17);

					double x = coords[i].x;
					double y = coords[i].y;

					double xScaled = x * 100000;
					xScaled = Math.round(xScaled);
					xScaled= xScaled/ 100000;

					double yScaled = y * 100000;
					yScaled = Math.round(yScaled);
					yScaled= yScaled/ 100000;

					coords[i].x = xScaled;
					coords[i].y = yScaled;
				}
				
				featureMap.put(index, coords);

				System.out.println("finished reading in " + index.x + "\t" + index.y);

			}
		}, new NullProgressListener() );


		
		
		
		
		//now re-loop through features and set the coordiantes to proper orientation
		for (Int2D index: featureMap.keySet()){
			
			System.out.println("starting to re-orienting " + index.x + "\t" + index.y);
			if (index.x == 173 && index.y == 126) {
				System.out.println("starting up");
			}

			int llCorner = getLLCorner(index.x, index.y);
			Coordinate[] coords = featureMap.get(index);
			Coordinate[] orientedCoords = new Coordinate[4];
			
			//TODO --check logic here to make sure are mapping up correctly
			if (llCorner == 0){ //starting coord is correct, so just need to reverse
				orientedCoords[0] = coords[0];
				orientedCoords[1] = coords[3];
				orientedCoords[2] = coords[2];
				orientedCoords[3] = coords[1];
			}
			else if (llCorner == 1) {
				orientedCoords[0] = coords[1];
				orientedCoords[1] = coords[0];
				orientedCoords[2] = coords[3];
				orientedCoords[3] = coords[2];
			}
			else if (llCorner == 2) {
				orientedCoords[0] = coords[2];
				orientedCoords[1] = coords[1];
				orientedCoords[2] = coords[0];
				orientedCoords[3] = coords[3];
			}
			else {
				orientedCoords[0] = coords[3];
				orientedCoords[1] = coords[2];
				orientedCoords[2] = coords[1];
				orientedCoords[3] = coords[0];
			}
			
			featureMap.put(index, orientedCoords);
			
			

		}



		// This is for stretching out offshore cells
		for (int j = 1; j<=jMax; j++) {
			//set the new values for each new index, and once is set, then delete the rest of indices that get combined with this one

			//first cell
			for (int i=0;i<tail.length;i++){

				Int2D headIndex = new Int2D(head[i],j);
				Int2D tailIndex = new Int2D(tail[i],j);
				Coordinate[] headCoords = featureMap.get(headIndex);
				Coordinate[] tailCoords = featureMap.get(tailIndex);

				Coordinate[] newCoords = new Coordinate[4];
				newCoords[0] = (Coordinate) headCoords[0].clone();
				newCoords[1] = (Coordinate) tailCoords[1].clone();
				newCoords[2] = (Coordinate) tailCoords[2].clone();
				newCoords[3] = (Coordinate) headCoords[3].clone();

				Int2D newIndex = new Int2D(i+1, j);

				stretchedMap.put(newIndex, newCoords);

			}

			//go through and change index for all else
			int nextCell = tail[tail.length-1]+1; //50
			int startIndex = tail.length+1; //here, 13
			for (int i=nextCell; i<iMax; i++){
				Int2D oldIndex = new Int2D(i, j);
				Int2D newIndex = new Int2D(startIndex++, j); 
				Coordinate[] coords = featureMap.get(oldIndex);
				stretchedMap.put(newIndex, coords);
			}


		}







		//Print values out

		for (int i=1; i<=iMax; i++){
			for (int j=1; j<=jMax; j++) {

				//Reset the corner coordinates of the cells to the E and W so match up exactly
				Int2D focalCell = new Int2D(i,j);
				Coordinate[] focalCoords = stretchedMap.get(focalCell);

				if (focalCoords != null){

					//Print out the corners
					//out.print((focalCell.x+2) + "\t" + (focalCell.y+2) + "\t");
					for (int k=0; k<focalCoords.length; k++){
						out.print(focalCoords[k].x + "\t" + focalCoords[k].y+ "\t" ); 
					}

					out.println();

					System.out.println("finished with cell " + i + "\t" + j);

				}
			}
		}







		out.close();
	}


	
	/**
	 * Returns the coordinate array index of the structured IJ grid coordinate that should be 
	 * considered the [0] starting array coordinate.  I.e., this returned coordinate should shapefile coordinate that is in the lower
	 * left corner
	 * @param I
	 * @param J
	 * @return
	 */
	public int getLLCorner(int I, int J){
		int llCorner = 0;
		Int2D index = new Int2D(I, J);
		Coordinate[] focalCoords = featureMap.get(index);
		
		//compare to northern cell
		Coordinate[] NCoords = featureMap.get(new Int2D(I, J+1));
		if (NCoords != null) {
			//int quad = getQuadrant(index,new Int2D(I, J+1) );
			//if N cell, then focal cell's 
			int[] equals = getEqualCoords(focalCoords, NCoords);

			//if contains a 0 and a 1, then ll would be 3
			if ( (equals[0] == 0 || equals[1] == 0) && 
					(equals[0] == 1 || equals[1] == 1)) return 3; 

			//if contains a 3 and a 0, then ll would be 2
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 0 || equals[1] == 0)) return 2; 

			//if contains a 3 and a 2, then ll would be 1
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 2 || equals[1] == 2)) return 1; 

			//if contains a 2 and a 1, then ll would be 0
			else if ( (equals[0] == 2 || equals[1] == 2) && 
					(equals[0] == 1 || equals[1] == 1)) return 0; 
		}

		//compare to east cell
		Coordinate[] ECoords = featureMap.get(new Int2D(I+1, J));
		if (ECoords != null) {
			//int quad = getQuadrant(index,new Int2D(I, J+1) );
			//if N cell, then focal cell's 
			int[] equals = getEqualCoords(focalCoords, ECoords);

			//if contains a 0 and a 1, then ll would be 3
			if ( (equals[0] == 0 || equals[1] == 0) && 
					(equals[0] == 1 || equals[1] == 1)) return 2; 

			//if contains a 3 and a 0, then ll would be 2
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 0 || equals[1] == 0)) return 1; 

			//if contains a 3 and a 2, then ll would be 1
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 2 || equals[1] == 2)) return 0; 

			//if contains a 2 and a 1, then ll would be 0
			else if ( (equals[0] == 2 || equals[1] == 2) && 
					(equals[0] == 1 || equals[1] == 1)) return 3; 
		}

		//compare to south cell
		Coordinate[] SCoords = featureMap.get(new Int2D(I, J-1));
		if (SCoords != null) {
			//int quad = getQuadrant(index,new Int2D(I, J+1) );
			//if N cell, then focal cell's 
			int[] equals = getEqualCoords(focalCoords, SCoords);

			//if contains a 0 and a 1, then ll would be 3
			if ( (equals[0] == 0 || equals[1] == 0) && 
					(equals[0] == 1 || equals[1] == 1)) return 1; 

			//if contains a 3 and a 0, then ll would be 2
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 0 || equals[1] == 0)) return 0; 

			//if contains a 3 and a 2, then ll would be 1
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 2 || equals[1] == 2)) return 3; 

			//if contains a 2 and a 1, then ll would be 0
			else if ( (equals[0] == 2 || equals[1] == 2) && 
					(equals[0] == 1 || equals[1] == 1)) return 2; 
		}
		
		//compare to west cell
		Coordinate[] WCoords = featureMap.get(new Int2D(I-1, J));
		if (WCoords != null) {
			//int quad = getQuadrant(index,new Int2D(I, J+1) );
			//if N cell, then focal cell's 
			int[] equals = getEqualCoords(focalCoords, WCoords);

			//if contains a 0 and a 1, then ll would be 3
			if ( (equals[0] == 0 || equals[1] == 0) && 
					(equals[0] == 1 || equals[1] == 1)) return 0; 

			//if contains a 3 and a 0, then ll would be 2
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 0 || equals[1] == 0)) return 3; 

			//if contains a 3 and a 2, then ll would be 1
			else if ( (equals[0] == 3 || equals[1] == 3) && 
					(equals[0] == 2 || equals[1] == 2)) return 2; 

			//if contains a 2 and a 1, then ll would be 0
			else if ( (equals[0] == 2 || equals[1] == 2) && 
					(equals[0] == 1 || equals[1] == 1)) return 1; 
		}
		return llCorner;
	}
	
	
	
	
	
	
	
	/**
	 * Returns the quadrant of where the other cell is compared to focal cell.  Here not sued
	 * 
	 * @param focal
	 * @param other
	 * @return
	 */
	public int getQuadrant(Int2D focal, Int2D other){
		
		Geometry focalGeom = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(featureMap.get(focal)), gf), null);
		Geometry otherGeom = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(featureMap.get(other)), gf), null);

		Coordinate focalCent = focalGeom.getCentroid().getCoordinate();
		Coordinate otherCent = otherGeom.getCentroid().getCoordinate();

		
		if (otherCent.x > focalCent.x && otherCent.y > focalCent.y) return 1;
		if (otherCent.x < focalCent.x && otherCent.y > focalCent.y) return 2;
		if (otherCent.x < focalCent.x && otherCent.y < focalCent.y) return 3;
		if (otherCent.x > focalCent.x && otherCent.y < focalCent.y) return 3;
		if (otherCent.x == focalCent.x && otherCent.y > focalCent.y) return 5;
		if (otherCent.x < focalCent.x && otherCent.y == focalCent.y) return 6;
		if (otherCent.x == focalCent.x && otherCent.y < focalCent.y) return 7;
		if (otherCent.x > focalCent.x && otherCent.y == focalCent.y) return 8;
		
		return 0;
	}
	
	
	
	
	/**
	 * Finds the indices of the focal cell that are shared with the neighboring cell, returning 
	 * the indices as an int array
	 * 
	 * @param focalCoords
	 * @param otherCoords
	 * @return
	 */
	public int[] getEqualCoords(Coordinate[] focalCoords, Coordinate[] otherCoords){
		int[] equals = new int[2];
		
		int counter=0;
		for (int i =0; i<4; i++){
			for (int j=0; j<4; j++){
				if (focalCoords[i].x == otherCoords[j].x && focalCoords[i].y== otherCoords[j].y){
					equals[counter] = i;
					counter++;
				}
			}
		}
		if (counter != 2) return null;  //then error
		return equals;
	}
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
		CH3D_Shp2Corners_TBSpecific c = new CH3D_Shp2Corners_TBSpecific();
		try {
			c.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
