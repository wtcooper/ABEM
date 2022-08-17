package us.fl.state.fwc.abem.hydro.efdc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;

public class EFDCInitialCondFromNCOM {

	int year = 2010;
	int month = 5;
	int day = 25;

	String fileLoc = "c:\\work\\data\\NCOM_AS\\ncom_relo_amseas_"; 
	String baseFile = "C:\\work\\data\\EFDC\\InitialConds\\"; //c:\\work\\data\\NCOM_AS\\";

	Calendar date = new GregorianCalendar(year, month-1, day); 

	final static double eastBound = -82.24;
	final static double westBound = -82.95;
	final static double northBound = 28.10;
	final static double southBound = 27.36;

	public void step(){

		date.setTimeZone(TimeZone.getTimeZone("GMT"));

		NumberFormat nf = NumberFormat.getInstance(); 
		nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
		String dateTag = year+nf.format(month)+nf.format(day)+"00";
		fileLoc += dateTag + ".nc";

		EFDCGrid efdc = new EFDCGrid("data/corners.inp", true);
		efdc.setCellSizeAndDepth("data/dxdy.inp", 8);
		
		// outfile (tab deliminted): lon  lat  value
		try {




			NetCDFFile ncFile = new NetCDFFile(fileLoc);
			ncFile.setVariables("depth", "lat", "lon", "salinity", "water_temp"); 		
			

			//cycle through salinity first, then temp
			for (int w=0; w<2; w++){
				String varName;
				if (w==0) varName = "salinity";
				else varName = "water_temp"; 
				
				//loop through all the depth layers in the EFDC model, and get 
				for (int z = 0; z< efdc.getNumDepthLayers(); z++ ){
					
					PrintWriter outFile = new PrintWriter(new FileWriter(baseFile + "EFDCInitial_" + varName + z + "_" + dateTag + ".txt", true));
					int minLat = ncFile.locate(ncFile.getVariable("lat"), southBound);
					int maxLat = ncFile.locate(ncFile.getVariable("lat"), northBound);
					int minLon = ncFile.locate(ncFile.getVariable("lon"), westBound);
					int maxLon = ncFile.locate(ncFile.getVariable("lon"), eastBound);

					for (int i=minLat; i<=maxLat; i++){
						for (int j=minLon; j<=maxLon; j++){

							double lat = ncFile.getValue("lat", new int[] {i}).doubleValue();
							double lon =ncFile.getValue("lon", new int[] {j}).doubleValue();
							Coordinate coord = new Coordinate(lon, lat); 
							CoordinateUtils.convertLatLonToUTM(coord, 17);

							//get the depth  
							EFDCCell cell = efdc.getGridCell(coord); 
							
							if (! (cell== null) ){
								System.out.println("accessing cell...");
								double depth = cell.getLayerCentroid(z);
							
							double value = ncFile.getValue(varName, new double[] {0, depth, i, j}, new boolean[] {true, false, true, true}, false).doubleValue(); 
							if (value > -25) {
								
								value+=20; //this is the "add_offset" value in the netCDF -- TODO -- need to put that in there!!!!!!!!!!!!!!!!!!!!  test
								outFile.println(coord.x + "\t" + coord.y + "\t" + value);
							}
							}
						}
					}
					
					outFile.close();
					System.out.println("finished " + varName + " for " + dateTag); 
				}
			}



		} catch (IOException e1) {
			e1.printStackTrace();
		} 
	}


	public static void main(String[] args) {
		EFDCInitialCondFromNCOM a =new EFDCInitialCondFromNCOM(); 
		a.step();
	}

}
