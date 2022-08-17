package us.fl.state.fwc.abem.hydro.ch3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class CH3D_Grid2Shp {


	String filename =
		"C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\SCH3TB01-UFL_WANAFe11-UFL_20050724T1200_20050724T1200_20050726T0000_86gsr1_Z.nc"
//			"C:\\temp\\CH3D_ECFL\\SCH3EC01-UFL_WANAFe11-UFL_20050827T1800_20050827T1800_20050901T1800_12gsr1_Z.nc"
//			"C:\\temp\\CH3D_NGoM\\SCH3NG01-UFL_WANAFe11-UFL_20050827T1800_20050827T1800_20050901T1800_12gsr1_Z.nc"
	; 

	String outFile = "C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\CH3D_TB_working.shp";

	String EPSG_code = "UTM17N";
	
	int timeDim = 30; //Integer.MAX_VALUE;
	//HERE, HSGRID is depth at cell center, X/YGrid are corner points, and I/JGrid are indices to each cell
	private String[] varName = {"IGRID", "JGRID", "HSGRID", "LWFLAGGRID"};
	private String timeName = "time";
	private String xName = "XGRID";
	private String yName = "YGRID";
	private String iDimName = "ICELLS";
	private String jDimName = "JCELLS";

	private NetCDFFile ncFile; 

	public void run() {

		/**Read in X/Y from netCDF, store in array, along with I/J pointers
		 * Export shp(), storing the I/J pointers
		 */




		//####################################
		//For each filename, create new shapefile


			try {
				ncFile = new NetCDFFile(filename); 
			} catch (IOException e) {
				e.printStackTrace();
			}

			//no interpolation because 
			//ncFile.setInterpolationAxes(latName, lonName);

			//create new array varNames to setVariabbles() method
			String[] varNames = new String[varName.length + 5];
			int counter=0;
			for (String var: varName){
				varNames[counter++] = var;
			}
			varNames[counter++] = timeName;
			varNames[counter++] = xName;
			varNames[counter++] = yName;
			varNames[counter++] = iDimName;
			varNames[counter++] = jDimName;
			//			ncFile.setVariables(latName, lonName, varName, timeName); 
			ncFile.setVariables(varNames);

			//		EFDCLandFile.setVariables(latName, lonName, landName);




			ArrayList<Coordinate[]> featureCoords = new ArrayList<Coordinate[]>(); 
			ArrayList<Object[]> featureVals = new ArrayList<Object[]>(); 
			Class[] attributeTypes = new Class[1 + varName.length]; // {Polygon.class, Double.class}; //new Class[2];
			String[] attributeNames = new String[1 + varName.length]; //{"the_geom", varName}; //new String[2];
			attributeTypes[0] = Polygon.class;
			attributeNames[0] = "the_geom";
			attributeTypes[1] = Integer.class;
			attributeNames[1] = "I";
			attributeTypes[2] = Integer.class;
			attributeNames[2] = "J";
			attributeTypes[3] = Double.class;
			attributeNames[3] = "Depth";
			attributeTypes[4] = Integer.class;
			attributeNames[4] = "WetDryFlag";


			for (int i=0; i< varName.length; i++){
				attributeTypes[i+1] = Double.class; 
				attributeNames[i+1] = varName[i];
			}

			//get the number of corners 
			int cornersDim = ncFile.getSingleDimension(xName);
			int iDim = ncFile.getScalarValue(iDimName).intValue();
			int jDim = ncFile.getScalarValue(jDimName).intValue();
			

			Coordinate[][] corners =new Coordinate[iDim+1][jDim+1]; 
			int[][] wetDry = new int[iDim+1][jDim+1];
			float[][] depth	 = new float[iDim+1][jDim+1];

			for (int i=0; i<cornersDim; i++) {
				int iIndex = ncFile.getValue("IGRID", new int[] {i}).intValue() - 1;
				int jIndex = ncFile.getValue("JGRID", new int[] {i}).intValue() - 1;

				float x = ncFile.getValue(xName, new int[] {i}).floatValue();
				float y = ncFile.getValue(yName, new int[] {i}).floatValue();
				corners[iIndex][jIndex] = new Coordinate(x,y,0);

				wetDry[iIndex][jIndex] = ncFile.getValue("LWFLAGGRID", new int[] {i}).intValue(); 
				depth[iIndex][jIndex] = ncFile.getValue("HSGRID", new int[] {i}).floatValue();

				System.out.println("read data from i: " + iIndex + "\tj: " + jIndex);
			}


			for (int i=0; i<corners.length-1; i++){
				for (int j=0; j<corners[i].length-1; j++){

					int iIndex = i+1, jIndex = j+1;
					Coordinate[] coords = new Coordinate[5];  
					coords[0] = corners[i][j];
					coords[1] = corners[i+1][j];
					coords[2] = corners[i+1][j+1];
					coords[3] = corners[i][j+1];
					coords[4] = corners[i][j];

					boolean writeData = true;
					for (int k=0; k<coords.length; k++){
						if (coords[k] == null || depth[i][j] >= 0) writeData = false; 
					}

					if (writeData){
						Object[] vals = {iIndex, jIndex, depth[i][j], wetDry[i][j]};
						featureCoords.add(coords);
						featureVals.add(vals);

						System.out.println("processed data from i: " + iIndex + "\tj: " + jIndex);
					}
				}
			}


			DeleteFiles delete = new DeleteFiles();
			delete.deleteByPrefix(outFile);
			SimpleShapefile shape = new SimpleShapefile(outFile); 
			shape.createShapefile(attributeTypes, attributeNames, 
					featureCoords, featureVals, EPSG_code);  

		}
	




	public static void main(String[] args) {
		CH3D_Grid2Shp c = new CH3D_Grid2Shp();
		c.run();
	}
}
