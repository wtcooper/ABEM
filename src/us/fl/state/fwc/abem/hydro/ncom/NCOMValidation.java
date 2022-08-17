package us.fl.state.fwc.abem.hydro.ncom;

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

import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;

public class NCOMValidation {

	public   final int startYear = 2010;
	public   final int startMonth = 7;
	public   final int startDay = 7;
	public   final int endYear = 2010;
	public   final int endMonth = 7;
	public   final int endDay = 31;
	
	Coordinate northCoord = new Coordinate(-82.877, 27.771);
	Coordinate westCoord = new Coordinate(-82.870, 27.625);
	Coordinate southCoord = new Coordinate(-82.775, 27.467);
	Coordinate egmontCoord = new Coordinate(-82.761, 27.591);
	Coordinate sunshineCoord = new Coordinate(-82.654, 27.623); 
	Coordinate ClearwaterTideStationCoord = new Coordinate(-82.8303, 27.98); 
	
	//TO SET variables
	Coordinate centroid = egmontCoord; //new Coordinate(-82.775, 27.467);
	static String comparison = "temp"; //"tide"; //"current"

	
	double meanTide = 0.354; 

	ArrayList<double[]> tideObs = new ArrayList<double[]>(); 
	double[] tideObsHours;
	double[] tideObsData; 

	ArrayList<double[]> currObs = new ArrayList<double[]>(); 
	double[] currObsHours;
	double[] uObsData; 
	double[] vObsData; 

	ArrayList<double[]> tempObs = new ArrayList<double[]>(); 
	double[] tempObsHours;
	double[] tempObsData; 


	String obsTideFileName = "c:\\work\\data\\BuoyData\\Egmont2010_May-JulyBuoy_tab.txt";
	String obsCurrentFileName = "c:\\work\\data\\CurrentData\\SunshineSkywayMay-July2010.txt";
	String obsTempFileName = "c:\\work\\data\\TemperatureData\\StPeteBuoyTempJuly2010.txt";

	Calendar startDate = new GregorianCalendar(startYear, startMonth-1, startDay); 
	Calendar endDate = new GregorianCalendar(endYear, endMonth-1, endDay); 
	NumberFormat nf = NumberFormat.getInstance(); 
	DecimalFormat twoDForm = new DecimalFormat("#.##");

	NetCDFFile ncFile; 
	String dateURL;
	String baseFile = "c:\\work\\data\\NCOM_AS\\";
	String nameURL = "ncom_relo_amseas_";



	public static void main(String[] args) {
		NCOMValidation test = new NCOMValidation();
		if (comparison.equals("tide")){
			test.readTideObs();
		}
		else if (comparison.equals("current")) {
			test.readCurrentObs();
		}
		else {
			test.readTempObs(); 
		}
			
			
		test.compareNCOM_TO_0BSERVED();
	}

	public void compareNCOM_TO_ECOM3D_TB(){

	}


	public void compareNCOM_TO_0BSERVED(){

		startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
		endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

		ncFile = null;
		//System.out.println("time" + "\t" + "NCOM" + "\t" + "EstTide" + "\t" + "ObsTide"); 

		while (startDate.before(endDate)) {

			int month = startDate.get(Calendar.MONTH) + 1;  
			int day = startDate.get(Calendar.DATE);  
			int year = startDate.get(Calendar.YEAR);

			nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
			dateURL = year+nf.format(month)+nf.format(day)+"00"; 

			//open the file and catch error if doesn't exist for missing days of data
			try {
				ncFile = new NetCDFFile(baseFile + nameURL + dateURL + ".nc");
				//System.out.println("connecting to file " + baseFile + nameURL + dateURL + ".nc");
			} catch (IOException e) {
				//System.out.println("file doesn't exist, skipping to next");
				startDate.add(Calendar.DAY_OF_YEAR, 1); // add a day to the date
				continue; 
			}

			ncFile.setInterpolationAxes("lat", "lon");
			ncFile.setVariables("time", "depth", "lat", "lon", "surf_el", "water_u", "water_v", "water_temp"); 


			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all time entries in a netCDF file |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

			//set the start time
			int time = (int) Math.round(ncFile.getValue("time", new int[] {0}).doubleValue());

			int counter=0;
			for (int i=0; i<ncFile.getSingleDimension("time"); i++){ // time units = "hour since 2000-01-01 00:00 UTC";

				//int hour = ncFile.getValue("time", new int[] {i}).intValue();
				//double hourFrac =  (double) hour  / 24d; 
				//the dateWithFrac will be the time variables to output for EFDC
				//double dateWithFrac = Double.valueOf(twoDForm.format( TimeUtils.getDaysSinceTidalEpoch(startDate) + hourFrac));


				double surfEl = ncFile.getValue("surf_el", new double[] {time, centroid.y, centroid.x}, new boolean[] {false, false, false}, true).doubleValue();
				double u = ncFile.getValue("water_u", new double[] {time, 0, centroid.y, centroid.x}, new boolean[] {false, false, false, false}, true).doubleValue() ;
				double v = ncFile.getValue("water_v", new double[] {time, 0, centroid.y, centroid.x}, new boolean[] {false, false, false, false}, true).doubleValue() ;
				double temp = ncFile.getValue("water_temp", new double[] {time, 0, centroid.y, centroid.x}, new boolean[] {false, false, false, false}, true).doubleValue() +20;
				
				
				Calendar tempDate = new GregorianCalendar(year, month-1, day, counter, 0);
				tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

				double hrsSinceEpoch = TimeUtils.getHoursSinceTidalEpoch(tempDate);

				if (comparison.equals("tide")){
					int tideObsIndex = locate(tideObsHours, hrsSinceEpoch);
					System.out.println(time + "\t" + surfEl + "\t" + TimeUtils.getTidalElevation(tempDate, "data/EgmontEstTidalConstituents.txt") + "\t" + tideObsData[tideObsIndex]);
				}
				else if (comparison.equals("current")) {
					int currObsIndex = locate(currObsHours, hrsSinceEpoch);
					System.out.println(time + "\t" + u + "\t" + uObsData[currObsIndex] + "\t" + v + "\t" + vObsData[currObsIndex]);
				}
				else  {
					int tempObsIndex = locate(tempObsHours, hrsSinceEpoch);
					System.out.println(time + "\t" + temp + "\t" + tempObsData[tempObsIndex] );
				}

				time += 3; 
				counter +=3;
			}


			startDate.add(Calendar.DAY_OF_YEAR, 1); // add a day to the date
		} // end of while loop

	}






