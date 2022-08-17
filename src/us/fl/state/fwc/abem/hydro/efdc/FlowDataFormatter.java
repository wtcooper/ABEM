package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.util.TimeUtils;

public class FlowDataFormatter {


	Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
	Calendar startDate = new GregorianCalendar(2010,4,25); 
	Calendar endDate = new GregorianCalendar(2010,7,18); 
	String inFile = "C:\\work\\data\\StreamFlowData\\May25_2010-Aug18_2010\\USGS02306647_SweetwaterCreek_20100525-20100818.txt"; 
	String outFile = "C:\\work\\data\\StreamFlowData\\May25_2010-Aug18_2010\\Rocky_5.25.10-8.17.10.txt"; 
	int numWaterLayers = 8; // will write out the same flow for each layer
	double convertFactor = 1.62/0.87; //1;  

	PrintWriter outWriter= null; //need Time 	Speed	Direction, in meters per second
	BufferedReader reader = null; 
	DecimalFormat twoDForm = new DecimalFormat("#.##");


	
	
	public static void main(String[] args) {
		FlowDataFormatter t = new FlowDataFormatter();
		t.step(); 
	}

	
	
	public void step(){

		startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
		endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 

		Calendar comparisonDate = (Calendar) startDate.clone(); 
		comparisonDate.add(Calendar.SECOND, (int) -(TimeUtils.SECS_PER_DAY)); // remove a day so that .before comparison below will work properly


		try { 
			outWriter = new PrintWriter(new FileWriter(outFile, true));
		} catch (IOException e) {e.printStackTrace();}


		File file = new File(inFile); 

		try {
			reader = new BufferedReader(new FileReader(file));
			/* First line of the data file is the header */
			String line = null;
			int numHeaderLines = 28; 
			for (int i=0; i<numHeaderLines; i++){
				line = reader.readLine(); 
				System.out.println(line);
			}

			for (line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				String dateString = tokens[2];
				String dateTokens[] = dateString.split("-"); 
				int year = Integer.parseInt(dateTokens[0]); 
				int month = Integer.parseInt(dateTokens[1])-1; 
				int day = Integer.parseInt(dateTokens[2]) ;

				Calendar date = new GregorianCalendar(year, month, day);
				date.setTimeZone(startDate.getTimeZone());

				if (comparisonDate.before(date) && date.before(endDate) && tokens.length>3) {

					double timeStamp = TimeUtils.getDaysSinceTidalEpoch(date);  					

					double flow = Double.parseDouble(tokens[3])*0.0283168466 * convertFactor; // need to convert to cubic meters per second 

					System.out.println(dateString + "\t" + date.getTime()); 

					outWriter.print(timeStamp);
					for (int i = 0; i<numWaterLayers; i++) {
						outWriter.print("\t" + Double.valueOf(twoDForm.format(flow))/(double)numWaterLayers ); 
					}
					outWriter.println(); 
				}
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		outWriter.close(); 
	}


	public int getDateDiffInDays(Calendar start, Calendar end){

		int timeDiff = 0;
		int startYear = start.get(Calendar.YEAR); 
		int endYear = end.get(Calendar.YEAR); 

		Calendar temp = new GregorianCalendar(startYear, 11, 31); 
		int daysInFirstYear =temp.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR); 
		timeDiff += daysInFirstYear; 

		for (int i = startYear+1; i<endYear ; i++){ // scroll through all years in between start year and endyear
			Calendar tempDate = new GregorianCalendar(i,11, 31); // set to last day of year, 
			timeDiff += tempDate.get(Calendar.DAY_OF_YEAR); 
		}

		timeDiff += end.get(Calendar.DAY_OF_YEAR); 

		return timeDiff; 
	}
}
