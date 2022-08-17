package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;

public class NCOM_2_EFDCBoundCond {

	

	public void step(){

		NumberFormat nf = NumberFormat.getInstance(); 

		NetCDFFile ncFile = null; 
		String dateURL;
		String baseFile = "c:\\work\\data\\NCOM_AS\\";
		String nameURL = "ncom_relo_amseas_";
		String directory = "C:\\work\\workspace\\EFDC\\TampaBay_WGS\\";
		
		int startYear = 2010;
		int startMonth = 5;
		int startDay = 25;
		int endYear = 2010;
		int endMonth = 9;
		int endDay = 07;

		Calendar tempDate; 
		double daysSinceEpoch;

		
		try {

			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all boundary condition cells |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

			EFDCGrid grid = new EFDCGrid(directory + "corners.inp", true); 

			//Read in an ASCI file which simply has I \t J values for the boundary cells
			BufferedReader reader = new BufferedReader(new FileReader(new File(directory + "EFDCBoundCells.txt")));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); //split("[ ]+"); 
				Int3D index = new Int3D();
				index.x = Integer.parseInt(tokens[0]);
				index.y = Integer.parseInt(tokens[1]);

				//add the grid cell to the boundCells
				Coordinate centroid = grid.getGridCells().get(index).getCentroidCoord();
				//convert from UTM to lat/lon
				CoordinateUtils.convertUTMToLatLon(centroid, 17, false);

				PrintWriter outFile = new PrintWriter(new FileWriter(baseFile + "SurfElevForEFDC/EFDCBoundCellElev_" + index.x +"_"+ index.y+".txt", true));


				//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all days to record |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

				Calendar startDate = new GregorianCalendar(startYear, startMonth-1, startDay); // this is JULY 1st!
				Calendar endDate = new GregorianCalendar(endYear, endMonth-1, endDay); 

				System.out.println("start day: " + startDate.getTime().toString() + "\tend day: " + endDate.getTime().toString());
				startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
				endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

				while (startDate.before(endDate)) {


					int month = startDate.get(Calendar.MONTH) + 1;  
					int day = startDate.get(Calendar.DATE);  
					int year = startDate.get(Calendar.YEAR);

					nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
					dateURL = year+nf.format(month)+nf.format(day)+"00"; 

					//open the file and catch error if doesn't exist for missing days of data
					try {
						ncFile = new NetCDFFile(baseFile + nameURL + dateURL + ".nc");
					} catch (IOException e) {
						//System.out.println("file doesn't exist, skipping to next");
						startDate.add(Calendar.DAY_OF_YEAR, 1); // add a day to the date
						continue; 
					}

					ncFile.setInterpolationAxes("lat", "lon");
					ncFile.setVariables("time", "depth", "lat", "lon", "surf_el"); 



					//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all time entries in a netCDF file |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 
					//set the start time HOUR
					int time = (int) Math.round(ncFile.getValue("time", new int[] {0}).doubleValue());

					int counter = 0; 
					for (int i=0; i<ncFile.getSingleDimension("time"); i++){

						double surfEl = ncFile.getValue("surf_el", new double[] {time, centroid.y, centroid.x}, new boolean[] {false, false, false}, true).doubleValue();
						
						tempDate = new GregorianCalendar(year, month-1, day, counter, 0);
						tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

						daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(tempDate);
						
						outFile.println(daysSinceEpoch + "\t" + surfEl);


						time += 3; 
						counter += 3; 
					}
					ncFile.closeFile();
					startDate.add(Calendar.DAY_OF_YEAR, 1); // add a day to the date
				}
				outFile.close();
				System.out.println("Finished writing (" + index.x + ", " + index.y + ")");
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		NCOM_2_EFDCBoundCond run =new NCOM_2_EFDCBoundCond(); 
		run.step();
	}

}