	public int locate(double[] ja, double val){
		int idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// Error check
			if (idx == -1) {
				//throw new IllegalArgumentException(var.getName() + " value "
				//+ val + " does not fall in the range " + ja[0] + " : "
				//+ ja[ja.length - 1] + ".");
				return 0;
			}

			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= ja.length) {
				return ja.length-1;
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
			reader = new BufferedReader(new FileReader(obsTideFileName));
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
					double tide = Double.parseDouble(tokens[17])*0.3048 ;//- meanTide; // convert to meters, then adjust so is at meanTide versus MLLH 

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




	public void readCurrentObs(){

		BufferedReader reader = null; 
		try {
			reader = new BufferedReader(new FileReader(obsCurrentFileName));
			/* First line of the data file is the header */

			int counter = 0; 
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				String dateString = tokens[0];
				String dateTokens[] = dateString.split("/"); 
				int month = Integer.parseInt(dateTokens[0]) -1; 
				int day = Integer.parseInt(dateTokens[1]); 
				int year = Integer.parseInt(dateTokens[2]) ;



				String timeString = tokens[1]; 
				String timeTokens[] = timeString.split(":");
				int hour = Integer.parseInt(timeTokens[0]);
				int min = Integer.parseInt(timeTokens[1]); 

				Calendar date = new GregorianCalendar(year, month, day, hour, min);


				//double hourFrac =  ((double) hour + ((double) min/60d))  / 24d; 
				//double dateWithFrac = Double.valueOf(twoDForm.format( TimeUtils.getDaysSinceTidalEpoch(date) + hourFrac));

				double current = Double.parseDouble(tokens[3]) * 0.51444444; //conver to meters per second from knots
				double angle = Math.toRadians(Double.parseDouble(tokens[4])); 

				double u = Math.cos(angle)* current; 
				double v= Math.sin(angle) * current; 

				Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
				epochStart.setTimeZone(date.getTimeZone()); 
				long dateMilli = date.getTimeInMillis() ;
				long epochMilli = epochStart.getTimeInMillis();
				long milliSecDiff = dateMilli - epochMilli; //date.getTimeInMillis() - epochStart.getTimeInMillis(); 
				double hrsSinceEpoch = (double) milliSecDiff/(1000*60*60);

				//double hrSinceEpoch = TimeUtils.getHoursSinceTidalEpoch(date); 

				double[] data = new double[] {hrsSinceEpoch, u, v}; 
				currObs.add(data);




				counter++;
				if ((counter%100)==0){
					//	System.out.println("counter " + counter + "\t" + date.getTime().toString());
				}
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			currObsHours = new double[currObs.size()];
			uObsData = new double[currObs.size()];
			vObsData = new double[currObs.size()];

			for (int i=0; i<currObs.size(); i++){
				double[] data = currObs.get(i);
				currObsHours[i] = data[0];
				uObsData[i] = data[1];
				vObsData[i] = data[2];
			}

			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	
	
	
	
	public void readTempObs(){

		BufferedReader reader = null; 
		try {
			reader = new BufferedReader(new FileReader(obsTempFileName));
			/* First line of the data file is the header */
			reader.readLine();
			reader.readLine(); 

			int counter = 0; 
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				int year = Integer.parseInt(tokens[0]) ;
				int month = Integer.parseInt(tokens[1]) -1; 
				int day = Integer.parseInt(tokens[2]); 
				int hour = Integer.parseInt(tokens[3]);
				int min = Integer.parseInt(tokens[4]); 

				Calendar date = new GregorianCalendar(year, month, day, hour, min);
				date.setTimeZone(TimeZone.getTimeZone("GMT")); 
				double hrsSinceEpoch = TimeUtils.getHoursSinceTidalEpoch(date); 

				double temp = Double.parseDouble(tokens[14]); 
				double[] data = new double[] {hrsSinceEpoch, temp}; 
				tempObs.add(data);




				counter++;
				if ((counter%100)==0){
					//	System.out.println("counter " + counter + "\t" + date.getTime().toString());
				}
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			tempObsHours = new double[tempObs.size()];
			tempObsData = new double[tempObs.size()];

			for (int i=0; i<tempObs.size(); i++){
				double[] data = tempObs.get(i);
				tempObsHours[i] = data[0];
				tempObsData[i] = data[1];
			}

			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
