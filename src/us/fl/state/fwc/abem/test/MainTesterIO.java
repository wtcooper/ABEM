package us.fl.state.fwc.abem.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.util.TimeUtils;

public class MainTesterIO {

	String inFile = "dataTest/SuesMEPSLunarCalc.txt";
	String outFile = "dataTest/SuesLunarPhases.txt";

	PrintWriter out= null; //need Time 	Speed	Direction, in meters per second
	BufferedReader reader = null; 



	public void step(){

		try { 
			out= new PrintWriter(new FileWriter(outFile, true));


			File file = new File(inFile); 
			reader = new BufferedReader(new FileReader(file));
			/* First line of the data file is the header */

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				
				int year = Integer.parseInt(tokens[0]) ;
				int month = Integer.parseInt(tokens[1]) -1; 
				int day = Integer.parseInt(tokens[2]); 

				Calendar date  = new GregorianCalendar(year, month, day) ; 
				
				double moonPhase = TimeUtils.getMoonPhase(date)/7.0;
				if (moonPhase > 3.5) {
					moonPhase = 0; //if closer to next new moon than the 3rd quater
				}
				else moonPhase = Math.round(moonPhase);

			//	out.println((int)moonPhase);
				//System.out.println(year + "\t" + month + "\t" + day + "\t" + (int)moonPhase);
				System.out.println((int)moonPhase);
			}

		} catch (IOException e) {e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out.close();
		}

	}



	public static void main(String[] args) {

		MainTesterIO mt = new MainTesterIO(); 
		mt.step(); 

	}
	public static int getLineNumber() {
		return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}

	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

	public static String getClassName() {
		return Thread.currentThread().getStackTrace()[2].getClassName();
	}
}
