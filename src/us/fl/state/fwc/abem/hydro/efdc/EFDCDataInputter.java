package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.commons.math.geometry.Vector3D;

import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TestingUtils;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;

public class EFDCDataInputter {

	int startYear = 2010;
	int startMonth = 4;
	int startDay = 0;
	int endYear = 2010;
	int endMonth = 10;
	int endDay = 1;

	int numDepthLayers = 8; 

	String directory = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";

	String baseOutput = directory; //"C:\\work\\data\\EFDC\\temp\\"; 
	String ncomInput = "C:\\work\\data\\NCOM_AS\\";

	String EFDCCornersFile = directory + "corners.inp";
	String EFDCBoundaryCellsFile = directory + "EFDCBoundCells.txt";

		String tidalConstituentFile = "C:\\work\\data\\ABEMData\\ClearwaterBeachTidalConstituents.txt";
	double tidalNAVDDatum = 1.064; 

	Calendar NCOMStartDate = new GregorianCalendar(2010, 5-1, 25); 

	ArrayList<Double> windObsTime = new ArrayList<Double>(); 
	ArrayList<double[]> windSpeedData = new ArrayList<double[]>();
	ArrayList<double[]> windDirData = new ArrayList<double[]>();
	ArrayList<double[]> tideData = new ArrayList<double[]>();

/*	public EFDCDataInputter (int startYear,	int startMonth, int startDay, int endYear, int endMonth, int endDay){

		this.startYear = startYear;
		this.startMonth = startMonth;
		this.startDay=startDay;
		this.endYear = endYear;
		this.endMonth = endMonth;
		this.endDay = endDay; 
	}		
*/
	public static void main(String[] args) {
		EFDCDataInputter e = new EFDCDataInputter(); //(2010, 5, 25, 2010, 9, 8);
		System.out.println("**** getting wind data ****");
		e.getWindData();
		System.out.println("**** getting surface elevation data ****");
		e.getInitialTidalElevations();
		System.out.println("**** getting stream flow data ****");
		e.getStreamFlowData();
	}






	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// GET WIND DATA
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||


