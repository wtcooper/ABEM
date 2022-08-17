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

public class CurrentDataFormatter {

	//Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
	Calendar startDate = new GregorianCalendar(2009,9,1); 
	Calendar endDate = new GregorianCalendar(2010,1,11); 
	double meanTide = 0.354; 
	int numHeaderLines = 0; 

	PrintWriter currentOut= null; //need Time 	Speed	Direction, in meters per second


	BufferedReader reader = null; 
	DecimalFormat twoDForm = new DecimalFormat("#.##");


	public static void main(String[] args) {
		CurrentDataFormatter t = new CurrentDataFormatter();
		t.step(); 
	}

	
	
	public void step(){

		try { 
			currentOut= new PrintWriter(new FileWriter("output/CurrentData.txt", true));

		} catch (IOException e) {e.printStackTrace();}


		File file = new File("dataTemp/Egmont10.1.09-01.30.10.txt"); 

		//int lastHour= 0; 
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

				String dateString = tokens[0];
				String dateTokens[] = dateString.split("/"); 
				int month = Integer.parseInt(dateTokens[0]) -1; 
				int day = Integer.parseInt(dateTokens[1]); 
				int year = Integer.parseInt(dateTokens[2]) ;
				
					

					Calendar date = new GregorianCalendar(year, month, day);
					if (startDate.before(date) && date.before(endDate) ) {

						String timeString = tokens[1]; 
						String timeTokens[] = timeString.split(":");
						int hour = Integer.parseInt(timeTokens[0]);
						int min = Integer.parseInt(timeTokens[1]); 

						double hourFrac =  ((double) hour + ((double) min/60d))  / 24d; 
						double dateWithFrac = Double.valueOf(twoDForm.format( TimeUtils.getDaysSinceTidalEpoch(date) + hourFrac));

						double current = Double.parseDouble(tokens[3]) * 0.51444444; //conver to meters per second from knots
						double angle = Math.toRadians(Double.parseDouble(tokens[4])); 
						
						double u = Math.cos(angle)* current; 
						double v= Math.sin(angle) * current; 
						
						//TODO -- need to figure out how to get time out as TecPlot -- maybe export model currents as TecPlot (TP) from ViewPlan, open and interpret
						currentOut.println(dateWithFrac + "\t" + u + "\t" + v);
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
	}

	


}
