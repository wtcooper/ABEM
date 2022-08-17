package us.fl.state.fwc.abem.test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.regression.SimpleRegression;

import cern.colt.Arrays;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

import com.vividsolutions.jts.geom.Coordinate;

import us.fl.state.fwc.abem.ThreadService;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.DataTable;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.SunriseSunset;
import us.fl.state.fwc.util.TextFileIO;
import us.fl.state.fwc.util.TimeUtils;

public class MainTester {

	DecimalFormat twoDForm = new DecimalFormat("#.##");
	NumberFormat nf = NumberFormat.getInstance(); 

	long seed = System.currentTimeMillis(); 
	public MersenneTwister64 m = new MersenneTwister64((int) seed); 
	public Uniform uniform= new Uniform(m); 
	public Normal normal= new Normal(0,1,m); 


	/**
	 * @param args
	 * @throws MathException 
	 */
	public static void main(String[] args) throws MathException {
		MainTester main = new MainTester();
		main.step(); 
	}



	public void step() {
		SeatroutParams params = new SeatroutParams();
		
		for (int i=0; i<=12; i++) {
			for (int j=0; j<100; j++){
			double Linf=normal.nextDouble(params.getLinf(0), params.getLinfStDev(0));
			double TL = params.getLengthAtAge(i+0.5, 0);
			double wt = params.getMassAtLength(TL, 0); 

			System.out.println(i + "\t" + TL + "\t" + wt);
			}
			}
	}

}


