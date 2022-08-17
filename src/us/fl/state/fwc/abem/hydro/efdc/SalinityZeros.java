package us.fl.state.fwc.abem.hydro.efdc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import us.fl.state.fwc.util.TimeUtils;

public class SalinityZeros {

	DecimalFormat twoDForm = new DecimalFormat("#.##");
	PrintWriter outWriter= null; //need Time 	Speed	Direction, in meters per second
	String outFile = "output/TemperatureValues10.1.09-01.30.10.txt";
	int numLayers = 8; 
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SalinityZeros main = new SalinityZeros();
		main.step();
	}


	public void step(){
		
		try { 
			outWriter = new PrintWriter(new FileWriter(outFile, true));
		} catch (IOException e) {e.printStackTrace();}

		Calendar startDate = new GregorianCalendar(2009,9,1); 
		Calendar endDate = new GregorianCalendar(2010,1,11); 

		while (startDate.before(endDate) ) {

			long timeStamp = Math.round(TimeUtils.getDaysSinceTidalEpoch(startDate) );
			outWriter.print(timeStamp);
			for (int i = 0 ; i< numLayers; i++){
				outWriter.print("\t" + TimeUtils.getTemperature(startDate)); 
			}
			outWriter.println(); 
			
			startDate.add(Calendar.DAY_OF_YEAR, 1); 
			
		}
		
		outWriter.close(); 
	}


}
