package us.fl.state.fwc.util.R;

import java.io.File;
import java.util.Random;

import rcaller.RCaller;
import rcaller.RCode;

public class RCallerTest {

	public void step() {
	try {

		/**
		 *  TOO FREAKIN SLOW!!!  
		 *  Based on writing a script file every single time....so much easier to use JRI in rJava!
		 *  
		 */


	      /*
	       * Creating Java's random number generator
	       */
	      Random random = new Random();

	      /*
	       * Creating RCaller
	       */
	      RCaller caller = new RCaller();
	      RCode code = new RCode();
	      /*
	       * Full path of the Rscript. Rscript is an executable file shipped with R.
	       * It is something like C:\\Program File\\R\\bin.... in Windows
	       */
	      caller.setRscriptExecutable("C:/Progra~1/R/R-2.13.0/bin/Rscript.exe");

	      
	     code.addRCode("source(\"C:/Java/workspace/Documents/DataSheets/Snook/Ron_Repro/ReproGAM.r\")");
	      code.addRCode("prediction<-list(predX = 10)");

	      caller.setRCode(code);
	      caller.runAndReturnResult("prediction");
	      
	      double[] results;

	      results = caller.getParser().getAsDoubleArray("predX");
	      System.out.println("pred is " + results[0]);

	      /*
	      double[] data = new double[100];
	      
	      for (int i = 0; i < data.length; i++) {
	        data[i] = random.nextGaussian();
	      }

	      code.addDoubleArray("x", data);

	      code.addRCode("my.mean<-mean(x)");
	      code.addRCode("my.var<-var(x)");
	      code.addRCode("my.sd<-sd(x)");
	      code.addRCode("my.min<-min(x)");
	      code.addRCode("my.max<-max(x)");
	      code.addRCode("my.standardized<-scale(x)");

	      code.addRCode(
	              "my.all<-list(mean=my.mean, variance=my.var, sd=my.sd, min=my.min, max=my.max, std=my.standardized)");

	      caller.setRCode(code);
	      caller.runAndReturnResult("my.all");
	      
	      double[] results;

	      results = caller.getParser().getAsDoubleArray("mean");
	      System.out.println("Mean is " + results[0]);

	      results = caller.getParser().getAsDoubleArray("variance");
	      System.out.println("Variance is " + results[0]);

	      results = caller.getParser().getAsDoubleArray("sd");
	      System.out.println("Standard deviation is " + results[0]);

	      results = caller.getParser().getAsDoubleArray("min");
	      System.out.println("Minimum is " + results[0]);

	      results = caller.getParser().getAsDoubleArray("max");
	      System.out.println("Maximum is " + results[0]);

	      results = caller.getParser().getAsDoubleArray("std");

	      System.out.println("Standardized x is ");
	      
	      for (int i = 0; i < results.length; i++) {
	        System.out.print(results[i] + ", ");
	      }
	      
	      */
	      
	    } catch (Exception e) {
	      System.out.println(e.toString());
	    }
	
	}
	
	public static void main(String[] args){
		RCallerTest test = new RCallerTest();
		test.step();
	}
}
