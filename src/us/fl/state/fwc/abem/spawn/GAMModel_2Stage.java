package us.fl.state.fwc.abem.spawn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class GAMModel_2Stage {

	public String capMonthInFile = "data/GAM_capMonth.txt";
	public String capTLInFile = "data/GAM_capTL.txt";
	public String actMonthInFile = "data/GAM_actMonth.txt";
	public String actTLInFile = "data/GAM_actTL.txt";



	/* 
	###############################################################
	
	 Fit to MEPS data, all data (even mature)
	2-stage fit: 	(1) Spawn capable out of entire dataset, thereby avoid maturity check
						(2) Active spawning out of spawn capable
	
	 ###############################################################
	 */
	
	
	/*
Family: binomial 
Link function: logit 

Formula:
SpawnCapable ~ ZoneCat + s(Month) + s(Size)

Parametric coefficients:
            Estimate Std. Error z value Pr(>|z|)   
(Intercept)  -9.7896     3.9477  -2.480  0.01315 * 
ZoneCatb     -0.2020     0.3217  -0.628  0.53010   
ZoneCatc     -0.4033     0.4186  -0.964  0.33523   
ZoneCatd     -1.5132     0.6332  -2.390  0.01686 * 
ZoneCate      2.6706     0.8143   3.280  0.00104 **
---
Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1 

Approximate significance of smooth terms:
           edf Ref.df Chi.sq  p-value    
s(Month) 7.899  8.099  82.69 1.59e-14 ***
s(Size)  1.947  2.499  80.89  < 2e-16 ***
---
Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1 

R-sq.(adj) =  0.786   Deviance explained = 74.6%
UBRE score = -0.61887  Scale est. = 1         n = 986	 */
	
	public double capIntercept  =  -9.7896; 
	double[] capZoneSlopes = {0,   -0.2020, -0.4033, -1.5132 , 2.6706};
	double  capZoneAvg =0.11042;
	double[] capMonthFx;
	double[] capMonthVal;
	double[] capTLFx;
	double[] capTLVal;

	
	/* Active Spawning:
Family: binomial 
Link function: logit 

Formula:
ActiveSpawn2 ~ ZoneCat + s(Month) + s(Size)

Parametric coefficients:
            Estimate Std. Error z value Pr(>|z|)    
(Intercept) -1.69310    0.22414  -7.554 4.23e-14 ***
ZoneCatb    -0.64772    0.38831  -1.668   0.0953 .  
ZoneCatc     0.49638    0.46704   1.063   0.2879    
ZoneCatd    -0.04879    0.80647  -0.061   0.9518    
ZoneCate     4.01271    0.41976   9.560  < 2e-16 ***
---
Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1 

Approximate significance of smooth terms:
           edf Ref.df Chi.sq p-value  
s(Month) 6.336   7.48 18.802  0.0118 *
s(Size)  1.142   1.27  3.048  0.1132  
---
Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1 

R-sq.(adj) =   0.56   Deviance explained = 48.2%
UBRE score = -0.2554  Scale est. = 1         n = 520	 */
	public double actIntercept = -1.69310; 
	double[] actZoneSlopes = {0, -0.64772, 0.49638, -0.04879, 4.01271};  
	double actZoneAvg =0.762516;
	double[] actMonthFx;
	double[] actMonthVal;
	double[] actTLFx;
	double[] actTLVal;


	
	public void initialize() {
		setArrays("capMonth", capMonthInFile);
		setArrays("capTL", capTLInFile);
		setArrays("actMonth", actMonthInFile);
		setArrays("actTL", actTLInFile);
	}
	
	
	

	/**
	 * Get the estimated probability of spawning for the time period over which a spawning
	 * indicator was present (e.g., with Sue's MEPS data, the time period is 2.5833 days).  
	 * 
	 * If want to use an 'average' zone, set zone=0, which is the average of partial effects.
	 * 
	 * 
	 * @param temp
	 * @param TL
	 * @param zone
	 * @return
	 */
	public double getCapableSpawnProb(double month, double TL, int zone){

		double probOfSpawn = 0;
		double addTerm = capIntercept;
		addTerm += getFunctionValue(month, capMonthVal, capMonthFx); 
		addTerm += getFunctionValue(TL, capTLVal, capTLFx);
		if (zone==0) addTerm += capZoneAvg;
		else addTerm += capZoneSlopes[zone-1];

		probOfSpawn =Math.exp(addTerm)/(1 + Math.exp(addTerm));

		return probOfSpawn;
	}



	public double getActiveSpawnProb(double month, double TL, int zone){

		double probOfSpawn = 0;
		double addTerm = actIntercept;
		addTerm += getFunctionValue(month, actMonthVal, actMonthFx); 
		addTerm += getFunctionValue(TL, actTLVal, actTLFx);
		if (zone==0) addTerm += actZoneAvg;
		else addTerm += actZoneSlopes[zone-1];

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

			if (variable.equals("capMonth")) {
				capMonthVal = val;
				capMonthFx = fx;
			}
			else if (variable.equals("capTL")) {
				capTLVal = val;
				capTLFx = fx;
			}
			else if (variable.equals("actMonth")) {
				actMonthVal = val;
				actMonthFx = fx;
			}
			else if (variable.equals("actTL")) {
				actTLVal = val;
				actTLFx = fx;
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






}
