package us.fl.state.fwc.abem.spawn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class GAMModel_1Stage {

	String monthInFile = "data/GAM_Month.txt";
	String TLInFile = "data/GAM_SizeEst.txt";


	/*
Family: binomial 
Link function: logit 

Formula:
ActiveSpawn2 ~ ZoneCat + s(Month) + s(Size)

Parametric coefficients:
            Estimate Std. Error z value Pr(>|z|)    
(Intercept)  -4.7882     1.2759  -3.753 0.000175 ***
ZoneCatb     -0.8140     0.3846  -2.116 0.034320 *  
ZoneCatc      0.2701     0.4386   0.616 0.538012    
ZoneCatd     -0.4331     0.7940  -0.545 0.585470    
---
Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1 

Approximate significance of smooth terms:
           edf Ref.df Chi.sq p-value   
s(Month) 5.759  6.631 18.359 0.00827 **
s(Size)  1.000  1.000  7.117 0.00764 **
---
Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1 

R-sq.(adj) =  0.129   Deviance explained = 25.7%
UBRE score = -0.61643  Scale est. = 1         n = 816
	 */

	/*For penalized reg splines, without Zone 5:*/
	double intercept  = -4.7882; 
	double[] zoneSlopes = {0, -0.8140, 0.2701 ,-0.4331};
	double zoneAvg =-0.24425;

	
	/*For fixed DFs, with Zone 5:
	 * Formula:
		ActiveSpawn2 ~ ZoneCat + s(Month, k = 6, fx = TRUE) + s(Size)


	double intercept  = -4.1163 ; 
	double[] zoneSlopes = {0, -0.7215, 0.4503 ,-0.5892, 4.1707};
	double zoneAvg =0.66206;
	*/
	
	/*For penalized regression splines, with Zone 5:
	double intercept  = -3.8568 ; 
	double[] zoneSlopes = {0, -0.7539, 0.4121 , -0.6152, 4.1619};
	double zoneAvg =0.64098;
	*/
	
	double[] monthFx;
	double[] monthVal;
	double[] TLFx;
	double[] TLVal;


	
	public void initialize() {
		setArrays("Month", monthInFile);
		setArrays("TL", TLInFile);
	}

	
	
	/**
	 * Get the estimated probability of spawning for the time period over which a spawning
	 * indicator was present (e.g., with Sue's MEPS data, the time period is 2.5833 days).  To
	 * convert this to continuous time formulation, first need to convert the probability to 
	 * instantanous rate [ =ln(-1/(prob-1)) ], then convert back to probability 
	 * [ =1-exp(-InstRt*(timeStep/2.54)) ].  
	 * 
	 * If want to use an 'average' zone, set zone=0, which is the average of partial effects.
	 * 
	 * 
	 * @param month
	 * @param TL
	 * @param zone
	 * @return
	 */
	public double getProbOfSpawn(double month, double TL, int zone){

		double probOfSpawn = 0;
		double addTerm = intercept;
		addTerm += getFunctionValue(month, monthVal, monthFx); 
		addTerm += getFunctionValue(TL, TLVal, TLFx);
		if (zone==0) addTerm += zoneAvg;
		else addTerm += zoneSlopes[zone-1];

		probOfSpawn =Math.exp(addTerm)/(1 + Math.exp(addTerm));

		return probOfSpawn;
	}




	public double getFunctionValue(double value, double[] valArray, double[] fxArray){
		double fxValue = 0;

		//get the idx as the index 
		int idx = Arrays.binarySearch(valArray, value);

		//if it's positive, then is exact match to data, so return the fxArray value
		if (idx >= 0) return fxArray[idx];

		//if not, then need to interpolate to get the fx value

		//here, idx is the index of the next highest value  

		
		// is less than lowest value, so set idx to 2nd value so that can get slope
		//between second and first value to interpolate backwards
		if (idx == -1) idx = 1; 	 
							
		
		//if greater than highest value, set idx to highest value
		else if (-(idx + 1) >= valArray.length)  idx = valArray.length-1; 

		
		else idx = -(idx + 1);
		
		
		//interpolate between the two values
		double slope = (fxArray[idx] - fxArray[idx-1]) / (valArray[idx] - valArray[idx-1]);
		//		double fracChange = (value- valArray[idx-1])/(valArray[idx] - valArray[idx-1]);
		double unitChange = value- valArray[idx-1];

		fxValue =  fxArray[idx-1] + unitChange*slope;
		return fxValue;
	}




	public void setArrays(String variable, String fileName){
		double[] val = null;
		double[] fx = null;

		try {
			File file = new File(fileName); 
			BufferedReader reader = new BufferedReader(new FileReader(file));
			ArrayList<double[]> tempList = new ArrayList<double[]>();

			reader.readLine(); //read in header line

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//String tokens[] = line.split("\\x20+"); // this is a "greedy qualifier regular expression in java -- don't understand but works
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				double value = Double.parseDouble(tokens[0]) ;
				double function = Double.parseDouble(tokens[1]) ;
				tempList.add(new double[] {value, function});

			}

			val = new double[tempList.size()];
			fx = new double[tempList.size()];
			for (int i =0; i<tempList.size(); i++){
				val[i] = tempList.get(i)[0];
				fx[i] = tempList.get(i)[1];
			}

			if (variable.equals("Month")) {
				monthVal = val;
				monthFx = fx;
			}
			else if (variable.equals("TL")) {
				TLVal = val;
				TLFx = fx;
			}
			else {
				System.out.println("wrong variable name!");
				System.exit(1);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}




	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}



}