	/*Pulls wind data from Egmont Key buoy off COMPS site using html access
	 * Outputs to pre-formated wser.inp
	 */
	public void getWindData(){

		NumberFormat timeNF = new DecimalFormat("#.00");
		NumberFormat valueNF = new DecimalFormat("#0.0000");


		String windSpeedURL = "http://comps.marine.usf.edu/index?view=data&id=EGK&data[]=WSR&" +
		"start_month=" + startMonth + "&start_day=" + startDay + "&start_year=" + startYear+ "&start_hour=0&" +
		"end_month=" + endMonth + "&end_day=" + endDay + "&end_year=" + endYear + "&end_hour=0&tz=0&method=View+Data";

		String windDirURL = "http://comps.marine.usf.edu/index?view=data&id=EGK&data[]=WDR&" +
		"start_month=" + startMonth + "&start_day=" + startDay + "&start_year=" + startYear+ "&start_hour=0&" +
		"end_month=" + endMonth + "&end_day=" + endDay + "&end_year=" + endYear + "&end_hour=0&tz=0&method=View+Data";

		Calendar tempDate; 
		double daysSinceEpoch;

		//holds all the data for an observation, then outputs at once so can tabulate the total num of measurements

		try {

			new File(baseOutput + "wser.inp").delete(); //delete if existing
			PrintWriter outFile = new PrintWriter(new FileWriter(baseOutput + "wser.inp", true));
			//output the header for pser.inp
			outFile.println("C ** , wser.inp Time Series FILE,  DDD " + new Date(System.currentTimeMillis()).toString() + ", manually created through script by W. Cooper");
			outFile.println("C **");
			outFile.println("C **  WIND FORCING FILE, USE WITH 7 APRIL 97 AND LATER VERSIONS OF EFDC");
			outFile.println("C **");
			outFile.println("C **  MASER(NW)     =NUMBER OF TIME DATA POINTS");
			outFile.println("C **  TCASER(NW)    =DATA TIME UNIT CONVERSION TO SECONDS");
			outFile.println("C **  TAASER(NW)    =ADDITIVE ADJUSTMENT OF TIME VALUES SAME UNITS AS INPUT TIMES");
			outFile.println("C **  WINDSCT(NW)   =WIND SPEED CONVERSION TO M/SEC");
			outFile.println("C **  ISWDINT(NW)   =DIRECTION CONVENTION");
			outFile.println("C **               0 DIRECTION TO");
			outFile.println("C **               1 DIRECTION FROM");
			outFile.println("C **               2 WINDS IS EAST VELOCITY, WINDD IS NORTH VELOCITY");
			outFile.println("C **");
			outFile.println("EE    EFDC_DS_WIND_HEIGHT (m):10.0");
			outFile.println("C **");
			outFile.println("C **  MASER  TCASER   TAASER  WINDSCT  ISWDINT");
			outFile.println("C **  TASER(M) WINDS(M) WINDD(M)");


			//|||||||||||||||||||||||||||||||||||||||||| Wind Speed Read ||||||||||||||||||||||||||||||||||||||||||
			URL url = new URL(windSpeedURL);
			BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
			String inputLine;

			boolean isHeader = true;

			while ((inputLine = input.readLine()) != null) {
				String tokens[] = inputLine.split("[ ]+");
				if (tokens[0].equals("YYYY-MM-DD")) {
					isHeader=false;
					continue; 
				}

				if (!isHeader){
					String dateTokens[] = tokens[0].split("-");
					String timeTokens[] = tokens[1].split(":");
					double speed = Double.parseDouble(tokens[2]);
					tempDate = new GregorianCalendar(Integer.parseInt(dateTokens[0]), Integer.parseInt(dateTokens[1])-1, Integer.parseInt(dateTokens[2]), Integer.parseInt(timeTokens[0]), Integer.parseInt(timeTokens[1]));
					tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
					daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(tempDate);
					double[] dataArr = {daysSinceEpoch, speed}; 
					windSpeedData.add(dataArr);  
				}
			}
			input.close();

			//|||||||||||||||||||||||||||||||||||||||||| Wind Dir Read ||||||||||||||||||||||||||||||||||||||||||
			url = new URL(windDirURL);
			input = new BufferedReader(new InputStreamReader(url.openStream()));

			isHeader = true;

			while ((inputLine = input.readLine()) != null) {
				String tokens[] = inputLine.split("[ ]+");
				if (tokens[0].equals("YYYY-MM-DD")) {
					isHeader=false;
					continue; 
				}

				if (!isHeader){
					String dateTokens[] = tokens[0].split("-");
					String timeTokens[] = tokens[1].split(":");
					double dir = Double.parseDouble(tokens[2]);
					
					//need to convert the dir to oceanographic versus meteorologic format, since EFDC takes in a value of the direction the wind is blowing and not coming from
					dir += 180;
					if (dir>360) dir -= 360; 
					if ( (dir > 360) || (dir <0) ){
						System.out.println("wind angle is wrong; exiting system");
						TestingUtils.dropBreadCrumb();
						System.exit(1); 
					}

					tempDate = new GregorianCalendar(Integer.parseInt(dateTokens[0]), Integer.parseInt(dateTokens[1])-1, Integer.parseInt(dateTokens[2]), Integer.parseInt(timeTokens[0]), Integer.parseInt(timeTokens[1]));
					tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
					daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(tempDate);
					double[] dataArr = {daysSinceEpoch, dir}; 
					windDirData.add(dataArr); 
					windObsTime.add(daysSinceEpoch); 
				}
			}
			input.close();



			//|||||||||||||||||||||||||||||||||||||||||| Remove duplicates ||||||||||||||||||||||||||||||||||||||||||

			//Check to make sure data arrays are the same
			
			if ((windSpeedData.size() != windDirData.size()) &&  windDirData.size() != windObsTime.size()){
				System.out.println("speed and direction data not the same size!!");
				System.exit(1);
			}

			
			//Loop through data and pull out repeats since the time, when formated to 2 decimal places at every 6 minutes, ends up with a single duplicate
			int counter = windObsTime.size(); 
			for (int i=1; i< counter; i++){
				String time = timeNF.format(windObsTime.get(i).doubleValue());
				String lastTime = timeNF.format(windObsTime.get(i-1).doubleValue());

				//if the last value is the same as the current value, then remove the value, and decrement the counter and the i
				if (time.equals(lastTime)){
					windObsTime.remove(i);
					windDirData.remove(i);
					windSpeedData.remove(i);
					counter--; 
					i--;
				}

				//NOMADS data is crap and doesn't match up well to Egmont Key data
				/*				else {
					Calendar date = TimeUtils.getDateFromDaysSinceTidalEpoch(windObsTime.get(i));
					double[] NOMADS = this.getNOMADSWindData(date); 
					
					System.out.println("Egmont mag: " + windSpeedData.get(i)[1] + "\tNOMADS mag: " + NOMADS[0] + "\tegmont dir: " + windDirData.get(i)[1] + "\tNOMADS dir: " + NOMADS[1]	);
				}
*/ 
			}

			//|||||||||||||||||||||||||||||||||||||||||| Print out data ||||||||||||||||||||||||||||||||||||||||||


			String headBlanks = "        ";
			String dataSize = new Integer(windSpeedData.size()).toString();
			dataSize = headBlanks.substring(0, (headBlanks.length())-dataSize.length()) + dataSize;   
			outFile.println(dataSize + "   86400       0       1       0 ' *** wind");

			String valueBlanks = "          ";
			for (int i=0; i<windSpeedData.size(); i++){
				double[] speedArr = windSpeedData.get(i);
				double[] dirArr = windDirData.get(i);

				if (speedArr[1] == -99.999 || dirArr[1] == -99.999) {
					counter = i;
					while (windSpeedData.get(counter++)[1] == -99.999 || windSpeedData.get(counter++)[1] == -99.999){


					}

					//TODO -- Check if NEXT measured wind velocity is greater than 1hr in the future (i.e., diff between speedArr[0] and windSpeedData.get(i-1)[0] ), 
					// and if so, get the most current wind measurement from Tampa International Airport to put in between, if can get the data 
				}
				else {
					String time = timeNF.format(speedArr[0]);
					String speed = valueNF.format(speedArr[1]);
					String dir = valueNF.format(dirArr[1]); 
					outFile.println(valueBlanks.substring(0, (valueBlanks.length()) - time.length()) + time + valueBlanks.substring(0, (valueBlanks.length()) - speed.length()) + speed + valueBlanks.substring(0, (valueBlanks.length()) - dir.length()) + dir );	
					
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}




	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// GET NOMADS WIND DATA 
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	/*Get's the NOAA NOMADS wind data for a Calendar date with hour/minute resolution
	 * Note: NOMADS data use here is 6 hourly time step, so must provide a Calendar with hour and minutes to get appropriate time.
	 */

	public double[] getNOMADSWindData(Calendar date){
		
		NumberFormat nf = NumberFormat.getInstance(); 
		nf.setMinimumIntegerDigits(2); 
		Coordinate egmontCoord = new Coordinate(-82.761 + 360, 27.591);

		double[] data = new double[2]; 

		//http://nomads.ncdc.noaa.gov/thredds/dodsC/oceanwindsfilesys/SI/uv/6hrly/netcdf/2000s/uv20100801rt.nc
		String baseURL = "http://nomads.ncdc.noaa.gov/thredds/dodsC/oceanwindsfilesys/SI/uv/6hrly/netcdf/2000s/";
		NetCDFFile ncFile = null;
		int month = date.get(Calendar.MONTH) + 1;  
		int day = date.get(Calendar.DATE);  
		int year = date.get(Calendar.YEAR);
		int hour = date.get(Calendar.HOUR_OF_DAY);
		int min = date.get(Calendar.MINUTE);
		double timeVal = hour+(double)min/60.0; 
		int timeIndex = 0;
		if (timeVal < 3) timeIndex = 0;
		else if (timeVal >=3 && timeVal <9) timeIndex = 1;
		else if (timeVal >=9 && timeVal <15) timeIndex = 2;
		else if (timeVal >=15 && timeVal <21) timeIndex = 3;
		else {
			timeIndex = 0; 
			day++; //increase the day so that pulls the next netCDF file
		}
		
		String fileName = baseURL + "uv" + year + nf.format(month) + nf.format(day) + "rt.nc"; 
		
		//open the file and catch error if doesn't exist for missing days of data
		try {
			ncFile = new NetCDFFile(fileName);
		} catch (IOException e) {
			System.out.println("couldn't open NetCDFFile"); 
			TestingUtils.dropBreadCrumb();
			System.exit(1); 
		}

		//DONT interpolate -- resolution is too course and leads to error
		//ncFile.setInterpolationAxes("lat", "lon");
		ncFile.setVariables("time", "zlev", "lat", "lon", "u", "v"); 

			double u = ncFile.getValue("u", new double[] {timeIndex, 0, egmontCoord.y, egmontCoord.x}, new boolean[] {true, true, false, false}, false).doubleValue() ;
			double v = ncFile.getValue("v", new double[] {timeIndex, 0, egmontCoord.y, egmontCoord.x}, new boolean[] {true, true, false, false}, false).doubleValue() ;

			Vector3D vec = new Vector3D(u, v, 0); 
			data[0] = vec.getNorm(); //magnitude
			data[1] = Math.toDegrees(vec.getAlpha()); //angle
			
		return data;
	}






	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// GET STREAM FLOW DATA
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	/*Pulls streamflow data from USGS guages using html access.
	 * Outputs to pre-formated qser.inp file
	 */
	public void getStreamFlowData(){

		NumberFormat nf = NumberFormat.getInstance(); 
		nf.setMinimumIntegerDigits(2); 
		NumberFormat timeNF = new DecimalFormat("#.00");
		NumberFormat valueNF = new DecimalFormat("#0.0000");


		//Hashmap includes: key = name, Object[0]=gaugeNum, Object[1]=area, Object[3]=long-term flow avg 1950-present, where data are available
		HashMap<String, Object[]> gaugeNums = new HashMap<String, Object[]>();
		gaugeNums.put("AlafiaRiver", new Object[] {"02301500", new Double(418), getFlowLongTermAvg("02301500")});
		gaugeNums.put("BullfrogCreek", new Object[] {"02300700", new Double(40.3), getFlowLongTermAvg("02300700")});
		gaugeNums.put("DelaneyCreek", new Object[] {"02301750", new Double(23.7), getFlowLongTermAvg("02301750")});
		gaugeNums.put("HillsboroughRiver", new Object[] {"02304500", new Double(650), getFlowLongTermAvg("02304500")});
		gaugeNums.put("ManateeRiver", new Object[] {"02299950", new Double(1), getFlowLongTermAvg("02299950")}); //NOTE: Meyers et al. doesn't have Manatee River listed, so i've added it in 
		gaugeNums.put("LittleManatee", new Object[] {"02300500", new Double(222), getFlowLongTermAvg("02300500")});
		gaugeNums.put("SulphurSprings", new Object[] {"02306000", new Double(1), getFlowLongTermAvg("02306000")});
		gaugeNums.put("RockyCreek", new Object[] {"02307000", new Double(35), getFlowLongTermAvg("02307000")});
		gaugeNums.put("SweetwaterCreek", new Object[] {"02306647", new Double(37.3), getFlowLongTermAvg("02306647")});
		gaugeNums.put("WardLakeOutfall", new Object[] {"02300042", new Double(59.5), getFlowLongTermAvg("02300042")});



		HashMap<String, ArrayList<double[]>> data = new HashMap<String, ArrayList<double[]>>(); 
		ArrayList<String> streamNamesOrdered = new ArrayList<String>(); 

		Calendar tempDate; 
		double daysSinceEpoch = 0;


		try {

			new File(baseOutput + "qser.inp").delete(); //delete if existing
			PrintWriter outFile = new PrintWriter(new FileWriter(baseOutput + "qser.inp", true));

			outFile.println("C ** , qser.inp Time Series FILE,  DDD  " + new Date(System.currentTimeMillis()).toString() + ", manually created through script by W. Cooper");
			outFile.println("C **");
			outFile.println("C **  InType MQSER(NS) TCQSER(NS) TAQSER(NS) RMULADJ(NS) ADDADJ(NS) ICHGQS");
			outFile.println("C **");
			outFile.println("C **  IF InType.EQ.1 THEN READ DEPTH WEIGHTS AND SINGLE VALUE OF QSER");
			outFile.println("C **                      ELSE READ A VALUE OF QSER FOR EACH LAYER");
			outFile.println("C **  ");
			outFile.println("C **  InType=1 Structure");
			outFile.println("C **  WKQ(K),K=1,KC");
			outFile.println("C **  TQSER(M,NS)    QSER(M,1,NS)          !(MQSER(NS) PAIRS FOR NS=1,NQSER SERIES)");
			outFile.println("C **");
			outFile.println("C **  InType=0 Structure");
			outFile.println("C **  TQSER(M,NS)    (QSER(M,K,NS),K=1,KC) !(MQSER(NS) PAIRS)");
			outFile.println("C **");

			BufferedReader reader = new BufferedReader(new FileReader(new File("C:\\work\\data\\StreamFlowData\\MeyersEtAlFlowTable.txt")));
			reader.readLine(); //read the header line
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {

				ArrayList<double[]> streamData = new ArrayList<double[]>(); 
				String tokens[] = line.split("\t"); 
				String streamName = tokens[2]; 
				streamNamesOrdered.add(streamName); 
				double avgFlowMeyers = Double.parseDouble(tokens[3]); 
				String gaugeName = tokens[5];
				double streamArea = Double.parseDouble(tokens[4]);
				double avgFlowU = 0; 
				String gaugeNum = null;
				double sourceArea =0; 

				if (streamName.equals("AlafiaRiver")){
					System.out.println(); 
				}
				//if the gauge source name isn't in the gaugeNums hashmap (i.e., for WWTP), then set the daily flow to average flow from start to end of time period
				if (! gaugeNums.containsKey(gaugeName)){
					Calendar startDate = new GregorianCalendar(startYear, startMonth-1, startDay); // 2010-6-1:  this is JULY 1st!
					Calendar endDate = new GregorianCalendar(endYear, endMonth-1, endDay); 
					startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
					endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

					while (startDate.before(endDate)){
						daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(startDate);
						double[] dataArr = {daysSinceEpoch, avgFlowMeyers}; 
						streamData.add(dataArr);  
						startDate.add(Calendar.DAY_OF_YEAR, 1); 
					}

				}

				//open up http URL and download data for the given stream/river
				else {
					gaugeNum = (String) gaugeNums.get(gaugeName)[0];
					sourceArea = ((Double) gaugeNums.get(gaugeName)[1]).doubleValue();

					//get Fu as per Meyers et al. 2007 (eq 3)
					avgFlowU = 1.2*(streamArea/sourceArea)*((Double) gaugeNums.get(gaugeName)[2]).doubleValue();
					//|||||||||||||||||||||||||||||||||||||||||| Read data from URL ||||||||||||||||||||||||||||||||||||||||||

					String startString = startYear + "-" + nf.format(startMonth) + "-" + nf.format(startDay); 
					String endString = endYear + "-" + nf.format(endMonth) + "-" + nf.format(endDay); 

					String streamURL = "http://waterdata.usgs.gov/nwis/dv?cb_00060=on&format=rdb&begin_date=" + startString + "&end_date=" + endString + "&site_no=" + gaugeNum + "&referred_module=sw";
					URL url = new URL(streamURL);
					BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
					String inputLine; 
					while ((inputLine = input.readLine()) != null) {
						String streamTokens[] = inputLine.split("\t");
						if (streamTokens[0].equals("USGS")) {

							if (streamTokens.length == 5) {
								double flow =0;

								flow = Double.parseDouble(streamTokens[3])*0.0283168466 ; //convert from ft^3/s to m^3/s
								flow = 1.2*flow*(streamArea/sourceArea)+0.081*avgFlowU; //convert to account for higher flow near mouth of river in urban areas than in headwaters (as per Meyers et al. 2007 eq. 1)

								String dateTokens[] = streamTokens[2].split("-");
								tempDate = new GregorianCalendar(Integer.parseInt(dateTokens[0]), Integer.parseInt(dateTokens[1])-1, Integer.parseInt(dateTokens[2]));
								tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
								daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(tempDate);
								double[] dataArr = {daysSinceEpoch, flow}; 
								streamData.add(dataArr);  

							}

							else {	
								//if missing data, then set to avgFlowMeyers
								daysSinceEpoch++;
								double[] dataArr = {daysSinceEpoch, avgFlowMeyers}; 
								streamData.add(dataArr);  

							}
						} // end of check to make sure record begins with USGS so only are assessing data entries
					}// end of looping through all records in URL address
					input.close();
					
					Calendar temp = TimeUtils.getDateFromDaysSinceTidalEpoch(daysSinceEpoch); 
					Calendar endDate = new GregorianCalendar(endYear, endMonth-1, endDay); 
					temp.setTimeZone(TimeZone.getTimeZone("GMT")); 
					endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

					while (temp.before(endDate)){
						daysSinceEpoch++;
						double[] dataArr = {daysSinceEpoch, avgFlowMeyers}; 
						streamData.add(dataArr);  
						temp.add(Calendar.DAY_OF_YEAR, 1);
					}
					
				} //end of else() statement to check if the source gauge name is in gaugeNums 
				data.put(streamName, streamData); 
			}//end of looping through MeyersEtAlFlowTable


			//||||||||||||||||||||||||||||||||||||||||||||||||||||
			// PRINT OUT DATA
			//||||||||||||||||||||||||||||||||||||||||||||||||||||

			//TODO -- need to get the same number of days for each, so NEED to fill in missing values



			//       0    1095   86400       0       1       0       0 ' *** Alafia
			//   7670.00    0.4250    0.4250    0.4250    0.4250    0.4250    0.4250    0.4250    0.4250

			for (int i=0; i<streamNamesOrdered.size(); i++){
				String streamName = streamNamesOrdered.get(i); 
				ArrayList<double[]> streamData = data.get(streamName); 

				String headBlanks = "        ";
				String dataSize = new Integer(streamData.size()).toString();
				dataSize = headBlanks.substring(0, (headBlanks.length())-dataSize.length()) + dataSize;   
				outFile.println("       0" + dataSize + "   86400       0       1       0       0 ' *** " + streamName);

				String valueBlanks = "          ";

				for (int j=0; j< streamData.size(); j++){

					double[] obs = streamData.get(j);
						String time = timeNF.format(obs[0]);
						String flow = valueNF.format(obs[1]/(double) numDepthLayers); //divide by numDepthLayers to evenly distribute among layers
						
						outFile.print(valueBlanks.substring(0, (valueBlanks.length()) - time.length()) + time); 
						//loop through all depth layers and 
						for (int k = 0; k<numDepthLayers; k++){
							outFile.print(valueBlanks.substring(0, (valueBlanks.length()) - flow.length()) + flow);
						}
						outFile.println();

				} // end loop over all entries for a stream
			}//end loop over all streams



			outFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}





	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// GET AVERAGE LONG TERM FLOW RATE
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	/*This returns the average long term flow rate, Fg (as per Meyers et al 2007 eq. 2)
	 * 
	 */
	public Double getFlowLongTermAvg(String gaugeNum) {

		System.out.println("Getting average long-term flow data for gauge # " + gaugeNum); 

		NumberFormat nf = NumberFormat.getInstance(); 
		nf.setMinimumIntegerDigits(2); 

		double avg = 0; 
		String startString = "1950-01-01";
		Calendar today = new GregorianCalendar();
		today.setTimeInMillis(System.currentTimeMillis());
		today.setTimeZone(TimeZone.getTimeZone("GMT"));
		int year = today.get(Calendar.YEAR);
		int month = today.get(Calendar.MONTH)-1;
		int day = today.get(Calendar.DAY_OF_MONTH);
		int totalObs = 0; 

		String endString = year + "-" + nf.format(month) + "-" + nf.format(day); 
		String streamURL = "http://waterdata.usgs.gov/nwis/dv?cb_00060=on&format=rdb&begin_date=" + startString + "&end_date=" + endString + "&site_no=" + gaugeNum + "&referred_module=sw";

		URL url;
		try {
			url = new URL(streamURL);
			BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
			String inputLine; 
			while ((inputLine = input.readLine()) != null) {
				String streamTokens[] = inputLine.split("\t");
				if (streamTokens[0].equals("USGS")) {

					if (streamTokens.length == 5) {
						try {
							avg += Double.parseDouble(streamTokens[3])*0.0283168466 ; //convert from ft^3/s to m^3/s
							totalObs++;
						}catch (NumberFormatException e) {
							System.out.println("not a double value at token: " + streamTokens[3] + " for line: " + inputLine);
						} 

					}

				}
			}
			input.close();

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		avg = (avg/(double)totalObs);  //
		Double value = new Double(avg); 
		return value; 
	}









	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	// GET TIDE DATA
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	/*Pulls surface elevation boundary cell conditions data from NCOM model
	 * Outputs to pre-formatted pser.inp file
	 * 
	 * If values are before start of NCOM time series, then will use the value from the Clearwater tidal constituent
	 *  
	 *   All values are referenced to NAVD-88 which is what NCOM and bathymetry are set to
	 */
	public void getInitialTidalElevations() {

		NCOMStartDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

		NumberFormat nf = NumberFormat.getInstance(); 

		NumberFormat timeNF = new DecimalFormat("#.00");
		NumberFormat valueNF = new DecimalFormat("#0.0000");

		NetCDFFile ncFile = null; 
		String dateURL;
		String baseFile = ncomInput;
		String nameURL = "ncom_relo_amseas_";

		Calendar tempDate; 
		double daysSinceEpoch;

		Double[] windObs = windObsTime.toArray(new Double[windObsTime.size()]); 


		//holds all the data for an observation, then outputs at once so can tabulate the total num of measurements

		try {

			new File(baseOutput + "pser.inp").delete(); //delete if existing
			PrintWriter outFile = new PrintWriter(new FileWriter(baseOutput + "pser.inp", true));
			//output the header for pser.inp
			outFile.println("C ** , pser.inp Time Series FILE,  DDD " + new Date(System.currentTimeMillis()).toString() + ", manually created through script by W. Cooper"); // 8/20/2010 1:04:20 PM
			outFile.println("C **   REPEATS NPSER TIMES");
			outFile.println("C **");
			outFile.println("C **   MPSER(NS)   TCPSER(NS)  TAPSER(NS)  RMULADJ(NS)  ADDADJ(NS)");
			outFile.println("C **");
			outFile.println("C **   TPSER(M,NS)  PSER(M,1,NS)   !(mpser(ns) pairs for ns=1,npser series))");


			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all boundary condition cells |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

			EFDCGrid grid = new EFDCGrid(EFDCCornersFile, true); 

			//Read in an ASCI file which simply has I \t J values for the boundary cells
			BufferedReader reader = new BufferedReader(new FileReader(new File(EFDCBoundaryCellsFile)));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {

				Calendar startDate = new GregorianCalendar(startYear, startMonth-1, startDay); // 2010-6-1:  this is JULY 1st!
				Calendar endDate = new GregorianCalendar(endYear, endMonth-1, endDay); 
				startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
				endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

				String tokens[] = line.split("\t"); //split("[ ]+"); 
				Int3D index = new Int3D();
				index.x = Integer.parseInt(tokens[0]);
				index.y = Integer.parseInt(tokens[1]);

				//add the grid cell to the boundCells
				Coordinate centroid = grid.getGridCells().get(index).getCentroidCoord();
				//convert from UTM to lat/lon
				CoordinateUtils.convertUTMToLatLon(centroid, 17, false);

				//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all days to record |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 

				while (startDate.before(endDate)) {


					int month = startDate.get(Calendar.MONTH)+1;  
					int day = startDate.get(Calendar.DATE);  
					int year = startDate.get(Calendar.YEAR);

					nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
					dateURL = year+nf.format(month)+nf.format(day)+"00"; 

					//===========================================================
					//===============if before NCOM start date, use harmonic constituents  
					//===========================================================
					if (startDate.before(NCOMStartDate)){

						//loop through a day, every 8 hrs
						for (int i=0; i<24; i = i+3){
							tempDate = new GregorianCalendar(year, month-1, day, i, 0);
							tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
							daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(tempDate);
							double surfEl = TimeUtils.getTidalElevation(tempDate, tidalConstituentFile) - tidalNAVDDatum; //substract the  

							//System.out.println("\t" + daysSinceEpoch + "\t" + surfEl);
							double[] dataArr = {daysSinceEpoch, surfEl}; 
							tideData.add(dataArr); 
						}
					}


					//===========================================================
					//===============else, use the NCOM surface elevation data  
					//===========================================================

					else {
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


						//===============loop over all time entries in a netCDF file==================  
						//set the start time HOUR
						int time = (int) Math.round(ncFile.getValue("time", new int[] {0}).doubleValue());

						int counter = 0; 
						for (int i=0; i<ncFile.getSingleDimension("time"); i++){
							double surfEl = ncFile.getValue("surf_el", new double[] {time, centroid.y, centroid.x}, new boolean[] {false, false, false}, true).doubleValue();

							tempDate = new GregorianCalendar(year, month-1, day, counter, 0);
							tempDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
							daysSinceEpoch = TimeUtils.getDaysSinceTidalEpoch(tempDate);

							/*Scale the surface elevation based on the wind as per Weisberg and Zheng 2006:
							 * 
							 * NOTE: THE wind direction data is scaled to represent where it's blowing to for EFDC, 
							 * but in Weisberg and Zheng, it's assummed they are talking about where wind is from based
							 * on their equation below
							 * 
							 * for wind direction omega = 110-230:		surfEl =  alpha*8.0*|speed|*sin( ((omega-110)/120)*PI)
							 * for wind direction omega = -30-90:			surfEl = -alpha*8.0*|speed|*sin( ((omega+30)/120)*PI)
							 * 
							 * where alpha = 0.0025
							 */
							int windIndex = locate(windObs, new Double(daysSinceEpoch)); 
							double windDir = windDirData.get(windIndex)[1] - 180;
							if (windDir < 0) windDir += 360 ; //rescale so represents where wind is cvoming from; 
							double windSpeed = windSpeedData.get(windIndex)[1];

							double tempDir = 0; 
							if (windDir > 270 && windDir <360) tempDir = -(360-windDir); //convert the 4th quadrant to negative value
							else tempDir = windDir;

							//as per equation (1) in Weisberg and Zheng 2006 (J. of Geophysical Research v 111, C01005)
							if (tempDir > 110 && tempDir < 230) surfEl += 0.0025*8.0*Math.abs(windSpeed)*Math.sin( ((tempDir-110)/120)*Math.PI);
							else if (tempDir > -30 && tempDir < 90) surfEl += -0.0025*8.0*Math.abs(windSpeed)*Math.sin( ((tempDir+30)/120)*Math.PI);

							double[] dataArr = {daysSinceEpoch, surfEl}; 
							tideData.add(dataArr); 

							time += 3; 
							counter += 3; 
						}
						ncFile.closeFile();
					} //end of else() for if startDate.before(NCOMStartDate)

					startDate.add(Calendar.DAY_OF_YEAR, 1); // add a day to the date
				}//end of looping through all days for a single boundary cell


				//PRINT out the data
				//first print the cell identifier line
				//     656   86400       0       1       0 ' *** 3_25
				//     656   86400       0       1       0 ' *** 11_15
				//  10006.00   -0.3199
				//  10006.13   -0.1079

				String headBlanks = "        ";
				String dataSize = new Integer(tideData.size()).toString();
				dataSize = headBlanks.substring(0, (headBlanks.length())-dataSize.length()) + dataSize;   
				outFile.println(dataSize + "   86400       0       1       0 ' *** " + index.x + "_" + index.y);

				String valueBlanks = "          ";
				for (int i=0; i<tideData.size(); i++){
					double[] dataArr = tideData.get(i);
					String time = timeNF.format(dataArr[0]);
					String value = valueNF.format(dataArr[1]);
					outFile.println(valueBlanks.substring(0, (valueBlanks.length()) - time.length()) + time + valueBlanks.substring(0, (valueBlanks.length()) - value.length()) + value );
				}

				//need to clear tide data here, else will keep adding to
				tideData.clear();
				
			} // end of reading through all boundary cells
			outFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}




	/*Locates and returns the index of an array where the value is closest too
	 * 
	 */
	public int locate(Double[] ja, Double val){
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


}




