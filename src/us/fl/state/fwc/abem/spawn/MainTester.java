package us.fl.state.fwc.abem.spawn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

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
import us.fl.state.fwc.util.R.TextConsole;

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
		main.getWeightAtAge(); 
	}

	public void getWeightAtAge() {

		SeatroutParams params = new SeatroutParams();
		
		for (int sex = 0; sex<=1; sex++) {
			System.out.println("sex: " + sex);
			for (int i=0; i<=12; i++) {
			double ageInDays = i*365+181;
			double TL = params.getLengthAtAge(params.getLinf(sex), (ageInDays)/365.25, sex);
			double groupBiomass = params.getMassAtLength(TL, sex) * 1;
			double TL_assess = params.getLengthAtAge_Assess(params.getLinf_Assess(0), (ageInDays)/365.25, sex);
			double mass_assess = params.getMassAtLength_Assess(TL_assess, sex);
			
			System.out.println(i + "\t" + TL + "\t" + groupBiomass + "\tassess:\t" + TL_assess + "\t" + mass_assess);
			}
		}
	}

	public void step() {

		Rengine re=new Rengine(null, false, new TextConsole(true)); 
		//File file = new File("data/GAMGood.txt");
		//String dir = file.getParentFile().getAbsolutePath();
		//dir = dir.replaceAll("\\\\+", "/");
		//re.eval("setwd(\"" + dir + "\")"); 
		
		
		PrintWriter out= null; 
		File fFile = new File("dataTest/out");

		try { 
			out= new PrintWriter(new FileWriter(fFile, true));
		} catch (IOException e) {e.printStackTrace();
		} finally {
			out.println("test");
			out.close();
			re.end();
		}
		
		
	}

}

