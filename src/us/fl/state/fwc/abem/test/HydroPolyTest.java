package us.fl.state.fwc.abem.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;

import flanagan.analysis.Regression;

public class HydroPolyTest {

	
	public void step() throws IOException{
		
		OLSMultipleLinearRegression regression1 = new OLSMultipleLinearRegression();
		OLSMultipleLinearRegression regression2 = new OLSMultipleLinearRegression();
		OLSMultipleLinearRegression regression3 = new OLSMultipleLinearRegression();
		ArrayList<Double> y = new ArrayList<Double>(); //double[] y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
		ArrayList<Double> x = new ArrayList<Double>();
		//HashMap<Double, ArrayList<Double>> data = new HashMap<Double, ArrayList<Double>>(); //double[][] x = new double[6][];

		BufferedReader reader = new BufferedReader(new FileReader("dataTest/test.dat"));
		/* First line of the data file is the header */
		String header = reader.readLine();

		//int counter = 0; 
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
			String tokens[] = line.split(","); // this is a "greedy qualifier regular expression in java -- don't understand but works
			y.add(Double.parseDouble(tokens[1]));
			x.add(Double.parseDouble(tokens[0]));
		}		
		
		
		double[] yArray = new double[y.size()];
		double[] xArray = new double[y.size()];

		double[] yScaled = new double[y.size()];
		double[] xScaled = new double[y.size()];
		double[][] x1Array = new double[1][yArray.length]; 
		double[][] x2Array = new double[2][yArray.length]; 
		double[][] x3Array = new double[3][yArray.length]; 


		for (int i=0; i<yArray.length; i++){
			yArray[i] = y.get(i).doubleValue();
			xArray[i] = x.get(i).doubleValue();
		}
		
		double yMean = StatUtils.mean(yArray);
		double xMean = StatUtils.mean(xArray); 

		for (int i=0; i<yArray.length; i++){

			yScaled[i] = yArray[i]-yMean;
			xScaled[i] = xArray[i]-xMean;
			
			x1Array[0][i] = xArray[i]-xMean;
			x2Array[0][i] = x1Array[0][i];
			x3Array[0][i] = x1Array[0][i];
			
			x2Array[1][i] = x1Array[0][i] * x1Array[0][i];
			x3Array[1][i] = x1Array[0][i] * x1Array[0][i];

			x3Array[2][i] = x1Array[0][i] * x1Array[0][i] * x1Array[0][i];

		}
		
		Regression flanReg = new Regression(x3Array, yScaled);
		flanReg.linear();
		double[] beta = flanReg.getBestEstimates();
		

		for (int i=0; i<beta.length; i++){
			System.out.println(beta[i]); 
		}
		
		
		
	}
	
	public static void main(String[] args) {

		HydroPolyTest mt = new HydroPolyTest(); 
		try {
			mt.step();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
}
