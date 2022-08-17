package us.fl.state.fwc.util.geo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.vividsolutions.jts.geom.Coordinate;

public class NetCDFFileTester {

	Coordinate northCoord = new Coordinate(-82.877, 27.771);
	Coordinate westCoord = new Coordinate(-82.870, 27.625);
	Coordinate southCoord = new Coordinate(-82.775, 27.467);
	Coordinate egmontCoord = new Coordinate(-82.761, 27.591);

	double meanTide = 0.354; 

	ArrayList<double[]> tideObs = new ArrayList<double[]>(); 
	double[] tideObsHours;
	double[] tideObsData; 

	String obsDataFileName = "c:\\work\\data\\BuoyData\\Egmont2010_May-JulyBuoy_tab.txt";

	Calendar startDate = new GregorianCalendar(2010, 4, 29); 
	Calendar endDate = new GregorianCalendar(2010, 5, 31); 
	NumberFormat nf = NumberFormat.getInstance(); 
	DecimalFormat twoDForm = new DecimalFormat("#.##");

	NetCDFFile ncFile; 
	String dateURL;
	String baseFile = "c:\\work\\data\\NCOM_AS\\";
	String nameURL = "ncom_relo_amseas_";



	public static void main(String[] args) {
		NetCDFFileTester test = new NetCDFFileTester();
		//test.readTideObs();
		test.step2();

	}

	public void step2(){

		startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

		int month = startDate.get(Calendar.MONTH) + 1;  
		int day = startDate.get(Calendar.DATE);  
		int year = startDate.get(Calendar.YEAR);

		nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
		dateURL = year+nf.format(month)+nf.format(day)+"00"; 

		try {
			ncFile = new NetCDFFile(baseFile + nameURL + dateURL + ".nc");
		} catch (IOException e) {}

		ncFile.setInterpolationAxes("lat", "lon");
		ncFile.setVariables("time", "depth", "lat", "lon", "surf_el", "water_u", "water_v", "water_temp"); 

		//set the start time

		Coordinate centroid = new Coordinate(-82.2, 27.6);

		double temp = ncFile.getClosestValue("water_temp", new double[] {0, 0, centroid.y, centroid.x}, new boolean[] {true, true, false, false}).doubleValue() ;

		System.out.println(/*time + "\t" + */temp);//+ "\t" + TimeUtils.getTidalElevation(tideDate, "data/StPeteTidalConstituents.txt") + "\t" + tideObsData[tideObsIndex]);



	}






	public int locate(double[] ja, double val){
		int idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// Error check
			if (idx == -1) {
				//throw new IllegalArgumentException(var.getName() + " value "
				//+ val + " does not fall in the range " + ja[0] + " : "
				//+ ja[ja.length - 1] + ".");
				return -1;
			}

			// If not an exact match - determine which value we're closer to
			if (-(idx + 2) >= ja.length) {
				return 0;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;
			if (val < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;

	}













	public void readTideObs(){

		BufferedReader reader = null; 
		try {
			reader = new BufferedReader(new FileReader(obsDataFileName));
			/* First line of the data file is the header */
			reader.readLine();
			reader.readLine();

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				//#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
				int year = Integer.parseInt(tokens[0]);
				int month = Integer.parseInt(tokens[1]) -1;
				int day = Integer.parseInt(tokens[2]);
				int hour = Integer.parseInt(tokens[3]);
				int min = Integer.parseInt(tokens[4]);

				Calendar date = new GregorianCalendar(year, month, day, hour, min);
				date.setTimeZone(TimeZone.getTimeZone("GMT"));


				if (!tokens[17].equals("MM") && !(Double.parseDouble(tokens[17]) == 99.)) {
					double tide = Double.parseDouble(tokens[17])*0.3048 - meanTide; // convert to meters, then adjust so is at meanTide versus MLLH 

					Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
					epochStart.setTimeZone(date.getTimeZone()); 
					long dateMilli = date.getTimeInMillis() ;
					long epochMilli = epochStart.getTimeInMillis();
					long milliSecDiff = dateMilli - epochMilli; //date.getTimeInMillis() - epochStart.getTimeInMillis(); 
					double hrsSinceEpoch = (double) milliSecDiff/(1000*60*60);

					//double hrSinceEpoch = TimeUtils.getHoursSinceTidalEpoch(date); 

					double[] data = new double[] {hrsSinceEpoch, tide}; 
					tideObs.add(data);

				}
			}
			//counter++;

		}catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			tideObsHours = new double[tideObs.size()];
			tideObsData = new double[tideObs.size()];

			for (int i=0; i<tideObs.size(); i++){
				double[] data = tideObs.get(i);
				tideObsHours[i] = data[0];
				tideObsData[i] = data[1];
			}

			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}



















	public void step(){

		//Tests OK
		NetCDFFile file = null;
		try {
			file = new NetCDFFile("dataTest/v3d_2010051500.nc");
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("file doesn't exist");
			System.exit(1); 
		}
		file.setVariables("Time", "Depth", "Latitude", "Longitude", "V_Velocity"); 
		System.out.println(file.getDataType("V_Velocity"));

		int latIndex = 300, lonIndex = 300, timeIndex=0, depthIndex=0;

		System.out.println();

		float value = file.getValue("V_Velocity", new int[] {timeIndex, depthIndex, latIndex, lonIndex}).floatValue();
		double latValue = file.getValue("Latitude", new int[] {latIndex}).doubleValue();
		double lonValue = file.getValue("Longitude", new int[] {lonIndex}).doubleValue();
		System.out.println("value at (" + lonValue + ", " +latValue + "): " + value);

		latIndex++;
		lonIndex++;
		value = file.getValue("V_Velocity", new int[] {timeIndex, depthIndex, latIndex, lonIndex}).floatValue();
		latValue = file.getValue("Latitude", new int[] {latIndex}).doubleValue();
		lonValue = file.getValue("Longitude", new int[] {lonIndex}).doubleValue();
		System.out.println("value at (" + lonValue + ", " +latValue + "): " + value);

		System.out.println();

		//lined up with netcdf index numbers and tracked the general change quite well
		double latPos = 24.97;
		double lonPos = -89;
		file.setInterpolationAxes("Latitude", "Longitude");
		file.setInterpolationRadius(3);
		//file.setScaleFactor(10);
		value = file.getValue("V_Velocity", new double[] {timeIndex, depthIndex, latPos, lonPos}, new boolean[] {false, false, false, false}, true).floatValue();
		System.out.println("value at (" + lonPos + ", " +latPos + "): " + value);

		//Test BiCubicSpline interpolation


		/*		NetCDFFile efdc = new NetCDFFile("dataTest/uvel_norotate_temp.nc");
		efdc.setVariables("time", "depth", "lat", "lon", "uvel", null, (String[]) null);
		System.out.println(efdc.getDataType("uvel"));
		int max = 100;
		for (int i=0; i<efdc.getDimension("lon"); i++){
		value = efdc.getValue("uvel", new int[] {1, 1, 100, i}).floatValue();
		System.out.println("value: " + value);
		}
		 */		

	}


}
