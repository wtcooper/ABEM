package us.fl.state.fwc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class TimeUtils {

	public static long SECS_PER_YEAR = (long) ((double) 60*60*24*365.25); 
	public static long SECS_PER_DAY = 60*60*24; 
	public static long MILLISECS_PER_DAY = 1000*60*60*24; 


	
	
	/**Returns a new instance of a GregorianCalendar with GMT time zone.
	 * E.g. usage:
	 * 
	 * TimeUtils.getNewCalendar(2010, 1, 1, 0, 0) would return a new date of 
	 * Jan 1st, 2010 0:00am
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param min
	 * @return
	 */
	public static GregorianCalendar getNewCalendar(int year, int month, int day, int hour, int min){
		GregorianCalendar cal = new GregorianCalendar(year, month-1, day, hour, min);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		return cal;
	}


	/**	Returns the phase of the moon as an integer from 0-7, where newMoon=0, and fullMoon=4.
	 * Calculation algorithm from http://www.voidware.com/moon_phase.htm, and tested against http://home.att.net/~srschmitt/script_moon_phase.html
	 * 
	 * @param currentTime - current time in seconds since start of simulation 
	 * @param scheduler - the simulation scheduler
	 * @return - phase of moon as int from 0-7 
	 */
	public static int getMoonPhase_8(Calendar currentDate) {
		/*
	      calculates the moon phase (0-7), accurate to 1 segment.
	      0 = > new moon.
	      4 => full moon.
		 */

		int y, m, d; 


		y = currentDate.get(Calendar.YEAR);
		m = currentDate.get(Calendar.MONTH); 
		d = currentDate.get(Calendar.DATE); 

		double c,e;
		double jd;
		int b;

		if (m < 2) {
			y--;
			m += 12;
		}
		++m;
		c = 365.25*y;
		e = 30.6*m;
		jd = c+e+d-694039.09;  /* jd is total days elapsed */
		jd /= 29.53;           /* divide by the moon cycle (29.53 days) */
		b = (int) jd;		   /* int(jd) -> b, take integer part of jd */
		jd -= b;		   /* subtract integer part to leave fractional part of original jd */
		b = (int) (jd*8 + 0.5);	   /* scale fraction from 0-8 and round by adding 0.5 */
		b = b & 7;		   /* 0 and 8 are the same so turn 8 into 0 */
		return b;
	}



	/**	Returns the phase of the moon as an integer from 0-27, where newMoon=0, and fullMoon=14.
	 * This gives values very close to the getMoonPhase_Complex() method, using a different and more complex algorithm
	 * Calculation algorithm here from http://www.voidware.com/moon_phase.htm, and tested against multiple online calculators -- works to the day
	 * 
	 * @param currentDate - current date of the simulation 
	 * @return - phase of moon as int from 0-27 
	 */

	public static int getMoonPhase(Calendar currentDate) {


		int y = currentDate.get(Calendar.YEAR);
		int m = currentDate.get(Calendar.MONTH); 
		int d = currentDate.get(Calendar.DATE); 
		double hr = ((double) currentDate.get(Calendar.HOUR))/24; //fraction of a day 


		double c,e;
		double jd;
		int b;

		if (m < 2) {
			y--;
			m += 12;
		}
		++m;
		c = 365.25*y;
		e = 30.6*m;
		jd = c+e+d + hr-694039.09;  /* jd is total days elapsed */
		jd /= 29.530588853;           /* divide by the moon cycle (29.53 days) */
		b = (int) jd;		   /* int(jd) -> b, take integer part of jd */
		jd -= b;		   /* subtract integer part to leave fractional part of original jd */
		b = (int) (jd*28 + 0.5);	   /* scale fraction from 0-8 and round by adding 0.5 */
		//**NOTE -- need to scale to an even number so that will have a mid day 
		if (b == 28) b = 0;		   /* 0 and 8 are the same so turn 8 into 0 */
		return b;
	}


	/**	Returns the moon phase, from 0-1, where 0=new and 1=full.
	 *  This is based on calculations given in the book Duffett-Smith, Peter. 1988. Practical Astronomy with Your Calculator, 3rd Ed. Cambridge Univ. Press, 
	 *  code adapted from Janet L. Stein Carter (University of Cincinnati-Clermont College) at http://biology.clc.uc.edu/steincarter/moon/moon%20code.htm
	 * 
	 * @param currentDate
	 * @return
	 */
	public static double getMoonPhase_Complex(Calendar currentDate){
		double Epoch = 2447891.5;
		int yyy = currentDate.get(Calendar.YEAR);
		int mmm = currentDate.get(Calendar.MONTH); 
		int ddd = currentDate.get(Calendar.DATE); 
		int hhh= currentDate.get(Calendar.HOUR);
		int mmn = currentDate.get(Calendar.MINUTE);

		double FractDay = hhh / 24;
		FractDay += mmn / 60 / 24;

		// this calculates the Julian Date
		if (mmm < 2) {  // if 1 or 2 (this has been changed to 0-base)
			yyy--;
			mmm += 12;
		}
		double BtoJD = 0, CtoJD=0;  
		if ((yyy >= 1583) || (yyy == 1582 && mmm > 10) || (yyy == 1582 && mmm == 10 && ddd >= 15)) {
			// if it's after Gregory changed the calendar
			double AtoJD = Math.floor(yyy / 100);
			BtoJD = 2 - AtoJD + Math.floor(AtoJD / 4);
		} else {
			BtoJD = 0;
		}
		if (yyy < 0) {
			CtoJD = Math.ceil((365.25 * yyy) - 0.75);
		} else {
			CtoJD = Math.floor(365.25 * yyy);
		}
		double DtoJD = Math.floor(30.6001 * (mmm + 1)); // gotta use 1 bec mmm is 1-based
		double JD = BtoJD + CtoJD + DtoJD + ddd + 1720994.5;

		double JDnow = JD + FractDay;
		double D = JDnow - Epoch;                      // find diff from 31 Dec 1989
		double n = D * (360 / 365.242191);                         //no 46-3
		if (n > 0) {
			n = n - Math.floor(Math.abs(n / 360)) * 360;    //no 46-3
		} else {
			n = n + (360 + Math.floor(Math.abs(n / 360)) * 360);  //no 46-3
		}
		double Mo = n + 279.403303 - 282.768422;                   //no 46-4;
		if(Mo < 0) { Mo = Mo + 360; }                            //no 46-4
		double Ec = 360 * .016713 * Math.sin(Mo * 3.141592654 / 180) / 3.141592654;        //no 46-5
		double lamda = n + Ec + 279.403303;                        //no 46-6
		if(lamda > 360) { lamda = lamda - 360; }                 //no 46-6
		double l = 13.1763966 * D + 318.351648;                    //no 65-4
		if (l > 0) {
			l = l - Math.floor(Math.abs(l / 360)) * 360;  //no 65-4
		} else {
			l = l + (360 + Math.floor(Math.abs(l / 360)) * 360);  //no 65-4
		}
		double Mm = l - .1114041 * D - 36.34041;                   //no 65-5
		if (Mm > 0) {
			Mm = Mm - Math.floor(Math.abs(Mm / 360)) * 360;                       //no 65-5
		} else {
			Mm = Mm + (360 + Math.floor(Math.abs(Mm / 360)) * 360);                       //no 65-5
		}
		double N65 = 318.510107 - .0529539 * D;                    //no 65-6
		if (N65 > 0) {
			N65 = N65 - Math.floor(Math.abs(N65 / 360)) * 360;                    //no 65-6
		} else {
			N65 = N65 + (360 + Math.floor(Math.abs(N65 / 360)) * 360);                    //no 65-6
		}
		double Ev = 1.2739 * Math.sin((2 * (l - lamda) - Mm) * 3.141592654 / 180);         //no 65-7
		double Ae = .1858 * Math.sin(Mo * 3.141592654 / 180);      //no 65-8
		double A3 = .37 * Math.sin(Mo * 3.141592654 / 180);        //no 65-8
		double Mmp = Mm + Ev - Ae - A3;                            //no 65-9
		Ec = 6.2886 * Math.sin(Mmp * 3.141592654 / 180);    //no 65-10
		double A4 = .214 * Math.sin((2 * Mmp) * 3.141592654 / 180);                        //no 65-11
		double lp = l + Ev + Ec - Ae + A4;                         //no 65-12
		double V = .6583 * Math.sin((2 * (lp - lamda)) * 3.141592654 / 180);               //no 65-13
		double lpp = lp + V;                                       //no 65-14
		double D67 = lpp - lamda;                                  //no 67-2
		double Ff = .5 * (1 - Math.cos(D67 * 3.141592654 / 180));      //no 67-3

		//double Xx = (Math.sin(D67 * 3.141592654 / 180));               // I added this to distinguish first from last quarters
		// figure out what phase the moon is in and what icon to use to go with it
		/*	    if(Ff < .02) {                                  //new
	    }
	    if((Ff > .45) && (Ff < .55) && (Xx > 0)) {      //first
	    }
	    if((Ff > .45) && (Ff < .55) && (Xx < 0)) {      //last
	    }                
	    if(Ff > .98) {                                  //full
	    }                                                                             
	    if((Ff > .02) && (Ff < .45) && (Xx > 0)) {      //waxing
	    }
	    if((Ff > .02) && (Ff < .45) && (Xx < 0)) {      //waning
	    }
	    if((Ff > .55) && (Ff < .98) && (Xx > 0)) {      //waxing gibbous
	    }
	    if((Ff > .55) && (Ff < .98) && (Xx < 0)) {      //waning gibbous
	    }
		 */
		return Ff; 
	}



	/**	Returns the total number of days until the next peak lunar phase
	 * 
	 * @param currentTime
	 * @param scheduler
	 * @param peakLunarPhase
	 * @return
	 */
	public static long getDaysTilPeakLunarPhase(Calendar currentDate, long peakLunarPhase){

		for (int i = 0; i < 30; i++){
			if (TimeUtils.getMoonPhase( currentDate) == peakLunarPhase) return i; 
		}
		return 99; 
	}


	/**Returns the difference in days between two dates
	 * 
	 * @param start- start date
	 * @param end - end date
	 * @return
	 */
	public static double getDateDifference(Calendar start, Calendar end, int calendarField){

		long milliSecDiff = end.getTimeInMillis() - start.getTimeInMillis(); 
		if (calendarField == Calendar.DAY_OF_YEAR) return (double) milliSecDiff/(1000*60*60*24);
		else if (calendarField == Calendar.HOUR) return (double) milliSecDiff/(1000*60*60);
		else if (calendarField == Calendar.MINUTE) return (double) milliSecDiff/(1000*60);
		else if (calendarField == Calendar.SECOND) return (double) milliSecDiff/(1000);
		else return Double.NaN;

	}

	/**
	 * Returns the days since the tidal epoch (current: January 1 1983)
	 * @param date
	 * @return
	 */

	public static double getDaysSinceTidalEpoch(Calendar date){
		Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
		epochStart.setTimeZone(TimeZone.getTimeZone("GMT"));
		long milliSecDiff = date.getTimeInMillis() - epochStart.getTimeInMillis(); 
		return (double) milliSecDiff/(1000*60*60*24); 
		
//		return getDateDifference(epochStart, date, Calendar.DAY_OF_YEAR); 
	}
	
	
	/**
	 * Returns a Calendar date, given the number of days since the tidal epoch (Jan 1, 1983)
	 * @param hrs
	 * @return Calendar date
	 */
	public static Calendar getDateFromDaysSinceTidalEpoch(double days){
		Calendar date = new GregorianCalendar(1983, 0, 1); 
		date.setTimeZone(TimeZone.getTimeZone("GMT"));
		// need to first convert hrs to days, then get remainder of hours and convert to seconds -- do this so can add to calendar appropriately
		int daysInt = (int) days; //(hrs/24d);
		int remainingSeconds = (int) ( (days - daysInt)*60*60*24);
		
		date.add(Calendar.DAY_OF_YEAR, daysInt);
		date.add(Calendar.SECOND, remainingSeconds); 

		return date; 
		
	}
	
	
	/**
	 * Returns a Calendar date, given the number of hours since the tidal epoch (Jan 1, 1983)
	 * @param hrs
	 * @return Calendar date
	 */
	public static Calendar getDateFromHrsSinceTidalEpoch(double hrs){
		Calendar date = new GregorianCalendar(1983, 0, 1); 
		date.setTimeZone(TimeZone.getTimeZone("GMT"));
		// need to first convert hrs to days, then get remainder of hours and convert to seconds -- do this so can add to calendar appropriately
		int days = (int) (hrs/24d);
		int remainingSeconds = (int) (( hrs - (days*24))*60*60);  
		
		date.add(Calendar.DAY_OF_YEAR, days);
		date.add(Calendar.SECOND, remainingSeconds); 

		return date; 
		
	}

	/**Returns the hours since the tidal epoch
	 * 
	 * @param date
	 * @return double hrs since tidal epoch (Jan 1, 1983 GMT)
	 */
	public static double getHoursSinceTidalEpoch(Calendar date){
		Calendar epochStart = new GregorianCalendar(1983, 0, 1); 
		epochStart.setTimeZone(TimeZone.getTimeZone("GMT")); 
		long milliSecDiff = date.getTimeInMillis() - epochStart.getTimeInMillis(); 
		return (double) milliSecDiff/(1000*60*60); 
	}

	
	/**
	 * Returns the tide level (in meters) for a given date, and given a file of tidal constituents. Tidal constituents must be in following format (tab delimited), in GMT:
	 * constituentName1		ampl(m)	phase(deg)	Speed(deg/hr)
	 * constituentName2		ampl(m)	phase(deg)	Speed(deg/hr)
	 * ....etc....
	 * Note: can be any number of constituents used, just make sure follow this format, with tab or space deliminted
	 * 
	 * @param date
	 * @param tideConstituentFileName
	 * @return
	 */
	public static double getTidalElevation(Calendar date, String tideConstituentFileName){

		double tide = 0; 
		double time = getHoursSinceTidalEpoch(date); 
		//System.out.println("hours since tidal epoch: " + time); 

		File file = new File(tideConstituentFileName); 
		BufferedReader reader = null; 
		try {
			reader = new BufferedReader(new FileReader(file));
			
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				String tokens[] = line.split("\t"); 

				double ampl = Double.parseDouble(tokens[1]); 
				double phase = Double.parseDouble(tokens[2]); 
				double speed = Double.parseDouble(tokens[3]); 
				tide += ampl*Math.cos(Math.toRadians((speed*time)-phase)); 
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

		return tide; 
	}


	/**
	 * Returns 
	 * @param date
	 * @param tideConstituentFileName
	 * @return
	 */
	public static boolean isIncomingTide(Calendar date, String tideConstituentFileName){
		//double currentTide = getTidalElevation(date, tideConstituentFileName); 
		double beforeTide=0, afterTide=0; 
		Calendar beforeTime = (Calendar) date.clone();
		Calendar afterTime = (Calendar) date.clone();
		beforeTime.add(Calendar.MINUTE, -1); 
		afterTime.add(Calendar.MINUTE, 1); 

		beforeTide += getTidalElevation(beforeTime, tideConstituentFileName);
		afterTide += getTidalElevation(afterTime, tideConstituentFileName);

		double slope = beforeTide - afterTide; 
		if (slope > 0){
			return false;
		}
		else return true;
		
	}
	

	/**	Returns the temperature in tampa bay of the water for a given month from a parameterized spline function, based on historical data from Tampa at two NOAA buoys, and tested against independent data from S. Barbieri 
	 * 
	 * @return temperature (degree C)
	 */
	public static double getTemperature(Calendar date){

		// fits a spline regression with 3 knots (days 175, 325, 366)  to get temperature for any given day of the year
		int dayOfYear = date.get(Calendar.DAY_OF_YEAR);
		double x1 = -0.14448*dayOfYear; 
		double x2= 0.00174*(dayOfYear*dayOfYear);
		double x3= -0.00000411*(dayOfYear*dayOfYear*dayOfYear); 
		double x4=0, x5=0, x6=0; 
		
		if (dayOfYear > 175) x4= Math.pow((dayOfYear-175), 3) * 0.00000269;
		if (dayOfYear > 325) x5= Math.pow((dayOfYear-325), 3) * 0.00002299;
//		if (dayOfYear > 366) x6= Math.pow((dayOfYear-366), 3) * -0.00002565; 
		
		return 22.10033+x1+x2+x3+x4+x5+x6; 

// 		gets the temperature from a sine function, where minimum temp is 18.6, max is 31.9, and the day of middle temperature (i.e., where sine function = 0) is 148.5
//		return 18.6+(31.9-18.6)*(1+Math.sin((dayOfYear/365)*(2*Math.PI) - ((148.5/365)*(2*Math.PI ))))/2;

	
	}

	
	
	
	
	
	
	//***********************************************************************/
	//***********************************************************************/
	//*												*/
	//*This section contains subroutines used in calculating solar position */
	//*												*/
	//***********************************************************************/
	//***********************************************************************/

	// Convert radian angle to degrees

	private static double radToDeg(double angleRad) 
		{
			return (180.0 * angleRad / Math.PI);
		}

	//*********************************************************************/

	// Convert degree angle to radians

		private static double degToRad(double angleDeg) 
		{
			return (Math.PI * angleDeg / 180.0);
		}

	//*********************************************************************/


	//***********************************************************************/
	//* Name:    calcDayOfYear								*/
	//* Type:    Function									*/
	//* Purpose: Finds numerical day-of-year from mn, day and lp year info  */
	//* Arguments:										*/
	//*   month: January = 1								*/
	//*   day  : 1 - 31									*/
	//*   lpyr : 1 if leap year, 0 if not						*/
	//* Return value:										*/
	//*   The numerical day of year							*/
	//***********************************************************************/

		private static double calcDayOfYear(int mn, int dy, int lpyr) 
		{
			int k = (lpyr==1 ? 1 : 2);
			double doy = Math.floor((275 * mn)/9) - k * Math.floor((mn + 9)/12) + dy -30;
			return doy;
		}


	//***********************************************************************/
	//* Name:    calcDayOfWeek								*/
	//* Type:    Function									*/
	//* Purpose: Derives weekday from Julian Day					*/
	//* Arguments:										*/
	//*   juld : Julian Day									*/
	//* Return value:										*/
	//*   String containing name of weekday						*/
	//***********************************************************************/

		private static String calcDayOfWeek(double juld)
		{
			double A = (juld + 1.5) % 7;
			String  DOW = (A==0)?"Sunday":(A==1)?"Monday":(A==2)?"Tuesday":(A==3)?"Wednesday":(A==4)?"Thursday":(A==5)?"Friday":"Saturday";
			return DOW;
		}


	//***********************************************************************/
	//* Name:    calcJD									*/
	//* Type:    Function									*/
	//* Purpose: Julian day from calendar day						*/
	//* Arguments:										*/
	//*   year : 4 digit year								*/
	//*   month: January = 1								*/
	//*   day  : 1 - 31									*/
	//* Return value:										*/
	//*   The Julian day corresponding to the date					*/
	//* Note:											*/
	//*   Number is returned for start of day.  Fractional days should be	*/
	//*   added later.									*/
	//***********************************************************************/

		private static double calcJD(int year, int month, int day)
		{
			if (month <= 2) {
				year -= 1;
				month += 12;
			}
			double A = Math.floor(year/100);
			double B = 2 - A + Math.floor(A/4);

			double JD = Math.floor(365.25*(year + 4716)) + Math.floor(30.6001*(month+1)) + day + B - 1524.5;
			return JD;
		}


		static String[] monthList;
		
	//***********************************************************************/
	//* Name:    calcDateFromJD								*/
	//* Type:    Function									*/
	//* Purpose: Calendar date from Julian Day					*/
	//* Arguments:										*/
	//*   jd   : Julian Day									*/
	//* Return value:										*/
	//*   String date in the form DD-MONTHNAME-YYYY					*/
	//* Note:											*/
	//***********************************************************************/

		private static String calcDateFromJD(double jd)
		{
			double z = Math.floor(jd + 0.5);
			double f = (jd + 0.5) - z;
			double A = 0;
			double alpha =0;
			if (z < 2299161) {
				A = z;
			} else {
				alpha = Math.floor((z - 1867216.25)/36524.25);
				A = z + 1 + alpha - Math.floor(alpha/4);
			}

			double B = A + 1524;
			double C = Math.floor((B - 122.1)/365.25);
			double D = Math.floor(365.25 * C);
			double E = Math.floor((B - D)/30.6001);

			double day = B - D - Math.floor(30.6001 * E) + f;
			double month = (E < 14) ? E - 1 : E - 13;
			double year = (month > 2) ? C - 4716 : C - 4715;

			// alert ("date: " + day + "-" + monthList[month-1].name + "-" + year);
			return (day + "-" + monthList[(int) (month-1)] + "-" + year);
		}


	//***********************************************************************/
	//* Name:    calcDayFromJD								*/
	//* Type:    Function									*/
	//* Purpose: Calendar day (minus year) from Julian Day			*/
	//* Arguments:										*/
	//*   jd   : Julian Day									*/
	//* Return value:										*/
	//*   String date in the form DD-MONTH						*/
	//***********************************************************************/

		private static String calcDayFromJD(double jd)
		{
			double z = Math.floor(jd + 0.5);
			double f = (jd + 0.5) - z;
			double A=0, alpha=0;
			if (z < 2299161) {
				A = z;
			} else {
				alpha = Math.floor((z - 1867216.25)/36524.25);
				A = z + 1 + alpha - Math.floor(alpha/4);
			}

			double B = A + 1524;
			double C = Math.floor((B - 122.1)/365.25);
			double D = Math.floor(365.25 * C);
			double E = Math.floor((B - D)/30.6001);

			double day = B - D - Math.floor(30.6001 * E) + f;
			double month = (E < 14) ? E - 1 : E - 13;
			double year = (month > 2) ? C - 4716 : C - 4715;

			return ((day<10 ? "0" : "") + day + monthList[(int) (month-1)]);
		}


	//***********************************************************************/
	//* Name:    calcTimeJulianCent							*/
	//* Type:    Function									*/
	//* Purpose: convert Julian Day to centuries since J2000.0.			*/
	//* Arguments:										*/
	//*   jd : the Julian Day to convert						*/
	//* Return value:										*/
	//*   the T value corresponding to the Julian Day				*/
	//***********************************************************************/

		private static double calcTimeJulianCent(double jd)
		{
			double T = (jd - 2451545.0)/36525.0;
			return T;
		}


	//***********************************************************************/
	//* Name:    calcJDFromJulianCent							*/
	//* Type:    Function									*/
	//* Purpose: convert centuries since J2000.0 to Julian Day.			*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   the Julian Day corresponding to the t value				*/
	//***********************************************************************/

		private static double calcJDFromJulianCent(double t)
		{
			double JD = t * 36525.0 + 2451545.0;
			return JD;
		}


	//***********************************************************************/
	//* Name:    calGeomMeanLongSun							*/
	//* Type:    Function									*/
	//* Purpose: calculate the Geometric Mean Longitude of the Sun		*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   the Geometric Mean Longitude of the Sun in degrees			*/
	//***********************************************************************/

		private static double calcGeomMeanLongSun(double t)
		{
			double L0 = 280.46646 + t * (36000.76983 + 0.0003032 * t);
			while(L0 > 360.0)
			{
				L0 -= 360.0;
			}
			while(L0 < 0.0)
			{
				L0 += 360.0;
			}
			return L0;		// in degrees
		}


	//***********************************************************************/
	//* Name:    calGeomAnomalySun							*/
	//* Type:    Function									*/
	//* Purpose: calculate the Geometric Mean Anomaly of the Sun		*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   the Geometric Mean Anomaly of the Sun in degrees			*/
	//***********************************************************************/

		private static double  calcGeomMeanAnomalySun(double t)
		{
			double M = 357.52911 + t * (35999.05029 - 0.0001537 * t);
			return M;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcEccentricityEarthOrbit						*/
	//* Type:    Function									*/
	//* Purpose: calculate the eccentricity of earth's orbit			*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   the unitless eccentricity							*/
	//***********************************************************************/


		private static double calcEccentricityEarthOrbit(double t)
		{
			double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
			return e;		// unitless
		}

	//***********************************************************************/
	//* Name:    calcSunEqOfCenter							*/
	//* Type:    Function									*/
	//* Purpose: calculate the equation of center for the sun			*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   in degrees										*/
	//***********************************************************************/


		private static double calcSunEqOfCenter(double t)
		{
			double m = calcGeomMeanAnomalySun(t);

			double  mrad = degToRad(m);
			double  sinm = Math.sin(mrad);
			double  sin2m = Math.sin(mrad+mrad);
			double  sin3m = Math.sin(mrad+mrad+mrad);

			double  C = sinm * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
			return C;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcSunTrueLong								*/
	//* Type:    Function									*/
	//* Purpose: calculate the true longitude of the sun				*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   sun's true longitude in degrees						*/
	//***********************************************************************/


		private static double  calcSunTrueLong(double t)
		{
			double  l0 = calcGeomMeanLongSun(t);
			double  c = calcSunEqOfCenter(t);

			double  O = l0 + c;
			return O;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcSunTrueAnomaly							*/
	//* Type:    Function									*/
	//* Purpose: calculate the true anamoly of the sun				*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   sun's true anamoly in degrees							*/
	//***********************************************************************/

		private static double calcSunTrueAnomaly(double t)
		{
			double m = calcGeomMeanAnomalySun(t);
			double c = calcSunEqOfCenter(t);

			double v = m + c;
			return v;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcSunRadVector								*/
	//* Type:    Function									*/
	//* Purpose: calculate the distance to the sun in AU				*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   sun radius vector in AUs							*/
	//***********************************************************************/

		private static double calcSunRadVector(double t)
		{
			double v = calcSunTrueAnomaly(t);
			double e = calcEccentricityEarthOrbit(t);
	 
			double R = (1.000001018 * (1 - e * e)) / (1 + e * Math.cos(degToRad(v)));
			return R;		// in AUs
		}

	//***********************************************************************/
	//* Name:    calcSunApparentLong							*/
	//* Type:    Function									*/
	//* Purpose: calculate the apparent longitude of the sun			*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   sun's apparent longitude in degrees						*/
	//***********************************************************************/

		private static double calcSunApparentLong(double t)
		{
			double o = calcSunTrueLong(t);

			double omega = 125.04 - 1934.136 * t;
			double lambda = o - 0.00569 - 0.00478 * Math.sin(degToRad(omega));
			return lambda;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcMeanObliquityOfEcliptic						*/
	//* Type:    Function									*/
	//* Purpose: calculate the mean obliquity of the ecliptic			*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   mean obliquity in degrees							*/
	//***********************************************************************/

		private static double calcMeanObliquityOfEcliptic(double t)
		{
			double seconds = 21.448 - t*(46.8150 + t*(0.00059 - t*(0.001813)));
			double e0 = 23.0 + (26.0 + (seconds/60.0))/60.0;
			return e0;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcObliquityCorrection						*/
	//* Type:    Function									*/
	//* Purpose: calculate the corrected obliquity of the ecliptic		*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   corrected obliquity in degrees						*/
	//***********************************************************************/

		private static double calcObliquityCorrection(double t)
		{
			double e0 = calcMeanObliquityOfEcliptic(t);

			double omega = 125.04 - 1934.136 * t;
			double e = e0 + 0.00256 * Math.cos(degToRad(omega));
			return e;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcSunRtAscension							*/
	//* Type:    Function									*/
	//* Purpose: calculate the right ascension of the sun				*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   sun's right ascension in degrees						*/
	//***********************************************************************/

		private static double calcSunRtAscension(double t)
		{
			double e = calcObliquityCorrection(t);
			double lambda = calcSunApparentLong(t);
	 
			double tananum = (Math.cos(degToRad(e)) * Math.sin(degToRad(lambda)));
			double tanadenom = (Math.cos(degToRad(lambda)));
			double alpha = radToDeg(Math.atan2(tananum, tanadenom));
			return alpha;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcSunDeclination							*/
	//* Type:    Function									*/
	//* Purpose: calculate the declination of the sun				*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   sun's declination in degrees							*/
	//***********************************************************************/

		private static double calcSunDeclination(double t)
		{
			double e = calcObliquityCorrection(t);
			double lambda = calcSunApparentLong(t);

			double sint = Math.sin(degToRad(e)) * Math.sin(degToRad(lambda));
			double theta = radToDeg(Math.asin(sint));
			return theta;		// in degrees
		}

	//***********************************************************************/
	//* Name:    calcEquationOfTime							*/
	//* Type:    Function									*/
	//* Purpose: calculate the difference between true solar time and mean	*/
	//*		solar time									*/
	//* Arguments:										*/
	//*   t : number of Julian centuries since J2000.0				*/
	//* Return value:										*/
	//*   equation of time in minutes of time						*/
	//***********************************************************************/

		private static double calcEquationOfTime(double t)
		{
			double epsilon = calcObliquityCorrection(t);
			double l0 = calcGeomMeanLongSun(t);
			double e = calcEccentricityEarthOrbit(t);
			double m = calcGeomMeanAnomalySun(t);

			double y = Math.tan(degToRad(epsilon)/2.0);
			y *= y;

			double sin2l0 = Math.sin(2.0 * degToRad(l0));
			double sinm   = Math.sin(degToRad(m));
			double cos2l0 = Math.cos(2.0 * degToRad(l0));
			double sin4l0 = Math.sin(4.0 * degToRad(l0));
			double sin2m  = Math.sin(2.0 * degToRad(m));

			double Etime = y * sin2l0 - 2.0 * e * sinm + 4.0 * e * y * sinm * cos2l0
					- 0.5 * y * y * sin4l0 - 1.25 * e * e * sin2m;

			return radToDeg(Etime)*4.0;	// in minutes of time
		}


	
		//***********************************************************************/
		//* Name:    calcHourAngleSunrise							*/
		//* Type:    Function									*/
		//* Purpose: calculate the hour angle of the sun at sunrise for the	*/
		//*			latitude								*/
		//* Arguments:										*/
		//*   lat : latitude of observer in degrees					*/
		//*	solarDec : declination angle of sun in degrees				*/
		//* Return value:										*/
		//*   hour angle of sunrise in radians						*/
		//***********************************************************************/

		private static  double calcHourAngleSunrise(double lat, double solarDec)
			{
				double latRad = degToRad(lat);
				double sdRad  = degToRad(solarDec);

				double HAarg = (Math.cos(degToRad(90.833))/(Math.cos(latRad)*Math.cos(sdRad))-Math.tan(latRad) * Math.tan(sdRad));

				double HA = (Math.acos(Math.cos(degToRad(90.833))/(Math.cos(latRad)*Math.cos(sdRad))-Math.tan(latRad) * Math.tan(sdRad)));

				return HA;		// in radians
			}

		//***********************************************************************/
		//* Name:    calcHourAngleSunset							*/
		//* Type:    Function									*/
		//* Purpose: calculate the hour angle of the sun at sunset for the	*/
		//*			latitude								*/
		//* Arguments:										*/
		//*   lat : latitude of observer in degrees					*/
		//*	solarDec : declination angle of sun in degrees				*/
		//* Return value:										*/
		//*   hour angle of sunset in radians						*/
		//***********************************************************************/

			private  static double calcHourAngleSunset(double lat, double solarDec)
			{
				double latRad = degToRad(lat);
				double sdRad  = degToRad(solarDec);

				double HAarg = (Math.cos(degToRad(90.833))/(Math.cos(latRad)*Math.cos(sdRad))-Math.tan(latRad) * Math.tan(sdRad));

				double HA = (Math.acos(Math.cos(degToRad(90.833))/(Math.cos(latRad)*Math.cos(sdRad))-Math.tan(latRad) * Math.tan(sdRad)));

				return -HA;		// in radians
			}



		//***********************************************************************/
		//* Name:    calcSolNoonUTC								*/
		//* Type:    Function									*/
		//* Purpose: calculate the Universal Coordinated Time (UTC) of solar	*/
		//*		noon for the given day at the given location on earth		*/
		//* Arguments:										*/
		//*   t : number of Julian centuries since J2000.0				*/
		//*   longitude : longitude of observer in degrees				*/
		//* Return value:										*/
		//*   time in minutes from zero Z							*/
		//***********************************************************************/

			private  static double calcSolNoonUTC(double t, double longitude)
			{
				// First pass uses approximate solar noon to calculate eqtime
				double tnoon = calcTimeJulianCent(calcJDFromJulianCent(t) + longitude/360.0);
				double eqTime = calcEquationOfTime(tnoon);
				double solNoonUTC = 720 + (longitude * 4) - eqTime; // min

				double newt = calcTimeJulianCent(calcJDFromJulianCent(t) -0.5 + solNoonUTC/1440.0); 

				eqTime = calcEquationOfTime(newt);
				// double solarNoonDec = calcSunDeclination(newt);
				solNoonUTC = 720 + (longitude * 4) - eqTime; // min
				
				return solNoonUTC;
			}

			

			//***********************************************************************/
			//* Name:    calcSunriseUTC								*/
			//* Type:    Function									*/
			//* Purpose: calculate the Universal Coordinated Time (UTC) of sunrise	*/
			//*			for the given day at the given location on earth	*/
			//* Arguments:										*/
			//*   JD  : julian day									*/
			//*   latitude : latitude of observer in degrees				*/
			//*   longitude : longitude of observer in degrees				*/
			//* Return value:										*/
			//*   time in minutes from zero Z							*/
			//***********************************************************************/

				public static  double getSunriseUTC(Calendar date, double latitude, double longitude)
				{
					

					int month = date.get(Calendar.MONTH);
					int day = date.get(Calendar.DAY_OF_MONTH);
					int year = date.get(Calendar.YEAR);

					double JD = calcJD(year, month, day);
					
					double t = calcTimeJulianCent(JD);

					// *** Find the time of solar noon at the location, and use
			        //     that declination. This is better than start of the 
			        //     Julian day

					double noonmin = calcSolNoonUTC(t, longitude);
					double tnoon = calcTimeJulianCent (JD+noonmin/1440.0);

					// *** First pass to approximate sunrise (using solar noon)

					double eqTime = calcEquationOfTime(tnoon);
					double solarDec = calcSunDeclination(tnoon);
					double hourAngle = calcHourAngleSunrise(latitude, solarDec);

					double delta = longitude - radToDeg(hourAngle);
					double timeDiff = 4 * delta;	// in minutes of time
					double timeUTC = 720 + timeDiff - eqTime;	// in minutes

					// alert("eqTime = " + eqTime + "\nsolarDec = " + solarDec + "\ntimeUTC = " + timeUTC);

					// *** Second pass includes fractional jday in gamma calc

					double newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC/1440.0); 
					eqTime = calcEquationOfTime(newt);
					solarDec = calcSunDeclination(newt);
					hourAngle = calcHourAngleSunrise(latitude, solarDec);
					delta = longitude - radToDeg(hourAngle);
					timeDiff = 4 * delta;
					timeUTC = 720 + timeDiff - eqTime; // in minutes

					// alert("eqTime = " + eqTime + "\nsolarDec = " + solarDec + "\ntimeUTC = " + timeUTC);

					return timeUTC;
				}
				
				
		//***********************************************************************/
		//* Name:    calcSunsetUTC								*/
		//* Type:    Function									*/
		//* Purpose: calculate the Universal Coordinated Time (UTC) of sunset	*/
		//*			for the given day at the given location on earth	*/
		//* Arguments:										*/
		//*   JD  : julian day									*/
		//*   latitude : latitude of observer in degrees				*/
		//*   longitude : longitude of observer in degrees				*/
		//* Return value:										*/
		//*   time in minutes from zero Z							*/
		//***********************************************************************/

			public static double getSunsetUTC(Calendar date, double latitude, double longitude)
			{

				int month = date.get(Calendar.MONTH);
				int day = date.get(Calendar.DAY_OF_MONTH);
				int year = date.get(Calendar.YEAR);

				double JD = calcJD(year, month, day);
				
				double t = calcTimeJulianCent(JD);

				// *** Find the time of solar noon at the location, and use
		        //     that declination. This is better than start of the 
	        //     Julian day

				double noonmin = calcSolNoonUTC(t, longitude);
				double tnoon = calcTimeJulianCent (JD+noonmin/1440.0);

				// First calculates sunrise and approx length of day

				double eqTime = calcEquationOfTime(tnoon);
				double solarDec = calcSunDeclination(tnoon);
				double hourAngle = calcHourAngleSunset(latitude, solarDec);

				double delta = longitude - radToDeg(hourAngle);
				double timeDiff = 4 * delta;
				double timeUTC = 720 + timeDiff - eqTime;

				// first pass used to include fractional day in gamma calc

				double newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC/1440.0); 
				eqTime = calcEquationOfTime(newt);
				solarDec = calcSunDeclination(newt);
				hourAngle = calcHourAngleSunset(latitude, solarDec);

				delta = longitude - radToDeg(hourAngle);
				timeDiff = 4 * delta;
				timeUTC = 720 + timeDiff - eqTime; // in minutes
	 

			return timeUTC;
			}
	
			

}
