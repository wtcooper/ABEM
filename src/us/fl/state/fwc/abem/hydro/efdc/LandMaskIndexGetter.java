package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ucar.nc2.Variable;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;

public class LandMaskIndexGetter {

	
	String EFDCDirectory = "c:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	String maskFile = EFDCDirectory + "mask.inp";
	ArrayList<Integer> landMaskListXs; //a set that holds just the x index values for faster check
	HashMap<Int3D, Int3D> landMaskList; 
	String gridLandFileName = "c:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\TB2SB_WGS_LandMaskForGridBarrier.nc"; //"c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file

	

	public void step() throws IOException{
		landMaskListXs = new ArrayList<Integer>(); 
		landMaskList = new HashMap<Int3D, Int3D>(); 
		
		// initiate the landGrid netCDF
		NetCDFFile ncFile = null; 
		try {
			ncFile = new NetCDFFile(gridLandFileName);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		ncFile.setVariables("lat", "lon", "landMask"); 
		Variable latVar = ncFile.getVariable("lat");
		Variable lonVar = ncFile.getVariable("lon"); 
		
		//initiiate EFDC grid
		EFDCGrid grid = new EFDCGrid(EFDCDirectory + "corners.inp", true); 
		File file = new File(maskFile); 

		BufferedReader reader = new BufferedReader(new FileReader(file));

		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			String tokens[] = line.split("[ ]+"); // this is a "greedy qualifier regular expression in java -- don't understand but works

			if (!tokens[0].equals("C") && tokens.length == 4){
				int i1 = Integer.parseInt(tokens[1]);
				int j1 = Integer.parseInt(tokens[2]);
				int type = Integer.parseInt(tokens[3]); 
				
				Coordinate coord = grid.getGridCell(new Int3D(i1, j1, 0)).getCentroidCoord(); 
				//convert from EFDC's UTM to Lat/Lon
				CoordinateUtils.convertUTMToLatLon(coord, 17, false); 
				
				//reset the i/j index to reflect the new expanded grid of the larger area
				i1 = ncFile.locate(lonVar, coord.x);
				j1 = ncFile.locate(latVar, coord.y);
				
				System.out.println(i1 + "\t" + j1 + "\t" + type);
				
				int i2 = 0; 
				@SuppressWarnings("unused")
				int j2 = 0; 
				if (type == 1){
					i2 = i1-1;
					j2 = j1;
				}
				else if (type == 2){
					i2 = i1;
					j2 = j1-1;
				}
				else if (type == 3){
					i2 = i1-1;
					j2 = j1-1;
				}
				
				landMaskListXs.add(i1);
				landMaskListXs.add(i2);
				
				
				
			}
		}
	}

	public static void main(String[] args) {
		LandMaskIndexGetter get = new LandMaskIndexGetter();
		try {
			get.step();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
