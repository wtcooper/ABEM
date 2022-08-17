package us.fl.state.fwc.abem.spawn;

import java.io.File;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import us.fl.state.fwc.util.R.TextConsole;

/**
 * Generalize Additive Model for seatrout active spawning, calling R directly through the rJava
 * JNI interface.
 * 
 * @author Wade.Cooper
 *
 */
public class SeatroutGAMModel {

	private Rengine re; 
	private REXP estimates; 
	private REXP stErrors; 
	public double intercept;
	private double midpointTL = 384.0809; //NEED to adjust this if data changes

	//flags to get the confidence bounds
	public boolean addTLUpper = false;
	public boolean addTLLower = false;
	public boolean addDateUpper = false;
	public boolean addDateLower = false;

	/**
	 * Constructor without a script file path -- manually need to run the GAM
	 */
	public SeatroutGAMModel(){
		re=new Rengine(null, false, new TextConsole(false)); 

		//need to set the working directory to the current directory
		//		String curDir = System.getProperty("user.dir");
		//		System.out.println(curDir);
		//		File file = new File(System.getProperty("user.dir"));
		//		String dir = file.getParentFile().getAbsolutePath();
		//		dir = dir.replaceAll("\\\\+", "/");
		//		re.eval("setwd(\"" + dir + "\")"); 

		//then source the 
		File file = new File("RScripts/SeatroutGAM.R");
		String dir = file.getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("source(\"" + dir + "\")"); 

		//get east intercept:
		re.eval("sum = summary(activeSpawn)");
		estimates = re.eval("sum$p.table");
		intercept = estimates.asDoubleArray()[0];


	}

	/**
	 * Constructor with script file path -- will source the script file to run the GAM
	 */
	public SeatroutGAMModel(String scriptPath){
		re=new Rengine(null, false, new TextConsole(false)); 

		//then source the 
		File file = new File(scriptPath);
		String dir = file.getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("source(\"" + dir + "\")"); 

		//get east intercept:
		re.eval("sum = summary(activeSpawn)");
		estimates = re.eval("sum$p.table");
		intercept = estimates.asDoubleArray()[0];

	}



	public double getProbOfSpawn(double TL, double DayOfYear, String confBound){
		double sum =0;

		re.assign("Size", new double[]{TL});
		re.assign("DayOfYear", new double[]{DayOfYear});
		re.assign("Zone", new int[] {1,2,3,4});
		sum+=intercept;

		re.eval("pred <- predict.gam(activeSpawn, data.frame(Zone, DayOfYear, Size), se.fit=TRUE, type=\"terms\")");
		estimates = re.eval("pred$fit");
		stErrors= re.eval("pred$se.fit");
		double est[][]=estimates.asDoubleMatrix();
		double se[][]=stErrors.asDoubleMatrix();

		//first get averages
		double means[]=new double[est[0].length];
		double seMeans[]=new double[est[0].length];

		for (int i=0; i<est[0].length; i++){
			for (int j=0; j<est.length; j++){
				means[i] += est[j][i];
				seMeans[i] += se[j][i];
				//if last measurement, get avg
				if (j==est.length-1) {
					means[i] /= est.length;
					sum += means[i];

					//if the size predictor and need to get confidence probability
					if (i== est[0].length-1) {
						seMeans[i] /= est.length;

						//need to do this because 
						if (confBound.equals("Upper") || confBound.equals("Lower")) {
							if (TL >= midpointTL ) {
								if (confBound.equals("Upper")) sum += seMeans[i]*1.97;
								else if (confBound.equals("Lower")) sum -= seMeans[i]*1.97;
							}
							else {
								if (confBound.equals("Upper")) sum -= seMeans[i]*1.97;
								else if (confBound.equals("Lower")) sum += seMeans[i]*1.97;
							}// for TL < midpoint
						} // end check if either Upper/Lower
					}
				}
			}
		}


		return Math.exp(sum)/(1 + Math.exp(sum));


	}


	public void close(){
		re.end();
	}
}
