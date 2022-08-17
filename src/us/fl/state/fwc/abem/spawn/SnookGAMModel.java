package us.fl.state.fwc.abem.spawn;

import java.io.File;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import us.fl.state.fwc.util.R.TextConsole;

public class SnookGAMModel {

	private Rengine re; 
	private REXP x; 
	private double eastIntercept;
	private double westIntercept;
	
	//flags
	public String coast = "West"; //default coast
	//flags to get the confidence bounds
	public boolean addTLUpper = false;
	public boolean addTLLower = false;
	public boolean addDateUpper = false;
	public boolean addDateLower = false;
	
	/**
	 * Constructor without a script file path -- manually need to run the GAM
	 */
	public SnookGAMModel(){
		re=new Rengine(null, false, new TextConsole(false)); 
		File file = new File("data/RonReproEastCoast.txt");
		String dir = file.getParentFile().getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("setwd(\"" + dir + "\")"); 
		
		//then source the 
		file = new File("RScripts/SnookGAM.R");
		dir = file.getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("source(\"" + dir + "\")"); 

		//get east intercept:
		re.eval("sum = summary(activeEast)");
		x = re.eval("sum$p.table");
		eastIntercept = x.asDoubleArray()[0];

		//get west intercept:
		re.eval("sum = summary(activeWest)");
		x = re.eval("sum$p.table");
		westIntercept = x.asDoubleArray()[0];
	}

	/**
	 * Constructor with script file path -- will source the script file to run the GAM
	 */
	public SnookGAMModel(String scriptPath){
		re=new Rengine(null, false, new TextConsole(false)); 
		File file = new File("data/RonReproEastCoast.txt");
		String dir = file.getParentFile().getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("setwd(\"" + dir + "\")"); 
		
		//then source the 
		file = new File(scriptPath);
		dir = file.getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("source(\"" + dir + "\")"); 

		//get east intercept:
		re.eval("sum = summary(activeEast)");
		x = re.eval("sum$p.table");
		eastIntercept = x.asDoubleArray()[0];

		//get west intercept:
		re.eval("sum = summary(activeWest)");
		x = re.eval("sum$p.table");
		westIntercept = x.asDoubleArray()[0];
	}
	
	
	
	public double getProbOfSpawn(double TL, double DayOfYear){
		double sum =0;
		
		re.assign("TL", new double[]{TL});
		re.assign("DayOfYear", new double[]{DayOfYear});

		//get the appropriate predicted values and add in the appropriate intercept
		if (coast.equals("east") || coast.equals("East")){
			re.eval("pred <- predict.gam(activeEast, data.frame(DayOfYear, TL), se.fit=TRUE, type=\"terms\")");
			sum+=eastIntercept;
		}
		else {
			re.eval("pred <- predict.gam(activeWest, data.frame(DayOfYear, TL), se.fit=TRUE, type=\"terms\")");
			sum+=westIntercept;
		}

		x = re.eval("pred$fit");
		double d[]=x.asDoubleArray();

		for (int i=0; i<d.length; i++){
			sum+=d[i];
		}

		return Math.exp(sum)/(1 + Math.exp(sum));

		
	}
	
	
}
