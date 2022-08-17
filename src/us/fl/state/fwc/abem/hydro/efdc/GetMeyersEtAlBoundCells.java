package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class GetMeyersEtAlBoundCells {

	NumberFormat nf = NumberFormat.getInstance(); 
	DecimalFormat twoDForm = new DecimalFormat("#.##");

	NetCDFFile ncFile = null; 
	String dateURL;
	String baseURL = "C:\\work\\data\\";
	String ncFileInput = baseURL + "LutherEtAlTBModelOutput\\nowcast_3508.0417-3508.0833.cdf";
	String textFileInput = baseURL + "StreamFlowData\\MeyersEtAlFlowTable.txt";
	String shapeFileOutput = baseURL+"MeyersBoundCells.shp";
	String timeVarName = "time", depthVarName = "sigma", 
	latVarName = "y", lonVarName = "x", elevVarName = "elev"; 

	PrintWriter outFile = null; 

	@SuppressWarnings("unchecked")
	public void getBoundaryCells(){

		try {

			//delete file if already exists
			new File(shapeFileOutput).delete();
			//if (outFileTest.exists()) outFileTest.delete(); 
			
			//outFile = new PrintWriter(new FileWriter(baseURL + "MeyersEtAlCellLocations.txt", true));

			System.out.println("Name\tMeyers_I\tMeyers_J\tLon\tLat\tUTM.x\tUTM.y");

			
			//open the file and catch error if doesn't exist for missing days of data
			try {
				ncFile = new NetCDFFile(ncFileInput);
			} catch (IOException e) {
				System.exit(0); 
			}

			ncFile.setVariables(latVarName, lonVarName); 

			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all boundary condition cells |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

			//SimpleShapefile shape = new SimpleShapefile(shapeFileOutput)	;
			Class[] attributes = {Point.class, String.class, Double.class, Double.class, String.class}; 
			//String[] attNames = {"Geometry", "Name", "AvgFlow(m/s)", "Area(m3/s)", "GuageSource"}; 
			ArrayList<Object[]> map = new ArrayList<Object[]>(); 
			
			
			//Read in an ASCI file with:  I	J	Name	Flow	Area	GaugeSource
			BufferedReader reader = new BufferedReader(new FileReader(new File(textFileInput)));
			reader.readLine(); //read header
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); //split("[ ]+"); 

				Int3D index = new Int3D();
				index.x = Integer.parseInt(tokens[0]);
				index.y = Integer.parseInt(tokens[1]);
				float lat = ncFile.getValue(latVarName, new int[] {index.y, index.x}).floatValue(); 
				float lon = ncFile.getValue(lonVarName, new int[] {index.y, index.x}).floatValue(); 

				String name = tokens[2];
				double avgFlow = Double.parseDouble(tokens[3]);
				double catchmentArea = Double.parseDouble(tokens[4]);
				String guageSource = tokens[5];

				Coordinate meyersCell = new Coordinate(lon, lat); 
				CoordinateUtils.convertLatLonToUTM(meyersCell, 17); 

				
				Object[] vals = {new Coordinate[]{meyersCell}, name, avgFlow, catchmentArea, guageSource}; 
				map.add(vals);
				
				System.out.println(name + "\t" + index.x + "\t" + index.y + "\t" + lon + "\t" + lat + "\t" + meyersCell.x + "\t" + meyersCell.y);
				
			}
			
			Object[][] attValues = new Object[map.size()][attributes.length]; 
			for (int i=0; i<map.size(); i++){
				attValues[i] = map.get(i); 
			}
			
			//shape.createShapefile(attributes, attNames, attValues, "UTM17N");
			
			ncFile.closeFile();
			//outFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	
	
	public void getAllCellsCentroids(){

		String shapeFileOutput = baseURL+"MeyersCells.shp";


			//delete file if already exists
			new File(shapeFileOutput).delete();
			//if (outFileTest.exists()) outFileTest.delete(); 
			
			
			//open the file and catch error if doesn't exist for missing days of data
			try {
				ncFile = new NetCDFFile(ncFileInput);
			} catch (IOException e) {
				System.exit(0); 
			}

			ncFile.setVariables(latVarName, lonVarName, elevVarName); 
			ncFile.turnOffScaleFactor(); 
			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all boundary condition cells |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

			SimpleShapefile shape = new SimpleShapefile(shapeFileOutput);
			ArrayList<Coordinate[]> coords = new ArrayList<Coordinate[]>(); 

			int latSize = 100; //ncFile.getSingleDimension(latVarName);
			int lonSize = 70; //ncFile.getSingleDimension(lonVarName);

			for (int i=0; i<latSize; i++){
				for (int j=0; j<lonSize; j++){
					short elev = ncFile.getValue(elevVarName, new int[] {0,i,j}).shortValue();
					if (!(elev==0)){
						float lat = ncFile.getValue(latVarName, new int[] {i, j}).floatValue(); 
						float lon = ncFile.getValue(lonVarName, new int[] {i, j}).floatValue(); 
						Coordinate coord = new Coordinate(lon, lat);
						CoordinateUtils.convertLatLonToUTM(coord, 17); 
						coords.add(new Coordinate[] {coord}); 
					}
				}
			}
			Coordinate[][] coordList = new Coordinate[coords.size()][1]; 
			for (int i=0; i<coords.size(); i++){
				coordList[i] = coords.get(i); 
			}
			
			shape.createShapefile(Point.class, coordList, null);
			
			ncFile.closeFile();
			//outFile.close();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		GetMeyersEtAlBoundCells run =new GetMeyersEtAlBoundCells(); 
		run.getBoundaryCells();
	}

}
