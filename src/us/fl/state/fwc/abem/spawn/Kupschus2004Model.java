package us.fl.state.fwc.abem.spawn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class Kupschus2004Model {

	String conditionInFile = "data/Kupschus04_ConditionData";
	String tempInFile = "data/Kupschus04_TempData";
	String TLInFile = "data/Kupschus04_TLData";
	String relTimeInFile = "data/Kupschus04_SpawnTimeData";
	String lunarInFile = "data/Kupschus04_MoonData";


	//best value match from param sweep through possible intercepts, where attempting to 
	//find the intercept that reproduces a max o .42 spawning under optimal temperature
	// for an average fish (i.e., average condition, optimal hours after sunset) 
	double intercept  = -3.3631499999991035; 

	double[] conditionFx;
	double[] conditionVal;
	double[] tempFx;
	double[] tempVal;
	double[] TLFx;
	double[] TLVal;
	double[] relTimeFx;
	double[] relTimeVal;
	double[] lunarFx;
	double[] lunarVal;

	
	
	public double getProbOfSpawn(double temp, double TL, double condition, 
			double relTime, double daysAfterNewMoon){

		double probOfSpawn = 0;
		double addTerm = intercept;
		addTerm += getFunctionValue(temp, tempVal, tempFx); 
		addTerm += getFunctionValue(TL, TLVal, TLFx);
		addTerm += getFunctionValue(condition, conditionVal, conditionFx);
		addTerm += getFunctionValue(relTime, relTimeVal, relTimeFx);
		addTerm += getFunctionValue(daysAfterNewMoon, lunarVal, lunarFx);

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

		if (idx == -1) return fxArray[0]; // is less than lowest value, so return lowest value
		if (-(idx + 1) >= valArray.length) return fxArray[valArray.length - 1]; //greater than highest value, so return highest value

		
		idx = -(idx + 1);

		//interpolate between the two values
		double slope = (fxArray[idx] - fxArray[idx-1]) / (valArray[idx] - valArray[idx-1]);
//		double fracChange = (value- valArray[idx-1])/(valArray[idx] - valArray[idx-1]);
		double unitChange = value- valArray[idx-1];
		
		fxValue =  fxArray[idx-1] + unitChange*slope;
		return fxValue;
	}
	
	

	public double getFunctionValue(double value, String variable){
		double fxValue = 0;
		double[] valArray = null;
		double[] fxArray = null;
		
		if (variable.equals("temp")) {
			valArray = tempVal;
			fxArray = tempFx;
		}
		else if (variable.equals("condition")) {
			valArray = conditionVal;
			fxArray = conditionFx;
		}
		else if (variable.equals("TL")) {
			valArray = TLVal;
			fxArray = TLFx;
		}
		else if (variable.equals("lunar")) {
			valArray = lunarVal;
			fxArray = lunarFx;
		}
		else if (variable.equals("relTime")) {
			valArray = relTimeVal;
			fxArray = relTimeFx;
		}
		else {
			System.out.println("wrong variable name!");
			System.exit(1);
		}
		
		
		//get the idx as the index 
		int idx = Arrays.binarySearch(valArray, value);
		
		//if it's positive, then is exact match to data, so return the fxArray value
		if (idx >= 0) return fxArray[idx];

		//if not, then need to interpolate to get the fx value

		//here, idx is the index of the next highest value  

		if (idx == -1) return fxArray[0]; // is less than lowest value, so return lowest value
		if (-(idx + 1) >= valArray.length) return fxArray[valArray.length - 1]; //greater than highest value, so return highest value

		idx = -(idx + 1);

		//interpolate between the two values
		double slope = (fxArray[idx] - fxArray[idx-1]) / (valArray[idx] - valArray[idx-1]);
//		double fracChange = (value- valArray[idx-1])/(valArray[idx] - valArray[idx-1]);
		double unitChange = value- valArray[idx-1];
		
		fxValue =  fxArray[idx-1] + unitChange*slope;
		return fxValue;
	}


	
	
	public void initialize() {

		setArrays("condition", conditionInFile);
		setArrays("temp", tempInFile);
		setArrays("TL", TLInFile);
		setArrays("relTime", relTimeInFile);
		setArrays("lunar", lunarInFile);

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

			if (variable.equals("temp")) {
				tempVal = val;
				tempFx = fx;
			}
			else if (variable.equals("condition")) {
				conditionVal = val;
				conditionFx = fx;
			}
			else if (variable.equals("TL")) {
				TLVal = val;
				TLFx = fx;
			}
			else if (variable.equals("lunar")) {
				lunarVal = val;
				lunarFx = fx;
			}
			else if (variable.equals("relTime")) {
				relTimeVal = val;
				relTimeFx = fx;
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
