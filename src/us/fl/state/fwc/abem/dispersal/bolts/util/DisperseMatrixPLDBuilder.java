package us.fl.state.fwc.abem.dispersal.bolts.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.TreeMap;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.util.TestingUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class DisperseMatrixPLDBuilder {

	String releaseFile =  "output/BOLTs/release_TB.txt"; 
	String releaseDatesFile = "output/BOLTs/releaseDates.txt";
	LinkedList<Calendar> releaseDates; 
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	String outputFolder = "c:\\work\\data\\BOLTs\\SeatroutRuns\\output5.6.11\\";
	ArrayList<String> releaseSites = new ArrayList<String>(); 
	ArrayList<Coordinate>  releaseSiteLocs = new ArrayList<Coordinate>(); 
	
	//replaces the releaseSites and releaseSiteLocs so have an ordered list
	TreeMap<String, Coordinate> releaseSiteMap = new TreeMap<String, Coordinate>(); 

	//NetCDF file properties
	String fileName = outputFolder + "disperseMatrixTB.nc";
	//String disperseFileName = outputFolder + "disperseMatrix.nc";
	String releaseSiteName = "releaseSiteID";
	String arrivalSiteName = "arrivalSiteID";
	String latName = "lat";
	String lonName = "lon";
	String probName = "disperseProb";
	String pldName = "avgPLD"; 

	long numPartsReleased = 100000 /*this is total particles released from release.txt file*/ 
											* 15 /*this is the number of release dates*/; 

	
	/**Main stepping method
	 * 
	 */
	public void step(){
		setReleaseSites(releaseFile); 
		setReleaseDates(releaseDatesFile); 

		try {
//			NetcdfFileWriteable pldMatrix = createNetCDFFile(fileName);
			NetcdfFileWriteable disperseMatrix = createNetCDFFile(fileName);

			//syntax here is sum[release ID][arrival ID]
			double[][] plds = new double[releaseSites.size()][releaseSites.size()];
			int[][] totalNum = new int[releaseSites.size()][releaseSites.size()];
			
			while (!releaseDates.isEmpty()){

				//get the next release date in the releaseDates queue
				GregorianCalendar releaseDate = (GregorianCalendar) releaseDates.poll();     
				String pldFolderName = this.outputFolder + df.format(releaseDate.getTime());
				
				File pldFolder = new File(pldFolderName);
				File[] pldListOfFiles = pldFolder.listFiles();

				
				for (int i=0; i<pldListOfFiles.length; i++){
					//loop through all files in each folder, and sum up the 
					File file = pldListOfFiles[i];
					
					int releaseNum = 0;
					String fileName = file.getName();
					if (fileName.equals("Bunces.pld")){
						//subtract 1 to make it an array index start of 0
						releaseNum = 72 - 1; 
					}
					else {
						String[] split = fileName.split("\\.");
						String[] split2 = split[0].split("release");
						releaseNum = Integer.parseInt(split2[1]);
					}
					
					
					try {
						BufferedReader reader = new BufferedReader(new FileReader(file));

						//loop over all release groups in the release.txt file
						for (String line = reader.readLine(); line != null; line = reader.readLine()) {
							String tokens[] = line.split("\t"); 

							int arrivalNum = 0;
							String arrivalSite = tokens[0];
							if (arrivalSite.equals("Bunces")){
								//subtract 1 to make it an array index start of 0
								arrivalNum  = 72 - 1; 
							}
							else {
								String[] split = arrivalSite.split("release");
								arrivalNum  = Integer.parseInt(split[1]);
							}

							double pldSum = Double.parseDouble(tokens[1]);
							int numArriving = Integer.parseInt(tokens[2]);
							
							//multiply pldSum by numArriving so get total pld time for the number
							//of observations
							plds[releaseNum][arrivalNum] += pldSum*numArriving; 
							totalNum[releaseNum][arrivalNum] += numArriving;
						}

						reader.close();

					} catch (FileNotFoundException e) {
						e.printStackTrace();
						System.out.println("catching error " + e);
						System.exit(1);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("catching error " + e);
						System.exit(1);
					}

				}//end of loop over all files in a release date folder


			} // end of while loop over release dates

			//loop through and convert to probabilities, 
			//relative to the total number originally released
			for (int i=0; i<plds.length; i++){
				for (int j=0; j<plds[0].length; j++){

					//need to make sure not dividing by 0
					double avgPLD = 0;
					if (totalNum[i][j] > 0) avgPLD = plds[i][j] / (double) totalNum[i][j]; 

					double[] pldArray = {avgPLD}; 
					Array pldValue = Array.factory(double.class, new int[]{1,1}, pldArray);
					disperseMatrix.write(pldName, new int[]{i,j}, pldValue);

					double disperseProb = totalNum[i][j] / (double) numPartsReleased; 
					double[] disperseArray = {disperseProb}; 
					Array disperseValue = Array.factory(double.class, new int[]{1,1}, disperseArray);
					disperseMatrix.write(probName, new int[]{i,j}, disperseValue);
					

				}
			}
			
			disperseMatrix.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}

	}





	/**Set's the release sites into a linked list queue
	 * 
	 * @param releaseFile
	 */
	public void setReleaseSites(String releaseFile){
		File file = new File(releaseFile); 
		BufferedReader reader; 

		try {
			reader = new BufferedReader(new FileReader(file));

			//loop over all release groups in the release.txt file
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); 

				float lat = Float.parseFloat(tokens[1]);
				float lon = Float.parseFloat(tokens[2]);
				Coordinate coord = new Coordinate(lon, lat, 0);
				String releaseSite = tokens[5];

				releaseSites.add(releaseSite); 
				releaseSiteLocs.add(coord); 

				releaseSiteMap.put(releaseSite, coord);

			}
			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			System.exit(1);
		}

	}



	/**Sets the release dates into an arraylist
	 * 
	 * @param releaseDatesFile
	 */
	public void setReleaseDates(String releaseDatesFile){

		releaseDates = new LinkedList<Calendar>(); 

		File file = new File(releaseDatesFile); 
		BufferedReader reader; 

		try {
			reader = new BufferedReader(new FileReader(file));

			//loop over all release groups in the release.txt file
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t");
				String dateString = tokens[0];
				String dateTokens[] = dateString.split("/"); 
				int month = Integer.parseInt(dateTokens[0]); 
				int day = Integer.parseInt(dateTokens[1]); 
				int year = Integer.parseInt(dateTokens[2]) ;

				String timeString = tokens[1]; 
				String timeTokens[] = timeString.split(":");
				int hour = Integer.parseInt(timeTokens[0]);
				int min = Integer.parseInt(timeTokens[1]); 

				GregorianCalendar releaseDate = 
					new GregorianCalendar(year, month-1, day, hour, min); 
				releaseDate.setTimeZone(TimeZone.getTimeZone("GMT"));
				releaseDates.add(releaseDate); 

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}

	}


	public NetcdfFileWriteable createNetCDFFile(String ncFilename) 
		throws IOException, InvalidRangeException{
		
		new File(ncFilename).delete();

		NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(ncFilename);


		Dimension[] dim = new Dimension[2];


		dim[0] = setDimension(ncFile, DataType.INT, releaseSiteName, 
				"locationID", releaseSites.size(), false);
		dim[1] = setDimension(ncFile, DataType.INT, arrivalSiteName, 
				"locationID", releaseSites.size(), false);

		ncFile.addVariable(probName, DataType.FLOAT, dim);
		ncFile.addVariable(pldName, DataType.FLOAT, dim);
		ncFile.addVariable(latName, DataType.FLOAT, new Dimension[] {dim[0]});
		ncFile.addVariable(lonName, DataType.FLOAT,new Dimension[] {dim[0]});

		ncFile.addVariableAttribute(probName, "units", 
				"prob. of arrival, relative to the total released pre-mortality");
		ncFile.addVariableAttribute(pldName, "units", "average PLD in days");
		ncFile.addVariableAttribute(latName, "units", "degrees_north");
		ncFile.addVariableAttribute(lonName, "units", "degrees_east");

		ncFile.create();

		//write out the Coordinates here
		ArrayFloat.D1 dataLon = new ArrayFloat.D1(dim[0].getLength());
		ArrayFloat.D1 dataLat = new ArrayFloat.D1(dim[0].getLength());
		ArrayInt.D1 dataID = new ArrayInt.D1(dim[0].getLength());

/*		for (int i=0; i<dim[0].getLength(); i++) {
			Coordinate coord = this.releaseSiteLocs.get(i);
			dataLon.set(i,  (float) coord.x); // save in
			dataLat.set(i,  (float) coord.y);
			dataID.set(i, i+1);
		}
*/
		
		for (String key: releaseSiteMap.keySet()){
			Coordinate coord = releaseSiteMap.get(key);
			String[] split = key.split("release");
			int ID =  Integer.parseInt(split[1]);
			dataLon.set(ID,  (float) coord.x); // save in
			dataLat.set(ID,  (float) coord.y);
			dataID.set(ID, ID);

		}

		ncFile.write(lonName, dataLon);
		ncFile.write(latName, dataLat);
		ncFile.write(releaseSiteName, dataID);
		ncFile.write(arrivalSiteName, dataID);
		

		return ncFile; 
	}


	private static Dimension setDimension(NetcdfFileWriteable ncFile,
			DataType type, String name, String units, int length, boolean isUnlimited) {

		Dimension dimension; 
		if (isUnlimited) {
			dimension = ncFile.addUnlimitedDimension(name);
		}
		else{
			dimension = ncFile.addDimension(name, length);
		}
		ncFile.addVariable(name, type, new Dimension[]{dimension});
		ncFile.addVariableAttribute(name, "units", units);


		return dimension;
	}



	public static void main(String[] args) {
		DisperseMatrixPLDBuilder dm = new DisperseMatrixPLDBuilder();
		dm.step();
	}


}
