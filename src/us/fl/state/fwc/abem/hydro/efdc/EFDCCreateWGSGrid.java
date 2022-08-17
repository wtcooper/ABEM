package us.fl.state.fwc.abem.hydro.efdc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.opengis.feature.simple.SimpleFeature;

import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.Shapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class EFDCCreateWGSGrid {

	PrintWriter outFile = null; //need Time 	Pressure - in hPa

	int nodesX = 140;
	int nodesY = 150+48;
	int cornersX = nodesX+2;
	int cornersY = nodesY+2;


	double dx = 0.004;
	double dy=0.004; 

	double originX = -82.8512 + dx*3; //-82.846;
	double originY = 27.462 - dy*48;

	Coordinate[][] corners = new Coordinate[cornersY][cornersX]; 


	public static void main(String[] args) {
		EFDCCreateWGSGrid grid = new EFDCCreateWGSGrid();
		grid.addExtraCellsToCorners();

	}


	public void createCornersFile(){

		//Open up a shapefile of a landmask
		Shapefile mask = new Shapefile("dataTest/EFDCLandMask2.shp", true); 
		mask.openShapefile();


		try { 
			new File("output/EFDCGridCorners.txt").delete();
			outFile= new PrintWriter(new FileWriter("output/EFDCGridCorners.txt", true));
		} catch (IOException e) {e.printStackTrace();}


		double sumX = originX;
		double sumY = originY;

		for (int i=0; i<cornersY; i++){
			for (int j=0; j<cornersX; j++){
				corners[i][j] = new Coordinate(sumX, sumY, 0);
				CoordinateUtils.convertLatLonToUTM(corners[i][j], 17); 
				sumX += dx;
			}
			sumX = originX;
			sumY += dy;
		}

		for (int i=0; i<nodesY; i++){
			for (int j=0; j<nodesX; j++){
				Coordinate node = new Coordinate(corners[i][j].x, corners[i][j].y, 0);
				CoordinateUtils.convertUTMToLatLon(node, 17, false);
				node.x += dx/2.0;
				node.y += dy/2.0; 

				//if there isn't land directly under the node
				if (mask.getFeature(node) == null){
					outFile.println(corners[i+1][j].x +"\t"+ corners[i+1][j].y +"\t"+ corners[i][j].x +"\t"+ corners[i][j].y +"\t"+ corners[i][j+1].x +"\t"+ corners[i][j+1].y +"\t"+ corners[i+1][j+1].x +"\t"+ corners[i+1][j+1].y);
				}
			}
		}

		outFile.close();
	}


	/**Adds extra cells to the existing EFDC grid, using the corners.inp and a new polygon where want new cells added
	 * 
	 */
	public void addExtraCellsToCorners(){


		String filename = "C:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\corners.inp"; 

		String extraShapeCoords = "C:\\EFDCData\\TestCases\\TBAngled_clip.shp";
		
		Shapefile extraShp = new Shapefile(extraShapeCoords, false);
		extraShp.openShapefile();
		SimpleFeature feature = extraShp.getFeature(new Coordinate(-82.6442, 27.3926,0)); 
		MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometry();
		Coordinate[] coords = geom.getCoordinates();
		
		/*		Coordinate[] coords = new Coordinate[5];
		coords[0] = new Coordinate(320569, 3078440, 0); 
		coords[1] = new Coordinate(323212, 3076108, 0); 
		coords[2] = new Coordinate(323575, 3049158, 0); 
		coords[3] = new Coordinate(327928, 3047240, 0); 
		coords[4] = new Coordinate(331660, 3042006, 0);
		coords[5] = new Coordinate(316630, 3041954, 0); 
		coords[6] = new Coordinate(317200, 3081394, 0); 
		coords[7] = new Coordinate(318755, 3081342, 0); 
		coords[8] = new Coordinate(320569, 3078440, 0); 


		coords[0] = new Coordinate(-82.8322, 27.5418, 0); 
		coords[1] = new Coordinate(-82.7464, 27.4564, 0); 
		coords[2] = new Coordinate(-82.6553, 27.456, 0); 
		coords[3] = new Coordinate(-82.7156, 27.5646, 0); 
		coords[4] = new Coordinate(-82.8322, 27.5418, 0); 
*/
		for (int i=0; i<coords.length; i++) {
			CoordinateUtils.convertLatLonToUTM(coords[i], 17); 
		}

		EFDCGrid grid = new EFDCGrid(filename, true); 

		GeometryFactory gf = new GeometryFactory();


		//geometry where new cells will be added
		Geometry newGeometry = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 


		String outputFilename = "C:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\cornersForGridGen.txt"; 

		try { 
			new File(outputFilename).delete();
			outFile= new PrintWriter(new FileWriter(outputFilename, true));
		} catch (IOException e) {e.printStackTrace();}


		double sumX = originX;
		double sumY = originY;

		for (int i=0; i<cornersY; i++){
			for (int j=0; j<cornersX; j++){
				corners[i][j] = new Coordinate(sumX, sumY, 0);
				CoordinateUtils.convertLatLonToUTM(corners[i][j], 17); 
				sumX += dx;
			}
			sumX = originX;
			sumY += dy;
		}

		for (int i=0; i<nodesY; i++){
			for (int j=0; j<nodesX; j++){
				Coordinate node = new Coordinate(corners[i][j].x, corners[i][j].y, 0);

				//convert to Lat/Lon to get midpoint, and then convert back
				CoordinateUtils.convertUTMToLatLon(node, 17, false); 
				node.x += dx/2.0;
				node.y += dy/2.0;
				CoordinateUtils.convertLatLonToUTM(node, 17); 

				//if there isn't the old 
				if (grid.getGridCell(node) != null){
					outFile.println(corners[i+1][j].x +"\t"+ corners[i+1][j].y +"\t"+ corners[i][j].x +"\t"+ corners[i][j].y +"\t"+ corners[i][j+1].x +"\t"+ corners[i][j+1].y +"\t"+ corners[i+1][j+1].x +"\t"+ corners[i+1][j+1].y);
					//System.out.println(corners[i+1][j].x +"\t"+ corners[i+1][j].y +"\t"+ corners[i][j].x +"\t"+ corners[i][j].y +"\t"+ corners[i][j+1].x +"\t"+ corners[i][j+1].y +"\t"+ corners[i+1][j+1].x +"\t"+ corners[i+1][j+1].y);
				}
				else {
					Geometry nodeGeom = (Geometry) gf.createPoint(node); 
					if (nodeGeom.intersects(newGeometry)) { 
						outFile.println(corners[i+1][j].x +"\t"+ corners[i+1][j].y +"\t"+ corners[i][j].x +"\t"+ corners[i][j].y +"\t"+ corners[i][j+1].x +"\t"+ corners[i][j+1].y +"\t"+ corners[i+1][j+1].x +"\t"+ corners[i+1][j+1].y);
						//System.out.println(corners[i+1][j].x +"\t"+ corners[i+1][j].y +"\t"+ corners[i][j].x +"\t"+ corners[i][j].y +"\t"+ corners[i][j+1].x +"\t"+ corners[i][j+1].y +"\t"+ corners[i+1][j+1].x +"\t"+ corners[i+1][j+1].y);
					}
				}
			}


		}
		
		outFile.close();


	}


	}
