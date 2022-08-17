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

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.util.TestingUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class DisperseMatrixBuilder {

	String releaseFile =  "output/BOLTs/release.txt"; // "c:\\work\\data\\BOLTs\\SeatroutRuns\\releaseTest.txt";
	String releaseDatesFile = "output/BOLTs/releaseDates.txt";
	LinkedList<Calendar> releaseDates; 
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	String outputFolder = "c:\\work\\data\\BOLTs\\SeatroutRuns\\outputTemp\\";
	ArrayList<String> releaseSites = new ArrayList<String>(); 
	ArrayList<Coordinate>  releaseSiteLocs = new ArrayList<Coordinate>(); 

	//NetCDF file properties
	String ncFileName = outputFolder + "disperseMatrix.nc";
	String releaseSiteName = "releaseSiteID";
	String arrivalSiteName = "arrivalSiteID";
	String latName = "lat";
	String lonName = "lon";
	String probName = "disperseProb";


	long numPartsReleased = 2500*15; 
	
	/**Main stepping method
	 * 
	 */
	public void step(){
		setReleaseSites(releaseFile); 
		setReleaseDates(releaseDatesFile); 

		try {
			NetcdfFileWriteable ncFile = createNetCDFFile(ncFileName);

			//syntax here is sum[release ID][arrival ID]
			double[][] sum = new double[releaseSites.size()][releaseSites.size()];

			while (!releaseDates.isEmpty()){

				//get the next release date in the releaseDates queue
				GregorianCalendar releaseDate = (GregorianCalendar) releaseDates.poll();     
				String folderName = outputFolder + df.format(releaseDate.getTime());

				File folder = new File(folderName);
				File[] listOfFiles = folder.listFiles();

				for (int i=0; i<listOfFiles.length; i++){
					//loop through all files in each folder, and sum up the 
					File file = listOfFiles[i];
					
					int releaseNum = 0;
					String fileName = file.getName();
					if (fileName.equals("Bunces.sum")){
						//subtract 1 to make it an array index start of 0
						releaseNum = 72 - 1; 
					}
					else {
						String[] split = fileName.split("\\.");
						String[] split2 = split[0].split("release");
						releaseNum = Integer.parseInt(split2[1]) - 1;
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
								arrivalNum  = Integer.parseInt(split[1]) - 1;
							}

							int arrivalSum = Integer.parseInt(tokens[1]);

							sum[releaseNum][arrivalNum] += arrivalSum; 
						
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

			//loop through and convert to probabilities, relative to the total number originally released
			for (int i=0; i<sum.length; i++){
				for (int j=0; j<sum[0].length; j++){
					double val = sum[i][j] / (double) numPartsReleased; 
					double[] doubleArray = {val}; 
					Array value = Array.factory(double.class, new int[]{1,1}, doubleArray);
					ncFile.write(probName, new int[]{i,j}, value);
				}
			}
			
			ncFile.close();
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

				GregorianCalendar releaseDate = new GregorianCalendar(year, month-1, day, hour, min); 
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


	public NetcdfFileWriteable createNetCDFFile(String ncFilename) throws IOException, InvalidRangeException{
		new File(ncFilename).delete();

		NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(ncFilename);


		Dimension[] dim = new Dimension[2];


		dim[0] = setDimension(ncFile, DataType.INT, releaseSiteName, "locationID", releaseSites.size(), false);
		dim[1] = setDimension(ncFile, DataType.INT, arrivalSiteName, "locationID", releaseSites.size(), false);

		ncFile.addVariable(probName, DataType.FLOAT, dim);
		ncFile.addVariable(latName, DataType.FLOAT, new Dimension[] {dim[0]});
		ncFile.addVariable(lonName, DataType.FLOAT,new Dimension[] {dim[0]});

		ncFile.addVariableAttribute(probName, "units", "prob. of arrival, relative to the total released pre-mortality");
		ncFile.addVariableAttribute(latName, "units", "degrees_north");
		ncFile.addVariableAttribute(lonName, "units", "degrees_east");

		ncFile.create();

		//write out the Coordinates here
		ArrayFloat.D1 dataLon = new ArrayFloat.D1(dim[0].getLength());
		ArrayFloat.D1 dataLat = new ArrayFloat.D1(dim[0].getLength());
		ArrayInt.D1 dataID = new ArrayInt.D1(dim[0].getLength());

		for (int i=0; i<dim[0].getLength(); i++) {
			Coordinate coord = this.releaseSiteLocs.get(i);
			dataLon.set(i,  (float) coord.x); // save in
			dataLat.set(i,  (float) coord.y);
			dataID.set(i, i+1);
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
		DisperseMatrixBuilder dm = new DisperseMatrixBuilder();
		dm.step();
	}


}
