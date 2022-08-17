package us.fl.state.fwc.abem.spawn;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import us.fl.state.fwc.util.TextFileIO;

public class GAMSplineTest {

	DecimalFormat twoDForm = new DecimalFormat("#.##");
	NumberFormat nf = NumberFormat.getInstance(); 

	public void run(){

		//TextFileIO test = new TextFileIO("dataTest/test");

		//test.println("test print");

		SeatroutGAMModel gam = new SeatroutGAMModel();

		for (int i=100; i<200; i++){
			System.out.println(i + "\t" + gam.getProbOfSpawn(600, i, "Est") 
					+ "\t" + gam.getProbOfSpawn(600, i, "Upper")
					+ "\t" + gam.getProbOfSpawn(600, i, "Lower"));
		}

		//String curDir = System.getProperty("user.dir");
		//System.out.println(curDir);

		

		//test.close();
		gam.close();

		
	}



	public static void main(String[] args) {
		GAMSplineTest test = new GAMSplineTest();
		test.run();
	}

}
