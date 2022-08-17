package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class EFDCLandMaskToShapefile {



	int xOffset = 313295;
	int yOffset	= 3113157;
	double cellSize=10; 

	//private  PrintWriter outFile = null; 

	ArrayList<ArrayList<Coordinate>> features = new ArrayList<ArrayList<Coordinate>>(); 


	public static void main(String[] args) {
		EFDCLandMaskToShapefile frc = new EFDCLandMaskToShapefile();
		frc.step("dataTest/EFDCGridLand5.p2d"); 
	}


	public void step(String filename) {

		try {

			BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); //split("[ ]+"); 

				ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
				while (!tokens[0].contains("EFDCGridLandMask5")){
					 
					Coordinate coord = new Coordinate(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]), 0); 
					CoordinateUtils.convertUTMToLatLon(coord, 17, false);
					
					coords.add(coord); 
					line = reader.readLine();
					if (line == null) break; 
					tokens = line.split("\t"); //split("[ ]+"); 
				}
				if (coords.size() > 0 ) features.add(coords); 

				System.out.println("finished writing " + tokens[0]); 
			}


			ArrayList<Coordinate[]> featuresConv = new ArrayList<Coordinate[]>(); 

			for (int i=0; i<features.size(); i++){
				ArrayList<Coordinate> list = features.get(i);
				Coordinate[] array = new Coordinate[list.size()]; 
				list.toArray(array);
				featuresConv.add(array); 
			}	
				
			DeleteFiles delete = new DeleteFiles();
			delete.deleteByPrefix("output", "EFDCLandMaskGood");
			SimpleShapefile shape = new SimpleShapefile("output/EFDCLandMaskGood.shp"); 
			shape.createShapefile(Polygon.class, featuresConv, "WGS84"); 
			

			


		} catch (IOException e) {
			e.printStackTrace();
		}

	}



}
