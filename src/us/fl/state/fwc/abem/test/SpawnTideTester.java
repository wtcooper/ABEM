package us.fl.state.fwc.abem.test;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import us.fl.state.fwc.util.TimeUtils;

public class SpawnTideTester {

	DecimalFormat twoDForm = new DecimalFormat("#.##");
	private  PrintWriter outFile = null; 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpawnTideTester main = new SpawnTideTester();
		main.step();
	}

	public void step(){


		Calendar date = new GregorianCalendar(2004,6,2, 0,0); // this is in EST 
		List<Calendar> spawnDates = new ArrayList<Calendar>();  

		//cycle through season
		for (int i = 0; i<150; i++){
			if ( TimeUtils.getMoonPhase(date)<2 || TimeUtils.getMoonPhase(date)>26 || (TimeUtils.getMoonPhase(date) >12 && TimeUtils.getMoonPhase(date)<16) ){
				spawnDates.add((Calendar) date.clone()); 
			}
			date.add(Calendar.DAY_OF_YEAR, 1); 
		}

		for (int i=0; i<spawnDates.size(); i++){
			Calendar spawnDate = spawnDates.get(i);
			int moonPhaseInt = TimeUtils.getMoonPhase(spawnDate); 
			String moonPhase = null;
			if (moonPhaseInt == 0 || moonPhaseInt == 1 || moonPhaseInt ==27) moonPhase = "new"; 
			else moonPhase = "full"; 
			
			System.out.println(spawnDate.getTime() + " moon phase: " + moonPhase);  
			spawnDate.add(Calendar.HOUR, 16); 

			for (int j=0; j<8; j++){
				System.out.println("\t\tIs " + spawnDate.get(Calendar.HOUR_OF_DAY) + "hr incoming tide??  Answer is..... " + TimeUtils.isIncomingTide(spawnDate, "data/TidalConstituents.txt"));
				spawnDate.add(Calendar.HOUR, 1); 
			}
		}
	}

	public void step2(){
		Calendar date = new GregorianCalendar(2004,4,4, 16,0); 
		System.out.println(date.getTime() + " moon phase: " + TimeUtils.getMoonPhase(date));  
		
		for (int j=0; j<8; j++){
			double hour = date.get(Calendar.HOUR_OF_DAY) ; 
			System.out.println("\t\tTide height for " + hour + "hr (" + twoDForm.format(hour/24d) +  "):\t" + twoDForm.format(TimeUtils.getTidalElevation(date, "data/TidalConstituents.txt")) + "\t\tIs this incoming?? Anwer: " + TimeUtils.isIncomingTide(date, "data/TidalConstituents.txt"));
			date.add(Calendar.HOUR, 1); 
		}

		
		
	}


}
