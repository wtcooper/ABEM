package us.fl.state.fwc.abem.hydro.ch3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class CH3D_Grid2Corners {


	String filename =
			"C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\SCH3TB01-UFL_WANAFe11-UFL_20050724T1200_20050724T1200_20050726T0000_86gsr1_Z.nc"
//			"C:\\temp\\SCH3CH01-UFL_WANAFe11-UFL_20050724T1200_20050724T1300_20050726T0000_86gsr1_Z.nc"
//			"C:\\temp\\CH3D_ECFL\\SCH3EC01-UFL_WANAFe11-UFL_20050827T1800_20050827T1800_20050901T1800_12gsr1_Z.nc"
//			"C:\\temp\\CH3D_NGoM\\SCH3NG01-UFL_WANAFe11-UFL_20050827T1800_20050827T1800_20050901T1800_12gsr1_Z.nc"
	; 

	String outFile = "C:\\temp\\CH3D_TB\\CH3D_TB_Corners_Grid2Corners.txt";
	String outOrientFile = "C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid\\Orientation.txt";

	String EPSG_code = "UTM17N";
	
	PrintWriter out= null; 
	PrintWriter outOrient = null; 

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
				new File(outFile).delete();
				out= new PrintWriter(new FileWriter(outFile, true));
				outOrient = new PrintWriter(new FileWriter(outOrientFile, true));
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
						
						int llCorner = 0; 
						if (coords[2].x < coords[0].x) llCorner = 1; 
						
						outOrient.println(iIndex + "\t" + jIndex + "\t" + llCorner);
						
						for (int k=0; k<coords.length-1; k++){
							//Coordinate newCoord = new Coordinate(coords[i].x, coords[i].y, 0);
							//CoordinateUtils.convertLatLonToUTM(newCoord, 17);
							out.print(coords[k].x + "\t" + coords[k].y + "\t" ); 
						}

						out.println();

						System.out.println("processed data from i: " + iIndex + "\tj: " + jIndex);
					}
				}
			}


			out.close();
			outOrient.close();

		}
	




	public static void main(String[] args) {
		CH3D_Grid2Corners c = new CH3D_Grid2Corners();
		c.run();
	}
}
