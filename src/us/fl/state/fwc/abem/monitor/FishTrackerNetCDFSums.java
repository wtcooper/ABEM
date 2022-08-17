package us.fl.state.fwc.abem.monitor;

import java.io.IOException;
import java.util.HashMap;

import us.fl.state.fwc.util.geo.NetCDFFile;

public class FishTrackerNetCDFSums {

	String filename = "output/FishSpatialSums_test.nc";
	private NetCDFFile ncFile; 
	private String[] varName = {"abundance", "biomass", "SSB", "TEP", "recruitment", "settlers", "adults"};
	private String timeName = "time";
	private String latName = "lat";
	private String lonName = "lon";

	double avgCellSize = 175050.3321; 

	//key is varName, val[0] = running average, val[1] = numObservations
	HashMap<String, double[]> averages = new HashMap<String, double[]>();

	//val[0] = min, val[1] = max
	HashMap<String, double[]> minmax= new HashMap<String, double[]>();

	public void run() {
		try {
			ncFile = new NetCDFFile(filename); 
		} catch (IOException e) {
			e.printStackTrace();
		}

		ncFile.setInterpolationAxes(latName, lonName);

		//create new array varNames to setVariabbles() method
		String[] varNames = new String[varName.length + 3];
		int counter=0;
		for (String var: varName){
			varNames[counter++] = var;
		}
		varNames[counter++] = timeName;
		varNames[counter++] = latName;
		varNames[counter++] = lonName;
		//			ncFile.setVariables(latName, lonName, varName, timeName); 
		ncFile.setVariables(varNames);


		int latDim = ncFile.getSingleDimension(latName);
		int lonDim = ncFile.getSingleDimension(lonName); 
		int timeDim = ncFile.getSingleDimension(timeName);
		double missingValue = ncFile.getMissingValue(varName[0]).doubleValue(); 

		for (int t = 0; t<timeDim; t++) {
			System.out.println("start time " + t + "...");
			for (int i=0; i<latDim; i++){
				for (int j=0; j<lonDim; j++){

					for (int k=0; k< varName.length; k++){
						String key = varName[k];
						double val = ncFile.getValue(key, new int[] {t, i, j}).doubleValue(); 

						if (val != missingValue) {



							//##################################
							//compute avgs:
							//##################################
							double[] avg = averages.get(key);

							if (avg == null) {
								avg = new double[2];
								avg[0] = val;
								avg[1] = 1;
								averages.put(key, avg);
							}
							else {
								avg[0] = (avg[0]*avg[1] + val)/(avg[1]+1); //compute new average
								avg[1] ++; //increment num observations
							}


							//##################################
							//compute min / max:
							//##################################

							if (val > 0) {
								double[] mm = minmax.get(key);
								if (mm == null) {
									mm = new double[2];
									mm[0] = val;
									mm[1] = val;
									minmax.put(key, mm);
								}
								else {
									if (val < mm[0]) mm[0] = val;
									if (val > mm[1]) mm[1] = val;
								}
							}

						}
					} //end loop over all variables
				}//end loop over lon
			}//end loop over lat

			//##################################
			//output averages after each time step
			//##################################

			for (int k=0; k< varName.length; k++){
				String key = varName[k];
				if (key.equals("settlers") || key.equals("adults")) {
					if (minmax.get(key) != null) System.out.println(varName[k] + " min:\t" + minmax.get(key)[0]/avgCellSize);
					if (averages.get(key) != null) System.out.println(varName[k] + " avg:\t" + averages.get(key)[0]/avgCellSize);
					if (minmax.get(key) != null) System.out.println(varName[k] + " max:\t" + minmax.get(key)[1]/avgCellSize);
				}
			}

		}//end loop over time


		//##################################
		//output averages
		//##################################
		for (int k=0; k< varName.length; k++){
			String key = varName[k];

			if (key.equals("settlers") || key.equals("adults")) {
				System.out.println(varName[k] + " min:\t" + minmax.get(key)[0]/avgCellSize);
				System.out.println(varName[k] + " avg:\t" + averages.get(key)[0]/avgCellSize);
				System.out.println(varName[k] + " max:\t" + minmax.get(key)[1]/avgCellSize);
			}
		}


	}//end run()

	public static void main(String[] args) {
		FishTrackerNetCDFSums f = new FishTrackerNetCDFSums();
		f.run();
	}
}
