package us.fl.state.fwc.abem.test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.util.charts.XYPlot;



public class MoonPhaseTest {

	static private Calendar startDate  = new GregorianCalendar(TimeZone.getTimeZone("GMT")) ; 
	static private Calendar currentDate ; 

	int days = 365; 
	
	double[][] data = new double[2][days]; 
	
	
	public static void main(String[] args) {
		MoonPhaseTest mp = new MoonPhaseTest();
		mp.step(); 
	}

	
	public void step(){

		startDate.set(2001, 0, 1, 0, 0, 0);  
		currentDate = (GregorianCalendar) startDate.clone(); 
//		currentDate.add(Calendar.DAY_OF_YEAR, 89); 
		

		for (int day=0; day<days; day++){
			
			double phase = phasecalc(); 
			data[0][day] = day;
			data[1][day] = phase; 

			System.out.println("date: " + currentDate.getTime()+ "\tphase: " + phase); 
			
			currentDate.add(Calendar.DAY_OF_YEAR, 1); 

		}

		XYPlot plot = new XYPlot("moon phase");
		plot.makeLinePlot("phase", "day", "prob", data, false, false); 

	}


public double phasecalc() {                               //button click handler

	double Epoch = 2447891.5;
	int yyy = currentDate.get(Calendar.YEAR);
	int mmm = currentDate.get(Calendar.MONTH); 
	int ddd = currentDate.get(Calendar.DATE); 
	int hhh= currentDate.get(Calendar.HOUR);
	int mmn = currentDate.get(Calendar.MINUTE);
	
    double FractDay = hhh / 24;
    FractDay += mmn / 60 / 24;

    // this calculates the Julian Date
    if (mmm < 2) {  // if 1 or 2 (this has been changed to 1-base)
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

    // add on hr & min fract of a day
    double JDnow = JD + FractDay;
    // because I was also using this for something else,
    // correct for time zone after convert to JD
    // if >= 1990, use local time zone, otherwise assume GMT
    // note, it doesn't matter if it calculates the date correctly if all we want is the timezone
    // just remember that the time zone correction must first be converted to a fraction of a day
 //   if (yyy >= 1990) {
   //         JDnow += tzone;
   // }

    // here is where the previous calculations start
    // note the new way of calculating D -- the answer is the same
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
    
    double Xx = (Math.sin(D67 * 3.141592654 / 180));               // I added this to distinguish first from last quarters
    // figure out what phase the moon is in and what icon to use to go with it
    if(Ff < .02) {                                  //new
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

    return Ff; 

}



}
