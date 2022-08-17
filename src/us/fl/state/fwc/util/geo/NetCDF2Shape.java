package us.fl.state.fwc.util.geo;

import java.io.IOException;
import java.util.ArrayList;

import us.fl.state.fwc.util.DeleteFiles;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class NetCDF2Shape {

	String[] filenames = {
			"output/FishSpatialSums_test.nc",
	}; 
	
	String[] outFiles = new String[filenames.length];

	int timeDim = 30; //Integer.MAX_VALUE;

	private String[] varName = {"abundance", "biomass", "SSB", "TEP", "recruitment", "settlers"};
	private String timeName = "time";
	private String latName = "lat";
	private String lonName = "lon";

	private NetCDFFile ncFile; 


	@SuppressWarnings("unchecked")
	public void step(){

		//####################################
		//Set up output files
		for (int i=0; i<filenames.length; i++){
			String filename[] = filenames[i].split("\\."); 
			outFiles[i] = filename[0] + ".shp";
		}



		//####################################
		//For each filename, create new shapefile

		for (int file = 0; file < filenames.length; file++){

			try {
				ncFile = new NetCDFFile(filenames[file]); 
			} catch (IOException e) {
				e.printStackTrace();
			}

			ncFile.setInterpolationAxes(latName, lonName);

			//create new array varNames to setVariabbles() method
			String[] varNames = new String[varName.length + 3];
			int counter=0;
			for (String var: varName){
				varNames[counter++] = var;
			}
			varNames[counter++] = timeName;
			varNames[counter++] = latName;
			varNames[counter++] = lonName;
			//			ncFile.setVariables(latName, lonName, varName, timeName); 
			ncFile.setVariables(varNames);

			//		EFDCLandFile.setVariables(latName, lonName, landName);

			ArrayList<Coordinate[]> featureCoords = new ArrayList<Coordinate[]>(); 
			ArrayList<Object[]> featureVals = new ArrayList<Object[]>(); 
			Class[] attributeTypes = new Class[1 + varName.length]; // {Polygon.class, Double.class}; //new Class[2];
			String[] attributeNames = new String[1 + varName.length]; //{"the_geom", varName}; //new String[2];
			attributeTypes[0] = Polygon.class;
			attributeNames[0] = "the_geom";
			for (int i=0; i< varName.length; i++){
				attributeTypes[i+1] = Double.class; 
				attributeNames[i+1] = varName[i];
			}
			String EPSG_code = "WGS84"; 





			int latDim = ncFile.getSingleDimension(latName);
			int lonDim = ncFile.getSingleDimension(lonName); 
			double missingValue = ncFile.getMissingValue(varName[0]).doubleValue(); 

			for (int i=0; i<latDim; i++){
				for (int j=0; j<lonDim; j++){

					Coordinate midCoord = 
						new Coordinate(ncFile.getValue(lonName, new int[] {j}).doubleValue(), 
								ncFile.getValue(latName, new int[] {i}).doubleValue(), 0);
					Coordinate[] coords = new Coordinate[5];  
					coords[0] = new Coordinate(midCoord.x-0.002, midCoord.y-0.002, 0);
					coords[1] = new Coordinate(midCoord.x+0.002, midCoord.y-0.002, 0);
					coords[2] = new Coordinate(midCoord.x+0.002, midCoord.y+0.002, 0);
					coords[3] = new Coordinate(midCoord.x-0.002, midCoord.y+0.002, 0);
					coords[4] = new Coordinate(midCoord.x-0.002, midCoord.y-0.002, 0);

					if (timeDim == Integer.MAX_VALUE) 
						timeDim = ncFile.getSingleDimension(timeName)-1;

					Object[] vals = null; // = new Object[varName.length];
					for (int k=0; k< varName.length; k++){
						double val = ncFile.getValue(varName[k], new int[] {timeDim, i, j}).doubleValue(); 
						if (val != missingValue) {
							if (vals == null) vals = new Object[varName.length];
							vals[k] = val; 
						}
					}
					
					if (vals != null){
						for (int l = 0; l < varName.length; l++){
							if (vals[l] == null) vals[l] = 0; 
						}
						featureCoords.add(coords);
						featureVals.add(vals);
					}

					
				}
			}


			DeleteFiles delete = new DeleteFiles();
			delete.deleteByPrefix(outFiles[file]);
			SimpleShapefile shape = new SimpleShapefile(outFiles[file]); 
			shape.createShapefile(attributeTypes, attributeNames, 
					featureCoords, featureVals, EPSG_code);  

		}

	}











	public static void main(String[] args) {
		NetCDF2Shape a = new NetCDF2Shape();
		a.step();
	}
}

