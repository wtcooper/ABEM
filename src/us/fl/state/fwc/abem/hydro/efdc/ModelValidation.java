package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.geometry.Vector3D;

import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;

public class ModelValidation {

	String dataType = "uvel"; 
	String cornersInFile = "dataTest/corners.inp"; 
	HashMap <Int3D, EFDCCell> gridCells; //  = new FastMap <PointLoc, EFDCCell>(); 
	HashMap<Integer, Int3D> gridIndexes; 

	HashMap<Long, Double[]> currentObs = new HashMap<Long, Double[]>(); 

	File currentInFile = new File("dataTest/SunshineSkywayJan2010.txt");
	File uFile = new File("dataTest/UUUDMPF.ASC");  
	File vFile = new File("dataTest/VVVDMPF.ASC");  
	
	int timeSize = 24;
	int depthSize = 8; 
	int latSize = 131;
	int lonSize = 113;
	BufferedReader uReader = null; 
	BufferedReader vReader = null; 
	BufferedReader reader = null; 

	Int3D skyway = new Int3D(45, 40); // the I,J index for where the Sunshine Skyway current meter is
	double skywayDepth = 34.33727; //ft
	double skywayMeterDepth = 10; //10ft below MLLW
	int skywayBin =(int) ((skywayDepth-skywayMeterDepth)/(skywayDepth/depthSize)) - 1 ; // should give an integer of the bin in which the meters need output; substract 1 since indexes are 0-7 verus 1-8

	String outFile = "dataTest/EFDCSkywayValidationJan2010.txt"; 
	PrintWriter outWriter= null; //need Time 	Speed	Direction, in meters per second


