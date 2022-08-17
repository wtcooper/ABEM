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

import us.fl.state.fwc.util.TimeUtils;

public class BuoyDataFormatter {

	//Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
	Calendar startDate = new GregorianCalendar(2009,9,1); 
	Calendar endDate = new GregorianCalendar(2010,1,11); 
	double meanTide = 0.354; 
	int numHeaderLines = 0; 

	PrintWriter windOut= null; //need Time 	Speed	Direction, in meters per second
	PrintWriter tideOut= null; //need Time 	Height, in ft -> convert to meters
	PrintWriter pressureOut= null; //need Time 	Pressure - in hPa
	PrintWriter airTempOut= null; //need Time 	Pressure - in hPa


	BufferedReader reader = null; 
	DecimalFormat twoDForm = new DecimalFormat("#.##");


	public static void main(String[] args) {
		BuoyDataFormatter t = new BuoyDataFormatter();
		t.step(); 
	}

	
	
	public void step(){

		try { 
			windOut= new PrintWriter(new FileWriter("output/Egmont10.1.09-01.30.10WindData.txt", true));
			tideOut= new PrintWriter(new FileWriter("output/Egmont10.1.09-01.30.10TideData.txt", true));
			pressureOut= new PrintWriter(new FileWriter("output/Egmont10.1.09-01.30.10PressureData.txt", true));
			airTempOut= new PrintWriter(new FileWriter("output/Egmont10.1.09-01.30.10AirTempData.txt", true));

		} catch (IOException e) {e.printStackTrace();}


		File file = new File("dataTemp/Egmont10.1.09-01.30.10.txt"); 

		int lastHour= 0; 
		try {
			reader = new BufferedReader(new FileReader(file));
			/* First line of the data file is the header */
			for (int i=0; i<numHeaderLines; i++){
			String head = reader.readLine();
			System.out.println("Header 1: " + head);
			}

			//int counter = 0; 
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

					//#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
					int year = Integer.parseInt(tokens[0]);
					int month = Integer.parseInt(tokens[1]) -1;
					int day = Integer.parseInt(tokens[2]);
					int hour = Integer.parseInt(tokens[3]);
					
					if (! (hour == lastHour) ){ // only pull hourly data
						lastHour = hour; 
					

					Calendar date = new GregorianCalendar(year, month, day);
					if (startDate.before(date) && date.before(endDate) ) {
	
						double hourFrac =  (double) hour  / 24d; 
						double dateWithFrac = Double.valueOf(twoDForm.format( TimeUtils.getDaysSinceTidalEpoch(date) + hourFrac));

						//wind output
						if (!tokens[4].equals("MM") && !tokens[5].equals("MM") && !tokens[6].equals("MM")) {
							if(!(Double.parseDouble(tokens[4])==99.) && !(Double.parseDouble(tokens[5])==99.) && !(Double.parseDouble(tokens[6])==99.)) {
							windOut.println(/*year + "\t" + month+ "\t" + day + "\t" + */dateWithFrac + "\t" + Double.parseDouble(tokens[5])+ "\t" + Double.parseDouble(tokens[4]));
						}
					}

						//pressure
						if (!tokens[11].equals("MM") && !(Double.parseDouble(tokens[11]) == 9999.) ) {
							pressureOut.println(dateWithFrac + "\t" + Double.parseDouble(tokens[11]));
						}

						//airTemp
						if (!tokens[12].equals("MM") && !(Double.parseDouble(tokens[12]) == 999.)) {
							airTempOut.println(dateWithFrac + "\t" + Double.parseDouble(tokens[12]));
						}

						//tide
						if (!tokens[16].equals("MM") && !(Double.parseDouble(tokens[16]) == 99.)) {
							double tide = Double.parseDouble(tokens[16])*0.3048 - meanTide; // convert to meters, then adjust so is at meanTide versus MLLH 
							tideOut.println(/*year + "\t" + month+ "\t" + day + "\t" + */dateWithFrac + "\t" + tide);
						}
					}
				}
				//counter++;
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
		windOut.close(); 
		tideOut.close();
		pressureOut.close();
		airTempOut.close();
	}

	


}
