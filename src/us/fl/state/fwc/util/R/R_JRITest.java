package us.fl.state.fwc.util.R;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;


public class R_JRITest {


	public void step(String[] args) {


		//TextConsole(true) will write output
		Rengine re=new Rengine(args, false, new TextConsole(false)); 
		REXP x; 

		//first set working directory
		File file = new File("data/RonReproEastCoast.txt");
		String dir = file.getParentFile().getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("setwd(\"" + dir + "\")"); 
		
		//then source the 
		file = new File("RScripts/SnookGAM.R");
		dir = file.getAbsolutePath();
		dir = dir.replaceAll("\\\\+", "/");
		re.eval("source(\"" + dir + "\")"); 
//		re.eval("source(\"C:/work/workspace/Documents/DataSheets/Snook/Ron_Repro/ReproGAM.r\")"); 

		
		double[] TL = {600};
		double[] DayOfYear = {200};

		re.assign("TL", TL);
		re.assign("DayOfYear", DayOfYear);
		
		//get prediction
		System.out.println("predictions:");
		re.eval("pred <- predict.gam(activeWest, data.frame(DayOfYear=c(150), TL=c(800)), se.fit=TRUE, type=\"terms\")");
		x = re.eval("pred$fit");
		double d[]=x.asDoubleArray();
		for (int i=0; i<d.length;i++) System.out.println(d[i]);
		

		//get se
		System.out.println("SE's:");
		x = re.eval("pred$se.fit");
		d=x.asDoubleArray();
		for (int i=0; i<d.length;i++) System.out.println(d[i]);

		//get summary
		System.out.println("Intercept:");
		re.eval("sum = summary(activeWest)");
		x = re.eval("sum$p.table");
		d=x.asDoubleArray();
		for (int i=0; i<d.length;i++) System.out.println(d[i]);

		
/*		for (int day=0; day<365; day++) {
			int numProbs = 6;
			double[] probs = new double[numProbs];
			for (int length=0; length<numProbs; length++){
				TL[0] = 500+length*100;
				DayOfYear[0] = day;

				re.assign("TL", TL);
				re.assign("DayOfYear", DayOfYear);
				re.eval("pred <- predict.gam(activeEast, data.frame(DayOfYear, TL), se.fit=TRUE, type=\"response\")");
				x = re.eval("pred$fit");
				d=x.asDoubleArray();

				probs[length] = d[0];
			}
			System.out.print(day + "\t");
			for (int i=0; i<probs.length; i++){
				System.out.print(probs[i] + "\t");
			}
			System.out.print("\n");
		}
*/
		re.end(); 

		

	}


	public static void main (String[] args) {
		R_JRITest test = new R_JRITest();
		test.step(args);
	}
}