	public void writeFile() {

		try { 
			outWriter = new PrintWriter(new FileWriter(outFile, true));
		} catch (IOException e) {e.printStackTrace();}


		outWriter.println("hrsSinceEpoch" + "\t" + "year" + "\t" + "month" + "\t" + "day"+ "\t" + "hr"+ "\t" + "u-model" + "\t" +"v-model"	+ "\t" + "u-obs" + "\t" +"v-obs");  
		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid
		gridCells =grid.getGridCells(); 
		gridIndexes = grid.getGridIndexMap();  // returns a HashMap of the I,J's indexed on the L value

		try {
			uReader = new BufferedReader(new FileReader(uFile));
			vReader = new BufferedReader(new FileReader(vFile)); 
			
			long hrsSinceEpoch = 0; 
			int timeIndex = -1; 
			int cellIndex = 1; 
			int latIndex = 0; 
			int lonIndex = 0; 


			//Scroll through all the lines of the ASCI file, and for each row (a single L, or I,J cell) get an array of the depth bins, where first in layer 1, or deepest layer
			for (String uLine = uReader.readLine(); uLine != null; uLine = uReader.readLine()) {
				String uTokens[] = uLine.split("[ ]+"); 
				String vLine = vReader.readLine(); 
				String vTokens[] = vLine.split("[ ]+"); 

				// if there's only 1 token per line, then this is the start of a new time, and this token is the timeStamp in days with decimal for hour
				if (uTokens.length == 2){ 

					System.out.println("reading time: " + uTokens[1]); 

					timeIndex++; // only do this after first index 
					cellIndex = 1; // reset so is the first L value 

					Double time = new Double(uTokens[1]);
					int daysSinceEpoch = time.intValue(); // turncate
					long hour = Math.round((time.doubleValue() - daysSinceEpoch)*24); // should return the hour of day
					hrsSinceEpoch  = daysSinceEpoch*24 + hour; 
				}


				else {
					float[] uFloatStorage = new float[depthSize]; 
					float[] vFloatStorage = new float[depthSize]; 

					for (int i=0; i<depthSize; i++){
						uFloatStorage[i] = Float.parseFloat(uTokens[i+1]); 
						vFloatStorage[i] = Float.parseFloat(vTokens[i+1]); 
						
					}

					// need to get the latIndex (J) and lonIndex (I) from the EFDC Grid, by passing the L index

					//System.out.println("cellIndex: " + cellIndex); 
					lonIndex = gridIndexes.get(cellIndex).x; 
					latIndex = gridIndexes.get(cellIndex).y; 

					if ( (lonIndex == skyway.x) && (latIndex == skyway.y) ){
						if (currentObs.containsKey(hrsSinceEpoch)){

							Calendar date = TimeUtils.getDateFromHrsSinceTidalEpoch(hrsSinceEpoch); 
							int year = date.get(Calendar.YEAR);
							int month = date.get(Calendar.MONTH); 
							int day = date.get(Calendar.DAY_OF_MONTH);
							int hr = date.get(Calendar.HOUR_OF_DAY); 
							
							// need to convert the vectors for the rotation of the grid so its in real space
							
							Vector3D vector1 = new Vector3D(uFloatStorage[skywayBin],0,0);
							Vector3D vector2 = new Vector3D(0,vFloatStorage[skywayBin],0);
							Vector3D vector3 = vector1.add(vector2); 
							double magnitude = vector3.getNorm();
							double angle = vector3.getAlpha();
							double rotation = Math.toRadians(12.60394); 
							angle += rotation;
							double u = Math.cos(angle)* magnitude; //x
							double v= Math.sin(angle) * magnitude; //y

							
							outWriter.println(hrsSinceEpoch + "\t" + year + "\t" + month+ "\t" + day+ "\t" + hr+ "\t" + u + "\t" +v+ "\t" + currentObs.get(hrsSinceEpoch)[0] + "\t" + currentObs.get(hrsSinceEpoch)[1]);  // if need to output the validation data, do so 
					}
					}

				} // end of else to pull out he data
				cellIndex++; 
			}	// end for loop over file

		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				uReader.close();
				vReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		outWriter.close(); // only close the outfile if should output the file
	}




	public void readCurrentObservations(){

		try {
			reader = new BufferedReader(new FileReader(currentInFile));
			/* First line of the data file is the header */
			String line = null;

			for (line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				if (tokens.length>1){ // skip missing lines

					String timeString = tokens[1];
					String timeTokens[] = timeString.split(":"); 
					int hr = Integer.parseInt(timeTokens[0]);
					int min = Integer.parseInt(timeTokens[1]); 

					if (min == 0) { // only record hourly observation

						String dateString = tokens[0];
						String dateTokens[] = dateString.split("/"); 
						int year = Integer.parseInt(dateTokens[2]); 
						int month = Integer.parseInt(dateTokens[0])-1; 
						int day = Integer.parseInt(dateTokens[1]) ;

						Calendar date = new GregorianCalendar(year, month, day, hr, min);

						long hrsSinceEpoch = Math.round(TimeUtils.getHoursSinceTidalEpoch(date)); 

						double magnitude = Double.parseDouble(tokens[2]) * 0.514444444 ;  //convert from knots to meters per second 
						if (magnitude <20)	 { // make sure only do real values
							double angle = Double.parseDouble(tokens[3]); //getAlpha(); 

							//NOTE: need to switch x&y calcs here because measured data is degrees north (0degrees=north), while calculatations are geometric degrees (i.e., 0degrees = due east)
							double v = Math.cos(Math.toRadians(angle))* magnitude; //y
							double u= Math.sin(Math.toRadians(angle)) * magnitude; //x
							
							Double[] obs = {u,v}; 
							
							currentObs.put(hrsSinceEpoch, obs);
						}
					}
				}
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}



	public void testCurrentObsMap(){
		Set<Long> set = currentObs.keySet();
		List<Long> l = new ArrayList<Long>(set); 
		Collections.sort(l); 
		Iterator<Long> it = l.iterator();
		while (it.hasNext()) {
			Long index = it.next();
			System.out.println(index + "\t" + TimeUtils.getDateFromHrsSinceTidalEpoch(index).getTime() + "\t" + currentObs.get(index)[0] + "\t" + currentObs.get(index)[1]); 
		}

	}



	public static void main(String[] args) {
		ModelValidation a = new ModelValidation();
		a.readCurrentObservations(); 
		//a.testCurrentObsMap(); 
		a.writeFile();
		
	}

}
